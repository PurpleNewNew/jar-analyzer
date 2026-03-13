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
import me.n1ar4.jar.analyzer.taint.TaintAnalysisProfile;
import me.n1ar4.jar.analyzer.taint.TaintSemanticSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaintSemanticSummaryReloadTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
        RuleRegistryTestSupport.clearRuleFiles();
    }

    @Test
    void shouldInvalidateSemanticSummaryCachesWhenRulesReload() throws Exception {
        Path model = tempDir.resolve("model.json");
        Path source = tempDir.resolve("source.json");
        Path sink = tempDir.resolve("sink.json");

        try {
            Files.writeString(model, buildModelJson(true), StandardCharsets.UTF_8);
            Files.writeString(source,
                    "{\"sourceAnnotations\":[],\"sourceModel\":[]}",
                    StandardCharsets.UTF_8);
            Files.writeString(sink, "{\"name\":\"reload-test\",\"levels\":{}}", StandardCharsets.UTF_8);

            DatabaseManager.clearAllData();
            RuleRegistryTestSupport.useRuleFiles(model, source, sink);

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
            RuleRegistryTestSupport.clearRuleFiles();
        }
    }

    @Test
    void shouldRefreshAdditionalStepHintsWhenRulesReload() throws Exception {
        Path model = tempDir.resolve("model.json");
        Path source = tempDir.resolve("source.json");
        Path sink = tempDir.resolve("sink.json");

        try {
            Files.writeString(model, "{\"summaryModel\":[],\"additionalStepHints\":[]}", StandardCharsets.UTF_8);
            Files.writeString(source,
                    "{\"sourceAnnotations\":[],\"sourceModel\":[]}",
                    StandardCharsets.UTF_8);
            Files.writeString(sink, "{\"name\":\"reload-test\",\"levels\":{}}", StandardCharsets.UTF_8);

            RuleRegistryTestSupport.useRuleFiles(model, source, sink);
            assertFalse(TaintAnalysisProfile.current().isAdditionalEnabled(TaintAnalysisProfile.AdditionalStep.CONTAINER));

            Files.writeString(model,
                    "{\"summaryModel\":[],\"additionalStepHints\":[\"container\"]}",
                    StandardCharsets.UTF_8);
            ModelRegistry.reload();

            assertTrue(TaintAnalysisProfile.current().isAdditionalEnabled(TaintAnalysisProfile.AdditionalStep.CONTAINER));
        } finally {
            RuleRegistryTestSupport.clearRuleFiles();
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

}
