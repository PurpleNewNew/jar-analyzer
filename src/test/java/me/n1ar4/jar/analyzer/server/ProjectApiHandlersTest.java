/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.server;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import me.n1ar4.jar.analyzer.mcp.backend.JarAnalyzerApiInvoker;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectApiHandlersTest {
    @Test
    void projectApiShouldSwitchAndDropStore() throws Exception {
        JarAnalyzerApiInvoker api = new JarAnalyzerApiInvoker(new ServerConfig());
        String project = "api-project-test";

        String storesBefore = api.get("/api/project/stores", Map.of());
        JSONObject storesBeforeJson = JSON.parseObject(storesBefore);
        assertTrue(storesBeforeJson.getBooleanValue("ok"));

        JSONObject selectBody = new JSONObject();
        selectBody.put("project", project);
        String selectOut = api.postJson("/api/project/select", selectBody.toJSONString());
        JSONObject selectJson = JSON.parseObject(selectOut);
        assertTrue(selectJson.getBooleanValue("ok"));
        assertEquals(project, selectJson.getJSONObject("data").getString("project"));

        String storesAfter = api.get("/api/project/stores", Map.of());
        JSONObject storesAfterJson = JSON.parseObject(storesAfter);
        assertTrue(storesAfterJson.getBooleanValue("ok"));
        assertEquals(project, storesAfterJson.getJSONObject("data").getString("activeProject"));

        JSONObject backBody = new JSONObject();
        backBody.put("project", "default");
        String backOut = api.postJson("/api/project/select", backBody.toJSONString());
        JSONObject backJson = JSON.parseObject(backOut);
        assertTrue(backJson.getBooleanValue("ok"));
        assertEquals("default", backJson.getJSONObject("data").getString("project"));

        JSONObject dropBody = new JSONObject();
        dropBody.put("project", project);
        String dropOut = api.postJson("/api/project/drop", dropBody.toJSONString());
        JSONObject dropJson = JSON.parseObject(dropOut);
        assertTrue(dropJson.getBooleanValue("ok"));
        assertTrue(dropJson.getJSONObject("data").getBooleanValue("deleted"));
    }
}

