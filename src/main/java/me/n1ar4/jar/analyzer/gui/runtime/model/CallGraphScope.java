package me.n1ar4.jar.analyzer.gui.runtime.model;

import java.util.Locale;

public enum CallGraphScope {
    ALL("all"),
    APP("app"),
    LIBRARY("library"),
    SDK("sdk"),
    GENERATED("generated"),
    EXCLUDED("excluded");

    private final String value;

    CallGraphScope(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static CallGraphScope fromValue(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        for (CallGraphScope scope : values()) {
            if (scope.value.equals(value)) {
                return scope;
            }
        }
        return APP;
    }
}
