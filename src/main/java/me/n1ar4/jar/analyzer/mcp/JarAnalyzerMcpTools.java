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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * MCP tools backed by in-process /api/* handlers.
 */
public final class JarAnalyzerMcpTools {
    private JarAnalyzerMcpTools() {
    }

    public static void registerAll(McpToolRegistry reg, JarAnalyzerApiInvoker api) {
        registerProjectTools(reg, api);
        registerJarTools(reg, api);
        registerSpringTools(reg, api);
        registerMethodClassTools(reg, api);
        registerCallGraphTools(reg, api);
        registerCodeTools(reg, api);
        registerResourceTools(reg, api);
        registerConfigUsageTools(reg, api);
        registerSemanticTools(reg, api);
        registerSecurityTools(reg, api);
        registerFlowTools(reg, api);
        registerQueryTools(reg, api);
    }

    public static void registerAuditFast(McpToolRegistry reg, JarAnalyzerApiInvoker api) {
        registerAll(reg, api);
    }

    public static void registerGraphLite(McpToolRegistry reg, JarAnalyzerApiInvoker api) {
        registerProjectTools(reg, api);
        registerCallGraphTools(reg, api);
        registerSemanticTools(reg, api);
        registerJarTools(reg, api);
        registerMethodClassTools(reg, api);
        registerQueryTools(reg, api);
    }

    public static void registerDfsTaint(McpToolRegistry reg, JarAnalyzerApiInvoker api) {
        registerFlowTools(reg, api);
        registerCodeTools(reg, api);
        registerQueryTools(reg, api);
    }

    public static void registerScaLeak(McpToolRegistry reg, JarAnalyzerApiInvoker api) {
        registerSecurityTools(reg, api);
    }

    public static void registerVulRules(McpToolRegistry reg, JarAnalyzerApiInvoker api) {
        registerSecurityTools(reg, api);
    }

    private static void registerProjectTools(McpToolRegistry reg, JarAnalyzerApiInvoker api) {
        JSONObject stores = McpToolSchemas.tool("project_stores", "List project graph stores and current active store.");
        reg.add(new McpTool("project_stores", stores, (ctx, args) -> call(api, "/api/project/stores", Map.of())));

        JSONObject select = McpToolSchemas.tool("project_select", "Switch active project graph store.");
        McpToolSchemas.addString(select, "project", true, "Project key.");
        reg.add(new McpTool("project_select", select, (ctx, args) -> {
            try {
                String project = require(args, "project");
                JSONObject body = new JSONObject();
                body.put("project", project);
                return callPost(api, "/api/project/select", body);
            } catch (Exception ex) {
                return McpToolResult.error(ex.getMessage());
            }
        }));

        JSONObject drop = McpToolSchemas.tool("project_drop", "Delete a non-active project graph store.");
        McpToolSchemas.addString(drop, "project", true, "Project key.");
        reg.add(new McpTool("project_drop", drop, (ctx, args) -> {
            try {
                String project = require(args, "project");
                JSONObject body = new JSONObject();
                body.put("project", project);
                return callPost(api, "/api/project/drop", body);
            } catch (Exception ex) {
                return McpToolResult.error(ex.getMessage());
            }
        }));
    }

    private static void registerJarTools(McpToolRegistry reg, JarAnalyzerApiInvoker api) {
        // jar_list
        JSONObject jarList = McpToolSchemas.tool("jar_list",
                "List jar metadata (jar_id/jar_name/jar_fingerprint).");
        McpToolSchemas.addString(jarList, "offset", false, "Offset (optional).");
        McpToolSchemas.addString(jarList, "limit", false, "Limit (optional).");
        reg.add(new McpTool("jar_list", jarList, (ctx, args) -> {
            Map<String, String> params = new HashMap<>();
            addIf(params, "offset", args.getString("offset"));
            addIf(params, "limit", args.getString("limit"));
            return call(api, "/api/meta/jars", params);
        }));

        // jar_resolve
        JSONObject jarResolve = McpToolSchemas.tool("jar_resolve",
                "Resolve jar metadata by class name.");
        McpToolSchemas.addString(jarResolve, "class", true, "Class name (dot or slash).");
        reg.add(new McpTool("jar_resolve", jarResolve, (ctx, args) -> {
            String className = require(args, "class");
            Map<String, String> params = new HashMap<>();
            params.put("class", className);
            return call(api, "/api/meta/jars/resolve", params);
        }));

        // class_info
        JSONObject classInfo = McpToolSchemas.tool("class_info",
                "Get class info (super class, interface, jar).");
        McpToolSchemas.addString(classInfo, "class", true, "Class name (dot or slash).");
        McpToolSchemas.addString(classInfo, "scope", false, "Scope filter: app|all (optional).");
        reg.add(new McpTool("class_info", classInfo, (ctx, args) -> {
            String className = require(args, "class");
            Map<String, String> params = new HashMap<>();
            params.put("class", className);
            addIf(params, "scope", args.getString("scope"));
            return call(api, "/api/class/info", params);
        }));
    }

    private static void registerSpringTools(McpToolRegistry reg, JarAnalyzerApiInvoker api) {
        // entrypoints_list
        JSONObject entrypoints = McpToolSchemas.tool("entrypoints_list",
                "List entrypoint classes by type.");
        McpToolSchemas.addString(entrypoints, "type", true,
                "Types: spring_controller, spring_interceptor, servlet, filter, listener, all.");
        McpToolSchemas.addString(entrypoints, "limit", false, "Limit per type (optional).");
        McpToolSchemas.addString(entrypoints, "scope", false, "Scope filter: app|all (optional).");
        reg.add(new McpTool("entrypoints_list", entrypoints, (ctx, args) -> {
            String type = require(args, "type");
            Map<String, String> params = new HashMap<>();
            params.put("type", type);
            addIf(params, "limit", args.getString("limit"));
            addIf(params, "scope", args.getString("scope"));
            return call(api, "/api/entrypoints", params);
        }));

        // spring_mappings
        JSONObject mappings = McpToolSchemas.tool("spring_mappings",
                "List Spring mappings (class-specific or global search).");
        McpToolSchemas.addString(mappings, "class", false, "Class name (optional).");
        McpToolSchemas.addString(mappings, "jarId", false, "Jar ID filter (optional).");
        McpToolSchemas.addString(mappings, "keyword", false, "Keyword filter (optional).");
        McpToolSchemas.addString(mappings, "offset", false, "Offset (optional).");
        McpToolSchemas.addString(mappings, "limit", false, "Limit (optional).");
        McpToolSchemas.addString(mappings, "scope", false, "Scope filter: app|all (optional).");
        reg.add(new McpTool("spring_mappings", mappings, (ctx, args) -> {
            Map<String, String> params = new HashMap<>();
            addIf(params, "class", args.getString("class"));
            addIf(params, "jarId", args.getString("jarId"));
            addIf(params, "keyword", args.getString("keyword"));
            addIf(params, "offset", args.getString("offset"));
            addIf(params, "limit", args.getString("limit"));
            addIf(params, "scope", args.getString("scope"));
            return call(api, "/api/entrypoints/mappings", params);
        }));
    }

    private static void registerMethodClassTools(McpToolRegistry reg, JarAnalyzerApiInvoker api) {
        // methods_search
        JSONObject search = McpToolSchemas.tool("methods_search",
                "Search methods by signature, string, or annotation.");
        McpToolSchemas.addString(search, "class", false, "Class name (optional).");
        McpToolSchemas.addString(search, "method", false, "Method name (optional).");
        McpToolSchemas.addString(search, "desc", false, "Method descriptor (optional).");
        McpToolSchemas.addString(search, "match", false, "Signature match: exact|like (optional).");
        McpToolSchemas.addString(search, "str", false, "String query (optional).");
        McpToolSchemas.addString(search, "strMode", false, "String match: auto|contains|prefix|equal|fts (optional).");
        McpToolSchemas.addString(search, "classLike", false, "Class prefix filter for string search (optional).");
        McpToolSchemas.addString(search, "anno", false, "Annotation names list (optional).");
        McpToolSchemas.addString(search, "annoMatch", false, "Annotation match: contains|equal (optional).");
        McpToolSchemas.addString(search, "annoScope", false, "Annotation scope: any|class|method (optional).");
        McpToolSchemas.addString(search, "jarId", false, "Jar ID filter (optional).");
        McpToolSchemas.addString(search, "offset", false, "Offset (optional).");
        McpToolSchemas.addString(search, "limit", false, "Limit (optional).");
        McpToolSchemas.addString(search, "scope", false, "Scope filter: app|all (optional).");
        reg.add(new McpTool("methods_search", search, (ctx, args) -> {
            Map<String, String> params = new HashMap<>();
            addIf(params, "class", args.getString("class"));
            addIf(params, "method", args.getString("method"));
            addIf(params, "desc", args.getString("desc"));
            addIf(params, "match", args.getString("match"));
            addIf(params, "str", args.getString("str"));
            addIf(params, "strMode", args.getString("strMode"));
            addIf(params, "classLike", args.getString("classLike"));
            addIf(params, "anno", args.getString("anno"));
            addIf(params, "annoMatch", args.getString("annoMatch"));
            addIf(params, "annoScope", args.getString("annoScope"));
            addIf(params, "jarId", args.getString("jarId"));
            addIf(params, "offset", args.getString("offset"));
            addIf(params, "limit", args.getString("limit"));
            addIf(params, "scope", args.getString("scope"));
            return call(api, "/api/methods/search", params);
        }));

        // methods_impls
        JSONObject impls = McpToolSchemas.tool("methods_impls",
                "Resolve method implementations or super implementations.");
        McpToolSchemas.addString(impls, "class", true, "Class name (dot or slash).");
        McpToolSchemas.addString(impls, "method", true, "Method name.");
        McpToolSchemas.addString(impls, "desc", false, "Method descriptor (optional).");
        McpToolSchemas.addString(impls, "direction", false, "impls|super (optional).");
        McpToolSchemas.addString(impls, "offset", false, "Offset (optional).");
        McpToolSchemas.addString(impls, "limit", false, "Limit (optional).");
        McpToolSchemas.addString(impls, "scope", false, "Scope filter: app|all (optional).");
        reg.add(new McpTool("methods_impls", impls, (ctx, args) -> {
            String className = require(args, "class");
            String methodName = require(args, "method");
            Map<String, String> params = new HashMap<>();
            params.put("class", className);
            params.put("method", methodName);
            addIf(params, "desc", args.getString("desc"));
            addIf(params, "direction", args.getString("direction"));
            addIf(params, "offset", args.getString("offset"));
            addIf(params, "limit", args.getString("limit"));
            addIf(params, "scope", args.getString("scope"));
            return call(api, "/api/methods/impls", params);
        }));
    }

    private static void registerCallGraphTools(McpToolRegistry reg, JarAnalyzerApiInvoker api) {
        // callgraph_edges
        JSONObject edges = McpToolSchemas.tool("callgraph_edges",
                "Query call graph edges with evidence/confidence.");
        McpToolSchemas.addString(edges, "class", true, "Class name.");
        McpToolSchemas.addString(edges, "method", true, "Method name.");
        McpToolSchemas.addString(edges, "desc", false, "Method descriptor (optional).");
        McpToolSchemas.addString(edges, "direction", false, "callers|callees (optional).");
        McpToolSchemas.addString(edges, "offset", false, "Offset (optional).");
        McpToolSchemas.addString(edges, "limit", false, "Limit (optional).");
        McpToolSchemas.addString(edges, "scope", false, "Scope filter: app|all (optional).");
        reg.add(new McpTool("callgraph_edges", edges, (ctx, args) -> {
            String className = require(args, "class");
            String methodName = require(args, "method");
            Map<String, String> params = new HashMap<>();
            params.put("class", className);
            params.put("method", methodName);
            addIf(params, "desc", args.getString("desc"));
            addIf(params, "direction", args.getString("direction"));
            addIf(params, "offset", args.getString("offset"));
            addIf(params, "limit", args.getString("limit"));
            addIf(params, "scope", args.getString("scope"));
            return call(api, "/api/callgraph/edges", params);
        }));

        // callgraph_by_sink
        JSONObject bySink = McpToolSchemas.tool("callgraph_by_sink",
                "Find callers by sink definition or sink name.");
        McpToolSchemas.addString(bySink, "sinkName", false, "Built-in sink name list (comma separated).");
        McpToolSchemas.addString(bySink, "sinkClass", false, "Sink class (optional).");
        McpToolSchemas.addString(bySink, "sinkMethod", false, "Sink method (optional).");
        McpToolSchemas.addString(bySink, "sinkDesc", false, "Sink desc (optional).");
        McpToolSchemas.addString(bySink, "items", false, "Batch JSON array of sinks (optional).");
        McpToolSchemas.addString(bySink, "limit", false, "Per-sink limit (optional).");
        McpToolSchemas.addString(bySink, "scope", false, "Scope filter: app|all (optional).");
        reg.add(new McpTool("callgraph_by_sink", bySink, (ctx, args) -> {
            Map<String, String> params = new HashMap<>();
            addIf(params, "sinkName", args.getString("sinkName"));
            addIf(params, "sinkClass", args.getString("sinkClass"));
            addIf(params, "sinkMethod", args.getString("sinkMethod"));
            addIf(params, "sinkDesc", args.getString("sinkDesc"));
            addIf(params, "items", args.getString("items"));
            addIf(params, "limit", args.getString("limit"));
            addIf(params, "scope", args.getString("scope"));
            return call(api, "/api/callgraph/by-sink", params);
        }));
    }

    private static void registerCodeTools(McpToolRegistry reg, JarAnalyzerApiInvoker api) {
        JSONObject code = McpToolSchemas.tool("code_get",
                "Get decompiled method code (CFR or Fernflower).");
        McpToolSchemas.addString(code, "engine", false, "cfr|fernflower (optional, default cfr).");
        McpToolSchemas.addString(code, "class", true, "Class name.");
        McpToolSchemas.addString(code, "method", true, "Method name.");
        McpToolSchemas.addString(code, "desc", false, "Method descriptor (optional).");
        McpToolSchemas.addString(code, "full", false, "Include full class code (optional).");
        reg.add(new McpTool("code_get", code, (ctx, args) -> {
            String className = require(args, "class");
            String methodName = require(args, "method");
            Map<String, String> params = new HashMap<>();
            params.put("class", className);
            params.put("method", methodName);
            addIf(params, "engine", args.getString("engine"));
            addIf(params, "desc", args.getString("desc"));
            addIf(params, "full", args.getString("full"));
            return call(api, "/api/code", params);
        }));
    }

    private static void registerResourceTools(McpToolRegistry reg, JarAnalyzerApiInvoker api) {
        // resources_list
        JSONObject list = McpToolSchemas.tool("resources_list", "List resources.");
        McpToolSchemas.addString(list, "path", false, "Path prefix (optional).");
        McpToolSchemas.addString(list, "jarId", false, "Jar ID filter (optional).");
        McpToolSchemas.addString(list, "offset", false, "Offset (optional).");
        McpToolSchemas.addString(list, "limit", false, "Limit (optional).");
        reg.add(new McpTool("resources_list", list, (ctx, args) -> {
            Map<String, String> params = new HashMap<>();
            addIf(params, "path", args.getString("path"));
            addIf(params, "jarId", args.getString("jarId"));
            addIf(params, "offset", args.getString("offset"));
            addIf(params, "limit", args.getString("limit"));
            return call(api, "/api/resources/list", params);
        }));

        // resources_get
        JSONObject get = McpToolSchemas.tool("resources_get",
                "Get resource content by id or jarId+path.");
        McpToolSchemas.addString(get, "id", false, "Resource id (optional).");
        McpToolSchemas.addString(get, "jarId", false, "Jar ID (optional, required for path lookup).");
        McpToolSchemas.addString(get, "path", false, "Resource path (optional).");
        McpToolSchemas.addString(get, "offset", false, "Offset (optional).");
        McpToolSchemas.addString(get, "limit", false, "Limit (optional).");
        McpToolSchemas.addString(get, "base64", false, "Force base64 (optional).");
        reg.add(new McpTool("resources_get", get, (ctx, args) -> {
            Map<String, String> params = new HashMap<>();
            addIf(params, "id", args.getString("id"));
            addIf(params, "jarId", args.getString("jarId"));
            addIf(params, "path", args.getString("path"));
            addIf(params, "offset", args.getString("offset"));
            addIf(params, "limit", args.getString("limit"));
            addIf(params, "base64", args.getString("base64"));
            return call(api, "/api/resources/get", params);
        }));

        // resources_search
        JSONObject search = McpToolSchemas.tool("resources_search",
                "Search text resources by keywords.");
        McpToolSchemas.addString(search, "query", true, "Search keywords (comma/newline separated).");
        McpToolSchemas.addString(search, "jarId", false, "Jar ID filter (optional).");
        McpToolSchemas.addString(search, "limit", false, "Limit (optional).");
        McpToolSchemas.addString(search, "maxBytes", false, "Max bytes to read per file (optional).");
        McpToolSchemas.addString(search, "mode", false, "Match mode: and|or (optional).");
        McpToolSchemas.addString(search, "case", false, "Case sensitive (optional).");
        reg.add(new McpTool("resources_search", search, (ctx, args) -> {
            String query = require(args, "query");
            Map<String, String> params = new HashMap<>();
            params.put("query", query);
            addIf(params, "jarId", args.getString("jarId"));
            addIf(params, "limit", args.getString("limit"));
            addIf(params, "maxBytes", args.getString("maxBytes"));
            addIf(params, "mode", args.getString("mode"));
            addIf(params, "case", args.getString("case"));
            return call(api, "/api/resources/search", params);
        }));
    }

    private static void registerConfigUsageTools(McpToolRegistry reg, JarAnalyzerApiInvoker api) {
        JSONObject tool = McpToolSchemas.tool("config_usage",
                "Link config keys to code usage and entrypoints.");
        McpToolSchemas.addString(tool, "keys", false, "Keys list (optional).");
        McpToolSchemas.addString(tool, "jarId", false, "Jar ID filter (optional).");
        McpToolSchemas.addString(tool, "maxKeys", false, "Max keys (optional).");
        McpToolSchemas.addString(tool, "maxPerKey", false, "Max methods per key (optional).");
        McpToolSchemas.addString(tool, "maxDepth", false, "Call graph depth (optional).");
        McpToolSchemas.addString(tool, "maxResources", false, "Max config items (optional).");
        McpToolSchemas.addString(tool, "maxBytes", false, "Max bytes per resource (optional).");
        McpToolSchemas.addString(tool, "mappingLimit", false, "Spring mapping limit (optional).");
        McpToolSchemas.addString(tool, "maxEntry", false, "Max entrypoints per method (optional).");
        McpToolSchemas.addString(tool, "mask", false, "Mask values (optional).");
        McpToolSchemas.addString(tool, "includeResources", false, "Include config resources (optional).");
        McpToolSchemas.addString(tool, "scope", false, "Scope filter: app|all (optional).");
        reg.add(new McpTool("config_usage", tool, (ctx, args) -> {
            Map<String, String> params = new HashMap<>();
            addIf(params, "keys", args.getString("keys"));
            addIf(params, "jarId", args.getString("jarId"));
            addIf(params, "maxKeys", args.getString("maxKeys"));
            addIf(params, "maxPerKey", args.getString("maxPerKey"));
            addIf(params, "maxDepth", args.getString("maxDepth"));
            addIf(params, "maxResources", args.getString("maxResources"));
            addIf(params, "maxBytes", args.getString("maxBytes"));
            addIf(params, "mappingLimit", args.getString("mappingLimit"));
            addIf(params, "maxEntry", args.getString("maxEntry"));
            addIf(params, "mask", args.getString("mask"));
            addIf(params, "includeResources", args.getString("includeResources"));
            addIf(params, "scope", args.getString("scope"));
            return call(api, "/api/config/usage", params);
        }));
    }

    private static void registerSemanticTools(McpToolRegistry reg, JarAnalyzerApiInvoker api) {
        JSONObject tool = McpToolSchemas.tool("semantic_hints",
                "Get semantic hints (authn/authz/validation/config boundaries).");
        McpToolSchemas.addString(tool, "jarId", false, "Jar ID filter (optional).");
        McpToolSchemas.addString(tool, "limit", false, "Limit per category (optional).");
        McpToolSchemas.addString(tool, "strLimit", false, "String search limit (optional).");
        McpToolSchemas.addString(tool, "scope", false, "Scope filter: app|all (optional).");
        reg.add(new McpTool("semantic_hints", tool, (ctx, args) -> {
            Map<String, String> params = new HashMap<>();
            addIf(params, "jarId", args.getString("jarId"));
            addIf(params, "limit", args.getString("limit"));
            addIf(params, "strLimit", args.getString("strLimit"));
            addIf(params, "scope", args.getString("scope"));
            return call(api, "/api/semantic/hints", params);
        }));
    }

    private static void registerSecurityTools(McpToolRegistry reg, JarAnalyzerApiInvoker api) {
        // vul_rules
        JSONObject rules = McpToolSchemas.tool("vul_rules",
                "List vulnerability rules.");
        reg.add(new McpTool("vul_rules", rules, (ctx, args) ->
                call(api, "/api/security/vul-rules", new HashMap<>())));

        // vul_search
        JSONObject search = McpToolSchemas.tool("vul_search",
                "Search call sites by vulnerability rules.");
        McpToolSchemas.addString(search, "name", false, "Rule names list (optional).");
        McpToolSchemas.addString(search, "level", false, "Rule level high/medium/low (optional).");
        McpToolSchemas.addString(search, "limit", false, "Per-rule limit (optional).");
        McpToolSchemas.addString(search, "totalLimit", false, "Total limit (optional).");
        McpToolSchemas.addString(search, "offset", false, "Offset (optional, groupBy=method).");
        McpToolSchemas.addString(search, "groupBy", false, "groupBy=rule|method (optional).");
        McpToolSchemas.addString(search, "blacklist", false, "Class/package blacklist (optional).");
        McpToolSchemas.addString(search, "whitelist", false, "Class/package whitelist (optional).");
        McpToolSchemas.addString(search, "jar", false, "Jar name filter (optional).");
        McpToolSchemas.addString(search, "jarId", false, "Jar ID filter (optional).");
        McpToolSchemas.addString(search, "scope", false, "Scope filter: app|all (optional).");
        reg.add(new McpTool("vul_search", search, (ctx, args) -> {
            Map<String, String> params = new HashMap<>();
            addIf(params, "name", args.getString("name"));
            addIf(params, "level", args.getString("level"));
            addIf(params, "limit", args.getString("limit"));
            addIf(params, "totalLimit", args.getString("totalLimit"));
            addIf(params, "offset", args.getString("offset"));
            addIf(params, "groupBy", args.getString("groupBy"));
            addIf(params, "blacklist", args.getString("blacklist"));
            addIf(params, "whitelist", args.getString("whitelist"));
            addIf(params, "jar", args.getString("jar"));
            addIf(params, "jarId", args.getString("jarId"));
            addIf(params, "scope", args.getString("scope"));
            return call(api, "/api/security/vul-search", params);
        }));

        // sca_scan
        JSONObject sca = McpToolSchemas.tool("sca_scan", "SCA dependency risk scan.");
        McpToolSchemas.addString(sca, "path", false, "Scan path (optional).");
        McpToolSchemas.addString(sca, "paths", false, "Multiple paths (optional).");
        McpToolSchemas.addString(sca, "log4j", false, "Enable log4j rules (optional).");
        McpToolSchemas.addString(sca, "fastjson", false, "Enable fastjson rules (optional).");
        McpToolSchemas.addString(sca, "shiro", false, "Enable shiro rules (optional).");
        reg.add(new McpTool("sca_scan", sca, (ctx, args) -> {
            Map<String, String> params = new HashMap<>();
            addIf(params, "path", args.getString("path"));
            addIf(params, "paths", args.getString("paths"));
            addIf(params, "log4j", args.getString("log4j"));
            addIf(params, "fastjson", args.getString("fastjson"));
            addIf(params, "shiro", args.getString("shiro"));
            return call(api, "/api/security/sca", params);
        }));

        // leak_scan
        JSONObject leak = McpToolSchemas.tool("leak_scan", "Sensitive info leak scan.");
        McpToolSchemas.addString(leak, "types", false, "Leak types (optional).");
        McpToolSchemas.addString(leak, "base64", false, "Enable base64 detection (optional).");
        McpToolSchemas.addString(leak, "limit", false, "Max results (optional).");
        McpToolSchemas.addString(leak, "whitelist", false, "Class/package whitelist (optional).");
        McpToolSchemas.addString(leak, "blacklist", false, "Class/package blacklist (optional).");
        McpToolSchemas.addString(leak, "jar", false, "Jar name filter (optional).");
        McpToolSchemas.addString(leak, "jarId", false, "Jar ID filter (optional).");
        McpToolSchemas.addString(leak, "scope", false, "Scope filter: app|all (optional).");
        reg.add(new McpTool("leak_scan", leak, (ctx, args) -> {
            Map<String, String> params = new HashMap<>();
            addIf(params, "types", args.getString("types"));
            addIf(params, "base64", args.getString("base64"));
            addIf(params, "limit", args.getString("limit"));
            addIf(params, "whitelist", args.getString("whitelist"));
            addIf(params, "blacklist", args.getString("blacklist"));
            addIf(params, "jar", args.getString("jar"));
            addIf(params, "jarId", args.getString("jarId"));
            addIf(params, "scope", args.getString("scope"));
            return call(api, "/api/security/leak", params);
        }));

        // gadget_scan
        JSONObject gadget = McpToolSchemas.tool("gadget_scan", "Gadget dependency scan.");
        McpToolSchemas.addString(gadget, "dir", true, "Dependency directory.");
        McpToolSchemas.addString(gadget, "native", false, "Enable native rules (optional).");
        McpToolSchemas.addString(gadget, "hessian", false, "Enable hessian rules (optional).");
        McpToolSchemas.addString(gadget, "fastjson", false, "Enable fastjson rules (optional).");
        McpToolSchemas.addString(gadget, "jdbc", false, "Enable jdbc rules (optional).");
        reg.add(new McpTool("gadget_scan", gadget, (ctx, args) -> {
            String dir = require(args, "dir");
            Map<String, String> params = new HashMap<>();
            params.put("dir", dir);
            addIf(params, "native", args.getString("native"));
            addIf(params, "hessian", args.getString("hessian"));
            addIf(params, "fastjson", args.getString("fastjson"));
            addIf(params, "jdbc", args.getString("jdbc"));
            return call(api, "/api/security/gadget", params);
        }));
    }

    private static void registerFlowTools(McpToolRegistry reg, JarAnalyzerApiInvoker api) {
        // flow_start
        JSONObject start = McpToolSchemas.tool("flow_start",
                "Start DFS or taint job.");
        McpToolSchemas.addString(start, "engine", true, "dfs|taint");
        McpToolSchemas.addString(start, "mode", false, "DFS mode: sink|source (optional).");
        McpToolSchemas.addString(start, "sinkName", false, "Built-in sink name list (optional).");
        McpToolSchemas.addString(start, "sinkClass", false, "Sink class (optional).");
        McpToolSchemas.addString(start, "sinkMethod", false, "Sink method (optional).");
        McpToolSchemas.addString(start, "sinkDesc", false, "Sink desc (optional).");
        McpToolSchemas.addString(start, "sourceClass", false, "Source class (optional).");
        McpToolSchemas.addString(start, "sourceMethod", false, "Source method (optional).");
        McpToolSchemas.addString(start, "sourceDesc", false, "Source desc (optional).");
        McpToolSchemas.addString(start, "searchAllSources", false, "Search all sources (optional).");
        McpToolSchemas.addString(start, "onlyFromWeb", false, "Only from web entrypoints (optional).");
        McpToolSchemas.addString(start, "depth", false, "DFS depth (optional).");
        McpToolSchemas.addString(start, "maxLimit", false, "DFS max edges (optional).");
        McpToolSchemas.addString(start, "maxPaths", false, "DFS max paths (optional).");
        McpToolSchemas.addString(start, "timeoutMs", false, "Timeout ms (optional).");
        McpToolSchemas.addString(start, "blacklist", false, "Blacklist classes/packages (optional).");
        McpToolSchemas.addString(start, "dfsJobId", false, "DFS job id (required for taint).");
        reg.add(new McpTool("flow_start", start, (ctx, args) -> {
            String engine = require(args, "engine").trim().toLowerCase(Locale.ROOT);
            Map<String, String> params = new HashMap<>();
            if ("dfs".equals(engine)) {
                addIf(params, "mode", args.getString("mode"));
                addIf(params, "sinkName", args.getString("sinkName"));
                addIf(params, "sinkClass", args.getString("sinkClass"));
                addIf(params, "sinkMethod", args.getString("sinkMethod"));
                addIf(params, "sinkDesc", args.getString("sinkDesc"));
                addIf(params, "sourceClass", args.getString("sourceClass"));
                addIf(params, "sourceMethod", args.getString("sourceMethod"));
                addIf(params, "sourceDesc", args.getString("sourceDesc"));
                addIf(params, "searchAllSources", args.getString("searchAllSources"));
                addIf(params, "onlyFromWeb", args.getString("onlyFromWeb"));
                addIf(params, "depth", args.getString("depth"));
                addIf(params, "maxLimit", args.getString("maxLimit"));
                addIf(params, "maxPaths", args.getString("maxPaths"));
                addIf(params, "timeoutMs", args.getString("timeoutMs"));
                addIf(params, "blacklist", args.getString("blacklist"));
                return call(api, "/api/flow/dfs", params);
            }
            if ("taint".equals(engine)) {
                String dfsJobId = require(args, "dfsJobId");
                params.put("dfsJobId", dfsJobId);
                addIf(params, "timeoutMs", args.getString("timeoutMs"));
                addIf(params, "maxPaths", args.getString("maxPaths"));
                return call(api, "/api/flow/taint", params);
            }
            return McpToolResult.error("engine must be dfs or taint");
        }));

        // flow_job
        JSONObject job = McpToolSchemas.tool("flow_job",
                "Query DFS/taint job status or results.");
        McpToolSchemas.addString(job, "engine", true, "dfs|taint");
        McpToolSchemas.addString(job, "jobId", true, "Job id.");
        McpToolSchemas.addString(job, "action", false, "status|results|cancel (optional, default status).");
        McpToolSchemas.addString(job, "offset", false, "Results offset (optional).");
        McpToolSchemas.addString(job, "limit", false, "Results limit (optional).");
        McpToolSchemas.addString(job, "compact", false, "Compact results (optional, dfs only).");
        reg.add(new McpTool("flow_job", job, (ctx, args) -> {
            String engine = require(args, "engine").trim().toLowerCase(Locale.ROOT);
            String jobId = require(args, "jobId").trim();
            String action = args.getString("action");
            if (action == null) {
                action = "";
            }
            action = action.trim().toLowerCase(Locale.ROOT);
            String path = "/api/flow/" + engine + "/jobs/" + jobId;
            Map<String, String> params = new HashMap<>();
            if ("results".equals(action)) {
                path = path + "/results";
                addIf(params, "offset", args.getString("offset"));
                addIf(params, "limit", args.getString("limit"));
                addIf(params, "compact", args.getString("compact"));
            } else if ("cancel".equals(action)) {
                path = path + "/cancel";
            }
            return call(api, path, params);
        }));
    }

    private static void registerQueryTools(McpToolRegistry reg, JarAnalyzerApiInvoker api) {
        JSONObject cypher = McpToolSchemas.tool("query_cypher", "Execute read-only Cypher query.");
        McpToolSchemas.addString(cypher, "query", true, "Cypher query text.");
        McpToolSchemas.addString(cypher, "params", false, "JSON object string for query params (optional).");
        McpToolSchemas.addString(cypher, "options", false,
                "JSON object string for options(maxRows,maxMs,maxHops,maxPaths,profile,expandBudget,pathBudget,timeoutCheckInterval).");
        reg.add(new McpTool("query_cypher", cypher, (ctx, args) -> {
            try {
                String query = require(args, "query");
                JSONObject body = buildQueryBody(query, args.getString("params"), args.getString("options"));
                return callPost(api, "/api/query/cypher", body);
            } catch (Exception ex) {
                return McpToolResult.error(ex.getMessage());
            }
        }));

        JSONObject explain = McpToolSchemas.tool("cypher_explain", "Explain Cypher logical plan.");
        McpToolSchemas.addString(explain, "query", true, "Cypher query text.");
        reg.add(new McpTool("cypher_explain", explain, (ctx, args) -> {
            try {
                String query = require(args, "query");
                JSONObject body = new JSONObject();
                body.put("query", query);
                return callPost(api, "/api/query/cypher/explain", body);
            } catch (Exception ex) {
                return McpToolResult.error(ex.getMessage());
            }
        }));

        JSONObject taint = McpToolSchemas.tool("taint_chain_cypher",
                "Run taint chain tracking through Cypher procedure ja.taint.track.");
        McpToolSchemas.addString(taint, "sourceClass", true, "Source class.");
        McpToolSchemas.addString(taint, "sourceMethod", true, "Source method.");
        McpToolSchemas.addString(taint, "sourceDesc", true, "Source descriptor.");
        McpToolSchemas.addString(taint, "sinkClass", true, "Sink class.");
        McpToolSchemas.addString(taint, "sinkMethod", true, "Sink method.");
        McpToolSchemas.addString(taint, "sinkDesc", true, "Sink descriptor.");
        McpToolSchemas.addString(taint, "depth", false, "Max hops/depth (optional).");
        McpToolSchemas.addString(taint, "timeoutMs", false, "Taint timeoutMs (optional).");
        McpToolSchemas.addString(taint, "maxPaths", false, "Taint maxPaths (optional).");
        McpToolSchemas.addString(taint, "maxRows", false, "Query maxRows (optional).");
        reg.add(new McpTool("taint_chain_cypher", taint, (ctx, args) -> {
            try {
                String sourceClass = require(args, "sourceClass");
                String sourceMethod = require(args, "sourceMethod");
                String sourceDesc = require(args, "sourceDesc");
                String sinkClass = require(args, "sinkClass");
                String sinkMethod = require(args, "sinkMethod");
                String sinkDesc = require(args, "sinkDesc");
                String depth = safeArg(args.getString("depth"), "8");
                String timeoutMs = safeArg(args.getString("timeoutMs"), "15000");
                String maxPaths = safeArg(args.getString("maxPaths"), "500");
                String maxRows = safeArg(args.getString("maxRows"), "500");

                String query = "CALL ja.taint.track($sourceClass,$sourceMethod,$sourceDesc,$sinkClass,$sinkMethod,$sinkDesc,$depth,$timeoutMs,$maxPaths) RETURN *";
                JSONObject params = new JSONObject();
                params.put("sourceClass", sourceClass);
                params.put("sourceMethod", sourceMethod);
                params.put("sourceDesc", sourceDesc);
                params.put("sinkClass", sinkClass);
                params.put("sinkMethod", sinkMethod);
                params.put("sinkDesc", sinkDesc);
                params.put("depth", depth);
                params.put("timeoutMs", timeoutMs);
                params.put("maxPaths", maxPaths);
                JSONObject options = new JSONObject();
                options.put("maxRows", maxRows);

                JSONObject body = new JSONObject();
                body.put("query", query);
                body.put("params", params);
                body.put("options", options);
                return callPost(api, "/api/query/cypher", body);
            } catch (Exception ex) {
                return McpToolResult.error(ex.getMessage());
            }
        }));
    }

    private static McpToolResult call(JarAnalyzerApiInvoker api, String path, Map<String, String> params) {
        try {
            String out = api.get(path, params);
            return McpToolResult.ok(out);
        } catch (Exception ex) {
            return McpToolResult.error(ex.getMessage());
        }
    }

    private static McpToolResult callPost(JarAnalyzerApiInvoker api, String path, JSONObject body) {
        try {
            String out = api.postJson(path, body == null ? "{}" : body.toJSONString());
            return McpToolResult.ok(out);
        } catch (Exception ex) {
            return McpToolResult.error(ex.getMessage());
        }
    }

    private static JSONObject buildQueryBody(String query, String paramsRaw, String optionsRaw) {
        JSONObject body = new JSONObject();
        body.put("query", query);
        JSONObject params = parseJsonObject(paramsRaw, "params");
        JSONObject options = parseJsonObject(optionsRaw, "options");
        if (params != null && !params.isEmpty()) {
            body.put("params", params);
        }
        if (options != null && !options.isEmpty()) {
            body.put("options", options);
        }
        return body;
    }

    private static JSONObject parseJsonObject(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            Object obj = JSON.parse(raw);
            if (obj instanceof JSONObject jsonObject) {
                return jsonObject;
            }
            throw new IllegalArgumentException(field + " must be json object");
        } catch (Exception ex) {
            throw new IllegalArgumentException("invalid " + field + " json: " + ex.getMessage());
        }
    }

    private static String safeArg(String value, String def) {
        String v = value == null ? "" : value.trim();
        return v.isEmpty() ? def : v;
    }

    private static void addIf(Map<String, String> params, String key, String value) {
        if (params == null || key == null) {
            return;
        }
        if (value == null) {
            return;
        }
        String v = value.strip();
        if (!v.isEmpty()) {
            params.put(key, v);
        }
    }

    private static String require(JSONObject args, String key) {
        if (args == null || key == null) {
            throw new IllegalArgumentException("required argument not found");
        }
        String v = args.getString(key);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("required argument \"" + key + "\" not found");
        }
        return v.strip();
    }
}
