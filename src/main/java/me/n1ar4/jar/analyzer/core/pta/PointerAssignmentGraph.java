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

    Set<MethodReference.Handle> getCallers() {
        HashSet<MethodReference.Handle> out = new HashSet<>();
        out.addAll(invokeSites.keySet());
        out.addAll(syntheticCalls.keySet());
        return out;
    }
}
