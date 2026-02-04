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
import java.util.Objects;
import java.util.Set;

public final class FlowEdge {
    private final FlowPort from;
    private final FlowPort to;
    private final Set<String> markers;
    private final String confidence;

    public FlowEdge(FlowPort from, FlowPort to, Set<String> markers, String confidence) {
        this.from = from;
        this.to = to;
        this.markers = markers == null ? Collections.emptySet() : new HashSet<>(markers);
        this.confidence = confidence == null ? "low" : confidence;
    }

    public FlowPort getFrom() {
        return from;
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
        if (!(o instanceof FlowEdge)) {
            return false;
        }
        FlowEdge flowEdge = (FlowEdge) o;
        return Objects.equals(from, flowEdge.from)
                && Objects.equals(to, flowEdge.to)
                && Objects.equals(markers, flowEdge.markers)
                && Objects.equals(confidence, flowEdge.confidence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, markers, confidence);
    }
}
