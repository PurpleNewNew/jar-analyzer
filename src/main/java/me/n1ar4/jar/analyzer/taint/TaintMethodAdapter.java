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

import me.n1ar4.jar.analyzer.core.AnalyzeEnv;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.taint.jvm.JVMRuntimeAdapter;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import me.n1ar4.jar.analyzer.taint.GetterSetterResolver;
import me.n1ar4.jar.analyzer.taint.GetterSetterSummary;
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
    private static final String CONST_PREFIX = "CONST:";
    private static final String CLASS_PREFIX = "CLASS:";
    private static final String FIELD_PREFIX = "REFLECT_FIELD:";
    private static final String CONTAINER_ELEMENT = "TAINT:CONTAINER_ELEMENT";
    private static final String CONTAINER_KEY = "TAINT:CONTAINER_KEY";
    private static final String CONTAINER_VALUE = "TAINT:CONTAINER_VALUE";
    private static final String GUARD_PATH_NORMALIZED = "TAINT:PATH_NORMALIZED";
    private static final String GUARD_PATH_ALLOWED = "TAINT:PATH_ALLOWED";
    private static final String GUARD_PATH_TRAVERSAL_SAFE = "TAINT:PATH_TRAVERSAL_SAFE";
    private static final String GUARD_HOST_VALUE = "TAINT:HOST_VALUE";
    private static final String GUARD_HOST_ALLOWED = "TAINT:HOST_ALLOWED";
    private static final String CLASS_OWNER = "java/lang/Class";
    private static final String CLASS_LOADER_OWNER = "java/lang/ClassLoader";   
    private static final String FIELD_OWNER = "java/lang/reflect/Field";        
    private static final Map<String, Set<String>> CONTAINER_ALIASES = new HashMap<>();

    private final String owner;
    private final int access;
    private final String name;
    private final String desc;
    private final int paramsNum;

    private final MethodReference.Handle next;
    private final AtomicReference<TaintPass> pass;
    private final SanitizerRule rule;
    private final TaintModelRule modelRule;
    private final StringBuilder text;
    private final boolean allowWeakDescMatch;
    private final boolean fieldAsSource;
    private final boolean returnAsSource;
    private final AtomicBoolean lowConfidence;
    private final int nextParamCount;
    private final String nextClass;
    private final Map<String, Set<String>> fieldTaint = new HashMap<>();
    private final String sinkKind;
    private final List<TaintGuardRule> guardRules;

    static {
        addContainerAlias("java/util/List", "java/util/Collection");
        addContainerAlias("java/util/Set", "java/util/Collection");
        addContainerAlias("java/util/Queue", "java/util/Collection");
        addContainerAlias("java/util/Deque", "java/util/Queue");
        addContainerAlias("java/util/ArrayList",
                "java/util/List", "java/util/Collection", "java/lang/Iterable");
        addContainerAlias("java/util/LinkedList",
                "java/util/List", "java/util/Deque", "java/util/Queue",
                "java/util/Collection", "java/lang/Iterable");
        addContainerAlias("java/util/Vector",
                "java/util/List", "java/util/Collection", "java/lang/Iterable");
        addContainerAlias("java/util/Stack",
                "java/util/List", "java/util/Collection", "java/lang/Iterable");
        addContainerAlias("java/util/HashSet",
                "java/util/Set", "java/util/Collection", "java/lang/Iterable");
        addContainerAlias("java/util/LinkedHashSet",
                "java/util/Set", "java/util/Collection", "java/lang/Iterable");
        addContainerAlias("java/util/TreeSet",
                "java/util/Set", "java/util/Collection", "java/lang/Iterable");
        addContainerAlias("java/util/concurrent/CopyOnWriteArrayList",
                "java/util/List", "java/util/Collection", "java/lang/Iterable");
        addContainerAlias("java/util/concurrent/CopyOnWriteArraySet",
                "java/util/Set", "java/util/Collection", "java/lang/Iterable");
        addContainerAlias("java/util/HashMap", "java/util/Map");
        addContainerAlias("java/util/LinkedHashMap", "java/util/Map");
        addContainerAlias("java/util/Hashtable", "java/util/Map");
        addContainerAlias("java/util/TreeMap", "java/util/Map");
        addContainerAlias("java/util/Properties", "java/util/Map");
        addContainerAlias("java/util/concurrent/ConcurrentHashMap", "java/util/Map");
        addContainerAlias("java/util/concurrent/ConcurrentSkipListMap", "java/util/Map");
        addContainerAlias("java/util/ArrayDeque",
                "java/util/Deque", "java/util/Queue", "java/util/Collection", "java/lang/Iterable");
        addContainerAlias("java/util/PriorityQueue",
                "java/util/Queue", "java/util/Collection", "java/lang/Iterable");
        addContainerAlias("java/util/concurrent/ConcurrentLinkedQueue",
                "java/util/Queue", "java/util/Collection", "java/lang/Iterable");
        addContainerAlias("java/util/concurrent/ConcurrentLinkedDeque",
                "java/util/Deque", "java/util/Queue", "java/util/Collection", "java/lang/Iterable");
        addContainerAlias("java/util/concurrent/LinkedBlockingQueue",
                "java/util/Queue", "java/util/Collection", "java/lang/Iterable");
        addContainerAlias("java/util/concurrent/ArrayBlockingQueue",
                "java/util/Queue", "java/util/Collection", "java/lang/Iterable");
        addContainerAlias("java/util/concurrent/PriorityBlockingQueue",
                "java/util/Queue", "java/util/Collection", "java/lang/Iterable");
        addContainerAlias("java/util/concurrent/DelayQueue",
                "java/util/Queue", "java/util/Collection", "java/lang/Iterable");
        addContainerAlias("java/util/concurrent/SynchronousQueue",
                "java/util/Queue", "java/util/Collection", "java/lang/Iterable");
        addContainerAlias("java/util/concurrent/LinkedTransferQueue",
                "java/util/Queue", "java/util/Collection", "java/lang/Iterable");
        addContainerAlias("java/util/concurrent/LinkedBlockingDeque",
                "java/util/Deque", "java/util/Queue", "java/util/Collection", "java/lang/Iterable");
    }

    public TaintMethodAdapter(final int api, final MethodVisitor mv, final String owner,
                              int access, String name, String desc, int paramsNum,
                              MethodReference.Handle next, AtomicReference<TaintPass> pass,
                              SanitizerRule rule, TaintModelRule modelRule,
                              StringBuilder text,
                              boolean allowWeakDescMatch, boolean fieldAsSource,
                              boolean returnAsSource, AtomicBoolean lowConfidence,
                              String sinkKind) {
        super(api, mv, owner, access, name, desc);
        this.owner = owner;
        this.access = access;
        this.name = name;
        this.desc = desc;
        this.paramsNum = paramsNum;
        this.next = next;
        this.pass = pass;
        this.rule = rule;
        this.modelRule = modelRule;
        this.text = text;
        this.allowWeakDescMatch = allowWeakDescMatch;
        this.fieldAsSource = fieldAsSource;
        this.returnAsSource = returnAsSource;
        this.lowConfidence = lowConfidence;
        this.sinkKind = sinkKind;
        this.guardRules = me.n1ar4.jar.analyzer.rules.ModelRegistry.getGuardRules();
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
        int argSlots = 0;
        for (Type type : argumentTypes) {
            argSlots += type.getSize();
        }

        // 如果是非静态方法 还需要考虑 this 引用
        if (opcode != Opcodes.INVOKESTATIC) {
            argCount++; // 包含 this 引用
            argSlots += 1;
        }

        boolean taintReturnFromReflectGet = false;
        String postInvokeMarker = null;
        if (stack.size() >= argSlots) {
            postInvokeMarker = resolveReflectionReturnMarker(owner, name, desc, stack, argSlots);
            if (isReflectFieldGetter(owner, name, desc)) {
                String fieldKey = resolveFieldKeyFromReceiver(stack, argSlots);
                if (fieldKey != null) {
                    Set<String> mark = fieldTaint.get(fieldKey);
                    taintReturnFromReflectGet = mark != null && !mark.isEmpty();
                }
            } else if (isReflectFieldSetter(owner, name, desc)) {
                String fieldKey = resolveFieldKeyFromReceiver(stack, argSlots);
                if (fieldKey != null) {
                    int valueSlots = argumentTypes.length > 0
                            ? argumentTypes[argumentTypes.length - 1].getSize()
                            : 1;
                    boolean valueTaint = false;
                    for (int i = 0; i < valueSlots; i++) {
                        int idx = stack.size() - 1 - i;
                        if (idx < 0 || idx >= stack.size()) {
                            break;
                        }
                        Set<String> valueSet = stack.get(idx);
                        if (valueSet != null && valueSet.contains(TaintAnalyzer.TAINT)) {
                            valueTaint = true;
                            break;
                        }
                    }
                    if (valueTaint) {
                        Set<String> t = new HashSet<>();
                        t.add(TaintAnalyzer.TAINT);
                        fieldTaint.put(fieldKey, t);
                    }
                }
            }
        }

        // 识别简单 getter/setter 以补充字段污点摘要
        GetterSetterSummary summary = GetterSetterResolver.resolve(owner, name, desc);
        boolean taintReturnFromGetter = false;
        if (summary != null) {
            String key = fieldKey(summary.getFieldOwner(),
                    summary.getFieldName(), summary.getFieldDesc());
            if (summary.isSetter()) {
                if (stack.size() >= argCount) {
                    int targetStackIndex;
                    if (opcode == Opcodes.INVOKESTATIC) {
                        targetStackIndex = stack.size() - argCount + summary.getParamIndex();
                    } else {
                        targetStackIndex = stack.size() - argCount + 1 + summary.getParamIndex();
                    }
                    if (targetStackIndex >= 0 && targetStackIndex < stack.size()) {
                        Set<String> targetParam = stack.get(targetStackIndex);
                        if (targetParam.contains(TaintAnalyzer.TAINT)) {
                            Set<String> t = new HashSet<>();
                            t.add(TaintAnalyzer.TAINT);
                            fieldTaint.put(key, t);
                        }
                    }
                }
            } else if (summary.isGetter()) {
                Set<String> mark = fieldTaint.get(key);
                taintReturnFromGetter = mark != null && !mark.isEmpty();
            }
        }

        boolean isStaticCall = opcode == Opcodes.INVOKESTATIC;
        String guardReturnMarker = resolveGuardReturnMarker(owner, name, desc, stack, argCount, isStaticCall);
        applyGuardSanitizers(owner, name, desc, stack, argCount, isStaticCall);

        ModelResult modelResult = new ModelResult();
        // 找到下个方法
        if (nextMatch) {
            modelResult = applyModelRules(owner, name, desc, stack, argCount,
                    isStaticCall);
            // 检查 stack 是否有足够的元素
            if (stack.size() >= argCount) {
                // 从栈顶开始检查参数（栈顶是最后一个参数）
                for (int i = 0; i < argCount; i++) {
                    int stackIndex = stack.size() - 1 - i; // 从栈顶往下
                    Set<String> item = stack.get(stackIndex);
                    if (containsHardTaint(item)) {
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
                        int argIndex = argumentTypes.length - 1 - i;
                        if (argIndex >= 0 && argIndex < argumentTypes.length) {
                            Type argType = argumentTypes[argIndex];
                            if (isLowRiskType(argType)) {
                                markLowConfidence("低风险类型参数: " + formatTypeName(argType));
                            }
                        }
                        String paramLabel = formatParamLabel(paramIndex);
                        logger.info("发现方法调用类型污点 - 方法调用传播 - 参数: {}", paramLabel);
                        text.append(String.format("发现方法调用类型污点 - 方法调用传播 - 参数: %s", paramLabel));
                        text.append("\n");
                    }
                }
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            if (taintReturnFromGetter || taintReturnFromReflectGet) {
                final Type returnType = Type.getReturnType(desc);
                if (returnType.getSort() != Type.VOID) {
                    applyTaintToTop(returnType.getSize());
                }
            }
            applyReturnMarkers(modelResult, desc);
            applyMarkerToTop(guardReturnMarker, desc);
            applyMarkerToTop(postInvokeMarker, desc);
            return;
        }

        // 检查 sanitizer 规则
        if (this.rule == null || this.rule.getRules() == null) {
            logger.warn("sanitizer rules not loaded, skipping sanitizer check");
            modelResult = applyModelRules(owner, name, desc, stack, argCount,
                    opcode == Opcodes.INVOKESTATIC);
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            if (taintReturnFromGetter || taintReturnFromReflectGet) {
                final Type returnType = Type.getReturnType(desc);
                if (returnType.getSort() != Type.VOID) {
                    applyTaintToTop(returnType.getSize());
                }
            }
            applyReturnMarkers(modelResult, desc);
            applyMarkerToTop(guardReturnMarker, desc);
            applyMarkerToTop(postInvokeMarker, desc);
            return;
        }

        List<Sanitizer> rules = this.rule.getRules();
        boolean match = false;

        // 检查每个 sanitizer 规则
        for (Sanitizer rule : rules) {
            if (!sanitizerKindMatches(rule)) {
                continue;
            }
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
            applyMarkerToTop(postInvokeMarker, desc);
            pass.set(TaintPass.fail());
            return;
        }

        modelResult = applyModelRules(owner, name, desc, stack, argCount,
                opcode == Opcodes.INVOKESTATIC);
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
            if (taintReturnFromGetter || taintReturnFromReflectGet) {
                if (returnType.getSort() != Type.VOID) {
                    applyTaintToTop(returnType.getSize());
                }
            }
            applyReturnMarkers(modelResult, desc);
            applyMarkerToTop(guardReturnMarker, desc);
            applyMarkerToTop(postInvokeMarker, desc);
            return;
        }

        super.visitMethodInsn(opcode, owner, name, desc, itf);
        if (taintReturnFromGetter || taintReturnFromReflectGet) {
            final Type returnType = Type.getReturnType(desc);
            if (returnType.getSort() != Type.VOID) {
                applyTaintToTop(returnType.getSize());
            }
        }
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
        applyReturnMarkers(modelResult, desc);
        applyMarkerToTop(guardReturnMarker, desc);
        applyMarkerToTop(postInvokeMarker, desc);
    }

    @Override
    public void visitLdcInsn(Object cst) {
        super.visitLdcInsn(cst);
        if (cst instanceof String) {
            addMarkerToTop(CONST_PREFIX + cst);
            return;
        }
        if (cst instanceof Type) {
            Type type = (Type) cst;
            if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
                addMarkerToTop(CLASS_PREFIX + type.getInternalName());
            }
        }
    }

    private static void addContainerAlias(String owner, String... parents) {
        Set<String> set = CONTAINER_ALIASES.computeIfAbsent(owner, k -> new HashSet<>());
        for (String parent : parents) {
            set.add(parent);
        }
    }

    private boolean containsHardTaint(Set<String> item) {
        return item != null && item.contains(TaintAnalyzer.TAINT);
    }

    private ModelResult applyModelRules(String owner, String name, String desc,
                                        List<Set<String>> stack, int argCount,
                                        boolean isStatic) {
        ModelResult result = new ModelResult();
        if (modelRule == null) {
            return result;
        }
        Set<String> candidates = resolveCandidateClasses(owner);
        for (String candidate : candidates) {
            List<TaintModel> rules = modelRule.getRules(candidate, name, desc);
            if (rules == null || rules.isEmpty()) {
                continue;
            }
            for (TaintModel rule : rules) {
                if (rule == null) {
                    continue;
                }
                if (!shouldApplyRule(rule, owner, candidate)) {
                    continue;
                }
                List<TaintFlow> flows = rule.getFlows();
                if (flows == null || flows.isEmpty()) {
                    continue;
                }
                for (TaintFlow flow : flows) {
                    if (flow == null) {
                        continue;
                    }
                    TaintPath fromPath = parsePath(flow.getFrom());
                    TaintPath toPath = parsePath(flow.getTo());
                    if (fromPath == null || toPath == null) {
                        continue;
                    }
                    if (!isPathTainted(fromPath, stack, argCount, isStatic)) {
                        continue;
                    }
                    applyPath(toPath, stack, argCount, isStatic, result);
                }
            }
        }
        return result;
    }

    private boolean shouldApplyRule(TaintModel rule, String owner, String candidate) {
        if (rule == null || owner == null || candidate == null) {
            return false;
        }
        if (owner.equals(candidate)) {
            return true;
        }
        Boolean subtypes = rule.getSubtypes();
        return subtypes != null && subtypes;
    }

    private Set<String> resolveCandidateClasses(String owner) {
        Set<String> candidates = new HashSet<>();
        if (owner == null || owner.isEmpty()) {
            return candidates;
        }
        candidates.add(owner);
        Set<String> aliases = CONTAINER_ALIASES.get(owner);
        if (aliases != null && !aliases.isEmpty()) {
            candidates.addAll(aliases);
        }
        if (AnalyzeEnv.inheritanceMap != null) {
            Set<ClassReference.Handle> parents = AnalyzeEnv.inheritanceMap.getSuperClasses(new ClassReference.Handle(owner));
            if (parents != null) {
                for (ClassReference.Handle parent : parents) {
                    if (parent != null && parent.getName() != null) {
                        candidates.add(parent.getName());
                    }
                }
            }
        }
        return candidates;
    }

    private void applyReturnMarkers(ModelResult modelResult, String methodDesc) {
        if (modelResult == null || modelResult.returnMarkers.isEmpty()) {
            return;
        }
        Type returnType = Type.getReturnType(methodDesc);
        if (returnType.getSort() == Type.VOID) {
            return;
        }
        for (String marker : modelResult.returnMarkers) {
            addMarkerToTop(marker);
        }
    }

    private void applyPath(TaintPath path, List<Set<String>> stack,
                           int argCount, boolean isStatic, ModelResult result) {
        if (path == null) {
            return;
        }
        String marker = markerForSlot(path.slot);
        if (marker == null) {
            return;
        }
        if (path.kind == PathKind.RETURN) {
            result.returnMarkers.add(marker);
            return;
        }
        List<Set<String>> targets = resolveStackSets(path, stack, argCount, isStatic);
        if (targets == null || targets.isEmpty()) {
            return;
        }
        for (Set<String> target : targets) {
            if (target != null) {
                target.add(marker);
            }
        }
    }

    private boolean isPathTainted(TaintPath path, List<Set<String>> stack,
                                  int argCount, boolean isStatic) {
        if (path == null || path.kind == PathKind.RETURN) {
            return false;
        }
        List<Set<String>> targets = resolveStackSets(path, stack, argCount, isStatic);
        if (targets == null || targets.isEmpty()) {
            return false;
        }
        for (Set<String> target : targets) {
            if (target == null) {
                continue;
            }
            switch (path.slot) {
                case SELF:
                    if (target.contains(TaintAnalyzer.TAINT)) {
                        return true;
                    }
                    break;
                case ELEMENT:
                    if (target.contains(CONTAINER_ELEMENT)) {
                        return true;
                    }
                    break;
                case KEY:
                    if (target.contains(CONTAINER_KEY)) {
                        return true;
                    }
                    break;
                case VALUE:
                    if (target.contains(CONTAINER_VALUE)) {
                        return true;
                    }
                    break;
                default:
                    break;
            }
        }
        return false;
    }

    private List<Set<String>> resolveStackSets(TaintPath path, List<Set<String>> stack,
                                               int argCount, boolean isStatic) {
        List<Set<String>> targets = new java.util.ArrayList<>();
        if (path == null || path.kind == PathKind.RETURN) {
            return targets;
        }
        int base = stack.size() - argCount;
        if (base < 0) {
            return targets;
        }
        if (path.kind == PathKind.THIS) {
            if (isStatic) {
                return targets;
            }
            int idx = base;
            if (idx >= 0 && idx < stack.size()) {
                targets.add(stack.get(idx));
            }
            return targets;
        }
        if (path.kind == PathKind.ARG) {
            int start = path.indexStart;
            int end = path.indexEnd;
            if (start < 0) {
                return targets;
            }
            if (end < start) {
                end = start;
            }
            for (int i = start; i <= end; i++) {
                int idx = isStatic ? base + i : base + 1 + i;
                if (idx < 0 || idx >= stack.size()) {
                    continue;
                }
                targets.add(stack.get(idx));
            }
        }
        return targets;
    }

    private TaintPath parsePath(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            return null;
        }
        String base = text;
        String suffix = null;
        int dot = text.indexOf('.');
        if (dot >= 0) {
            base = text.substring(0, dot);
            suffix = text.substring(dot + 1);
        }
        PathKind kind = parseKind(base);
        if (kind == null) {
            return null;
        }
        int start = -1;
        int end = -1;
        if (kind == PathKind.ARG) {
            int[] range = parseArgRange(base);
            if (range == null || range.length != 2) {
                return null;
            }
            start = range[0];
            end = range[1];
        }
        PathSlot slot = parseSlot(suffix);
        if (slot == null) {
            return null;
        }
        TaintPath path = new TaintPath();
        path.kind = kind;
        path.indexStart = start;
        path.indexEnd = end;
        path.slot = slot;
        return path;
    }

    private PathKind parseKind(String base) {
        if (base == null) {
            return null;
        }
        String lower = base.trim().toLowerCase();
        if ("this".equals(lower) || "receiver".equals(lower)) {
            return PathKind.THIS;
        }
        if ("return".equals(lower) || "returnvalue".equals(lower) || "ret".equals(lower)) {
            return PathKind.RETURN;
        }
        if (lower.startsWith("arg")) {
            return PathKind.ARG;
        }
        if (lower.startsWith("argument[")) {
            return PathKind.ARG;
        }
        return null;
    }

    private int[] parseArgRange(String base) {
        String lower = base.trim().toLowerCase();
        if (lower.startsWith("arg")) {
            String num = lower.substring(3);
            return parseRange(num);
        }
        if (lower.startsWith("argument[") && lower.endsWith("]")) {
            String num = lower.substring("argument[".length(), lower.length() - 1);
            return parseRange(num);
        }
        return null;
    }

    private int[] parseRange(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        int dots = text.indexOf("..");
        if (dots >= 0) {
            String left = text.substring(0, dots);
            String right = text.substring(dots + 2);
            Integer start = parseNumber(left);
            Integer end = parseNumber(right);
            if (start == null || end == null) {
                return null;
            }
            if (end < start) {
                return null;
            }
            return new int[]{start, end};
        }
        Integer single = parseNumber(text);
        if (single == null) {
            return null;
        }
        return new int[]{single, single};
    }

    private Integer parseNumber(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                return null;
            }
        }
        try {
            return Integer.parseInt(text);
        } catch (Exception e) {
            return null;
        }
    }

    private PathSlot parseSlot(String suffix) {
        if (suffix == null || suffix.trim().isEmpty()) {
            return PathSlot.SELF;
        }
        String lower = suffix.trim().toLowerCase();
        if ("element".equals(lower) || "elem".equals(lower) || "arrayelement".equals(lower)
                || "array".equals(lower) || "arrayelem".equals(lower)) {
            return PathSlot.ELEMENT;
        }
        if ("key".equals(lower) || "mapkey".equals(lower)) {
            return PathSlot.KEY;
        }
        if ("value".equals(lower) || "mapvalue".equals(lower)) {
            return PathSlot.VALUE;
        }
        return null;
    }

    private String markerForSlot(PathSlot slot) {
        if (slot == null) {
            return null;
        }
        switch (slot) {
            case SELF:
                return TaintAnalyzer.TAINT;
            case ELEMENT:
                return CONTAINER_ELEMENT;
            case KEY:
                return CONTAINER_KEY;
            case VALUE:
                return CONTAINER_VALUE;
            default:
                return null;
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

    private enum PathKind {
        THIS,
        ARG,
        RETURN
    }

    private enum PathSlot {
        SELF,
        ELEMENT,
        KEY,
        VALUE
    }

    private static final class TaintPath {
        private PathKind kind;
        private int indexStart = -1;
        private int indexEnd = -1;
        private PathSlot slot = PathSlot.SELF;
    }

    private static final class ModelResult {
        private final Set<String> returnMarkers = new HashSet<>();
    }

    private void taintAllParams() {
        for (int i = 0; i < localVariables.size(); i++) {
            localVariables.set(i, TaintAnalyzer.TAINT);
        }
    }

    private void applyMarkerToTop(String marker, String methodDesc) {
        if (marker == null) {
            return;
        }
        Type returnType = Type.getReturnType(methodDesc);
        if (returnType.getSort() == Type.VOID) {
            return;
        }
        addMarkerToTop(marker);
    }

    private void addMarkerToTop(String marker) {
        if (marker == null) {
            return;
        }
        List<Set<String>> stack = this.operandStack.getList();
        if (stack.isEmpty()) {
            return;
        }
        stack.get(stack.size() - 1).add(marker);
    }

    private boolean sanitizerKindMatches(Sanitizer rule) {
        if (rule == null) {
            return true;
        }
        return kindMatches(rule.getKind());
    }

    private boolean guardKindMatches(TaintGuardRule rule) {
        if (rule == null) {
            return true;
        }
        return kindMatches(rule.getKind());
    }

    private boolean kindMatches(String ruleKind) {
        String rk = normalizeKind(ruleKind);
        if (rk == null || rk.isEmpty() || "any".equals(rk) || "all".equals(rk)) {
            return true;
        }
        String sk = normalizeKind(this.sinkKind);
        if (sk == null || sk.isEmpty()) {
            return true;
        }
        String[] parts = rk.split("[,|/\\s]+");
        for (String part : parts) {
            if (part == null || part.isEmpty()) {
                continue;
            }
            if (part.equals(sk)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeKind(String kind) {
        if (kind == null) {
            return null;
        }
        String k = kind.trim().toLowerCase();
        return k.isEmpty() ? null : k;
    }

    private String resolveGuardReturnMarker(String owner, String name, String desc,
                                            List<Set<String>> stack, int argCount,
                                            boolean isStatic) {
        if (guardRules == null || guardRules.isEmpty()) {
            return null;
        }
        boolean receiverTainted = hasReceiverTaint(stack, argCount, isStatic);
        boolean argsTainted = hasAnyTaint(stack, argCount);
        for (TaintGuardRule rule : guardRules) {
            if (!isGuardEnabled(rule) || !guardKindMatches(rule)) {
                continue;
            }
            if (!guardRuleMatches(rule, owner, name, desc)) {
                continue;
            }
            String type = normalizeGuardType(rule.getType());
            if ("path-normalize".equals(type)) {
                if (argsTainted || receiverTainted) {
                    return GUARD_PATH_NORMALIZED;
                }
            } else if ("host-extract".equals(type)) {
                if (argsTainted || receiverTainted) {
                    return GUARD_HOST_VALUE;
                }
            }
        }
        return null;
    }

    private void applyGuardSanitizers(String owner, String name, String desc,
                                      List<Set<String>> stack, int argCount,
                                      boolean isStatic) {
        if (guardRules == null || guardRules.isEmpty()) {
            return;
        }
        for (TaintGuardRule rule : guardRules) {
            if (!isGuardEnabled(rule) || !guardKindMatches(rule)) {
                continue;
            }
            if (!guardRuleMatches(rule, owner, name, desc)) {
                continue;
            }
            String type = normalizeGuardType(rule.getType());
            if ("path-prefix".equals(type)) {
                applyPathPrefixGuard(rule, stack, argCount, isStatic);
            } else if ("path-traversal".equals(type)) {
                applyPathTraversalGuard(rule, stack, argCount, isStatic);
            } else if ("host-allowlist".equals(type)) {
                applyHostAllowlistGuard(rule, stack, argCount, isStatic);
            }
        }
    }

    private void applyPathPrefixGuard(TaintGuardRule rule, List<Set<String>> stack,
                                      int argCount, boolean isStatic) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        Set<String> receiver = resolveReceiverSet(stack, argCount, isStatic);
        if (receiver == null) {
            return;
        }
        if (Boolean.TRUE.equals(rule.getRequireNormalized())
                && !(receiver.contains(GUARD_PATH_NORMALIZED) || receiver.contains(GUARD_PATH_TRAVERSAL_SAFE))) {
            return;
        }
        String argConst = resolveGuardConstArg(rule, stack, argCount, isStatic);
        if (!allowlistMatch(argConst, rule.getAllowlist())) {
            return;
        }
        applyGuardEffect(rule, receiver, GUARD_PATH_ALLOWED, "路径前缀允许");
    }

    private void applyPathTraversalGuard(TaintGuardRule rule, List<Set<String>> stack,
                                         int argCount, boolean isStatic) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        Set<String> receiver = resolveReceiverSet(stack, argCount, isStatic);
        if (receiver == null) {
            return;
        }
        String argConst = resolveGuardConstArg(rule, stack, argCount, isStatic);
        if (argConst == null || argConst.isEmpty()) {
            return;
        }
        List<String> blocklist = rule.getAllowlist();
        if (blocklist != null && !blocklist.isEmpty()) {
            if (!allowlistMatch(argConst, blocklist)) {
                return;
            }
        } else if (!"..".equals(argConst)) {
            return;
        }
        applyGuardEffect(rule, receiver, GUARD_PATH_TRAVERSAL_SAFE, "路径穿越检查");
    }

    private void applyHostAllowlistGuard(TaintGuardRule rule, List<Set<String>> stack,
                                         int argCount, boolean isStatic) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        Set<String> receiver = resolveReceiverSet(stack, argCount, isStatic);
        if (receiver == null || !receiver.contains(GUARD_HOST_VALUE)) {
            return;
        }
        String argConst = resolveGuardConstArg(rule, stack, argCount, isStatic);
        if (!allowlistMatch(argConst, rule.getAllowlist())) {
            return;
        }
        applyGuardEffect(rule, receiver, GUARD_HOST_ALLOWED, "Host allowlist 命中");
    }

    private void applyGuardEffect(TaintGuardRule rule, Set<String> target,
                                  String marker, String reason) {
        if (target == null) {
            return;
        }
        target.add(marker);
        if (isGuardHardMode(rule)) {
            target.remove(TaintAnalyzer.TAINT);
            if (reason != null) {
                logger.info("guard sanitizer applied: {}", reason);
                text.append("guard sanitizer: ").append(reason).append("\n");
            }
        } else if (reason != null) {
            markLowConfidence("guard " + reason);
        }
    }

    private boolean isGuardHardMode(TaintGuardRule rule) {
        if (rule == null || rule.getMode() == null) {
            return false;
        }
        return "hard".equals(rule.getMode().trim().toLowerCase());
    }

    private String resolveGuardConstArg(TaintGuardRule rule, List<Set<String>> stack,
                                        int argCount, boolean isStatic) {
        Integer paramIndex = rule.getParamIndex();
        if (paramIndex == null || paramIndex < 0) {
            return null;
        }
        int idx = resolveParamStackIndex(stack, argCount, isStatic, paramIndex);
        if (idx < 0 || idx >= stack.size()) {
            return null;
        }
        return resolveConstString(stack.get(idx));
    }

    private Set<String> resolveReceiverSet(List<Set<String>> stack, int argCount, boolean isStatic) {
        if (isStatic) {
            return null;
        }
        int base = stack.size() - argCount;
        if (base < 0 || base >= stack.size()) {
            return null;
        }
        return stack.get(base);
    }

    private int resolveParamStackIndex(List<Set<String>> stack, int argCount, boolean isStatic, int paramIndex) {
        int base = stack.size() - argCount;
        if (base < 0) {
            return -1;
        }
        return isStatic ? base + paramIndex : base + 1 + paramIndex;
    }

    private boolean allowlistMatch(String value, List<String> allowlist) {
        if (value == null || allowlist == null || allowlist.isEmpty()) {
            return false;
        }
        for (String raw : allowlist) {
            if (raw == null) {
                continue;
            }
            String rule = raw.trim();
            if (rule.isEmpty()) {
                continue;
            }
            if (rule.endsWith("*")) {
                String prefix = rule.substring(0, rule.length() - 1);
                if (value.startsWith(prefix)) {
                    return true;
                }
            } else if (value.equals(rule)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyTaint(List<Set<String>> stack, int argCount) {
        if (stack == null || stack.isEmpty() || argCount <= 0) {
            return false;
        }
        int base = stack.size() - argCount;
        if (base < 0) {
            return false;
        }
        for (int i = base; i < stack.size(); i++) {
            Set<String> item = stack.get(i);
            if (item != null && item.contains(TaintAnalyzer.TAINT)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasReceiverTaint(List<Set<String>> stack, int argCount, boolean isStatic) {
        if (stack == null || stack.isEmpty() || isStatic) {
            return false;
        }
        Set<String> receiver = resolveReceiverSet(stack, argCount, false);
        return receiver != null && receiver.contains(TaintAnalyzer.TAINT);
    }

    private boolean isGuardEnabled(TaintGuardRule rule) {
        return rule != null && Boolean.TRUE.equals(rule.getEnabled());
    }

    private String normalizeGuardType(String type) {
        if (type == null) {
            return "";
        }
        return type.trim().toLowerCase();
    }

    private boolean guardRuleMatches(TaintGuardRule rule, String owner, String name, String desc) {
        if (rule == null) {
            return false;
        }
        if (rule.getClassName() == null || rule.getMethodName() == null) {
            return false;
        }
        if (!rule.getClassName().equals(owner)) {
            return false;
        }
        if (!rule.getMethodName().equals(name)) {
            return false;
        }
        String ruleDesc = rule.getMethodDesc();
        if (ruleDesc == null || ruleDesc.trim().isEmpty() || "*".equals(ruleDesc)
                || "null".equalsIgnoreCase(ruleDesc)) {
            return true;
        }
        return ruleDesc.equals(desc);
    }

    private String resolveReflectionReturnMarker(String owner, String name, String desc,
                                                 List<Set<String>> stack, int argSlots) {
        if (isClassForName(owner, name, desc) || isClassLoadClass(owner, name, desc)) {
            int argIndex = stack.size() - 1;
            String className = resolveConstString(stack.get(argIndex));
            className = normalizeClassName(className);
            return className == null ? null : CLASS_PREFIX + className;
        }
        if (isClassGetField(owner, name, desc)) {
            int base = stack.size() - argSlots;
            Set<String> receiver = base >= 0 ? stack.get(base) : null;
            Set<String> arg = base + 1 < stack.size() ? stack.get(base + 1) : null;
            String className = resolveClassName(receiver);
            String fieldName = resolveConstString(arg);
            String fieldDesc = resolveFieldDesc(className, fieldName);
            if (fieldDesc == null) {
                return null;
            }
            return FIELD_PREFIX + fieldKey(className, fieldName, fieldDesc);
        }
        return null;
    }

    private String resolveFieldKeyFromReceiver(List<Set<String>> stack, int argSlots) {
        int base = stack.size() - argSlots;
        if (base < 0 || base >= stack.size()) {
            return null;
        }
        return extractSingleMarker(stack.get(base), FIELD_PREFIX);
    }

    private boolean isClassForName(String owner, String name, String desc) {
        return CLASS_OWNER.equals(owner)
                && "forName".equals(name)
                && "(Ljava/lang/String;)Ljava/lang/Class;".equals(desc);
    }

    private boolean isClassLoadClass(String owner, String name, String desc) {
        return CLASS_LOADER_OWNER.equals(owner)
                && "loadClass".equals(name)
                && "(Ljava/lang/String;)Ljava/lang/Class;".equals(desc);
    }

    private boolean isClassGetField(String owner, String name, String desc) {
        return CLASS_OWNER.equals(owner)
                && ("getField".equals(name) || "getDeclaredField".equals(name))
                && "(Ljava/lang/String;)Ljava/lang/reflect/Field;".equals(desc);
    }

    private boolean isReflectFieldGetter(String owner, String name, String desc) {
        if (!FIELD_OWNER.equals(owner) || desc == null || !desc.startsWith("(Ljava/lang/Object;")) {
            return false;
        }
        return "get".equals(name)
                || "getBoolean".equals(name)
                || "getByte".equals(name)
                || "getChar".equals(name)
                || "getShort".equals(name)
                || "getInt".equals(name)
                || "getLong".equals(name)
                || "getFloat".equals(name)
                || "getDouble".equals(name);
    }

    private boolean isReflectFieldSetter(String owner, String name, String desc) {
        if (!FIELD_OWNER.equals(owner) || desc == null || !desc.startsWith("(Ljava/lang/Object;")) {
            return false;
        }
        return "set".equals(name)
                || "setBoolean".equals(name)
                || "setByte".equals(name)
                || "setChar".equals(name)
                || "setShort".equals(name)
                || "setInt".equals(name)
                || "setLong".equals(name)
                || "setFloat".equals(name)
                || "setDouble".equals(name);
    }

    private String resolveConstString(Set<String> set) {
        return extractSingleMarker(set, CONST_PREFIX);
    }

    private String resolveClassName(Set<String> set) {
        return extractSingleMarker(set, CLASS_PREFIX);
    }

    private String extractSingleMarker(Set<String> set, String prefix) {
        if (set == null || set.isEmpty()) {
            return null;
        }
        String found = null;
        for (String s : set) {
            if (!s.startsWith(prefix)) {
                continue;
            }
            if (found != null && !found.equals(s)) {
                return null;
            }
            found = s;
        }
        return found == null ? null : found.substring(prefix.length());
    }

    private String resolveFieldDesc(String owner, String fieldName) {
        if (owner == null || fieldName == null) {
            return null;
        }
        ClassReference clazz = AnalyzeEnv.classMap.get(new ClassReference.Handle(owner));
        if (clazz == null || clazz.getMembers() == null) {
            return null;
        }
        for (ClassReference.Member member : clazz.getMembers()) {
            if (fieldName.equals(member.getName())) {
                return member.getDesc();
            }
        }
        return null;
    }

    private String normalizeClassName(String name) {
        if (name == null) {
            return null;
        }
        String cls = name.trim();
        if (cls.startsWith("L") && cls.endsWith(";")) {
            cls = cls.substring(1, cls.length() - 1);
        }
        if (cls.endsWith(".class")) {
            cls = cls.substring(0, cls.length() - 6);
        }
        if (cls.contains(".")) {
            cls = cls.replace('.', '/');
        }
        if (cls.isEmpty()) {
            return null;
        }
        return cls;
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

    private boolean isLowRiskType(Type type) {
        if (type == null) {
            return false;
        }
        int sort = type.getSort();
        if (sort == Type.BOOLEAN || sort == Type.BYTE || sort == Type.CHAR
                || sort == Type.SHORT || sort == Type.INT || sort == Type.LONG
                || sort == Type.FLOAT || sort == Type.DOUBLE) {
            return true;
        }
        if (sort != Type.OBJECT) {
            return false;
        }
        String name = type.getInternalName();
        if (name == null) {
            return false;
        }
        if ("java/lang/Number".equals(name)
                || "java/lang/Integer".equals(name)
                || "java/lang/Long".equals(name)
                || "java/lang/Short".equals(name)
                || "java/lang/Byte".equals(name)
                || "java/lang/Boolean".equals(name)
                || "java/lang/Character".equals(name)
                || "java/lang/Float".equals(name)
                || "java/lang/Double".equals(name)
                || "java/math/BigInteger".equals(name)
                || "java/math/BigDecimal".equals(name)
                || "java/util/UUID".equals(name)
                || "java/lang/Enum".equals(name)) {
            return true;
        }
        return false;
    }

    private String formatTypeName(Type type) {
        if (type == null) {
            return "unknown";
        }
        if (type.getSort() == Type.OBJECT) {
            String name = type.getInternalName();
            return name == null ? "unknown" : name;
        }
        return type.getClassName();
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
