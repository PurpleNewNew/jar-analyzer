/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.server;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.core.reference.AnnoReference;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.mcp.backend.JarAnalyzerApiInvoker;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jGraphSnapshotLoader;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jProjectStore;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectMetadataSnapshotStoreTestHook;
import me.n1ar4.jar.analyzer.starter.Const;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiHandlerRegressionTest {
    private final String originalProjectKey = ActiveProjectContext.getPublishedActiveProjectKey();
    private final String originalProjectAlias = ActiveProjectContext.getPublishedActiveProjectAlias();
    private final List<String> projectKeys = new ArrayList<>();

    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
        EngineContext.setEngine(null);
        ActiveProjectContext.setActiveProject(originalProjectKey, originalProjectAlias);
        for (String projectKey : projectKeys) {
            Neo4jProjectStore.getInstance().deleteProjectStore(projectKey);
            Neo4jGraphSnapshotLoader.invalidate(projectKey);
        }
        projectKeys.clear();
    }

    @Test
    void methodsSearchShouldRespectJarIdForExactLookup() throws Exception {
        String projectKey = prepareProject(
                List.of(
                        new MethodSpec(1, "dup-a.jar", "demo/Dup", "run", "()V"),
                        new MethodSpec(2, "dup-b.jar", "demo/Dup", "run", "()V")
                ),
                List.of(),
                Map.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        JSONObject json = JSON.parseObject(withProject(projectKey, () ->
                api.get("/api/methods/search", Map.of(
                        "class", "demo.Dup",
                        "method", "run",
                        "desc", "()V",
                        "jarId", "2"
                ))));

        assertTrue(json.getBooleanValue("ok"));
        JSONArray data = json.getJSONArray("data");
        assertEquals(1, data.size());
        assertEquals(2, data.getJSONObject(0).getIntValue("jarId"));
        assertEquals("dup-b.jar", data.getJSONObject(0).getString("jarName"));
    }

    @Test
    void callgraphBySinkShouldSplitDuplicateSinkTargetsByJar() throws Exception {
        MethodSpec sinkA = new MethodSpec(1, "sink-a.jar", "demo/Sink", "run", "()V");
        MethodSpec sinkB = new MethodSpec(2, "sink-b.jar", "demo/Sink", "run", "()V");
        MethodSpec callerA = new MethodSpec(1, "sink-a.jar", "demo/CallerA", "reach", "()V");
        MethodSpec callerB = new MethodSpec(2, "sink-b.jar", "demo/CallerB", "reach", "()V");
        String projectKey = prepareProject(
                List.of(sinkA, sinkB, callerA, callerB),
                List.of(
                        new CallEdgeSpec(callerA, sinkA),
                        new CallEdgeSpec(callerB, sinkB)
                ),
                Map.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        JSONObject json = JSON.parseObject(withProject(projectKey, () ->
                api.get("/api/callgraph/by-sink", Map.of(
                        "sinkClass", "demo.Sink",
                        "sinkMethod", "run",
                        "sinkDesc", "()V"
                ))));

        assertTrue(json.getBooleanValue("ok"));
        JSONArray data = json.getJSONArray("data");
        assertEquals(2, data.size());

        JSONObject first = data.getJSONObject(0);
        JSONObject second = data.getJSONObject(1);
        assertEquals(1, first.getJSONObject("sink").getIntValue("jarId"));
        assertEquals(2, second.getJSONObject("sink").getIntValue("jarId"));
        assertEquals("demo/CallerA", first.getJSONArray("results").getJSONObject(0).getString("className"));
        assertEquals("demo/CallerB", second.getJSONArray("results").getJSONObject(0).getString("className"));
    }

    @Test
    void configUsageShouldWalkPastFirstCallerPageWhenSearchingEntrypoints() throws Exception {
        MethodSpec target = new MethodSpec(1, "config.jar", "demo/ConfigHolder", "read", "()V");
        List<MethodSpec> methods = new ArrayList<>();
        methods.add(target);
        List<CallEdgeSpec> edges = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            MethodSpec helper = new MethodSpec(1, "config.jar",
                    String.format("aa/Helper%03d", i), "call", "()V");
            methods.add(helper);
            edges.add(new CallEdgeSpec(helper, target));
        }
        MethodSpec servlet = new MethodSpec(1, "config.jar", "zz/ServletEntry", "doGet",
                "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V");
        methods.add(servlet);
        edges.add(new CallEdgeSpec(servlet, target));

        Map<MethodReference.Handle, List<String>> methodStrings = new LinkedHashMap<>();
        methodStrings.put(target.handle(), List.of("demo.key"));
        String projectKey = prepareProject(
                methods,
                edges,
                methodStrings,
                Set.of("zz/ServletEntry"),
                Set.of(),
                Set.of(),
                Set.of()
        );
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        JSONObject json = JSON.parseObject(withProject(projectKey, () ->
                api.get("/api/config/usage", Map.of(
                        "keys", "demo.key",
                        "includeResources", "false",
                        "maxDepth", "1",
                        "maxEntry", "1"
                ))));

        assertTrue(json.getBooleanValue("ok"));
        JSONArray data = json.getJSONArray("data");
        assertEquals(1, data.size());
        JSONArray usages = data.getJSONObject(0).getJSONArray("usages");
        assertEquals(1, usages.size());
        JSONArray entrypoints = usages.getJSONObject(0).getJSONArray("entrypoints");
        assertFalse(entrypoints.isEmpty());
        JSONObject entrypoint = entrypoints.getJSONObject(0);
        assertEquals("servlet", entrypoint.getString("type"));
        assertEquals("zz/ServletEntry", entrypoint.getString("className"));
    }

    @Test
    void dfsEndpointShouldRejectNonActiveProjectOverride() throws Exception {
        MethodSpec source = new MethodSpec(1, "flow.jar", "demo/Source", "entry", "()V");
        MethodSpec sink = new MethodSpec(1, "flow.jar", "demo/Sink", "sink", "()V");
        String activeProjectKey = prepareProject(
                List.of(source, sink),
                List.of(new CallEdgeSpec(source, sink)),
                Map.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
        String otherProjectKey = prepareProject(
                List.of(source, sink),
                List.of(new CallEdgeSpec(source, sink)),
                Map.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
        ActiveProjectContext.setActiveProject(activeProjectKey, activeProjectKey);

        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());
        Exception ex = assertThrows(Exception.class, () -> api.get("/api/flow/dfs", Map.of(
                "mode", "source",
                "sourceClass", "demo.Source",
                "sourceMethod", "entry",
                "sourceDesc", "()V",
                "sinkClass", "demo.Sink",
                "sinkMethod", "sink",
                "sinkDesc", "()V",
                "projectKey", otherProjectKey
        )));

        assertTrue(ex.getMessage().contains("\"code\":\"project_switch_required\""));
    }

    @Test
    void dfsCompletedJobResultsShouldRemainReadableAfterActiveProjectSwitch() throws Exception {
        MethodSpec source = new MethodSpec(1, "flow.jar", "demo/Source", "entry", "()V");
        MethodSpec sink = new MethodSpec(1, "flow.jar", "demo/Sink", "sink", "()V");
        String projectA = prepareProject(
                List.of(source, sink),
                List.of(new CallEdgeSpec(source, sink)),
                Map.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
        String projectB = prepareProject(
                List.of(source, sink),
                List.of(new CallEdgeSpec(source, sink)),
                Map.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        ActiveProjectContext.setActiveProject(projectA, projectA);
        JSONObject dfsJson = JSON.parseObject(api.get("/api/flow/dfs", Map.of(
                "mode", "source",
                "sourceClass", "demo.Source",
                "sourceMethod", "entry",
                "sourceDesc", "()V",
                "sinkClass", "demo.Sink",
                "sinkMethod", "sink",
                "sinkDesc", "()V"
        )));
        String dfsJobId = dfsJson.getJSONObject("data").getString("jobId");
        waitForJobDone(api, "/api/flow/dfs/jobs/", dfsJobId);

        ActiveProjectContext.setActiveProject(projectB, projectB);
        JSONObject statusJson = JSON.parseObject(api.get("/api/flow/dfs/jobs/" + dfsJobId, Map.of()));
        assertTrue(statusJson.getBooleanValue("ok"));
        assertEquals("done", statusJson.getJSONObject("data").getString("status"));
        assertTrue(statusJson.getJSONObject("data").getBooleanValue("stale"));
        assertEquals("project_switch_required", statusJson.getJSONObject("data").getString("staleReason"));

        JSONObject resultsJson = JSON.parseObject(api.get("/api/flow/dfs/jobs/" + dfsJobId + "/results", Map.of()));
        assertTrue(resultsJson.getBooleanValue("ok"));
        assertTrue(resultsJson.getJSONObject("data").getBooleanValue("stale"));
        assertEquals("project_switch_required", resultsJson.getJSONObject("data").getString("staleReason"));
        assertFalse(resultsJson.getJSONObject("data").getJSONArray("items").isEmpty());
    }

    @Test
    void dfsCompletedJobShouldRemainStaleAfterSwitchAwayAndBack() throws Exception {
        MethodSpec source = new MethodSpec(1, "flow.jar", "demo/Source", "entry", "()V");
        MethodSpec sink = new MethodSpec(1, "flow.jar", "demo/Sink", "sink", "()V");
        String projectA = prepareProject(
                List.of(source, sink),
                List.of(new CallEdgeSpec(source, sink)),
                Map.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
        String projectB = prepareProject(
                List.of(source, sink),
                List.of(new CallEdgeSpec(source, sink)),
                Map.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        ActiveProjectContext.setActiveProject(projectA, projectA);
        JSONObject dfsJson = JSON.parseObject(api.get("/api/flow/dfs", Map.of(
                "mode", "source",
                "sourceClass", "demo.Source",
                "sourceMethod", "entry",
                "sourceDesc", "()V",
                "sinkClass", "demo.Sink",
                "sinkMethod", "sink",
                "sinkDesc", "()V"
        )));
        String dfsJobId = dfsJson.getJSONObject("data").getString("jobId");
        waitForJobDone(api, "/api/flow/dfs/jobs/", dfsJobId);

        ActiveProjectContext.setActiveProject(projectB, projectB);
        ActiveProjectContext.setActiveProject(projectA, projectA);

        JSONObject statusJson = JSON.parseObject(api.get("/api/flow/dfs/jobs/" + dfsJobId, Map.of()));
        assertTrue(statusJson.getBooleanValue("ok"));
        assertTrue(statusJson.getJSONObject("data").getBooleanValue("stale"));
        assertEquals("project_switch_required", statusJson.getJSONObject("data").getString("staleReason"));

        JSONObject resultsJson = JSON.parseObject(api.get("/api/flow/dfs/jobs/" + dfsJobId + "/results", Map.of()));
        assertTrue(resultsJson.getBooleanValue("ok"));
        assertTrue(resultsJson.getJSONObject("data").getBooleanValue("stale"));
        assertEquals("project_switch_required", resultsJson.getJSONObject("data").getString("staleReason"));
    }

    @Test
    void taintEndpointShouldAcceptSinkModeDfsJobsWhenPathOrderIsForward() throws Exception {
        MethodSpec source = new MethodSpec(1, "flow.jar", "demo/Source", "entry", "()V");
        MethodSpec sink = new MethodSpec(1, "flow.jar", "demo/Sink", "sink", "()V");
        String projectKey = prepareProject(
                List.of(source, sink),
                List.of(new CallEdgeSpec(source, sink)),
                Map.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
        ActiveProjectContext.setActiveProject(projectKey, projectKey);
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        JSONObject dfsJson = JSON.parseObject(api.get("/api/flow/dfs", Map.of(
                "mode", "sink",
                "sourceClass", "demo.Source",
                "sourceMethod", "entry",
                "sourceDesc", "()V",
                "sinkClass", "demo.Sink",
                "sinkMethod", "sink",
                "sinkDesc", "()V"
        )));
        String dfsJobId = dfsJson.getJSONObject("data").getString("jobId");
        waitForJobDone(api, "/api/flow/dfs/jobs/", dfsJobId);

        JSONObject taintJson = JSON.parseObject(api.get("/api/flow/taint", Map.of(
                "dfsJobId", dfsJobId
        )));
        assertTrue(taintJson.getBooleanValue("ok"));
        String taintJobId = taintJson.getJSONObject("data").getString("jobId");
        waitForJobDone(api, "/api/flow/taint/jobs/", taintJobId);
    }

    @Test
    void dfsAndTaintShouldIgnoreRuntimeModelChangesWhenBuildSeqUnchanged() throws Exception {
        MethodSpec source = new MethodSpec(1, "flow.jar", "demo/Source", "entry", "()V");
        MethodSpec sink = new MethodSpec(1, "flow.jar", "demo/Sink", "sink", "()V");
        String projectKey = prepareProject(
                List.of(source, sink),
                List.of(new CallEdgeSpec(source, sink)),
                Map.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
        ActiveProjectContext.setActiveProject(projectKey, projectKey);
        Path projectJar = Path.of("/tmp/jar-analyzer/" + projectKey + ".jar");
        ProjectRuntimeContext.restoreProjectRuntime(
                projectKey,
                11L,
                ProjectModel.artifact(projectJar, null, List.of(projectJar), false)
        );
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        JSONObject dfsJson = JSON.parseObject(api.get("/api/flow/dfs", Map.of(
                "mode", "source",
                "sourceClass", "demo.Source",
                "sourceMethod", "entry",
                "sourceDesc", "()V",
                "sinkClass", "demo.Sink",
                "sinkMethod", "sink",
                "sinkDesc", "()V"
        )));
        String dfsJobId = dfsJson.getJSONObject("data").getString("jobId");
        assertEquals(11L, dfsJson.getJSONObject("data").getLongValue("buildSeq"));
        waitForJobDone(api, "/api/flow/dfs/jobs/", dfsJobId);

        ProjectRuntimeContext.replaceProjectModel(ProjectModel.artifact(projectJar, null, List.of(projectJar), true));

        JSONObject dfsStatus = JSON.parseObject(api.get("/api/flow/dfs/jobs/" + dfsJobId, Map.of()));
        assertTrue(dfsStatus.getBooleanValue("ok"));
        assertEquals("done", dfsStatus.getJSONObject("data").getString("status"));
        assertEquals(11L, dfsStatus.getJSONObject("data").getLongValue("buildSeq"));
        assertFalse(dfsStatus.getJSONObject("data").getBooleanValue("stale"));

        JSONObject dfsResults = JSON.parseObject(api.get("/api/flow/dfs/jobs/" + dfsJobId + "/results", Map.of()));
        assertTrue(dfsResults.getBooleanValue("ok"));
        assertEquals(11L, dfsResults.getJSONObject("data").getLongValue("buildSeq"));
        assertFalse(dfsResults.getJSONObject("data").getBooleanValue("stale"));

        JSONObject taintJson = JSON.parseObject(api.get("/api/flow/taint", Map.of(
                "dfsJobId", dfsJobId
        )));
        assertTrue(taintJson.getBooleanValue("ok"));
        assertEquals(11L, taintJson.getJSONObject("data").getLongValue("buildSeq"));
        String taintJobId = taintJson.getJSONObject("data").getString("jobId");
        waitForJobDone(api, "/api/flow/taint/jobs/", taintJobId);
    }

    @Test
    void taintEndpointShouldRejectDfsJobsFromNonActiveProject() throws Exception {
        MethodSpec source = new MethodSpec(1, "flow.jar", "demo/Source", "entry", "()V");
        MethodSpec sink = new MethodSpec(1, "flow.jar", "demo/Sink", "sink", "()V");
        String projectA = prepareProject(
                List.of(source, sink),
                List.of(new CallEdgeSpec(source, sink)),
                Map.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
        String projectB = prepareProject(
                List.of(source, sink),
                List.of(new CallEdgeSpec(source, sink)),
                Map.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        ActiveProjectContext.setActiveProject(projectA, projectA);
        JSONObject dfsJson = JSON.parseObject(api.get("/api/flow/dfs", Map.of(
                "mode", "source",
                "sourceClass", "demo.Source",
                "sourceMethod", "entry",
                "sourceDesc", "()V",
                "sinkClass", "demo.Sink",
                "sinkMethod", "sink",
                "sinkDesc", "()V"
        )));
        String dfsJobId = dfsJson.getJSONObject("data").getString("jobId");
        waitForJobDone(api, "/api/flow/dfs/jobs/", dfsJobId);

        ActiveProjectContext.setActiveProject(projectB, projectB);
        Exception ex = assertThrows(Exception.class, () -> api.get("/api/flow/taint", Map.of(
                "dfsJobId", dfsJobId
        )));
        assertTrue(ex.getMessage().contains("\"code\":\"project_switch_required\""));
    }

    @Test
    void taintCompletedJobResultsShouldRemainReadableAfterActiveProjectSwitch() throws Exception {
        MethodSpec source = new MethodSpec(1, "flow.jar", "demo/Source", "entry", "()V");
        MethodSpec sink = new MethodSpec(1, "flow.jar", "demo/Sink", "sink", "()V");
        String projectA = prepareProject(
                List.of(source, sink),
                List.of(new CallEdgeSpec(source, sink)),
                Map.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
        String projectB = prepareProject(
                List.of(source, sink),
                List.of(new CallEdgeSpec(source, sink)),
                Map.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
        ActiveProjectContext.setActiveProject(projectA, projectA);
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        JSONObject dfsJson = JSON.parseObject(api.get("/api/flow/dfs", Map.of(
                "mode", "source",
                "sourceClass", "demo.Source",
                "sourceMethod", "entry",
                "sourceDesc", "()V",
                "sinkClass", "demo.Sink",
                "sinkMethod", "sink",
                "sinkDesc", "()V"
        )));
        String dfsJobId = dfsJson.getJSONObject("data").getString("jobId");
        waitForJobDone(api, "/api/flow/dfs/jobs/", dfsJobId);

        JSONObject taintJson = JSON.parseObject(api.get("/api/flow/taint", Map.of(
                "dfsJobId", dfsJobId
        )));
        String taintJobId = taintJson.getJSONObject("data").getString("jobId");
        waitForJobDone(api, "/api/flow/taint/jobs/", taintJobId);

        ActiveProjectContext.setActiveProject(projectB, projectB);
        JSONObject statusJson = JSON.parseObject(api.get("/api/flow/taint/jobs/" + taintJobId, Map.of()));
        assertTrue(statusJson.getBooleanValue("ok"));
        assertEquals("done", statusJson.getJSONObject("data").getString("status"));
        assertTrue(statusJson.getJSONObject("data").getBooleanValue("stale"));
        assertEquals("project_switch_required", statusJson.getJSONObject("data").getString("staleReason"));

        JSONObject resultsJson = JSON.parseObject(api.get("/api/flow/taint/jobs/" + taintJobId + "/results", Map.of()));
        assertTrue(resultsJson.getBooleanValue("ok"));
        assertTrue(resultsJson.getJSONObject("data").getBooleanValue("stale"));
        assertEquals("project_switch_required", resultsJson.getJSONObject("data").getString("staleReason"));
    }

    @Test
    void taintCompletedJobShouldRemainStaleAfterSwitchAwayAndBack() throws Exception {
        MethodSpec source = new MethodSpec(1, "flow.jar", "demo/Source", "entry", "()V");
        MethodSpec sink = new MethodSpec(1, "flow.jar", "demo/Sink", "sink", "()V");
        String projectA = prepareProject(
                List.of(source, sink),
                List.of(new CallEdgeSpec(source, sink)),
                Map.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
        String projectB = prepareProject(
                List.of(source, sink),
                List.of(new CallEdgeSpec(source, sink)),
                Map.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
        ActiveProjectContext.setActiveProject(projectA, projectA);
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        JSONObject dfsJson = JSON.parseObject(api.get("/api/flow/dfs", Map.of(
                "mode", "source",
                "sourceClass", "demo.Source",
                "sourceMethod", "entry",
                "sourceDesc", "()V",
                "sinkClass", "demo.Sink",
                "sinkMethod", "sink",
                "sinkDesc", "()V"
        )));
        String dfsJobId = dfsJson.getJSONObject("data").getString("jobId");
        waitForJobDone(api, "/api/flow/dfs/jobs/", dfsJobId);

        JSONObject taintJson = JSON.parseObject(api.get("/api/flow/taint", Map.of(
                "dfsJobId", dfsJobId
        )));
        String taintJobId = taintJson.getJSONObject("data").getString("jobId");
        waitForJobDone(api, "/api/flow/taint/jobs/", taintJobId);

        ActiveProjectContext.setActiveProject(projectB, projectB);
        ActiveProjectContext.setActiveProject(projectA, projectA);

        JSONObject statusJson = JSON.parseObject(api.get("/api/flow/taint/jobs/" + taintJobId, Map.of()));
        assertTrue(statusJson.getBooleanValue("ok"));
        assertTrue(statusJson.getJSONObject("data").getBooleanValue("stale"));
        assertEquals("project_switch_required", statusJson.getJSONObject("data").getString("staleReason"));

        JSONObject resultsJson = JSON.parseObject(api.get("/api/flow/taint/jobs/" + taintJobId + "/results", Map.of()));
        assertTrue(resultsJson.getBooleanValue("ok"));
        assertTrue(resultsJson.getJSONObject("data").getBooleanValue("stale"));
        assertEquals("project_switch_required", resultsJson.getJSONObject("data").getString("staleReason"));
    }

    @Test
    void methodsSearchShouldNotMarkExactlyFullLastPageAsTruncated() throws Exception {
        List<MethodSpec> methods = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            methods.add(new MethodSpec(1, "page.jar", "demo/Page", "m" + i, "()V"));
        }
        String projectKey = prepareProject(
                methods,
                List.of(),
                Map.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        JSONObject json = JSON.parseObject(withProject(projectKey, () ->
                api.get("/api/methods/search", Map.of(
                        "class", "demo.Page",
                        "limit", "200"
                ))));

        assertTrue(json.getBooleanValue("ok"));
        JSONObject meta = json.getJSONObject("meta");
        assertEquals(200, meta.getIntValue("count"));
        assertEquals(200, meta.getIntValue("total"));
        assertFalse(meta.getBooleanValue("truncated"));
    }

    @Test
    void callgraphEdgesShouldNotMarkExactlyFullLastPageAsTruncated() throws Exception {
        MethodSpec sink = new MethodSpec(1, "edges.jar", "demo/Sink", "sink", "()V");
        List<MethodSpec> methods = new ArrayList<>();
        methods.add(sink);
        List<CallEdgeSpec> edges = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            MethodSpec caller = new MethodSpec(1, "edges.jar", "demo/Caller" + i, "reach", "()V");
            methods.add(caller);
            edges.add(new CallEdgeSpec(caller, sink));
        }
        String projectKey = prepareProject(
                methods,
                edges,
                Map.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        JSONObject json = JSON.parseObject(withProject(projectKey, () ->
                api.get("/api/callgraph/edges", Map.of(
                        "class", "demo.Sink",
                        "method", "sink",
                        "desc", "()V",
                        "limit", "200"
                ))));

        assertTrue(json.getBooleanValue("ok"));
        JSONObject meta = json.getJSONObject("meta");
        assertEquals(200, meta.getIntValue("count"));
        assertEquals(200, meta.getIntValue("total"));
        assertFalse(meta.getBooleanValue("truncated"));
    }

    @Test
    void methodsSearchStringShouldRemainTruncatedWhenExtraVisibleResultExistsAfterFilteredHit() throws Exception {
        MethodSpec appA = new MethodSpec(1, "search.jar", "demo/AppA", "first", "()V");
        MethodSpec jdk = new MethodSpec(1, "search.jar", "java/lang/String", "valueOf", "()V");
        MethodSpec appB = new MethodSpec(1, "search.jar", "demo/AppB", "second", "()V");
        Map<MethodReference.Handle, List<String>> methodStrings = new LinkedHashMap<>();
        methodStrings.put(appA.handle(), List.of("needle"));
        methodStrings.put(jdk.handle(), List.of("needle"));
        methodStrings.put(appB.handle(), List.of("needle"));
        String projectKey = prepareProject(
                List.of(appA, jdk, appB),
                List.of(),
                methodStrings,
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        JSONObject json = JSON.parseObject(withProject(projectKey, () ->
                api.get("/api/methods/search", Map.of(
                        "str", "needle",
                        "limit", "1"
                ))));

        assertTrue(json.getBooleanValue("ok"));
        JSONArray data = json.getJSONArray("data");
        assertEquals(1, data.size());
        assertEquals("demo/AppA", data.getJSONObject(0).getString("className"));
        JSONObject meta = json.getJSONObject("meta");
        assertEquals(2, meta.getIntValue("total"));
        assertTrue(meta.getBooleanValue("truncated"));
    }

    @Test
    void methodsSearchAnnoShouldReportVisibleTotalAndTruncation() throws Exception {
        String trackedAnno = "Ldemo/Tracked;";
        MethodSpec appA = new MethodSpec(1, "anno.jar", "demo/AnnoA", "first", "()V", Set.of(trackedAnno));
        MethodSpec jdk = new MethodSpec(1, "anno.jar", "java/lang/String", "valueOf", "()V", Set.of(trackedAnno));
        MethodSpec appB = new MethodSpec(1, "anno.jar", "demo/AnnoB", "second", "()V", Set.of(trackedAnno));
        String projectKey = prepareProject(
                List.of(appA, jdk, appB),
                List.of(),
                Map.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        JSONObject json = JSON.parseObject(withProject(projectKey, () ->
                api.get("/api/methods/search", Map.of(
                        "anno", "demo.Tracked",
                        "annoMatch", "equal",
                        "limit", "1"
                ))));

        assertTrue(json.getBooleanValue("ok"));
        JSONArray data = json.getJSONArray("data");
        assertEquals(1, data.size());
        assertEquals("demo/AnnoA", data.getJSONObject(0).getString("className"));
        JSONObject meta = json.getJSONObject("meta");
        assertEquals(2, meta.getIntValue("total"));
        assertTrue(meta.getBooleanValue("truncated"));
    }

    private String prepareProject(List<MethodSpec> methods,
                                  List<CallEdgeSpec> edges,
                                  Map<MethodReference.Handle, List<String>> methodStrings,
                                  Set<String> servlets,
                                  Set<String> filters,
                                  Set<String> listeners,
                                  Set<String> interceptors) {
        String projectKey = "api-regression-" + Long.toHexString(System.nanoTime());
        projectKeys.add(projectKey);

        LinkedHashMap<String, ClassReference> classes = new LinkedHashMap<>();
        LinkedHashSet<MethodReference> methodRefs = new LinkedHashSet<>();
        List<String> jarPaths = new ArrayList<>();
        LinkedHashMap<Integer, String> jarPathsById = new LinkedHashMap<>();
        for (MethodSpec method : methods) {
            if (method == null) {
                continue;
            }
            jarPathsById.putIfAbsent(method.jarId(), "/tmp/jar-analyzer/" + projectKey + "/" + method.jarName());
            classes.computeIfAbsent(
                    method.className() + "#" + method.jarId(),
                    ignore -> new ClassReference(
                            61,
                            1,
                            method.className(),
                            "java/lang/Object",
                            List.of(),
                            false,
                            List.of(),
                            Set.of(),
                            method.jarName(),
                            method.jarId()
                    )
            );
            methodRefs.add(new MethodReference(
                    new ClassReference.Handle(method.className(), method.jarId()),
                    method.methodName(),
                    method.methodDesc(),
                    false,
                    method.annotations().stream().map(AnnoReference::new).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)),
                    1,
                    10,
                    method.jarName(),
                    method.jarId()
            ));
        }
        for (Integer jarId : new java.util.TreeSet<>(jarPathsById.keySet())) {
            jarPaths.add(jarPathsById.get(jarId));
        }

        ProjectModel model = ProjectModel.artifact(
                Path.of("/tmp/jar-analyzer/" + projectKey + ".jar"),
                null,
                List.of(Path.of("/tmp/jar-analyzer/" + projectKey + ".jar")),
                false
        );
        ProjectRuntimeSnapshot snapshot = DatabaseManager.buildProjectRuntimeSnapshot(
                11L,
                model,
                jarPaths,
                Set.of(),
                new LinkedHashSet<>(classes.values()),
                methodRefs,
                methodStrings == null ? Map.of() : methodStrings,
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new ArrayList<>(interceptors),
                new ArrayList<>(servlets),
                new ArrayList<>(filters),
                new ArrayList<>(listeners)
        );
        DatabaseManager.restoreProjectRuntime(projectKey, snapshot);
        ProjectMetadataSnapshotStoreTestHook.write(projectKey, snapshot);
        writeGraph(projectKey, methods, edges);
        ConfigFile config = new ConfigFile();
        config.setDbPath(Neo4jProjectStore.getInstance().resolveProjectHome(projectKey).toString());
        config.setTempPath(Const.tempDir);
        config.setLang("en");
        config.setDecompileCacheSize("16");
        EngineContext.setEngine(new me.n1ar4.jar.analyzer.engine.CoreEngine(config));
        return projectKey;
    }

    private void writeGraph(String projectKey, List<MethodSpec> methods, List<CallEdgeSpec> edges) {
        var database = Neo4jProjectStore.getInstance().database(projectKey);
        Map<String, Long> nodeIds = new LinkedHashMap<>();
        long nextNodeId = 1L;
        long nextEdgeId = 1L;
        try (var tx = database.beginTx()) {
            tx.execute("MATCH (n) DETACH DELETE n");

            Node meta = tx.createNode(Label.label("JAMeta"));
            meta.setProperty("key", "build_meta");
            meta.setProperty("build_seq", 11L);

            for (MethodSpec method : methods) {
                if (method == null) {
                    continue;
                }
                long nodeId = nextNodeId++;
                Node node = tx.createNode(Label.label("JANode"));
                node.setProperty("node_id", nodeId);
                node.setProperty("kind", "method");
                node.setProperty("jar_id", method.jarId());
                node.setProperty("class_name", method.className());
                node.setProperty("method_name", method.methodName());
                node.setProperty("method_desc", method.methodDesc());
                node.setProperty("call_site_key", "");
                node.setProperty("line_number", -1);
                node.setProperty("call_index", -1);
                node.setProperty("source_flags", 0);
                nodeIds.put(method.key(), nodeId);
            }

            for (CallEdgeSpec edge : edges) {
                if (edge == null || edge.caller() == null || edge.callee() == null) {
                    continue;
                }
                Long srcId = nodeIds.get(edge.caller().key());
                Long dstId = nodeIds.get(edge.callee().key());
                if (srcId == null || dstId == null) {
                    continue;
                }
                Node src = tx.findNode(Label.label("JANode"), "node_id", srcId);
                Node dst = tx.findNode(Label.label("JANode"), "node_id", dstId);
                if (src == null || dst == null) {
                    continue;
                }
                var rel = src.createRelationshipTo(dst, RelationshipType.withName(edge.relType()));
                rel.setProperty("edge_id", nextEdgeId++);
                rel.setProperty("confidence", "high");
                rel.setProperty("evidence", "unit");
                rel.setProperty("op_code", 0);
            }
            tx.commit();
        }
        Neo4jGraphSnapshotLoader.invalidate(projectKey);
    }

    private static String withProject(String projectKey, ThrowingSupplier<String> supplier) throws Exception {
        try {
            return ActiveProjectContext.withProject(projectKey, () -> {
                try {
                    return supplier.get();
                } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
            });
        } catch (IllegalStateException ex) {
            if (ex.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw ex;
        }
    }

    private static void waitForJobDone(JarAnalyzerApiInvoker api, String prefix, String jobId) throws Exception {
        long deadline = System.currentTimeMillis() + 10_000L;
        while (System.currentTimeMillis() < deadline) {
            JSONObject json = JSON.parseObject(api.get(prefix + jobId, Map.of()));
            String status = json.getJSONObject("data").getString("status");
            if ("done".equalsIgnoreCase(status)) {
                return;
            }
            if ("failed".equalsIgnoreCase(status) || "canceled".equalsIgnoreCase(status)) {
                throw new AssertionError("job did not finish successfully: " + json);
            }
            Thread.sleep(50L);
        }
        throw new AssertionError("job did not finish before timeout: " + jobId);
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    private record MethodSpec(int jarId,
                              String jarName,
                              String className,
                              String methodName,
                              String methodDesc,
                              Set<String> annotations) {
        private MethodSpec(int jarId,
                           String jarName,
                           String className,
                           String methodName,
                           String methodDesc) {
            this(jarId, jarName, className, methodName, methodDesc, Set.of());
        }

        private String key() {
            return className + "#" + methodName + "#" + methodDesc + "#" + jarId;
        }

        private MethodReference.Handle handle() {
            return new MethodReference.Handle(
                    new ClassReference.Handle(className, jarId),
                    methodName,
                    methodDesc
            );
        }
    }

    private record CallEdgeSpec(MethodSpec caller, MethodSpec callee, String relType) {
        private CallEdgeSpec(MethodSpec caller, MethodSpec callee) {
            this(caller, callee, "CALLS_DIRECT");
        }
    }
}
