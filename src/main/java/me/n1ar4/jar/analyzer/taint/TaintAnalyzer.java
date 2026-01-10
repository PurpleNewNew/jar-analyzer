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
import java.util.concurrent.atomic.AtomicReference;

public class TaintAnalyzer {
    private static final Logger logger = LogManager.getLogger();

    public static final String TAINT = "TAINT";

    @SuppressWarnings("all")
    public static List<TaintResult> analyze(List<DFSResult> resultList) {
        List<TaintResult> taintResult = new ArrayList<>();

        InputStream sin = TaintAnalyzer.class.getClassLoader().getResourceAsStream("sanitizer.json");
        SanitizerRule rule = SanitizerRule.loadJSON(sin);
        logger.info("污点分析加载 sanitizer 规则数量：{}", rule.getRules().size());

        CoreEngine engine = MainForm.getEngine();
        for (DFSResult result : resultList) {
            boolean thisChainSuccess = false;
            StringBuilder text = new StringBuilder();
            System.out.println("####################### 污点分析进行中 #######################");
            List<MethodReference.Handle> methodList = result.getMethodList();

            // 上一个方法调用 污点传递到第几个参数
            // ！！关键！！
            // 方法之间 数据流/污点传播 完全靠该字段实现
            AtomicReference<TaintPass> pass = new AtomicReference<>(TaintPass.fail());

            // 遍历 chains
            for (int i = 0; i < methodList.size(); i++) {
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
                    // 2025/08/31 预期不符 BUG
                    // 设计缺陷：当前污点传递是“参数级启发式”，首段无法建立数据流时会导致后续链段仍是 FAIL，
                    // 为避免无意义的噪声分析，这里直接中断，但也可能提前终止真实可达的链路。
                    if (i != 0) {
                        logger.info("第 {} 个链分析结束", i);
                        text.append(String.format("第 %d 个链分析结束", i));
                        text.append("\n");
                        break;
                    }
                    // 第一次开始
                    logger.info("开始污点分析 - 链开始 - 无数据流");
                    text.append("开始污点分析 - 链开始 - 无数据流");
                    text.append("\n");
                    // 注意：这里是“参数级启发式”，默认把首方法的所有参数当作潜在 SOURCE
                    // 并不进行严格的源点追踪（字段/别名/返回值等更细粒度数据流未覆盖）
                    // 遍历所有 source 的参数
                    // 认为所有参数都可能是 source
                    for (int k = 0; k < paramCount; k++) {
                        try {
                            logger.info("开始分析方法 {} 第 {} 个参数", m.getName(), k);
                            text.append(String.format("开始分析方法 %s 第 %d 个参数", m.getName(), k));
                            text.append("\n");
                            TaintClassVisitor tcv = new TaintClassVisitor(k, m, next, pass, rule, text);
                            ClassReader cr = new ClassReader(clsBytes);
                            cr.accept(tcv, Const.AnalyzeASMOptions);
                            pass = tcv.getPass();
                            String passLabel = pass.get().formatLabel();
                            logger.info("数据流结果 - 传播到参数 {}", passLabel);
                            text.append(String.format("数据流结果 - 传播到参数 %s", passLabel));
                            text.append("\n");
                            // 无法抵达第二个 chain 认为有问题
                            if (!pass.get().isFail()) {
                                break;
                            }
                        } catch (Exception e) {
                            logger.error("污点分析 - 链开始 - 错误: {}", e.toString());
                        }
                    }
                } else {
                    // 第二个 chain 开始
                    // 只要顺利 即可继续分析
                    try {
                        TaintClassVisitor tcv = new TaintClassVisitor(pass.get().toParamIndex(), m, next, pass, rule, text);
                        ClassReader cr = new ClassReader(clsBytes);
                        cr.accept(tcv, Const.AnalyzeASMOptions);
                        pass = tcv.getPass();
                        String passLabel = pass.get().formatLabel();
                        logger.info("数据流结果 - 传播到参数 {}", passLabel);
                        text.append(String.format("数据流结果 - 传播到参数 %s", passLabel));
                        text.append("\n");
                    } catch (Exception e) {
                        logger.error("污点分析 - 链中 - 错误: {}", e.toString());
                    }
                    if (pass.get().isFail()) {
                        break;
                    }
                }
            }

            if (thisChainSuccess) {
                TaintResult r = new TaintResult();
                r.setDfsResult(result);
                r.setSuccess(true);
                r.setTaintText(text.toString());
                taintResult.add(r);
            } else {
                // 2025/10/13
                // 污点分析失败的也应该加入
                TaintResult r = new TaintResult();
                r.setDfsResult(result);
                r.setSuccess(false);
                r.setTaintText(text.toString());
                taintResult.add(r);
            }
        }

        return taintResult;
    }

}
