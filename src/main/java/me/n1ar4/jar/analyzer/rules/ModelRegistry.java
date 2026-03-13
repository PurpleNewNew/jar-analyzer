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

import com.alibaba.fastjson2.JSON;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.taint.Sanitizer;
import me.n1ar4.jar.analyzer.taint.SanitizerRule;
import me.n1ar4.jar.analyzer.taint.TaintGuardRule;
import me.n1ar4.jar.analyzer.taint.TaintModel;
import me.n1ar4.jar.analyzer.taint.TaintModelRule;
import me.n1ar4.jar.analyzer.utils.IOUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class ModelRegistry {
    private static final Logger logger = LogManager.getLogger();
    private static final String MODEL_PATH = "rules/model.json";
    private static final String SOURCE_PATH = "rules/source.json";
    private static final AtomicLong VERSION_SEQ = new AtomicLong(0L);
    private static final long CHANGE_CHECK_INTERVAL_MS = 1000L;

    private static volatile Snapshot cachedSnapshot;
    private static volatile long nextCheckAfterMs;
    private static volatile boolean forceCheckNow;
    private static volatile String modelPathOverrideForTesting;
    private static volatile String sourcePathOverrideForTesting;

    private ModelRegistry() {
    }

    public static UnifiedModel getModel() {
        return currentSnapshot().model;
    }

    public static long getVersion() {
        return currentSnapshot().version;
    }

    public static String getRulesFingerprint() {
        return currentSnapshot().rulesFingerprint;
    }

    public static RuleValidationSummary getRuleValidation() {
        return currentSnapshot().validation;
    }

    public static long reload() {
        synchronized (ModelRegistry.class) {
            cachedSnapshot = null;
            nextCheckAfterMs = 0L;
            forceCheckNow = false;
        }
        return currentSnapshot().version;
    }

    public static long checkNow() {
        synchronized (ModelRegistry.class) {
            nextCheckAfterMs = 0L;
            forceCheckNow = true;
        }
        return currentSnapshot().version;
    }

    public static void setRulePathsForTesting(String modelPath, String sourcePath) {
        synchronized (ModelRegistry.class) {
            modelPathOverrideForTesting = safe(modelPath);
            sourcePathOverrideForTesting = safe(sourcePath);
            cachedSnapshot = null;
            nextCheckAfterMs = 0L;
            forceCheckNow = false;
        }
    }

    public static void clearRulePathsForTesting() {
        synchronized (ModelRegistry.class) {
            modelPathOverrideForTesting = null;
            sourcePathOverrideForTesting = null;
            cachedSnapshot = null;
            nextCheckAfterMs = 0L;
            forceCheckNow = false;
        }
    }

    public static List<SourceModel> getSourceModels() {
        UnifiedModel model = getModel();
        if (model != null && model.getSourceModel() != null) {
            return model.getSourceModel();
        }
        return Collections.emptyList();
    }

    public static List<SinkModel> getSinkModels() {
        UnifiedModel model = getModel();
        if (model != null && model.getSinkModel() != null) {
            return model.getSinkModel();
        }
        return Collections.emptyList();
    }

    public static String resolveSinkKind(MethodReference.Handle sink) {
        SinkDescriptor descriptor = resolveSinkDescriptor(sink);
        return descriptor.hasRuleMatch() ? descriptor.getKind() : null;
    }

    public static SinkDescriptor resolveSinkDescriptor(MethodReference.Handle sink) {
        SinkModel model = findSinkModel(sink);
        if (model != null) {
            return new SinkDescriptor(
                    normalizeSinkKind(model.getCategory()),
                    safe(model.getCategory()),
                    SinkRuleSupport.normalizeSeverity(model.getSeverity(), ""),
                    SinkRuleSupport.normalizeRuleTier(model.getRuleTier()),
                    SinkRuleSupport.normalizeTags(model.getTags())
            );
        }
        SinkDescriptor dynamicDescriptor = resolveDynamicSinkDescriptor(sink);
        return dynamicDescriptor == null ? SinkDescriptor.empty() : dynamicDescriptor;
    }

    public static SanitizerRule getSanitizerRule() {
        UnifiedModel model = getModel();
        List<Sanitizer> rules = new ArrayList<>();
        if (model != null) {
            if (model.getSanitizerModel() != null) {
                rules.addAll(model.getSanitizerModel());
            }
            if (model.getNeutralModel() != null) {
                rules.addAll(model.getNeutralModel());
            }
        }
        if (!rules.isEmpty()) {
            SanitizerRule rule = new SanitizerRule();
            rule.setRules(rules);
            return rule;
        }
        logger.warn("model.json sanitizerModel is empty");
        return new SanitizerRule();
    }

    public static SanitizerRule getBarrierRule() {
        return getSanitizerRule();
    }

    public static TaintModelRule getTaintModelRule() {
        UnifiedModel model = getModel();
        List<TaintModel> merged = new ArrayList<>();
        if (model != null) {
            if (model.getSummaryModel() != null && !model.getSummaryModel().isEmpty()) {
                merged.addAll(model.getSummaryModel());
            }
            if (model.getAdditionalTaintSteps() != null) {
                merged.addAll(model.getAdditionalTaintSteps());
            }
        }
        if (!merged.isEmpty()) {
            TaintModelRule rule = new TaintModelRule();
            rule.setRules(merged);
            return rule;
        }
        logger.warn("model.json summaryModel/additionalTaintSteps is empty");
        return new TaintModelRule();
    }

    public static TaintModelRule getSummaryModelRule() {
        UnifiedModel model = getModel();
        if (model != null && model.getSummaryModel() != null && !model.getSummaryModel().isEmpty()) {
            TaintModelRule rule = new TaintModelRule();
            rule.setRules(model.getSummaryModel());
            return rule;
        }
        logger.warn("model.json summaryModel is empty");
        return new TaintModelRule();
    }

    public static TaintModelRule getAdditionalModelRule() {
        UnifiedModel model = getModel();
        if (model != null && model.getAdditionalTaintSteps() != null && !model.getAdditionalTaintSteps().isEmpty()) {
            TaintModelRule rule = new TaintModelRule();
            rule.setRules(model.getAdditionalTaintSteps());
            return rule;
        }
        logger.warn("model.json additionalTaintSteps is empty");
        return new TaintModelRule();
    }

    public static List<String> getAdditionalStepHints() {
        UnifiedModel model = getModel();
        if (model == null || model.getAdditionalStepHints() == null) {
            return Collections.emptyList();
        }
        return model.getAdditionalStepHints();
    }

    public static PruningPolicy getPruningPolicy() {
        UnifiedModel model = getModel();
        if (model == null || model.getPruningPolicy() == null) {
            return PruningPolicy.defaults();
        }
        return model.getPruningPolicy();
    }

    public static List<TaintGuardRule> getGuardRules() {
        UnifiedModel model = getModel();
        if (model != null && model.getGuardSanitizers() != null) {
            return model.getGuardSanitizers();
        }
        return Collections.emptyList();
    }

    public static List<String> getSourceAnnotations() {
        UnifiedModel model = getModel();
        if (model != null && model.getSourceAnnotations() != null) {
            return model.getSourceAnnotations();
        }
        return Collections.emptyList();
    }

    public static SourceRuleSnapshot getSourceRuleSnapshot() {
        Snapshot snapshot = currentSnapshot();
        UnifiedModel model = snapshot.model;
        List<SourceModel> sourceModels = model == null || model.getSourceModel() == null
                ? Collections.emptyList()
                : model.getSourceModel();
        List<String> sourceAnnotations = model == null || model.getSourceAnnotations() == null
                ? Collections.emptyList()
                : model.getSourceAnnotations();
        return new SourceRuleSnapshot(sourceModels, sourceAnnotations);
    }

    public static TaintRuleContext getTaintRuleContext() {
        Snapshot snapshot = currentSnapshot();
        UnifiedModel model = snapshot.model;
        List<Sanitizer> rules = new ArrayList<>();
        if (model != null) {
            if (model.getSanitizerModel() != null) {
                rules.addAll(model.getSanitizerModel());
            }
            if (model.getNeutralModel() != null) {
                rules.addAll(model.getNeutralModel());
            }
        }
        SanitizerRule barrierRule = new SanitizerRule();
        if (!rules.isEmpty()) {
            barrierRule.setRules(rules);
        }
        PruningPolicy pruningPolicy = model == null || model.getPruningPolicy() == null
                ? PruningPolicy.defaults()
                : model.getPruningPolicy();
        List<TaintGuardRule> guardRules = model == null || model.getGuardSanitizers() == null
                ? Collections.emptyList()
                : model.getGuardSanitizers();
        return new TaintRuleContext(barrierRule, pruningPolicy, guardRules);
    }

    private static Snapshot currentSnapshot() {
        Snapshot local = cachedSnapshot;
        long now = System.currentTimeMillis();
        boolean fullCheckRequested = forceCheckNow;
        if (!fullCheckRequested && local != null && now < nextCheckAfterMs) {
            return local;
        }
        String modelPath = resolvePath(modelPathOverrideForTesting, MODEL_PATH);
        String sourcePath = resolvePath(sourcePathOverrideForTesting, SOURCE_PATH);
        RuleFileVersion.State modelProbe = fullCheckRequested
                ? RuleFileVersion.stamp(modelPath, "model")
                : RuleFileVersion.probe(modelPath, "model");
        RuleFileVersion.State sourceProbe = fullCheckRequested
                ? RuleFileVersion.stamp(sourcePath, "source")
                : RuleFileVersion.probe(sourcePath, "source");
        long sinkVersion = SinkRuleRegistry.getVersion();
        if (!fullCheckRequested && isUpToDateByProbe(local, modelPath, sourcePath, modelProbe, sourceProbe, sinkVersion)) {
            nextCheckAfterMs = now + CHANGE_CHECK_INTERVAL_MS;
            return local;
        }
        synchronized (ModelRegistry.class) {
            now = System.currentTimeMillis();
            Snapshot latest = cachedSnapshot;
            fullCheckRequested = forceCheckNow;
            if (!fullCheckRequested && latest != null && now < nextCheckAfterMs) {
                return latest;
            }
            modelPath = resolvePath(modelPathOverrideForTesting, MODEL_PATH);
            sourcePath = resolvePath(sourcePathOverrideForTesting, SOURCE_PATH);
            RuleFileVersion.State modelState = fullCheckRequested
                    ? RuleFileVersion.stamp(modelPath, "model")
                    : RuleFileVersion.probe(modelPath, "model");
            RuleFileVersion.State sourceState = fullCheckRequested
                    ? RuleFileVersion.stamp(sourcePath, "source")
                    : RuleFileVersion.probe(sourcePath, "source");
            sinkVersion = SinkRuleRegistry.getVersion();
            boolean upToDate = fullCheckRequested
                    ? isUpToDateByStamp(latest, modelPath, sourcePath, modelState, sourceState, sinkVersion)
                    : isUpToDateByProbe(latest, modelPath, sourcePath, modelState, sourceState, sinkVersion);
            if (upToDate) {
                forceCheckNow = false;
                nextCheckAfterMs = now + CHANGE_CHECK_INTERVAL_MS;
                return latest;
            }
            LoadedModel loaded = loadUnifiedModel(modelPath, sourcePath);
            RuleFileVersion.State modelStamp = RuleFileVersion.stamp(modelPath, "model");
            RuleFileVersion.State sourceStamp = RuleFileVersion.stamp(sourcePath, "source");
            String rulesFingerprint = buildRulesFingerprint(modelPath, modelStamp, sourcePath, sourceStamp, sinkVersion);
            Snapshot refreshed = new Snapshot(
                    modelPath,
                    sourcePath,
                    modelStamp,
                    sourceStamp,
                    sinkVersion,
                    loaded.model(),
                    loaded.validation(),
                    VERSION_SEQ.incrementAndGet(),
                    rulesFingerprint
            );
            cachedSnapshot = refreshed;
            forceCheckNow = false;
            nextCheckAfterMs = now + CHANGE_CHECK_INTERVAL_MS;
            logger.info("model registry refreshed: version={} modelPath={} sourcePath={} sinkVersion={}",
                    refreshed.version, modelPath, sourcePath, sinkVersion);
            return refreshed;
        }
    }

    private static boolean isUpToDateByProbe(Snapshot snapshot,
                                             String modelPath,
                                             String sourcePath,
                                             RuleFileVersion.State modelProbe,
                                             RuleFileVersion.State sourceProbe,
                                             long sinkVersion) {
        if (snapshot == null || modelProbe == null || sourceProbe == null) {
            return false;
        }
        return modelPath.equals(snapshot.modelPath)
                && sourcePath.equals(snapshot.sourcePath)
                && snapshot.modelStamp.sameProbe(modelProbe)
                && snapshot.sourceStamp.sameProbe(sourceProbe)
                && sinkVersion == snapshot.sinkVersion;
    }

    private static boolean isUpToDateByStamp(Snapshot snapshot,
                                             String modelPath,
                                             String sourcePath,
                                             RuleFileVersion.State modelStamp,
                                             RuleFileVersion.State sourceStamp,
                                             long sinkVersion) {
        if (!isUpToDateByProbe(snapshot, modelPath, sourcePath, modelStamp, sourceStamp, sinkVersion)) {
            return false;
        }
        return snapshot.modelStamp.sameStamped(modelStamp)
                && snapshot.sourceStamp.sameStamped(sourceStamp)
                && sinkVersion == snapshot.sinkVersion;
    }

    private static String resolvePath(String overridePath, String fallback) {
        String override = overridePath;
        if (override == null || override.isBlank()) {
            return fallback;
        }
        return override.trim();
    }

    private static LoadedModel loadUnifiedModel(String modelPath, String sourcePath) {
        LoadedRuleFile modelFile = loadModelFile(modelPath, DslRuleCompiler.RuleFileDomain.MODEL);
        UnifiedModel merged = modelFile.model();
        if (merged == null) {
            logger.error("CRITICAL: {} is missing or invalid - " +
                    "taint analysis will run with empty sanitizer/summary rules, " +
                    "which may produce excessive false positives", modelPath);
            merged = new UnifiedModel();
        }
        LoadedRuleFile sourceFile = loadModelFile(sourcePath, DslRuleCompiler.RuleFileDomain.SOURCE);
        UnifiedModel source = sourceFile.model();
        if (source != null) {
            merged.setSourceModel(source.getSourceModel());
            merged.setSourceAnnotations(source.getSourceAnnotations());
        } else {
            merged.setSourceModel(Collections.emptyList());
            merged.setSourceAnnotations(Collections.emptyList());
        }
        merged.setSinkModel(SinkRuleRegistry.getSinkModels());
        PruningPolicy pruningPolicy = merged.getPruningPolicy();
        logger.info("rule registry loaded: sourceModels={}, sourceAnnotations={}, sinkModels={}, pruningPolicy={}, pruningScenarios={}",
                merged.getSourceModel() == null ? 0 : merged.getSourceModel().size(),
                merged.getSourceAnnotations() == null ? 0 : merged.getSourceAnnotations().size(),
                merged.getSinkModel() == null ? 0 : merged.getSinkModel().size(),
                pruningPolicy == null ? "default" : "custom",
                pruningPolicy == null ? 0 : pruningPolicy.scenarioCount());
        return new LoadedModel(
                merged,
                RuleValidationSummary.combine("model/source", modelFile.validation(), sourceFile.validation())
        );
    }

    private static LoadedRuleFile loadModelFile(String rulePath, DslRuleCompiler.RuleFileDomain domain) {
        Path path = Paths.get(rulePath);
        if (!Files.exists(path)) {
            logger.warn("{} not found", path);
            RuleValidationSummary validation = RuleValidationSummary.builder(domain.validationName(), rulePath)
                    .error(0, "", "", "rule_file_missing", "rule file not found")
                    .build();
            return new LoadedRuleFile(null, validation);
        }
        try (InputStream in = Files.newInputStream(path)) {
            String jsonData = IOUtil.readString(in);
            if (jsonData == null || jsonData.trim().isEmpty()) {
                logger.warn("{} is empty", path);
                RuleValidationSummary validation = RuleValidationSummary.builder(domain.validationName(), rulePath)
                        .error(0, "", "", "rule_file_empty", "rule file is empty")
                        .build();
                return new LoadedRuleFile(null, validation);
            }
            UnifiedModel model = JSON.parseObject(jsonData, UnifiedModel.class);
            if (model == null) {
                logger.warn("failed to parse {}", path);
                RuleValidationSummary validation = RuleValidationSummary.builder(domain.validationName(), rulePath)
                        .error(0, "", "", "rule_file_invalid", "failed to parse rule file")
                        .build();
                return new LoadedRuleFile(null, validation);
            }
            DslRuleCompiler.CompileResult compileResult = DslRuleCompiler.compileInto(model, path.toString(), domain);
            if (compileResult.totalCompiled() > 0) {
                logger.info("loaded rule file: {} (dsl compiled: summary={} additional={} sanitizer={} neutral={} guard={} pruningHint={} source={} sourceAnnotations={} rejected={})",
                        path,
                        compileResult.getSummaryRules(),
                        compileResult.getAdditionalRules(),
                        compileResult.getSanitizerRules(),
                        compileResult.getNeutralRules(),
                        compileResult.getGuardRules(),
                        compileResult.getPruningHintRules(),
                        compileResult.getSourceRules(),
                        compileResult.getSourceAnnotations(),
                        compileResult.getRejectedRules());
                return new LoadedRuleFile(model, compileResult.getValidation());
            }
            if (compileResult.getValidation().hasIssues()) {
                logger.warn("loaded rule file with validation issues: {} errors={} warnings={}",
                        path,
                        compileResult.getValidation().getErrorCount(),
                        compileResult.getValidation().getWarningCount());
            } else {
                logger.info("loaded rule file: {}", path);
            }
            return new LoadedRuleFile(model, compileResult.getValidation());
        } catch (Exception ex) {
            logger.warn("load {} failed: {}", path, ex.toString());
            RuleValidationSummary validation = RuleValidationSummary.builder(domain.validationName(), rulePath)
                    .error(0, "", "", "rule_file_load_failed", ex.toString())
                    .build();
            return new LoadedRuleFile(null, validation);
        }
    }

    private static String normalizeClassName(String className) {
        return safe(className).replace('.', '/');
    }

    private static String normalizeSinkKind(String raw) {
        String value = safe(raw).toLowerCase();
        if (value.isBlank()) {
            return "";
        }
        if (value.contains("sql")) {
            return "sql";
        }
        if (value.contains("ssrf")) {
            return "ssrf";
        }
        if (value.contains("xss")) {
            return "xss";
        }
        if (value.contains("file") || value.contains("path")) {
            return "file";
        }
        if (value.contains("rpc")) {
            return "rpc";
        }
        if (value.contains("jndi")) {
            return "jndi";
        }
        if (value.contains("rce")) {
            return "rce";
        }
        if (value.contains("xxe")) {
            return "xxe";
        }
        if (value.contains("deserialize")) {
            return "deserialize";
        }
        return value;
    }

    private static SinkModel findSinkModel(MethodReference.Handle sink) {
        return SinkRuleRegistry.findSinkByHandle(sink);
    }

    private static SinkDescriptor resolveDynamicSinkDescriptor(MethodReference.Handle sink) {
        if (sink == null || sink.getClassReference() == null) {
            return null;
        }
        MyBatisMapperXmlIndex.Result index = MyBatisMapperXmlIndex.currentProject();
        if (index == null) {
            return null;
        }
        MyBatisMapperXmlIndex.SinkPattern pattern = index.resolve(
                sink.getClassReference().getName(),
                sink.getName(),
                sink.getDesc()
        );
        if (pattern == null) {
            return null;
        }
        return new SinkDescriptor(
                "sql",
                "sql",
                "high",
                "project-xml",
                List.of("mybatis", "xml", "dynamic-sql")
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String buildRulesFingerprint(String modelPath,
                                                RuleFileVersion.State modelStamp,
                                                String sourcePath,
                                                RuleFileVersion.State sourceStamp,
                                                long sinkVersion) {
        String modelPart = modelStamp == null ? "missing" : modelStamp.fingerprint();
        String sourcePart = sourceStamp == null ? "missing" : sourceStamp.fingerprint();
        return "model=" + modelPath + "@" + modelPart
                + "|source=" + sourcePath + "@" + sourcePart
                + "|sink=v" + sinkVersion;
    }

    private static final class Snapshot {
        private final String modelPath;
        private final String sourcePath;
        private final RuleFileVersion.State modelStamp;
        private final RuleFileVersion.State sourceStamp;
        private final long sinkVersion;
        private final UnifiedModel model;
        private final RuleValidationSummary validation;
        private final long version;
        private final String rulesFingerprint;

        private Snapshot(String modelPath,
                         String sourcePath,
                         RuleFileVersion.State modelStamp,
                         RuleFileVersion.State sourceStamp,
                         long sinkVersion,
                         UnifiedModel model,
                         RuleValidationSummary validation,
                         long version,
                         String rulesFingerprint) {
            this.modelPath = modelPath;
            this.sourcePath = sourcePath;
            this.modelStamp = modelStamp;
            this.sourceStamp = sourceStamp;
            this.sinkVersion = sinkVersion;
            this.model = model == null ? new UnifiedModel() : model;
            this.validation = validation == null
                    ? RuleValidationSummary.builder("model/source", "").build()
                    : validation;
            this.version = version;
            this.rulesFingerprint = rulesFingerprint == null ? "" : rulesFingerprint;
        }
    }

    private record LoadedRuleFile(UnifiedModel model, RuleValidationSummary validation) {
    }

    private record LoadedModel(UnifiedModel model, RuleValidationSummary validation) {
    }

    public static final class SinkDescriptor {
        private static final SinkDescriptor EMPTY = new SinkDescriptor("", "", "", "", Collections.emptyList());

        private final String kind;
        private final String category;
        private final String severity;
        private final String ruleTier;
        private final List<String> tags;

        private SinkDescriptor(String kind,
                               String category,
                               String severity,
                               String ruleTier,
                               List<String> tags) {
            this.kind = safe(kind).toLowerCase();
            this.category = safe(category);
            this.severity = safe(severity).toLowerCase();
            this.ruleTier = safe(ruleTier).toLowerCase();
            this.tags = tags == null || tags.isEmpty() ? Collections.emptyList() : List.copyOf(tags);
        }

        public static SinkDescriptor empty() {
            return EMPTY;
        }

        public boolean hasRuleMatch() {
            return !kind.isBlank() || !ruleTier.isBlank() || !tags.isEmpty() || !category.isBlank();
        }

        public String getKind() {
            return kind;
        }

        public String getCategory() {
            return category;
        }

        public String getSeverity() {
            return severity;
        }

        public String getRuleTier() {
            return ruleTier;
        }

        public List<String> getTags() {
            return tags;
        }
    }

    public record SourceRuleSnapshot(List<SourceModel> sourceModels, List<String> sourceAnnotations) {
        public SourceRuleSnapshot {
            sourceModels = sourceModels == null ? Collections.emptyList() : List.copyOf(sourceModels);
            sourceAnnotations = sourceAnnotations == null ? Collections.emptyList() : List.copyOf(sourceAnnotations);
        }
    }

    public record TaintRuleContext(SanitizerRule barrierRule,
                                   PruningPolicy pruningPolicy,
                                   List<TaintGuardRule> guardRules) {
        public TaintRuleContext {
            barrierRule = barrierRule == null ? new SanitizerRule() : barrierRule;
            pruningPolicy = pruningPolicy == null ? PruningPolicy.defaults() : pruningPolicy;
            guardRules = guardRules == null ? Collections.emptyList() : List.copyOf(guardRules);
        }
    }
}
