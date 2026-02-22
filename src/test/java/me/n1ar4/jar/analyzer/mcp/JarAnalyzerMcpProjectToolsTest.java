/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.mcp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import me.n1ar4.jar.analyzer.mcp.backend.JarAnalyzerApiInvoker;
import me.n1ar4.jar.analyzer.server.ServerConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JarAnalyzerMcpProjectToolsTest {
    @Test
    void shouldRegisterProjectTools() {
        McpToolRegistry registry = new McpToolRegistry();
        JarAnalyzerMcpTools.registerAll(registry, new JarAnalyzerApiInvoker(new ServerConfig()));
        assertNotNull(registry.get("project_stores"));
        assertNotNull(registry.get("project_select"));
        assertNotNull(registry.get("project_drop"));
    }

    @Test
    void projectToolsShouldCallApi() throws Exception {
        McpToolRegistry registry = new McpToolRegistry();
        JarAnalyzerMcpTools.registerAll(registry, new JarAnalyzerApiInvoker(new ServerConfig()));
        McpToolCallContext ctx = new McpToolCallContext(Map.of());
        String project = "mcp-project-test";

        McpToolResult storesResult = registry.get("project_stores").getHandler().call(ctx, new JSONObject());
        assertFalse(storesResult.isError());
        JSONObject storesJson = JSON.parseObject(storesResult.getText());
        assertTrue(storesJson.getBooleanValue("ok"));

        JSONObject selectArgs = new JSONObject();
        selectArgs.put("project", project);
        McpToolResult selectResult = registry.get("project_select").getHandler().call(ctx, selectArgs);
        assertFalse(selectResult.isError());
        JSONObject selectJson = JSON.parseObject(selectResult.getText());
        assertTrue(selectJson.getBooleanValue("ok"));
        assertEquals(project, selectJson.getJSONObject("data").getString("project"));

        JSONObject backArgs = new JSONObject();
        backArgs.put("project", "default");
        McpToolResult backResult = registry.get("project_select").getHandler().call(ctx, backArgs);
        assertFalse(backResult.isError());

        JSONObject dropArgs = new JSONObject();
        dropArgs.put("project", project);
        McpToolResult dropResult = registry.get("project_drop").getHandler().call(ctx, dropArgs);
        assertFalse(dropResult.isError());
        JSONObject dropJson = JSON.parseObject(dropResult.getText());
        assertTrue(dropJson.getBooleanValue("ok"));
    }
}

