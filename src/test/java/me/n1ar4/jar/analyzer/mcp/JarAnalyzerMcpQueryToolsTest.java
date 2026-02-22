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
import me.n1ar4.jar.analyzer.mcp.backend.JarAnalyzerApiInvoker;
import me.n1ar4.jar.analyzer.server.ServerConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JarAnalyzerMcpQueryToolsTest {
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
        McpToolRegistry registry = new McpToolRegistry();
        JarAnalyzerMcpTools.registerAll(registry, new JarAnalyzerApiInvoker(new ServerConfig()));
        McpToolCallContext ctx = new McpToolCallContext(Map.of());

        McpToolResult listResult = registry.get("project_list").getHandler().call(ctx, new JSONObject());
        assertFalse(listResult.isError());
        JSONObject listJson = JSON.parseObject(listResult.getText());
        assertTrue(listJson.getBooleanValue("ok"));

        JSONObject cypherArgs = new JSONObject();
        cypherArgs.put("query", "MATCH (m:JANode) RETURN m LIMIT 1");
        McpToolResult cypherResult = registry.get("query_cypher").getHandler().call(ctx, cypherArgs);
        assertFalse(cypherResult.isError());
        JSONObject cypherJson = JSON.parseObject(cypherResult.getText());
        assertTrue(cypherJson.getBooleanValue("ok"));
    }
}
