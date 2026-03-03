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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class TaieEdgeMapper {
    private TaieEdgeMapper() {
    }

    public static MappingResult map(CallGraph<Invoke, JMethod> callGraph,
                                    Map<MethodReference.Handle, MethodReference> methodMap,
                                    Map<Integer, ProjectOrigin> jarOrigins) {
        if (callGraph == null || methodMap == null || methodMap.isEmpty()) {
            return MappingResult.empty();
        }

        Map<String, MethodReference.Handle> lookup = buildLookup(methodMap);
        Map<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls = new HashMap<>();
        Map<MethodCallKey, MethodCallMeta> callMeta = new HashMap<>();

        List<Edge<Invoke, JMethod>> edges = callGraph.edges().toList();
        int totalEdges = edges.size();
        int keptEdges = 0;
        int skippedNonTargetCaller = 0;
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
            if (!isTargetCaller(caller, jarOrigins)) {
                skippedNonTargetCaller++;
                continue;
            }
            MethodReference.Handle callee = resolveHandle(calleeMethod, lookup);
            if (callee == null) {
                unresolvedCallee++;
                continue;
            }

            HashSet<MethodReference.Handle> callees = methodCalls.computeIfAbsent(caller, ignore -> new HashSet<>());
            MethodCallUtils.addCallee(callees, callee);
            CallKind kind = edge.getKind();
            MethodCallMeta.record(
                    callMeta,
                    MethodCallKey.of(caller, callee),
                    resolveEdgeType(kind),
                    resolveConfidence(kind),
                    "taie:" + kindName(kind),
                    resolveOpcode(kind)
            );
            keptEdges++;
        }

        return new MappingResult(methodCalls, callMeta, totalEdges, keptEdges,
                skippedNonTargetCaller, unresolvedCaller, unresolvedCallee);
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

    private static boolean isTargetCaller(MethodReference.Handle caller,
                                          Map<Integer, ProjectOrigin> jarOrigins) {
        if (caller == null || caller.getClassReference() == null) {
            return false;
        }
        Integer jarId = caller.getJarId();
        if (jarId != null && jarId > 0 && jarOrigins != null && !jarOrigins.isEmpty()) {
            ProjectOrigin origin = jarOrigins.get(jarId);
            if (origin != null) {
                return origin == ProjectOrigin.APP;
            }
        }
        String className = normalizeClassName(caller.getClassReference().getName());
        if (AnalysisScopeRules.isForceTargetClass(className)) {
            return true;
        }
        if (AnalysisScopeRules.isJdkClass(className)) {
            return false;
        }
        return !AnalysisScopeRules.isCommonLibraryClass(className);
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

    public record MappingResult(Map<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
                                Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                                int totalEdges,
                                int keptEdges,
                                int skippedNonTargetCaller,
                                int unresolvedCaller,
                                int unresolvedCallee) {
        private static MappingResult empty() {
            return new MappingResult(Map.of(), Map.of(), 0, 0, 0, 0, 0);
        }
    }
}
