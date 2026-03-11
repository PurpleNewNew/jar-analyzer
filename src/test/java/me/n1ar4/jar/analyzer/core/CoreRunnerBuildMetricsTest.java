package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jProjectStore;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectRegistryService;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Label;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreRunnerBuildMetricsTest {
    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
        ProjectRuntimeContext.clear();
        ProjectRegistryService.getInstance().cleanupTemporaryProject();
        ProjectRegistryService.getInstance().activateTemporaryProject();
    }

    @Test
    void buildShouldExposeStructuredStageMetricsAndPersistFlattenedMeta() {
        Path jar = FixtureJars.callbackTestJar();
        ProjectRuntimeContext.updateResolveInnerJars(false);

        CoreRunner.BuildResult result = CoreRunner.run(jar, null, false, false, null);
        assertNotNull(result);
        assertTrue(result.getBuildWallMs() > 0L);

        Map<String, CoreRunner.BuildStageMetric> metrics = result.getStageMetrics();
        assertNotNull(metrics);
        assertTrue(metrics.containsKey("resolve_inputs"));
        assertTrue(metrics.containsKey("prepare_class_files"));
        assertTrue(metrics.containsKey("discovery"));
        assertTrue(metrics.containsKey("class_analysis"));
        assertTrue(metrics.containsKey("framework_entry"));
        assertTrue(metrics.containsKey("method_semantic"));
        assertTrue(metrics.containsKey("bytecode_symbol"));
        assertTrue(metrics.containsKey("callgraph"));
        assertTrue(metrics.containsKey("build_runtime_snapshot"));
        assertTrue(metrics.containsKey("publish_runtime"));
        assertTrue(metrics.containsKey("refresh_caches"));
        assertTrue(metrics.containsKey("neo4j_commit"));

        CoreRunner.BuildStageMetric discovery = result.getStageMetric("discovery");
        CoreRunner.BuildStageMetric symbol = result.getStageMetric("bytecode_symbol");
        CoreRunner.BuildStageMetric callGraph = result.getStageMetric("callgraph");
        CoreRunner.BuildStageMetric commit = result.getStageMetric("neo4j_commit");
        assertNotNull(discovery);
        assertNotNull(symbol);
        assertNotNull(callGraph);
        assertNotNull(commit);
        assertEquals(result.getClassFileCount(), intMetric(discovery, "class_files"));
        assertEquals(result.getClassCount(), intMetric(discovery, "classes"));
        assertEquals(result.getMethodCount(), intMetric(discovery, "methods"));
        assertEquals(intMetric(symbol, "call_sites"), result.getCallSiteCount());
        assertTrue(intMetric(callGraph, "total_edges") >= 0);
        assertFalse(callGraph.getDetails().containsKey("oracle_edges"));
        assertEquals(result.getEdgeCount(), intMetric(commit, "edges"));
        assertTrue(result.getPeakHeapUsedBytes() > 0L);
        assertTrue(result.getPeakHeapCommittedBytes() >= result.getPeakHeapUsedBytes());
        assertTrue(result.getHeapMaxBytes() >= result.getPeakHeapCommittedBytes());
        assertTrue(result.getCallGraphMode() != null && !result.getCallGraphMode().isBlank());
        assertTrue(symbol.getDurationMs() >= 0L);
        assertTrue(callGraph.getDurationMs() >= 0L);
        assertTrue(commit.getDurationMs() >= 0L);
        assertTrue(discovery.getDetails().containsKey("heap_used_bytes"));
        assertTrue(callGraph.getDetails().containsKey("heap_committed_bytes"));

        var database = Neo4jProjectStore.getInstance().database(ActiveProjectContext.getActiveProjectKey());
        try (var tx = database.beginTx();
             var it = tx.findNodes(Label.label("JAMeta"), "key", "build_meta")) {
            assertTrue(it.hasNext(), "build_meta node should exist");
            var meta = it.next();
            assertEquals(result.getBuildWallMs(), ((Number) meta.getProperty("build_wall_ms")).longValue());
            assertEquals(result.getPeakHeapUsedBytes(), ((Number) meta.getProperty("build_peak_heap_used_bytes")).longValue());
            assertEquals(result.getPeakHeapCommittedBytes(), ((Number) meta.getProperty("build_peak_heap_committed_bytes")).longValue());
            assertEquals(result.getHeapMaxBytes(), ((Number) meta.getProperty("build_heap_max_bytes")).longValue());
            assertEquals(discovery.getDurationMs(), ((Number) meta.getProperty("build_stage_discovery_ms")).longValue());
            assertEquals(callGraph.getDurationMs(), ((Number) meta.getProperty("build_stage_callgraph_ms")).longValue());
            assertFalse(meta.hasProperty("build_stage_taie_callgraph_ms"));
            assertFalse(meta.hasProperty("oracle_edge_count"));
            assertFalse(meta.hasProperty("taie_edge_count"));
            assertEquals(commit.getDurationMs(), ((Number) meta.getProperty("build_stage_neo4j_commit_ms")).longValue());
            assertEquals(intMetric(symbol, "call_sites"),
                    ((Number) meta.getProperty("build_stage_bytecode_symbol_call_sites")).intValue());
            assertEquals(intMetric(symbol, "local_vars"),
                    ((Number) meta.getProperty("build_stage_bytecode_symbol_local_vars")).intValue());
            tx.commit();
        }
    }

    private static int intMetric(CoreRunner.BuildStageMetric metric, String key) {
        assertNotNull(metric);
        Object value = metric.getDetails().get(key);
        assertNotNull(value, "missing metric detail: " + key);
        return ((Number) value).intValue();
    }
}
