/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.rules;

import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleRegistryReloadTest {
    private static final String MODEL_PROP = "jar.analyzer.rules.model.path";
    private static final String SOURCE_PROP = "jar.analyzer.rules.source.path";
    private static final String SINK_PROP = "jar.analyzer.rules.sink.path";

    @TempDir
    Path tempDir;

    @Test
    void shouldHotReloadModelAndSinkRulesWhenFilesChange() throws Exception {
        Path model = tempDir.resolve("model.json");
        Path source = tempDir.resolve("source.json");
        Path sink = tempDir.resolve("sink.json");

        String oldModelProp = System.getProperty(MODEL_PROP);
        String oldSourceProp = System.getProperty(SOURCE_PROP);
        String oldSinkProp = System.getProperty(SINK_PROP);
        try {
            Files.writeString(model, "{\"summaryModel\":[],\"additionalStepHints\":[]}", StandardCharsets.UTF_8);
            Files.writeString(source,
                    "{\"sourceAnnotations\":[\"Ldemo/Old;\"],\"sourceModel\":[]}",
                    StandardCharsets.UTF_8);
            Files.writeString(sink, buildSinkJson("jndi"), StandardCharsets.UTF_8);

            System.setProperty(MODEL_PROP, model.toString());
            System.setProperty(SOURCE_PROP, source.toString());
            System.setProperty(SINK_PROP, sink.toString());

            long sinkV1 = SinkRuleRegistry.reload();
            long modelV1 = ModelRegistry.reload();
            assertTrue(sinkV1 > 0);
            assertTrue(modelV1 > 0);
            assertTrue(ModelRegistry.getSourceAnnotations().contains("Ldemo/Old;"));
            assertEquals("jndi", ModelRegistry.resolveSinkKind(sinkHandle()));
            String fp1 = ModelRegistry.getRulesFingerprint();

            long sourceSize = Files.size(source);
            long sinkSize = Files.size(sink);
            FileTime sourceTime = Files.getLastModifiedTime(source);
            FileTime sinkTime = Files.getLastModifiedTime(sink);

            Files.writeString(source,
                    "{\"sourceAnnotations\":[\"Ldemo/New;\"],\"sourceModel\":[]}",
                    StandardCharsets.UTF_8);
            Files.writeString(sink, buildSinkJson("sqlx"), StandardCharsets.UTF_8);
            assertEquals(sourceSize, Files.size(source));
            assertEquals(sinkSize, Files.size(sink));
            Files.setLastModifiedTime(source, sourceTime);
            Files.setLastModifiedTime(sink, sinkTime);

            long sinkV2 = SinkRuleRegistry.checkNow();
            long modelV2 = ModelRegistry.checkNow();
            String fp2 = ModelRegistry.getRulesFingerprint();
            assertTrue(sinkV2 > sinkV1);
            assertTrue(modelV2 > modelV1);
            assertTrue(ModelRegistry.getSourceAnnotations().contains("Ldemo/New;"));
            assertEquals("sql", ModelRegistry.resolveSinkKind(sinkHandle()));
            assertTrue(fp2 != null && !fp2.isBlank() && !fp2.equals(fp1));
        } finally {
            restore(MODEL_PROP, oldModelProp);
            restore(SOURCE_PROP, oldSourceProp);
            restore(SINK_PROP, oldSinkProp);
            SinkRuleRegistry.reload();
            ModelRegistry.reload();
        }
    }

    private static String buildSinkJson(String category) {
        return "{\n"
                + "  \"name\": \"reload-test\",\n"
                + "  \"levels\": {\n"
                + "    \"high\": {\n"
                + "      \"" + category + "\": [\n"
                + "        {\n"
                + "          \"className\": \"javax/naming/Context\",\n"
                + "          \"methodName\": \"lookup\",\n"
                + "          \"methodDesc\": \"(Ljava/lang/String;)Ljava/lang/Object;\"\n"
                + "        }\n"
                + "      ]\n"
                + "    }\n"
                + "  }\n"
                + "}";
    }

    private static MethodReference.Handle sinkHandle() {
        return new MethodReference.Handle(
                new ClassReference.Handle("javax/naming/Context"),
                "lookup",
                "(Ljava/lang/String;)Ljava/lang/Object;"
        );
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
