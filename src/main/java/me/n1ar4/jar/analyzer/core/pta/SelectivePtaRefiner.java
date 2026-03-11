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

import me.n1ar4.jar.analyzer.core.CallSiteKeyUtil;
import me.n1ar4.jar.analyzer.core.InheritanceMap;
import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.MethodCallUtils;
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
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class SelectivePtaRefiner {
    private static final Logger logger = LogManager.getLogger();
    private static final PtaBudget DEFAULT_BUDGET = new PtaBudget(8, 64, 64, 64, 8, 32, 8);
    private static final int MAX_SOURCES_PER_VALUE = 8;
    private static final String ARRAY_COPY_OWNER = "java/lang/System";
    private static final String ARRAY_COPY_NAME = "arraycopy";
    private static final String ARRAY_COPY_DESC = "(Ljava/lang/Object;ILjava/lang/Object;II)V";

    private SelectivePtaRefiner() {
    }

    public static Result refine(BuildContext context,
                                InheritanceMap inheritanceMap) {
        if (context == null
                || context.callSites == null
                || context.callSites.isEmpty()
                || context.methodMap == null
                || context.methodMap.isEmpty()
                || inheritanceMap == null) {
            return Result.empty();
        }
        MethodLookup lookup = MethodLookup.build(context.methodMap);
        Map<MethodReference.Handle, MethodUnit> methodUnits = loadMethodUnits(context, lookup);
        if (methodUnits.isEmpty()) {
            return Result.empty();
        }
        PtaBudget budget = DEFAULT_BUDGET;
        int hotspotSites = 0;
        int refinedSites = 0;
        int ptaEdges = 0;
        int fieldSites = 0;
        int arraySites = 0;
        int arrayCopySites = 0;
        for (CallSiteEntity site : context.callSites) {
            if (!isHotspot(site, context.classMap, inheritanceMap)) {
                continue;
            }
            hotspotSites++;
            MethodReference.Handle caller = lookup.resolve(
                    site.getCallerClassName(),
                    site.getCallerMethodName(),
                    site.getCallerMethodDesc(),
                    site.getJarId()
            );
            if (caller == null) {
                continue;
            }
            MethodUnit unit = methodUnits.get(caller);
            if (unit == null) {
                continue;
            }
            PtaInvokeSite invokeSite = unit.findCallSite(site);
            if (invokeSite == null || invokeSite.insn() == null || invokeSite.insn().getOpcode() <= 0) {
                continue;
            }
            Frame<SourceValue> frame = unit.frameAt(invokeSite.insnIndex());
            if (frame == null) {
                continue;
            }
            int argCount = Type.getArgumentTypes(invokeSite.desc()).length;
            int receiverIndex = frame.getStackSize() - argCount - 1;
            if (receiverIndex < 0) {
                continue;
            }
            ResolvedValue receiver = resolveValue(frame.getStack(receiverIndex),
                    unit,
                    invokeSite.insnIndex(),
                    inheritanceMap,
                    lookup,
                    budget,
                    new HashSet<>());
            if (receiver.isEmpty() || receiver.types().isEmpty()) {
                continue;
            }
            LinkedHashSet<MethodReference.Handle> targets = resolveTargets(
                    receiver.types(),
                    invokeSite.name(),
                    invokeSite.desc(),
                    context.methodMap,
                    inheritanceMap,
                    budget.maxTargetsPerCall()
            );
            if (targets.isEmpty()) {
                continue;
            }
            refinedSites++;
            if (receiver.evidence().contains("field")) {
                fieldSites++;
            }
            if (receiver.evidence().contains("array")) {
                arraySites++;
            }
            if (receiver.evidence().contains("arraycopy")) {
                arrayCopySites++;
            }
            HashSet<MethodReference.Handle> callees = context.methodCalls.computeIfAbsent(caller, ignore -> new HashSet<>());
            String reason = buildReason(receiver.evidence(), invokeSite.context());
            String confidence = targets.size() == 1 ? MethodCallMeta.CONF_HIGH : MethodCallMeta.CONF_MEDIUM;
            for (MethodReference.Handle target : targets) {
                MethodReference.Handle callTarget = new MethodReference.Handle(
                        target.getClassReference(),
                        invokeSite.opcode(),
                        target.getName(),
                        target.getDesc()
                );
                MethodCallMeta beforeMeta = MethodCallMeta.resolve(context.methodCallMeta, caller, callTarget);
                int beforeBits = beforeMeta == null ? 0 : beforeMeta.getEvidenceBits();
                boolean inserted = MethodCallUtils.addCallee(callees, callTarget);
                MethodCallMeta.record(
                        context.methodCallMeta,
                        MethodCallKey.of(caller, callTarget),
                        MethodCallMeta.TYPE_PTA,
                        confidence,
                        reason,
                        invokeSite.opcode()
                );
                MethodCallMeta afterMeta = MethodCallMeta.resolve(context.methodCallMeta, caller, callTarget);
                int afterBits = afterMeta == null ? 0 : afterMeta.getEvidenceBits();
                boolean gainedPtaEvidence = (beforeBits & MethodCallMeta.EVIDENCE_PTA) == 0
                        && (afterBits & MethodCallMeta.EVIDENCE_PTA) != 0;
                if (inserted || gainedPtaEvidence) {
                    ptaEdges++;
                }
            }
        }
        return new Result(
                ptaEdges,
                refinedSites,
                hotspotSites,
                fieldSites,
                arraySites,
                arrayCopySites,
                methodUnits.size()
        );
    }

    private static boolean isHotspot(CallSiteEntity site,
                                     Map<ClassReference.Handle, ClassReference> classMap,
                                     InheritanceMap inheritanceMap) {
        if (site == null || site.getOpCode() == null) {
            return false;
        }
        int opcode = site.getOpCode();
        if (opcode != Opcodes.INVOKEVIRTUAL && opcode != Opcodes.INVOKEINTERFACE) {
            return false;
        }
        if (site.getCallIndex() == null || site.getCallIndex() < 0) {
            return false;
        }
        String receiverType = safe(site.getReceiverType());
        if (receiverType.isBlank()) {
            return true;
        }
        String declaredOwner = normalizeClassName(site.getCalleeOwner());
        String normalizedReceiver = normalizeClassName(receiverType);
        if (!normalizedReceiver.isEmpty() && normalizedReceiver.equals(declaredOwner)) {
            return true;
        }
        if (classMap == null || classMap.isEmpty() || inheritanceMap == null) {
            return false;
        }
        ClassReference.Handle receiverHandle = inheritanceMap.resolveClass(normalizedReceiver, site.getJarId());
        if (receiverHandle == null) {
            return false;
        }
        ClassReference receiver = classMap.get(receiverHandle);
        if (receiver == null) {
            return false;
        }
        if (receiver.isInterface()) {
            return true;
        }
        int access = receiver.getAccess() == null ? 0 : receiver.getAccess();
        return (access & Opcodes.ACC_ABSTRACT) != 0;
    }

    private static String buildReason(Set<String> evidence,
                                      PtaContext context) {
        StringBuilder sb = new StringBuilder();
        if (evidence != null && !evidence.isEmpty()) {
            int i = 0;
            for (String item : evidence) {
                if (item == null || item.isBlank()) {
                    continue;
                }
                if (i++ > 0) {
                    sb.append("+");
                }
                sb.append(item.trim());
            }
        }
        if (sb.length() == 0) {
            sb.append("pta");
        }
        if (context != null) {
            sb.append("+pta_ctx=").append(context.render());
        }
        return sb.toString();
    }

    private static LinkedHashSet<MethodReference.Handle> resolveTargets(Set<ClassReference.Handle> receiverTypes,
                                                                        String methodName,
                                                                        String methodDesc,
                                                                        Map<MethodReference.Handle, MethodReference> methodMap,
                                                                        InheritanceMap inheritanceMap,
                                                                        int maxTargetsPerCall) {
        if (receiverTypes == null || receiverTypes.isEmpty()
                || methodName == null || methodName.isBlank()
                || methodDesc == null || methodDesc.isBlank()
                || methodMap == null || methodMap.isEmpty()
                || inheritanceMap == null) {
            return new LinkedHashSet<>();
        }
        LinkedHashSet<MethodReference.Handle> out = new LinkedHashSet<>();
        for (ClassReference.Handle receiverType : receiverTypes) {
            MethodReference.Handle target = resolveInHierarchy(receiverType, methodName, methodDesc, methodMap, inheritanceMap);
            if (target == null) {
                continue;
            }
            out.add(target);
            if (out.size() > maxTargetsPerCall) {
                return new LinkedHashSet<>();
            }
        }
        return out;
    }

    private static MethodReference.Handle resolveInHierarchy(ClassReference.Handle start,
                                                             String methodName,
                                                             String methodDesc,
                                                             Map<MethodReference.Handle, MethodReference> methodMap,
                                                             InheritanceMap inheritanceMap) {
        if (start == null || methodMap == null || methodMap.isEmpty() || inheritanceMap == null) {
            return null;
        }
        ArrayDeque<ClassReference.Handle> queue = new ArrayDeque<>();
        HashSet<ClassReference.Handle> visited = new HashSet<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            ClassReference.Handle current = queue.removeFirst();
            if (current == null || !visited.add(current)) {
                continue;
            }
            MethodReference.Handle exact = new MethodReference.Handle(current, methodName, methodDesc);
            MethodReference method = methodMap.get(exact);
            if (method != null) {
                return method.getHandle();
            }
            for (ClassReference.Handle parent : inheritanceMap.getDirectParents(current)) {
                if (parent != null) {
                    queue.addLast(parent);
                }
            }
        }
        return null;
    }

    private static Map<MethodReference.Handle, MethodUnit> loadMethodUnits(BuildContext context,
                                                                           MethodLookup lookup) {
        if (context == null || context.classFileList == null || context.classFileList.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<MethodReference.Handle, MethodUnit> out = new LinkedHashMap<>();
        for (ClassFileEntity file : context.classFileList) {
            if (file == null || file.getFile() == null || file.getFile().length == 0) {
                continue;
            }
            try {
                ClassNode cn = new ClassNode();
                new ClassReader(file.getFile()).accept(cn, Const.GlobalASMOptions);
                if (cn.methods == null || cn.methods.isEmpty()) {
                    continue;
                }
                for (MethodNode mn : cn.methods) {
                    if (mn == null || mn.instructions == null || mn.instructions.size() == 0) {
                        continue;
                    }
                    MethodReference.Handle handle = lookup.resolve(cn.name, mn.name, mn.desc, file.getJarId());
                    if (handle == null) {
                        continue;
                    }
                    Frame<SourceValue>[] frames = analyzeFrames(cn.name, mn);
                    if (frames == null) {
                        continue;
                    }
                    MethodUnit unit = new MethodUnit(handle,
                            cn,
                            mn,
                            frames,
                            buildInvokeSites(handle, mn.instructions));
                    out.put(handle, unit);
                }
            } catch (Exception ex) {
                logger.debug("pta unit load failed: {}", ex.toString());
            }
        }
        if (!out.isEmpty()) {
            Map<String, List<MethodUnit>> byClass = new HashMap<>();
            for (MethodUnit unit : out.values()) {
                if (unit == null || unit.handle() == null || unit.handle().getClassReference() == null) {
                    continue;
                }
                byClass.computeIfAbsent(
                        safe(unit.handle().getClassReference().getName()),
                        ignore -> new ArrayList<>()
                ).add(unit);
            }
            for (List<MethodUnit> group : byClass.values()) {
                if (group == null || group.isEmpty()) {
                    continue;
                }
                for (MethodUnit unit : group) {
                    if (unit == null) {
                        continue;
                    }
                    for (MethodUnit sibling : group) {
                        if (sibling != null && sibling.handle() != null) {
                            unit.siblings().put(sibling.handle(), sibling);
                        }
                    }
                }
            }
        }
        return out.isEmpty() ? Map.of() : Map.copyOf(out);
    }

    private static Frame<SourceValue>[] analyzeFrames(String owner, MethodNode method) {
        try {
            Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());
            return analyzer.analyze(owner, method);
        } catch (Exception ex) {
            logger.debug("pta frame analyze failed: {}", ex.toString());
            return null;
        }
    }

    private static List<PtaInvokeSite> buildInvokeSites(MethodReference.Handle caller,
                                                        InsnList instructions) {
        if (caller == null || instructions == null || instructions.size() == 0) {
            return List.of();
        }
        ArrayList<PtaInvokeSite> out = new ArrayList<>();
        int callIndex = 0;
        int currentLine = -1;
        for (int i = 0; i < instructions.size(); i++) {
            AbstractInsnNode insn = instructions.get(i);
            if (insn instanceof org.objectweb.asm.tree.LineNumberNode ln) {
                currentLine = ln.line;
                continue;
            }
            if (!(insn instanceof MethodInsnNode mi)) {
                continue;
            }
            CallSiteEntity site = new CallSiteEntity();
            site.setCallerClassName(caller.getClassReference() == null ? "" : caller.getClassReference().getName());
            site.setCallerMethodName(caller.getName());
            site.setCallerMethodDesc(caller.getDesc());
            site.setCalleeOwner(mi.owner);
            site.setCalleeMethodName(mi.name);
            site.setCalleeMethodDesc(mi.desc);
            site.setOpCode(mi.getOpcode());
            site.setLineNumber(currentLine);
            site.setCallIndex(callIndex);
            site.setJarId(caller.getJarId());
            String key = CallSiteKeyUtil.buildCallSiteKey(site, i);
            out.add(new PtaInvokeSite(
                    key,
                    new PtaContextMethod(caller, PtaContext.root()),
                    mi.owner,
                    mi.name,
                    mi.desc,
                    mi.getOpcode(),
                    callIndex,
                    currentLine,
                    i,
                    mi
            ));
            callIndex++;
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static ResolvedValue resolveValue(SourceValue value,
                                              MethodUnit unit,
                                              int limitIndex,
                                              InheritanceMap inheritanceMap,
                                              MethodLookup lookup,
                                              PtaBudget budget,
                                              Set<String> path) {
        if (value == null || value.insns == null || value.insns.isEmpty()) {
            return ResolvedValue.empty();
        }
        ResolvedValue out = ResolvedValue.empty();
        int count = 0;
        for (AbstractInsnNode src : value.insns) {
            if (src == null) {
                continue;
            }
            if (count++ >= MAX_SOURCES_PER_VALUE) {
                break;
            }
            out = out.merge(resolveSource(src, unit, limitIndex, inheritanceMap, lookup, budget, path));
            if (out.types().size() > budget.maxTargetsPerCall()) {
                return ResolvedValue.empty();
            }
        }
        return out;
    }

    private static ResolvedValue resolveSource(AbstractInsnNode src,
                                               MethodUnit unit,
                                               int limitIndex,
                                               InheritanceMap inheritanceMap,
                                               MethodLookup lookup,
                                               PtaBudget budget,
                                               Set<String> path) {
        if (src == null || unit == null) {
            return ResolvedValue.empty();
        }
        int idx = unit.indexOf(src);
        if (idx < 0 || idx > limitIndex) {
            return ResolvedValue.empty();
        }
        String pathKey = unit.id() + "|" + idx + "|" + src.getOpcode() + "|" + limitIndex;
        if (!path.add(pathKey)) {
            return ResolvedValue.empty();
        }
        try {
            if (path.size() > budget.maxResolutionDepth()) {
                return ResolvedValue.empty();
            }
            if (src instanceof TypeInsnNode tin) {
                if (tin.getOpcode() == Opcodes.NEW) {
                    ClassReference.Handle type = resolveClassHandle(tin.desc, unit.handle().getJarId(), inheritanceMap);
                    if (type == null) {
                        return ResolvedValue.empty();
                    }
                    return ResolvedValue.of(type, new OriginKey(unit.id(), idx, "new"));
                }
                if (tin.getOpcode() == Opcodes.ANEWARRAY) {
                    return ResolvedValue.ofOrigin(new OriginKey(unit.id(), idx, "array"));
                }
                if (tin.getOpcode() == Opcodes.CHECKCAST) {
                    Frame<SourceValue> frame = unit.frameAt(idx);
                    if (frame == null || frame.getStackSize() <= 0) {
                        return ResolvedValue.empty();
                    }
                    return resolveValue(frame.getStack(frame.getStackSize() - 1),
                            unit,
                            idx,
                            inheritanceMap,
                            lookup,
                            budget,
                            path);
                }
            }
            if (src instanceof MultiANewArrayInsnNode) {
                return ResolvedValue.ofOrigin(new OriginKey(unit.id(), idx, "array"));
            }
            if (src instanceof VarInsnNode varInsn) {
                if (varInsn.getOpcode() == Opcodes.ALOAD) {
                    return resolveLocalLoad(varInsn, unit, idx, inheritanceMap, lookup, budget, path);
                }
                return ResolvedValue.empty();
            }
            if (src instanceof FieldInsnNode fin) {
                if (fin.getOpcode() == Opcodes.GETFIELD) {
                    return resolveFieldLoad(fin, unit, idx, inheritanceMap, lookup, budget, path);
                }
                return ResolvedValue.empty();
            }
            if (src instanceof InsnNode insn) {
                if (insn.getOpcode() == Opcodes.AALOAD) {
                    return resolveArrayLoad(unit, idx, inheritanceMap, lookup, budget, path);
                }
                if (insn.getOpcode() == Opcodes.DUP
                        || insn.getOpcode() == Opcodes.DUP_X1
                        || insn.getOpcode() == Opcodes.DUP_X2
                        || insn.getOpcode() == Opcodes.DUP2
                        || insn.getOpcode() == Opcodes.DUP2_X1
                        || insn.getOpcode() == Opcodes.DUP2_X2) {
                    return resolveDuplicatedValue(unit, idx, inheritanceMap, lookup, budget, path);
                }
            }
            if (src instanceof MethodInsnNode mi) {
                if (mi.getOpcode() == Opcodes.INVOKESPECIAL && "<init>".equals(mi.name)) {
                    ClassReference.Handle type = resolveClassHandle(mi.owner, unit.handle().getJarId(), inheritanceMap);
                    if (type == null) {
                        return ResolvedValue.empty();
                    }
                    return ResolvedValue.of(type, new OriginKey(unit.id(), idx, "ctor")).addEvidence("ctor");
                }
                return resolveMethodReturn(mi, unit, idx, inheritanceMap, lookup, budget, path);
            }
            return ResolvedValue.empty();
        } finally {
            path.remove(pathKey);
        }
    }

    private static ResolvedValue resolveLocalLoad(VarInsnNode loadInsn,
                                                  MethodUnit unit,
                                                  int limitIndex,
                                                  InheritanceMap inheritanceMap,
                                                  MethodLookup lookup,
                                                  PtaBudget budget,
                                                  Set<String> path) {
        int storeIndex = findPreviousStore(unit, loadInsn.var, limitIndex, Opcodes.ASTORE);
        if (storeIndex < 0) {
            return ResolvedValue.empty();
        }
        Frame<SourceValue> storeFrame = unit.frameAt(storeIndex);
        if (storeFrame == null || storeFrame.getStackSize() <= 0) {
            return ResolvedValue.empty();
        }
        ResolvedValue local = resolveValue(storeFrame.getStack(storeFrame.getStackSize() - 1),
                unit,
                storeIndex,
                inheritanceMap,
                lookup,
                budget,
                path);
        return local.addEvidence("local");
    }

    private static ResolvedValue resolveFieldLoad(FieldInsnNode fieldInsn,
                                                  MethodUnit unit,
                                                  int limitIndex,
                                                  InheritanceMap inheritanceMap,
                                                  MethodLookup lookup,
                                                  PtaBudget budget,
                                                  Set<String> path) {
        Frame<SourceValue> frame = unit.frameAt(limitIndex);
        if (frame == null || frame.getStackSize() <= 0) {
            return ResolvedValue.empty();
        }
        ResolvedValue base = resolveValue(frame.getStack(frame.getStackSize() - 1),
                unit,
                limitIndex,
                inheritanceMap,
                lookup,
                budget,
                path);
        if (base.origins().isEmpty()) {
            return ResolvedValue.empty();
        }
        ResolvedValue out = ResolvedValue.empty();
        int storeHits = 0;
        for (int i = limitIndex - 1; i >= 0 && storeHits < budget.maxFieldStoreScan(); i--) {
            AbstractInsnNode insn = unit.instructions().get(i);
            if (!(insn instanceof FieldInsnNode putField)
                    || putField.getOpcode() != Opcodes.PUTFIELD
                    || !sameField(fieldInsn, putField)) {
                continue;
            }
            Frame<SourceValue> storeFrame = unit.frameAt(i);
            if (storeFrame == null || storeFrame.getStackSize() < 2) {
                continue;
            }
            int stackSize = storeFrame.getStackSize();
            ResolvedValue storeBase = resolveValue(storeFrame.getStack(stackSize - 2),
                    unit,
                    i,
                    inheritanceMap,
                    lookup,
                    budget,
                    path);
            if (!intersects(base.origins(), storeBase.origins())) {
                continue;
            }
            ResolvedValue stored = resolveValue(storeFrame.getStack(stackSize - 1),
                    unit,
                    i,
                    inheritanceMap,
                    lookup,
                    budget,
                    path);
            if (stored.isEmpty()) {
                continue;
            }
            out = out.merge(stored.addEvidence("field"));
            storeHits++;
        }
        return out;
    }

    private static ResolvedValue resolveArrayLoad(MethodUnit unit,
                                                  int limitIndex,
                                                  InheritanceMap inheritanceMap,
                                                  MethodLookup lookup,
                                                  PtaBudget budget,
                                                  Set<String> path) {
        Frame<SourceValue> frame = unit.frameAt(limitIndex);
        if (frame == null || frame.getStackSize() < 2) {
            return ResolvedValue.empty();
        }
        int stackSize = frame.getStackSize();
        ResolvedValue array = resolveValue(frame.getStack(stackSize - 2),
                unit,
                limitIndex,
                inheritanceMap,
                lookup,
                budget,
                path);
        if (array.origins().isEmpty()) {
            return ResolvedValue.empty();
        }
        Integer index = resolveIntValue(frame.getStack(stackSize - 1), unit, limitIndex, budget, path);
        return resolveArrayElement(array.origins(), index, unit, limitIndex, inheritanceMap, lookup, budget, path)
                .addEvidence("array");
    }

    private static ResolvedValue resolveDuplicatedValue(MethodUnit unit,
                                                        int limitIndex,
                                                        InheritanceMap inheritanceMap,
                                                        MethodLookup lookup,
                                                        PtaBudget budget,
                                                        Set<String> path) {
        Frame<SourceValue> frame = unit.frameAt(limitIndex);
        if (frame == null || frame.getStackSize() <= 0) {
            return ResolvedValue.empty();
        }
        return resolveValue(frame.getStack(frame.getStackSize() - 1),
                unit,
                limitIndex,
                inheritanceMap,
                lookup,
                budget,
                path).addEvidence("dup");
    }

    private static ResolvedValue resolveArrayElement(Set<OriginKey> arrayOrigins,
                                                     Integer targetIndex,
                                                     MethodUnit unit,
                                                     int limitIndex,
                                                     InheritanceMap inheritanceMap,
                                                     MethodLookup lookup,
                                                     PtaBudget budget,
                                                     Set<String> path) {
        if (arrayOrigins == null || arrayOrigins.isEmpty()) {
            return ResolvedValue.empty();
        }
        ResolvedValue out = ResolvedValue.empty();
        int storeHits = 0;
        for (int i = limitIndex - 1; i >= 0 && storeHits < budget.maxArrayStoreScan(); i--) {
            AbstractInsnNode insn = unit.instructions().get(i);
            if (insn instanceof InsnNode raw && raw.getOpcode() == Opcodes.AASTORE) {
                Frame<SourceValue> frame = unit.frameAt(i);
                if (frame == null || frame.getStackSize() < 3) {
                    continue;
                }
                int stackSize = frame.getStackSize();
                ResolvedValue storeArray = resolveValue(frame.getStack(stackSize - 3),
                        unit,
                        i,
                        inheritanceMap,
                        lookup,
                        budget,
                        path);
                if (!intersects(arrayOrigins, storeArray.origins())) {
                    continue;
                }
                Integer storeIndex = resolveIntValue(frame.getStack(stackSize - 2), unit, i, budget, path);
                if (!indexMatches(targetIndex, storeIndex)) {
                    continue;
                }
                ResolvedValue stored = resolveValue(frame.getStack(stackSize - 1),
                        unit,
                        i,
                        inheritanceMap,
                        lookup,
                        budget,
                        path);
                if (stored.isEmpty()) {
                    continue;
                }
                out = out.merge(stored.addEvidence("array"));
                storeHits++;
                continue;
            }
            if (!(insn instanceof MethodInsnNode mi)
                    || !ARRAY_COPY_OWNER.equals(mi.owner)
                    || !ARRAY_COPY_NAME.equals(mi.name)
                    || !ARRAY_COPY_DESC.equals(mi.desc)) {
                continue;
            }
            Frame<SourceValue> frame = unit.frameAt(i);
            if (frame == null || frame.getStackSize() < 5) {
                continue;
            }
            int stackSize = frame.getStackSize();
            ResolvedValue dstArray = resolveValue(frame.getStack(stackSize - 3),
                    unit,
                    i,
                    inheritanceMap,
                    lookup,
                    budget,
                    path);
            if (!intersects(arrayOrigins, dstArray.origins())) {
                continue;
            }
            Integer srcPos = resolveIntValue(frame.getStack(stackSize - 4), unit, i, budget, path);
            Integer dstPos = resolveIntValue(frame.getStack(stackSize - 2), unit, i, budget, path);
            Integer length = resolveIntValue(frame.getStack(stackSize - 1), unit, i, budget, path);
            Integer srcIndex = translateArrayCopyIndex(targetIndex, srcPos, dstPos, length);
            if (targetIndex != null && srcIndex == null) {
                continue;
            }
            ResolvedValue srcArray = resolveValue(frame.getStack(stackSize - 5),
                    unit,
                    i,
                    inheritanceMap,
                    lookup,
                    budget,
                    path);
            if (srcArray.origins().isEmpty()) {
                continue;
            }
            ResolvedValue copied = resolveArrayElement(srcArray.origins(),
                    srcIndex,
                    unit,
                    i,
                    inheritanceMap,
                    lookup,
                    budget,
                    path);
            if (copied.isEmpty()) {
                continue;
            }
            out = out.merge(copied.addEvidence("arraycopy"));
            storeHits++;
        }
        return out;
    }

    private static ResolvedValue resolveMethodReturn(MethodInsnNode invoke,
                                                     MethodUnit callerUnit,
                                                     int limitIndex,
                                                     InheritanceMap inheritanceMap,
                                                     MethodLookup lookup,
                                                     PtaBudget budget,
                                                     Set<String> path) {
        if (callerUnit == null || invoke == null) {
            return ResolvedValue.empty();
        }
        Type returnType = Type.getReturnType(invoke.desc);
        if (returnType.getSort() != Type.OBJECT && returnType.getSort() != Type.ARRAY) {
            return ResolvedValue.empty();
        }
        MethodReference.Handle callee = lookup.resolve(invoke.owner, invoke.name, invoke.desc, callerUnit.handle().getJarId());
        if (callee == null || !callerUnit.isSameClass(callee)) {
            return ResolvedValue.empty();
        }
        MethodUnit calleeUnit = callerUnit.siblings().get(callee);
        if (calleeUnit == null) {
            return ResolvedValue.empty();
        }
        ResolvedValue out = ResolvedValue.empty();
        for (int i = 0; i < calleeUnit.instructions().size(); i++) {
            AbstractInsnNode insn = calleeUnit.instructions().get(i);
            if (insn == null || insn.getOpcode() != Opcodes.ARETURN) {
                continue;
            }
            Frame<SourceValue> frame = calleeUnit.frameAt(i);
            if (frame == null || frame.getStackSize() <= 0) {
                continue;
            }
            ResolvedValue returned = resolveValue(frame.getStack(frame.getStackSize() - 1),
                    calleeUnit,
                    i,
                    inheritanceMap,
                    lookup,
                    budget,
                    path);
            if (!returned.isEmpty()) {
                out = out.merge(returned.addEvidence("return"));
            }
        }
        return out;
    }

    private static Integer resolveIntValue(SourceValue value,
                                           MethodUnit unit,
                                           int limitIndex,
                                           PtaBudget budget,
                                           Set<String> path) {
        if (value == null || value.insns == null || value.insns.isEmpty() || unit == null) {
            return null;
        }
        Integer resolved = null;
        int count = 0;
        for (AbstractInsnNode src : value.insns) {
            if (src == null) {
                continue;
            }
            if (count++ >= MAX_SOURCES_PER_VALUE) {
                break;
            }
            Integer candidate = resolveIntSource(src, unit, limitIndex, budget, path);
            if (candidate == null) {
                return null;
            }
            if (resolved == null) {
                resolved = candidate;
            } else if (!Objects.equals(resolved, candidate)) {
                return null;
            }
        }
        return resolved;
    }

    private static Integer resolveIntSource(AbstractInsnNode src,
                                            MethodUnit unit,
                                            int limitIndex,
                                            PtaBudget budget,
                                            Set<String> path) {
        if (src == null || unit == null) {
            return null;
        }
        int idx = unit.indexOf(src);
        if (idx < 0 || idx > limitIndex) {
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
        if (src instanceof VarInsnNode varInsn) {
            int opcode = varInsn.getOpcode();
            if (opcode >= Opcodes.ILOAD && opcode <= Opcodes.ALOAD) {
                int storeIndex = findPreviousNumericStore(unit, varInsn.var, limitIndex);
                if (storeIndex < 0) {
                    return null;
                }
                Frame<SourceValue> frame = unit.frameAt(storeIndex);
                if (frame == null || frame.getStackSize() <= 0) {
                    return null;
                }
                return resolveIntValue(frame.getStack(frame.getStackSize() - 1), unit, storeIndex, budget, path);
            }
        }
        return null;
    }

    private static Integer translateArrayCopyIndex(Integer targetIndex,
                                                   Integer srcPos,
                                                   Integer dstPos,
                                                   Integer length) {
        if (targetIndex == null) {
            return null;
        }
        if (srcPos == null || dstPos == null || length == null || length <= 0) {
            return null;
        }
        int delta = targetIndex - dstPos;
        if (delta < 0 || delta >= length) {
            return null;
        }
        return srcPos + delta;
    }

    private static boolean indexMatches(Integer targetIndex,
                                        Integer storeIndex) {
        if (targetIndex == null || storeIndex == null) {
            return true;
        }
        return Objects.equals(targetIndex, storeIndex);
    }

    private static int findPreviousStore(MethodUnit unit,
                                         int var,
                                         int limitIndex,
                                         int expectedOpcode) {
        if (unit == null || var < 0 || limitIndex <= 0) {
            return -1;
        }
        int floor = Math.max(0, limitIndex - 256);
        for (int i = limitIndex - 1; i >= floor; i--) {
            AbstractInsnNode insn = unit.instructions().get(i);
            if (!(insn instanceof VarInsnNode vin)) {
                continue;
            }
            if (vin.var == var && vin.getOpcode() == expectedOpcode) {
                return i;
            }
        }
        return -1;
    }

    private static int findPreviousNumericStore(MethodUnit unit,
                                                int var,
                                                int limitIndex) {
        if (unit == null || var < 0 || limitIndex <= 0) {
            return -1;
        }
        int floor = Math.max(0, limitIndex - 256);
        for (int i = limitIndex - 1; i >= floor; i--) {
            AbstractInsnNode insn = unit.instructions().get(i);
            if (!(insn instanceof VarInsnNode vin)) {
                continue;
            }
            int opcode = vin.getOpcode();
            if (vin.var == var
                    && (opcode == Opcodes.ISTORE
                    || opcode == Opcodes.LSTORE
                    || opcode == Opcodes.FSTORE
                    || opcode == Opcodes.DSTORE)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean sameField(FieldInsnNode left,
                                     FieldInsnNode right) {
        if (left == null || right == null) {
            return false;
        }
        return safe(left.owner).equals(safe(right.owner))
                && safe(left.name).equals(safe(right.name))
                && safe(left.desc).equals(safe(right.desc));
    }

    private static boolean intersects(Set<OriginKey> left,
                                      Set<OriginKey> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return false;
        }
        for (OriginKey item : left) {
            if (right.contains(item)) {
                return true;
            }
        }
        return false;
    }

    private static ClassReference.Handle resolveClassHandle(String className,
                                                            Integer jarId,
                                                            InheritanceMap inheritanceMap) {
        if (className == null || className.isBlank() || inheritanceMap == null) {
            return null;
        }
        ClassReference.Handle resolved = inheritanceMap.resolveClass(className, jarId);
        if (resolved != null) {
            return resolved;
        }
        return new ClassReference.Handle(normalizeClassName(className), normalizeJarId(jarId));
    }

    private static String normalizeClassName(String className) {
        if (className == null) {
            return "";
        }
        String normalized = className.trim();
        if (normalized.isEmpty()) {
            return "";
        }
        if (normalized.startsWith("L") && normalized.endsWith(";")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (normalized.indexOf('.') >= 0) {
            normalized = normalized.replace('.', '/');
        }
        return normalized;
    }

    private static int normalizeJarId(Integer jarId) {
        return jarId == null || jarId < 0 ? -1 : jarId;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public record Result(int ptaEdges,
                         int refinedCallSites,
                         int hotspotCallSites,
                         int fieldSites,
                         int arraySites,
                         int arrayCopySites,
                         int scannedMethods) {
        public static Result empty() {
            return new Result(0, 0, 0, 0, 0, 0, 0);
        }
    }

    private record PtaBudget(int maxResolutionDepth,
                             int maxFieldStoreScan,
                             int maxArrayStoreScan,
                             int maxTargetsPerCall,
                             int contextDepth,
                             int maxContextsPerMethod,
                             int maxContextsPerSite) {
    }

    private record OriginKey(String methodId, int insnIndex, String kind) {
    }

    private record ResolvedValue(Set<ClassReference.Handle> types,
                                 Set<OriginKey> origins,
                                 Set<String> evidence) {
        private static ResolvedValue empty() {
            return new ResolvedValue(Set.of(), Set.of(), Set.of());
        }

        private static ResolvedValue of(ClassReference.Handle type, OriginKey origin) {
            LinkedHashSet<ClassReference.Handle> types = new LinkedHashSet<>();
            if (type != null) {
                types.add(type);
            }
            LinkedHashSet<OriginKey> origins = new LinkedHashSet<>();
            if (origin != null) {
                origins.add(origin);
            }
            return new ResolvedValue(Set.copyOf(types), Set.copyOf(origins), Set.of("alloc"));
        }

        private static ResolvedValue ofOrigin(OriginKey origin) {
            LinkedHashSet<OriginKey> origins = new LinkedHashSet<>();
            if (origin != null) {
                origins.add(origin);
            }
            return new ResolvedValue(Set.of(), Set.copyOf(origins), Set.of());
        }

        private boolean isEmpty() {
            return types.isEmpty() && origins.isEmpty();
        }

        private ResolvedValue addEvidence(String evidenceItem) {
            if (evidenceItem == null || evidenceItem.isBlank()) {
                return this;
            }
            LinkedHashSet<String> out = new LinkedHashSet<>(evidence);
            out.add(evidenceItem);
            return new ResolvedValue(types, origins, Set.copyOf(out));
        }

        private ResolvedValue merge(ResolvedValue other) {
            if (other == null || other.isEmpty()) {
                return this;
            }
            if (isEmpty()) {
                return other;
            }
            LinkedHashSet<ClassReference.Handle> mergedTypes = new LinkedHashSet<>(types);
            mergedTypes.addAll(other.types);
            LinkedHashSet<OriginKey> mergedOrigins = new LinkedHashSet<>(origins);
            mergedOrigins.addAll(other.origins);
            LinkedHashSet<String> mergedEvidence = new LinkedHashSet<>(evidence);
            mergedEvidence.addAll(other.evidence);
            return new ResolvedValue(Set.copyOf(mergedTypes), Set.copyOf(mergedOrigins), Set.copyOf(mergedEvidence));
        }
    }

    private record PtaContext(List<String> callChain) {
        private static final PtaContext ROOT = new PtaContext(List.of());

        private static PtaContext root() {
            return ROOT;
        }

        private String render() {
            if (callChain == null || callChain.isEmpty()) {
                return "root";
            }
            return String.join("->", callChain);
        }
    }

    private record PtaContextMethod(MethodReference.Handle method, PtaContext context) {
        private String id() {
            String owner = method == null || method.getClassReference() == null ? "" : method.getClassReference().getName();
            String name = method == null ? "" : method.getName();
            String desc = method == null ? "" : method.getDesc();
            String ctx = context == null ? "root" : context.render();
            return owner + "#" + name + desc + "|" + ctx;
        }
    }

    private record PtaInvokeSite(String callSiteKey,
                                 PtaContextMethod caller,
                                 String owner,
                                 String name,
                                 String desc,
                                 int opcode,
                                 int callIndex,
                                 int lineNumber,
                                 int insnIndex,
                                 MethodInsnNode insn) {
        private PtaContext context() {
            return caller == null ? PtaContext.root() : caller.context();
        }
    }

    private static final class MethodUnit {
        private final MethodReference.Handle handle;
        private final ClassNode classNode;
        private final MethodNode methodNode;
        private final Frame<SourceValue>[] frames;
        private final List<PtaInvokeSite> invokeSites;
        private final Map<MethodReference.Handle, MethodUnit> siblings;

        private MethodUnit(MethodReference.Handle handle,
                           ClassNode classNode,
                           MethodNode methodNode,
                           Frame<SourceValue>[] frames,
                           List<PtaInvokeSite> invokeSites) {
            this.handle = handle;
            this.classNode = classNode;
            this.methodNode = methodNode;
            this.frames = frames;
            this.invokeSites = invokeSites == null ? List.of() : invokeSites;
            this.siblings = new HashMap<>();
        }

        private MethodReference.Handle handle() {
            return handle;
        }

        private String id() {
            String owner = handle == null || handle.getClassReference() == null ? "" : handle.getClassReference().getName();
            String name = handle == null ? "" : handle.getName();
            String desc = handle == null ? "" : handle.getDesc();
            return owner + "#" + name + desc;
        }

        private InsnList instructions() {
            return methodNode.instructions;
        }

        private int indexOf(AbstractInsnNode insn) {
            return methodNode.instructions.indexOf(insn);
        }

        private Frame<SourceValue> frameAt(int index) {
            if (frames == null || index < 0 || index >= frames.length) {
                return null;
            }
            return frames[index];
        }

        private PtaInvokeSite findCallSite(CallSiteEntity site) {
            if (site == null || invokeSites.isEmpty()) {
                return null;
            }
            for (PtaInvokeSite invokeSite : invokeSites) {
                if (invokeSite == null) {
                    continue;
                }
                if (invokeSite.callIndex() != safeInt(site.getCallIndex())) {
                    continue;
                }
                if (!safe(invokeSite.owner()).equals(safe(site.getCalleeOwner()))) {
                    continue;
                }
                if (!safe(invokeSite.name()).equals(safe(site.getCalleeMethodName()))) {
                    continue;
                }
                if (!safe(invokeSite.desc()).equals(safe(site.getCalleeMethodDesc()))) {
                    continue;
                }
                return invokeSite;
            }
            return null;
        }

        private boolean isSameClass(MethodReference.Handle other) {
            if (handle == null || handle.getClassReference() == null || other == null || other.getClassReference() == null) {
                return false;
            }
            return safe(handle.getClassReference().getName()).equals(safe(other.getClassReference().getName()));
        }

        private Map<MethodReference.Handle, MethodUnit> siblings() {
            return siblings;
        }
    }

    private static int safeInt(Integer value) {
        return value == null ? -1 : value;
    }

    private static final class MethodLookup {
        private final Map<MethodReference.Handle, MethodReference.Handle> exact;
        private final Map<String, MethodReference.Handle> uniqueBySignature;
        private final Set<String> ambiguousSignatures;

        private MethodLookup(Map<MethodReference.Handle, MethodReference.Handle> exact,
                             Map<String, MethodReference.Handle> uniqueBySignature,
                             Set<String> ambiguousSignatures) {
            this.exact = exact == null ? Map.of() : exact;
            this.uniqueBySignature = uniqueBySignature == null ? Map.of() : uniqueBySignature;
            this.ambiguousSignatures = ambiguousSignatures == null ? Set.of() : ambiguousSignatures;
        }

        private static MethodLookup build(Map<MethodReference.Handle, MethodReference> methodMap) {
            if (methodMap == null || methodMap.isEmpty()) {
                return new MethodLookup(Map.of(), Map.of(), Set.of());
            }
            LinkedHashMap<MethodReference.Handle, MethodReference.Handle> exact = new LinkedHashMap<>();
            LinkedHashMap<String, MethodReference.Handle> unique = new LinkedHashMap<>();
            LinkedHashSet<String> ambiguous = new LinkedHashSet<>();
            List<MethodReference> methods = new ArrayList<>(methodMap.values());
            methods.sort(Comparator
                    .comparing((MethodReference method) -> safe(method == null || method.getClassReference() == null ? null : method.getClassReference().getName()))
                    .thenComparing(method -> safe(method == null ? null : method.getName()))
                    .thenComparing(method -> safe(method == null ? null : method.getDesc()))
                    .thenComparingInt(method -> normalizeJarId(method == null ? null : method.getJarId())));
            for (MethodReference method : methods) {
                if (method == null || method.getClassReference() == null) {
                    continue;
                }
                MethodReference.Handle handle = method.getHandle();
                exact.put(handle, handle);
                String signature = methodSignature(method.getClassReference().getName(), method.getName(), method.getDesc());
                if (signature.isBlank() || ambiguous.contains(signature)) {
                    continue;
                }
                MethodReference.Handle existing = unique.putIfAbsent(signature, handle);
                if (existing != null && !existing.equals(handle)) {
                    unique.remove(signature);
                    ambiguous.add(signature);
                }
            }
            return new MethodLookup(Map.copyOf(exact), Map.copyOf(unique), Set.copyOf(ambiguous));
        }

        private MethodReference.Handle resolve(String className,
                                               String methodName,
                                               String desc,
                                               Integer preferredJarId) {
            String owner = normalizeClassName(className);
            String name = safe(methodName);
            String methodDesc = safe(desc);
            if (owner.isBlank() || name.isBlank() || methodDesc.isBlank()) {
                return null;
            }
            int jarId = normalizeJarId(preferredJarId);
            if (jarId >= 0) {
                MethodReference.Handle preferred = new MethodReference.Handle(
                        new ClassReference.Handle(owner, jarId),
                        name,
                        methodDesc
                );
                MethodReference.Handle exactPreferred = exact.get(preferred);
                if (exactPreferred != null) {
                    return exactPreferred;
                }
            }
            MethodReference.Handle loose = exact.get(new MethodReference.Handle(
                    new ClassReference.Handle(owner, -1),
                    name,
                    methodDesc
            ));
            if (loose != null) {
                return loose;
            }
            String signature = methodSignature(owner, name, methodDesc);
            if (ambiguousSignatures.contains(signature)) {
                return null;
            }
            return uniqueBySignature.get(signature);
        }

        private static String methodSignature(String owner,
                                              String methodName,
                                              String desc) {
            String normalizedOwner = normalizeClassName(owner);
            String normalizedMethod = safe(methodName);
            String normalizedDesc = safe(desc);
            if (normalizedOwner.isBlank() || normalizedMethod.isBlank() || normalizedDesc.isBlank()) {
                return "";
            }
            return normalizedOwner + "#" + normalizedMethod + normalizedDesc;
        }
    }
}
