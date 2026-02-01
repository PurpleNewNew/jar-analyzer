/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.taint;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.RuntimeClassResolver;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class TaintAnalyzer {
    private static final Logger logger = LogManager.getLogger();

    public static final String TAINT = "TAINT";
    private static final int SEGMENT_CACHE_MAX = 512;
    private static final Map<String, SegmentCache> SEGMENT_CACHE =
            Collections.synchronizedMap(new LinkedHashMap<String, SegmentCache>(SEGMENT_CACHE_MAX, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, SegmentCache> eldest) {
                    return size() > SEGMENT_CACHE_MAX;
                }
            });
    private static volatile String SEGMENT_CACHE_CONTEXT = "";

    @SuppressWarnings("all")
    public static List<TaintResult> analyze(List<DFSResult> resultList) {
        return analyze(resultList, null, null, null, null, false);
    }

    @SuppressWarnings("all")
    public static List<TaintResult> analyze(List<DFSResult> resultList, Integer timeoutMs, Integer maxPaths) {
        return analyze(resultList, timeoutMs, maxPaths, null, null, false);
    }

    @SuppressWarnings("all")
    public static List<TaintResult> analyze(List<DFSResult> resultList,
                                            Integer timeoutMs,
                                            Integer maxPaths,
                                            AtomicBoolean cancelFlag) {
        return analyze(resultList, timeoutMs, maxPaths, cancelFlag, null, false);
    }

    @SuppressWarnings("all")
    public static List<TaintResult> analyze(List<DFSResult> resultList,
                                            Integer timeoutMs,
                                            Integer maxPaths,
                                            AtomicBoolean cancelFlag,
                                            Integer seedParam,
                                            boolean strictSeed) {
        List<TaintResult> taintResult = new ArrayList<>();

        TaintPropagationConfig propagationConfig = TaintPropagationConfig.resolve();
        SanitizerRule rule = propagationConfig.getBarrierRule();
        int sanitizerCount = rule.getRules() == null ? 0 : rule.getRules().size();
        logger.info("污点分析加载 barrier 规则数量：{}", sanitizerCount);

        TaintModelRule summaryRule = propagationConfig.getSummaryRule();
        int summaryCount = summaryRule.getRules() == null ? 0 : summaryRule.getRules().size();
        logger.info("污点分析加载 summary 规则数量：{}", summaryCount);

        TaintModelRule additionalRule = propagationConfig.getAdditionalRule();
        int additionalCount = additionalRule.getRules() == null ? 0 : additionalRule.getRules().size();
        logger.info("污点分析加载 additional 规则数量：{}", additionalCount);

        TaintPropagationMode propagationMode = propagationConfig.getPropagationMode();
        TaintAnalysisProfile profile = propagationConfig.getProfile();
        logger.info("taint propagation mode: {}", propagationMode.name().toLowerCase());
        logger.info("taint profile: {}", profile.getLevel().name().toLowerCase());
        boolean seedHeuristicEnabled = profile == null || profile.isSeedHeuristicEnabled();

        CoreEngine engine = MainForm.getEngine();
        long startNs = System.nanoTime();
        int processed = 0;
        boolean truncated = false;
        String truncateReason = "";
        outer:
        for (DFSResult result : resultList) {
            if (shouldCancel(cancelFlag)) {
                truncated = true;
                truncateReason = "taint_canceled";
                break;
            }
            if (timeoutMs != null && timeoutMs > 0) {
                long elapsed = (System.nanoTime() - startNs) / 1_000_000L;
                if (elapsed >= timeoutMs) {
                    truncated = true;
                    truncateReason = "taint_timeout";
                    break;
                }
            }
            if (maxPaths != null && maxPaths > 0 && processed >= maxPaths) {
                truncated = true;
                truncateReason = "taint_maxPaths";
                break;
            }
            boolean thisChainSuccess = false;
            boolean chainUnproven = false;
            AtomicBoolean lowConfidence = new AtomicBoolean(false);
            StringBuilder text = new StringBuilder();
            text.append("taint profile: ").append(profile.getLevel().name().toLowerCase()).append("\n");
            text.append("taint propagation mode: ").append(propagationMode.name().toLowerCase()).append("\n");
            String sinkKind = resolveSinkKind(result);
            System.out.println("####################### 污点分析进行中 #######################");
            List<MethodReference.Handle> methodList = result.getMethodList();

            if (methodList == null || methodList.isEmpty()) {
                TaintResult r = new TaintResult();
                r.setDfsResult(result);
                r.setSuccess(false);
                String reason = result.isTruncated() ? result.getTruncateReason() : "empty_chain";
                r.setTaintText("TAINT SKIP: " + reason);
                taintResult.add(r);
                processed++;
                continue;
            }

            // 上一个方法调用 污点传递到第几个参数
            // ！！关键！！
            // 方法之间 数据流/污点传播 完全靠该字段实现
            AtomicReference<TaintPass> pass = new AtomicReference<>(TaintPass.fail());

            // 遍历 chains
            for (int i = 0; i < methodList.size(); i++) {
                if (shouldCancel(cancelFlag)) {
                    truncated = true;
                    truncateReason = "taint_canceled";
                    break outer;
                }
                // 不分析最后一个 chain
                // 因为最后一个一般是 jdk 的 sink
                // 但是用户很可能不加载 jdk 的东西
                // 如果只要上一个可以到达最后一个
                // 即可认为污点分析成功
                if (i == methodList.size() - 1) {
                    logger.info("污点分析执行结束");
                    text.append("污点分析执行结束");
                    text.append("\n");
                    if (!pass.get().isFail()) {
                        thisChainSuccess = true;
                        logger.info("该链污点分析结果：通过");
                        text.append("该链污点分析结果：通过");
                        text.append("\n");
                    }
                    break;
                }

                MethodReference.Handle m = methodList.get(i);
                MethodReference.Handle next = methodList.get(i + 1);

                String classOrigin = m.getClassReference().getName();
                classOrigin = classOrigin.replace(".", "/");
                String absPath = engine.getAbsPath(classOrigin);

                if (absPath == null || absPath.trim().isEmpty()) {
                    logger.warn("污点分析找不到类: {}", m.getClassReference().getName());
                    break;
                }
                byte[] clsBytes = me.n1ar4.jar.analyzer.utils.BytecodeCache.read(Paths.get(absPath));
                if (clsBytes == null || clsBytes.length == 0) {
                    logger.error("污点分析读文件错误: {}", absPath);
                    return new ArrayList<>();
                }

                String desc = m.getDesc();
                Type[] argumentTypes = Type.getArgumentTypes(desc);
                int paramCount = argumentTypes.length;

                logger.info("方法: {} 参数数量: {}", m.getName(), paramCount);
                text.append(String.format("方法: %s 参数数量: %d", m.getName(), paramCount));
                text.append("\n");

                if (pass.get().isFail()) {
                    if (i == 0) {
                        logger.info("开始污点分析 - 链开始 - 无数据流");
                        text.append("开始污点分析 - 链开始 - 无数据流");
                        text.append("\n");
                    } else {
                        text.append(String.format("第 %d 个链段无已知污点，尝试重新建立数据流", i + 1));
                        text.append("\n");
                    }

                    boolean segmentOk = false;
                    if (i == 0 && seedParam != null) {
                        if (!isSeedParamValid(seedParam, paramCount)) {
                            text.append("固定起点参数无效: ").append(formatParamLabel(seedParam)).append("\n");
                            if (strictSeed) {
                                chainUnproven = true;
                                text.append("严格模式: 固定起点参数无效，终止链段分析\n");
                                break;
                            }
                        } else {
                            text.append("使用固定起点参数: ").append(formatParamLabel(seedParam)).append("\n");
                            boolean seedHeuristic = seedParam == Sanitizer.NO_PARAM && seedHeuristicEnabled;
                            SegmentSourceMode seedMode = seedHeuristic
                                    ? SegmentSourceMode.FIELD_AND_RETURN
                                    : SegmentSourceMode.NONE;
                            if (seedParam == Sanitizer.NO_PARAM && !seedHeuristicEnabled) {
                                text.append("strict profile: skip heuristic seed\n");
                            }
                            segmentOk = runSegment(clsBytes, m, next, seedParam, propagationConfig, text, pass,
                                    lowConfidence, seedMode, sinkKind);
                            if (!segmentOk && strictSeed) {
                                chainUnproven = true;
                                text.append("严格模式: 固定起点参数未通过，终止链段分析\n");
                                break;
                            }
                        }
                    }

                    // 1) 参数作为 source
                    if (!segmentOk) {
                        for (int k = 0; k < paramCount; k++) {
                            if (shouldCancel(cancelFlag)) {
                                truncated = true;
                                truncateReason = "taint_canceled";
                                break outer;
                            }
                            segmentOk = runSegment(clsBytes, m, next, k, propagationConfig, text, pass,
                                    lowConfidence, SegmentSourceMode.NONE, sinkKind);
                            if (segmentOk) {
                                break;
                            }
                        }
                    }

                    // 2) this 作为 source
                    if (!segmentOk) {
                        segmentOk = runSegment(clsBytes, m, next, Sanitizer.THIS_PARAM, propagationConfig, text, pass,
                                lowConfidence, SegmentSourceMode.NONE, sinkKind);
                    }

                    // 3) 启发式：字段/返回值作为 source
                    if (!segmentOk) {
                        if (seedHeuristicEnabled) {
                            text.append("heuristic field/return as source\n");
                            segmentOk = runSegment(clsBytes, m, next, Sanitizer.NO_PARAM, propagationConfig, text, pass,
                                    lowConfidence, SegmentSourceMode.FIELD_AND_RETURN, sinkKind);
                        } else {
                            text.append("strict profile: skip heuristic field/return source\n");
                        }
                    }

                    if (!segmentOk) {
                        chainUnproven = true;
                        text.append(String.format("第 %d 个链段未证明，继续尝试后续链段", i + 1));
                        text.append("\n");
                        continue;
                    }
                } else {
                    boolean segmentOk = runSegmentWithPass(clsBytes, m, next, propagationConfig, text, pass,
                            lowConfidence, sinkKind);
                    if (!segmentOk) {
                        chainUnproven = true;
                        text.append(String.format("第 %d 个链段未证明，继续尝试后续链段", i + 1));
                        text.append("\n");
                        continue;
                    }
                }
            }

            if (chainUnproven) {
                lowConfidence.set(true);
                text.append("低置信: 存在未证明链段\n");
            }

            if (thisChainSuccess) {
                TaintResult r = new TaintResult();
                r.setDfsResult(result);
                r.setSuccess(true);
                r.setLowConfidence(lowConfidence.get());
                r.setTaintText(text.toString());
                taintResult.add(r);
            } else {
                // 2025/10/13
                // 污点分析失败的也应该加入
                TaintResult r = new TaintResult();
                r.setDfsResult(result);
                r.setSuccess(false);
                r.setLowConfidence(lowConfidence.get());
                r.setTaintText(text.toString());
                taintResult.add(r);
            }
            processed++;
        }

        if (truncated) {
            DFSResult meta = new DFSResult();
            meta.setMethodList(new ArrayList<>());
            meta.setEdges(new ArrayList<>());
            meta.setDepth(0);
            meta.setMode(DFSResult.FROM_SOURCE_TO_ALL);
            meta.setTruncated(true);
            meta.setTruncateReason(truncateReason);
            meta.setRecommend("Try increase timeoutMs or reduce depth/maxLimit/blacklist.");
            meta.setPathCount(processed);
            meta.setElapsedMs((System.nanoTime() - startNs) / 1_000_000L);
            TaintResult r = new TaintResult();
            r.setDfsResult(meta);
            r.setSuccess(false);
            r.setTaintText("TAINT TRUNCATED: " + truncateReason);
            taintResult.add(r);
        }

        return taintResult;
    }

    private static boolean runSegment(byte[] clsBytes,
                                      MethodReference.Handle cur,
                                      MethodReference.Handle next,
                                      int seedParam,
                                      TaintPropagationConfig propagationConfig,
                                      StringBuilder text,
                                      AtomicReference<TaintPass> pass,
                                      AtomicBoolean lowConfidence,
                                      SegmentSourceMode sourceMode,
                                      String sinkKind) {
        try {
            SegmentSourceMode mode = sourceMode == null ? SegmentSourceMode.NONE : sourceMode;
            TaintPropagationConfig config = propagationConfig == null
                    ? TaintPropagationConfig.resolve()
                    : propagationConfig;
            String contextKey = buildSegmentContextKey(config);
            ensureSegmentCacheContext(contextKey);
            String cacheKey = buildSegmentCacheKey(cur, next, seedParam, mode, sinkKind);
            SegmentCache cached = SEGMENT_CACHE.get(cacheKey);
            if (cached != null) {
                if (text != null && cached.text != null && !cached.text.isEmpty()) {
                    text.append(cached.text);
                }
                if (cached.lowConfidence && lowConfidence != null) {
                    lowConfidence.set(true);
                }
                pass.set(cached.pass);
                return cached.success;
            }
            int beforeLen = text == null ? 0 : text.length();
            boolean beforeLow = lowConfidence != null && lowConfidence.get();
            String label = formatParamLabel(seedParam);
            logger.info("开始分析方法 {} 参数: {}", cur.getName(), label);
            text.append(String.format("开始分析方法 %s 参数: %s", cur.getName(), label));
            text.append("\n");
            pass.set(TaintPass.fail());
            TaintClassVisitor tcv = new TaintClassVisitor(seedParam, cur, next, pass,
                    config.getBarrierRule(),
                    config.getSummaryRule(),
                    config.getAdditionalRule(),
                    config.getGuardRules(),
                    config.getProfile(),
                    config.getPropagationMode(),
                    text,
                    true, mode.fieldAsSource, mode.returnAsSource, lowConfidence, sinkKind);
            ClassReader cr = new ClassReader(clsBytes);
            cr.accept(tcv, Const.GlobalASMOptions);
            String passLabel = pass.get().formatLabel();
            logger.info("数据流结果 - 传播到参数 {}", passLabel);
            text.append(String.format("数据流结果 - 传播到参数 %s", passLabel));
            text.append("\n");
            boolean ok = !pass.get().isFail();
            boolean afterLow = lowConfidence != null && lowConfidence.get();
            String segmentText = "";
            if (text != null && text.length() >= beforeLen) {
                segmentText = text.substring(beforeLen);
            }
            SegmentCache entry = new SegmentCache(ok, pass.get(), afterLow && !beforeLow, segmentText);
            SEGMENT_CACHE.put(cacheKey, entry);
            return ok;
        } catch (Exception e) {
            logger.error("污点分析 - 链中 - 错误: {}", e.toString());
            return false;
        }
    }

    private static boolean runSegmentWithPass(byte[] clsBytes,
                                              MethodReference.Handle cur,
                                              MethodReference.Handle next,
                                              TaintPropagationConfig propagationConfig,
                                              StringBuilder text,
                                              AtomicReference<TaintPass> pass,
                                              AtomicBoolean lowConfidence,
                                              String sinkKind) {
        if (pass == null) {
            return false;
        }
        TaintPass current = pass.get();
        if (current == null || current.isFail()) {
            return false;
        }
        if (current.hasAllParams()) {
            AtomicReference<TaintPass> localPass = new AtomicReference<>(TaintPass.fail());
            boolean ok = runSegment(clsBytes, cur, next, Sanitizer.ALL_PARAMS, propagationConfig, text, localPass,
                    lowConfidence, SegmentSourceMode.NONE, sinkKind);
            pass.set(localPass.get());
            return ok;
        }
        TaintPass merged = TaintPass.fail();
        boolean anyOk = false;
        for (Integer seed : current.getParamIndices()) {
            if (seed == null) {
                continue;
            }
            AtomicReference<TaintPass> localPass = new AtomicReference<>(TaintPass.fail());
            boolean ok = runSegment(clsBytes, cur, next, seed, propagationConfig, text, localPass,
                    lowConfidence, SegmentSourceMode.NONE, sinkKind);
            if (ok) {
                anyOk = true;
                merged = merged.merge(localPass.get());
            }
        }
        if (!anyOk) {
            pass.set(TaintPass.fail());
            return false;
        }
        pass.set(merged);
        return true;
    }

    private static void ensureSegmentCacheContext(String contextKey) {
        String normalized = contextKey == null ? "" : contextKey;
        if (normalized.equals(SEGMENT_CACHE_CONTEXT)) {
            return;
        }
        synchronized (SEGMENT_CACHE) {
            if (!normalized.equals(SEGMENT_CACHE_CONTEXT)) {
                SEGMENT_CACHE.clear();
                SEGMENT_CACHE_CONTEXT = normalized;
            }
        }
    }

    private static String buildSegmentContextKey(TaintPropagationConfig config) {
        long rootSeq = RuntimeClassResolver.getRootSeq();
        long buildSeq = DatabaseManager.getBuildSeq();
        String mode = "unknown";
        String level = "unknown";
        String steps = "";
        int summaryCount = 0;
        int additionalCount = 0;
        int barrierCount = 0;
        int guardCount = 0;
        if (config != null) {
            TaintPropagationMode propagationMode = config.getPropagationMode();
            if (propagationMode != null) {
                mode = propagationMode.name();
            }
            TaintAnalysisProfile profile = config.getProfile();
            if (profile != null) {
                if (profile.getLevel() != null) {
                    level = profile.getLevel().name();
                }
                steps = encodeAdditionalSteps(profile);
            }
            summaryCount = countRules(config.getSummaryRule());
            additionalCount = countRules(config.getAdditionalRule());
            barrierCount = countSanitizers(config.getBarrierRule());
            List<TaintGuardRule> guards = config.getGuardRules();
            guardCount = guards == null ? 0 : guards.size();
        }
        return rootSeq + "#" + buildSeq + "#" + mode + "#" + level + "#"
                + steps + "#" + summaryCount + "#" + additionalCount + "#"
                + barrierCount + "#" + guardCount;
    }

    private static int countRules(TaintModelRule rule) {
        if (rule == null || rule.getRules() == null) {
            return 0;
        }
        return rule.getRules().size();
    }

    private static int countSanitizers(SanitizerRule rule) {
        if (rule == null || rule.getRules() == null) {
            return 0;
        }
        return rule.getRules().size();
    }

    private static String encodeAdditionalSteps(TaintAnalysisProfile profile) {
        if (profile == null) {
            return "";
        }
        java.util.Set<TaintAnalysisProfile.AdditionalStep> steps = profile.getAdditionalSteps();
        if (steps == null || steps.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (TaintAnalysisProfile.AdditionalStep step : TaintAnalysisProfile.AdditionalStep.values()) {
            if (steps.contains(step)) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(step.name());
            }
        }
        return sb.toString();
    }

    private static String buildSegmentCacheKey(MethodReference.Handle cur,
                                               MethodReference.Handle next,
                                               int seedParam,
                                               SegmentSourceMode sourceMode,
                                               String sinkKind) {
        SegmentSourceMode mode = sourceMode == null ? SegmentSourceMode.NONE : sourceMode;
        StringBuilder sb = new StringBuilder();
        sb.append(cur.getClassReference().getName()).append("#")
                .append(cur.getName()).append(cur.getDesc()).append("->")
                .append(next.getClassReference().getName()).append("#")
                .append(next.getName()).append(next.getDesc())
                .append("|seed=").append(seedParam)
                .append("|mode=").append(mode.name())
                .append("|kind=").append(sinkKind == null ? "" : sinkKind);
        return sb.toString();
    }

    private enum SegmentSourceMode {
        NONE(false, false),
        FIELD_ONLY(true, false),
        RETURN_ONLY(false, true),
        FIELD_AND_RETURN(true, true);

        private final boolean fieldAsSource;
        private final boolean returnAsSource;

        SegmentSourceMode(boolean fieldAsSource, boolean returnAsSource) {
            this.fieldAsSource = fieldAsSource;
            this.returnAsSource = returnAsSource;
        }
    }

    private static final class SegmentCache {
        private final boolean success;
        private final TaintPass pass;
        private final boolean lowConfidence;
        private final String text;

        private SegmentCache(boolean success, TaintPass pass, boolean lowConfidence, String text) {
            this.success = success;
            this.pass = pass == null ? TaintPass.fail() : pass;
            this.lowConfidence = lowConfidence;
            this.text = text;
        }
    }

    private static String formatParamLabel(int paramIndex) {
        if (paramIndex == Sanitizer.THIS_PARAM) {
            return "this";
        }
        if (paramIndex == Sanitizer.ALL_PARAMS) {
            return "all";
        }
        if (paramIndex == Sanitizer.NO_PARAM) {
            return "none";
        }
        return String.valueOf(paramIndex);
    }

    private static boolean isSeedParamValid(int seedParam, int paramCount) {
        if (seedParam == Sanitizer.NO_PARAM
                || seedParam == Sanitizer.THIS_PARAM
                || seedParam == Sanitizer.ALL_PARAMS) {
            return true;
        }
        return seedParam >= 0 && seedParam < paramCount;
    }

    private static String resolveSinkKind(DFSResult result) {
        if (result == null) {
            return null;
        }
        MethodReference.Handle sink = result.getSink();
        if (sink == null) {
            List<MethodReference.Handle> chain = result.getMethodList();
            if (chain != null && !chain.isEmpty()) {
                sink = chain.get(chain.size() - 1);
            }
        }
        return me.n1ar4.jar.analyzer.rules.ModelRegistry.resolveSinkKind(sink);
    }

    private static boolean shouldCancel(AtomicBoolean cancelFlag) {
        return Thread.currentThread().isInterrupted()
                || (cancelFlag != null && cancelFlag.get());
    }
}
