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
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.rules.ModelRegistry;
import me.n1ar4.jar.analyzer.rules.SinkRuleRegistry;
import me.n1ar4.jar.analyzer.taint.TaintResult;
import me.n1ar4.support.PruningPolicyFixture;
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

class GraphPruningPolicyTest {
    private static final String MODEL_PROP = "jar.analyzer.rules.model.path";
    private static final String SOURCE_PROP = "jar.analyzer.rules.source.path";
    private static final String SINK_PROP = "jar.analyzer.rules.sink.path";

    @TempDir
    Path tempDir;

    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
        ProjectRuntimeContext.clear();
        SinkRuleRegistry.reload();
        ModelRegistry.reload();
    }

    @Test
    void sourceSelectionRulesShouldIgnorePersistedGraphFlagsUntilRuleAppears() throws Exception {
        Path model = tempDir.resolve("model.json");
        Path source = tempDir.resolve("source.json");
        Path sink = tempDir.resolve("sink.json");

        String oldModelProp = System.getProperty(MODEL_PROP);
        String oldSourceProp = System.getProperty(SOURCE_PROP);
        String oldSinkProp = System.getProperty(SINK_PROP);
        try {
            Files.writeString(model, modelJson("""
                    {
                      "sourceSelection": "rules"
                    }
                    """), StandardCharsets.UTF_8);
            Files.writeString(source, "{\"sourceAnnotations\":[],\"sourceModel\":[]}", StandardCharsets.UTF_8);
            Files.writeString(sink, "{\"name\":\"pruning-test\",\"levels\":{}}", StandardCharsets.UTF_8);

            System.setProperty(MODEL_PROP, model.toString());
            System.setProperty(SOURCE_PROP, source.toString());
            System.setProperty(SINK_PROP, sink.toString());
            SinkRuleRegistry.reload();
            ModelRegistry.reload();

            restoreRuntime(
                    method("app/Controller", "entry", "()V", Set.of()),
                    method("app/Sink", "sink", "()V", Set.of())
            );

            GraphSnapshot snapshot = sourceSelectionSnapshot();
            FlowOptions options = FlowOptions.builder()
                    .fromSink(true)
                    .searchAllSources(true)
                    .onlyFromWeb(true)
                    .depth(3)
                    .sink("app/Sink", "sink", "()V")
                    .build();

            List<TaintResult> before = new GraphTaintEngine().track(snapshot, options, null).results();
            assertTrue(before.isEmpty());

            Files.writeString(source,
                    "{\"sourceAnnotations\":[],\"sourceModel\":[{\"className\":\"app/Controller\",\"methodName\":\"entry\",\"methodDesc\":\"()V\",\"kind\":\"web\"}]}",
                    StandardCharsets.UTF_8);
            ModelRegistry.reload();

            List<TaintResult> after = new GraphTaintEngine().track(snapshot, options, null).results();
            assertTrue(hasSource(after, "app/Controller", "entry"));
        } finally {
            restore(MODEL_PROP, oldModelProp);
            restore(SOURCE_PROP, oldSourceProp);
            restore(SINK_PROP, oldSinkProp);
        }
    }

    @Test
    void scenarioSourceSelectionShouldOnlyApplyForMatchingBackwardAllSourcesSinkKind() throws Exception {
        Path model = tempDir.resolve("model.json");
        Path source = tempDir.resolve("source.json");
        Path sink = tempDir.resolve("sink.json");

        String oldModelProp = System.getProperty(MODEL_PROP);
        String oldSourceProp = System.getProperty(SOURCE_PROP);
        String oldSinkProp = System.getProperty(SINK_PROP);
        try {
            Files.writeString(model, modelJson("""
                    {
                      "sourceSelection": "merged",
                      "scenarios": [
                        {
                          "name": "xss-live-sources",
                          "sinkKinds": ["xss"],
                          "mode": "sink",
                          "searchAllSources": true,
                          "sourceSelection": "rules"
                        }
                      ]
                    }
                    """), StandardCharsets.UTF_8);
            Files.writeString(source, "{\"sourceAnnotations\":[],\"sourceModel\":[]}", StandardCharsets.UTF_8);
            Files.writeString(sink, "{\"name\":\"pruning-test\",\"levels\":{}}", StandardCharsets.UTF_8);

            System.setProperty(MODEL_PROP, model.toString());
            System.setProperty(SOURCE_PROP, source.toString());
            System.setProperty(SINK_PROP, sink.toString());
            SinkRuleRegistry.reload();
            ModelRegistry.reload();

            restoreRuntime(
                    method("app/Controller", "entry", "()V", Set.of()),
                    method("java/io/PrintWriter", "write", "(Ljava/lang/String;)V", Set.of()),
                    method("app/Sink", "sink", "()V", Set.of())
            );

            FlowOptions xssOptions = backwardAllSourcesOptions("java/io/PrintWriter", "write", "(Ljava/lang/String;)V");
            List<TaintResult> before = new GraphTaintEngine().track(
                    sourceSelectionSnapshot("java/io/PrintWriter", "write", "(Ljava/lang/String;)V"),
                    xssOptions,
                    null
            ).results();
            assertTrue(before.isEmpty());

            FlowOptions genericOptions = backwardAllSourcesOptions("app/Sink", "sink", "()V");
            List<TaintResult> generic = new GraphTaintEngine().track(
                    sourceSelectionSnapshot("app/Sink", "sink", "()V"),
                    genericOptions,
                    null
            ).results();
            assertTrue(hasSource(generic, "app/Controller", "entry"));

            Files.writeString(source,
                    "{\"sourceAnnotations\":[],\"sourceModel\":[{\"className\":\"app/Controller\",\"methodName\":\"entry\",\"methodDesc\":\"()V\",\"kind\":\"web\"}]}",
                    StandardCharsets.UTF_8);
            ModelRegistry.reload();

            List<TaintResult> after = new GraphTaintEngine().track(
                    sourceSelectionSnapshot("java/io/PrintWriter", "write", "(Ljava/lang/String;)V"),
                    xssOptions,
                    null
            ).results();
            assertTrue(hasSource(after, "app/Controller", "entry"));
        } finally {
            restore(MODEL_PROP, oldModelProp);
            restore(SOURCE_PROP, oldSourceProp);
            restore(SINK_PROP, oldSinkProp);
        }
    }

    @Test
    void scenarioSourceSelectionShouldHonorSinkTags() throws Exception {
        Path model = tempDir.resolve("model.json");
        Path source = tempDir.resolve("source.json");
        Path sink = tempDir.resolve("sink.json");

        String oldModelProp = System.getProperty(MODEL_PROP);
        String oldSourceProp = System.getProperty(SOURCE_PROP);
        String oldSinkProp = System.getProperty(SINK_PROP);
        try {
            Files.writeString(model, modelJson("""
                    {
                      "sourceSelection": "merged",
                      "scenarios": [
                        {
                          "name": "reflect-live-sources",
                          "sinkTags": ["reflect"],
                          "mode": "sink",
                          "searchAllSources": true,
                          "sourceSelection": "rules"
                        }
                      ]
                    }
                    """), StandardCharsets.UTF_8);
            Files.writeString(source, "{\"sourceAnnotations\":[],\"sourceModel\":[]}", StandardCharsets.UTF_8);
            Files.writeString(sink, sinkRuleJson("""
                    {
                      "high": {
                        "xss-reflect": [
                          {
                            "className": "demo/TaggedSink",
                            "methodName": "sink",
                            "methodDesc": "()V"
                          }
                        ]
                      }
                    }
                    """), StandardCharsets.UTF_8);

            System.setProperty(MODEL_PROP, model.toString());
            System.setProperty(SOURCE_PROP, source.toString());
            System.setProperty(SINK_PROP, sink.toString());
            SinkRuleRegistry.reload();
            ModelRegistry.reload();

            restoreRuntime(
                    method("app/Controller", "entry", "()V", Set.of()),
                    method("demo/TaggedSink", "sink", "()V", Set.of()),
                    method("demo/GenericSink", "sink", "()V", Set.of())
            );

            List<TaintResult> before = new GraphTaintEngine().track(
                    sourceSelectionSnapshot("demo/TaggedSink", "sink", "()V"),
                    backwardAllSourcesOptions("demo/TaggedSink", "sink", "()V"),
                    null
            ).results();
            assertTrue(before.isEmpty());

            List<TaintResult> generic = new GraphTaintEngine().track(
                    sourceSelectionSnapshot("demo/GenericSink", "sink", "()V"),
                    backwardAllSourcesOptions("demo/GenericSink", "sink", "()V"),
                    null
            ).results();
            assertTrue(hasSource(generic, "app/Controller", "entry"));

            Files.writeString(source,
                    "{\"sourceAnnotations\":[],\"sourceModel\":[{\"className\":\"app/Controller\",\"methodName\":\"entry\",\"methodDesc\":\"()V\",\"kind\":\"web\"}]}",
                    StandardCharsets.UTF_8);
            ModelRegistry.reload();

            List<TaintResult> after = new GraphTaintEngine().track(
                    sourceSelectionSnapshot("demo/TaggedSink", "sink", "()V"),
                    backwardAllSourcesOptions("demo/TaggedSink", "sink", "()V"),
                    null
            ).results();
            assertTrue(hasSource(after, "app/Controller", "entry"));
        } finally {
            restore(MODEL_PROP, oldModelProp);
            restore(SOURCE_PROP, oldSourceProp);
            restore(SINK_PROP, oldSinkProp);
        }
    }

    @Test
    void sanitizerModeIgnoreShouldKeepPrunedPath() throws Exception {
        Path model = tempDir.resolve("model.json");
        Path source = tempDir.resolve("source.json");
        Path sink = tempDir.resolve("sink.json");

        String oldModelProp = System.getProperty(MODEL_PROP);
        String oldSourceProp = System.getProperty(SOURCE_PROP);
        String oldSinkProp = System.getProperty(SINK_PROP);
        try {
            PruningPolicyFixture.FixtureData fixture = PruningPolicyFixture.installSanitizerFixture();
            Files.writeString(model, sanitizerModelJson("hard"), StandardCharsets.UTF_8);
            Files.writeString(source, "{\"sourceAnnotations\":[],\"sourceModel\":[]}", StandardCharsets.UTF_8);
            Files.writeString(sink, "{\"name\":\"pruning-test\",\"levels\":{}}", StandardCharsets.UTF_8);

            System.setProperty(MODEL_PROP, model.toString());
            System.setProperty(SOURCE_PROP, source.toString());
            System.setProperty(SINK_PROP, sink.toString());
            SinkRuleRegistry.reload();
            ModelRegistry.reload();

            FlowOptions options = explicitOptions(fixture);
            List<TaintResult> hard = new GraphTaintEngine().track(fixture.snapshot(), options, null).results();
            assertTrue(hard.isEmpty());

            Files.writeString(model, sanitizerModelJson("ignore"), StandardCharsets.UTF_8);
            ModelRegistry.reload();

            List<TaintResult> ignore = new GraphTaintEngine().track(fixture.snapshot(), options, null).results();
            assertEquals(1, ignore.size());
            assertTrue(ignore.get(0).isSuccess());
        } finally {
            restore(MODEL_PROP, oldModelProp);
            restore(SOURCE_PROP, oldSourceProp);
            restore(SINK_PROP, oldSinkProp);
        }
    }

    @Test
    void additionalFlowsFalseShouldCutContainerDerivedPath() throws Exception {
        Path model = tempDir.resolve("model.json");
        Path source = tempDir.resolve("source.json");
        Path sink = tempDir.resolve("sink.json");

        String oldModelProp = System.getProperty(MODEL_PROP);
        String oldSourceProp = System.getProperty(SOURCE_PROP);
        String oldSinkProp = System.getProperty(SINK_PROP);
        try {
            PruningPolicyFixture.FixtureData fixture = PruningPolicyFixture.installAdditionalFixture();
            Files.writeString(model, modelJson("""
                    {
                      "allowAdditionalFlows": true
                    }
                    """), StandardCharsets.UTF_8);
            Files.writeString(source, "{\"sourceAnnotations\":[],\"sourceModel\":[]}", StandardCharsets.UTF_8);
            Files.writeString(sink, "{\"name\":\"pruning-test\",\"levels\":{}}", StandardCharsets.UTF_8);

            System.setProperty(MODEL_PROP, model.toString());
            System.setProperty(SOURCE_PROP, source.toString());
            System.setProperty(SINK_PROP, sink.toString());
            SinkRuleRegistry.reload();
            ModelRegistry.reload();

            FlowOptions options = explicitOptions(fixture);
            List<TaintResult> allowed = new GraphTaintEngine().track(fixture.snapshot(), options, null).results();
            assertEquals(1, allowed.size());
            assertTrue(allowed.get(0).isSuccess());

            Files.writeString(model, modelJson("""
                    {
                      "allowAdditionalFlows": false
                    }
                    """), StandardCharsets.UTF_8);
            ModelRegistry.reload();

            List<TaintResult> blocked = new GraphTaintEngine().track(fixture.snapshot(), options, null).results();
            assertTrue(blocked.isEmpty());
        } finally {
            restore(MODEL_PROP, oldModelProp);
            restore(SOURCE_PROP, oldSourceProp);
            restore(SINK_PROP, oldSinkProp);
        }
    }

    @Test
    void confidenceModeStrictShouldRejectLowConfidenceFallbackPath() throws Exception {
        Path model = tempDir.resolve("model.json");
        Path source = tempDir.resolve("source.json");
        Path sink = tempDir.resolve("sink.json");

        String oldModelProp = System.getProperty(MODEL_PROP);
        String oldSourceProp = System.getProperty(SOURCE_PROP);
        String oldSinkProp = System.getProperty(SINK_PROP);
        try {
            Files.writeString(model, modelJson("""
                    {
                      "confidenceMode": "balanced"
                    }
                    """), StandardCharsets.UTF_8);
            Files.writeString(source, "{\"sourceAnnotations\":[],\"sourceModel\":[]}", StandardCharsets.UTF_8);
            Files.writeString(sink, "{\"name\":\"pruning-test\",\"levels\":{}}", StandardCharsets.UTF_8);

            System.setProperty(MODEL_PROP, model.toString());
            System.setProperty(SOURCE_PROP, source.toString());
            System.setProperty(SINK_PROP, sink.toString());
            SinkRuleRegistry.reload();
            ModelRegistry.reload();

            GraphSnapshot snapshot = lowConfidenceSnapshot();
            FlowOptions options = FlowOptions.builder()
                    .fromSink(false)
                    .depth(2)
                    .source("demo/Source", "entry", "()V")
                    .sink("demo/Sink", "sink", "()V")
                    .build();

            List<TaintResult> balanced = new GraphTaintEngine().track(snapshot, options, null).results();
            assertEquals(1, balanced.size());
            assertFalse(balanced.get(0).isSuccess());

            Files.writeString(model, modelJson("""
                    {
                      "confidenceMode": "strict"
                    }
                    """), StandardCharsets.UTF_8);
            ModelRegistry.reload();

            List<TaintResult> strict = new GraphTaintEngine().track(snapshot, options, null).results();
            assertTrue(strict.isEmpty());
        } finally {
            restore(MODEL_PROP, oldModelProp);
            restore(SOURCE_PROP, oldSourceProp);
            restore(SINK_PROP, oldSinkProp);
        }
    }

    @Test
    void scenarioConfidenceOverrideShouldOnlyTightenMatchingSinkKind() throws Exception {
        Path model = tempDir.resolve("model.json");
        Path source = tempDir.resolve("source.json");
        Path sink = tempDir.resolve("sink.json");

        String oldModelProp = System.getProperty(MODEL_PROP);
        String oldSourceProp = System.getProperty(SOURCE_PROP);
        String oldSinkProp = System.getProperty(SINK_PROP);
        try {
            Files.writeString(model, modelJson("""
                    {
                      "confidenceMode": "balanced",
                      "scenarios": [
                        {
                          "name": "sql-strict",
                          "sinkKinds": ["sql"],
                          "mode": "source",
                          "confidenceMode": "strict"
                        }
                      ]
                    }
                    """), StandardCharsets.UTF_8);
            Files.writeString(source, "{\"sourceAnnotations\":[],\"sourceModel\":[]}", StandardCharsets.UTF_8);
            Files.writeString(sink, "{\"name\":\"pruning-test\",\"levels\":{}}", StandardCharsets.UTF_8);

            System.setProperty(MODEL_PROP, model.toString());
            System.setProperty(SOURCE_PROP, source.toString());
            System.setProperty(SINK_PROP, sink.toString());
            SinkRuleRegistry.reload();
            ModelRegistry.reload();

            FlowOptions genericOptions = FlowOptions.builder()
                    .fromSink(false)
                    .depth(2)
                    .source("demo/Source", "entry", "()V")
                    .sink("demo/Sink", "sink", "()V")
                    .build();
            List<TaintResult> generic = new GraphTaintEngine().track(
                    lowConfidenceSnapshot("demo/Sink", "sink", "()V"),
                    genericOptions,
                    null
            ).results();
            assertEquals(1, generic.size());
            assertFalse(generic.get(0).isSuccess());

            FlowOptions sqlOptions = FlowOptions.builder()
                    .fromSink(false)
                    .depth(2)
                    .source("demo/Source", "entry", "()V")
                    .sink("java/sql/Statement", "execute", "(Ljava/lang/String;)Z")
                    .build();
            List<TaintResult> sql = new GraphTaintEngine().track(
                    lowConfidenceSnapshot("java/sql/Statement", "execute", "(Ljava/lang/String;)Z"),
                    sqlOptions,
                    null
            ).results();
            assertTrue(sql.isEmpty());
        } finally {
            restore(MODEL_PROP, oldModelProp);
            restore(SOURCE_PROP, oldSourceProp);
            restore(SINK_PROP, oldSinkProp);
        }
    }

    @Test
    void scenarioConfidenceOverrideShouldHonorSinkTier() throws Exception {
        Path model = tempDir.resolve("model.json");
        Path source = tempDir.resolve("source.json");
        Path sink = tempDir.resolve("sink.json");

        String oldModelProp = System.getProperty(MODEL_PROP);
        String oldSourceProp = System.getProperty(SOURCE_PROP);
        String oldSinkProp = System.getProperty(SINK_PROP);
        try {
            Files.writeString(model, modelJson("""
                    {
                      "confidenceMode": "balanced",
                      "scenarios": [
                        {
                          "name": "hard-only-strict",
                          "sinkTier": "hard",
                          "mode": "source",
                          "confidenceMode": "strict"
                        }
                      ]
                    }
                    """), StandardCharsets.UTF_8);
            Files.writeString(source, "{\"sourceAnnotations\":[],\"sourceModel\":[]}", StandardCharsets.UTF_8);
            Files.writeString(sink, sinkRuleJson("""
                    {
                      "high": {
                        "sql-injection": [
                          {
                            "className": "demo/HardSink",
                            "methodName": "sink",
                            "methodDesc": "()V"
                          }
                        ]
                      },
                      "low": {
                        "sql-observe": [
                          {
                            "className": "demo/SoftSink",
                            "methodName": "sink",
                            "methodDesc": "()V"
                          }
                        ]
                      }
                    }
                    """), StandardCharsets.UTF_8);

            System.setProperty(MODEL_PROP, model.toString());
            System.setProperty(SOURCE_PROP, source.toString());
            System.setProperty(SINK_PROP, sink.toString());
            SinkRuleRegistry.reload();
            ModelRegistry.reload();

            FlowOptions softOptions = FlowOptions.builder()
                    .fromSink(false)
                    .depth(2)
                    .source("demo/Source", "entry", "()V")
                    .sink("demo/SoftSink", "sink", "()V")
                    .build();
            List<TaintResult> soft = new GraphTaintEngine().track(
                    lowConfidenceSnapshot("demo/SoftSink", "sink", "()V"),
                    softOptions,
                    null
            ).results();
            assertEquals(1, soft.size());
            assertFalse(soft.get(0).isSuccess());

            FlowOptions hardOptions = FlowOptions.builder()
                    .fromSink(false)
                    .depth(2)
                    .source("demo/Source", "entry", "()V")
                    .sink("demo/HardSink", "sink", "()V")
                    .build();
            List<TaintResult> hard = new GraphTaintEngine().track(
                    lowConfidenceSnapshot("demo/HardSink", "sink", "()V"),
                    hardOptions,
                    null
            ).results();
            assertTrue(hard.isEmpty());
        } finally {
            restore(MODEL_PROP, oldModelProp);
            restore(SOURCE_PROP, oldSourceProp);
            restore(SINK_PROP, oldSinkProp);
        }
    }

    private static FlowOptions explicitOptions(PruningPolicyFixture.FixtureData fixture) {
        return FlowOptions.builder()
                .fromSink(false)
                .depth(4)
                .source(fixture.sourceClass(), fixture.sourceMethod(), fixture.sourceDesc())
                .sink(fixture.sinkClass(), fixture.sinkMethod(), fixture.sinkDesc())
                .build();
    }

    private static FlowOptions backwardAllSourcesOptions(String sinkClass, String sinkMethod, String sinkDesc) {
        return FlowOptions.builder()
                .fromSink(true)
                .searchAllSources(true)
                .onlyFromWeb(true)
                .depth(3)
                .sink(sinkClass, sinkMethod, sinkDesc)
                .build();
    }

    private static GraphSnapshot sourceSelectionSnapshot() {
        return sourceSelectionSnapshot("app/Sink", "sink", "()V");
    }

    private static GraphSnapshot sourceSelectionSnapshot(String sinkClass, String sinkMethod, String sinkDesc) {
        Map<Long, GraphNode> nodes = new LinkedHashMap<>();
        nodes.put(1L, new GraphNode(
                1L,
                "method",
                1,
                "app/Controller",
                "entry",
                "()V",
                "",
                -1,
                -1,
                GraphNode.SOURCE_FLAG_ANY | GraphNode.SOURCE_FLAG_WEB
        ));
        nodes.put(2L, new GraphNode(2L, "method", 1, sinkClass, sinkMethod, sinkDesc, "", -1, -1));
        Map<Long, List<GraphEdge>> outgoing = new LinkedHashMap<>();
        Map<Long, List<GraphEdge>> incoming = new LinkedHashMap<>();
        addEdge(outgoing, incoming, new GraphEdge(31L, 1L, 2L, "CALLS_DIRECT", "high", "unit", 0));
        return GraphSnapshot.of(61L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphSnapshot lowConfidenceSnapshot() {
        return lowConfidenceSnapshot("demo/Sink", "sink", "()V");
    }

    private static GraphSnapshot lowConfidenceSnapshot(String sinkClass, String sinkMethod, String sinkDesc) {
        Map<Long, GraphNode> nodes = new LinkedHashMap<>();
        nodes.put(1L, new GraphNode(1L, "method", 1, "demo/Source", "entry", "()V", "", -1, -1));
        nodes.put(2L, new GraphNode(2L, "method", 1, sinkClass, sinkMethod, sinkDesc, "", -1, -1));
        Map<Long, List<GraphEdge>> outgoing = new LinkedHashMap<>();
        Map<Long, List<GraphEdge>> incoming = new LinkedHashMap<>();
        addEdge(outgoing, incoming, new GraphEdge(41L, 1L, 2L, "CALLS_DIRECT", "low", "unit", 0));
        return GraphSnapshot.of(62L, nodes, outgoing, incoming, Map.of());
    }

    private static String modelJson(String pruningPolicyJson) {
        return "{\n"
                + "  \"summaryModel\": [],\n"
                + "  \"additionalTaintSteps\": [],\n"
                + "  \"sanitizerModel\": [],\n"
                + "  \"additionalStepHints\": [\"rules\", \"container\", \"array\", \"builder\", \"optional\", \"stream\"],\n"
                + "  \"pruningPolicy\": " + pruningPolicyJson.trim() + "\n"
                + "}";
    }

    private static String sanitizerModelJson(String sanitizerMode) {
        return "{\n"
                + "  \"summaryModel\": [],\n"
                + "  \"additionalTaintSteps\": [],\n"
                + "  \"sanitizerModel\": [\n"
                + "    {\n"
                + "      \"className\": \"" + PruningPolicyFixture.SanitizerBridge.class.getName().replace('.', '/') + "\",\n"
                + "      \"methodName\": \"cleanAndSink\",\n"
                + "      \"methodDesc\": \"(Ljava/lang/String;)V\",\n"
                + "      \"paramIndex\": 0,\n"
                + "      \"kind\": \"\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"additionalStepHints\": [\"rules\", \"container\", \"array\", \"builder\", \"optional\", \"stream\"],\n"
                + "  \"pruningPolicy\": {\n"
                + "    \"sanitizerMode\": \"" + sanitizerMode + "\"\n"
                + "  }\n"
                + "}";
    }

    private static String sinkRuleJson(String levelsJson) {
        return "{\n"
                + "  \"name\": \"pruning-test\",\n"
                + "  \"levels\": " + levelsJson.trim() + "\n"
                + "}";
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
                Path.of("/tmp/jar-analyzer/pruning-policy-test.jar"),
                null,
                List.of(Path.of("/tmp/jar-analyzer/pruning-policy-test.jar")),
                false
        );
        Set<MethodReference> refs = new LinkedHashSet<>();
        if (methods != null) {
            Collections.addAll(refs, methods);
        }
        ProjectRuntimeSnapshot snapshot = DatabaseManager.buildProjectRuntimeSnapshot(
                1L,
                model,
                List.of("/tmp/jar-analyzer/pruning-policy-test.jar"),
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

    private static void addEdge(Map<Long, List<GraphEdge>> outgoing,
                                Map<Long, List<GraphEdge>> incoming,
                                GraphEdge edge) {
        outgoing.computeIfAbsent(edge.getSrcId(), ignore -> new ArrayList<>()).add(edge);
        incoming.computeIfAbsent(edge.getDstId(), ignore -> new ArrayList<>()).add(edge);
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
        SinkRuleRegistry.reload();
        ModelRegistry.reload();
    }
}
