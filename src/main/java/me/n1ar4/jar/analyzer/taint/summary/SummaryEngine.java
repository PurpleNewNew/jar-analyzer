/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.taint.summary;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.rules.ModelRegistry;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.taint.TaintAnalysisProfile;
import me.n1ar4.jar.analyzer.taint.TaintPropagationConfig;
import me.n1ar4.jar.analyzer.taint.TaintPropagationMode;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class SummaryEngine {
    private static final Logger logger = LogManager.getLogger();
    private static final String PROP_SUMMARY_CACHE = "jar.analyzer.taint.summary.cache.size";
    private static final String PROP_DB_CACHE = "jar.analyzer.taint.summary.cache.db";
    private static final String CACHE_TYPE = "summary";
    private final SummaryCache cache;
    private final SummaryBuilder builder;
    private final boolean dbCacheEnabled;
    private final Object ruleContextLock = new Object();
    private volatile long ruleVersion = -1L;
    private volatile String ruleFingerprint;
    private volatile long projectBuildSeq = -1L;
    private volatile String projectKey;
    private volatile String fingerprint;

    public SummaryEngine() {
        int size = 2048;
        try {
            String raw = System.getProperty(PROP_SUMMARY_CACHE);
            if (raw != null && !raw.trim().isEmpty()) {
                size = Integer.parseInt(raw.trim());
            }
        } catch (NumberFormatException ex) {
            logger.debug("invalid summary cache size: {}", System.getProperty(PROP_SUMMARY_CACHE));
        }
        this.cache = new SummaryCache(size);
        this.builder = new SummaryBuilder();
        this.dbCacheEnabled = resolveDbCacheEnabled();
    }

    public MethodSummary getSummary(MethodReference.Handle handle) {
        if (handle == null) {
            return null;
        }
        ensureRuleContext();
        MethodSummary cached = cache.get(handle);
        if (cached != null) {
            return cached;
        }
        if (dbCacheEnabled) {
            MethodSummary fromDb = loadFromDb(handle);
            if (fromDb != null) {
                cache.put(handle, fromDb);
                return fromDb;
            }
        }
        MethodSummary summary = builder.build(handle);
        if (summary != null) {
            cache.put(handle, summary);
            if (dbCacheEnabled && !summary.isUnknown()) {
                persistToDb(handle, summary);
            }
        }
        return summary;
    }

    private void ensureRuleContext() {
        long currentVersion = ModelRegistry.getVersion();
        String currentFingerprint = ModelRegistry.getRulesFingerprint();
        String currentProjectKey = ActiveProjectContext.getActiveProjectKey();
        long currentBuildSeq = DatabaseManager.getProjectBuildSeq(currentProjectKey);
        String existing = fingerprint;
        if (currentVersion == ruleVersion
                && currentFingerprint.equals(ruleFingerprint)
                && currentBuildSeq == projectBuildSeq
                && currentProjectKey.equals(projectKey)
                && existing != null
                && !existing.isEmpty()) {
            return;
        }
        synchronized (ruleContextLock) {
            long latestVersion = ModelRegistry.getVersion();
            String latestRuleFingerprint = ModelRegistry.getRulesFingerprint();
            String latestProjectKey = ActiveProjectContext.getActiveProjectKey();
            long latestBuildSeq = DatabaseManager.getProjectBuildSeq(latestProjectKey);
            boolean ruleChanged = latestVersion != ruleVersion || !latestRuleFingerprint.equals(ruleFingerprint);
            boolean projectChanged = !latestProjectKey.equals(projectKey);
            boolean buildChanged = latestBuildSeq != projectBuildSeq;
            if (ruleChanged || projectChanged || buildChanged) {
                cache.clear();
                if (dbCacheEnabled && ruleChanged) {
                    DatabaseManager.clearSemanticCacheType(CACHE_TYPE);
                }
            }
            if (ruleChanged) {
                ruleVersion = latestVersion;
                ruleFingerprint = latestRuleFingerprint;
                fingerprint = buildFingerprint(latestRuleFingerprint);
            }
            if (ruleChanged || projectChanged || buildChanged) {
                projectBuildSeq = latestBuildSeq;
                projectKey = latestProjectKey;
            }
            if (ruleChanged || projectChanged || buildChanged) {
                logger.info("summary engine context refreshed: projectKey={} buildSeq={} version={} fingerprint={}",
                        latestProjectKey, latestBuildSeq, latestVersion, fingerprint);
            }
        }
    }

    private boolean resolveDbCacheEnabled() {
        String raw = System.getProperty(PROP_DB_CACHE);
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        return !"false".equalsIgnoreCase(raw.trim());
    }

    private MethodSummary loadFromDb(MethodReference.Handle handle) {
        try {
            String key = buildCacheKey(handle);
            String cached = DatabaseManager.getSemanticCacheValue(key, CACHE_TYPE);
            if (cached == null || cached.trim().isEmpty()) {
                return null;
            }
            return MethodSummarySerde.fromCacheValue(cached);
        } catch (Exception ex) {
            logger.debug("load summary from db cache failed: {}", ex.toString());
            return null;
        }
    }

    private void persistToDb(MethodReference.Handle handle, MethodSummary summary) {
        try {
            String key = buildCacheKey(handle);
            String value = MethodSummarySerde.toCacheValue(summary);
            if (value == null || value.isEmpty()) {
                return;
            }
            DatabaseManager.putSemanticCacheValue(key, CACHE_TYPE, value);
        } catch (Exception ex) {
            logger.debug("persist summary to db cache failed: {}", ex.toString());
        }
    }

    private String buildCacheKey(MethodReference.Handle handle) {
        String currentProjectKey = ActiveProjectContext.getActiveProjectKey();
        long buildSeq = DatabaseManager.getProjectBuildSeq(currentProjectKey);
        String fp = fingerprint;
        if (fp == null || fp.isBlank()) {
            fp = buildFingerprint(ModelRegistry.getRulesFingerprint());
            fingerprint = fp;
        }
        int jarId = normalizeJarId(handle.getJarId());
        return currentProjectKey + "|" + buildSeq + "|" + handle.getClassReference().getName()
                + "#" + handle.getName()
                + "#" + handle.getDesc()
                + "#" + jarId
                + "|" + fp;
    }

    private static int normalizeJarId(Integer jarId) {
        if (jarId == null || jarId < 0) {
            return -1;
        }
        return jarId;
    }

    private static String buildFingerprint(String rulesFingerprint) {
        TaintPropagationConfig config = TaintPropagationConfig.resolve();
        TaintPropagationMode mode = config == null ? null : config.getPropagationMode();
        TaintAnalysisProfile profile = config == null ? null : config.getProfile();
        String level = profile == null || profile.getLevel() == null ? "unknown" : profile.getLevel().name().toLowerCase();
        String modeName = mode == null ? "unknown" : mode.name().toLowerCase();
        String steps = encodeAdditionalSteps(profile == null ? null : profile.getAdditionalSteps());
        String modelStamp = rulesFingerprint == null || rulesFingerprint.isBlank() ? "unknown" : rulesFingerprint;
        return "v1"
                + "|app=" + Const.version
                + "|mode=" + modeName
                + "|profile=" + level
                + "|steps=" + steps
                + "|model=" + modelStamp;
    }

    private static String encodeAdditionalSteps(Set<TaintAnalysisProfile.AdditionalStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return "";
        }
        List<String> list = new ArrayList<>();
        for (TaintAnalysisProfile.AdditionalStep step : steps) {
            if (step != null) {
                list.add(step.name().toLowerCase());
            }
        }
        Collections.sort(list);
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(s);
        }
        return sb.toString();
    }

}
