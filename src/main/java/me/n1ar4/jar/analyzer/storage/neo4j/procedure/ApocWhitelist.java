/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.storage.neo4j.procedure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ApocWhitelist {
    public static final String APOC_WHITELIST_PROP = "jar.analyzer.cypher.apoc.whitelist";
    private static final String UNSUPPORTED_CODE = "cypher_feature_not_supported";
    private static final Map<String, List<String>> CATEGORY_FUNCTIONS;
    private static final Map<String, String> CANONICAL_FUNCTIONS;
    private static final List<String> DEFAULT_FUNCTIONS;
    private static final List<String> ALL_FUNCTION_NAMES;
    private static final Set<String> ALL_FUNCTIONS;

    static {
        Map<String, List<String>> categories = new LinkedHashMap<>();
        categories.put("coll", List.of(
                "apoc.coll.contains",
                "apoc.coll.containsAll",
                "apoc.coll.toSet",
                "apoc.coll.intersection",
                "apoc.coll.subtract",
                "apoc.coll.flatten"
        ));
        categories.put("map", List.of(
                "apoc.map.fromPairs",
                "apoc.map.fromLists",
                "apoc.map.values",
                "apoc.map.merge",
                "apoc.map.mergeList",
                "apoc.map.get",
                "apoc.map.removeKeys"
        ));
        categories.put("text", List.of(
                "apoc.text.indexOf",
                "apoc.text.replace",
                "apoc.text.split",
                "apoc.text.join",
                "apoc.text.clean",
                "apoc.text.urlencode",
                "apoc.text.urldecode"
        ));
        CATEGORY_FUNCTIONS = Collections.unmodifiableMap(categories);
        Map<String, String> canonicalFunctions = new LinkedHashMap<>();
        LinkedHashSet<String> defaults = new LinkedHashSet<>();
        for (List<String> functions : categories.values()) {
            defaults.addAll(functions);
            for (String function : functions) {
                canonicalFunctions.put(normalizeToken(function), function);
            }
        }
        CANONICAL_FUNCTIONS = Collections.unmodifiableMap(canonicalFunctions);
        DEFAULT_FUNCTIONS = List.copyOf(defaults);
        ALL_FUNCTION_NAMES = List.copyOf(canonicalFunctions.values());
        ALL_FUNCTIONS = Set.copyOf(CANONICAL_FUNCTIONS.keySet());
    }

    private ApocWhitelist() {
    }

    public static List<String> effectiveWhitelist() {
        return resolveWhitelist(System.getProperty(APOC_WHITELIST_PROP));
    }

    public static String whitelistMode() {
        String raw = normalizeToken(System.getProperty(APOC_WHITELIST_PROP));
        if (raw.isBlank() || "default".equals(raw)) {
            return "default";
        }
        if (isOffToken(raw)) {
            return "off";
        }
        if ("all".equals(raw)) {
            return "all";
        }
        return "custom";
    }

    public static boolean isAllowed(String functionName) {
        String normalized = normalizeFunctionName(functionName);
        for (String function : effectiveWhitelist()) {
            if (normalizeFunctionName(function).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    public static void requireAllowed(String functionName) {
        String normalized = normalizeFunctionName(functionName);
        if (!isAllowed(normalized)) {
            throw new IllegalArgumentException(
                    UNSUPPORTED_CODE + ": apoc function not allowed: "
                            + CANONICAL_FUNCTIONS.getOrDefault(normalized, functionName));
        }
    }

    static List<String> resolveWhitelist(String rawProperty) {
        String raw = normalizeToken(rawProperty);
        if (raw.isBlank() || "default".equals(raw)) {
            return DEFAULT_FUNCTIONS;
        }
        if ("all".equals(raw)) {
            return ALL_FUNCTION_NAMES;
        }
        if (isOffToken(raw)) {
            return List.of();
        }
        LinkedHashSet<String> resolved = new LinkedHashSet<>();
        for (String token : raw.split(",")) {
            String normalized = normalizeToken(token);
            if (normalized.isBlank() || isOffToken(normalized)) {
                continue;
            }
            if ("default".equals(normalized)) {
                resolved.addAll(DEFAULT_FUNCTIONS);
                continue;
            }
            if ("all".equals(normalized)) {
                resolved.addAll(ALL_FUNCTION_NAMES);
                continue;
            }
            List<String> category = CATEGORY_FUNCTIONS.get(normalized);
            if (category != null) {
                resolved.addAll(category);
                continue;
            }
            if (ALL_FUNCTIONS.contains(normalized)) {
                resolved.add(CANONICAL_FUNCTIONS.get(normalized));
            }
        }
        return List.copyOf(resolved);
    }

    private static boolean isOffToken(String token) {
        return "none".equals(token) || "off".equals(token) || "disabled".equals(token);
    }

    private static String normalizeFunctionName(String value) {
        return normalizeToken(value);
    }

    private static String normalizeToken(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
