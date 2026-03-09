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
import me.n1ar4.jar.analyzer.engine.SearchCondition;
import me.n1ar4.jar.analyzer.rules.sink.SinkRule;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.Type;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
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
    private static final String SINK_JSON_PROP = "jar.analyzer.rules.sink.path";
    private static final String SINK_JSON_PATH = "rules/sink.json";
    private static final AtomicLong VERSION_SEQ = new AtomicLong(0L);
    private static final long CHANGE_CHECK_INTERVAL_MS = 1000L;

    private static volatile Snapshot cachedSnapshot;
    private static volatile long nextCheckAfterMs;

    private SinkRuleRegistry() {
    }

    public static SinkRule getSinkRule() {
        return currentSnapshot().rule;
    }

    public static List<SinkModel> getSinkModels() {
        return currentSnapshot().sinkModels;
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
        }
        return currentSnapshot().version;
    }

    public static long checkNow() {
        synchronized (SinkRuleRegistry.class) {
            nextCheckAfterMs = 0L;
        }
        return currentSnapshot().version;
    }

    private static Snapshot currentSnapshot() {
        Snapshot local = cachedSnapshot;
        long now = System.currentTimeMillis();
        if (local != null && now < nextCheckAfterMs) {
            return local;
        }
        String sinkPath = resolveSinkPath();
        RuleFileStamp stamp = RuleFileStamp.of(sinkPath);
        if (isUpToDate(local, sinkPath, stamp)) {
            nextCheckAfterMs = now + CHANGE_CHECK_INTERVAL_MS;
            return local;
        }
        synchronized (SinkRuleRegistry.class) {
            now = System.currentTimeMillis();
            Snapshot latest = cachedSnapshot;
            if (latest != null && now < nextCheckAfterMs) {
                return latest;
            }
            sinkPath = resolveSinkPath();
            stamp = RuleFileStamp.of(sinkPath);
            if (isUpToDate(latest, sinkPath, stamp)) {
                nextCheckAfterMs = now + CHANGE_CHECK_INTERVAL_MS;
                return latest;
            }
            LoadedSinkRule loaded = loadSinkRule(sinkPath);
            SinkRule rule = loaded.rule();
            List<SinkModel> models = loadSinkModelsFromRule(rule);
            Snapshot refreshed = new Snapshot(
                    sinkPath,
                    stamp,
                    rule,
                    loaded.validation(),
                    models.isEmpty() ? Collections.emptyList() : List.copyOf(models),
                    VERSION_SEQ.incrementAndGet()
            );
            cachedSnapshot = refreshed;
            nextCheckAfterMs = now + CHANGE_CHECK_INTERVAL_MS;
            logger.info("sink rule registry refreshed: path={} version={} sinks={}",
                    sinkPath,
                    refreshed.version,
                    refreshed.sinkModels.size());
            return refreshed;
        }
    }

    private static boolean isUpToDate(Snapshot snapshot, String sinkPath, RuleFileStamp stamp) {
        if (snapshot == null || stamp == null) {
            return false;
        }
        return sinkPath.equals(snapshot.sinkPath) && stamp.equals(snapshot.stamp);
    }

    private static String resolveSinkPath() {
        String override = System.getProperty(SINK_JSON_PROP);
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
        private final RuleFileStamp stamp;
        private final SinkRule rule;
        private final RuleValidationSummary validation;
        private final List<SinkModel> sinkModels;
        private final long version;

        private Snapshot(String sinkPath,
                         RuleFileStamp stamp,
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

    private static final class RuleFileStamp {
        private final boolean exists;
        private final long modifiedTime;
        private final long size;
        private final String contentHash;

        private RuleFileStamp(boolean exists, long modifiedTime, long size, String contentHash) {
            this.exists = exists;
            this.modifiedTime = modifiedTime;
            this.size = size;
            this.contentHash = contentHash == null ? "" : contentHash;
        }

        private static RuleFileStamp of(String rawPath) {
            if (rawPath == null || rawPath.isBlank()) {
                return new RuleFileStamp(false, -1L, -1L, "");
            }
            try {
                Path path = Paths.get(rawPath);
                if (!Files.exists(path)) {
                    return new RuleFileStamp(false, -1L, -1L, "");
                }
                byte[] data = Files.readAllBytes(path);
                long modified = Files.getLastModifiedTime(path).toMillis();
                long fileSize = data.length;
                return new RuleFileStamp(true, modified, fileSize, sha256Hex(data));
            } catch (Exception ex) {
                logger.debug("resolve sink rule stamp failed: path={} err={}", rawPath, ex.toString());
                return new RuleFileStamp(false, -1L, -1L, "");
            }
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
                    && size == other.size
                    && contentHash.equals(other.contentHash);
        }

        @Override
        public int hashCode() {
            int result = Boolean.hashCode(exists);
            result = 31 * result + Long.hashCode(modifiedTime);
            result = 31 * result + Long.hashCode(size);
            result = 31 * result + contentHash.hashCode();
            return result;
        }
    }

    private static String sha256Hex(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (Exception ex) {
            logger.debug("compute sink rule hash failed: {}", ex.toString());
            return "";
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
