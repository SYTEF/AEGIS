package io.github.sytef.aegis.foundation.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter(new SafeRequestPathPolicy());

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void preservesValidHeaderInResponseRequestAndMdc() throws Exception {
        MockHttpServletRequest request = request("/actuator/info");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "Request.Valid_1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isEqualTo("Request.Valid_1"));

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("Request.Valid_1");
        assertThat(request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE)).isEqualTo("Request.Valid_1");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void replacesInvalidHeaderWithoutEchoingIt() throws Exception {
        MockHttpServletRequest request = request("/actuator/info");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "rejected value");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {});

        String generated = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertUuidV4(generated);
        assertThat(generated).doesNotContain("rejected value");
    }

    @Test
    void replacesMultipleHeaderValues() throws Exception {
        MockHttpServletRequest request = request("/actuator/info");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "first-valid");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "second-valid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {});

        String generated = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertUuidV4(generated);
        assertThat(generated).isNotIn("first-valid", "second-valid");
    }

    @Test
    void restoresPreviousMdcValueEvenWhenChainFails() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "outer-context");
        MockHttpServletRequest request = request("/failure");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response, failingChain()))
                .isInstanceOf(ServletException.class)
                .hasMessage("synthetic failure");

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isEqualTo("outer-context");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isNotBlank();
    }

    @Test
    void isolatesConcurrentRequestsDeterministically() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Observation> first = executor.submit(() -> observeConcurrentRequest("concurrent-one", barrier));
            Future<Observation> second = executor.submit(() -> observeConcurrentRequest("concurrent-two", barrier));

            assertThat(first.get(5, TimeUnit.SECONDS))
                    .isEqualTo(new Observation("concurrent-one", null));
            assertThat(second.get(5, TimeUnit.SECONDS))
                    .isEqualTo(new Observation("concurrent-two", null));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void logsSafeStructuredFieldsWithoutQueryOrRejectedHeader() throws Exception {
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(CorrelationIdFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            MockHttpServletRequest request = request("/safe-path");
            request.setQueryString("secret=must-not-appear");
            request.addHeader(CorrelationIdFilter.HEADER_NAME, "rejected value");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {});

            ILoggingEvent event = appender.list.getLast();
            Map<String, Object> fields = event.getKeyValuePairs().stream()
                    .collect(Collectors.toMap(pair -> pair.key, pair -> pair.value));
            assertThat(event.getFormattedMessage()).isEqualTo("HTTP request completed");
            assertThat(event.getMDCPropertyMap().get(CorrelationIdFilter.MDC_KEY))
                    .isEqualTo(response.getHeader(CorrelationIdFilter.HEADER_NAME));
            assertThat(fields)
                    .containsEntry("event", "http.request.completed")
                    .containsEntry("method", "GET")
                    .containsEntry("path", "/safe-path")
                    .containsEntry("status", 200);
            assertThat(event.toString()).doesNotContain("must-not-appear", "rejected value");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void recordsEscapingErrorAsFailureWithoutSwallowingIt() {
        MockHttpServletRequest request = request("/fatal");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AssertionError failure = new AssertionError("synthetic fatal failure");
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(CorrelationIdFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            assertThatThrownBy(() -> filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
                        throw failure;
                    }))
                    .isSameAs(failure);

            Map<String, Object> fields = appender.list.getLast().getKeyValuePairs().stream()
                    .collect(Collectors.toMap(pair -> pair.key, pair -> pair.value));
            assertThat(fields).containsEntry("status", 500);
            assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void reusesCorrelationDuringErrorDispatchAndCleansMdc() throws Exception {
        MockHttpServletRequest request = request("/failure");
        request.setDispatcherType(DispatcherType.ERROR);
        request.setAttribute("jakarta.servlet.error.request_uri", "/failure");
        request.setAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE, "error-dispatch-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isEqualTo("error-dispatch-id"));

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("error-dispatch-id");
        assertThat(request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE)).isEqualTo("error-dispatch-id");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void doesNotOverwriteHeaderWhenResponseIsAlreadyCommitted() throws Exception {
        MockHttpServletRequest request = request("/committed");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "request-correlation-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setHeader(CorrelationIdFilter.HEADER_NAME, "committed-correlation-id");
        response.setCommitted(true);

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isEqualTo("request-correlation-id"));

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("committed-correlation-id");
        assertThat(request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE)).isEqualTo("request-correlation-id");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    private Observation observeConcurrentRequest(String correlationId, CyclicBarrier barrier) throws Exception {
        MockHttpServletRequest request = request("/concurrent");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, correlationId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] observed = new String[1];

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
                observed[0] = MDC.get(CorrelationIdFilter.MDC_KEY);
            } catch (Exception exception) {
                throw new ServletException(exception);
            }
        });

        return new Observation(observed[0], MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    private static MockHttpServletRequest request(String path) {
        return new MockHttpServletRequest("GET", path);
    }

    private static FilterChain failingChain() {
        return (request, response) -> {
            throw new ServletException("synthetic failure");
        };
    }

    private static void assertUuidV4(String value) {
        UUID uuid = UUID.fromString(value);
        assertThat(uuid.version()).isEqualTo(4);
        assertThat(uuid.variant()).isEqualTo(2);
        assertThat(uuid.toString()).isEqualTo(value);
    }

    private record Observation(String duringRequest, String afterRequest) {}
}
