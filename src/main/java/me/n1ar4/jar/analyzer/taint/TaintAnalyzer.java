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

import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class TaintAnalyzer {
    private static final Logger logger = LogManager.getLogger();

    public static final String TAINT = "TAINT";

    @SuppressWarnings("all")
    public static List<TaintResult> analyze(List<DFSResult> resultList) {
        return analyze(resultList, null, null);
    }

    @SuppressWarnings("all")
    public static List<TaintResult> analyze(List<DFSResult> resultList, Integer timeoutMs, Integer maxPaths) {
        return analyze(resultList, timeoutMs, maxPaths, null);
    }

    @SuppressWarnings("all")
    public static List<TaintResult> analyze(List<DFSResult> resultList,
                                            Integer timeoutMs,
                                            Integer maxPaths,
                                            AtomicBoolean cancelFlag) {
        List<TaintResult> taintResult = new ArrayList<>();

        InputStream sin = TaintAnalyzer.class.getClassLoader().getResourceAsStream("sanitizer.json");
        SanitizerRule rule = SanitizerRule.loadJSON(sin);
        logger.info("污点分析加载 sanitizer 规则数量：{}", rule.getRules().size());

        InputStream tin = TaintAnalyzer.class.getClassLoader().getResourceAsStream("taint-model.json");
        TaintModelRule modelRule = TaintModelRule.loadJSON(tin);
        int modelCount = modelRule.getRules() == null ? 0 : modelRule.getRules().size();
        logger.info("污点分析加载 taint model 规则数量：{}", modelCount);

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
                byte[] clsBytes;
                try {
                    clsBytes = Files.readAllBytes(Paths.get(absPath));
                } catch (Exception ex) {
                    logger.error("污点分析读文件错误: {}", ex.toString());
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
                    // 1) 参数作为 source
                    for (int k = 0; k < paramCount; k++) {
                        if (shouldCancel(cancelFlag)) {
                            truncated = true;
                            truncateReason = "taint_canceled";
                            break outer;
                        }
                        segmentOk = runSegment(clsBytes, m, next, k, rule, modelRule, text, pass,
                                lowConfidence, false, false);
                        if (segmentOk) {
                            break;
                        }
                    }

                    // 2) this 作为 source
                    if (!segmentOk) {
                        segmentOk = runSegment(clsBytes, m, next, Sanitizer.THIS_PARAM, rule, modelRule, text, pass,
                                lowConfidence, false, false);
                    }

                    // 3) 启发式：字段/返回值作为 source
                    if (!segmentOk) {
                        text.append("启发式: 字段/返回值作为源\n");
                        segmentOk = runSegment(clsBytes, m, next, Sanitizer.NO_PARAM, rule, modelRule, text, pass,
                                lowConfidence, true, true);
                    }

                    if (!segmentOk) {
                        chainUnproven = true;
                        text.append(String.format("第 %d 个链段未证明，继续尝试后续链段", i + 1));
                        text.append("\n");
                        continue;
                    }
                } else {
                    boolean segmentOk = runSegment(clsBytes, m, next, pass.get().toParamIndex(), rule, modelRule, text, pass,
                            lowConfidence, false, false);
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
                                      SanitizerRule rule,
                                      TaintModelRule modelRule,
                                      StringBuilder text,
                                      AtomicReference<TaintPass> pass,
                                      AtomicBoolean lowConfidence,
                                      boolean fieldAsSource,
                                      boolean returnAsSource) {
        try {
            String label = formatParamLabel(seedParam);
            logger.info("开始分析方法 {} 参数: {}", cur.getName(), label);
            text.append(String.format("开始分析方法 %s 参数: %s", cur.getName(), label));
            text.append("\n");
            pass.set(TaintPass.fail());
            TaintClassVisitor tcv = new TaintClassVisitor(seedParam, cur, next, pass, rule,
                    modelRule, text,
                    true, fieldAsSource, returnAsSource, lowConfidence);
            ClassReader cr = new ClassReader(clsBytes);
            cr.accept(tcv, Const.AnalyzeASMOptions);
            String passLabel = pass.get().formatLabel();
            logger.info("数据流结果 - 传播到参数 {}", passLabel);
            text.append(String.format("数据流结果 - 传播到参数 %s", passLabel));
            text.append("\n");
            return !pass.get().isFail();
        } catch (Exception e) {
            logger.error("污点分析 - 链中 - 错误: {}", e.toString());
            return false;
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

    private static boolean shouldCancel(AtomicBoolean cancelFlag) {
        return Thread.currentThread().isInterrupted()
                || (cancelFlag != null && cancelFlag.get());
    }
}
