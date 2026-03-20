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
import me.n1ar4.jar.analyzer.core.ProjectStateUtil;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.project.ProjectRuntimeState;
import me.n1ar4.jar.analyzer.rules.ModelRegistry;
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
    private static final int DEFAULT_CACHE_SIZE = 2048;
    private static final String CACHE_TYPE = "summary";
    private final SummaryCache cache;
    private final SummaryBuilder builder;
    private final boolean dbCacheEnabled;
    private final Object ruleContextLock = new Object();
    private volatile long ruleVersion = -1L;
    private volatile String ruleFingerprint;
    private volatile String projectRuntimeKey;
    private volatile String fingerprint;

    public SummaryEngine() {
        this.cache = new SummaryCache(DEFAULT_CACHE_SIZE);
        this.builder = new SummaryBuilder();
        this.dbCacheEnabled = true;
    }

    public MethodSummary getSummary(MethodReference.Handle handle) {
        return getSummary(handle, null);
    }

    public MethodSummary getSummary(MethodReference.Handle handle, TaintPropagationConfig propagationConfig) {
        if (handle == null) {
            return null;
        }
        ensureRuleContext(propagationConfig);
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
        MethodSummary summary = builder.build(handle, propagationConfig);
        if (summary != null) {
            cache.put(handle, summary);
            if (dbCacheEnabled && !summary.isUnknown()) {
                persistToDb(handle, summary);
            }
        }
        return summary;
    }

    private void ensureRuleContext() {
        ensureRuleContext(null);
    }

    private void ensureRuleContext(TaintPropagationConfig propagationConfig) {
        TaintPropagationConfig effectiveConfig = propagationConfig == null
                ? TaintPropagationConfig.resolve()
                : propagationConfig;
        long currentVersion = ModelRegistry.getVersion();
        String currentFingerprint = ModelRegistry.getRulesFingerprint();
        String currentRuntimeKey = ProjectStateUtil.runtimeCacheKey();
        String currentCombinedFingerprint = buildFingerprint(currentFingerprint, effectiveConfig);
        String existing = fingerprint;
        if (currentVersion == ruleVersion
                && currentFingerprint.equals(ruleFingerprint)
                && currentRuntimeKey.equals(projectRuntimeKey)
                && currentCombinedFingerprint.equals(existing)
                && existing != null
                && !existing.isEmpty()) {
            return;
        }
        synchronized (ruleContextLock) {
            TaintPropagationConfig latestConfig = propagationConfig == null
                    ? TaintPropagationConfig.resolve()
                    : propagationConfig;
            long latestVersion = ModelRegistry.getVersion();
            String latestRuleFingerprint = ModelRegistry.getRulesFingerprint();
            String latestRuntimeKey = ProjectStateUtil.runtimeCacheKey();
            String latestCombinedFingerprint = buildFingerprint(latestRuleFingerprint, latestConfig);
            boolean ruleChanged = latestVersion != ruleVersion || !latestRuleFingerprint.equals(ruleFingerprint);
            boolean runtimeChanged = !latestRuntimeKey.equals(projectRuntimeKey);
            boolean analysisChanged = !latestCombinedFingerprint.equals(fingerprint);
            if (ruleChanged || runtimeChanged || analysisChanged) {
                cache.clear();
                if (dbCacheEnabled && ruleChanged) {
                    DatabaseManager.clearSemanticCacheType(CACHE_TYPE);
                }
            }
            if (ruleChanged) {
                ruleVersion = latestVersion;
                ruleFingerprint = latestRuleFingerprint;
            }
            if (ruleChanged || runtimeChanged || analysisChanged) {
                projectRuntimeKey = latestRuntimeKey;
                fingerprint = latestCombinedFingerprint;
            }
            if (ruleChanged || runtimeChanged || analysisChanged) {
                ProjectRuntimeState runtimeState = ProjectStateUtil.runtimeState();
                logger.info("summary engine context refreshed: projectKey={} buildSeq={} version={} fingerprint={}",
                        runtimeState.projectKey(), runtimeState.buildSeq(), latestVersion, fingerprint);
            }
        }
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
        String runtimeKey = ProjectStateUtil.runtimeCacheKey();
        String fp = fingerprint;
        if (fp == null || fp.isBlank()) {
            fp = buildFingerprint(ModelRegistry.getRulesFingerprint(), TaintPropagationConfig.resolve());
            fingerprint = fp;
        }
        int jarId = normalizeJarId(handle.getJarId());
        return runtimeKey + "|" + handle.getClassReference().getName()
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

    private static String buildFingerprint(String rulesFingerprint, TaintPropagationConfig config) {
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
