/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.core.asm;

import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.MethodCallUtils;
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
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ReflectionCallResolver {
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
    private static final String REASON_CLASS_CONST = "class_const";
    private static final String REASON_FOR_NAME = "for_name";
    private static final String REASON_CLASS_LOADER = "class_loader";
    private static final String REASON_CLASS_NEW_INSTANCE = "class_new_instance";
    private static final String REASON_METHOD_HANDLE = "method_handle";
    private static final String REASON_LAMBDA = "lambda";
    private static final String REASON_UNKNOWN = "unknown";

    private ReflectionCallResolver() {
    }

    public static void appendReflectionEdges(
            ClassFileEntity file,
            HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
            Map<MethodReference.Handle, MethodReference> methodMap,
            Map<MethodCallKey, MethodCallMeta> methodCallMeta) {
        if (file == null || methodCalls == null || methodMap == null || methodMap.isEmpty()) {
            return;
        }
        try {
            byte[] bytes = file.getFile();
            if (bytes == null || bytes.length == 0) {
                return;
            }
            ClassReader cr = new ClassReader(bytes);
            ClassNode cn = new ClassNode();
            cr.accept(cn, Const.GlobalASMOptions);
            appendReflectionEdges(cn, methodCalls, methodMap, methodCallMeta, true);
        } catch (Exception ex) {
            logger.warn("reflection edge build failed: {}", ex.toString());
        }
    }

    public static void appendReflectionEdges(
            ClassNode cn,
            HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
            Map<MethodReference.Handle, MethodReference> methodMap,
            Map<MethodCallKey, MethodCallMeta> methodCallMeta) {
        appendReflectionEdges(cn, methodCalls, methodMap, methodCallMeta, true);
    }

    public static void appendReflectionEdges(
            ClassNode cn,
            HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
            Map<MethodReference.Handle, MethodReference> methodMap,
            Map<MethodCallKey, MethodCallMeta> methodCallMeta,
            boolean includeInvokeDynamic) {
        if (cn == null || methodCalls == null || methodMap == null || methodMap.isEmpty()) {
            return;
        }
        if (cn.methods == null || cn.methods.isEmpty()) {
            return;
        }
        Map<String, String> staticStrings = collectStaticStringConstants(cn);
        if (includeInvokeDynamic) {
            appendInvokeDynamicEdges(cn, methodCalls, methodMap, methodCallMeta);
        }
        for (MethodNode mn : cn.methods) {
            if (mn == null || mn.instructions == null || mn.instructions.size() == 0) {
                continue;
            }
            if (!containsReflectionInvoke(mn.instructions)) {
                continue;
            }
            resolveInMethod(cn.name, mn, cn, staticStrings, methodCalls, methodMap, methodCallMeta);
        }
    }

    private static void appendInvokeDynamicEdges(
            ClassNode cn,
            HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
            Map<MethodReference.Handle, MethodReference> methodMap,
            Map<MethodCallKey, MethodCallMeta> methodCallMeta) {
        if (cn == null || cn.methods == null || cn.methods.isEmpty()) {
            return;
        }
        for (MethodNode mn : cn.methods) {
            if (mn == null || mn.instructions == null || mn.instructions.size() == 0) {
                continue;
            }
            InsnList instructions = mn.instructions;
            for (int i = 0, n = instructions.size(); i < n; i++) {
                AbstractInsnNode insn = instructions.get(i);
                if (!(insn instanceof InvokeDynamicInsnNode)) {
                    continue;
                }
                LambdaTarget lambda = resolveLambdaTarget((InvokeDynamicInsnNode) insn);
                if (lambda == null) {
                    continue;
                }
                if (!methodMap.containsKey(lambda.implHandle)) {
                    continue;
                }
                MethodReference.Handle caller = new MethodReference.Handle(
                        new ClassReference.Handle(cn.name), mn.name, mn.desc);
                HashSet<MethodReference.Handle> callees =
                        methodCalls.computeIfAbsent(caller, k -> new HashSet<>());
                MethodReference.Handle implHandle = lambda.implHandle;
                if (implHandle.getOpcode() == null || implHandle.getOpcode() < 0) {
                    implHandle = new MethodReference.Handle(
                            implHandle.getClassReference(),
                            Opcodes.INVOKEDYNAMIC,
                            implHandle.getName(),
                            implHandle.getDesc());
                }
                MethodCallUtils.addCallee(callees, implHandle);
                recordEdgeMeta(methodCallMeta, caller, implHandle,
                        MethodCallMeta.TYPE_INDY, MethodCallMeta.CONF_MEDIUM, REASON_LAMBDA);
                if (lambda.samHandle != null && methodMap.containsKey(lambda.samHandle)) {
                    HashSet<MethodReference.Handle> samCallees =
                            methodCalls.computeIfAbsent(lambda.samHandle, k -> new HashSet<>());
                    MethodCallUtils.addCallee(samCallees, implHandle);
                    recordEdgeMeta(methodCallMeta, lambda.samHandle, implHandle,
                            MethodCallMeta.TYPE_INDY, MethodCallMeta.CONF_MEDIUM, REASON_LAMBDA);
                }
            }
        }
    }

    private static LambdaTarget resolveLambdaTarget(InvokeDynamicInsnNode indy) {
        if (indy == null || indy.bsm == null) {
            return null;
        }
        if (!LMF_OWNER.equals(indy.bsm.getOwner())) {
            return null;
        }
        String bsmName = indy.bsm.getName();
        if (!"metafactory".equals(bsmName) && !"altMetafactory".equals(bsmName)) {
            return null;
        }
        if (indy.bsmArgs == null || indy.bsmArgs.length < 3) {
            return null;
        }
        Object arg0 = indy.bsmArgs[0];
        Object arg1 = indy.bsmArgs[1];
        Object arg2 = indy.bsmArgs[2];
        if (!(arg0 instanceof Type) || !(arg1 instanceof Handle) || !(arg2 instanceof Type)) {
            return null;
        }
        Type samType = (Type) arg0;
        Type instantiatedType = (Type) arg2;
        if (samType.getSort() != Type.METHOD || instantiatedType.getSort() != Type.METHOD) {
            return null;
        }
        Handle impl = (Handle) arg1;
        Type fiType = Type.getReturnType(indy.desc);
        if (fiType.getSort() != Type.OBJECT) {
            return null;
        }
        MethodReference.Handle samHandle = new MethodReference.Handle(
                new ClassReference.Handle(fiType.getInternalName()),
                indy.name, samType.getDescriptor());
        MethodReference.Handle implHandle = new MethodReference.Handle(
                new ClassReference.Handle(impl.getOwner()),
                impl.getName(), impl.getDesc());
        return new LambdaTarget(samHandle, implHandle);
    }

    private static final class LambdaTarget {
        private final MethodReference.Handle samHandle;
        private final MethodReference.Handle implHandle;

        private LambdaTarget(MethodReference.Handle samHandle,
                             MethodReference.Handle implHandle) {
            this.samHandle = samHandle;
            this.implHandle = implHandle;
        }
    }

    private static final class ResolvedConst {
        private final Object value;
        private final String reason;

        private ResolvedConst(Object value, String reason) {
            this.value = value;
            this.reason = reason;
        }
    }

    private static final ResolvedConst UNRESOLVED_CONST = new ResolvedConst(null, REASON_UNKNOWN);

    private static final class ResolvedString {
        private final String value;
        private final String reason;

        private ResolvedString(String value, String reason) {
            this.value = value;
            this.reason = reason;
        }
    }

    private static final class TargetResolve {
        private final List<MethodReference.Handle> targets;
        private final String reason;

        private TargetResolve(List<MethodReference.Handle> targets, String reason) {
            this.targets = targets;
            this.reason = reason;
        }
    }

    private static final class ResolveContext {
        private final Frame<SourceValue>[] frames;
        private final InsnList instructions;
        private final ClassNode cn;
        private final Map<String, String> staticStrings;
        private final Map<AbstractInsnNode, ResolvedConst> constCache = new IdentityHashMap<>();
        private final Map<AbstractInsnNode, List<Type>> classArrayCache = new IdentityHashMap<>();
        private final Set<AbstractInsnNode> classArrayMiss =
                Collections.newSetFromMap(new IdentityHashMap<>());

        private ResolveContext(Frame<SourceValue>[] frames,
                               InsnList instructions,
                               ClassNode cn,
                               Map<String, String> staticStrings) {
            this.frames = frames;
            this.instructions = instructions;
            this.cn = cn;
            this.staticStrings = staticStrings;
        }
    }

    private static boolean containsReflectionInvoke(InsnList instructions) {
        for (int i = 0, n = instructions.size(); i < n; i++) {
            AbstractInsnNode insn = instructions.get(i);
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode mi = (MethodInsnNode) insn;
                if (isReflectionInvoke(mi) || isClassNewInstance(mi) || isMethodHandleInvoke(mi)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void resolveInMethod(
            String owner,
            MethodNode mn,
            ClassNode cn,
            Map<String, String> staticStrings,
            HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
            Map<MethodReference.Handle, MethodReference> methodMap,
            Map<MethodCallKey, MethodCallMeta> methodCallMeta) {
        try {
            Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());
            Frame<SourceValue>[] frames = analyzer.analyze(owner, mn);
            InsnList instructions = mn.instructions;
            ResolveContext ctx = new ResolveContext(frames, instructions, cn, staticStrings);
            for (int i = 0, n = instructions.size(); i < n; i++) {
                AbstractInsnNode insn = instructions.get(i);
                if (!(insn instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode invoke = (MethodInsnNode) insn;
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
                    MethodReference.Handle target = new MethodReference.Handle(
                            new ClassReference.Handle(classInfo.value), "<init>", "()V");
                    if (!methodMap.containsKey(target)) {
                        continue;
                    }
                    MethodReference.Handle callTarget =
                            withSyntheticOpcode(target, methodMap, Opcodes.INVOKESPECIAL);
                    MethodReference.Handle caller = new MethodReference.Handle(
                            new ClassReference.Handle(owner), mn.name, mn.desc);
                    HashSet<MethodReference.Handle> callees =
                            methodCalls.computeIfAbsent(caller, k -> new HashSet<>());
                    MethodCallUtils.addCallee(callees, callTarget);
                    recordEdgeMeta(methodCallMeta, caller, callTarget,
                            MethodCallMeta.TYPE_REFLECTION, MethodCallMeta.CONF_LOW,
                            REASON_CLASS_NEW_INSTANCE + "_" + classInfo.reason);
                } else if (isReflectionInvoke(invoke)) {
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
                    SourceValue objVal = frame.getStack(objIndex);
                    MethodInsnNode creator = findSingleCreator(objVal);
                    if (creator == null) {
                        continue;
                    }
                    TargetResolve resolved =
                            resolveTarget(creator, ctx, methodMap);
                    if (resolved == null || resolved.targets == null || resolved.targets.isEmpty()) {
                        continue;
                    }
                    MethodReference.Handle caller = new MethodReference.Handle(
                            new ClassReference.Handle(owner), mn.name, mn.desc);
                    HashSet<MethodReference.Handle> callees =
                            methodCalls.computeIfAbsent(caller, k -> new HashSet<>());
                    for (MethodReference.Handle target : resolved.targets) {
                        MethodReference.Handle callTarget =
                                withSyntheticOpcode(target, methodMap, Opcodes.INVOKEVIRTUAL);
                        MethodCallUtils.addCallee(callees, callTarget);
                        recordEdgeMeta(methodCallMeta, caller, callTarget,
                                MethodCallMeta.TYPE_REFLECTION, MethodCallMeta.CONF_LOW, resolved.reason);
                    }
                } else if (isMethodHandleInvoke(invoke)) {
                    Frame<SourceValue> frame = frames[i];
                    if (frame == null) {
                        continue;
                    }
                    int argCount = Type.getArgumentTypes(invoke.desc).length;
                    int objIndex = frame.getStackSize() - argCount - 1;
                    if (objIndex < 0) {
                        continue;
                    }
                    SourceValue objVal = frame.getStack(objIndex);
                    MethodInsnNode creator = findSingleHandleCreator(objVal);
                    if (creator == null) {
                        continue;
                    }
                    TargetResolve resolved =
                            resolveHandleTarget(creator, ctx, methodMap);
                    if (resolved == null || resolved.targets == null || resolved.targets.isEmpty()) {
                        continue;
                    }
                    MethodReference.Handle caller = new MethodReference.Handle(
                            new ClassReference.Handle(owner), mn.name, mn.desc);
                    HashSet<MethodReference.Handle> callees =
                            methodCalls.computeIfAbsent(caller, k -> new HashSet<>());
                    for (MethodReference.Handle target : resolved.targets) {
                        int fallbackOpcode = "<init>".equals(target.getName())
                                ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL;
                        MethodReference.Handle callTarget =
                                withSyntheticOpcode(target, methodMap, fallbackOpcode);
                        MethodCallUtils.addCallee(callees, callTarget);
                        recordEdgeMeta(methodCallMeta, caller, callTarget,
                                MethodCallMeta.TYPE_METHOD_HANDLE, MethodCallMeta.CONF_LOW, resolved.reason);
                    }
                }
            }
        } catch (Exception ex) {
            logger.warn("reflection analyze failed: {}.{}", owner, mn.name);
        }
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

    private static MethodInsnNode findSingleCreator(SourceValue value) {
        if (value == null || value.insns == null || value.insns.isEmpty()) {
            return null;
        }
        MethodInsnNode found = null;
        for (AbstractInsnNode src : value.insns) {
            if (!(src instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode mi = (MethodInsnNode) src;
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

    private static MethodInsnNode findSingleHandleCreator(SourceValue value) {
        if (value == null || value.insns == null || value.insns.isEmpty()) {
            return null;
        }
        MethodInsnNode found = null;
        for (AbstractInsnNode src : value.insns) {
            if (!(src instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode mi = (MethodInsnNode) src;
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

    private static TargetResolve resolveTarget(
            MethodInsnNode creator,
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
        ResolvedString classInfo = resolveClassNameWithReason(frame.getStack(objIndex), ctx);
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
            SourceValue nameVal = frame.getStack(stackSize - argCount);
            nameInfo = resolveStringConstantWithReason(nameVal, ctx, 0);
            if (nameInfo == null || nameInfo.value == null || nameInfo.value.isEmpty()) {
                return null;
            }
            SourceValue paramVal = frame.getStack(stackSize - argCount + 1);    
            params = resolveClassArray(paramVal, ctx);
            methodName = nameInfo.value;
        } else if (isGetConstructor(creator)) {
            SourceValue paramVal = frame.getStack(stackSize - argCount);        
            params = resolveClassArray(paramVal, ctx);
            methodName = "<init>";
        } else {
            return null;
        }
        String reason = buildReflectionReason(classInfo, nameInfo);
        List<MethodReference.Handle> targets;
        if (params == null) {
            targets = findUniqueMethodByName(methodMap, classInfo.value, methodName);
        } else {
            targets = findMethods(methodMap, classInfo.value, methodName, params);
        }
        if (targets == null || targets.isEmpty()) {
            return null;
        }
        return new TargetResolve(targets, reason);
    }

    private static TargetResolve resolveHandleTarget(
            MethodInsnNode creator,
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
        int stackSize = frame.getStackSize();
        int base = stackSize - argCount;
        if (base < 0) {
            return null;
        }
        ResolvedString classInfo;
        String methodName = null;
        MethodTypeInfo mt;
        if ("findConstructor".equals(lookupName)) {
            SourceValue classVal = frame.getStack(base);
            SourceValue mtVal = frame.getStack(base + 1);
            classInfo = resolveClassNameWithReason(classVal, ctx);
            mt = resolveMethodType(mtVal, ctx);
            if (classInfo == null || classInfo.value == null || mt == null) {
                return null;
            }
            if (mt.returnType.getSort() != Type.VOID) {
                return null;
            }
            methodName = "<init>";
        } else if ("findSpecial".equals(lookupName)) {
            if (argCount != 4) {
                return null;
            }
            SourceValue classVal = frame.getStack(base);
            SourceValue nameVal = frame.getStack(base + 1);
            SourceValue mtVal = frame.getStack(base + 2);
            classInfo = resolveClassNameWithReason(classVal, ctx);
            ResolvedString nameInfo = resolveStringConstantWithReason(nameVal, ctx, 0);
            mt = resolveMethodType(mtVal, ctx);
            if (classInfo == null || classInfo.value == null || nameInfo == null || nameInfo.value == null || mt == null) {
                return null;
            }
            methodName = nameInfo.value;
        } else {
            if (argCount != 3) {
                return null;
            }
            SourceValue classVal = frame.getStack(base);
            SourceValue nameVal = frame.getStack(base + 1);
            SourceValue mtVal = frame.getStack(base + 2);
            classInfo = resolveClassNameWithReason(classVal, ctx);
            ResolvedString nameInfo = resolveStringConstantWithReason(nameVal, ctx, 0);
            mt = resolveMethodType(mtVal, ctx);
            if (classInfo == null || classInfo.value == null || nameInfo == null || nameInfo.value == null || mt == null) {
                return null;
            }
            methodName = nameInfo.value;
        }
        String desc = Type.getMethodDescriptor(mt.returnType, mt.paramTypes.toArray(new Type[0]));
        List<MethodReference.Handle> out = new ArrayList<>();
        for (MethodReference.Handle handle : methodMap.keySet()) {
            if (!classInfo.value.equals(handle.getClassReference().getName())) {
                continue;
            }
            if (!methodName.equals(handle.getName())) {
                continue;
            }
            if (!desc.equals(handle.getDesc())) {
                continue;
            }
            out.add(handle);
        }
        if (out.isEmpty()) {
            return null;
        }
        return new TargetResolve(out, REASON_METHOD_HANDLE);
    }

    private static boolean isGetMethod(MethodInsnNode mi) {
        return CLASS_OWNER.equals(mi.owner)
                && ("getMethod".equals(mi.name) || "getDeclaredMethod".equals(mi.name))
                && "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;".equals(mi.desc);
    }

    private static boolean isMethodHandleInvoke(MethodInsnNode mi) {
        if (mi == null || !MH_OWNER.equals(mi.owner)) {
            return false;
        }
        if (!"invoke".equals(mi.name) && !"invokeExact".equals(mi.name)
                && !"invokeWithArguments".equals(mi.name)) {
            return false;
        }
        return mi.desc != null && mi.desc.startsWith("(");
    }

    private static boolean isLookupFind(MethodInsnNode mi) {
        if (mi == null || !LOOKUP_OWNER.equals(mi.owner)) {
            return false;
        }
        String name = mi.name;
        if ("findVirtual".equals(name)
                && "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;".equals(mi.desc)) {
            return true;
        }
        if ("findStatic".equals(name)
                && "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;".equals(mi.desc)) {
            return true;
        }
        if ("findSpecial".equals(name)
                && "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;".equals(mi.desc)) {
            return true;
        }
        return "findConstructor".equals(name)
                && "(Ljava/lang/Class;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;".equals(mi.desc);
    }

    private static boolean isGetConstructor(MethodInsnNode mi) {
        return CLASS_OWNER.equals(mi.owner)
                && ("getConstructor".equals(mi.name) || "getDeclaredConstructor".equals(mi.name))
                && "([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;".equals(mi.desc);
    }

    private static List<MethodReference.Handle> findMethods(
            Map<MethodReference.Handle, MethodReference> methodMap,
            String className,
            String methodName,
            List<Type> params) {
        if (className == null || methodName == null || params == null) {
            return null;
        }
        List<MethodReference.Handle> out = new ArrayList<>();
        for (MethodReference.Handle handle : methodMap.keySet()) {
            if (!className.equals(handle.getClassReference().getName())) {
                continue;
            }
            if (!methodName.equals(handle.getName())) {
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

    private static List<MethodReference.Handle> findUniqueMethodByName(
            Map<MethodReference.Handle, MethodReference> methodMap,
            String className,
            String methodName) {
        if (className == null || methodName == null) {
            return null;
        }
        MethodReference.Handle found = null;
        for (MethodReference.Handle handle : methodMap.keySet()) {
            if (!className.equals(handle.getClassReference().getName())) {
                continue;
            }
            if (!methodName.equals(handle.getName())) {
                continue;
            }
            if (found != null && !found.equals(handle)) {
                return null;
            }
            found = handle;
        }
        if (found == null) {
            return null;
        }
        return Collections.singletonList(found);
    }

    private static String resolveClassName(
            SourceValue value,
            ResolveContext ctx) {
        AbstractInsnNode src = getSingleInsn(value);
        if (src instanceof LdcInsnNode) {
            Object cst = ((LdcInsnNode) src).cst;
            if (cst instanceof Type) {
                return ((Type) cst).getInternalName();
            }
        } else if (src instanceof VarInsnNode) {
            ResolvedString local = resolveClassNameFromLocal((VarInsnNode) src, ctx, 0);
            if (local != null) {
                return normalizeClassName(local.value);
            }
        } else if (src instanceof MethodInsnNode) {
            MethodInsnNode mi = (MethodInsnNode) src;
            if (isForName(mi) || isLoadClass(mi)) {
                String name = resolveClassNameFromCall(mi, ctx);
                return normalizeClassName(name);
            }
        } else if (src instanceof InvokeDynamicInsnNode || src instanceof MethodInsnNode
                || src instanceof FieldInsnNode) {
            String name = resolveStringConstant(value, ctx, 0);
            return normalizeClassName(name);
        }
        return null;
    }

    private static ResolvedString resolveClassNameWithReason(
            SourceValue value,
            ResolveContext ctx) {
        AbstractInsnNode src = getSingleInsn(value);
        if (src instanceof LdcInsnNode) {
            Object cst = ((LdcInsnNode) src).cst;
            if (cst instanceof Type) {
                return new ResolvedString(((Type) cst).getInternalName(), REASON_CLASS_CONST);
            }
        } else if (src instanceof VarInsnNode) {
            ResolvedString local = resolveClassNameFromLocal((VarInsnNode) src, ctx, 0);
            if (local != null && local.value != null) {
                String normalized = normalizeClassName(local.value);
                if (normalized != null) {
                    return new ResolvedString(normalized, combineReasons(REASON_LOCAL, local.reason));
                }
            }
        } else if (src instanceof MethodInsnNode) {
            MethodInsnNode mi = (MethodInsnNode) src;
            if (isForName(mi) || isLoadClass(mi)) {
                ResolvedString name = resolveClassNameFromCallWithReason(mi, ctx, 0);
                if (name == null || name.value == null) {
                    return null;
                }
                String normalized = normalizeClassName(name.value);
                if (normalized == null) {
                    return null;
                }
                return new ResolvedString(normalized, REASON_FOR_NAME + "_" + name.reason);
            }
        } else if (src instanceof InvokeDynamicInsnNode || src instanceof MethodInsnNode
                || src instanceof FieldInsnNode) {
            ResolvedString name = resolveStringConstantWithReason(value, ctx, 0);
            if (name == null || name.value == null) {
                return null;
            }
            String normalized = normalizeClassName(name.value);
            if (normalized == null) {
                return null;
            }
            return new ResolvedString(normalized, name.reason);
        }
        return null;
    }

    private static boolean isForName(MethodInsnNode mi) {
        return CLASS_OWNER.equals(mi.owner)
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

    private static String resolveClassNameFromCall(
            MethodInsnNode mi,
            ResolveContext ctx) {
        int idx = ctx.instructions.indexOf(mi);
        if (idx < 0) {
            return null;
        }
        Frame<SourceValue> frame = ctx.frames[idx];
        if (frame == null) {
            return null;
        }
        int argCount = Type.getArgumentTypes(mi.desc).length;
        int stackSize = frame.getStackSize();
        int argIndex = stackSize - argCount;
        if (argIndex < 0) {
            return null;
        }
        return resolveStringConstant(frame.getStack(argIndex), ctx, 0);
    }

    private static ResolvedString resolveClassNameFromCallWithReason(
            MethodInsnNode mi,
            ResolveContext ctx,
            int depth) {
        int idx = ctx.instructions.indexOf(mi);
        if (idx < 0) {
            return null;
        }
        Frame<SourceValue> frame = ctx.frames[idx];
        if (frame == null) {
            return null;
        }
        int argCount = Type.getArgumentTypes(mi.desc).length;
        int stackSize = frame.getStackSize();
        int argIndex = stackSize - argCount;
        if (argIndex < 0) {
            return null;
        }
        ResolvedString className = resolveStringConstantWithReason(frame.getStack(argIndex), ctx, depth + 1);
        if (className == null || className.value == null) {
            return null;
        }
        String reason = className.reason;
        if (isForName(mi) && "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;".equals(mi.desc)) {
            if (argIndex + 2 < stackSize) {
                String loaderReason = resolveClassLoaderReason(frame.getStack(argIndex + 2), ctx, depth + 1);
                reason = combineReasons(reason, REASON_CLASS_LOADER + "_" + loaderReason);
            }
        } else if (isLoadClass(mi)) {
            int recvIndex = stackSize - argCount - 1;
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
        if (loadIndex < 0) {
            return null;
        }
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

    private static String resolveStringConstant(SourceValue value,
                                                ResolveContext ctx,
                                                int depth) {
        Object resolved = resolveConstValue(value, ctx, depth);
        if (resolved instanceof String) {
            String s = ((String) resolved).trim();
            return s.isEmpty() ? null : s;
        }
        return null;
    }

    private static ResolvedString resolveStringConstantWithReason(SourceValue value,
                                                                  ResolveContext ctx,
                                                                  int depth) {
        ResolvedConst resolved = resolveConstValueWithReason(value, ctx, depth);
        if (resolved != null && resolved.value instanceof String) {
            String s = ((String) resolved.value).trim();
            if (!s.isEmpty()) {
                return new ResolvedString(s, resolved.reason == null ? REASON_UNKNOWN : resolved.reason);
            }
        }
        return null;
    }

    private static String combineReasons(String left, String right) {
        String l = left == null ? "" : left.trim();
        String r = right == null ? "" : right.trim();
        if (l.isEmpty()) {
            return r.isEmpty() ? REASON_UNKNOWN : r;
        }
        if (r.isEmpty()) {
            return l;
        }
        return l + "+" + r;
    }

    private static String resolveClassLoaderReason(SourceValue value,
                                                   ResolveContext ctx,
                                                   int depth) {
        if (depth > MAX_CONST_DEPTH) {
            return REASON_UNKNOWN;
        }
        AbstractInsnNode src = getSingleInsn(value);
        if (!(src instanceof MethodInsnNode)) {
            return REASON_UNKNOWN;
        }
        MethodInsnNode mi = (MethodInsnNode) src;
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
        String v = raw.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if ((c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '_' || c == '-') {
                sb.append(c);
            } else if (c == '/' || c == '.') {
                sb.append('_');
            }
        }
        if (sb.length() == 0) {
            return REASON_UNKNOWN;
        }
        return sb.toString();
    }

    private static String buildReflectionReason(ResolvedString classInfo, ResolvedString nameInfo) {
        String classReason = classInfo == null ? REASON_UNKNOWN : classInfo.reason;
        String nameReason = nameInfo == null ? REASON_UNKNOWN : nameInfo.reason;
        StringBuilder sb = new StringBuilder();
        if (classReason != null && !classReason.isEmpty()) {
            sb.append("class_").append(classReason);
        }
        if (nameReason != null && !nameReason.isEmpty()) {
            if (sb.length() > 0) {
                sb.append("+");
            }
            sb.append("name_").append(nameReason);
        }
        return sb.length() == 0 ? REASON_UNKNOWN : sb.toString();
    }

    private static List<Type> resolveClassArray(
            SourceValue value,
            ResolveContext ctx) {
        AbstractInsnNode src = getSingleInsn(value);
        if (!(src instanceof TypeInsnNode)) {
            return null;
        }
        if (ctx.classArrayCache.containsKey(src)) {
            return ctx.classArrayCache.get(src);
        }
        if (ctx.classArrayMiss.contains(src)) {
            return null;
        }
        TypeInsnNode tin = (TypeInsnNode) src;
        if (tin.getOpcode() != Opcodes.ANEWARRAY || !"java/lang/Class".equals(tin.desc)) {
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
            SourceValue arrVal = frame.getStack(stackSize - 3);
            if (!containsSource(arrVal, src)) {
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
        if (expectedSize == 0) {
            List<Type> out = elements.isEmpty() ? Collections.emptyList() : null;
            if (out == null) {
                ctx.classArrayMiss.add(src);
                return null;
            }
            ctx.classArrayCache.put(src, out);
            return out;
        }
        if (elements.size() != expectedSize) {
            ctx.classArrayMiss.add(src);
            return null;
        }
        List<Type> out = new ArrayList<>(expectedSize);
        for (int i = 0; i < expectedSize; i++) {
            Type t = elements.get(i);
            if (t == null) {
                ctx.classArrayMiss.add(src);
                return null;
            }
            out.add(t);
        }
        ctx.classArrayCache.put(src, out);
        return out;
    }

    private static Type resolveTypeConstant(
            SourceValue value,
            ResolveContext ctx) {
        AbstractInsnNode src = getSingleInsn(value);
        if (src instanceof LdcInsnNode) {
            Object cst = ((LdcInsnNode) src).cst;
            if (cst instanceof Type) {
                return (Type) cst;
            }
        } else if (src instanceof FieldInsnNode) {
            FieldInsnNode fin = (FieldInsnNode) src;
            if ("TYPE".equals(fin.name) && "Ljava/lang/Class;".equals(fin.desc)) {
                return primitiveTypeForOwner(fin.owner);
            }
        } else if (src instanceof MethodInsnNode) {
            MethodInsnNode mi = (MethodInsnNode) src;
            if (isForName(mi) || isLoadClass(mi)) {
                String name = resolveClassNameFromCall(mi, ctx);
                String cls = normalizeClassName(name);
                if (cls != null) {
                    return Type.getObjectType(cls);
                }
            }
        } else if (src instanceof InvokeDynamicInsnNode || src instanceof FieldInsnNode) {
            String name = resolveStringConstant(value, ctx, 0);
            String cls = normalizeClassName(name);
            if (cls != null) {
                return Type.getObjectType(cls);
            }
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
        return null;
    }

    private static Integer resolveIntConstant(SourceValue value) {
        AbstractInsnNode src = getSingleInsn(value);
        if (src instanceof InsnNode) {
            switch (src.getOpcode()) {
                case Opcodes.ICONST_M1:
                    return -1;
                case Opcodes.ICONST_0:
                    return 0;
                case Opcodes.ICONST_1:
                    return 1;
                case Opcodes.ICONST_2:
                    return 2;
                case Opcodes.ICONST_3:
                    return 3;
                case Opcodes.ICONST_4:
                    return 4;
                case Opcodes.ICONST_5:
                    return 5;
                default:
                    return null;
            }
        } else if (src instanceof IntInsnNode) {
            IntInsnNode insn = (IntInsnNode) src;
            return insn.operand;
        } else if (src instanceof LdcInsnNode) {
            Object cst = ((LdcInsnNode) src).cst;
            if (cst instanceof Integer) {
                return (Integer) cst;
            }
        }
        return null;
    }

    private static Map<String, String> collectStaticStringConstants(ClassNode cn) {
        if (cn == null || cn.fields == null || cn.fields.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> out = new HashMap<>();
        cn.fields.forEach(f -> {
            if (f == null || f.value == null) {
                return;
            }
            if ((f.access & (Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)) == (Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)) {
                if (f.value instanceof String) {
                    String key = cn.name + "#" + f.name;
                    out.put(key, ((String) f.value).trim());
                }
            }
        });
        return out;
    }

    private static Object resolveConstValue(SourceValue value,
                                            ResolveContext ctx,
                                            int depth) {
        ResolvedConst resolved = resolveConstValueWithReason(value, ctx, depth);
        return resolved == null ? null : resolved.value;
    }

    private static ResolvedConst resolveConstValueWithReason(SourceValue value,
                                                             ResolveContext ctx,
                                                             int depth) {
        if (depth > MAX_CONST_DEPTH) {
            return null;
        }
        AbstractInsnNode src = getSingleInsn(value);
        if (src == null) {
            return null;
        }
        ResolvedConst cached = ctx.constCache.get(src);
        if (cached != null) {
            return cached == UNRESOLVED_CONST ? null : cached;
        }
        ResolvedConst resolved = null;
        if (src instanceof LdcInsnNode) {
            Object cst = ((LdcInsnNode) src).cst;
            if (cst == null) {
                resolved = null;
            } else {
                resolved = new ResolvedConst(cst, REASON_CONST);
            }
        }
        if (src instanceof InsnNode) {
            switch (src.getOpcode()) {
                case Opcodes.ICONST_M1:
                    resolved = new ResolvedConst(-1, REASON_CONST);
                    break;
                case Opcodes.ICONST_0:
                    resolved = new ResolvedConst(0, REASON_CONST);
                    break;
                case Opcodes.ICONST_1:
                    resolved = new ResolvedConst(1, REASON_CONST);
                    break;
                case Opcodes.ICONST_2:
                    resolved = new ResolvedConst(2, REASON_CONST);
                    break;
                case Opcodes.ICONST_3:
                    resolved = new ResolvedConst(3, REASON_CONST);
                    break;
                case Opcodes.ICONST_4:
                    resolved = new ResolvedConst(4, REASON_CONST);
                    break;
                case Opcodes.ICONST_5:
                    resolved = new ResolvedConst(5, REASON_CONST);
                    break;
                case Opcodes.LCONST_0:
                    resolved = new ResolvedConst(0L, REASON_CONST);
                    break;
                case Opcodes.LCONST_1:
                    resolved = new ResolvedConst(1L, REASON_CONST);
                    break;
                case Opcodes.FCONST_0:
                    resolved = new ResolvedConst(0.0f, REASON_CONST);
                    break;
                case Opcodes.FCONST_1:
                    resolved = new ResolvedConst(1.0f, REASON_CONST);
                    break;
                case Opcodes.FCONST_2:
                    resolved = new ResolvedConst(2.0f, REASON_CONST);
                    break;
                case Opcodes.DCONST_0:
                    resolved = new ResolvedConst(0.0d, REASON_CONST);
                    break;
                case Opcodes.DCONST_1:
                    resolved = new ResolvedConst(1.0d, REASON_CONST);
                    break;
                default:
                    resolved = null;
                    break;
            }
        }
        if (src instanceof IntInsnNode) {
            resolved = new ResolvedConst(((IntInsnNode) src).operand, REASON_CONST);
        }
        if (src instanceof VarInsnNode) {
            VarInsnNode vin = (VarInsnNode) src;
            if (vin.getOpcode() == Opcodes.ALOAD) {
                int loadIndex = ctx.instructions.indexOf(vin);
                int storeIndex = findPreviousAstore(ctx, loadIndex, vin.var);
                if (storeIndex >= 0 && ctx.frames[storeIndex] != null) {
                    Frame<SourceValue> storeFrame = ctx.frames[storeIndex];
                    if (storeFrame.getStackSize() > 0) {
                        ResolvedConst local = resolveConstValueWithReason(
                                storeFrame.getStack(storeFrame.getStackSize() - 1),
                                ctx,
                                depth + 1);
                        if (local != null) {
                            resolved = new ResolvedConst(local.value, combineReasons(REASON_LOCAL, local.reason));
                        }
                    }
                }
            }
        }
        if (src instanceof FieldInsnNode) {
            FieldInsnNode fin = (FieldInsnNode) src;
            if (fin.getOpcode() == Opcodes.GETSTATIC) {
                String key = fin.owner + "#" + fin.name;
                if (ctx.staticStrings != null && ctx.staticStrings.containsKey(key)) {
                    resolved = new ResolvedConst(ctx.staticStrings.get(key), REASON_STATIC);
                }
            }
        }
        if (src instanceof InvokeDynamicInsnNode) {
            String v = resolveInvokeDynamicString((InvokeDynamicInsnNode) src, ctx, depth + 1);
            if (v != null) {
                resolved = new ResolvedConst(v, REASON_CONCAT);
            }
        }
        if (src instanceof MethodInsnNode) {
            MethodInsnNode mi = (MethodInsnNode) src;
            if (isStringBuilderToString(mi)) {
                String v = resolveStringBuilderToString(mi, ctx, depth + 1);
                if (v != null) {
                    resolved = new ResolvedConst(v, REASON_STRING_BUILDER);
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
                String v = resolveStringValueOf(mi, ctx, depth + 1);
                if (v != null) {
                    resolved = new ResolvedConst(v, REASON_VALUE_OF);
                }
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
        int stackSize = frame.getStackSize();
        int base = stackSize - argCount;
        if (base < 0) {
            return null;
        }

        if ("java/lang/String".equals(mi.owner)) {
            if (base - 1 < 0) {
                return null;
            }
            Object recv = resolveConstValue(frame.getStack(base - 1), ctx, depth + 1);
            if (!(recv instanceof String)) {
                return null;
            }
            String self = (String) recv;
            if ("concat".equals(mi.name)
                    && "(Ljava/lang/String;)Ljava/lang/String;".equals(mi.desc)) {
                Object arg = resolveConstValue(frame.getStack(base), ctx, depth + 1);
                if (arg == null) {
                    return null;
                }
                return new ResolvedConst(self.concat(String.valueOf(arg)), REASON_STRING_API);
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
            if ("toUpperCase".equals(mi.name) && "()Ljava/lang/String;".equals(mi.desc)) {
                return new ResolvedConst(self.toUpperCase(Locale.ROOT), REASON_STRING_API);
            }
            if ("substring".equals(mi.name) && "(I)Ljava/lang/String;".equals(mi.desc)) {
                Object beginObj = resolveConstValue(frame.getStack(base), ctx, depth + 1);
                if (!(beginObj instanceof Integer)) {
                    return null;
                }
                int begin = (Integer) beginObj;
                if (begin < 0 || begin > self.length()) {
                    return null;
                }
                return new ResolvedConst(self.substring(begin), REASON_STRING_API);
            }
            if ("substring".equals(mi.name) && "(II)Ljava/lang/String;".equals(mi.desc)) {
                Object beginObj = resolveConstValue(frame.getStack(base), ctx, depth + 1);
                Object endObj = resolveConstValue(frame.getStack(base + 1), ctx, depth + 1);
                if (!(beginObj instanceof Integer) || !(endObj instanceof Integer)) {
                    return null;
                }
                int begin = (Integer) beginObj;
                int end = (Integer) endObj;
                if (begin < 0 || end < begin || end > self.length()) {
                    return null;
                }
                return new ResolvedConst(self.substring(begin, end), REASON_STRING_API);
            }
            if ("replace".equals(mi.name)
                    && "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;".equals(mi.desc)) {
                Object fromObj = resolveConstValue(frame.getStack(base), ctx, depth + 1);
                Object toObj = resolveConstValue(frame.getStack(base + 1), ctx, depth + 1);
                if (fromObj == null || toObj == null) {
                    return null;
                }
                return new ResolvedConst(self.replace(String.valueOf(fromObj), String.valueOf(toObj)),
                        REASON_STRING_API);
            }
            if ("replace".equals(mi.name) && "(CC)Ljava/lang/String;".equals(mi.desc)) {
                Character from = resolveCharConstant(frame.getStack(base), ctx, depth + 1);
                Character to = resolveCharConstant(frame.getStack(base + 1), ctx, depth + 1);
                if (from == null || to == null) {
                    return null;
                }
                return new ResolvedConst(self.replace(from, to), REASON_STRING_API);
            }
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
            String internal = classInfo.value;
            String dotName = internal.replace('/', '.');
            String out;
            if ("getSimpleName".equals(mi.name)) {
                int idxDot = dotName.lastIndexOf('.');
                out = idxDot >= 0 ? dotName.substring(idxDot + 1) : dotName;
            } else {
                out = dotName;
            }
            return new ResolvedConst(out, combineReasons(REASON_STRING_API, classInfo.reason));
        }
        return null;
    }

    private static Character resolveCharConstant(SourceValue value,
                                                 ResolveContext ctx,
                                                 int depth) {
        Object v = resolveConstValue(value, ctx, depth + 1);
        if (v instanceof Character) {
            return (Character) v;
        }
        if (v instanceof Integer) {
            int iv = (Integer) v;
            if (iv < Character.MIN_VALUE || iv > Character.MAX_VALUE) {
                return null;
            }
            return (char) iv;
        }
        return null;
    }

    private static boolean isStringBuilderToString(MethodInsnNode mi) {
        if (mi == null || !"toString".equals(mi.name) || !"()Ljava/lang/String;".equals(mi.desc)) {
            return false;
        }
        return "java/lang/StringBuilder".equals(mi.owner)
                || "java/lang/StringBuffer".equals(mi.owner);
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
        int stackSize = frame.getStackSize();
        int argIndex = stackSize - argCount;
        if (argIndex < 0) {
            return null;
        }
        Object v = resolveConstValue(frame.getStack(argIndex), ctx, depth + 1);
        if (v == null) {
            return null;
        }
        return String.valueOf(v);
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
        List<String> dyn = new ArrayList<>();
        int stackSize = frame.getStackSize();
        int base = stackSize - args.length;
        if (base < 0) {
            return null;
        }
        for (int i = 0; i < args.length; i++) {
            Object v = resolveConstValue(frame.getStack(base + i),
                    ctx, depth + 1);
            if (v == null) {
                return null;
            }
            dyn.add(String.valueOf(v));
        }
        String bsmName = indy.bsm.getName();
        if ("makeConcat".equals(bsmName)) {
            StringBuilder sb = new StringBuilder();
            for (String s : dyn) {
                sb.append(s);
            }
            return sb.toString();
        }
        if (!"makeConcatWithConstants".equals(bsmName)) {
            return null;
        }
        if (indy.bsmArgs == null || indy.bsmArgs.length == 0) {
            return null;
        }
        if (!(indy.bsmArgs[0] instanceof String)) {
            return null;
        }
        String recipe = (String) indy.bsmArgs[0];
        List<String> consts = new ArrayList<>();
        for (int i = 1; i < indy.bsmArgs.length; i++) {
            Object c = indy.bsmArgs[i];
            if (c == null) {
                return null;
            }
            consts.add(String.valueOf(c));
        }
        return applyConcatRecipe(recipe, dyn, consts);
    }

    private static String applyConcatRecipe(String recipe, List<String> dyn, List<String> consts) {
        StringBuilder sb = new StringBuilder();
        int dynIdx = 0;
        int constIdx = 0;
        for (int i = 0; i < recipe.length(); i++) {
            char c = recipe.charAt(i);
            if (c == '\u0001') {
                if (dynIdx >= dyn.size()) {
                    return null;
                }
                sb.append(dyn.get(dynIdx++));
            } else if (c == '\u0002') {
                if (constIdx >= consts.size()) {
                    return null;
                }
                sb.append(consts.get(constIdx++));
            } else {
                sb.append(c);
            }
        }
        if (dynIdx != dyn.size() || constIdx != consts.size()) {
            return null;
        }
        return sb.toString();
    }

    private static String resolveStringBuilderToString(MethodInsnNode mi,
                                                       ResolveContext ctx,
                                                       int depth) {
        int idx = ctx.instructions.indexOf(mi);
        if (idx < 0 || ctx.frames[idx] == null) {
            return null;
        }
        Frame<SourceValue> frame = ctx.frames[idx];
        int stackSize = frame.getStackSize();
        if (stackSize < 1) {
            return null;
        }
        SourceValue recv = frame.getStack(stackSize - 1);
        TypeInsnNode newInsn = findSingleNewInsn(recv);
        if (newInsn == null) {
            return null;
        }
        int newIndex = ctx.instructions.indexOf(newInsn);
        if (newIndex < 0 || newIndex > idx) {
            return null;
        }
        for (int i = newIndex; i <= idx; i++) {
            AbstractInsnNode n = ctx.instructions.get(i);
            if (n instanceof JumpInsnNode
                    || n instanceof LookupSwitchInsnNode
                    || n instanceof TableSwitchInsnNode) {
                return null;
            }
        }
        StringBuilder sb = new StringBuilder();
        boolean initSeen = false;
        for (int i = newIndex; i <= idx; i++) {
            AbstractInsnNode insn = ctx.instructions.get(i);
            if (!(insn instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode m = (MethodInsnNode) insn;
            if (!isBuilderOwner(m.owner)) {
                continue;
            }
            Frame<SourceValue> f = ctx.frames[i];
            if (f == null) {
                return null;
            }
            int argCount = Type.getArgumentTypes(m.desc).length;
            int recvIndex = f.getStackSize() - argCount - 1;
            if (recvIndex < 0) {
                return null;
            }
            if (!containsSource(f.getStack(recvIndex), newInsn)) {
                continue;
            }
            if ("<init>".equals(m.name)) {
                if (argCount == 0) {
                    initSeen = true;
                    continue;
                }
                if (argCount != 1) {
                    return null;
                }
                Object v = resolveConstValue(f.getStack(recvIndex + 1),
                        ctx, depth + 1);
                if (v == null) {
                    return null;
                }
                sb.append(String.valueOf(v));
                initSeen = true;
                continue;
            }
            if ("append".equals(m.name) && argCount == 1) {
                Object v = resolveConstValue(f.getStack(recvIndex + 1),
                        ctx, depth + 1);
                if (v == null) {
                    return null;
                }
                sb.append(String.valueOf(v));
            }
        }
        if (!initSeen && sb.length() == 0) {
            return null;
        }
        return sb.toString();
    }

    private static boolean isBuilderOwner(String owner) {
        return "java/lang/StringBuilder".equals(owner)
                || "java/lang/StringBuffer".equals(owner);
    }

    private static TypeInsnNode findSingleNewInsn(SourceValue value) {
        if (value == null || value.insns == null || value.insns.isEmpty()) {
            return null;
        }
        TypeInsnNode found = null;
        for (AbstractInsnNode insn : value.insns) {
            if (!(insn instanceof TypeInsnNode)) {
                continue;
            }
            TypeInsnNode t = (TypeInsnNode) insn;
            if (t.getOpcode() != Opcodes.NEW || !isBuilderOwner(t.desc)) {
                continue;
            }
            if (found != null && found != t) {
                return null;
            }
            found = t;
        }
        return found;
    }

    private static AbstractInsnNode getSingleInsn(SourceValue value) {
        if (value == null || value.insns == null || value.insns.size() != 1) {
            return null;
        }
        return value.insns.iterator().next();
    }

    private static boolean containsSource(SourceValue value, AbstractInsnNode target) {
        return value != null && value.insns != null && value.insns.contains(target);
    }

    private static int findPreviousAstore(ResolveContext ctx, int fromIndex, int var) {
        if (ctx == null || ctx.instructions == null || fromIndex <= 0 || var < 0) {
            return -1;
        }
        int scanFloor = Math.max(0, fromIndex - 256);
        for (int i = fromIndex - 1; i >= scanFloor; i--) {
            AbstractInsnNode insn = ctx.instructions.get(i);
            if (!(insn instanceof VarInsnNode)) {
                continue;
            }
            VarInsnNode vin = (VarInsnNode) insn;
            if (vin.var != var) {
                continue;
            }
            if (vin.getOpcode() == Opcodes.ASTORE) {
                return i;
            }
        }
        return -1;
    }

    private static void recordEdgeMeta(Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                                       MethodReference.Handle caller,
                                       MethodReference.Handle callee,
                                       String type,
                                       String confidence) {
        recordEdgeMeta(methodCallMeta, caller, callee, type, confidence, null);
    }

    private static void recordEdgeMeta(Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                                       MethodReference.Handle caller,
                                       MethodReference.Handle callee,
                                       String type,
                                       String confidence,
                                       String reason) {
        if (methodCallMeta == null) {
            return;
        }
        MethodCallMeta.record(methodCallMeta, MethodCallKey.of(caller, callee),
                type, confidence, reason, callee == null ? null : callee.getOpcode());
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
        if (methodMap != null) {
            MethodReference method = methodMap.get(target);
            if (method != null && method.isStatic()) {
                return Opcodes.INVOKESTATIC;
            }
        }
        if ("<init>".equals(target.getName())) {
            return Opcodes.INVOKESPECIAL;
        }
        return fallbackOpcode;
    }

    private static String normalizeClassName(String name) {
        if (name == null) {
            return null;
        }
        String v = name.trim();
        if (v.isEmpty()) {
            return null;
        }
        if (v.startsWith("[")) {
            return null;
        }
        if (v.contains("/")) {
            return v;
        }
        return v.replace('.', '/');
    }

    private static MethodTypeInfo resolveMethodType(SourceValue value,
                                                    ResolveContext ctx) {
        AbstractInsnNode src = getSingleInsn(value);
        if (!(src instanceof MethodInsnNode)) {
            return null;
        }
        MethodInsnNode mi = (MethodInsnNode) src;
        if (!MT_OWNER.equals(mi.owner)) {
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
                Type returnType = mt.getReturnType();
                Type[] args = mt.getArgumentTypes();
                List<Type> params = new ArrayList<>();
                Collections.addAll(params, args);
                return new MethodTypeInfo(returnType, params);
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
        int argCount = argTypes.length;
        int stackSize = frame.getStackSize();
        int base = stackSize - argCount;
        if (base < 0) {
            return null;
        }
        List<Type> params = new ArrayList<>();
        Type returnType = null;
        for (int i = 0; i < argTypes.length; i++) {
            Type t = argTypes[i];
            SourceValue argVal = frame.getStack(base + i);
            if (i == 0) {
                returnType = resolveTypeConstant(argVal, ctx);
                if (returnType == null) {
                    return null;
                }
                continue;
            }
            if (t.getSort() == Type.ARRAY && "java/lang/Class".equals(t.getElementType().getInternalName())) {
                List<Type> extra = resolveClassArray(argVal, ctx);
                if (extra == null) {
                    return null;
                }
                params.addAll(extra);
                continue;
            }
            if (!"java/lang/Class".equals(t.getInternalName())) {
                return null;
            }
            Type paramType = resolveTypeConstant(argVal, ctx);
            if (paramType == null) {
                return null;
            }
            params.add(paramType);
        }
        if (returnType == null) {
            return null;
        }
        return new MethodTypeInfo(returnType, params);
    }

    private static String resolveStringFromCall(MethodInsnNode mi,
                                                ResolveContext ctx) {
        int idx = ctx.instructions.indexOf(mi);
        if (idx < 0 || ctx.frames[idx] == null) {
            return null;
        }
        Frame<SourceValue> frame = ctx.frames[idx];
        int argCount = Type.getArgumentTypes(mi.desc).length;
        int stackSize = frame.getStackSize();
        int argIndex = stackSize - argCount;
        if (argIndex < 0) {
            return null;
        }
        return resolveStringConstant(frame.getStack(argIndex), ctx, 0);
    }

    private static class MethodTypeInfo {
        private final Type returnType;
        private final List<Type> paramTypes;

        private MethodTypeInfo(Type returnType, List<Type> paramTypes) {
            this.returnType = returnType;
            this.paramTypes = paramTypes;
        }
    }
}
