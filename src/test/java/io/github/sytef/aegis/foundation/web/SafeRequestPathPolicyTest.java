package io.github.sytef.aegis.foundation.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

class SafeRequestPathPolicyTest {

    private final SafeRequestPathPolicy policy = new SafeRequestPathPolicy();

    @Test
    void preservesValidPathWithoutQueryString() {
        MockHttpServletRequest request = request("/valid-path_1/resource.json?secret=must-not-appear");

        assertThat(policy.forLogging(request)).isEqualTo("/valid-path_1/resource.json");
        assertThat(policy.forProblemInstance(request).toString()).isEqualTo("/valid-path_1/resource.json");
    }

    @Test
    void removesAllMatrixParametersAndControlCharacters() {
        MockHttpServletRequest request = request("/missing;jsessionid=SEGREDO/child;token=SEGREDO\r\nFORGED");

        assertThat(policy.forLogging(request)).isEqualTo("/missing/child");
        assertThat(policy.forProblemInstance(request).toString()).isEqualTo("/missing/child");
    }

    @Test
    void removesPercentEncodedMatrixDelimiterAndControlCharacters() {
        MockHttpServletRequest request = request("/missing%3Btoken%3DSEGREDO/safe%0d%0a-path");

        assertThat(policy.forLogging(request)).isEqualTo("/missing/safe-path");
        assertThat(policy.forProblemInstance(request).toString()).isEqualTo("/missing/safe-path");
    }

    @Test
    void limitsConservativeFallback() {
        MockHttpServletRequest request = request("/" + "a".repeat(700));

        assertThat(policy.forLogging(request))
                .hasSize(SafeRequestPathPolicy.MAX_PATH_LENGTH)
                .startsWith("/");
        assertThat(policy.forProblemInstance(request).toString()).hasSize(SafeRequestPathPolicy.MAX_PATH_LENGTH);
    }

    @Test
    void prefersResolvedRoutePatternForLogging() {
        MockHttpServletRequest request = request("/products/customer-controlled-id");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/products/{id}");

        assertThat(policy.forLogging(request)).isEqualTo("/products/{id}");
        assertThat(policy.forProblemInstance(request).toString()).isEqualTo("/products/customer-controlled-id");
    }

    @Test
    void fallsBackToRootForInvalidUriRepresentation() {
        MockHttpServletRequest request = request("relative\\invalid path");

        assertThat(policy.forLogging(request)).isEqualTo("/");
        assertThat(policy.forProblemInstance(request).toString()).isEqualTo("/");
    }

    private static MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setRequestURI(uri);
        return request;
    }
}
