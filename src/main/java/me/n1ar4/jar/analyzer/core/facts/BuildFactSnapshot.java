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

import me.n1ar4.jar.analyzer.core.InheritanceMap;
import me.n1ar4.jar.analyzer.core.bytecode.BuildBytecodeWorkspace;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record BuildFactSnapshot(TypeFacts types,
                                MethodFacts methods,
                                SymbolFacts symbols,
                                SemanticFacts semantics,
                                BytecodeFacts bytecode,
                                ConstraintFacts constraints) {
    public BuildFactSnapshot {
        types = types == null ? TypeFacts.empty() : types;
        methods = methods == null ? MethodFacts.empty() : methods;
        symbols = symbols == null ? SymbolFacts.empty() : symbols;
        semantics = semantics == null ? SemanticFacts.empty() : semantics;
        bytecode = bytecode == null ? BytecodeFacts.empty() : bytecode;
        constraints = constraints == null ? ConstraintFacts.empty() : constraints;
    }

    public static BuildFactSnapshot empty() {
        return new BuildFactSnapshot(
                TypeFacts.empty(),
                MethodFacts.empty(),
                SymbolFacts.empty(),
                SemanticFacts.empty(),
                BytecodeFacts.empty(),
                ConstraintFacts.empty()
        );
    }

    public record TypeFacts(Map<ClassReference.Handle, ClassReference> classesByHandle,
                            InheritanceMap inheritanceMap,
                            Set<ClassReference.Handle> instantiatedClasses) {
        public TypeFacts {
            classesByHandle = immutableMapView(classesByHandle);
            inheritanceMap = inheritanceMap == null
                    ? InheritanceMap.fromClasses(Map.of())
                    : inheritanceMap;
            instantiatedClasses = immutableSetView(instantiatedClasses);
        }

        public static TypeFacts empty() {
            return new TypeFacts(Map.of(), InheritanceMap.fromClasses(Map.of()), Set.of());
        }
    }

    public record MethodFacts(Map<MethodReference.Handle, MethodReference> methodsByHandle) {
        public MethodFacts {
            methodsByHandle = immutableMapView(methodsByHandle);
        }

        public static MethodFacts empty() {
            return new MethodFacts(Map.of());
        }
    }

    public record SymbolFacts(List<CallSiteEntity> callSites,
                              Map<MethodReference.Handle, List<CallSiteEntity>> callSitesByCaller) {
        public SymbolFacts {
            callSites = immutableListView(callSites);
            callSitesByCaller = immutableMapView(callSitesByCaller);
        }

        public static SymbolFacts empty() {
            return new SymbolFacts(List.of(), Map.of());
        }
    }

    public record SemanticFacts(Map<MethodReference.Handle, Integer> explicitSourceMethodFlags,
                                Map<MethodReference.Handle, Integer> methodSemanticFlags) {
        public SemanticFacts {
            explicitSourceMethodFlags = immutableMapView(explicitSourceMethodFlags);
            methodSemanticFlags = immutableMapView(methodSemanticFlags);
        }

        public static SemanticFacts empty() {
            return new SemanticFacts(Map.of(), Map.of());
        }
    }

    public record BytecodeFacts(BuildBytecodeWorkspace workspace) {
        public BytecodeFacts {
            workspace = workspace == null ? BuildBytecodeWorkspace.empty() : workspace;
        }

        public static BytecodeFacts empty() {
            return new BytecodeFacts(BuildBytecodeWorkspace.empty());
        }
    }

    public record ConstraintFacts(Map<String, String> receiverVarByCallSiteKey,
                                  Map<String, AliasValueFact> instanceFieldFactsByKey,
                                  Map<MethodReference.Handle, MethodConstraintFacts> methodsByHandle) {
        public ConstraintFacts {
            receiverVarByCallSiteKey = immutableMapView(receiverVarByCallSiteKey);
            instanceFieldFactsByKey = immutableAliasFactMap(instanceFieldFactsByKey);
            methodsByHandle = immutableMapView(methodsByHandle);
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
            return immutableListView(values);
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
                out.put(entry.getKey(), immutableListView(entry.getValue()));
            }
            return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
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
            return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
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
            return immutableMapView(values);
        }

        private static <T> List<T> immutableList(List<T> values) {
            return immutableListView(values);
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
        return immutableMapView(values);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static <T> List<T> immutableListView(List<T> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(values);
    }

    private static <K, V> Map<K, V> immutableMapView(Map<K, V> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(values);
    }

    private static <T> Set<T> immutableSetView(Set<T> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(values);
    }
}
