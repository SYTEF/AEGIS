package io.github.sytef.aegis.foundation.web;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;

@Component
public final class SafeRequestPathPolicy {

    static final int MAX_PATH_LENGTH = 512;

    public String forLogging(HttpServletRequest request) {
        Object routePattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (routePattern != null) {
            String safePattern = sanitize(routePattern.toString());
            if (!safePattern.equals("/")) {
                return safePattern;
            }
        }
        return sanitize(request.getRequestURI());
    }

    public URI forProblemInstance(HttpServletRequest request) {
        String safePath = sanitize(request.getRequestURI());
        try {
            return URI.create(safePath);
        } catch (IllegalArgumentException exception) {
            return URI.create("/");
        }
    }

    String sanitize(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return "/";
        }

        StringBuilder safePath = new StringBuilder(Math.min(candidate.length(), MAX_PATH_LENGTH));
        boolean insideMatrixParameter = false;
        for (int index = 0; index < candidate.length() && safePath.length() < MAX_PATH_LENGTH; index++) {
            char current = candidate.charAt(index);
            int percentEncodedCharacter = decodePercentEncodedCharacter(candidate, index);
            if (current == '?') {
                break;
            }
            if (current == ';' || percentEncodedCharacter == ';') {
                insideMatrixParameter = true;
                if (percentEncodedCharacter >= 0) {
                    index += 2;
                }
                continue;
            }
            if (insideMatrixParameter) {
                if (current == '/' || percentEncodedCharacter == '/') {
                    insideMatrixParameter = false;
                    safePath.append('/');
                    if (percentEncodedCharacter >= 0) {
                        index += 2;
                    }
                }
                continue;
            }
            if (Character.isISOControl(current)
                    || (percentEncodedCharacter >= 0 && Character.isISOControl(percentEncodedCharacter))) {
                if (percentEncodedCharacter >= 0) {
                    index += 2;
                }
                continue;
            }
            safePath.append(current);
        }

        String result = safePath.toString();
        if (!result.startsWith("/")) {
            return "/";
        }
        while (result.startsWith("//")) {
            result = result.substring(1);
        }
        return result.isBlank() ? "/" : result;
    }

    private static int decodePercentEncodedCharacter(String value, int percentIndex) {
        if (value.charAt(percentIndex) != '%' || percentIndex + 2 >= value.length()) {
            return -1;
        }
        int high = Character.digit(value.charAt(percentIndex + 1), 16);
        int low = Character.digit(value.charAt(percentIndex + 2), 16);
        return high < 0 || low < 0 ? -1 : (high << 4) + low;
    }
}
