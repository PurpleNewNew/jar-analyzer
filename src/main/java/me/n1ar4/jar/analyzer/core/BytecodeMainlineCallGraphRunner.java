/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.core.build.BuildContext;
import me.n1ar4.jar.analyzer.core.bytecode.BuildBytecodeWorkspace;
import me.n1ar4.jar.analyzer.core.edge.BuildEdgeAccumulator;
import me.n1ar4.jar.analyzer.core.facts.BuildFactAssembler;
import me.n1ar4.jar.analyzer.core.facts.BuildFactSnapshot;
import me.n1ar4.jar.analyzer.core.facts.BytecodeFactRunner;
import me.n1ar4.jar.analyzer.core.pta.SelectivePtaRefiner;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class BytecodeMainlineCallGraphRunner {
    public static final String ENGINE = "bytecode-mainline";
    public static final String MODE_SEMANTIC_V1 = "bytecode:semantic-v1";
    public static final String MODE_FAST_V1 = "bytecode:fast-v1";
    public static final String MODE_BALANCED_V1 = "bytecode:balanced-v1";
    public static final String MODE_PRECISION_V1 = "bytecode:precision-v1";

    private BytecodeMainlineCallGraphRunner() {
    }

    public static Result run(BuildContext context) {
        if (context == null) {
            return Result.empty();
        }
        BuildBytecodeWorkspace workspace = BuildBytecodeWorkspace.parse(context.classFileList);
        BytecodeFactRunner.Result facts = BytecodeFactRunner.collect(context, null, Map.of(), List.of(), workspace);
        Result result = run(facts.snapshot(), facts.edges(), Settings.legacySemanticV1());
        facts.edges().copyInto(context);
        return result;
    }

    public static Result run(BuildFactSnapshot snapshot,
                             BuildEdgeAccumulator edges,
                             Settings settings) {
        if (snapshot == null || edges == null) {
            return Result.empty();
        }
        BuildContext legacyView = BuildFactAssembler.legacyView(snapshot, edges);
        Settings resolved = settings == null ? Settings.legacySemanticV1() : settings;
        InheritanceMap inheritanceMap = snapshot.types().inheritanceMap();
        BuildBytecodeWorkspace workspace = snapshot.bytecode().workspace();
        MethodLookup lookup = MethodLookup.build(snapshot.methods().methodsByHandle());
        SeedResult seeded = seedDeclaredEdges(
                legacyView.methodCalls,
                legacyView.methodCallMeta,
                snapshot.symbols().callSites(),
                lookup
        );
        Set<ClassReference.Handle> instantiatedClasses = snapshot.types().instantiatedClasses().isEmpty()
                ? DispatchCallResolver.collectInstantiatedClasses(workspace, inheritanceMap)
                : snapshot.types().instantiatedClasses();
        int typedDispatchEdges = TypedDispatchResolver.expandWithTypes(
                legacyView.methodCalls,
                legacyView.methodCallMeta,
                snapshot.methods().methodsByHandle(),
                snapshot.types().classesByHandle(),
                inheritanceMap,
                snapshot.symbols().callSites()
        );
        int dispatchExpansionEdges = DispatchCallResolver.expandVirtualCalls(
                legacyView.methodCalls,
                legacyView.methodCallMeta,
                snapshot.methods().methodsByHandle(),
                snapshot.types().classesByHandle(),
                inheritanceMap,
                instantiatedClasses
        );
        BytecodeMainlineReflectionResolver.Result reflectionResult =
                BytecodeMainlineReflectionResolver.appendEdges(legacyView, workspace, lookup);
        BytecodeMainlineSemanticEdgeRunner.Result semanticResult =
                BytecodeMainlineSemanticEdgeRunner.appendEdges(
                        legacyView,
                        inheritanceMap,
                        instantiatedClasses,
                        lookup
                );
        BuildEdgeAccumulator updated = BuildEdgeAccumulator.fromContext(legacyView);
        SelectivePtaRefiner.Result ptaResult = resolved.enableSelectivePta()
                ? SelectivePtaRefiner.refine(snapshot, updated, workspace, inheritanceMap)
                : SelectivePtaRefiner.Result.empty();
        edges.methodCalls().clear();
        edges.methodCallMeta().clear();
        edges.methodCalls().putAll(updated.methodCalls());
        edges.methodCallMeta().putAll(updated.methodCallMeta());
        long totalEdges = countEdges(edges.methodCalls());
        return new Result(
                seeded.directEdges(),
                seeded.declaredDispatchEdges(),
                seeded.invokeDynamicEdges() + reflectionResult.invokeDynamicEdges(),
                typedDispatchEdges,
                dispatchExpansionEdges,
                reflectionResult.reflectionEdges(),
                reflectionResult.methodHandleEdges(),
                semanticResult.callbackEdges(),
                semanticResult.frameworkEdges(),
                semanticResult.threadStartEdges(),
                semanticResult.executorEdges(),
                semanticResult.completableFutureEdges(),
                semanticResult.doPrivilegedEdges(),
                semanticResult.dynamicProxyEdges(),
                semanticResult.triggerBridgeEdges(),
                ptaResult.ptaEdges(),
                ptaResult.refinedCallSites(),
                ptaResult.hotspotCallSites(),
                ptaResult.fieldSites(),
                ptaResult.arraySites(),
                ptaResult.arrayCopySites(),
                instantiatedClasses.size(),
                seeded.unresolvedCallerCount(),
                seeded.unresolvedDeclaredTargetCount(),
                totalEdges
        );
    }

    public static Result run(BuildContext context, Settings settings) {
        if (context == null) {
            return Result.empty();
        }
        BytecodeFactRunner.Result facts = BytecodeFactRunner.collect(context, null, Map.of(), List.of());
        Result result = run(facts.snapshot(), facts.edges(), settings);
        facts.edges().copyInto(context);
        return result;
    }

    private static SeedResult seedDeclaredEdges(Map<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
                                                Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                                                List<CallSiteEntity> callSites,
                                                MethodLookup lookup) {
        if (methodCalls == null || methodCallMeta == null || callSites == null || callSites.isEmpty() || lookup == null) {
            return SeedResult.empty();
        }
        int directEdges = 0;
        int declaredDispatchEdges = 0;
        int invokeDynamicEdges = 0;
        int unresolvedCallerCount = 0;
        int unresolvedDeclaredTargetCount = 0;
        for (CallSiteEntity site : callSites) {
            if (site == null) {
                continue;
            }
            MethodReference.Handle caller = lookup.resolve(
                    site.getCallerClassName(),
                    site.getCallerMethodName(),
                    site.getCallerMethodDesc(),
                    site.getJarId()
            );
            if (caller == null) {
                unresolvedCallerCount++;
                continue;
            }
            MethodReference.Handle callee = lookup.resolve(
                    site.getCalleeOwner(),
                    site.getCalleeMethodName(),
                    site.getCalleeMethodDesc(),
                    null
            );
            if (callee == null) {
                unresolvedDeclaredTargetCount++;
                continue;
            }
            int opcode = site.getOpCode() == null ? -1 : site.getOpCode();
            EdgeSeed seed = edgeSeedForOpcode(opcode);
            if (seed == null) {
                continue;
            }
            HashSet<MethodReference.Handle> callees = methodCalls.computeIfAbsent(caller, ignore -> new HashSet<>());
            MethodReference.Handle scopedCallee = new MethodReference.Handle(
                    callee.getClassReference(),
                    opcode,
                    callee.getName(),
                    callee.getDesc()
            );
            boolean inserted = MethodCallUtils.addCallee(callees, scopedCallee);
            MethodCallMeta.record(
                    methodCallMeta,
                    MethodCallKey.of(caller, scopedCallee),
                    seed.type(),
                    seed.confidence(),
                    seed.reason(),
                    opcode
            );
            if (!inserted) {
                continue;
            }
            switch (seed.kind()) {
                case DIRECT -> directEdges++;
                case DISPATCH -> declaredDispatchEdges++;
                case INDY -> invokeDynamicEdges++;
            }
        }
        return new SeedResult(
                directEdges,
                declaredDispatchEdges,
                invokeDynamicEdges,
                unresolvedCallerCount,
                unresolvedDeclaredTargetCount
        );
    }

    private static EdgeSeed edgeSeedForOpcode(int opcode) {
        return switch (opcode) {
            case Opcodes.INVOKESTATIC -> new EdgeSeed(SeedKind.DIRECT, MethodCallMeta.TYPE_DIRECT, MethodCallMeta.CONF_HIGH, "bytecode_static");
            case Opcodes.INVOKESPECIAL -> new EdgeSeed(SeedKind.DIRECT, MethodCallMeta.TYPE_DIRECT, MethodCallMeta.CONF_HIGH, "bytecode_special");
            case Opcodes.INVOKEVIRTUAL -> new EdgeSeed(SeedKind.DISPATCH, MethodCallMeta.TYPE_DISPATCH, MethodCallMeta.CONF_LOW, "declared:virtual");
            case Opcodes.INVOKEINTERFACE -> new EdgeSeed(SeedKind.DISPATCH, MethodCallMeta.TYPE_DISPATCH, MethodCallMeta.CONF_LOW, "declared:interface");
            case Opcodes.INVOKEDYNAMIC -> new EdgeSeed(SeedKind.INDY, MethodCallMeta.TYPE_INDY, MethodCallMeta.CONF_MEDIUM, "bytecode_indy");
            default -> null;
        };
    }

    private static long countEdges(Map<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls) {
        if (methodCalls == null || methodCalls.isEmpty()) {
            return 0L;
        }
        long count = 0L;
        for (Set<MethodReference.Handle> callees : methodCalls.values()) {
            if (callees != null) {
                count += callees.size();
            }
        }
        return count;
    }

    public record Result(int directEdges,
                         int declaredDispatchEdges,
                         int invokeDynamicEdges,
                         int typedDispatchEdges,
                         int dispatchExpansionEdges,
                         int reflectionEdges,
                         int methodHandleEdges,
                         int callbackEdges,
                         int frameworkEdges,
                         int threadStartEdges,
                         int executorEdges,
                         int completableFutureEdges,
                         int doPrivilegedEdges,
                         int dynamicProxyEdges,
                         int triggerBridgeEdges,
                         int ptaEdges,
                         int refinedPtaCallSites,
                         int ptaHotspotCallSites,
                         int ptaFieldSites,
                         int ptaArraySites,
                         int ptaArrayCopySites,
                         int instantiatedClassCount,
                         int unresolvedCallerCount,
                         int unresolvedDeclaredTargetCount,
                         long totalEdges) {
        static Result empty() {
            return new Result(
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0L
            );
        }
    }

    private record SeedResult(int directEdges,
                              int declaredDispatchEdges,
                              int invokeDynamicEdges,
                              int unresolvedCallerCount,
                              int unresolvedDeclaredTargetCount) {
        private static SeedResult empty() {
            return new SeedResult(0, 0, 0, 0, 0);
        }
    }

    private enum SeedKind {
        DIRECT,
        DISPATCH,
        INDY
    }

    private record EdgeSeed(SeedKind kind, String type, String confidence, String reason) {
    }

    public record Settings(String modeMeta, boolean enableSelectivePta) {
        public Settings {
            modeMeta = modeMeta == null || modeMeta.isBlank() ? MODE_SEMANTIC_V1 : modeMeta;
        }

        public static Settings legacySemanticV1() {
            return new Settings(MODE_SEMANTIC_V1, true);
        }
    }

    static final class MethodLookup {
        private final Map<MethodReference.Handle, MethodReference.Handle> exact;
        private final Map<String, MethodReference.Handle> uniqueBySignature;
        private final Set<String> ambiguousSignatures;

        private MethodLookup(Map<MethodReference.Handle, MethodReference.Handle> exact,
                             Map<String, MethodReference.Handle> uniqueBySignature,
                             Set<String> ambiguousSignatures) {
            this.exact = exact == null ? Collections.emptyMap() : exact;
            this.uniqueBySignature = uniqueBySignature == null ? Collections.emptyMap() : uniqueBySignature;
            this.ambiguousSignatures = ambiguousSignatures == null ? Collections.emptySet() : ambiguousSignatures;
        }

        static MethodLookup build(Map<MethodReference.Handle, MethodReference> methodMap) {
            if (methodMap == null || methodMap.isEmpty()) {
                return new MethodLookup(Map.of(), Map.of(), Set.of());
            }
            Map<MethodReference.Handle, MethodReference.Handle> exact = new LinkedHashMap<>();
            Map<String, MethodReference.Handle> unique = new LinkedHashMap<>();
            Set<String> ambiguous = new HashSet<>();
            for (MethodReference method : methodMap.values()) {
                if (method == null || method.getClassReference() == null) {
                    continue;
                }
                MethodReference.Handle handle = method.getHandle();
                exact.put(handle, handle);
                String key = methodKey(
                        method.getClassReference().getName(),
                        method.getName(),
                        method.getDesc()
                );
                if (key.isEmpty() || ambiguous.contains(key)) {
                    continue;
                }
                MethodReference.Handle existing = unique.putIfAbsent(key, handle);
                if (existing != null && !existing.equals(handle)) {
                    unique.remove(key);
                    ambiguous.add(key);
                }
            }
            return new MethodLookup(
                    Collections.unmodifiableMap(exact),
                    Collections.unmodifiableMap(unique),
                    Collections.unmodifiableSet(new HashSet<>(ambiguous))
            );
        }

        MethodReference.Handle resolve(String className,
                                       String methodName,
                                       String desc,
                                       Integer preferredJarId) {
            String normalizedClass = normalizeClassName(className);
            String normalizedMethod = safe(methodName);
            String normalizedDesc = safe(desc);
            if (normalizedClass.isEmpty() || normalizedMethod.isEmpty() || normalizedDesc.isEmpty()) {
                return null;
            }
            int normalizedJarId = normalizeJarId(preferredJarId);
            if (normalizedJarId >= 0) {
                MethodReference.Handle preferred = new MethodReference.Handle(
                        new ClassReference.Handle(normalizedClass, normalizedJarId),
                        normalizedMethod,
                        normalizedDesc
                );
                MethodReference.Handle exactPreferred = exact.get(preferred);
                if (exactPreferred != null) {
                    return exactPreferred;
                }
            }
            MethodReference.Handle loose = exact.get(new MethodReference.Handle(
                    new ClassReference.Handle(normalizedClass, -1),
                    normalizedMethod,
                    normalizedDesc
            ));
            if (loose != null) {
                return loose;
            }
            String key = methodKey(normalizedClass, normalizedMethod, normalizedDesc);
            if (ambiguousSignatures.contains(key)) {
                return null;
            }
            return uniqueBySignature.get(key);
        }

        private static String methodKey(String className, String methodName, String desc) {
            String cls = normalizeClassName(className);
            String name = safe(methodName);
            String methodDesc = safe(desc);
            if (cls.isEmpty() || name.isEmpty() || methodDesc.isEmpty()) {
                return "";
            }
            return cls + "#" + name + methodDesc;
        }
    }

    private static String normalizeClassName(String className) {
        if (className == null) {
            return "";
        }
        String normalized = className.trim();
        if (normalized.isEmpty() || normalized.startsWith("[")) {
            return "";
        }
        if (normalized.startsWith("L") && normalized.endsWith(";")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (normalized.indexOf('.') >= 0) {
            normalized = normalized.replace('.', '/');
        }
        return normalized;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static int normalizeJarId(Integer jarId) {
        if (jarId == null || jarId < 0) {
            return -1;
        }
        return jarId;
    }
}
