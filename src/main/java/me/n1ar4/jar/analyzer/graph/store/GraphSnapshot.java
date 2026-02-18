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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;

public final class GraphSnapshot {
    private final long buildSeq;
    private final Map<Long, GraphNode> nodeMap;
    private final List<GraphNode> allNodes;

    private final Map<Long, int[]> outgoingAdj;
    private final Map<Long, int[]> incomingAdj;
    private final Map<Long, List<GraphEdge>> outgoingViews;
    private final Map<Long, List<GraphEdge>> incomingViews;

    private final long[] edgeIds;
    private final long[] srcIds;
    private final long[] dstIds;
    private final String[] relTypes;
    private final String[] confidences;
    private final String[] evidences;
    private final int[] opCodes;
    private final GraphEdge[] edgeCache;

    private final Map<String, List<GraphNode>> labelIndex;
    private final Map<String, List<GraphNode>> kindIndex;
    private final Map<String, List<GraphNode>> classNameIndex;
    private final Map<String, List<GraphNode>> methodNameIndex;
    private final Map<String, List<GraphNode>> methodSignatureIndex;
    private final Map<String, Long> methodScopedIndex;
    private final Map<String, Long> methodLooseIndex;

    GraphSnapshot(Map<Long, GraphNode> nodeMap,
                  Map<Long, List<GraphEdge>> outgoing,
                  Map<Long, List<GraphEdge>> incoming,
                  Map<String, List<GraphNode>> labelIndex) {
        this(-1L, nodeMap, outgoing, incoming, labelIndex);
    }

    GraphSnapshot(long buildSeq,
                  Map<Long, GraphNode> nodeMap,
                  Map<Long, List<GraphEdge>> outgoing,
                  Map<Long, List<GraphEdge>> incoming,
                  Map<String, List<GraphNode>> labelIndex) {
        this(buildSeq, nodeMap, labelIndex, encodeEdges(outgoing, incoming));
    }

    private GraphSnapshot(long buildSeq,
                          Map<Long, GraphNode> nodeMap,
                          Map<String, List<GraphNode>> labelIndex,
                          EncodedEdges encoded) {
        this.buildSeq = buildSeq;
        this.nodeMap = nodeMap == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(nodeMap));
        this.allNodes = Collections.unmodifiableList(new ArrayList<>(this.nodeMap.values()));

        this.edgeIds = encoded.edgeIds;
        this.srcIds = encoded.srcIds;
        this.dstIds = encoded.dstIds;
        this.relTypes = encoded.relTypes;
        this.confidences = encoded.confidences;
        this.evidences = encoded.evidences;
        this.opCodes = encoded.opCodes;
        this.edgeCache = new GraphEdge[edgeIds.length];

        this.outgoingAdj = freezeAdjacency(encoded.outgoingAdj);
        this.incomingAdj = freezeAdjacency(encoded.incomingAdj);
        this.outgoingViews = buildEdgeViews(this.outgoingAdj);
        this.incomingViews = buildEdgeViews(this.incomingAdj);

        this.labelIndex = freezeNodeIndex(labelIndex);
        this.kindIndex = buildKindIndex(this.nodeMap.values());
        this.classNameIndex = buildClassNameIndex(this.nodeMap.values());
        this.methodNameIndex = buildMethodNameIndex(this.nodeMap.values());
        this.methodSignatureIndex = buildMethodSignatureIndex(this.nodeMap.values());

        MethodNodeIndex methodIndex = buildMethodNodeIndex(this.nodeMap.values());
        this.methodScopedIndex = methodIndex.scoped;
        this.methodLooseIndex = methodIndex.loose;
    }

    public static GraphSnapshot empty() {
        return new GraphSnapshot(
                -1L,
                Collections.emptyMap(),
                Collections.emptyMap(),
                new EncodedEdges(
                        new long[0],
                        new long[0],
                        new long[0],
                        new String[0],
                        new String[0],
                        new String[0],
                        new int[0],
                        Collections.emptyMap(),
                        Collections.emptyMap()
                )
        );
    }

    public static GraphSnapshot of(long buildSeq,
                                   Map<Long, GraphNode> nodeMap,
                                   Map<Long, List<GraphEdge>> outgoing,
                                   Map<Long, List<GraphEdge>> incoming,
                                   Map<String, List<GraphNode>> labelIndex) {
        return new GraphSnapshot(buildSeq, nodeMap, outgoing, incoming, labelIndex);
    }

    public static GraphSnapshot ofCompressed(long buildSeq,
                                             Map<Long, GraphNode> nodeMap,
                                             Map<Long, int[]> outgoingAdj,
                                             Map<Long, int[]> incomingAdj,
                                             long[] edgeIds,
                                             long[] srcIds,
                                             long[] dstIds,
                                             String[] relTypes,
                                             String[] confidences,
                                             String[] evidences,
                                             int[] opCodes,
                                             Map<String, List<GraphNode>> labelIndex) {
        EncodedEdges encoded = new EncodedEdges(
                copy(edgeIds),
                copy(srcIds),
                copy(dstIds),
                copy(relTypes),
                copy(confidences),
                copy(evidences),
                copy(opCodes),
                outgoingAdj == null ? Collections.emptyMap() : outgoingAdj,
                incomingAdj == null ? Collections.emptyMap() : incomingAdj
        );
        return new GraphSnapshot(buildSeq, nodeMap, labelIndex, encoded);
    }

    public long getBuildSeq() {
        return buildSeq;
    }

    public GraphNode getNode(long nodeId) {
        return nodeMap.get(nodeId);
    }

    public Collection<GraphNode> getNodesView() {
        return allNodes;
    }

    public List<GraphNode> getNodesByLabel(String label) {
        return new ArrayList<>(getNodesByLabelView(label));
    }

    public List<GraphNode> getNodesByLabelView(String label) {
        if (label == null || label.isBlank()) {
            return allNodes;
        }
        List<GraphNode> data = labelIndex.get(label.toLowerCase());
        return data == null ? Collections.emptyList() : data;
    }

    public List<GraphNode> getNodesByKindView(String kind) {
        if (kind == null || kind.isBlank()) {
            return allNodes;
        }
        List<GraphNode> data = kindIndex.get(kind.toLowerCase());
        return data == null ? Collections.emptyList() : data;
    }

    public List<GraphNode> getNodesByClassNameView(String className) {
        String key = normalizeClass(className);
        if (key.isEmpty()) {
            return allNodes;
        }
        List<GraphNode> data = classNameIndex.get(key);
        return data == null ? Collections.emptyList() : data;
    }

    public List<GraphNode> getNodesByMethodNameView(String methodName) {
        String key = safe(methodName);
        if (key.isEmpty()) {
            return allNodes;
        }
        List<GraphNode> data = methodNameIndex.get(key);
        return data == null ? Collections.emptyList() : data;
    }

    public List<GraphNode> getNodesByMethodSignatureView(String className,
                                                         String methodName,
                                                         String methodDesc) {
        String key = methodSignatureKey(className, methodName, methodDesc);
        if (key.isEmpty()) {
            return Collections.emptyList();
        }
        List<GraphNode> data = methodSignatureIndex.get(key);
        return data == null ? Collections.emptyList() : data;
    }

    public List<GraphEdge> getOutgoing(long nodeId) {
        return new ArrayList<>(getOutgoingView(nodeId));
    }

    public List<GraphEdge> getOutgoingView(long nodeId) {
        List<GraphEdge> edges = outgoingViews.get(nodeId);
        return edges == null ? Collections.emptyList() : edges;
    }

    public List<GraphEdge> getIncoming(long nodeId) {
        return new ArrayList<>(getIncomingView(nodeId));
    }

    public List<GraphEdge> getIncomingView(long nodeId) {
        List<GraphEdge> edges = incomingViews.get(nodeId);
        return edges == null ? Collections.emptyList() : edges;
    }

    public int getOutgoingDegree(long nodeId) {
        int[] edges = outgoingAdj.get(nodeId);
        return edges == null ? 0 : edges.length;
    }

    public int getIncomingDegree(long nodeId) {
        int[] edges = incomingAdj.get(nodeId);
        return edges == null ? 0 : edges.length;
    }

    public long findMethodNodeId(String className, String methodName, String methodDesc, Integer jarId) {
        String clazz = normalizeClass(className);
        String name = safe(methodName);
        String desc = safe(methodDesc);
        if (clazz.isEmpty() || name.isEmpty() || desc.isEmpty()) {
            return -1L;
        }
        if (jarId != null && jarId >= 0) {
            Long scoped = methodScopedIndex.get(methodScopedKey(clazz, name, desc, jarId));
            if (scoped != null) {
                return scoped;
            }
        }
        Long loose = methodLooseIndex.get(methodLooseKey(clazz, name, desc));
        return loose == null ? -1L : loose;
    }

    public int getNodeCount() {
        return nodeMap.size();
    }

    public int getEdgeCount() {
        return edgeIds.length;
    }

    private GraphEdge edgeAt(int index) {
        GraphEdge cached = edgeCache[index];
        if (cached != null) {
            return cached;
        }
        GraphEdge created = new GraphEdge(
                edgeIds[index],
                srcIds[index],
                dstIds[index],
                relTypes[index],
                confidences[index],
                evidences[index],
                opCodes[index]
        );
        edgeCache[index] = created;
        return created;
    }

    private Map<Long, List<GraphEdge>> buildEdgeViews(Map<Long, int[]> adjacency) {
        if (adjacency == null || adjacency.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<GraphEdge>> out = new HashMap<>(adjacency.size() * 2);
        for (Map.Entry<Long, int[]> entry : adjacency.entrySet()) {
            out.put(entry.getKey(), Collections.unmodifiableList(new EdgeListView(entry.getValue())));
        }
        return Collections.unmodifiableMap(out);
    }

    private static EncodedEdges encodeEdges(Map<Long, List<GraphEdge>> outgoing,
                                            Map<Long, List<GraphEdge>> incoming) {
        Map<Long, List<GraphEdge>> source = (outgoing != null && !outgoing.isEmpty()) ? outgoing : incoming;
        if (source == null || source.isEmpty()) {
            return new EncodedEdges(
                    new long[0],
                    new long[0],
                    new long[0],
                    new String[0],
                    new String[0],
                    new String[0],
                    new int[0],
                    Collections.emptyMap(),
                    Collections.emptyMap()
            );
        }

        int estimated = 0;
        for (List<GraphEdge> edges : source.values()) {
            if (edges != null) {
                estimated += edges.size();
            }
        }
        EdgeEncoder encoder = new EdgeEncoder(Math.max(16, estimated));
        for (List<GraphEdge> edges : source.values()) {
            if (edges == null || edges.isEmpty()) {
                continue;
            }
            for (GraphEdge edge : edges) {
                if (edge == null) {
                    continue;
                }
                encoder.add(edge);
            }
        }
        return encoder.freeze();
    }

    private static MethodNodeIndex buildMethodNodeIndex(Collection<GraphNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return new MethodNodeIndex(Collections.emptyMap(), Collections.emptyMap());
        }
        Map<String, Long> scoped = new HashMap<>(nodes.size());
        Map<String, Long> loose = new HashMap<>(nodes.size());
        for (GraphNode node : nodes) {
            if (node == null || !"method".equalsIgnoreCase(node.getKind())) {
                continue;
            }
            String clazz = normalizeClass(node.getClassName());
            String name = safe(node.getMethodName());
            String desc = safe(node.getMethodDesc());
            if (clazz.isEmpty() || name.isEmpty() || desc.isEmpty()) {
                continue;
            }
            long nodeId = node.getNodeId();
            scoped.merge(methodScopedKey(clazz, name, desc, node.getJarId()), nodeId, Math::min);
            loose.merge(methodLooseKey(clazz, name, desc), nodeId, Math::min);
        }
        return new MethodNodeIndex(Collections.unmodifiableMap(scoped), Collections.unmodifiableMap(loose));
    }

    private static Map<String, List<GraphNode>> buildKindIndex(Collection<GraphNode> nodes) {
        return buildNodeIndex(nodes, node -> safe(node.getKind()).toLowerCase());
    }

    private static Map<String, List<GraphNode>> buildClassNameIndex(Collection<GraphNode> nodes) {
        return buildNodeIndex(nodes, node -> normalizeClass(node.getClassName()));
    }

    private static Map<String, List<GraphNode>> buildMethodNameIndex(Collection<GraphNode> nodes) {
        return buildNodeIndex(nodes, node -> safe(node.getMethodName()));
    }

    private static Map<String, List<GraphNode>> buildMethodSignatureIndex(Collection<GraphNode> nodes) {
        return buildNodeIndex(nodes,
                node -> methodSignatureKey(node.getClassName(), node.getMethodName(), node.getMethodDesc()));
    }

    private static Map<String, List<GraphNode>> buildNodeIndex(Collection<GraphNode> nodes,
                                                               NodeIndexKeyResolver resolver) {
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<GraphNode>> out = new HashMap<>();
        for (GraphNode node : nodes) {
            if (node == null) {
                continue;
            }
            String key = resolver.resolve(node);
            if (key == null || key.isEmpty()) {
                continue;
            }
            out.computeIfAbsent(key, ignore -> new ArrayList<>()).add(node);
        }
        return freezeNodeIndex(out);
    }

    private static String methodScopedKey(String className, String methodName, String methodDesc, int jarId) {
        return methodLooseKey(className, methodName, methodDesc) + "#" + jarId;
    }

    private static String methodLooseKey(String className, String methodName, String methodDesc) {
        return normalizeClass(className) + "#" + safe(methodName) + "#" + safe(methodDesc);
    }

    private static String methodSignatureKey(String className, String methodName, String methodDesc) {
        String clazz = normalizeClass(className);
        String name = safe(methodName);
        String desc = safe(methodDesc);
        if (clazz.isEmpty() || name.isEmpty() || desc.isEmpty()) {
            return "";
        }
        return clazz + "#" + name + "#" + desc;
    }

    private static String normalizeClass(String value) {
        return safe(value).replace('.', '/');
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static long[] copy(long[] source) {
        if (source == null || source.length == 0) {
            return new long[0];
        }
        long[] out = new long[source.length];
        System.arraycopy(source, 0, out, 0, source.length);
        return out;
    }

    private static int[] copy(int[] source) {
        if (source == null || source.length == 0) {
            return new int[0];
        }
        int[] out = new int[source.length];
        System.arraycopy(source, 0, out, 0, source.length);
        return out;
    }

    private static String[] copy(String[] source) {
        if (source == null || source.length == 0) {
            return new String[0];
        }
        String[] out = new String[source.length];
        System.arraycopy(source, 0, out, 0, source.length);
        return out;
    }

    private static Map<Long, int[]> freezeAdjacency(Map<Long, int[]> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, int[]> out = new HashMap<>(source.size() * 2);
        for (Map.Entry<Long, int[]> entry : source.entrySet()) {
            out.put(entry.getKey(), copy(entry.getValue()));
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

    private static final class EdgeEncoder {
        private long[] edgeIds;
        private long[] srcIds;
        private long[] dstIds;
        private String[] relTypes;
        private String[] confidences;
        private String[] evidences;
        private int[] opCodes;
        private int size;

        private final Map<Long, IntArrayBuilder> outgoing = new HashMap<>();
        private final Map<Long, IntArrayBuilder> incoming = new HashMap<>();

        private EdgeEncoder(int initCapacity) {
            this.edgeIds = new long[initCapacity];
            this.srcIds = new long[initCapacity];
            this.dstIds = new long[initCapacity];
            this.relTypes = new String[initCapacity];
            this.confidences = new String[initCapacity];
            this.evidences = new String[initCapacity];
            this.opCodes = new int[initCapacity];
            this.size = 0;
        }

        private void add(GraphEdge edge) {
            int idx = append(edge);
            outgoing.computeIfAbsent(edge.getSrcId(), ignore -> new IntArrayBuilder()).add(idx);
            incoming.computeIfAbsent(edge.getDstId(), ignore -> new IntArrayBuilder()).add(idx);
        }

        private int append(GraphEdge edge) {
            if (size >= edgeIds.length) {
                grow();
            }
            int idx = size++;
            edgeIds[idx] = edge.getEdgeId();
            srcIds[idx] = edge.getSrcId();
            dstIds[idx] = edge.getDstId();
            relTypes[idx] = safe(edge.getRelType());
            confidences[idx] = safe(edge.getConfidence());
            evidences[idx] = safe(edge.getEvidence());
            opCodes[idx] = edge.getOpCode();
            return idx;
        }

        private void grow() {
            int next = Math.max(16, edgeIds.length * 2);
            edgeIds = copyOf(edgeIds, next);
            srcIds = copyOf(srcIds, next);
            dstIds = copyOf(dstIds, next);
            relTypes = copyOf(relTypes, next);
            confidences = copyOf(confidences, next);
            evidences = copyOf(evidences, next);
            opCodes = copyOf(opCodes, next);
        }

        private EncodedEdges freeze() {
            return new EncodedEdges(
                    trim(edgeIds, size),
                    trim(srcIds, size),
                    trim(dstIds, size),
                    trim(relTypes, size),
                    trim(confidences, size),
                    trim(evidences, size),
                    trim(opCodes, size),
                    freezeAdjacencyBuilders(outgoing),
                    freezeAdjacencyBuilders(incoming)
            );
        }

        private static long[] copyOf(long[] source, int newSize) {
            long[] out = new long[newSize];
            System.arraycopy(source, 0, out, 0, Math.min(source.length, newSize));
            return out;
        }

        private static int[] copyOf(int[] source, int newSize) {
            int[] out = new int[newSize];
            System.arraycopy(source, 0, out, 0, Math.min(source.length, newSize));
            return out;
        }

        private static String[] copyOf(String[] source, int newSize) {
            String[] out = new String[newSize];
            System.arraycopy(source, 0, out, 0, Math.min(source.length, newSize));
            return out;
        }

        private static long[] trim(long[] source, int size) {
            if (size <= 0) {
                return new long[0];
            }
            long[] out = new long[size];
            System.arraycopy(source, 0, out, 0, size);
            return out;
        }

        private static int[] trim(int[] source, int size) {
            if (size <= 0) {
                return new int[0];
            }
            int[] out = new int[size];
            System.arraycopy(source, 0, out, 0, size);
            return out;
        }

        private static String[] trim(String[] source, int size) {
            if (size <= 0) {
                return new String[0];
            }
            String[] out = new String[size];
            System.arraycopy(source, 0, out, 0, size);
            return out;
        }
    }

    private static Map<Long, int[]> freezeAdjacencyBuilders(Map<Long, IntArrayBuilder> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, int[]> out = new HashMap<>(source.size() * 2);
        for (Map.Entry<Long, IntArrayBuilder> entry : source.entrySet()) {
            out.put(entry.getKey(), entry.getValue().toArray());
        }
        return out;
    }

    private static final class IntArrayBuilder {
        private int[] data = new int[8];
        private int size = 0;

        private void add(int value) {
            if (size >= data.length) {
                int[] next = new int[data.length * 2];
                System.arraycopy(data, 0, next, 0, data.length);
                data = next;
            }
            data[size++] = value;
        }

        private int[] toArray() {
            if (size == 0) {
                return new int[0];
            }
            int[] out = new int[size];
            System.arraycopy(data, 0, out, 0, size);
            return out;
        }
    }

    private final class EdgeListView extends AbstractList<GraphEdge> implements RandomAccess {
        private final int[] edgeIndexes;

        private EdgeListView(int[] edgeIndexes) {
            this.edgeIndexes = edgeIndexes == null ? new int[0] : edgeIndexes;
        }

        @Override
        public GraphEdge get(int index) {
            if (index < 0 || index >= edgeIndexes.length) {
                throw new IndexOutOfBoundsException("index=" + index + ", size=" + edgeIndexes.length);
            }
            return edgeAt(edgeIndexes[index]);
        }

        @Override
        public int size() {
            return edgeIndexes.length;
        }
    }

    @FunctionalInterface
    private interface NodeIndexKeyResolver {
        String resolve(GraphNode node);
    }

    private record MethodNodeIndex(Map<String, Long> scoped, Map<String, Long> loose) {
    }

    private record EncodedEdges(long[] edgeIds,
                                long[] srcIds,
                                long[] dstIds,
                                String[] relTypes,
                                String[] confidences,
                                String[] evidences,
                                int[] opCodes,
                                Map<Long, int[]> outgoingAdj,
                                Map<Long, int[]> incomingAdj) {
    }
}
