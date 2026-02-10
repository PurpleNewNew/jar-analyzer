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

import com.alibaba.fastjson2.JSONObject;

/**
 * A single MCP tool definition plus its handler.
 */
public final class McpTool {
    private final String name;
    private final JSONObject definition;
    private final McpToolHandler handler;

    public McpTool(String name, JSONObject definition, McpToolHandler handler) {
        this.name = name;
        this.definition = definition;
        this.handler = handler;
    }

    public String getName() {
        return name;
    }

    public JSONObject getDefinition() {
        return definition;
    }

    public McpToolHandler getHandler() {
        return handler;
    }
}

