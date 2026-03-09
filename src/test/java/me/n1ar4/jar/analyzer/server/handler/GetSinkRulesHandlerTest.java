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

    @Test
    void shouldExposeSinkDslValidationAndCompiledEntries() throws Exception {
        Path sinkFile = tempDir.resolve("sink-dsl.json");
        Files.writeString(sinkFile, """
                {
                  "name": "dsl-sinks",
                  "levels": {},
                  "dsl": {
                    "rules": [
                      {
                        "id": "http-open",
                        "kind": "sink",
                        "match": {
                          "className": "demo/Net",
                          "methodName": "open",
                          "methodDesc": "(Ljava/lang/String;)V"
                        },
                        "sinkCategory": "ssrf",
                        "severity": "high",
                        "ruleTier": "hard",
                        "tags": ["outbound", "http"]
                      },
                      {
                        "id": "wrong-kind",
                        "kind": "summary",
                        "match": {
                          "className": "demo/Bad",
                          "methodName": "ignore"
                        }
                      }
                    ]
                  }
                }
                """, StandardCharsets.UTF_8);
        System.setProperty(PROP, sinkFile.toString());
        SinkRuleRegistry.reload();

        NanoHTTPD.Response response = new GetSinkRulesHandler().handle(null);
        JSONObject root = JSON.parseObject(readBody(response));
        JSONObject data = root.getJSONObject("data");
        JSONObject validation = data.getJSONObject("validation");

        assertEquals(1, SinkRuleRegistry.getSinkModels().size());
        assertEquals(1, data.getIntValue("count"));
        assertEquals(false, validation.getBoolean("ok"));
        assertEquals(1, validation.getIntValue("compiledRules"));
        assertEquals(1, validation.getIntValue("rejectedRules"));
        assertEquals(1, validation.getIntValue("errorCount"));
        assertEquals("ssrf", SinkRuleRegistry.getSinkModels().get(0).getCategory());
        assertEquals("hard", SinkRuleRegistry.getSinkModels().get(0).getRuleTier());
        assertEquals(2, SinkRuleRegistry.getSinkModels().get(0).getTags().size());
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
