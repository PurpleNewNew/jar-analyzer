/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.utils;

public final class ArchiveVirtualPath {
    private static final String CLASS_PREFIX = "jar-class://";
    private static final String RESOURCE_PREFIX = "jar-resource://";

    private ArchiveVirtualPath() {
    }

    public static String forClass(Integer jarId, String entryPath) {
        int id = jarId == null ? -1 : jarId;
        String entry = normalizeEntry(entryPath);
        if (id <= 0 || entry.isEmpty()) {
            return "";
        }
        return CLASS_PREFIX + id + "/" + entry;
    }

    public static String forResource(Integer jarId, String entryPath) {
        int id = jarId == null ? -1 : jarId;
        String entry = normalizeEntry(entryPath);
        if (id <= 0 || entry.isEmpty()) {
            return "";
        }
        return RESOURCE_PREFIX + id + "/" + entry;
    }

    public static boolean isVirtualClassPath(String value) {
        return value != null && value.startsWith(CLASS_PREFIX);
    }

    public static boolean isVirtualResourcePath(String value) {
        return value != null && value.startsWith(RESOURCE_PREFIX);
    }

    public static boolean isVirtualPath(String value) {
        return isVirtualClassPath(value) || isVirtualResourcePath(value);
    }

    public static Locator parseClass(String value) {
        return parse(value, CLASS_PREFIX);
    }

    public static Locator parseResource(String value) {
        return parse(value, RESOURCE_PREFIX);
    }

    private static Locator parse(String value, String prefix) {
        if (value == null || !value.startsWith(prefix)) {
            return null;
        }
        String rest = value.substring(prefix.length());
        int slash = rest.indexOf('/');
        if (slash <= 0 || slash >= rest.length() - 1) {
            return null;
        }
        String idPart = rest.substring(0, slash).trim();
        String entry = normalizeEntry(rest.substring(slash + 1));
        if (entry.isEmpty()) {
            return null;
        }
        int jarId;
        try {
            jarId = Integer.parseInt(idPart);
        } catch (NumberFormatException ex) {
            return null;
        }
        if (jarId <= 0) {
            return null;
        }
        return new Locator(jarId, entry);
    }

    private static String normalizeEntry(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().replace("\\", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    public record Locator(int jarId, String entryPath) {
    }
}
