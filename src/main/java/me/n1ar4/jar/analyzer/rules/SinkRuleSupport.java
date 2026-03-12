/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.rules;

import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public final class SinkRuleSupport {
    private static final Logger logger = LogManager.getLogger();

    private SinkRuleSupport() {
    }

    public static String normalizeSeverity(String raw, String fallback) {
        String value = safe(raw).toLowerCase(Locale.ROOT);
        if ("high".equals(value) || "medium".equals(value) || "low".equals(value)) {
            return value;
        }
        String normalizedFallback = safe(fallback).toLowerCase(Locale.ROOT);
        if ("high".equals(normalizedFallback) || "medium".equals(normalizedFallback) || "low".equals(normalizedFallback)) {
            return normalizedFallback;
        }
        return "";
    }

    public static String normalizeRuleTier(String raw) {
        String value = safe(raw).toLowerCase(Locale.ROOT);
        if (SinkModel.TIER_HARD.equals(value)
                || SinkModel.TIER_SOFT.equals(value)
                || SinkModel.TIER_CLUE.equals(value)) {
            return value;
        }
        return "";
    }

    public static String resolveRuleTier(String methodDesc, String severity) {
        if (isWildcardDesc(methodDesc)) {
            return SinkModel.TIER_CLUE;
        }
        if ("low".equals(normalizeSeverity(severity, ""))) {
            return SinkModel.TIER_SOFT;
        }
        return SinkModel.TIER_HARD;
    }

    public static List<String> normalizeTags(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        for (String item : raw) {
            String normalized = safe(item).toLowerCase(Locale.ROOT);
            if (!normalized.isBlank()) {
                tags.add(normalized);
            }
        }
        return tags.isEmpty() ? Collections.emptyList() : List.copyOf(tags);
    }

    public static List<String> mergeTags(List<String> existing, List<String> incoming) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (existing != null) {
            tags.addAll(normalizeTags(existing));
        }
        if (incoming != null) {
            tags.addAll(normalizeTags(incoming));
        }
        return tags.isEmpty() ? Collections.emptyList() : List.copyOf(tags);
    }

    public static List<String> buildTags(String category) {
        if (category == null) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        for (String part : category.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (!part.isBlank()) {
                tags.add(part);
            }
        }
        return tags.isEmpty() ? Collections.emptyList() : List.copyOf(tags);
    }

    public static String buildBoxName(String className, String methodName, String methodDesc) {
        String simpleClass = className;
        if (className != null) {
            int idx = className.lastIndexOf('/');
            if (idx >= 0 && idx < className.length() - 1) {
                simpleClass = className.substring(idx + 1);
            }
            simpleClass = simpleClass.replace('$', '.');
        }
        String name = methodName == null ? "" : methodName;
        String params = "";
        if (!isWildcardDesc(methodDesc)) {
            try {
                Type[] args = Type.getArgumentTypes(methodDesc);
                if (args.length > 0) {
                    List<String> names = new ArrayList<>(args.length);
                    for (Type arg : args) {
                        String value = arg.getClassName();
                        int dot = value.lastIndexOf('.');
                        names.add(dot >= 0 ? value.substring(dot + 1) : value);
                    }
                    params = "(" + String.join(", ", names) + ")";
                }
            } catch (Exception ex) {
                logger.debug("parse method desc failed: {}: {}", methodDesc, ex.toString());
            }
        }
        return simpleClass + "." + name + params;
    }

    public static boolean isWildcardDesc(String desc) {
        if (desc == null) {
            return true;
        }
        String value = desc.trim();
        return value.isEmpty() || "*".equals(value) || "null".equalsIgnoreCase(value);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
