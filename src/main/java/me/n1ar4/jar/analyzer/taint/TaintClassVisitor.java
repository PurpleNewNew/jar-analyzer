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
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.JSRInlinerAdapter;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class TaintClassVisitor extends ClassVisitor {
    private static final Logger logger = LogManager.getLogger();

    private String className;
    private final int paramsNum;
    private final MethodReference.Handle cur;
    private final MethodReference.Handle next;
    private final AtomicReference<TaintPass> pass;
    private boolean iface;
    private final SanitizerRule rule;
    private final TaintModelRule summaryRule;
    private final TaintModelRule additionalRule;
    private final List<TaintGuardRule> guardRules;
    private final TaintAnalysisProfile profile;
    private final TaintPropagationMode propagationMode;
    private final StringBuilder text;
    private final boolean allowWeakDescMatch;
    private final boolean fieldAsSource;
    private final boolean returnAsSource;
    private final AtomicBoolean lowConfidence;
    private final int expectedParamCount;
    private final String sinkKind;

    public TaintClassVisitor(int i,
                             MethodReference.Handle cur, MethodReference.Handle next,
                             AtomicReference<TaintPass> pass, SanitizerRule rule,
                             TaintModelRule summaryRule, TaintModelRule additionalRule,
                             List<TaintGuardRule> guardRules,
                             TaintAnalysisProfile profile, TaintPropagationMode propagationMode,
                             StringBuilder text,
                             boolean allowWeakDescMatch, boolean fieldAsSource,
                             boolean returnAsSource, AtomicBoolean lowConfidence,
                             String sinkKind) {
        super(Const.ASMVersion);
        this.paramsNum = i;
        this.cur = cur;
        this.next = next;
        this.pass = pass;
        this.rule = rule;
        this.summaryRule = summaryRule;
        this.additionalRule = additionalRule;
        this.guardRules = guardRules;
        this.profile = profile;
        this.propagationMode = propagationMode;
        this.text = text;
        this.allowWeakDescMatch = allowWeakDescMatch;
        this.fieldAsSource = fieldAsSource;
        this.returnAsSource = returnAsSource;
        this.lowConfidence = lowConfidence;
        this.expectedParamCount = Type.getArgumentTypes(cur.getDesc()).length;
        this.sinkKind = sinkKind;
    }

    @Override
    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
        this.iface = (access & Opcodes.ACC_INTERFACE) != 0;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        boolean exactMatch = name.equals(this.cur.getName()) && desc.equals(this.cur.getDesc());
        boolean weakMatch = false;
        if (!exactMatch && this.allowWeakDescMatch && name.equals(this.cur.getName())) {
            int paramCount = Type.getArgumentTypes(desc).length;
            weakMatch = (paramCount == this.expectedParamCount);
        }
        if (this.iface) {
            if (exactMatch || weakMatch) {
                if (weakMatch) {
                    markLowConfidence("desc弱匹配");
                }
                if (paramsNum != Sanitizer.NO_PARAM) {
                    pass.set(TaintPass.fromParamIndex(paramsNum));
                    logger.info("污点分析进行中 {} - {} - {}", cur.getClassReference().getName(), cur.getName(), cur.getDesc());
                    text.append(String.format("污点分析进行中 %s - %s - %s",
                            cur.getClassReference().getName(), cur.getName(), cur.getDesc()));
                    text.append("\n");
                    String paramLabel = formatParamLabel(paramsNum);
                    logger.info("接口方法污点直传 - 参数: {}", paramLabel);
                    text.append(String.format("接口方法污点直传 - 参数: %s", paramLabel));
                    text.append("\n");
                    markLowConfidence("接口直传");
                } else {
                    logger.info("接口方法跳过污点传播：无参数源");
                    text.append("接口方法跳过污点传播：无参数源\n");
                }
            }
            return mv;
        }
        if (exactMatch || weakMatch) {
            if (weakMatch) {
                markLowConfidence("desc弱匹配");
            }
            TaintMethodAdapter tma = new TaintMethodAdapter(
                    api, mv, this.className, access, name, desc, this.paramsNum,
                    next, pass, rule, summaryRule, additionalRule,
                    guardRules, profile, propagationMode,
                    text, this.allowWeakDescMatch,
                    this.fieldAsSource, this.returnAsSource, this.lowConfidence,
                    this.sinkKind);
            return new JSRInlinerAdapter(tma, access, name, desc, signature, exceptions);
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
    }

    public AtomicReference<TaintPass> getPass() {
        return this.pass;
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

    private void markLowConfidence(String reason) {
        if (lowConfidence != null) {
            lowConfidence.set(true);
        }
        String msg = "低置信: " + reason;
        logger.info(msg);
        text.append(msg).append("\n");
    }
}
