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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class CommonWhitelistUtil {
    private static final Logger logger = LogManager.getLogger();
    private static final String CONFIG_PATH = "rules/common-whitelist.json";
    private static volatile FilterConfig config;

    private CommonWhitelistUtil() {
    }

    public static List<String> getClassPrefixes() {
        return getConfig().classPrefixes;
    }

    public static List<String> getJarPrefixes() {
        return getConfig().jarPrefixes;
    }

    public static boolean hasClassPrefixes() {
        List<String> prefixes = getConfig().classPrefixes;
        return prefixes != null && !prefixes.isEmpty();
    }

    public static boolean hasJarPrefixes() {
        List<String> prefixes = getConfig().jarPrefixes;
        return prefixes != null && !prefixes.isEmpty();
    }

    public static boolean isWhitelistedClass(String className) {
        if (StringUtil.isNull(className)) {
            return true;
        }
        String norm = className.replace('.', '/');
        return isWhitelistedClassNormalized(norm);
    }

    public static boolean isWhitelistedClassNormalized(String className) {
        if (StringUtil.isNull(className)) {
            return true;
        }
        List<String> prefixes = getConfig().classPrefixes;
        if (prefixes == null || prefixes.isEmpty()) {
            return true;
        }
        String norm = className;
        for (String prefix : prefixes) {
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

    public static boolean isWhitelistedJar(String jarName) {
        if (StringUtil.isNull(jarName)) {
            return true;
        }
        List<String> prefixes = getConfig().jarPrefixes;
        if (prefixes == null || prefixes.isEmpty()) {
            return true;
        }
        String name = normalizeJarName(jarName);
        if (name.isEmpty()) {
            return true;
        }
        for (String prefix : prefixes) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public static String buildClassPrefixText() {
        StringBuilder sb = new StringBuilder();
        sb.append("# class / package white list (rules/common-whitelist.json)\n");
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
        sb.append("# jar white list (rules/common-whitelist.json)\n");
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
        FilterConfig disk = loadConfig();
        List<String> jarPrefixes = normalizeJarPrefixes(disk.jarPrefixes);
        if (sameList(disk.classPrefixes, normalized)) {
            return;
        }
        writeConfig(normalized, jarPrefixes);
        FilterConfig out = new FilterConfig();
        out.classPrefixes = normalized;
        out.jarPrefixes = jarPrefixes;
        config = out;
    }

    public static synchronized void saveJarPrefixes(List<String> jarPrefixes) {
        List<String> normalized = normalizeJarPrefixes(jarPrefixes);
        FilterConfig disk = loadConfig();
        List<String> classPrefixes = normalizeClassPrefixes(disk.classPrefixes);
        if (sameList(disk.jarPrefixes, normalized)) {
            return;
        }
        writeConfig(classPrefixes, normalized);
        FilterConfig out = new FilterConfig();
        out.classPrefixes = classPrefixes;
        out.jarPrefixes = normalized;
        config = out;
    }

    public static synchronized void reload() {
        config = null;
    }

    private static FilterConfig getConfig() {
        if (config != null) {
            return config;
        }
        synchronized (CommonWhitelistUtil.class) {
            if (config == null) {
                config = loadConfig();
            }
        }
        return config;
    }

    private static FilterConfig loadConfig() {
        FilterConfig out = new FilterConfig();
        out.classPrefixes = Collections.emptyList();
        out.jarPrefixes = Collections.emptyList();

        Path path = Paths.get(CONFIG_PATH);
        if (!Files.exists(path)) {
            logger.warn("rules/common-whitelist.json not found");
            return out;
        }
        try (InputStream is = Files.newInputStream(path)) {
            loadConfig(is, out);
        } catch (Exception ex) {
            logger.warn("load rules/common-whitelist.json failed: {}", ex.getMessage());
        }
        return out;
    }

    private static void loadConfig(InputStream is, FilterConfig out) throws Exception {
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
    }

    private static void applyObject(JSONObject obj, FilterConfig out) {
        List<String> classPrefixes = obj.getList("classPrefixes", String.class);
        List<String> jarPrefixes = obj.getList("jarPrefixes", String.class);
        if (classPrefixes != null) {
            out.classPrefixes = normalizeClassPrefixes(classPrefixes);
        }
        if (jarPrefixes != null) {
            out.jarPrefixes = normalizeJarPrefixes(jarPrefixes);
        }
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

    private static void writeConfig(List<String> classPrefixes, List<String> jarPrefixes) {
        try {
            Path path = Paths.get(CONFIG_PATH);
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            JSONObject obj = new JSONObject();
            obj.put("classPrefixes", classPrefixes);
            obj.put("jarPrefixes", jarPrefixes);
            String json = JSON.toJSONString(obj, JSONWriter.Feature.PrettyFormat);
            Files.write(path, json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            logger.warn("save rules/common-whitelist.json failed: {}", ex.getMessage());
        }
    }

    private static final class FilterConfig {
        private List<String> classPrefixes;
        private List<String> jarPrefixes;
    }
}
