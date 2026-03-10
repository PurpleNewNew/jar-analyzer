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
import com.alibaba.fastjson2.JSONObject;
import me.n1ar4.jar.analyzer.mcp.backend.JarAnalyzerApiInvoker;
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.storage.neo4j.procedure.ApocWhitelist;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jGraphSnapshotLoader;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jProjectStore;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectMetadataSnapshotStoreTestHook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.neo4j.graphdb.Label;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryApiHandlersTest {
    private final String originalProjectKey = ActiveProjectContext.getPublishedActiveProjectKey();
    private final String originalProjectAlias = ActiveProjectContext.getPublishedActiveProjectAlias();
    private final String originalApocWhitelist = System.getProperty(ApocWhitelist.APOC_WHITELIST_PROP);
    private final List<String> projectKeys = new ArrayList<>();

    @AfterEach
    void cleanup() {
        ActiveProjectContext.setActiveProject(originalProjectKey, originalProjectAlias);
        if (originalApocWhitelist == null) {
            System.clearProperty(ApocWhitelist.APOC_WHITELIST_PROP);
        } else {
            System.setProperty(ApocWhitelist.APOC_WHITELIST_PROP, originalApocWhitelist);
        }
        for (String projectKey : projectKeys) {
            Neo4jProjectStore.getInstance().deleteProjectStore(projectKey);
            Neo4jGraphSnapshotLoader.invalidate(projectKey);
        }
        projectKeys.clear();
    }

    @Test
    void cypherEndpointShouldReturnUnifiedShape() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());
        String projectKey = prepareReadyProject();

        JSONObject cypherBody = new JSONObject();
        cypherBody.put("query", "MATCH (m:JANode)-[r]->(n:JANode) RETURN m, r, n LIMIT 5");
        String cypherOut = api.postJson("/api/query/cypher", cypherBody.toJSONString());
        JSONObject cypherJson = JSON.parseObject(cypherOut);
        assertEquals(true, cypherJson.getBoolean("ok"));
        assertTrue(cypherJson.getJSONObject("data").containsKey("columns"));
        assertTrue(cypherJson.getJSONObject("data").containsKey("rows"));
        assertTrue(cypherJson.containsKey("meta"));
    }

    @Test
    void cypherExplainAndCapabilitiesShouldWork() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());
        String projectKey = prepareReadyProject();

        JSONObject explainBody = new JSONObject();
        explainBody.put("query", "MATCH (m:Method)-[r]->(n) RETURN m, r, n LIMIT 3");
        String explainOut = api.postJson("/api/query/cypher/explain", explainBody.toJSONString());
        JSONObject explainJson = JSON.parseObject(explainOut);
        assertEquals(true, explainJson.getBoolean("ok"));
        assertTrue(explainJson.getJSONObject("data").containsKey("operators"));

        String capabilitiesOut = api.get("/api/query/cypher/capabilities", Map.of());
        JSONObject capabilitiesJson = JSON.parseObject(capabilitiesOut);
        assertEquals(true, capabilitiesJson.getBoolean("ok"));
        JSONObject data = capabilitiesJson.getJSONObject("data");
        assertEquals(true, data.getBoolean("readOnly"));
        assertTrue(data.containsKey("procedures"));
        assertTrue(data.containsKey("functions"));
        assertTrue(data.getJSONArray("profiles").contains("long-chain"));
        assertTrue(data.getJSONArray("options").contains("expandBudget"));
        assertTrue(data.getJSONArray("options").contains("pathBudget"));
        assertTrue(data.getJSONArray("options").contains("timeoutCheckInterval"));
        assertEquals("native-only", data.getString("procedureMode"));
        assertEquals("read-only-whitelist", data.getString("apocMode"));
        assertEquals("default", data.getString("apocWhitelistMode"));
        assertEquals(ApocWhitelist.APOC_WHITELIST_PROP, data.getString("apocWhitelistProperty"));
        assertTrue(data.getJSONArray("apocWhitelist").contains("apoc.text.join"));
        assertFalse(data.containsKey("legacyCompatibility"));
        assertTrue(data.containsKey("ruleValidation"));
        JSONObject ruleValidation = data.getJSONObject("ruleValidation");
        assertEquals(true, ruleValidation.getBoolean("ok"));
        assertTrue(ruleValidation.containsKey("modelSource"));
        assertTrue(ruleValidation.containsKey("sink"));
        assertTrue(data.getJSONArray("functions").contains("ja.isSink"));
        assertTrue(data.getJSONArray("functions").contains("ja.ruleValidation"));
        assertTrue(data.getJSONArray("functions").contains("ja.ruleValidationIssues"));
        assertTrue(data.getJSONArray("procedures").contains("ja.path.from_to_pruned"));
    }

    @Test
    void cypherEndpointsShouldRejectGetRequests() {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());
        String projectKey = prepareReadyProject();

        Exception cypher = assertThrows(Exception.class,
                () -> api.get("/api/query/cypher", Map.of("query", "MATCH (m:JANode) RETURN m LIMIT 1")));
        assertTrue(cypher.getMessage().contains("\"code\":\"method_not_allowed\""));

        Exception explain = assertThrows(Exception.class,
                () -> api.get("/api/query/cypher/explain", Map.of("query", "MATCH (m:JANode) RETURN m LIMIT 1")));
        assertTrue(explain.getMessage().contains("\"code\":\"method_not_allowed\""));
    }

    @Test
    void malformedJsonBodyShouldReturnInvalidRequestAcrossEndpoints() {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        Exception cypher = assertThrows(Exception.class,
                () -> api.postJson("/api/query/cypher", "{invalid-json"));
        assertTrue(cypher.getMessage().contains("\"code\":\"invalid_request\""));

        Exception explain = assertThrows(Exception.class,
                () -> api.postJson("/api/query/cypher/explain", "{invalid-json"));
        assertTrue(explain.getMessage().contains("\"code\":\"invalid_request\""));

        Exception register = assertThrows(Exception.class,
                () -> api.postJson("/api/projects/register", "{invalid-json"));
        assertTrue(register.getMessage().contains("\"code\":\"invalid_request\""));

        Exception projectSwitch = assertThrows(Exception.class,
                () -> api.postJson("/api/projects/switch", "{invalid-json"));
        assertTrue(projectSwitch.getMessage().contains("\"code\":\"invalid_request\""));
    }

    @Test
    void cypherShouldIgnoreWriteKeywordsInsideLiteralAndComment() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());
        String projectKey = prepareReadyProject();

        JSONObject literalBody = new JSONObject();
        literalBody.put("query",
                "MATCH (m:JANode) WHERE 'create merge set delete remove drop schema' CONTAINS 'create' RETURN m LIMIT 1");
        String literalOut = api.postJson("/api/query/cypher", literalBody.toJSONString());
        JSONObject literalJson = JSON.parseObject(literalOut);
        assertEquals(true, literalJson.getBoolean("ok"));

        JSONObject commentBody = new JSONObject();
        commentBody.put("query",
                "MATCH (m:JANode) /* create merge set delete remove drop schema */ RETURN m LIMIT 1");
        String commentOut = api.postJson("/api/query/cypher", commentBody.toJSONString());
        JSONObject commentJson = JSON.parseObject(commentOut);
        assertEquals(true, commentJson.getBoolean("ok"));
    }

    @Test
    void cypherShouldAllowReadPropertyNamedSet() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());
        String projectKey = prepareReadyProject();
        JSONObject body = new JSONObject();
        body.put("query", "MATCH (m:JANode) RETURN m.set LIMIT 1");
        String out = api.postJson("/api/query/cypher", body.toJSONString());
        JSONObject json = JSON.parseObject(out);
        assertEquals(true, json.getBoolean("ok"));
    }

    @Test
    void cypherShouldRejectSetWriteClause() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());
        String projectKey = prepareReadyProject();
        JSONObject body = new JSONObject();
        body.put("query", "MATCH (m:JANode) SET m.test_flag = 1 RETURN m LIMIT 1");
        Exception ex = assertThrows(Exception.class,
                () -> api.postJson("/api/query/cypher", body.toJSONString()));
        assertTrue(ex.getMessage().contains("\"code\":\"cypher_feature_not_supported\""));
    }

    @Test
    void cypherShouldRejectNonActiveProjectOverride() {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());
        String activeProjectKey = prepareReadyProject();
        String otherProjectKey = prepareReadyProject();
        ActiveProjectContext.setActiveProject(activeProjectKey, activeProjectKey);

        JSONObject body = new JSONObject();
        body.put("query", "MATCH (m:JANode) RETURN m LIMIT 1");
        body.put("projectKey", otherProjectKey);

        Exception ex = assertThrows(Exception.class,
                () -> api.postJson("/api/query/cypher", body.toJSONString()));
        assertTrue(ex.getMessage().contains("\"code\":\"project_switch_required\""));
    }

    @Test
    void cypherShouldExecuteNativeJaProceduresAndFunctions() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());
        String projectKey = prepareReadyProject();

        JSONObject pathBody = new JSONObject();
        pathBody.put("query",
                "CALL ja.path.shortest(1, 3, 4) " +
                        "YIELD path_id, node_ids, edge_ids, confidence, evidence " +
                        "RETURN path_id, node_ids, edge_ids, confidence, evidence");
        String pathOut = api.postJson("/api/query/cypher", pathBody.toJSONString());
        JSONObject pathJson = JSON.parseObject(pathOut);
        assertEquals(true, pathJson.getBoolean("ok"));
        assertEquals(1, pathJson.getJSONObject("data").getJSONArray("rows").size());
        assertEquals("1,2,3", pathJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).getString(1));
        assertEquals("11,12", pathJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).getString(2));

        JSONObject udfBody = new JSONObject();
        udfBody.put("query",
                "MATCH (m:JANode) " +
                        "RETURN m.node_id AS nodeId, " +
                        "ja.isSource(m) AS isSource, " +
                        "ja.isSink(m) AS isSink, " +
                        "ja.sinkKind(m) AS sinkKind, " +
                        "ja.ruleVersion() AS ruleVersion, " +
                        "ja.rulesFingerprint() AS rulesFingerprint, " +
                        "ja.ruleValidation().ok AS validationOk, " +
                        "size(ja.ruleValidationIssues('sink')) AS sinkIssueCount " +
                        "ORDER BY nodeId");
        String udfOut = api.postJson("/api/query/cypher", udfBody.toJSONString());
        JSONObject udfJson = JSON.parseObject(udfOut);
        assertEquals(true, udfJson.getBoolean("ok"));
        assertEquals(Boolean.TRUE, udfJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).get(1));
        assertEquals(Boolean.FALSE, udfJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).get(2));
        assertEquals(Boolean.TRUE, udfJson.getJSONObject("data").getJSONArray("rows").getJSONArray(2).get(2));
        assertEquals("rce", udfJson.getJSONObject("data").getJSONArray("rows").getJSONArray(2).getString(3));
        Object ruleVersion = udfJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).get(4);
        assertTrue(ruleVersion instanceof Number && ((Number) ruleVersion).longValue() > 0L);
        assertTrue(!udfJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).getString(5).isBlank());
        assertEquals(Boolean.TRUE, udfJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).get(6));
        Object sinkIssueCount = udfJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).get(7);
        assertTrue(sinkIssueCount instanceof Number);

        JSONObject explainBody = new JSONObject();
        explainBody.put("query",
                "CALL ja.path.shortest(1, 3, 4) " +
                        "YIELD path_id, node_ids, edge_ids, confidence, evidence " +
                        "RETURN path_id, node_ids, edge_ids, confidence, evidence");
        String explainOut = api.postJson("/api/query/cypher/explain", explainBody.toJSONString());
        JSONObject explainJson = JSON.parseObject(explainOut);
        assertEquals(true, explainJson.getBoolean("ok"));
        assertEquals("neo4j", explainJson.getJSONObject("data").getString("engine"));
    }

    @Test
    void ruleValidationEndpointShouldReturnUnifiedSummary() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        JSONObject json = JSON.parseObject(api.get("/api/security/rule-validation", Map.of()));
        assertEquals(true, json.getBoolean("ok"));
        JSONObject data = json.getJSONObject("data");
        assertTrue(data.containsKey("ok"));
        assertTrue(data.containsKey("model"));
        assertTrue(data.containsKey("source"));
        assertTrue(data.containsKey("modelSource"));
        assertTrue(data.containsKey("sink"));
        assertTrue(data.containsKey("issues"));
    }

    @Test
    void ruleValidationEndpointShouldRejectInvalidScope() {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());
        Exception ex = assertThrows(Exception.class,
                () -> api.get("/api/security/rule-validation", Map.of("scope", "bogus")));
        assertTrue(ex.getMessage().contains("\"code\":\"rule_validation_scope_invalid\""));
    }

    @Test
    void cypherShouldExecuteWhitelistedApocReadOnlyFunctions() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());
        String projectKey = prepareReadyProject();

        JSONObject body = new JSONObject();
        body.put("query",
                "RETURN apoc.text.join(['a','b'], '-') AS joined, " +
                        "apoc.coll.toSet([1,1,2]) AS dedup, " +
                        "apoc.map.fromPairs([['k',1],['v',2]]) AS mapped, " +
                        "apoc.text.clean('Ä-b 9') AS cleaned");
        JSONObject json = JSON.parseObject(api.postJson("/api/query/cypher", body.toJSONString()));
        assertEquals(true, json.getBoolean("ok"));
        assertEquals("a-b", json.getJSONObject("data").getJSONArray("rows").getJSONArray(0).getString(0));
        assertEquals("[1,2]", json.getJSONObject("data").getJSONArray("rows").getJSONArray(0).getJSONArray(1).toJSONString());
        JSONObject mapped = json.getJSONObject("data").getJSONArray("rows").getJSONArray(0).getJSONObject(2);
        assertEquals(1, mapped.getIntValue("k"));
        assertEquals(2, mapped.getIntValue("v"));
        assertEquals("aeb9", json.getJSONObject("data").getJSONArray("rows").getJSONArray(0).getString(3));
    }

    @Test
    void cypherShouldRejectDisabledApocFunctions() throws Exception {
        System.setProperty(ApocWhitelist.APOC_WHITELIST_PROP, "none");
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());
        String projectKey = prepareReadyProject();

        JSONObject capabilities = JSON.parseObject(api.get("/api/query/cypher/capabilities", Map.of()));
        assertEquals("off", capabilities.getJSONObject("data").getString("apocWhitelistMode"));
        assertEquals(0, capabilities.getJSONObject("data").getJSONArray("apocWhitelist").size());

        JSONObject body = new JSONObject();
        body.put("query", "RETURN apoc.text.join(['a','b'], '-') AS joined");
        Exception ex = assertThrows(Exception.class,
                () -> api.postJson("/api/query/cypher", body.toJSONString()));
        assertTrue(ex.getMessage().contains("\"code\":\"cypher_feature_not_supported\""));
        assertTrue(ex.getMessage().contains("apoc function not allowed: apoc.text.join"));
    }

    @Test
    void cypherShouldHonorCustomApocWhitelist() throws Exception {
        System.setProperty(ApocWhitelist.APOC_WHITELIST_PROP, "apoc.text.join");
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());
        String projectKey = prepareReadyProject();

        JSONObject capabilities = JSON.parseObject(api.get("/api/query/cypher/capabilities", Map.of()));
        assertEquals("custom", capabilities.getJSONObject("data").getString("apocWhitelistMode"));
        assertEquals(1, capabilities.getJSONObject("data").getJSONArray("apocWhitelist").size());
        assertEquals("apoc.text.join", capabilities.getJSONObject("data").getJSONArray("apocWhitelist").getString(0));

        JSONObject joinBody = new JSONObject();
        joinBody.put("query", "RETURN apoc.text.join(['a','b'], '-') AS joined");
        JSONObject joinJson = JSON.parseObject(api.postJson("/api/query/cypher", joinBody.toJSONString()));
        assertEquals(true, joinJson.getBoolean("ok"));

        JSONObject blockedBody = new JSONObject();
        blockedBody.put("query", "RETURN apoc.map.fromPairs([['k',1]]) AS mapped");
        Exception ex = assertThrows(Exception.class,
                () -> api.postJson("/api/query/cypher", blockedBody.toJSONString()));
        assertTrue(ex.getMessage().contains("\"code\":\"cypher_feature_not_supported\""));
        assertTrue(ex.getMessage().contains("apoc function not allowed: apoc.map.fromPairs"));
    }

    @Test
    void legacyJaCallSyntaxShouldBeRejected() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());
        String projectKey = prepareReadyProject();

        JSONObject pathBody = new JSONObject();
        pathBody.put("query", "CALL ja.path.shortest(1, 3, 4) RETURN path_id, node_ids, edge_ids, confidence, evidence");
        Exception pathEx = assertThrows(Exception.class,
                () -> api.postJson("/api/query/cypher", pathBody.toJSONString()));
        assertTrue(pathEx.getMessage().contains("\"code\":\"cypher_query_invalid\""));

        JSONObject explainBody = new JSONObject();
        explainBody.put("query", "CALL ja.path.shortest(1, 3, 4) RETURN *");
        Exception explainEx = assertThrows(Exception.class,
                () -> api.postJson("/api/query/cypher/explain", explainBody.toJSONString()));
        assertTrue(explainEx.getMessage().contains("\"code\":\"cypher_query_invalid\""));
    }

    private String prepareReadyProject() {
        String projectKey = "query-api-" + Long.toHexString(System.nanoTime());
        projectKeys.add(projectKey);
        ProjectModel model = ProjectModel.artifact(
                Path.of("/tmp/jar-analyzer/" + projectKey + ".jar"),
                null,
                List.of(Path.of("/tmp/jar-analyzer/" + projectKey + ".jar")),
                false
        );
        ProjectRuntimeSnapshot snapshot = new ProjectRuntimeSnapshot(
                ProjectRuntimeSnapshot.CURRENT_SCHEMA_VERSION,
                9L,
                new ProjectRuntimeSnapshot.ProjectModelData(
                        model.buildMode().name(),
                        model.primaryInputPath().toString(),
                        "",
                        List.of(),
                        List.of(model.primaryInputPath().toString()),
                        false
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                java.util.Set.of(),
                java.util.Set.of(),
                java.util.Set.of(),
                java.util.Set.of()
        );
        ProjectMetadataSnapshotStoreTestHook.write(projectKey, snapshot);
        var database = Neo4jProjectStore.getInstance().database(projectKey);
        try (var tx = database.beginTx()) {
            tx.execute("MATCH (m:JAMeta {key:'build_meta'}) DETACH DELETE m");
            var meta = tx.createNode(Label.label("JAMeta"));
            meta.setProperty("key", "build_meta");
            meta.setProperty("build_seq", 9L);
            var source = tx.createNode(Label.label("JANode"), Label.label("Method"), Label.label("Source"), Label.label("SourceWeb"));
            source.setProperty("node_id", 1L);
            source.setProperty("kind", "method");
            source.setProperty("jar_id", 1);
            source.setProperty("class_name", "demo/Ready");
            source.setProperty("method_name", "run");
            source.setProperty("method_desc", "()V");
            source.setProperty("call_site_key", "");
            source.setProperty("line_number", -1);
            source.setProperty("call_index", -1);
            source.setProperty("source_flags", 3);

            var middle = tx.createNode(Label.label("JANode"), Label.label("Method"));
            middle.setProperty("node_id", 2L);
            middle.setProperty("kind", "method");
            middle.setProperty("jar_id", 1);
            middle.setProperty("class_name", "demo/Mid");
            middle.setProperty("method_name", "step");
            middle.setProperty("method_desc", "()V");
            middle.setProperty("call_site_key", "");
            middle.setProperty("line_number", -1);
            middle.setProperty("call_index", -1);
            middle.setProperty("source_flags", 0);

            var sink = tx.createNode(Label.label("JANode"), Label.label("Method"));
            sink.setProperty("node_id", 3L);
            sink.setProperty("kind", "method");
            sink.setProperty("jar_id", 1);
            sink.setProperty("class_name", "java/lang/Runtime");
            sink.setProperty("method_name", "exec");
            sink.setProperty("method_desc", "(Ljava/lang/String;)Ljava/lang/Process;");
            sink.setProperty("call_site_key", "");
            sink.setProperty("line_number", -1);
            sink.setProperty("call_index", -1);
            sink.setProperty("source_flags", 0);

            var firstEdge = source.createRelationshipTo(middle, () -> "CALLS_DIRECT");
            firstEdge.setProperty("edge_id", 11L);
            firstEdge.setProperty("confidence", "high");
            firstEdge.setProperty("evidence", "unit");
            firstEdge.setProperty("op_code", 0);

            var secondEdge = middle.createRelationshipTo(sink, () -> "CALLS_DIRECT");
            secondEdge.setProperty("edge_id", 12L);
            secondEdge.setProperty("confidence", "high");
            secondEdge.setProperty("evidence", "unit");
            secondEdge.setProperty("op_code", 0);
            tx.commit();
        }
        Neo4jGraphSnapshotLoader.invalidate(projectKey);
        ActiveProjectContext.setActiveProject(projectKey, projectKey);
        return projectKey;
    }
}
