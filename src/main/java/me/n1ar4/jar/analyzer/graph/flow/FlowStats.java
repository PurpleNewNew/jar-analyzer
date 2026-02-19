/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.flow;

public final class FlowStats {
    private final int nodeCount;
    private final int edgeCount;
    private final int pathCount;
    private final long elapsedMs;
    private final FlowTruncation truncation;

    public FlowStats(int nodeCount,
                     int edgeCount,
                     int pathCount,
                     long elapsedMs,
                     FlowTruncation truncation) {
        this.nodeCount = Math.max(0, nodeCount);
        this.edgeCount = Math.max(0, edgeCount);
        this.pathCount = Math.max(0, pathCount);
        this.elapsedMs = Math.max(0L, elapsedMs);
        this.truncation = truncation == null ? FlowTruncation.none() : truncation;
    }

    public static FlowStats empty() {
        return new FlowStats(0, 0, 0, 0L, FlowTruncation.none());
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public int getEdgeCount() {
        return edgeCount;
    }

    public int getPathCount() {
        return pathCount;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public FlowTruncation getTruncation() {
        return truncation;
    }
}
