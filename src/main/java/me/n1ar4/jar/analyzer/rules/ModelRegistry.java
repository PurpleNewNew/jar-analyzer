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
import me.n1ar4.jar.analyzer.utils.IOUtils;
import me.n1ar4.jar.analyzer.utils.YamlUtil;
import me.n1ar4.jar.analyzer.engine.SearchCondition;
import me.n1ar4.jar.analyzer.gui.vul.Rule;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.Type;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public final class ModelRegistry {
    private static final Logger logger = LogManager.getLogger();
    private static final String MODEL_PATH = "rules/model.json";
    private static final String VUL_PATH = "rules/vulnerability.yaml";
    private static volatile UnifiedModel cached;
    private static volatile List<SinkModel> cachedSinkModels;

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

    public static List<SinkModel> getSinkModels() {
        List<SinkModel> cachedLocal = cachedSinkModels;
        if (cachedLocal != null && !cachedLocal.isEmpty()) {
            return cachedLocal;
        }
        synchronized (ModelRegistry.class) {
            if (cachedSinkModels == null || cachedSinkModels.isEmpty()) {
                cachedSinkModels = loadSinkModelsFromVulnerability();
            }
            if (cachedSinkModels == null) {
                return Collections.emptyList();
            }
            return cachedSinkModels;
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

    public static String resolveSinkKind(MethodReference.Handle sink) {
        if (sink == null) {
            return null;
        }
        String className = sink.getClassReference().getName();
        String methodName = sink.getName();
        String methodDesc = sink.getDesc();
        for (SinkModel model : getSinkModels()) {
            if (model == null) {
                continue;
            }
            if (!matchMethod(model, className, methodName, methodDesc)) {
                continue;
            }
            return normalizeSinkKind(model.getCategory());
        }
        return null;
    }

    public static String resolveSinkTier(MethodReference.Handle sink) {
        if (sink == null) {
            return null;
        }
        String className = sink.getClassReference().getName();
        String methodName = sink.getName();
        String methodDesc = sink.getDesc();
        SinkModel best = null;
        for (SinkModel model : getSinkModels()) {
            if (model == null) {
                continue;
            }
            if (!matchMethod(model, className, methodName, methodDesc)) {
                continue;
            }
            if (best == null || ruleTierRank(model.getRuleTier()) > ruleTierRank(best.getRuleTier())) {
                best = model;
            }
        }
        if (best != null && best.getRuleTier() != null && !best.getRuleTier().trim().isEmpty()) {
            return best.getRuleTier();
        }
        return guessRuleTier(methodDesc);
    }

    private static boolean matchMethod(SinkModel model, String className, String methodName, String methodDesc) {
        if (model.getClassName() == null || model.getMethodName() == null) {
            return false;
        }
        if (!model.getClassName().equals(className)) {
            return false;
        }
        if (!model.getMethodName().equals(methodName)) {
            return false;
        }
        String desc = model.getMethodDesc();
        if (desc == null || desc.trim().isEmpty() || "*".equals(desc) || "null".equalsIgnoreCase(desc)) {
            return true;
        }
        return desc.equals(methodDesc);
    }

    private static String normalizeSinkKind(String category) {
        if (category == null) {
            return null;
        }
        String c = category.trim().toLowerCase();
        if (c.isEmpty()) {
            return null;
        }
        if (c.contains("sql")) {
            return "sql";
        }
        if (c.contains("ssrf")) {
            return "ssrf";
        }
        if (c.contains("xss")) {
            return "xss";
        }
        if (c.contains("file")) {
            return "file";
        }
        if (c.contains("rpc")) {
            return "rpc";
        }
        if (c.contains("jndi")) {
            return "jndi";
        }
        if (c.contains("rce")) {
            return "rce";
        }
        if (c.contains("xxe")) {
            return "xxe";
        }
        if (c.contains("deserialize")) {
            return "deserialize";
        }
        return c;
    }

    private static List<SinkModel> loadSinkModelsFromVulnerability() {
        Rule rule = loadVulnerabilityRule();
        if (rule == null || rule.getLevels() == null) {
            logger.warn("vulnerability.yaml levels is empty");
            return Collections.emptyList();
        }
        Map<String, SinkModel> sinkMap = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, List<SearchCondition>>> levelEntry : rule.getLevels().entrySet()) {
            String severity = levelEntry.getKey();
            Map<String, List<SearchCondition>> byType = levelEntry.getValue();
            if (byType == null) {
                continue;
            }
            for (Map.Entry<String, List<SearchCondition>> typeEntry : byType.entrySet()) {
                String category = typeEntry.getKey();
                List<SearchCondition> conditions = typeEntry.getValue();
                if (conditions == null || conditions.isEmpty()) {
                    continue;
                }
                for (SearchCondition condition : conditions) {
                    if (condition == null) {
                        continue;
                    }
                    String className = normalizeClassName(condition.getClassName());
                    String methodName = condition.getMethodName();
                    String methodDesc = condition.getMethodDesc();
                    if (isBlank(className) || isBlank(methodName) || isBlank(methodDesc)) {
                        continue;
                    }
                    String normalizedDesc = methodDesc.trim();
                    if ("null".equalsIgnoreCase(normalizedDesc)) {
                        normalizedDesc = "*";
                    }
                    String key = className + "#" + methodName + "#" + normalizedDesc;
                    SinkModel existing = sinkMap.get(key);
                    if (existing == null) {
                        SinkModel model = new SinkModel();
                        model.setClassName(className);
                        model.setMethodName(methodName);
                        model.setMethodDesc(normalizedDesc);
                        model.setCategory(category);
                        String normalizedSeverity = normalizeSeverity(severity);
                        model.setSeverity(normalizedSeverity);
                        model.setRuleTier(resolveRuleTier(normalizedSeverity, normalizedDesc));
                        model.setTags(buildTags(category));
                        model.setBoxName(buildBoxName(className, methodName, normalizedDesc));
                        sinkMap.put(key, model);
                    } else {
                        String existingSev = existing.getSeverity();
                        String incoming = normalizeSeverity(severity);
                        if (severityRank(incoming) > severityRank(existingSev)) {
                            existing.setSeverity(incoming);
                        }
                        if (isBlank(existing.getCategory()) && !isBlank(category)) {
                            existing.setCategory(category);
                        }
                        String incomingTier = resolveRuleTier(incoming, normalizedDesc);
                        if (ruleTierRank(incomingTier) > ruleTierRank(existing.getRuleTier())) {
                            existing.setRuleTier(incomingTier);
                        }
                    }
                }
            }
        }
        if (sinkMap.isEmpty()) {
            logger.warn("vulnerability.yaml sink list is empty");
        } else {
            logger.info("loaded {} sinks from vulnerability.yaml", sinkMap.size());
        }
        return new ArrayList<>(sinkMap.values());
    }

    private static Rule loadVulnerabilityRule() {
        Path vPath = Paths.get(VUL_PATH);
        if (!Files.exists(vPath)) {
            logger.warn("vulnerability.yaml not found: {}", vPath.toString());
            return null;
        }
        try (InputStream in = Files.newInputStream(vPath)) {
            byte[] yamlData = IOUtils.readAllBytes(in);
            return YamlUtil.loadAs(yamlData);
        } catch (Exception ex) {
            logger.warn("load vulnerability.yaml failed: {}", ex.toString());
            return null;
        }
    }

    private static String normalizeClassName(String className) {
        if (className == null) {
            return null;
        }
        String v = className.trim();
        if (v.isEmpty()) {
            return null;
        }
        return v.replace('.', '/');
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String normalizeSeverity(String severity) {
        if (severity == null) {
            return "medium";
        }
        String v = severity.trim().toLowerCase();
        if ("high".equals(v) || "medium".equals(v) || "low".equals(v)) {
            return v;
        }
        return "medium";
    }

    private static int severityRank(String severity) {
        if ("high".equals(severity)) {
            return 3;
        }
        if ("medium".equals(severity)) {
            return 2;
        }
        if ("low".equals(severity)) {
            return 1;
        }
        return 0;
    }

    private static String resolveRuleTier(String severity, String methodDesc) {
        if (isWildcardDesc(methodDesc)) {
            return SinkModel.TIER_CLUE;
        }
        if ("low".equals(severity)) {
            return SinkModel.TIER_SOFT;
        }
        return SinkModel.TIER_HARD;
    }

    private static String guessRuleTier(String methodDesc) {
        return isWildcardDesc(methodDesc) ? SinkModel.TIER_CLUE : SinkModel.TIER_HARD;
    }

    private static int ruleTierRank(String tier) {
        if (SinkModel.TIER_HARD.equals(tier)) {
            return 3;
        }
        if (SinkModel.TIER_SOFT.equals(tier)) {
            return 2;
        }
        if (SinkModel.TIER_CLUE.equals(tier)) {
            return 1;
        }
        return 0;
    }

    private static boolean isWildcardDesc(String desc) {
        if (desc == null) {
            return true;
        }
        String v = desc.trim();
        if (v.isEmpty()) {
            return true;
        }
        if ("*".equals(v)) {
            return true;
        }
        return "null".equalsIgnoreCase(v);
    }

    private static List<String> buildTags(String category) {
        if (category == null) {
            return Collections.emptyList();
        }
        String[] parts = category.toLowerCase().split("[^a-z0-9]+");
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        for (String part : parts) {
            if (part == null || part.isEmpty()) {
                continue;
            }
            tags.add(part);
        }
        return tags.isEmpty() ? Collections.emptyList() : new ArrayList<>(tags);
    }

    private static String buildBoxName(String className, String methodName, String methodDesc) {
        String simpleClass = className;
        if (className != null) {
            int idx = className.lastIndexOf('/');
            if (idx >= 0 && idx < className.length() - 1) {
                simpleClass = className.substring(idx + 1);
            }
            simpleClass = simpleClass.replace('$', '.');
        }
        String name = methodName == null ? "" : methodName;
        String params = "";
        if (methodDesc != null && !methodDesc.trim().isEmpty()) {
            try {
                Type[] args = Type.getArgumentTypes(methodDesc);
                if (args.length > 0) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("(");
                    for (int i = 0; i < args.length; i++) {
                        if (i > 0) {
                            sb.append(", ");
                        }
                        String t = args[i].getClassName();
                        int dot = t.lastIndexOf('.');
                        sb.append(dot >= 0 ? t.substring(dot + 1) : t);
                    }
                    sb.append(")");
                    params = sb.toString();
                }
            } catch (Exception ignored) {
            }
        }
        return simpleClass + "." + name + params;
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
