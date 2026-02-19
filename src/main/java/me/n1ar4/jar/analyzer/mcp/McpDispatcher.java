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
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal MCP JSON-RPC dispatcher for Jar Analyzer tools.
 * <p>
 * Supports: initialize, ping, tools/list, tools/call.
 */
public final class McpDispatcher {
    private static final Logger logger = LogManager.getLogger();

    private final String serverName;
    private final String serverVersion;
    private final McpToolRegistry registry;
    private final boolean mcpAuthEnabled;
    private final String mcpToken;

    public McpDispatcher(String serverName,
                         String serverVersion,
                         McpToolRegistry registry,
                         boolean mcpAuthEnabled,
                         String mcpToken) {
        this.serverName = serverName == null ? "jar-analyzer-mcp" : serverName;
        this.serverVersion = serverVersion == null ? "unknown" : serverVersion;
        this.registry = registry == null ? new McpToolRegistry() : registry;
        this.mcpAuthEnabled = mcpAuthEnabled;
        this.mcpToken = mcpToken == null ? "" : mcpToken;
    }

    /**
     * Handle a JSON-RPC message and return a JSON-RPC response object (or null for notifications).
     */
    public JSONObject handle(byte[] raw, Map<String, List<String>> headers) {
        if (raw == null || raw.length == 0) {
            return errorResponse(null, McpConstants.PARSE_ERROR, "Failed to parse message");
        }
        JSONObject base;
        try {
            base = JSON.parseObject(new String(raw, StandardCharsets.UTF_8));
        } catch (Exception ex) {
            return errorResponse(null, McpConstants.PARSE_ERROR, "Failed to parse message");
        }
        if (base == null) {
            return errorResponse(null, McpConstants.PARSE_ERROR, "Failed to parse message");
        }

        Object id = base.get("id");
        String jsonrpc = base.getString("jsonrpc");
        if (!McpConstants.JSONRPC_VERSION.equals(jsonrpc)) {
            return errorResponse(id, McpConstants.INVALID_REQUEST, "Invalid JSON-RPC version");
        }

        // Notifications have no id and should not yield a response.
        if (id == null) {
            return null;
        }

        // Ignore response messages (client responding to our ping, etc).
        if (base.containsKey("result")) {
            return null;
        }

        String method = base.getString("method");
        if (method == null || method.isBlank()) {
            return errorResponse(id, McpConstants.INVALID_REQUEST, "Invalid request");
        }
        method = method.strip();

        try {
            switch (method) {
                case "initialize":
                    return okResponse(id, initializeResult(base));
                case "ping":
                    return okResponse(id, new JSONObject(new LinkedHashMap<>()));
                case "tools/list":
                    return okResponse(id, listToolsResult());
                case "tools/call":
                    return okResponse(id, callToolResult(base, headers));
                default:
                    return errorResponse(id, McpConstants.METHOD_NOT_FOUND, "Method not found");
            }
        } catch (McpMethodNotFoundException ex) {
            return errorResponse(id, McpConstants.METHOD_NOT_FOUND, ex.getMessage());
        } catch (McpInvalidParamsException ex) {
            return errorResponse(id, McpConstants.INVALID_PARAMS, ex.getMessage());
        } catch (Exception ex) {
            logger.debug("mcp dispatch failed: {}", ex.toString());
            return errorResponse(id, McpConstants.INTERNAL_ERROR, "Internal error");
        }
    }

    private JSONObject initializeResult(JSONObject base) {
        JSONObject params = base.getJSONObject("params");
        String clientVersion = params == null ? null : params.getString("protocolVersion");
        String negotiated = negotiateProtocolVersion(clientVersion);

        JSONObject capabilities = new JSONObject(new LinkedHashMap<>());
        JSONObject tools = new JSONObject(new LinkedHashMap<>());
        tools.put("listChanged", false);
        capabilities.put("tools", tools);

        JSONObject serverInfo = new JSONObject(new LinkedHashMap<>());
        serverInfo.put("name", serverName);
        serverInfo.put("version", serverVersion);

        JSONObject result = new JSONObject(new LinkedHashMap<>());
        result.put("protocolVersion", negotiated);
        result.put("capabilities", capabilities);
        result.put("serverInfo", serverInfo);
        return result;
    }

    private static String negotiateProtocolVersion(String clientVersion) {
        String v = clientVersion == null ? "" : clientVersion.strip();
        if (v.isEmpty()) {
            throw new McpInvalidParamsException("protocolVersion is required");
        }
        if (McpConstants.VALID_PROTOCOL_VERSIONS.contains(v)) {
            return v;
        }
        throw new McpInvalidParamsException("unsupported protocolVersion: " + v);
    }

    private JSONObject listToolsResult() {
        JSONObject result = new JSONObject(new LinkedHashMap<>());
        result.put("tools", registry.listToolDefinitions());
        return result;
    }

    private JSONObject callToolResult(JSONObject base, Map<String, List<String>> headers) {
        JSONObject params = base.getJSONObject("params");
        if (params == null) {
            // Treat missing params as invalid.
            return toolErrorResult("invalid params");
        }
        String name = params.getString("name");
        if (name == null || name.isBlank()) {
            return toolErrorResult("invalid params");
        }
        JSONObject arguments = null;
        Object argObj = params.get("arguments");
        if (argObj instanceof JSONObject) {
            arguments = (JSONObject) argObj;
        } else if (argObj != null) {
            // Best-effort: allow non-object arguments, but convert to JSONObject if possible.
            try {
                arguments = JSON.parseObject(JSON.toJSONString(argObj));
            } catch (Exception ignored) {
            }
        }
        if (arguments == null) {
            arguments = new JSONObject();
        }

        McpTool tool = registry.get(name);
        if (tool == null) {
            throw new McpMethodNotFoundException("tool '" + name + "' not found");
        }

        // Tool-level auth.
        if (mcpAuthEnabled) {
            String token = firstHeader(headers, "Token");
            if (token == null || token.isBlank() || !token.equals(mcpToken)) {
                return toolErrorResult("need token error");
            }
        }

        McpToolCallContext ctx = new McpToolCallContext(headers);
        McpToolResult result;
        try {
            result = tool.getHandler().call(ctx, arguments);
        } catch (Exception ex) {
            // Tool errors should be returned in-band (isError=true), not as JSON-RPC errors.
            return toolErrorResult(ex.getMessage());
        }
        if (result == null) {
            return toolErrorResult("tool returned null");
        }
        return toolTextResult(result.getText(), result.isError());
    }

    private static String firstHeader(Map<String, List<String>> headers, String key) {
        if (headers == null || key == null) {
            return null;
        }
        List<String> values = headers.get(key);
        if (values != null && !values.isEmpty()) {
            return values.get(0);
        }
        values = headers.get(key.toLowerCase());
        if (values != null && !values.isEmpty()) {
            return values.get(0);
        }
        values = headers.get(key.toUpperCase());
        if (values != null && !values.isEmpty()) {
            return values.get(0);
        }
        return null;
    }

    private static JSONObject toolTextResult(String text, boolean isError) {
        JSONObject result = new JSONObject(new LinkedHashMap<>());
        JSONArray content = new JSONArray();
        JSONObject item = new JSONObject(new LinkedHashMap<>());
        item.put("type", "text");
        item.put("text", text == null ? "" : text);
        content.add(item);
        result.put("content", content);
        if (isError) {
            result.put("isError", true);
        }
        return result;
    }

    private static JSONObject toolErrorResult(String text) {
        return toolTextResult(text, true);
    }

    private static JSONObject okResponse(Object id, JSONObject result) {
        JSONObject resp = new JSONObject(new LinkedHashMap<>());
        resp.put("jsonrpc", McpConstants.JSONRPC_VERSION);
        resp.put("id", id);
        resp.put("result", result == null ? new JSONObject(new LinkedHashMap<>()) : result);
        return resp;
    }

    private static JSONObject errorResponse(Object id, int code, String message) {
        JSONObject err = new JSONObject(new LinkedHashMap<>());
        err.put("code", code);
        err.put("message", message == null ? "" : message);

        JSONObject resp = new JSONObject(new LinkedHashMap<>());
        resp.put("jsonrpc", McpConstants.JSONRPC_VERSION);
        resp.put("id", id);
        resp.put("error", err);
        return resp;
    }

    private static final class McpInvalidParamsException extends RuntimeException {
        private McpInvalidParamsException(String msg) {
            super(msg);
        }
    }

    private static final class McpMethodNotFoundException extends RuntimeException {
        private McpMethodNotFoundException(String msg) {
            super(msg);
        }
    }

    // No other public helpers; transports should call {@link #handle(byte[], Map)}.
}
