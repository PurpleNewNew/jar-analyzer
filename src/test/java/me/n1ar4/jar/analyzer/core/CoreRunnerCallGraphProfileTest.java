package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectRegistryService;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreRunnerCallGraphProfileTest {
    @AfterEach
    void cleanup() {
        System.clearProperty("jar.analyzer.callgraph.engine");
        System.clearProperty(CallGraphPlan.CALL_GRAPH_PROFILE_PROP);
        System.clearProperty("jar.analyzer.analysis.profile");
        GraphStore.invalidateCache();
        DatabaseManager.clearAllData();
        ProjectRuntimeContext.clear();
        ProjectRegistryService.getInstance().cleanupTemporaryProject();
        ProjectRegistryService.getInstance().activateTemporaryProject();
    }

    @Test
    void fastProfileShouldRouteToBytecodeMainline() {
        System.setProperty(CallGraphPlan.CALL_GRAPH_PROFILE_PROP, CallGraphPlan.PROFILE_FAST);
        Path jar = FixtureJars.callbackTestJar();
        ProjectRuntimeContext.updateResolveInnerJars(false);

        CoreRunner.BuildResult result = CoreRunner.run(jar, null, false, false, null);

        assertNotNull(result);
        assertEquals(CallGraphPlan.ENGINE_BYTECODE, result.getCallGraphEngine());
        assertEquals(BytecodeMainlineCallGraphRunner.MODE_FAST_V1, result.getCallGraphMode());
        assertEquals(CallGraphPlan.PROFILE_FAST, result.getAnalysisProfile());

        CoreRunner.BuildStageMetric metric = result.getStageMetric("taie_callgraph");
        assertNotNull(metric);
        Map<String, Object> details = metric.getDetails();
        assertEquals(CallGraphPlan.ENGINE_BYTECODE, details.get("engine"));
        assertEquals(CallGraphPlan.PROFILE_FAST, details.get("analysis_profile"));
        assertEquals(0, ((Number) details.get("pta_edges")).intValue());
    }

    @Test
    void oracleTaieProfileShouldUseOracleEngineMarker() {
        System.setProperty(CallGraphPlan.CALL_GRAPH_PROFILE_PROP, CallGraphPlan.PROFILE_ORACLE_TAIE);
        System.setProperty("jar.analyzer.analysis.profile", "fast");
        Path jar = FixtureJars.callbackTestJar();
        ProjectRuntimeContext.updateResolveInnerJars(false);

        CoreRunner.BuildResult result = CoreRunner.run(jar, null, false, false, null);

        assertNotNull(result);
        assertEquals(CallGraphPlan.ENGINE_ORACLE_TAIE, result.getCallGraphEngine());
        assertEquals("oracle-taie:fast", result.getCallGraphMode());
        assertEquals(CallGraphPlan.PROFILE_ORACLE_TAIE, result.getAnalysisProfile());
        assertTrue(result.getTaieReachableMethodCount() > 0);

        CoreRunner.BuildStageMetric metric = result.getStageMetric("taie_callgraph");
        assertNotNull(metric);
        assertEquals(CallGraphPlan.ENGINE_ORACLE_TAIE, metric.getDetails().get("engine"));
        assertEquals(CallGraphPlan.PROFILE_ORACLE_TAIE, metric.getDetails().get("analysis_profile"));
    }
}
