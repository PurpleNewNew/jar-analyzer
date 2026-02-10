/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.mcp.report;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import me.n1ar4.jar.analyzer.mcp.McpTool;
import me.n1ar4.jar.analyzer.mcp.McpToolRegistry;
import me.n1ar4.jar.analyzer.mcp.McpToolResult;
import me.n1ar4.jar.analyzer.mcp.McpToolSchemas;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP tool: report
 */
public final class ReportMcpTools {
    private ReportMcpTools() {
    }

    public static void register(McpToolRegistry reg, ReportHub hub) {
        if (reg == null) {
            return;
        }
        ReportHub realHub = hub == null ? new ReportHub() : hub;

        JSONObject tool = McpToolSchemas.tool("report", "report vulnerable tool");
        McpToolSchemas.addString(tool, "type", true, "vulnerable type");
        McpToolSchemas.addString(tool, "reason", true, "vulnerable reason");
        McpToolSchemas.addNumber(tool, "score", true, "vulnerable score(max:10,min:1)");

        Map<String, JSONObject> props = new LinkedHashMap<>();
        props.put("class", schemaString());
        props.put("method", schemaString());
        props.put("desc", schemaString());
        McpToolSchemas.addArray(tool, "trace", true, "vulnerable trace", McpToolSchemas.schemaObject(props));

        reg.add(new McpTool("report", tool, (ctx, args) -> {
            if (args == null) {
                return McpToolResult.error("invalid params");
            }
            ReportData data = new ReportData();
            data.setType(require(args, "type"));
            data.setReason(require(args, "reason"));
            data.setScore(args.getIntValue("score"));

            JSONArray trace = args.getJSONArray("trace");
            if (trace == null) {
                return McpToolResult.error("required argument \"trace\" not found");
            }
            for (int i = 0; i < trace.size(); i++) {
                Object itemObj = trace.get(i);
                if (!(itemObj instanceof JSONObject)) {
                    continue;
                }
                JSONObject item = (JSONObject) itemObj;
                ReportData.TraceItem t = new ReportData.TraceItem();
                t.setClazz(item.getString("class"));
                t.setMethod(item.getString("method"));
                t.setDesc(item.getString("desc"));
                data.getTrace().add(t);
            }

            realHub.broadcast(data);
            realHub.save(data);
            return McpToolResult.ok("report data received and broadcasted successfully");
        }));
    }

    private static String require(JSONObject args, String key) {
        if (args == null || key == null) {
            throw new IllegalArgumentException("required argument not found");
        }
        String v = args.getString(key);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("required argument \"" + key + "\" not found");
        }
        return v;
    }

    private static JSONObject schemaString() {
        JSONObject s = new JSONObject(new LinkedHashMap<>());
        s.put("type", "string");
        return s;
    }
}
