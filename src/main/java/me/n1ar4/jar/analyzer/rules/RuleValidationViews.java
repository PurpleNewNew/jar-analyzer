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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RuleValidationViews {
    private RuleValidationViews() {
    }

    public static Map<String, Object> combinedValidationMap() {
        RuleValidationSummary modelSourceValidation = ModelRegistry.getRuleValidation();
        RuleValidationSummary modelValidation = nestedScopeSummary(modelSourceValidation, "model");
        RuleValidationSummary sourceValidation = nestedScopeSummary(modelSourceValidation, "source");
        RuleValidationSummary sinkValidation = SinkRuleRegistry.getRuleValidation();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", modelSourceValidation.isOk() && sinkValidation.isOk());
        out.put("compiledRules", modelSourceValidation.getCompiledRules() + sinkValidation.getCompiledRules());
        out.put("rejectedRules", modelSourceValidation.getRejectedRules() + sinkValidation.getRejectedRules());
        out.put("errorCount", modelSourceValidation.getErrorCount() + sinkValidation.getErrorCount());
        out.put("warningCount", modelSourceValidation.getWarningCount() + sinkValidation.getWarningCount());
        out.put("model", modelValidation.toMap());
        out.put("source", sourceValidation.toMap());
        out.put("modelSource", modelSourceValidation.toMap());
        out.put("sink", sinkValidation.toMap());
        return out;
    }

    public static List<Map<String, Object>> issueMaps(String scope) {
        String normalized = normalizeScope(scope);
        if (normalized == null) {
            throw new IllegalArgumentException("invalid rule validation scope: " + safe(scope));
        }
        List<Map<String, Object>> out = new ArrayList<>();
        RuleValidationSummary modelSourceValidation = ModelRegistry.getRuleValidation();
        if ("all".equals(normalized) || "model".equals(normalized) || "modelSource".equals(normalized)) {
            collectIssues(out, nestedScopeSummary(modelSourceValidation, "model"), "model");
        }
        if ("all".equals(normalized) || "source".equals(normalized) || "modelSource".equals(normalized)) {
            collectIssues(out, nestedScopeSummary(modelSourceValidation, "source"), "source");
        }
        if ("all".equals(normalized) || "sink".equals(normalized)) {
            collectIssues(out, SinkRuleRegistry.getRuleValidation(), "sink");
        }
        return out;
    }

    public static boolean isSupportedScope(String scope) {
        return normalizeScope(scope) != null;
    }

    private static void collectIssues(List<Map<String, Object>> out, RuleValidationSummary summary, String scope) {
        if (out == null || summary == null) {
            return;
        }
        if (!summary.getFiles().isEmpty()) {
            for (RuleValidationSummary file : summary.getFiles()) {
                collectIssues(out, file, normalizeIssueScope(file, scope));
            }
            return;
        }
        for (RuleValidationIssue issue : summary.getIssues()) {
            if (issue == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>(issue.toMap());
            item.put("scope", normalizeIssueScope(summary, scope));
            if (!summary.getName().isBlank()) {
                item.put("domain", summary.getName());
            }
            if (!summary.getPath().isBlank()) {
                item.put("path", summary.getPath());
            }
            out.add(item);
        }
    }

    private static String normalizeScope(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || "all".equals(normalized)) {
            return "all";
        }
        if ("modelsource".equals(normalized) || "model/source".equals(normalized) || "model_source".equals(normalized)) {
            return "modelSource";
        }
        if ("model".equals(normalized)) {
            return "model";
        }
        if ("source".equals(normalized)) {
            return "source";
        }
        if ("sink".equals(normalized)) {
            return "sink";
        }
        return null;
    }

    private static RuleValidationSummary nestedScopeSummary(RuleValidationSummary summary, String scope) {
        if (summary == null) {
            return emptySummary(scope);
        }
        for (RuleValidationSummary file : summary.getFiles()) {
            if (file == null) {
                continue;
            }
            if (scope.equalsIgnoreCase(file.getName())) {
                return file;
            }
        }
        return emptySummary(scope);
    }

    private static RuleValidationSummary emptySummary(String name) {
        return RuleValidationSummary.builder(name, "").build();
    }

    private static String normalizeIssueScope(RuleValidationSummary summary, String fallback) {
        if (summary == null) {
            return fallback;
        }
        String name = safe(summary.getName()).toLowerCase(Locale.ROOT);
        if ("model".equals(name) || "source".equals(name) || "sink".equals(name)) {
            return name;
        }
        return fallback;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
