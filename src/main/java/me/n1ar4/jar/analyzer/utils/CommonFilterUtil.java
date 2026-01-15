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
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import me.n1ar4.jar.analyzer.utils.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class CommonFilterUtil {
    private static final Logger logger = LogManager.getLogger();
    private static final String CONFIG_PATH = "rules/common-filter.json";
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
    private static volatile FilterConfig config;

    private CommonFilterUtil() {
    }

    public static List<String> getClassPrefixes() {
        return getConfig().classPrefixes;
    }

    public static List<String> getJarPrefixes() {
        return getConfig().jarPrefixes;
    }

    public static boolean isFilteredClass(String className) {
        if (StringUtil.isNull(className)) {
            return false;
        }
        String norm = className.replace('.', '/');
        for (String prefix : getConfig().classPrefixes) {
            if (norm.startsWith(prefix)) {
                return true;
            }
        }
        return false;
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

        Path path = Paths.get(CONFIG_PATH);
        if (!Files.exists(path)) {
            logger.warn("rules/common-filter.json not found");
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
            logger.warn("load rules/common-filter.json failed: {}", ex.getMessage());
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
        if (classPrefixes != null) {
            out.classPrefixes = mergeClassPrefixes(out.classPrefixes, classPrefixes);
        }
        if (jarPrefixes != null) {
            out.jarPrefixes = normalizeJarPrefixes(jarPrefixes);
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

    private static String normalizeClassPrefix(String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim();
        if (v.isEmpty()) {
            return "";
        }
        v = v.replace(".", "/");
        if (!v.endsWith("/")) {
            v = v + "/";
        }
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

    private static class FilterConfig {
        private List<String> classPrefixes;
        private List<String> jarPrefixes;
    }
}
