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

import me.n1ar4.jar.analyzer.analyze.spring.SpringController;
import me.n1ar4.jar.analyzer.analyze.spring.SpringMapping;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.WebEntryMethodSpec;
import me.n1ar4.jar.analyzer.core.WebEntryMethods;
import me.n1ar4.jar.analyzer.core.reference.AnnoReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;

import java.util.ArrayList;
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

    public static ActiveSourceResolution resolveCurrentSourceFlags(String className,
                                                                  String methodName,
                                                                  String methodDesc,
                                                                  Integer jarId) {
        String cls = safe(className).replace('.', '/');
        String name = safe(methodName);
        String desc = safe(methodDesc);
        int normalizedJarId = jarId == null ? -1 : jarId;
        if (cls.isBlank() || name.isBlank() || desc.isBlank()) {
            return new ActiveSourceResolution(0, false);
        }

        int flags = 0;
        boolean resolved = false;
        RuleSnapshot snapshot = snapshotCurrentRules();
        List<MethodReference> methods = DatabaseManager.getMethodReferencesByClass(cls);
        if (methods != null && !methods.isEmpty()) {
            resolved = true;
            for (MethodReference method : methods) {
                if (!matchesMethod(method, cls, name, desc, normalizedJarId)) {
                    continue;
                }
                flags |= resolveRuleFlags(method, snapshot);
            }
        }
        int explicitFlags = resolveExplicitProjectFlags(cls, name, desc);
        if (explicitFlags != 0) {
            flags |= explicitFlags;
            resolved = true;
        }
        if (flags != 0) {
            flags |= GraphNode.SOURCE_FLAG_ANY;
        }
        return new ActiveSourceResolution(flags, resolved);
    }

    public static List<String> describeFlags(int flags) {
        if (flags <= 0) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        if ((flags & GraphNode.SOURCE_FLAG_ANY) != 0) {
            out.add("Source");
        }
        if ((flags & GraphNode.SOURCE_FLAG_WEB) != 0) {
            out.add("Web");
        }
        if ((flags & GraphNode.SOURCE_FLAG_MODEL) != 0) {
            out.add("Model");
        }
        if ((flags & GraphNode.SOURCE_FLAG_ANNOTATION) != 0) {
            out.add("Annotation");
        }
        if ((flags & GraphNode.SOURCE_FLAG_RPC) != 0) {
            out.add("Rpc");
        }
        return List.copyOf(out);
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

    public static boolean isServletEntry(String methodName, String methodDesc) {
        String name = safe(methodName);
        String desc = safe(methodDesc);
        if (name.isBlank() || desc.isBlank()) {
            return false;
        }
        if (!("doGet".equals(name) || "doPost".equals(name) || "doPut".equals(name)
                || "doDelete".equals(name) || "doHead".equals(name)
                || "doOptions".equals(name) || "doTrace".equals(name)
                || "service".equals(name))) {
            return false;
        }
        return "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V".equals(desc)
                || "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;)V".equals(desc)
                || "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;)V".equals(desc)
                || "(Ljakarta/servlet/ServletRequest;Ljakarta/servlet/ServletResponse;)V".equals(desc);
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

    private static int resolveExplicitProjectFlags(String className,
                                                   String methodName,
                                                   String methodDesc) {
        int flags = 0;
        if (matchesSpringControllerMapping(className, methodName, methodDesc)) {
            flags |= GraphNode.SOURCE_FLAG_WEB;
        }
        if (matchesWebEntryClass(DatabaseManager.getServlets(), className, methodName, methodDesc, WebEntryMethods.SERVLET_ENTRY_METHODS)) {
            flags |= GraphNode.SOURCE_FLAG_WEB;
        }
        if (matchesWebEntryClass(DatabaseManager.getFilters(), className, methodName, methodDesc, WebEntryMethods.FILTER_ENTRY_METHODS)) {
            flags |= GraphNode.SOURCE_FLAG_WEB;
        }
        if (matchesWebEntryClass(DatabaseManager.getSpringInterceptors(), className, methodName, methodDesc, WebEntryMethods.INTERCEPTOR_ENTRY_METHODS)) {
            flags |= GraphNode.SOURCE_FLAG_WEB;
        }
        if (matchesWebEntryClass(DatabaseManager.getListeners(), className, methodName, methodDesc, WebEntryMethods.LISTENER_ENTRY_METHODS)) {
            flags |= GraphNode.SOURCE_FLAG_WEB;
        }
        if (isServletEntry(methodName, methodDesc)) {
            flags |= GraphNode.SOURCE_FLAG_WEB;
        }
        return flags;
    }

    private static boolean matchesMethod(MethodReference method,
                                         String className,
                                         String methodName,
                                         String methodDesc,
                                         int jarId) {
        if (method == null) {
            return false;
        }
        String owner = safe(method.getClassReference() == null ? null : method.getClassReference().getName()).replace('.', '/');
        if (!className.equals(owner)) {
            return false;
        }
        if (!methodName.equals(safe(method.getName())) || !methodDesc.equals(safe(method.getDesc()))) {
            return false;
        }
        if (jarId < 0) {
            return true;
        }
        Integer methodJarId = method.getJarId();
        if (methodJarId == null || methodJarId < 0) {
            return true;
        }
        return jarId == methodJarId;
    }

    private static boolean matchesSpringControllerMapping(String className,
                                                          String methodName,
                                                          String methodDesc) {
        List<SpringController> controllers = DatabaseManager.getSpringControllers();
        if (controllers == null || controllers.isEmpty()) {
            return false;
        }
        for (SpringController controller : controllers) {
            if (controller == null || controller.getMappings() == null) {
                continue;
            }
            for (SpringMapping mapping : controller.getMappings()) {
                MethodReference.Handle handle = mapping == null ? null : mapping.getMethodName();
                String owner = safe(handle == null || handle.getClassReference() == null ? null : handle.getClassReference().getName()).replace('.', '/');
                if (!className.equals(owner)) {
                    continue;
                }
                if (methodName.equals(safe(handle.getName())) && methodDesc.equals(safe(handle.getDesc()))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesWebEntryClass(Set<String> classes,
                                                String className,
                                                String methodName,
                                                String methodDesc,
                                                Set<WebEntryMethodSpec> specs) {
        if (classes == null || classes.isEmpty() || specs == null || specs.isEmpty()) {
            return false;
        }
        boolean classMatched = false;
        for (String candidate : classes) {
            if (className.equals(safe(candidate).replace('.', '/'))) {
                classMatched = true;
                break;
            }
        }
        if (!classMatched) {
            return false;
        }
        for (WebEntryMethodSpec spec : specs) {
            if (spec == null) {
                continue;
            }
            if (methodName.equals(safe(spec.name())) && methodDesc.equals(safe(spec.desc()))) {
                return true;
            }
        }
        return false;
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

    public record ActiveSourceResolution(int flags, boolean resolved) {
    }
}
