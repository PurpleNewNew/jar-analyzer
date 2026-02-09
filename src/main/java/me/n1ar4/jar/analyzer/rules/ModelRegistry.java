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
        boolean hasSummary = false;
        if (model != null) {
            if (model.getSummaryModel() != null) {
                if (!model.getSummaryModel().isEmpty()) {
                    merged.addAll(model.getSummaryModel());
                    hasSummary = true;
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
        Path path = Paths.get(MODEL_PATH);
        if (!Files.exists(path)) {
            logger.warn("model.json not found: {}", path.toString());
            return new UnifiedModel();
        }
        try (InputStream in = Files.newInputStream(path)) {
            String jsonData = IOUtil.readString(in);
            if (jsonData == null || jsonData.trim().isEmpty()) {
                logger.warn("model.json is empty");
                return new UnifiedModel();
            }
            UnifiedModel model = JSON.parseObject(jsonData, UnifiedModel.class);
            if (model == null) {
                logger.warn("failed to parse model.json");
                return new UnifiedModel();
            }
            logger.info("loaded unified model from {}", path.toString());
            return model;
        } catch (Exception ex) {
            logger.warn("load model.json failed: {}", ex.toString());
            return new UnifiedModel();
        }
    }

}
