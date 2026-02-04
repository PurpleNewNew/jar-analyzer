/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.taint.summary;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class MethodSummary {
    private final Set<FlowEdge> edges = new HashSet<>();
    private final Set<CallFlow> callFlows = new HashSet<>();
    private boolean hasSideEffect;
    private boolean unknown;

    public void addEdge(FlowEdge edge) {
        if (edge != null) {
            edges.add(edge);
        }
    }

    public Set<FlowEdge> getEdges() {
        return Collections.unmodifiableSet(edges);
    }

    public void addCallFlow(CallFlow flow) {
        if (flow != null) {
            callFlows.add(flow);
        }
    }

    public Set<CallFlow> getCallFlows() {
        return Collections.unmodifiableSet(callFlows);
    }

    public boolean hasSideEffect() {
        return hasSideEffect;
    }

    public void setHasSideEffect(boolean hasSideEffect) {
        this.hasSideEffect = hasSideEffect;
    }

    public boolean isUnknown() {
        return unknown;
    }

    public void setUnknown(boolean unknown) {
        this.unknown = unknown;
    }
}
