/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.engine;

public enum DecompileType {
    FERNFLOWER("fernflower"),
    CFR("cfr");

    private final String key;

    DecompileType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static DecompileType parse(String value, DecompileType def) {
        if (value == null) {
            return def;
        }
        String v = value.trim().toLowerCase();
        if (v.isEmpty() || "null".equals(v)) {
            return def;
        }
        if ("cfr".equals(v)) {
            return CFR;
        }
        if ("fernflower".equals(v) || "fern".equals(v) || "ff".equals(v)) {
            return FERNFLOWER;
        }
        return def;
    }
}
