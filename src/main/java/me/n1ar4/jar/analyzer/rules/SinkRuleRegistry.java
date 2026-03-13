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
import me.n1ar4.jar.analyzer.engine.SearchCondition;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.rules.sink.SinkRule;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.Type;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sink registry backed by rules/sink.json.
 */
public final class SinkRuleRegistry {
    private static final Logger logger = LogManager.getLogger();
    private static final String SINK_JSON_PATH = "rules/sink.json";
    private static final AtomicLong VERSION_SEQ = new AtomicLong(0L);
    private static final long CHANGE_CHECK_INTERVAL_MS = 1000L;

    private static volatile Snapshot cachedSnapshot;
    private static volatile long nextCheckAfterMs;
    private static volatile boolean forceCheckNow;
    private static volatile String sinkPathOverrideForTesting;

    private SinkRuleRegistry() {
    }

    public static SinkRule getSinkRule() {
        return currentSnapshot().rule;
    }

    public static List<SinkModel> getSinkModels() {
        return currentSnapshot().sinkModels;
    }

    public static SinkModel findSinkByName(String name) {
        if (name == null) {
            return null;
        }
        String key = name.trim();
        if (key.isEmpty()) {
            return null;
        }
        String simpleKey = stripParams(key);
        for (SinkModel sink : currentSnapshot().sinkModels) {
            if (sink == null) {
                continue;
            }
            String boxName = sink.getBoxName();
            if (boxName != null && boxName.equals(key)) {
                return sink;
            }
            if (boxName != null && boxName.equalsIgnoreCase(key)) {
                return sink;
            }
            String simpleName = buildSimpleName(sink);
            if (simpleName != null && simpleName.equalsIgnoreCase(simpleKey)) {
                return sink;
            }
        }
        return null;
    }

    public static SinkModel findSinkByHandle(MethodReference.Handle sink) {
        if (sink == null || sink.getClassReference() == null) {
            return null;
        }
        String className = normalizeClassName(sink.getClassReference().getName());
        String methodName = safe(sink.getName());
        String methodDesc = safe(sink.getDesc());
        if (isBlank(className) || isBlank(methodName)) {
            return null;
        }
        for (SinkModel model : currentSnapshot().sinkModels) {
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
            return model;
        }
        return null;
    }

    public static long getVersion() {
        return currentSnapshot().version;
    }

    public static RuleValidationSummary getRuleValidation() {
        return currentSnapshot().validation;
    }

    public static long reload() {
        synchronized (SinkRuleRegistry.class) {
            cachedSnapshot = null;
            nextCheckAfterMs = 0L;
            forceCheckNow = false;
        }
        return currentSnapshot().version;
    }

    public static long checkNow() {
        synchronized (SinkRuleRegistry.class) {
            nextCheckAfterMs = 0L;
            forceCheckNow = true;
        }
        return currentSnapshot().version;
    }

    public static void setSinkPathForTesting(String path) {
        synchronized (SinkRuleRegistry.class) {
            sinkPathOverrideForTesting = safe(path);
            cachedSnapshot = null;
            nextCheckAfterMs = 0L;
            forceCheckNow = false;
        }
    }

    public static void clearSinkPathForTesting() {
        synchronized (SinkRuleRegistry.class) {
            sinkPathOverrideForTesting = null;
            cachedSnapshot = null;
            nextCheckAfterMs = 0L;
            forceCheckNow = false;
        }
    }

    private static Snapshot currentSnapshot() {
        Snapshot local = cachedSnapshot;
        long now = System.currentTimeMillis();
        boolean fullCheckRequested = forceCheckNow;
        if (!fullCheckRequested && local != null && now < nextCheckAfterMs) {
            return local;
        }
        String sinkPath = resolveSinkPath();
        RuleFileVersion.State sinkState = fullCheckRequested
                ? RuleFileVersion.stamp(sinkPath, "sink")
                : RuleFileVersion.probe(sinkPath, "sink");
        if (!fullCheckRequested && isUpToDateByProbe(local, sinkPath, sinkState)) {
            nextCheckAfterMs = now + CHANGE_CHECK_INTERVAL_MS;
            return local;
        }
        synchronized (SinkRuleRegistry.class) {
            now = System.currentTimeMillis();
            Snapshot latest = cachedSnapshot;
            fullCheckRequested = forceCheckNow;
            if (!fullCheckRequested && latest != null && now < nextCheckAfterMs) {
                return latest;
            }
            sinkPath = resolveSinkPath();
            sinkState = fullCheckRequested
                    ? RuleFileVersion.stamp(sinkPath, "sink")
                    : RuleFileVersion.probe(sinkPath, "sink");
            boolean upToDate = fullCheckRequested
                    ? isUpToDateByStamp(latest, sinkPath, sinkState)
                    : isUpToDateByProbe(latest, sinkPath, sinkState);
            if (upToDate) {
                forceCheckNow = false;
                nextCheckAfterMs = now + CHANGE_CHECK_INTERVAL_MS;
                return latest;
            }
            LoadedSinkRule loaded = loadSinkRule(sinkPath);
            SinkRule rule = loaded.rule();
            List<SinkModel> models = loadSinkModelsFromRule(rule);
            RuleFileVersion.State sinkStamp = RuleFileVersion.stamp(sinkPath, "sink");
            Snapshot refreshed = new Snapshot(
                    sinkPath,
                    sinkStamp,
                    rule,
                    loaded.validation(),
                    models.isEmpty() ? Collections.emptyList() : List.copyOf(models),
                    VERSION_SEQ.incrementAndGet()
            );
            cachedSnapshot = refreshed;
            forceCheckNow = false;
            nextCheckAfterMs = now + CHANGE_CHECK_INTERVAL_MS;
            logger.info("sink rule registry refreshed: path={} version={} sinks={}",
                    sinkPath,
                    refreshed.version,
                    refreshed.sinkModels.size());
            return refreshed;
        }
    }

    private static boolean isUpToDateByProbe(Snapshot snapshot, String sinkPath, RuleFileVersion.State stamp) {
        if (snapshot == null || stamp == null) {
            return false;
        }
        return sinkPath.equals(snapshot.sinkPath) && snapshot.stamp.sameProbe(stamp);
    }

    private static boolean isUpToDateByStamp(Snapshot snapshot, String sinkPath, RuleFileVersion.State stamp) {
        return isUpToDateByProbe(snapshot, sinkPath, stamp)
                && snapshot.stamp.sameStamped(stamp);
    }

    private static String resolveSinkPath() {
        String override = sinkPathOverrideForTesting;
        if (override == null || override.isBlank()) {
            return SINK_JSON_PATH;
        }
        return override.trim();
    }

    private static List<SinkModel> loadSinkModelsFromRule(SinkRule rule) {
        if (rule == null || rule.getLevels() == null) {
            logger.warn("sink rules levels is empty");
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
                        String normalizedSeverity = SinkRuleSupport.normalizeSeverity(severity, "medium");
                        model.setSeverity(normalizedSeverity);
                        String explicitTier = SinkRuleSupport.normalizeRuleTier(condition.getRuleTier());
                        model.setRuleTier(explicitTier.isBlank()
                                ? SinkRuleSupport.resolveRuleTier(normalizedDesc, normalizedSeverity)
                                : explicitTier);
                        List<String> explicitTags = SinkRuleSupport.normalizeTags(condition.getTags());
                        model.setTags(explicitTags.isEmpty() ? SinkRuleSupport.buildTags(category) : explicitTags);
                        String explicitBoxName = safe(condition.getBoxName());
                        model.setBoxName(explicitBoxName.isBlank()
                                ? SinkRuleSupport.buildBoxName(className, methodName, normalizedDesc)
                                : explicitBoxName);
                        sinkMap.put(key, model);
                    } else {
                        String existingSev = existing.getSeverity();
                        String incoming = SinkRuleSupport.normalizeSeverity(severity, "medium");
                        if (severityRank(incoming) > severityRank(existingSev)) {
                            existing.setSeverity(incoming);
                        }
                        if (isBlank(existing.getCategory()) && !isBlank(category)) {
                            existing.setCategory(category);
                        }
                        String explicitTier = SinkRuleSupport.normalizeRuleTier(condition.getRuleTier());
                        String incomingTier = explicitTier.isBlank()
                                ? SinkRuleSupport.resolveRuleTier(normalizedDesc, incoming)
                                : explicitTier;
                        if (ruleTierRank(incomingTier) > ruleTierRank(existing.getRuleTier())) {
                            existing.setRuleTier(incomingTier);
                        }
                        List<String> mergedTags = SinkRuleSupport.mergeTags(existing.getTags(), condition.getTags());
                        if (!mergedTags.isEmpty()) {
                            existing.setTags(mergedTags);
                        }
                        if (isBlank(existing.getBoxName()) && !isBlank(condition.getBoxName())) {
                            existing.setBoxName(condition.getBoxName().trim());
                        }
                    }
                }
            }
        }
        if (sinkMap.isEmpty()) {
            logger.warn("sink rules list is empty");
        } else {
            logger.info("loaded {} sinks from sink rules", sinkMap.size());
        }
        return new ArrayList<>(sinkMap.values());
    }

    private static LoadedSinkRule loadSinkRule(String sinkRulePath) {
        Path sinkPath = Paths.get(sinkRulePath);
        if (!Files.exists(sinkPath)) {
            logger.error("CRITICAL: {} not found - sink-based analysis will have no rules", sinkPath);
            RuleValidationSummary validation = RuleValidationSummary.builder("sink", sinkRulePath)
                    .error(0, "", "", "rule_file_missing", "sink rule file not found")
                    .build();
            return new LoadedSinkRule(null, validation);
        }
        try {
            byte[] jsonData = Files.readAllBytes(sinkPath);
            SinkRule rule = JSON.parseObject(new String(jsonData, StandardCharsets.UTF_8), SinkRule.class);
            if (rule == null) {
                logger.warn("load sink.json got null rule");
                RuleValidationSummary validation = RuleValidationSummary.builder("sink", sinkRulePath)
                        .error(0, "", "", "rule_file_invalid", "load sink.json got null rule")
                        .build();
                return new LoadedSinkRule(null, validation);
            }
            RuleValidationSummary validation = SinkDslCompiler.compileInto(rule, sinkRulePath);
            if (validation.hasIssues()) {
                logger.warn("loaded sink rule with validation issues: {} errors={} warnings={}",
                        sinkRulePath,
                        validation.getErrorCount(),
                        validation.getWarningCount());
            }
            return new LoadedSinkRule(rule, validation);
        } catch (Exception ex) {
            logger.warn("load sink.json failed: {}", ex.toString());
            RuleValidationSummary validation = RuleValidationSummary.builder("sink", sinkRulePath)
                    .error(0, "", "", "rule_file_load_failed", ex.toString())
                    .build();
            return new LoadedSinkRule(null, validation);
        }
    }

    private static final class Snapshot {
        private final String sinkPath;
        private final RuleFileVersion.State stamp;
        private final SinkRule rule;
        private final RuleValidationSummary validation;
        private final List<SinkModel> sinkModels;
        private final long version;

        private Snapshot(String sinkPath,
                         RuleFileVersion.State stamp,
                         SinkRule rule,
                         RuleValidationSummary validation,
                         List<SinkModel> sinkModels,
                         long version) {
            this.sinkPath = sinkPath;
            this.stamp = stamp;
            this.rule = rule;
            this.validation = validation == null
                    ? RuleValidationSummary.builder("sink", sinkPath).build()
                    : validation;
            this.sinkModels = sinkModels == null ? Collections.emptyList() : sinkModels;
            this.version = version;
        }
    }

    private record LoadedSinkRule(SinkRule rule, RuleValidationSummary validation) {
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

    private static String normalizeSinkDesc(String desc) {
        String value = safe(desc);
        if (value.isBlank() || "null".equalsIgnoreCase(value)) {
            return "*";
        }
        return value;
    }

    private static String stripParams(String name) {
        int idx = name.indexOf('(');
        if (idx > 0) {
            return name.substring(0, idx);
        }
        return name;
    }

    private static String buildSimpleName(SinkModel sink) {
        if (sink == null) {
            return null;
        }
        String className = sink.getClassName();
        String methodName = sink.getMethodName();
        if (className == null || methodName == null) {
            return null;
        }
        String simple = className;
        int idx = simple.lastIndexOf('/');
        if (idx >= 0 && idx < simple.length() - 1) {
            simple = simple.substring(idx + 1);
        }
        simple = simple.replace('$', '.');
        return simple + "." + methodName;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
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

}
