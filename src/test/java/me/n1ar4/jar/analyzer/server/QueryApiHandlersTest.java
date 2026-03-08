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
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jGraphSnapshotLoader;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jProjectStore;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectMetadataSnapshotStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.neo4j.graphdb.Label;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryApiHandlersTest {
    private final String originalProjectKey = ActiveProjectContext.getPublishedActiveProjectKey();
    private final String originalProjectAlias = ActiveProjectContext.getPublishedActiveProjectAlias();
    private final List<String> projectKeys = new ArrayList<>();

    @AfterEach
    void cleanup() {
        ActiveProjectContext.setActiveProject(originalProjectKey, originalProjectAlias);
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
        assertTrue(data.getJSONArray("profiles").contains("long-chain"));
        assertTrue(data.getJSONArray("options").contains("expandBudget"));
        assertTrue(data.getJSONArray("options").contains("pathBudget"));
        assertTrue(data.getJSONArray("options").contains("timeoutCheckInterval"));
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
        ProjectMetadataSnapshotStore.getInstance().write(projectKey, snapshot);
        var database = Neo4jProjectStore.getInstance().database(projectKey);
        try (var tx = database.beginTx()) {
            tx.execute("MATCH (m:JAMeta {key:'build_meta'}) DETACH DELETE m");
            var meta = tx.createNode(Label.label("JAMeta"));
            meta.setProperty("key", "build_meta");
            meta.setProperty("build_seq", 9L);
            var node = tx.createNode(Label.label("JANode"));
            node.setProperty("node_id", 1L);
            node.setProperty("kind", "method");
            node.setProperty("jar_id", 1);
            node.setProperty("class_name", "demo/Ready");
            node.setProperty("method_name", "run");
            node.setProperty("method_desc", "()V");
            node.setProperty("call_site_key", "");
            node.setProperty("line_number", -1);
            node.setProperty("call_index", -1);
            node.setProperty("source_flags", 0);
            tx.commit();
        }
        Neo4jGraphSnapshotLoader.invalidate(projectKey);
        ActiveProjectContext.setActiveProject(projectKey, projectKey);
        return projectKey;
    }
}
