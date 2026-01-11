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
import me.n1ar4.jar.analyzer.taint.jvm.JVMRuntimeAdapter;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


public class TaintMethodAdapter extends JVMRuntimeAdapter<String> {
    private static final Logger logger = LogManager.getLogger();

    private final String owner;
    private final int access;
    private final String name;
    private final String desc;
    private final int paramsNum;

    private final MethodReference.Handle next;
    private final AtomicReference<TaintPass> pass;
    private final SanitizerRule rule;
    private final StringBuilder text;
    private final boolean allowWeakDescMatch;
    private final boolean fieldAsSource;
    private final boolean returnAsSource;
    private final AtomicBoolean lowConfidence;
    private final int nextParamCount;
    private final String nextClass;
    private final Map<String, Set<String>> fieldTaint = new HashMap<>();

    public TaintMethodAdapter(final int api, final MethodVisitor mv, final String owner,
                              int access, String name, String desc, int paramsNum,
                              MethodReference.Handle next, AtomicReference<TaintPass> pass,
                              SanitizerRule rule, StringBuilder text,
                              boolean allowWeakDescMatch, boolean fieldAsSource,
                              boolean returnAsSource, AtomicBoolean lowConfidence) {
        super(api, mv, owner, access, name, desc);
        this.owner = owner;
        this.access = access;
        this.name = name;
        this.desc = desc;
        this.paramsNum = paramsNum;
        this.next = next;
        this.pass = pass;
        this.rule = rule;
        this.text = text;
        this.allowWeakDescMatch = allowWeakDescMatch;
        this.fieldAsSource = fieldAsSource;
        this.returnAsSource = returnAsSource;
        this.lowConfidence = lowConfidence;
        this.nextParamCount = Type.getArgumentTypes(next.getDesc()).length;
        this.nextClass = next.getClassReference().getName().replace(".", "/");
    }

    @Override
    public void visitCode() {
        // 改造成设置污点为第n个参数
        super.visitCode();
        if (paramsNum == Sanitizer.NO_PARAM) {
            // 不设置参数污点，仅依赖字段/返回值启发式
        } else if (paramsNum == Sanitizer.ALL_PARAMS) {
            taintAllParams();
        } else if ((this.access & Opcodes.ACC_STATIC) == 0) {
            if (paramsNum == Sanitizer.THIS_PARAM) {
                localVariables.set(0, TaintAnalyzer.TAINT);
            } else {
                // 非 STATIC 第 0 是 THIS
                localVariables.set(paramsNum + 1, TaintAnalyzer.TAINT);
            }
        } else {
            if (paramsNum == Sanitizer.THIS_PARAM) {
                logger.info("静态方法跳过 this 污点源");
            } else {
                localVariables.set(paramsNum, TaintAnalyzer.TAINT);
            }
        }
        logger.info("污点分析进行中 {} - {} - {}", this.owner, this.name, this.desc);
        text.append(String.format("污点分析进行中 %s - %s - %s", this.owner, this.name, this.desc));
        text.append("\n");
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        String key = fieldKey(owner, name, desc);
        int typeSize = Type.getType(desc).getSize();
        List<Set<String>> stack = this.operandStack.getList();
        switch (opcode) {
            case Opcodes.GETSTATIC:
            case Opcodes.GETFIELD: {
                boolean taint = false;
                Set<String> mark = fieldTaint.get(key);
                if (mark != null && !mark.isEmpty()) {
                    taint = true;
                } else if (fieldAsSource) {
                    taint = true;
                    markLowConfidence("字段读取启发式");
                }
                super.visitFieldInsn(opcode, owner, name, desc);
                if (taint) {
                    applyTaintToTop(typeSize);
                }
                return;
            }
            case Opcodes.PUTSTATIC:
            case Opcodes.PUTFIELD: {
                boolean valueTaint = false;
                int need = typeSize + (opcode == Opcodes.PUTFIELD ? 1 : 0);
                if (stack.size() >= need) {
                    for (int i = 0; i < typeSize; i++) {
                        int idx = stack.size() - 1 - i;
                        Set<String> item = stack.get(idx);
                        if (item.contains(TaintAnalyzer.TAINT)) {
                            valueTaint = true;
                            break;
                        }
                    }
                }
                if (valueTaint) {
                    Set<String> t = new HashSet<>();
                    t.add(TaintAnalyzer.TAINT);
                    fieldTaint.put(key, t);
                }
                super.visitFieldInsn(opcode, owner, name, desc);
                return;
            }
            default:
                super.visitFieldInsn(opcode, owner, name, desc);
        }
    }

    @Override
    @SuppressWarnings("all")
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        // 简单的污点分析
        // 我认为所有的方法都应该传播污点
        // 除非遇到 Sanitizer 白名单 否则认为没问题
        boolean nextMatch = isNextInvocation(owner, name, desc);

        // 计算方法参数数量
        List<Set<String>> stack = this.operandStack.getList();
        Type[] argumentTypes = Type.getArgumentTypes(desc);
        int argCount = argumentTypes.length;

        // 如果是非静态方法 还需要考虑 this 引用
        if (opcode != Opcodes.INVOKESTATIC) {
            argCount++; // 包含 this 引用
        }

        // 找到下个方法
        if (nextMatch) {
            // 检查 stack 是否有足够的元素
            if (stack.size() >= argCount) {
                // 从栈顶开始检查参数（栈顶是最后一个参数）
                for (int i = 0; i < argCount; i++) {
                    int stackIndex = stack.size() - 1 - i; // 从栈顶往下
                    Set<String> item = stack.get(stackIndex);
                    if (item.contains(TaintAnalyzer.TAINT)) {
                        // 计算实际的参数位置
                        int paramIndex;
                        if (opcode == Opcodes.INVOKESTATIC) {
                            // 静态方法：参数从0开始
                            paramIndex = argCount - 1 - i;
                        } else {
                            // 非静态方法：0 是 this 参数从 1 开始
                            if (i == argCount - 1) {
                                paramIndex = Sanitizer.THIS_PARAM; // this 引用
                            } else {
                                paramIndex = argCount - 1 - i;
                                // 处理 0
                                paramIndex--;
                            }
                        }
                        // 记录数据流
                        pass.set(TaintPass.fromParamIndex(paramIndex));
                        String paramLabel = formatParamLabel(paramIndex);
                        logger.info("发现方法调用类型污点 - 方法调用传播 - 参数: {}", paramLabel);
                        text.append(String.format("发现方法调用类型污点 - 方法调用传播 - 参数: %s", paramLabel));
                        text.append("\n");
                    }
                }
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }

        // 检查 sanitizer 规则
        if (this.rule == null || this.rule.getRules() == null) {
            logger.warn("sanitizer rules not loaded, skipping sanitizer check");
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }

        List<Sanitizer> rules = this.rule.getRules();
        boolean match = false;

        // 检查每个 sanitizer 规则
        for (Sanitizer rule : rules) {
            if (owner.equals(rule.getClassName()) &&
                    name.equals(rule.getMethodName()) &&
                    desc.equals(rule.getMethodDesc())) {

                int ruleIndex = rule.getParamIndex();
                // 检查参数索引匹配
                if (ruleIndex == Sanitizer.ALL_PARAMS) {
                    // 如果规则适用于所有参数，直接匹配
                    match = true;
                    break;
                }

                if (ruleIndex < 0 && ruleIndex != Sanitizer.THIS_PARAM) {
                    continue;
                }

                // 检查特定参数索引是否被污染
                if (stack.size() >= argCount) {
                    int targetStackIndex;
                    if (opcode == Opcodes.INVOKESTATIC) {
                        if (ruleIndex == Sanitizer.THIS_PARAM) {
                            continue;
                        }
                        // 静态方法：参数从栈底开始
                        targetStackIndex = stack.size() - argCount + ruleIndex;
                    } else {
                        // 非静态方法：this 在栈底，参数索引不包含 this
                        if (ruleIndex == Sanitizer.THIS_PARAM) {
                            targetStackIndex = stack.size() - argCount;
                        } else {
                            targetStackIndex = stack.size() - argCount + 1 + ruleIndex;
                        }
                    }

                    if (targetStackIndex >= 0 && targetStackIndex < stack.size()) {
                        Set<String> targetParam = stack.get(targetStackIndex);
                        if (targetParam.contains("TAINT")) {
                            match = true;
                            String paramLabel = formatParamLabel(ruleIndex);
                            logger.info("污点命中 净化器 规则 - {} - {} - {} - 参数: {}", 
                                    owner, name, desc, paramLabel);
                            text.append(String.format("污点命中 净化器 规则 - %s - %s - %s - 参数: %s",
                                    owner, name, desc, paramLabel));
                            text.append("\n");
                            break;
                        }
                    }
                }
            }
        }

        if (match) {
            // 命中 sanitizer，停止污点传播
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            pass.set(TaintPass.fail());
            return;
        }

        boolean hasArgTaint = false;
        if (stack.size() >= argCount) {
            for (int i = 0; i < argCount; i++) {
                int stackIndex = stack.size() - 1 - i;
                Set<String> item = stack.get(stackIndex);
                if (item.contains(TaintAnalyzer.TAINT)) {
                    hasArgTaint = true;
                    break;
                }
            }
        }

        if (hasArgTaint) {
            // 只要方法输入包含了污点 且方法有返回值
            // 就认为方法的返回值可以被污点传播到
            // 执行 JVM 模拟
            final Type returnType = Type.getReturnType(desc);
            final int retSize = returnType.getSize();
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            // 如果返回值不是void 根据返回值大小设置栈顶污点
            if (returnType.getSort() != Type.VOID) {
                // 为返回值在栈顶设置污点（修改现有位置，不是add）
                for (int j = 0; j < retSize; j++) {
                    int topIndex = stack.size() - retSize + j;
                    Set<String> taintSet = new HashSet<>();
                    taintSet.add(TaintAnalyzer.TAINT);
                    stack.set(topIndex, taintSet);
                }
            }
            return;
        }

        super.visitMethodInsn(opcode, owner, name, desc, itf);
        if (returnAsSource) {
            final Type returnType = Type.getReturnType(desc);
            if (returnType.getSort() != Type.VOID) {
                markLowConfidence("返回值启发式");
                int retSize = returnType.getSize();
                for (int j = 0; j < retSize; j++) {
                    int topIndex = stack.size() - retSize + j;
                    Set<String> taintSet = new HashSet<>();
                    taintSet.add(TaintAnalyzer.TAINT);
                    stack.set(topIndex, taintSet);
                }
            }
        }
    }

    private void applyTaintToTop(int slots) {
        List<Set<String>> stack = this.operandStack.getList();
        for (int i = 0; i < slots; i++) {
            int topIndex = stack.size() - 1 - i;
            if (topIndex < 0 || topIndex >= stack.size()) {
                continue;
            }
            Set<String> taintSet = new HashSet<>();
            taintSet.add(TaintAnalyzer.TAINT);
            stack.set(topIndex, taintSet);
        }
    }

    private void taintAllParams() {
        for (int i = 0; i < localVariables.size(); i++) {
            localVariables.set(i, TaintAnalyzer.TAINT);
        }
    }

    private String fieldKey(String owner, String name, String desc) {
        return owner + "#" + name + "#" + desc;
    }

    private boolean isNextInvocation(String owner, String name, String desc) {
        if (!owner.equals(nextClass) || !name.equals(next.getName())) {
            return false;
        }
        if (desc.equals(next.getDesc())) {
            return true;
        }
        if (!allowWeakDescMatch) {
            return false;
        }
        int paramCount = Type.getArgumentTypes(desc).length;
        if (paramCount == nextParamCount) {
            markLowConfidence("next desc弱匹配");
            return true;
        }
        return false;
    }

    private void markLowConfidence(String reason) {
        if (lowConfidence != null) {
            lowConfidence.set(true);
        }
        String msg = "低置信: " + reason;
        logger.info(msg);
        text.append(msg).append("\n");
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
}
