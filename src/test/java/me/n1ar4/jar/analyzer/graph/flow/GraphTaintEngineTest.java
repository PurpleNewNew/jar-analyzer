/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.flow;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.core.reference.AnnoReference;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.graph.flow.model.FlowPathEdge;
import me.n1ar4.jar.analyzer.graph.flow.model.FlowPath;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.rules.ModelRegistry;
import me.n1ar4.jar.analyzer.rules.RuleRegistryTestSupport;
import me.n1ar4.jar.analyzer.rules.SinkRuleRegistry;
import me.n1ar4.jar.analyzer.taint.TaintResult;
import me.n1ar4.support.PrunedFlowFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphTaintEngineTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
        ProjectRuntimeContext.clear();
        RuleRegistryTestSupport.clearRuleFiles();
    }

    @Test
    void trackShouldPreserveJarScopedSourceAndSinkSelection() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(1L, methodNode(1L, 1, "dup/Source", "entry", "()V"));
        nodes.put(2L, methodNode(2L, 2, "dup/Source", "entry", "()V"));
        nodes.put(3L, methodNode(3L, 1, "dup/Sink", "sink", "()V"));
        nodes.put(4L, methodNode(4L, 2, "dup/Sink", "sink", "()V"));

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        addEdge(outgoing, incoming, new GraphEdge(11L, 1L, 3L, "CALLS_DIRECT", "high", "unit", 0));
        addEdge(outgoing, incoming, new GraphEdge(12L, 2L, 4L, "CALLS_DIRECT", "high", "unit", 0));

        GraphSnapshot snapshot = GraphSnapshot.of(1L, nodes, outgoing, incoming, Map.of());
        FlowOptions options = FlowOptions.builder()
                .fromSink(false)
                .depth(4)
                .source("dup/Source", "entry", "()V")
                .sourceJarId(1)
                .sink("dup/Sink", "sink", "()V")
                .sinkJarId(1)
                .build();

        List<TaintResult> results = new GraphTaintEngine().track(snapshot, options, null).results();

        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getDfsResult().getSource().getJarId());
        assertEquals(1, results.get(0).getDfsResult().getSink().getJarId());
    }

    @Test
    void analyzeDfsResultsShouldPreserveOriginalModeWhenTruncated() {
        FlowPath dfs = new FlowPath();
        dfs.setMode(FlowPath.FROM_SINK_TO_SOURCE);
        dfs.setMethodList(List.of(methodHandle("demo/Sink", "sink", "()V", 1)));
        dfs.setEdges(List.of());

        List<TaintResult> results = new GraphTaintEngine()
                .analyzeDfsResults(List.of(dfs), 1000, 10, new AtomicBoolean(true), null)
                .results();

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getDfsResult().isTruncated());
        assertEquals(FlowPath.FROM_SINK_TO_SOURCE, results.get(0).getDfsResult().getMode());
    }

    @Test
    void analyzeDfsResultsShouldNotMarkImpreciseSegmentsAsVerified() {
        MethodReference.Handle source = methodHandle("demo/Source", "entry", "()V", 1);
        MethodReference.Handle sink = methodHandle("demo/Sink", "sink", "()V", 1);
        FlowPathEdge edge = new FlowPathEdge();
        edge.setFrom(source);
        edge.setTo(sink);
        edge.setType("reflection");
        edge.setConfidence("low");
        edge.setEvidence("unit");

        FlowPath dfs = new FlowPath();
        dfs.setMode(FlowPath.FROM_SOURCE_TO_SINK);
        dfs.setMethodList(List.of(source, sink));
        dfs.setEdges(List.of(edge));

        List<TaintResult> results = new GraphTaintEngine()
                .analyzeDfsResults(List.of(dfs), 1000, 10, new AtomicBoolean(false), null)
                .results();

        assertEquals(1, results.size());
        assertFalse(results.get(0).isSuccess());
        assertTrue(results.get(0).isLowConfidence());
        assertTrue(results.get(0).getTaintText().contains("taint path not fully proven"));
    }

    @Test
    void trackShouldUsePrunedSearchForExplicitSemanticPath() {
        PrunedFlowFixture.FixtureData fixture = PrunedFlowFixture.install();
        FlowOptions options = FlowOptions.builder()
                .fromSink(false)
                .searchAllSources(false)
                .depth(8)
                .source(fixture.sourceClass(), fixture.sourceMethod(), fixture.sourceDesc())
                .sink(fixture.sinkClass(), fixture.sinkMethod(), fixture.sinkDesc())
                .build();

        List<TaintResult> results = new GraphTaintEngine().track(fixture.snapshot(), options, null).results();

        assertEquals(1, results.size());
        assertTrue(results.get(0).isSuccess());
        assertTrue(results.get(0).getTaintText().contains("search backend: graph-pruned"));
        assertTrue(results.get(0).getTaintText().contains("taint path verified"));
        assertEquals(5, results.get(0).getDfsResult().getMethodList().size());
    }

    @Test
    void trackShouldUsePrunedSearchForBackwardSemanticPath() {
        PrunedFlowFixture.FixtureData fixture = PrunedFlowFixture.install();
        FlowOptions options = FlowOptions.builder()
                .fromSink(true)
                .searchAllSources(false)
                .depth(8)
                .source(fixture.sourceClass(), fixture.sourceMethod(), fixture.sourceDesc())
                .sink(fixture.sinkClass(), fixture.sinkMethod(), fixture.sinkDesc())
                .build();

        List<TaintResult> results = new GraphTaintEngine().track(fixture.snapshot(), options, null).results();

        assertEquals(1, results.size());
        assertTrue(results.get(0).isSuccess());
        assertEquals(FlowPath.FROM_SINK_TO_SOURCE, results.get(0).getDfsResult().getMode());
        assertTrue(results.get(0).getTaintText().contains("search backend: graph-pruned"));
        assertTrue(results.get(0).getTaintText().contains("taint path verified"));
        assertEquals(fixture.sourceClass(), results.get(0).getDfsResult().getSource().getClassReference().getName());
    }

    @Test
    void trackShouldUsePrunedSearchWhenSearchingAllSources() {
        PrunedFlowFixture.FixtureData fixture = PrunedFlowFixture.install();
        FlowOptions options = FlowOptions.builder()
                .fromSink(true)
                .searchAllSources(true)
                .depth(8)
                .sink(fixture.sinkClass(), fixture.sinkMethod(), fixture.sinkDesc())
                .build();

        List<TaintResult> results = new GraphTaintEngine().track(fixture.snapshot(), options, null).results();

        assertEquals(1, results.size());
        assertTrue(results.get(0).isSuccess());
        assertEquals(FlowPath.FROM_SOURCE_TO_ALL, results.get(0).getDfsResult().getMode());
        assertTrue(results.get(0).getTaintText().contains("search backend: graph-pruned"));
        assertEquals(fixture.sourceClass(), results.get(0).getDfsResult().getSource().getClassReference().getName());
        assertEquals(5, results.get(0).getDfsResult().getMethodList().size());
    }

    @Test
    void trackShouldHonorHotReloadedSourceModelWhenSearchingAllSources() throws Exception {
        Path model = tempDir.resolve("model.json");
        Path source = tempDir.resolve("source.json");
        Path sink = tempDir.resolve("sink.json");
        try {
            Files.writeString(model, "{\"summaryModel\":[],\"additionalStepHints\":[]}", StandardCharsets.UTF_8);
            Files.writeString(source, "{\"sourceAnnotations\":[],\"sourceModel\":[]}", StandardCharsets.UTF_8);
            Files.writeString(sink, "{\"name\":\"taint-test\",\"levels\":{}}", StandardCharsets.UTF_8);

            RuleRegistryTestSupport.useRuleFiles(model, source, sink);
            SinkRuleRegistry.reload();
            ModelRegistry.reload();

            restoreRuntime(
                    method("app/Helper", "helper", "(Ljava/lang/String;)V", Set.of()),
                    method("app/Controller", "entry", "(Ljava/lang/String;)V", Set.of()),
                    method("app/Sink", "sink", "(Ljava/lang/String;)V", Set.of())
            );

            GraphSnapshot snapshot = graphSnapshot(
                    new GraphNode(1L, "method", 1, "app/Helper", "helper", "(Ljava/lang/String;)V", "", -1, -1),
                    new GraphNode(2L, "method", 1, "app/Controller", "entry", "(Ljava/lang/String;)V", "", -1, -1),
                    new GraphNode(3L, "method", 1, "app/Sink", "sink", "(Ljava/lang/String;)V", "", -1, -1)
            );

            List<TaintResult> before = new GraphTaintEngine().track(snapshot, FlowOptions.builder()
                    .fromSink(true)
                    .searchAllSources(true)
                    .depth(4)
                    .sink("app/Sink", "sink", "(Ljava/lang/String;)V")
                    .build(), null).results();
            assertFalse(hasSource(before, "app/Controller", "entry"));

            Files.writeString(source,
                    "{\"sourceAnnotations\":[],\"sourceModel\":[{\"className\":\"app/Controller\",\"methodName\":\"entry\",\"methodDesc\":\"(Ljava/lang/String;)V\",\"kind\":\"custom\"}]}",
                    StandardCharsets.UTF_8);
            ModelRegistry.reload();

            List<TaintResult> after = new GraphTaintEngine().track(snapshot, FlowOptions.builder()
                    .fromSink(true)
                    .searchAllSources(true)
                    .depth(4)
                    .sink("app/Sink", "sink", "(Ljava/lang/String;)V")
                    .build(), null).results();
            assertTrue(hasSource(after, "app/Controller", "entry"));
            assertTrue(after.stream().anyMatch(result ->
                    result.getTaintText() != null && result.getTaintText().contains("search backend: graph-pruned")));
        } finally {
            RuleRegistryTestSupport.clearRuleFiles();
            SinkRuleRegistry.reload();
            ModelRegistry.reload();
        }
    }

    @Test
    void trackShouldHonorHotReloadedDslSourceRuleWhenSearchingAllSources() throws Exception {
        Path model = tempDir.resolve("model-dsl.json");
        Path source = tempDir.resolve("source-dsl.json");
        Path sink = tempDir.resolve("sink-dsl.json");
        try {
            Files.writeString(model, "{\"summaryModel\":[],\"additionalStepHints\":[]}", StandardCharsets.UTF_8);
            Files.writeString(source, "{\"sourceAnnotations\":[],\"sourceModel\":[],\"dsl\":{\"rules\":[]}}", StandardCharsets.UTF_8);
            Files.writeString(sink, "{\"name\":\"taint-test\",\"levels\":{}}", StandardCharsets.UTF_8);

            RuleRegistryTestSupport.useRuleFiles(model, source, sink);
            SinkRuleRegistry.reload();
            ModelRegistry.reload();

            restoreRuntime(
                    method("app/Helper", "helper", "(Ljava/lang/String;)V", Set.of()),
                    method("app/Controller", "entry", "(Ljava/lang/String;)V", Set.of()),
                    method("app/Sink", "sink", "(Ljava/lang/String;)V", Set.of())
            );

            GraphSnapshot snapshot = graphSnapshot(
                    new GraphNode(1L, "method", 1, "app/Helper", "helper", "(Ljava/lang/String;)V", "", -1, -1),
                    new GraphNode(2L, "method", 1, "app/Controller", "entry", "(Ljava/lang/String;)V", "", -1, -1),
                    new GraphNode(3L, "method", 1, "app/Sink", "sink", "(Ljava/lang/String;)V", "", -1, -1)
            );

            List<TaintResult> before = new GraphTaintEngine().track(snapshot, FlowOptions.builder()
                    .fromSink(true)
                    .searchAllSources(true)
                    .depth(4)
                    .sink("app/Sink", "sink", "(Ljava/lang/String;)V")
                    .build(), null).results();
            assertFalse(hasSource(before, "app/Controller", "entry"));

            Files.writeString(source, """
                    {
                      "sourceAnnotations": [],
                      "sourceModel": [],
                      "dsl": {
                        "rules": [
                          {
                            "id": "controller-source",
                            "kind": "source",
                            "match": {
                              "className": "app/Controller",
                              "methodName": "entry",
                              "methodDesc": "(Ljava/lang/String;)V"
                            },
                            "sourceKind": "custom"
                          }
                        ]
                      }
                    }
                    """, StandardCharsets.UTF_8);
            ModelRegistry.reload();

            List<TaintResult> after = new GraphTaintEngine().track(snapshot, FlowOptions.builder()
                    .fromSink(true)
                    .searchAllSources(true)
                    .depth(4)
                    .sink("app/Sink", "sink", "(Ljava/lang/String;)V")
                    .build(), null).results();
            assertTrue(hasSource(after, "app/Controller", "entry"));
            assertTrue(after.stream().anyMatch(result ->
                    result.getTaintText() != null && result.getTaintText().contains("search backend: graph-pruned")));
        } finally {
            RuleRegistryTestSupport.clearRuleFiles();
            SinkRuleRegistry.reload();
            ModelRegistry.reload();
        }
    }

    @Test
    void trackShouldDropRemovedWebSourceRulesWithoutRebuildEvenWhenGraphFlagsPersist() throws Exception {
        Path model = tempDir.resolve("model-web-remove.json");
        Path source = tempDir.resolve("source-web-remove.json");
        Path sink = tempDir.resolve("sink-web-remove.json");
        try {
            Files.writeString(model, "{\"summaryModel\":[],\"additionalStepHints\":[]}", StandardCharsets.UTF_8);
            Files.writeString(source,
                    "{\"sourceAnnotations\":[\"org.springframework.web.bind.annotation.GetMapping\"],\"sourceModel\":[]}",
                    StandardCharsets.UTF_8);
            Files.writeString(sink, "{\"name\":\"taint-test\",\"levels\":{}}", StandardCharsets.UTF_8);

            RuleRegistryTestSupport.useRuleFiles(model, source, sink);
            SinkRuleRegistry.reload();
            ModelRegistry.reload();

            restoreRuntime(
                    method("app/Helper", "helper", "(Ljava/lang/String;)V", Set.of()),
                    method("app/Controller", "entry", "(Ljava/lang/String;)V",
                            Set.of(new AnnoReference("Lorg/springframework/web/bind/annotation/GetMapping;"))),
                    method("app/Sink", "sink", "(Ljava/lang/String;)V", Set.of())
            );

            GraphSnapshot snapshot = graphSnapshot(
                    new GraphNode(1L, "method", 1, "app/Helper", "helper", "(Ljava/lang/String;)V", "", -1, -1),
                    new GraphNode(2L, "method", 1, "app/Controller", "entry", "(Ljava/lang/String;)V", "", -1, -1,
                            GraphNode.SOURCE_FLAG_ANY | GraphNode.SOURCE_FLAG_WEB),
                    new GraphNode(3L, "method", 1, "app/Sink", "sink", "(Ljava/lang/String;)V", "", -1, -1)
            );

            List<TaintResult> before = new GraphTaintEngine().track(snapshot, FlowOptions.builder()
                    .fromSink(true)
                    .searchAllSources(true)
                    .onlyFromWeb(true)
                    .depth(4)
                    .sink("app/Sink", "sink", "(Ljava/lang/String;)V")
                    .build(), null).results();
            assertTrue(hasSource(before, "app/Controller", "entry"));

            Files.writeString(source, "{\"sourceAnnotations\":[],\"sourceModel\":[]}", StandardCharsets.UTF_8);
            ModelRegistry.reload();

            List<TaintResult> after = new GraphTaintEngine().track(snapshot, FlowOptions.builder()
                    .fromSink(true)
                    .searchAllSources(true)
                    .onlyFromWeb(true)
                    .depth(4)
                    .sink("app/Sink", "sink", "(Ljava/lang/String;)V")
                    .build(), null).results();
            assertFalse(hasSource(after, "app/Controller", "entry"));
        } finally {
            RuleRegistryTestSupport.clearRuleFiles();
            SinkRuleRegistry.reload();
            ModelRegistry.reload();
        }
    }

    private static GraphNode methodNode(long id, int jarId, String clazz, String method, String desc) {
        return new GraphNode(id, "method", jarId, clazz, method, desc, "", -1, -1);
    }

    private static MethodReference.Handle methodHandle(String clazz, String method, String desc, int jarId) {
        return new MethodReference.Handle(new ClassReference.Handle(clazz, jarId), method, desc);
    }

    private static void addEdge(Map<Long, List<GraphEdge>> outgoing,
                                Map<Long, List<GraphEdge>> incoming,
                                GraphEdge edge) {
        outgoing.computeIfAbsent(edge.getSrcId(), ignore -> new ArrayList<>()).add(edge);
        incoming.computeIfAbsent(edge.getDstId(), ignore -> new ArrayList<>()).add(edge);
    }

    private static boolean hasSource(List<TaintResult> results, String className, String methodName) {
        if (results == null || results.isEmpty()) {
            return false;
        }
        for (TaintResult result : results) {
            if (result == null || result.getDfsResult() == null || result.getDfsResult().getSource() == null) {
                continue;
            }
            MethodReference.Handle source = result.getDfsResult().getSource();
            if (source.getClassReference() != null
                    && className.equals(source.getClassReference().getName())
                    && methodName.equals(source.getName())) {
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
                Path.of("/tmp/jar-analyzer/taint-test.jar"),
                null,
                List.of(Path.of("/tmp/jar-analyzer/taint-test.jar")),
                false
        );
        Set<MethodReference> refs = new LinkedHashSet<>();
        if (methods != null) {
            Collections.addAll(refs, methods);
        }
        ProjectRuntimeSnapshot snapshot = DatabaseManager.buildProjectRuntimeSnapshot(
                1L,
                model,
                List.of("/tmp/jar-analyzer/taint-test.jar"),
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
