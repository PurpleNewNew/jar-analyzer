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

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.taint.TaintSemanticSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaintSemanticSummaryReloadTest {
    private static final String MODEL_PROP = "jar.analyzer.rules.model.path";
    private static final String SOURCE_PROP = "jar.analyzer.rules.source.path";
    private static final String SINK_PROP = "jar.analyzer.rules.sink.path";

    @TempDir
    Path tempDir;

    @Test
    void shouldInvalidateSemanticSummaryCachesWhenRulesReload() throws Exception {
        Path model = tempDir.resolve("model.json");
        Path source = tempDir.resolve("source.json");
        Path sink = tempDir.resolve("sink.json");

        String oldModelProp = System.getProperty(MODEL_PROP);
        String oldSourceProp = System.getProperty(SOURCE_PROP);
        String oldSinkProp = System.getProperty(SINK_PROP);
        try {
            Files.writeString(model, buildModelJson(true), StandardCharsets.UTF_8);
            Files.writeString(source,
                    "{\"sourceAnnotations\":[],\"sourceModel\":[]}",
                    StandardCharsets.UTF_8);
            Files.writeString(sink, "{\"name\":\"reload-test\",\"levels\":{}}", StandardCharsets.UTF_8);

            System.setProperty(MODEL_PROP, model.toString());
            System.setProperty(SOURCE_PROP, source.toString());
            System.setProperty(SINK_PROP, sink.toString());

            DatabaseManager.clearAllData();
            SinkRuleRegistry.reload();
            ModelRegistry.reload();

            TaintSemanticSummary.ReturnFlowSummary firstSummary = TaintSemanticSummary.resolveReturnFlow(
                    ModelRegistry.getSummaryModelRule(),
                    "demo/Target",
                    "copy",
                    "(Ljava/lang/Object;)Ljava/lang/Object;",
                    1
            );
            TaintSemanticSummary.CallGateDecision firstGate = TaintSemanticSummary.resolveCallGate(
                    ModelRegistry.getSummaryModelRule(),
                    "demo/Target",
                    "copy",
                    "(Ljava/lang/Object;)Ljava/lang/Object;",
                    1,
                    Set.of(0)
            );
            assertTrue(firstSummary.isKnown());
            assertTrue(firstSummary.allowsAny(Set.of(0)));
            assertTrue(firstGate.isAllowed());

            Files.writeString(model, buildModelJson(false), StandardCharsets.UTF_8);
            ModelRegistry.reload();

            TaintSemanticSummary.ReturnFlowSummary secondSummary = TaintSemanticSummary.resolveReturnFlow(
                    ModelRegistry.getSummaryModelRule(),
                    "demo/Target",
                    "copy",
                    "(Ljava/lang/Object;)Ljava/lang/Object;",
                    1
            );
            TaintSemanticSummary.CallGateDecision secondGate = TaintSemanticSummary.resolveCallGate(
                    ModelRegistry.getSummaryModelRule(),
                    "demo/Target",
                    "copy",
                    "(Ljava/lang/Object;)Ljava/lang/Object;",
                    1,
                    Set.of(0)
            );
            assertFalse(secondSummary.isKnown());
            assertFalse(secondGate.isKnown());
        } finally {
            DatabaseManager.clearAllData();
            restore(MODEL_PROP, oldModelProp);
            restore(SOURCE_PROP, oldSourceProp);
            restore(SINK_PROP, oldSinkProp);
            SinkRuleRegistry.reload();
            ModelRegistry.reload();
        }
    }

    private static String buildModelJson(boolean enabled) {
        if (!enabled) {
            return "{\"summaryModel\":[],\"additionalStepHints\":[]}";
        }
        return "{\n"
                + "  \"summaryModel\": [\n"
                + "    {\n"
                + "      \"className\": \"demo/Target\",\n"
                + "      \"methodName\": \"copy\",\n"
                + "      \"methodDesc\": \"(Ljava/lang/Object;)Ljava/lang/Object;\",\n"
                + "      \"flows\": [\n"
                + "        {\n"
                + "          \"from\": \"arg0\",\n"
                + "          \"to\": \"return\"\n"
                + "        }\n"
                + "      ],\n"
                + "      \"subtypes\": false\n"
                + "    }\n"
                + "  ],\n"
                + "  \"additionalStepHints\": []\n"
                + "}";
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
