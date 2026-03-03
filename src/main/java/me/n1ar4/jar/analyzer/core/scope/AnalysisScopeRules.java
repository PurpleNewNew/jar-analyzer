/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.scope;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AnalysisScopeRules {
    private static final Logger logger = LogManager.getLogger();
    private static final String CONFIG_PATH = "rules/analysis-scope.json";

    private static final List<String> DEFAULT_JDK_CLASS_PREFIXES = List.of(
            "java/",
            "javax/",
            "sun/",
            "com/sun/",
            "jdk/",
            "org/w3c/",
            "org/xml/",
            "org/ietf/",
            "org/omg/",
            "com/oracle/",
            "javafx/"
    );

    private static final List<String> DEFAULT_SDK_JAR_PREFIXES = List.of(
            "rt.jar",
            "jce.jar",
            "tools.jar",
            "jsse.jar",
            "jrt-fs.jar",
            "java.",
            "jdk.",
            "javafx-"
    );

    private static final List<String> DEFAULT_RESOURCE_ALLOW_PREFIXES = List.of(
            "boot-inf/classes/",
            "web-inf/classes/",
            "config/",
            "mapper/",
            "mappers/",
            "static/",
            "public/",
            "templates/",
            "meta-inf/resources/"
    );

    private static volatile RulesData config;

    private AnalysisScopeRules() {
    }

    public static List<String> getForceTargetClassPrefixes() {
        return getConfig().forceTargetClassPrefixes;
    }

    public static List<String> getForceTargetJarPrefixes() {
        return getConfig().forceTargetJarPrefixes;
    }

    public static List<String> getJdkClassPrefixes() {
        return getConfig().jdkClassPrefixes;
    }

    public static List<String> getSdkJarPrefixes() {
        return getConfig().sdkJarPrefixes;
    }

    public static List<String> getCommonLibraryClassPrefixes() {
        return getConfig().commonLibraryClassPrefixes;
    }

    public static List<String> getCommonLibraryJarPrefixes() {
        return getConfig().commonLibraryJarPrefixes;
    }

    public static List<String> getResourceAllowPrefixes() {
        return getConfig().resourceAllowPrefixes;
    }

    public static boolean isForceTargetClass(String className) {
        return matchesClassPrefix(className, getConfig().forceTargetClassPrefixes);
    }

    public static boolean isJdkClass(String className) {
        return matchesClassPrefix(className, getConfig().jdkClassPrefixes);
    }

    public static boolean isCommonLibraryClass(String className) {
        return matchesClassPrefix(className, getConfig().commonLibraryClassPrefixes);
    }

    public static boolean isForceTargetJar(String jarName) {
        return matchesJarPrefix(jarName, getConfig().forceTargetJarPrefixes);
    }

    public static boolean isSdkJar(String jarName) {
        return matchesJarPrefix(jarName, getConfig().sdkJarPrefixes);
    }

    public static boolean isCommonLibraryJar(String jarName) {
        return matchesJarPrefix(jarName, getConfig().commonLibraryJarPrefixes);
    }

    public static synchronized void saveForceTargetClassPrefixes(List<String> prefixes) {
        RulesData current = getConfig();
        RulesData next = new RulesData(
                normalizeClassPrefixes(prefixes),
                current.forceTargetJarPrefixes,
                current.jdkClassPrefixes,
                current.sdkJarPrefixes,
                current.commonLibraryClassPrefixes,
                current.commonLibraryJarPrefixes,
                current.resourceAllowPrefixes
        );
        write(next);
    }

    public static synchronized void saveForceTargetJarPrefixes(List<String> prefixes) {
        RulesData current = getConfig();
        RulesData next = new RulesData(
                current.forceTargetClassPrefixes,
                normalizeJarPrefixes(prefixes),
                current.jdkClassPrefixes,
                current.sdkJarPrefixes,
                current.commonLibraryClassPrefixes,
                current.commonLibraryJarPrefixes,
                current.resourceAllowPrefixes
        );
        write(next);
    }

    public static synchronized void saveCommonLibraryClassPrefixes(List<String> prefixes) {
        RulesData current = getConfig();
        RulesData next = new RulesData(
                current.forceTargetClassPrefixes,
                current.forceTargetJarPrefixes,
                current.jdkClassPrefixes,
                current.sdkJarPrefixes,
                normalizeClassPrefixes(prefixes),
                current.commonLibraryJarPrefixes,
                current.resourceAllowPrefixes
        );
        write(next);
    }

    public static synchronized void saveCommonLibraryJarPrefixes(List<String> prefixes) {
        RulesData current = getConfig();
        RulesData next = new RulesData(
                current.forceTargetClassPrefixes,
                current.forceTargetJarPrefixes,
                current.jdkClassPrefixes,
                current.sdkJarPrefixes,
                current.commonLibraryClassPrefixes,
                normalizeJarPrefixes(prefixes),
                current.resourceAllowPrefixes
        );
        write(next);
    }

    public static synchronized void reload() {
        config = null;
    }

    private static void write(RulesData next) {
        RulesData safe = next == null ? defaultConfig() : next;
        try {
            Path path = Paths.get(CONFIG_PATH);
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            JSONObject obj = new JSONObject();
            obj.put("forceTargetClassPrefixes", safe.forceTargetClassPrefixes);
            obj.put("forceTargetJarPrefixes", safe.forceTargetJarPrefixes);
            obj.put("jdkClassPrefixes", safe.jdkClassPrefixes);
            obj.put("sdkJarPrefixes", safe.sdkJarPrefixes);
            obj.put("commonLibraryClassPrefixes", safe.commonLibraryClassPrefixes);
            obj.put("commonLibraryJarPrefixes", safe.commonLibraryJarPrefixes);
            obj.put("resourceAllowPrefixes", safe.resourceAllowPrefixes);
            String json = JSON.toJSONString(obj, JSONWriter.Feature.PrettyFormat);
            Files.write(path, json.getBytes(StandardCharsets.UTF_8));
            config = safe;
        } catch (Exception ex) {
            logger.warn("save {} failed: {}", CONFIG_PATH, ex.getMessage());
            config = safe;
        }
    }

    private static RulesData getConfig() {
        RulesData local = config;
        if (local != null) {
            return local;
        }
        synchronized (AnalysisScopeRules.class) {
            if (config == null) {
                config = loadConfig();
            }
            return config;
        }
    }

    private static RulesData loadConfig() {
        RulesData defaults = defaultConfig();
        Path path = Paths.get(CONFIG_PATH);
        if (!Files.exists(path)) {
            logger.warn("{} not found", CONFIG_PATH);
            return defaults;
        }
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            Object parsed = JSON.parse(text);
            if (!(parsed instanceof JSONObject obj)) {
                return defaults;
            }
            List<String> forceTargetClassPrefixes = normalizeClassPrefixes(
                    obj.getList("forceTargetClassPrefixes", String.class));
            List<String> forceTargetJarPrefixes = normalizeJarPrefixes(
                    obj.getList("forceTargetJarPrefixes", String.class));
            List<String> jdkClassPrefixes = mergeClassPrefixes(
                    DEFAULT_JDK_CLASS_PREFIXES,
                    obj.getList("jdkClassPrefixes", String.class));
            List<String> sdkJarPrefixes = mergeJarPrefixes(
                    DEFAULT_SDK_JAR_PREFIXES,
                    obj.getList("sdkJarPrefixes", String.class));
            List<String> commonClassPrefixes = normalizeClassPrefixes(
                    obj.getList("commonLibraryClassPrefixes", String.class));
            List<String> commonJarPrefixes = normalizeJarPrefixes(
                    obj.getList("commonLibraryJarPrefixes", String.class));
            List<String> resourceAllowPrefixes = mergeResourcePrefixes(
                    DEFAULT_RESOURCE_ALLOW_PREFIXES,
                    obj.getList("resourceAllowPrefixes", String.class));
            return new RulesData(
                    forceTargetClassPrefixes,
                    forceTargetJarPrefixes,
                    jdkClassPrefixes,
                    sdkJarPrefixes,
                    commonClassPrefixes,
                    commonJarPrefixes,
                    resourceAllowPrefixes
            );
        } catch (Exception ex) {
            logger.warn("load {} failed: {}", CONFIG_PATH, ex.getMessage());
            return defaults;
        }
    }

    private static RulesData defaultConfig() {
        return new RulesData(
                List.of(),
                List.of(),
                normalizeClassPrefixes(DEFAULT_JDK_CLASS_PREFIXES),
                normalizeJarPrefixes(DEFAULT_SDK_JAR_PREFIXES),
                List.of(),
                List.of(),
                normalizeResourcePrefixes(DEFAULT_RESOURCE_ALLOW_PREFIXES)
        );
    }

    private static List<String> mergeClassPrefixes(List<String> defaults, List<String> current) {
        Set<String> merged = new LinkedHashSet<>(normalizeClassPrefixes(defaults));
        merged.addAll(normalizeClassPrefixes(current));
        return List.copyOf(merged);
    }

    private static List<String> mergeJarPrefixes(List<String> defaults, List<String> current) {
        Set<String> merged = new LinkedHashSet<>(normalizeJarPrefixes(defaults));
        merged.addAll(normalizeJarPrefixes(current));
        return List.copyOf(merged);
    }

    private static List<String> mergeResourcePrefixes(List<String> defaults, List<String> current) {
        Set<String> merged = new LinkedHashSet<>(normalizeResourcePrefixes(defaults));
        merged.addAll(normalizeResourcePrefixes(current));
        return List.copyOf(merged);
    }

    private static boolean matchesClassPrefix(String className, List<String> prefixes) {
        if (className == null || className.isBlank() || prefixes == null || prefixes.isEmpty()) {
            return false;
        }
        String normalized = normalizeClassName(className);
        if (normalized.isEmpty()) {
            return false;
        }
        for (String prefix : prefixes) {
            if (prefix == null || prefix.isEmpty()) {
                continue;
            }
            if (prefix.endsWith("/")) {
                if (normalized.startsWith(prefix)) {
                    return true;
                }
                String exact = prefix.substring(0, prefix.length() - 1);
                if (normalized.equals(exact)) {
                    return true;
                }
            } else if (normalized.equals(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesJarPrefix(String jarName, List<String> prefixes) {
        if (jarName == null || jarName.isBlank() || prefixes == null || prefixes.isEmpty()) {
            return false;
        }
        String normalized = normalizeJarName(jarName);
        if (normalized.isEmpty()) {
            return false;
        }
        for (String prefix : prefixes) {
            if (prefix == null || prefix.isEmpty()) {
                continue;
            }
            if (normalized.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeClassName(String className) {
        String normalized = className.trim();
        if (normalized.endsWith(".class")) {
            normalized = normalized.substring(0, normalized.length() - ".class".length());
        }
        normalized = normalized.replace('\\', '/').replace('.', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static String normalizeJarName(String jarName) {
        String normalized = jarName.trim().replace('\\', '/').toLowerCase(Locale.ROOT);
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < normalized.length()) {
            normalized = normalized.substring(slash + 1);
        }
        return normalized;
    }

    private static List<String> normalizeClassPrefixes(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> out = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String normalized = value.trim()
                    .replace('\\', '/')
                    .replace('.', '/')
                    .replaceAll("\\*+$", "");
            if (!normalized.isEmpty()) {
                out.add(normalized);
            }
        }
        return out.isEmpty() ? Collections.emptyList() : List.copyOf(out);
    }

    private static List<String> normalizeJarPrefixes(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> out = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String normalized = value.trim()
                    .replace('\\', '/')
                    .toLowerCase(Locale.ROOT)
                    .replaceAll("\\*+$", "");
            if (!normalized.isEmpty()) {
                out.add(normalized);
            }
        }
        return out.isEmpty() ? Collections.emptyList() : List.copyOf(out);
    }

    private static List<String> normalizeResourcePrefixes(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> out = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String normalized = value.trim()
                    .replace('\\', '/')
                    .toLowerCase(Locale.ROOT)
                    .replaceAll("\\*+$", "");
            if (normalized.isEmpty()) {
                continue;
            }
            if (!normalized.endsWith("/")) {
                normalized = normalized + "/";
            }
            out.add(normalized);
        }
        return out.isEmpty() ? Collections.emptyList() : List.copyOf(out);
    }

    public static String buildPrefixText(List<String> prefixes, String title) {
        String safeTitle = title == null ? "" : title.trim();
        List<String> safePrefixes = prefixes == null ? List.of() : prefixes;
        StringBuilder sb = new StringBuilder();
        if (!safeTitle.isEmpty()) {
            sb.append("# ").append(safeTitle).append('\n');
        }
        for (String prefix : safePrefixes) {
            if (prefix == null) {
                continue;
            }
            String value = prefix.trim();
            if (value.isEmpty()) {
                continue;
            }
            sb.append(value);
            if (!value.endsWith(";")) {
                sb.append(';');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static final class RulesData {
        private final List<String> forceTargetClassPrefixes;
        private final List<String> forceTargetJarPrefixes;
        private final List<String> jdkClassPrefixes;
        private final List<String> sdkJarPrefixes;
        private final List<String> commonLibraryClassPrefixes;
        private final List<String> commonLibraryJarPrefixes;
        private final List<String> resourceAllowPrefixes;

        private RulesData(List<String> forceTargetClassPrefixes,
                          List<String> forceTargetJarPrefixes,
                          List<String> jdkClassPrefixes,
                          List<String> sdkJarPrefixes,
                          List<String> commonLibraryClassPrefixes,
                          List<String> commonLibraryJarPrefixes,
                          List<String> resourceAllowPrefixes) {
            this.forceTargetClassPrefixes = forceTargetClassPrefixes == null
                    ? List.of() : List.copyOf(forceTargetClassPrefixes);
            this.forceTargetJarPrefixes = forceTargetJarPrefixes == null
                    ? List.of() : List.copyOf(forceTargetJarPrefixes);
            this.jdkClassPrefixes = jdkClassPrefixes == null
                    ? List.of() : List.copyOf(jdkClassPrefixes);
            this.sdkJarPrefixes = sdkJarPrefixes == null
                    ? List.of() : List.copyOf(sdkJarPrefixes);
            this.commonLibraryClassPrefixes = commonLibraryClassPrefixes == null
                    ? List.of() : List.copyOf(commonLibraryClassPrefixes);
            this.commonLibraryJarPrefixes = commonLibraryJarPrefixes == null
                    ? List.of() : List.copyOf(commonLibraryJarPrefixes);
            this.resourceAllowPrefixes = resourceAllowPrefixes == null
                    ? List.of() : List.copyOf(resourceAllowPrefixes);
        }
    }
}
