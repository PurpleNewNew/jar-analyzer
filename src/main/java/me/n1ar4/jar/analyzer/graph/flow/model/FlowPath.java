/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.graph.flow.model;

import me.n1ar4.jar.analyzer.core.reference.MethodReference;

import java.util.List;

public class FlowPath {
    public static final int FROM_SOURCE_TO_SINK = FlowPathMode.FROM_SOURCE_TO_SINK.code();
    public static final int FROM_SINK_TO_SOURCE = FlowPathMode.FROM_SINK_TO_SOURCE.code();
    public static final int FROM_SOURCE_TO_ALL = FlowPathMode.FROM_SOURCE_TO_ALL.code();

    private List<MethodReference.Handle> methodList;
    private List<FlowPathEdge> edges;
    private int depth;
    private MethodReference.Handle source;
    private MethodReference.Handle sink;
    private int mode = FROM_SOURCE_TO_SINK;
    private boolean truncated;
    private String truncateReason;
    private String recommend;
    private int nodeCount;
    private int edgeCount;
    private int pathCount;
    private long elapsedMs;

    public List<MethodReference.Handle> getMethodList() {
        return methodList;
    }

    public void setMethodList(List<MethodReference.Handle> methodList) {
        this.methodList = methodList;
    }

    public List<FlowPathEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<FlowPathEdge> edges) {
        this.edges = edges;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public MethodReference.Handle getSource() {
        return source;
    }

    public void setSource(MethodReference.Handle source) {
        this.source = source;
    }

    public MethodReference.Handle getSink() {
        return sink;
    }

    public void setSink(MethodReference.Handle sink) {
        this.sink = sink;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = FlowPathMode.fromCode(mode).code();
    }

    public FlowPathMode modeEnum() {
        return FlowPathMode.fromCode(mode);
    }

    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    public String getTruncateReason() {
        return truncateReason;
    }

    public void setTruncateReason(String truncateReason) {
        this.truncateReason = truncateReason;
    }

    public String getRecommend() {
        return recommend;
    }

    public void setRecommend(String recommend) {
        this.recommend = recommend;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(int nodeCount) {
        this.nodeCount = nodeCount;
    }

    public int getEdgeCount() {
        return edgeCount;
    }

    public void setEdgeCount(int edgeCount) {
        this.edgeCount = edgeCount;
    }

    public int getPathCount() {
        return pathCount;
    }

    public void setPathCount(int pathCount) {
        this.pathCount = pathCount;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    @Override
    public String toString() {
        return "FlowPath{" +
                "methodList=" + methodList +
                ", edges=" + edges +
                ", depth=" + depth +
                ", source=" + source +
                ", sink=" + sink +
                ", mode=" + mode +
                '}';
    }
}
