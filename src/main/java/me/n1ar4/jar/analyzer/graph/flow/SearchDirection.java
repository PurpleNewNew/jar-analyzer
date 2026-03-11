/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.flow;

import java.util.Locale;

public enum SearchDirection {
    FORWARD("forward"),
    BACKWARD("backward"),
    BIDIRECTIONAL("bidirectional");

    private final String displayName;

    SearchDirection(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static SearchDirection parse(String raw, SearchDirection defaultValue) {
        String value = normalize(raw);
        if (value.isEmpty()) {
            return defaultValue == null ? FORWARD : defaultValue;
        }
        return switch (value) {
            case "forward", "source", "fromsource" -> FORWARD;
            case "backward", "reverse", "sink", "fromsink" -> BACKWARD;
            case "bidirectional", "both", "bi" -> BIDIRECTIONAL;
            default -> throw new IllegalArgumentException("invalid_request");
        };
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.length() >= 2) {
            char first = normalized.charAt(0);
            char last = normalized.charAt(normalized.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                normalized = normalized.substring(1, normalized.length() - 1).trim();
            }
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}
