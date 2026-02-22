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
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import me.n1ar4.jar.analyzer.mcp.backend.JarAnalyzerApiInvoker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryApiHandlersTest {
    @Test
    void sqlEndpointShouldReturnUnifiedShape() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        JSONObject sqlBody = new JSONObject();
        sqlBody.put("query", "select 1 as x");
        String sqlOut = api.postJson("/api/query/sql", sqlBody.toJSONString());
        JSONObject sqlJson = JSON.parseObject(sqlOut);
        assertEquals(true, sqlJson.getBoolean("ok"));
        assertTrue(sqlJson.getJSONObject("data").containsKey("columns"));
        assertTrue(sqlJson.getJSONObject("data").containsKey("rows"));
        assertTrue(sqlJson.containsKey("meta"));
    }

    @Test
    void sqlEndpointShouldBindNamedParams() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());

        JSONObject sqlBody = new JSONObject();
        sqlBody.put("query", "select :v as x");
        JSONObject params = new JSONObject();
        params.put("v", 7);
        sqlBody.put("params", params);

        String sqlOut = api.postJson("/api/query/sql", sqlBody.toJSONString());
        JSONObject sqlJson = JSON.parseObject(sqlOut);
        assertEquals(true, sqlJson.getBoolean("ok"));
        JSONArray rows = sqlJson.getJSONObject("data").getJSONArray("rows");
        Number value = rows.getJSONArray(0).getObject(0, Number.class);
        assertEquals(7, value.intValue());
    }
}
