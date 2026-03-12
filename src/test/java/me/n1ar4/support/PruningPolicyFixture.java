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

public final class PruningPolicyFixture {
    private static final int JAR_ID = 1;
    private static final String DESC = "(Ljava/lang/String;)V";
    private static final String SANITIZER_SOURCE_CLASS = internalName(SanitizerSource.class);
    private static final String SANITIZER_BRIDGE_CLASS = internalName(SanitizerBridge.class);
    private static final String ADDITIONAL_SOURCE_CLASS = internalName(AdditionalSource.class);
    private static final String ADDITIONAL_BRIDGE_CLASS = internalName(AdditionalBridge.class);
    private static final String SINK_CLASS = internalName(Sink.class);

    private PruningPolicyFixture() {
    }

    public static FixtureData installSanitizerFixture() {
        DatabaseManager.saveClassFiles(Set.of(
                classFile(SanitizerSource.class),
                classFile(SanitizerBridge.class),
                classFile(Sink.class)
        ));
        return new FixtureData(
                sanitizerSnapshot(),
                SANITIZER_SOURCE_CLASS,
                "entry",
                DESC,
                SINK_CLASS,
                "sink",
                DESC
        );
    }

    public static FixtureData installAdditionalFixture() {
        DatabaseManager.saveClassFiles(Set.of(
                classFile(AdditionalSource.class),
                classFile(AdditionalBridge.class),
                classFile(Sink.class)
        ));
        return new FixtureData(
                additionalSnapshot(),
                ADDITIONAL_SOURCE_CLASS,
                "entry",
                DESC,
                SINK_CLASS,
                "sink",
                DESC
        );
    }

    private static GraphSnapshot sanitizerSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(1L, methodNode(1L, SANITIZER_SOURCE_CLASS, "entry"));
        nodes.put(2L, methodNode(2L, SANITIZER_BRIDGE_CLASS, "cleanAndSink"));
        nodes.put(3L, methodNode(3L, SINK_CLASS, "sink"));

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        addEdge(outgoing, incoming, new GraphEdge(11L, 1L, 2L, "CALLS_DIRECT", "high", "fixture", 0));
        addEdge(outgoing, incoming, new GraphEdge(12L, 2L, 3L, "CALLS_DIRECT", "high", "fixture", 0));
        return GraphSnapshot.of(51L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphSnapshot additionalSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(11L, methodNode(11L, ADDITIONAL_SOURCE_CLASS, "entry"));
        nodes.put(12L, methodNode(12L, ADDITIONAL_BRIDGE_CLASS, "pass"));
        nodes.put(13L, methodNode(13L, SINK_CLASS, "sink"));

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        addEdge(outgoing, incoming, new GraphEdge(21L, 11L, 12L, "CALLS_DIRECT", "high", "fixture", 0));
        addEdge(outgoing, incoming, new GraphEdge(22L, 12L, 13L, "CALLS_DIRECT", "high", "fixture", 0));
        return GraphSnapshot.of(52L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphNode methodNode(long id, String clazz, String method) {
        return new GraphNode(id, "method", JAR_ID, clazz, method, DESC, "", -1, -1);
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
                              String sinkDesc) {
    }

    public static final class SanitizerSource {
        public static void entry(String value) {
            SanitizerBridge.cleanAndSink(value);
        }
    }

    public static final class SanitizerBridge {
        public static void cleanAndSink(String value) {
            Sink.sink(value);
        }
    }

    public static final class AdditionalSource {
        public static void entry(String value) {
            AdditionalBridge.pass(value);
        }
    }

    public static final class AdditionalBridge {
        public static void pass(String value) {
            java.util.List<String> list = new java.util.ArrayList<>();
            list.add(value);
            Sink.sink(list.get(0));
        }
    }

    public static final class Sink {
        public static void sink(String value) {
        }
    }
}
