/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.server.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.rules.SinkRuleRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GetSinkRulesHandlerTest {
    private static final String PROP = "jar.analyzer.rules.sink.path";
    private String originalSinkPath = System.getProperty(PROP);

    @TempDir
    Path tempDir;

    @AfterEach
    void cleanup() {
        restoreProperty(PROP, originalSinkPath);
        SinkRuleRegistry.reload();
    }

    @Test
    void shouldReportNormalizedSinkEntryCount() throws Exception {
        Path sinkFile = tempDir.resolve("sink.json");
        Files.writeString(sinkFile, """
                {
                  "name": "test-sinks",
                  "levels": {
                    "high": {
                      "sql": [
                        {"className":"demo/A","methodName":"one","methodDesc":"()V"},
                        {"className":"demo/B","methodName":"two","methodDesc":"()V"}
                      ]
                    }
                  }
                }
                """, StandardCharsets.UTF_8);
        System.setProperty(PROP, sinkFile.toString());
        SinkRuleRegistry.reload();

        NanoHTTPD.Response response = new GetSinkRulesHandler().handle(null);
        JSONObject root = JSON.parseObject(readBody(response));
        JSONObject data = root.getJSONObject("data");

        assertEquals(2, SinkRuleRegistry.getSinkModels().size());
        assertEquals(2, data.getIntValue("count"));
    }

    private static String readBody(NanoHTTPD.Response response) throws Exception {
        try (InputStream in = response.getData()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
