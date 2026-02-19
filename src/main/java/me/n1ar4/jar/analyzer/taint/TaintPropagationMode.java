/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.taint;

import me.n1ar4.jar.analyzer.meta.CompatibilityCode;

import java.util.Locale;

@CompatibilityCode(
        primary = "STRICT/BALANCED semantic taint propagation modes",
        reason = "COMPAT mode is retained as compatibility switch for historical taint behavior"
)
public enum TaintPropagationMode {
    STRICT,
    BALANCED,
    COMPAT;

    private static final String PROP_KEY = "jar.analyzer.taint.propagation";
    private static final TaintPropagationMode CURRENT = resolve();

    public static TaintPropagationMode current() {
        return CURRENT;
    }

    private static TaintPropagationMode resolve() {
        String value = System.getProperty(PROP_KEY);
        if (value == null || value.trim().isEmpty()) {
            return BALANCED;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("strict".equals(normalized)) {
            return STRICT;
        }
        if ("compat".equals(normalized) || "compatible".equals(normalized) || "legacy".equals(normalized)) {
            return COMPAT;
        }
        if ("balanced".equals(normalized) || "default".equals(normalized)) {
            return BALANCED;
        }
        return BALANCED;
    }
}
