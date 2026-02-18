/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.pta;

import me.n1ar4.jar.analyzer.core.reference.MethodReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class PointerAssignmentGraph {
    private final Map<PtaAllocNode, Set<PtaVarNode>> allocEdges = new HashMap<>();
    private final Map<PtaVarNode, Set<PtaVarNode>> assignEdges = new HashMap<>();
    private final Map<MethodReference.Handle, List<PtaInvokeSite>> invokeSites = new HashMap<>();
    private final Map<MethodReference.Handle, List<PtaSyntheticCall>> syntheticCalls = new HashMap<>();
    private final List<FieldStoreEdge> fieldStoreEdges = new ArrayList<>();
    private final List<FieldLoadEdge> fieldLoadEdges = new ArrayList<>();
    private final List<ArrayStoreEdge> arrayStoreEdges = new ArrayList<>();
    private final List<ArrayLoadEdge> arrayLoadEdges = new ArrayList<>();
    private final List<ArrayCopyEdge> arrayCopyEdges = new ArrayList<>();

    void addAllocEdge(PtaAllocNode from, PtaVarNode to) {
        if (from == null || to == null) {
            return;
        }
        allocEdges.computeIfAbsent(from, k -> new HashSet<>()).add(to);
    }

    void addAssignEdge(PtaVarNode from, PtaVarNode to) {
        if (from == null || to == null) {
            return;
        }
        assignEdges.computeIfAbsent(from, k -> new HashSet<>()).add(to);
    }

    void addInvokeSite(MethodReference.Handle caller, PtaInvokeSite site) {
        if (caller == null || site == null) {
            return;
        }
        invokeSites.computeIfAbsent(caller, k -> new ArrayList<>()).add(site);
    }

    void addSyntheticCall(MethodReference.Handle caller, PtaSyntheticCall call) {
        if (caller == null || call == null) {
            return;
        }
        syntheticCalls.computeIfAbsent(caller, k -> new ArrayList<>()).add(call);
    }

    void addFieldStoreEdge(FieldStoreEdge edge) {
        if (edge == null || edge.getValueVar() == null || edge.getFieldSig() == null) {
            return;
        }
        fieldStoreEdges.add(edge);
    }

    void addFieldLoadEdge(FieldLoadEdge edge) {
        if (edge == null || edge.getToVar() == null || edge.getFieldSig() == null) {
            return;
        }
        fieldLoadEdges.add(edge);
    }

    void addArrayStoreEdge(ArrayStoreEdge edge) {
        if (edge == null || edge.getArrayVar() == null || edge.getValueVar() == null) {
            return;
        }
        arrayStoreEdges.add(edge);
    }

    void addArrayLoadEdge(ArrayLoadEdge edge) {
        if (edge == null || edge.getArrayVar() == null || edge.getToVar() == null) {
            return;
        }
        arrayLoadEdges.add(edge);
    }

    void addArrayCopyEdge(ArrayCopyEdge edge) {
        if (edge == null || edge.getSrcArrayVar() == null || edge.getDstArrayVar() == null) {
            return;
        }
        arrayCopyEdges.add(edge);
    }

    Map<PtaAllocNode, Set<PtaVarNode>> getAllocEdges() {
        return allocEdges;
    }

    Set<PtaVarNode> getAssignTargets(PtaVarNode from) {
        Set<PtaVarNode> targets = assignEdges.get(from);
        if (targets == null || targets.isEmpty()) {
            return Collections.emptySet();
        }
        return targets;
    }

    List<PtaInvokeSite> getInvokeSites(MethodReference.Handle caller) {
        List<PtaInvokeSite> list = invokeSites.get(caller);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list;
    }

    List<PtaSyntheticCall> getSyntheticCalls(MethodReference.Handle caller) {
        List<PtaSyntheticCall> list = syntheticCalls.get(caller);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list;
    }

    List<FieldStoreEdge> getFieldStoreEdges() {
        if (fieldStoreEdges.isEmpty()) {
            return Collections.emptyList();
        }
        return fieldStoreEdges;
    }

    List<FieldLoadEdge> getFieldLoadEdges() {
        if (fieldLoadEdges.isEmpty()) {
            return Collections.emptyList();
        }
        return fieldLoadEdges;
    }

    List<ArrayStoreEdge> getArrayStoreEdges() {
        if (arrayStoreEdges.isEmpty()) {
            return Collections.emptyList();
        }
        return arrayStoreEdges;
    }

    List<ArrayLoadEdge> getArrayLoadEdges() {
        if (arrayLoadEdges.isEmpty()) {
            return Collections.emptyList();
        }
        return arrayLoadEdges;
    }

    List<ArrayCopyEdge> getArrayCopyEdges() {
        if (arrayCopyEdges.isEmpty()) {
            return Collections.emptyList();
        }
        return arrayCopyEdges;
    }

    Set<MethodReference.Handle> getCallers() {
        HashSet<MethodReference.Handle> out = new HashSet<>();
        out.addAll(invokeSites.keySet());
        out.addAll(syntheticCalls.keySet());
        return out;
    }

    static final class FieldStoreEdge {
        private final PtaVarNode baseVar;
        private final PtaVarNode valueVar;
        private final String fieldSig;
        private final boolean isStatic;

        FieldStoreEdge(PtaVarNode baseVar, PtaVarNode valueVar, String fieldSig, boolean isStatic) {
            this.baseVar = baseVar;
            this.valueVar = valueVar;
            this.fieldSig = fieldSig;
            this.isStatic = isStatic;
        }

        PtaVarNode getBaseVar() {
            return baseVar;
        }

        PtaVarNode getValueVar() {
            return valueVar;
        }

        String getFieldSig() {
            return fieldSig;
        }

        boolean isStatic() {
            return isStatic;
        }
    }

    static final class FieldLoadEdge {
        private final PtaVarNode baseVar;
        private final PtaVarNode toVar;
        private final String fieldSig;
        private final boolean isStatic;

        FieldLoadEdge(PtaVarNode baseVar, PtaVarNode toVar, String fieldSig, boolean isStatic) {
            this.baseVar = baseVar;
            this.toVar = toVar;
            this.fieldSig = fieldSig;
            this.isStatic = isStatic;
        }

        PtaVarNode getBaseVar() {
            return baseVar;
        }

        PtaVarNode getToVar() {
            return toVar;
        }

        String getFieldSig() {
            return fieldSig;
        }

        boolean isStatic() {
            return isStatic;
        }
    }

    static final class ArrayStoreEdge {
        private final PtaVarNode arrayVar;
        private final PtaVarNode valueVar;
        private final Integer index;

        ArrayStoreEdge(PtaVarNode arrayVar, PtaVarNode valueVar, Integer index) {
            this.arrayVar = arrayVar;
            this.valueVar = valueVar;
            this.index = index;
        }

        PtaVarNode getArrayVar() {
            return arrayVar;
        }

        PtaVarNode getValueVar() {
            return valueVar;
        }

        Integer getIndex() {
            return index;
        }
    }

    static final class ArrayLoadEdge {
        private final PtaVarNode arrayVar;
        private final PtaVarNode toVar;
        private final Integer index;

        ArrayLoadEdge(PtaVarNode arrayVar, PtaVarNode toVar, Integer index) {
            this.arrayVar = arrayVar;
            this.toVar = toVar;
            this.index = index;
        }

        PtaVarNode getArrayVar() {
            return arrayVar;
        }

        PtaVarNode getToVar() {
            return toVar;
        }

        Integer getIndex() {
            return index;
        }
    }

    static final class ArrayCopyEdge {
        private final PtaVarNode srcArrayVar;
        private final PtaVarNode dstArrayVar;

        ArrayCopyEdge(PtaVarNode srcArrayVar, PtaVarNode dstArrayVar) {
            this.srcArrayVar = srcArrayVar;
            this.dstArrayVar = dstArrayVar;
        }

        PtaVarNode getSrcArrayVar() {
            return srcArrayVar;
        }

        PtaVarNode getDstArrayVar() {
            return dstArrayVar;
        }
    }
}
