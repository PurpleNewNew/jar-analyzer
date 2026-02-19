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
import me.n1ar4.jar.analyzer.meta.CompatibilityCode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small helpers to build MCP tool JSON schema objects.
 * <p>
 * Keep it intentionally minimal (enough for MCP clients to render tool params).
 */
public final class McpToolSchemas {
    private McpToolSchemas() {
    }

    @CompatibilityCode(
            primary = "Minimal MCP tool schema generation",
            reason = "Keep default annotation hints for broad compatibility with older MCP clients"
    )
    public static JSONObject tool(String name, String description) {
        JSONObject t = new JSONObject(new LinkedHashMap<>());
        t.put("name", name);
        if (description != null && !description.isBlank()) {
            t.put("description", description);
        }
        t.put("inputSchema", emptyObjectSchema());

        // Keep annotations present (with defaults) for broad MCP client compatibility.
        JSONObject annotations = new JSONObject(new LinkedHashMap<>());
        annotations.put("readOnlyHint", false);
        annotations.put("destructiveHint", true);
        annotations.put("idempotentHint", false);
        annotations.put("openWorldHint", false);
        t.put("annotations", annotations);
        return t;
    }

    public static void addString(JSONObject tool, String key, boolean required, String desc) {
        addProperty(tool, key, required, schemaString(desc));
    }

    public static void addNumber(JSONObject tool, String key, boolean required, String desc) {
        addProperty(tool, key, required, schemaNumber(desc));
    }

    public static void addArray(JSONObject tool, String key, boolean required, String desc, JSONObject itemsSchema) {
        JSONObject schema = new JSONObject(new LinkedHashMap<>());
        schema.put("type", "array");
        if (desc != null && !desc.isBlank()) {
            schema.put("description", desc);
        }
        if (itemsSchema != null) {
            schema.put("items", itemsSchema);
        }
        addProperty(tool, key, required, schema);
    }

    private static void addProperty(JSONObject tool, String key, boolean required, JSONObject propSchema) {
        if (tool == null || key == null || key.isBlank()) {
            return;
        }
        JSONObject inputSchema = tool.getJSONObject("inputSchema");
        if (inputSchema == null) {
            inputSchema = emptyObjectSchema();
            tool.put("inputSchema", inputSchema);
        }
        JSONObject props = inputSchema.getJSONObject("properties");
        if (props == null) {
            props = new JSONObject(new LinkedHashMap<>());
            inputSchema.put("properties", props);
        }
        props.put(key, propSchema == null ? schemaString(null) : propSchema);

        if (required) {
            JSONArray req = inputSchema.getJSONArray("required");
            if (req == null) {
                req = new JSONArray();
                inputSchema.put("required", req);
            }
            if (!req.contains(key)) {
                req.add(key);
            }
        }
    }

    private static JSONObject emptyObjectSchema() {
        JSONObject schema = new JSONObject(new LinkedHashMap<>());
        schema.put("type", "object");
        schema.put("properties", new JSONObject(new LinkedHashMap<>()));
        return schema;
    }

    private static JSONObject schemaString(String desc) {
        JSONObject schema = new JSONObject(new LinkedHashMap<>());
        schema.put("type", "string");
        if (desc != null && !desc.isBlank()) {
            schema.put("description", desc);
        }
        return schema;
    }

    private static JSONObject schemaNumber(String desc) {
        JSONObject schema = new JSONObject(new LinkedHashMap<>());
        schema.put("type", "number");
        if (desc != null && !desc.isBlank()) {
            schema.put("description", desc);
        }
        return schema;
    }

    public static JSONObject schemaObject(Map<String, JSONObject> props) {
        JSONObject schema = new JSONObject(new LinkedHashMap<>());
        schema.put("type", "object");
        JSONObject p = new JSONObject(new LinkedHashMap<>());
        if (props != null) {
            for (Map.Entry<String, JSONObject> entry : props.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                p.put(entry.getKey(), entry.getValue());
            }
        }
        schema.put("properties", p);
        return schema;
    }
}
