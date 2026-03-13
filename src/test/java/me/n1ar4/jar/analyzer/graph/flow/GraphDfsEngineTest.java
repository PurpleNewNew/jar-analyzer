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
import me.n1ar4.jar.analyzer.rules.RuleRegistryTestSupport;
import me.n1ar4.jar.analyzer.rules.SinkRuleRegistry;
import me.n1ar4.jar.analyzer.graph.flow.model.FlowPath;
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

    @TempDir
    Path tempDir;

    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
        RuleRegistryTestSupport.clearRuleFiles();
    }

    @Test
    void searchAllSourcesShouldIncludeResolvedSourceWithIncomingEdges() throws Exception {
        Path model = tempDir.resolve("model-incoming.json");
        Path source = tempDir.resolve("source-incoming.json");
        Path sink = tempDir.resolve("sink-incoming.json");
        try {
            Files.writeString(model, "{\"summaryModel\":[],\"additionalStepHints\":[]}", StandardCharsets.UTF_8);
            Files.writeString(source,
                    "{\"sourceAnnotations\":[],\"sourceModel\":[{\"className\":\"app/Controller\",\"methodName\":\"entry\",\"methodDesc\":\"()V\",\"kind\":\"custom\"}]}",
                    StandardCharsets.UTF_8);
            Files.writeString(sink, "{\"name\":\"dfs-test\",\"levels\":{}}", StandardCharsets.UTF_8);

            RuleRegistryTestSupport.useRuleFiles(model, source, sink);
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

            List<FlowPath> results = new GraphDfsEngine().run(snapshot, FlowOptions.builder()
                    .fromSink(true)
                    .searchAllSources(true)
                    .depth(4)
                    .sink("app/Sink", "sink", "()V")
                    .build(), null).results();

            assertFalse(results.isEmpty());
            assertTrue(hasSource(results, "app/Controller", "entry"));
        } finally {
            RuleRegistryTestSupport.clearRuleFiles();
            SinkRuleRegistry.reload();
            ModelRegistry.reload();
        }
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

        List<FlowPath> results = new GraphDfsEngine().run(snapshot, options, null).results();

        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getSource().getJarId());
        assertEquals(1, results.get(0).getSink().getJarId());
    }

    @Test
    void preciseSearchShouldKeepParallelCallSitePathsDistinct() {
        Map<Long, GraphNode> nodes = new LinkedHashMap<>();
        nodes.put(1L, new GraphNode(1L, "method", 1, "app/Source", "entry", "()V", "", -1, -1));
        nodes.put(2L, new GraphNode(2L, "method", 1, "app/Sink", "sink", "()V", "", -1, -1));

        Map<Long, List<GraphEdge>> outgoing = new LinkedHashMap<>();
        Map<Long, List<GraphEdge>> incoming = new LinkedHashMap<>();
        addEdge(outgoing, incoming, new GraphEdge(31L, 1L, 2L, "CALLS_DIRECT", "high", "unit", "", 0, "cs-1", 10, 0));
        addEdge(outgoing, incoming, new GraphEdge(32L, 1L, 2L, "CALLS_DIRECT", "high", "unit", "", 0, "cs-2", 20, 1));

        GraphSnapshot snapshot = GraphSnapshot.of(3L, nodes, outgoing, incoming, Map.of());
        FlowOptions options = FlowOptions.builder()
                .fromSink(false)
                .depth(2)
                .source("app/Source", "entry", "()V")
                .sink("app/Sink", "sink", "()V")
                .build();

        List<FlowPath> results = new GraphDfsEngine().run(snapshot, options, null).results();

        assertEquals(2, results.size());
        assertEquals(List.of("cs-1", "cs-2"),
                results.stream()
                        .map(result -> result.getEdges().get(0).getCallSiteKey())
                        .toList());
    }

    @Test
    void preciseSinkModeShouldSearchFromSinkButKeepForwardOrderedResults() {
        Map<Long, GraphNode> nodes = new LinkedHashMap<>();
        nodes.put(1L, new GraphNode(1L, "method", 1, "app/Source", "entry", "()V", "", -1, -1));
        nodes.put(2L, new GraphNode(2L, "method", 1, "app/Alpha", "step", "()V", "", -1, -1));
        nodes.put(3L, new GraphNode(3L, "method", 1, "app/Beta", "step", "()V", "", -1, -1));
        nodes.put(4L, new GraphNode(4L, "method", 1, "app/Sink", "sink", "()V", "", -1, -1));

        Map<Long, List<GraphEdge>> outgoing = new LinkedHashMap<>();
        Map<Long, List<GraphEdge>> incoming = new LinkedHashMap<>();
        addEdge(outgoing, incoming, new GraphEdge(200L, 1L, 2L, "CALLS_DIRECT", "high", "unit", 0));
        addEdge(outgoing, incoming, new GraphEdge(1L, 2L, 4L, "CALLS_DIRECT", "high", "unit", 0));
        addEdge(outgoing, incoming, new GraphEdge(100L, 1L, 3L, "CALLS_DIRECT", "high", "unit", 0));
        addEdge(outgoing, incoming, new GraphEdge(2L, 3L, 4L, "CALLS_DIRECT", "high", "unit", 0));

        GraphSnapshot snapshot = GraphSnapshot.of(4L, nodes, outgoing, incoming, Map.of());
        FlowOptions options = FlowOptions.builder()
                .fromSink(true)
                .searchAllSources(false)
                .depth(3)
                .maxPaths(1)
                .source("app/Source", "entry", "()V")
                .sink("app/Sink", "sink", "()V")
                .build();

        List<FlowPath> results = new GraphDfsEngine().run(snapshot, options, null).results();

        assertEquals(1, results.size());
        FlowPath result = results.get(0);
        assertEquals(FlowPath.FROM_SINK_TO_SOURCE, result.getMode());
        assertEquals(List.of("app/Source", "app/Alpha", "app/Sink"),
                result.getMethodList().stream()
                        .map(handle -> handle.getClassReference().getName())
                        .toList());
        assertEquals("app/Source", result.getEdges().get(0).getFrom().getClassReference().getName());
        assertEquals("app/Alpha", result.getEdges().get(0).getTo().getClassReference().getName());
        assertEquals("app/Alpha", result.getEdges().get(1).getFrom().getClassReference().getName());
        assertEquals("app/Sink", result.getEdges().get(1).getTo().getClassReference().getName());
    }

    @Test
    void searchAllSourcesShouldHonorHotReloadedSourceModelWithoutRebuild() throws Exception {
        Path model = tempDir.resolve("model.json");
        Path source = tempDir.resolve("source.json");
        Path sink = tempDir.resolve("sink.json");
        try {
            Files.writeString(model, "{\"summaryModel\":[],\"additionalStepHints\":[]}", StandardCharsets.UTF_8);
            Files.writeString(source, "{\"sourceAnnotations\":[],\"sourceModel\":[]}", StandardCharsets.UTF_8);
            Files.writeString(sink, "{\"name\":\"dfs-test\",\"levels\":{}}", StandardCharsets.UTF_8);

            RuleRegistryTestSupport.useRuleFiles(model, source, sink);
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

            List<FlowPath> before = new GraphDfsEngine().run(snapshot, FlowOptions.builder()
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

            List<FlowPath> after = new GraphDfsEngine().run(snapshot, FlowOptions.builder()
                    .fromSink(true)
                    .searchAllSources(true)
                    .depth(4)
                    .sink("app/Sink", "sink", "()V")
                    .build(), null).results();
            assertTrue(hasSource(after, "app/Controller", "entry"));
        } finally {
            RuleRegistryTestSupport.clearRuleFiles();
            SinkRuleRegistry.reload();
            ModelRegistry.reload();
        }
    }

    @Test
    void onlyFromWebShouldHonorHotReloadedSourceAnnotationsWithoutRebuild() throws Exception {
        Path model = tempDir.resolve("model-web.json");
        Path source = tempDir.resolve("source-web.json");
        Path sink = tempDir.resolve("sink-web.json");
        try {
            Files.writeString(model, "{\"summaryModel\":[],\"additionalStepHints\":[]}", StandardCharsets.UTF_8);
            Files.writeString(source, "{\"sourceAnnotations\":[],\"sourceModel\":[]}", StandardCharsets.UTF_8);
            Files.writeString(sink, "{\"name\":\"dfs-test\",\"levels\":{}}", StandardCharsets.UTF_8);

            RuleRegistryTestSupport.useRuleFiles(model, source, sink);
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

            List<FlowPath> before = new GraphDfsEngine().run(snapshot, FlowOptions.builder()
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

            List<FlowPath> after = new GraphDfsEngine().run(snapshot, FlowOptions.builder()
                    .fromSink(true)
                    .searchAllSources(true)
                    .onlyFromWeb(true)
                    .depth(4)
                    .sink("app/Sink", "sink", "()V")
                    .build(), null).results();
            assertTrue(hasSource(after, "app/Controller", "entry"));
        } finally {
            RuleRegistryTestSupport.clearRuleFiles();
            SinkRuleRegistry.reload();
            ModelRegistry.reload();
        }
    }

    @Test
    void searchAllSourcesShouldDropRemovedSourceModelWithoutRebuildEvenWhenGraphFlagsPersist() throws Exception {
        Path model = tempDir.resolve("model-remove.json");
        Path source = tempDir.resolve("source-remove.json");
        Path sink = tempDir.resolve("sink-remove.json");
        try {
            Files.writeString(model, "{\"summaryModel\":[],\"additionalStepHints\":[]}", StandardCharsets.UTF_8);
            Files.writeString(source,
                    "{\"sourceAnnotations\":[],\"sourceModel\":[{\"className\":\"app/Controller\",\"methodName\":\"entry\",\"methodDesc\":\"()V\",\"kind\":\"custom\"}]}",
                    StandardCharsets.UTF_8);
            Files.writeString(sink, "{\"name\":\"dfs-test\",\"levels\":{}}", StandardCharsets.UTF_8);

            RuleRegistryTestSupport.useRuleFiles(model, source, sink);
            SinkRuleRegistry.reload();
            ModelRegistry.reload();

            restoreRuntime(
                    method("app/Helper", "helper", "()V", Set.of()),
                    method("app/Controller", "entry", "()V", Set.of()),
                    method("app/Sink", "sink", "()V", Set.of())
            );

            GraphSnapshot snapshot = graphSnapshot(
                    new GraphNode(1L, "method", 1, "app/Helper", "helper", "()V", "", -1, -1),
                    new GraphNode(2L, "method", 1, "app/Controller", "entry", "()V", "", -1, -1, GraphNode.SOURCE_FLAG_ANY),
                    new GraphNode(3L, "method", 1, "app/Sink", "sink", "()V", "", -1, -1)
            );

            List<FlowPath> before = new GraphDfsEngine().run(snapshot, FlowOptions.builder()
                    .fromSink(true)
                    .searchAllSources(true)
                    .depth(4)
                    .sink("app/Sink", "sink", "()V")
                    .build(), null).results();
            assertTrue(hasSource(before, "app/Controller", "entry"));

            Files.writeString(source, "{\"sourceAnnotations\":[],\"sourceModel\":[]}", StandardCharsets.UTF_8);
            ModelRegistry.reload();

            List<FlowPath> after = new GraphDfsEngine().run(snapshot, FlowOptions.builder()
                    .fromSink(true)
                    .searchAllSources(true)
                    .depth(4)
                    .sink("app/Sink", "sink", "()V")
                    .build(), null).results();
            assertFalse(hasSource(after, "app/Controller", "entry"));
        } finally {
            RuleRegistryTestSupport.clearRuleFiles();
            SinkRuleRegistry.reload();
            ModelRegistry.reload();
        }
    }

    @Test
    void onlyFromWebShouldDropRemovedSourceAnnotationsWithoutRebuildEvenWhenGraphFlagsPersist() throws Exception {
        Path model = tempDir.resolve("model-web-remove.json");
        Path source = tempDir.resolve("source-web-remove.json");
        Path sink = tempDir.resolve("sink-web-remove.json");
        try {
            Files.writeString(model, "{\"summaryModel\":[],\"additionalStepHints\":[]}", StandardCharsets.UTF_8);
            Files.writeString(source,
                    "{\"sourceAnnotations\":[\"org.springframework.web.bind.annotation.GetMapping\"],\"sourceModel\":[]}",
                    StandardCharsets.UTF_8);
            Files.writeString(sink, "{\"name\":\"dfs-test\",\"levels\":{}}", StandardCharsets.UTF_8);

            RuleRegistryTestSupport.useRuleFiles(model, source, sink);
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
                    new GraphNode(2L, "method", 1, "app/Controller", "entry", "()V", "", -1, -1,
                            GraphNode.SOURCE_FLAG_ANY | GraphNode.SOURCE_FLAG_WEB),
                    new GraphNode(3L, "method", 1, "app/Sink", "sink", "()V", "", -1, -1)
            );

            List<FlowPath> before = new GraphDfsEngine().run(snapshot, FlowOptions.builder()
                    .fromSink(true)
                    .searchAllSources(true)
                    .onlyFromWeb(true)
                    .depth(4)
                    .sink("app/Sink", "sink", "()V")
                    .build(), null).results();
            assertTrue(hasSource(before, "app/Controller", "entry"));

            Files.writeString(source, "{\"sourceAnnotations\":[],\"sourceModel\":[]}", StandardCharsets.UTF_8);
            ModelRegistry.reload();

            List<FlowPath> after = new GraphDfsEngine().run(snapshot, FlowOptions.builder()
                    .fromSink(true)
                    .searchAllSources(true)
                    .onlyFromWeb(true)
                    .depth(4)
                    .sink("app/Sink", "sink", "()V")
                    .build(), null).results();
            assertFalse(hasSource(after, "app/Controller", "entry"));
        } finally {
            RuleRegistryTestSupport.clearRuleFiles();
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

    private static boolean hasSource(List<FlowPath> results, String className, String methodName) {
        if (results == null || results.isEmpty()) {
            return false;
        }
        for (FlowPath result : results) {
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
}
