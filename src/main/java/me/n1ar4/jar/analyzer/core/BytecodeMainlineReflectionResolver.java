/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.core.build.BuildContext;
import me.n1ar4.jar.analyzer.core.bytecode.BuildBytecodeWorkspace;
import me.n1ar4.jar.analyzer.core.facts.BuildFactSnapshot;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class BytecodeMainlineReflectionResolver {
    private static final Logger logger = LogManager.getLogger();

    private static final String CLASS_OWNER = "java/lang/Class";
    private static final String METHOD_OWNER = "java/lang/reflect/Method";
    private static final String CTOR_OWNER = "java/lang/reflect/Constructor";
    private static final String MH_OWNER = "java/lang/invoke/MethodHandle";
    private static final String LOOKUP_OWNER = "java/lang/invoke/MethodHandles$Lookup";
    private static final String MT_OWNER = "java/lang/invoke/MethodType";
    private static final String SCF_OWNER = "java/lang/invoke/StringConcatFactory";
    private static final String LMF_OWNER = "java/lang/invoke/LambdaMetafactory";
    private static final int MAX_CONST_DEPTH = 10;
    private static final String REASON_CONST = "const";
    private static final String REASON_STATIC = "static";
    private static final String REASON_CONCAT = "concat";
    private static final String REASON_STRING_BUILDER = "string_builder";
    private static final String REASON_VALUE_OF = "value_of";
    private static final String REASON_STRING_API = "string_api";
    private static final String REASON_LOCAL = "local";
    private static final String REASON_CAST = "cast";
    private static final String REASON_USER_METHOD = "user_method";
    private static final String REASON_CLASS_CONST = "class_const";
    private static final String REASON_FOR_NAME = "for_name";
    private static final String REASON_CLASS_LOADER = "class_loader";
    private static final String REASON_CLASS_NEW_INSTANCE = "class_new_instance";
    private static final String REASON_METHOD_HANDLE = "method_handle";
    private static final String REASON_LAMBDA = "lambda";
    private static final String REASON_UNKNOWN = "unknown";
    private static final int IMPRECISE_TARGET_THRESHOLD = 4;

    private BytecodeMainlineReflectionResolver() {
    }

    static Result appendEdges(BuildFactSnapshot snapshot,
                              BuildContext context,
                              BuildBytecodeWorkspace workspace,
                              BytecodeMainlineCallGraphRunner.MethodLookup lookup) {
        if (context == null
                || lookup == null
                || workspace == null
                || workspace.parsedClasses().isEmpty()
                || context.methodMap == null
                || context.methodMap.isEmpty()) {
            return Result.empty();
        }
        Map<String, BuildFactSnapshot.AliasValueFact> instanceFieldFacts =
                snapshot == null || snapshot.constraints().instanceFieldFactsByKey().isEmpty()
                        ? collectInstanceFieldFacts(workspace)
                        : snapshot.constraints().instanceFieldFactsByKey();
        int reflectionEdges = 0;
        int methodHandleEdges = 0;
        int invokeDynamicEdges = 0;
        HintStats hintStats = HintStats.empty();
        for (BuildBytecodeWorkspace.ParsedClass parsedClass : workspace.parsedClasses()) {
            if (parsedClass == null || parsedClass.classNode() == null) {
                continue;
            }
            try {
                ClassNode cn = parsedClass.classNode();
                Map<String, String> staticStrings = collectStaticStringConstants(cn);
                invokeDynamicEdges += appendInvokeDynamicEdges(cn, parsedClass.file(), context, lookup);
                for (BuildBytecodeWorkspace.ParsedMethod parsedMethod : parsedClass.methods()) {
                    MethodNode mn = parsedMethod.methodNode();
                    if (mn == null || mn.instructions == null || mn.instructions.size() == 0) {
                        continue;
                    }
                    if (!containsReflectionInvoke(mn.instructions)) {
                        continue;
                    }
                    MethodReference.Handle caller = resolveMethodHandle(
                            lookup,
                            context.methodMap,
                            cn.name,
                            mn.name,
                            mn.desc,
                            parsedClass.jarId()
                    );
                    if (caller == null) {
                        continue;
                    }
                    BuildFactSnapshot.MethodConstraintFacts constraints = snapshot == null
                            ? BuildFactSnapshot.MethodConstraintFacts.empty()
                            : snapshot.constraints().methodConstraints(caller);
                    MethodResolveResult hintResult = applyHintEdges(
                            caller,
                            mn,
                            constraints.reflectionHints(),
                            context.methodCalls,
                            context.methodCallMeta,
                            context.methodMap
                    );
                    reflectionEdges += hintResult.reflectionEdges();
                    methodHandleEdges += hintResult.methodHandleEdges();
                    hintStats = hintStats.merge(summarizeHints(constraints.reflectionHints()));
                    if (!constraints.reflectionHints().isEmpty()) {
                        continue;
                    }
                    MethodResolveResult resolveResult = resolveInMethod(
                            caller,
                            mn,
                            cn.name,
                            cn,
                            parsedMethod.sourceFrames(),
                            constraints,
                            staticStrings,
                            instanceFieldFacts,
                            context.methodCalls,
                            context.methodMap,
                            context.methodCallMeta
                    );
                    reflectionEdges += resolveResult.reflectionEdges();
                    methodHandleEdges += resolveResult.methodHandleEdges();
                }
            } catch (Exception ex) {
                logger.warn("bytecode-mainline reflection edge build failed: {}", ex.toString());
            }
        }
        return new Result(
                reflectionEdges,
                methodHandleEdges,
                invokeDynamicEdges,
                hintStats.constSites(),
                hintStats.logSites(),
                hintStats.castSites(),
                hintStats.unknownSites(),
                hintStats.impreciseSites(),
                hintStats.thresholdExceededSites()
        );
    }

    static Map<MethodReference.Handle, BuildFactSnapshot.MethodReflectionHints> collectReflectionHints(
            BuildBytecodeWorkspace workspace,
            Map<MethodReference.Handle, MethodReference> methodMap,
            Map<String, BuildFactSnapshot.AliasValueFact> instanceFieldFacts) {
        if (workspace == null
                || workspace.parsedClasses().isEmpty()
                || methodMap == null
                || methodMap.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<MethodReference.Handle, BuildFactSnapshot.MethodReflectionHints> out = new LinkedHashMap<>();
        for (BuildBytecodeWorkspace.ParsedClass parsedClass : workspace.parsedClasses()) {
            if (parsedClass == null || parsedClass.classNode() == null || parsedClass.methods().isEmpty()) {
                continue;
            }
            Map<String, String> staticStrings = collectStaticStringConstants(parsedClass.classNode());
            for (BuildBytecodeWorkspace.ParsedMethod parsedMethod : parsedClass.methods()) {
                MethodNode mn = parsedMethod == null ? null : parsedMethod.methodNode();
                if (mn == null || mn.instructions == null || mn.instructions.size() == 0) {
                    continue;
                }
                if (!containsReflectionInvoke(mn.instructions)) {
                    continue;
                }
                MethodReference.Handle handle = new MethodReference.Handle(
                        new ClassReference.Handle(parsedClass.className(), parsedClass.jarId()),
                        mn.name,
                        mn.desc
                );
                BuildFactSnapshot.MethodReflectionHints hints = collectMethodHints(
                        parsedClass.classNode(),
                        parsedMethod,
                        staticStrings,
                        instanceFieldFacts,
                        methodMap
                );
                if (!hints.isEmpty()) {
                    out.put(handle, hints);
                }
            }
        }
        return out.isEmpty() ? Map.of() : Map.copyOf(out);
    }

    private static BuildFactSnapshot.MethodReflectionHints collectMethodHints(ClassNode classNode,
                                                                              BuildBytecodeWorkspace.ParsedMethod parsedMethod,
                                                                              Map<String, String> staticStrings,
                                                                              Map<String, BuildFactSnapshot.AliasValueFact> instanceFieldFacts,
                                                                              Map<MethodReference.Handle, MethodReference> methodMap) {
        MethodNode mn = parsedMethod == null ? null : parsedMethod.methodNode();
        if (classNode == null
                || mn == null
                || mn.instructions == null
                || mn.instructions.size() == 0
                || methodMap == null
                || methodMap.isEmpty()) {
            return BuildFactSnapshot.MethodReflectionHints.empty();
        }
        try {
            Frame<SourceValue>[] frames = parsedMethod.sourceFrames();
            if (frames == null) {
                Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());
                frames = analyzer.analyze(classNode.name, mn);
            }
            ResolveContext ctx = new ResolveContext(frames, mn.instructions, classNode, staticStrings, instanceFieldFacts);
            LinkedHashMap<Integer, BuildFactSnapshot.MethodInvokeHint> classNewInstanceHints = new LinkedHashMap<>();
            LinkedHashMap<Integer, BuildFactSnapshot.MethodInvokeHint> reflectionInvokeHints = new LinkedHashMap<>();
            LinkedHashMap<Integer, BuildFactSnapshot.MethodInvokeHint> methodHandleInvokeHints = new LinkedHashMap<>();
            ArrayList<BuildFactSnapshot.ReflectionHintDiagnostic> diagnostics = new ArrayList<>();
            for (int i = 0, n = mn.instructions.size(); i < n; i++) {
                AbstractInsnNode insn = mn.instructions.get(i);
                if (!(insn instanceof MethodInsnNode invoke)) {
                    continue;
                }
                if (isClassNewInstance(invoke)) {
                    Frame<SourceValue> frame = frames[i];
                    if (frame == null) {
                        continue;
                    }
                    int objIndex = frame.getStackSize() - 1;
                    if (objIndex < 0) {
                        continue;
                    }
                    ResolvedString classInfo = resolveClassNameWithReason(frame.getStack(objIndex), ctx);
                    if (classInfo == null || classInfo.value == null) {
                        continue;
                    }
                    MethodReference.Handle target = findUniqueMethod(
                            methodMap,
                            classInfo.value,
                            "<init>",
                            "()V"
                    );
                    if (target != null) {
                        classNewInstanceHints.put(
                                i,
                                new BuildFactSnapshot.MethodInvokeHint(
                                        i,
                                        List.of(target),
                                        REASON_CLASS_NEW_INSTANCE + "_" + classInfo.reason,
                                        BuildFactSnapshot.ReflectionHintTier.fromReason(REASON_CLASS_NEW_INSTANCE + "_" + classInfo.reason),
                                        false
                                )
                        );
                    }
                    continue;
                }
                if (isReflectionInvoke(invoke)) {
                    Frame<SourceValue> frame = frames[i];
                    if (frame == null) {
                        continue;
                    }
                    int argSlots = 0;
                    for (Type type : Type.getArgumentTypes(invoke.desc)) {
                        argSlots += type.getSize();
                    }
                    int objIndex = frame.getStackSize() - argSlots - 1;
                    if (objIndex < 0) {
                        continue;
                    }
                    MethodInsnNode creator = findSingleCreator(frame.getStack(objIndex), ctx, 0);
                    if (creator == null) {
                        continue;
                    }
                    TargetResolve resolved = resolveTarget(creator, invoke, ctx, methodMap);
                    BuildFactSnapshot.MethodInvokeHint hint = toInvokeHint(i, resolved);
                    if (hint != null) {
                        reflectionInvokeHints.put(i, hint);
                    }
                    BuildFactSnapshot.ReflectionHintDiagnostic diagnostic = toDiagnostic(i, "reflection_invoke", resolved);
                    if (diagnostic != null) {
                        diagnostics.add(diagnostic);
                    }
                    continue;
                }
                if (!isMethodHandleInvoke(invoke)) {
                    continue;
                }
                Frame<SourceValue> frame = frames[i];
                if (frame == null) {
                    continue;
                }
                int argCount = Type.getArgumentTypes(invoke.desc).length;
                int objIndex = frame.getStackSize() - argCount - 1;
                if (objIndex < 0) {
                    continue;
                }
                MethodInsnNode creator = findSingleHandleCreator(frame.getStack(objIndex), ctx, 0);
                if (creator == null) {
                    continue;
                }
                TargetResolve resolved = resolveHandleTarget(creator, ctx, methodMap);
                BuildFactSnapshot.MethodInvokeHint hint = toInvokeHint(i, resolved);
                if (hint != null) {
                    methodHandleInvokeHints.put(i, hint);
                }
                BuildFactSnapshot.ReflectionHintDiagnostic diagnostic = toDiagnostic(i, "method_handle", resolved);
                if (diagnostic != null) {
                    diagnostics.add(diagnostic);
                }
            }
            return new BuildFactSnapshot.MethodReflectionHints(
                    classNewInstanceHints,
                    reflectionInvokeHints,
                    methodHandleInvokeHints,
                    diagnostics
            );
        } catch (Exception ex) {
            logger.warn("bytecode-mainline reflection facts collect failed: {}.{}", classNode.name, mn.name);
            return BuildFactSnapshot.MethodReflectionHints.empty();
        }
    }

    private static int appendInvokeDynamicEdges(ClassNode cn,
                                                ClassFileEntity file,
                                                BuildContext context,
                                                BytecodeMainlineCallGraphRunner.MethodLookup lookup) {
        if (cn == null || cn.methods == null || cn.methods.isEmpty()) {
            return 0;
        }
        int added = 0;
        for (MethodNode mn : cn.methods) {
            if (mn == null || mn.instructions == null || mn.instructions.size() == 0) {
                continue;
            }
            MethodReference.Handle caller = null;
            InsnList instructions = mn.instructions;
            for (int i = 0, n = instructions.size(); i < n; i++) {
                AbstractInsnNode insn = instructions.get(i);
                if (!(insn instanceof InvokeDynamicInsnNode indy)) {
                    continue;
                }
                LambdaTarget lambda = resolveLambdaTarget(indy);
                if (lambda == null) {
                    continue;
                }
                MethodReference.Handle implHandle = resolveMethodHandle(
                        lookup,
                        context.methodMap,
                        lambda.implOwner(),
                        lambda.implName(),
                        lambda.implDesc(),
                        file == null ? null : file.getJarId()
                );
                if (implHandle == null) {
                    continue;
                }
                if (caller == null) {
                    caller = resolveMethodHandle(
                            lookup,
                            context.methodMap,
                            cn.name,
                            mn.name,
                            mn.desc,
                            file == null ? null : file.getJarId()
                    );
                }
                if (caller == null) {
                    continue;
                }
                MethodReference.Handle callTarget = new MethodReference.Handle(
                        implHandle.getClassReference(),
                        Opcodes.INVOKEDYNAMIC,
                        implHandle.getName(),
                        implHandle.getDesc()
                );
                if (addEdge(
                        context.methodCalls,
                        context.methodCallMeta,
                        caller,
                        callTarget,
                        MethodCallMeta.TYPE_INDY,
                        MethodCallMeta.CONF_MEDIUM,
                        lambda.reason()
                )) {
                    added++;
                }
                MethodReference.Handle samCaller = resolveMethodHandle(
                        lookup,
                        context.methodMap,
                        lambda.samOwner(),
                        lambda.samName(),
                        lambda.samDesc(),
                        null
                );
                if (samCaller != null && addEdge(
                        context.methodCalls,
                        context.methodCallMeta,
                        samCaller,
                        callTarget,
                        MethodCallMeta.TYPE_INDY,
                        MethodCallMeta.CONF_MEDIUM,
                        lambda.reason()
                )) {
                    added++;
                }
            }
        }
        return added;
    }

    private static BuildFactSnapshot.MethodInvokeHint toInvokeHint(int insnIndex,
                                                                   TargetResolve resolved) {
        if (resolved == null || resolved.targets() == null || resolved.targets().isEmpty()) {
            return null;
        }
        return new BuildFactSnapshot.MethodInvokeHint(
                insnIndex,
                resolved.targets(),
                resolved.reason(),
                resolved.tier(),
                resolved.imprecise()
        );
    }

    private static BuildFactSnapshot.ReflectionHintDiagnostic toDiagnostic(int insnIndex,
                                                                           String kind,
                                                                           TargetResolve resolved) {
        if (resolved == null || !resolved.thresholdExceeded()) {
            return null;
        }
        return new BuildFactSnapshot.ReflectionHintDiagnostic(
                insnIndex,
                kind,
                resolved.tier(),
                resolved.reason(),
                resolved.candidateCount(),
                true
        );
    }

    private static HintStats summarizeHints(BuildFactSnapshot.MethodReflectionHints hints) {
        if (hints == null || hints.isEmpty()) {
            return HintStats.empty();
        }
        int constSites = 0;
        int logSites = 0;
        int castSites = 0;
        int unknownSites = 0;
        int impreciseSites = 0;
        for (BuildFactSnapshot.MethodInvokeHint hint : collectAllHints(hints)) {
            if (hint == null) {
                continue;
            }
            switch (hint.tier()) {
                case CONST -> constSites++;
                case LOG -> logSites++;
                case CAST -> castSites++;
                case UNKNOWN -> unknownSites++;
            }
            if (hint.imprecise()) {
                impreciseSites++;
            }
        }
        int thresholdExceededSites = 0;
        for (BuildFactSnapshot.ReflectionHintDiagnostic diagnostic : hints.diagnostics()) {
            if (diagnostic == null) {
                continue;
            }
            switch (diagnostic.tier()) {
                case CONST -> constSites++;
                case LOG -> logSites++;
                case CAST -> castSites++;
                case UNKNOWN -> unknownSites++;
            }
            if (diagnostic.thresholdExceeded()) {
                thresholdExceededSites++;
                impreciseSites++;
            }
        }
        return new HintStats(
                constSites,
                logSites,
                castSites,
                unknownSites,
                impreciseSites,
                thresholdExceededSites
        );
    }

    private static List<BuildFactSnapshot.MethodInvokeHint> collectAllHints(BuildFactSnapshot.MethodReflectionHints hints) {
        if (hints == null) {
            return List.of();
        }
        ArrayList<BuildFactSnapshot.MethodInvokeHint> out = new ArrayList<>();
        out.addAll(hints.classNewInstanceHints().values());
        out.addAll(hints.reflectionInvokeHints().values());
        out.addAll(hints.methodHandleInvokeHints().values());
        return out;
    }

    private static MethodResolveResult applyHintEdges(MethodReference.Handle caller,
                                                      MethodNode mn,
                                                      BuildFactSnapshot.MethodReflectionHints reflectionHints,
                                                      Map<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
                                                      Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                                                      Map<MethodReference.Handle, MethodReference> methodMap) {
        if (caller == null
                || mn == null
                || mn.instructions == null
                || mn.instructions.size() == 0
                || reflectionHints == null
                || reflectionHints.isEmpty()) {
            return MethodResolveResult.empty();
        }
        int reflectionEdges = 0;
        int methodHandleEdges = 0;
        for (int i = 0, n = mn.instructions.size(); i < n; i++) {
            AbstractInsnNode insn = mn.instructions.get(i);
            if (!(insn instanceof MethodInsnNode invoke)) {
                continue;
            }
            if (isClassNewInstance(invoke)) {
                reflectionEdges += appendHintEdges(
                        caller,
                        reflectionHints.classNewInstanceHint(i),
                        methodCalls,
                        methodCallMeta,
                        methodMap,
                        MethodCallMeta.TYPE_REFLECTION
                );
                continue;
            }
            if (isReflectionInvoke(invoke)) {
                reflectionEdges += appendHintEdges(
                        caller,
                        reflectionHints.reflectionInvokeHint(i),
                        methodCalls,
                        methodCallMeta,
                        methodMap,
                        MethodCallMeta.TYPE_REFLECTION
                );
                continue;
            }
            if (isMethodHandleInvoke(invoke)) {
                methodHandleEdges += appendHintEdges(
                        caller,
                        reflectionHints.methodHandleInvokeHint(i),
                        methodCalls,
                        methodCallMeta,
                        methodMap,
                        MethodCallMeta.TYPE_METHOD_HANDLE
                );
            }
        }
        return new MethodResolveResult(reflectionEdges, methodHandleEdges);
    }

    private static int appendHintEdges(MethodReference.Handle caller,
                                       BuildFactSnapshot.MethodInvokeHint hint,
                                       Map<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
                                       Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                                       Map<MethodReference.Handle, MethodReference> methodMap,
                                       String edgeType) {
        if (caller == null
                || hint == null
                || hint.targets() == null
                || hint.targets().isEmpty()
                || methodCalls == null
                || methodCallMeta == null) {
            return 0;
        }
        int added = 0;
        for (MethodReference.Handle target : hint.targets()) {
            if (target == null) {
                continue;
            }
            int fallbackOpcode = "<init>".equals(target.getName())
                    ? Opcodes.INVOKESPECIAL
                    : Opcodes.INVOKEVIRTUAL;
            MethodReference.Handle callTarget = withSyntheticOpcode(target, methodMap, fallbackOpcode);
            if (addEdge(
                    methodCalls,
                    methodCallMeta,
                    caller,
                    callTarget,
                    edgeType,
                    MethodCallMeta.CONF_LOW,
                    appendHintEvidence(hint)
            )) {
                added++;
            }
        }
        return added;
    }

    private static String appendHintEvidence(BuildFactSnapshot.MethodInvokeHint hint) {
        if (hint == null) {
            return "";
        }
        String reason = safe(hint.reason());
        String tier = hint.tier() == null ? "unknown" : hint.tier().name().toLowerCase(Locale.ROOT);
        String evidence = reason.isBlank() ? "tier=" + tier : reason + ";tier=" + tier;
        if (hint.imprecise()) {
            evidence += ";imprecise=1";
        }
        return evidence;
    }

    private static MethodResolveResult resolveInMethod(MethodReference.Handle caller,
                                                       MethodNode mn,
                                                       String owner,
                                                       ClassNode classNode,
                                                       Frame<SourceValue>[] precomputedFrames,
                                                       BuildFactSnapshot.MethodConstraintFacts methodConstraints,
                                                       Map<String, String> staticStrings,
                                                       Map<String, BuildFactSnapshot.AliasValueFact> instanceFieldFacts,
                                                       Map<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
                                                       Map<MethodReference.Handle, MethodReference> methodMap,
                                                       Map<MethodCallKey, MethodCallMeta> methodCallMeta) {
        try {
            Frame<SourceValue>[] frames = precomputedFrames;
            if (frames == null) {
                Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());
                frames = analyzer.analyze(owner, mn);
            }
            ResolveContext ctx = new ResolveContext(
                    frames,
                    mn.instructions,
                    classNode,
                    staticStrings,
                    instanceFieldFacts,
                    methodConstraints
            );
            int reflectionEdges = 0;
            int methodHandleEdges = 0;
            for (int i = 0, n = mn.instructions.size(); i < n; i++) {
                AbstractInsnNode insn = mn.instructions.get(i);
                if (!(insn instanceof MethodInsnNode invoke)) {
                    continue;
                }
                if (isClassNewInstance(invoke)) {
                    Frame<SourceValue> frame = frames[i];
                    if (frame == null) {
                        continue;
                    }
                    int objIndex = frame.getStackSize() - 1;
                    if (objIndex < 0) {
                        continue;
                    }
                    ResolvedString classInfo = resolveClassNameWithReason(frame.getStack(objIndex), ctx);
                    if (classInfo == null || classInfo.value == null) {
                        continue;
                    }
                    MethodReference.Handle target = findUniqueMethod(
                            methodMap,
                            classInfo.value,
                            "<init>",
                            "()V"
                    );
                    if (target == null) {
                        continue;
                    }
                    MethodReference.Handle callTarget = withSyntheticOpcode(target, methodMap, Opcodes.INVOKESPECIAL);
                    if (addEdge(
                            methodCalls,
                            methodCallMeta,
                            caller,
                            callTarget,
                            MethodCallMeta.TYPE_REFLECTION,
                            MethodCallMeta.CONF_LOW,
                            REASON_CLASS_NEW_INSTANCE + "_" + classInfo.reason
                    )) {
                        reflectionEdges++;
                    }
                    continue;
                }
                if (isReflectionInvoke(invoke)) {
                    Frame<SourceValue> frame = frames[i];
                    if (frame == null) {
                        continue;
                    }
                    int argSlots = 0;
                    for (Type type : Type.getArgumentTypes(invoke.desc)) {
                        argSlots += type.getSize();
                    }
                    int objIndex = frame.getStackSize() - argSlots - 1;
                    if (objIndex < 0) {
                        continue;
                    }
                    MethodInsnNode creator = findSingleCreator(frame.getStack(objIndex), ctx, 0);
                    if (creator == null) {
                        continue;
                    }
                    TargetResolve resolved = resolveTarget(creator, invoke, ctx, methodMap);
                    if (resolved == null || resolved.targets() == null || resolved.targets().isEmpty()) {
                        continue;
                    }
                    for (MethodReference.Handle target : resolved.targets()) {
                        MethodReference.Handle callTarget =
                                withSyntheticOpcode(target, methodMap, Opcodes.INVOKEVIRTUAL);
                        if (addEdge(
                                methodCalls,
                                methodCallMeta,
                                caller,
                                callTarget,
                                MethodCallMeta.TYPE_REFLECTION,
                                MethodCallMeta.CONF_LOW,
                                resolved.reason()
                        )) {
                            reflectionEdges++;
                        }
                    }
                    continue;
                }
                if (!isMethodHandleInvoke(invoke)) {
                    continue;
                }
                Frame<SourceValue> frame = frames[i];
                if (frame == null) {
                    continue;
                }
                int argCount = Type.getArgumentTypes(invoke.desc).length;
                int objIndex = frame.getStackSize() - argCount - 1;
                if (objIndex < 0) {
                    continue;
                }
                MethodInsnNode creator = findSingleHandleCreator(frame.getStack(objIndex), ctx, 0);
                if (creator == null) {
                    continue;
                }
                TargetResolve resolved = resolveHandleTarget(creator, ctx, methodMap);
                if (resolved == null || resolved.targets == null || resolved.targets.isEmpty()) {
                    continue;
                }
                for (MethodReference.Handle target : resolved.targets) {
                    int fallbackOpcode = "<init>".equals(target.getName())
                            ? Opcodes.INVOKESPECIAL
                            : Opcodes.INVOKEVIRTUAL;
                    MethodReference.Handle callTarget = withSyntheticOpcode(target, methodMap, fallbackOpcode);
                    if (addEdge(
                            methodCalls,
                            methodCallMeta,
                            caller,
                            callTarget,
                            MethodCallMeta.TYPE_METHOD_HANDLE,
                            MethodCallMeta.CONF_LOW,
                            resolved.reason
                    )) {
                        methodHandleEdges++;
                    }
                }
            }
            return new MethodResolveResult(reflectionEdges, methodHandleEdges);
        } catch (Exception ex) {
            logger.warn("bytecode-mainline reflection analyze failed: {}.{}", owner, mn.name);
            return MethodResolveResult.empty();
        }
    }

    private static LambdaTarget resolveLambdaTarget(InvokeDynamicInsnNode indy) {
        if (indy == null || indy.bsm == null || !LMF_OWNER.equals(indy.bsm.getOwner())) {
            return null;
        }
        String bsmName = indy.bsm.getName();
        if (!"metafactory".equals(bsmName) && !"altMetafactory".equals(bsmName)) {
            return null;
        }
        if (indy.bsmArgs == null || indy.bsmArgs.length < 3) {
            return null;
        }
        if (!(indy.bsmArgs[0] instanceof Type samType)
                || !(indy.bsmArgs[1] instanceof Handle impl)
                || !(indy.bsmArgs[2] instanceof Type instantiatedType)) {
            return null;
        }
        if (samType.getSort() != Type.METHOD || instantiatedType.getSort() != Type.METHOD) {
            return null;
        }
        Type fiType = Type.getReturnType(indy.desc);
        if (fiType.getSort() != Type.OBJECT) {
            return null;
        }
        return new LambdaTarget(
                lambdaReason(impl),
                fiType.getInternalName(),
                indy.name,
                samType.getDescriptor(),
                impl.getOwner(),
                impl.getName(),
                impl.getDesc()
        );
    }

    private static boolean containsReflectionInvoke(InsnList instructions) {
        for (int i = 0, n = instructions.size(); i < n; i++) {
            AbstractInsnNode insn = instructions.get(i);
            if (!(insn instanceof MethodInsnNode mi)) {
                continue;
            }
            if (isReflectionInvoke(mi) || isClassNewInstance(mi) || isMethodHandleInvoke(mi)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isReflectionInvoke(MethodInsnNode mi) {
        if (mi == null) {
            return false;
        }
        if (METHOD_OWNER.equals(mi.owner)
                && "invoke".equals(mi.name)
                && "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;".equals(mi.desc)) {
            return true;
        }
        return CTOR_OWNER.equals(mi.owner)
                && "newInstance".equals(mi.name)
                && "([Ljava/lang/Object;)Ljava/lang/Object;".equals(mi.desc);
    }

    private static boolean isClassNewInstance(MethodInsnNode mi) {
        return mi != null
                && CLASS_OWNER.equals(mi.owner)
                && "newInstance".equals(mi.name)
                && "()Ljava/lang/Object;".equals(mi.desc);
    }

    private static boolean isMethodHandleInvoke(MethodInsnNode mi) {
        if (mi == null || !MH_OWNER.equals(mi.owner)) {
            return false;
        }
        if (!"invoke".equals(mi.name) && !"invokeExact".equals(mi.name) && !"invokeWithArguments".equals(mi.name)) {
            return false;
        }
        return mi.desc != null && mi.desc.startsWith("(");
    }

    private static MethodInsnNode findSingleCreator(SourceValue value,
                                                    ResolveContext ctx,
                                                    int depth) {
        if (value == null || value.insns == null || value.insns.isEmpty() || depth > MAX_CONST_DEPTH) {
            return null;
        }
        MethodInsnNode found = null;
        for (AbstractInsnNode src : value.insns) {
            AbstractInsnNode normalized = canonicalInsn(src, ctx);
            if (normalized instanceof VarInsnNode varInsn && varInsn.getOpcode() == Opcodes.ALOAD && ctx != null) {
                int loadIndex = ctx.instructions.indexOf(varInsn);
                int storeIndex = findPreviousAstore(ctx, loadIndex, varInsn.var);
                if (storeIndex >= 0 && ctx.frames[storeIndex] != null) {
                    Frame<SourceValue> storeFrame = ctx.frames[storeIndex];
                    if (storeFrame.getStackSize() > 0) {
                        MethodInsnNode nested = findSingleCreator(
                                storeFrame.getStack(storeFrame.getStackSize() - 1),
                                ctx,
                                depth + 1
                        );
                        if (nested != null) {
                            if (found != null && found != nested) {
                                return null;
                            }
                            found = nested;
                        }
                    }
                }
                continue;
            }
            if (!(normalized instanceof MethodInsnNode mi)) {
                continue;
            }
            if (!isGetMethod(mi) && !isGetConstructor(mi)) {
                continue;
            }
            if (found != null && found != mi) {
                return null;
            }
            found = mi;
        }
        return found;
    }

    private static MethodInsnNode findSingleHandleCreator(SourceValue value,
                                                          ResolveContext ctx,
                                                          int depth) {
        if (value == null || value.insns == null || value.insns.isEmpty() || depth > MAX_CONST_DEPTH) {
            return null;
        }
        MethodInsnNode found = null;
        for (AbstractInsnNode src : value.insns) {
            AbstractInsnNode normalized = canonicalInsn(src, ctx);
            if (normalized instanceof VarInsnNode varInsn && varInsn.getOpcode() == Opcodes.ALOAD && ctx != null) {
                int loadIndex = ctx.instructions.indexOf(varInsn);
                int storeIndex = findPreviousAstore(ctx, loadIndex, varInsn.var);
                if (storeIndex >= 0 && ctx.frames[storeIndex] != null) {
                    Frame<SourceValue> storeFrame = ctx.frames[storeIndex];
                    if (storeFrame.getStackSize() > 0) {
                        MethodInsnNode nested = findSingleHandleCreator(
                                storeFrame.getStack(storeFrame.getStackSize() - 1),
                                ctx,
                                depth + 1
                        );
                        if (nested != null) {
                            if (found != null && found != nested) {
                                return null;
                            }
                            found = nested;
                        }
                    }
                }
                continue;
            }
            if (!(normalized instanceof MethodInsnNode mi)) {
                continue;
            }
            MethodInsnNode adapted = resolveHandleAdapterCreator(mi, ctx, depth + 1);
            if (adapted != null) {
                if (found != null && found != adapted) {
                    return null;
                }
                found = adapted;
                continue;
            }
            if (!isLookupFind(mi)) {
                continue;
            }
            if (found != null && found != mi) {
                return null;
            }
            found = mi;
        }
        return found;
    }

    private static TargetResolve resolveTarget(MethodInsnNode creator,
                                               MethodInsnNode invoke,
                                               ResolveContext ctx,
                                               Map<MethodReference.Handle, MethodReference> methodMap) {
        int idx = ctx.instructions.indexOf(creator);
        if (idx < 0) {
            return null;
        }
        Frame<SourceValue> frame = ctx.frames[idx];
        if (frame == null) {
            return null;
        }
        int argCount = Type.getArgumentTypes(creator.desc).length;
        int stackSize = frame.getStackSize();
        int objIndex = stackSize - argCount - 1;
        if (objIndex < 0) {
            return null;
        }
        ResolvedString classInfo = resolveClassNameForReflection(frame.getStack(objIndex), ctx);
        if ((classInfo == null || classInfo.value == null) && invoke != null) {
            classInfo = resolveReceiverTypeAtInvoke(invoke, ctx);
        }
        if (classInfo == null || classInfo.value == null) {
            return null;
        }
        List<Type> params;
        String methodName;
        ResolvedString nameInfo = null;
        if (isGetMethod(creator)) {
            if (argCount != 2) {
                return null;
            }
            nameInfo = resolveStringOperandForReflection(frame.getStack(stackSize - argCount), ctx, 0);
            if (nameInfo == null || nameInfo.value == null || nameInfo.value.isEmpty()) {
                return null;
            }
            params = resolveClassArray(frame.getStack(stackSize - argCount + 1), ctx);
            methodName = nameInfo.value;
        } else if (isGetConstructor(creator)) {
            params = resolveClassArray(frame.getStack(stackSize - argCount), ctx);
            methodName = "<init>";
        } else {
            return null;
        }
        String reason = buildReflectionReason(classInfo, nameInfo);
        List<MethodReference.Handle> targets = params == null
                ? findMethodsByName(methodMap, classInfo.value, methodName)
                : findMethods(methodMap, classInfo.value, methodName, params);
        return buildTargetResolve(targets, reason);
    }

    private static TargetResolve resolveHandleTarget(MethodInsnNode creator,
                                                     ResolveContext ctx,
                                                     Map<MethodReference.Handle, MethodReference> methodMap) {
        int idx = ctx.instructions.indexOf(creator);
        if (idx < 0) {
            return null;
        }
        Frame<SourceValue> frame = ctx.frames[idx];
        if (frame == null) {
            return null;
        }
        String lookupName = creator.name;
        int argCount = Type.getArgumentTypes(creator.desc).length;
        int base = frame.getStackSize() - argCount;
        if (base < 0) {
            return null;
        }
        ResolvedString classInfo;
        String methodName;
        MethodTypeInfo mt;
        if ("findConstructor".equals(lookupName)) {
            classInfo = resolveClassNameForReflection(frame.getStack(base), ctx);
            mt = resolveMethodType(frame.getStack(base + 1), ctx);
            if (classInfo == null || classInfo.value == null || mt == null || mt.returnType.getSort() != Type.VOID) {
                return null;
            }
            methodName = "<init>";
        } else if ("findSpecial".equals(lookupName)) {
            if (argCount != 4) {
                return null;
            }
            classInfo = resolveClassNameForReflection(frame.getStack(base), ctx);
            ResolvedString nameInfo = resolveStringOperandForReflection(frame.getStack(base + 1), ctx, 0);
            mt = resolveMethodType(frame.getStack(base + 2), ctx);
            if (classInfo == null || classInfo.value == null || nameInfo == null || nameInfo.value == null || mt == null) {
                return null;
            }
            methodName = nameInfo.value;
        } else {
            if (argCount != 3) {
                return null;
            }
            classInfo = resolveClassNameForReflection(frame.getStack(base), ctx);
            ResolvedString nameInfo = resolveStringOperandForReflection(frame.getStack(base + 1), ctx, 0);
            mt = resolveMethodType(frame.getStack(base + 2), ctx);
            if (classInfo == null || classInfo.value == null || nameInfo == null || nameInfo.value == null || mt == null) {
                return null;
            }
            methodName = nameInfo.value;
        }
        String desc = Type.getMethodDescriptor(mt.returnType, mt.paramTypes.toArray(new Type[0]));
        ArrayList<MethodReference.Handle> out = new ArrayList<>();
        for (MethodReference.Handle handle : methodMap.keySet()) {
            if (handle == null || handle.getClassReference() == null) {
                continue;
            }
            if (!classInfo.value.equals(handle.getClassReference().getName())) {
                continue;
            }
            if (!methodName.equals(handle.getName()) || !desc.equals(handle.getDesc())) {
                continue;
            }
            out.add(handle);
        }
        return buildTargetResolve(out, REASON_METHOD_HANDLE + "_" + lookupName.toLowerCase(Locale.ROOT));
    }

    private static boolean isGetMethod(MethodInsnNode mi) {
        return mi != null
                && CLASS_OWNER.equals(mi.owner)
                && ("getMethod".equals(mi.name) || "getDeclaredMethod".equals(mi.name))
                && "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;".equals(mi.desc);
    }

    private static boolean isGetConstructor(MethodInsnNode mi) {
        return mi != null
                && CLASS_OWNER.equals(mi.owner)
                && ("getConstructor".equals(mi.name) || "getDeclaredConstructor".equals(mi.name))
                && "([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;".equals(mi.desc);
    }

    private static boolean isLookupFind(MethodInsnNode mi) {
        if (mi == null || !LOOKUP_OWNER.equals(mi.owner)) {
            return false;
        }
        if ("findVirtual".equals(mi.name)
                && "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;".equals(mi.desc)) {
            return true;
        }
        if ("findStatic".equals(mi.name)
                && "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;".equals(mi.desc)) {
            return true;
        }
        if ("findSpecial".equals(mi.name)
                && "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;".equals(mi.desc)) {
            return true;
        }
        return "findConstructor".equals(mi.name)
                && "(Ljava/lang/Class;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;".equals(mi.desc);
    }

    private static MethodInsnNode resolveHandleAdapterCreator(MethodInsnNode adapter,
                                                              ResolveContext ctx,
                                                              int depth) {
        if (adapter == null || ctx == null || depth > MAX_CONST_DEPTH || !isMethodHandleAdapter(adapter)) {
            return null;
        }
        int idx = ctx.instructions.indexOf(adapter);
        if (idx < 0 || idx >= ctx.frames.length) {
            return null;
        }
        Frame<SourceValue> frame = ctx.frames[idx];
        if (frame == null || frame.getStackSize() <= 0) {
            return null;
        }
        SourceValue baseHandle = resolveAdapterBaseHandle(adapter, frame);
        if (baseHandle == null) {
            return null;
        }
        return findSingleHandleCreator(baseHandle, ctx, depth + 1);
    }

    private static boolean isMethodHandleAdapter(MethodInsnNode mi) {
        if (mi == null || mi.desc == null || !mi.desc.endsWith(")Ljava/lang/invoke/MethodHandle;")) {
            return false;
        }
        if (MH_OWNER.equals(mi.owner)) {
            return "bindTo".equals(mi.name)
                    || "asType".equals(mi.name)
                    || "asCollector".equals(mi.name)
                    || "asSpreader".equals(mi.name)
                    || "asVarargsCollector".equals(mi.name);
        }
        if ("java/lang/invoke/MethodHandles".equals(mi.owner)) {
            return "insertArguments".equals(mi.name)
                    || "explicitCastArguments".equals(mi.name)
                    || "dropArguments".equals(mi.name)
                    || "permuteArguments".equals(mi.name);
        }
        return false;
    }

    private static SourceValue resolveAdapterBaseHandle(MethodInsnNode adapter,
                                                        Frame<SourceValue> frame) {
        if (adapter == null || frame == null) {
            return null;
        }
        Type[] argTypes = Type.getArgumentTypes(adapter.desc);
        int argSlots = 0;
        for (Type type : argTypes) {
            argSlots += type.getSize();
        }
        if (MH_OWNER.equals(adapter.owner)) {
            int receiverIndex = frame.getStackSize() - argSlots - 1;
            if (receiverIndex < 0 || receiverIndex >= frame.getStackSize()) {
                return null;
            }
            return frame.getStack(receiverIndex);
        }
        if (!"java/lang/invoke/MethodHandles".equals(adapter.owner)) {
            return null;
        }
        int base = frame.getStackSize() - argSlots;
        if (base < 0 || base >= frame.getStackSize()) {
            return null;
        }
        return frame.getStack(base);
    }

    private static List<MethodReference.Handle> findMethods(Map<MethodReference.Handle, MethodReference> methodMap,
                                                            String className,
                                                            String methodName,
                                                            List<Type> params) {
        if (className == null || methodName == null || params == null) {
            return null;
        }
        ArrayList<MethodReference.Handle> out = new ArrayList<>();
        for (MethodReference.Handle handle : methodMap.keySet()) {
            if (handle == null || handle.getClassReference() == null) {
                continue;
            }
            if (!className.equals(handle.getClassReference().getName())
                    || !methodName.equals(handle.getName())) {
                continue;
            }
            Type[] args = Type.getArgumentTypes(handle.getDesc());
            if (args.length != params.size()) {
                continue;
            }
            boolean match = true;
            for (int i = 0; i < args.length; i++) {
                if (!args[i].equals(params.get(i))) {
                    match = false;
                    break;
                }
            }
            if (match) {
                out.add(handle);
            }
        }
        return out.isEmpty() ? null : out;
    }

    private static List<MethodReference.Handle> findMethodsByName(Map<MethodReference.Handle, MethodReference> methodMap,
                                                                  String className,
                                                                  String methodName) {
        if (className == null || methodName == null) {
            return null;
        }
        ArrayList<MethodReference.Handle> out = new ArrayList<>();
        for (MethodReference.Handle handle : methodMap.keySet()) {
            if (handle == null || handle.getClassReference() == null) {
                continue;
            }
            if (!className.equals(handle.getClassReference().getName())
                    || !methodName.equals(handle.getName())) {
                continue;
            }
            out.add(handle);
        }
        return out.isEmpty() ? null : out;
    }

    private static MethodReference.Handle findUniqueMethod(Map<MethodReference.Handle, MethodReference> methodMap,
                                                           String className,
                                                           String methodName,
                                                           String methodDesc) {
        if (methodMap == null || methodMap.isEmpty()) {
            return null;
        }
        MethodReference.Handle found = null;
        for (MethodReference.Handle handle : methodMap.keySet()) {
            if (handle == null || handle.getClassReference() == null) {
                continue;
            }
            if (!safe(className).equals(safe(handle.getClassReference().getName()))
                    || !safe(methodName).equals(safe(handle.getName()))
                    || !safe(methodDesc).equals(safe(handle.getDesc()))) {
                continue;
            }
            if (found != null && !found.equals(handle)) {
                return null;
            }
            found = handle;
        }
        return found;
    }

    private static ResolvedString resolveClassNameWithReason(SourceValue value,
                                                             ResolveContext ctx) {
        AbstractInsnNode src = getSingleInsn(value, ctx);
        if (src instanceof LdcInsnNode ldc && ldc.cst instanceof Type type) {
            return new ResolvedString(type.getInternalName(), REASON_CLASS_CONST);
        }
        if (src instanceof VarInsnNode varInsn) {
            ResolvedString local = resolveClassNameFromLocal(varInsn, ctx, 0);
            if (local == null || local.value == null) {
                return null;
            }
            String normalized = normalizeClassName(local.value);
            return normalized == null ? null : new ResolvedString(normalized, combineReasons(REASON_LOCAL, local.reason));
        }
        if (src instanceof MethodInsnNode mi) {
            if (isObjectGetClass(mi)) {
                return resolveObjectGetClassName(mi, ctx, 0);
            }
            if (!isForName(mi) && !isLoadClass(mi)) {
                return null;
            }
            ResolvedString name = resolveClassNameFromCallWithReason(mi, ctx, 0);
            if (name == null || name.value == null) {
                return null;
            }
            String normalized = normalizeClassName(name.value);
            return normalized == null ? null : new ResolvedString(normalized, REASON_FOR_NAME + "_" + name.reason);
        }
        if (src instanceof FieldInsnNode fin && fin.getOpcode() == Opcodes.GETFIELD) {
            ResolvedString type = resolveInstanceFieldObjectType(fin, ctx);
            if (type == null || type.value == null) {
                return null;
            }
            String normalized = normalizeClassName(type.value);
            return normalized == null ? null : new ResolvedString(normalized, type.reason);
        }
        if (src instanceof InvokeDynamicInsnNode || src instanceof FieldInsnNode) {
            ResolvedString name = resolveStringConstantWithReason(value, ctx, 0);
            if (name == null || name.value == null) {
                return null;
            }
            String normalized = normalizeClassName(name.value);
            return normalized == null ? null : new ResolvedString(normalized, name.reason);
        }
        return null;
    }

    private static boolean isForName(MethodInsnNode mi) {
        return mi != null
                && CLASS_OWNER.equals(mi.owner)
                && "forName".equals(mi.name)
                && ("(Ljava/lang/String;)Ljava/lang/Class;".equals(mi.desc)
                || "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;".equals(mi.desc));
    }

    private static boolean isLoadClass(MethodInsnNode mi) {
        return mi != null
                && "loadClass".equals(mi.name)
                && ("(Ljava/lang/String;)Ljava/lang/Class;".equals(mi.desc)
                || "(Ljava/lang/String;Z)Ljava/lang/Class;".equals(mi.desc))
                && ("java/lang/ClassLoader".equals(mi.owner)
                || (mi.owner != null && mi.owner.endsWith("ClassLoader")));
    }

    private static ResolvedString resolveClassNameFromCallWithReason(MethodInsnNode mi,
                                                                     ResolveContext ctx,
                                                                     int depth) {
        int idx = ctx.instructions.indexOf(mi);
        if (idx < 0 || ctx.frames[idx] == null) {
            return null;
        }
        Frame<SourceValue> frame = ctx.frames[idx];
        int argCount = Type.getArgumentTypes(mi.desc).length;
        int argIndex = frame.getStackSize() - argCount;
        if (argIndex < 0) {
            return null;
        }
        ResolvedString className = resolveStringOperandForReflection(frame.getStack(argIndex), ctx, depth);
        if (className == null || className.value == null) {
            AbstractInsnNode argSrc = getSingleInsn(frame.getStack(argIndex), ctx);
            className = resolveStableStringFact(argSrc, ctx, depth);
            if (className == null || className.value == null) {
                className = resolveDirectStringFact(argSrc, ctx, depth);
            }
            if ((className == null || className.value == null) && argSrc instanceof VarInsnNode) {
                AbstractInsnNode stored = getStoredSourceInsn(argSrc, ctx);
                className = resolveStableStringFact(stored, ctx, depth);
                if (className == null || className.value == null) {
                    className = resolveDirectStringFact(stored, ctx, depth);
                }
                if (className != null) {
                    className = new ResolvedString(className.value, combineReasons(REASON_LOCAL, className.reason));
                }
            }
        }
        if (className == null || className.value == null) {
            return null;
        }
        String reason = className.reason;
        if (isForName(mi) && "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;".equals(mi.desc)) {
            if (argIndex + 2 < frame.getStackSize()) {
                String loaderReason = resolveClassLoaderReason(frame.getStack(argIndex + 2), ctx, depth + 1);
                reason = combineReasons(reason, REASON_CLASS_LOADER + "_" + loaderReason);
            }
        } else if (isLoadClass(mi)) {
            int recvIndex = frame.getStackSize() - argCount - 1;
            if (recvIndex >= 0) {
                String loaderReason = resolveClassLoaderReason(frame.getStack(recvIndex), ctx, depth + 1);
                reason = combineReasons(reason, REASON_CLASS_LOADER + "_" + loaderReason);
            }
        }
        return new ResolvedString(className.value, reason);
    }

    private static ResolvedString resolveClassNameFromLocal(VarInsnNode load,
                                                            ResolveContext ctx,
                                                            int depth) {
        if (load == null || ctx == null || depth > MAX_CONST_DEPTH || load.getOpcode() != Opcodes.ALOAD) {
            return null;
        }
        int loadIndex = ctx.instructions.indexOf(load);
        int storeIndex = findPreviousAstore(ctx, loadIndex, load.var);
        if (storeIndex < 0 || ctx.frames[storeIndex] == null) {
            return null;
        }
        Frame<SourceValue> frame = ctx.frames[storeIndex];
        if (frame.getStackSize() <= 0) {
            return null;
        }
        ResolvedString resolved = resolveClassNameWithReason(frame.getStack(frame.getStackSize() - 1), ctx);
        if (resolved == null) {
            return null;
        }
        return new ResolvedString(resolved.value, combineReasons(REASON_LOCAL, resolved.reason));
    }

    private static boolean isObjectGetClass(MethodInsnNode mi) {
        return mi != null
                && "getClass".equals(mi.name)
                && "()Ljava/lang/Class;".equals(mi.desc);
    }

    private static ResolvedString resolveObjectGetClassName(MethodInsnNode mi,
                                                            ResolveContext ctx,
                                                            int depth) {
        if (mi == null || ctx == null || depth > MAX_CONST_DEPTH) {
            return null;
        }
        int idx = ctx.instructions.indexOf(mi);
        if (idx < 0 || ctx.frames[idx] == null) {
            return null;
        }
        Frame<SourceValue> frame = ctx.frames[idx];
        int recvIndex = frame.getStackSize() - 1;
        if (recvIndex < 0) {
            return null;
        }
        ResolvedString objectType = resolveRuntimeObjectType(frame.getStack(recvIndex), ctx, depth + 1);
        if (objectType == null || objectType.value == null) {
            return null;
        }
        String normalized = normalizeClassName(objectType.value);
        return normalized == null ? null : new ResolvedString(normalized, combineReasons("object_get_class", objectType.reason));
    }

    private static ResolvedString resolveRuntimeObjectType(SourceValue value,
                                                           ResolveContext ctx,
                                                           int depth) {
        if (value == null || value.insns == null || value.insns.isEmpty() || depth > MAX_CONST_DEPTH) {
            return null;
        }
        ResolvedString found = null;
        for (AbstractInsnNode src : value.insns) {
            ResolvedString candidate = resolveRuntimeObjectTypeFromInsn(canonicalInsn(src, ctx), ctx, depth + 1);
            if (candidate == null || candidate.value == null) {
                continue;
            }
            if (found == null) {
                found = candidate;
                continue;
            }
            if (!safe(found.value).equals(safe(candidate.value))) {
                return null;
            }
        }
        return found;
    }

    private static ResolvedString resolveRuntimeObjectTypeFromInsn(AbstractInsnNode src,
                                                                   ResolveContext ctx,
                                                                   int depth) {
        if (src == null || ctx == null || depth > MAX_CONST_DEPTH) {
            return null;
        }
        if (src instanceof InsnNode insn && isDupInsn(insn.getOpcode())) {
            AbstractInsnNode previous = previousValueProducer(ctx.instructions, ctx.instructions.indexOf(src));
            if (previous != null) {
                return resolveRuntimeObjectTypeFromInsn(previous, ctx, depth + 1);
            }
        }
        if (src instanceof InsnNode insn && insn.getOpcode() == Opcodes.AALOAD) {
            return resolveArrayElementObjectType(insn, ctx, depth + 1);
        }
        if (src instanceof TypeInsnNode typeInsn) {
            if (typeInsn.desc != null && !typeInsn.desc.isBlank()) {
                String reason = typeInsn.getOpcode() == Opcodes.CHECKCAST
                        ? REASON_CAST
                        : REASON_CONST;
                return new ResolvedString(typeInsn.desc, reason);
            }
            return null;
        }
        if (src instanceof FieldInsnNode fieldInsn) {
            if (fieldInsn.getOpcode() == Opcodes.GETFIELD) {
                ResolvedString fieldType = resolveInstanceFieldObjectType(fieldInsn, ctx);
                if (fieldType != null && fieldType.value != null) {
                    return fieldType;
                }
            }
            Type fieldType = Type.getType(fieldInsn.desc);
            if (fieldType.getSort() == Type.OBJECT) {
                return new ResolvedString(fieldType.getInternalName(), REASON_STATIC);
            }
            return null;
        }
        if (src instanceof MethodInsnNode mi) {
            if (isObjectGetClass(mi)) {
                return null;
            }
            Type returnType = Type.getReturnType(mi.desc);
            if (returnType.getSort() == Type.OBJECT) {
                return new ResolvedString(returnType.getInternalName(), REASON_USER_METHOD);
            }
            return null;
        }
        if (src instanceof VarInsnNode varInsn && varInsn.getOpcode() == Opcodes.ALOAD) {
            AbstractInsnNode stored = getStoredSourceInsn(varInsn, ctx);
            if (stored != null) {
                ResolvedString local = resolveRuntimeObjectTypeFromInsn(stored, ctx, depth + 1);
                if (local != null && local.value != null) {
                    return new ResolvedString(local.value, combineReasons(REASON_LOCAL, local.reason));
                }
            }
            if (ctx.classNode != null && varInsn.var == 0) {
                return new ResolvedString(ctx.classNode.name, REASON_LOCAL);
            }
        }
        return null;
    }

    private static boolean isDupInsn(int opcode) {
        return opcode == Opcodes.DUP
                || opcode == Opcodes.DUP_X1
                || opcode == Opcodes.DUP_X2
                || opcode == Opcodes.DUP2
                || opcode == Opcodes.DUP2_X1
                || opcode == Opcodes.DUP2_X2;
    }

    private static AbstractInsnNode previousValueProducer(InsnList instructions,
                                                          int index) {
        if (instructions == null || index <= 0) {
            return null;
        }
        for (int i = index - 1; i >= 0; i--) {
            AbstractInsnNode candidate = instructions.get(i);
            if (candidate == null || candidate.getOpcode() < 0) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    private static String directObjectType(AbstractInsnNode src,
                                           ResolveContext ctx,
                                           int depth) {
        ResolvedString resolved = resolveRuntimeObjectTypeFromInsn(src, ctx, depth);
        return resolved == null ? null : resolved.value;
    }

    private static ResolvedString resolveInstanceFieldString(FieldInsnNode fin,
                                                             ResolveContext ctx) {
        if (fin == null || ctx == null || fin.getOpcode() != Opcodes.GETFIELD) {
            return null;
        }
        BuildFactSnapshot.AliasValueFact facts = ctx.instanceFieldFacts.get(fieldKey(fin.owner, fin.name));
        if (facts == null) {
            return null;
        }
        String value = facts.uniqueStringValue();
        return value == null ? null : new ResolvedString(value, "field_fact");
    }

    private static ResolvedString resolveInstanceFieldObjectType(FieldInsnNode fin,
                                                                 ResolveContext ctx) {
        if (fin == null || ctx == null || fin.getOpcode() != Opcodes.GETFIELD) {
            return null;
        }
        BuildFactSnapshot.AliasValueFact facts = ctx.instanceFieldFacts.get(fieldKey(fin.owner, fin.name));
        if (facts == null) {
            return null;
        }
        String value = facts.uniqueObjectType();
        return value == null ? null : new ResolvedString(value, "field_fact");
    }

    private static ResolvedString resolveArrayElementString(AbstractInsnNode src,
                                                            ResolveContext ctx,
                                                            int depth) {
        BuildFactSnapshot.AliasValueFact facts = resolveArrayElementFacts(src, ctx, depth);
        if (facts == null) {
            return null;
        }
        String value = facts.uniqueStringValue();
        return value == null ? null : new ResolvedString(value, "array_fact");
    }

    private static ResolvedString resolveArrayElementObjectType(AbstractInsnNode src,
                                                                ResolveContext ctx,
                                                                int depth) {
        BuildFactSnapshot.AliasValueFact facts = resolveArrayElementFacts(src, ctx, depth);
        if (facts == null) {
            return null;
        }
        String value = facts.uniqueObjectType();
        return value == null ? null : new ResolvedString(value, "array_fact");
    }

    private static BuildFactSnapshot.AliasValueFact resolveArrayElementFacts(AbstractInsnNode src,
                                                                             ResolveContext ctx,
                                                                             int depth) {
        if (!(src instanceof InsnNode insn)
                || ctx == null
                || ctx.methodConstraints == null
                || insn.getOpcode() != Opcodes.AALOAD
                || depth > MAX_CONST_DEPTH) {
            return null;
        }
        int insnIndex = ctx.instructions.indexOf(insn);
        if (insnIndex < 0 || insnIndex >= ctx.frames.length) {
            return null;
        }
        Frame<SourceValue> frame = ctx.frames[insnIndex];
        if (frame == null || frame.getStackSize() < 2) {
            return null;
        }
        int stackSize = frame.getStackSize();
        Integer elementIndex = resolveStableIntFact(getSingleInsn(frame.getStack(stackSize - 1), ctx), ctx, depth + 1);
        if (elementIndex == null || elementIndex < 0) {
            elementIndex = resolveIntConstant(frame.getStack(stackSize - 1));
        }
        if (elementIndex == null || elementIndex < 0) {
            return null;
        }
        BuildFactSnapshot.AliasValueFact found = null;
        for (Integer arrayVar : resolveArrayVars(frame.getStack(stackSize - 2), ctx, depth + 1)) {
            BuildFactSnapshot.AliasValueFact candidate = ctx.methodConstraints.arrayElementFact(arrayVar, elementIndex);
            if (candidate == null || candidate.isEmpty()) {
                continue;
            }
            if (found == null) {
                found = candidate;
                continue;
            }
            if (!found.equals(candidate)) {
                return null;
            }
        }
        return found;
    }

    private static ResolvedString resolveStringConstantWithReason(SourceValue value,
                                                                  ResolveContext ctx,
                                                                  int depth) {
        ResolvedConst resolved = resolveConstValueWithReason(value, ctx, depth);
        if ((resolved == null || !(resolved.value instanceof String)) && ctx != null && depth <= MAX_CONST_DEPTH) {
            ResolvedString fallback = resolveStringFromInsn(getSingleInsn(value, ctx), ctx, depth + 1);
            if (fallback != null && fallback.value != null) {
                return fallback;
            }
        }
        if (resolved != null && resolved.value instanceof String stringValue) {
            String trimmed = stringValue.trim();
            if (!trimmed.isEmpty()) {
                return new ResolvedString(trimmed, resolved.reason == null ? REASON_UNKNOWN : resolved.reason);
            }
        }
        return null;
    }

    private static ResolvedString resolveStringOperand(SourceValue value,
                                                       ResolveContext ctx,
                                                       int depth) {
        ResolvedString resolved = resolveStringConstantWithReason(value, ctx, depth);
        if (resolved != null && resolved.value != null) {
            return resolved;
        }
        return resolveStringOperandDirect(value, ctx, depth + 1);
    }

    private static ResolvedString resolveStringOperandStrict(SourceValue value,
                                                             ResolveContext ctx,
                                                             int depth) {
        ResolvedString resolved = resolveStringOperand(value, ctx, depth);
        if (resolved != null && resolved.value != null) {
            return resolved;
        }
        return resolveStringSourceStrict(getSingleInsn(value, ctx), ctx, depth + 1);
    }

    private static ResolvedString resolveStringOperandForReflection(SourceValue value,
                                                                    ResolveContext ctx,
                                                                    int depth) {
        ResolvedString stable = resolveStableStringFact(getSingleInsn(value, ctx), ctx, depth);
        if (stable != null && stable.value != null) {
            return stable;
        }
        ResolvedString resolved = resolveStringOperandStrict(value, ctx, depth);
        if (resolved != null && resolved.value != null) {
            return resolved;
        }
        return null;
    }

    private static ResolvedString resolveStringFromSingleSource(SourceValue value,
                                                                ResolveContext ctx,
                                                                int depth) {
        return value == null ? null : resolveStringFromInsn(getSingleInsn(value, ctx), ctx, depth);
    }

    private static ResolvedString resolveStringOperandDirect(SourceValue value,
                                                             ResolveContext ctx,
                                                             int depth) {
        return value == null ? null : resolveStringFromInsn(getSingleInsn(value, ctx), ctx, depth);
    }

    private static ResolvedString resolveStringSourceStrict(AbstractInsnNode src,
                                                            ResolveContext ctx,
                                                            int depth) {
        if (src == null || ctx == null || depth > MAX_CONST_DEPTH) {
            return null;
        }
        if (src instanceof LdcInsnNode ldc && ldc.cst instanceof String stringValue) {
            return normalizeResolvedString(stringValue, REASON_CONST);
        }
        if (src instanceof InsnNode insn && insn.getOpcode() == Opcodes.AALOAD) {
            return resolveArrayElementString(insn, ctx, depth + 1);
        }
        if (src instanceof FieldInsnNode fin && fin.getOpcode() == Opcodes.GETSTATIC) {
            String key = fin.owner + "#" + fin.name;
            return normalizeResolvedString(ctx.staticStrings.get(key), REASON_STATIC);
        }
        if (src instanceof FieldInsnNode fin && fin.getOpcode() == Opcodes.GETFIELD) {
            return resolveInstanceFieldString(fin, ctx);
        }
        if (src instanceof InvokeDynamicInsnNode indy) {
            return normalizeResolvedString(resolveInvokeDynamicString(indy, ctx, depth + 1), REASON_CONCAT);
        }
        if (src instanceof VarInsnNode varInsn && varInsn.getOpcode() == Opcodes.ALOAD) {
            int loadIndex = ctx.instructions.indexOf(varInsn);
            int storeIndex = findPreviousAstore(ctx, loadIndex, varInsn.var);
            if (storeIndex >= 0 && ctx.frames[storeIndex] != null) {
                Frame<SourceValue> storeFrame = ctx.frames[storeIndex];
                if (storeFrame.getStackSize() > 0) {
                    ResolvedString local = resolveStringSourceStrict(
                            getSingleInsn(storeFrame.getStack(storeFrame.getStackSize() - 1), ctx),
                            ctx,
                            depth + 1
                    );
                    if (local != null) {
                        return new ResolvedString(local.value, combineReasons(REASON_LOCAL, local.reason));
                    }
                }
            }
            return null;
        }
        if (!(src instanceof MethodInsnNode mi)) {
            return null;
        }
        if (isStringBuilderToString(mi)) {
            ResolvedString built = normalizeResolvedString(resolveStringBuilderToString(mi, ctx, depth + 1), REASON_STRING_BUILDER);
            if (built != null) {
                return built;
            }
        }
        ResolvedString stringMethod = resolveStringMethodResult(mi, ctx, depth + 1);
        if (stringMethod != null) {
            return stringMethod;
        }
        if ("java/lang/String".equals(mi.owner)
                && "valueOf".equals(mi.name)
                && mi.desc.startsWith("(")
                && mi.desc.endsWith(")Ljava/lang/String;")) {
            ResolvedString valueOf = normalizeResolvedString(resolveStringValueOf(mi, ctx, depth + 1), REASON_VALUE_OF);
            if (valueOf != null) {
                return valueOf;
            }
        }
        ResolvedConst userMethodConst = resolveUserMethodConst(mi, ctx, depth + 1);
        if (userMethodConst != null && userMethodConst.value instanceof String stringValue) {
            return normalizeResolvedString(stringValue, userMethodConst.reason);
        }
        return null;
    }

    private static ResolvedString resolveReflectionStringSource(AbstractInsnNode src,
                                                                ResolveContext ctx,
                                                                int depth) {
        if (src == null || ctx == null || depth > MAX_CONST_DEPTH) {
            return null;
        }
        if (src instanceof LdcInsnNode ldc && ldc.cst instanceof String stringValue) {
            return normalizeResolvedString(stringValue, REASON_CONST);
        }
        if (src instanceof InsnNode insn && insn.getOpcode() == Opcodes.AALOAD) {
            return resolveArrayElementString(insn, ctx, depth + 1);
        }
        if (src instanceof FieldInsnNode fin && fin.getOpcode() == Opcodes.GETSTATIC) {
            String key = fin.owner + "#" + fin.name;
            return normalizeResolvedString(ctx.staticStrings.get(key), REASON_STATIC);
        }
        if (src instanceof FieldInsnNode fin && fin.getOpcode() == Opcodes.GETFIELD) {
            return resolveInstanceFieldString(fin, ctx);
        }
        if (src instanceof InvokeDynamicInsnNode indy) {
            return normalizeResolvedString(resolveInvokeDynamicString(indy, ctx, depth + 1), REASON_CONCAT);
        }
        if (!(src instanceof MethodInsnNode mi)) {
            return null;
        }
        return resolveReflectionStringLeaf(mi, ctx, depth + 1);
    }

    private static ResolvedString resolveReflectionStringLeaf(AbstractInsnNode src,
                                                              ResolveContext ctx,
                                                              int depth) {
        if (src == null || ctx == null || depth > MAX_CONST_DEPTH) {
            return null;
        }
        if (src instanceof LdcInsnNode ldc && ldc.cst instanceof String stringValue) {
            return normalizeResolvedString(stringValue, REASON_CONST);
        }
        if (src instanceof InsnNode insn && insn.getOpcode() == Opcodes.AALOAD) {
            return resolveArrayElementString(insn, ctx, depth + 1);
        }
        if (src instanceof FieldInsnNode fin && fin.getOpcode() == Opcodes.GETSTATIC) {
            String key = fin.owner + "#" + fin.name;
            return normalizeResolvedString(ctx.staticStrings.get(key), REASON_STATIC);
        }
        if (src instanceof FieldInsnNode fin && fin.getOpcode() == Opcodes.GETFIELD) {
            return resolveInstanceFieldString(fin, ctx);
        }
        if (src instanceof InvokeDynamicInsnNode indy) {
            return normalizeResolvedString(resolveInvokeDynamicString(indy, ctx, depth + 1), REASON_CONCAT);
        }
        if (!(src instanceof MethodInsnNode mi)) {
            return null;
        }
        if (isStringBuilderToString(mi)) {
            ResolvedString built = normalizeResolvedString(resolveStringBuilderToString(mi, ctx, depth + 1), REASON_STRING_BUILDER);
            if (built != null) {
                return built;
            }
        }
        ResolvedString stringMethod = resolveStringMethodResult(mi, ctx, depth + 1);
        if (stringMethod != null) {
            return stringMethod;
        }
        if ("java/lang/String".equals(mi.owner)
                && "valueOf".equals(mi.name)
                && mi.desc.startsWith("(")
                && mi.desc.endsWith(")Ljava/lang/String;")) {
            return normalizeResolvedString(resolveStringValueOf(mi, ctx, depth + 1), REASON_VALUE_OF);
        }
        ResolvedConst userMethodConst = resolveUserMethodConst(mi, ctx, depth + 1);
        if (userMethodConst != null && userMethodConst.value instanceof String stringValue) {
            return normalizeResolvedString(stringValue, userMethodConst.reason);
        }
        return null;
    }

    private static AbstractInsnNode getStoredSourceInsn(AbstractInsnNode src,
                                                        ResolveContext ctx) {
        if (!(src instanceof VarInsnNode varInsn)
                || ctx == null
                || ctx.instructions == null
                || varInsn.getOpcode() != Opcodes.ALOAD) {
            return null;
        }
        int loadIndex = ctx.instructions.indexOf(varInsn);
        int storeIndex = findPreviousAstore(ctx, loadIndex, varInsn.var);
        if (storeIndex < 0 || ctx.frames[storeIndex] == null) {
            return null;
        }
        Frame<SourceValue> storeFrame = ctx.frames[storeIndex];
        if (storeFrame.getStackSize() <= 0) {
            return null;
        }
        return getSingleInsn(storeFrame.getStack(storeFrame.getStackSize() - 1), ctx);
    }

    private static Set<Integer> resolveArrayVars(SourceValue value,
                                                 ResolveContext ctx,
                                                 int depth) {
        if (value == null || value.insns == null || value.insns.isEmpty() || ctx == null || depth > MAX_CONST_DEPTH) {
            return Set.of();
        }
        LinkedHashSet<Integer> out = new LinkedHashSet<>();
        for (AbstractInsnNode src : value.insns) {
            collectArrayVar(canonicalInsn(src, ctx), ctx, depth + 1, out);
        }
        return out.isEmpty() ? Set.of() : Set.copyOf(out);
    }

    private static void collectArrayVar(AbstractInsnNode src,
                                        ResolveContext ctx,
                                        int depth,
                                        Set<Integer> out) {
        if (src == null || ctx == null || out == null || depth > MAX_CONST_DEPTH) {
            return;
        }
        if (src instanceof VarInsnNode varInsn && varInsn.getOpcode() == Opcodes.ALOAD) {
            out.add(varInsn.var);
            return;
        }
        if (src instanceof InsnNode insn && isDupInsn(insn.getOpcode())) {
            AbstractInsnNode previous = previousValueProducer(ctx.instructions, ctx.instructions.indexOf(insn));
            collectArrayVar(previous, ctx, depth + 1, out);
            return;
        }
        if (src instanceof VarInsnNode varInsn) {
            collectArrayVar(getStoredSourceInsn(varInsn, ctx), ctx, depth + 1, out);
        }
    }

    private static ResolvedString resolveClassNameForReflection(SourceValue value,
                                                                ResolveContext ctx) {
        AbstractInsnNode src = getSingleInsn(value, ctx);
        ResolvedString direct = resolveClassNameFromSourceDirect(src, ctx);
        if (direct != null && direct.value != null) {
            return direct;
        }
        ResolvedString className = resolveStableStringFact(src, ctx, 1);
        if (className != null && className.value != null) {
            String normalized = normalizeClassName(className.value);
            if (normalized != null) {
                return new ResolvedString(normalized, className.reason);
            }
        }
        ResolvedString resolved = resolveClassNameWithReason(value, ctx);
        if (resolved != null && resolved.value != null) {
            return resolved;
        }
        return null;
    }

    private static ResolvedString resolveDirectStringFact(AbstractInsnNode src,
                                                          ResolveContext ctx,
                                                          int depth) {
        if (src == null || ctx == null || depth > MAX_CONST_DEPTH) {
            return null;
        }
        if (src instanceof LdcInsnNode ldc && ldc.cst instanceof String stringValue) {
            return normalizeResolvedString(stringValue, REASON_CONST);
        }
        if (src instanceof InsnNode insn && insn.getOpcode() == Opcodes.AALOAD) {
            return resolveArrayElementString(insn, ctx, depth + 1);
        }
        if (src instanceof FieldInsnNode fin && fin.getOpcode() == Opcodes.GETSTATIC) {
            String key = fin.owner + "#" + fin.name;
            return normalizeResolvedString(ctx.staticStrings.get(key), REASON_STATIC);
        }
        if (src instanceof FieldInsnNode fin && fin.getOpcode() == Opcodes.GETFIELD) {
            return resolveInstanceFieldString(fin, ctx);
        }
        if (src instanceof InvokeDynamicInsnNode indy) {
            return normalizeResolvedString(resolveInvokeDynamicString(indy, ctx, depth + 1), REASON_CONCAT);
        }
        if (!(src instanceof MethodInsnNode mi)) {
            return null;
        }
        if (isStringBuilderToString(mi)) {
            ResolvedString built = normalizeResolvedString(resolveStringBuilderToString(mi, ctx, depth + 1), REASON_STRING_BUILDER);
            if (built != null) {
                return built;
            }
        }
        ResolvedString stringMethod = resolveStringMethodResult(mi, ctx, depth + 1);
        if (stringMethod != null) {
            return stringMethod;
        }
        if ("java/lang/String".equals(mi.owner)
                && "valueOf".equals(mi.name)
                && mi.desc.startsWith("(")
                && mi.desc.endsWith(")Ljava/lang/String;")) {
            return normalizeResolvedString(resolveStringValueOf(mi, ctx, depth + 1), REASON_VALUE_OF);
        }
        ResolvedConst userMethodConst = resolveUserMethodConst(mi, ctx, depth + 1);
        if (userMethodConst != null && userMethodConst.value instanceof String stringValue) {
            return normalizeResolvedString(stringValue, userMethodConst.reason);
        }
        return null;
    }

    private static ResolvedString resolveStableStringFact(AbstractInsnNode src,
                                                          ResolveContext ctx,
                                                          int depth) {
        if (src == null || ctx == null || depth > MAX_CONST_DEPTH) {
            return null;
        }
        if (src instanceof LdcInsnNode ldc && ldc.cst instanceof String stringValue) {
            return normalizeResolvedString(stringValue, REASON_CONST);
        }
        if (src instanceof FieldInsnNode fin && fin.getOpcode() == Opcodes.GETSTATIC) {
            String key = fin.owner + "#" + fin.name;
            return normalizeResolvedString(ctx.staticStrings.get(key), REASON_STATIC);
        }
        if (src instanceof FieldInsnNode fin && fin.getOpcode() == Opcodes.GETFIELD) {
            return resolveInstanceFieldString(fin, ctx);
        }
        if (src instanceof InvokeDynamicInsnNode indy) {
            return normalizeResolvedString(resolveInvokeDynamicString(indy, ctx, depth + 1), REASON_CONCAT);
        }
        if (src instanceof VarInsnNode varInsn && varInsn.getOpcode() == Opcodes.ALOAD) {
            AbstractInsnNode stored = getStoredSourceInsn(varInsn, ctx);
            if (stored == null) {
                return null;
            }
            ResolvedString local = resolveStableStringFact(stored, ctx, depth + 1);
            return local == null ? null : new ResolvedString(local.value, combineReasons(REASON_LOCAL, local.reason));
        }
        if (!(src instanceof MethodInsnNode mi)) {
            return null;
        }
        if (isStringBuilderToString(mi)) {
            ResolvedString built = normalizeResolvedString(resolveStringBuilderToString(mi, ctx, depth + 1), REASON_STRING_BUILDER);
            if (built != null) {
                return built;
            }
        }
        int idx = ctx.instructions.indexOf(mi);
        if (idx < 0 || ctx.frames[idx] == null) {
            return null;
        }
        Frame<SourceValue> frame = ctx.frames[idx];
        int argCount = Type.getArgumentTypes(mi.desc).length;
        int base = frame.getStackSize() - argCount;
        if (base < 0) {
            return null;
        }
        if ("java/lang/String".equals(mi.owner) && base - 1 >= 0) {
            ResolvedString recv = resolveStableStringFact(getSingleInsn(frame.getStack(base - 1), ctx), ctx, depth + 1);
            if (recv == null || recv.value == null) {
                return null;
            }
            String self = recv.value;
            if ("trim".equals(mi.name) && "()Ljava/lang/String;".equals(mi.desc)) {
                return normalizeResolvedString(self.trim(), REASON_STRING_API);
            }
            if ("intern".equals(mi.name) && "()Ljava/lang/String;".equals(mi.desc)) {
                return normalizeResolvedString(self, REASON_STRING_API);
            }
            if ("toLowerCase".equals(mi.name)
                    && ("()Ljava/lang/String;".equals(mi.desc)
                    || "(Ljava/util/Locale;)Ljava/lang/String;".equals(mi.desc))) {
                return normalizeResolvedString(self.toLowerCase(Locale.ROOT), REASON_STRING_API);
            }
            if ("toUpperCase".equals(mi.name)
                    && ("()Ljava/lang/String;".equals(mi.desc)
                    || "(Ljava/util/Locale;)Ljava/lang/String;".equals(mi.desc))) {
                return normalizeResolvedString(self.toUpperCase(Locale.ROOT), REASON_STRING_API);
            }
            if ("concat".equals(mi.name) && "(Ljava/lang/String;)Ljava/lang/String;".equals(mi.desc)) {
                ResolvedString arg = resolveStableStringFact(getSingleInsn(frame.getStack(base), ctx), ctx, depth + 1);
                return arg == null ? null : normalizeResolvedString(self.concat(arg.value), REASON_STRING_API);
            }
            if ("substring".equals(mi.name) && "(I)Ljava/lang/String;".equals(mi.desc)) {
                Integer begin = resolveStableIntFact(getSingleInsn(frame.getStack(base), ctx), ctx, depth + 1);
                if (begin == null || begin < 0 || begin > self.length()) {
                    return null;
                }
                return normalizeResolvedString(self.substring(begin), REASON_STRING_API);
            }
            if ("substring".equals(mi.name) && "(II)Ljava/lang/String;".equals(mi.desc)) {
                Integer begin = resolveStableIntFact(getSingleInsn(frame.getStack(base), ctx), ctx, depth + 1);
                Integer end = resolveStableIntFact(getSingleInsn(frame.getStack(base + 1), ctx), ctx, depth + 1);
                if (begin == null || end == null || begin < 0 || end < begin || end > self.length()) {
                    return null;
                }
                return normalizeResolvedString(self.substring(begin, end), REASON_STRING_API);
            }
        }
        if ("java/lang/String".equals(mi.owner)
                && "valueOf".equals(mi.name)
                && mi.desc.startsWith("(")
                && mi.desc.endsWith(")Ljava/lang/String;")
                && base >= 0) {
            ResolvedString arg = resolveStableStringFact(getSingleInsn(frame.getStack(base), ctx), ctx, depth + 1);
            if (arg != null) {
                return arg;
            }
        }
        if (ctx.classNode != null
                && safe(ctx.classNode.name).equals(safe(mi.owner))
                && Type.getArgumentTypes(mi.desc).length == 0
                && Type.getReturnType(mi.desc).getSort() == Type.OBJECT
                && "java/lang/String".equals(Type.getReturnType(mi.desc).getInternalName())) {
            MethodNode target = findDeclaredMethod(ctx.classNode, mi.name, mi.desc);
            if (target == null || target.instructions == null || target.instructions.size() == 0) {
                return null;
            }
            try {
                Analyzer<SourceValue> nestedAnalyzer = new Analyzer<>(new SourceInterpreter());
                Frame<SourceValue>[] nestedFrames = nestedAnalyzer.analyze(ctx.classNode.name, target);
                ResolveContext nested = new ResolveContext(
                        nestedFrames,
                        target.instructions,
                        ctx.classNode,
                        ctx.staticStrings,
                        ctx.instanceFieldFacts,
                        BuildFactSnapshot.MethodConstraintFacts.empty(),
                        ctx.methodReturnCache,
                        ctx.methodReturnInProgress
                );
                ResolvedString found = null;
                for (int i = 0; i < target.instructions.size(); i++) {
                    AbstractInsnNode insn = target.instructions.get(i);
                    if (insn == null || insn.getOpcode() != Opcodes.ARETURN) {
                        continue;
                    }
                    Frame<SourceValue> returnFrame = nestedFrames[i];
                    if (returnFrame == null || returnFrame.getStackSize() <= 0) {
                        return null;
                    }
                    ResolvedString candidate = resolveStableStringFact(
                            getSingleInsn(returnFrame.getStack(returnFrame.getStackSize() - 1), nested),
                            nested,
                            depth + 1
                    );
                    if (candidate == null || candidate.value == null) {
                        return null;
                    }
                    candidate = new ResolvedString(candidate.value, combineReasons(REASON_USER_METHOD, candidate.reason));
                    if (found == null) {
                        found = candidate;
                    } else if (!safe(found.value).equals(safe(candidate.value))) {
                        return null;
                    }
                }
                return found;
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private static Integer resolveStableIntFact(AbstractInsnNode src,
                                                ResolveContext ctx,
                                                int depth) {
        if (src == null || ctx == null || depth > MAX_CONST_DEPTH) {
            return null;
        }
        if (src instanceof InsnNode insn) {
            return switch (insn.getOpcode()) {
                case Opcodes.ICONST_M1 -> -1;
                case Opcodes.ICONST_0 -> 0;
                case Opcodes.ICONST_1 -> 1;
                case Opcodes.ICONST_2 -> 2;
                case Opcodes.ICONST_3 -> 3;
                case Opcodes.ICONST_4 -> 4;
                case Opcodes.ICONST_5 -> 5;
                default -> null;
            };
        }
        if (src instanceof IntInsnNode intInsn) {
            return intInsn.operand;
        }
        if (src instanceof LdcInsnNode ldc && ldc.cst instanceof Integer intValue) {
            return intValue;
        }
        if (!(src instanceof MethodInsnNode mi) || !"java/lang/String".equals(mi.owner)) {
            return null;
        }
        int idx = ctx.instructions.indexOf(mi);
        if (idx < 0 || ctx.frames[idx] == null) {
            return null;
        }
        Frame<SourceValue> frame = ctx.frames[idx];
        int argCount = Type.getArgumentTypes(mi.desc).length;
        int base = frame.getStackSize() - argCount;
        if (base < 0 || base - 1 < 0) {
            return null;
        }
        ResolvedString recv = resolveStableStringFact(getSingleInsn(frame.getStack(base - 1), ctx), ctx, depth + 1);
        if (recv == null || recv.value == null) {
            return null;
        }
        if ("indexOf".equals(mi.name) && "(I)I".equals(mi.desc)) {
            Integer needle = resolveStableIntFact(getSingleInsn(frame.getStack(base), ctx), ctx, depth + 1);
            return needle == null ? null : recv.value.indexOf(needle);
        }
        if ("indexOf".equals(mi.name) && "(Ljava/lang/String;)I".equals(mi.desc)) {
            ResolvedString needle = resolveStableStringFact(getSingleInsn(frame.getStack(base), ctx), ctx, depth + 1);
            return needle == null ? null : recv.value.indexOf(needle.value);
        }
        return null;
    }

    private static ResolvedString resolveClassNameFromSourceDirect(AbstractInsnNode src,
                                                                   ResolveContext ctx) {
        if (src == null || ctx == null) {
            return null;
        }
        if (src instanceof VarInsnNode varInsn && varInsn.getOpcode() == Opcodes.ALOAD) {
            ResolvedString local = resolveClassNameFromSourceDirect(getStoredSourceInsn(varInsn, ctx), ctx);
            if (local != null && local.value != null) {
                return new ResolvedString(local.value, combineReasons(REASON_LOCAL, local.reason));
            }
        }
        if (src instanceof LdcInsnNode ldc && ldc.cst instanceof Type type) {
            return new ResolvedString(type.getInternalName(), REASON_CLASS_CONST);
        }
        if (src instanceof InsnNode insn && insn.getOpcode() == Opcodes.AALOAD) {
            ResolvedString className = resolveArrayElementString(insn, ctx, 0);
            if (className == null || className.value == null) {
                return null;
            }
            String normalized = normalizeClassName(className.value);
            return normalized == null ? null : new ResolvedString(normalized, className.reason);
        }
        if (src instanceof FieldInsnNode fin && fin.getOpcode() == Opcodes.GETFIELD) {
            return resolveInstanceFieldObjectType(fin, ctx);
        }
        if (src instanceof MethodInsnNode mi && isObjectGetClass(mi)) {
            return resolveObjectGetClassName(mi, ctx, 0);
        }
        if (src instanceof MethodInsnNode mi && (isForName(mi) || isLoadClass(mi))) {
            ResolvedString className = resolveClassNameFromCallWithReason(mi, ctx, 0);
            if (className == null || className.value == null) {
                return null;
            }
            String normalized = normalizeClassName(className.value);
            return normalized == null ? null : new ResolvedString(normalized, REASON_FOR_NAME + "_" + className.reason);
        }
        if (src instanceof FieldInsnNode || src instanceof InvokeDynamicInsnNode) {
            ResolvedString className = resolveReflectionStringLeaf(src, ctx, 0);
            if (className == null || className.value == null) {
                return null;
            }
            String normalized = normalizeClassName(className.value);
            return normalized == null ? null : new ResolvedString(normalized, className.reason);
        }
        return null;
    }

    private static ResolvedString resolveStringFromInsn(AbstractInsnNode src,
                                                        ResolveContext ctx,
                                                        int depth) {
        if (src == null || ctx == null || depth > MAX_CONST_DEPTH) {
            return null;
        }
        if (src instanceof LdcInsnNode ldc && ldc.cst instanceof String stringValue) {
            return normalizeResolvedString(stringValue, REASON_CONST);
        }
        if (src instanceof InsnNode insn && insn.getOpcode() == Opcodes.AALOAD) {
            return resolveArrayElementString(insn, ctx, depth + 1);
        }
        if (src instanceof VarInsnNode varInsn && varInsn.getOpcode() == Opcodes.ALOAD) {
            int loadIndex = ctx.instructions.indexOf(varInsn);
            int storeIndex = findPreviousAstore(ctx, loadIndex, varInsn.var);
            if (storeIndex >= 0 && ctx.frames[storeIndex] != null) {
                Frame<SourceValue> storeFrame = ctx.frames[storeIndex];
                if (storeFrame.getStackSize() > 0) {
                    ResolvedString local = resolveStringFromInsn(
                            getSingleInsn(storeFrame.getStack(storeFrame.getStackSize() - 1), ctx),
                            ctx,
                            depth + 1
                    );
                    if (local != null) {
                        return new ResolvedString(local.value, combineReasons(REASON_LOCAL, local.reason));
                    }
                }
            }
            return null;
        }
        if (src instanceof FieldInsnNode fin && fin.getOpcode() == Opcodes.GETSTATIC) {
            String key = fin.owner + "#" + fin.name;
            return normalizeResolvedString(ctx.staticStrings.get(key), REASON_STATIC);
        }
        if (src instanceof FieldInsnNode fin && fin.getOpcode() == Opcodes.GETFIELD) {
            return resolveInstanceFieldString(fin, ctx);
        }
        if (src instanceof InvokeDynamicInsnNode indy) {
            return normalizeResolvedString(resolveInvokeDynamicString(indy, ctx, depth + 1), REASON_CONCAT);
        }
        if (!(src instanceof MethodInsnNode mi)) {
            return null;
        }
        if (isStringBuilderToString(mi)) {
            ResolvedString built = normalizeResolvedString(resolveStringBuilderToString(mi, ctx, depth + 1), REASON_STRING_BUILDER);
            if (built != null) {
                return built;
            }
        }
        ResolvedString stringMethod = resolveStringMethodResult(mi, ctx, depth + 1);
        if (stringMethod != null) {
            return stringMethod;
        }
        ResolvedConst apiConst = resolveStringApiConst(mi, ctx, depth + 1);
        if (apiConst != null && apiConst.value instanceof String stringValue) {
            ResolvedString resolved = normalizeResolvedString(stringValue, apiConst.reason);
            if (resolved != null) {
                return resolved;
            }
        }
        if ("java/lang/String".equals(mi.owner)
                && "valueOf".equals(mi.name)
                && mi.desc.startsWith("(")
                && mi.desc.endsWith(")Ljava/lang/String;")) {
            ResolvedString valueOf = normalizeResolvedString(resolveStringValueOf(mi, ctx, depth + 1), REASON_VALUE_OF);
            if (valueOf != null) {
                return valueOf;
            }
        }
        ResolvedConst userMethodConst = resolveUserMethodConst(mi, ctx, depth + 1);
        if (userMethodConst != null && userMethodConst.value instanceof String stringValue) {
            return normalizeResolvedString(stringValue, userMethodConst.reason);
        }
        return null;
    }

    private static ResolvedString resolveStringMethodResult(MethodInsnNode mi,
                                                            ResolveContext ctx,
                                                            int depth) {
        if (mi == null || ctx == null || depth > MAX_CONST_DEPTH) {
            return null;
        }
        int idx = ctx.instructions.indexOf(mi);
        if (idx < 0 || ctx.frames[idx] == null) {
            return null;
        }
        Frame<SourceValue> frame = ctx.frames[idx];
        int argCount = Type.getArgumentTypes(mi.desc).length;
        int base = frame.getStackSize() - argCount;
        if (base < 0) {
            return null;
        }
        if ("java/lang/String".equals(mi.owner)) {
            if (base - 1 < 0) {
                return null;
            }
            ResolvedString recv = resolveStringOperand(frame.getStack(base - 1), ctx, depth + 1);
            if (recv == null || recv.value == null) {
                return null;
            }
            String self = recv.value;
            if ("concat".equals(mi.name) && "(Ljava/lang/String;)Ljava/lang/String;".equals(mi.desc)) {
                ResolvedString arg = resolveStringOperand(frame.getStack(base), ctx, depth + 1);
                return arg == null ? null : normalizeResolvedString(self.concat(arg.value), REASON_STRING_API);
            }
            if ("trim".equals(mi.name) && "()Ljava/lang/String;".equals(mi.desc)) {
                return normalizeResolvedString(self.trim(), REASON_STRING_API);
            }
            if ("intern".equals(mi.name) && "()Ljava/lang/String;".equals(mi.desc)) {
                return normalizeResolvedString(self, REASON_STRING_API);
            }
            if ("toLowerCase".equals(mi.name)
                    && ("()Ljava/lang/String;".equals(mi.desc)
                    || "(Ljava/util/Locale;)Ljava/lang/String;".equals(mi.desc))) {
                return normalizeResolvedString(self.toLowerCase(Locale.ROOT), REASON_STRING_API);
            }
            if ("toUpperCase".equals(mi.name)
                    && ("()Ljava/lang/String;".equals(mi.desc)
                    || "(Ljava/util/Locale;)Ljava/lang/String;".equals(mi.desc))) {
                return normalizeResolvedString(self.toUpperCase(Locale.ROOT), REASON_STRING_API);
            }
            if ("substring".equals(mi.name) && "(I)Ljava/lang/String;".equals(mi.desc)) {
                ResolvedConst beginObj = resolveConstValueWithReason(frame.getStack(base), ctx, depth + 1);
                if (beginObj == null || !(beginObj.value instanceof Integer begin) || begin < 0 || begin > self.length()) {
                    return null;
                }
                return normalizeResolvedString(self.substring(begin), REASON_STRING_API);
            }
            if ("substring".equals(mi.name) && "(II)Ljava/lang/String;".equals(mi.desc)) {
                ResolvedConst beginObj = resolveConstValueWithReason(frame.getStack(base), ctx, depth + 1);
                ResolvedConst endObj = resolveConstValueWithReason(frame.getStack(base + 1), ctx, depth + 1);
                if (beginObj == null || endObj == null
                        || !(beginObj.value instanceof Integer begin)
                        || !(endObj.value instanceof Integer end)
                        || begin < 0 || end < begin || end > self.length()) {
                    return null;
                }
                return normalizeResolvedString(self.substring(begin, end), REASON_STRING_API);
            }
            if ("replace".equals(mi.name)
                    && "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;".equals(mi.desc)) {
                ResolvedString from = resolveStringOperand(frame.getStack(base), ctx, depth + 1);
                ResolvedString to = resolveStringOperand(frame.getStack(base + 1), ctx, depth + 1);
                return from == null || to == null
                        ? null
                        : normalizeResolvedString(self.replace(from.value, to.value), REASON_STRING_API);
            }
            if ("replace".equals(mi.name) && "(CC)Ljava/lang/String;".equals(mi.desc)) {
                Character from = resolveCharConstant(frame.getStack(base), ctx, depth + 1);
                Character to = resolveCharConstant(frame.getStack(base + 1), ctx, depth + 1);
                return from == null || to == null
                        ? null
                        : normalizeResolvedString(self.replace(from, to), REASON_STRING_API);
            }
            return null;
        }
        if (CLASS_OWNER.equals(mi.owner)
                && ("getName".equals(mi.name)
                || "getTypeName".equals(mi.name)
                || "getCanonicalName".equals(mi.name)
                || "getSimpleName".equals(mi.name))
                && "()Ljava/lang/String;".equals(mi.desc)) {
            if (base - 1 < 0) {
                return null;
            }
            ResolvedString classInfo = resolveClassNameWithReason(frame.getStack(base - 1), ctx);
            if (classInfo == null || classInfo.value == null) {
                return null;
            }
            String dotName = classInfo.value.replace('/', '.');
            String out = "getSimpleName".equals(mi.name)
                    ? dotName.substring(dotName.lastIndexOf('.') + 1)
                    : dotName;
            return normalizeResolvedString(out, combineReasons(REASON_STRING_API, classInfo.reason));
        }
        return null;
    }

    private static ResolvedString normalizeResolvedString(String raw,
                                                          String reason) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return new ResolvedString(trimmed, reason == null ? REASON_UNKNOWN : reason);
    }

    private static String resolveClassLoaderReason(SourceValue value,
                                                   ResolveContext ctx,
                                                   int depth) {
        if (depth > MAX_CONST_DEPTH) {
            return REASON_UNKNOWN;
        }
        AbstractInsnNode src = getSingleInsn(value, ctx);
        if (!(src instanceof MethodInsnNode mi)) {
            return REASON_UNKNOWN;
        }
        if (CLASS_OWNER.equals(mi.owner)
                && "getClassLoader".equals(mi.name)
                && "()Ljava/lang/ClassLoader;".equals(mi.desc)) {
            int idx = ctx.instructions.indexOf(mi);
            if (idx < 0 || ctx.frames[idx] == null) {
                return "get_class_loader";
            }
            Frame<SourceValue> frame = ctx.frames[idx];
            int recvIndex = frame.getStackSize() - 1;
            if (recvIndex < 0) {
                return "get_class_loader";
            }
            ResolvedString classInfo = resolveClassNameWithReason(frame.getStack(recvIndex), ctx);
            if (classInfo == null || classInfo.value == null) {
                return "get_class_loader";
            }
            return "from_" + normalizeReasonFragment(classInfo.value);
        }
        if ("java/lang/ClassLoader".equals(mi.owner)
                && "getSystemClassLoader".equals(mi.name)
                && "()Ljava/lang/ClassLoader;".equals(mi.desc)) {
            return "system";
        }
        if ("java/lang/Thread".equals(mi.owner)
                && "getContextClassLoader".equals(mi.name)
                && "()Ljava/lang/ClassLoader;".equals(mi.desc)) {
            return "thread_context";
        }
        if ("java/lang/ClassLoader".equals(mi.owner)
                && "getParent".equals(mi.name)
                && "()Ljava/lang/ClassLoader;".equals(mi.desc)) {
            int idx = ctx.instructions.indexOf(mi);
            if (idx < 0 || ctx.frames[idx] == null) {
                return "parent";
            }
            Frame<SourceValue> frame = ctx.frames[idx];
            int recvIndex = frame.getStackSize() - 1;
            if (recvIndex < 0) {
                return "parent";
            }
            String parentReason = resolveClassLoaderReason(frame.getStack(recvIndex), ctx, depth + 1);
            return "parent_" + normalizeReasonFragment(parentReason);
        }
        return REASON_UNKNOWN;
    }

    private static String normalizeReasonFragment(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return REASON_UNKNOWN;
        }
        StringBuilder sb = new StringBuilder();
        String value = raw.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-') {
                sb.append(c);
            } else if (c == '/' || c == '.') {
                sb.append('_');
            }
        }
        return sb.isEmpty() ? REASON_UNKNOWN : sb.toString();
    }

    private static String buildReflectionReason(ResolvedString classInfo, ResolvedString nameInfo) {
        String classReason = classInfo == null ? REASON_UNKNOWN : classInfo.reason;
        String nameReason = nameInfo == null ? REASON_UNKNOWN : nameInfo.reason;
        StringBuilder sb = new StringBuilder();
        if (classReason != null && !classReason.isEmpty()) {
            sb.append("class_").append(classReason);
        }
        if (nameReason != null && !nameReason.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append("+");
            }
            sb.append("name_").append(nameReason);
        }
        return sb.isEmpty() ? REASON_UNKNOWN : sb.toString();
    }

    private static TargetResolve buildTargetResolve(List<MethodReference.Handle> targets,
                                                    String reason) {
        if (targets == null || targets.isEmpty()) {
            return null;
        }
        BuildFactSnapshot.ReflectionHintTier tier =
                BuildFactSnapshot.ReflectionHintTier.fromReason(reason);
        int candidateCount = targets.size();
        if (candidateCount > IMPRECISE_TARGET_THRESHOLD) {
            return new TargetResolve(List.of(), reason, tier, true, true, candidateCount);
        }
        return new TargetResolve(
                List.copyOf(targets),
                reason,
                tier,
                candidateCount > 1,
                false,
                candidateCount
        );
    }

    private static ResolvedString resolveReceiverTypeAtInvoke(MethodInsnNode invoke,
                                                              ResolveContext ctx) {
        if (invoke == null || ctx == null || ctx.instructions == null || ctx.frames == null) {
            return null;
        }
        int idx = ctx.instructions.indexOf(invoke);
        if (idx < 0 || idx >= ctx.frames.length) {
            return null;
        }
        Frame<SourceValue> frame = ctx.frames[idx];
        if (frame == null) {
            return null;
        }
        int argSlots = 0;
        for (Type type : Type.getArgumentTypes(invoke.desc)) {
            argSlots += type.getSize();
        }
        int receiverArgIndex = frame.getStackSize() - argSlots;
        if (receiverArgIndex < 0 || receiverArgIndex >= frame.getStackSize()) {
            return null;
        }
        ResolvedString receiverType = resolveRuntimeObjectType(frame.getStack(receiverArgIndex), ctx, 0);
        if (receiverType == null
                || receiverType.value == null
                || receiverType.reason == null
                || !receiverType.reason.contains(REASON_CAST)) {
            return null;
        }
        String normalized = normalizeClassName(receiverType.value);
        return normalized == null ? null : new ResolvedString(normalized, receiverType.reason);
    }

    private static List<Type> resolveClassArray(SourceValue value,
                                                ResolveContext ctx) {
        AbstractInsnNode src = getSingleInsn(value, ctx);
        if (!(src instanceof TypeInsnNode tin)
                || tin.getOpcode() != Opcodes.ANEWARRAY
                || !"java/lang/Class".equals(tin.desc)) {
            return null;
        }
        if (ctx.classArrayCache.containsKey(src)) {
            return ctx.classArrayCache.get(src);
        }
        if (ctx.classArrayMiss.contains(src)) {
            return null;
        }
        int srcIndex = ctx.instructions.indexOf(src);
        if (srcIndex < 0 || ctx.frames[srcIndex] == null) {
            return null;
        }
        Frame<SourceValue> srcFrame = ctx.frames[srcIndex];
        Integer expectedSize = resolveIntConstant(srcFrame.getStack(srcFrame.getStackSize() - 1));
        if (expectedSize == null || expectedSize < 0) {
            return null;
        }
        Map<Integer, Type> elements = new HashMap<>();
        for (int i = 0, n = ctx.instructions.size(); i < n; i++) {
            AbstractInsnNode insn = ctx.instructions.get(i);
            if (!(insn instanceof InsnNode) || insn.getOpcode() != Opcodes.AASTORE) {
                continue;
            }
            Frame<SourceValue> frame = ctx.frames[i];
            if (frame == null || frame.getStackSize() < 3) {
                continue;
            }
            int stackSize = frame.getStackSize();
            if (!containsSource(frame.getStack(stackSize - 3), src)) {
                continue;
            }
            Integer index = resolveIntConstant(frame.getStack(stackSize - 2));
            Type type = resolveTypeConstant(frame.getStack(stackSize - 1), ctx);
            if (index == null || type == null) {
                ctx.classArrayMiss.add(src);
                return null;
            }
            elements.put(index, type);
        }
        List<Type> out;
        if (expectedSize == 0) {
            out = elements.isEmpty() ? Collections.emptyList() : null;
        } else if (elements.size() == expectedSize) {
            out = new ArrayList<>(expectedSize);
            for (int i = 0; i < expectedSize; i++) {
                Type t = elements.get(i);
                if (t == null) {
                    ctx.classArrayMiss.add(src);
                    return null;
                }
                out.add(t);
            }
        } else {
            out = null;
        }
        if (out == null) {
            ctx.classArrayMiss.add(src);
            return null;
        }
        ctx.classArrayCache.put(src, out);
        return out;
    }

    private static Type resolveTypeConstant(SourceValue value,
                                            ResolveContext ctx) {
        AbstractInsnNode src = getSingleInsn(value, ctx);
        if (src instanceof LdcInsnNode ldc && ldc.cst instanceof Type type) {
            return type;
        }
        if (src instanceof FieldInsnNode fin) {
            if ("TYPE".equals(fin.name) && "Ljava/lang/Class;".equals(fin.desc)) {
                return primitiveTypeForOwner(fin.owner);
            }
        }
        if (src instanceof MethodInsnNode mi) {
            if (isForName(mi) || isLoadClass(mi)) {
                ResolvedString resolved = resolveClassNameFromCallWithReason(mi, ctx, 0);
                String className = normalizeClassName(resolved == null ? null : resolved.value);
                return className == null ? null : Type.getObjectType(className);
            }
        }
        if (src instanceof InvokeDynamicInsnNode || src instanceof FieldInsnNode) {
            ResolvedString name = resolveStringConstantWithReason(value, ctx, 0);
            String className = normalizeClassName(name == null ? null : name.value);
            return className == null ? null : Type.getObjectType(className);
        }
        return null;
    }

    private static Type primitiveTypeForOwner(String owner) {
        if ("java/lang/Boolean".equals(owner)) {
            return Type.BOOLEAN_TYPE;
        }
        if ("java/lang/Byte".equals(owner)) {
            return Type.BYTE_TYPE;
        }
        if ("java/lang/Character".equals(owner)) {
            return Type.CHAR_TYPE;
        }
        if ("java/lang/Short".equals(owner)) {
            return Type.SHORT_TYPE;
        }
        if ("java/lang/Integer".equals(owner)) {
            return Type.INT_TYPE;
        }
        if ("java/lang/Long".equals(owner)) {
            return Type.LONG_TYPE;
        }
        if ("java/lang/Float".equals(owner)) {
            return Type.FLOAT_TYPE;
        }
        if ("java/lang/Double".equals(owner)) {
            return Type.DOUBLE_TYPE;
        }
        if ("java/lang/Void".equals(owner)) {
            return Type.VOID_TYPE;
        }
        return null;
    }

    private static Integer resolveIntConstant(SourceValue value) {
        AbstractInsnNode src = getSingleInsn(value);
        if (src instanceof InsnNode) {
            return switch (src.getOpcode()) {
                case Opcodes.ICONST_M1 -> -1;
                case Opcodes.ICONST_0 -> 0;
                case Opcodes.ICONST_1 -> 1;
                case Opcodes.ICONST_2 -> 2;
                case Opcodes.ICONST_3 -> 3;
                case Opcodes.ICONST_4 -> 4;
                case Opcodes.ICONST_5 -> 5;
                default -> null;
            };
        }
        if (src instanceof IntInsnNode intInsn) {
            return intInsn.operand;
        }
        if (src instanceof LdcInsnNode ldc && ldc.cst instanceof Integer valueInt) {
            return valueInt;
        }
        return null;
    }

    private static Map<String, String> collectStaticStringConstants(ClassNode cn) {
        if (cn == null || cn.fields == null || cn.fields.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> out = new HashMap<>();
        cn.fields.forEach(field -> {
            if (field == null || field.value == null) {
                return;
            }
            if ((field.access & (Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)) == (Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)
                    && field.value instanceof String stringValue) {
                out.put(cn.name + "#" + field.name, stringValue.trim());
            }
        });
        return out;
    }

    static Map<String, BuildFactSnapshot.AliasValueFact> collectInstanceFieldFacts(BuildBytecodeWorkspace workspace) {
        if (workspace == null || workspace.parsedClasses().isEmpty()) {
            return Map.of();
        }
        Map<String, List<FieldBinding>> bindingsByCtor = collectConstructorFieldBindings(workspace);
        if (bindingsByCtor.isEmpty()) {
            return Map.of();
        }
        Map<String, InstanceFieldFactsBuilder> builders = new LinkedHashMap<>();
        for (BuildBytecodeWorkspace.ParsedClass parsedClass : workspace.parsedClasses()) {
            if (parsedClass == null || parsedClass.classNode() == null || parsedClass.methods().isEmpty()) {
                continue;
            }
            Map<String, String> staticStrings = collectStaticStringConstants(parsedClass.classNode());
            for (BuildBytecodeWorkspace.ParsedMethod parsedMethod : parsedClass.methods()) {
                MethodNode methodNode = parsedMethod.methodNode();
                if (methodNode == null || methodNode.instructions == null || methodNode.instructions.size() == 0) {
                    continue;
                }
                if (!containsRelevantConstructorInvoke(methodNode, bindingsByCtor)) {
                    continue;
                }
                Frame<SourceValue>[] frames = parsedMethod.sourceFrames();
                if (frames == null) {
                    continue;
                }
                ResolveContext ctx = new ResolveContext(
                        frames,
                        methodNode.instructions,
                        parsedClass.classNode(),
                        staticStrings,
                        Map.of()
                );
                for (int i = 0, n = methodNode.instructions.size(); i < n; i++) {
                    AbstractInsnNode insn = methodNode.instructions.get(i);
                    if (!(insn instanceof MethodInsnNode mi)
                            || mi.getOpcode() != Opcodes.INVOKESPECIAL
                            || !"<init>".equals(mi.name)) {
                        continue;
                    }
                    List<FieldBinding> bindings = bindingsByCtor.get(constructorKey(mi.owner, mi.desc));
                    if (bindings == null || bindings.isEmpty()) {
                        continue;
                    }
                    Frame<SourceValue> frame = frames[i];
                    if (frame == null) {
                        continue;
                    }
                    Type[] argTypes = Type.getArgumentTypes(mi.desc);
                    for (FieldBinding binding : bindings) {
                        if (binding == null) {
                            continue;
                        }
                        InstanceFieldFactsBuilder builder = builders.computeIfAbsent(
                                fieldKey(binding.owner(), binding.fieldName()),
                                ignore -> new InstanceFieldFactsBuilder()
                        );
                        switch (binding.kind()) {
                            case STRING_CONST -> builder.addString(binding.literal());
                            case OBJECT_CONST -> builder.addObjectType(binding.literal());
                            case STRING_ARG, OBJECT_ARG -> {
                                int stackIndex = resolveArgumentStackIndex(frame.getStackSize(), argTypes, binding.argIndex());
                                if (stackIndex < 0 || stackIndex >= frame.getStackSize()) {
                                    continue;
                                }
                                SourceValue argValue = frame.getStack(stackIndex);
                                if (binding.kind() == FieldBindingKind.STRING_ARG) {
                                    ResolvedString resolved = resolveStringOperandForReflection(argValue, ctx, 0);
                                    if (resolved != null) {
                                        builder.addString(resolved.value);
                                    }
                                } else {
                                    ResolvedString resolved = resolveRuntimeObjectType(argValue, ctx, 0);
                                    if (resolved != null) {
                                        builder.addObjectType(resolved.value);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (builders.isEmpty()) {
            return Map.of();
        }
        Map<String, BuildFactSnapshot.AliasValueFact> out = new LinkedHashMap<>();
        for (Map.Entry<String, InstanceFieldFactsBuilder> entry : builders.entrySet()) {
            BuildFactSnapshot.AliasValueFact facts =
                    entry.getValue() == null ? BuildFactSnapshot.AliasValueFact.empty() : entry.getValue().build();
            if (!facts.isEmpty()) {
                out.put(entry.getKey(), facts);
            }
        }
        return out.isEmpty() ? Map.of() : Map.copyOf(out);
    }

    private static Map<String, List<FieldBinding>> collectConstructorFieldBindings(BuildBytecodeWorkspace workspace) {
        Map<String, List<FieldBinding>> out = new LinkedHashMap<>();
        if (workspace == null || workspace.parsedClasses().isEmpty()) {
            return out;
        }
        for (BuildBytecodeWorkspace.ParsedClass parsedClass : workspace.parsedClasses()) {
            if (parsedClass == null || parsedClass.classNode() == null || parsedClass.methods().isEmpty()) {
                continue;
            }
            Map<String, String> staticStrings = collectStaticStringConstants(parsedClass.classNode());
            for (BuildBytecodeWorkspace.ParsedMethod parsedMethod : parsedClass.methods()) {
                MethodNode methodNode = parsedMethod.methodNode();
                if (methodNode == null
                        || !"<init>".equals(methodNode.name)
                        || methodNode.instructions == null
                        || methodNode.instructions.size() == 0) {
                    continue;
                }
                if (!containsOwnFieldStore(methodNode, parsedClass.className())) {
                    continue;
                }
                Frame<SourceValue>[] frames = parsedMethod.sourceFrames();
                if (frames == null) {
                    continue;
                }
                ResolveContext ctx = new ResolveContext(
                        frames,
                        methodNode.instructions,
                        parsedClass.classNode(),
                        staticStrings,
                        Map.of()
                );
                Map<Integer, Integer> argIndexBySlot = constructorArgIndexBySlot(methodNode.desc, methodNode.access);
                List<FieldBinding> bindings = new ArrayList<>();
                for (int i = 0, n = methodNode.instructions.size(); i < n; i++) {
                    AbstractInsnNode insn = methodNode.instructions.get(i);
                    if (!(insn instanceof FieldInsnNode fin)
                            || fin.getOpcode() != Opcodes.PUTFIELD
                            || !safe(parsedClass.className()).equals(safe(fin.owner))) {
                        continue;
                    }
                    Frame<SourceValue> frame = frames[i];
                    if (frame == null || frame.getStackSize() <= 0) {
                        continue;
                    }
                    FieldBinding binding = resolveFieldBinding(
                            fin,
                            frame.getStack(frame.getStackSize() - 1),
                            ctx,
                            argIndexBySlot
                    );
                    if (binding != null) {
                        bindings.add(binding);
                    }
                }
                if (!bindings.isEmpty()) {
                    out.put(constructorKey(parsedClass.className(), methodNode.desc), List.copyOf(bindings));
                }
            }
        }
        return out;
    }

    private static boolean containsRelevantConstructorInvoke(MethodNode methodNode,
                                                             Map<String, List<FieldBinding>> bindingsByCtor) {
        if (methodNode == null
                || methodNode.instructions == null
                || methodNode.instructions.size() == 0
                || bindingsByCtor == null
                || bindingsByCtor.isEmpty()) {
            return false;
        }
        for (int i = 0, n = methodNode.instructions.size(); i < n; i++) {
            AbstractInsnNode insn = methodNode.instructions.get(i);
            if (!(insn instanceof MethodInsnNode mi)
                    || mi.getOpcode() != Opcodes.INVOKESPECIAL
                    || !"<init>".equals(mi.name)) {
                continue;
            }
            if (bindingsByCtor.containsKey(constructorKey(mi.owner, mi.desc))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsOwnFieldStore(MethodNode methodNode,
                                                 String owner) {
        if (methodNode == null
                || methodNode.instructions == null
                || methodNode.instructions.size() == 0
                || owner == null
                || owner.isBlank()) {
            return false;
        }
        for (int i = 0, n = methodNode.instructions.size(); i < n; i++) {
            AbstractInsnNode insn = methodNode.instructions.get(i);
            if (!(insn instanceof FieldInsnNode fin)
                    || fin.getOpcode() != Opcodes.PUTFIELD) {
                continue;
            }
            if (owner.equals(fin.owner)) {
                return true;
            }
        }
        return false;
    }

    private static FieldBinding resolveFieldBinding(FieldInsnNode fieldInsn,
                                                    SourceValue value,
                                                    ResolveContext ctx,
                                                    Map<Integer, Integer> argIndexBySlot) {
        if (fieldInsn == null || value == null) {
            return null;
        }
        AbstractInsnNode src = getSingleInsn(value, ctx);
        if (src == null) {
            return null;
        }
        Type fieldType = Type.getType(fieldInsn.desc);
        if (src instanceof VarInsnNode varInsn && varInsn.getOpcode() == Opcodes.ALOAD) {
            Integer argIndex = argIndexBySlot.get(varInsn.var);
            if (argIndex == null || argIndex < 0) {
                return null;
            }
            if (fieldType.getSort() == Type.OBJECT && "java/lang/String".equals(fieldType.getInternalName())) {
                return new FieldBinding(fieldInsn.owner, fieldInsn.name, FieldBindingKind.STRING_ARG, argIndex, null);
            }
            if (fieldType.getSort() == Type.OBJECT || fieldType.getSort() == Type.ARRAY) {
                return new FieldBinding(fieldInsn.owner, fieldInsn.name, FieldBindingKind.OBJECT_ARG, argIndex, null);
            }
            return null;
        }
        if (src instanceof LdcInsnNode ldc
                && ldc.cst instanceof String stringValue
                && fieldType.getSort() == Type.OBJECT
                && "java/lang/String".equals(fieldType.getInternalName())) {
            return new FieldBinding(fieldInsn.owner, fieldInsn.name, FieldBindingKind.STRING_CONST, null, stringValue.trim());
        }
        String directType = directObjectType(src, ctx, 0);
        if (directType != null && (fieldType.getSort() == Type.OBJECT || fieldType.getSort() == Type.ARRAY)) {
            return new FieldBinding(fieldInsn.owner, fieldInsn.name, FieldBindingKind.OBJECT_CONST, null, directType);
        }
        return null;
    }

    private static Map<Integer, Integer> constructorArgIndexBySlot(String desc, int access) {
        Map<Integer, Integer> out = new HashMap<>();
        Type[] args = Type.getArgumentTypes(desc);
        int slot = (access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
        for (int i = 0; i < args.length; i++) {
            out.put(slot, i);
            slot += args[i].getSize();
        }
        return out;
    }

    private static int resolveArgumentStackIndex(int stackSize, Type[] argTypes, Integer argIndex) {
        if (argTypes == null || argIndex == null || argIndex < 0 || argIndex >= argTypes.length) {
            return -1;
        }
        int totalArgSlots = 0;
        for (Type argType : argTypes) {
            totalArgSlots += argType == null ? 1 : argType.getSize();
        }
        int index = stackSize - totalArgSlots;
        for (int i = 0; i < argIndex; i++) {
            Type argType = argTypes[i];
            index += argType == null ? 1 : argType.getSize();
        }
        return index;
    }

    private static ResolvedConst resolveConstValueWithReason(SourceValue value,
                                                             ResolveContext ctx,
                                                             int depth) {
        if (depth > MAX_CONST_DEPTH) {
            return null;
        }
        AbstractInsnNode src = getSingleInsn(value, ctx);
        if (src == null) {
            return null;
        }
        ResolvedConst cached = ctx.constCache.get(src);
        if (cached != null) {
            return cached == UNRESOLVED_CONST ? null : cached;
        }
        ResolvedConst resolved = null;
        if (src instanceof LdcInsnNode ldc) {
            Object cst = ldc.cst;
            if (cst != null) {
                resolved = new ResolvedConst(cst, REASON_CONST);
            }
        }
        if (src instanceof InsnNode) {
            resolved = switch (src.getOpcode()) {
                case Opcodes.ICONST_M1 -> new ResolvedConst(-1, REASON_CONST);
                case Opcodes.ICONST_0 -> new ResolvedConst(0, REASON_CONST);
                case Opcodes.ICONST_1 -> new ResolvedConst(1, REASON_CONST);
                case Opcodes.ICONST_2 -> new ResolvedConst(2, REASON_CONST);
                case Opcodes.ICONST_3 -> new ResolvedConst(3, REASON_CONST);
                case Opcodes.ICONST_4 -> new ResolvedConst(4, REASON_CONST);
                case Opcodes.ICONST_5 -> new ResolvedConst(5, REASON_CONST);
                case Opcodes.LCONST_0 -> new ResolvedConst(0L, REASON_CONST);
                case Opcodes.LCONST_1 -> new ResolvedConst(1L, REASON_CONST);
                case Opcodes.FCONST_0 -> new ResolvedConst(0.0f, REASON_CONST);
                case Opcodes.FCONST_1 -> new ResolvedConst(1.0f, REASON_CONST);
                case Opcodes.FCONST_2 -> new ResolvedConst(2.0f, REASON_CONST);
                case Opcodes.DCONST_0 -> new ResolvedConst(0.0d, REASON_CONST);
                case Opcodes.DCONST_1 -> new ResolvedConst(1.0d, REASON_CONST);
                default -> resolved;
            };
        }
        if (src instanceof IntInsnNode intInsn) {
            resolved = new ResolvedConst(intInsn.operand, REASON_CONST);
        }
        if (src instanceof VarInsnNode vin && vin.getOpcode() == Opcodes.ALOAD) {
            int loadIndex = ctx.instructions.indexOf(vin);
            int storeIndex = findPreviousAstore(ctx, loadIndex, vin.var);
            if (storeIndex >= 0 && ctx.frames[storeIndex] != null) {
                Frame<SourceValue> storeFrame = ctx.frames[storeIndex];
                if (storeFrame.getStackSize() > 0) {
                    ResolvedConst local = resolveConstValueWithReason(
                            storeFrame.getStack(storeFrame.getStackSize() - 1),
                            ctx,
                            depth + 1
                    );
                    if (local != null) {
                        resolved = new ResolvedConst(local.value, combineReasons(REASON_LOCAL, local.reason));
                    }
                }
            }
        }
        if (src instanceof FieldInsnNode fin && fin.getOpcode() == Opcodes.GETSTATIC) {
            String key = fin.owner + "#" + fin.name;
            if (ctx.staticStrings.containsKey(key)) {
                resolved = new ResolvedConst(ctx.staticStrings.get(key), REASON_STATIC);
            }
        }
        if (src instanceof InvokeDynamicInsnNode indy) {
            String valueString = resolveInvokeDynamicString(indy, ctx, depth + 1);
            if (valueString != null) {
                resolved = new ResolvedConst(valueString, REASON_CONCAT);
            }
        }
        if (src instanceof MethodInsnNode mi) {
            if (isStringBuilderToString(mi)) {
                String valueString = resolveStringBuilderToString(mi, ctx, depth + 1);
                if (valueString != null) {
                    resolved = new ResolvedConst(valueString, REASON_STRING_BUILDER);
                }
            }
            ResolvedConst stringApiConst = resolveStringApiConst(mi, ctx, depth + 1);
            if (stringApiConst != null) {
                resolved = stringApiConst;
            }
            if ("java/lang/String".equals(mi.owner)
                    && "valueOf".equals(mi.name)
                    && mi.desc.startsWith("(")
                    && mi.desc.endsWith(")Ljava/lang/String;")) {
                String valueString = resolveStringValueOf(mi, ctx, depth + 1);
                if (valueString != null) {
                    resolved = new ResolvedConst(valueString, REASON_VALUE_OF);
                }
            }
            ResolvedConst userMethodConst = resolveUserMethodConst(mi, ctx, depth + 1);
            if (userMethodConst != null) {
                resolved = userMethodConst;
            }
        }
        if (resolved == null) {
            ctx.constCache.put(src, UNRESOLVED_CONST);
            return null;
        }
        ctx.constCache.put(src, resolved);
        return resolved;
    }

    private static ResolvedConst resolveStringApiConst(MethodInsnNode mi,
                                                       ResolveContext ctx,
                                                       int depth) {
        if (mi == null || ctx == null || depth > MAX_CONST_DEPTH) {
            return null;
        }
        int idx = ctx.instructions.indexOf(mi);
        if (idx < 0 || ctx.frames[idx] == null) {
            return null;
        }
        Frame<SourceValue> frame = ctx.frames[idx];
        Type[] argTypes = Type.getArgumentTypes(mi.desc);
        int argCount = argTypes.length;
        int base = frame.getStackSize() - argCount;
        if (base < 0) {
            return null;
        }
        ResolvedString stringMethod = resolveStringMethodResult(mi, ctx, depth + 1);
        if (stringMethod != null) {
            return new ResolvedConst(stringMethod.value, stringMethod.reason);
        }
        if ("java/lang/String".equals(mi.owner)) {
            if (base - 1 < 0) {
                return null;
            }
            ResolvedString recv = resolveStringOperand(frame.getStack(base - 1), ctx, depth + 1);
            if (recv == null || recv.value == null) {
                return null;
            }
            String self = recv.value;
            if ("concat".equals(mi.name) && "(Ljava/lang/String;)Ljava/lang/String;".equals(mi.desc)) {
                ResolvedConst arg = resolveConstValueWithReason(frame.getStack(base), ctx, depth + 1);
                return arg == null ? null : new ResolvedConst(self.concat(String.valueOf(arg.value)), REASON_STRING_API);
            }
            if ("trim".equals(mi.name) && "()Ljava/lang/String;".equals(mi.desc)) {
                return new ResolvedConst(self.trim(), REASON_STRING_API);
            }
            if ("intern".equals(mi.name) && "()Ljava/lang/String;".equals(mi.desc)) {
                return new ResolvedConst(self, REASON_STRING_API);
            }
            if ("toLowerCase".equals(mi.name) && "()Ljava/lang/String;".equals(mi.desc)) {
                return new ResolvedConst(self.toLowerCase(Locale.ROOT), REASON_STRING_API);
            }
            if ("toLowerCase".equals(mi.name) && "(Ljava/util/Locale;)Ljava/lang/String;".equals(mi.desc)) {
                return new ResolvedConst(self.toLowerCase(Locale.ROOT), REASON_STRING_API);
            }
            if ("toUpperCase".equals(mi.name) && "()Ljava/lang/String;".equals(mi.desc)) {
                return new ResolvedConst(self.toUpperCase(Locale.ROOT), REASON_STRING_API);
            }
            if ("toUpperCase".equals(mi.name) && "(Ljava/util/Locale;)Ljava/lang/String;".equals(mi.desc)) {
                return new ResolvedConst(self.toUpperCase(Locale.ROOT), REASON_STRING_API);
            }
            if ("indexOf".equals(mi.name) && "(I)I".equals(mi.desc)) {
                ResolvedConst needle = resolveConstValueWithReason(frame.getStack(base), ctx, depth + 1);
                if (needle == null || !(needle.value instanceof Integer intValue)) {
                    return null;
                }
                return new ResolvedConst(self.indexOf(intValue), REASON_STRING_API);
            }
            if ("indexOf".equals(mi.name) && "(Ljava/lang/String;)I".equals(mi.desc)) {
                ResolvedString needle = resolveStringOperand(frame.getStack(base), ctx, depth + 1);
                if (needle == null || needle.value == null) {
                    return null;
                }
                return new ResolvedConst(self.indexOf(needle.value), REASON_STRING_API);
            }
        }
        return null;
    }

    private static Character resolveCharConstant(SourceValue value,
                                                 ResolveContext ctx,
                                                 int depth) {
        ResolvedConst resolved = resolveConstValueWithReason(value, ctx, depth + 1);
        if (resolved == null) {
            return null;
        }
        if (resolved.value instanceof Character c) {
            return c;
        }
        if (resolved.value instanceof Integer intValue
                && intValue >= Character.MIN_VALUE
                && intValue <= Character.MAX_VALUE) {
            return (char) intValue.intValue();
        }
        return null;
    }

    private static boolean isStringBuilderToString(MethodInsnNode mi) {
        if (mi == null || !"toString".equals(mi.name) || !"()Ljava/lang/String;".equals(mi.desc)) {
            return false;
        }
        return "java/lang/StringBuilder".equals(mi.owner) || "java/lang/StringBuffer".equals(mi.owner);
    }

    private static String resolveStringValueOf(MethodInsnNode mi,
                                               ResolveContext ctx,
                                               int depth) {
        int idx = ctx.instructions.indexOf(mi);
        if (idx < 0 || ctx.frames[idx] == null) {
            return null;
        }
        Frame<SourceValue> frame = ctx.frames[idx];
        int argCount = Type.getArgumentTypes(mi.desc).length;
        int argIndex = frame.getStackSize() - argCount;
        if (argIndex < 0) {
            return null;
        }
        ResolvedConst value = resolveConstValueWithReason(frame.getStack(argIndex), ctx, depth + 1);
        return value == null ? null : String.valueOf(value.value);
    }

    private static String resolveInvokeDynamicString(InvokeDynamicInsnNode indy,
                                                     ResolveContext ctx,
                                                     int depth) {
        if (indy == null || indy.bsm == null || !SCF_OWNER.equals(indy.bsm.getOwner())) {
            return null;
        }
        int idx = ctx.instructions.indexOf(indy);
        if (idx < 0 || ctx.frames[idx] == null) {
            return null;
        }
        Frame<SourceValue> frame = ctx.frames[idx];
        Type[] args = Type.getArgumentTypes(indy.desc);
        int base = frame.getStackSize() - args.length;
        if (base < 0) {
            return null;
        }
        List<String> dyn = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            ResolvedConst value = resolveConstValueWithReason(frame.getStack(base + i), ctx, depth + 1);
            if (value == null) {
                return null;
            }
            dyn.add(String.valueOf(value.value));
        }
        String bsmName = indy.bsm.getName();
        if ("makeConcat".equals(bsmName)) {
            StringBuilder sb = new StringBuilder();
            dyn.forEach(sb::append);
            return sb.toString();
        }
        if (!"makeConcatWithConstants".equals(bsmName) || indy.bsmArgs == null || indy.bsmArgs.length == 0) {
            return null;
        }
        if (!(indy.bsmArgs[0] instanceof String recipe)) {
            return null;
        }
        List<String> consts = new ArrayList<>();
        for (int i = 1; i < indy.bsmArgs.length; i++) {
            Object constant = indy.bsmArgs[i];
            if (constant == null) {
                return null;
            }
            consts.add(String.valueOf(constant));
        }
        return applyConcatRecipe(recipe, dyn, consts);
    }

    private static String applyConcatRecipe(String recipe,
                                            List<String> dyn,
                                            List<String> consts) {
        StringBuilder sb = new StringBuilder();
        int dynIndex = 0;
        int constIndex = 0;
        for (int i = 0; i < recipe.length(); i++) {
            char c = recipe.charAt(i);
            if (c == '\u0001') {
                if (dynIndex >= dyn.size()) {
                    return null;
                }
                sb.append(dyn.get(dynIndex++));
            } else if (c == '\u0002') {
                if (constIndex >= consts.size()) {
                    return null;
                }
                sb.append(consts.get(constIndex++));
            } else {
                sb.append(c);
            }
        }
        return dynIndex == dyn.size() && constIndex == consts.size() ? sb.toString() : null;
    }

    private static String resolveStringBuilderToString(MethodInsnNode mi,
                                                       ResolveContext ctx,
                                                       int depth) {
        int idx = ctx.instructions.indexOf(mi);
        if (idx < 0 || ctx.frames[idx] == null) {
            return null;
        }
        Frame<SourceValue> frame = ctx.frames[idx];
        if (frame.getStackSize() < 1) {
            return null;
        }
        TypeInsnNode newInsn = findBuilderRootNewInsn(frame.getStack(frame.getStackSize() - 1), ctx, depth + 1);
        if (newInsn == null) {
            return null;
        }
        int newIndex = ctx.instructions.indexOf(newInsn);
        if (newIndex < 0 || newIndex > idx) {
            return null;
        }
        for (int i = newIndex; i <= idx; i++) {
            AbstractInsnNode insn = ctx.instructions.get(i);
            if (insn instanceof JumpInsnNode
                    || insn instanceof LookupSwitchInsnNode
                    || insn instanceof TableSwitchInsnNode) {
                return null;
            }
        }
        StringBuilder sb = new StringBuilder();
        boolean initSeen = false;
        for (int i = newIndex; i <= idx; i++) {
            AbstractInsnNode insn = ctx.instructions.get(i);
            if (!(insn instanceof MethodInsnNode methodInsn) || !isBuilderOwner(methodInsn.owner)) {
                continue;
            }
            Frame<SourceValue> methodFrame = ctx.frames[i];
            if (methodFrame == null) {
                return null;
            }
            int argCount = Type.getArgumentTypes(methodInsn.desc).length;
            int recvIndex = methodFrame.getStackSize() - argCount - 1;
            if (recvIndex < 0
                    || !containsBuilderOrigin(methodFrame.getStack(recvIndex), ctx, newInsn, depth + 1)) {
                continue;
            }
            if ("<init>".equals(methodInsn.name)) {
                if (argCount == 0) {
                    initSeen = true;
                    continue;
                }
                if (argCount != 1) {
                    return null;
                }
                ResolvedConst value = resolveConstValueWithReason(methodFrame.getStack(recvIndex + 1), ctx, depth + 1);
                if (value == null) {
                    return null;
                }
                sb.append(String.valueOf(value.value));
                initSeen = true;
                continue;
            }
            if ("append".equals(methodInsn.name) && argCount == 1) {
                ResolvedConst value = resolveConstValueWithReason(methodFrame.getStack(recvIndex + 1), ctx, depth + 1);
                if (value == null) {
                    return null;
                }
                sb.append(String.valueOf(value.value));
            }
        }
        if (!initSeen && sb.isEmpty()) {
            return null;
        }
        return sb.toString();
    }

    private static boolean isBuilderOwner(String owner) {
        return "java/lang/StringBuilder".equals(owner) || "java/lang/StringBuffer".equals(owner);
    }

    private static TypeInsnNode findBuilderRootNewInsn(SourceValue value,
                                                       ResolveContext ctx,
                                                       int depth) {
        if (value == null
                || value.insns == null
                || value.insns.isEmpty()
                || ctx == null
                || depth > MAX_CONST_DEPTH) {
            return null;
        }
        TypeInsnNode found = null;
        for (AbstractInsnNode insn : value.insns) {
            TypeInsnNode candidate = null;
            if (insn instanceof TypeInsnNode typeInsn
                    && typeInsn.getOpcode() == Opcodes.NEW
                    && isBuilderOwner(typeInsn.desc)) {
                candidate = typeInsn;
            } else if (insn instanceof InsnNode dupInsn && isBuilderDupOpcode(dupInsn.getOpcode())) {
                candidate = resolveBuilderRootFromDup(dupInsn, ctx, depth + 1);
            } else if (insn instanceof VarInsnNode varInsn
                    && varInsn.getOpcode() == Opcodes.ALOAD) {
                int loadIndex = ctx.instructions.indexOf(varInsn);
                int storeIndex = findPreviousAstore(ctx, loadIndex, varInsn.var);
                if (storeIndex >= 0 && ctx.frames[storeIndex] != null) {
                    Frame<SourceValue> storeFrame = ctx.frames[storeIndex];
                    if (storeFrame.getStackSize() > 0) {
                        candidate = findBuilderRootNewInsn(
                                storeFrame.getStack(storeFrame.getStackSize() - 1),
                                ctx,
                                depth + 1
                        );
                    }
                }
            } else if (insn instanceof MethodInsnNode mi && isBuilderChainProducer(mi)) {
                int idx = ctx.instructions.indexOf(mi);
                if (idx >= 0 && ctx.frames[idx] != null) {
                    Frame<SourceValue> frame = ctx.frames[idx];
                    int argCount = Type.getArgumentTypes(mi.desc).length;
                    int recvIndex = frame.getStackSize() - argCount - 1;
                    if (recvIndex >= 0) {
                        candidate = findBuilderRootNewInsn(frame.getStack(recvIndex), ctx, depth + 1);
                    }
                }
            }
            if (candidate == null) {
                continue;
            }
            if (found != null && found != candidate) {
                return null;
            }
            found = candidate;
        }
        return found;
    }

    private static boolean containsBuilderOrigin(SourceValue value,
                                                 ResolveContext ctx,
                                                 AbstractInsnNode root,
                                                 int depth) {
        if (value == null
                || value.insns == null
                || value.insns.isEmpty()
                || ctx == null
                || root == null
                || depth > MAX_CONST_DEPTH) {
            return false;
        }
        if (value.insns.contains(root)) {
            return true;
        }
        for (AbstractInsnNode src : value.insns) {
            if (src instanceof InsnNode dupInsn && isBuilderDupOpcode(dupInsn.getOpcode())) {
                if (containsBuilderOrigin(resolveDupSourceValue(dupInsn, ctx), ctx, root, depth + 1)) {
                    return true;
                }
                continue;
            }
            if (src instanceof VarInsnNode varInsn && varInsn.getOpcode() == Opcodes.ALOAD) {
                int loadIndex = ctx.instructions.indexOf(varInsn);
                int storeIndex = findPreviousAstore(ctx, loadIndex, varInsn.var);
                if (storeIndex >= 0 && ctx.frames[storeIndex] != null) {
                    Frame<SourceValue> storeFrame = ctx.frames[storeIndex];
                    if (storeFrame.getStackSize() > 0
                            && containsBuilderOrigin(
                            storeFrame.getStack(storeFrame.getStackSize() - 1),
                            ctx,
                            root,
                            depth + 1)) {
                        return true;
                    }
                }
                continue;
            }
            if (!(src instanceof MethodInsnNode mi) || !isBuilderChainProducer(mi)) {
                continue;
            }
            int idx = ctx.instructions.indexOf(mi);
            if (idx < 0 || ctx.frames[idx] == null) {
                continue;
            }
            Frame<SourceValue> frame = ctx.frames[idx];
            int argCount = Type.getArgumentTypes(mi.desc).length;
            int recvIndex = frame.getStackSize() - argCount - 1;
            if (recvIndex >= 0
                    && containsBuilderOrigin(frame.getStack(recvIndex), ctx, root, depth + 1)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBuilderChainProducer(MethodInsnNode mi) {
        if (mi == null || !isBuilderOwner(mi.owner)) {
            return false;
        }
        if (!mi.desc.endsWith(")L" + mi.owner + ";")) {
            return false;
        }
        return !"toString".equals(mi.name);
    }

    private static boolean isBuilderDupOpcode(int opcode) {
        return opcode == Opcodes.DUP
                || opcode == Opcodes.DUP_X1
                || opcode == Opcodes.DUP_X2
                || opcode == Opcodes.DUP2
                || opcode == Opcodes.DUP2_X1
                || opcode == Opcodes.DUP2_X2;
    }

    private static TypeInsnNode resolveBuilderRootFromDup(InsnNode dupInsn,
                                                          ResolveContext ctx,
                                                          int depth) {
        SourceValue source = resolveDupSourceValue(dupInsn, ctx);
        return source == null ? null : findBuilderRootNewInsn(source, ctx, depth + 1);
    }

    private static SourceValue resolveDupSourceValue(InsnNode dupInsn,
                                                     ResolveContext ctx) {
        if (dupInsn == null || ctx == null || ctx.instructions == null || ctx.frames == null) {
            return null;
        }
        int idx = ctx.instructions.indexOf(dupInsn);
        if (idx < 0 || idx >= ctx.frames.length) {
            return null;
        }
        Frame<SourceValue> frame = ctx.frames[idx];
        if (frame == null || frame.getStackSize() <= 0) {
            return null;
        }
        return frame.getStack(frame.getStackSize() - 1);
    }

    private static AbstractInsnNode getSingleInsn(SourceValue value) {
        if (value == null || value.insns == null || value.insns.size() != 1) {
            return null;
        }
        return value.insns.iterator().next();
    }

    private static AbstractInsnNode getSingleInsn(SourceValue value,
                                                  ResolveContext ctx) {
        if (value == null || value.insns == null || value.insns.size() != 1) {
            return null;
        }
        AbstractInsnNode src = value.insns.iterator().next();
        return canonicalInsn(src, ctx);
    }

    private static AbstractInsnNode canonicalInsn(AbstractInsnNode src,
                                                  ResolveContext ctx) {
        if (src == null || ctx == null || ctx.instructions == null) {
            return src;
        }
        if (ctx.instructions.indexOf(src) >= 0) {
            return src;
        }
        AbstractInsnNode matched = null;
        for (int i = 0, n = ctx.instructions.size(); i < n; i++) {
            AbstractInsnNode candidate = ctx.instructions.get(i);
            if (!sameInsn(src, candidate)) {
                continue;
            }
            if (matched != null && matched != candidate) {
                return src;
            }
            matched = candidate;
        }
        return matched == null ? src : matched;
    }

    private static boolean sameInsn(AbstractInsnNode left,
                                    AbstractInsnNode right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null || left.getClass() != right.getClass()) {
            return false;
        }
        if (left.getOpcode() != right.getOpcode()) {
            return false;
        }
        if (left instanceof MethodInsnNode a && right instanceof MethodInsnNode b) {
            return a.itf == b.itf
                    && safe(a.owner).equals(safe(b.owner))
                    && safe(a.name).equals(safe(b.name))
                    && safe(a.desc).equals(safe(b.desc));
        }
        if (left instanceof FieldInsnNode a && right instanceof FieldInsnNode b) {
            return safe(a.owner).equals(safe(b.owner))
                    && safe(a.name).equals(safe(b.name))
                    && safe(a.desc).equals(safe(b.desc));
        }
        if (left instanceof VarInsnNode a && right instanceof VarInsnNode b) {
            return a.var == b.var;
        }
        if (left instanceof TypeInsnNode a && right instanceof TypeInsnNode b) {
            return safe(a.desc).equals(safe(b.desc));
        }
        if (left instanceof LdcInsnNode a && right instanceof LdcInsnNode b) {
            return java.util.Objects.equals(a.cst, b.cst);
        }
        if (left instanceof IntInsnNode a && right instanceof IntInsnNode b) {
            return a.operand == b.operand;
        }
        if (left instanceof InvokeDynamicInsnNode a && right instanceof InvokeDynamicInsnNode b) {
            if (!safe(a.name).equals(safe(b.name))
                    || !safe(a.desc).equals(safe(b.desc))
                    || a.bsm == null
                    || b.bsm == null) {
                return false;
            }
            return safe(a.bsm.getOwner()).equals(safe(b.bsm.getOwner()))
                    && safe(a.bsm.getName()).equals(safe(b.bsm.getName()))
                    && safe(a.bsm.getDesc()).equals(safe(b.bsm.getDesc()));
        }
        return true;
    }

    private static boolean containsSource(SourceValue value,
                                          AbstractInsnNode target) {
        return value != null && value.insns != null && value.insns.contains(target);
    }

    private static int findPreviousAstore(ResolveContext ctx,
                                          int fromIndex,
                                          int var) {
        if (ctx == null || ctx.instructions == null || fromIndex <= 0 || var < 0) {
            return -1;
        }
        int scanFloor = Math.max(0, fromIndex - 256);
        for (int i = fromIndex - 1; i >= scanFloor; i--) {
            AbstractInsnNode insn = ctx.instructions.get(i);
            if (!(insn instanceof VarInsnNode vin)) {
                continue;
            }
            if (vin.var == var && vin.getOpcode() == Opcodes.ASTORE) {
                return i;
            }
        }
        return -1;
    }

    private static MethodReference.Handle withSyntheticOpcode(MethodReference.Handle target,
                                                              Map<MethodReference.Handle, MethodReference> methodMap,
                                                              int fallbackOpcode) {
        if (target == null) {
            return null;
        }
        int opcode = resolveSyntheticOpcode(target, methodMap, fallbackOpcode);
        return new MethodReference.Handle(
                target.getClassReference(),
                opcode,
                target.getName(),
                target.getDesc()
        );
    }

    private static int resolveSyntheticOpcode(MethodReference.Handle target,
                                              Map<MethodReference.Handle, MethodReference> methodMap,
                                              int fallbackOpcode) {
        if (target == null) {
            return fallbackOpcode;
        }
        Integer fromHandle = target.getOpcode();
        if (fromHandle != null && fromHandle > 0) {
            return fromHandle;
        }
        MethodReference method = methodMap == null ? null : methodMap.get(target);
        if (method != null && method.isStatic()) {
            return Opcodes.INVOKESTATIC;
        }
        if ("<init>".equals(target.getName())) {
            return Opcodes.INVOKESPECIAL;
        }
        return fallbackOpcode;
    }

    private static MethodTypeInfo resolveMethodType(SourceValue value,
                                                    ResolveContext ctx) {
        return resolveMethodType(value, ctx, 0);
    }

    private static MethodTypeInfo resolveMethodType(SourceValue value,
                                                    ResolveContext ctx,
                                                    int depth) {
        if (depth > MAX_CONST_DEPTH) {
            return null;
        }
        AbstractInsnNode src = getSingleInsn(value, ctx);
        if (src instanceof VarInsnNode varInsn && varInsn.getOpcode() == Opcodes.ALOAD) {
            int loadIndex = ctx.instructions.indexOf(varInsn);
            int storeIndex = findPreviousAstore(ctx, loadIndex, varInsn.var);
            if (storeIndex >= 0 && ctx.frames[storeIndex] != null) {
                Frame<SourceValue> storeFrame = ctx.frames[storeIndex];
                if (storeFrame.getStackSize() > 0) {
                    return resolveMethodType(storeFrame.getStack(storeFrame.getStackSize() - 1), ctx, depth + 1);
                }
            }
            return null;
        }
        if (!(src instanceof MethodInsnNode mi) || !MT_OWNER.equals(mi.owner)) {
            return null;
        }
        if ("fromMethodDescriptorString".equals(mi.name)
                && "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;".equals(mi.desc)) {
            String desc = resolveStringFromCall(mi, ctx);
            if (desc == null) {
                return null;
            }
            try {
                Type mt = Type.getMethodType(desc);
                Type[] args = mt.getArgumentTypes();
                ArrayList<Type> params = new ArrayList<>(args.length);
                Collections.addAll(params, args);
                return new MethodTypeInfo(mt.getReturnType(), params);
            } catch (IllegalArgumentException ex) {
                logger.debug("parse MethodType descriptor failed: {}: {}", desc, ex.toString());
                return null;
            }
        }
        if (!"methodType".equals(mi.name)) {
            return null;
        }
        int idx = ctx.instructions.indexOf(mi);
        if (idx < 0 || ctx.frames[idx] == null) {
            return null;
        }
        Frame<SourceValue> frame = ctx.frames[idx];
        Type[] argTypes = Type.getArgumentTypes(mi.desc);
        int base = frame.getStackSize() - argTypes.length;
        if (base < 0) {
            return null;
        }
        ArrayList<Type> params = new ArrayList<>();
        Type returnType = null;
        for (int i = 0; i < argTypes.length; i++) {
            Type declared = argTypes[i];
            SourceValue argVal = frame.getStack(base + i);
            if (i == 0) {
                returnType = resolveTypeConstant(argVal, ctx);
                if (returnType == null) {
                    return null;
                }
                continue;
            }
            if (declared.getSort() == Type.ARRAY && "java/lang/Class".equals(declared.getElementType().getInternalName())) {
                List<Type> extra = resolveClassArray(argVal, ctx);
                if (extra == null) {
                    return null;
                }
                params.addAll(extra);
                continue;
            }
            if (!"java/lang/Class".equals(declared.getInternalName())) {
                return null;
            }
            Type paramType = resolveTypeConstant(argVal, ctx);
            if (paramType == null) {
                return null;
            }
            params.add(paramType);
        }
        return returnType == null ? null : new MethodTypeInfo(returnType, params);
    }

    private static String resolveStringFromCall(MethodInsnNode mi,
                                                ResolveContext ctx) {
        int idx = ctx.instructions.indexOf(mi);
        if (idx < 0 || ctx.frames[idx] == null) {
            return null;
        }
        Frame<SourceValue> frame = ctx.frames[idx];
        int argCount = Type.getArgumentTypes(mi.desc).length;
        int argIndex = frame.getStackSize() - argCount;
        if (argIndex < 0) {
            return null;
        }
        ResolvedString resolved = resolveStringOperand(frame.getStack(argIndex), ctx, 0);
        return resolved == null ? null : resolved.value;
    }

    private static ResolvedConst resolveUserMethodConst(MethodInsnNode mi,
                                                        ResolveContext ctx,
                                                        int depth) {
        if (mi == null
                || ctx == null
                || ctx.classNode == null
                || depth > MAX_CONST_DEPTH
                || !safe(ctx.classNode.name).equals(safe(mi.owner))
                || Type.getArgumentTypes(mi.desc).length != 0) {
            return null;
        }
        Type returnType = Type.getReturnType(mi.desc);
        if (returnType.getSort() == Type.VOID) {
            return null;
        }
        String key = buildMethodKey(mi.owner, mi.name, mi.desc);
        if (ctx.methodReturnCache.containsKey(key)) {
            ResolvedConst cached = ctx.methodReturnCache.get(key);
            return cached == UNRESOLVED_CONST ? null : cached;
        }
        if (!ctx.methodReturnInProgress.add(key)) {
            return null;
        }
        try {
            MethodNode target = findDeclaredMethod(ctx.classNode, mi.name, mi.desc);
            if (target == null || target.instructions == null || target.instructions.size() == 0) {
                ctx.methodReturnCache.put(key, UNRESOLVED_CONST);
                return null;
            }
            Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());
            Frame<SourceValue>[] frames = analyzer.analyze(ctx.classNode.name, target);
            ResolveContext nested = new ResolveContext(
                    frames,
                    target.instructions,
                    ctx.classNode,
                    ctx.staticStrings,
                    ctx.instanceFieldFacts,
                    BuildFactSnapshot.MethodConstraintFacts.empty(),
                    ctx.methodReturnCache,
                    ctx.methodReturnInProgress
            );
            ResolvedConst resolved = null;
            for (int i = 0, n = target.instructions.size(); i < n; i++) {
                AbstractInsnNode insn = target.instructions.get(i);
                int opcode = insn == null ? -1 : insn.getOpcode();
                if (opcode != Opcodes.ARETURN
                        && opcode != Opcodes.IRETURN
                        && opcode != Opcodes.LRETURN
                        && opcode != Opcodes.FRETURN
                        && opcode != Opcodes.DRETURN) {
                    continue;
                }
                Frame<SourceValue> frame = frames[i];
                if (frame == null || frame.getStackSize() <= 0) {
                    ctx.methodReturnCache.put(key, UNRESOLVED_CONST);
                    return null;
                }
                ResolvedConst candidate = resolveConstValueWithReason(
                        frame.getStack(frame.getStackSize() - 1),
                        nested,
                        depth + 1
                );
                if (candidate == null) {
                    ctx.methodReturnCache.put(key, UNRESOLVED_CONST);
                    return null;
                }
                candidate = new ResolvedConst(candidate.value, combineReasons(REASON_USER_METHOD, candidate.reason));
                if (resolved == null) {
                    resolved = candidate;
                    continue;
                }
                if (!java.util.Objects.equals(resolved.value, candidate.value)) {
                    ctx.methodReturnCache.put(key, UNRESOLVED_CONST);
                    return null;
                }
            }
            ctx.methodReturnCache.put(key, resolved == null ? UNRESOLVED_CONST : resolved);
            return resolved;
        } catch (Exception ex) {
            logger.debug("resolve user method const failed: {}#{}{} {}", mi.owner, mi.name, mi.desc, ex.toString());
            ctx.methodReturnCache.put(key, UNRESOLVED_CONST);
            return null;
        } finally {
            ctx.methodReturnInProgress.remove(key);
        }
    }

    private static MethodNode findDeclaredMethod(ClassNode classNode,
                                                 String methodName,
                                                 String methodDesc) {
        if (classNode == null || classNode.methods == null || classNode.methods.isEmpty()) {
            return null;
        }
        MethodNode found = null;
        for (MethodNode method : classNode.methods) {
            if (method == null
                    || !safe(methodName).equals(safe(method.name))
                    || !safe(methodDesc).equals(safe(method.desc))) {
                continue;
            }
            if (found != null && found != method) {
                return null;
            }
            found = method;
        }
        return found;
    }

    private static String buildMethodKey(String owner,
                                         String methodName,
                                         String methodDesc) {
        return safe(owner) + "#" + safe(methodName) + safe(methodDesc);
    }

    private static MethodReference.Handle resolveMethodHandle(BytecodeMainlineCallGraphRunner.MethodLookup lookup,
                                                              Map<MethodReference.Handle, MethodReference> methodMap,
                                                              String className,
                                                              String methodName,
                                                              String methodDesc,
                                                              Integer preferredJarId) {
        if (lookup != null) {
            MethodReference.Handle resolved = lookup.resolve(className, methodName, methodDesc, preferredJarId);
            if (resolved != null) {
                return resolved;
            }
        }
        return findUniqueMethod(methodMap, normalizeClassName(className), safe(methodName), safe(methodDesc));
    }

    private static boolean addEdge(Map<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
                                   Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                                   MethodReference.Handle caller,
                                   MethodReference.Handle callee,
                                   String type,
                                   String confidence,
                                   String reason) {
        if (methodCalls == null || caller == null || callee == null) {
            return false;
        }
        HashSet<MethodReference.Handle> callees = methodCalls.computeIfAbsent(caller, ignore -> new HashSet<>());
        boolean inserted = MethodCallUtils.addCallee(callees, callee);
        MethodCallMeta.record(
                methodCallMeta,
                MethodCallKey.of(caller, callee),
                type,
                confidence,
                reason,
                callee.getOpcode()
        );
        return inserted;
    }

    private static String combineReasons(String left,
                                         String right) {
        String l = safe(left);
        String r = safe(right);
        if (l.isEmpty()) {
            return r.isEmpty() ? REASON_UNKNOWN : r;
        }
        if (r.isEmpty()) {
            return l;
        }
        return l + "+" + r;
    }

    private static String normalizeClassName(String name) {
        if (name == null) {
            return null;
        }
        String value = name.trim();
        if (value.isEmpty() || value.startsWith("[")) {
            return null;
        }
        if (value.contains("/")) {
            return value;
        }
        return value.replace('.', '/');
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    record Result(int reflectionEdges,
                  int methodHandleEdges,
                  int invokeDynamicEdges,
                  int hintConstSites,
                  int hintLogSites,
                  int hintCastSites,
                  int hintUnknownSites,
                  int hintImpreciseSites,
                  int hintThresholdExceededSites) {
        static Result empty() {
            return new Result(0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    private record LambdaTarget(String reason,
                                String samOwner,
                                String samName,
                                String samDesc,
                                String implOwner,
                                String implName,
                                String implDesc) {
    }

    private static String constructorKey(String owner, String desc) {
        String normalizedOwner = safe(owner);
        String normalizedDesc = safe(desc);
        if (normalizedOwner.isBlank() || normalizedDesc.isBlank()) {
            return "";
        }
        return normalizedOwner + "#" + normalizedDesc;
    }

    private static String fieldKey(String owner, String fieldName) {
        String normalizedOwner = safe(owner);
        String normalizedField = safe(fieldName);
        if (normalizedOwner.isBlank() || normalizedField.isBlank()) {
            return "";
        }
        return normalizedOwner + "#" + normalizedField;
    }

    private record ResolvedConst(Object value, String reason) {
    }

    private record ResolvedString(String value, String reason) {
    }

    private record TargetResolve(List<MethodReference.Handle> targets,
                                 String reason,
                                 BuildFactSnapshot.ReflectionHintTier tier,
                                 boolean imprecise,
                                 boolean thresholdExceeded,
                                 int candidateCount) {
    }

    private record MethodTypeInfo(Type returnType, List<Type> paramTypes) {
    }

    private record MethodResolveResult(int reflectionEdges,
                                       int methodHandleEdges) {
        private static MethodResolveResult empty() {
            return new MethodResolveResult(0, 0);
        }
    }

    private record HintStats(int constSites,
                             int logSites,
                             int castSites,
                             int unknownSites,
                             int impreciseSites,
                             int thresholdExceededSites) {
        private static HintStats empty() {
            return new HintStats(0, 0, 0, 0, 0, 0);
        }

        private HintStats merge(HintStats other) {
            if (other == null) {
                return this;
            }
            return new HintStats(
                    constSites + other.constSites,
                    logSites + other.logSites,
                    castSites + other.castSites,
                    unknownSites + other.unknownSites,
                    impreciseSites + other.impreciseSites,
                    thresholdExceededSites + other.thresholdExceededSites
            );
        }
    }

    private static String lambdaReason(Handle impl) {
        if (impl == null) {
            return REASON_LAMBDA;
        }
        return switch (impl.getTag()) {
            case Opcodes.H_NEWINVOKESPECIAL -> REASON_LAMBDA + "_constructor_ref";
            case Opcodes.H_INVOKESTATIC -> impl.getName() != null && impl.getName().startsWith("lambda$")
                    ? REASON_LAMBDA + "_body"
                    : REASON_LAMBDA + "_static_ref";
            case Opcodes.H_INVOKEVIRTUAL, Opcodes.H_INVOKEINTERFACE -> REASON_LAMBDA + "_virtual_ref";
            case Opcodes.H_INVOKESPECIAL -> REASON_LAMBDA + "_special_ref";
            default -> REASON_LAMBDA;
        };
    }

    private static final ResolvedConst UNRESOLVED_CONST = new ResolvedConst(null, REASON_UNKNOWN);

    private enum FieldBindingKind {
        STRING_ARG,
        OBJECT_ARG,
        STRING_CONST,
        OBJECT_CONST
    }

    private record FieldBinding(String owner,
                                String fieldName,
                                FieldBindingKind kind,
                                Integer argIndex,
                                String literal) {
    }

    private static final class InstanceFieldFactsBuilder {
        private final LinkedHashSet<String> stringValues = new LinkedHashSet<>();
        private final LinkedHashSet<String> objectTypes = new LinkedHashSet<>();

        private void addString(String value) {
            String normalized = safe(value);
            if (!normalized.isBlank()) {
                stringValues.add(normalized);
            }
        }

        private void addObjectType(String value) {
            String normalized = safe(value);
            if (!normalized.isBlank()) {
                objectTypes.add(normalized);
            }
        }

        private BuildFactSnapshot.AliasValueFact build() {
            return new BuildFactSnapshot.AliasValueFact(
                    stringValues.isEmpty() ? Set.of() : Set.copyOf(stringValues),
                    objectTypes.isEmpty() ? Set.of() : Set.copyOf(objectTypes)
            );
        }
    }

    private static final class ResolveContext {
        private final Frame<SourceValue>[] frames;
        private final InsnList instructions;
        private final ClassNode classNode;
        private final Map<String, String> staticStrings;
        private final Map<String, BuildFactSnapshot.AliasValueFact> instanceFieldFacts;
        private final BuildFactSnapshot.MethodConstraintFacts methodConstraints;
        private final Map<AbstractInsnNode, ResolvedConst> constCache = new IdentityHashMap<>();
        private final Map<AbstractInsnNode, List<Type>> classArrayCache = new IdentityHashMap<>();
        private final Set<AbstractInsnNode> classArrayMiss =
                Collections.newSetFromMap(new IdentityHashMap<>());
        private final Map<String, ResolvedConst> methodReturnCache;
        private final Set<String> methodReturnInProgress;

        private ResolveContext(Frame<SourceValue>[] frames,
                               InsnList instructions,
                               ClassNode classNode,
                               Map<String, String> staticStrings) {
            this(frames, instructions, classNode, staticStrings, Map.of(), BuildFactSnapshot.MethodConstraintFacts.empty(), new HashMap<>(), new HashSet<>());
        }

        private ResolveContext(Frame<SourceValue>[] frames,
                               InsnList instructions,
                               ClassNode classNode,
                               Map<String, String> staticStrings,
                               Map<String, BuildFactSnapshot.AliasValueFact> instanceFieldFacts) {
            this(frames, instructions, classNode, staticStrings, instanceFieldFacts, BuildFactSnapshot.MethodConstraintFacts.empty(), new HashMap<>(), new HashSet<>());
        }

        private ResolveContext(Frame<SourceValue>[] frames,
                               InsnList instructions,
                               ClassNode classNode,
                               Map<String, String> staticStrings,
                               Map<String, BuildFactSnapshot.AliasValueFact> instanceFieldFacts,
                               BuildFactSnapshot.MethodConstraintFacts methodConstraints) {
            this(frames, instructions, classNode, staticStrings, instanceFieldFacts, methodConstraints, new HashMap<>(), new HashSet<>());
        }

        private ResolveContext(Frame<SourceValue>[] frames,
                               InsnList instructions,
                               ClassNode classNode,
                               Map<String, String> staticStrings,
                               Map<String, BuildFactSnapshot.AliasValueFact> instanceFieldFacts,
                               BuildFactSnapshot.MethodConstraintFacts methodConstraints,
                               Map<String, ResolvedConst> methodReturnCache,
                               Set<String> methodReturnInProgress) {
            this.frames = frames;
            this.instructions = instructions;
            this.classNode = classNode;
            this.staticStrings = staticStrings == null ? Map.of() : staticStrings;
            this.instanceFieldFacts = instanceFieldFacts == null ? Map.of() : instanceFieldFacts;
            this.methodConstraints = methodConstraints == null
                    ? BuildFactSnapshot.MethodConstraintFacts.empty()
                    : methodConstraints;
            this.methodReturnCache = methodReturnCache == null ? new HashMap<>() : methodReturnCache;
            this.methodReturnInProgress = methodReturnInProgress == null ? new HashSet<>() : methodReturnInProgress;
        }
    }
}
