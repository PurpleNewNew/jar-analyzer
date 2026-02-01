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
    private final TaintModelRule summaryRule;
    private final TaintModelRule additionalRule;
    private final TaintAnalysisProfile profile;
    private final TaintPropagationMode propagationMode;
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
                              SanitizerRule rule, TaintModelRule summaryRule, TaintModelRule additionalRule,
                              List<TaintGuardRule> guardRules, TaintAnalysisProfile profile,
                              TaintPropagationMode propagationMode,
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
        this.summaryRule = summaryRule;
        this.additionalRule = additionalRule;
        this.profile = profile == null ? TaintAnalysisProfile.current() : profile;
        this.propagationMode = propagationMode == null ? TaintPropagationMode.current() : propagationMode;
        this.text = text;
        this.allowWeakDescMatch = allowWeakDescMatch;
        this.fieldAsSource = fieldAsSource;
        this.returnAsSource = returnAsSource;
        this.lowConfidence = lowConfidence;
        this.sinkKind = sinkKind;
        this.guardRules = guardRules;
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
        Type fieldType = Type.getType(desc);
        int typeSize = fieldType.getSize();
        boolean enableContainer = isAdditionalEnabled(TaintAnalysisProfile.AdditionalStep.CONTAINER);
        boolean enableArray = isAdditionalEnabled(TaintAnalysisProfile.AdditionalStep.ARRAY);
        boolean enableOptional = isAdditionalEnabled(TaintAnalysisProfile.AdditionalStep.OPTIONAL);
        boolean enableStream = isAdditionalEnabled(TaintAnalysisProfile.AdditionalStep.STREAM);
        List<Set<String>> stack = this.operandStack.getList();
        switch (opcode) {
            case Opcodes.GETSTATIC:
            case Opcodes.GETFIELD: {
                Set<String> markers = new HashSet<>();
                Set<String> mark = fieldTaint.get(key);
                if (mark != null && !mark.isEmpty()) {
                    markers.addAll(filterMarkersForType(mark, fieldType, enableContainer, enableArray,
                            enableOptional, enableStream));
                }
                if (fieldAsSource) {
                    markers.add(TaintAnalyzer.TAINT);
                    markLowConfidence("字段作为污点源");
                }
                super.visitFieldInsn(opcode, owner, name, desc);
                if (!markers.isEmpty()) {
                    addMarkersToTop(typeSize, markers);
                }
                return;
            }
            case Opcodes.PUTSTATIC:
            case Opcodes.PUTFIELD: {
                int need = typeSize + (opcode == Opcodes.PUTFIELD ? 1 : 0);
                if (stack.size() >= need) {
                    Set<String> valueMarkers = collectValueMarkers(stack, typeSize);
                    if (!valueMarkers.isEmpty()) {
                        Set<String> filtered = filterMarkersForType(valueMarkers, fieldType,
                                enableContainer, enableArray, enableOptional, enableStream);
                        if (!filtered.isEmpty()) {
                            fieldTaint.put(key, filtered);
                        }
                    }
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

        boolean isStaticCall = opcode == Opcodes.INVOKESTATIC;
        boolean enableReflect = isAdditionalEnabled(TaintAnalysisProfile.AdditionalStep.REFLECT_FIELD);
        boolean enableGetterSetter = isAdditionalEnabled(TaintAnalysisProfile.AdditionalStep.GETTER_SETTER);
        boolean enableContainer = isAdditionalEnabled(TaintAnalysisProfile.AdditionalStep.CONTAINER);
        boolean enableArray = isAdditionalEnabled(TaintAnalysisProfile.AdditionalStep.ARRAY);
        boolean enableBuilder = isAdditionalEnabled(TaintAnalysisProfile.AdditionalStep.BUILDER);
        boolean enableOptional = isAdditionalEnabled(TaintAnalysisProfile.AdditionalStep.OPTIONAL);
        boolean enableStream = isAdditionalEnabled(TaintAnalysisProfile.AdditionalStep.STREAM);
        boolean enableAdditionalRules = isAdditionalEnabled(TaintAnalysisProfile.AdditionalStep.RULES);
        Set<String> containerReturnMarkers = resolveContainerReturnMarkers(
                owner, name, desc, stack, argumentTypes, argCount, isStaticCall,
                enableContainer, enableArray, enableBuilder, enableOptional, enableStream);

        Set<String> fieldReturnMarkers = null;
        String postInvokeMarker = null;
        if (enableReflect && stack.size() >= argSlots) {
            postInvokeMarker = resolveReflectionReturnMarker(owner, name, desc, stack, argSlots);
            if (isReflectFieldGetter(owner, name, desc)) {
                String fieldKey = resolveFieldKeyFromReceiver(stack, argSlots);
                if (fieldKey != null) {
                    Set<String> mark = fieldTaint.get(fieldKey);
                    if (mark != null && !mark.isEmpty()) {
                        String fieldDesc = resolveFieldDescFromKey(fieldKey);
                        Type fieldType = safeTypeFromDesc(fieldDesc);
                        Set<String> filtered = filterMarkersForType(mark, fieldType, enableContainer, enableArray,
                                enableOptional, enableStream);
                        if (!filtered.isEmpty()) {
                            if (fieldReturnMarkers == null) {
                                fieldReturnMarkers = new HashSet<>();
                            }
                            fieldReturnMarkers.addAll(filtered);
                        }
                    }
                }
            } else if (isReflectFieldSetter(owner, name, desc)) {
                String fieldKey = resolveFieldKeyFromReceiver(stack, argSlots);
                if (fieldKey != null) {
                    int valueSlots = argumentTypes.length > 0
                            ? argumentTypes[argumentTypes.length - 1].getSize()
                            : 1;
                    Set<String> valueMarkers = collectValueMarkers(stack, valueSlots);
                    if (!valueMarkers.isEmpty()) {
                        String fieldDesc = resolveFieldDescFromKey(fieldKey);
                        Type fieldType = safeTypeFromDesc(fieldDesc);
                        Set<String> filtered = filterMarkersForType(valueMarkers, fieldType,
                                enableContainer, enableArray, enableOptional, enableStream);
                        if (!filtered.isEmpty()) {
                            fieldTaint.put(fieldKey, filtered);
                        }
                    }
                }
            }
        }

        // 识别简单 getter/setter 以补充字段污点摘要
        GetterSetterSummary summary = enableGetterSetter
                ? GetterSetterResolver.resolve(owner, name, desc)
                : null;
        if (summary != null) {
            String key = fieldKey(summary.getFieldOwner(),
                    summary.getFieldName(), summary.getFieldDesc());
            Type fieldType = safeTypeFromDesc(summary.getFieldDesc());
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
                        Set<String> markers = new HashSet<>();
                        mergeTaintMarkers(markers, targetParam);
                        if (!markers.isEmpty()) {
                            Set<String> filtered = filterMarkersForType(markers, fieldType,
                                    enableContainer, enableArray, enableOptional, enableStream);
                            if (!filtered.isEmpty()) {
                                fieldTaint.put(key, filtered);
                            }
                        }
                    }
                }
            } else if (summary.isGetter()) {
                Set<String> mark = fieldTaint.get(key);
                if (mark != null && !mark.isEmpty()) {
                    Set<String> filtered = filterMarkersForType(mark, fieldType, enableContainer, enableArray,
                            enableOptional, enableStream);
                    if (!filtered.isEmpty()) {
                        if (fieldReturnMarkers == null) {
                            fieldReturnMarkers = new HashSet<>();
                        }
                        fieldReturnMarkers.addAll(filtered);
                    }
                }
            }
        }

        String guardReturnMarker = resolveGuardReturnMarker(owner, name, desc, stack, argCount, isStaticCall);
        applyGuardSanitizers(owner, name, desc, stack, argCount, isStaticCall);

        ModelResult modelResult = new ModelResult();
        ModelResult additionalResult = new ModelResult();
        // 找到下个方法
        if (nextMatch) {
            if (isSanitizerMatched(owner, name, desc, stack, argCount, opcode)) {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
                applyMarkerToTop(postInvokeMarker, desc);
                pass.set(TaintPass.fail());
                appendText("taint propagation blocked by barrier");
                return;
            }
            modelResult = applyModelRules(summaryRule, owner, name, desc, stack, argCount,
                    isStaticCall);
            if (enableAdditionalRules) {
                additionalResult = applyModelRules(additionalRule, owner, name, desc, stack, argCount,
                        isStaticCall);
            }
            Set<Integer> taintedParams = new HashSet<>();
            int totalSlots = argSlots + (opcode == Opcodes.INVOKESTATIC ? 0 : 1);
            if (stack.size() >= totalSlots) {
                for (int i = 0; i < totalSlots; i++) {
                    int stackIndex = stack.size() - 1 - i;
                    Set<String> item = stack.get(stackIndex);
                    if (item == null || item.isEmpty()) {
                        continue;
                    }
                    if (!hasAnyTaintMarker(item)) {
                        continue;
                    }
                    boolean hardTaint = containsHardTaint(item);
                    int paramIndex = resolveParamIndexFromStack(i, argumentTypes, argSlots,
                            opcode == Opcodes.INVOKESTATIC);
                    if (paramIndex == -1) {
                        continue;
                    }
                    boolean allowContainer = allowContainerForParam(paramIndex, argumentTypes,
                            enableContainer, enableArray, enableOptional, enableStream, owner);
                    if (!hardTaint && !allowContainer) {
                        continue;
                    }
                    if (!hardTaint) {
                        markLowConfidence("container marker propagation");
                    }
                    taintedParams.add(paramIndex);
                    int argIndex = paramIndex;
                    if (argIndex >= 0 && argIndex < argumentTypes.length) {
                        Type argType = argumentTypes[argIndex];
                        if (isLowRiskType(argType)) {
                            markLowConfidence("low risk argument type: " + formatTypeName(argType));
                        }
                    }
                    String paramLabel = formatParamLabel(paramIndex);
                    logger.info("taint detected at callsite param: {}", paramLabel);
                    text.append(String.format("taint detected at callsite param: %s", paramLabel));
                    text.append("\n");
                }
            }
            pass.set(TaintPass.fromParamIndices(taintedParams));
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            if (fieldReturnMarkers != null && !fieldReturnMarkers.isEmpty()) {
                final Type returnType = Type.getReturnType(desc);
                if (returnType.getSort() != Type.VOID) {
                    addMarkersToTop(returnType.getSize(), fieldReturnMarkers);
                }
            }
            applyReturnMarkers(modelResult, desc);
            applyReturnMarkers(additionalResult, desc);
            applyExtraReturnMarkers(containerReturnMarkers, desc);
            applyMarkerToTop(guardReturnMarker, desc);
            applyMarkerToTop(postInvokeMarker, desc);
            return;
        }

        if (this.rule == null || this.rule.getRules() == null) {
            logger.warn("sanitizer rules not loaded, skipping sanitizer check");
            modelResult = applyModelRules(summaryRule, owner, name, desc, stack, argCount,
                    opcode == Opcodes.INVOKESTATIC);
            if (enableAdditionalRules) {
                additionalResult = applyModelRules(additionalRule, owner, name, desc, stack, argCount,
                        opcode == Opcodes.INVOKESTATIC);
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            if (fieldReturnMarkers != null && !fieldReturnMarkers.isEmpty()) {
                final Type returnType = Type.getReturnType(desc);
                if (returnType.getSort() != Type.VOID) {
                    addMarkersToTop(returnType.getSize(), fieldReturnMarkers);
                }
            }
            applyReturnMarkers(modelResult, desc);
            applyReturnMarkers(additionalResult, desc);
            applyExtraReturnMarkers(containerReturnMarkers, desc);
            applyMarkerToTop(guardReturnMarker, desc);
            applyMarkerToTop(postInvokeMarker, desc);
            return;
        }

        if (isSanitizerMatched(owner, name, desc, stack, argCount, opcode)) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            applyMarkerToTop(postInvokeMarker, desc);
            pass.set(TaintPass.fail());
            appendText("taint propagation blocked by barrier");
            return;
        }

        modelResult = applyModelRules(summaryRule, owner, name, desc, stack, argCount,
                opcode == Opcodes.INVOKESTATIC);
        if (enableAdditionalRules) {
            additionalResult = applyModelRules(additionalRule, owner, name, desc, stack, argCount,
                    opcode == Opcodes.INVOKESTATIC);
        }
        Set<Integer> taintedParams = collectTaintedParamIndices(stack, argSlots, isStaticCall,
                argumentTypes, enableContainer, enableArray, enableOptional, enableStream, owner);
        Set<String> mergedArgTaint = mergeArgTaintSets(stack, argSlots, isStaticCall,
                argumentTypes, enableContainer, enableArray, enableOptional, enableStream, owner);
        boolean hasArgTaint = !mergedArgTaint.isEmpty();
        boolean explicitHardReturn = hasHardReturnMarker(modelResult)
                || hasHardReturnMarker(additionalResult)
                || (containerReturnMarkers != null && containerReturnMarkers.contains(TaintAnalyzer.TAINT));
        boolean weakReturn = false;
        boolean propagateMerged = false;
        boolean semanticDenied = false;
        TaintSemanticSummary.ReturnFlowSummary returnSummary = null;
        TaintSemanticSummary.CallGateDecision gateDecision = null;
        boolean strictGate = propagationMode != TaintPropagationMode.COMPAT
                && (propagationMode == TaintPropagationMode.STRICT
                || (profile != null && profile.isSemanticGateEnabled()));
        if (hasArgTaint) {
            if (strictGate) {
                Integer jarId = resolveJarId(owner);
                gateDecision = TaintSemanticSummary.resolveCallGate(summaryRule, owner, name, desc, jarId, taintedParams);
                if (gateDecision.isKnown() && !gateDecision.isAllowed()) {
                    semanticDenied = true;
                    appendText("taint propagation blocked by semantic gate");
                }
            } else if (propagationMode != TaintPropagationMode.COMPAT) {
                Integer jarId = resolveJarId(owner);
                returnSummary = TaintSemanticSummary.resolveReturnFlow(summaryRule, owner, name, desc, jarId);
            }
        }
        if (hasArgTaint) {
            if (propagationMode == TaintPropagationMode.COMPAT) {
                propagateMerged = true;
            } else if (!semanticDenied && explicitHardReturn) {
                propagateMerged = true;
            } else if (!semanticDenied && gateDecision != null
                    && gateDecision.isKnown() && gateDecision.isAllowed()) {
                propagateMerged = true;
            } else if (!semanticDenied && returnSummary != null
                    && returnSummary.isKnown() && returnSummary.allowsAny(taintedParams)) {
                propagateMerged = true;
            } else if (!semanticDenied && propagationMode == TaintPropagationMode.BALANCED
                    && (profile == null || profile.isSeedHeuristicEnabled())
                    && shouldWeakReturnPropagation(owner, argumentTypes, stack, argCount,
                    isStaticCall, Type.getReturnType(desc))) {
                propagateMerged = true;
                weakReturn = true;
            }
        }

        final Type returnType = Type.getReturnType(desc);
        final int retSize = returnType.getSize();
        super.visitMethodInsn(opcode, owner, name, desc, itf);
        if (propagateMerged && returnType.getSort() != Type.VOID) {
            boolean allowContainer = allowContainerMarkers(returnType);
            Set<String> taintSet = stripContainerMarkers(mergedArgTaint, allowContainer);
            if (!taintSet.contains(TaintAnalyzer.TAINT)) {
                taintSet.add(TaintAnalyzer.TAINT);
            }
            for (int j = 0; j < retSize; j++) {
                int topIndex = stack.size() - retSize + j;
                stack.set(topIndex, new HashSet<>(taintSet));
            }
            if (weakReturn) {
                markLowConfidence("返回值启发式");
            }
        }
        if (fieldReturnMarkers != null && !fieldReturnMarkers.isEmpty()) {
            if (returnType.getSort() != Type.VOID) {
                addMarkersToTop(returnType.getSize(), fieldReturnMarkers);
            }
        }
        if (returnAsSource) {
            if (returnType.getSort() != Type.VOID) {
                markLowConfidence("返回值启发式");
                for (int j = 0; j < retSize; j++) {
                    int topIndex = stack.size() - retSize + j;
                    Set<String> taintSet = new HashSet<>();
                    taintSet.add(TaintAnalyzer.TAINT);
                    stack.set(topIndex, taintSet);
                }
            }
        }
        applyReturnMarkers(modelResult, desc);
        applyReturnMarkers(additionalResult, desc);
        applyExtraReturnMarkers(containerReturnMarkers, desc);
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

    private boolean hasHardReturnMarker(ModelResult modelResult) {
        return modelResult != null
                && modelResult.returnMarkers != null
                && modelResult.returnMarkers.contains(TaintAnalyzer.TAINT);
    }

    private boolean isSanitizerMatched(String owner, String name, String desc,
                                       List<Set<String>> stack, int argCount,
                                       int opcode) {
        if (this.rule == null || this.rule.getRules() == null) {
            return false;
        }
        List<Sanitizer> rules = this.rule.getRules();
        for (Sanitizer rule : rules) {
            if (rule == null) {
                continue;
            }
            if (!sanitizerKindMatches(rule)) {
                continue;
            }
            if (!owner.equals(rule.getClassName())
                    || !name.equals(rule.getMethodName())
                    || !desc.equals(rule.getMethodDesc())) {
                continue;
            }

            int ruleIndex = rule.getParamIndex();
            if (ruleIndex == Sanitizer.ALL_PARAMS) {
                return true;
            }

            if (ruleIndex < 0 && ruleIndex != Sanitizer.THIS_PARAM) {
                continue;
            }

            if (stack.size() < argCount) {
                continue;
            }

            int targetStackIndex;
            if (opcode == Opcodes.INVOKESTATIC) {
                if (ruleIndex == Sanitizer.THIS_PARAM) {
                    continue;
                }
                targetStackIndex = stack.size() - argCount + ruleIndex;
            } else {
                if (ruleIndex == Sanitizer.THIS_PARAM) {
                    targetStackIndex = stack.size() - argCount;
                } else {
                    targetStackIndex = stack.size() - argCount + 1 + ruleIndex;
                }
            }

            if (targetStackIndex < 0 || targetStackIndex >= stack.size()) {
                continue;
            }
            Set<String> targetParam = stack.get(targetStackIndex);
            if (targetParam != null && targetParam.contains(TaintAnalyzer.TAINT)) {
                String paramLabel = formatParamLabel(ruleIndex);
                logger.info("污点参数 命中 Sanitizer - {} - {} - {} - 参数: {}",
                        owner, name, desc, paramLabel);
                text.append(String.format("污点参数 命中 Sanitizer - %s - %s - %s - 参数: %s",
                        owner, name, desc, paramLabel));
                text.append("\n");
                return true;
            }
        }
        return false;
    }

    private boolean shouldWeakReturnPropagation(String owner, Type[] argumentTypes,
                                                List<Set<String>> stack, int argCount,
                                                boolean isStatic, Type returnType) {
        if (returnType == null || returnType.getSort() == Type.VOID) {
            return false;
        }
        if (!isStatic && returnType.getSort() == Type.OBJECT) {
            String returnOwner = returnType.getInternalName();
            if (returnOwner != null && returnOwner.equals(owner)
                    && hasReceiverTaint(stack, argCount, false)) {
                return true;
            }
        }
        if (argumentTypes == null || argumentTypes.length == 0) {
            return false;
        }
        for (int i = 0; i < argumentTypes.length; i++) {
            Type argType = argumentTypes[i];
            if (argType == null || !argType.equals(returnType)) {
                continue;
            }
            Set<String> paramSet = resolveParamSet(stack, argCount, isStatic, i);
            if (paramSet != null && paramSet.contains(TaintAnalyzer.TAINT)) {
                return true;
            }
        }
        return false;
    }

    private ModelResult applyModelRules(TaintModelRule modelRule,
                                        String owner, String name, String desc,
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

    private Set<String> resolveContainerReturnMarkers(String owner,
                                                      String name,
                                                      String desc,
                                                      List<Set<String>> stack,
                                                      Type[] argumentTypes,
                                                      int argCount,
                                                      boolean isStatic,
                                                      boolean enableContainer,
                                                      boolean enableArray,
                                                      boolean enableBuilder,
                                                      boolean enableOptional,
                                                      boolean enableStream) {
        Set<String> markers = new HashSet<>();
        if (stack == null || stack.isEmpty()) {
            return markers;
        }
        if (!enableContainer && !enableArray && !enableBuilder && !enableOptional && !enableStream) {
            return markers;
        }
        if (isStatic) {
            if (enableOptional && isOptionalLike(owner)) {
                applyOptionalStaticEffects(owner, name, stack, argCount, markers);
                if (!markers.isEmpty()) {
                    return markers;
                }
            }
            if (enableContainer || enableArray) {
                applyStaticContainerEffects(owner, name, desc, stack, argCount, argumentTypes, markers,
                        enableContainer, enableArray);
            }
            return markers;
        }
        Set<String> receiver = resolveReceiverSet(stack, argCount, false);
        if (receiver == null) {
            return markers;
        }
        if (enableOptional && isOptionalLike(owner)) {
            applyOptionalInstanceEffects(name, desc, receiver, stack, argCount, argumentTypes, markers);
            return markers;
        }
        if (enableStream && isStreamLike(owner)) {
            applyStreamEffects(name, desc, receiver, stack, argCount, argumentTypes, markers);
            return markers;
        }
        if (enableBuilder && isStringBuilderLike(owner)) {
            applyBuilderEffects(name, stack, argCount, receiver, argumentTypes);
        }
        if (!enableContainer) {
            return markers;
        }
        if (isMapEntryLike(owner)) {
            applyEntryEffects(name, receiver, stack, argCount, markers);
        }
        if (isIteratorLike(owner) || isEnumerationLike(owner)) {
            applyIteratorEffects(name, receiver, markers);
        }
        if (isMapLike(owner)) {
            applyMapEffects(name, receiver, stack, argCount, markers, argumentTypes);
        }
        if (isCollectionLike(owner)) {
            applyCollectionEffects(name, receiver, stack, argCount, markers, argumentTypes);
        }
        return markers;
    }

    private void applyStaticContainerEffects(String owner,
                                             String name,
                                             String desc,
                                             List<Set<String>> stack,
                                             int argCount,
                                             Type[] argumentTypes,
                                             Set<String> markers,
                                             boolean enableContainer,
                                             boolean enableArray) {
        if (enableArray) {
            if ("java/lang/System".equals(owner) && "arraycopy".equals(name)
                    && "(Ljava/lang/Object;ILjava/lang/Object;II)V".equals(desc)) {
                Set<String> src = resolveParamSet(stack, argCount, true, 0);
                Set<String> dest = resolveParamSet(stack, argCount, true, 2);
                if (dest != null && src != null && hasAnyTaintMarker(src)) {
                    markContainerValue(dest, CONTAINER_ELEMENT, "arraycopy");
                }
                return;
            }
            if ("java/util/Arrays".equals(owner)) {
                if ("asList".equals(name)) {
                    Set<String> arg = resolveParamSet(stack, argCount, true, 0);
                    if (arg != null && hasAnyTaintMarker(arg)) {
                        markers.add(CONTAINER_ELEMENT);
                        markers.add(TaintAnalyzer.TAINT);
                    }
                    return;
                }
                if ("copyOf".equals(name) || "copyOfRange".equals(name)) {
                    Set<String> arg = resolveParamSet(stack, argCount, true, 0);
                    if (arg != null && hasAnyTaintMarker(arg)) {
                        markers.add(CONTAINER_ELEMENT);
                        markers.add(TaintAnalyzer.TAINT);
                    }
                    return;
                }
            }
        }
        if (!enableContainer) {
            return;
        }
        if ("java/util/Collections".equals(owner)) {
            if (name.startsWith("unmodifiable") || name.startsWith("synchronized") || name.startsWith("checked")) {
                Set<String> arg = resolveParamSet(stack, argCount, true, 0);
                if (arg != null) {
                    copyContainerMarkers(arg, markers);
                }
                return;
            }
            if ("singletonList".equals(name) || "singleton".equals(name)) {
                Set<String> arg = resolveParamSet(stack, argCount, true, 0);
                if (arg != null && hasAnyTaintMarker(arg)) {
                    markers.add(CONTAINER_ELEMENT);
                    markers.add(TaintAnalyzer.TAINT);
                }
                return;
            }
            if ("singletonMap".equals(name)) {
                Set<String> key = resolveParamSet(stack, argCount, true, 0);
                Set<String> val = resolveParamSet(stack, argCount, true, 1);
                if (key != null && hasAnyTaintMarker(key)) {
                    markers.add(CONTAINER_KEY);
                }
                if (val != null && hasAnyTaintMarker(val)) {
                    markers.add(CONTAINER_VALUE);
                    markers.add(TaintAnalyzer.TAINT);
                }
            }
        }
    }

    private void applyBuilderEffects(String name,
                                     List<Set<String>> stack,
                                     int argCount,
                                     Set<String> receiver,
                                     Type[] argumentTypes) {
        if (receiver == null || name == null) {
            return;
        }
        if ("append".equals(name) || "insert".equals(name) || "replace".equals(name)) {
            Set<String> last = resolveLastParamSet(stack, argCount, false, argumentTypes);
            if (last != null && hasAnyTaintMarker(last)) {
                receiver.add(TaintAnalyzer.TAINT);
                markLowConfidence("StringBuilder 追加污染");
            }
        }
    }

    private void applyEntryEffects(String name,
                                   Set<String> receiver,
                                   List<Set<String>> stack,
                                   int argCount,
                                   Set<String> markers) {
        if (receiver == null || name == null) {
            return;
        }
        if ("getKey".equals(name) && receiver.contains(CONTAINER_KEY)) {
            markers.add(CONTAINER_KEY);
            markers.add(TaintAnalyzer.TAINT);
            return;
        }
        if ("getValue".equals(name) && receiver.contains(CONTAINER_VALUE)) {
            markers.add(CONTAINER_VALUE);
            markers.add(TaintAnalyzer.TAINT);
            return;
        }
        if ("setValue".equals(name)) {
            Set<String> val = resolveParamSet(stack, argCount, false, 0);
            if (val != null && hasAnyTaintMarker(val)) {
                receiver.add(CONTAINER_VALUE);
                receiver.add(TaintAnalyzer.TAINT);
                markLowConfidence("Map.Entry setValue");
            }
        }
    }

    private void applyIteratorEffects(String name, Set<String> receiver, Set<String> markers) {
        if (receiver == null || name == null) {
            return;
        }
        if ("next".equals(name) || "nextElement".equals(name)) {
            if (hasElementMarker(receiver)) {
                markers.add(TaintAnalyzer.TAINT);
                copyElementMarkers(receiver, markers);
            }
        }
    }

    private void applyMapEffects(String name,
                                 Set<String> receiver,
                                 List<Set<String>> stack,
                                 int argCount,
                                 Set<String> markers,
                                 Type[] argumentTypes) {
        if (receiver == null || name == null) {
            return;
        }
        if ("clear".equals(name)) {
            receiver.remove(CONTAINER_KEY);
            receiver.remove(CONTAINER_VALUE);
            return;
        }
        if ("put".equals(name) || "putIfAbsent".equals(name)
                || "replace".equals(name) || "merge".equals(name)
                || "compute".equals(name) || "computeIfAbsent".equals(name)
                || "computeIfPresent".equals(name)) {
            Set<String> key = resolveParamSet(stack, argCount, false, 0);
            Set<String> val = resolveParamSet(stack, argCount, false, 1);
            if (key != null && hasAnyTaintMarker(key)) {
                receiver.add(CONTAINER_KEY);
                markLowConfidence("Map 键污染");
            }
            if (val != null && hasAnyTaintMarker(val)) {
                markContainerValue(receiver, CONTAINER_VALUE, "Map 值污染");
            }
            return;
        }
        if ("putAll".equals(name)) {
            Set<String> src = resolveParamSet(stack, argCount, false, 0);
            if (src != null) {
                if (src.contains(CONTAINER_KEY)) {
                    receiver.add(CONTAINER_KEY);
                }
                if (src.contains(CONTAINER_VALUE) || src.contains(TaintAnalyzer.TAINT)) {
                    markContainerValue(receiver, CONTAINER_VALUE, "Map 复制污染");
                }
            }
            return;
        }
        if ("get".equals(name) || "getOrDefault".equals(name)
                || "remove".equals(name) || "replace".equals(name)) {
            if (receiver.contains(CONTAINER_VALUE)) {
                markers.add(CONTAINER_VALUE);
                markers.add(TaintAnalyzer.TAINT);
            }
            return;
        }
        if ("keySet".equals(name)) {
            if (receiver.contains(CONTAINER_KEY)) {
                markers.add(CONTAINER_KEY);
                markers.add(TaintAnalyzer.TAINT);
            }
            return;
        }
        if ("values".equals(name)) {
            if (receiver.contains(CONTAINER_VALUE)) {
                markers.add(CONTAINER_VALUE);
                markers.add(TaintAnalyzer.TAINT);
            }
            return;
        }
        if ("entrySet".equals(name)) {
            if (receiver.contains(CONTAINER_KEY)) {
                markers.add(CONTAINER_KEY);
            }
            if (receiver.contains(CONTAINER_VALUE)) {
                markers.add(CONTAINER_VALUE);
            }
            if (!markers.isEmpty()) {
                markers.add(TaintAnalyzer.TAINT);
            }
        }
    }

    private void applyCollectionEffects(String name,
                                        Set<String> receiver,
                                        List<Set<String>> stack,
                                        int argCount,
                                        Set<String> markers,
                                        Type[] argumentTypes) {
        if (receiver == null || name == null) {
            return;
        }
        if ("clear".equals(name)) {
            receiver.remove(CONTAINER_ELEMENT);
            return;
        }
        if (isCollectionWrite(name)) {
            Set<String> elem = resolveLastParamSet(stack, argCount, false, argumentTypes);
            if (elem != null && hasAnyTaintMarker(elem)) {
                markContainerValue(receiver, CONTAINER_ELEMENT, "集合写入污染");
            }
            if ("addAll".equals(name) && elem != null) {
                if (elem.contains(CONTAINER_ELEMENT)) {
                    receiver.add(CONTAINER_ELEMENT);
                }
            }
            return;
        }
        if (isCollectionRead(name)) {
            if (hasElementMarker(receiver)) {
                markers.add(TaintAnalyzer.TAINT);
                copyElementMarkers(receiver, markers);
            }
            return;
        }
        if (isIteratorFactory(name)) {
            if (hasElementMarker(receiver)) {
                copyElementMarkers(receiver, markers);
            }
        }
        if ("toArray".equals(name)) {
            if (hasElementMarker(receiver)) {
                markers.add(CONTAINER_ELEMENT);
                markers.add(TaintAnalyzer.TAINT);
            }
        }
    }

    private void applyOptionalStaticEffects(String owner,
                                            String name,
                                            List<Set<String>> stack,
                                            int argCount,
                                            Set<String> markers) {
        if (!isOptionalLike(owner) || name == null) {
            return;
        }
        if ("of".equals(name) || "ofNullable".equals(name)) {
            Set<String> arg = resolveParamSet(stack, argCount, true, 0);
            if (arg != null && hasAnyTaintMarker(arg)) {
                markers.add(CONTAINER_ELEMENT);
                markers.add(TaintAnalyzer.TAINT);
            }
        }
    }

    private void applyOptionalInstanceEffects(String name,
                                              String desc,
                                              Set<String> receiver,
                                              List<Set<String>> stack,
                                              int argCount,
                                              Type[] argumentTypes,
                                              Set<String> markers) {
        if (receiver == null || name == null) {
            return;
        }
        boolean hasElement = hasElementMarker(receiver);
        if ("get".equals(name) || "orElse".equals(name)
                || "orElseGet".equals(name) || "orElseThrow".equals(name)) {
            if (hasElement) {
                markers.add(TaintAnalyzer.TAINT);
                copyElementMarkers(receiver, markers);
            }
            if ("orElse".equals(name)) {
                Set<String> fallback = resolveLastParamSet(stack, argCount, false, argumentTypes);
                if (fallback != null && hasAnyTaintMarker(fallback)) {
                    markers.add(TaintAnalyzer.TAINT);
                }
            }
            return;
        }
        if ("map".equals(name) || "flatMap".equals(name) || "filter".equals(name)
                || "or".equals(name) || "stream".equals(name)) {
            if (hasElement) {
                copyElementMarkers(receiver, markers);
                markers.add(TaintAnalyzer.TAINT);
            }
        }
    }

    private void applyStreamEffects(String name,
                                    String desc,
                                    Set<String> receiver,
                                    List<Set<String>> stack,
                                    int argCount,
                                    Type[] argumentTypes,
                                    Set<String> markers) {
        if (receiver == null || name == null) {
            return;
        }
        if (!hasElementMarker(receiver)) {
            return;
        }
        if (isStreamIntermediate(name)) {
            copyElementMarkers(receiver, markers);
            return;
        }
        if (isStreamTerminalContainer(name)) {
            markers.add(CONTAINER_ELEMENT);
            markers.add(TaintAnalyzer.TAINT);
            return;
        }
        if (isStreamTerminalElement(name)) {
            copyElementMarkers(receiver, markers);
            markers.add(TaintAnalyzer.TAINT);
        }
    }

    private boolean isStreamIntermediate(String name) {
        return "map".equals(name)
                || "mapToInt".equals(name)
                || "mapToLong".equals(name)
                || "mapToDouble".equals(name)
                || "flatMap".equals(name)
                || "flatMapToInt".equals(name)
                || "flatMapToLong".equals(name)
                || "flatMapToDouble".equals(name)
                || "filter".equals(name)
                || "distinct".equals(name)
                || "sorted".equals(name)
                || "limit".equals(name)
                || "skip".equals(name)
                || "peek".equals(name)
                || "sequential".equals(name)
                || "parallel".equals(name)
                || "unordered".equals(name)
                || "onClose".equals(name)
                || "takeWhile".equals(name)
                || "dropWhile".equals(name)
                || "boxed".equals(name)
                || "iterator".equals(name)
                || "spliterator".equals(name);
    }

    private boolean isStreamTerminalElement(String name) {
        return "findFirst".equals(name)
                || "findAny".equals(name)
                || "reduce".equals(name)
                || "min".equals(name)
                || "max".equals(name);
    }

    private boolean isStreamTerminalContainer(String name) {
        return "collect".equals(name)
                || "toArray".equals(name)
                || "toList".equals(name);
    }

    private Set<String> resolveLastParamSet(List<Set<String>> stack,
                                            int argCount,
                                            boolean isStatic,
                                            Type[] argumentTypes) {
        if (argumentTypes == null || argumentTypes.length == 0) {
            return null;
        }
        int last = argumentTypes.length - 1;
        return resolveParamSet(stack, argCount, isStatic, last);
    }

    private Set<String> resolveParamSet(List<Set<String>> stack,
                                        int argCount,
                                        boolean isStatic,
                                        int paramIndex) {
        int idx = resolveParamStackIndex(stack, argCount, isStatic, paramIndex);
        if (idx < 0 || idx >= stack.size()) {
            return null;
        }
        return stack.get(idx);
    }

    private boolean hasAnyTaintMarker(Set<String> set) {
        if (set == null || set.isEmpty()) {
            return false;
        }
        return set.contains(TaintAnalyzer.TAINT)
                || set.contains(CONTAINER_ELEMENT)
                || set.contains(CONTAINER_KEY)
                || set.contains(CONTAINER_VALUE);
    }

    private boolean hasElementMarker(Set<String> receiver) {
        if (receiver == null) {
            return false;
        }
        return receiver.contains(CONTAINER_ELEMENT)
                || receiver.contains(CONTAINER_KEY)
                || receiver.contains(CONTAINER_VALUE);
    }

    private void copyElementMarkers(Set<String> receiver, Set<String> markers) {
        if (receiver == null || markers == null) {
            return;
        }
        if (receiver.contains(CONTAINER_ELEMENT)) {
            markers.add(CONTAINER_ELEMENT);
        }
        if (receiver.contains(CONTAINER_KEY)) {
            markers.add(CONTAINER_KEY);
        }
        if (receiver.contains(CONTAINER_VALUE)) {
            markers.add(CONTAINER_VALUE);
        }
    }

    private void copyContainerMarkers(Set<String> source, Set<String> markers) {
        if (source == null || markers == null) {
            return;
        }
        if (source.contains(CONTAINER_ELEMENT)) {
            markers.add(CONTAINER_ELEMENT);
        }
        if (source.contains(CONTAINER_KEY)) {
            markers.add(CONTAINER_KEY);
        }
        if (source.contains(CONTAINER_VALUE)) {
            markers.add(CONTAINER_VALUE);
        }
        if (source.contains(TaintAnalyzer.TAINT)) {
            markers.add(TaintAnalyzer.TAINT);
        }
    }

    private void markContainerValue(Set<String> receiver, String marker, String reason) {
        if (receiver == null || marker == null) {
            return;
        }
        receiver.add(marker);
        if (CONTAINER_ELEMENT.equals(marker) || CONTAINER_VALUE.equals(marker)) {
            receiver.add(TaintAnalyzer.TAINT);
        }
        if (reason != null) {
            markLowConfidence(reason);
        }
    }

    private boolean isCollectionLike(String owner) {
        if (owner == null) {
            return false;
        }
        if ("java/util/Collection".equals(owner)
                || "java/util/List".equals(owner)
                || "java/util/Set".equals(owner)
                || "java/util/Queue".equals(owner)
                || "java/util/Deque".equals(owner)
                || "java/lang/Iterable".equals(owner)) {
            return true;
        }
        Set<String> aliases = CONTAINER_ALIASES.get(owner);
        if (aliases == null) {
            return false;
        }
        return aliases.contains("java/util/Collection")
                || aliases.contains("java/util/List")
                || aliases.contains("java/util/Set")
                || aliases.contains("java/util/Queue")
                || aliases.contains("java/util/Deque")
                || aliases.contains("java/lang/Iterable");
    }

    private boolean isMapLike(String owner) {
        if (owner == null) {
            return false;
        }
        if ("java/util/Map".equals(owner)) {
            return true;
        }
        Set<String> aliases = CONTAINER_ALIASES.get(owner);
        if (aliases == null) {
            return false;
        }
        return aliases.contains("java/util/Map");
    }

    private boolean isMapEntryLike(String owner) {
        return "java/util/Map$Entry".equals(owner);
    }

    private boolean isIteratorLike(String owner) {
        return "java/util/Iterator".equals(owner) || "java/util/ListIterator".equals(owner);
    }

    private boolean isEnumerationLike(String owner) {
        return "java/util/Enumeration".equals(owner);
    }

    private boolean isStringBuilderLike(String owner) {
        return "java/lang/StringBuilder".equals(owner) || "java/lang/StringBuffer".equals(owner);
    }

    private boolean isOptionalLike(String owner) {
        return "java/util/Optional".equals(owner)
                || "java/util/OptionalInt".equals(owner)
                || "java/util/OptionalLong".equals(owner)
                || "java/util/OptionalDouble".equals(owner);
    }

    private boolean isStreamLike(String owner) {
        return "java/util/stream/Stream".equals(owner)
                || "java/util/stream/IntStream".equals(owner)
                || "java/util/stream/LongStream".equals(owner)
                || "java/util/stream/DoubleStream".equals(owner)
                || "java/util/stream/BaseStream".equals(owner);
    }

    private boolean isCollectionWrite(String name) {
        return "add".equals(name)
                || "addAll".equals(name)
                || "offer".equals(name)
                || "push".equals(name)
                || "put".equals(name)
                || "set".equals(name)
                || "addFirst".equals(name)
                || "addLast".equals(name)
                || "enqueue".equals(name);
    }

    private boolean isCollectionRead(String name) {
        return "get".equals(name)
                || "poll".equals(name)
                || "pop".equals(name)
                || "peek".equals(name)
                || "element".equals(name)
                || "remove".equals(name)
                || "first".equals(name)
                || "last".equals(name)
                || "getFirst".equals(name)
                || "getLast".equals(name)
                || "removeFirst".equals(name)
                || "removeLast".equals(name);
    }

    private boolean isIteratorFactory(String name) {
        return "iterator".equals(name)
                || "listIterator".equals(name)
                || "spliterator".equals(name)
                || "stream".equals(name);
    }

    private int resolveParamIndexFromStack(int stackOffset,
                                           Type[] argumentTypes,
                                           int argSlots,
                                           boolean isStaticCall) {
        if (stackOffset < 0) {
            return -1;
        }
        if (!isStaticCall && stackOffset == argSlots) {
            return Sanitizer.THIS_PARAM;
        }
        if (stackOffset >= argSlots || argumentTypes == null) {
            return -1;
        }
        int slotIndex = argSlots - 1 - stackOffset;
        int cursor = 0;
        for (int i = 0; i < argumentTypes.length; i++) {
            int size = argumentTypes[i].getSize();
            int next = cursor + size;
            if (slotIndex >= cursor && slotIndex < next) {
                return i;
            }
            cursor = next;
        }
        return -1;
    }

    private boolean allowContainerForParam(int paramIndex,
                                           Type[] argumentTypes,
                                           boolean enableContainer,
                                           boolean enableArray,
                                           boolean enableOptional,
                                           boolean enableStream,
                                           String owner) {
        if (paramIndex == Sanitizer.THIS_PARAM) {
            return isContainerInternalName(owner, enableContainer, enableOptional, enableStream)
                    || (enableArray && isArrayInternalName(owner));
        }
        if (paramIndex < 0 || argumentTypes == null || argumentTypes.length == 0) {
            return false;
        }
        if (paramIndex >= argumentTypes.length) {
            return false;
        }
        return isContainerType(argumentTypes[paramIndex], enableContainer, enableArray, enableOptional, enableStream);
    }

    private boolean isContainerType(Type type,
                                    boolean enableContainer,
                                    boolean enableArray,
                                    boolean enableOptional,
                                    boolean enableStream) {
        if (type == null) {
            return false;
        }
        int sort = type.getSort();
        if (sort == Type.ARRAY) {
            return enableArray;
        }
        if (sort != Type.OBJECT) {
            return false;
        }
        String internalName = type.getInternalName();
        return isContainerInternalName(internalName, enableContainer, enableOptional, enableStream);
    }

    private boolean isContainerInternalName(String internalName,
                                            boolean enableContainer,
                                            boolean enableOptional,
                                            boolean enableStream) {
        if (internalName == null) {
            return false;
        }
        if (enableContainer && (isCollectionLike(internalName)
                || isMapLike(internalName)
                || isMapEntryLike(internalName)
                || isIteratorLike(internalName)
                || isEnumerationLike(internalName))) {
            return true;
        }
        if (enableOptional && isOptionalLike(internalName)) {
            return true;
        }
        if (enableStream && isStreamLike(internalName)) {
            return true;
        }
        if (AnalyzeEnv.inheritanceMap == null) {
            return false;
        }
        Set<ClassReference.Handle> parents = AnalyzeEnv.inheritanceMap.getSuperClasses(
                new ClassReference.Handle(internalName));
        if (parents == null || parents.isEmpty()) {
            return false;
        }
        for (ClassReference.Handle parent : parents) {
            if (parent == null || parent.getName() == null) {
                continue;
            }
            String name = parent.getName();
            if (enableContainer && (isCollectionLike(name)
                    || isMapLike(name)
                    || isMapEntryLike(name)
                    || isIteratorLike(name)
                    || isEnumerationLike(name))) {
                return true;
            }
            if (enableOptional && isOptionalLike(name)) {
                return true;
            }
            if (enableStream && isStreamLike(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean isArrayInternalName(String internalName) {
        return internalName != null && internalName.startsWith("[");
    }

    private Set<String> stripContainerMarkers(Set<String> markers, boolean allowContainer) {
        if (markers == null || markers.isEmpty()) {
            return new HashSet<>();
        }
        if (allowContainer) {
            return new HashSet<>(markers);
        }
        Set<String> filtered = new HashSet<>(markers);
        filtered.remove(CONTAINER_ELEMENT);
        filtered.remove(CONTAINER_KEY);
        filtered.remove(CONTAINER_VALUE);
        return filtered;
    }

    private Set<String> filterMarkersForType(Set<String> markers,
                                             Type type,
                                             boolean enableContainer,
                                             boolean enableArray,
                                             boolean enableOptional,
                                             boolean enableStream) {
        if (markers == null || markers.isEmpty()) {
            return new HashSet<>();
        }
        if (type == null) {
            return new HashSet<>(markers);
        }
        boolean allowContainer = isContainerType(type, enableContainer, enableArray, enableOptional, enableStream);
        if (allowContainer) {
            return new HashSet<>(markers);
        }
        Set<String> filtered = new HashSet<>(markers);
        filtered.remove(CONTAINER_ELEMENT);
        filtered.remove(CONTAINER_KEY);
        filtered.remove(CONTAINER_VALUE);
        return filtered;
    }

    private Set<Integer> collectTaintedParamIndices(List<Set<String>> stack,
                                                    int argSlots,
                                                    boolean isStaticCall,
                                                    Type[] argumentTypes,
                                                    boolean enableContainer,
                                                    boolean enableArray,
                                                    boolean enableOptional,
                                                    boolean enableStream,
                                                    String owner) {
        Set<Integer> indices = new HashSet<>();
        if (stack == null || stack.isEmpty() || argSlots <= 0 && isStaticCall) {
            return indices;
        }
        int totalSlots = argSlots + (isStaticCall ? 0 : 1);
        if (stack.size() < totalSlots) {
            return indices;
        }
        for (int i = 0; i < totalSlots; i++) {
            int stackIndex = stack.size() - 1 - i;
            Set<String> item = stack.get(stackIndex);
            if (item == null || item.isEmpty()) {
                continue;
            }
            if (!hasAnyTaintMarker(item)) {
                continue;
            }
            boolean hardTaint = containsHardTaint(item);
            int paramIndex = resolveParamIndexFromStack(i, argumentTypes, argSlots, isStaticCall);
            if (paramIndex == -1) {
                continue;
            }
            if (!hardTaint) {
                boolean allowContainer = allowContainerForParam(paramIndex, argumentTypes,
                        enableContainer, enableArray, enableOptional, enableStream, owner);
                if (!allowContainer) {
                    continue;
                }
            }
            indices.add(paramIndex);
        }
        return indices;
    }

    private Set<String> mergeArgTaintSets(List<Set<String>> stack,
                                          int argSlots,
                                          boolean isStatic,
                                          Type[] argumentTypes,
                                          boolean enableContainer,
                                          boolean enableArray,
                                          boolean enableOptional,
                                          boolean enableStream,
                                          String owner) {
        Set<String> merged = new HashSet<>();
        if (stack == null || stack.isEmpty() || (argSlots <= 0 && isStatic)) {
            return merged;
        }
        int totalSlots = argSlots + (isStatic ? 0 : 1);
        if (stack.size() < totalSlots) {
            return merged;
        }
        boolean usedContainer = false;
        for (int i = 0; i < totalSlots; i++) {
            int stackIndex = stack.size() - 1 - i;
            if (stackIndex < 0 || stackIndex >= stack.size()) {
                continue;
            }
            Set<String> item = stack.get(stackIndex);
            if (item == null || item.isEmpty()) {
                continue;
            }
            boolean hardTaint = item.contains(TaintAnalyzer.TAINT);
            boolean hasContainer = hasElementMarker(item);
            int paramIndex = resolveParamIndexFromStack(i, argumentTypes, argSlots, isStatic);
            if (paramIndex == -1) {
                continue;
            }
            boolean allowContainer = allowContainerForParam(paramIndex, argumentTypes,
                    enableContainer, enableArray, enableOptional, enableStream, owner);
            if (hardTaint) {
                merged.addAll(stripContainerMarkers(item, allowContainer));
                continue;
            }
            if (hasContainer && allowContainer) {
                merged.addAll(item);
                usedContainer = true;
            }
        }
        if (usedContainer && !merged.isEmpty() && !merged.contains(TaintAnalyzer.TAINT)) {
            merged.add(TaintAnalyzer.TAINT);
            markLowConfidence("容器别名传播");
        }
        return merged;
    }

    private void applyReturnMarkers(ModelResult modelResult, String methodDesc) {
        if (modelResult == null || modelResult.returnMarkers.isEmpty()) {
            return;
        }
        Type returnType = Type.getReturnType(methodDesc);
        if (returnType.getSort() == Type.VOID) {
            return;
        }
        boolean allowContainer = allowContainerMarkers(returnType);
        for (String marker : modelResult.returnMarkers) {
            if (!allowContainer && isContainerMarker(marker)) {
                continue;
            }
            addMarkerToTop(marker);
        }
    }

    private void applyExtraReturnMarkers(Set<String> markers, String methodDesc) {
        if (markers == null || markers.isEmpty()) {
            return;
        }
        Type returnType = Type.getReturnType(methodDesc);
        if (returnType.getSort() == Type.VOID) {
            return;
        }
        boolean allowContainer = allowContainerMarkers(returnType);
        for (String marker : markers) {
            if (!allowContainer && isContainerMarker(marker)) {
                continue;
            }
            addMarkerToTop(marker);
        }
    }

    private boolean allowContainerMarkers(Type returnType) {
        if (returnType == null) {
            return false;
        }
        boolean enableContainer = isAdditionalEnabled(TaintAnalysisProfile.AdditionalStep.CONTAINER);
        boolean enableArray = isAdditionalEnabled(TaintAnalysisProfile.AdditionalStep.ARRAY);
        boolean enableOptional = isAdditionalEnabled(TaintAnalysisProfile.AdditionalStep.OPTIONAL);
        boolean enableStream = isAdditionalEnabled(TaintAnalysisProfile.AdditionalStep.STREAM);
        return isContainerType(returnType, enableContainer, enableArray, enableOptional, enableStream);
    }

    private boolean isContainerMarker(String marker) {
        return CONTAINER_ELEMENT.equals(marker)
                || CONTAINER_KEY.equals(marker)
                || CONTAINER_VALUE.equals(marker);
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

    private void addMarkersToTop(int slots, Set<String> markers) {
        if (markers == null || markers.isEmpty()) {
            return;
        }
        List<Set<String>> stack = this.operandStack.getList();
        for (int i = 0; i < slots; i++) {
            int topIndex = stack.size() - 1 - i;
            if (topIndex < 0 || topIndex >= stack.size()) {
                continue;
            }
            Set<String> target = stack.get(topIndex);
            if (target == null) {
                target = new HashSet<>();
                stack.set(topIndex, target);
            }
            target.addAll(markers);
        }
    }

    private Set<String> collectValueMarkers(List<Set<String>> stack, int slots) {
        Set<String> markers = new HashSet<>();
        if (stack == null || stack.isEmpty() || slots <= 0) {
            return markers;
        }
        for (int i = 0; i < slots; i++) {
            int idx = stack.size() - 1 - i;
            if (idx < 0 || idx >= stack.size()) {
                continue;
            }
            mergeTaintMarkers(markers, stack.get(idx));
        }
        return markers;
    }

    private void mergeTaintMarkers(Set<String> target, Set<String> source) {
        if (target == null || source == null || source.isEmpty()) {
            return;
        }
        if (source.contains(TaintAnalyzer.TAINT)) {
            target.add(TaintAnalyzer.TAINT);
        }
        if (source.contains(CONTAINER_ELEMENT)) {
            target.add(CONTAINER_ELEMENT);
        }
        if (source.contains(CONTAINER_KEY)) {
            target.add(CONTAINER_KEY);
        }
        if (source.contains(CONTAINER_VALUE)) {
            target.add(CONTAINER_VALUE);
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

    private String resolveFieldDescFromKey(String fieldKey) {
        if (fieldKey == null || fieldKey.isEmpty()) {
            return null;
        }
        int idx = fieldKey.lastIndexOf('#');
        if (idx < 0 || idx + 1 >= fieldKey.length()) {
            return null;
        }
        return fieldKey.substring(idx + 1);
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

    private Type safeTypeFromDesc(String desc) {
        if (desc == null || desc.isEmpty()) {
            return null;
        }
        try {
            return Type.getType(desc);
        } catch (Throwable t) {
            return null;
        }
    }

    private Integer resolveJarId(String owner) {
        if (owner == null) {
            return null;
        }
        ClassReference clazz = AnalyzeEnv.classMap.get(new ClassReference.Handle(owner));
        if (clazz == null) {
            return null;
        }
        return clazz.getJarId();
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

    private boolean isAdditionalEnabled(TaintAnalysisProfile.AdditionalStep step) {
        return profile != null && profile.isAdditionalEnabled(step);
    }

    private void appendText(String message) {
        if (message == null) {
            return;
        }
        logger.info(message);
        if (text != null) {
            text.append(message).append("\n");
        }
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
