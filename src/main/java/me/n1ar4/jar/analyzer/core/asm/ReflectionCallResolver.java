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
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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

    private ReflectionCallResolver() {
    }

    public static void appendReflectionEdges(
            ClassFileEntity file,
            HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
            Map<MethodReference.Handle, MethodReference> methodMap) {
        if (file == null || methodCalls == null || methodMap == null || methodMap.isEmpty()) {
            return;
        }
        try {
            ClassReader cr = new ClassReader(file.getFile());
            ClassNode cn = new ClassNode();
            cr.accept(cn, Const.AnalyzeASMOptions);
            if (cn.methods == null || cn.methods.isEmpty()) {
                return;
            }
            Map<String, String> staticStrings = collectStaticStringConstants(cn);
            appendInvokeDynamicEdges(cn, methodCalls, methodMap);
            for (MethodNode mn : cn.methods) {
                if (mn == null || mn.instructions == null || mn.instructions.size() == 0) {
                    continue;
                }
                if (!containsReflectionInvoke(mn.instructions)) {
                    continue;
                }
                resolveInMethod(cn.name, mn, cn, staticStrings, methodCalls, methodMap);
            }
        } catch (Exception ex) {
            logger.warn("reflection edge build failed: {}", ex.toString());
        }
    }

    private static void appendInvokeDynamicEdges(
            ClassNode cn,
            HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
            Map<MethodReference.Handle, MethodReference> methodMap) {
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
                callees.add(lambda.implHandle);
                if (lambda.samHandle != null && methodMap.containsKey(lambda.samHandle)) {
                    HashSet<MethodReference.Handle> samCallees =
                            methodCalls.computeIfAbsent(lambda.samHandle, k -> new HashSet<>());
                    samCallees.add(lambda.implHandle);
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

    private static boolean containsReflectionInvoke(InsnList instructions) {
        for (int i = 0, n = instructions.size(); i < n; i++) {
            AbstractInsnNode insn = instructions.get(i);
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode mi = (MethodInsnNode) insn;
                if (isReflectionInvoke(mi) || isMethodHandleInvoke(mi)) {
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
            Map<MethodReference.Handle, MethodReference> methodMap) {
        try {
            Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());
            Frame<SourceValue>[] frames = analyzer.analyze(owner, mn);
            InsnList instructions = mn.instructions;
            for (int i = 0, n = instructions.size(); i < n; i++) {
                AbstractInsnNode insn = instructions.get(i);
                if (!(insn instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode invoke = (MethodInsnNode) insn;
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
                    SourceValue objVal = frame.getStack(objIndex);
                    MethodInsnNode creator = findSingleCreator(objVal);
                    if (creator == null) {
                        continue;
                    }
                    List<MethodReference.Handle> targets =
                            resolveTarget(creator, frames, instructions, cn, staticStrings, methodMap);
                    if (targets == null || targets.isEmpty()) {
                        continue;
                    }
                    MethodReference.Handle caller = new MethodReference.Handle(
                            new ClassReference.Handle(owner), mn.name, mn.desc);
                    HashSet<MethodReference.Handle> callees =
                            methodCalls.computeIfAbsent(caller, k -> new HashSet<>());
                    callees.addAll(targets);
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
                    List<MethodReference.Handle> targets =
                            resolveHandleTarget(creator, frames, instructions, cn, staticStrings, methodMap);
                    if (targets == null || targets.isEmpty()) {
                        continue;
                    }
                    MethodReference.Handle caller = new MethodReference.Handle(
                            new ClassReference.Handle(owner), mn.name, mn.desc);
                    HashSet<MethodReference.Handle> callees =
                            methodCalls.computeIfAbsent(caller, k -> new HashSet<>());
                    callees.addAll(targets);
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

    private static List<MethodReference.Handle> resolveTarget(
            MethodInsnNode creator,
            Frame<SourceValue>[] frames,
            InsnList instructions,
            ClassNode cn,
            Map<String, String> staticStrings,
            Map<MethodReference.Handle, MethodReference> methodMap) {
        int idx = instructions.indexOf(creator);
        if (idx < 0) {
            return null;
        }
        Frame<SourceValue> frame = frames[idx];
        if (frame == null) {
            return null;
        }
        int argCount = Type.getArgumentTypes(creator.desc).length;
        int stackSize = frame.getStackSize();
        int objIndex = stackSize - argCount - 1;
        if (objIndex < 0) {
            return null;
        }
        String className = resolveClassName(frame.getStack(objIndex), frames, instructions, cn, staticStrings);
        if (className == null) {
            return null;
        }
        List<Type> params;
        String methodName;
        if (isGetMethod(creator)) {
            if (argCount != 2) {
                return null;
            }
            SourceValue nameVal = frame.getStack(stackSize - argCount);
            String name = resolveStringConstant(nameVal, frames, instructions, cn, staticStrings, 0);
            if (name == null || name.isEmpty()) {
                return null;
            }
            SourceValue paramVal = frame.getStack(stackSize - argCount + 1);    
            params = resolveClassArray(paramVal, frames, instructions, cn, staticStrings);
            methodName = name;
        } else if (isGetConstructor(creator)) {
            SourceValue paramVal = frame.getStack(stackSize - argCount);        
            params = resolveClassArray(paramVal, frames, instructions, cn, staticStrings);
            methodName = "<init>";
        } else {
            return null;
        }
        if (params == null) {
            return findUniqueMethodByName(methodMap, className, methodName);
        }
        return findMethods(methodMap, className, methodName, params);
    }

    private static List<MethodReference.Handle> resolveHandleTarget(
            MethodInsnNode creator,
            Frame<SourceValue>[] frames,
            InsnList instructions,
            ClassNode cn,
            Map<String, String> staticStrings,
            Map<MethodReference.Handle, MethodReference> methodMap) {
        int idx = instructions.indexOf(creator);
        if (idx < 0) {
            return null;
        }
        Frame<SourceValue> frame = frames[idx];
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
        String className;
        String methodName = null;
        MethodTypeInfo mt;
        if ("findConstructor".equals(lookupName)) {
            SourceValue classVal = frame.getStack(base);
            SourceValue mtVal = frame.getStack(base + 1);
            className = resolveClassName(classVal, frames, instructions, cn, staticStrings);
            mt = resolveMethodType(mtVal, frames, instructions, cn, staticStrings);
            if (className == null || mt == null) {
                return null;
            }
            if (mt.returnType.getSort() != Type.VOID) {
                return null;
            }
            methodName = "<init>";
        } else {
            if (argCount != 3) {
                return null;
            }
            SourceValue classVal = frame.getStack(base);
            SourceValue nameVal = frame.getStack(base + 1);
            SourceValue mtVal = frame.getStack(base + 2);
            className = resolveClassName(classVal, frames, instructions, cn, staticStrings);
            String name = resolveStringConstant(nameVal, frames, instructions, cn, staticStrings, 0);
            mt = resolveMethodType(mtVal, frames, instructions, cn, staticStrings);
            if (className == null || name == null || mt == null) {
                return null;
            }
            methodName = name;
        }
        String desc = Type.getMethodDescriptor(mt.returnType, mt.paramTypes.toArray(new Type[0]));
        List<MethodReference.Handle> out = new ArrayList<>();
        for (MethodReference.Handle handle : methodMap.keySet()) {
            if (!className.equals(handle.getClassReference().getName())) {
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
        return out.isEmpty() ? null : out;
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
        if (!"invoke".equals(mi.name) && !"invokeExact".equals(mi.name)) {
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
            Frame<SourceValue>[] frames,
            InsnList instructions,
            ClassNode cn,
            Map<String, String> staticStrings) {
        AbstractInsnNode src = getSingleInsn(value);
        if (src instanceof LdcInsnNode) {
            Object cst = ((LdcInsnNode) src).cst;
            if (cst instanceof Type) {
                return ((Type) cst).getInternalName();
            }
        } else if (src instanceof MethodInsnNode) {
            MethodInsnNode mi = (MethodInsnNode) src;
            if (isForName(mi) || isLoadClass(mi)) {
                String name = resolveClassNameFromCall(mi, frames, instructions, cn, staticStrings);
                return normalizeClassName(name);
            }
        } else if (src instanceof InvokeDynamicInsnNode || src instanceof MethodInsnNode
                || src instanceof FieldInsnNode) {
            String name = resolveStringConstant(value, frames, instructions, cn, staticStrings, 0);
            return normalizeClassName(name);
        }
        return null;
    }

    private static boolean isForName(MethodInsnNode mi) {
        return CLASS_OWNER.equals(mi.owner)
                && "forName".equals(mi.name)
                && "(Ljava/lang/String;)Ljava/lang/Class;".equals(mi.desc);
    }

    private static boolean isLoadClass(MethodInsnNode mi) {
        return "java/lang/ClassLoader".equals(mi.owner)
                && "loadClass".equals(mi.name)
                && "(Ljava/lang/String;)Ljava/lang/Class;".equals(mi.desc);
    }

    private static String resolveClassNameFromCall(
            MethodInsnNode mi,
            Frame<SourceValue>[] frames,
            InsnList instructions,
            ClassNode cn,
            Map<String, String> staticStrings) {
        int idx = instructions.indexOf(mi);
        if (idx < 0) {
            return null;
        }
        Frame<SourceValue> frame = frames[idx];
        if (frame == null) {
            return null;
        }
        int argCount = Type.getArgumentTypes(mi.desc).length;
        int stackSize = frame.getStackSize();
        int argIndex = stackSize - argCount;
        if (argIndex < 0) {
            return null;
        }
        return resolveStringConstant(frame.getStack(argIndex), frames, instructions, cn, staticStrings, 0);
    }

    private static String resolveStringConstant(SourceValue value,
                                                Frame<SourceValue>[] frames,
                                                InsnList instructions,
                                                ClassNode cn,
                                                Map<String, String> staticStrings,
                                                int depth) {
        Object resolved = resolveConstValue(value, frames, instructions, cn, staticStrings, depth);
        if (resolved instanceof String) {
            String s = ((String) resolved).trim();
            return s.isEmpty() ? null : s;
        }
        return null;
    }

    private static List<Type> resolveClassArray(
            SourceValue value,
            Frame<SourceValue>[] frames,
            InsnList instructions,
            ClassNode cn,
            Map<String, String> staticStrings) {
        AbstractInsnNode src = getSingleInsn(value);
        if (!(src instanceof TypeInsnNode)) {
            return null;
        }
        TypeInsnNode tin = (TypeInsnNode) src;
        if (tin.getOpcode() != Opcodes.ANEWARRAY || !"java/lang/Class".equals(tin.desc)) {
            return null;
        }
        int srcIndex = instructions.indexOf(src);
        if (srcIndex < 0 || frames[srcIndex] == null) {
            return null;
        }
        Frame<SourceValue> srcFrame = frames[srcIndex];
        Integer expectedSize = resolveIntConstant(srcFrame.getStack(srcFrame.getStackSize() - 1));
        if (expectedSize == null || expectedSize < 0) {
            return null;
        }
        Map<Integer, Type> elements = new HashMap<>();
        for (int i = 0, n = instructions.size(); i < n; i++) {
            AbstractInsnNode insn = instructions.get(i);
            if (!(insn instanceof InsnNode) || insn.getOpcode() != Opcodes.AASTORE) {
                continue;
            }
            Frame<SourceValue> frame = frames[i];
            if (frame == null || frame.getStackSize() < 3) {
                continue;
            }
            int stackSize = frame.getStackSize();
            SourceValue arrVal = frame.getStack(stackSize - 3);
            if (!containsSource(arrVal, src)) {
                continue;
            }
            Integer index = resolveIntConstant(frame.getStack(stackSize - 2));
            Type type = resolveTypeConstant(frame.getStack(stackSize - 1), frames, instructions, cn, staticStrings);
            if (index == null || type == null) {
                return null;
            }
            elements.put(index, type);
        }
        if (expectedSize == 0) {
            return elements.isEmpty() ? Collections.emptyList() : null;
        }
        if (elements.size() != expectedSize) {
            return null;
        }
        List<Type> out = new ArrayList<>(expectedSize);
        for (int i = 0; i < expectedSize; i++) {
            Type t = elements.get(i);
            if (t == null) {
                return null;
            }
            out.add(t);
        }
        return out;
    }

    private static Type resolveTypeConstant(
            SourceValue value,
            Frame<SourceValue>[] frames,
            InsnList instructions,
            ClassNode cn,
            Map<String, String> staticStrings) {
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
                String name = resolveClassNameFromCall(mi, frames, instructions, cn, staticStrings);
                String cls = normalizeClassName(name);
                if (cls != null) {
                    return Type.getObjectType(cls);
                }
            }
        } else if (src instanceof InvokeDynamicInsnNode || src instanceof FieldInsnNode) {
            String name = resolveStringConstant(value, frames, instructions, cn, staticStrings, 0);
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
                                            Frame<SourceValue>[] frames,
                                            InsnList instructions,
                                            ClassNode cn,
                                            Map<String, String> staticStrings,
                                            int depth) {
        if (depth > MAX_CONST_DEPTH) {
            return null;
        }
        AbstractInsnNode src = getSingleInsn(value);
        if (src == null) {
            return null;
        }
        if (src instanceof LdcInsnNode) {
            Object cst = ((LdcInsnNode) src).cst;
            if (cst == null) {
                return null;
            }
            return cst;
        }
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
                case Opcodes.LCONST_0:
                    return 0L;
                case Opcodes.LCONST_1:
                    return 1L;
                case Opcodes.FCONST_0:
                    return 0.0f;
                case Opcodes.FCONST_1:
                    return 1.0f;
                case Opcodes.FCONST_2:
                    return 2.0f;
                case Opcodes.DCONST_0:
                    return 0.0d;
                case Opcodes.DCONST_1:
                    return 1.0d;
                default:
                    return null;
            }
        }
        if (src instanceof IntInsnNode) {
            return ((IntInsnNode) src).operand;
        }
        if (src instanceof FieldInsnNode) {
            FieldInsnNode fin = (FieldInsnNode) src;
            if (fin.getOpcode() == Opcodes.GETSTATIC) {
                String key = fin.owner + "#" + fin.name;
                if (staticStrings != null && staticStrings.containsKey(key)) {
                    return staticStrings.get(key);
                }
            }
        }
        if (src instanceof InvokeDynamicInsnNode) {
            return resolveInvokeDynamicString((InvokeDynamicInsnNode) src, frames, instructions,
                    cn, staticStrings, depth + 1);
        }
        if (src instanceof MethodInsnNode) {
            MethodInsnNode mi = (MethodInsnNode) src;
            if (isStringBuilderToString(mi)) {
                return resolveStringBuilderToString(mi, frames, instructions, cn, staticStrings, depth + 1);
            }
            if ("java/lang/String".equals(mi.owner)
                    && "valueOf".equals(mi.name)
                    && mi.desc.startsWith("(")
                    && mi.desc.endsWith(")Ljava/lang/String;")) {
                String v = resolveStringValueOf(mi, frames, instructions, cn, staticStrings, depth + 1);
                if (v != null) {
                    return v;
                }
            }
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
                                               Frame<SourceValue>[] frames,
                                               InsnList instructions,
                                               ClassNode cn,
                                               Map<String, String> staticStrings,
                                               int depth) {
        int idx = instructions.indexOf(mi);
        if (idx < 0 || frames[idx] == null) {
            return null;
        }
        Frame<SourceValue> frame = frames[idx];
        int argCount = Type.getArgumentTypes(mi.desc).length;
        int stackSize = frame.getStackSize();
        int argIndex = stackSize - argCount;
        if (argIndex < 0) {
            return null;
        }
        Object v = resolveConstValue(frame.getStack(argIndex), frames, instructions,
                cn, staticStrings, depth + 1);
        if (v == null) {
            return null;
        }
        return String.valueOf(v);
    }

    private static String resolveInvokeDynamicString(InvokeDynamicInsnNode indy,
                                                     Frame<SourceValue>[] frames,
                                                     InsnList instructions,
                                                     ClassNode cn,
                                                     Map<String, String> staticStrings,
                                                     int depth) {
        if (indy == null || indy.bsm == null || !SCF_OWNER.equals(indy.bsm.getOwner())) {
            return null;
        }
        int idx = instructions.indexOf(indy);
        if (idx < 0 || frames[idx] == null) {
            return null;
        }
        Frame<SourceValue> frame = frames[idx];
        Type[] args = Type.getArgumentTypes(indy.desc);
        List<String> dyn = new ArrayList<>();
        int stackSize = frame.getStackSize();
        int base = stackSize - args.length;
        if (base < 0) {
            return null;
        }
        for (int i = 0; i < args.length; i++) {
            Object v = resolveConstValue(frame.getStack(base + i),
                    frames, instructions, cn, staticStrings, depth + 1);
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
                                                       Frame<SourceValue>[] frames,
                                                       InsnList instructions,
                                                       ClassNode cn,
                                                       Map<String, String> staticStrings,
                                                       int depth) {
        int idx = instructions.indexOf(mi);
        if (idx < 0 || frames[idx] == null) {
            return null;
        }
        Frame<SourceValue> frame = frames[idx];
        int stackSize = frame.getStackSize();
        if (stackSize < 1) {
            return null;
        }
        SourceValue recv = frame.getStack(stackSize - 1);
        TypeInsnNode newInsn = findSingleNewInsn(recv);
        if (newInsn == null) {
            return null;
        }
        int newIndex = instructions.indexOf(newInsn);
        if (newIndex < 0 || newIndex > idx) {
            return null;
        }
        for (int i = newIndex; i <= idx; i++) {
            AbstractInsnNode n = instructions.get(i);
            if (n instanceof JumpInsnNode
                    || n instanceof LookupSwitchInsnNode
                    || n instanceof TableSwitchInsnNode) {
                return null;
            }
        }
        StringBuilder sb = new StringBuilder();
        boolean initSeen = false;
        for (int i = newIndex; i <= idx; i++) {
            AbstractInsnNode insn = instructions.get(i);
            if (!(insn instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode m = (MethodInsnNode) insn;
            if (!isBuilderOwner(m.owner)) {
                continue;
            }
            Frame<SourceValue> f = frames[i];
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
                        frames, instructions, cn, staticStrings, depth + 1);
                if (v == null) {
                    return null;
                }
                sb.append(String.valueOf(v));
                initSeen = true;
                continue;
            }
            if ("append".equals(m.name) && argCount == 1) {
                Object v = resolveConstValue(f.getStack(recvIndex + 1),
                        frames, instructions, cn, staticStrings, depth + 1);
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
                                                    Frame<SourceValue>[] frames,
                                                    InsnList instructions,
                                                    ClassNode cn,
                                                    Map<String, String> staticStrings) {
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
            String desc = resolveStringFromCall(mi, frames, instructions, cn, staticStrings);
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
            } catch (Exception ignored) {
                return null;
            }
        }
        if (!"methodType".equals(mi.name)) {
            return null;
        }
        int idx = instructions.indexOf(mi);
        if (idx < 0 || frames[idx] == null) {
            return null;
        }
        Frame<SourceValue> frame = frames[idx];
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
                returnType = resolveTypeConstant(argVal, frames, instructions, cn, staticStrings);
                if (returnType == null) {
                    return null;
                }
                continue;
            }
            if (t.getSort() == Type.ARRAY && "java/lang/Class".equals(t.getElementType().getInternalName())) {
                List<Type> extra = resolveClassArray(argVal, frames, instructions, cn, staticStrings);
                if (extra == null) {
                    return null;
                }
                params.addAll(extra);
                continue;
            }
            if (!"java/lang/Class".equals(t.getInternalName())) {
                return null;
            }
            Type paramType = resolveTypeConstant(argVal, frames, instructions, cn, staticStrings);
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
                                                Frame<SourceValue>[] frames,
                                                InsnList instructions,
                                                ClassNode cn,
                                                Map<String, String> staticStrings) {
        int idx = instructions.indexOf(mi);
        if (idx < 0 || frames[idx] == null) {
            return null;
        }
        Frame<SourceValue> frame = frames[idx];
        int argCount = Type.getArgumentTypes(mi.desc).length;
        int stackSize = frame.getStackSize();
        int argIndex = stackSize - argCount;
        if (argIndex < 0) {
            return null;
        }
        return resolveStringConstant(frame.getStack(argIndex), frames, instructions, cn, staticStrings, 0);
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
