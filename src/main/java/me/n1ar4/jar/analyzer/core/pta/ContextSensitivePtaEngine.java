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
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
                    callIndex++;
                    continue;
                }
                if (!(insn instanceof VarInsnNode)) {
                    continue;
                }
                VarInsnNode store = (VarInsnNode) insn;
                if (store.getOpcode() != Opcodes.ASTORE) {
                    continue;
                }
                collectAssignAndAlloc(caller, store, frame, out, i, currentLine);
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
            if (arg != null && arg.getSort() == Type.OBJECT) {
                out.addAlloc(caller,
                        localVarId(localIdx),
                        new ClassReference.Handle(arg.getInternalName()),
                        "arg@" + methodKey(caller) + "#" + localIdx);
            }
            localIdx += arg == null ? 1 : arg.getSize();
        }
    }

    private static void collectAssignAndAlloc(MethodReference.Handle caller,
                                              VarInsnNode store,
                                              Frame<SourceValue> frame,
                                              PtaConstraintIndex out,
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
                    ClassReference.Handle fieldType = normalizeRefType(Type.getType(fi.desc));
                    if (fieldType != null) {
                        out.addAlloc(caller, toVar, fieldType, "field@" + allocTag);
                    }
                }
            }
        }
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
        SourceValue value = frame.getStack(recvIndex);
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
        if (metaMap == null || metaMap.isEmpty() || caller == null || callee == null) {
            return null;
        }
        MethodCallKey scoped = MethodCallKey.of(caller, callee);
        if (scoped != null) {
            MethodCallMeta scopedMeta = metaMap.get(scoped);
            if (scopedMeta != null) {
                return scopedMeta;
            }
        }
        MethodCallKey loose = new MethodCallKey(
                caller.getClassReference().getName(),
                caller.getName(),
                caller.getDesc(),
                callee.getClassReference().getName(),
                callee.getName(),
                callee.getDesc()
        );
        return metaMap.get(loose);
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

        private String receiverVarId(String callSiteKey) {
            if (callSiteKey == null || callSiteKey.trim().isEmpty()) {
                return null;
            }
            return receiverVarByCallSite.get(callSiteKey);
        }

        private void bindReceiverVar(String callSiteKey, String receiverVarId) {
            if (callSiteKey == null || callSiteKey.trim().isEmpty()
                    || receiverVarId == null || receiverVarId.trim().isEmpty()) {
                return;
            }
            receiverVarByCallSite.putIfAbsent(callSiteKey, receiverVarId.trim());
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

    private static final class Solver {
        private final BuildContext ctx;
        private final PointerAssignmentGraph graph;
        private final PtaSolverConfig config;
        private final InheritanceMap inheritance;

        private final Map<PtaVarNode, Set<ClassReference.Handle>> pointsTo = new HashMap<>();
        private final ArrayDeque<PtaVarNode> varWorkList = new ArrayDeque<>();
        private final ArrayDeque<PtaContextMethod> methodWorkList = new ArrayDeque<>();
        private final Set<PtaContextMethod> queuedMethods = new HashSet<>();
        private final Set<PtaContextMethod> knownMethods = new HashSet<>();
        private final Map<PtaContextMethod, Set<ClassReference.Handle>> contextThisTypes = new HashMap<>();

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
        }

        private Result solve() {
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
            return result;
        }

        private void seedAllocEdges() {
            for (Map.Entry<PtaAllocNode, Set<PtaVarNode>> entry : graph.getAllocEdges().entrySet()) {
                if (entry.getKey() == null || entry.getKey().getType() == null
                        || entry.getValue() == null || entry.getValue().isEmpty()) {
                    continue;
                }
                for (PtaVarNode var : entry.getValue()) {
                    addPoint(var, entry.getKey().getType());
                }
            }
        }

        private void flushVarWorkList() {
            while (!varWorkList.isEmpty()) {
                PtaVarNode from = varWorkList.pollFirst();
                if (from == null) {
                    continue;
                }
                Set<ClassReference.Handle> fromTypes = pointsTo.get(from);
                if (fromTypes == null || fromTypes.isEmpty()) {
                    continue;
                }
                Set<PtaVarNode> assignTargets = graph.getAssignTargets(from);
                if (assignTargets.isEmpty()) {
                    continue;
                }
                for (PtaVarNode to : assignTargets) {
                    if (to == null) {
                        continue;
                    }
                    for (ClassReference.Handle t : fromTypes) {
                        addPoint(to, t);
                    }
                }
            }
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
            if (!knownMethods.contains(method) && knownMethods.size() >= config.getMaxContextMethods()) {
                return;
            }
            knownMethods.add(method);
            if (receiverType != null) {
                contextThisTypes.computeIfAbsent(method, k -> new LinkedHashSet<>()).add(receiverType);
                addPoint(new PtaVarNode(method, localVarId(0)), receiverType);
                PtaContextMethod rootMethod = new PtaContextMethod(method.getMethod(), PtaContext.root());
                addPoint(new PtaVarNode(rootMethod, localVarId(0)), receiverType);
            }
            if (queuedMethods.add(method)) {
                methodWorkList.addLast(method);
            }
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
                    addEdge(caller, target,
                            synthetic.getType(),
                            synthetic.getConfidence(),
                            reason,
                            synthetic.getOpcode());
                    PtaContext calleeCtx = callerCtxMethod.getContext().extend(
                            "syn:" + target.getName(),
                            config.getContextDepth());
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
                    addEdge(caller, target,
                            MethodCallMeta.TYPE_PTA,
                            confidence,
                            reason,
                            site.getOpcode());

                    PtaContext calleeCtx = callerCtxMethod.getContext().extend(
                            site.siteToken(),
                            config.getContextDepth());
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
            Set<ClassReference.Handle> existing = pointsTo.get(recvVar);
            if (existing != null && !existing.isEmpty()) {
                out.addAll(existing);
            }
            if (out.isEmpty()) {
                PtaContextMethod rootMethod = new PtaContextMethod(callerCtxMethod.getMethod(), PtaContext.root());
                PtaVarNode rootRecvVar = new PtaVarNode(rootMethod, site.receiverVarId());
                Set<ClassReference.Handle> rootTypes = pointsTo.get(rootRecvVar);
                if (rootTypes != null && !rootTypes.isEmpty()) {
                    out.addAll(rootTypes);
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
                    addPoint(recvVar, type);
                }
            }
            return out;
        }

        private Set<ClassReference.Handle> collectFallbackReceiverTypes(ClassReference.Handle declaredOwner) {
            LinkedHashSet<ClassReference.Handle> out = new LinkedHashSet<>();
            if (declaredOwner == null) {
                return out;
            }
            if (ctx == null || ctx.instantiatedClasses == null || ctx.instantiatedClasses.isEmpty()) {
                return out;
            }
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
            return out;
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
            ClassReference.Handle cur = new ClassReference.Handle(type.getName());
            for (int depth = 0; depth < 32 && cur != null; depth++) {
                MethodReference.Handle probe = new MethodReference.Handle(cur, methodName, methodDesc);
                if (ctx.methodMap.containsKey(probe)) {
                    return canonicalHandle(probe, ctx.methodMap);
                }
                if (ctx.classMap == null || ctx.classMap.isEmpty()) {
                    return null;
                }
                ClassReference classRef = ctx.classMap.get(cur);
                if (classRef == null) {
                    return null;
                }
                String superName = classRef.getSuperClass();
                if (superName == null || superName.trim().isEmpty()) {
                    return null;
                }
                cur = new ClassReference.Handle(superName);
            }
            return null;
        }

        private Set<MethodReference.Handle> resolveInterfaceDefaults(ClassReference.Handle type,
                                                                     String methodName,
                                                                     String methodDesc) {
            LinkedHashSet<MethodReference.Handle> out = new LinkedHashSet<>();
            if (type == null || methodName == null || methodDesc == null
                    || inheritance == null || ctx == null || ctx.methodMap == null) {
                return out;
            }
            Set<ClassReference.Handle> parents = inheritance.getSuperClasses(type);
            if (parents == null || parents.isEmpty()) {
                return out;
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
            return out;
        }

        private boolean isAssignable(ClassReference.Handle type, ClassReference.Handle parent) {
            if (type == null || type.getName() == null) {
                return false;
            }
            if (parent == null || parent.getName() == null) {
                return true;
            }
            if (type.getName().equals(parent.getName())) {
                return true;
            }
            if (inheritance == null) {
                return false;
            }
            return inheritance.isSubclassOf(new ClassReference.Handle(type.getName()),
                    new ClassReference.Handle(parent.getName()));
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
                    + ";types=" + typeCount
                    + ";site=" + shorten(token, 80);
        }

        private void addEdge(MethodReference.Handle rawCaller,
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
            MethodCallMeta.record(
                    ctx.methodCallMeta,
                    MethodCallKey.of(caller, callTarget),
                    edgeType == null ? MethodCallMeta.TYPE_PTA : edgeType,
                    confidence == null ? MethodCallMeta.CONF_LOW : confidence,
                    reason,
                    callTarget.getOpcode()
            );
            if (added) {
                result.edgesAdded++;
            }
        }

        private void addPoint(PtaVarNode varNode, ClassReference.Handle type) {
            if (varNode == null || type == null || type.getName() == null) {
                return;
            }
            Set<ClassReference.Handle> set = pointsTo.computeIfAbsent(varNode, k -> new LinkedHashSet<>());
            if (set.add(new ClassReference.Handle(type.getName(), type.getJarId()))) {
                varWorkList.addLast(varNode);
            }
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

        public long getDurationMs() {
            return durationMs;
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT,
                    "edges=%d, ctxProcessed=%d, ctxTotal=%d, invokeSites=%d, synthetic=%d, "
                            + "limitSkips=%d, incrementalSkips=%d, durationMs=%d",
                    edgesAdded,
                    contextMethodsProcessed,
                    contextMethodCount,
                    invokeSitesProcessed,
                    syntheticSitesProcessed,
                    dispatchSkippedByLimit,
                    incrementalSkips,
                    durationMs);
        }
    }
}
