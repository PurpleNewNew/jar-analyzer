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
import me.n1ar4.jar.analyzer.taint.Sanitizer;
import me.n1ar4.jar.analyzer.taint.TaintGuardRule;
import me.n1ar4.jar.analyzer.taint.TaintModelRule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleRegistryReloadTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void cleanup() {
        RuleRegistryTestSupport.clearRuleFiles();
    }

    @Test
    void shouldHotReloadModelAndSinkRulesWhenFilesChange() throws Exception {
        Path model = tempDir.resolve("model.json");
        Path source = tempDir.resolve("source.json");
        Path sink = tempDir.resolve("sink.json");

        try {
            Files.writeString(model, "{\"summaryModel\":[],\"additionalStepHints\":[]}", StandardCharsets.UTF_8);
            Files.writeString(source,
                    "{\"sourceAnnotations\":[\"Ldemo/Old;\"],\"sourceModel\":[]}",
                    StandardCharsets.UTF_8);
            Files.writeString(sink, buildSinkJson("jndi"), StandardCharsets.UTF_8);

            RuleRegistryTestSupport.useRuleFiles(model, source, sink);
            long sinkV1 = SinkRuleRegistry.getVersion();
            long modelV1 = ModelRegistry.getVersion();
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
            RuleRegistryTestSupport.clearRuleFiles();
        }
    }

    @Test
    void shouldCompileDslRulesIntoExistingUnifiedModelShapes() throws Exception {
        Path model = tempDir.resolve("model-dsl.json");
        Path source = tempDir.resolve("source-dsl.json");
        Path sink = tempDir.resolve("sink-dsl.json");

        try {
            Files.writeString(model, """
                    {
                      "summaryModel": [],
                      "additionalTaintSteps": [],
                      "sanitizerModel": [],
                      "dsl": {
                        "rules": [
                          {
                            "id": "summary-pass",
                            "kind": "summary",
                            "match": {
                              "className": "demo/Flow",
                              "methodName": "pass",
                              "methodDesc": "(Ljava/lang/String;)Ljava/lang/String;",
                              "subtypes": true
                            },
                            "flows": [
                              {
                                "from": "arg0",
                                "to": "return"
                              }
                            ]
                          },
                          {
                            "id": "additional-bind",
                            "kind": "additional",
                            "match": {
                              "className": "demo/Extra",
                              "methodName": "bind",
                              "methodDesc": "(Ljava/lang/String;)V"
                            },
                            "flows": [
                              {
                                "from": "arg0",
                                "to": "this.value"
                              }
                            ]
                          },
                          {
                            "id": "sanitize-clean",
                            "kind": "sanitizer",
                            "match": {
                              "className": "demo/Safe",
                              "methodName": "clean",
                              "methodDesc": "(Ljava/lang/String;)Ljava/lang/String;"
                            },
                            "target": "arg0",
                            "sanitizerKind": "sql"
                          }
                        ]
                      }
                    }
                    """, StandardCharsets.UTF_8);
            Files.writeString(source, "{\"sourceAnnotations\":[],\"sourceModel\":[]}", StandardCharsets.UTF_8);
            Files.writeString(sink, "{\"name\":\"dsl-test\",\"levels\":{}}", StandardCharsets.UTF_8);

            RuleRegistryTestSupport.useRuleFiles(model, source, sink);

            TaintModelRule summaryRule = ModelRegistry.getSummaryModelRule();
            assertEquals(1, summaryRule.getRules().size());
            assertEquals("demo/Flow", summaryRule.getRules().get(0).getClassName());
            assertEquals("pass", summaryRule.getRules().get(0).getMethodName());
            assertEquals("(Ljava/lang/String;)Ljava/lang/String;", summaryRule.getRules().get(0).getMethodDesc());
            assertTrue(Boolean.TRUE.equals(summaryRule.getRules().get(0).getSubtypes()));
            assertEquals("arg0", summaryRule.getRules().get(0).getFlows().get(0).getFrom());
            assertEquals("return", summaryRule.getRules().get(0).getFlows().get(0).getTo());

            TaintModelRule additionalRule = ModelRegistry.getAdditionalModelRule();
            assertEquals(1, additionalRule.getRules().size());
            assertEquals("demo/Extra", additionalRule.getRules().get(0).getClassName());
            assertEquals("this.value", additionalRule.getRules().get(0).getFlows().get(0).getTo());

            Sanitizer sanitizer = ModelRegistry.getSanitizerRule().getRules().get(0);
            assertEquals("demo/Safe", sanitizer.getClassName());
            assertEquals("clean", sanitizer.getMethodName());
            assertEquals("(Ljava/lang/String;)Ljava/lang/String;", sanitizer.getMethodDesc());
            assertEquals(0, sanitizer.getParamIndex());
            assertEquals("sql", sanitizer.getKind());
        } finally {
            RuleRegistryTestSupport.clearRuleFiles();
        }
    }

    @Test
    void shouldCompileGuardAndPruningDslAndExposeValidationSummary() throws Exception {
        Path model = tempDir.resolve("model-guard-dsl.json");
        Path source = tempDir.resolve("source-guard-dsl.json");
        Path sink = tempDir.resolve("sink-guard-dsl.json");

        try {
            Files.writeString(model, """
                    {
                      "summaryModel": [],
                      "guardSanitizers": [],
                      "additionalStepHints": [],
                      "dsl": {
                        "rules": [
                          {
                            "id": "guard-prefix",
                            "kind": "guard",
                            "match": {
                              "className": "java/lang/String",
                              "methodName": "startsWith",
                              "methodDesc": "(Ljava/lang/String;)Z"
                            },
                            "type": "path-prefix",
                            "paramIndex": 0,
                            "allowlist": ["/safe/*"],
                            "enabled": true,
                            "mode": "hard",
                            "requireNormalized": true,
                            "appliesToKind": "file"
                          },
                          {
                            "id": "hint-extra",
                            "kind": "pruning-hint",
                            "hints": ["collection", "stream", "bad-hint"]
                          },
                          {
                            "id": "wrong-domain",
                            "kind": "source",
                            "match": {
                              "className": "demo/Bad",
                              "methodName": "entry",
                              "methodDesc": "()V"
                            }
                          }
                        ]
                      }
                    }
                    """, StandardCharsets.UTF_8);
            Files.writeString(source, "{\"sourceAnnotations\":[],\"sourceModel\":[]}", StandardCharsets.UTF_8);
            Files.writeString(sink, "{\"name\":\"dsl-test\",\"levels\":{}}", StandardCharsets.UTF_8);

            RuleRegistryTestSupport.useRuleFiles(model, source, sink);

            assertEquals(1, ModelRegistry.getGuardRules().size());
            TaintGuardRule guardRule = ModelRegistry.getGuardRules().get(0);
            assertEquals("file", guardRule.getKind());
            assertEquals("path-prefix", guardRule.getType());
            assertEquals(0, guardRule.getParamIndex());
            assertEquals("hard", guardRule.getMode());
            assertEquals(true, guardRule.getRequireNormalized());

            assertTrue(ModelRegistry.getAdditionalStepHints().contains("container"));
            assertTrue(ModelRegistry.getAdditionalStepHints().contains("stream"));
            assertFalse(ModelRegistry.getAdditionalStepHints().contains("bad-hint"));

            RuleValidationSummary validation = ModelRegistry.getRuleValidation();
            assertEquals(2, validation.getCompiledRules());
            assertEquals(1, validation.getRejectedRules());
            assertEquals(1, validation.getErrorCount());
            assertEquals(1, validation.getWarningCount());
            assertTrue(validation.toMap().toString().contains("unsupported_dsl_kind"));
            assertTrue(validation.toMap().toString().contains("pruning_hint_invalid"));
        } finally {
            RuleRegistryTestSupport.clearRuleFiles();
        }
    }

    @Test
    void shouldSplitModelAndSourceValidationScopes() throws Exception {
        Path model = tempDir.resolve("model-scope.json");
        Path source = tempDir.resolve("source-scope.json");
        Path sink = tempDir.resolve("sink-scope.json");

        try {
            Files.writeString(model, """
                    {
                      "summaryModel": [],
                      "dsl": {
                        "rules": [
                          {
                            "id": "wrong-model-kind",
                            "kind": "source",
                            "match": {
                              "className": "demo/Bad",
                              "methodName": "entry",
                              "methodDesc": "()V"
                            }
                          }
                        ]
                      }
                    }
                    """, StandardCharsets.UTF_8);
            Files.writeString(source, """
                    {
                      "sourceAnnotations": [],
                      "sourceModel": [],
                      "dsl": {
                        "rules": [
                          {
                            "id": "missing-annotation",
                            "kind": "source-annotation"
                          }
                        ]
                      }
                    }
                    """, StandardCharsets.UTF_8);
            Files.writeString(sink, "{\"name\":\"scope-test\",\"levels\":{}}", StandardCharsets.UTF_8);

            RuleRegistryTestSupport.useRuleFiles(model, source, sink);

            Map<String, Object> combined = RuleValidationViews.combinedValidationMap();
            assertTrue(combined.containsKey("model"));
            assertTrue(combined.containsKey("source"));
            assertTrue(combined.containsKey("modelSource"));

            List<Map<String, Object>> modelIssues = RuleValidationViews.issueMaps("model");
            assertEquals(1, modelIssues.size());
            assertEquals("model", modelIssues.get(0).get("scope"));
            assertEquals("unsupported_dsl_kind", modelIssues.get(0).get("code"));

            List<Map<String, Object>> sourceIssues = RuleValidationViews.issueMaps("source");
            assertEquals(1, sourceIssues.size());
            assertEquals("source", sourceIssues.get(0).get("scope"));
            assertEquals("source_annotation_missing", sourceIssues.get(0).get("code"));

            List<Map<String, Object>> mergedIssues = RuleValidationViews.issueMaps("modelSource");
            assertEquals(2, mergedIssues.size());
            assertNotNull(mergedIssues.stream().filter(item -> "model".equals(item.get("scope"))).findFirst().orElse(null));
            assertNotNull(mergedIssues.stream().filter(item -> "source".equals(item.get("scope"))).findFirst().orElse(null));

            assertThrows(IllegalArgumentException.class, () -> RuleValidationViews.issueMaps("bogus"));
        } finally {
            RuleRegistryTestSupport.clearRuleFiles();
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

}
