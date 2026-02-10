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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class McpToolRegistry {
    private final Map<String, McpTool> tools = new HashMap<>();

    public void add(McpTool tool) {
        if (tool == null || tool.getName() == null || tool.getName().trim().isEmpty()) {
            return;
        }
        tools.put(tool.getName(), tool);
    }

    public McpTool get(String name) {
        if (name == null) {
            return null;
        }
        return tools.get(name);
    }

    public JSONArray listToolDefinitions() {
        List<McpTool> list = new ArrayList<>(tools.values());
        list.sort(Comparator.comparing(McpTool::getName));
        JSONArray arr = new JSONArray();
        for (McpTool tool : list) {
            JSONObject def = tool.getDefinition();
            if (def != null) {
                arr.add(def);
            }
        }
        return arr;
    }

    public List<String> listToolNames() {
        if (tools.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>(tools.keySet());
        names.sort(String::compareTo);
        return names;
    }
}

