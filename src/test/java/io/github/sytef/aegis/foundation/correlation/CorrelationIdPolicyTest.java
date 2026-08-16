package io.github.sytef.aegis.foundation.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class CorrelationIdPolicyTest {

    private static final String UUID_PATTERN = "[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}";

    private final CorrelationIdPolicy policy = new CorrelationIdPolicy();

    @Test
    void preservesOneCharacterValueExactly() {
        assertThat(policy.resolve(List.of("A"))).isEqualTo("A");
    }

    @Test
    void preservesOneHundredAndTwentyEightCharacterValueExactly() {
        String value = "a".repeat(CorrelationIdPolicy.MAX_LENGTH);

        assertThat(policy.resolve(List.of(value))).isEqualTo(value);
    }

    @Test
    void generatesUuidWhenHeaderIsAbsent() {
        assertGenerated(policy.resolve(List.of()));
    }

    @Test
    void generatesUuidForNullValue() {
        assertGenerated(policy.resolve(Collections.singletonList(null)));
    }

    @ParameterizedTest
    @MethodSource("invalidValues")
    void replacesInvalidValueWithoutNormalizingIt(String invalidValue) {
        String resolved = policy.resolve(List.of(invalidValue));

        assertGenerated(resolved);
        assertThat(resolved).isNotEqualTo(invalidValue);
    }

    @Test
    void rejectsMultipleValuesEvenWhenEachValueIsValid() {
        String resolved = policy.resolve(List.of("first-valid", "second-valid"));

        assertGenerated(resolved);
        assertThat(resolved).isNotIn("first-valid", "second-valid");
    }

    private static Stream<String> invalidValues() {
        return Stream.of(
                "",
                "a".repeat(CorrelationIdPolicy.MAX_LENGTH + 1),
                "contains space",
                " leading",
                "trailing ",
                "inválido",
                "invalid:value",
                "line\r\nbreak");
    }

    private static void assertGenerated(String value) {
        assertThat(value).matches(UUID_PATTERN);
    }
}
