/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.server;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import me.n1ar4.jar.analyzer.mcp.backend.JarAnalyzerApiInvoker;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryApiHandlersTest {
    @Test
    void cypherEndpointShouldReturnUnifiedShapeAndSqlShouldBeOffline() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        JSONObject sqlBody = new JSONObject();
        sqlBody.put("query", "select 1 as x");
        Exception sqlError = assertThrows(Exception.class,
                () -> api.postJson("/api/query/sql", sqlBody.toJSONString()));
        assertTrue(sqlError.getMessage().contains("sql_api_removed"));

        JSONObject cypherBody = new JSONObject();
        cypherBody.put("query", "RETURN 1 AS x");
        String cypherOut = api.postJson("/api/query/cypher", cypherBody.toJSONString());
        JSONObject cypherJson = JSON.parseObject(cypherOut);
        assertEquals(true, cypherJson.getBoolean("ok"));
        assertTrue(cypherJson.getJSONObject("data").containsKey("columns"));
        assertTrue(cypherJson.getJSONObject("data").containsKey("rows"));
        assertTrue(cypherJson.containsKey("meta"));
    }

    @Test
    void cypherExplainAndCapabilitiesShouldWork() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        JSONObject explainBody = new JSONObject();
        explainBody.put("query", "RETURN 1 AS x");
        String explainOut = api.postJson("/api/query/cypher/explain", explainBody.toJSONString());
        JSONObject explainJson = JSON.parseObject(explainOut);
        assertEquals(true, explainJson.getBoolean("ok"));
        assertTrue(explainJson.getJSONObject("data").containsKey("operators"));

        String capabilitiesOut = api.get("/api/query/cypher/capabilities", Map.of());
        JSONObject capabilitiesJson = JSON.parseObject(capabilitiesOut);
        assertEquals(true, capabilitiesJson.getBoolean("ok"));
        JSONObject data = capabilitiesJson.getJSONObject("data");
        assertEquals(true, data.getBoolean("readOnly"));
        assertEquals("neo4j-embedded-lite", data.getString("engine"));
        assertTrue(data.getJSONArray("profiles").contains("long-chain"));
        assertTrue(data.getJSONArray("options").contains("expandBudget"));
        assertTrue(data.getJSONArray("options").contains("pathBudget"));
        assertTrue(data.getJSONArray("options").contains("timeoutCheckInterval"));
    }

    @Test
    void cypherEndpointShouldRejectCommandStatements() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        JSONObject body = new JSONObject();
        body.put("query", "SHOW INDEXES");
        Exception error = assertThrows(Exception.class,
                () -> api.postJson("/api/query/cypher", body.toJSONString()));
        assertTrue(error.getMessage().contains("cypher_parse_error")
                || error.getMessage().contains("unsupported"));
    }

    @Test
    void cypherEndpointShouldRejectLoadCsv() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        JSONObject body = new JSONObject();
        body.put("query", "LOAD CSV FROM 'file:///tmp/a.csv' AS row RETURN row");
        Exception error = assertThrows(Exception.class,
                () -> api.postJson("/api/query/cypher", body.toJSONString()));
        String msg = error.getMessage();
        assertTrue(msg.contains("http 400"));
        assertTrue(msg.contains("cypher_read_only")
                || msg.contains("cypher_parse_error")
                || msg.contains("feature disabled: LOAD CSV"));
    }

    @Test
    void cypherEndpointShouldRejectGraphReferenceRuntimePath() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        JSONObject body = new JSONObject();
        body.put("query", "USE graph.byName('neo4j') RETURN 1 AS x");
        assertThrows(Exception.class,
                () -> api.postJson("/api/query/cypher", body.toJSONString()));
    }

    @Test
    void cypherEndpointShouldEnforceSingleRuntime() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        JSONObject okBody = new JSONObject();
        okBody.put("query", "CYPHER runtime=slotted RETURN 1 AS x");
        String okOut = api.postJson("/api/query/cypher", okBody.toJSONString());
        JSONObject okJson = JSON.parseObject(okOut);
        assertEquals(true, okJson.getBoolean("ok"));

        JSONObject badBody = new JSONObject();
        badBody.put("query", "CYPHER runtime=interpreted RETURN 1 AS x");
        Exception badError = assertThrows(Exception.class,
                () -> api.postJson("/api/query/cypher", badBody.toJSONString()));
        String msg = badError.getMessage();
        assertTrue(msg.contains("runtime")
                || msg.contains("unsupported")
                || msg.contains("cypher_parse_error"));
    }
}
