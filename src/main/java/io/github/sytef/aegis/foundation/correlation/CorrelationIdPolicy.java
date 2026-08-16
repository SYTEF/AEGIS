package io.github.sytef.aegis.foundation.correlation;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public final class CorrelationIdPolicy {

    public static final int MAX_LENGTH = 128;

    private static final Pattern ALLOWED_VALUE = Pattern.compile("[A-Za-z0-9._-]{1," + MAX_LENGTH + "}");

    public String resolve(List<String> receivedValues) {
        if (receivedValues.size() == 1 && isValid(receivedValues.getFirst())) {
            return receivedValues.getFirst();
        }
        return UUID.randomUUID().toString();
    }

    public boolean isValid(String value) {
        return value != null && ALLOWED_VALUE.matcher(value).matches();
    }
}
