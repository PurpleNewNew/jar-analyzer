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

import me.n1ar4.jar.analyzer.taint.Sanitizer;
import me.n1ar4.jar.analyzer.taint.TaintAnalysisProfile;
import me.n1ar4.jar.analyzer.taint.TaintFlow;
import me.n1ar4.jar.analyzer.taint.TaintGuardRule;
import me.n1ar4.jar.analyzer.taint.TaintModel;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class DslRuleCompiler {
    private static final Logger logger = LogManager.getLogger();

    private DslRuleCompiler() {
    }

    public static CompileResult compileInto(UnifiedModel model, String rulePath, RuleFileDomain domain) {
        RuleValidationSummary.Builder validation = RuleValidationSummary.builder(domain.validationName(), rulePath);
        if (model == null || model.getDsl() == null || model.getDsl().getRules() == null || model.getDsl().getRules().isEmpty()) {
            return CompileResult.empty(validation.build());
        }
        List<SourceModel> sourceModels = copy(model.getSourceModel());
        List<String> sourceAnnotations = copyStrings(model.getSourceAnnotations());
        List<TaintModel> summaryModels = copy(model.getSummaryModel());
        List<TaintModel> additionalModels = copy(model.getAdditionalTaintSteps());
        List<Sanitizer> sanitizerModels = copy(model.getSanitizerModel());
        List<Sanitizer> neutralModels = copy(model.getNeutralModel());
        List<TaintGuardRule> guardRules = copy(model.getGuardSanitizers());
        List<String> additionalHints = copyStrings(model.getAdditionalStepHints());
        CompileResult result = new CompileResult(validation);
        int index = 0;
        for (RuleDslConfig.RuleSpec spec : model.getDsl().getRules()) {
            index++;
            if (spec == null) {
                continue;
            }
            String normalizedKind = normalizeKind(spec.getKind());
            if (!domain.supports(normalizedKind)) {
                reject(validation, rulePath, index, spec, "unsupported_dsl_kind",
                        domain.unsupportedMessage());
                result.rejectedRules++;
                continue;
            }
            switch (normalizedKind) {
                case "summary" -> {
                    TaintModel compiled = compileFlowRule(spec, rulePath, index, validation);
                    if (compiled != null) {
                        summaryModels.add(compiled);
                        result.summaryRules++;
                        validation.compiled("summary");
                    } else {
                        result.rejectedRules++;
                    }
                }
                case "additional", "additional-step" -> {
                    TaintModel compiled = compileFlowRule(spec, rulePath, index, validation);
                    if (compiled != null) {
                        additionalModels.add(compiled);
                        result.additionalRules++;
                        validation.compiled("additional");
                    } else {
                        result.rejectedRules++;
                    }
                }
                case "sanitizer" -> {
                    Sanitizer compiled = compileSanitizerRule(spec, rulePath, index, validation);
                    if (compiled != null) {
                        sanitizerModels.add(compiled);
                        result.sanitizerRules++;
                        validation.compiled("sanitizer");
                    } else {
                        result.rejectedRules++;
                    }
                }
                case "neutral" -> {
                    Sanitizer compiled = compileSanitizerRule(spec, rulePath, index, validation);
                    if (compiled != null) {
                        neutralModels.add(compiled);
                        result.neutralRules++;
                        validation.compiled("neutral");
                    } else {
                        result.rejectedRules++;
                    }
                }
                case "guard" -> {
                    TaintGuardRule compiled = compileGuardRule(spec, rulePath, index, validation);
                    if (compiled != null) {
                        guardRules.add(compiled);
                        result.guardRules++;
                        validation.compiled("guard");
                    } else {
                        result.rejectedRules++;
                    }
                }
                case "pruning-hint", "pruning-hints", "additional-step-hint", "additional-step-hints" -> {
                    List<String> compiled = compilePruningHints(spec, rulePath, index, validation);
                    if (!compiled.isEmpty()) {
                        additionalHints.addAll(compiled);
                        result.pruningHintRules++;
                        validation.compiled("pruning-hint");
                    } else {
                        result.rejectedRules++;
                    }
                }
                case "source" -> {
                    SourceModel compiled = compileSourceRule(spec, rulePath, index, validation);
                    if (compiled != null) {
                        sourceModels.add(compiled);
                        result.sourceRules++;
                        validation.compiled("source");
                    } else {
                        result.rejectedRules++;
                    }
                }
                case "source-annotation" -> {
                    String compiled = compileSourceAnnotationRule(spec, rulePath, index, validation);
                    if (compiled != null) {
                        sourceAnnotations.add(compiled);
                        result.sourceAnnotations++;
                        validation.compiled("source-annotation");
                    } else {
                        result.rejectedRules++;
                    }
                }
                default -> {
                    reject(validation, rulePath, index, spec, "unsupported_dsl_kind",
                            "unsupported dsl kind");
                    result.rejectedRules++;
                }
            }
        }
        model.setSourceModel(sourceModels.isEmpty() ? Collections.emptyList() : List.copyOf(sourceModels));
        model.setSourceAnnotations(sourceAnnotations.isEmpty() ? Collections.emptyList() : dedupeStrings(sourceAnnotations));
        model.setSummaryModel(summaryModels.isEmpty() ? Collections.emptyList() : List.copyOf(summaryModels));
        model.setAdditionalTaintSteps(additionalModels.isEmpty() ? Collections.emptyList() : List.copyOf(additionalModels));
        model.setSanitizerModel(sanitizerModels.isEmpty() ? Collections.emptyList() : List.copyOf(sanitizerModels));
        model.setNeutralModel(neutralModels.isEmpty() ? Collections.emptyList() : List.copyOf(neutralModels));
        model.setGuardSanitizers(guardRules.isEmpty() ? Collections.emptyList() : List.copyOf(guardRules));
        model.setAdditionalStepHints(additionalHints.isEmpty() ? Collections.emptyList() : dedupeStrings(additionalHints));
        result.validation = validation.build();
        if (result.totalCompiled() > 0 || result.validation.hasIssues()) {
            logger.info("compiled dsl rules: path={} summary={} additional={} sanitizer={} neutral={} guard={} pruningHint={} source={} sourceAnnotations={} rejected={} warnings={}",
                    rulePath,
                    result.summaryRules,
                    result.additionalRules,
                    result.sanitizerRules,
                    result.neutralRules,
                    result.guardRules,
                    result.pruningHintRules,
                    result.sourceRules,
                    result.sourceAnnotations,
                    result.validation.getRejectedRules(),
                    result.validation.getWarningCount());
        }
        return result;
    }

    private static TaintModel compileFlowRule(RuleDslConfig.RuleSpec spec,
                                              String rulePath,
                                              int index,
                                              RuleValidationSummary.Builder validation) {
        RuleDslConfig.MethodMatch match = spec.getMatch();
        if (match == null) {
            reject(validation, rulePath, index, spec, "flow_match_missing",
                    "flow rule requires match.className and match.methodName");
            return null;
        }
        String className = normalizeClassName(match.getClassName());
        String methodName = safe(match.getMethodName());
        if (className.isBlank() || methodName.isBlank()) {
            reject(validation, rulePath, index, spec, "flow_signature_missing",
                    "flow rule requires match.className and match.methodName");
            return null;
        }
        List<TaintFlow> flows = compileFlows(spec.getFlows());
        if (flows.isEmpty()) {
            reject(validation, rulePath, index, spec, "flow_edges_missing",
                    "flow rule requires at least one valid flow");
            return null;
        }
        TaintModel model = new TaintModel();
        model.setClassName(className);
        model.setMethodName(methodName);
        model.setMethodDesc(normalizeMethodDesc(match.getMethodDesc()));
        model.setSubtypes(Boolean.TRUE.equals(match.getSubtypes()));
        model.setFlows(flows);
        return model;
    }

    private static Sanitizer compileSanitizerRule(RuleDslConfig.RuleSpec spec,
                                                  String rulePath,
                                                  int index,
                                                  RuleValidationSummary.Builder validation) {
        RuleDslConfig.MethodMatch match = spec.getMatch();
        if (match == null) {
            reject(validation, rulePath, index, spec, "sanitizer_match_missing",
                    "sanitizer rule requires match.className, match.methodName and match.methodDesc");
            return null;
        }
        String className = normalizeClassName(match.getClassName());
        String methodName = safe(match.getMethodName());
        String methodDesc = safe(match.getMethodDesc());
        if (className.isBlank() || methodName.isBlank() || methodDesc.isBlank()) {
            reject(validation, rulePath, index, spec, "sanitizer_signature_missing",
                    "sanitizer rule requires match.className, match.methodName and match.methodDesc");
            return null;
        }
        Integer target = parseSanitizerTarget(spec.getTarget());
        if (target == null) {
            reject(validation, rulePath, index, spec, "sanitizer_target_invalid",
                    "sanitizer rule target must be this|all|none|argN|arg[N]");
            return null;
        }
        Sanitizer sanitizer = new Sanitizer();
        sanitizer.setClassName(className);
        sanitizer.setMethodName(methodName);
        sanitizer.setMethodDesc(methodDesc);
        sanitizer.setParamIndex(target);
        sanitizer.setKind(resolveAppliesToKind(spec));
        return sanitizer;
    }

    private static TaintGuardRule compileGuardRule(RuleDslConfig.RuleSpec spec,
                                                   String rulePath,
                                                   int index,
                                                   RuleValidationSummary.Builder validation) {
        RuleDslConfig.MethodMatch match = spec.getMatch();
        if (match == null) {
            reject(validation, rulePath, index, spec, "guard_match_missing",
                    "guard rule requires match.className and match.methodName");
            return null;
        }
        String className = normalizeClassName(match.getClassName());
        String methodName = safe(match.getMethodName());
        if (className.isBlank() || methodName.isBlank()) {
            reject(validation, rulePath, index, spec, "guard_signature_missing",
                    "guard rule requires match.className and match.methodName");
            return null;
        }
        String type = normalizeGuardType(spec.getType());
        if (type.isBlank()) {
            reject(validation, rulePath, index, spec, "guard_type_missing",
                    "guard rule requires type");
            return null;
        }
        if (!isSupportedGuardType(type)) {
            reject(validation, rulePath, index, spec, "guard_type_invalid",
                    "guard rule type must be path-prefix|path-traversal|host-allowlist|path-normalize|host-extract");
            return null;
        }
        Integer paramIndex = spec.getParamIndex();
        if (requiresGuardParam(type) && (paramIndex == null || paramIndex < 0)) {
            reject(validation, rulePath, index, spec, "guard_param_missing",
                    "guard rule requires paramIndex >= 0 for this guard type");
            return null;
        }
        if (paramIndex != null && paramIndex < 0) {
            reject(validation, rulePath, index, spec, "guard_param_invalid",
                    "guard rule paramIndex must be >= 0");
            return null;
        }
        TaintGuardRule rule = new TaintGuardRule();
        rule.setKind(resolveAppliesToKind(spec));
        rule.setType(type);
        rule.setClassName(className);
        rule.setMethodName(methodName);
        rule.setMethodDesc(normalizeMethodDesc(match.getMethodDesc()));
        rule.setParamIndex(paramIndex);
        rule.setAllowlist(normalizeStrings(spec.getAllowlist()));
        rule.setEnabled(spec.getEnabled() == null ? Boolean.TRUE : spec.getEnabled());
        rule.setMode(normalizeGuardMode(spec.getMode()));
        rule.setRequireNormalized(Boolean.TRUE.equals(spec.getRequireNormalized()));
        return rule;
    }

    private static List<String> compilePruningHints(RuleDslConfig.RuleSpec spec,
                                                    String rulePath,
                                                    int index,
                                                    RuleValidationSummary.Builder validation) {
        LinkedHashSet<String> resolved = new LinkedHashSet<>();
        LinkedHashSet<String> invalid = new LinkedHashSet<>();
        if (spec.getHint() != null && !spec.getHint().isBlank()) {
            addPruningHint(spec.getHint(), resolved, invalid);
        }
        if (spec.getHints() != null) {
            for (String raw : spec.getHints()) {
                addPruningHint(raw, resolved, invalid);
            }
        }
        if (!invalid.isEmpty()) {
            validation.warning(index, safe(spec.getId()), safe(spec.getKind()), "pruning_hint_invalid",
                    "ignored unsupported pruning hints: " + String.join(",", invalid));
            logger.warn("partially ignored pruning hints: path={} index={} id={} invalid={}",
                    rulePath, index, safe(spec.getId()), String.join(",", invalid));
        }
        if (resolved.isEmpty()) {
            reject(validation, rulePath, index, spec, "pruning_hint_missing",
                    "pruning-hint rule requires hint or hints with supported values");
            return Collections.emptyList();
        }
        return List.copyOf(resolved);
    }

    private static void addPruningHint(String raw, Set<String> resolved, Set<String> invalid) {
        String canonical = TaintAnalysisProfile.canonicalAdditionalHint(raw);
        if (canonical == null || canonical.isBlank()) {
            String value = safe(raw);
            if (!value.isBlank()) {
                invalid.add(value);
            }
            return;
        }
        resolved.add(canonical);
    }

    private static SourceModel compileSourceRule(RuleDslConfig.RuleSpec spec,
                                                 String rulePath,
                                                 int index,
                                                 RuleValidationSummary.Builder validation) {
        RuleDslConfig.MethodMatch match = spec.getMatch();
        if (match == null) {
            reject(validation, rulePath, index, spec, "source_match_missing",
                    "source rule requires match.className and match.methodName");
            return null;
        }
        String className = normalizeClassName(match.getClassName());
        String methodName = safe(match.getMethodName());
        if (className.isBlank() || methodName.isBlank()) {
            reject(validation, rulePath, index, spec, "source_signature_missing",
                    "source rule requires match.className and match.methodName");
            return null;
        }
        SourceModel source = new SourceModel();
        source.setClassName(className);
        source.setMethodName(methodName);
        source.setMethodDesc(normalizeMethodDesc(match.getMethodDesc()));
        source.setKind(safe(spec.getSourceKind()).isBlank() ? "custom" : safe(spec.getSourceKind()));
        return source;
    }

    private static String compileSourceAnnotationRule(RuleDslConfig.RuleSpec spec,
                                                      String rulePath,
                                                      int index,
                                                      RuleValidationSummary.Builder validation) {
        String normalized = SourceRuleSupport.normalizeAnnotationName(spec.getAnnotation());
        if (normalized.isBlank()) {
            reject(validation, rulePath, index, spec, "source_annotation_missing",
                    "source-annotation rule requires annotation");
            return null;
        }
        return normalized;
    }

    private static List<TaintFlow> compileFlows(List<TaintFlow> rawFlows) {
        if (rawFlows == null || rawFlows.isEmpty()) {
            return Collections.emptyList();
        }
        List<TaintFlow> compiled = new ArrayList<>();
        for (TaintFlow flow : rawFlows) {
            if (flow == null) {
                continue;
            }
            String from = safe(flow.getFrom());
            String to = safe(flow.getTo());
            if (from.isBlank() || to.isBlank()) {
                continue;
            }
            TaintFlow compiledFlow = new TaintFlow();
            compiledFlow.setFrom(from);
            compiledFlow.setTo(to);
            compiled.add(compiledFlow);
        }
        return compiled.isEmpty() ? Collections.emptyList() : List.copyOf(compiled);
    }

    private static Integer parseSanitizerTarget(String raw) {
        String value = safe(raw).toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            return null;
        }
        if ("this".equals(value)) {
            return Sanitizer.THIS_PARAM;
        }
        if ("all".equals(value) || "*".equals(value) || "all-params".equals(value)) {
            return Sanitizer.ALL_PARAMS;
        }
        if ("none".equals(value)) {
            return Sanitizer.NO_PARAM;
        }
        if (value.startsWith("arg[")) {
            int end = value.indexOf(']');
            if (end > 4) {
                return parseInt(value.substring(4, end));
            }
            return null;
        }
        if (value.startsWith("arg")) {
            return parseInt(value.substring(3));
        }
        if (value.startsWith("param")) {
            return parseInt(value.substring(5));
        }
        return null;
    }

    private static Integer parseInt(String raw) {
        try {
            int value = Integer.parseInt(raw.trim());
            return value < 0 ? null : value;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String resolveAppliesToKind(RuleDslConfig.RuleSpec spec) {
        String appliesTo = safe(spec == null ? null : spec.getAppliesToKind());
        if (!appliesTo.isBlank()) {
            return appliesTo;
        }
        return safe(spec == null ? null : spec.getSanitizerKind());
    }

    private static boolean isSupportedGuardType(String type) {
        return "path-prefix".equals(type)
                || "path-traversal".equals(type)
                || "host-allowlist".equals(type)
                || "path-normalize".equals(type)
                || "host-extract".equals(type);
    }

    private static boolean requiresGuardParam(String type) {
        return "path-prefix".equals(type)
                || "path-traversal".equals(type)
                || "host-allowlist".equals(type);
    }

    private static String normalizeGuardType(String value) {
        return safe(value).toLowerCase(Locale.ROOT);
    }

    private static String normalizeGuardMode(String value) {
        String normalized = safe(value).toLowerCase(Locale.ROOT);
        if ("hard".equals(normalized) || "soft".equals(normalized)) {
            return normalized;
        }
        return "soft";
    }

    private static String normalizeClassName(String value) {
        return safe(value).replace('.', '/');
    }

    private static String normalizeMethodDesc(String value) {
        String normalized = safe(value);
        if (normalized.isBlank() || "null".equalsIgnoreCase(normalized)) {
            return "*";
        }
        return normalized;
    }

    private static String normalizeKind(String value) {
        return safe(value).toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static List<String> normalizeStrings(List<String> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String item : source) {
            String normalized = safe(item);
            if (!normalized.isBlank()) {
                out.add(normalized);
            }
        }
        return out.isEmpty() ? Collections.emptyList() : List.copyOf(out);
    }

    private static <T> List<T> copy(List<T> source) {
        return source == null || source.isEmpty() ? new ArrayList<>() : new ArrayList<>(source);
    }

    private static List<String> copyStrings(List<String> source) {
        return source == null || source.isEmpty() ? new ArrayList<>() : new ArrayList<>(source);
    }

    private static List<String> dedupeStrings(List<String> source) {
        Set<String> deduped = new LinkedHashSet<>();
        for (String item : source) {
            String normalized = safe(item);
            if (!normalized.isBlank()) {
                deduped.add(normalized);
            }
        }
        return deduped.isEmpty() ? Collections.emptyList() : List.copyOf(deduped);
    }

    private static void reject(RuleValidationSummary.Builder validation,
                               String rulePath,
                               int index,
                               RuleDslConfig.RuleSpec spec,
                               String code,
                               String message) {
        validation.error(index, safe(spec == null ? null : spec.getId()), safe(spec == null ? null : spec.getKind()), code, message);
        logger.warn("skip dsl rule: path={} index={} id={} kind={} code={} reason={}",
                rulePath,
                index,
                safe(spec == null ? null : spec.getId()),
                safe(spec == null ? null : spec.getKind()),
                code,
                message);
    }

    public enum RuleFileDomain {
        MODEL("model", "model.json dsl.rules[] only supports summary|additional|sanitizer|neutral|guard|pruning-hint"),
        SOURCE("source", "source.json dsl.rules[] only supports source|source-annotation");

        private final String validationName;
        private final String unsupportedMessage;

        RuleFileDomain(String validationName, String unsupportedMessage) {
            this.validationName = validationName;
            this.unsupportedMessage = unsupportedMessage;
        }

        public String validationName() {
            return validationName;
        }

        public String unsupportedMessage() {
            return unsupportedMessage;
        }

        public boolean supports(String kind) {
            if (kind == null || kind.isBlank()) {
                return false;
            }
            if (this == MODEL) {
                return "summary".equals(kind)
                        || "additional".equals(kind)
                        || "additional-step".equals(kind)
                        || "sanitizer".equals(kind)
                        || "neutral".equals(kind)
                        || "guard".equals(kind)
                        || "pruning-hint".equals(kind)
                        || "pruning-hints".equals(kind)
                        || "additional-step-hint".equals(kind)
                        || "additional-step-hints".equals(kind);
            }
            return "source".equals(kind) || "source-annotation".equals(kind);
        }
    }

    public static final class CompileResult {
        private int summaryRules;
        private int additionalRules;
        private int sanitizerRules;
        private int neutralRules;
        private int sourceRules;
        private int sourceAnnotations;
        private int guardRules;
        private int pruningHintRules;
        private int rejectedRules;
        private RuleValidationSummary validation;

        private CompileResult(RuleValidationSummary.Builder validation) {
            this.validation = validation.build();
        }

        public static CompileResult empty(RuleValidationSummary validation) {
            CompileResult result = new CompileResult(RuleValidationSummary.builder(validation.getName(), validation.getPath()));
            result.validation = validation;
            return result;
        }

        public int totalCompiled() {
            return summaryRules + additionalRules + sanitizerRules + neutralRules + sourceRules + sourceAnnotations
                    + guardRules + pruningHintRules;
        }

        public int getSummaryRules() {
            return summaryRules;
        }

        public int getAdditionalRules() {
            return additionalRules;
        }

        public int getSanitizerRules() {
            return sanitizerRules;
        }

        public int getNeutralRules() {
            return neutralRules;
        }

        public int getSourceRules() {
            return sourceRules;
        }

        public int getSourceAnnotations() {
            return sourceAnnotations;
        }

        public int getGuardRules() {
            return guardRules;
        }

        public int getPruningHintRules() {
            return pruningHintRules;
        }

        public int getRejectedRules() {
            return rejectedRules;
        }

        public RuleValidationSummary getValidation() {
            return validation;
        }
    }
}
