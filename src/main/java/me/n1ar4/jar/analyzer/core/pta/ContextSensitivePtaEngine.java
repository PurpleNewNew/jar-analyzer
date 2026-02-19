/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.pta;

import me.n1ar4.jar.analyzer.analyze.spring.SpringController;
import me.n1ar4.jar.analyzer.analyze.spring.SpringMapping;
import me.n1ar4.jar.analyzer.core.CallSiteKeyUtil;
import me.n1ar4.jar.analyzer.core.InheritanceMap;
import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.MethodCallUtils;
import me.n1ar4.jar.analyzer.core.asm.ReflectionCallResolver;
import me.n1ar4.jar.analyzer.core.build.BuildContext;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

public final class ContextSensitivePtaEngine {
    private static final Logger logger = LogManager.getLogger();
    private static final int MAX_FALLBACK_TYPES = 512;

    private ContextSensitivePtaEngine() {
    }

    public static Result run(BuildContext ctx, List<CallSiteEntity> callSites) {
        if (ctx == null || ctx.methodMap == null || ctx.methodMap.isEmpty()) {
            return Result.empty();
        }
        long start = System.nanoTime();
        String incrementalScope = buildIncrementalScope(ctx);
        PointerAssignmentGraph graph = buildGraph(ctx, callSites);
        if (graph.getCallers().isEmpty()) {
            return Result.empty();
        }
        PtaSolverConfig config = PtaSolverConfig.fromSystemProperties();
        Solver solver = new Solver(ctx, graph, config, incrementalScope);
        Result result = solver.solve();
        result.durationMs = (System.nanoTime() - start) / 1_000_000L;
        return result;
    }

    public static void clearIncrementalCache() {
        IncrementalPtaState.resetAll();
    }

    private static PointerAssignmentGraph buildGraph(BuildContext ctx, List<CallSiteEntity> callSites) {
        PointerAssignmentGraph graph = new PointerAssignmentGraph();
        PtaConstraintIndex constraintIndex = buildConstraintIndex(ctx);
        appendInvokeSites(graph, callSites, ctx.methodMap, constraintIndex);
        constraintIndex.applyToGraph(graph, ctx.methodMap);
        appendReflectionSyntheticCalls(graph, ctx);
        return graph;
    }

    private static void appendInvokeSites(PointerAssignmentGraph graph,
                                          List<CallSiteEntity> callSites,
                                          Map<MethodReference.Handle, MethodReference> methodMap,
                                          PtaConstraintIndex constraintIndex) {
        if (graph == null || callSites == null || callSites.isEmpty()) {
            return;
        }
        PtaContext root = PtaContext.root();
        for (CallSiteEntity site : callSites) {
            if (site == null) {
                continue;
            }
            int opcode = normalizeOpcode(site.getOpCode());
            if (opcode != Opcodes.INVOKEVIRTUAL && opcode != Opcodes.INVOKEINTERFACE) {
                continue;
            }
            MethodReference.Handle caller = toCallerHandle(site);
            caller = canonicalHandle(caller, methodMap);
            if (caller == null) {
                continue;
            }
            String owner = normalizeClassName(site.getCalleeOwner());
            String name = site.getCalleeMethodName();
            String desc = site.getCalleeMethodDesc();
            if (owner == null || name == null || desc == null) {
                continue;
            }
            String receiverType = normalizeClassName(site.getReceiverType());
            String callSiteKey = site.getCallSiteKey();
            if (callSiteKey == null || callSiteKey.trim().isEmpty()) {
                callSiteKey = CallSiteKeyUtil.buildCallSiteKey(site);
            }
            String receiverVarId = constraintIndex.receiverVarId(callSiteKey);
            PtaInvokeSite invokeSite = new PtaInvokeSite(
                    callSiteKey,
                    owner,
                    name,
                    desc,
                    opcode,
                    receiverVarId,
                    receiverType,
                    normalizeIndex(site.getCallIndex()),
                    normalizeIndex(site.getLineNumber())
            );
            graph.addInvokeSite(caller, invokeSite);
            if (receiverType != null) {
                PtaContextMethod contextMethod = new PtaContextMethod(caller, root);
                PtaVarNode recvVar = new PtaVarNode(contextMethod, invokeSite.receiverVarId());
                PtaAllocNode alloc = new PtaAllocNode(new ClassReference.Handle(receiverType),
                        "typed@" + invokeSite.siteToken());
                graph.addAllocEdge(alloc, recvVar);
            }
        }
    }

    private static PtaConstraintIndex buildConstraintIndex(BuildContext ctx) {
        PtaConstraintIndex out = new PtaConstraintIndex();
        if (ctx == null || ctx.classFileList == null || ctx.classFileList.isEmpty()
                || ctx.methodMap == null || ctx.methodMap.isEmpty()) {
            return out;
        }
        for (ClassFileEntity file : ctx.classFileList) {
            if (file == null || file.getFile() == null || file.getFile().length == 0) {
                continue;
            }
            try {
                ClassReader cr = new ClassReader(file.getFile());
                ClassNode cn = new ClassNode();
                cr.accept(cn, Const.GlobalASMOptions);
                extractConstraintsFromClass(file, cn, ctx.methodMap, out);
            } catch (Exception ex) {
                logger.debug("pta constraint collect failed: {}", ex.toString());
            }
        }
        return out;
    }

    private static void extractConstraintsFromClass(ClassFileEntity file,
                                                    ClassNode cn,
                                                    Map<MethodReference.Handle, MethodReference> methodMap,
                                                    PtaConstraintIndex out) {
        if (cn == null || cn.methods == null || cn.methods.isEmpty() || out == null) {
            return;
        }
        for (MethodNode mn : cn.methods) {
            if (mn == null || mn.instructions == null || mn.instructions.size() == 0) {
                continue;
            }
            MethodReference.Handle raw = new MethodReference.Handle(
                    new ClassReference.Handle(cn.name, normalizeJarId(file == null ? null : file.getJarId())),
                    mn.name,
                    mn.desc
            );
            MethodReference.Handle caller = canonicalHandle(raw, methodMap);
            if (caller == null) {
                continue;
            }
            seedMethodLocals(caller, mn, cn, out);

            Frame<SourceValue>[] frames;
            try {
                Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());
                frames = analyzer.analyze(cn.name, mn);
            } catch (Exception ex) {
                logger.debug("pta constraint frame analyze failed: {}", ex.toString());
                continue;
            }
            seedExceptionHandlers(caller, mn, out);

            int callIndex = 0;
            InsnList instructions = mn.instructions;
            int currentLine = -1;
            for (int i = 0; i < instructions.size(); i++) {
                AbstractInsnNode insn = instructions.get(i);
                if (insn instanceof LineNumberNode) {
                    currentLine = ((LineNumberNode) insn).line;
                    continue;
                }
                Frame<SourceValue> frame = i < frames.length ? frames[i] : null;
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode invoke = (MethodInsnNode) insn;
                    if (invoke.getOpcode() == Opcodes.INVOKEVIRTUAL
                            || invoke.getOpcode() == Opcodes.INVOKEINTERFACE) {
                        String recvVarId = resolveReceiverVarId(invoke, frame);
                        if (recvVarId != null) {
                            String callSiteKey = buildCallSiteKey(
                                    file == null ? null : file.getJarId(),
                                    cn.name,
                                    mn.name,
                                    mn.desc,
                                    invoke.owner,
                                    invoke.name,
                                    invoke.desc,
                                    invoke.getOpcode(),
                                    callIndex
                            );
                            out.bindReceiverVar(callSiteKey, recvVarId);
                        }
                    }
                    collectNativeModelConstraints(caller, invoke, frame, out);
                    callIndex++;
                    continue;
                }
                if (insn instanceof FieldInsnNode) {
                    collectFieldStoreConstraints(caller, (FieldInsnNode) insn, frame, out);
                    continue;
                }
                if (insn instanceof InsnNode) {
                    int opcode = insn.getOpcode();
                    if (opcode == Opcodes.AASTORE) {
                        collectArrayStoreConstraints(caller, frame, out);
                        continue;
                    }
                    if (opcode == Opcodes.ATHROW) {
                        collectThrowConstraints(caller, frame, out, i, currentLine);
                        continue;
                    }
                }
                if (!(insn instanceof VarInsnNode)) {
                    continue;
                }
                VarInsnNode store = (VarInsnNode) insn;
                if (store.getOpcode() != Opcodes.ASTORE) {
                    continue;
                }
                collectAssignAndAlloc(caller, store, frame, out,
                        instructions, frames, i, currentLine);
            }
        }
    }

    private static void seedMethodLocals(MethodReference.Handle caller,
                                         MethodNode mn,
                                         ClassNode cn,
                                         PtaConstraintIndex out) {
        if (caller == null || mn == null || cn == null || out == null) {
            return;
        }
        int localIdx = 0;
        if ((mn.access & Opcodes.ACC_STATIC) == 0) {
            out.addAlloc(caller,
                    localVarId(localIdx),
                    new ClassReference.Handle(cn.name),
                    "this@" + methodKey(caller));
            localIdx++;
        }
        Type[] args = Type.getArgumentTypes(mn.desc);
        for (Type arg : args) {
            if (arg != null && (arg.getSort() == Type.OBJECT || arg.getSort() == Type.ARRAY)) {
                ClassReference.Handle argType = normalizeRefType(arg);
                if (argType == null) {
                    localIdx += arg.getSize();
                    continue;
                }
                out.addAlloc(caller,
                        localVarId(localIdx),
                        argType,
                        "arg@" + methodKey(caller) + "#" + localIdx);
            }
            localIdx += arg == null ? 1 : arg.getSize();
        }
    }

    private static void collectAssignAndAlloc(MethodReference.Handle caller,
                                              VarInsnNode store,
                                              Frame<SourceValue> frame,
                                              PtaConstraintIndex out,
                                              InsnList instructions,
                                              Frame<SourceValue>[] frames,
                                              int insnIndex,
                                              int lineNumber) {
        if (caller == null || store == null || out == null || frame == null || frame.getStackSize() <= 0) {
            return;
        }
        String toVar = localVarId(store.var);
        SourceValue value = frame.getStack(frame.getStackSize() - 1);
        if (value == null || value.insns == null || value.insns.isEmpty()) {
            return;
        }
        String allocTag = "insn@" + methodKey(caller) + "#" + insnIndex + "@" + lineNumber;
        for (AbstractInsnNode src : value.insns) {
            if (src instanceof VarInsnNode) {
                VarInsnNode vin = (VarInsnNode) src;
                if (vin.getOpcode() == Opcodes.ALOAD) {
                    out.addAssign(caller, localVarId(vin.var), toVar);
                }
                continue;
            }
            if (src instanceof TypeInsnNode) {
                TypeInsnNode tn = (TypeInsnNode) src;
                if (tn.getOpcode() == Opcodes.NEW) {
                    String type = normalizeClassName(tn.desc);
                    if (type != null) {
                        out.addAlloc(caller, toVar, new ClassReference.Handle(type), "new@" + allocTag);
                    }
                }
                continue;
            }
            if (src instanceof MethodInsnNode) {
                MethodInsnNode mi = (MethodInsnNode) src;
                ClassReference.Handle retType = normalizeRefType(Type.getReturnType(mi.desc));
                if (retType != null) {
                    out.addAlloc(caller, toVar, retType, "ret@" + allocTag);
                }
                continue;
            }
            if (src instanceof FieldInsnNode) {
                FieldInsnNode fi = (FieldInsnNode) src;
                if (fi.getOpcode() == Opcodes.GETFIELD || fi.getOpcode() == Opcodes.GETSTATIC) {
                    if (fi.getOpcode() == Opcodes.GETSTATIC) {
                        out.addStaticFieldLoad(caller, fieldSig(fi), toVar);
                    } else {
                        String baseVar = resolveFieldBaseVarId(fi, instructions, frames);
                        if (baseVar != null) {
                            out.addFieldLoad(caller, baseVar, fieldSig(fi), toVar);
                        }
                    }
                    ClassReference.Handle fieldType = normalizeRefType(Type.getType(fi.desc));
                    if (fieldType != null) {
                        out.addAlloc(caller, toVar, fieldType, "field@" + allocTag);
                    }
                }
                continue;
            }
            if (src instanceof InsnNode) {
                int opcode = src.getOpcode();
                if (opcode == Opcodes.AALOAD) {
                    int srcIndex = instructions == null ? -1 : instructions.indexOf(src);
                    Frame<SourceValue> srcFrame = (frames != null && srcIndex >= 0 && srcIndex < frames.length)
                            ? frames[srcIndex] : null;
                    if (srcFrame != null && srcFrame.getStackSize() >= 2) {
                        SourceValue arrVal = srcFrame.getStack(srcFrame.getStackSize() - 2);
                        SourceValue idxVal = srcFrame.getStack(srcFrame.getStackSize() - 1);
                        String arrayVarId = resolvePrimaryLocalVarId(arrVal);
                        if (arrayVarId != null) {
                            out.addArrayLoad(caller, arrayVarId, resolveIntConstant(idxVal), toVar);
                        }
                    }
                }
            }
        }
    }

    private static void collectFieldStoreConstraints(MethodReference.Handle caller,
                                                     FieldInsnNode fi,
                                                     Frame<SourceValue> frame,
                                                     PtaConstraintIndex out) {
        if (caller == null || fi == null || frame == null || out == null) {
            return;
        }
        int opcode = fi.getOpcode();
        String fieldSig = fieldSig(fi);
        if (fieldSig == null || fieldSig.isEmpty()) {
            return;
        }
        if (opcode == Opcodes.PUTSTATIC) {
            if (frame.getStackSize() < 1) {
                return;
            }
            String valueVar = resolvePrimaryLocalVarId(frame.getStack(frame.getStackSize() - 1));
            if (valueVar != null) {
                out.addStaticFieldStore(caller, fieldSig, valueVar);
            }
            return;
        }
        if (opcode != Opcodes.PUTFIELD || frame.getStackSize() < 2) {
            return;
        }
        String baseVar = resolvePrimaryLocalVarId(frame.getStack(frame.getStackSize() - 2));
        String valueVar = resolvePrimaryLocalVarId(frame.getStack(frame.getStackSize() - 1));
        if (baseVar != null && valueVar != null) {
            out.addFieldStore(caller, baseVar, fieldSig, valueVar);
        }
    }

    private static void collectArrayStoreConstraints(MethodReference.Handle caller,
                                                     Frame<SourceValue> frame,
                                                     PtaConstraintIndex out) {
        if (caller == null || frame == null || out == null || frame.getStackSize() < 3) {
            return;
        }
        String arrayVar = resolvePrimaryLocalVarId(frame.getStack(frame.getStackSize() - 3));
        String valueVar = resolvePrimaryLocalVarId(frame.getStack(frame.getStackSize() - 1));
        Integer index = resolveIntConstant(frame.getStack(frame.getStackSize() - 2));
        if (arrayVar != null && valueVar != null) {
            out.addArrayStore(caller, arrayVar, index, valueVar);
        }
    }

    private static void collectThrowConstraints(MethodReference.Handle caller,
                                                Frame<SourceValue> frame,
                                                PtaConstraintIndex out,
                                                int insnIndex,
                                                int lineNumber) {
        if (caller == null || frame == null || out == null || frame.getStackSize() < 1) {
            return;
        }
        String thrownVar = resolvePrimaryLocalVarId(frame.getStack(frame.getStackSize() - 1));
        if (thrownVar == null) {
            return;
        }
        String sinkVar = "thrown@" + methodKey(caller) + "#" + insnIndex;
        out.addAssign(caller, thrownVar, sinkVar);
        out.addAlloc(caller, sinkVar, new ClassReference.Handle("java/lang/Throwable"),
                "throw@" + methodKey(caller) + "#" + insnIndex + "@" + lineNumber);
    }

    private static void collectNativeModelConstraints(MethodReference.Handle caller,
                                                      MethodInsnNode invoke,
                                                      Frame<SourceValue> frame,
                                                      PtaConstraintIndex out) {
        if (caller == null || invoke == null || frame == null || out == null) {
            return;
        }
        if ("java/lang/System".equals(invoke.owner)
                && "arraycopy".equals(invoke.name)
                && "(Ljava/lang/Object;ILjava/lang/Object;II)V".equals(invoke.desc)
                && frame.getStackSize() >= 5) {
            int base = frame.getStackSize() - 5;
            String srcArrayVar = resolvePrimaryLocalVarId(frame.getStack(base));
            String dstArrayVar = resolvePrimaryLocalVarId(frame.getStack(base + 2));
            if (srcArrayVar != null && dstArrayVar != null) {
                out.addArrayCopy(caller, srcArrayVar, dstArrayVar);
            }
        }
    }

    private static void seedExceptionHandlers(MethodReference.Handle caller,
                                              MethodNode mn,
                                              PtaConstraintIndex out) {
        if (caller == null || mn == null || mn.instructions == null || out == null
                || mn.tryCatchBlocks == null || mn.tryCatchBlocks.isEmpty()) {
            return;
        }
        InsnList instructions = mn.instructions;
        for (TryCatchBlockNode tcb : mn.tryCatchBlocks) {
            if (tcb == null || tcb.handler == null) {
                continue;
            }
            int handlerIndex = instructions.indexOf(tcb.handler);
            if (handlerIndex < 0) {
                continue;
            }
            String catchVar = null;
            int scanEnd = Math.min(instructions.size(), handlerIndex + 8);
            for (int i = handlerIndex; i < scanEnd; i++) {
                AbstractInsnNode insn = instructions.get(i);
                if (insn instanceof LabelNode || insn instanceof LineNumberNode) {
                    continue;
                }
                if (!(insn instanceof VarInsnNode)) {
                    break;
                }
                VarInsnNode vin = (VarInsnNode) insn;
                if (vin.getOpcode() != Opcodes.ASTORE) {
                    break;
                }
                catchVar = localVarId(vin.var);
                break;
            }
            if (catchVar == null) {
                continue;
            }
            String typeName = tcb.type == null ? "java/lang/Throwable" : normalizeClassName(tcb.type);
            if (typeName == null) {
                typeName = "java/lang/Throwable";
            }
            out.addAlloc(caller, catchVar, new ClassReference.Handle(typeName),
                    "catch@" + methodKey(caller) + "#" + handlerIndex);
        }
    }

    private static String fieldSig(FieldInsnNode fi) {
        if (fi == null || fi.owner == null || fi.name == null || fi.desc == null) {
            return "";
        }
        return fi.owner + "#" + fi.name + "#" + fi.desc;
    }

    private static String resolveFieldBaseVarId(FieldInsnNode fi,
                                                InsnList instructions,
                                                Frame<SourceValue>[] frames) {
        if (fi == null || instructions == null || frames == null || fi.getOpcode() != Opcodes.GETFIELD) {
            return null;
        }
        int idx = instructions.indexOf(fi);
        if (idx < 0 || idx >= frames.length) {
            return null;
        }
        Frame<SourceValue> frame = frames[idx];
        if (frame == null || frame.getStackSize() < 1) {
            return null;
        }
        return resolvePrimaryLocalVarId(frame.getStack(frame.getStackSize() - 1));
    }

    private static String resolvePrimaryLocalVarId(SourceValue value) {
        if (value == null || value.insns == null || value.insns.isEmpty()) {
            return null;
        }
        int best = Integer.MAX_VALUE;
        for (AbstractInsnNode src : value.insns) {
            if (!(src instanceof VarInsnNode)) {
                continue;
            }
            VarInsnNode vin = (VarInsnNode) src;
            if (vin.getOpcode() == Opcodes.ALOAD && vin.var >= 0) {
                best = Math.min(best, vin.var);
            }
        }
        if (best == Integer.MAX_VALUE) {
            return null;
        }
        return localVarId(best);
    }

    private static Integer resolveIntConstant(SourceValue value) {
        if (value == null || value.insns == null || value.insns.size() != 1) {
            return null;
        }
        AbstractInsnNode src = value.insns.iterator().next();
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
                    break;
            }
        }
        if (src instanceof IntInsnNode) {
            return ((IntInsnNode) src).operand;
        }
        if (src instanceof LdcInsnNode) {
            Object cst = ((LdcInsnNode) src).cst;
            if (cst instanceof Integer) {
                return (Integer) cst;
            }
        }
        return null;
    }

    private static String resolveReceiverVarId(MethodInsnNode invoke, Frame<SourceValue> frame) {
        if (invoke == null || frame == null) {
            return null;
        }
        int argSlots = 0;
        for (Type type : Type.getArgumentTypes(invoke.desc)) {
            if (type != null) {
                argSlots += type.getSize();
            }
        }
        int recvIndex = frame.getStackSize() - argSlots - 1;
        if (recvIndex < 0) {
            return null;
        }
        return resolvePrimaryLocalVarId(frame.getStack(recvIndex));
    }

    private static String buildCallSiteKey(Integer jarId,
                                           String callerClass,
                                           String callerMethod,
                                           String callerDesc,
                                           String calleeOwner,
                                           String calleeName,
                                           String calleeDesc,
                                           int opcode,
                                           int callIndex) {
        CallSiteEntity site = new CallSiteEntity();
        site.setJarId(jarId == null ? -1 : jarId);
        site.setCallerClassName(callerClass);
        site.setCallerMethodName(callerMethod);
        site.setCallerMethodDesc(callerDesc);
        site.setCalleeOwner(calleeOwner);
        site.setCalleeMethodName(calleeName);
        site.setCalleeMethodDesc(calleeDesc);
        site.setOpCode(opcode);
        site.setCallIndex(callIndex);
        return CallSiteKeyUtil.buildCallSiteKey(site);
    }

    private static String buildIncrementalScope(BuildContext ctx) {
        if (ctx == null || ctx.classFileList == null || ctx.classFileList.isEmpty()) {
            return "empty";
        }
        ArrayList<String> classes = new ArrayList<>(ctx.classFileList.size());
        for (ClassFileEntity file : ctx.classFileList) {
            if (file == null || file.getClassName() == null) {
                continue;
            }
            String name = file.getClassName().trim();
            if (name.isEmpty()) {
                continue;
            }
            if (name.endsWith(".class")) {
                name = name.substring(0, name.length() - 6);
            }
            classes.add(name);
        }
        if (classes.isEmpty()) {
            return "empty";
        }
        classes.sort(String::compareTo);
        int hash = 1;
        int limit = Math.min(classes.size(), 4096);
        for (int i = 0; i < limit; i++) {
            hash = 31 * hash + classes.get(i).hashCode();
        }
        return "c" + classes.size() + ":" + Integer.toUnsignedString(hash, 16);
    }

    private static void appendReflectionSyntheticCalls(PointerAssignmentGraph graph, BuildContext ctx) {
        if (graph == null || ctx == null || ctx.classFileList == null || ctx.classFileList.isEmpty()) {
            return;
        }
        HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> reflectionCalls = new HashMap<>();
        Map<MethodCallKey, MethodCallMeta> reflectionMeta = new HashMap<>();

        for (ClassFileEntity file : ctx.classFileList) {
            if (file == null) {
                continue;
            }
            byte[] bytes = file.getFile();
            if (bytes == null || bytes.length == 0) {
                continue;
            }
            try {
                ClassReader cr = new ClassReader(bytes);
                ClassNode cn = new ClassNode();
                cr.accept(cn, Const.GlobalASMOptions);
                ReflectionCallResolver.appendReflectionEdges(
                        cn,
                        reflectionCalls,
                        ctx.methodMap,
                        reflectionMeta,
                        false
                );
            } catch (Exception ex) {
                logger.debug("pta reflection collect failed: {}", ex.toString());
            }
        }

        if (reflectionCalls.isEmpty()) {
            return;
        }

        for (Map.Entry<MethodReference.Handle, HashSet<MethodReference.Handle>> entry : reflectionCalls.entrySet()) {
            MethodReference.Handle caller = canonicalHandle(entry.getKey(), ctx.methodMap);
            if (caller == null) {
                continue;
            }
            Set<MethodReference.Handle> targets = entry.getValue();
            if (targets == null || targets.isEmpty()) {
                continue;
            }
            for (MethodReference.Handle targetRaw : targets) {
                MethodReference.Handle target = canonicalHandle(targetRaw, ctx.methodMap);
                if (target == null) {
                    continue;
                }
                MethodCallMeta meta = resolveMeta(reflectionMeta, caller, target);
                String type = meta == null ? MethodCallMeta.TYPE_REFLECTION : meta.getType();
                if (!MethodCallMeta.TYPE_REFLECTION.equals(type)
                        && !MethodCallMeta.TYPE_METHOD_HANDLE.equals(type)) {
                    continue;
                }
                String confidence = meta == null ? MethodCallMeta.CONF_MEDIUM : meta.getConfidence();
                String evidence = meta == null ? "" : meta.getEvidence();
                String reason = "pta_" + type;
                if (evidence != null && !evidence.trim().isEmpty()) {
                    reason = reason + ":" + shorten(evidence.trim(), 96);
                }
                int opcode = meta == null ? -1 : meta.getBestOpcode();
                if (opcode <= 0) {
                    opcode = normalizeOpcode(target.getOpcode());
                }
                graph.addSyntheticCall(caller, new PtaSyntheticCall(target, type, confidence, reason, opcode));
            }
        }
    }

    private static MethodCallMeta resolveMeta(Map<MethodCallKey, MethodCallMeta> metaMap,
                                              MethodReference.Handle caller,
                                              MethodReference.Handle callee) {
        return MethodCallMeta.resolve(metaMap, caller, callee);
    }

    private static MethodReference.Handle toCallerHandle(CallSiteEntity site) {
        if (site == null || site.getCallerClassName() == null
                || site.getCallerMethodName() == null || site.getCallerMethodDesc() == null) {
            return null;
        }
        ClassReference.Handle clazz = new ClassReference.Handle(
                site.getCallerClassName(),
                normalizeJarId(site.getJarId())
        );
        return new MethodReference.Handle(clazz, site.getCallerMethodName(), site.getCallerMethodDesc());
    }

    private static MethodReference.Handle canonicalHandle(MethodReference.Handle handle,
                                                          Map<MethodReference.Handle, MethodReference> methodMap) {
        if (handle == null || handle.getClassReference() == null) {
            return null;
        }
        String owner = handle.getClassReference().getName();
        if (owner == null || handle.getName() == null || handle.getDesc() == null) {
            return null;
        }
        MethodReference probe = methodMap == null ? null : methodMap.get(
                new MethodReference.Handle(new ClassReference.Handle(owner), handle.getName(), handle.getDesc())
        );
        int jarId = probe == null || probe.getJarId() == null
                ? normalizeJarId(handle.getJarId()) : normalizeJarId(probe.getJarId());
        int opcode = normalizeOpcode(handle.getOpcode());
        ClassReference.Handle canonicalClass = new ClassReference.Handle(owner, jarId);
        return new MethodReference.Handle(canonicalClass, opcode, handle.getName(), handle.getDesc());
    }

    private static String normalizeClassName(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.startsWith("[")) {
            return null;
        }
        if (value.length() == 1) {
            return null;
        }
        if (value.startsWith("L") && value.endsWith(";")) {
            value = value.substring(1, value.length() - 1);
        }
        if (value.contains(".")) {
            value = value.replace('.', '/');
        }
        return value;
    }

    private static int normalizeJarId(Integer jarId) {
        return jarId == null ? -1 : jarId;
    }

    private static int normalizeOpcode(Integer opcode) {
        return opcode == null ? -1 : opcode;
    }

    private static int normalizeIndex(Integer idx) {
        return idx == null ? -1 : idx;
    }

    private static ClassReference.Handle normalizeRefType(Type type) {
        if (type == null) {
            return null;
        }
        if (type.getSort() == Type.OBJECT) {
            String name = normalizeClassName(type.getInternalName());
            if (name != null) {
                return new ClassReference.Handle(name);
            }
            return null;
        }
        if (type.getSort() == Type.ARRAY) {
            return new ClassReference.Handle(type.getDescriptor());
        }
        return null;
    }

    private static String localVarId(int idx) {
        if (idx < 0) {
            return "v?";
        }
        return "v" + idx;
    }

    private static String shorten(String value, int max) {
        if (value == null) {
            return "";
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private static final class PtaConstraintIndex {
        private final Map<String, String> receiverVarByCallSite = new HashMap<>();
        private final Map<MethodReference.Handle, Set<AssignSeed>> assignByMethod = new HashMap<>();
        private final Map<MethodReference.Handle, Set<AllocSeed>> allocByMethod = new HashMap<>();
        private final Map<MethodReference.Handle, Set<FieldStoreSeed>> fieldStoreByMethod = new HashMap<>();
        private final Map<MethodReference.Handle, Set<FieldLoadSeed>> fieldLoadByMethod = new HashMap<>();
        private final Map<MethodReference.Handle, Set<ArrayStoreSeed>> arrayStoreByMethod = new HashMap<>();
        private final Map<MethodReference.Handle, Set<ArrayLoadSeed>> arrayLoadByMethod = new HashMap<>();
        private final Map<MethodReference.Handle, Set<ArrayCopySeed>> arrayCopyByMethod = new HashMap<>();

        private String receiverVarId(String callSiteKey) {
            if (callSiteKey == null || callSiteKey.trim().isEmpty()) {
                return null;
            }
            String trimmed = callSiteKey.trim();
            String direct = receiverVarByCallSite.get(trimmed);
            if (direct != null) {
                return direct;
            }
            String loose = toLooseCallSiteKey(trimmed);
            if (loose == null) {
                return null;
            }
            return receiverVarByCallSite.get(loose);
        }

        private void bindReceiverVar(String callSiteKey, String receiverVarId) {
            if (callSiteKey == null || callSiteKey.trim().isEmpty()
                    || receiverVarId == null || receiverVarId.trim().isEmpty()) {
                return;
            }
            String key = callSiteKey.trim();
            String value = receiverVarId.trim();
            receiverVarByCallSite.putIfAbsent(key, value);
            String loose = toLooseCallSiteKey(key);
            if (loose != null) {
                receiverVarByCallSite.putIfAbsent(loose, value);
            }
        }

        private static String toLooseCallSiteKey(String key) {
            if (key == null) {
                return null;
            }
            int first = key.indexOf('|');
            if (first <= 0 || first + 1 >= key.length()) {
                return null;
            }
            return key.substring(first + 1);
        }

        private void addAssign(MethodReference.Handle method, String fromVarId, String toVarId) {
            if (method == null || fromVarId == null || toVarId == null
                    || fromVarId.trim().isEmpty() || toVarId.trim().isEmpty()) {
                return;
            }
            if (fromVarId.equals(toVarId)) {
                return;
            }
            assignByMethod.computeIfAbsent(method, k -> new LinkedHashSet<>())
                    .add(new AssignSeed(fromVarId, toVarId));
        }

        private void addAlloc(MethodReference.Handle method,
                              String varId,
                              ClassReference.Handle type,
                              String allocSite) {
            if (method == null || varId == null || varId.trim().isEmpty()
                    || type == null || type.getName() == null) {
                return;
            }
            allocByMethod.computeIfAbsent(method, k -> new LinkedHashSet<>())
                    .add(new AllocSeed(varId, type, allocSite));
        }

        private void addFieldStore(MethodReference.Handle method,
                                   String baseVarId,
                                   String fieldSig,
                                   String valueVarId) {
            if (method == null || baseVarId == null || valueVarId == null
                    || fieldSig == null || fieldSig.trim().isEmpty()) {
                return;
            }
            fieldStoreByMethod.computeIfAbsent(method, k -> new LinkedHashSet<>())
                    .add(new FieldStoreSeed(baseVarId, fieldSig.trim(), valueVarId, false));
        }

        private void addStaticFieldStore(MethodReference.Handle method,
                                         String fieldSig,
                                         String valueVarId) {
            if (method == null || valueVarId == null
                    || fieldSig == null || fieldSig.trim().isEmpty()) {
                return;
            }
            fieldStoreByMethod.computeIfAbsent(method, k -> new LinkedHashSet<>())
                    .add(new FieldStoreSeed(null, fieldSig.trim(), valueVarId, true));
        }

        private void addFieldLoad(MethodReference.Handle method,
                                  String baseVarId,
                                  String fieldSig,
                                  String toVarId) {
            if (method == null || baseVarId == null || toVarId == null
                    || fieldSig == null || fieldSig.trim().isEmpty()) {
                return;
            }
            fieldLoadByMethod.computeIfAbsent(method, k -> new LinkedHashSet<>())
                    .add(new FieldLoadSeed(baseVarId, fieldSig.trim(), toVarId, false));
        }

        private void addStaticFieldLoad(MethodReference.Handle method,
                                        String fieldSig,
                                        String toVarId) {
            if (method == null || toVarId == null
                    || fieldSig == null || fieldSig.trim().isEmpty()) {
                return;
            }
            fieldLoadByMethod.computeIfAbsent(method, k -> new LinkedHashSet<>())
                    .add(new FieldLoadSeed(null, fieldSig.trim(), toVarId, true));
        }

        private void addArrayStore(MethodReference.Handle method,
                                   String arrayVarId,
                                   Integer index,
                                   String valueVarId) {
            if (method == null || arrayVarId == null || valueVarId == null) {
                return;
            }
            arrayStoreByMethod.computeIfAbsent(method, k -> new LinkedHashSet<>())
                    .add(new ArrayStoreSeed(arrayVarId, index, valueVarId));
        }

        private void addArrayLoad(MethodReference.Handle method,
                                  String arrayVarId,
                                  Integer index,
                                  String toVarId) {
            if (method == null || arrayVarId == null || toVarId == null) {
                return;
            }
            arrayLoadByMethod.computeIfAbsent(method, k -> new LinkedHashSet<>())
                    .add(new ArrayLoadSeed(arrayVarId, index, toVarId));
        }

        private void addArrayCopy(MethodReference.Handle method,
                                  String srcArrayVarId,
                                  String dstArrayVarId) {
            if (method == null || srcArrayVarId == null || dstArrayVarId == null) {
                return;
            }
            arrayCopyByMethod.computeIfAbsent(method, k -> new LinkedHashSet<>())
                    .add(new ArrayCopySeed(srcArrayVarId, dstArrayVarId));
        }

        private void applyToGraph(PointerAssignmentGraph graph,
                                  Map<MethodReference.Handle, MethodReference> methodMap) {
            if (graph == null || methodMap == null || methodMap.isEmpty()) {
                return;
            }
            PtaContext root = PtaContext.root();
            for (Map.Entry<MethodReference.Handle, Set<AssignSeed>> entry : assignByMethod.entrySet()) {
                MethodReference.Handle method = canonicalHandle(entry.getKey(), methodMap);
                if (method == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                    continue;
                }
                PtaContextMethod owner = new PtaContextMethod(method, root);
                for (AssignSeed seed : entry.getValue()) {
                    if (seed == null) {
                        continue;
                    }
                    graph.addAssignEdge(
                            new PtaVarNode(owner, seed.fromVarId),
                            new PtaVarNode(owner, seed.toVarId));
                }
            }
            for (Map.Entry<MethodReference.Handle, Set<AllocSeed>> entry : allocByMethod.entrySet()) {
                MethodReference.Handle method = canonicalHandle(entry.getKey(), methodMap);
                if (method == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                    continue;
                }
                PtaContextMethod owner = new PtaContextMethod(method, root);
                for (AllocSeed seed : entry.getValue()) {
                    if (seed == null || seed.type == null || seed.type.getName() == null) {
                        continue;
                    }
                    graph.addAllocEdge(
                            new PtaAllocNode(seed.type, seed.allocSite),
                            new PtaVarNode(owner, seed.varId));
                }
            }
            for (Map.Entry<MethodReference.Handle, Set<FieldStoreSeed>> entry : fieldStoreByMethod.entrySet()) {
                MethodReference.Handle method = canonicalHandle(entry.getKey(), methodMap);
                if (method == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                    continue;
                }
                PtaContextMethod owner = new PtaContextMethod(method, root);
                for (FieldStoreSeed seed : entry.getValue()) {
                    if (seed == null) {
                        continue;
                    }
                    PtaVarNode base = seed.isStatic ? null : new PtaVarNode(owner, seed.baseVarId);
                    PtaVarNode value = new PtaVarNode(owner, seed.valueVarId);
                    graph.addFieldStoreEdge(new PointerAssignmentGraph.FieldStoreEdge(
                            base, value, seed.fieldSig, seed.isStatic));
                }
            }
            for (Map.Entry<MethodReference.Handle, Set<FieldLoadSeed>> entry : fieldLoadByMethod.entrySet()) {
                MethodReference.Handle method = canonicalHandle(entry.getKey(), methodMap);
                if (method == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                    continue;
                }
                PtaContextMethod owner = new PtaContextMethod(method, root);
                for (FieldLoadSeed seed : entry.getValue()) {
                    if (seed == null) {
                        continue;
                    }
                    PtaVarNode base = seed.isStatic ? null : new PtaVarNode(owner, seed.baseVarId);
                    PtaVarNode to = new PtaVarNode(owner, seed.toVarId);
                    graph.addFieldLoadEdge(new PointerAssignmentGraph.FieldLoadEdge(
                            base, to, seed.fieldSig, seed.isStatic));
                }
            }
            for (Map.Entry<MethodReference.Handle, Set<ArrayStoreSeed>> entry : arrayStoreByMethod.entrySet()) {
                MethodReference.Handle method = canonicalHandle(entry.getKey(), methodMap);
                if (method == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                    continue;
                }
                PtaContextMethod owner = new PtaContextMethod(method, root);
                for (ArrayStoreSeed seed : entry.getValue()) {
                    if (seed == null) {
                        continue;
                    }
                    graph.addArrayStoreEdge(new PointerAssignmentGraph.ArrayStoreEdge(
                            new PtaVarNode(owner, seed.arrayVarId),
                            new PtaVarNode(owner, seed.valueVarId),
                            seed.index));
                }
            }
            for (Map.Entry<MethodReference.Handle, Set<ArrayLoadSeed>> entry : arrayLoadByMethod.entrySet()) {
                MethodReference.Handle method = canonicalHandle(entry.getKey(), methodMap);
                if (method == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                    continue;
                }
                PtaContextMethod owner = new PtaContextMethod(method, root);
                for (ArrayLoadSeed seed : entry.getValue()) {
                    if (seed == null) {
                        continue;
                    }
                    graph.addArrayLoadEdge(new PointerAssignmentGraph.ArrayLoadEdge(
                            new PtaVarNode(owner, seed.arrayVarId),
                            new PtaVarNode(owner, seed.toVarId),
                            seed.index));
                }
            }
            for (Map.Entry<MethodReference.Handle, Set<ArrayCopySeed>> entry : arrayCopyByMethod.entrySet()) {
                MethodReference.Handle method = canonicalHandle(entry.getKey(), methodMap);
                if (method == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                    continue;
                }
                PtaContextMethod owner = new PtaContextMethod(method, root);
                for (ArrayCopySeed seed : entry.getValue()) {
                    if (seed == null) {
                        continue;
                    }
                    graph.addArrayCopyEdge(new PointerAssignmentGraph.ArrayCopyEdge(
                            new PtaVarNode(owner, seed.srcArrayVarId),
                            new PtaVarNode(owner, seed.dstArrayVarId)));
                }
            }
        }
    }

    private static final class AssignSeed {
        private final String fromVarId;
        private final String toVarId;

        private AssignSeed(String fromVarId, String toVarId) {
            this.fromVarId = fromVarId;
            this.toVarId = toVarId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AssignSeed that = (AssignSeed) o;
            return Objects.equals(fromVarId, that.fromVarId) && Objects.equals(toVarId, that.toVarId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fromVarId, toVarId);
        }
    }

    private static final class AllocSeed {
        private final String varId;
        private final ClassReference.Handle type;
        private final String allocSite;

        private AllocSeed(String varId, ClassReference.Handle type, String allocSite) {
            this.varId = varId;
            this.type = type == null ? null : new ClassReference.Handle(type.getName(), type.getJarId());
            this.allocSite = allocSite == null ? "" : allocSite;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AllocSeed allocSeed = (AllocSeed) o;
            return Objects.equals(varId, allocSeed.varId)
                    && Objects.equals(type, allocSeed.type)
                    && Objects.equals(allocSite, allocSeed.allocSite);
        }

        @Override
        public int hashCode() {
            return Objects.hash(varId, type, allocSite);
        }
    }

    private static final class FieldStoreSeed {
        private final String baseVarId;
        private final String fieldSig;
        private final String valueVarId;
        private final boolean isStatic;

        private FieldStoreSeed(String baseVarId, String fieldSig, String valueVarId, boolean isStatic) {
            this.baseVarId = baseVarId;
            this.fieldSig = fieldSig;
            this.valueVarId = valueVarId;
            this.isStatic = isStatic;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            FieldStoreSeed that = (FieldStoreSeed) o;
            return isStatic == that.isStatic
                    && Objects.equals(baseVarId, that.baseVarId)
                    && Objects.equals(fieldSig, that.fieldSig)
                    && Objects.equals(valueVarId, that.valueVarId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(baseVarId, fieldSig, valueVarId, isStatic);
        }
    }

    private static final class FieldLoadSeed {
        private final String baseVarId;
        private final String fieldSig;
        private final String toVarId;
        private final boolean isStatic;

        private FieldLoadSeed(String baseVarId, String fieldSig, String toVarId, boolean isStatic) {
            this.baseVarId = baseVarId;
            this.fieldSig = fieldSig;
            this.toVarId = toVarId;
            this.isStatic = isStatic;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            FieldLoadSeed that = (FieldLoadSeed) o;
            return isStatic == that.isStatic
                    && Objects.equals(baseVarId, that.baseVarId)
                    && Objects.equals(fieldSig, that.fieldSig)
                    && Objects.equals(toVarId, that.toVarId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(baseVarId, fieldSig, toVarId, isStatic);
        }
    }

    private static final class ArrayStoreSeed {
        private final String arrayVarId;
        private final Integer index;
        private final String valueVarId;

        private ArrayStoreSeed(String arrayVarId, Integer index, String valueVarId) {
            this.arrayVarId = arrayVarId;
            this.index = index;
            this.valueVarId = valueVarId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ArrayStoreSeed that = (ArrayStoreSeed) o;
            return Objects.equals(arrayVarId, that.arrayVarId)
                    && Objects.equals(index, that.index)
                    && Objects.equals(valueVarId, that.valueVarId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(arrayVarId, index, valueVarId);
        }
    }

    private static final class ArrayLoadSeed {
        private final String arrayVarId;
        private final Integer index;
        private final String toVarId;

        private ArrayLoadSeed(String arrayVarId, Integer index, String toVarId) {
            this.arrayVarId = arrayVarId;
            this.index = index;
            this.toVarId = toVarId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ArrayLoadSeed that = (ArrayLoadSeed) o;
            return Objects.equals(arrayVarId, that.arrayVarId)
                    && Objects.equals(index, that.index)
                    && Objects.equals(toVarId, that.toVarId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(arrayVarId, index, toVarId);
        }
    }

    private static final class ArrayCopySeed {
        private final String srcArrayVarId;
        private final String dstArrayVarId;

        private ArrayCopySeed(String srcArrayVarId, String dstArrayVarId) {
            this.srcArrayVarId = srcArrayVarId;
            this.dstArrayVarId = dstArrayVarId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ArrayCopySeed that = (ArrayCopySeed) o;
            return Objects.equals(srcArrayVarId, that.srcArrayVarId)
                    && Objects.equals(dstArrayVarId, that.dstArrayVarId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(srcArrayVarId, dstArrayVarId);
        }
    }

    private static final class Solver {
        private final BuildContext ctx;
        private final PointerAssignmentGraph graph;
        private final PtaSolverConfig config;
        private final InheritanceMap inheritance;
        private final HeapObjectIndexer objectIndexer = new HeapObjectIndexer();
        private final PtaCompositePlugin plugin;

        private final Map<PtaVarNode, PtaPointsToSet> pointsTo = new HashMap<>();
        private final Map<String, PtaPointsToSet> heapSlots = new HashMap<>();
        private final ArrayDeque<PtaVarNode> varWorkList = new ArrayDeque<>();
        private final ArrayDeque<PtaContextMethod> methodWorkList = new ArrayDeque<>();
        private final Set<PtaContextMethod> queuedMethods = new HashSet<>();
        private final Set<PtaContextMethod> knownMethods = new HashSet<>();
        private final Set<MethodReference.Handle> reachedMethods = new HashSet<>();
        private final Map<PtaContextMethod, Set<ClassReference.Handle>> contextThisTypes = new HashMap<>();
        private final Map<DispatchLookupKey, MethodReference.Handle> hierarchyDispatchCache = new HashMap<>();
        private final Map<DispatchLookupKey, Set<MethodReference.Handle>> interfaceDefaultsCache = new HashMap<>();
        private final Map<String, Set<ClassReference.Handle>> fallbackReceiverCache = new HashMap<>();
        private final Map<String, Boolean> assignableCache = new HashMap<>();
        private final Map<MethodReference.Handle, Integer> contextCountByMethod = new HashMap<>();
        private final Map<String, Integer> contextCountBySite = new HashMap<>();
        private final Set<MethodReference.Handle> objectWidenReported = new HashSet<>();
        private final Map<DescriptorCompatibilityKey, Boolean> descriptorCompatCache = new HashMap<>();
        private final Map<String, Type> asmTypeCache = new HashMap<>();
        private final Set<String> invalidTypeDescriptors = new HashSet<>();

        private final Map<PtaVarNode, Set<PointerAssignmentGraph.FieldStoreEdge>> fieldStoresByBase = new HashMap<>();
        private final Map<PtaVarNode, Set<PointerAssignmentGraph.FieldStoreEdge>> fieldStoresByValue = new HashMap<>();
        private final Map<PtaVarNode, Set<PointerAssignmentGraph.FieldLoadEdge>> fieldLoadsByBase = new HashMap<>();
        private final Map<String, Set<PointerAssignmentGraph.FieldLoadEdge>> fieldLoadsBySig = new HashMap<>();
        private final Map<PtaVarNode, Set<PointerAssignmentGraph.ArrayStoreEdge>> arrayStoresByBase = new HashMap<>();
        private final Map<PtaVarNode, Set<PointerAssignmentGraph.ArrayStoreEdge>> arrayStoresByValue = new HashMap<>();
        private final Map<PtaVarNode, Set<PointerAssignmentGraph.ArrayLoadEdge>> arrayLoadsByBase = new HashMap<>();
        private final Map<PtaVarNode, Set<PointerAssignmentGraph.ArrayCopyEdge>> arrayCopiesBySrc = new HashMap<>();
        private final Map<PtaVarNode, Set<PointerAssignmentGraph.ArrayCopyEdge>> arrayCopiesByDst = new HashMap<>();

        private final IncrementalPtaState incrementalState;
        private final Set<String> incrementalSeen = new HashSet<>();

        private final Result result = new Result();

        private Solver(BuildContext ctx,
                       PointerAssignmentGraph graph,
                       PtaSolverConfig config,
                       String incrementalScope) {
            this.ctx = ctx;
            this.graph = graph;
            this.config = config;
            this.inheritance = ctx == null ? null : ctx.inheritanceMap;
            this.incrementalState = new IncrementalPtaState(incrementalScope);
            this.plugin = PtaPluginLoader.load(config);
            this.plugin.setBridge(new SolverPluginBridge());
            indexHeapEdges();
        }

        private void indexHeapEdges() {
            for (PointerAssignmentGraph.FieldStoreEdge edge : graph.getFieldStoreEdges()) {
                if (edge == null || edge.getValueVar() == null) {
                    continue;
                }
                if (!edge.isStatic() && edge.getBaseVar() != null) {
                    fieldStoresByBase.computeIfAbsent(edge.getBaseVar(), k -> new LinkedHashSet<>()).add(edge);
                }
                fieldStoresByValue.computeIfAbsent(edge.getValueVar(), k -> new LinkedHashSet<>()).add(edge);
            }
            for (PointerAssignmentGraph.FieldLoadEdge edge : graph.getFieldLoadEdges()) {
                if (edge == null || edge.getToVar() == null) {
                    continue;
                }
                if (!edge.isStatic() && edge.getBaseVar() != null) {
                    fieldLoadsByBase.computeIfAbsent(edge.getBaseVar(), k -> new LinkedHashSet<>()).add(edge);
                }
                fieldLoadsBySig.computeIfAbsent(edge.getFieldSig(), k -> new LinkedHashSet<>()).add(edge);
            }
            for (PointerAssignmentGraph.ArrayStoreEdge edge : graph.getArrayStoreEdges()) {
                if (edge == null || edge.getArrayVar() == null || edge.getValueVar() == null) {
                    continue;
                }
                arrayStoresByBase.computeIfAbsent(edge.getArrayVar(), k -> new LinkedHashSet<>()).add(edge);
                arrayStoresByValue.computeIfAbsent(edge.getValueVar(), k -> new LinkedHashSet<>()).add(edge);
            }
            for (PointerAssignmentGraph.ArrayLoadEdge edge : graph.getArrayLoadEdges()) {
                if (edge == null || edge.getArrayVar() == null || edge.getToVar() == null) {
                    continue;
                }
                arrayLoadsByBase.computeIfAbsent(edge.getArrayVar(), k -> new LinkedHashSet<>()).add(edge);
            }
            for (PointerAssignmentGraph.ArrayCopyEdge edge : graph.getArrayCopyEdges()) {
                if (edge == null || edge.getSrcArrayVar() == null || edge.getDstArrayVar() == null) {
                    continue;
                }
                arrayCopiesBySrc.computeIfAbsent(edge.getSrcArrayVar(), k -> new LinkedHashSet<>()).add(edge);
                arrayCopiesByDst.computeIfAbsent(edge.getDstArrayVar(), k -> new LinkedHashSet<>()).add(edge);
            }
        }

        private Result solve() {
            plugin.onStart();
            seedAllocEdges();
            flushVarWorkList();
            seedEntryMethods();

            int loopGuard = 0;
            int loopMax = Math.max(1024, config.getMaxContextMethods() * 6);
            while (!methodWorkList.isEmpty() && loopGuard < loopMax) {
                PtaContextMethod cm = methodWorkList.pollFirst();
                queuedMethods.remove(cm);
                if (cm == null || cm.getMethod() == null) {
                    loopGuard++;
                    continue;
                }
                processContextMethod(cm);
                flushVarWorkList();
                loopGuard++;
            }
            if (loopGuard >= loopMax) {
                logger.warn("pta loop limit reached: {}", loopMax);
            }

            if (config.isIncremental()) {
                incrementalState.cleanup(incrementalSeen);
            }
            result.contextMethodCount = knownMethods.size();
            plugin.onFinish(result);
            return result;
        }

        private void seedAllocEdges() {
            for (Map.Entry<PtaAllocNode, Set<PtaVarNode>> entry : graph.getAllocEdges().entrySet()) {
                if (entry.getKey() == null || entry.getKey().getType() == null
                        || entry.getValue() == null || entry.getValue().isEmpty()) {
                    continue;
                }
                for (PtaVarNode var : entry.getValue()) {
                    addPoint(var, createAllocObject(entry.getKey(), var));
                }
            }
        }

        private PtaHeapObject createAllocObject(PtaAllocNode alloc, PtaVarNode var) {
            ClassReference.Handle type = alloc.getType();
            String allocSite = alloc == null || alloc.getAllocSite() == null ? "" : alloc.getAllocSite();
            String ctxKey = allocationContext(var);
            return new PtaHeapObject(
                    new ClassReference.Handle(type.getName(), type.getJarId()),
                    allocSite,
                    ctxKey
            );
        }

        private String allocationContext(PtaVarNode var) {
            if (var == null || var.getOwner() == null || var.getOwner().getContext() == null) {
                return "root";
            }
            MethodReference.Handle ownerMethod = var.getOwner().getMethod();
            int activeContexts = ownerMethod == null ? 0 : contextCountByMethod.getOrDefault(ownerMethod, 1);
            int baseDepth = config.objectSensitivityDepthFor(ownerMethod);
            int depth = config.objectSensitivityDepthFor(ownerMethod, activeContexts);
            if (ownerMethod != null && depth < baseDepth && objectWidenReported.add(ownerMethod)) {
                result.adaptiveObjectWidened++;
            }
            return var.getOwner().getContext().renderDepth(depth);
        }

        private void flushVarWorkList() {
            while (!varWorkList.isEmpty()) {
                PtaVarNode from = varWorkList.pollFirst();
                if (from == null) {
                    continue;
                }
                PtaPointsToSet fromObjs = pointsTo.get(from);
                if (fromObjs == null || fromObjs.isEmpty()) {
                    continue;
                }
                Set<PtaVarNode> assignTargets = graph.getAssignTargets(from);
                if (!assignTargets.isEmpty()) {
                    for (PtaVarNode to : assignTargets) {
                        if (to == null) {
                            continue;
                        }
                        for (PtaHeapObject obj : fromObjs) {
                            addPoint(to, obj);
                        }
                    }
                }
                propagateHeapEffects(from);
            }
        }

        private void propagateHeapEffects(PtaVarNode var) {
            if (var == null) {
                return;
            }
            Set<PointerAssignmentGraph.FieldStoreEdge> storesByBase = fieldStoresByBase.get(var);
            if (storesByBase != null) {
                for (PointerAssignmentGraph.FieldStoreEdge edge : storesByBase) {
                    applyFieldStore(edge);
                }
            }
            Set<PointerAssignmentGraph.FieldStoreEdge> storesByValue = fieldStoresByValue.get(var);
            if (storesByValue != null) {
                for (PointerAssignmentGraph.FieldStoreEdge edge : storesByValue) {
                    applyFieldStore(edge);
                }
            }
            Set<PointerAssignmentGraph.FieldLoadEdge> loadsByBase = fieldLoadsByBase.get(var);
            if (loadsByBase != null) {
                for (PointerAssignmentGraph.FieldLoadEdge edge : loadsByBase) {
                    applyFieldLoad(edge);
                }
            }
            Set<PointerAssignmentGraph.ArrayStoreEdge> arrStoresByBase = arrayStoresByBase.get(var);
            if (arrStoresByBase != null) {
                for (PointerAssignmentGraph.ArrayStoreEdge edge : arrStoresByBase) {
                    applyArrayStore(edge);
                }
            }
            Set<PointerAssignmentGraph.ArrayStoreEdge> arrStoresByValue = arrayStoresByValue.get(var);
            if (arrStoresByValue != null) {
                for (PointerAssignmentGraph.ArrayStoreEdge edge : arrStoresByValue) {
                    applyArrayStore(edge);
                }
            }
            Set<PointerAssignmentGraph.ArrayLoadEdge> arrLoads = arrayLoadsByBase.get(var);
            if (arrLoads != null) {
                for (PointerAssignmentGraph.ArrayLoadEdge edge : arrLoads) {
                    applyArrayLoad(edge);
                }
            }
            Set<PointerAssignmentGraph.ArrayCopyEdge> copiesFromSrc = arrayCopiesBySrc.get(var);
            if (copiesFromSrc != null) {
                for (PointerAssignmentGraph.ArrayCopyEdge edge : copiesFromSrc) {
                    applyArrayCopy(edge);
                }
            }
            Set<PointerAssignmentGraph.ArrayCopyEdge> copiesFromDst = arrayCopiesByDst.get(var);
            if (copiesFromDst != null) {
                for (PointerAssignmentGraph.ArrayCopyEdge edge : copiesFromDst) {
                    applyArrayCopy(edge);
                }
            }
        }

        private void applyFieldStore(PointerAssignmentGraph.FieldStoreEdge edge) {
            if (edge == null || edge.getValueVar() == null || edge.getFieldSig() == null) {
                return;
            }
            PtaPointsToSet values = filterByFieldType(pointsTo.get(edge.getValueVar()), edge.getFieldSig());
            if (values == null || values.isEmpty()) {
                return;
            }
            if (edge.isStatic()) {
                String slot = "S|" + edge.getFieldSig();
                if (addSlotPoints(slot, values)) {
                    propagateStaticFieldLoads(edge.getFieldSig(), slot);
                }
                return;
            }
            PtaVarNode baseVar = edge.getBaseVar();
            PtaPointsToSet bases = baseVar == null ? null : pointsTo.get(baseVar);
            if (bases == null || bases.isEmpty()) {
                return;
            }
            for (PtaHeapObject base : bases) {
                String slot = fieldSlotKey(base, edge.getFieldSig());
                if (!addSlotPoints(slot, values)) {
                    continue;
                }
                propagateInstanceFieldLoads(edge.getFieldSig(), slot);
            }
        }

        private void applyFieldLoad(PointerAssignmentGraph.FieldLoadEdge edge) {
            if (edge == null || edge.getToVar() == null || edge.getFieldSig() == null) {
                return;
            }
            if (edge.isStatic()) {
                PtaPointsToSet values = heapSlots.get("S|" + edge.getFieldSig());
                if (values == null || values.isEmpty()) {
                    return;
                }
                for (PtaHeapObject value : values) {
                    addPoint(edge.getToVar(), value);
                }
                return;
            }
            PtaVarNode baseVar = edge.getBaseVar();
            PtaPointsToSet bases = baseVar == null ? null : pointsTo.get(baseVar);
            if (bases == null || bases.isEmpty()) {
                return;
            }
            for (PtaHeapObject base : bases) {
                String slot = fieldSlotKey(base, edge.getFieldSig());
                PtaPointsToSet values = heapSlots.get(slot);
                if (values == null || values.isEmpty()) {
                    continue;
                }
                for (PtaHeapObject value : values) {
                    addPoint(edge.getToVar(), value);
                }
            }
        }

        private void propagateStaticFieldLoads(String fieldSig, String slot) {
            PtaPointsToSet values = heapSlots.get(slot);
            if (values == null || values.isEmpty()) {
                return;
            }
            Set<PointerAssignmentGraph.FieldLoadEdge> loads = fieldLoadsBySig.get(fieldSig);
            if (loads == null || loads.isEmpty()) {
                return;
            }
            for (PointerAssignmentGraph.FieldLoadEdge load : loads) {
                if (load == null || !load.isStatic()) {
                    continue;
                }
                for (PtaHeapObject value : values) {
                    addPoint(load.getToVar(), value);
                }
            }
        }

        private void propagateInstanceFieldLoads(String fieldSig, String slot) {
            PtaPointsToSet values = heapSlots.get(slot);
            if (values == null || values.isEmpty()) {
                return;
            }
            Set<PointerAssignmentGraph.FieldLoadEdge> loads = fieldLoadsBySig.get(fieldSig);
            if (loads == null || loads.isEmpty()) {
                return;
            }
            for (PointerAssignmentGraph.FieldLoadEdge load : loads) {
                if (load == null || load.isStatic() || load.getBaseVar() == null) {
                    continue;
                }
                PtaPointsToSet loadBases = pointsTo.get(load.getBaseVar());
                if (loadBases == null || loadBases.isEmpty()) {
                    continue;
                }
                boolean match = false;
                for (PtaHeapObject base : loadBases) {
                    if (slot.equals(fieldSlotKey(base, fieldSig))) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    continue;
                }
                for (PtaHeapObject value : values) {
                    addPoint(load.getToVar(), value);
                }
            }
        }

        private void applyArrayStore(PointerAssignmentGraph.ArrayStoreEdge edge) {
            if (edge == null || edge.getArrayVar() == null || edge.getValueVar() == null) {
                return;
            }
            PtaPointsToSet arrays = pointsTo.get(edge.getArrayVar());
            PtaPointsToSet values = pointsTo.get(edge.getValueVar());
            if (arrays == null || arrays.isEmpty() || values == null || values.isEmpty()) {
                return;
            }
            for (PtaHeapObject arrayObj : arrays) {
                PtaPointsToSet compatibleValues = filterByArrayElementType(values, arrayObj);
                if (compatibleValues.isEmpty()) {
                    continue;
                }
                boolean changed = false;
                String wildcardSlot = arrayWildcardSlotKey(arrayObj);
                if (addSlotPoints(wildcardSlot, compatibleValues)) {
                    changed = true;
                }
                String preciseSlot = arrayPreciseSlotKey(arrayObj, edge.getIndex());
                if (!wildcardSlot.equals(preciseSlot) && addSlotPoints(preciseSlot, compatibleValues)) {
                    changed = true;
                }
                if (changed) {
                    propagateArrayLoads(arrayObj);
                }
            }
        }

        private void applyArrayLoad(PointerAssignmentGraph.ArrayLoadEdge edge) {
            if (edge == null || edge.getArrayVar() == null || edge.getToVar() == null) {
                return;
            }
            PtaPointsToSet arrays = pointsTo.get(edge.getArrayVar());
            if (arrays == null || arrays.isEmpty()) {
                return;
            }
            for (PtaHeapObject arrayObj : arrays) {
                PtaPointsToSet wildcard = heapSlots.get(arrayWildcardSlotKey(arrayObj));
                if (wildcard != null) {
                    for (PtaHeapObject value : wildcard) {
                        addPoint(edge.getToVar(), value);
                    }
                }
                String preciseSlot = arrayPreciseSlotKey(arrayObj, edge.getIndex());
                PtaPointsToSet precise = heapSlots.get(preciseSlot);
                if (precise != null) {
                    for (PtaHeapObject value : precise) {
                        addPoint(edge.getToVar(), value);
                    }
                }
            }
        }

        private void applyArrayCopy(PointerAssignmentGraph.ArrayCopyEdge edge) {
            if (edge == null || edge.getSrcArrayVar() == null || edge.getDstArrayVar() == null) {
                return;
            }
            PtaPointsToSet srcArrays = pointsTo.get(edge.getSrcArrayVar());
            PtaPointsToSet dstArrays = pointsTo.get(edge.getDstArrayVar());
            if (srcArrays == null || srcArrays.isEmpty() || dstArrays == null || dstArrays.isEmpty()) {
                return;
            }
            for (PtaHeapObject src : srcArrays) {
                PtaPointsToSet srcValues = heapSlots.get(arrayWildcardSlotKey(src));
                if (srcValues == null || srcValues.isEmpty()) {
                    continue;
                }
                for (PtaHeapObject dst : dstArrays) {
                    PtaPointsToSet compatibleValues = filterByArrayElementType(srcValues, dst);
                    if (compatibleValues.isEmpty()) {
                        continue;
                    }
                    String dstSlot = arrayWildcardSlotKey(dst);
                    if (!addSlotPoints(dstSlot, compatibleValues)) {
                        continue;
                    }
                    propagateArrayLoads(dst);
                }
            }
        }

        private void propagateArrayLoads(PtaHeapObject changedArray) {
            if (changedArray == null || changedArray.getType() == null) {
                return;
            }
            for (PointerAssignmentGraph.ArrayLoadEdge load : graph.getArrayLoadEdges()) {
                if (load == null || load.getArrayVar() == null || load.getToVar() == null) {
                    continue;
                }
                PtaPointsToSet bases = pointsTo.get(load.getArrayVar());
                if (bases == null || bases.isEmpty()) {
                    continue;
                }
                boolean alias = false;
                for (PtaHeapObject base : bases) {
                    if (arrayWildcardSlotKey(base).equals(arrayWildcardSlotKey(changedArray))) {
                        alias = true;
                        break;
                    }
                }
                if (!alias) {
                    continue;
                }
                applyArrayLoad(load);
            }
        }

        private boolean addSlotPoints(String slotKey, PtaPointsToSet values) {
            if (slotKey == null || values == null || values.isEmpty()) {
                return false;
            }
            PtaPointsToSet slot = heapSlots.computeIfAbsent(slotKey, k -> newPointsToSet());
            return slot.addAll(values);
        }

        private String fieldSlotKey(PtaHeapObject base, String fieldSig) {
            if (base == null || fieldSig == null) {
                return "F|*|" + fieldSig;
            }
            int depth = config.getFieldSensitivityDepth();
            if (depth <= 0) {
                return "F|*|" + fieldSig;
            }
            if (depth == 1) {
                return "F|" + base.allocSiteKey() + "|" + fieldSig;
            }
            return "F|" + base.identityKey() + "|" + fieldSig;
        }

        private String arrayWildcardSlotKey(PtaHeapObject arrayObj) {
            return "A|" + arrayObjectPart(arrayObj) + "|*";
        }

        private String arrayPreciseSlotKey(PtaHeapObject arrayObj, Integer index) {
            if (config.getArraySensitivityDepth() < 2 || index == null) {
                return arrayWildcardSlotKey(arrayObj);
            }
            return "A|" + arrayObjectPart(arrayObj) + "|" + index;
        }

        private String arrayObjectPart(PtaHeapObject arrayObj) {
            if (arrayObj == null) {
                return "*";
            }
            int depth = config.getArraySensitivityDepth();
            if (depth <= 0) {
                return "*";
            }
            if (depth == 1) {
                return arrayObj.allocSiteKey();
            }
            return arrayObj.identityKey();
        }

        private void seedEntryMethods() {
            List<MethodReference.Handle> entries = collectEntryMethods();
            for (MethodReference.Handle method : entries) {
                if (method == null) {
                    continue;
                }
                enqueueMethod(new PtaContextMethod(method, PtaContext.root()), null);
            }
        }

        private List<MethodReference.Handle> collectEntryMethods() {
            LinkedHashSet<MethodReference.Handle> out = new LinkedHashSet<>();
            if (ctx == null || ctx.methodMap == null) {
                return Collections.emptyList();
            }
            for (MethodReference.Handle caller : graph.getCallers()) {
                MethodReference.Handle canonical = canonicalHandle(caller, ctx.methodMap);
                if (canonical != null) {
                    out.add(canonical);
                }
            }
            if (ctx.methodCalls != null && !ctx.methodCalls.isEmpty()) {
                for (MethodReference.Handle caller : ctx.methodCalls.keySet()) {
                    MethodReference.Handle canonical = canonicalHandle(caller, ctx.methodMap);
                    if (canonical != null) {
                        out.add(canonical);
                    }
                }
            }
            for (MethodReference.Handle method : ctx.methodMap.keySet()) {
                if (method == null) {
                    continue;
                }
                if ("main".equals(method.getName()) && "([Ljava/lang/String;)V".equals(method.getDesc())) {
                    MethodReference.Handle canonical = canonicalHandle(method, ctx.methodMap);
                    if (canonical != null) {
                        out.add(canonical);
                    }
                }
            }
            if (ctx.controllers != null) {
                for (SpringController controller : ctx.controllers) {
                    if (controller == null || controller.getMappings() == null) {
                        continue;
                    }
                    for (SpringMapping mapping : controller.getMappings()) {
                        if (mapping == null) {
                            continue;
                        }
                        MethodReference.Handle method = canonicalHandle(mapping.getMethodName(), ctx.methodMap);
                        if (method != null) {
                            out.add(method);
                        }
                    }
                }
            }
            ArrayList<MethodReference.Handle> sorted = new ArrayList<>(out);
            if (sorted.size() > 1) {
                sorted.sort(Comparator.comparing(ContextSensitivePtaEngine::methodKey));
            }
            return sorted;
        }

        private void enqueueMethod(PtaContextMethod method, ClassReference.Handle receiverType) {
            if (method == null || method.getMethod() == null) {
                return;
            }
            PtaContextMethod effective = adaptContextByMethodBudget(method);
            if (!knownMethods.contains(effective) && knownMethods.size() >= config.getMaxContextMethods()) {
                return;
            }
            if (reachedMethods.add(effective.getMethod())) {
                plugin.onNewMethod(effective.getMethod());
            }
            if (knownMethods.add(effective)) {
                contextCountByMethod.merge(effective.getMethod(), 1, Integer::sum);
                plugin.onNewContextMethod(effective);
            }
            if (receiverType != null) {
                contextThisTypes.computeIfAbsent(effective, k -> new LinkedHashSet<>()).add(receiverType);
                addTypePoint(new PtaVarNode(effective, localVarId(0)), receiverType, "recv@" + effective.id());
                PtaContextMethod rootMethod = new PtaContextMethod(effective.getMethod(), PtaContext.root());
                addTypePoint(new PtaVarNode(rootMethod, localVarId(0)), receiverType, "recv@root");
            }
            if (queuedMethods.add(effective)) {
                methodWorkList.addLast(effective);
            }
        }

        private PtaContextMethod adaptContextByMethodBudget(PtaContextMethod method) {
            if (method == null || method.getMethod() == null || !config.isAdaptiveEnabled()) {
                return method;
            }
            if (PtaContext.root().equals(method.getContext())) {
                return method;
            }
            int limit = config.getAdaptiveMaxContextsPerMethod();
            if (limit <= 0) {
                result.adaptiveContextWidened++;
                return new PtaContextMethod(method.getMethod(), PtaContext.root());
            }
            int seen = contextCountByMethod.getOrDefault(method.getMethod(), 0);
            if (seen < limit) {
                return method;
            }
            result.adaptiveContextWidened++;
            return new PtaContextMethod(method.getMethod(), PtaContext.root());
        }

        private PtaContext adaptContextForSiteBudget(PtaContext candidate,
                                                     String siteBudgetKey,
                                                     int receiverTypeCount) {
            if (candidate == null) {
                return PtaContext.root();
            }
            if (!config.isAdaptiveEnabled() || PtaContext.root().equals(candidate)) {
                return candidate;
            }
            if (receiverTypeCount >= config.getAdaptiveHugeDispatchThreshold()) {
                result.adaptiveContextWidened++;
                return PtaContext.root();
            }
            if (siteBudgetKey == null || siteBudgetKey.isEmpty()) {
                return candidate;
            }
            int limit = config.getAdaptiveMaxContextsPerSite();
            if (limit <= 0) {
                result.adaptiveContextWidened++;
                return PtaContext.root();
            }
            int seen = contextCountBySite.getOrDefault(siteBudgetKey, 0);
            if (seen >= limit) {
                result.adaptiveContextWidened++;
                return PtaContext.root();
            }
            contextCountBySite.put(siteBudgetKey, seen + 1);
            return candidate;
        }

        private String buildSiteBudgetKey(PtaContextMethod callerContext,
                                          String callSiteToken,
                                          MethodReference.Handle target) {
            String callerId = callerContext == null ? "unknown" : callerContext.id();
            String token = callSiteToken == null || callSiteToken.isBlank()
                    ? "site" : shorten(callSiteToken.trim(), 80);
            String callee = methodKey(target);
            return callerId + "|" + token + "|" + callee;
        }

        private void processContextMethod(PtaContextMethod callerCtxMethod) {
            result.contextMethodsProcessed++;
            MethodReference.Handle caller = callerCtxMethod.getMethod();

            List<PtaSyntheticCall> syntheticCalls = graph.getSyntheticCalls(caller);
            if (!syntheticCalls.isEmpty()) {
                for (PtaSyntheticCall synthetic : syntheticCalls) {
                    if (synthetic == null) {
                        continue;
                    }
                    String incrementalId = callerCtxMethod.id() + "|synthetic|"
                            + methodKey(synthetic.getTarget()) + "|" + synthetic.getReason();
                    int fingerprint = Objects.hash(synthetic.getReason(), synthetic.getType(),
                            synthetic.getConfidence(), contextThisTypes.get(callerCtxMethod));
                    if (!shouldProcess(incrementalId, fingerprint)) {
                        result.incrementalSkips++;
                        continue;
                    }
                    result.syntheticSitesProcessed++;
                    MethodReference.Handle target = canonicalHandle(synthetic.getTarget(), ctx.methodMap);
                    if (target == null) {
                        continue;
                    }
                    String reason = "ctx=" + callerCtxMethod.getContext().render() + ";" + synthetic.getReason();
                    addEdge(callerCtxMethod, caller, target,
                            synthetic.getType(),
                            synthetic.getConfidence(),
                            reason,
                            synthetic.getOpcode());
                    int ctxDepth = config.contextDepthForDispatch(target, null, 1);
                    String callSiteToken = "syn:" + target.getName();
                    PtaContext calleeCtx = ctxDepth <= 0
                            ? PtaContext.root()
                            : callerCtxMethod.getContext().extend(
                            callSiteToken,
                            ctxDepth);
                    String siteKey = buildSiteBudgetKey(callerCtxMethod, callSiteToken, target);
                    calleeCtx = adaptContextForSiteBudget(calleeCtx, siteKey, 1);
                    enqueueMethod(new PtaContextMethod(target, calleeCtx), null);
                }
            }

            List<PtaInvokeSite> invokeSites = graph.getInvokeSites(caller);
            if (invokeSites.isEmpty()) {
                return;
            }
            for (PtaInvokeSite site : invokeSites) {
                if (site == null) {
                    continue;
                }
                result.invokeSitesProcessed++;
                Set<ClassReference.Handle> receiverTypes = resolveReceiverTypes(callerCtxMethod, site);
                if (receiverTypes.isEmpty()) {
                    continue;
                }
                String incrementalId = callerCtxMethod.id() + "|site|" + site.siteToken();
                int fingerprint = Objects.hash(site, receiverTypes);
                if (!shouldProcess(incrementalId, fingerprint)) {
                    result.incrementalSkips++;
                    continue;
                }

                LinkedHashMap<MethodReference.Handle, ClassReference.Handle> targets =
                        resolveDispatchTargets(site, receiverTypes);
                if (targets.isEmpty()) {
                    continue;
                }

                String confidence = resolveConfidence(receiverTypes.size(), targets.size());
                int limit = config.getMaxTargetsPerCall();
                int count = 0;
                for (Map.Entry<MethodReference.Handle, ClassReference.Handle> targetEntry : targets.entrySet()) {
                    if (count >= limit) {
                        result.dispatchSkippedByLimit++;
                        continue;
                    }
                    count++;
                    MethodReference.Handle target = targetEntry.getKey();
                    ClassReference.Handle receiverType = targetEntry.getValue();
                    String reason = buildPtaReason(callerCtxMethod, site, receiverType, receiverTypes.size());
                    addEdge(callerCtxMethod, caller, target,
                            MethodCallMeta.TYPE_PTA,
                            confidence,
                            reason,
                            site.getOpcode());

                    int ctxDepth = config.contextDepthForDispatch(
                            target,
                            site.getDeclaredOwner(),
                            receiverTypes.size());
                    PtaContext calleeCtx = ctxDepth <= 0
                            ? PtaContext.root()
                            : callerCtxMethod.getContext().extend(
                            site.siteToken(),
                            ctxDepth);
                    String siteKey = buildSiteBudgetKey(callerCtxMethod, site.siteToken(), target);
                    calleeCtx = adaptContextForSiteBudget(calleeCtx, siteKey, receiverTypes.size());
                    enqueueMethod(new PtaContextMethod(target, calleeCtx), receiverType);
                }
            }
        }

        private boolean shouldProcess(String unitId, int fingerprint) {
            incrementalSeen.add(unitId);
            if (!config.isIncremental()) {
                return true;
            }
            return incrementalState.shouldProcess(unitId, fingerprint);
        }

        private Set<ClassReference.Handle> resolveReceiverTypes(PtaContextMethod callerCtxMethod,
                                                                PtaInvokeSite site) {
            LinkedHashSet<ClassReference.Handle> out = new LinkedHashSet<>();
            if (callerCtxMethod == null || site == null) {
                return out;
            }
            PtaVarNode recvVar = new PtaVarNode(callerCtxMethod, site.receiverVarId());
            PtaPointsToSet existing = pointsTo.get(recvVar);
            if (existing != null && !existing.isEmpty()) {
                for (PtaHeapObject obj : existing) {
                    out.add(obj.getType());
                }
            }
            if (out.isEmpty()) {
                PtaContextMethod rootMethod = new PtaContextMethod(callerCtxMethod.getMethod(), PtaContext.root());
                PtaVarNode rootRecvVar = new PtaVarNode(rootMethod, site.receiverVarId());
                PtaPointsToSet rootObjs = pointsTo.get(rootRecvVar);
                if (rootObjs != null && !rootObjs.isEmpty()) {
                    for (PtaHeapObject obj : rootObjs) {
                        out.add(obj.getType());
                    }
                }
            }

            ClassReference.Handle declaredOwner = null;
            if (site.getDeclaredOwner() != null) {
                declaredOwner = new ClassReference.Handle(site.getDeclaredOwner());
            }

            if (out.isEmpty()) {
                String receiverType = normalizeClassName(site.getReceiverType());
                if (receiverType != null) {
                    out.add(new ClassReference.Handle(receiverType));
                }
            }

            if (out.isEmpty()) {
                Set<ClassReference.Handle> thisTypes = contextThisTypes.get(callerCtxMethod);
                if (thisTypes != null && !thisTypes.isEmpty()) {
                    for (ClassReference.Handle type : thisTypes) {
                        if (isAssignable(type, declaredOwner)) {
                            out.add(type);
                        }
                    }
                }
            }

            if (out.isEmpty()) {
                out.addAll(collectFallbackReceiverTypes(declaredOwner));
            }

            if (declaredOwner != null && out.isEmpty()) {
                out.add(declaredOwner);
            }

            if (!out.isEmpty()) {
                for (ClassReference.Handle type : out) {
                    addTypePoint(recvVar, type, "recv@" + site.siteToken());
                }
            }
            return out;
        }

        private Set<ClassReference.Handle> collectFallbackReceiverTypes(ClassReference.Handle declaredOwner) {
            if (declaredOwner == null) {
                return Collections.emptySet();
            }
            String ownerName = declaredOwner.getName();
            if (ownerName == null || ownerName.isEmpty()) {
                return Collections.emptySet();
            }
            Set<ClassReference.Handle> cached = fallbackReceiverCache.get(ownerName);
            if (cached != null) {
                return cached;
            }
            if (ctx == null || ctx.instantiatedClasses == null || ctx.instantiatedClasses.isEmpty()) {
                fallbackReceiverCache.put(ownerName, Collections.emptySet());
                return Collections.emptySet();
            }
            LinkedHashSet<ClassReference.Handle> out = new LinkedHashSet<>();
            ArrayList<ClassReference.Handle> candidates = new ArrayList<>(ctx.instantiatedClasses);
            if (candidates.size() > 1) {
                candidates.sort(Comparator.comparing(ClassReference.Handle::getName,
                        Comparator.nullsLast(String::compareTo)));
            }
            for (ClassReference.Handle inst : candidates) {
                if (inst == null || inst.getName() == null) {
                    continue;
                }
                if (!isAssignable(inst, declaredOwner)) {
                    continue;
                }
                out.add(inst);
                if (out.size() >= MAX_FALLBACK_TYPES) {
                    break;
                }
            }
            Set<ClassReference.Handle> frozen = out.isEmpty()
                    ? Collections.emptySet()
                    : Collections.unmodifiableSet(out);
            fallbackReceiverCache.put(ownerName, frozen);
            return frozen;
        }

        private LinkedHashMap<MethodReference.Handle, ClassReference.Handle> resolveDispatchTargets(
                PtaInvokeSite site,
                Set<ClassReference.Handle> receiverTypes) {
            LinkedHashMap<MethodReference.Handle, ClassReference.Handle> out = new LinkedHashMap<>();
            if (site == null || receiverTypes == null || receiverTypes.isEmpty()) {
                return out;
            }
            for (ClassReference.Handle receiverType : receiverTypes) {
                if (receiverType == null || receiverType.getName() == null) {
                    continue;
                }
                MethodReference.Handle target = resolveInHierarchy(
                        receiverType,
                        site.getCalleeName(),
                        site.getCalleeDesc());
                if (target != null) {
                    out.putIfAbsent(target, receiverType);
                    continue;
                }
                Set<MethodReference.Handle> defaults = resolveInterfaceDefaults(
                        receiverType,
                        site.getCalleeName(),
                        site.getCalleeDesc());
                for (MethodReference.Handle def : defaults) {
                    out.putIfAbsent(def, receiverType);
                }
            }
            return out;
        }

        private MethodReference.Handle resolveInHierarchy(ClassReference.Handle type,
                                                          String methodName,
                                                          String methodDesc) {
            if (type == null || methodName == null || methodDesc == null
                    || ctx == null || ctx.methodMap == null) {
                return null;
            }
            DispatchLookupKey lookupKey = new DispatchLookupKey(type.getName(), methodName, methodDesc);
            if (hierarchyDispatchCache.containsKey(lookupKey)) {
                return hierarchyDispatchCache.get(lookupKey);
            }
            MethodReference.Handle resolved = null;
            ClassReference.Handle cur = new ClassReference.Handle(type.getName());
            for (int depth = 0; depth < 32 && cur != null; depth++) {
                MethodReference.Handle probe = new MethodReference.Handle(cur, methodName, methodDesc);
                if (ctx.methodMap.containsKey(probe)) {
                    resolved = canonicalHandle(probe, ctx.methodMap);
                    break;
                }
                if (ctx.classMap == null || ctx.classMap.isEmpty()) {
                    break;
                }
                ClassReference classRef = ctx.classMap.get(cur);
                if (classRef == null) {
                    break;
                }
                String superName = classRef.getSuperClass();
                if (superName == null || superName.trim().isEmpty()) {
                    break;
                }
                cur = new ClassReference.Handle(superName);
            }
            hierarchyDispatchCache.put(lookupKey, resolved);
            return resolved;
        }

        private Set<MethodReference.Handle> resolveInterfaceDefaults(ClassReference.Handle type,
                                                                     String methodName,
                                                                     String methodDesc) {
            if (type == null || methodName == null || methodDesc == null
                    || inheritance == null || ctx == null || ctx.methodMap == null) {
                return Collections.emptySet();
            }
            DispatchLookupKey lookupKey = new DispatchLookupKey(type.getName(), methodName, methodDesc);
            Set<MethodReference.Handle> cached = interfaceDefaultsCache.get(lookupKey);
            if (cached != null) {
                return cached;
            }
            LinkedHashSet<MethodReference.Handle> out = new LinkedHashSet<>();
            Set<ClassReference.Handle> parents = inheritance.getSuperClasses(type);
            if (parents == null || parents.isEmpty()) {
                interfaceDefaultsCache.put(lookupKey, Collections.emptySet());
                return Collections.emptySet();
            }
            for (ClassReference.Handle parent : parents) {
                MethodReference.Handle probe = new MethodReference.Handle(parent, methodName, methodDesc);
                if (!ctx.methodMap.containsKey(probe)) {
                    continue;
                }
                MethodReference.Handle canonical = canonicalHandle(probe, ctx.methodMap);
                if (canonical != null) {
                    out.add(canonical);
                }
            }
            Set<MethodReference.Handle> frozen = out.isEmpty()
                    ? Collections.emptySet()
                    : Collections.unmodifiableSet(out);
            interfaceDefaultsCache.put(lookupKey, frozen);
            return frozen;
        }

        private boolean isAssignable(ClassReference.Handle type, ClassReference.Handle parent) {
            if (type == null || type.getName() == null) {
                return false;
            }
            if (parent == null || parent.getName() == null) {
                return true;
            }
            String typeName = type.getName();
            String parentName = parent.getName();
            if (typeName.equals(parentName)) {
                return true;
            }
            String key = typeName + "->" + parentName;
            Boolean cached = assignableCache.get(key);
            if (cached != null) {
                return cached;
            }
            boolean result = inheritance != null
                    && inheritance.isSubclassOf(new ClassReference.Handle(typeName),
                    new ClassReference.Handle(parentName));
            assignableCache.put(key, result);
            return result;
        }

        private String resolveConfidence(int receiverTypeCount, int targetCount) {
            if (receiverTypeCount <= 1 && targetCount <= 2) {
                return MethodCallMeta.CONF_HIGH;
            }
            if (receiverTypeCount <= 8 && targetCount <= 32) {
                return MethodCallMeta.CONF_MEDIUM;
            }
            return MethodCallMeta.CONF_LOW;
        }

        private String buildPtaReason(PtaContextMethod caller,
                                      PtaInvokeSite site,
                                      ClassReference.Handle receiverType,
                                      int typeCount) {
            String ctxKey = caller == null || caller.getContext() == null
                    ? "root" : caller.getContext().render();
            String recv = receiverType == null || receiverType.getName() == null
                    ? "unknown" : receiverType.getName();
            String owner = site == null ? "unknown" : site.getDeclaredOwner();
            String token = site == null ? "site" : site.siteToken();
            return "pta_ctx=" + shorten(ctxKey, 80)
                    + ";recv=" + recv
                    + ";decl=" + owner
                    + ";var=" + (site == null ? "?" : site.receiverVarId())
                    + ";types=" + typeCount
                    + ";site=" + shorten(token, 80);
        }

        private void addEdge(PtaContextMethod callerContext,
                             MethodReference.Handle rawCaller,
                             MethodReference.Handle rawTarget,
                             String edgeType,
                             String confidence,
                             String reason,
                             int opcode) {
            MethodReference.Handle caller = canonicalHandle(rawCaller, ctx.methodMap);
            MethodReference.Handle target = canonicalHandle(rawTarget, ctx.methodMap);
            if (caller == null || target == null) {
                return;
            }
            MethodReference.Handle callTarget = new MethodReference.Handle(
                    target.getClassReference(),
                    opcode <= 0 ? normalizeOpcode(target.getOpcode()) : opcode,
                    target.getName(),
                    target.getDesc()
            );
            HashSet<MethodReference.Handle> callees =
                    ctx.methodCalls.computeIfAbsent(caller, k -> new HashSet<>());
            boolean added = MethodCallUtils.addCallee(callees, callTarget);
            String finalEdgeType = edgeType == null ? MethodCallMeta.TYPE_PTA : edgeType;
            String finalConfidence = confidence == null ? MethodCallMeta.CONF_LOW : confidence;
            MethodCallMeta.record(
                    ctx.methodCallMeta,
                    MethodCallKey.of(caller, callTarget),
                    finalEdgeType,
                    finalConfidence,
                    reason,
                    callTarget.getOpcode()
            );
            if (added) {
                result.edgesAdded++;
            }
            plugin.onNewCallEdge(callerContext, caller, callTarget,
                    finalEdgeType, finalConfidence, callTarget.getOpcode());
        }

        private void addSemanticEdge(PtaContextMethod callerContext,
                                     MethodReference.Handle target,
                                     String edgeType,
                                     String confidence,
                                     String reason,
                                     int opcode,
                                     ClassReference.Handle receiverType,
                                     String callSiteToken) {
            if (callerContext == null || callerContext.getMethod() == null || target == null) {
                return;
            }
            addEdge(callerContext, callerContext.getMethod(), target,
                    edgeType, confidence, reason, opcode);
            int ctxDepth = config.contextDepthForDispatch(target, null, 1);
            PtaContext calleeCtx = ctxDepth <= 0
                    ? PtaContext.root()
                    : callerContext.getContext().extend(callSiteToken, ctxDepth);
            String siteKey = buildSiteBudgetKey(callerContext, callSiteToken, target);
            calleeCtx = adaptContextForSiteBudget(calleeCtx, siteKey, 1);
            enqueueMethod(new PtaContextMethod(target, calleeCtx), receiverType);
        }

        private final class SolverPluginBridge implements PtaPluginBridge {
            @Override
            public BuildContext getBuildContext() {
                return ctx;
            }

            @Override
            public PtaSolverConfig getConfig() {
                return config;
            }

            @Override
            public void addSemanticEdge(PtaContextMethod callerContext,
                                        MethodReference.Handle target,
                                        String edgeType,
                                        String confidence,
                                        String reason,
                                        int opcode,
                                        ClassReference.Handle receiverType,
                                        String callSiteToken) {
                String token = callSiteToken == null || callSiteToken.isBlank()
                        ? "sem:" + (target == null ? "callback" : target.getName())
                        : callSiteToken;
                Solver.this.addSemanticEdge(callerContext, target,
                        edgeType, confidence, reason, opcode, receiverType, token);
            }
        }

        private void addTypePoint(PtaVarNode varNode, ClassReference.Handle type, String allocSiteTag) {
            if (varNode == null || type == null || type.getName() == null) {
                return;
            }
            PtaHeapObject obj = new PtaHeapObject(
                    new ClassReference.Handle(type.getName(), type.getJarId()),
                    allocSiteTag == null ? "type" : allocSiteTag,
                    allocationContext(varNode));
            addPoint(varNode, obj);
        }

        private void addPoint(PtaVarNode varNode, PtaHeapObject obj) {
            if (varNode == null || obj == null || obj.getType() == null || obj.getType().getName() == null) {
                return;
            }
            PtaPointsToSet set = pointsTo.computeIfAbsent(varNode, k -> newPointsToSet());
            if (set.add(obj)) {
                MethodReference.Handle ownerMethod = varNode.getOwner() == null
                        ? null : varNode.getOwner().getMethod();
                plugin.onNewPointsToObject(varNode, ownerMethod);
                varWorkList.addLast(varNode);
            }
        }

        private PtaPointsToSet newPointsToSet() {
            return new PtaPointsToSet(objectIndexer);
        }

        private PtaPointsToSet filterByFieldType(PtaPointsToSet values, String fieldSig) {
            if (values == null || values.isEmpty()) {
                return newPointsToSet();
            }
            String fieldDesc = fieldDescFromSig(fieldSig);
            if (fieldDesc == null || fieldDesc.isEmpty()) {
                return values;
            }
            return filterByTypeDescriptor(values, fieldDesc);
        }

        private PtaPointsToSet filterByArrayElementType(PtaPointsToSet values, PtaHeapObject arrayObj) {
            if (values == null || values.isEmpty() || arrayObj == null
                    || arrayObj.getType() == null || arrayObj.getType().getName() == null) {
                return newPointsToSet();
            }
            String arrayDesc = toDescriptor(arrayObj.getType().getName());
            if (arrayDesc == null || arrayDesc.isEmpty() || arrayDesc.charAt(0) != '[') {
                return values;
            }
            try {
                Type arrayType = Type.getType(arrayDesc);
                Type elemType = arrayType.getElementType();
                if (elemType == null) {
                    return values;
                }
                int sort = elemType.getSort();
                if (sort != Type.OBJECT && sort != Type.ARRAY) {
                    return newPointsToSet();
                }
                return filterByTypeDescriptor(values, elemType.getDescriptor());
            } catch (Exception ex) {
                return values;
            }
        }

        private PtaPointsToSet filterByTypeDescriptor(PtaPointsToSet values, String targetDesc) {
            if (values == null || values.isEmpty()) {
                return newPointsToSet();
            }
            if (targetDesc == null || targetDesc.isEmpty()) {
                return values;
            }
            if (targetDesc.charAt(0) != 'L' && targetDesc.charAt(0) != '[') {
                return newPointsToSet();
            }
            PtaPointsToSet out = newPointsToSet();
            for (PtaHeapObject obj : values) {
                if (obj == null) {
                    continue;
                }
                if (isCompatibleWithDescriptor(obj, targetDesc)) {
                    out.add(obj);
                }
            }
            return out;
        }

        private boolean isCompatibleWithDescriptor(PtaHeapObject obj, String targetDesc) {
            if (obj == null || obj.getType() == null || obj.getType().getName() == null
                    || targetDesc == null || targetDesc.isEmpty()) {
                return false;
            }
            String sourceType = obj.getType().getName();
            DescriptorCompatibilityKey key = new DescriptorCompatibilityKey(sourceType, targetDesc);
            Boolean cached = descriptorCompatCache.get(key);
            if (cached != null) {
                return cached;
            }
            String sourceDesc = toDescriptor(sourceType);
            if (sourceDesc == null || sourceDesc.isEmpty()) {
                return false;
            }
            final Type src = parseAsmType(sourceDesc);
            final Type target = parseAsmType(targetDesc);
            if (src == null || target == null) {
                descriptorCompatCache.put(key, false);
                return false;
            }
            boolean compatible;
            if (target.getSort() == Type.OBJECT) {
                String targetType = target.getInternalName();
                if (targetType == null || targetType.isEmpty()) {
                    descriptorCompatCache.put(key, false);
                    return false;
                }
                if (src.getSort() == Type.OBJECT) {
                    String srcType = src.getInternalName();
                    if (srcType == null || srcType.isEmpty()) {
                        descriptorCompatCache.put(key, false);
                        return false;
                    }
                    compatible = isAssignable(new ClassReference.Handle(srcType), new ClassReference.Handle(targetType));
                    descriptorCompatCache.put(key, compatible);
                    return compatible;
                }
                if (src.getSort() == Type.ARRAY) {
                    compatible = "java/lang/Object".equals(targetType)
                            || "java/lang/Cloneable".equals(targetType)
                            || "java/io/Serializable".equals(targetType);
                    descriptorCompatCache.put(key, compatible);
                    return compatible;
                }
                descriptorCompatCache.put(key, false);
                return false;
            }
            if (target.getSort() == Type.ARRAY) {
                compatible = isArrayAssignable(src, target);
                descriptorCompatCache.put(key, compatible);
                return compatible;
            }
            descriptorCompatCache.put(key, false);
            return false;
        }

        private boolean isArrayAssignable(Type src, Type target) {
            if (src == null || target == null || src.getSort() != Type.ARRAY || target.getSort() != Type.ARRAY) {
                return false;
            }
            String srcDesc = src.getDescriptor();
            String targetDesc = target.getDescriptor();
            if (Objects.equals(srcDesc, targetDesc)) {
                return true;
            }
            int srcDim = src.getDimensions();
            int targetDim = target.getDimensions();
            if (srcDim != targetDim) {
                return false;
            }
            Type srcElem = src.getElementType();
            Type targetElem = target.getElementType();
            if (srcElem == null || targetElem == null) {
                return false;
            }
            if (srcElem.getSort() == Type.OBJECT && targetElem.getSort() == Type.OBJECT) {
                String srcType = srcElem.getInternalName();
                String targetType = targetElem.getInternalName();
                if (srcType == null || targetType == null) {
                    return false;
                }
                return isAssignable(new ClassReference.Handle(srcType), new ClassReference.Handle(targetType));
            }
            if (srcElem.getSort() == Type.ARRAY && targetElem.getSort() == Type.ARRAY) {
                return isArrayAssignable(srcElem, targetElem);
            }
            return srcElem.getSort() == targetElem.getSort();
        }

        private String fieldDescFromSig(String fieldSig) {
            if (fieldSig == null || fieldSig.isEmpty()) {
                return null;
            }
            int idx = fieldSig.lastIndexOf('#');
            if (idx < 0 || idx >= fieldSig.length() - 1) {
                return null;
            }
            return fieldSig.substring(idx + 1);
        }

        private String toDescriptor(String typeName) {
            if (typeName == null || typeName.isEmpty()) {
                return null;
            }
            if (typeName.charAt(0) == '[') {
                return typeName;
            }
            return "L" + typeName + ";";
        }

        private Type parseAsmType(String desc) {
            if (desc == null || desc.isEmpty()) {
                return null;
            }
            if (invalidTypeDescriptors.contains(desc)) {
                return null;
            }
            Type cached = asmTypeCache.get(desc);
            if (cached != null) {
                return cached;
            }
            try {
                Type parsed = Type.getType(desc);
                asmTypeCache.put(desc, parsed);
                return parsed;
            } catch (Exception ex) {
                invalidTypeDescriptors.add(desc);
                return null;
            }
        }

        private static final class DispatchLookupKey {
            private final String owner;
            private final String name;
            private final String desc;

            private DispatchLookupKey(String owner, String name, String desc) {
                this.owner = owner == null ? "" : owner;
                this.name = name == null ? "" : name;
                this.desc = desc == null ? "" : desc;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                DispatchLookupKey that = (DispatchLookupKey) o;
                return Objects.equals(owner, that.owner)
                        && Objects.equals(name, that.name)
                        && Objects.equals(desc, that.desc);
            }

            @Override
            public int hashCode() {
                return Objects.hash(owner, name, desc);
            }
        }

        private static final class DescriptorCompatibilityKey {
            private final String sourceType;
            private final String targetDesc;

            private DescriptorCompatibilityKey(String sourceType, String targetDesc) {
                this.sourceType = sourceType == null ? "" : sourceType;
                this.targetDesc = targetDesc == null ? "" : targetDesc;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                DescriptorCompatibilityKey that = (DescriptorCompatibilityKey) o;
                return Objects.equals(sourceType, that.sourceType)
                        && Objects.equals(targetDesc, that.targetDesc);
            }

            @Override
            public int hashCode() {
                return Objects.hash(sourceType, targetDesc);
            }
        }

        private static final class HeapObjectIndexer {
            private final Map<PtaHeapObject, Integer> indexByObject = new HashMap<>();
            private final List<PtaHeapObject> objectByIndex = new ArrayList<>(256);

            private int indexOf(PtaHeapObject obj) {
                Integer idx = indexByObject.get(obj);
                if (idx != null) {
                    return idx;
                }
                int next = objectByIndex.size();
                indexByObject.put(obj, next);
                objectByIndex.add(obj);
                return next;
            }

            private PtaHeapObject objectAt(int index) {
                if (index < 0 || index >= objectByIndex.size()) {
                    return null;
                }
                return objectByIndex.get(index);
            }
        }

        private static final class PtaPointsToSet implements Iterable<PtaHeapObject> {
            private static final int SMALL_LIMIT = 8;
            private final HeapObjectIndexer indexer;
            private final int[] small = new int[SMALL_LIMIT];
            private int smallSize;
            private BitSet dense;
            private int size;

            private PtaPointsToSet(HeapObjectIndexer indexer) {
                this.indexer = indexer;
            }

            private boolean add(PtaHeapObject obj) {
                if (obj == null) {
                    return false;
                }
                return addIndex(indexer.indexOf(obj));
            }

            private boolean addAll(PtaPointsToSet other) {
                if (other == null || other.isEmpty()) {
                    return false;
                }
                boolean changed = false;
                if (other.dense != null) {
                    for (int bit = other.dense.nextSetBit(0); bit >= 0; bit = other.dense.nextSetBit(bit + 1)) {
                        changed |= addIndex(bit);
                    }
                } else {
                    for (int i = 0; i < other.smallSize; i++) {
                        changed |= addIndex(other.small[i]);
                    }
                }
                return changed;
            }

            private boolean isEmpty() {
                return size == 0;
            }

            private boolean addIndex(int idx) {
                if (idx < 0) {
                    return false;
                }
                if (dense == null) {
                    for (int i = 0; i < smallSize; i++) {
                        if (small[i] == idx) {
                            return false;
                        }
                    }
                    if (smallSize < SMALL_LIMIT) {
                        small[smallSize++] = idx;
                        size++;
                        return true;
                    }
                    dense = new BitSet();
                    for (int i = 0; i < smallSize; i++) {
                        dense.set(small[i]);
                    }
                }
                if (dense.get(idx)) {
                    return false;
                }
                dense.set(idx);
                size++;
                return true;
            }

            @Override
            public Iterator<PtaHeapObject> iterator() {
                if (dense == null) {
                    return new Iterator<>() {
                        private int pos;

                        @Override
                        public boolean hasNext() {
                            return pos < smallSize;
                        }

                        @Override
                        public PtaHeapObject next() {
                            if (!hasNext()) {
                                throw new NoSuchElementException();
                            }
                            return indexer.objectAt(small[pos++]);
                        }
                    };
                }
                return new Iterator<>() {
                    private int bit = dense.nextSetBit(0);

                    @Override
                    public boolean hasNext() {
                        return bit >= 0;
                    }

                    @Override
                    public PtaHeapObject next() {
                        if (bit < 0) {
                            throw new NoSuchElementException();
                        }
                        int cur = bit;
                        bit = dense.nextSetBit(bit + 1);
                        return indexer.objectAt(cur);
                    }
                };
            }
        }
    }

    private static final class PtaHeapObject {
        private final ClassReference.Handle type;
        private final String allocSite;
        private final String contextKey;

        private PtaHeapObject(ClassReference.Handle type, String allocSite, String contextKey) {
            this.type = type == null || type.getName() == null
                    ? null : new ClassReference.Handle(type.getName(), type.getJarId());
            this.allocSite = allocSite == null ? "" : allocSite;
            this.contextKey = contextKey == null ? "root" : contextKey;
        }

        private ClassReference.Handle getType() {
            return type;
        }

        private String allocSiteKey() {
            if (allocSite.isEmpty()) {
                return "unknown";
            }
            return allocSite;
        }

        private String identityKey() {
            return allocSiteKey() + "@" + contextKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PtaHeapObject that = (PtaHeapObject) o;
            return Objects.equals(type, that.type)
                    && Objects.equals(allocSite, that.allocSite)
                    && Objects.equals(contextKey, that.contextKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, allocSite, contextKey);
        }
    }

    private static String methodKey(MethodReference.Handle method) {
        if (method == null || method.getClassReference() == null) {
            return "";
        }
        return method.getClassReference().getName() + "."
                + method.getName() + method.getDesc() + "@"
                + normalizeJarId(method.getJarId());
    }

    public static final class Result {
        private int edgesAdded;
        private int contextMethodsProcessed;
        private int contextMethodCount;
        private int invokeSitesProcessed;
        private int syntheticSitesProcessed;
        private int dispatchSkippedByLimit;
        private int incrementalSkips;
        private int adaptiveContextWidened;
        private int adaptiveObjectWidened;
        private long durationMs;

        private static Result empty() {
            return new Result();
        }

        public int getEdgesAdded() {
            return edgesAdded;
        }

        public int getContextMethodsProcessed() {
            return contextMethodsProcessed;
        }

        public int getContextMethodCount() {
            return contextMethodCount;
        }

        public int getInvokeSitesProcessed() {
            return invokeSitesProcessed;
        }

        public int getSyntheticSitesProcessed() {
            return syntheticSitesProcessed;
        }

        public int getDispatchSkippedByLimit() {
            return dispatchSkippedByLimit;
        }

        public int getIncrementalSkips() {
            return incrementalSkips;
        }

        public int getAdaptiveContextWidened() {
            return adaptiveContextWidened;
        }

        public int getAdaptiveObjectWidened() {
            return adaptiveObjectWidened;
        }

        public long getDurationMs() {
            return durationMs;
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT,
                    "edges=%d, ctxProcessed=%d, ctxTotal=%d, invokeSites=%d, synthetic=%d, "
                            + "limitSkips=%d, incrementalSkips=%d, ctxWiden=%d, objWiden=%d, durationMs=%d",
                    edgesAdded,
                    contextMethodsProcessed,
                    contextMethodCount,
                    invokeSitesProcessed,
                    syntheticSitesProcessed,
                    dispatchSkippedByLimit,
                    incrementalSkips,
                    adaptiveContextWidened,
                    adaptiveObjectWidened,
                    durationMs);
        }
    }
}
