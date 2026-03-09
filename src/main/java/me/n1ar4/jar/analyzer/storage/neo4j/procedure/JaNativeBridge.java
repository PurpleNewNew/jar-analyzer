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

import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.graph.proc.ProcedureRegistry;
import me.n1ar4.jar.analyzer.graph.query.QueryOptions;
import me.n1ar4.jar.analyzer.graph.query.QueryResult;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.rules.ModelRegistry;
import me.n1ar4.jar.analyzer.rules.RuleValidationViews;
import org.neo4j.graphdb.Node;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JaNativeBridge {
    private static final ProcedureRegistry PROCEDURES = new ProcedureRegistry();
    private static final GraphStore GRAPH_STORE = new GraphStore();

    private JaNativeBridge() {
    }

    public static QueryResult executeProcedure(String procName, List<Object> args) {
        Map<String, Object> params = new LinkedHashMap<>();
        List<String> argExprs = new ArrayList<>();
        if (args != null) {
            for (int i = 0; i < args.size(); i++) {
                String key = "p" + i;
                params.put(key, args.get(i));
                argExprs.add("$" + key);
            }
        }
        QueryOptions options = NativeJaQueryContext.currentOptions();
        String projectKey = NativeJaQueryContext.currentProjectKey();
        GraphSnapshot snapshot = GRAPH_STORE.loadSnapshot(projectKey);
        return PROCEDURES.execute(procName, argExprs, params, options, snapshot);
    }

    public static List<JaProcedureRow> toProcedureRows(QueryResult result) {
        if (result == null || result.getRows().isEmpty()) {
            return List.of();
        }
        List<JaProcedureRow> out = new ArrayList<>(result.getRows().size());
        for (List<Object> row : result.getRows()) {
            if (row == null || row.size() < 7) {
                continue;
            }
            out.add(new JaProcedureRow(
                    toLong(row.get(0)),
                    toLong(row.get(1)),
                    stringValue(row.get(2)),
                    stringValue(row.get(3)),
                    toDouble(row.get(4)),
                    stringValue(row.get(5)),
                    stringValue(row.get(6))
            ));
        }
        return out;
    }

    public static boolean isSource(Object target) {
        NodeHandle handle = resolveNodeHandle(target);
        if (handle == null) {
            return false;
        }
        return handle.sourceFlags != 0;
    }

    public static boolean isSink(Object target) {
        return !sinkKind(target).isBlank();
    }

    public static String sinkKind(Object target) {
        NodeHandle handle = resolveNodeHandle(target);
        if (handle == null || !"method".equalsIgnoreCase(handle.kind)) {
            return "";
        }
        MethodReference.Handle method = new MethodReference.Handle(
                new ClassReference.Handle(handle.className, handle.jarId),
                handle.methodName,
                handle.methodDesc
        );
        String kind = ModelRegistry.resolveSinkKind(method);
        return kind == null ? "" : kind;
    }

    public static long ruleVersion() {
        return ModelRegistry.getVersion();
    }

    public static String rulesFingerprint() {
        return ModelRegistry.getRulesFingerprint();
    }

    public static Map<String, Object> ruleValidation() {
        return RuleValidationViews.combinedValidationMap();
    }

    public static List<Map<String, Object>> ruleValidationIssues(String scope) {
        if (!RuleValidationViews.isSupportedScope(scope)) {
            throw new IllegalArgumentException("invalid rule validation scope: " + String.valueOf(scope));
        }
        return RuleValidationViews.issueMaps(scope);
    }

    private static NodeHandle resolveNodeHandle(Object target) {
        if (target == null) {
            return null;
        }
        if (target instanceof Node node) {
            return NodeHandle.fromNode(node);
        }
        if (target instanceof GraphNode graphNode) {
            return NodeHandle.fromGraphNode(graphNode);
        }
        if (target instanceof Number number) {
            GraphSnapshot snapshot = loadSnapshotQuietly();
            if (snapshot == null) {
                return null;
            }
            GraphNode node = snapshot.getNode(number.longValue());
            return node == null ? null : NodeHandle.fromGraphNode(node);
        }
        if (target instanceof Map<?, ?> map) {
            return NodeHandle.fromMap(map);
        }
        String raw = stringValue(target);
        if (raw.isBlank()) {
            return null;
        }
        if (raw.contains("#")) {
            GraphSnapshot snapshot = loadSnapshotQuietly();
            if (snapshot == null) {
                return null;
            }
            String[] parts = raw.split("#", 4);
            if (parts.length < 3) {
                return null;
            }
            Integer jarId = parts.length >= 4 ? toNullableInt(parts[3]) : null;
            long nodeId = snapshot.findMethodNodeId(parts[0], parts[1], parts[2], jarId);
            if (nodeId <= 0L) {
                return null;
            }
            GraphNode node = snapshot.getNode(nodeId);
            return node == null ? null : NodeHandle.fromGraphNode(node);
        }
        return null;
    }

    private static GraphSnapshot loadSnapshotQuietly() {
        try {
            return GRAPH_STORE.loadSnapshot(NativeJaQueryContext.currentProjectKey());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String normalizeClass(String value) {
        return stringValue(value).replace('.', '/');
    }

    private static Integer toNullableInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(stringValue(value));
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(stringValue(value));
        } catch (Exception ignored) {
            return 0.0D;
        }
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    public static final class JaProcedureRow {
        public long path_id;
        public long hop;
        public String node_ids;
        public String edge_ids;
        public double score;
        public String confidence;
        public String evidence;

        public JaProcedureRow(long pathId,
                              long hop,
                              String nodeIds,
                              String edgeIds,
                              double score,
                              String confidence,
                              String evidence) {
            this.path_id = pathId;
            this.hop = hop;
            this.node_ids = nodeIds == null ? "" : nodeIds;
            this.edge_ids = edgeIds == null ? "" : edgeIds;
            this.score = score;
            this.confidence = confidence == null ? "" : confidence;
            this.evidence = evidence == null ? "" : evidence;
        }
    }

    private static final class NodeHandle {
        private final String kind;
        private final int jarId;
        private final String className;
        private final String methodName;
        private final String methodDesc;
        private final int sourceFlags;

        private NodeHandle(String kind,
                           int jarId,
                           String className,
                           String methodName,
                           String methodDesc,
                           int sourceFlags) {
            this.kind = kind == null ? "" : kind;
            this.jarId = jarId;
            this.className = normalizeClass(className);
            this.methodName = methodName == null ? "" : methodName;
            this.methodDesc = methodDesc == null ? "" : methodDesc;
            this.sourceFlags = Math.max(0, sourceFlags);
        }

        private static NodeHandle fromNode(Node node) {
            if (node == null) {
                return null;
            }
            return new NodeHandle(
                    stringValue(node.getProperty("kind", "")),
                    toNullableInt(node.getProperty("jar_id", -1)) == null ? -1 : toNullableInt(node.getProperty("jar_id", -1)),
                    stringValue(node.getProperty("class_name", "")),
                    stringValue(node.getProperty("method_name", "")),
                    stringValue(node.getProperty("method_desc", "")),
                    toNullableInt(node.getProperty("source_flags", 0)) == null ? 0 : toNullableInt(node.getProperty("source_flags", 0))
            );
        }

        private static NodeHandle fromGraphNode(GraphNode node) {
            if (node == null) {
                return null;
            }
            return new NodeHandle(
                    node.getKind(),
                    node.getJarId(),
                    node.getClassName(),
                    node.getMethodName(),
                    node.getMethodDesc(),
                    node.getSourceFlags()
            );
        }

        private static NodeHandle fromMap(Map<?, ?> map) {
            if (map == null || map.isEmpty()) {
                return null;
            }
            Object propsValue = map.get("properties");
            if (propsValue instanceof Map<?, ?> props) {
                String kind = valueOrFallback(map.get("kind"), props.get("kind"));
                Integer jarId = firstInt(map.get("jar_id"), props.get("jar_id"));
                String className = valueOrFallback(map.get("class_name"), props.get("class_name"));
                String methodName = valueOrFallback(map.get("method_name"), props.get("method_name"));
                String methodDesc = valueOrFallback(map.get("method_desc"), props.get("method_desc"));
                Integer sourceFlags = firstInt(map.get("source_flags"), props.get("source_flags"));
                return new NodeHandle(
                        kind,
                        jarId == null ? -1 : jarId,
                        className,
                        methodName,
                        methodDesc,
                        sourceFlags == null ? 0 : sourceFlags
                );
            }
            return new NodeHandle(
                    stringValue(map.get("kind")),
                    firstInt(map.get("jar_id")) == null ? -1 : firstInt(map.get("jar_id")),
                    stringValue(map.get("class_name")),
                    stringValue(map.get("method_name")),
                    stringValue(map.get("method_desc")),
                    firstInt(map.get("source_flags")) == null ? 0 : firstInt(map.get("source_flags"))
            );
        }

        private static Integer firstInt(Object... candidates) {
            if (candidates == null) {
                return null;
            }
            for (Object candidate : candidates) {
                Integer value = toNullableInt(candidate);
                if (value != null) {
                    return value;
                }
            }
            return null;
        }

        private static String valueOrFallback(Object first, Object second) {
            String value = stringValue(first);
            return value.isBlank() ? stringValue(second) : value;
        }
    }
}
