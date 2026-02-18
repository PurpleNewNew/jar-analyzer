/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.graph.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GraphSnapshot {
    private final Map<Long, GraphNode> nodeMap;
    private final Map<Long, List<GraphEdge>> outgoing;
    private final Map<Long, List<GraphEdge>> incoming;
    private final Map<String, List<GraphNode>> labelIndex;

    GraphSnapshot(Map<Long, GraphNode> nodeMap,
                  Map<Long, List<GraphEdge>> outgoing,
                  Map<Long, List<GraphEdge>> incoming,
                  Map<String, List<GraphNode>> labelIndex) {
        this.nodeMap = nodeMap == null ? Collections.emptyMap() : Collections.unmodifiableMap(nodeMap);
        this.outgoing = freezeEdgeIndex(outgoing);
        this.incoming = freezeEdgeIndex(incoming);
        this.labelIndex = freezeNodeIndex(labelIndex);
    }

    public GraphNode getNode(long nodeId) {
        return nodeMap.get(nodeId);
    }

    public List<GraphNode> getNodesByLabel(String label) {
        if (label == null || label.isBlank()) {
            return new ArrayList<>(nodeMap.values());
        }
        List<GraphNode> data = labelIndex.get(label.toLowerCase());
        if (data == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(data);
    }

    public List<GraphEdge> getOutgoing(long nodeId) {
        List<GraphEdge> edges = outgoing.get(nodeId);
        if (edges == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(edges);
    }

    public List<GraphEdge> getIncoming(long nodeId) {
        List<GraphEdge> edges = incoming.get(nodeId);
        if (edges == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(edges);
    }

    public int getNodeCount() {
        return nodeMap.size();
    }

    private static Map<Long, List<GraphEdge>> freezeEdgeIndex(Map<Long, List<GraphEdge>> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<GraphEdge>> out = new HashMap<>(source.size() * 2);
        for (Map.Entry<Long, List<GraphEdge>> entry : source.entrySet()) {
            out.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }
        return Collections.unmodifiableMap(out);
    }

    private static Map<String, List<GraphNode>> freezeNodeIndex(Map<String, List<GraphNode>> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<GraphNode>> out = new HashMap<>(source.size() * 2);
        for (Map.Entry<String, List<GraphNode>> entry : source.entrySet()) {
            out.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }
        return Collections.unmodifiableMap(out);
    }
}
