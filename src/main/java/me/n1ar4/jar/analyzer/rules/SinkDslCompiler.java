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

import me.n1ar4.jar.analyzer.engine.SearchCondition;
import me.n1ar4.jar.analyzer.rules.sink.SinkRule;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SinkDslCompiler {
    private static final Logger logger = LogManager.getLogger();

    private SinkDslCompiler() {
    }

    public static RuleValidationSummary compileInto(SinkRule rule, String rulePath) {
        RuleValidationSummary.Builder validation = RuleValidationSummary.builder("sink", rulePath);
        if (rule == null || rule.getDsl() == null || rule.getDsl().getRules() == null || rule.getDsl().getRules().isEmpty()) {
            return validation.build();
        }
        Map<String, Map<String, List<SearchCondition>>> levels = copyLevels(rule.getLevels());
        int index = 0;
        for (RuleDslConfig.RuleSpec spec : rule.getDsl().getRules()) {
            index++;
            if (spec == null) {
                continue;
            }
            String normalizedKind = normalize(spec.getKind());
            if (!"sink".equals(normalizedKind)) {
                reject(validation, rulePath, index, spec, "unsupported_sink_dsl_kind",
                        "sink.json dsl.rules[] only supports kind=sink");
                continue;
            }
            SearchCondition compiled = compileSinkRule(spec, validation, rulePath, index);
            if (compiled == null) {
                continue;
            }
            String severity = SinkRuleSupport.normalizeSeverity(spec.getSeverity(), "medium");
            String category = normalize(spec.getSinkCategory());
            levels.computeIfAbsent(severity, ignore -> new LinkedHashMap<>())
                    .computeIfAbsent(category, ignore -> new ArrayList<>())
                    .add(compiled);
            validation.compiled("sink");
        }
        rule.setLevels(levels.isEmpty() ? Collections.emptyMap() : levels);
        RuleValidationSummary summary = validation.build();
        if (summary.getCompiledRules() > 0) {
            logger.info("compiled sink dsl rules: path={} sink={} rejected={} warnings={}",
                    rulePath,
                    summary.getCompiledRules(),
                    summary.getRejectedRules(),
                    summary.getWarningCount());
        }
        return summary;
    }

    private static SearchCondition compileSinkRule(RuleDslConfig.RuleSpec spec,
                                                   RuleValidationSummary.Builder validation,
                                                   String rulePath,
                                                   int index) {
        RuleDslConfig.MethodMatch match = spec.getMatch();
        if (match == null) {
            reject(validation, rulePath, index, spec, "sink_match_missing",
                    "sink rule requires match.className and match.methodName");
            return null;
        }
        String className = normalizeClass(match.getClassName());
        String methodName = normalize(match.getMethodName());
        if (className.isBlank() || methodName.isBlank()) {
            reject(validation, rulePath, index, spec, "sink_signature_missing",
                    "sink rule requires match.className and match.methodName");
            return null;
        }
        String category = normalize(spec.getSinkCategory());
        if (category.isBlank()) {
            reject(validation, rulePath, index, spec, "sink_category_missing",
                    "sink rule requires sinkCategory");
            return null;
        }
        SearchCondition condition = new SearchCondition();
        condition.setClassName(className);
        condition.setMethodName(methodName);
        condition.setMethodDesc(normalizeMethodDesc(match.getMethodDesc()));
        condition.setLevel(SinkRuleSupport.normalizeSeverity(spec.getSeverity(), "medium"));
        String explicitTier = SinkRuleSupport.normalizeRuleTier(spec.getRuleTier());
        condition.setRuleTier(explicitTier.isBlank()
                ? SinkRuleSupport.resolveRuleTier(condition.getMethodDesc(), condition.getLevel())
                : explicitTier);
        List<String> tags = SinkRuleSupport.normalizeTags(spec.getTags());
        condition.setTags(tags.isEmpty() ? SinkRuleSupport.buildTags(category) : tags);
        condition.setBoxName(normalize(spec.getBoxName()));
        return condition;
    }

    private static Map<String, Map<String, List<SearchCondition>>> copyLevels(Map<String, Map<String, List<SearchCondition>>> source) {
        if (source == null || source.isEmpty()) {
            return new LinkedHashMap<>();
        }
        LinkedHashMap<String, Map<String, List<SearchCondition>>> out = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, List<SearchCondition>>> levelEntry : source.entrySet()) {
            if (levelEntry == null || levelEntry.getKey() == null) {
                continue;
            }
            LinkedHashMap<String, List<SearchCondition>> byType = new LinkedHashMap<>();
            if (levelEntry.getValue() != null) {
                for (Map.Entry<String, List<SearchCondition>> typeEntry : levelEntry.getValue().entrySet()) {
                    if (typeEntry == null || typeEntry.getKey() == null) {
                        continue;
                    }
                    List<SearchCondition> list = typeEntry.getValue() == null
                            ? new ArrayList<>()
                            : new ArrayList<>(typeEntry.getValue());
                    byType.put(typeEntry.getKey(), list);
                }
            }
            out.put(levelEntry.getKey(), byType);
        }
        return out;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeClass(String value) {
        return normalize(value).replace('.', '/');
    }

    private static String normalizeMethodDesc(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank() || "null".equalsIgnoreCase(normalized)) {
            return "*";
        }
        return normalized;
    }

    private static void reject(RuleValidationSummary.Builder validation,
                               String rulePath,
                               int index,
                               RuleDslConfig.RuleSpec spec,
                               String code,
                               String message) {
        validation.error(index, safeId(spec), safeKind(spec), code, message);
        logger.warn("skip sink dsl rule: path={} index={} id={} kind={} code={} reason={}",
                rulePath,
                index,
                safeId(spec),
                safeKind(spec),
                code,
                message);
    }

    private static String safeId(RuleDslConfig.RuleSpec spec) {
        return spec == null ? "" : normalize(spec.getId());
    }

    private static String safeKind(RuleDslConfig.RuleSpec spec) {
        return spec == null ? "" : normalize(spec.getKind());
    }
}
