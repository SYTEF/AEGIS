package io.github.sytef.aegis.foundation.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ApiProblemDetailsTest.FailureEndpointConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class ApiProblemDetailsTest {

    private static final String VALID_CORRELATION_ID = "Problem.Valid_ID-1";
    private static final String SENSITIVE_EXCEPTION_MESSAGE = "synthetic-secret-must-not-leak";
    private static final String FIRST_ORIGIN_SECRET = "first-origin-secret-must-not-leak";
    private static final String SECOND_ORIGIN_SECRET = "second-origin-secret-must-not-leak";
    private static final Set<String> PROBLEM_FIELDS = Set.of(
            "type", "title", "status", "detail", "instance", "code", "correlationId", "timestamp");

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void unexpectedErrorUsesSafeProblemDetailsAndPreservesCorrelation(CapturedOutput output)
            throws Exception {
        HttpResponse<String> response = get(
                "/_test/unexpected?secret=query-must-not-leak", List.of(VALID_CORRELATION_ID));

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.headers().firstValue("Content-Type").orElseThrow())
                .startsWith("application/problem+json");
        assertThat(response.headers().firstValue(CorrelationIdFilter.HEADER_NAME).orElseThrow())
                .isEqualTo(VALID_CORRELATION_ID);
        assertThat(JsonPath.<String>read(response.body(), "$.type"))
                .isEqualTo("urn:aegis:problem:unexpected-error");
        assertThat(JsonPath.<String>read(response.body(), "$.title"))
                .isEqualTo("Erro interno do servidor");
        assertThat(JsonPath.<Integer>read(response.body(), "$.status")).isEqualTo(500);
        assertThat(JsonPath.<String>read(response.body(), "$.detail"))
                .isEqualTo("Ocorreu um erro inesperado.");
        assertThat(JsonPath.<String>read(response.body(), "$.instance"))
                .isEqualTo("/_test/unexpected");
        assertThat(JsonPath.<String>read(response.body(), "$.code"))
                .isEqualTo("UNEXPECTED_ERROR");
        assertThat(JsonPath.<String>read(response.body(), "$.correlationId"))
                .isEqualTo(VALID_CORRELATION_ID);
        assertThat(Instant.parse(JsonPath.read(response.body(), "$.timestamp"))).isNotNull();
        assertExactProblemFields(response.body());
        assertThat(response.body())
                .doesNotContain(
                        SENSITIVE_EXCEPTION_MESSAGE,
                        "IllegalStateException",
                        "stackTrace",
                        "query-must-not-leak");
        assertThat(output.getAll())
                .doesNotContain(
                        SENSITIVE_EXCEPTION_MESSAGE,
                        "query-must-not-leak",
                        "started by",
                        System.getProperty("user.dir"))
                .contains(
                        "\"correlationId\":\"" + VALID_CORRELATION_ID + "\"",
                        "\"exceptionType\":\"java.lang.IllegalStateException\"",
                        "\"failureOriginMethod\":\"failUnexpectedly\"");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("expectedProblemScenarios")
    void expectedFrameworkErrorsUseClosedSafeContract(
            String name,
            String method,
            String path,
            int expectedStatus,
            String expectedCode,
            String accept,
            String contentType,
            String body)
            throws Exception {
        HttpResponse<String> response = request(
                method, path, List.of(VALID_CORRELATION_ID), accept, contentType, body);

        assertProblemContract(response, expectedStatus, expectedCode);
        assertThat(response.body())
                .doesNotContain(
                        "matrix-query-secret",
                        "request-body-secret",
                        "Exception",
                        "stackTrace")
                .doesNotContain("?", ";");
    }

    @Test
    void invalidCorrelationIdIsReplacedConsistentlyInHeaderAndProblemBody() throws Exception {
        HttpResponse<String> response = get("/_test/unexpected", List.of("rejected:value"));

        String responseCorrelationId =
                response.headers().firstValue(CorrelationIdFilter.HEADER_NAME).orElseThrow();
        assertUuidV4(responseCorrelationId);
        assertThat(responseCorrelationId).isNotEqualTo("rejected:value");
        assertThat(JsonPath.<String>read(response.body(), "$.correlationId"))
                .isEqualTo(responseCorrelationId);
        assertThat(response.body()).doesNotContain("rejected:value");
    }

    @Test
    void multipleCorrelationHeadersAreReplaced() throws Exception {
        HttpResponse<String> response = get("/missing", List.of("first-valid", "second-valid"));

        String responseCorrelationId =
                response.headers().firstValue(CorrelationIdFilter.HEADER_NAME).orElseThrow();
        assertUuidV4(responseCorrelationId);
        assertThat(responseCorrelationId).isNotIn("first-valid", "second-valid");
        assertThat(JsonPath.<String>read(response.body(), "$.correlationId"))
                .isEqualTo(responseCorrelationId);
    }

    @Test
    void notFoundProblemDoesNotIncludeQueryString() throws Exception {
        HttpResponse<String> response = get("/missing?secret=query-must-not-leak", List.of());

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(JsonPath.<String>read(response.body(), "$.instance")).isEqualTo("/missing");
        assertThat(response.body()).doesNotContain("query-must-not-leak");
        assertThat(JsonPath.<String>read(response.body(), "$.correlationId"))
                .isEqualTo(response.headers().firstValue(CorrelationIdFilter.HEADER_NAME).orElseThrow());
    }

    @Test
    void matrixParametersNeverReachResponseProblemDetailsOrLogs(CapturedOutput output) throws Exception {
        for (String path : List.of(
                "/missing;jsessionid=SEGREDO",
                "/missing;token=SEGREDO",
                "/missing%3Btoken%3DSEGREDO")) {
            HttpResponse<String> response = get(path, List.of(VALID_CORRELATION_ID));

            assertThat(response.statusCode()).isEqualTo(404);
            assertThat(JsonPath.<String>read(response.body(), "$.instance")).isEqualTo("/missing");
            assertThat(response.body()).doesNotContain("SEGREDO", ";jsessionid", ";token");
            assertThat(response.headers().firstValue(CorrelationIdFilter.HEADER_NAME).orElseThrow())
                    .isEqualTo(VALID_CORRELATION_ID);
        }

        assertThat(output.getAll()).doesNotContain("SEGREDO", ";jsessionid", ";token", "%3Btoken");
    }

    @Test
    void sameExceptionTypeAtDifferentOriginsProducesDistinguishableSafeDiagnostics(CapturedOutput output)
            throws Exception {
        HttpResponse<String> first = get("/_test/origin/first", List.of(VALID_CORRELATION_ID));
        HttpResponse<String> second = get("/_test/origin/second", List.of(VALID_CORRELATION_ID));

        assertThat(first.statusCode()).isEqualTo(500);
        assertThat(second.statusCode()).isEqualTo(500);
        assertThat(output.getAll())
                .contains(
                        "\"failureOriginMethod\":\"failFromFirstOrigin\"",
                        "\"failureOriginMethod\":\"failFromSecondOrigin\"")
                .doesNotContain(FIRST_ORIGIN_SECRET, SECOND_ORIGIN_SECRET);
    }

    private HttpResponse<String> get(String path, List<String> correlationIds)
            throws IOException, InterruptedException {
        return request("GET", path, correlationIds, null, null, null);
    }

    private HttpResponse<String> request(
            String method,
            String path,
            List<String> correlationIds,
            String accept,
            String contentType,
            String body)
            throws IOException, InterruptedException {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl() + path)).method(method, publisher);
        correlationIds.forEach(value -> request.header(CorrelationIdFilter.HEADER_NAME, value));
        if (accept != null) {
            request.header("Accept", accept);
        }
        if (contentType != null) {
            request.header("Content-Type", contentType);
        }
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static Stream<Arguments> expectedProblemScenarios() {
        return Stream.of(
                Arguments.of(
                        "400",
                        "GET",
                        "/_test/number?value=not-a-number&secret=matrix-query-secret",
                        400,
                        "BAD_REQUEST",
                        null,
                        null,
                        null),
                Arguments.of(
                        "404",
                        "GET",
                        "/missing?secret=matrix-query-secret",
                        404,
                        "NOT_FOUND",
                        null,
                        null,
                        null),
                Arguments.of(
                        "405", "POST", "/_test/method", 405, "METHOD_NOT_ALLOWED", null, null, null),
                Arguments.of(
                        "406",
                        "GET",
                        "/_test/representation",
                        406,
                        "NOT_ACCEPTABLE",
                        MediaType.APPLICATION_XML_VALUE,
                        null,
                        null),
                Arguments.of(
                        "415",
                        "POST",
                        "/_test/content",
                        415,
                        "UNSUPPORTED_MEDIA_TYPE",
                        null,
                        MediaType.TEXT_PLAIN_VALUE,
                        "request-body-secret"));
    }

    private static void assertProblemContract(
            HttpResponse<String> response, int expectedStatus, String expectedCode) {
        assertThat(response.statusCode()).isEqualTo(expectedStatus);
        assertThat(response.headers().firstValue("Content-Type").orElseThrow())
                .startsWith("application/problem+json");
        assertThat(response.headers().firstValue(CorrelationIdFilter.HEADER_NAME).orElseThrow())
                .isEqualTo(VALID_CORRELATION_ID);
        assertExactProblemFields(response.body());
        assertThat(JsonPath.<Integer>read(response.body(), "$.status")).isEqualTo(expectedStatus);
        assertThat(JsonPath.<String>read(response.body(), "$.code")).isEqualTo(expectedCode);
        assertThat(JsonPath.<String>read(response.body(), "$.correlationId"))
                .isEqualTo(VALID_CORRELATION_ID);
        assertThat(JsonPath.<String>read(response.body(), "$.title")).isNotBlank();
        assertThat(JsonPath.<String>read(response.body(), "$.detail")).isNotBlank();
        assertThat(Instant.parse(JsonPath.read(response.body(), "$.timestamp"))).isNotNull();
    }

    private static void assertExactProblemFields(String body) {
        Map<String, Object> problem = JsonPath.parse(body).read("$");
        assertThat(problem.keySet()).containsExactlyInAnyOrderElementsOf(PROBLEM_FIELDS);
    }

    private static void assertUuidV4(String value) {
        UUID uuid = UUID.fromString(value);
        assertThat(uuid.version()).isEqualTo(4);
        assertThat(uuid.variant()).isEqualTo(2);
        assertThat(uuid.toString()).isEqualTo(value);
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailureEndpointConfiguration {

        @Bean
        FailureEndpoint failureEndpoint() {
            return new FailureEndpoint();
        }
    }

    @RestController
    static class FailureEndpoint {

        @GetMapping("/_test/unexpected")
        void failUnexpectedly() {
            throw new IllegalStateException(SENSITIVE_EXCEPTION_MESSAGE);
        }

        @GetMapping("/_test/number")
        void requireNumber(@RequestParam int value) {}

        @GetMapping("/_test/method")
        void getOnly() {}

        @GetMapping(value = "/_test/representation", produces = MediaType.APPLICATION_JSON_VALUE)
        Map<String, String> jsonRepresentation() {
            return Map.of("status", "ok");
        }

        @PostMapping(value = "/_test/content", consumes = MediaType.APPLICATION_JSON_VALUE)
        void consumeJson() {}

        @GetMapping("/_test/origin/first")
        void failFromFirstOrigin() {
            throw new IllegalStateException(FIRST_ORIGIN_SECRET);
        }

        @GetMapping("/_test/origin/second")
        void failFromSecondOrigin() {
            throw new IllegalStateException(SECOND_ORIGIN_SECRET);
        }
    }
}
