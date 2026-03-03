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

public final class ModelRegistry {
    private static final Logger logger = LogManager.getLogger();
    private static final String MODEL_PATH = "rules/model.json";
    private static final String SOURCE_PATH = "rules/source.json";
    private static volatile UnifiedModel cached;

    private ModelRegistry() {
    }

    public static UnifiedModel getModel() {
        UnifiedModel model = cached;
        if (model != null) {
            return model;
        }
        synchronized (ModelRegistry.class) {
            if (cached == null) {
                cached = loadUnifiedModel();
            }
            return cached;
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
            if (model.getSummaryModel() != null) {
                if (!model.getSummaryModel().isEmpty()) {
                    merged.addAll(model.getSummaryModel());
                }
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

    private static UnifiedModel loadUnifiedModel() {
        UnifiedModel merged = loadModelFile(MODEL_PATH);
        if (merged == null) {
            logger.error("CRITICAL: {} is missing or invalid - " +
                    "taint analysis will run with empty sanitizer/summary rules, " +
                    "which may produce excessive false positives", MODEL_PATH);
            merged = new UnifiedModel();
        }
        UnifiedModel source = loadModelFile(SOURCE_PATH);
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

}
