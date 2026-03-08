/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.mcp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.mcp.backend.JarAnalyzerApiInvoker;
import me.n1ar4.jar.analyzer.server.ServerConfig;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jGraphSnapshotLoader;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jProjectStore;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectMetadataSnapshotStoreTestHook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Label;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JarAnalyzerMcpQueryToolsTest {
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
    void shouldRegisterQueryTools() {
        McpToolRegistry registry = new McpToolRegistry();
        JarAnalyzerMcpTools.registerAll(registry, new JarAnalyzerApiInvoker(new ServerConfig()));
        assertNotNull(registry.get("project_list"));
        assertNotNull(registry.get("project_active"));
        assertNotNull(registry.get("project_register"));
        assertNotNull(registry.get("project_switch"));
        assertNotNull(registry.get("project_remove"));
        assertNotNull(registry.get("query_cypher"));
        assertNotNull(registry.get("cypher_explain"));
        assertNotNull(registry.get("taint_chain_cypher"));
    }

    @Test
    void projectAndCypherToolsShouldCallApi() throws Exception {
        String projectKey = prepareReadyProject();
        McpToolRegistry registry = new McpToolRegistry();
        JarAnalyzerMcpTools.registerAll(registry, new JarAnalyzerApiInvoker(new ServerConfig()));
        McpToolCallContext ctx = new McpToolCallContext(Map.of());

        McpToolResult listResult = registry.get("project_list").getHandler().call(ctx, new JSONObject());
        assertFalse(listResult.isError());
        JSONObject listJson = JSON.parseObject(listResult.getText());
        assertTrue(listJson.getBooleanValue("ok"));

        ActiveProjectContext.setActiveProject(projectKey, projectKey);
        JSONObject cypherArgs = new JSONObject();
        cypherArgs.put("query", "MATCH (m:JANode) RETURN m LIMIT 1");
        McpToolResult cypherResult = registry.get("query_cypher").getHandler().call(ctx, cypherArgs);
        assertFalse(cypherResult.isError());
        JSONObject cypherJson = JSON.parseObject(cypherResult.getText());
        assertTrue(cypherJson.getBooleanValue("ok"));
    }

    private String prepareReadyProject() {
        String projectKey = "mcp-query-" + Long.toHexString(System.nanoTime());
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
