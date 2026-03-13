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

import java.util.Locale;

public enum TaintPropagationMode {
    STRICT,
    BALANCED;

    public static final String PROP_KEY = "jar.analyzer.taint.propagation";

    public static TaintPropagationMode current() {
        return resolve(System.getProperty(PROP_KEY));
    }

    static TaintPropagationMode resolve(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BALANCED;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("strict".equals(normalized)) {
            return STRICT;
        }
        if ("balanced".equals(normalized) || "default".equals(normalized)) {
            return BALANCED;
        }
        throw new IllegalArgumentException(
                "invalid " + PROP_KEY + "=" + value + ", supported: strict|balanced");
    }
}
