/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.flow;

import java.util.Locale;

public enum TraversalMode {
    CALL_ONLY,
    CALL_ALIAS;

    public static TraversalMode parse(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "call+alias" -> CALL_ALIAS;
            default -> CALL_ONLY;
        };
    }

    public boolean includesAlias() {
        return this == CALL_ALIAS;
    }

    public String displayName() {
        return this == CALL_ALIAS ? "call+alias" : "call-only";
    }
}
