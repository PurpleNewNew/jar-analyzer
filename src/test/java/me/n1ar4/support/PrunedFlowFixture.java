/*
 * GPLv3 License
 */

package me.n1ar4.support;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.facts.ClassFileEntity;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PrunedFlowFixture {
    private static final int JAR_ID = 1;
    private static final String DESC = "(Ljava/lang/String;)V";
    private static final String SOURCE_CLASS = internalName(Source.class);
    private static final String LAYER1_CLASS = internalName(Layer1.class);
    private static final String LAYER2_CLASS = internalName(Layer2.class);
    private static final String LAYER3_CLASS = internalName(Layer3.class);
    private static final String SINK_CLASS = internalName(Sink.class);

    private PrunedFlowFixture() {
    }

    public static FixtureData install() {
        DatabaseManager.saveClassFiles(Set.of(
                classFile(Source.class),
                classFile(Layer1.class),
                classFile(Layer2.class),
                classFile(Layer3.class),
                classFile(Sink.class)
        ));
        return new FixtureData(
                buildSnapshot(),
                SOURCE_CLASS,
                "entry",
                DESC,
                SINK_CLASS,
                "sink",
                DESC,
                1L,
                14L
        );
    }

    private static GraphSnapshot buildSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();

        nodes.put(1L, sourceMethodNode(1L, SOURCE_CLASS, "entry"));
        nodes.put(2L, methodNode(2L, LAYER1_CLASS, "good"));
        nodes.put(3L, methodNode(3L, LAYER1_CLASS, "noiseA"));
        nodes.put(4L, methodNode(4L, LAYER1_CLASS, "noiseB"));
        nodes.put(5L, methodNode(5L, LAYER1_CLASS, "noiseC"));
        nodes.put(6L, methodNode(6L, LAYER2_CLASS, "good"));
        nodes.put(7L, methodNode(7L, LAYER2_CLASS, "noiseA"));
        nodes.put(8L, methodNode(8L, LAYER2_CLASS, "noiseB"));
        nodes.put(9L, methodNode(9L, LAYER2_CLASS, "noiseC"));
        nodes.put(10L, methodNode(10L, LAYER3_CLASS, "good"));
        nodes.put(11L, methodNode(11L, LAYER3_CLASS, "noiseA"));
        nodes.put(12L, methodNode(12L, LAYER3_CLASS, "noiseB"));
        nodes.put(13L, methodNode(13L, LAYER3_CLASS, "noiseC"));
        nodes.put(14L, methodNode(14L, SINK_CLASS, "sink"));

        long edgeId = 1000L;
        for (long dst = 2L; dst <= 5L; dst++) {
            addEdge(outgoing, incoming, new GraphEdge(edgeId++, 1L, dst, "CALLS_DIRECT", "high", "fixture", 0));
        }
        for (long src = 2L; src <= 5L; src++) {
            for (long dst = 6L; dst <= 9L; dst++) {
                addEdge(outgoing, incoming, new GraphEdge(edgeId++, src, dst, "CALLS_DIRECT", "high", "fixture", 0));
            }
        }
        for (long src = 6L; src <= 9L; src++) {
            for (long dst = 10L; dst <= 13L; dst++) {
                addEdge(outgoing, incoming, new GraphEdge(edgeId++, src, dst, "CALLS_DIRECT", "high", "fixture", 0));
            }
        }
        for (long src = 10L; src <= 13L; src++) {
            addEdge(outgoing, incoming, new GraphEdge(edgeId++, src, 14L, "CALLS_DIRECT", "high", "fixture", 0));
        }
        return GraphSnapshot.of(41L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphNode methodNode(long id, String clazz, String method) {
        return new GraphNode(id, "method", JAR_ID, clazz, method, DESC, "", -1, -1);
    }

    private static GraphNode sourceMethodNode(long id, String clazz, String method) {
        return new GraphNode(id, "method", JAR_ID, clazz, method, DESC, "", -1, -1, GraphNode.SOURCE_FLAG_ANY);
    }

    private static void addEdge(Map<Long, List<GraphEdge>> outgoing,
                                Map<Long, List<GraphEdge>> incoming,
                                GraphEdge edge) {
        outgoing.computeIfAbsent(edge.getSrcId(), ignore -> new ArrayList<>()).add(edge);
        incoming.computeIfAbsent(edge.getDstId(), ignore -> new ArrayList<>()).add(edge);
    }

    private static ClassFileEntity classFile(Class<?> type) {
        try {
            URL resource = type.getClassLoader().getResource(resourceName(type));
            if (resource == null) {
                throw new IllegalStateException("missing fixture bytecode: " + type.getName());
            }
            Path path = Paths.get(resource.toURI());
            return new ClassFileEntity(internalName(type), path, JAR_ID);
        } catch (Exception ex) {
            throw new IllegalStateException("load fixture bytecode failed: " + type.getName(), ex);
        }
    }

    private static String resourceName(Class<?> type) {
        return type.getName().replace('.', '/') + ".class";
    }

    private static String internalName(Class<?> type) {
        return type.getName().replace('.', '/');
    }

    public record FixtureData(GraphSnapshot snapshot,
                              String sourceClass,
                              String sourceMethod,
                              String sourceDesc,
                              String sinkClass,
                              String sinkMethod,
                              String sinkDesc,
                              long sourceNodeId,
                              long sinkNodeId) {
    }

    public static final class Source {
        public static void entry(String value) {
            Layer1.good(value);
            Layer1.noiseA("safe");
            Layer1.noiseB("safe");
            Layer1.noiseC("safe");
        }
    }

    public static final class Layer1 {
        public static void good(String value) {
            Layer2.good(value);
            Layer2.noiseA("safe");
            Layer2.noiseB("safe");
            Layer2.noiseC("safe");
        }

        public static void noiseA(String value) {
            Layer2.good("safe");
            Layer2.noiseA("safe");
            Layer2.noiseB("safe");
            Layer2.noiseC("safe");
        }

        public static void noiseB(String value) {
            Layer2.good("safe");
            Layer2.noiseA("safe");
            Layer2.noiseB("safe");
            Layer2.noiseC("safe");
        }

        public static void noiseC(String value) {
            Layer2.good("safe");
            Layer2.noiseA("safe");
            Layer2.noiseB("safe");
            Layer2.noiseC("safe");
        }
    }

    public static final class Layer2 {
        public static void good(String value) {
            Layer3.good(value);
            Layer3.noiseA("safe");
            Layer3.noiseB("safe");
            Layer3.noiseC("safe");
        }

        public static void noiseA(String value) {
            Layer3.good("safe");
            Layer3.noiseA("safe");
            Layer3.noiseB("safe");
            Layer3.noiseC("safe");
        }

        public static void noiseB(String value) {
            Layer3.good("safe");
            Layer3.noiseA("safe");
            Layer3.noiseB("safe");
            Layer3.noiseC("safe");
        }

        public static void noiseC(String value) {
            Layer3.good("safe");
            Layer3.noiseA("safe");
            Layer3.noiseB("safe");
            Layer3.noiseC("safe");
        }
    }

    public static final class Layer3 {
        public static void good(String value) {
            Sink.sink(value);
        }

        public static void noiseA(String value) {
            Sink.sink("safe");
        }

        public static void noiseB(String value) {
            Sink.sink("safe");
        }

        public static void noiseC(String value) {
            Sink.sink("safe");
        }
    }

    public static final class Sink {
        public static void sink(String value) {
        }
    }
}
