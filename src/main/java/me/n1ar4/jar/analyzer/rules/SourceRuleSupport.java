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

import me.n1ar4.jar.analyzer.core.reference.AnnoReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SourceRuleSupport {
    private SourceRuleSupport() {
    }

    public static RuleSnapshot snapshotCurrentRules() {
        ModelRegistry.checkNow();
        return RuleSnapshot.from(ModelRegistry.getSourceModels(), ModelRegistry.getSourceAnnotations());
    }

    public static int resolveRuleFlags(MethodReference method, RuleSnapshot snapshot) {
        if (method == null || snapshot == null || snapshot.isEmpty()) {
            return 0;
        }
        String cls = safe(method.getClassReference() == null ? null : method.getClassReference().getName()).replace('.', '/');
        String name = safe(method.getName());
        String desc = safe(method.getDesc());
        if (cls.isBlank() || name.isBlank() || desc.isBlank()) {
            return 0;
        }

        int flags = 0;
        Integer exact = snapshot.modelFlags().get(new MethodRuleKey(cls, name, desc));
        if (exact != null) {
            flags |= exact;
        }
        Integer wildcard = snapshot.modelFlags().get(new MethodRuleKey(cls, name, "*"));
        if (wildcard != null) {
            flags |= wildcard;
        }

        Set<AnnoReference> annos = method.getAnnotations();
        if (annos != null && !annos.isEmpty() && !snapshot.sourceAnnotations().isEmpty()) {
            for (AnnoReference anno : annos) {
                String normalized = normalizeAnnotationName(anno == null ? null : anno.getAnnoName());
                if (normalized.isBlank() || !snapshot.sourceAnnotations().contains(normalized)) {
                    continue;
                }
                flags |= GraphNode.SOURCE_FLAG_ANNOTATION;
                if (isWebAnnotation(normalized)) {
                    flags |= GraphNode.SOURCE_FLAG_WEB;
                }
                if (isRpcAnnotation(normalized)) {
                    flags |= GraphNode.SOURCE_FLAG_RPC;
                }
            }
        }

        if (flags != 0) {
            flags |= GraphNode.SOURCE_FLAG_ANY;
        }
        return flags;
    }

    public static String normalizeSourceModelDesc(String desc) {
        String value = safe(desc);
        if (value.isBlank() || "null".equalsIgnoreCase(value)) {
            return "*";
        }
        return value;
    }

    public static String normalizeAnnotationName(String raw) {
        String value = safe(raw);
        if (value.isBlank()) {
            return "";
        }
        if (value.charAt(0) == '@') {
            value = value.substring(1);
        }
        value = value.replace('.', '/');
        if (!value.startsWith("L")) {
            value = "L" + value;
        }
        if (!value.endsWith(";")) {
            value = value + ";";
        }
        return value;
    }

    public static boolean isWebSourceKind(String kind) {
        String value = safe(kind).toLowerCase(Locale.ROOT);
        return value.contains("web")
                || value.contains("http")
                || value.contains("rest")
                || value.contains("servlet")
                || value.contains("controller");
    }

    public static boolean isRpcSourceKind(String kind) {
        String value = safe(kind).toLowerCase(Locale.ROOT);
        return value.contains("rpc")
                || value.contains("grpc")
                || value.contains("dubbo")
                || value.contains("webservice");
    }

    private static boolean isWebAnnotation(String annotation) {
        String value = safe(annotation).toLowerCase(Locale.ROOT);
        return value.contains("/springframework/web/")
                || value.contains("/ws/rs/")
                || value.contains("/servlet/");
    }

    private static boolean isRpcAnnotation(String annotation) {
        String value = safe(annotation).toLowerCase(Locale.ROOT);
        return value.contains("/dubbo/")
                || value.contains("/grpc/")
                || value.contains("/jws/webservice;");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public record RuleSnapshot(Map<MethodRuleKey, Integer> modelFlags, Set<String> sourceAnnotations) {
        static RuleSnapshot from(List<SourceModel> sourceModels, List<String> sourceAnnotations) {
            Map<MethodRuleKey, Integer> modelFlags = new HashMap<>();
            if (sourceModels != null) {
                for (SourceModel model : sourceModels) {
                    if (model == null) {
                        continue;
                    }
                    String cls = safe(model.getClassName()).replace('.', '/');
                    String method = safe(model.getMethodName());
                    String desc = normalizeSourceModelDesc(model.getMethodDesc());
                    if (cls.isBlank() || method.isBlank()) {
                        continue;
                    }
                    int flags = GraphNode.SOURCE_FLAG_MODEL;
                    if (isWebSourceKind(model.getKind())) {
                        flags |= GraphNode.SOURCE_FLAG_WEB;
                    }
                    if (isRpcSourceKind(model.getKind())) {
                        flags |= GraphNode.SOURCE_FLAG_RPC;
                    }
                    modelFlags.merge(new MethodRuleKey(cls, method, desc), flags, (left, right) -> left | right);
                }
            }

            Set<String> normalizedAnnotations = new HashSet<>();
            if (sourceAnnotations != null) {
                for (String raw : sourceAnnotations) {
                    String normalized = normalizeAnnotationName(raw);
                    if (!normalized.isBlank()) {
                        normalizedAnnotations.add(normalized);
                    }
                }
            }
            return new RuleSnapshot(
                    modelFlags.isEmpty() ? Collections.emptyMap() : Map.copyOf(modelFlags),
                    normalizedAnnotations.isEmpty() ? Collections.emptySet() : Set.copyOf(normalizedAnnotations)
            );
        }

        public boolean isEmpty() {
            return modelFlags.isEmpty() && sourceAnnotations.isEmpty();
        }
    }

    public record MethodRuleKey(String className, String methodName, String methodDesc) {
    }
}
