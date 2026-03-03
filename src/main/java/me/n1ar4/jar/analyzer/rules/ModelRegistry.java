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
import me.n1ar4.jar.analyzer.chains.SinkModel;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class ModelRegistry {
    private static final Logger logger = LogManager.getLogger();
    private static final String MODEL_PATH_PROP = "jar.analyzer.rules.model.path";
    private static final String SOURCE_PATH_PROP = "jar.analyzer.rules.source.path";
    private static final String MODEL_PATH = "rules/model.json";
    private static final String SOURCE_PATH = "rules/source.json";
    private static final AtomicLong VERSION_SEQ = new AtomicLong(0L);
    private static final long CHANGE_CHECK_INTERVAL_MS = 1000L;

    private static volatile Snapshot cachedSnapshot;
    private static volatile long nextCheckAfterMs;

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

    public static long reload() {
        synchronized (ModelRegistry.class) {
            cachedSnapshot = null;
            nextCheckAfterMs = 0L;
        }
        return currentSnapshot().version;
    }

    public static long checkNow() {
        synchronized (ModelRegistry.class) {
            nextCheckAfterMs = 0L;
        }
        return currentSnapshot().version;
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
        if (sink == null || sink.getClassReference() == null) {
            return null;
        }
        String className = normalizeClassName(sink.getClassReference().getName());
        String methodName = safe(sink.getName());
        String methodDesc = safe(sink.getDesc());
        if (className.isBlank() || methodName.isBlank()) {
            return null;
        }
        for (SinkModel model : getSinkModels()) {
            if (model == null) {
                continue;
            }
            if (!className.equals(normalizeClassName(model.getClassName()))) {
                continue;
            }
            if (!methodName.equals(safe(model.getMethodName()))) {
                continue;
            }
            String desc = normalizeSinkDesc(model.getMethodDesc());
            if (!"*".equals(desc) && !desc.equals(methodDesc)) {
                continue;
            }
            String kind = normalizeSinkKind(model.getCategory());
            if (!kind.isBlank()) {
                return kind;
            }
        }
        return null;
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

    private static Snapshot currentSnapshot() {
        Snapshot local = cachedSnapshot;
        long now = System.currentTimeMillis();
        if (local != null && now < nextCheckAfterMs) {
            return local;
        }
        String modelPath = resolvePath(MODEL_PATH_PROP, MODEL_PATH);
        String sourcePath = resolvePath(SOURCE_PATH_PROP, SOURCE_PATH);
        RuleFileStamp modelStamp = RuleFileStamp.of(modelPath);
        RuleFileStamp sourceStamp = RuleFileStamp.of(sourcePath);
        long sinkVersion = SinkRuleRegistry.getVersion();
        if (isUpToDate(local, modelPath, sourcePath, modelStamp, sourceStamp, sinkVersion)) {
            nextCheckAfterMs = now + CHANGE_CHECK_INTERVAL_MS;
            return local;
        }
        synchronized (ModelRegistry.class) {
            now = System.currentTimeMillis();
            Snapshot latest = cachedSnapshot;
            if (latest != null && now < nextCheckAfterMs) {
                return latest;
            }
            modelPath = resolvePath(MODEL_PATH_PROP, MODEL_PATH);
            sourcePath = resolvePath(SOURCE_PATH_PROP, SOURCE_PATH);
            modelStamp = RuleFileStamp.of(modelPath);
            sourceStamp = RuleFileStamp.of(sourcePath);
            sinkVersion = SinkRuleRegistry.getVersion();
            if (isUpToDate(latest, modelPath, sourcePath, modelStamp, sourceStamp, sinkVersion)) {
                nextCheckAfterMs = now + CHANGE_CHECK_INTERVAL_MS;
                return latest;
            }
            UnifiedModel model = loadUnifiedModel(modelPath, sourcePath);
            String rulesFingerprint = buildRulesFingerprint(modelPath, modelStamp, sourcePath, sourceStamp, sinkVersion);
            Snapshot refreshed = new Snapshot(
                    modelPath,
                    sourcePath,
                    modelStamp,
                    sourceStamp,
                    sinkVersion,
                    model,
                    VERSION_SEQ.incrementAndGet(),
                    rulesFingerprint
            );
            cachedSnapshot = refreshed;
            nextCheckAfterMs = now + CHANGE_CHECK_INTERVAL_MS;
            logger.info("model registry refreshed: version={} modelPath={} sourcePath={} sinkVersion={}",
                    refreshed.version, modelPath, sourcePath, sinkVersion);
            return refreshed;
        }
    }

    private static boolean isUpToDate(Snapshot snapshot,
                                      String modelPath,
                                      String sourcePath,
                                      RuleFileStamp modelStamp,
                                      RuleFileStamp sourceStamp,
                                      long sinkVersion) {
        if (snapshot == null || modelStamp == null || sourceStamp == null) {
            return false;
        }
        return modelPath.equals(snapshot.modelPath)
                && sourcePath.equals(snapshot.sourcePath)
                && modelStamp.equals(snapshot.modelStamp)
                && sourceStamp.equals(snapshot.sourceStamp)
                && sinkVersion == snapshot.sinkVersion;
    }

    private static String resolvePath(String propKey, String fallback) {
        String override = System.getProperty(propKey);
        if (override == null || override.isBlank()) {
            return fallback;
        }
        return override.trim();
    }

    private static UnifiedModel loadUnifiedModel(String modelPath, String sourcePath) {
        UnifiedModel merged = loadModelFile(modelPath);
        if (merged == null) {
            logger.error("CRITICAL: {} is missing or invalid - " +
                    "taint analysis will run with empty sanitizer/summary rules, " +
                    "which may produce excessive false positives", modelPath);
            merged = new UnifiedModel();
        }
        UnifiedModel source = loadModelFile(sourcePath);
        if (source != null) {
            merged.setSourceModel(source.getSourceModel());
            merged.setSourceAnnotations(source.getSourceAnnotations());
        } else {
            merged.setSourceModel(Collections.emptyList());
            merged.setSourceAnnotations(Collections.emptyList());
        }
        merged.setSinkModel(SinkRuleRegistry.getSinkModels());
        logger.info("rule registry loaded: sourceModels={}, sourceAnnotations={}, sinkModels={}",
                merged.getSourceModel() == null ? 0 : merged.getSourceModel().size(),
                merged.getSourceAnnotations() == null ? 0 : merged.getSourceAnnotations().size(),
                merged.getSinkModel() == null ? 0 : merged.getSinkModel().size());
        return merged;
    }

    private static UnifiedModel loadModelFile(String rulePath) {
        Path path = Paths.get(rulePath);
        if (!Files.exists(path)) {
            logger.warn("{} not found", path);
            return null;
        }
        try (InputStream in = Files.newInputStream(path)) {
            String jsonData = IOUtil.readString(in);
            if (jsonData == null || jsonData.trim().isEmpty()) {
                logger.warn("{} is empty", path);
                return null;
            }
            UnifiedModel model = JSON.parseObject(jsonData, UnifiedModel.class);
            if (model == null) {
                logger.warn("failed to parse {}", path);
                return null;
            }
            logger.info("loaded rule file: {}", path);
            return model;
        } catch (Exception ex) {
            logger.warn("load {} failed: {}", path, ex.toString());
            return null;
        }
    }

    private static String normalizeClassName(String className) {
        return safe(className).replace('.', '/');
    }

    private static String normalizeSinkDesc(String desc) {
        String value = safe(desc);
        if (value.isBlank() || "null".equalsIgnoreCase(value)) {
            return "*";
        }
        return value;
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

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String buildRulesFingerprint(String modelPath,
                                                RuleFileStamp modelStamp,
                                                String sourcePath,
                                                RuleFileStamp sourceStamp,
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
        private final RuleFileStamp modelStamp;
        private final RuleFileStamp sourceStamp;
        private final long sinkVersion;
        private final UnifiedModel model;
        private final long version;
        private final String rulesFingerprint;

        private Snapshot(String modelPath,
                         String sourcePath,
                         RuleFileStamp modelStamp,
                         RuleFileStamp sourceStamp,
                         long sinkVersion,
                         UnifiedModel model,
                         long version,
                         String rulesFingerprint) {
            this.modelPath = modelPath;
            this.sourcePath = sourcePath;
            this.modelStamp = modelStamp;
            this.sourceStamp = sourceStamp;
            this.sinkVersion = sinkVersion;
            this.model = model == null ? new UnifiedModel() : model;
            this.version = version;
            this.rulesFingerprint = rulesFingerprint == null ? "" : rulesFingerprint;
        }
    }

    private static final class RuleFileStamp {
        private final boolean exists;
        private final long modifiedTime;
        private final long size;

        private RuleFileStamp(boolean exists, long modifiedTime, long size) {
            this.exists = exists;
            this.modifiedTime = modifiedTime;
            this.size = size;
        }

        private static RuleFileStamp of(String rawPath) {
            if (rawPath == null || rawPath.isBlank()) {
                return new RuleFileStamp(false, -1L, -1L);
            }
            try {
                Path path = Paths.get(rawPath);
                if (!Files.exists(path)) {
                    return new RuleFileStamp(false, -1L, -1L);
                }
                long modified = Files.getLastModifiedTime(path).toMillis();
                long fileSize = Files.size(path);
                return new RuleFileStamp(true, modified, fileSize);
            } catch (Exception ex) {
                logger.debug("resolve model rule stamp failed: path={} err={}", rawPath, ex.toString());
                return new RuleFileStamp(false, -1L, -1L);
            }
        }

        private String fingerprint() {
            if (!exists) {
                return "missing";
            }
            return modifiedTime + ":" + size;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof RuleFileStamp other)) {
                return false;
            }
            return exists == other.exists
                    && modifiedTime == other.modifiedTime
                    && size == other.size;
        }

        @Override
        public int hashCode() {
            int result = Boolean.hashCode(exists);
            result = 31 * result + Long.hashCode(modifiedTime);
            result = 31 * result + Long.hashCode(size);
            return result;
        }
    }
}
