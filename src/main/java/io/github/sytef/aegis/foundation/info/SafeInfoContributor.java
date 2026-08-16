package io.github.sytef.aegis.foundation.info;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

@Component
public final class SafeInfoContributor implements InfoContributor {

    private static final Pattern VALID_COMMIT_SHA = Pattern.compile("[A-Fa-f0-9]{7,64}");

    private final BuildProperties buildProperties;
    private final String commitSha;

    public SafeInfoContributor(
            BuildProperties buildProperties, @Value("${aegis.build.commit-sha:}") String commitSha) {
        this.buildProperties = buildProperties;
        this.commitSha = commitSha;
    }

    @Override
    public void contribute(Info.Builder builder) {
        String version = buildProperties.getVersion();
        Map<String, Object> service = Map.of(
                "name", "aegis",
                "applicationVersion", version);

        Map<String, Object> build = new LinkedHashMap<>();
        build.put("version", version);
        if (VALID_COMMIT_SHA.matcher(commitSha).matches()) {
            build.put("commitSha", commitSha.toLowerCase(Locale.ROOT));
        }

        builder.withDetail("service", service).withDetail("build", build);
    }
}
