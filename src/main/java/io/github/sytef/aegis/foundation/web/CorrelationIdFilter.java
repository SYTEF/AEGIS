package io.github.sytef.aegis.foundation.web;

import io.github.sytef.aegis.foundation.correlation.CorrelationIdPolicy;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";
    public static final String REQUEST_ATTRIBUTE = CorrelationIdFilter.class.getName() + ".correlationId";

    private static final Logger LOGGER = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private final CorrelationIdPolicy correlationIdPolicy = new CorrelationIdPolicy();
    private final SafeRequestPathPolicy safeRequestPathPolicy;

    public CorrelationIdFilter(SafeRequestPathPolicy safeRequestPathPolicy) {
        this.safeRequestPathPolicy = safeRequestPathPolicy;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = effectiveCorrelationId(request);
        String previousCorrelationId = MDC.get(MDC_KEY);
        long startedAt = System.nanoTime();
        boolean failed = false;

        request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
        if (!response.isCommitted()) {
            response.setHeader(HEADER_NAME, correlationId);
        }
        MDC.put(MDC_KEY, correlationId);

        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException | Error exception) {
            failed = true;
            throw exception;
        } finally {
            try {
                if (request.getDispatcherType() == DispatcherType.REQUEST) {
                    logRequestCompletion(request, response, startedAt, failed);
                }
            } finally {
                restoreMdc(previousCorrelationId);
            }
        }
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    private String effectiveCorrelationId(HttpServletRequest request) {
        Object existing = request.getAttribute(REQUEST_ATTRIBUTE);
        if (existing instanceof String correlationId && correlationIdPolicy.isValid(correlationId)) {
            return correlationId;
        }

        List<String> values = Collections.list(request.getHeaders(HEADER_NAME));
        return correlationIdPolicy.resolve(values);
    }

    private void logRequestCompletion(
            HttpServletRequest request, HttpServletResponse response, long startedAt, boolean failed) {
        long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;
        int status = failed ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : response.getStatus();

        LOGGER.atInfo()
                .addKeyValue("event", "http.request.completed")
                .addKeyValue("method", request.getMethod())
                .addKeyValue("path", safeRequestPathPolicy.forLogging(request))
                .addKeyValue("status", status)
                .addKeyValue("durationMs", durationMillis)
                .log("HTTP request completed");
    }

    private static void restoreMdc(String previousCorrelationId) {
        if (previousCorrelationId == null) {
            MDC.remove(MDC_KEY);
        } else {
            MDC.put(MDC_KEY, previousCorrelationId);
        }
    }
}
