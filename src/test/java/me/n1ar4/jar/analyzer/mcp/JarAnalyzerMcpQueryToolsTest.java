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
        assertNotNull(registry.get("query_sql"));
    }

    @Test
    void querySqlToolShouldCallApi() throws Exception {
        McpToolRegistry registry = new McpToolRegistry();
        JarAnalyzerMcpTools.registerAll(registry, new JarAnalyzerApiInvoker(new ServerConfig()));
        McpToolCallContext ctx = new McpToolCallContext(Map.of());

        JSONObject sqlArgs = new JSONObject();
        sqlArgs.put("query", "select 1 as x");
        McpToolResult sqlResult = registry.get("query_sql").getHandler().call(ctx, sqlArgs);
        assertFalse(sqlResult.isError());
        JSONObject sqlJson = JSON.parseObject(sqlResult.getText());
        assertTrue(sqlJson.getBooleanValue("ok"));
    }
}
