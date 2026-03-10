/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.query;

import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.graph.model.GraphRelationType;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.rules.ModelRegistry;
import me.n1ar4.jar.analyzer.rules.SourceRuleSupport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class QueryDisplayModel {
    private static final List<String> PHYSICAL_NODE_LABELS = List.of("JANode", "Method", "Class");
    private static final List<String> PUBLIC_NODE_LABELS = List.of("Method", "Class");
    private static final List<String> PUBLIC_RELATION_GROUPS = List.of("CALL", "ALIAS", "HAS", "EXTEND", "INTERFACES");
    private static final List<String> PUBLIC_SEMANTIC_BADGES = List.of("Source", "Sink", "Web", "Rpc", "Sink:<kind>");
    private static final List<String> TRAVERSAL_MODES = List.of("call-only", "call+alias");

    private QueryDisplayModel() {
    }

    public static Map<String, Object> capabilitiesView() {
        LinkedHashMap<String, Object> model = new LinkedHashMap<>();
        model.put("physicalNodeLabels", PHYSICAL_NODE_LABELS);
        List<String> physicalRelationshipTypes = new ArrayList<>(GraphRelationType.physicalCallRelationTypes());
        physicalRelationshipTypes.add("ALIAS");
        physicalRelationshipTypes.add("HAS");
        physicalRelationshipTypes.add("EXTEND");
        physicalRelationshipTypes.add("INTERFACES");
        model.put("physicalRelationshipTypes", List.copyOf(physicalRelationshipTypes));
        model.put("publicNodeLabels", PUBLIC_NODE_LABELS);
        model.put("publicRelationGroups", PUBLIC_RELATION_GROUPS);
        model.put("publicSemanticBadges", PUBLIC_SEMANTIC_BADGES);
        model.put("traversalModes", TRAVERSAL_MODES);
        model.put("defaultTraversalMode", "call-only");
        model.put("semanticMode", "dynamic-ja-functions");
        model.put("logicalCypherPatternRewrite", true);
        model.put("logicalCypherTypePredicateRewrite", true);
        model.put("logicalCypherParameterizedTypePredicateRewrite", true);
        model.put("logicalCypherRelationshipTypes", PUBLIC_RELATION_GROUPS);
        return model;
    }

    public static List<String> displayLabels(String kind, Collection<String> rawLabels) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        String displayKind = displayKindLabel(kind);
        if (!displayKind.isEmpty()) {
            out.add(displayKind);
        }
        if (rawLabels == null || rawLabels.isEmpty()) {
            return new ArrayList<>(out);
        }
        LinkedHashSet<String> fallback = new LinkedHashSet<>();
        for (String raw : rawLabels) {
            String label = safe(raw);
            if (label.isEmpty() || "JANode".equalsIgnoreCase(label) || isSemanticDisplayLabel(label)) {
                continue;
            }
            if ("method".equalsIgnoreCase(label)) {
                out.add("Method");
                continue;
            }
            if ("class".equalsIgnoreCase(label)) {
                out.add("Class");
                continue;
            }
            fallback.add(label);
        }
        if (out.isEmpty()) {
            out.addAll(fallback);
        }
        return new ArrayList<>(out);
    }

    public static Map<String, Object> projectNodeProperties(Map<String, Object> rawProperties) {
        Map<String, Object> input = rawProperties == null ? Map.of() : rawProperties;
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            putIfPresent(out, entry.getKey(), entry.getValue());
        }

        String kind = safeText(firstValue(input, "kind"));
        Integer jarId = parseInt(firstValue(input, "jar_id", "jarId"));
        String className = normalizeClassName(safeText(firstValue(input, "class_name", "className")));
        String methodName = safeText(firstValue(input, "method_name", "methodName"));
        String methodDesc = safeText(firstValue(input, "method_desc", "methodDesc"));
        String callSiteKey = safeText(firstValue(input, "call_site_key", "callSiteKey"));
        Integer lineNumber = parseInt(firstValue(input, "line_number", "lineNumber"));
        Integer callIndex = parseInt(firstValue(input, "call_index", "callIndex"));
        Integer sourceFlags = parseInt(firstValue(input, "source_flags", "sourceFlags"));
        Boolean isInterface = parseBoolean(firstValue(input, "is_interface", "isInterface"));
        Boolean isAbstract = parseBoolean(firstValue(input, "is_abstract", "isAbstract"));

        putIfPresent(out, "kind", kind);
        putIfPresent(out, "jar_id", jarId);
        putIfPresent(out, "class_name", className);
        putIfPresent(out, "method_name", methodName);
        putIfPresent(out, "method_desc", methodDesc);
        putIfPresent(out, "call_site_key", callSiteKey);
        if (lineNumber != null && lineNumber >= 0) {
            out.put("line_number", lineNumber);
        }
        if (callIndex != null && callIndex >= 0) {
            out.put("call_index", callIndex);
        }
        if (sourceFlags != null && sourceFlags >= 0) {
            out.put("source_flags", sourceFlags);
        }
        if (isInterface != null) {
            out.put("is_interface", isInterface);
        }
        if (isAbstract != null) {
            out.put("is_abstract", isAbstract);
        }

        out.putAll(nodeSemanticProperties(kind, jarId, className, methodName, methodDesc));
        return out;
    }

    public static Map<String, Object> projectRelationshipProperties(String relType, Map<String, Object> rawProperties) {
        Map<String, Object> input = rawProperties == null ? Map.of() : rawProperties;
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            putIfPresent(out, entry.getKey(), entry.getValue());
        }
        String effectiveRelType = safe(relType);
        if (effectiveRelType.isBlank()) {
            effectiveRelType = safeText(firstValue(input, "rel_type", "type"));
        }
        String confidence = safeText(firstValue(input, "confidence"));
        String evidence = safeText(firstValue(input, "evidence"));
        String aliasKind = safeText(firstValue(input, "alias_kind", "aliasKind"));
        String callSiteKey = safeText(firstValue(input, "call_site_key", "callSiteKey"));
        Integer lineNumber = parseInt(firstValue(input, "line_number", "lineNumber"));
        Integer callIndex = parseInt(firstValue(input, "call_index", "callIndex"));
        Integer opCode = parseInt(firstValue(input, "op_code", "opCode"));

        putIfPresent(out, "rel_type", effectiveRelType);
        putIfPresent(out, "display_rel_type", GraphRelationType.relationGroup(effectiveRelType));
        putIfPresent(out, "rel_subtype", GraphRelationType.relationSubtype(effectiveRelType));
        putIfPresent(out, "confidence", confidence);
        putIfPresent(out, "evidence", evidence);
        putIfPresent(out, "alias_kind", aliasKind);
        putIfPresent(out, "call_site_key", callSiteKey);
        if (lineNumber != null && lineNumber >= 0) {
            out.put("line_number", lineNumber);
        }
        if (callIndex != null && callIndex >= 0) {
            out.put("call_index", callIndex);
        }
        if (opCode != null && opCode >= 0) {
            out.put("op_code", opCode);
        }
        return out;
    }

    private static Map<String, Object> nodeSemanticProperties(String kind,
                                                              Integer jarId,
                                                              String className,
                                                              String methodName,
                                                              String methodDesc) {
        if (!"method".equalsIgnoreCase(safe(kind))) {
            return Map.of();
        }
        int effectiveSourceFlags = SourceRuleSupport.resolveCurrentSourceFlags(
                className,
                methodName,
                methodDesc,
                jarId
        );
        List<String> sourceBadges = publicSourceBadges(effectiveSourceFlags);
        String sinkKind = resolveSinkKind(jarId, className, methodName, methodDesc);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (effectiveSourceFlags > 0) {
            out.put("source_flags_effective", effectiveSourceFlags);
        }
        if (!sourceBadges.isEmpty()) {
            out.put("source_badges", sourceBadges);
        }
        out.put("is_source", effectiveSourceFlags != 0);
        out.put("is_sink", !sinkKind.isBlank());
        putIfPresent(out, "sink_kind", sinkKind);
        return out;
    }

    private static List<String> publicSourceBadges(int flags) {
        if (flags <= 0) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        if ((flags & GraphNode.SOURCE_FLAG_WEB) != 0) {
            out.add("Web");
        }
        if ((flags & GraphNode.SOURCE_FLAG_RPC) != 0) {
            out.add("Rpc");
        }
        return List.copyOf(out);
    }

    private static String resolveSinkKind(Integer jarId,
                                          String className,
                                          String methodName,
                                          String methodDesc) {
        String owner = normalizeClassName(className);
        String name = safe(methodName);
        String desc = safe(methodDesc);
        int normalizedJarId = jarId == null ? -1 : jarId;
        if (owner.isBlank() || name.isBlank() || desc.isBlank()) {
            return "";
        }
        MethodReference.Handle handle = new MethodReference.Handle(
                new ClassReference.Handle(owner, normalizedJarId),
                name,
                desc
        );
        String sinkKind = ModelRegistry.resolveSinkKind(handle);
        return sinkKind == null ? "" : sinkKind;
    }

    private static Object firstValue(Map<String, Object> values, String... keys) {
        if (values == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            Object value = values.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static Integer parseInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = safeText(value);
        if (text.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Boolean parseBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = safeText(value);
        if (text.isBlank()) {
            return null;
        }
        return Boolean.parseBoolean(text);
    }

    private static boolean isSemanticDisplayLabel(String label) {
        String normalized = safe(label);
        return "Source".equalsIgnoreCase(normalized)
                || "SourceWeb".equalsIgnoreCase(normalized)
                || "SourceModel".equalsIgnoreCase(normalized)
                || "SourceAnno".equalsIgnoreCase(normalized)
                || "SourceRpc".equalsIgnoreCase(normalized)
                || "Sink".equalsIgnoreCase(normalized)
                || "Endpoint".equalsIgnoreCase(normalized)
                || "Web".equalsIgnoreCase(normalized)
                || "Rpc".equalsIgnoreCase(normalized);
    }

    private static String displayKindLabel(String kind) {
        String normalized = safe(kind).toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "method" -> "Method";
            case "class" -> "Class";
            default -> "";
        };
    }

    private static String normalizeClassName(String value) {
        return safe(value).replace('.', '/');
    }

    private static void putIfPresent(Map<String, Object> out, String key, Object value) {
        if (out == null || key == null || key.isBlank() || value == null) {
            return;
        }
        if (value instanceof String text && text.isBlank()) {
            return;
        }
        out.put(key, value);
    }

    private static String safeText(Object value) {
        return safe(value == null ? null : String.valueOf(value));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
