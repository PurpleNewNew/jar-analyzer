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
        JSONObject firstNode = cypherJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).getJSONObject(0);
        assertEquals("[\"Method\"]", firstNode.getJSONArray("labels").toJSONString());
        JSONObject firstEdge = cypherJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).getJSONObject(1);
        assertEquals("CALL", firstEdge.getJSONObject("properties").getString("display_rel_type"));
        assertEquals("direct", firstEdge.getJSONObject("properties").getString("rel_subtype"));
    }

    @Test
    void cypherCapabilitiesShouldWork() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());
        String projectKey = prepareReadyProject();

        String capabilitiesOut = api.get("/api/query/cypher/capabilities", Map.of());
        JSONObject capabilitiesJson = JSON.parseObject(capabilitiesOut);
        assertEquals(true, capabilitiesJson.getBoolean("ok"));
        JSONObject data = capabilitiesJson.getJSONObject("data");
        assertEquals(true, data.getBoolean("readOnly"));
        assertTrue(data.containsKey("procedures"));
        assertTrue(data.containsKey("functions"));
        assertEquals("[\"maxRows\"]", data.getJSONArray("options").toJSONString());
        assertEquals("server-managed", data.getString("budgetMode"));
        assertEquals("native-only", data.getString("procedureMode"));
        assertEquals("read-only-whitelist", data.getString("apocMode"));
        assertEquals("default", data.getString("apocWhitelistMode"));
        assertEquals(ApocWhitelist.APOC_WHITELIST_PROP, data.getString("apocWhitelistProperty"));
        assertTrue(data.getJSONArray("apocWhitelist").contains("apoc.text.join"));
        assertFalse(data.containsKey("legacyCompatibility"));
        assertTrue(data.containsKey("ruleValidation"));
        assertTrue(data.containsKey("graphModel"));
        JSONObject graphModel = data.getJSONObject("graphModel");
        assertTrue(graphModel.getJSONArray("publicNodeLabels").contains("Method"));
        assertTrue(graphModel.getJSONArray("publicNodeLabels").contains("Class"));
        assertTrue(graphModel.getJSONArray("publicRelationGroups").contains("CALL"));
        assertTrue(graphModel.getJSONArray("publicRelationGroups").contains("ALIAS"));
        assertEquals("call-only", graphModel.getString("defaultTraversalMode"));
        assertEquals("dynamic-ja-functions", graphModel.getString("semanticMode"));
        assertEquals(true, graphModel.getBoolean("logicalCypherPatternRewrite"));
        assertEquals(true, graphModel.getBoolean("logicalCypherTypePredicateRewrite"));
        assertEquals(true, graphModel.getBoolean("logicalCypherParameterizedTypePredicateRewrite"));
        assertTrue(graphModel.getJSONArray("logicalCypherRelationshipTypes").contains("CALL"));
        assertTrue(graphModel.getJSONArray("logicalCypherRelationshipTypes").contains("ALIAS"));
        JSONObject ruleValidation = data.getJSONObject("ruleValidation");
        assertEquals(true, ruleValidation.getBoolean("ok"));
        assertTrue(ruleValidation.containsKey("modelSource"));
        assertTrue(ruleValidation.containsKey("sink"));
        assertTrue(data.getJSONArray("functions").contains("ja.isSink"));
        assertTrue(data.getJSONArray("functions").contains("ja.relGroup"));
        assertTrue(data.getJSONArray("functions").contains("ja.relSubtype"));
        assertTrue(data.getJSONArray("functions").contains("ja.ruleValidation"));
        assertTrue(data.getJSONArray("functions").contains("ja.ruleValidationIssues"));
        assertTrue(data.getJSONArray("procedures").contains("ja.gadget.track"));
        assertTrue(data.getJSONArray("procedures").contains("ja.path.gadget"));
        assertTrue(data.getJSONArray("procedures").contains("ja.path.from_to_pruned"));
    }

    @Test
    void cypherEndpointsShouldRejectGetRequests() {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());
        String projectKey = prepareReadyProject();

        Exception cypher = assertThrows(Exception.class,
                () -> api.get("/api/query/cypher", Map.of("query", "MATCH (m:JANode) RETURN m LIMIT 1")));
        assertTrue(cypher.getMessage().contains("\"code\":\"method_not_allowed\""));

    }

    @Test
    void malformedJsonBodyShouldReturnInvalidRequestAcrossEndpoints() {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        Exception cypher = assertThrows(Exception.class,
                () -> api.postJson("/api/query/cypher", "{invalid-json"));
        assertTrue(cypher.getMessage().contains("\"code\":\"invalid_request\""));

        Exception register = assertThrows(Exception.class,
                () -> api.postJson("/api/projects/register", "{invalid-json"));
        assertTrue(register.getMessage().contains("\"code\":\"invalid_request\""));

        Exception projectSwitch = assertThrows(Exception.class,
                () -> api.postJson("/api/projects/switch", "{invalid-json"));
        assertTrue(projectSwitch.getMessage().contains("\"code\":\"invalid_request\""));
    }

    @Test
    void projectsEndpointShouldExposeRegistryState() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());
        prepareReadyProject();

        JSONObject json = JSON.parseObject(api.get("/api/projects", Map.of()));

        assertEquals(true, json.getBoolean("ok"));
        JSONObject data = json.getJSONObject("data");
        assertTrue(data.containsKey("registryState"));
        assertTrue(data.containsKey("registryMessage"));
    }

    @Test
    void cypherShouldRejectRemovedQueryBudgetOptions() {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());
        String projectKey = prepareReadyProject();

        JSONObject body = new JSONObject();
        body.put("query", "MATCH (m:Method) RETURN m LIMIT 1");
        JSONObject options = new JSONObject();
        options.put("maxMs", 30000);
        body.put("options", options);

        Exception ex = assertThrows(Exception.class,
                () -> api.postJson("/api/query/cypher", body.toJSONString()));
        assertTrue(ex.getMessage().contains("\"code\":\"invalid_request\""));
        assertTrue(ex.getMessage().contains("unsupported query option"));
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

        JSONObject gadgetBody = new JSONObject();
        gadgetBody.put("query",
                "CALL ja.path.gadget(1, 3, 4, 10) " +
                        "YIELD path_id, node_ids, edge_ids, confidence, evidence " +
                        "RETURN path_id, node_ids, edge_ids, confidence, evidence");
        String gadgetOut = api.postJson("/api/query/cypher", gadgetBody.toJSONString());
        JSONObject gadgetJson = JSON.parseObject(gadgetOut);
        assertEquals(true, gadgetJson.getBoolean("ok"));
        assertEquals(0, gadgetJson.getJSONObject("data").getJSONArray("rows").size());

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
        assertEquals(Boolean.FALSE, udfJson.getJSONObject("data").getJSONArray("rows").getJSONArray(1).get(1));
        assertEquals(Boolean.FALSE, udfJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).get(2));
        assertEquals(Boolean.TRUE, udfJson.getJSONObject("data").getJSONArray("rows").getJSONArray(2).get(2));
        assertEquals("rce", udfJson.getJSONObject("data").getJSONArray("rows").getJSONArray(2).getString(3));
        Object ruleVersion = udfJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).get(4);
        assertTrue(ruleVersion instanceof Number && ((Number) ruleVersion).longValue() > 0L);
        assertTrue(!udfJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).getString(5).isBlank());
        assertEquals(Boolean.TRUE, udfJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).get(6));
        Object sinkIssueCount = udfJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).get(7);
        assertTrue(sinkIssueCount instanceof Number);

        JSONObject relBody = new JSONObject();
        relBody.put("query",
                "MATCH (:JANode)-[r]->(:JANode) " +
                        "RETURN ja.relGroup(type(r)) AS relGroup, ja.relSubtype(type(r)) AS relSubtype LIMIT 1");
        JSONObject relJson = JSON.parseObject(api.postJson("/api/query/cypher", relBody.toJSONString()));
        assertEquals(true, relJson.getBoolean("ok"));
        assertEquals("CALL", relJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).getString(0));
        assertEquals("direct", relJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).getString(1));

    }

    @Test
    void cypherNodeProjectionShouldExposeDynamicSemanticProperties() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());
        String projectKey = prepareReadyProject();

        JSONObject body = new JSONObject();
        body.put("query", "MATCH (m:Method) RETURN m ORDER BY m.node_id");
        JSONObject json = JSON.parseObject(api.postJson("/api/query/cypher", body.toJSONString()));
        assertEquals(true, json.getBoolean("ok"));
        JSONObject source = json.getJSONObject("data").getJSONArray("rows").getJSONArray(0).getJSONObject(0);
        JSONObject middle = json.getJSONObject("data").getJSONArray("rows").getJSONArray(1).getJSONObject(0);
        JSONObject sink = json.getJSONObject("data").getJSONArray("rows").getJSONArray(2).getJSONObject(0);
        assertEquals("[\"Method\"]", source.getJSONArray("labels").toJSONString());
        assertEquals(Boolean.TRUE, source.getJSONObject("properties").getBoolean("is_source"));
        assertEquals("[\"Rpc\"]", source.getJSONObject("properties").getJSONArray("source_badges").toJSONString());
        assertEquals(Boolean.FALSE, middle.getJSONObject("properties").getBoolean("is_source"));
        assertEquals(3, middle.getJSONObject("properties").getIntValue("source_flags"));
        assertEquals(Boolean.TRUE, sink.getJSONObject("properties").getBoolean("is_sink"));
        assertEquals("rce", sink.getJSONObject("properties").getString("sink_kind"));
    }

    @Test
    void cypherShouldSupportLogicalCallRelationshipTypeInPatterns() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());
        String projectKey = prepareReadyProject();

        JSONObject body = new JSONObject();
        body.put("query", "MATCH (m:Method)-[r:CALL]->(n:Method) RETURN m, r, n ORDER BY m.node_id");
        JSONObject json = JSON.parseObject(api.postJson("/api/query/cypher", body.toJSONString()));
        assertEquals(true, json.getBoolean("ok"));
        assertEquals(2, json.getJSONObject("data").getJSONArray("rows").size());
        JSONObject firstEdge = json.getJSONObject("data").getJSONArray("rows").getJSONArray(0).getJSONObject(1);
        assertEquals("CALL", firstEdge.getJSONObject("properties").getString("display_rel_type"));
        assertEquals("CALLS_DIRECT", firstEdge.getString("type"));

    }

    @Test
    void cypherShouldSupportLogicalTypePredicates() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());
        String projectKey = prepareReadyProject();

        JSONObject equalsBody = new JSONObject();
        equalsBody.put("query", "MATCH ()-[r]->() WHERE type(r) = 'CALL' RETURN count(r)");
        JSONObject equalsJson = JSON.parseObject(api.postJson("/api/query/cypher", equalsBody.toJSONString()));
        assertEquals(true, equalsJson.getBoolean("ok"));
        assertEquals(2, ((Number) equalsJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).get(0)).intValue());

        JSONObject inBody = new JSONObject();
        inBody.put("query", "MATCH ()-[r]->() WHERE type(r) IN ['CALL'] RETURN count(r)");
        JSONObject inJson = JSON.parseObject(api.postJson("/api/query/cypher", inBody.toJSONString()));
        assertEquals(true, inJson.getBoolean("ok"));
        assertEquals(2, ((Number) inJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).get(0)).intValue());

        JSONObject neqBody = new JSONObject();
        neqBody.put("query", "MATCH ()-[r]->() WHERE type(r) <> 'CALL' RETURN count(r)");
        JSONObject neqJson = JSON.parseObject(api.postJson("/api/query/cypher", neqBody.toJSONString()));
        assertEquals(true, neqJson.getBoolean("ok"));
        assertEquals(0, ((Number) neqJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).get(0)).intValue());

        JSONObject reverseEqualsBody = new JSONObject();
        reverseEqualsBody.put("query", "MATCH ()-[r]->() WHERE 'CALL' = type(r) RETURN count(r)");
        JSONObject reverseEqualsJson = JSON.parseObject(api.postJson("/api/query/cypher", reverseEqualsBody.toJSONString()));
        assertEquals(true, reverseEqualsJson.getBoolean("ok"));
        assertEquals(2, ((Number) reverseEqualsJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).get(0)).intValue());

        JSONObject reverseNeqBody = new JSONObject();
        reverseNeqBody.put("query", "MATCH ()-[r]->() WHERE 'CALL' <> type(r) RETURN count(r)");
        JSONObject reverseNeqJson = JSON.parseObject(api.postJson("/api/query/cypher", reverseNeqBody.toJSONString()));
        assertEquals(true, reverseNeqJson.getBoolean("ok"));
        assertEquals(0, ((Number) reverseNeqJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).get(0)).intValue());

        JSONObject physicalBody = new JSONObject();
        physicalBody.put("query", "MATCH ()-[r]->() RETURN type(r) AS relType ORDER BY relType LIMIT 1");
        JSONObject physicalJson = JSON.parseObject(api.postJson("/api/query/cypher", physicalBody.toJSONString()));
        assertEquals(true, physicalJson.getBoolean("ok"));
        assertEquals("CALLS_DIRECT", physicalJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).getString(0));
    }

    @Test
    void cypherShouldSupportParameterizedLogicalTypePredicates() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());
        String projectKey = prepareReadyProject();

        JSONObject equalsBody = new JSONObject();
        equalsBody.put("query", "MATCH ()-[r]->() WHERE type(r) = $relType RETURN count(r)");
        JSONObject equalsParams = new JSONObject();
        equalsParams.put("relType", "CALL");
        equalsBody.put("params", equalsParams);
        JSONObject equalsJson = JSON.parseObject(api.postJson("/api/query/cypher", equalsBody.toJSONString()));
        assertEquals(true, equalsJson.getBoolean("ok"));
        assertEquals(2, ((Number) equalsJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).get(0)).intValue());

        JSONObject reverseBody = new JSONObject();
        reverseBody.put("query", "MATCH ()-[r]->() WHERE $relType = type(r) RETURN count(r)");
        JSONObject reverseParams = new JSONObject();
        reverseParams.put("relType", "CALL");
        reverseBody.put("params", reverseParams);
        JSONObject reverseJson = JSON.parseObject(api.postJson("/api/query/cypher", reverseBody.toJSONString()));
        assertEquals(true, reverseJson.getBoolean("ok"));
        assertEquals(2, ((Number) reverseJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).get(0)).intValue());

        JSONObject listBody = new JSONObject();
        listBody.put("query", "MATCH ()-[r]->() WHERE type(r) IN $relTypes RETURN count(r)");
        JSONObject listParams = new JSONObject();
        listParams.put("relTypes", List.of("CALL"));
        listBody.put("params", listParams);
        JSONObject listJson = JSON.parseObject(api.postJson("/api/query/cypher", listBody.toJSONString()));
        assertEquals(true, listJson.getBoolean("ok"));
        assertEquals(2, ((Number) listJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).get(0)).intValue());

        JSONObject physicalBody = new JSONObject();
        physicalBody.put("query", "MATCH ()-[r]->() WHERE type(r) = $relType RETURN count(r)");
        JSONObject physicalParams = new JSONObject();
        physicalParams.put("relType", "CALLS_DIRECT");
        physicalBody.put("params", physicalParams);
        JSONObject physicalJson = JSON.parseObject(api.postJson("/api/query/cypher", physicalBody.toJSONString()));
        assertEquals(true, physicalJson.getBoolean("ok"));
        assertEquals(2, ((Number) physicalJson.getJSONObject("data").getJSONArray("rows").getJSONArray(0).get(0)).intValue());
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
                List.of(
                        new ProjectRuntimeSnapshot.MethodReferenceData(
                                new ProjectRuntimeSnapshot.ClassHandleData("org/apache/dubbo/rpc/service/GenericService", 1),
                                List.of(),
                                "invoke",
                                "(Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;",
                                1,
                                false,
                                -1,
                                "fixture.jar",
                                1
                        ),
                        new ProjectRuntimeSnapshot.MethodReferenceData(
                                new ProjectRuntimeSnapshot.ClassHandleData("demo/Mid", 1),
                                List.of(),
                                "step",
                                "()V",
                                1,
                                false,
                                -1,
                                "fixture.jar",
                                1
                        ),
                        new ProjectRuntimeSnapshot.MethodReferenceData(
                                new ProjectRuntimeSnapshot.ClassHandleData("java/lang/Runtime", 1),
                                List.of(),
                                "exec",
                                "(Ljava/lang/String;)Ljava/lang/Process;",
                                1,
                                false,
                                -1,
                                "fixture.jar",
                                1
                        )
                ),
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
            var source = tx.createNode(Label.label("JANode"), Label.label("Method"));
            source.setProperty("node_id", 1L);
            source.setProperty("kind", "method");
            source.setProperty("jar_id", 1);
            source.setProperty("class_name", "org/apache/dubbo/rpc/service/GenericService");
            source.setProperty("method_name", "invoke");
            source.setProperty("method_desc", "(Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
            source.setProperty("call_site_key", "");
            source.setProperty("line_number", -1);
            source.setProperty("call_index", -1);
            source.setProperty("source_flags", 0);

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
            middle.setProperty("source_flags", 3);

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
