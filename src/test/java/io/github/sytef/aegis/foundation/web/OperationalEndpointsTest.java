package io.github.sytef.aegis.foundation.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "aegis.build.commit-sha=ABCDEF1234567890ABCDEF1234567890ABCDEF12",
            "aegis.test.secret=must-not-be-exposed"
        })
class OperationalEndpointsTest {

    private static final String CORRELATION_ID = "Operational.Valid_ID-1";

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void livenessReportsOnlyProcessAvailability() throws Exception {
        HttpResponse<String> response = get("/actuator/health/liveness", CORRELATION_ID);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElseThrow())
                .startsWith("application/vnd.spring-boot.actuator.v3+json");
        assertThat(response.headers().firstValue(CorrelationIdFilter.HEADER_NAME).orElseThrow())
                .isEqualTo(CORRELATION_ID);
        JSONAssert.assertEquals("{\"status\":\"UP\"}", response.body(), JSONCompareMode.STRICT);
    }

    @Test
    void readinessReportsOnlyCurrentApplicationReadiness() throws Exception {
        HttpResponse<String> response = get("/actuator/health/readiness", CORRELATION_ID);

        assertThat(response.statusCode()).isEqualTo(200);
        JSONAssert.assertEquals("{\"status\":\"UP\"}", response.body(), JSONCompareMode.STRICT);
    }

    @Test
    void infoUsesClosedAllowlist() throws Exception {
        HttpResponse<String> response = get("/actuator/info", CORRELATION_ID);

        assertThat(response.statusCode()).isEqualTo(200);
        JSONAssert.assertEquals(
                """
                {
                  "service": {
                    "name": "aegis",
                    "applicationVersion": "0.1.0-SNAPSHOT"
                  },
                  "build": {
                    "version": "0.1.0-SNAPSHOT",
                    "commitSha": "abcdef1234567890abcdef1234567890abcdef12"
                  }
                }
                """,
                response.body(),
                JSONCompareMode.STRICT);
        assertThat(response.body())
                .doesNotContain(
                        "must-not-be-exposed",
                        "java.version",
                        "user.dir",
                        "os.name",
                        "process",
                        "hostname",
                        "arguments");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/actuator/env",
                "/actuator/beans",
                "/actuator/configprops",
                "/actuator/metrics",
                "/actuator/mappings",
                "/actuator/loggers",
                "/actuator/heapdump",
                "/actuator/threaddump"
            })
    void nonAllowlistedActuatorEndpointIsUnavailable(String endpoint) throws Exception {
        HttpResponse<String> response = get(endpoint, CORRELATION_ID);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.headers().firstValue("Content-Type").orElseThrow())
                .startsWith("application/problem+json");
        assertThat(response.body()).doesNotContain("environment", "propertySources", "must-not-be-exposed");
    }

    private HttpResponse<String> get(String path, String correlationId)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + path))
                .header(CorrelationIdFilter.HEADER_NAME, correlationId)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + port;
    }
}
