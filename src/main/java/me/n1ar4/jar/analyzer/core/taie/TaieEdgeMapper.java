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
import me.n1ar4.jar.analyzer.core.reference.AnnoReference;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
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
                                    Map<Integer, ProjectOrigin> jarOrigins,
                                    EdgePolicy edgePolicy) {
        if (callGraph == null || methodMap == null || methodMap.isEmpty()) {
            return MappingResult.empty();
        }
        EdgePolicy policy = edgePolicy == null ? EdgePolicy.APP_CALLER : edgePolicy;

        MethodLookup lookup = buildLookup(methodMap);
        Map<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls = new HashMap<>();
        Map<MethodCallKey, MethodCallMeta> callMeta = new HashMap<>();
        List<ResolvedEdge> resolvedEdges = new ArrayList<>();
        List<MethodReference> syntheticMethods = new ArrayList<>();

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
                caller = ensureSyntheticHandle(callerMethod, methodMap, lookup, syntheticMethods);
            }
            if (caller == null) {
                unresolvedCaller++;
                continue;
            }
            MethodReference.Handle callee = resolveHandle(calleeMethod, lookup);
            if (callee == null) {
                callee = ensureSyntheticHandle(calleeMethod, methodMap, lookup, syntheticMethods);
            }
            if (callee == null) {
                unresolvedCallee++;
                continue;
            }
            ProjectOrigin callerOrigin = resolveOrigin(caller, jarOrigins);
            ProjectOrigin calleeOrigin = resolveOrigin(callee, jarOrigins);
            resolvedEdges.add(new ResolvedEdge(caller, callee, edge.getKind(), callerOrigin, calleeOrigin));
        }

        Set<MethodReference.Handle> reachableAppCallers = policy == EdgePolicy.REACHABLE_APP
                ? collectReachableNonSdkCallers(resolvedEdges)
                : Set.of();
        int keptEdges = 0;
        int skippedByPolicy = 0;
        for (ResolvedEdge edge : resolvedEdges) {
            if (!shouldKeep(edge, reachableAppCallers, policy)) {
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
                skippedByPolicy, unresolvedCaller, unresolvedCallee, policy.value(), syntheticMethods);
    }

    private static MethodLookup buildLookup(Map<MethodReference.Handle, MethodReference> methodMap) {
        Map<String, MethodReference.Handle> exact = new HashMap<>();
        Map<String, List<MethodReference.Handle>> byClassAndMethod = new HashMap<>();
        Map<String, List<MethodReference.Handle>> byClassMethodArity = new HashMap<>();
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
            MethodReference.Handle handle = method.getHandle();
            exact.putIfAbsent(key, handle);
            String classMethod = classMethodKey(className, methodName);
            byClassAndMethod.computeIfAbsent(classMethod, ignore -> new ArrayList<>()).add(handle);
            int arity = descriptorArity(desc);
            if (arity >= 0) {
                String classMethodArity = classMethodArityKey(className, methodName, arity);
                byClassMethodArity.computeIfAbsent(classMethodArity, ignore -> new ArrayList<>()).add(handle);
            }
        }
        return new MethodLookup(exact, byClassAndMethod, byClassMethodArity);
    }

    private static MethodReference.Handle resolveHandle(JMethod method,
                                                        MethodLookup lookup) {
        if (method == null || method.getDeclaringClass() == null) {
            return null;
        }
        String className = normalizeClassName(method.getDeclaringClass().getName());
        String methodName = safe(method.getName());
        String desc = buildMethodDesc(method);
        if (className.isEmpty() || methodName.isEmpty() || desc.isEmpty()) {
            return null;
        }
        MethodReference.Handle exact = lookup.exact().get(methodKey(className, methodName, desc));
        if (exact != null) {
            return exact;
        }
        List<Type> paramTypes = method.getParamTypes();
        int arity = paramTypes == null ? 0 : paramTypes.size();
        List<MethodReference.Handle> arityCandidates = lookup.byClassMethodArity().get(
                classMethodArityKey(className, methodName, arity)
        );
        if (arityCandidates != null && arityCandidates.size() == 1) {
            return arityCandidates.get(0);
        }
        List<MethodReference.Handle> nameCandidates = lookup.byClassAndMethod().get(
                classMethodKey(className, methodName)
        );
        if (nameCandidates != null && nameCandidates.size() == 1) {
            return nameCandidates.get(0);
        }
        return null;
    }

    private static MethodReference.Handle ensureSyntheticHandle(JMethod method,
                                                                Map<MethodReference.Handle, MethodReference> methodMap,
                                                                MethodLookup lookup,
                                                                List<MethodReference> syntheticMethods) {
        if (method == null || method.getDeclaringClass() == null || methodMap == null || lookup == null) {
            return null;
        }
        String className = normalizeClassName(method.getDeclaringClass().getName());
        String methodName = safe(method.getName());
        String desc = buildMethodDesc(method);
        if (className.isEmpty() || methodName.isEmpty() || desc.isEmpty()) {
            return null;
        }
        if (AnalysisScopeRules.isJdkClass(className)) {
            return null;
        }
        if (AnalysisScopeRules.isCommonLibraryClass(className)
                && !AnalysisScopeRules.isForceTargetClass(className)) {
            return null;
        }
        MethodReference.Handle exact = lookup.exact().get(methodKey(className, methodName, desc));
        if (exact != null) {
            return exact;
        }
        ClassReference.Handle classHandle = new ClassReference.Handle(className, -1);
        int access = buildAccess(method);
        MethodReference synthesized = new MethodReference(
                classHandle,
                methodName,
                desc,
                method.isStatic(),
                new HashSet<AnnoReference>(),
                access,
                -1,
                "taie-synth",
                -1
        );
        MethodReference.Handle handle = synthesized.getHandle();
        MethodReference existing = methodMap.putIfAbsent(handle, synthesized);
        MethodReference target = existing == null ? synthesized : existing;
        MethodReference.Handle targetHandle = target.getHandle();
        if (existing == null && syntheticMethods != null) {
            syntheticMethods.add(target);
        }
        registerLookupEntry(lookup, target);
        return targetHandle;
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

    private static String classMethodKey(String className, String methodName) {
        return className + "#" + methodName;
    }

    private static String classMethodArityKey(String className, String methodName, int arity) {
        return className + "#" + methodName + "#" + arity;
    }

    private static String normalizeClassName(String className) {
        if (className == null) {
            return "";
        }
        String normalized = className.trim().replace('.', '/').replace('\\', '/');
        if (normalized.startsWith("L") && normalized.endsWith(";") && normalized.length() > 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (normalized.endsWith(".class")) {
            normalized = normalized.substring(0, normalized.length() - ".class".length());
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static int descriptorArity(String desc) {
        if (desc == null || desc.isBlank()) {
            return -1;
        }
        try {
            return org.objectweb.asm.Type.getArgumentTypes(desc).length;
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static int buildAccess(JMethod method) {
        if (method == null || method.getModifiers() == null) {
            return 0;
        }
        int access = 0;
        for (pascal.taie.language.classes.Modifier modifier : method.getModifiers()) {
            if (modifier == null) {
                continue;
            }
            switch (modifier) {
                case PUBLIC -> access |= Opcodes.ACC_PUBLIC;
                case PRIVATE -> access |= Opcodes.ACC_PRIVATE;
                case PROTECTED -> access |= Opcodes.ACC_PROTECTED;
                case STATIC -> access |= Opcodes.ACC_STATIC;
                case FINAL -> access |= Opcodes.ACC_FINAL;
                case ABSTRACT -> access |= Opcodes.ACC_ABSTRACT;
                case NATIVE -> access |= Opcodes.ACC_NATIVE;
                case SYNCHRONIZED -> access |= Opcodes.ACC_SYNCHRONIZED;
                case STRICTFP -> access |= Opcodes.ACC_STRICT;
                case TRANSIENT -> access |= Opcodes.ACC_TRANSIENT;
                case VOLATILE -> access |= Opcodes.ACC_VOLATILE;
                case BRIDGE -> access |= Opcodes.ACC_BRIDGE;
                case VARARGS -> access |= Opcodes.ACC_VARARGS;
                case SYNTHETIC -> access |= Opcodes.ACC_SYNTHETIC;
                default -> {
                }
            }
        }
        return access;
    }

    private static void registerLookupEntry(MethodLookup lookup, MethodReference method) {
        if (lookup == null || method == null || method.getClassReference() == null) {
            return;
        }
        String className = normalizeClassName(method.getClassReference().getName());
        String methodName = safe(method.getName());
        String desc = safe(method.getDesc());
        if (className.isEmpty() || methodName.isEmpty() || desc.isEmpty()) {
            return;
        }
        MethodReference.Handle handle = method.getHandle();
        lookup.exact().putIfAbsent(methodKey(className, methodName, desc), handle);
        lookup.byClassAndMethod()
                .computeIfAbsent(classMethodKey(className, methodName), ignore -> new ArrayList<>())
                .add(handle);
        int arity = descriptorArity(desc);
        if (arity >= 0) {
            lookup.byClassMethodArity()
                    .computeIfAbsent(classMethodArityKey(className, methodName, arity), ignore -> new ArrayList<>())
                    .add(handle);
        }
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

    private record MethodLookup(Map<String, MethodReference.Handle> exact,
                                Map<String, List<MethodReference.Handle>> byClassAndMethod,
                                Map<String, List<MethodReference.Handle>> byClassMethodArity) {
    }

    public record MappingResult(Map<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
                                Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                                int totalEdges,
                                int keptEdges,
                                int skippedByPolicy,
                                int unresolvedCaller,
                                int unresolvedCallee,
                                String edgePolicy,
                                List<MethodReference> syntheticMethods) {
        private static MappingResult empty() {
            return new MappingResult(Map.of(), Map.of(), 0, 0, 0, 0, 0, EdgePolicy.APP_CALLER.value(), List.of());
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
            return fromValue(System.getProperty(EDGE_POLICY_PROP, APP_CALLER.value));
        }

        public static EdgePolicy fromValue(String raw) {
            if (raw == null || raw.isBlank()) {
                return APP_CALLER;
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "app-caller", "app", "app_only", "app-only", "target-caller" -> APP_CALLER;
                case "non-sdk-caller", "non_sdk_caller", "non-sdk", "non_sdk", "all-non-sdk" -> NON_SDK_CALLER;
                case "full", "all" -> FULL;
                case "reachable-app", "reachable_app", "reachable", "app-reachable" -> REACHABLE_APP;
                default -> APP_CALLER;
            };
        }
    }
}
