/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.taie;

import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.MethodCallUtils;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.core.scope.AnalysisScopeRules;
import me.n1ar4.jar.analyzer.engine.project.ProjectOrigin;
import org.objectweb.asm.Opcodes;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class TaieEdgeMapper {
    private static final String EDGE_POLICY_PROP = "jar.analyzer.taie.edge.policy";

    private TaieEdgeMapper() {
    }

    public static EdgePolicy resolveEdgePolicy() {
        return EdgePolicy.fromSystemProperty();
    }

    public static MappingResult map(CallGraph<Invoke, JMethod> callGraph,
                                    Map<MethodReference.Handle, MethodReference> methodMap,
                                    Map<Integer, ProjectOrigin> jarOrigins) {
        return map(callGraph, methodMap, jarOrigins, resolveEdgePolicy());
    }

    public static MappingResult map(CallGraph<Invoke, JMethod> callGraph,
                                    Map<MethodReference.Handle, MethodReference> methodMap,
                                    Map<Integer, ProjectOrigin> jarOrigins,
                                    EdgePolicy edgePolicy) {
        if (callGraph == null || methodMap == null || methodMap.isEmpty()) {
            return MappingResult.empty();
        }
        EdgePolicy policy = edgePolicy == null ? EdgePolicy.REACHABLE_APP : edgePolicy;

        Map<String, MethodReference.Handle> lookup = buildLookup(methodMap);
        Map<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls = new HashMap<>();
        Map<MethodCallKey, MethodCallMeta> callMeta = new HashMap<>();
        List<ResolvedEdge> resolvedEdges = new ArrayList<>();

        List<Edge<Invoke, JMethod>> edges = callGraph.edges().toList();
        int totalEdges = edges.size();
        int unresolvedCaller = 0;
        int unresolvedCallee = 0;

        for (Edge<Invoke, JMethod> edge : edges) {
            if (edge == null || edge.getCallSite() == null) {
                continue;
            }
            JMethod callerMethod = edge.getCallSite().getContainer();
            JMethod calleeMethod = edge.getCallee();
            MethodReference.Handle caller = resolveHandle(callerMethod, lookup);
            if (caller == null) {
                unresolvedCaller++;
                continue;
            }
            MethodReference.Handle callee = resolveHandle(calleeMethod, lookup);
            if (callee == null) {
                unresolvedCallee++;
                continue;
            }
            ProjectOrigin callerOrigin = resolveOrigin(caller, jarOrigins);
            ProjectOrigin calleeOrigin = resolveOrigin(callee, jarOrigins);
            resolvedEdges.add(new ResolvedEdge(caller, callee, edge.getKind(), callerOrigin, calleeOrigin));
        }

        Set<MethodReference.Handle> allowedCallers = resolveAllowedCallers(resolvedEdges, policy);
        int keptEdges = 0;
        int skippedByPolicy = 0;
        for (ResolvedEdge edge : resolvedEdges) {
            if (!shouldKeep(edge, allowedCallers, policy)) {
                skippedByPolicy++;
                continue;
            }
            HashSet<MethodReference.Handle> callees = methodCalls.computeIfAbsent(edge.caller(), ignore -> new HashSet<>());
            MethodCallUtils.addCallee(callees, edge.callee());
            MethodCallMeta.record(
                    callMeta,
                    MethodCallKey.of(edge.caller(), edge.callee()),
                    resolveEdgeType(edge.kind()),
                    resolveConfidence(edge.kind()),
                    "taie:" + kindName(edge.kind()),
                    resolveOpcode(edge.kind())
            );
            keptEdges++;
        }

        return new MappingResult(methodCalls, callMeta, totalEdges, keptEdges,
                skippedByPolicy, unresolvedCaller, unresolvedCallee, policy.value());
    }

    private static Map<String, MethodReference.Handle> buildLookup(Map<MethodReference.Handle, MethodReference> methodMap) {
        Map<String, MethodReference.Handle> lookup = new HashMap<>();
        for (MethodReference method : methodMap.values()) {
            if (method == null || method.getClassReference() == null) {
                continue;
            }
            String className = normalizeClassName(method.getClassReference().getName());
            String methodName = safe(method.getName());
            String desc = safe(method.getDesc());
            if (className.isEmpty() || methodName.isEmpty() || desc.isEmpty()) {
                continue;
            }
            String key = methodKey(className, methodName, desc);
            lookup.putIfAbsent(key, method.getHandle());
        }
        return lookup;
    }

    private static MethodReference.Handle resolveHandle(JMethod method,
                                                        Map<String, MethodReference.Handle> lookup) {
        if (method == null || method.getDeclaringClass() == null) {
            return null;
        }
        String className = normalizeClassName(method.getDeclaringClass().getName());
        String methodName = safe(method.getName());
        String desc = buildMethodDesc(method);
        if (className.isEmpty() || methodName.isEmpty() || desc.isEmpty()) {
            return null;
        }
        return lookup.get(methodKey(className, methodName, desc));
    }

    private static ProjectOrigin resolveOrigin(MethodReference.Handle handle,
                                               Map<Integer, ProjectOrigin> jarOrigins) {
        if (handle == null || handle.getClassReference() == null) {
            return ProjectOrigin.LIBRARY;
        }
        Integer jarId = handle.getJarId();
        if (jarId != null && jarId > 0 && jarOrigins != null && !jarOrigins.isEmpty()) {
            ProjectOrigin origin = jarOrigins.get(jarId);
            if (origin != null) {
                return origin;
            }
        }
        String className = normalizeClassName(handle.getClassReference().getName());
        if (AnalysisScopeRules.isForceTargetClass(className)) {
            return ProjectOrigin.APP;
        }
        if (AnalysisScopeRules.isJdkClass(className)) {
            return ProjectOrigin.SDK;
        }
        if (AnalysisScopeRules.isCommonLibraryClass(className)) {
            return ProjectOrigin.LIBRARY;
        }
        return ProjectOrigin.APP;
    }

    private static Set<MethodReference.Handle> resolveAllowedCallers(List<ResolvedEdge> edges,
                                                                     EdgePolicy policy) {
        if (edges == null || edges.isEmpty()) {
            return Set.of();
        }
        return switch (policy) {
            case APP_CALLER -> collectCallersByOrigin(edges, ProjectOrigin.APP);
            case NON_SDK_CALLER -> collectCallersByNonSdk(edges);
            case FULL -> collectAllCallers(edges);
            case REACHABLE_APP -> collectReachableNonSdkCallers(edges);
        };
    }

    private static Set<MethodReference.Handle> collectCallersByOrigin(List<ResolvedEdge> edges,
                                                                       ProjectOrigin targetOrigin) {
        Set<MethodReference.Handle> out = new LinkedHashSet<>();
        for (ResolvedEdge edge : edges) {
            if (edge == null || edge.caller() == null) {
                continue;
            }
            if (edge.callerOrigin() == targetOrigin) {
                out.add(edge.caller());
            }
        }
        return out;
    }

    private static Set<MethodReference.Handle> collectCallersByNonSdk(List<ResolvedEdge> edges) {
        Set<MethodReference.Handle> out = new LinkedHashSet<>();
        for (ResolvedEdge edge : edges) {
            if (edge == null || edge.caller() == null) {
                continue;
            }
            if (edge.callerOrigin() != ProjectOrigin.SDK) {
                out.add(edge.caller());
            }
        }
        return out;
    }

    private static Set<MethodReference.Handle> collectAllCallers(List<ResolvedEdge> edges) {
        Set<MethodReference.Handle> out = new LinkedHashSet<>();
        for (ResolvedEdge edge : edges) {
            if (edge != null && edge.caller() != null) {
                out.add(edge.caller());
            }
        }
        return out;
    }

    private static Set<MethodReference.Handle> collectReachableNonSdkCallers(List<ResolvedEdge> edges) {
        Set<MethodReference.Handle> seeds = new LinkedHashSet<>();
        Map<MethodReference.Handle, Set<MethodReference.Handle>> links = new HashMap<>();
        for (ResolvedEdge edge : edges) {
            if (edge == null || edge.caller() == null || edge.callee() == null) {
                continue;
            }
            if (edge.callerOrigin() == ProjectOrigin.APP) {
                seeds.add(edge.caller());
            }
            if (edge.calleeOrigin() == ProjectOrigin.APP) {
                seeds.add(edge.callee());
            }
            if (edge.callerOrigin() == ProjectOrigin.SDK || edge.calleeOrigin() == ProjectOrigin.SDK) {
                continue;
            }
            links.computeIfAbsent(edge.caller(), ignore -> new LinkedHashSet<>()).add(edge.callee());
            links.computeIfAbsent(edge.callee(), ignore -> new LinkedHashSet<>()).add(edge.caller());
        }
        if (seeds.isEmpty()) {
            return Set.of();
        }

        Set<MethodReference.Handle> reachable = new LinkedHashSet<>();
        ArrayDeque<MethodReference.Handle> queue = new ArrayDeque<>();
        for (MethodReference.Handle seed : seeds) {
            if (seed != null && reachable.add(seed)) {
                queue.add(seed);
            }
        }
        while (!queue.isEmpty()) {
            MethodReference.Handle current = queue.poll();
            Set<MethodReference.Handle> next = links.get(current);
            if (next == null || next.isEmpty()) {
                continue;
            }
            for (MethodReference.Handle candidate : next) {
                if (candidate != null && reachable.add(candidate)) {
                    queue.add(candidate);
                }
            }
        }

        Set<MethodReference.Handle> callers = new LinkedHashSet<>();
        for (ResolvedEdge edge : edges) {
            if (edge == null || edge.caller() == null) {
                continue;
            }
            if (edge.callerOrigin() == ProjectOrigin.SDK) {
                continue;
            }
            if (reachable.contains(edge.caller())) {
                callers.add(edge.caller());
            }
        }
        return callers;
    }

    private static boolean shouldKeep(ResolvedEdge edge,
                                      Set<MethodReference.Handle> allowedCallers,
                                      EdgePolicy policy) {
        if (edge == null || edge.caller() == null) {
            return false;
        }
        return switch (policy) {
            case FULL -> true;
            case APP_CALLER -> edge.callerOrigin() == ProjectOrigin.APP;
            case NON_SDK_CALLER -> edge.callerOrigin() != ProjectOrigin.SDK;
            case REACHABLE_APP -> edge.callerOrigin() != ProjectOrigin.SDK
                    && allowedCallers.contains(edge.caller());
        };
    }

    private static String methodKey(String className, String methodName, String desc) {
        return className + "#" + methodName + "#" + desc;
    }

    private static String normalizeClassName(String className) {
        if (className == null) {
            return "";
        }
        String normalized = className.trim().replace('.', '/').replace('\\', '/');
        if (normalized.endsWith(".class")) {
            normalized = normalized.substring(0, normalized.length() - ".class".length());
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static String buildMethodDesc(JMethod method) {
        if (method == null) {
            return "";
        }
        List<Type> paramTypes = method.getParamTypes();
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        if (paramTypes != null) {
            for (Type paramType : paramTypes) {
                sb.append(toJvmTypeDesc(paramType));
            }
        }
        sb.append(')');
        sb.append(toJvmTypeDesc(method.getReturnType()));
        return sb.toString();
    }

    private static String toJvmTypeDesc(Type type) {
        if (type == null) {
            return "Ljava/lang/Object;";
        }
        String name = safe(type.getName());
        if (name.isEmpty()) {
            return "Ljava/lang/Object;";
        }
        int dims = 0;
        while (name.endsWith("[]")) {
            dims++;
            name = name.substring(0, name.length() - 2);
        }
        String base = switch (name) {
            case "void" -> "V";
            case "boolean" -> "Z";
            case "byte" -> "B";
            case "char" -> "C";
            case "short" -> "S";
            case "int" -> "I";
            case "long" -> "J";
            case "float" -> "F";
            case "double" -> "D";
            default -> "L" + name.replace('.', '/') + ";";
        };
        if (dims <= 0) {
            return base;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dims; i++) {
            sb.append('[');
        }
        sb.append(base);
        return sb.toString();
    }

    private static String resolveEdgeType(CallKind kind) {
        if (kind == null) {
            return MethodCallMeta.TYPE_UNKNOWN;
        }
        return switch (kind) {
            case STATIC, SPECIAL -> MethodCallMeta.TYPE_DIRECT;
            case VIRTUAL, INTERFACE -> MethodCallMeta.TYPE_DISPATCH;
            case DYNAMIC -> MethodCallMeta.TYPE_INDY;
            case OTHER -> MethodCallMeta.TYPE_UNKNOWN;
        };
    }

    private static String resolveConfidence(CallKind kind) {
        if (kind == null) {
            return MethodCallMeta.CONF_LOW;
        }
        return switch (kind) {
            case STATIC, SPECIAL -> MethodCallMeta.CONF_HIGH;
            case VIRTUAL, INTERFACE, DYNAMIC -> MethodCallMeta.CONF_MEDIUM;
            case OTHER -> MethodCallMeta.CONF_LOW;
        };
    }

    private static int resolveOpcode(CallKind kind) {
        if (kind == null) {
            return -1;
        }
        return switch (kind) {
            case STATIC -> Opcodes.INVOKESTATIC;
            case SPECIAL -> Opcodes.INVOKESPECIAL;
            case VIRTUAL -> Opcodes.INVOKEVIRTUAL;
            case INTERFACE -> Opcodes.INVOKEINTERFACE;
            case DYNAMIC -> Opcodes.INVOKEDYNAMIC;
            case OTHER -> -1;
        };
    }

    private static String kindName(CallKind kind) {
        if (kind == null) {
            return "unknown";
        }
        return kind.name().toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record ResolvedEdge(MethodReference.Handle caller,
                                MethodReference.Handle callee,
                                CallKind kind,
                                ProjectOrigin callerOrigin,
                                ProjectOrigin calleeOrigin) {
    }

    public record MappingResult(Map<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
                                Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                                int totalEdges,
                                int keptEdges,
                                int skippedByPolicy,
                                int unresolvedCaller,
                                int unresolvedCallee,
                                String edgePolicy) {
        private static MappingResult empty() {
            return new MappingResult(Map.of(), Map.of(), 0, 0, 0, 0, 0, EdgePolicy.REACHABLE_APP.value());
        }
    }

    public enum EdgePolicy {
        REACHABLE_APP("reachable-app"),
        APP_CALLER("app-caller"),
        NON_SDK_CALLER("non-sdk-caller"),
        FULL("full");

        private final String value;

        EdgePolicy(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public static EdgePolicy fromSystemProperty() {
            return fromValue(System.getProperty(EDGE_POLICY_PROP, REACHABLE_APP.value));
        }

        public static EdgePolicy fromValue(String raw) {
            if (raw == null || raw.isBlank()) {
                return REACHABLE_APP;
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "app-caller", "app", "app_only", "app-only", "target-caller" -> APP_CALLER;
                case "non-sdk-caller", "non_sdk_caller", "non-sdk", "non_sdk", "all-non-sdk" -> NON_SDK_CALLER;
                case "full", "all" -> FULL;
                case "reachable-app", "reachable_app", "reachable", "app-reachable", "default" -> REACHABLE_APP;
                default -> REACHABLE_APP;
            };
        }
    }
}
