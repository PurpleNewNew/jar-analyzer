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

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import me.n1ar4.jar.analyzer.utils.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import me.n1ar4.jar.analyzer.starter.Const;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CommonFilterUtil {
    private static final Logger logger = LogManager.getLogger();
    private static final String CONFIG_PATH = "rules/search-filter.json";
    private static final String[] DEFAULT_JDK_PREFIXES = new String[]{
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
            "javafx/",
    };
    private static final String[] DEFAULT_RESOURCE_ALLOW_PREFIXES = new String[]{
            "boot-inf/classes/",
            "web-inf/classes/",
            "config/",
            "mapper/",
            "mappers/",
            "static/",
            "public/",
            "templates/",
            "meta-inf/resources/"
    };
    private static volatile FilterConfig config;

    private CommonFilterUtil() {
    }

    public static List<String> getClassPrefixes() {
        return getConfig().classPrefixes;
    }

    public static List<String> getJarPrefixes() {
        return getConfig().jarPrefixes;
    }

    public static List<String> getResourceAllowPrefixes() {
        return getConfig().resourceAllowPrefixes;
    }

    public static boolean isFilteredClass(String className) {
        if (StringUtil.isNull(className)) {
            return false;
        }
        if (isModuleInfoClass(className)) {
            return true;
        }
        String norm = className.replace('.', '/');
        for (String prefix : getConfig().classPrefixes) {
            if (prefix == null || prefix.isEmpty()) {
                continue;
            }
            if (prefix.endsWith("/")) {
                if (norm.startsWith(prefix)) {
                    return true;
                }
                String exact = prefix.substring(0, prefix.length() - 1);
                if (norm.equals(exact)) {
                    return true;
                }
            } else if (norm.equals(prefix)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isModuleInfoClassName(String className) {
        return isModuleInfoClass(className);
    }

    public static boolean isFilteredResourcePath(String resourcePath) {
        if (StringUtil.isNull(resourcePath)) {
            return false;
        }
        String normalized = normalizeResourcePath(resourcePath);
        if (normalized.isEmpty()) {
            return false;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        String path = stripJarRootPrefix(lower);
        if (isHardBlacklistedResource(path)) {
            return true;
        }
        return !isResourceAllowed(path);
    }

    private static boolean isModuleInfoClass(String className) {
        if (className == null) {
            return false;
        }
        String norm = className.trim();
        if (norm.isEmpty()) {
            return false;
        }
        if (norm.endsWith(".class")) {
            norm = norm.substring(0, norm.length() - ".class".length());
        }
        norm = norm.replace('.', '/');
        if (norm.startsWith("/")) {
            norm = norm.substring(1);
        }
        return "module-info".equalsIgnoreCase(norm);
    }

    private static boolean isModuleInfoResource(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        String base = baseName(path);
        if (base.isEmpty()) {
            return false;
        }
        String name = base.toLowerCase(Locale.ROOT);
        if ("module-info.class".equals(name)) {
            return true;
        }
        return "module-info.java".equals(name);
    }

    private static boolean isHardBlacklistedResource(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        if (isModuleInfoResource(path)) {
            return true;
        }
        if (path.endsWith(".kotlin_module")) {
            return true;
        }
        if (isHardMetaInfNoise(path)) {
            return true;
        }
        String stripped = stripBootWebClassesPrefix(path);
        if (!stripped.equals(path)) {
            if (isModuleInfoResource(stripped)) {
                return true;
            }
            if (stripped.endsWith(".kotlin_module")) {
                return true;
            }
            return isHardMetaInfNoise(stripped);
        }
        return false;
    }

    private static boolean isHardMetaInfNoise(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        if (path.startsWith("meta-inf/maven/")) {
            return true;
        }
        if (path.startsWith("meta-inf/") && endsWithAny(path, ".sf", ".rsa", ".dsa", ".ec")) {
            return true;
        }
        return false;
    }

    private static boolean isResourceAllowed(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        String stripped = stripBootWebClassesPrefix(path);
        if (isAllowedByPrefixes(path) || isAllowedByPrefixes(stripped)) {
            return true;
        }
        if (isAllowedByFileName(path) || isAllowedByFileName(stripped)) {
            return true;
        }
        return isRootResource(path) || isRootResource(stripped);
    }

    private static boolean isAllowedByPrefixes(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        for (String prefix : getConfig().resourceAllowPrefixes) {
            if (prefix == null || prefix.isEmpty()) {
                continue;
            }
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAllowedByFileName(String path) {
        String base = baseName(path);
        if (base.isEmpty()) {
            return false;
        }
        if (base.startsWith("application") && (base.endsWith(".yml") || base.endsWith(".yaml"))) {
            return true;
        }
        if (base.endsWith(".properties")) {
            return true;
        }
        return base.endsWith(".xml");
    }

    private static boolean isRootResource(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return path.indexOf('/') < 0;
    }

    private static String stripBootWebClassesPrefix(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        String bootPrefix = "boot-inf/classes/";
        if (path.startsWith(bootPrefix)) {
            return path.substring(bootPrefix.length());
        }
        String webPrefix = "web-inf/classes/";
        if (path.startsWith(webPrefix)) {
            return path.substring(webPrefix.length());
        }
        return path;
    }

    private static String stripJarRootPrefix(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        int slash = path.indexOf('/');
        if (slash <= 0) {
            return path;
        }
        String head = path.substring(0, slash);
        if (isJarRootName(head)) {
            return path.substring(slash + 1);
        }
        return path;
    }

    private static boolean isJarRootName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (!name.startsWith("jar-")) {
            return false;
        }
        int lastDash = name.lastIndexOf('-');
        if (lastDash <= 3) {
            return false;
        }
        String idPart = name.substring(4, lastDash);
        if (idPart.isEmpty()) {
            return false;
        }
        for (int i = 0; i < idPart.length(); i++) {
            if (!Character.isDigit(idPart.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBuildInfo(String path) {
        String base = baseName(path);
        if (base.isEmpty()) {
            return false;
        }
        return "build-info.properties".equals(base) || "git.properties".equals(base);
    }

    private static boolean isNoiseDoc(String path) {
        String base = baseName(path);
        if (base.isEmpty()) {
            return false;
        }
        if (startsWithAny(base, "license", "licence", "notice", "readme", "changelog", "changes",
                "dependencies", "copyright")) {
            return true;
        }
        String name = base.toLowerCase(Locale.ROOT);
        return name.equals("al2.0")
                || name.equals("apache-2.0")
                || name.equals("apache-2.0.txt")
                || name.equals("lgpl2.1")
                || name.equals("lgpl-2.1")
                || name.equals("gpl2")
                || name.equals("gpl-2.0")
                || name.equals("gpl3")
                || name.equals("gpl-3.0")
                || name.equals("mpl2.0")
                || name.equals("mpl-2.0")
                || name.equals("cddl1.0")
                || name.equals("cddl-1.0")
                || name.equals("epl1.0")
                || name.equals("epl-1.0")
                || name.equals("epl2.0")
                || name.equals("epl-2.0")
                || name.equals("bsd-2-clause")
                || name.equals("bsd-3-clause")
                || name.equals("mit")
                || name.equals("mit-license");
    }

    private static boolean startsWithAny(String value, String... prefixes) {
        if (value == null || value.isEmpty() || prefixes == null) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        for (String prefix : prefixes) {
            if (prefix == null || prefix.isEmpty()) {
                continue;
            }
            if (lower.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean endsWithAny(String value, String... suffixes) {
        if (value == null || value.isEmpty() || suffixes == null) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        for (String suffix : suffixes) {
            if (suffix == null || suffix.isEmpty()) {
                continue;
            }
            if (lower.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private static String baseName(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        int idx = path.lastIndexOf('/');
        if (idx >= 0 && idx + 1 < path.length()) {
            return path.substring(idx + 1);
        }
        return path;
    }

    private static String normalizeResourcePath(String path) {
        String normalized = path.replace("\\", "/");
        String tempRoot = Const.tempDir.replace("\\", "/");
        String tempPrefix = tempRoot.endsWith("/") ? tempRoot : tempRoot + "/";
        int idx = normalized.indexOf(tempPrefix);
        if (idx >= 0) {
            normalized = normalized.substring(idx + tempPrefix.length());
            String resourcesPrefix = Const.resourceDir + "/";
            if (normalized.startsWith(resourcesPrefix)) {
                normalized = normalized.substring(resourcesPrefix.length());
                int slash = normalized.indexOf('/');
                if (slash >= 0 && slash + 1 < normalized.length()) {
                    normalized = normalized.substring(slash + 1);
                }
            }
        }
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    public static String buildClassPrefixText() {
        StringBuilder sb = new StringBuilder();
        sb.append("# class / package black list (rules/search-filter.json)\n");
        for (String prefix : getClassPrefixes()) {
            if (prefix == null) {
                continue;
            }
            String value = prefix.trim();
            if (value.isEmpty()) {
                continue;
            }
            sb.append(value);
            if (!value.endsWith(";")) {
                sb.append(";");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public static String buildJarPrefixText() {
        StringBuilder sb = new StringBuilder();
        sb.append("# jar black list (rules/search-filter.json)\n");
        for (String prefix : getJarPrefixes()) {
            if (prefix == null) {
                continue;
            }
            String value = prefix.trim();
            if (value.isEmpty()) {
                continue;
            }
            sb.append(value);
            if (!value.endsWith(";")) {
                sb.append(";");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public static synchronized void saveClassPrefixes(List<String> classPrefixes) {
        List<String> normalized = normalizeClassPrefixes(classPrefixes);
        List<String> merged = mergeClassPrefixes(Arrays.asList(DEFAULT_JDK_PREFIXES), normalized);
        FilterConfig disk = loadConfig();
        List<String> jarPrefixes = normalizeJarPrefixes(disk.jarPrefixes);
        List<String> resourceAllowPrefixes = normalizeResourcePrefixes(disk.resourceAllowPrefixes);
        if (sameList(disk.classPrefixes, merged)) {
            return;
        }
        writeConfig(merged, jarPrefixes, resourceAllowPrefixes);
        FilterConfig out = new FilterConfig();
        out.classPrefixes = merged;
        out.jarPrefixes = jarPrefixes;
        out.resourceAllowPrefixes = resourceAllowPrefixes;
        config = out;
    }

    public static synchronized void saveJarPrefixes(List<String> jarPrefixes) {
        List<String> normalized = normalizeJarPrefixes(jarPrefixes);
        FilterConfig disk = loadConfig();
        List<String> classPrefixes = normalizeClassPrefixes(disk.classPrefixes);
        List<String> resourceAllowPrefixes = normalizeResourcePrefixes(disk.resourceAllowPrefixes);
        if (sameList(disk.jarPrefixes, normalized)) {
            return;
        }
        writeConfig(classPrefixes, normalized, resourceAllowPrefixes);
        FilterConfig out = new FilterConfig();
        out.classPrefixes = classPrefixes;
        out.jarPrefixes = normalized;
        out.resourceAllowPrefixes = resourceAllowPrefixes;
        config = out;
    }

    public static synchronized void reload() {
        config = null;
    }

    public static boolean isFilteredJar(String jarName) {
        if (StringUtil.isNull(jarName)) {
            return false;
        }
        String name = normalizeJarName(jarName);
        if (name.isEmpty()) {
            return false;
        }
        for (String prefix : getConfig().jarPrefixes) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static FilterConfig getConfig() {
        if (config != null) {
            return config;
        }
        synchronized (CommonFilterUtil.class) {
            if (config == null) {
                config = loadConfig();
            }
        }
        return config;
    }

    private static FilterConfig loadConfig() {
        FilterConfig out = new FilterConfig();
        out.classPrefixes = normalizeClassPrefixes(Arrays.asList(DEFAULT_JDK_PREFIXES));
        out.jarPrefixes = Collections.emptyList();
        out.resourceAllowPrefixes = normalizeResourcePrefixes(Arrays.asList(DEFAULT_RESOURCE_ALLOW_PREFIXES));

        Path path = Paths.get(CONFIG_PATH);
        if (!Files.exists(path)) {
            logger.warn("rules/search-filter.json not found");
            return out;
        }
        try (InputStream is = Files.newInputStream(path)) {
            String text = new String(IOUtils.readAllBytes(is), StandardCharsets.UTF_8);
            Object parsed = JSON.parse(text);
            if (parsed instanceof JSONObject) {
                applyObject((JSONObject) parsed, out);
            } else if (parsed instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) parsed;
                List<String> values = new ArrayList<>();
                for (Object item : list) {
                    if (item != null) {
                        values.add(item.toString());
                    }
                }
                out.jarPrefixes = normalizeJarPrefixes(values);
            }
        } catch (Exception ex) {
            logger.warn("load rules/search-filter.json failed: {}", ex.getMessage());
        }
        return out;
    }

    private static void applyObject(JSONObject obj, FilterConfig out) {
        List<String> classPrefixes = obj.getList("classPrefixes", String.class);
        if (classPrefixes == null) {
            classPrefixes = obj.getList("jdkPrefixes", String.class);
        }
        List<String> jarPrefixes = obj.getList("jarPrefixes", String.class);
        if (jarPrefixes == null) {
            jarPrefixes = obj.getList("thirdPartyPrefixes", String.class);
        }
        List<String> resourceAllowPrefixes = obj.getList("resourceAllowPrefixes", String.class);
        if (classPrefixes != null) {
            out.classPrefixes = mergeClassPrefixes(out.classPrefixes, classPrefixes);
        }
        if (jarPrefixes != null) {
            out.jarPrefixes = normalizeJarPrefixes(jarPrefixes);
        }
        if (resourceAllowPrefixes != null) {
            out.resourceAllowPrefixes = normalizeResourcePrefixes(resourceAllowPrefixes);
        }
    }

    private static List<String> mergeClassPrefixes(List<String> base, List<String> extra) {
        Set<String> merged = new LinkedHashSet<>();
        merged.addAll(normalizeClassPrefixes(base));
        merged.addAll(normalizeClassPrefixes(extra));
        return new ArrayList<>(merged);
    }

    private static List<String> normalizeClassPrefixes(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> out = new LinkedHashSet<>();
        for (String value : values) {
            String norm = normalizeClassPrefix(value);
            if (!norm.isEmpty()) {
                out.add(norm);
            }
        }
        return new ArrayList<>(out);
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
            String norm = value.trim().toLowerCase();
            if (!norm.isEmpty()) {
                out.add(norm);
            }
        }
        return new ArrayList<>(out);
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
            String norm = value.trim().replace("\\", "/").toLowerCase(Locale.ROOT);
            if (norm.isEmpty()) {
                continue;
            }
            if (!norm.endsWith("/")) {
                norm = norm + "/";
            }
            out.add(norm);
        }
        return new ArrayList<>(out);
    }

    private static String normalizeClassPrefix(String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim();
        if (v.isEmpty()) {
            return "";
        }
        v = v.replace(".", "/");
        return v;
    }

    private static String normalizeJarName(String jarName) {
        String name = jarName.replace("\\", "/").toLowerCase();
        int slash = name.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < name.length()) {
            return name.substring(slash + 1);
        }
        return name;
    }

    private static boolean sameList(List<String> left, List<String> right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (left.size() != right.size()) {
            return false;
        }
        for (int i = 0; i < left.size(); i++) {
            if (!left.get(i).equals(right.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static void writeConfig(List<String> classPrefixes,
                                    List<String> jarPrefixes,
                                    List<String> resourceAllowPrefixes) {
        try {
            Path path = Paths.get(CONFIG_PATH);
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            JSONObject obj = new JSONObject();
            obj.put("classPrefixes", classPrefixes);
            obj.put("jarPrefixes", jarPrefixes);
            obj.put("resourceAllowPrefixes", resourceAllowPrefixes);
            String json = JSON.toJSONString(obj, JSONWriter.Feature.PrettyFormat);
            Files.write(path, json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            logger.warn("save rules/search-filter.json failed: {}", ex.getMessage());
        }
    }

    private static class FilterConfig {
        private List<String> classPrefixes;
        private List<String> jarPrefixes;
        private List<String> resourceAllowPrefixes;
    }
}
