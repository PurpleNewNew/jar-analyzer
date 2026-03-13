package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.engine.model.CallEdgeView;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectRegistryService;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreRunnerCallGraphProfileTest {
    @AfterEach
    void cleanup() {
        System.clearProperty(CallGraphPlan.CALL_GRAPH_PROFILE_PROP);
        GraphStore.invalidateCache();
        EngineContext.setEngine(null);
        DatabaseManager.clearAllData();
        ProjectRuntimeContext.clear();
        ProjectRegistryService.getInstance().cleanupTemporaryProject();
        ProjectRegistryService.getInstance().activateTemporaryProject();
    }

    @Test
    void defaultBuildShouldUseBalancedBytecodeProfile() {
        Path jar = FixtureJars.callbackTestJar();
        ProjectRuntimeContext.updateResolveInnerJars(false);

        CoreRunner.BuildResult result = CoreRunner.run(jar, null, false, null);

        assertNotNull(result);
        assertEquals(CallGraphPlan.ENGINE_BYTECODE_PTA, result.getCallGraphEngine());
        assertEquals(BytecodeMainlineCallGraphRunner.MODE_BALANCED_V1, result.getCallGraphMode());
        assertEquals(CallGraphPlan.PROFILE_BALANCED, result.getAnalysisProfile());

        CoreRunner.BuildStageMetric metric = result.getStageMetric("callgraph");
        assertNotNull(metric);
        Map<String, Object> details = metric.getDetails();
        assertEquals(CallGraphPlan.ENGINE_BYTECODE_PTA, details.get("engine"));
        assertEquals(CallGraphPlan.PROFILE_BALANCED, details.get("analysis_profile"));
        assertTrue(((Number) details.get("pta_edges")).intValue() >= 0);
    }

    @Test
    void fastProfileShouldRouteToBytecodeMainline() {
        System.setProperty(CallGraphPlan.CALL_GRAPH_PROFILE_PROP, CallGraphPlan.PROFILE_FAST);
        Path jar = FixtureJars.callbackTestJar();
        ProjectRuntimeContext.updateResolveInnerJars(false);

        CoreRunner.BuildResult result = CoreRunner.run(jar, null, false, null);

        assertNotNull(result);
        assertEquals(CallGraphPlan.ENGINE_BYTECODE, result.getCallGraphEngine());
        assertEquals(BytecodeMainlineCallGraphRunner.MODE_FAST_V1, result.getCallGraphMode());
        assertEquals(CallGraphPlan.PROFILE_FAST, result.getAnalysisProfile());

        CoreRunner.BuildStageMetric metric = result.getStageMetric("callgraph");
        assertNotNull(metric);
        Map<String, Object> details = metric.getDetails();
        assertEquals(CallGraphPlan.ENGINE_BYTECODE, details.get("engine"));
        assertEquals(CallGraphPlan.PROFILE_FAST, details.get("analysis_profile"));
        assertEquals(0, ((Number) details.get("pta_edges")).intValue());
    }

    @Test
    void precisionProfileShouldUsePrecisionBudgetProfile() {
        System.setProperty(CallGraphPlan.CALL_GRAPH_PROFILE_PROP, CallGraphPlan.PROFILE_PRECISION);
        Path jar = FixtureJars.callbackTestJar();
        ProjectRuntimeContext.updateResolveInnerJars(false);

        CoreRunner.BuildResult result = CoreRunner.run(jar, null, false, null);

        assertNotNull(result);
        assertEquals(CallGraphPlan.ENGINE_BYTECODE_PTA, result.getCallGraphEngine());
        assertEquals(BytecodeMainlineCallGraphRunner.MODE_PRECISION_V1, result.getCallGraphMode());
        assertEquals(CallGraphPlan.PROFILE_PRECISION, result.getAnalysisProfile());

        CoreRunner.BuildStageMetric metric = result.getStageMetric("callgraph");
        assertNotNull(metric);
        Map<String, Object> details = metric.getDetails();
        assertEquals(CallGraphPlan.ENGINE_BYTECODE_PTA, details.get("engine"));
        assertEquals(CallGraphPlan.PROFILE_PRECISION, details.get("analysis_profile"));
        assertTrue((Boolean) details.get("precision_mode"));
        assertEquals("precision", details.get("pta_budget_profile"));
        assertTrue(((Number) details.get("pta_precision_selected_call_sites")).intValue() > 0);
        assertTrue(((Number) details.get("pta_precision_semantic_call_sites")).intValue() > 0);
    }

    @Test
    void balancedProfileShouldNotEscalateSemanticConcreteReceiverSite() {
        Path jar = FixtureJars.callbackTestJar();
        ProjectRuntimeContext.updateResolveInnerJars(false);

        CoreRunner.BuildResult result = CoreRunner.run(jar, null, false, null);

        assertNotNull(result);
        assertEquals(CallGraphPlan.PROFILE_BALANCED, result.getAnalysisProfile());
        GraphStore.invalidateCache();
        EngineContext.setEngine(null);
        CoreEngine engine = new CoreEngine(config());
        EngineContext.setEngine(engine);
        ArrayList<CallEdgeView> invokeEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/MyInvocationHandler",
                "invoke",
                "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;",
                0,
                Integer.MAX_VALUE
        );
        assertFalse(invokeEdges.isEmpty());
        assertFalse(invokeEdges.stream().anyMatch(edge ->
                "me/n1ar4/cb/FastTask".equals(edge.getCalleeClassName())
                        && "run".equals(edge.getCalleeMethodName())
                        && "()V".equals(edge.getCalleeMethodDesc())
                        && edge.getEdgeEvidence() != null
                        && edge.getEdgeEvidence().contains("pta_selector=semantic")));
    }

    @Test
    void precisionProfileShouldEscalateSemanticConcreteReceiverSite() {
        System.setProperty(CallGraphPlan.CALL_GRAPH_PROFILE_PROP, CallGraphPlan.PROFILE_PRECISION);
        Path jar = FixtureJars.callbackTestJar();
        ProjectRuntimeContext.updateResolveInnerJars(false);

        CoreRunner.BuildResult result = CoreRunner.run(jar, null, false, null);

        assertNotNull(result);
        assertEquals(CallGraphPlan.PROFILE_PRECISION, result.getAnalysisProfile());
        CoreRunner.BuildStageMetric metric = result.getStageMetric("callgraph");
        assertNotNull(metric);
        Map<String, Object> details = metric.getDetails();
        assertTrue(((Number) details.get("pta_precision_selected_call_sites")).intValue() > 0);
        assertTrue(((Number) details.get("pta_precision_semantic_call_sites")).intValue() > 0);

        GraphStore.invalidateCache();
        EngineContext.setEngine(null);
        CoreEngine engine = new CoreEngine(config());
        EngineContext.setEngine(engine);
        ArrayList<CallEdgeView> invokeEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/MyInvocationHandler",
                "invoke",
                "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;",
                0,
                Integer.MAX_VALUE
        );
        assertFalse(invokeEdges.isEmpty());
        assertTrue(invokeEdges.stream().anyMatch(edge ->
                "me/n1ar4/cb/FastTask".equals(edge.getCalleeClassName())
                        && "run".equals(edge.getCalleeMethodName())
                        && "()V".equals(edge.getCalleeMethodDesc())
                        && "pta".equals(edge.getEdgeType())
                        && edge.getEdgeEvidence() != null
                        && edge.getEdgeEvidence().contains("semantic")));
    }

    @Test
    void invalidProfileShouldFailFast() {
        System.setProperty(CallGraphPlan.CALL_GRAPH_PROFILE_PROP, "unsupported-profile");
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> CoreRunner.run(FixtureJars.callbackTestJar(), null, false, null)
        );
        assertTrue(ex.getMessage().contains("jar.analyzer.callgraph.profile"));
        assertTrue(ex.getMessage().contains("fast|balanced|precision"));
    }

    private static ConfigFile config() {
        ConfigFile config = new ConfigFile();
        config.setDbPath("test-db");
        return config;
    }
}
