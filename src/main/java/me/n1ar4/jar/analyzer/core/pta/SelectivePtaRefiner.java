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
import me.n1ar4.jar.analyzer.core.bytecode.BuildBytecodeWorkspace;
import me.n1ar4.jar.analyzer.core.edge.BuildEdgeAccumulator;
import me.n1ar4.jar.analyzer.core.facts.BuildFactSnapshot;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.rules.MethodSemanticFlags;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
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
import org.objectweb.asm.tree.analysis.Frame;
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
    private static final PtaBudget PRECISION_BUDGET = new PtaBudget(12, 128, 128, 128, 12, 64, 16);
    private static final int MAX_SOURCES_PER_VALUE = 8;
    private static final String JAVA_LANG_OBJECT = "java/lang/Object";
    private static final int PRECISION_SEMANTIC_MASK = MethodSemanticFlags.WEB_ENTRY
            | MethodSemanticFlags.RPC_ENTRY
            | MethodSemanticFlags.SPRING_ENDPOINT
            | MethodSemanticFlags.JSP_ENDPOINT
            | MethodSemanticFlags.STRUTS_ACTION
            | MethodSemanticFlags.SERVLET_CALLBACK
            | MethodSemanticFlags.NETTY_HANDLER
            | MethodSemanticFlags.MYBATIS_DYNAMIC_SQL
            | MethodSemanticFlags.SERIALIZABLE_OWNER
            | MethodSemanticFlags.DESERIALIZATION_CALLBACK
            | MethodSemanticFlags.INVOCATION_HANDLER
            | MethodSemanticFlags.COMPARATOR_CALLBACK
            | MethodSemanticFlags.TRANSFORMER_CALLBACK
            | MethodSemanticFlags.COLLECTION_CONTAINER
            | MethodSemanticFlags.COMPARABLE_CALLBACK
            | MethodSemanticFlags.TOSTRING_TRIGGER
            | MethodSemanticFlags.HASHCODE_TRIGGER
            | MethodSemanticFlags.EQUALS_TRIGGER;
    private SelectivePtaRefiner() {
    }

    public static Result refine(BuildFactSnapshot snapshot,
                                BuildEdgeAccumulator edges,
                                InheritanceMap inheritanceMap) {
        BuildBytecodeWorkspace workspace = snapshot == null
                ? BuildBytecodeWorkspace.empty()
                : snapshot.bytecode().workspace();
        return refine(snapshot, edges, workspace, inheritanceMap, false);
    }

    public static Result refine(BuildFactSnapshot snapshot,
                                BuildEdgeAccumulator edges,
                                BuildBytecodeWorkspace workspace,
                                InheritanceMap inheritanceMap) {
        return refine(snapshot, edges, workspace, inheritanceMap, false);
    }

    public static Result refine(BuildFactSnapshot snapshot,
                                BuildEdgeAccumulator edges,
                                BuildBytecodeWorkspace workspace,
                                InheritanceMap inheritanceMap,
                                boolean precisionMode) {
        if (snapshot == null
                || edges == null
                || snapshot.symbols().callSites().isEmpty()
                || snapshot.methods().methodsByHandle().isEmpty()
                || inheritanceMap == null) {
            return Result.empty();
        }
        MethodLookup lookup = MethodLookup.build(snapshot.methods().methodsByHandle());
        Map<MethodReference.Handle, MethodUnit> methodUnits = loadMethodUnits(snapshot, workspace, lookup);
        if (methodUnits.isEmpty()) {
            return Result.empty();
        }
        int hotspotSites = 0;
        int refinedSites = 0;
        int ptaEdges = 0;
        int fieldSites = 0;
        int arraySites = 0;
        int arrayCopySites = 0;
        int precisionSelectedSites = 0;
        int precisionSemanticSites = 0;
        int precisionReflectionSites = 0;
        int precisionTriggerSites = 0;
        int precisionHighFanoutSites = 0;
        for (CallSiteEntity site : snapshot.symbols().callSites()) {
            PrecisionSelection selection = selectHotspot(
                    site,
                    snapshot,
                    edges,
                    inheritanceMap,
                    precisionMode,
                    lookup,
                    methodUnits
            );
            if (!selection.enabled()) {
                continue;
            }
            hotspotSites++;
            if (selection.highPrecision()) {
                precisionSelectedSites++;
                if (selection.semanticSelected()) {
                    precisionSemanticSites++;
                }
                if (selection.reflectionSelected()) {
                    precisionReflectionSites++;
                }
                if (selection.triggerSelected()) {
                    precisionTriggerSites++;
                }
                if (selection.highFanoutSelected()) {
                    precisionHighFanoutSites++;
                }
            }
            PtaBudget budget = selection.highPrecision() ? PRECISION_BUDGET : DEFAULT_BUDGET;
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
                    snapshot.methods().methodsByHandle(),
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
            HashSet<MethodReference.Handle> callees = edges.methodCalls().computeIfAbsent(caller, ignore -> new HashSet<>());
            String reason = buildReason(
                    receiver.evidence(),
                    invokeSite.context(),
                    snapshot.constraints().receiverVarByCallSiteKey().get(invokeSite.callSiteKey()),
                    selection.selectorTags()
            );
            String confidence = targets.size() == 1 ? MethodCallMeta.CONF_HIGH : MethodCallMeta.CONF_MEDIUM;
            for (MethodReference.Handle target : targets) {
                MethodReference.Handle callTarget = new MethodReference.Handle(
                        target.getClassReference(),
                        invokeSite.opcode(),
                        target.getName(),
                        target.getDesc()
                );
                MethodCallMeta beforeMeta = MethodCallMeta.resolve(edges.methodCallMeta(), caller, callTarget);
                int beforeBits = beforeMeta == null ? 0 : beforeMeta.getEvidenceBits();
                boolean inserted = MethodCallUtils.addCallee(callees, callTarget);
                MethodCallMeta.record(
                        edges.methodCallMeta(),
                        MethodCallKey.of(caller, callTarget),
                        MethodCallMeta.TYPE_PTA,
                        confidence,
                        reason,
                        invokeSite.opcode()
                );
                MethodCallMeta afterMeta = MethodCallMeta.resolve(edges.methodCallMeta(), caller, callTarget);
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
                methodUnits.size(),
                precisionSelectedSites,
                precisionSemanticSites,
                precisionReflectionSites,
                precisionTriggerSites,
                precisionHighFanoutSites
        );
    }

    private static PrecisionSelection selectHotspot(CallSiteEntity site,
                                                    BuildFactSnapshot snapshot,
                                                    BuildEdgeAccumulator edges,
                                                    InheritanceMap inheritanceMap,
                                                    boolean precisionMode,
                                                    MethodLookup lookup,
                                                    Map<MethodReference.Handle, MethodUnit> methodUnits) {
        if (!isEligibleInvokeSite(site)) {
            return PrecisionSelection.disabled();
        }
        boolean dispatchHotspot = isDispatchHotspot(site, snapshot.types().classesByHandle(), inheritanceMap);
        if (!precisionMode) {
            return dispatchHotspot ? PrecisionSelection.dispatchOnly() : PrecisionSelection.disabled();
        }
        MethodReference.Handle caller = lookup.resolve(
                site.getCallerClassName(),
                site.getCallerMethodName(),
                site.getCallerMethodDesc(),
                site.getJarId()
        );
        MethodUnit unit = caller == null ? null : methodUnits.get(caller);
        boolean semantic = caller != null && hasPrecisionSemanticSignal(snapshot, caller);
        boolean reflection = unit != null && hasReflectionPrecisionSignal(unit.constraints());
        boolean trigger = isTriggerBridgeSite(site);
        boolean highFanout = caller != null && isHighFanoutSite(edges, caller, site);
        boolean enabled = dispatchHotspot || semantic || reflection || trigger || highFanout;
        if (!enabled) {
            return PrecisionSelection.disabled();
        }
        boolean highPrecision = semantic || reflection || trigger || highFanout;
        LinkedHashSet<String> selectorTags = new LinkedHashSet<>();
        if (semantic) {
            selectorTags.add("semantic");
        }
        if (reflection) {
            selectorTags.add("reflection");
        }
        if (trigger) {
            selectorTags.add("trigger");
        }
        if (highFanout) {
            selectorTags.add("fanout");
        }
        return new PrecisionSelection(
                true,
                highPrecision,
                semantic,
                reflection,
                trigger,
                highFanout,
                selectorTags.isEmpty() ? Set.of() : Set.copyOf(selectorTags)
        );
    }

    private static boolean isDispatchHotspot(CallSiteEntity site,
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

    private static boolean isEligibleInvokeSite(CallSiteEntity site) {
        if (site == null || site.getOpCode() == null) {
            return false;
        }
        int opcode = site.getOpCode();
        if (opcode != Opcodes.INVOKEVIRTUAL && opcode != Opcodes.INVOKEINTERFACE) {
            return false;
        }
        return site.getCallIndex() != null && site.getCallIndex() >= 0;
    }

    private static boolean hasPrecisionSemanticSignal(BuildFactSnapshot snapshot,
                                                      MethodReference.Handle caller) {
        if (snapshot == null || caller == null) {
            return false;
        }
        int explicitFlags = snapshot.semantics().explicitSourceMethodFlags().getOrDefault(caller, 0);
        if (explicitFlags != 0) {
            return true;
        }
        int semanticFlags = snapshot.semantics().methodSemanticFlags().getOrDefault(caller, 0);
        return (semanticFlags & PRECISION_SEMANTIC_MASK) != 0;
    }

    private static boolean hasReflectionPrecisionSignal(BuildFactSnapshot.MethodConstraintFacts constraints) {
        if (constraints == null) {
            return false;
        }
        return !constraints.reflectionHints().isEmpty() || !constraints.nativeModelHints().isEmpty();
    }

    private static boolean isTriggerBridgeSite(CallSiteEntity site) {
        if (site == null) {
            return false;
        }
        if (!JAVA_LANG_OBJECT.equals(normalizeClassName(site.getCalleeOwner()))) {
            return false;
        }
        String name = safe(site.getCalleeMethodName());
        String desc = safe(site.getCalleeMethodDesc());
        return ("toString".equals(name) && "()Ljava/lang/String;".equals(desc))
                || ("hashCode".equals(name) && "()I".equals(desc))
                || ("equals".equals(name) && "(Ljava/lang/Object;)Z".equals(desc));
    }

    private static boolean isHighFanoutSite(BuildEdgeAccumulator edges,
                                            MethodReference.Handle caller,
                                            CallSiteEntity site) {
        if (edges == null || caller == null || site == null) {
            return false;
        }
        Set<MethodReference.Handle> callees = edges.methodCalls().get(caller);
        if (callees == null || callees.isEmpty()) {
            return false;
        }
        int matches = 0;
        for (MethodReference.Handle callee : callees) {
            if (callee == null) {
                continue;
            }
            if (!safe(callee.getName()).equals(safe(site.getCalleeMethodName()))) {
                continue;
            }
            if (!safe(callee.getDesc()).equals(safe(site.getCalleeMethodDesc()))) {
                continue;
            }
            matches++;
            if (matches >= 3) {
                return true;
            }
        }
        return false;
    }

    private static String buildReason(Set<String> evidence,
                                      PtaContext context,
                                      String receiverVar,
                                      Set<String> selectorTags) {
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
        if (receiverVar != null && !receiverVar.isBlank()) {
            sb.append("+receiver=").append(receiverVar.trim());
        }
        if (selectorTags != null && !selectorTags.isEmpty()) {
            sb.append("+pta_selector=").append(String.join(",", selectorTags));
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

    private static Map<MethodReference.Handle, MethodUnit> loadMethodUnits(BuildFactSnapshot snapshot,
                                                                           BuildBytecodeWorkspace workspace,
                                                                           MethodLookup lookup) {
        if (snapshot == null || workspace == null || workspace.parsedClasses().isEmpty()) {
            return Map.of();
        }
        Map<MethodReference.Handle, List<CallSiteEntity>> callSitesByCaller = snapshot.symbols().callSitesByCaller();
        LinkedHashMap<MethodReference.Handle, MethodUnit> out = new LinkedHashMap<>();
        for (BuildBytecodeWorkspace.ParsedClass parsedClass : workspace.parsedClasses()) {
            if (parsedClass == null || parsedClass.classNode() == null) {
                continue;
            }
            try {
                ClassNode cn = parsedClass.classNode();
                if (cn.methods == null || cn.methods.isEmpty()) {
                    continue;
                }
                for (BuildBytecodeWorkspace.ParsedMethod parsedMethod : parsedClass.methods()) {
                    MethodNode mn = parsedMethod.methodNode();
                    if (mn == null || mn.instructions == null || mn.instructions.size() == 0) {
                        continue;
                    }
                    MethodReference.Handle handle = lookup.resolve(cn.name, mn.name, mn.desc, parsedClass.jarId());
                    if (handle == null) {
                        continue;
                    }
                    Frame<SourceValue>[] frames = parsedMethod.sourceFrames();
                    if (frames == null) {
                        continue;
                    }
                    BuildFactSnapshot.MethodConstraintFacts constraints = snapshot.constraints().methodConstraints(handle);
                    MethodUnit unit = new MethodUnit(handle,
                            mn,
                            frames,
                            constraints,
                            buildInvokeSites(handle, mn.instructions, callSitesByCaller.get(handle)));
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

    private static List<PtaInvokeSite> buildInvokeSites(MethodReference.Handle caller,
                                                        InsnList instructions,
                                                        List<CallSiteEntity> callSites) {
        if (caller == null || instructions == null || instructions.size() == 0
                || callSites == null || callSites.isEmpty()) {
            return List.of();
        }
        ArrayList<PtaInvokeSite> out = new ArrayList<>();
        ArrayList<CallSiteEntity> orderedSites = new ArrayList<>(callSites);
        orderedSites.sort(Comparator
                .comparingInt((CallSiteEntity site) -> CallSiteKeyUtil.parseInsnIndex(site == null ? null : site.getCallSiteKey()))
                .thenComparingInt(site -> safeInt(site == null ? null : site.getCallIndex())));
        for (CallSiteEntity site : orderedSites) {
            if (site == null) {
                continue;
            }
            int insnIndex = CallSiteKeyUtil.parseInsnIndex(site.getCallSiteKey());
            if (insnIndex < 0 || insnIndex >= instructions.size()) {
                continue;
            }
            AbstractInsnNode insn = instructions.get(insnIndex);
            if (!(insn instanceof MethodInsnNode mi)) {
                continue;
            }
            if (!safe(mi.owner).equals(safe(site.getCalleeOwner()))
                    || !safe(mi.name).equals(safe(site.getCalleeMethodName()))
                    || !safe(mi.desc).equals(safe(site.getCalleeMethodDesc()))
                    || mi.getOpcode() != safeInt(site.getOpCode())) {
                continue;
            }
            out.add(new PtaInvokeSite(
                    safe(site.getCallSiteKey()),
                    new PtaContextMethod(caller, PtaContext.root()),
                    mi.owner,
                    mi.name,
                    mi.desc,
                    mi.getOpcode(),
                    safeInt(site.getCallIndex()),
                    safeInt(site.getLineNumber()),
                    insnIndex,
                    mi
            ));
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
        BuildFactSnapshot.AssignEdge store = unit.constraints().findPreviousObjectAssign(
                loadInsn.var,
                limitIndex,
                Math.max(0, limitIndex - 256)
        );
        if (store == null) {
            return ResolvedValue.empty();
        }
        Frame<SourceValue> storeFrame = unit.frameAt(store.insnIndex());
        if (storeFrame == null || storeFrame.getStackSize() <= 0) {
            return ResolvedValue.empty();
        }
        ResolvedValue local = resolveValue(storeFrame.getStack(storeFrame.getStackSize() - 1),
                unit,
                store.insnIndex(),
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
        for (BuildFactSnapshot.FieldEdge storeEdge : unit.constraints().fieldStoreEdges(
                fieldInsn.owner,
                fieldInsn.name,
                fieldInsn.desc
        )) {
            if (storeEdge == null || storeEdge.insnIndex() >= limitIndex || storeHits >= budget.maxFieldStoreScan()) {
                continue;
            }
            int storeIndex = storeEdge.insnIndex();
            Frame<SourceValue> storeFrame = unit.frameAt(storeIndex);
            if (storeFrame == null || storeFrame.getStackSize() < 2) {
                continue;
            }
            int stackSize = storeFrame.getStackSize();
            ResolvedValue storeBase = resolveValue(storeFrame.getStack(stackSize - 2),
                    unit,
                    storeIndex,
                    inheritanceMap,
                    lookup,
                    budget,
                    path);
            if (!intersects(base.origins(), storeBase.origins())) {
                continue;
            }
            ResolvedValue stored = resolveValue(storeFrame.getStack(stackSize - 1),
                    unit,
                    storeIndex,
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
        for (BuildFactSnapshot.ArrayAccessEdge storeEdge : unit.constraints().arrayStoreEdges()) {
            if (storeEdge == null || storeEdge.insnIndex() >= limitIndex || storeHits >= budget.maxArrayStoreScan()) {
                continue;
            }
            int storeIndex = storeEdge.insnIndex();
            Frame<SourceValue> frame = unit.frameAt(storeIndex);
            if (frame == null || frame.getStackSize() < 3) {
                continue;
            }
            int stackSize = frame.getStackSize();
            ResolvedValue storeArray = resolveValue(frame.getStack(stackSize - 3),
                    unit,
                    storeIndex,
                    inheritanceMap,
                    lookup,
                    budget,
                    path);
            if (!intersects(arrayOrigins, storeArray.origins())) {
                continue;
            }
            Integer arrayIndex = resolveIntValue(frame.getStack(stackSize - 2), unit, storeIndex, budget, path);
            if (!indexMatches(targetIndex, arrayIndex)) {
                continue;
            }
            ResolvedValue stored = resolveValue(frame.getStack(stackSize - 1),
                    unit,
                    storeIndex,
                    inheritanceMap,
                    lookup,
                    budget,
                    path);
            if (stored.isEmpty()) {
                continue;
            }
            out = out.merge(stored.addEvidence("array"));
            storeHits++;
        }
        for (BuildFactSnapshot.ArrayCopyEdge copyEdge : unit.constraints().arrayCopyEdges()) {
            if (copyEdge == null || copyEdge.insnIndex() >= limitIndex || storeHits >= budget.maxArrayStoreScan()) {
                continue;
            }
            int copyIndex = copyEdge.insnIndex();
            Frame<SourceValue> frame = unit.frameAt(copyIndex);
            if (frame == null || frame.getStackSize() < 5) {
                continue;
            }
            int stackSize = frame.getStackSize();
            ResolvedValue dstArray = resolveValue(frame.getStack(stackSize - 3),
                    unit,
                    copyIndex,
                    inheritanceMap,
                    lookup,
                    budget,
                    path);
            if (!intersects(arrayOrigins, dstArray.origins())) {
                continue;
            }
            Integer srcPos = resolveIntValue(frame.getStack(stackSize - 4), unit, copyIndex, budget, path);
            Integer dstPos = resolveIntValue(frame.getStack(stackSize - 2), unit, copyIndex, budget, path);
            Integer length = resolveIntValue(frame.getStack(stackSize - 1), unit, copyIndex, budget, path);
            Integer srcIndex = translateArrayCopyIndex(targetIndex, srcPos, dstPos, length);
            if (targetIndex != null && srcIndex == null) {
                continue;
            }
            ResolvedValue srcArray = resolveValue(frame.getStack(stackSize - 5),
                    unit,
                    copyIndex,
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
                    copyIndex,
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
        for (BuildFactSnapshot.ReturnEdge returnEdge : calleeUnit.constraints().returnEdges()) {
            if (returnEdge == null) {
                continue;
            }
            int returnIndex = returnEdge.insnIndex();
            Frame<SourceValue> frame = calleeUnit.frameAt(returnIndex);
            if (frame == null || frame.getStackSize() <= 0) {
                continue;
            }
            ResolvedValue returned = resolveValue(frame.getStack(frame.getStackSize() - 1),
                    calleeUnit,
                    returnIndex,
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
                BuildFactSnapshot.AssignEdge store = unit.constraints().findPreviousNumericAssign(
                        varInsn.var,
                        limitIndex,
                        Math.max(0, limitIndex - 256)
                );
                if (store == null) {
                    return null;
                }
                Frame<SourceValue> frame = unit.frameAt(store.insnIndex());
                if (frame == null || frame.getStackSize() <= 0) {
                    return null;
                }
                return resolveIntValue(frame.getStack(frame.getStackSize() - 1), unit, store.insnIndex(), budget, path);
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
                         int scannedMethods,
                         int precisionSelectedCallSites,
                         int precisionSemanticCallSites,
                         int precisionReflectionCallSites,
                         int precisionTriggerCallSites,
                         int precisionHighFanoutCallSites) {
        public static Result empty() {
            return new Result(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    private record PrecisionSelection(boolean enabled,
                                      boolean highPrecision,
                                      boolean semanticSelected,
                                      boolean reflectionSelected,
                                      boolean triggerSelected,
                                      boolean highFanoutSelected,
                                      Set<String> selectorTags) {
        private static PrecisionSelection disabled() {
            return new PrecisionSelection(false, false, false, false, false, false, Set.of());
        }

        private static PrecisionSelection dispatchOnly() {
            return new PrecisionSelection(true, false, false, false, false, false, Set.of());
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
        private final MethodNode methodNode;
        private final Frame<SourceValue>[] frames;
        private final BuildFactSnapshot.MethodConstraintFacts constraints;
        private final List<PtaInvokeSite> invokeSites;
        private final Map<MethodReference.Handle, MethodUnit> siblings;

        private MethodUnit(MethodReference.Handle handle,
                           MethodNode methodNode,
                           Frame<SourceValue>[] frames,
                           BuildFactSnapshot.MethodConstraintFacts constraints,
                           List<PtaInvokeSite> invokeSites) {
            this.handle = handle;
            this.methodNode = methodNode;
            this.frames = frames;
            this.constraints = constraints == null
                    ? BuildFactSnapshot.MethodConstraintFacts.empty()
                    : constraints;
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

        private BuildFactSnapshot.MethodConstraintFacts constraints() {
            return constraints;
        }

        private PtaInvokeSite findCallSite(CallSiteEntity site) {
            if (site == null || invokeSites.isEmpty()) {
                return null;
            }
            String key = safe(site.getCallSiteKey());
            if (!key.isEmpty()) {
                for (PtaInvokeSite invokeSite : invokeSites) {
                    if (invokeSite != null && key.equals(safe(invokeSite.callSiteKey()))) {
                        return invokeSite;
                    }
                }
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
