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
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryApiHandlersTest {
    @Test
    void sqlAndCypherEndpointsShouldReturnUnifiedShape() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        JSONObject sqlBody = new JSONObject();
        sqlBody.put("query", "select 1 as x");
        String sqlOut = api.postJson("/api/query/sql", sqlBody.toJSONString());
        JSONObject sqlJson = JSON.parseObject(sqlOut);
        assertEquals(true, sqlJson.getBoolean("ok"));
        assertTrue(sqlJson.getJSONObject("data").containsKey("columns"));
        assertTrue(sqlJson.getJSONObject("data").containsKey("rows"));
        assertTrue(sqlJson.containsKey("meta"));

        JSONObject cypherBody = new JSONObject();
        cypherBody.put("query", "MATCH (m:Method)-[r]->(n) RETURN m, r, n LIMIT 5");
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
        explainBody.put("query", "MATCH (m:Method)-[r]->(n) RETURN m, r, n LIMIT 3");
        String explainOut = api.postJson("/api/query/cypher/explain", explainBody.toJSONString());
        JSONObject explainJson = JSON.parseObject(explainOut);
        assertEquals(true, explainJson.getBoolean("ok"));
        assertTrue(explainJson.getJSONObject("data").containsKey("operators"));

        String capabilitiesOut = api.get("/api/query/cypher/capabilities", Map.of());
        JSONObject capabilitiesJson = JSON.parseObject(capabilitiesOut);
        assertEquals(true, capabilitiesJson.getBoolean("ok"));
        JSONObject data = capabilitiesJson.getJSONObject("data");
        assertEquals(true, data.getBoolean("readOnly"));
        assertTrue(data.containsKey("procedures"));
    }
}

