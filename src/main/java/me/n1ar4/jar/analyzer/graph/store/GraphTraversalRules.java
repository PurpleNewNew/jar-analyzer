/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.store;

import java.util.Set;

public final class GraphTraversalRules {
    private GraphTraversalRules() {
    }

    public static boolean isMethodCallEdge(GraphSnapshot snapshot, GraphEdge edge) {
        return isMethodFlowEdge(snapshot, edge, false);
    }

    public static boolean isMethodFlowEdge(GraphSnapshot snapshot,
                                           GraphEdge edge,
                                           boolean includeAlias) {
        if (snapshot == null || edge == null) {
            return false;
        }
        long srcId = edge.getSrcId();
        long dstId = edge.getDstId();
        if (srcId <= 0L || dstId <= 0L || srcId == dstId) {
            return false;
        }
        if (!isFlowEdge(edge.getRelType(), includeAlias)) {
            return false;
        }
        GraphNode src = snapshot.getNode(srcId);
        GraphNode dst = snapshot.getNode(dstId);
        return isMethodNode(src) && isMethodNode(dst);
    }

    public static boolean isTraversable(GraphSnapshot snapshot,
                                        GraphEdge edge,
                                        Set<String> blacklist,
                                        String minConfidence) {
        return isTraversable(snapshot, edge, blacklist, minConfidence, false);
    }

    public static boolean isTraversable(GraphSnapshot snapshot,
                                        GraphEdge edge,
                                        Set<String> blacklist,
                                        String minConfidence,
                                        boolean includeAlias) {
        if (!isMethodFlowEdge(snapshot, edge, includeAlias)) {
            return false;
        }
        GraphNode src = snapshot.getNode(edge.getSrcId());
        GraphNode dst = snapshot.getNode(edge.getDstId());
        if (isBlacklisted(src, blacklist) || isBlacklisted(dst, blacklist)) {
            return false;
        }
        return meetsMinConfidence(edge.getConfidence(), minConfidence);
    }

    public static boolean isCallEdge(String relType) {
        if (relType == null || relType.isBlank()) {
            return false;
        }
        return relType.startsWith("CALLS_");
    }

    public static boolean isAliasEdge(String relType) {
        if (relType == null || relType.isBlank()) {
            return false;
        }
        return "ALIAS".equalsIgnoreCase(relType.trim());
    }

    public static boolean isFlowEdge(String relType, boolean includeAlias) {
        return isCallEdge(relType) || (includeAlias && isAliasEdge(relType));
    }

    public static boolean isMethodNode(GraphNode node) {
        return node != null && "method".equalsIgnoreCase(node.getKind());
    }

    public static boolean meetsMinConfidence(String current, String min) {
        return confidenceScore(normalizeConfidence(current)) >= confidenceScore(normalizeConfidence(min));
    }

    public static String normalizeConfidence(String confidence) {
        if (confidence == null) {
            return "low";
        }
        String value = confidence.trim().toLowerCase();
        if ("high".equals(value) || "medium".equals(value) || "low".equals(value)) {
            return value;
        }
        return "low";
    }

    public static int confidenceScore(String confidence) {
        return switch (normalizeConfidence(confidence)) {
            case "high" -> 3;
            case "medium" -> 2;
            default -> 1;
        };
    }

    private static boolean isBlacklisted(GraphNode node, Set<String> blacklist) {
        if (node == null || blacklist == null || blacklist.isEmpty()) {
            return false;
        }
        String clazz = node.getClassName();
        return clazz != null && blacklist.contains(clazz);
    }
}
