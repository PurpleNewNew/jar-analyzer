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

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class McpDispatcherTest {
    @Test
    void toolsCallShouldAcceptLowercaseTokenHeader() {
        McpToolRegistry registry = new McpToolRegistry();
        registry.add(new McpTool(
                "echo",
                new JSONObject(),
                (ctx, args) -> McpToolResult.ok("ok")
        ));
        McpDispatcher dispatcher = new McpDispatcher(
                "test",
                "1.0",
                registry,
                true,
                "secret"
        );

        JSONObject request = new JSONObject();
        request.put("jsonrpc", McpConstants.JSONRPC_VERSION);
        request.put("id", 1);
        request.put("method", "tools/call");
        JSONObject params = new JSONObject();
        params.put("name", "echo");
        params.put("arguments", new JSONObject());
        request.put("params", params);

        JSONObject response = dispatcher.handle(
                request.toJSONString().getBytes(StandardCharsets.UTF_8),
                Map.of("token", List.of("secret"))
        );

        assertEquals(1, response.getIntValue("id"));
        JSONObject result = response.getJSONObject("result");
        assertFalse(result.getBooleanValue("isError"));
        JSONArray content = result.getJSONArray("content");
        assertEquals("ok", content.getJSONObject(0).getString("text"));
    }

    @Test
    void notificationsShouldNotYieldResponse() {
        McpDispatcher dispatcher = new McpDispatcher(
                "test",
                "1.0",
                new McpToolRegistry(),
                false,
                ""
        );
        JSONObject request = new JSONObject();
        request.put("jsonrpc", McpConstants.JSONRPC_VERSION);
        request.put("method", "ping");

        JSONObject response = dispatcher.handle(
                request.toJSONString().getBytes(StandardCharsets.UTF_8),
                Map.of()
        );

        assertNull(response);
    }
}
