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

    public static TaintPropagationMode parse(String value) {
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
                "invalid taint propagation mode=" + value + ", supported: strict|balanced");
    }
}
