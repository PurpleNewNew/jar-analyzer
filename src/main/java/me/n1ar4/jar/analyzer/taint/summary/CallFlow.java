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

import me.n1ar4.jar.analyzer.core.reference.MethodReference;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class CallFlow {
    private final FlowPort from;
    private final MethodReference.Handle callee;
    private final FlowPort to;
    private final Set<String> markers;
    private final String confidence;

    public CallFlow(FlowPort from,
                    MethodReference.Handle callee,
                    FlowPort to,
                    Set<String> markers,
                    String confidence) {
        this.from = from;
        this.callee = callee;
        this.to = to;
        this.markers = markers == null ? Collections.emptySet() : new HashSet<>(markers);
        this.confidence = confidence == null ? "low" : confidence;
    }

    public FlowPort getFrom() {
        return from;
    }

    public MethodReference.Handle getCallee() {
        return callee;
    }

    public FlowPort getTo() {
        return to;
    }

    public Set<String> getMarkers() {
        return Collections.unmodifiableSet(markers);
    }

    public String getConfidence() {
        return confidence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CallFlow)) {
            return false;
        }
        CallFlow callFlow = (CallFlow) o;
        return Objects.equals(from, callFlow.from)
                && Objects.equals(callee, callFlow.callee)
                && Objects.equals(to, callFlow.to)
                && Objects.equals(markers, callFlow.markers)
                && Objects.equals(confidence, callFlow.confidence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, callee, to, markers, confidence);
    }
}
