package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.entity.MethodCallResult;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectRegistryService;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreRunnerBytecodeMainlineTest {
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
    void callbackFixtureShouldBuildWithBalancedBytecodeMainlineProfile() {
        System.setProperty(CallGraphPlan.CALL_GRAPH_PROFILE_PROP, CallGraphPlan.PROFILE_BALANCED);
        Path jar = FixtureJars.callbackTestJar();
        ProjectRuntimeContext.updateResolveInnerJars(false);

        CoreRunner.BuildResult result = CoreRunner.run(jar, null, false, null);
        assertNotNull(result);
        assertEquals(CallGraphPlan.ENGINE_BYTECODE_PTA, result.getCallGraphEngine());
        assertEquals(BytecodeMainlineCallGraphRunner.MODE_BALANCED_V1, result.getCallGraphMode());
        assertTrue(result.getEdgeCount() > 0L);

        CoreRunner.BuildStageMetric metric = result.getStageMetric("callgraph");
        assertNotNull(metric);
        Map<String, Object> details = metric.getDetails();
        assertEquals(CallGraphPlan.ENGINE_BYTECODE_PTA, details.get("engine"));
        assertTrue(((Number) details.get("direct_edges")).intValue() > 0);
        assertTrue(((Number) details.get("declared_dispatch_edges")).intValue() > 0);
        assertTrue(((Number) details.get("typed_dispatch_edges")).intValue() > 0);
        assertTrue(((Number) details.get("callback_edges")).intValue() > 0);
        assertTrue(((Number) details.get("reflection_edges")).intValue() > 0);
        assertTrue(((Number) details.get("method_handle_edges")).intValue() > 0);
        assertTrue(((Number) details.get("reflection_hint_const_sites")).intValue() > 0);
        assertTrue(((Number) details.get("reflection_hint_cast_sites")).intValue() > 0);
        assertTrue(((Number) details.get("reflection_hint_imprecise_sites")).intValue() > 0);
        assertTrue(((Number) details.get("reflection_hint_threshold_exceeded_sites")).intValue() > 0);
        assertTrue(((Number) details.get("pta_edges")).intValue() > 0);
        assertTrue(((Number) details.get("instantiated_classes")).intValue() > 0);

        CoreEngine engine = new CoreEngine(config());
        EngineContext.setEngine(engine);
        ArrayList<MethodCallResult> fieldDispatchEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "ptaFieldSensitiveDispatch",
                "()V",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> noiseDispatchEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "ptaNoiseInstantiate",
                "()V",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> arrayDispatchEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "ptaArraySensitiveDispatch",
                "()V",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> arrayCopyDispatchEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "ptaNativeArrayCopyDispatch",
                "()V",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> threadStartEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "threadStart",
                "()V",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> executorSubmitEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "executorSubmit",
                "()V",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> doPrivilegedEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "doPrivileged",
                "()Ljava/lang/Object;",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> completableEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "completableSupply",
                "()V",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> dynamicProxyEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "dynamicProxy",
                "()V",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> cglibProxyEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "cglibProxy",
                "()V",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> reflectInvokeEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "reflectInvoke",
                "()V",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> reflectLoaderChainEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "reflectWithClassLoaderChain",
                "()V",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> reflectLoadClassApiEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "reflectViaLoadClassApi",
                "()V",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> reflectHelperFlowEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "reflectViaHelperFlow",
                "()V",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> reflectFieldAliasEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "reflectViaFieldAliasFacts",
                "()V",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> reflectArrayCopyAliasEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "reflectViaStringArrayCopyAlias",
                "()V",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> methodHandleEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "methodHandleDirect",
                "()V",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> methodHandleStaticEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "methodHandleFindStatic",
                "()V",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> methodHandleCtorEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "methodHandleFindConstructor",
                "()V",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> methodHandleBindToEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "methodHandleBindToReceiver",
                "()V",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> methodHandleSpecialEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "methodHandleFindSpecial",
                "()V",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> methodHandleHelperFlowEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "methodHandleViaHelperFlow",
                "()V",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> indyStaticRefEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "invokeDynamicStaticMethodRef",
                "()V",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> indyCtorRefEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "invokeDynamicConstructorRef",
                "()V",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> reflectCastFallbackEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "reflectViaCastFallback",
                "(Ljava/lang/String;)V",
                0,
                Integer.MAX_VALUE
        );
        ArrayList<MethodCallResult> reflectImpreciseThresholdEdges = engine.getCallEdgesByCaller(
                "me/n1ar4/cb/CallbackEntry",
                "reflectViaImpreciseThreshold",
                "(Ljava/lang/String;)V",
                0,
                Integer.MAX_VALUE
        );
        assertFalse(fieldDispatchEdges.isEmpty());
        assertFalse(noiseDispatchEdges.isEmpty());
        assertFalse(arrayDispatchEdges.isEmpty());
        assertFalse(arrayCopyDispatchEdges.isEmpty());
        assertTrue(fieldDispatchEdges.stream().anyMatch(edge ->
                "me/n1ar4/cb/FastTask".equals(edge.getCalleeClassName())
                        && "run".equals(edge.getCalleeMethodName())
                        && "()V".equals(edge.getCalleeMethodDesc())));
        assertTrue(noiseDispatchEdges.stream().anyMatch(edge ->
                "me/n1ar4/cb/SlowTask".equals(edge.getCalleeClassName())
                        && "run".equals(edge.getCalleeMethodName())
                        && "()V".equals(edge.getCalleeMethodDesc())));
        MethodCallResult fieldPtaEdge = fieldDispatchEdges.stream()
                .filter(edge -> "me/n1ar4/cb/FastTask".equals(edge.getCalleeClassName())
                        && "run".equals(edge.getCalleeMethodName())
                        && "()V".equals(edge.getCalleeMethodDesc())
                        && "pta".equals(edge.getEdgeType())
                        && edge.getEdgeEvidence() != null
                        && edge.getEdgeEvidence().contains("field")
                        && edge.getEdgeEvidence().contains("pta_ctx="))
                .findFirst()
                .orElse(null);
        assertNotNull(fieldPtaEdge);
        assertTrue(fieldPtaEdge.getCallSiteKey() != null && !fieldPtaEdge.getCallSiteKey().isBlank());
        MethodCallResult arrayPtaEdge = arrayDispatchEdges.stream()
                .filter(edge -> "me/n1ar4/cb/FastTask".equals(edge.getCalleeClassName())
                        && "run".equals(edge.getCalleeMethodName())
                        && "()V".equals(edge.getCalleeMethodDesc())
                        && "pta".equals(edge.getEdgeType())
                        && edge.getEdgeEvidence() != null
                        && edge.getEdgeEvidence().contains("array")
                        && edge.getEdgeEvidence().contains("pta_ctx="))
                .findFirst()
                .orElse(null);
        assertNotNull(arrayPtaEdge);
        assertTrue(arrayPtaEdge.getCallSiteKey() != null && !arrayPtaEdge.getCallSiteKey().isBlank());
        MethodCallResult arrayCopyPtaEdge = arrayCopyDispatchEdges.stream()
                .filter(edge -> "me/n1ar4/cb/FastTask".equals(edge.getCalleeClassName())
                        && "run".equals(edge.getCalleeMethodName())
                        && "()V".equals(edge.getCalleeMethodDesc())
                        && "pta".equals(edge.getEdgeType())
                        && edge.getEdgeEvidence() != null
                        && edge.getEdgeEvidence().contains("arraycopy")
                        && edge.getEdgeEvidence().contains("pta_ctx="))
                .findFirst()
                .orElse(null);
        assertNotNull(arrayCopyPtaEdge);
        assertTrue(arrayCopyPtaEdge.getCallSiteKey() != null && !arrayCopyPtaEdge.getCallSiteKey().isBlank());
        assertEdge(threadStartEdges, "me/n1ar4/cb/MyThread", "run", "()V", "thread_start");
        assertEdge(executorSubmitEdges, "me/n1ar4/cb/MyRunnable", "run", "()V", "executor_runnable");
        assertEdge(doPrivilegedEdges, "me/n1ar4/cb/MyAction", "run", "()Ljava/lang/Object;", "do_privileged");
        assertEdge(completableEdges, "me/n1ar4/cb/MySupplier", "get", "()Ljava/lang/Object;", "completable_future_supplier");
        assertEdge(dynamicProxyEdges,
                "me/n1ar4/cb/MyInvocationHandler",
                "invoke",
                "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;",
                "dynamic_proxy_jdk");
        assertEdge(cglibProxyEdges,
                "me/n1ar4/cb/MyCglibInterceptor",
                "intercept",
                "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;Lorg/springframework/cglib/proxy/MethodProxy;)Ljava/lang/Object;",
                "dynamic_proxy_cglib");
        assertEdge(reflectInvokeEdges, "me/n1ar4/cb/ReflectionTarget", "target", "()V", "class_class_const");
        assertEdge(reflectLoaderChainEdges, "me/n1ar4/cb/ReflectionTarget", "target", "()V", "for_name");
        assertEdge(reflectLoadClassApiEdges, "me/n1ar4/cb/ReflectionTarget", "target", "()V", "class_local+for_name");
        assertEdge(reflectHelperFlowEdges, "me/n1ar4/cb/ReflectionTarget", "target", "()V", "class_local+for_name");
        assertEdge(reflectFieldAliasEdges, "me/n1ar4/cb/ReflectionTarget", "target", "()V", "field_fact");
        assertEdge(reflectArrayCopyAliasEdges, "me/n1ar4/cb/ReflectionTarget", "target", "()V", "array_fact");
        assertEdge(reflectCastFallbackEdges, "me/n1ar4/cb/ReflectionTarget", "target", "()V", "tier=cast");
        assertEdge(methodHandleEdges, "me/n1ar4/cb/ReflectionTarget", "target", "()V", "method_handle");
        assertEdge(methodHandleStaticEdges, "me/n1ar4/cb/ReflectionTarget", "staticTarget", "()V", "method_handle_findstatic");
        assertEdge(methodHandleCtorEdges, "me/n1ar4/cb/ReflectionTarget", "<init>", "()V", "method_handle_findconstructor");
        assertEdge(methodHandleBindToEdges, "me/n1ar4/cb/ReflectionTarget", "target", "()V", "method_handle_findvirtual");
        assertEdge(methodHandleSpecialEdges, "me/n1ar4/cb/ReflectionTarget", "specialTarget", "()V", "method_handle_findspecial");
        assertEdge(methodHandleHelperFlowEdges, "me/n1ar4/cb/ReflectionTarget", "target", "()V", "method_handle");
        assertEdge(indyStaticRefEdges, "me/n1ar4/cb/ReflectionTarget", "staticTarget", "()V", "lambda_static_ref");
        assertEdge(indyCtorRefEdges, "me/n1ar4/cb/ReflectionTarget", "<init>", "()V", "lambda_constructor_ref");
        assertTrue(reflectImpreciseThresholdEdges.stream().noneMatch(edge ->
                "me/n1ar4/cb/ReflectionTarget".equals(edge.getCalleeClassName())
                        && "overloaded".equals(edge.getCalleeMethodName())
                        && "reflection".equals(edge.getEdgeType())));
    }

    @Test
    void frameworkFixtureShouldMergeFrameworkSemanticEvidenceIntoBalancedBytecodeEdges() {
        System.setProperty(CallGraphPlan.CALL_GRAPH_PROFILE_PROP, CallGraphPlan.PROFILE_BALANCED);
        Path jar = FixtureJars.frameworkStackTestJar();
        ProjectRuntimeContext.updateResolveInnerJars(false);

        CoreRunner.BuildResult result = CoreRunner.run(jar, null, false, null);
        assertNotNull(result);
        assertEquals(CallGraphPlan.ENGINE_BYTECODE_PTA, result.getCallGraphEngine());
        assertEquals(BytecodeMainlineCallGraphRunner.MODE_BALANCED_V1, result.getCallGraphMode());

        CoreRunner.BuildStageMetric metric = result.getStageMetric("callgraph");
        assertNotNull(metric);
        assertTrue(metric.getDetails().containsKey("framework_edges"));
        assertTrue(metric.getDetails().containsKey("framework_caller_candidates"));
        assertTrue(metric.getDetails().containsKey("framework_truncated_rules"));

        CoreEngine engine = new CoreEngine(config());
        EngineContext.setEngine(engine);
        ArrayList<MethodCallResult> nettyDispatchEdges = engine.getCallEdgesByCaller(
                "io/netty/channel/SimpleChannelInboundHandler",
                "channelRead",
                "(Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Object;)V",
                0,
                Integer.MAX_VALUE
        );
        assertFalse(nettyDispatchEdges.isEmpty());
        assertTrue(nettyDispatchEdges.stream().anyMatch(edge ->
                "fixture/framework/netty/AuthHandler".equals(edge.getCalleeClassName())
                        && "channelRead0".equals(edge.getCalleeMethodName())
                        && "(Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Object;)V".equals(edge.getCalleeMethodDesc())
                        && edge.getEdgeEvidence() != null
                        && edge.getEdgeEvidence().contains("framework:medium:netty_dispatch")));
    }

    private static ConfigFile config() {
        ConfigFile config = new ConfigFile();
        config.setDbPath("test-db");
        return config;
    }

    private static void assertEdge(List<MethodCallResult> edges,
                                   String calleeClass,
                                   String calleeMethod,
                                   String calleeDesc,
                                   String evidenceToken) {
        assertNotNull(edges);
        assertTrue(edges.stream().anyMatch(edge ->
                        calleeClass.equals(edge.getCalleeClassName())
                                && calleeMethod.equals(edge.getCalleeMethodName())
                                && calleeDesc.equals(edge.getCalleeMethodDesc())
                                && (evidenceToken == null
                                || (edge.getEdgeEvidence() != null && edge.getEdgeEvidence().contains(evidenceToken)))),
                () -> "missing edge to " + calleeClass + "#" + calleeMethod + calleeDesc + " with evidence " + evidenceToken);
    }
}
