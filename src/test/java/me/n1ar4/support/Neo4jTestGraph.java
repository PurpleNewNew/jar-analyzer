/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.support;

import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class Neo4jTestGraph {
    private Neo4jTestGraph() {
    }

    public static GraphSnapshot snapshot() {
        return new GraphStore().loadSnapshot();
    }

    public static int countMethodNodes() {
        return snapshot().getNodesByKindView("method").size();
    }

    public static int countDistinctMethodClasses() {
        Set<String> classes = new HashSet<>();
        for (GraphNode node : snapshot().getNodesByKindView("method")) {
            classes.add(normalizeClass(node.getClassName()));
        }
        return classes.size();
    }

    public static List<CallEdge> callEdges() {
        GraphSnapshot snapshot = snapshot();
        List<CallEdge> out = new ArrayList<>();
        List<GraphNode> methods = snapshot.getNodesByKindView("method");
        for (GraphNode src : methods) {
            for (GraphEdge edge : snapshot.getOutgoingView(src.getNodeId())) {
                if (!isCallEdge(edge)) {
                    continue;
                }
                GraphNode dst = snapshot.getNode(edge.getDstId());
                if (dst == null || !"method".equalsIgnoreCase(dst.getKind())) {
                    continue;
                }
                out.add(new CallEdge(src, dst, edge));
            }
        }
        return out;
    }

    public static int countCallEdges(Predicate<CallEdge> predicate) {
        int count = 0;
        for (CallEdge edge : callEdges()) {
            if (predicate == null || predicate.test(edge)) {
                count++;
            }
        }
        return count;
    }

    public static MethodRef pickFirstMethod() {
        List<GraphNode> methods = new ArrayList<>(snapshot().getNodesByKindView("method"));
        methods.sort(Comparator
                .comparingInt(GraphNode::getJarId)
                .thenComparing(node -> normalizeClass(node.getClassName()))
                .thenComparing(node -> safe(node.getMethodName()))
                .thenComparing(node -> safe(node.getMethodDesc())));
        if (methods.isEmpty()) {
            return null;
        }
        GraphNode first = methods.get(0);
        return new MethodRef(
                normalizeClass(first.getClassName()),
                safe(first.getMethodName()),
                safe(first.getMethodDesc()));
    }

    public static EdgeRef pickCalleeWithSingleCaller() {
        List<CallEdge> edges = callEdges();
        if (edges.isEmpty()) {
            return null;
        }
        edges.sort(Comparator
                .comparingInt((CallEdge e) -> e.callee().getJarId())
                .thenComparing(e -> normalizeClass(e.callee().getClassName()))
                .thenComparing(e -> safe(e.callee().getMethodName()))
                .thenComparing(e -> safe(e.callee().getMethodDesc()))
                .thenComparing(e -> normalizeClass(e.caller().getClassName()))
                .thenComparing(e -> safe(e.caller().getMethodName()))
                .thenComparing(e -> safe(e.caller().getMethodDesc())));

        int idx = 0;
        while (idx < edges.size()) {
            CallEdge first = edges.get(idx);
            String calleeKey = methodKey(first.callee());
            String callerKey = methodKey(first.caller());
            boolean multiCaller = false;
            int next = idx + 1;
            while (next < edges.size() && calleeKey.equals(methodKey(edges.get(next).callee()))) {
                if (!callerKey.equals(methodKey(edges.get(next).caller()))) {
                    multiCaller = true;
                }
                next++;
            }
            if (!multiCaller) {
                return new EdgeRef(
                        normalizeClass(first.caller().getClassName()),
                        safe(first.caller().getMethodName()),
                        safe(first.caller().getMethodDesc()),
                        normalizeClass(first.callee().getClassName()),
                        safe(first.callee().getMethodName()),
                        safe(first.callee().getMethodDesc()));
            }
            idx = next;
        }
        return null;
    }

    public static boolean existsCallEdge(String callerClass,
                                         String callerMethod,
                                         String callerDesc,
                                         String calleeClass,
                                         String calleeMethod,
                                         String calleeDesc,
                                         String edgeType,
                                         String evidenceContains) {
        for (CallEdge call : callEdges()) {
            if (!matchesMethod(call.caller(), callerClass, callerMethod, callerDesc)) {
                continue;
            }
            if (!matchesMethod(call.callee(), calleeClass, calleeMethod, calleeDesc)) {
                continue;
            }
            if (!safe(edgeType).isBlank() && !safe(edgeType).equals(resolveEdgeType(call.edge()))) {
                continue;
            }
            if (!safe(evidenceContains).isBlank()
                    && !safe(call.edge().getEvidence()).contains(safe(evidenceContains))) {
                continue;
            }
            return true;
        }
        return false;
    }

    public static String resolveEdgeType(GraphEdge edge) {
        if (edge == null) {
            return "direct";
        }
        String rel = safe(edge.getRelType());
        return switch (rel) {
            case "CALLS_DISPATCH" -> "dispatch";
            case "CALLS_REFLECTION" -> "reflection";
            case "CALLS_CALLBACK" -> "callback";
            case "CALLS_OVERRIDE" -> "override";
            case "CALLS_FRAMEWORK" -> "framework";
            case "CALLS_METHOD_HANDLE" -> "method_handle";
            case "CALLS_PTA" -> "pta";
            default -> resolveFromEvidence(edge.getEvidence());
        };
    }

    private static boolean isCallEdge(GraphEdge edge) {
        return edge != null
                && edge.getRelType() != null
                && edge.getRelType().startsWith("CALLS_");
    }

    private static boolean matchesMethod(GraphNode node,
                                         String className,
                                         String methodName,
                                         String methodDesc) {
        if (node == null) {
            return false;
        }
        String cls = normalizeClass(className);
        if (!cls.isBlank() && !normalizeClass(node.getClassName()).equals(cls)) {
            return false;
        }
        String method = safe(methodName);
        if (!method.isBlank() && !safe(node.getMethodName()).equals(method)) {
            return false;
        }
        String desc = safe(methodDesc);
        return desc.isBlank() || safe(node.getMethodDesc()).equals(desc);
    }

    private static String resolveFromEvidence(String evidence) {
        String ev = safe(evidence).toLowerCase();
        if (ev.contains("pta:") || ev.contains("pta_ctx=")) {
            return "pta";
        }
        if (ev.contains("method_handle")) {
            return "method_handle";
        }
        if (ev.contains("framework") || ev.contains("spring_web_entry")) {
            return "framework";
        }
        if (ev.contains("callback") || ev.contains("dynamic_proxy")) {
            return "callback";
        }
        if (ev.contains("reflection") || ev.contains("tamiflex") || ev.contains("reflect")) {
            return "reflection";
        }
        return "direct";
    }

    private static String normalizeClass(String value) {
        String out = safe(value).replace('\\', '/');
        if (out.endsWith(".class")) {
            out = out.substring(0, out.length() - ".class".length());
        }
        return out;
    }

    private static String methodKey(GraphNode node) {
        return normalizeClass(node.getClassName())
                + "#" + safe(node.getMethodName())
                + "#" + safe(node.getMethodDesc());
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public record CallEdge(GraphNode caller, GraphNode callee, GraphEdge edge) {
    }

    public record MethodRef(String className, String methodName, String methodDesc) {
    }

    public record EdgeRef(String callerClassName,
                          String callerMethodName,
                          String callerMethodDesc,
                          String calleeClassName,
                          String calleeMethodName,
                          String calleeMethodDesc) {
    }
}
