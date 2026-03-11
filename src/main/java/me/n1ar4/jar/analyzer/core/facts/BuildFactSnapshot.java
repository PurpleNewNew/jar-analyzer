/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.facts;

import me.n1ar4.jar.analyzer.analyze.spring.SpringController;
import me.n1ar4.jar.analyzer.core.InheritanceMap;
import me.n1ar4.jar.analyzer.core.bytecode.BuildBytecodeWorkspace;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.project.ProjectOrigin;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.LocalVarEntity;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record BuildFactSnapshot(ArchiveFacts archives,
                                TypeFacts types,
                                MethodFacts methods,
                                SymbolFacts symbols,
                                ResourceFacts resources,
                                SemanticFacts semantics,
                                BytecodeFacts bytecode,
                                ConstraintFacts constraints) {
    public BuildFactSnapshot {
        archives = archives == null ? ArchiveFacts.empty() : archives;
        types = types == null ? TypeFacts.empty() : types;
        methods = methods == null ? MethodFacts.empty() : methods;
        symbols = symbols == null ? SymbolFacts.empty() : symbols;
        resources = resources == null ? ResourceFacts.empty() : resources;
        semantics = semantics == null ? SemanticFacts.empty() : semantics;
        bytecode = bytecode == null ? BytecodeFacts.empty() : bytecode;
        constraints = constraints == null ? ConstraintFacts.empty() : constraints;
    }

    public static BuildFactSnapshot empty() {
        return new BuildFactSnapshot(
                ArchiveFacts.empty(),
                TypeFacts.empty(),
                MethodFacts.empty(),
                SymbolFacts.empty(),
                ResourceFacts.empty(),
                SemanticFacts.empty(),
                BytecodeFacts.empty(),
                ConstraintFacts.empty()
        );
    }

    public record ArchiveFacts(List<String> allArchives,
                               int targetArchiveCount,
                               int libraryArchiveCount,
                               int sdkEntryCount,
                               Map<Integer, ProjectOrigin> jarOriginsById,
                               boolean projectMode) {
        public ArchiveFacts {
            allArchives = allArchives == null ? List.of() : List.copyOf(allArchives);
            jarOriginsById = jarOriginsById == null || jarOriginsById.isEmpty()
                    ? Map.of()
                    : Map.copyOf(jarOriginsById);
            targetArchiveCount = Math.max(0, targetArchiveCount);
            libraryArchiveCount = Math.max(0, libraryArchiveCount);
            sdkEntryCount = Math.max(0, sdkEntryCount);
        }

        public static ArchiveFacts empty() {
            return new ArchiveFacts(List.of(), 0, 0, 0, Map.of(), false);
        }
    }

    public record TypeFacts(Map<ClassReference.Handle, ClassReference> classesByHandle,
                            InheritanceMap inheritanceMap,
                            Set<ClassReference.Handle> instantiatedClasses) {
        public TypeFacts {
            classesByHandle = classesByHandle == null || classesByHandle.isEmpty()
                    ? Map.of()
                    : Map.copyOf(classesByHandle);
            inheritanceMap = inheritanceMap == null
                    ? InheritanceMap.fromClasses(Map.of())
                    : inheritanceMap;
            instantiatedClasses = instantiatedClasses == null || instantiatedClasses.isEmpty()
                    ? Set.of()
                    : Set.copyOf(instantiatedClasses);
        }

        public static TypeFacts empty() {
            return new TypeFacts(Map.of(), InheritanceMap.fromClasses(Map.of()), Set.of());
        }
    }

    public record MethodFacts(Map<MethodReference.Handle, MethodReference> methodsByHandle,
                              Map<ClassReference.Handle, List<MethodReference.Handle>> methodsByClass) {
        public MethodFacts {
            methodsByHandle = methodsByHandle == null || methodsByHandle.isEmpty()
                    ? Map.of()
                    : Map.copyOf(methodsByHandle);
            methodsByClass = methodsByClass == null || methodsByClass.isEmpty()
                    ? Map.of()
                    : Map.copyOf(methodsByClass);
        }

        public static MethodFacts empty() {
            return new MethodFacts(Map.of(), Map.of());
        }
    }

    public record SymbolFacts(List<CallSiteEntity> callSites,
                              Map<MethodReference.Handle, List<CallSiteEntity>> callSitesByCaller,
                              Map<String, CallSiteEntity> callSitesByKey,
                              List<LocalVarEntity> localVars,
                              Map<MethodReference.Handle, List<LocalVarEntity>> localVarsByMethod,
                              Map<MethodReference.Handle, List<String>> stringLiteralsByMethod,
                              Map<MethodReference.Handle, List<String>> annotationStringsByMethod) {
        public SymbolFacts {
            callSites = callSites == null ? List.of() : List.copyOf(callSites);
            callSitesByCaller = callSitesByCaller == null || callSitesByCaller.isEmpty()
                    ? Map.of()
                    : Map.copyOf(callSitesByCaller);
            callSitesByKey = callSitesByKey == null || callSitesByKey.isEmpty()
                    ? Map.of()
                    : Map.copyOf(callSitesByKey);
            localVars = localVars == null ? List.of() : List.copyOf(localVars);
            localVarsByMethod = localVarsByMethod == null || localVarsByMethod.isEmpty()
                    ? Map.of()
                    : Map.copyOf(localVarsByMethod);
            stringLiteralsByMethod = stringLiteralsByMethod == null || stringLiteralsByMethod.isEmpty()
                    ? Map.of()
                    : Map.copyOf(stringLiteralsByMethod);
            annotationStringsByMethod = annotationStringsByMethod == null || annotationStringsByMethod.isEmpty()
                    ? Map.of()
                    : Map.copyOf(annotationStringsByMethod);
        }

        public static SymbolFacts empty() {
            return new SymbolFacts(List.of(), Map.of(), Map.of(), List.of(), Map.of(), Map.of(), Map.of());
        }
    }

    public record ResourceFacts(List<ResourceEntity> resources) {
        public ResourceFacts {
            resources = resources == null ? List.of() : List.copyOf(resources);
        }

        public static ResourceFacts empty() {
            return new ResourceFacts(List.of());
        }
    }

    public record SemanticFacts(List<SpringController> controllers,
                                List<String> interceptors,
                                List<String> servlets,
                                List<String> filters,
                                List<String> listeners,
                                Map<MethodReference.Handle, Integer> explicitSourceMethodFlags,
                                Map<MethodReference.Handle, Integer> methodSemanticFlags) {
        public SemanticFacts {
            controllers = controllers == null ? List.of() : List.copyOf(controllers);
            interceptors = interceptors == null ? List.of() : List.copyOf(interceptors);
            servlets = servlets == null ? List.of() : List.copyOf(servlets);
            filters = filters == null ? List.of() : List.copyOf(filters);
            listeners = listeners == null ? List.of() : List.copyOf(listeners);
            explicitSourceMethodFlags = explicitSourceMethodFlags == null || explicitSourceMethodFlags.isEmpty()
                    ? Map.of()
                    : Map.copyOf(explicitSourceMethodFlags);
            methodSemanticFlags = methodSemanticFlags == null || methodSemanticFlags.isEmpty()
                    ? Map.of()
                    : Map.copyOf(methodSemanticFlags);
        }

        public static SemanticFacts empty() {
            return new SemanticFacts(List.of(), List.of(), List.of(), List.of(), List.of(), Map.of(), Map.of());
        }
    }

    public record BytecodeFacts(Set<ClassFileEntity> classFiles,
                                BuildBytecodeWorkspace workspace) {
        public BytecodeFacts {
            classFiles = classFiles == null || classFiles.isEmpty()
                    ? Set.of()
                    : Set.copyOf(classFiles);
            workspace = workspace == null ? BuildBytecodeWorkspace.empty() : workspace;
        }

        public static BytecodeFacts empty() {
            return new BytecodeFacts(Set.of(), BuildBytecodeWorkspace.empty());
        }
    }

    public record ConstraintFacts(Map<String, String> receiverVarByCallSiteKey,
                                  Map<String, AliasValueFact> instanceFieldFactsByKey,
                                  Map<MethodReference.Handle, MethodConstraintFacts> methodsByHandle) {
        public ConstraintFacts {
            receiverVarByCallSiteKey = receiverVarByCallSiteKey == null || receiverVarByCallSiteKey.isEmpty()
                    ? Map.of()
                    : Map.copyOf(receiverVarByCallSiteKey);
            instanceFieldFactsByKey = immutableAliasFactMap(instanceFieldFactsByKey);
            methodsByHandle = methodsByHandle == null || methodsByHandle.isEmpty()
                    ? Map.of()
                    : Map.copyOf(methodsByHandle);
        }

        public AliasValueFact instanceFieldFact(String owner,
                                                String name) {
            if (instanceFieldFactsByKey.isEmpty()) {
                return AliasValueFact.empty();
            }
            AliasValueFact facts = instanceFieldFactsByKey.get(safe(owner) + "#" + safe(name));
            return facts == null ? AliasValueFact.empty() : facts;
        }

        public MethodConstraintFacts methodConstraints(MethodReference.Handle handle) {
            if (handle == null || methodsByHandle.isEmpty()) {
                return MethodConstraintFacts.empty();
            }
            MethodConstraintFacts facts = methodsByHandle.get(handle);
            return facts == null ? MethodConstraintFacts.empty() : facts;
        }

        public static ConstraintFacts empty() {
            return new ConstraintFacts(Map.of(), Map.of(), Map.of());
        }
    }

    public record MethodConstraintFacts(List<AllocEdge> allocEdges,
                                        Map<Integer, List<AssignEdge>> objectAssignEdgesByVar,
                                        Map<Integer, List<AssignEdge>> numericAssignEdgesByVar,
                                        Map<String, List<FieldEdge>> fieldLoadEdgesByFieldKey,
                                        Map<String, List<FieldEdge>> fieldStoreEdgesByFieldKey,
                                        List<ArrayAccessEdge> arrayLoadEdges,
                                        List<ArrayAccessEdge> arrayStoreEdges,
                                        Map<Integer, Map<Integer, AliasValueFact>> arrayElementFactsByVar,
                                        List<ArrayCopyEdge> arrayCopyEdges,
                                        List<ReturnEdge> returnEdges,
                                        List<NativeModelHint> nativeModelHints,
                                        MethodReflectionHints reflectionHints) {
        public MethodConstraintFacts {
            allocEdges = immutableList(allocEdges);
            objectAssignEdgesByVar = immutableMapOfLists(objectAssignEdgesByVar);
            numericAssignEdgesByVar = immutableMapOfLists(numericAssignEdgesByVar);
            fieldLoadEdgesByFieldKey = immutableMapOfLists(fieldLoadEdgesByFieldKey);
            fieldStoreEdgesByFieldKey = immutableMapOfLists(fieldStoreEdgesByFieldKey);
            arrayLoadEdges = immutableList(arrayLoadEdges);
            arrayStoreEdges = immutableList(arrayStoreEdges);
            arrayElementFactsByVar = immutableNestedAliasFactMap(arrayElementFactsByVar);
            arrayCopyEdges = immutableList(arrayCopyEdges);
            returnEdges = immutableList(returnEdges);
            nativeModelHints = immutableList(nativeModelHints);
            reflectionHints = reflectionHints == null ? MethodReflectionHints.empty() : reflectionHints;
        }

        public AssignEdge findPreviousObjectAssign(int varIndex,
                                                  int limitIndex,
                                                  int floorIndex) {
            return findPreviousAssign(objectAssignEdgesByVar.get(varIndex), limitIndex, floorIndex);
        }

        public AssignEdge findPreviousNumericAssign(int varIndex,
                                                   int limitIndex,
                                                   int floorIndex) {
            return findPreviousAssign(numericAssignEdgesByVar.get(varIndex), limitIndex, floorIndex);
        }

        public List<FieldEdge> fieldStoreEdges(String owner,
                                               String name,
                                               String desc) {
            return fieldStoreEdgesByFieldKey.getOrDefault(FieldEdge.key(owner, name, desc), List.of());
        }

        public List<FieldEdge> fieldLoadEdges(String owner,
                                              String name,
                                              String desc) {
            return fieldLoadEdgesByFieldKey.getOrDefault(FieldEdge.key(owner, name, desc), List.of());
        }

        public AliasValueFact arrayElementFact(int arrayVar,
                                               int elementIndex) {
            if (arrayElementFactsByVar.isEmpty()) {
                return AliasValueFact.empty();
            }
            Map<Integer, AliasValueFact> elements = arrayElementFactsByVar.get(arrayVar);
            if (elements == null || elements.isEmpty()) {
                return AliasValueFact.empty();
            }
            AliasValueFact facts = elements.get(elementIndex);
            return facts == null ? AliasValueFact.empty() : facts;
        }

        public static MethodConstraintFacts empty() {
            return new MethodConstraintFacts(
                    List.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    List.of(),
                    List.of(),
                    Map.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    MethodReflectionHints.empty()
            );
        }

        private static AssignEdge findPreviousAssign(List<AssignEdge> edges,
                                                     int limitIndex,
                                                     int floorIndex) {
            if (edges == null || edges.isEmpty()) {
                return null;
            }
            for (AssignEdge edge : edges) {
                if (edge == null) {
                    continue;
                }
                if (edge.insnIndex() < limitIndex && edge.insnIndex() >= floorIndex) {
                    return edge;
                }
            }
            return null;
        }

        private static <T> List<T> immutableList(List<T> values) {
            return values == null || values.isEmpty() ? List.of() : List.copyOf(values);
        }

        private static <K, V> Map<K, List<V>> immutableMapOfLists(Map<K, List<V>> values) {
            if (values == null || values.isEmpty()) {
                return Map.of();
            }
            java.util.LinkedHashMap<K, List<V>> out = new java.util.LinkedHashMap<>();
            for (Map.Entry<K, List<V>> entry : values.entrySet()) {
                if (entry == null || entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                    continue;
                }
                out.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            return out.isEmpty() ? Map.of() : Map.copyOf(out);
        }

        private static Map<Integer, Map<Integer, AliasValueFact>> immutableNestedAliasFactMap(
                Map<Integer, Map<Integer, AliasValueFact>> values) {
            if (values == null || values.isEmpty()) {
                return Map.of();
            }
            java.util.LinkedHashMap<Integer, Map<Integer, AliasValueFact>> out = new java.util.LinkedHashMap<>();
            for (Map.Entry<Integer, Map<Integer, AliasValueFact>> entry : values.entrySet()) {
                if (entry == null || entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                    continue;
                }
                Map<Integer, AliasValueFact> nested = immutableAliasFactMap(entry.getValue());
                if (!nested.isEmpty()) {
                    out.put(entry.getKey(), nested);
                }
            }
            return out.isEmpty() ? Map.of() : Map.copyOf(out);
        }
    }

    public record AliasValueFact(Set<String> stringValues,
                                 Set<String> objectTypes) {
        public AliasValueFact {
            stringValues = immutableSet(stringValues);
            objectTypes = immutableSet(objectTypes);
        }

        public boolean isEmpty() {
            return stringValues.isEmpty() && objectTypes.isEmpty();
        }

        public String uniqueStringValue() {
            return uniqueValue(stringValues);
        }

        public String uniqueObjectType() {
            return uniqueValue(objectTypes);
        }

        public static AliasValueFact empty() {
            return new AliasValueFact(Set.of(), Set.of());
        }

        private static Set<String> immutableSet(Set<String> values) {
            if (values == null || values.isEmpty()) {
                return Set.of();
            }
            java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
            for (String value : values) {
                String normalized = safe(value);
                if (!normalized.isBlank()) {
                    out.add(normalized);
                }
            }
            return out.isEmpty() ? Set.of() : Set.copyOf(out);
        }

        private static String uniqueValue(Set<String> values) {
            if (values == null || values.size() != 1) {
                return null;
            }
            String value = values.iterator().next();
            return value == null || value.isBlank() ? null : value;
        }
    }

    public record MethodReflectionHints(Map<Integer, MethodInvokeHint> classNewInstanceHints,
                                        Map<Integer, MethodInvokeHint> reflectionInvokeHints,
                                        Map<Integer, MethodInvokeHint> methodHandleInvokeHints,
                                        List<ReflectionHintDiagnostic> diagnostics) {
        public MethodReflectionHints {
            classNewInstanceHints = immutableMap(classNewInstanceHints);
            reflectionInvokeHints = immutableMap(reflectionInvokeHints);
            methodHandleInvokeHints = immutableMap(methodHandleInvokeHints);
            diagnostics = immutableList(diagnostics);
        }

        public MethodInvokeHint classNewInstanceHint(int insnIndex) {
            return classNewInstanceHints.get(insnIndex);
        }

        public MethodInvokeHint reflectionInvokeHint(int insnIndex) {
            return reflectionInvokeHints.get(insnIndex);
        }

        public MethodInvokeHint methodHandleInvokeHint(int insnIndex) {
            return methodHandleInvokeHints.get(insnIndex);
        }

        public boolean isEmpty() {
            return classNewInstanceHints.isEmpty()
                    && reflectionInvokeHints.isEmpty()
                    && methodHandleInvokeHints.isEmpty()
                    && diagnostics.isEmpty();
        }

        public static MethodReflectionHints empty() {
            return new MethodReflectionHints(Map.of(), Map.of(), Map.of(), List.of());
        }

        private static <K, V> Map<K, V> immutableMap(Map<K, V> values) {
            if (values == null || values.isEmpty()) {
                return Map.of();
            }
            java.util.LinkedHashMap<K, V> out = new java.util.LinkedHashMap<>();
            for (Map.Entry<K, V> entry : values.entrySet()) {
                if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                out.put(entry.getKey(), entry.getValue());
            }
            return out.isEmpty() ? Map.of() : Map.copyOf(out);
        }

        private static <T> List<T> immutableList(List<T> values) {
            return values == null || values.isEmpty() ? List.of() : List.copyOf(values);
        }
    }

    public record MethodInvokeHint(int insnIndex,
                                   List<MethodReference.Handle> targets,
                                   String reason,
                                   ReflectionHintTier tier,
                                   boolean imprecise) {
        public MethodInvokeHint {
            targets = targets == null || targets.isEmpty() ? List.of() : List.copyOf(targets);
            reason = reason == null ? "" : reason;
            tier = tier == null ? ReflectionHintTier.fromReason(reason) : tier;
        }
    }

    public record ReflectionHintDiagnostic(int insnIndex,
                                           String kind,
                                           ReflectionHintTier tier,
                                           String reason,
                                           int candidateCount,
                                           boolean thresholdExceeded) {
        public ReflectionHintDiagnostic {
            kind = kind == null ? "" : kind;
            tier = tier == null ? ReflectionHintTier.fromReason(reason) : tier;
            reason = reason == null ? "" : reason;
            candidateCount = Math.max(0, candidateCount);
        }
    }

    public enum ReflectionHintTier {
        CONST,
        LOG,
        CAST,
        UNKNOWN;

        public static ReflectionHintTier fromReason(String reason) {
            String normalized = reason == null ? "" : reason.trim().toLowerCase(java.util.Locale.ROOT);
            if (normalized.isEmpty()) {
                return UNKNOWN;
            }
            if (normalized.contains("cast")) {
                return CAST;
            }
            if (normalized.contains("log") || normalized.contains("tamiflex")) {
                return LOG;
            }
            if ("unknown".equals(normalized)) {
                return UNKNOWN;
            }
            return CONST;
        }
    }

    public record AllocEdge(int insnIndex,
                            String kind,
                            String typeName) {
    }

    public record AssignEdge(int insnIndex,
                             int varIndex,
                             int opcode) {
    }

    public record FieldEdge(int insnIndex,
                            String owner,
                            String name,
                            String desc) {
        public static String key(String owner, String name, String desc) {
            return safe(owner) + "|" + safe(name) + "|" + safe(desc);
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }
    }

    public record ArrayAccessEdge(int insnIndex) {
    }

    public record ArrayCopyEdge(int insnIndex) {
    }

    public record ReturnEdge(int insnIndex) {
    }

    public record NativeModelHint(int insnIndex,
                                  String kind,
                                  String owner,
                                  String name,
                                  String desc) {
    }

    private static <K> Map<K, AliasValueFact> immutableAliasFactMap(Map<K, AliasValueFact> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        java.util.LinkedHashMap<K, AliasValueFact> out = new java.util.LinkedHashMap<>();
        for (Map.Entry<K, AliasValueFact> entry : values.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            out.put(entry.getKey(), entry.getValue());
        }
        return out.isEmpty() ? Map.of() : Map.copyOf(out);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
