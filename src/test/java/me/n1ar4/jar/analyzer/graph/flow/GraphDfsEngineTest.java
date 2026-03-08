/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.flow;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.core.reference.AnnoReference;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.rules.ModelRegistry;
import me.n1ar4.jar.analyzer.rules.SinkRuleRegistry;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphDfsEngineTest {
    private static final String MODEL_PROP = "jar.analyzer.rules.model.path";
    private static final String SOURCE_PROP = "jar.analyzer.rules.source.path";
    private static final String SINK_PROP = "jar.analyzer.rules.sink.path";

    @TempDir
    Path tempDir;

    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
    }

    @Test
    void searchAllSourcesShouldIncludeMarkedSourceWithIncomingEdges() {
        Map<Long, GraphNode> nodes = new LinkedHashMap<>();
        nodes.put(1L, new GraphNode(1L, "method", 1, "app/Helper", "helper", "()V", "", -1, -1));
        nodes.put(2L, new GraphNode(
                2L,
                "method",
                1,
                "app/Controller",
                "entry",
                "()V",
                "",
                -1,
                -1,
                GraphNode.SOURCE_FLAG_ANY
        ));
        nodes.put(3L, new GraphNode(3L, "method", 1, "app/Sink", "sink", "()V", "", -1, -1));

        Map<Long, List<GraphEdge>> outgoing = new LinkedHashMap<>();
        Map<Long, List<GraphEdge>> incoming = new LinkedHashMap<>();
        addEdge(outgoing, incoming, new GraphEdge(11L, 1L, 2L, "CALLS_DIRECT", "high", "unit", 0));
        addEdge(outgoing, incoming, new GraphEdge(12L, 2L, 3L, "CALLS_DIRECT", "high", "unit", 0));

        GraphSnapshot snapshot = GraphSnapshot.of(1L, nodes, outgoing, incoming, Map.of());
        FlowOptions options = FlowOptions.builder()
                .fromSink(true)
                .searchAllSources(true)
                .depth(4)
                .sink("app/Sink", "sink", "()V")
                .build();

        List<DFSResult> results = new GraphDfsEngine().run(snapshot, options, null).results();

        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(result ->
                result.getSource() != null
                        && result.getMethodList() != null
                        && result.getMethodList().size() == 2
                        && "app/Controller".equals(result.getSource().getClassReference().getName())
                        && "entry".equals(result.getSource().getName())));
    }

    @Test
    void preciseSearchShouldRespectSourceAndSinkJarIds() {
        Map<Long, GraphNode> nodes = new LinkedHashMap<>();
        nodes.put(1L, new GraphNode(1L, "method", 1, "dup/Source", "entry", "()V", "", -1, -1));
        nodes.put(2L, new GraphNode(2L, "method", 2, "dup/Source", "entry", "()V", "", -1, -1));
        nodes.put(3L, new GraphNode(3L, "method", 1, "dup/Sink", "sink", "()V", "", -1, -1));
        nodes.put(4L, new GraphNode(4L, "method", 2, "dup/Sink", "sink", "()V", "", -1, -1));

        Map<Long, List<GraphEdge>> outgoing = new LinkedHashMap<>();
        Map<Long, List<GraphEdge>> incoming = new LinkedHashMap<>();
        addEdge(outgoing, incoming, new GraphEdge(21L, 1L, 3L, "CALLS_DIRECT", "high", "unit", 0));
        addEdge(outgoing, incoming, new GraphEdge(22L, 2L, 4L, "CALLS_DIRECT", "high", "unit", 0));

        GraphSnapshot snapshot = GraphSnapshot.of(2L, nodes, outgoing, incoming, Map.of());
        FlowOptions options = FlowOptions.builder()
                .fromSink(false)
                .depth(4)
                .source("dup/Source", "entry", "()V")
                .sourceJarId(1)
                .sink("dup/Sink", "sink", "()V")
                .sinkJarId(1)
                .build();

        List<DFSResult> results = new GraphDfsEngine().run(snapshot, options, null).results();

        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getSource().getJarId());
        assertEquals(1, results.get(0).getSink().getJarId());
    }

    @Test
    void searchAllSourcesShouldHonorHotReloadedSourceModelWithoutRebuild() throws Exception {
        Path model = tempDir.resolve("model.json");
        Path source = tempDir.resolve("source.json");
        Path sink = tempDir.resolve("sink.json");

        String oldModelProp = System.getProperty(MODEL_PROP);
        String oldSourceProp = System.getProperty(SOURCE_PROP);
        String oldSinkProp = System.getProperty(SINK_PROP);
        try {
            Files.writeString(model, "{\"summaryModel\":[],\"additionalStepHints\":[]}", StandardCharsets.UTF_8);
            Files.writeString(source, "{\"sourceAnnotations\":[],\"sourceModel\":[]}", StandardCharsets.UTF_8);
            Files.writeString(sink, "{\"name\":\"dfs-test\",\"levels\":{}}", StandardCharsets.UTF_8);

            System.setProperty(MODEL_PROP, model.toString());
            System.setProperty(SOURCE_PROP, source.toString());
            System.setProperty(SINK_PROP, sink.toString());
            SinkRuleRegistry.reload();
            ModelRegistry.reload();

            restoreRuntime(
                    method("app/Helper", "helper", "()V", Set.of()),
                    method("app/Controller", "entry", "()V", Set.of()),
                    method("app/Sink", "sink", "()V", Set.of())
            );

            GraphSnapshot snapshot = graphSnapshot(
                    new GraphNode(1L, "method", 1, "app/Helper", "helper", "()V", "", -1, -1),
                    new GraphNode(2L, "method", 1, "app/Controller", "entry", "()V", "", -1, -1),
                    new GraphNode(3L, "method", 1, "app/Sink", "sink", "()V", "", -1, -1)
            );

            List<DFSResult> before = new GraphDfsEngine().run(snapshot, FlowOptions.builder()
                    .fromSink(true)
                    .searchAllSources(true)
                    .depth(4)
                    .sink("app/Sink", "sink", "()V")
                    .build(), null).results();
            assertFalse(hasSource(before, "app/Controller", "entry"));

            Files.writeString(source,
                    "{\"sourceAnnotations\":[],\"sourceModel\":[{\"className\":\"app/Controller\",\"methodName\":\"entry\",\"methodDesc\":\"()V\",\"kind\":\"custom\"}]}",
                    StandardCharsets.UTF_8);
            ModelRegistry.reload();

            List<DFSResult> after = new GraphDfsEngine().run(snapshot, FlowOptions.builder()
                    .fromSink(true)
                    .searchAllSources(true)
                    .depth(4)
                    .sink("app/Sink", "sink", "()V")
                    .build(), null).results();
            assertTrue(hasSource(after, "app/Controller", "entry"));
        } finally {
            restoreProperty(MODEL_PROP, oldModelProp);
            restoreProperty(SOURCE_PROP, oldSourceProp);
            restoreProperty(SINK_PROP, oldSinkProp);
            SinkRuleRegistry.reload();
            ModelRegistry.reload();
        }
    }

    @Test
    void onlyFromWebShouldHonorHotReloadedSourceAnnotationsWithoutRebuild() throws Exception {
        Path model = tempDir.resolve("model-web.json");
        Path source = tempDir.resolve("source-web.json");
        Path sink = tempDir.resolve("sink-web.json");

        String oldModelProp = System.getProperty(MODEL_PROP);
        String oldSourceProp = System.getProperty(SOURCE_PROP);
        String oldSinkProp = System.getProperty(SINK_PROP);
        try {
            Files.writeString(model, "{\"summaryModel\":[],\"additionalStepHints\":[]}", StandardCharsets.UTF_8);
            Files.writeString(source, "{\"sourceAnnotations\":[],\"sourceModel\":[]}", StandardCharsets.UTF_8);
            Files.writeString(sink, "{\"name\":\"dfs-test\",\"levels\":{}}", StandardCharsets.UTF_8);

            System.setProperty(MODEL_PROP, model.toString());
            System.setProperty(SOURCE_PROP, source.toString());
            System.setProperty(SINK_PROP, sink.toString());
            SinkRuleRegistry.reload();
            ModelRegistry.reload();

            restoreRuntime(
                    method("app/Helper", "helper", "()V", Set.of()),
                    method("app/Controller", "entry", "()V",
                            Set.of(new AnnoReference("Lorg/springframework/web/bind/annotation/GetMapping;"))),
                    method("app/Sink", "sink", "()V", Set.of())
            );

            GraphSnapshot snapshot = graphSnapshot(
                    new GraphNode(1L, "method", 1, "app/Helper", "helper", "()V", "", -1, -1),
                    new GraphNode(2L, "method", 1, "app/Controller", "entry", "()V", "", -1, -1),
                    new GraphNode(3L, "method", 1, "app/Sink", "sink", "()V", "", -1, -1)
            );

            List<DFSResult> before = new GraphDfsEngine().run(snapshot, FlowOptions.builder()
                    .fromSink(true)
                    .searchAllSources(true)
                    .onlyFromWeb(true)
                    .depth(4)
                    .sink("app/Sink", "sink", "()V")
                    .build(), null).results();
            assertTrue(before.isEmpty());

            Files.writeString(source,
                    "{\"sourceAnnotations\":[\"org.springframework.web.bind.annotation.GetMapping\"],\"sourceModel\":[]}",
                    StandardCharsets.UTF_8);
            ModelRegistry.reload();

            List<DFSResult> after = new GraphDfsEngine().run(snapshot, FlowOptions.builder()
                    .fromSink(true)
                    .searchAllSources(true)
                    .onlyFromWeb(true)
                    .depth(4)
                    .sink("app/Sink", "sink", "()V")
                    .build(), null).results();
            assertTrue(hasSource(after, "app/Controller", "entry"));
        } finally {
            restoreProperty(MODEL_PROP, oldModelProp);
            restoreProperty(SOURCE_PROP, oldSourceProp);
            restoreProperty(SINK_PROP, oldSinkProp);
            SinkRuleRegistry.reload();
            ModelRegistry.reload();
        }
    }

    private static void addEdge(Map<Long, List<GraphEdge>> outgoing,
                                Map<Long, List<GraphEdge>> incoming,
                                GraphEdge edge) {
        outgoing.computeIfAbsent(edge.getSrcId(), ignore -> new ArrayList<>()).add(edge);
        incoming.computeIfAbsent(edge.getDstId(), ignore -> new ArrayList<>()).add(edge);
    }

    private static boolean hasSource(List<DFSResult> results, String className, String methodName) {
        if (results == null || results.isEmpty()) {
            return false;
        }
        for (DFSResult result : results) {
            if (result == null || result.getSource() == null || result.getSource().getClassReference() == null) {
                continue;
            }
            if (className.equals(result.getSource().getClassReference().getName())
                    && methodName.equals(result.getSource().getName())) {
                return true;
            }
        }
        return false;
    }

    private static GraphSnapshot graphSnapshot(GraphNode helper, GraphNode source, GraphNode sink) {
        Map<Long, GraphNode> nodes = new LinkedHashMap<>();
        nodes.put(helper.getNodeId(), helper);
        nodes.put(source.getNodeId(), source);
        nodes.put(sink.getNodeId(), sink);
        Map<Long, List<GraphEdge>> outgoing = new LinkedHashMap<>();
        Map<Long, List<GraphEdge>> incoming = new LinkedHashMap<>();
        addEdge(outgoing, incoming, new GraphEdge(31L, helper.getNodeId(), source.getNodeId(), "CALLS_DIRECT", "high", "unit", 0));
        addEdge(outgoing, incoming, new GraphEdge(32L, source.getNodeId(), sink.getNodeId(), "CALLS_DIRECT", "high", "unit", 0));
        return GraphSnapshot.of(3L, nodes, outgoing, incoming, Map.of());
    }

    private static MethodReference method(String owner, String name, String desc, Set<AnnoReference> annotations) {
        return new MethodReference(
                new ClassReference.Handle(owner, 1),
                name,
                desc,
                false,
                annotations == null ? Set.of() : annotations,
                1,
                1,
                "app.jar",
                1
        );
    }

    private static void restoreRuntime(MethodReference... methods) {
        ProjectModel model = ProjectModel.artifact(
                Path.of("/tmp/jar-analyzer/dfs-test.jar"),
                null,
                List.of(Path.of("/tmp/jar-analyzer/dfs-test.jar")),
                false
        );
        Set<MethodReference> refs = new LinkedHashSet<>();
        if (methods != null) {
            Collections.addAll(refs, methods);
        }
        ProjectRuntimeSnapshot snapshot = DatabaseManager.buildProjectRuntimeSnapshot(
                1L,
                model,
                List.of("/tmp/jar-analyzer/dfs-test.jar"),
                Set.of(),
                Set.of(),
                refs,
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        DatabaseManager.restoreProjectRuntime(snapshot);
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
