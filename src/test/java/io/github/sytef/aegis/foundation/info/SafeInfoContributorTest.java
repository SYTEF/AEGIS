package io.github.sytef.aegis.foundation.info;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.info.BuildProperties;

class SafeInfoContributorTest {

    @Test
    void exposesOnlyAllowlistedInformationAndNormalizesValidSha() {
        Info info = infoFor("ABCDEF1234567890ABCDEF1234567890ABCDEF12");

        assertThat(info.getDetails()).containsOnlyKeys("service", "build");
        assertThat(asMap(info.get("service")))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "name", "aegis",
                        "applicationVersion", "0.1.0-SNAPSHOT"));
        assertThat(asMap(info.get("build")))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "version", "0.1.0-SNAPSHOT",
                        "commitSha", "abcdef1234567890abcdef1234567890abcdef12"));
    }

    @Test
    void acceptsSevenCharacterCommitShaBoundary() {
        assertThat(asMap(infoFor("ABCDEF1").get("build")))
                .containsEntry("commitSha", "abcdef1");
    }

    @Test
    void acceptsSixtyFourCharacterCommitShaBoundary() {
        String commitSha = "A".repeat(64);

        assertThat(asMap(infoFor(commitSha).get("build")))
                .containsEntry("commitSha", "a".repeat(64));
    }

    @ParameterizedTest
    @MethodSource("invalidCommitShas")
    void omitsInvalidOrAbsentCommitSha(String invalidCommitSha) {
        Info info = infoFor(invalidCommitSha);

        assertThat(asMap(info.get("build")))
                .containsExactlyInAnyOrderEntriesOf(Map.of("version", "0.1.0-SNAPSHOT"));
    }

    private static Stream<String> invalidCommitShas() {
        return Stream.of(
                "",
                "abcdef",
                "g123456",
                "abc def1",
                "a".repeat(65));
    }

    private static Info infoFor(String commitSha) {
        Properties properties = new Properties();
        properties.setProperty("version", "0.1.0-SNAPSHOT");
        SafeInfoContributor contributor =
                new SafeInfoContributor(new BuildProperties(properties), commitSha);
        Info.Builder builder = new Info.Builder();
        contributor.contribute(builder);
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) value;
    }
}
