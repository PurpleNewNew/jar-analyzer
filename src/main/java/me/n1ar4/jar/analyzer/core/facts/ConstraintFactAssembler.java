/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.facts;

import me.n1ar4.jar.analyzer.core.CallSiteKeyUtil;
import me.n1ar4.jar.analyzer.core.ReflectionConstraintAnalyzer;
import me.n1ar4.jar.analyzer.core.bytecode.BuildBytecodeWorkspace;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ConstraintFactAssembler {
    private ConstraintFactAssembler() {
    }

    static BuildFactSnapshot.ConstraintFacts assemble(List<CallSiteEntity> callSites,
                                                     Map<MethodReference.Handle, MethodReference> methodMap,
                                                     BuildBytecodeWorkspace workspace) {
        if (workspace == null || workspace.parsedClasses().isEmpty()) {
            return BuildFactSnapshot.ConstraintFacts.empty();
        }
        Map<String, ParsedMethodBinding> methodsByKey = indexParsedMethods(workspace);
        Map<String, BuildFactSnapshot.AliasValueFact> instanceFieldFactsByKey =
                ReflectionConstraintAnalyzer.collectInstanceFieldFacts(workspace);
        Map<MethodReference.Handle, BuildFactSnapshot.MethodReflectionHints> reflectionHintsByHandle =
                ReflectionConstraintAnalyzer.collect(workspace, methodMap, instanceFieldFactsByKey);
        LinkedHashMap<MethodReference.Handle, BuildFactSnapshot.MethodConstraintFacts> methodsByHandle = new LinkedHashMap<>();
        for (ParsedMethodBinding binding : methodsByKey.values()) {
            if (binding == null || binding.handle() == null || binding.parsedMethod() == null) {
                continue;
            }
            methodsByHandle.put(
                    binding.handle(),
                    buildMethodConstraints(
                            binding.parsedMethod(),
                            reflectionHintsByHandle.get(binding.handle()),
                            instanceFieldFactsByKey
                    )
            );
        }
        LinkedHashMap<String, String> receiverVarByCallSiteKey = buildReceiverVarFacts(callSites, methodsByKey);
        if (receiverVarByCallSiteKey.isEmpty()
                && instanceFieldFactsByKey.isEmpty()
                && methodsByHandle.isEmpty()) {
            return BuildFactSnapshot.ConstraintFacts.empty();
        }
        return new BuildFactSnapshot.ConstraintFacts(
                receiverVarByCallSiteKey,
                instanceFieldFactsByKey,
                methodsByHandle
        );
    }

    private static LinkedHashMap<String, String> buildReceiverVarFacts(List<CallSiteEntity> callSites,
                                                                       Map<String, ParsedMethodBinding> methodsByKey) {
        LinkedHashMap<String, String> receiverVarByCallSiteKey = new LinkedHashMap<>();
        if (callSites == null || callSites.isEmpty() || methodsByKey.isEmpty()) {
            return receiverVarByCallSiteKey;
        }
        for (CallSiteEntity site : callSites) {
            if (site == null || !isReceiverConstraintSite(site)) {
                continue;
            }
            String callSiteKey = site.getCallSiteKey();
            if (callSiteKey == null || callSiteKey.isBlank()) {
                continue;
            }
            ParsedMethodBinding binding = methodsByKey.get(methodKey(
                    site.getCallerClassName(),
                    site.getCallerMethodName(),
                    site.getCallerMethodDesc(),
                    site.getJarId()
            ));
            if (binding == null) {
                continue;
            }
            String receiverVar = resolveReceiverLocalVar(site, binding.parsedMethod());
            if (receiverVar != null && !receiverVar.isBlank()) {
                receiverVarByCallSiteKey.put(callSiteKey, receiverVar);
            }
        }
        return receiverVarByCallSiteKey;
    }

    private static Map<String, ParsedMethodBinding> indexParsedMethods(BuildBytecodeWorkspace workspace) {
        LinkedHashMap<String, ParsedMethodBinding> out = new LinkedHashMap<>();
        for (BuildBytecodeWorkspace.ParsedClass parsedClass : workspace.parsedClasses()) {
            if (parsedClass == null || parsedClass.classNode() == null || parsedClass.methods().isEmpty()) {
                continue;
            }
            for (BuildBytecodeWorkspace.ParsedMethod parsedMethod : parsedClass.methods()) {
                MethodNode methodNode = parsedMethod == null ? null : parsedMethod.methodNode();
                if (methodNode == null) {
                    continue;
                }
                MethodReference.Handle handle = new MethodReference.Handle(
                        new ClassReference.Handle(parsedClass.className(), parsedClass.jarId()),
                        methodNode.name,
                        methodNode.desc
                );
                out.put(methodKey(
                        parsedClass.className(),
                        methodNode.name,
                        methodNode.desc,
                        parsedClass.jarId()
                ), new ParsedMethodBinding(handle, parsedMethod));
            }
        }
        return out.isEmpty() ? Map.of() : Map.copyOf(out);
    }

    private static BuildFactSnapshot.MethodConstraintFacts buildMethodConstraints(
            BuildBytecodeWorkspace.ParsedMethod parsedMethod,
            BuildFactSnapshot.MethodReflectionHints reflectionHints,
            Map<String, BuildFactSnapshot.AliasValueFact> instanceFieldFactsByKey) {
        MethodNode methodNode = parsedMethod == null ? null : parsedMethod.methodNode();
        if (methodNode == null || methodNode.instructions == null || methodNode.instructions.size() == 0) {
            return BuildFactSnapshot.MethodConstraintFacts.empty();
        }
        Frame<SourceValue>[] frames = null;
        Map<String, String> staticStrings = collectStaticStringConstants(parsedMethod.owner().classNode());
        ArrayList<BuildFactSnapshot.AllocEdge> allocEdges = new ArrayList<>();
        LinkedHashMap<Integer, List<BuildFactSnapshot.AssignEdge>> objectAssignEdgesByVar = new LinkedHashMap<>();
        LinkedHashMap<Integer, List<BuildFactSnapshot.AssignEdge>> numericAssignEdgesByVar = new LinkedHashMap<>();
        LinkedHashMap<String, List<BuildFactSnapshot.FieldEdge>> fieldLoadEdgesByFieldKey = new LinkedHashMap<>();
        LinkedHashMap<String, List<BuildFactSnapshot.FieldEdge>> fieldStoreEdgesByFieldKey = new LinkedHashMap<>();
        ArrayList<BuildFactSnapshot.ArrayAccessEdge> arrayLoadEdges = new ArrayList<>();
        ArrayList<BuildFactSnapshot.ArrayAccessEdge> arrayStoreEdges = new ArrayList<>();
        LinkedHashMap<Integer, Map<Integer, BuildFactSnapshot.AliasValueFact>> arrayElementFactsByVar =
                new LinkedHashMap<>();
        ArrayList<BuildFactSnapshot.ArrayCopyEdge> arrayCopyEdges = new ArrayList<>();
        ArrayList<BuildFactSnapshot.ReturnEdge> returnEdges = new ArrayList<>();
        ArrayList<BuildFactSnapshot.NativeModelHint> nativeModelHints = new ArrayList<>();
        for (int i = 0; i < methodNode.instructions.size(); i++) {
            AbstractInsnNode insn = methodNode.instructions.get(i);
            if (insn == null) {
                continue;
            }
            if (insn instanceof TypeInsnNode typeInsn) {
                if (typeInsn.getOpcode() == Opcodes.NEW) {
                    allocEdges.add(new BuildFactSnapshot.AllocEdge(i, "new", safe(typeInsn.desc)));
                } else if (typeInsn.getOpcode() == Opcodes.ANEWARRAY) {
                    allocEdges.add(new BuildFactSnapshot.AllocEdge(i, "array", safe(typeInsn.desc)));
                }
                continue;
            }
            if (insn instanceof MultiANewArrayInsnNode multiArrayInsn) {
                allocEdges.add(new BuildFactSnapshot.AllocEdge(i, "array", safe(multiArrayInsn.desc)));
                continue;
            }
            if (insn instanceof VarInsnNode varInsn) {
                int opcode = varInsn.getOpcode();
                if (opcode == Opcodes.ASTORE) {
                    objectAssignEdgesByVar.computeIfAbsent(varInsn.var, ignore -> new ArrayList<>())
                            .add(new BuildFactSnapshot.AssignEdge(i, varInsn.var, opcode));
                } else if (opcode == Opcodes.ISTORE
                        || opcode == Opcodes.LSTORE
                        || opcode == Opcodes.FSTORE
                        || opcode == Opcodes.DSTORE) {
                    numericAssignEdgesByVar.computeIfAbsent(varInsn.var, ignore -> new ArrayList<>())
                            .add(new BuildFactSnapshot.AssignEdge(i, varInsn.var, opcode));
                }
                continue;
            }
            if (insn instanceof FieldInsnNode fieldInsn) {
                BuildFactSnapshot.FieldEdge edge = new BuildFactSnapshot.FieldEdge(
                        i,
                        fieldInsn.owner,
                        fieldInsn.name,
                        fieldInsn.desc
                );
                if (fieldInsn.getOpcode() == Opcodes.GETFIELD) {
                    fieldLoadEdgesByFieldKey.computeIfAbsent(
                            BuildFactSnapshot.FieldEdge.key(fieldInsn.owner, fieldInsn.name, fieldInsn.desc),
                            ignore -> new ArrayList<>()
                    ).add(edge);
                } else if (fieldInsn.getOpcode() == Opcodes.PUTFIELD) {
                    fieldStoreEdgesByFieldKey.computeIfAbsent(
                            BuildFactSnapshot.FieldEdge.key(fieldInsn.owner, fieldInsn.name, fieldInsn.desc),
                            ignore -> new ArrayList<>()
                    ).add(edge);
                }
                continue;
            }
            if (insn instanceof InsnNode rawInsn) {
                if (rawInsn.getOpcode() == Opcodes.AALOAD) {
                    arrayLoadEdges.add(new BuildFactSnapshot.ArrayAccessEdge(i));
                } else if (rawInsn.getOpcode() == Opcodes.AASTORE) {
                    arrayStoreEdges.add(new BuildFactSnapshot.ArrayAccessEdge(i));
                    frames = ensureSourceFrames(parsedMethod, frames);
                    collectArrayStoreFacts(
                            rawInsn,
                            methodNode,
                            frames,
                            staticStrings,
                            instanceFieldFactsByKey,
                            arrayElementFactsByVar
                    );
                } else if (rawInsn.getOpcode() == Opcodes.ARETURN) {
                    returnEdges.add(new BuildFactSnapshot.ReturnEdge(i));
                }
                continue;
            }
            if (insn instanceof MethodInsnNode methodInsn
                    && isArrayCopy(methodInsn)) {
                arrayCopyEdges.add(new BuildFactSnapshot.ArrayCopyEdge(i));
                nativeModelHints.add(new BuildFactSnapshot.NativeModelHint(
                        i,
                    "arraycopy",
                    methodInsn.owner,
                    methodInsn.name,
                    methodInsn.desc
                ));
                frames = ensureSourceFrames(parsedMethod, frames);
                collectArrayCopyFacts(methodInsn, methodNode, frames, arrayElementFactsByVar);
            }
        }
        sortAssignMapDescending(objectAssignEdgesByVar);
        sortAssignMapDescending(numericAssignEdgesByVar);
        sortFieldMapDescending(fieldLoadEdgesByFieldKey);
        sortFieldMapDescending(fieldStoreEdgesByFieldKey);
        sortDescending(arrayLoadEdges, BuildFactSnapshot.ArrayAccessEdge::insnIndex);
        sortDescending(arrayStoreEdges, BuildFactSnapshot.ArrayAccessEdge::insnIndex);
        sortDescending(arrayCopyEdges, BuildFactSnapshot.ArrayCopyEdge::insnIndex);
        returnEdges.sort(Comparator.comparingInt(BuildFactSnapshot.ReturnEdge::insnIndex));
        allocEdges.sort(Comparator.comparingInt(BuildFactSnapshot.AllocEdge::insnIndex));
        nativeModelHints.sort(Comparator.comparingInt(BuildFactSnapshot.NativeModelHint::insnIndex));
        return new BuildFactSnapshot.MethodConstraintFacts(
                allocEdges,
                objectAssignEdgesByVar,
                numericAssignEdgesByVar,
                fieldLoadEdgesByFieldKey,
                fieldStoreEdgesByFieldKey,
                arrayLoadEdges,
                arrayStoreEdges,
                arrayElementFactsByVar,
                arrayCopyEdges,
                returnEdges,
                nativeModelHints,
                reflectionHints
        );
    }

    private static String resolveReceiverLocalVar(CallSiteEntity site,
                                                  BuildBytecodeWorkspace.ParsedMethod parsedMethod) {
        if (site == null || parsedMethod == null || parsedMethod.methodNode() == null) {
            return null;
        }
        int insnIndex = CallSiteKeyUtil.parseInsnIndex(site.getCallSiteKey());
        MethodNode methodNode = parsedMethod.methodNode();
        if (insnIndex < 0 || methodNode.instructions == null || insnIndex >= methodNode.instructions.size()) {
            return null;
        }
        AbstractInsnNode insn = methodNode.instructions.get(insnIndex);
        if (!(insn instanceof MethodInsnNode methodInsn) || !matchesCallSite(site, methodInsn)) {
            return null;
        }
        Frame<SourceValue>[] frames = parsedMethod.sourceFrames();
        if (frames == null || insnIndex >= frames.length) {
            return null;
        }
        Frame<SourceValue> frame = frames[insnIndex];
        if (frame == null) {
            return null;
        }
        int argCount = Type.getArgumentTypes(methodInsn.desc).length;
        int receiverIndex = frame.getStackSize() - argCount - 1;
        if (receiverIndex < 0) {
            return null;
        }
        SourceValue receiver = frame.getStack(receiverIndex);
        if (receiver == null || receiver.insns == null || receiver.insns.isEmpty()) {
            return null;
        }
        Map<Integer, List<LocalVarSlot>> localVarSlots = buildLocalVarSlots(methodNode);
        ReceiverVarCandidate best = null;
        for (AbstractInsnNode sourceInsn : receiver.insns) {
            if (!(sourceInsn instanceof VarInsnNode varInsn) || varInsn.getOpcode() != Opcodes.ALOAD) {
                continue;
            }
            ReceiverVarCandidate candidate = resolveReceiverCandidate(
                    parsedMethod.owner().classNode(),
                    methodNode,
                    varInsn.var,
                    insnIndex,
                    localVarSlots
            );
            if (candidate == null) {
                continue;
            }
            if (best == null
                    || candidate.score() > best.score()
                    || (candidate.score() == best.score() && candidate.slot() < best.slot())) {
                best = candidate;
            }
        }
        return best == null ? null : best.name();
    }

    private static Frame<SourceValue>[] ensureSourceFrames(BuildBytecodeWorkspace.ParsedMethod parsedMethod,
                                                           Frame<SourceValue>[] current) {
        if (current != null || parsedMethod == null) {
            return current;
        }
        return parsedMethod.sourceFrames();
    }

    private static void collectArrayStoreFacts(AbstractInsnNode insn,
                                               MethodNode methodNode,
                                               Frame<SourceValue>[] frames,
                                               Map<String, String> staticStrings,
                                               Map<String, BuildFactSnapshot.AliasValueFact> instanceFieldFactsByKey,
                                               Map<Integer, Map<Integer, BuildFactSnapshot.AliasValueFact>> arrayElementFactsByVar) {
        if (insn == null || methodNode == null || frames == null || arrayElementFactsByVar == null) {
            return;
        }
        int insnIndex = methodNode.instructions.indexOf(insn);
        if (insnIndex < 0 || insnIndex >= frames.length) {
            return;
        }
        Frame<SourceValue> frame = frames[insnIndex];
        if (frame == null || frame.getStackSize() < 3) {
            return;
        }
        int stackSize = frame.getStackSize();
        Integer elementIndex = resolveStableInt(frame.getStack(stackSize - 2), methodNode, frames, 0);
        if (elementIndex == null || elementIndex < 0) {
            return;
        }
        BuildFactSnapshot.AliasValueFact valueFact = resolveAliasValueFact(
                frame.getStack(stackSize - 1),
                methodNode,
                frames,
                staticStrings,
                instanceFieldFactsByKey,
                0
        );
        if (valueFact == null || valueFact.isEmpty()) {
            return;
        }
        for (Integer arrayVar : resolveArrayVars(frame.getStack(stackSize - 3), methodNode, frames, 0)) {
            mergeArrayElementFact(arrayElementFactsByVar, arrayVar, elementIndex, valueFact);
        }
    }

    private static void collectArrayCopyFacts(MethodInsnNode methodInsn,
                                              MethodNode methodNode,
                                              Frame<SourceValue>[] frames,
                                              Map<Integer, Map<Integer, BuildFactSnapshot.AliasValueFact>> arrayElementFactsByVar) {
        if (methodInsn == null || methodNode == null || frames == null || arrayElementFactsByVar == null) {
            return;
        }
        int insnIndex = methodNode.instructions.indexOf(methodInsn);
        if (insnIndex < 0 || insnIndex >= frames.length) {
            return;
        }
        Frame<SourceValue> frame = frames[insnIndex];
        if (frame == null || frame.getStackSize() < 5) {
            return;
        }
        int base = frame.getStackSize() - 5;
        if (base < 0) {
            return;
        }
        Integer srcIndex = resolveStableInt(frame.getStack(base + 1), methodNode, frames, 0);
        Integer dstIndex = resolveStableInt(frame.getStack(base + 3), methodNode, frames, 0);
        Integer length = resolveStableInt(frame.getStack(base + 4), methodNode, frames, 0);
        if (srcIndex == null || dstIndex == null || length == null || srcIndex < 0 || dstIndex < 0 || length <= 0) {
            return;
        }
        Set<Integer> srcVars = resolveArrayVars(frame.getStack(base), methodNode, frames, 0);
        Set<Integer> dstVars = resolveArrayVars(frame.getStack(base + 2), methodNode, frames, 0);
        if (srcVars.isEmpty() || dstVars.isEmpty()) {
            return;
        }
        for (Integer srcVar : srcVars) {
            for (Integer dstVar : dstVars) {
                for (int offset = 0; offset < length; offset++) {
                    BuildFactSnapshot.AliasValueFact sourceFact =
                            lookupArrayElementFact(arrayElementFactsByVar, srcVar, srcIndex + offset);
                    if (sourceFact == null || sourceFact.isEmpty()) {
                        continue;
                    }
                    mergeArrayElementFact(arrayElementFactsByVar, dstVar, dstIndex + offset, sourceFact);
                }
            }
        }
    }

    private static void mergeArrayElementFact(Map<Integer, Map<Integer, BuildFactSnapshot.AliasValueFact>> arrayElementFactsByVar,
                                              Integer arrayVar,
                                              Integer elementIndex,
                                              BuildFactSnapshot.AliasValueFact valueFact) {
        if (arrayVar == null || arrayVar < 0 || elementIndex == null || elementIndex < 0 || valueFact == null || valueFact.isEmpty()) {
            return;
        }
        Map<Integer, BuildFactSnapshot.AliasValueFact> elements =
                arrayElementFactsByVar.computeIfAbsent(arrayVar, ignore -> new LinkedHashMap<>());
        BuildFactSnapshot.AliasValueFact existing = elements.get(elementIndex);
        elements.put(elementIndex, mergeAliasFacts(existing, valueFact));
    }

    private static BuildFactSnapshot.AliasValueFact lookupArrayElementFact(
            Map<Integer, Map<Integer, BuildFactSnapshot.AliasValueFact>> arrayElementFactsByVar,
            Integer arrayVar,
            Integer elementIndex) {
        if (arrayVar == null || elementIndex == null || arrayElementFactsByVar == null || arrayElementFactsByVar.isEmpty()) {
            return null;
        }
        Map<Integer, BuildFactSnapshot.AliasValueFact> elements = arrayElementFactsByVar.get(arrayVar);
        if (elements == null || elements.isEmpty()) {
            return null;
        }
        return elements.get(elementIndex);
    }

    private static BuildFactSnapshot.AliasValueFact resolveAliasValueFact(SourceValue value,
                                                                          MethodNode methodNode,
                                                                          Frame<SourceValue>[] frames,
                                                                          Map<String, String> staticStrings,
                                                                          Map<String, BuildFactSnapshot.AliasValueFact> instanceFieldFactsByKey,
                                                                          int depth) {
        if (value == null || value.insns == null || value.insns.isEmpty() || depth > 8) {
            return BuildFactSnapshot.AliasValueFact.empty();
        }
        BuildFactSnapshot.AliasValueFact found = BuildFactSnapshot.AliasValueFact.empty();
        for (AbstractInsnNode src : value.insns) {
            BuildFactSnapshot.AliasValueFact candidate = resolveAliasValueFactFromInsn(
                    src,
                    methodNode,
                    frames,
                    staticStrings,
                    instanceFieldFactsByKey,
                    depth + 1
            );
            found = mergeAliasFacts(found, candidate);
        }
        return found;
    }

    private static BuildFactSnapshot.AliasValueFact resolveAliasValueFactFromInsn(
            AbstractInsnNode src,
            MethodNode methodNode,
            Frame<SourceValue>[] frames,
            Map<String, String> staticStrings,
            Map<String, BuildFactSnapshot.AliasValueFact> instanceFieldFactsByKey,
            int depth) {
        if (src == null || methodNode == null || depth > 8) {
            return BuildFactSnapshot.AliasValueFact.empty();
        }
        if (src instanceof LdcInsnNode ldc && ldc.cst instanceof String stringValue) {
            return new BuildFactSnapshot.AliasValueFact(Set.of(stringValue), Set.of());
        }
        if (src instanceof TypeInsnNode typeInsn) {
            if (typeInsn.getOpcode() == Opcodes.NEW || typeInsn.getOpcode() == Opcodes.CHECKCAST) {
                return new BuildFactSnapshot.AliasValueFact(Set.of(), Set.of(safe(typeInsn.desc)));
            }
            return BuildFactSnapshot.AliasValueFact.empty();
        }
        if (src instanceof FieldInsnNode fieldInsn && fieldInsn.getOpcode() == Opcodes.GETSTATIC) {
            String value = staticStrings == null ? null : staticStrings.get(fieldInsn.owner + "#" + fieldInsn.name);
            return value == null || value.isBlank()
                    ? BuildFactSnapshot.AliasValueFact.empty()
                    : new BuildFactSnapshot.AliasValueFact(Set.of(value), Set.of());
        }
        if (src instanceof FieldInsnNode fieldInsn && fieldInsn.getOpcode() == Opcodes.GETFIELD) {
            BuildFactSnapshot.AliasValueFact facts = instanceFieldFactsByKey == null
                    ? null
                    : instanceFieldFactsByKey.get(fieldKey(fieldInsn.owner, fieldInsn.name));
            return facts == null ? BuildFactSnapshot.AliasValueFact.empty() : facts;
        }
        if (src instanceof VarInsnNode varInsn && varInsn.getOpcode() == Opcodes.ALOAD) {
            AbstractInsnNode stored = getStoredSourceInsn(methodNode, frames, varInsn.var, methodNode.instructions.indexOf(varInsn));
            if (stored == null) {
                return BuildFactSnapshot.AliasValueFact.empty();
            }
            return resolveAliasValueFactFromInsn(stored, methodNode, frames, staticStrings, instanceFieldFactsByKey, depth + 1);
        }
        return BuildFactSnapshot.AliasValueFact.empty();
    }

    private static Set<Integer> resolveArrayVars(SourceValue value,
                                                 MethodNode methodNode,
                                                 Frame<SourceValue>[] frames,
                                                 int depth) {
        if (value == null || value.insns == null || value.insns.isEmpty() || depth > 8) {
            return Set.of();
        }
        LinkedHashSet<Integer> out = new LinkedHashSet<>();
        for (AbstractInsnNode src : value.insns) {
            collectArrayVar(src, methodNode, frames, depth + 1, out);
        }
        return out.isEmpty() ? Set.of() : Set.copyOf(out);
    }

    private static void collectArrayVar(AbstractInsnNode src,
                                        MethodNode methodNode,
                                        Frame<SourceValue>[] frames,
                                        int depth,
                                        Set<Integer> out) {
        if (src == null || methodNode == null || out == null || depth > 8) {
            return;
        }
        if (src instanceof VarInsnNode varInsn && varInsn.getOpcode() == Opcodes.ALOAD) {
            out.add(varInsn.var);
            return;
        }
        if (src instanceof VarInsnNode varInsn && varInsn.getOpcode() == Opcodes.ASTORE) {
            out.add(varInsn.var);
            return;
        }
        if (src instanceof TypeInsnNode || src instanceof MultiANewArrayInsnNode) {
            return;
        }
        if (src instanceof InsnNode insn && insn.getOpcode() == Opcodes.DUP) {
            AbstractInsnNode previous = previousValueInsn(methodNode.instructions, methodNode.instructions.indexOf(insn));
            collectArrayVar(previous, methodNode, frames, depth + 1, out);
            return;
        }
        if (src instanceof VarInsnNode varInsn) {
            AbstractInsnNode stored = getStoredSourceInsn(methodNode, frames, varInsn.var, methodNode.instructions.indexOf(varInsn));
            collectArrayVar(stored, methodNode, frames, depth + 1, out);
        }
    }

    private static AbstractInsnNode getStoredSourceInsn(MethodNode methodNode,
                                                        Frame<SourceValue>[] frames,
                                                        int slot,
                                                        int loadIndex) {
        int storeIndex = findPreviousAstore(methodNode, loadIndex, slot);
        if (storeIndex < 0 || frames == null || storeIndex >= frames.length || frames[storeIndex] == null) {
            return null;
        }
        Frame<SourceValue> storeFrame = frames[storeIndex];
        if (storeFrame.getStackSize() <= 0) {
            return null;
        }
        SourceValue value = storeFrame.getStack(storeFrame.getStackSize() - 1);
        return getSingleInsn(value);
    }

    private static int findPreviousAstore(MethodNode methodNode,
                                          int limitIndex,
                                          int slot) {
        if (methodNode == null || methodNode.instructions == null || limitIndex <= 0 || slot < 0) {
            return -1;
        }
        for (int i = limitIndex - 1; i >= 0; i--) {
            AbstractInsnNode candidate = methodNode.instructions.get(i);
            if (candidate instanceof VarInsnNode varInsn
                    && varInsn.getOpcode() == Opcodes.ASTORE
                    && varInsn.var == slot) {
                return i;
            }
        }
        return -1;
    }

    private static Integer resolveStableInt(SourceValue value,
                                            MethodNode methodNode,
                                            Frame<SourceValue>[] frames,
                                            int depth) {
        if (value == null || value.insns == null || value.insns.isEmpty() || depth > 8) {
            return null;
        }
        Integer found = null;
        for (AbstractInsnNode src : value.insns) {
            Integer candidate = resolveStableIntFromInsn(src, methodNode, frames, depth + 1);
            if (candidate == null) {
                continue;
            }
            if (found != null && !found.equals(candidate)) {
                return null;
            }
            found = candidate;
        }
        return found;
    }

    private static Integer resolveStableIntFromInsn(AbstractInsnNode src,
                                                    MethodNode methodNode,
                                                    Frame<SourceValue>[] frames,
                                                    int depth) {
        if (src == null || methodNode == null || depth > 8) {
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
        if (src instanceof org.objectweb.asm.tree.IntInsnNode intInsn) {
            return intInsn.operand;
        }
        if (src instanceof org.objectweb.asm.tree.LdcInsnNode ldc && ldc.cst instanceof Integer value) {
            return value;
        }
        if (src instanceof VarInsnNode varInsn) {
            AbstractInsnNode stored = getStoredSourceInsn(methodNode, frames, varInsn.var, methodNode.instructions.indexOf(varInsn));
            return resolveStableIntFromInsn(stored, methodNode, frames, depth + 1);
        }
        return null;
    }

    private static AbstractInsnNode getSingleInsn(SourceValue value) {
        if (value == null || value.insns == null || value.insns.size() != 1) {
            return null;
        }
        return value.insns.iterator().next();
    }

    private static AbstractInsnNode previousValueInsn(org.objectweb.asm.tree.InsnList instructions,
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

    private static BuildFactSnapshot.AliasValueFact mergeAliasFacts(BuildFactSnapshot.AliasValueFact left,
                                                                    BuildFactSnapshot.AliasValueFact right) {
        if (left == null || left.isEmpty()) {
            return right == null ? BuildFactSnapshot.AliasValueFact.empty() : right;
        }
        if (right == null || right.isEmpty()) {
            return left;
        }
        LinkedHashSet<String> strings = new LinkedHashSet<>(left.stringValues());
        strings.addAll(right.stringValues());
        LinkedHashSet<String> objectTypes = new LinkedHashSet<>(left.objectTypes());
        objectTypes.addAll(right.objectTypes());
        return new BuildFactSnapshot.AliasValueFact(strings, objectTypes);
    }

    private static ReceiverVarCandidate resolveReceiverCandidate(ClassNode classNode,
                                                                MethodNode methodNode,
                                                                int slot,
                                                                int insnIndex,
                                                                Map<Integer, List<LocalVarSlot>> localVarSlots) {
        if (slot < 0) {
            return null;
        }
        List<LocalVarSlot> ranges = localVarSlots.get(slot);
        if (ranges != null) {
            for (LocalVarSlot range : ranges) {
                if (range != null && range.contains(insnIndex) && range.name() != null && !range.name().isBlank()) {
                    return new ReceiverVarCandidate(range.name(), slot, 3);
                }
            }
        }
        boolean isStatic = methodNode != null && (methodNode.access & Opcodes.ACC_STATIC) != 0;
        if (!isStatic && slot == 0 && classNode != null && classNode.name != null && !classNode.name.isBlank()) {
            return new ReceiverVarCandidate("this", slot, 2);
        }
        return new ReceiverVarCandidate("slot:" + slot, slot, 1);
    }

    private static Map<Integer, List<LocalVarSlot>> buildLocalVarSlots(MethodNode methodNode) {
        if (methodNode == null || methodNode.localVariables == null || methodNode.localVariables.isEmpty()) {
            return Map.of();
        }
        Map<LabelNode, Integer> labelIndex = new HashMap<>();
        if (methodNode.instructions != null) {
            for (int i = 0; i < methodNode.instructions.size(); i++) {
                AbstractInsnNode insn = methodNode.instructions.get(i);
                if (insn instanceof LabelNode label) {
                    labelIndex.put(label, i);
                }
            }
        }
        LinkedHashMap<Integer, List<LocalVarSlot>> out = new LinkedHashMap<>();
        for (LocalVariableNode localVar : methodNode.localVariables) {
            if (localVar == null) {
                continue;
            }
            int start = labelIndex.getOrDefault(localVar.start, -1);
            int end = labelIndex.getOrDefault(localVar.end, -1);
            out.computeIfAbsent(localVar.index, ignore -> new ArrayList<>())
                    .add(new LocalVarSlot(start, end, localVar.name));
        }
        if (out.isEmpty()) {
            return Map.of();
        }
        for (List<LocalVarSlot> ranges : out.values()) {
            ranges.sort(Comparator.comparingInt(LocalVarSlot::start));
        }
        return Map.copyOf(out);
    }

    private static void sortAssignMapDescending(Map<Integer, List<BuildFactSnapshot.AssignEdge>> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        for (List<BuildFactSnapshot.AssignEdge> items : values.values()) {
            sortDescending(items, BuildFactSnapshot.AssignEdge::insnIndex);
        }
    }

    private static void sortFieldMapDescending(Map<String, List<BuildFactSnapshot.FieldEdge>> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        for (List<BuildFactSnapshot.FieldEdge> items : values.values()) {
            sortDescending(items, BuildFactSnapshot.FieldEdge::insnIndex);
        }
    }

    private static <T> void sortDescending(List<T> values,
                                           java.util.function.ToIntFunction<T> indexOf) {
        if (values == null || values.size() <= 1) {
            return;
        }
        values.sort(Comparator.comparingInt(indexOf).reversed());
    }

    private static boolean matchesCallSite(CallSiteEntity site, MethodInsnNode methodInsn) {
        if (site == null || methodInsn == null) {
            return false;
        }
        return safe(site.getCalleeOwner()).equals(safe(methodInsn.owner))
                && safe(site.getCalleeMethodName()).equals(safe(methodInsn.name))
                && safe(site.getCalleeMethodDesc()).equals(safe(methodInsn.desc))
                && normalizeOpcode(site.getOpCode()) == methodInsn.getOpcode();
    }

    private static boolean isReceiverConstraintSite(CallSiteEntity site) {
        int opcode = normalizeOpcode(site == null ? null : site.getOpCode());
        return opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKEINTERFACE;
    }

    private static boolean isArrayCopy(MethodInsnNode methodInsn) {
        if (methodInsn == null) {
            return false;
        }
        return "java/lang/System".equals(methodInsn.owner)
                && "arraycopy".equals(methodInsn.name)
                && "(Ljava/lang/Object;ILjava/lang/Object;II)V".equals(methodInsn.desc);
    }

    private static Map<String, String> collectStaticStringConstants(ClassNode classNode) {
        if (classNode == null || classNode.fields == null || classNode.fields.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        classNode.fields.forEach(field -> {
            if (field == null || field.value == null || !(field.value instanceof String stringValue)) {
                return;
            }
            if ((field.access & (Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)) == (Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)) {
                out.put(classNode.name + "#" + field.name, stringValue.trim());
            }
        });
        return out.isEmpty() ? Map.of() : Map.copyOf(out);
    }

    private static String fieldKey(String owner, String name) {
        String normalizedOwner = safe(owner);
        String normalizedName = safe(name);
        if (normalizedOwner.isBlank() || normalizedName.isBlank()) {
            return "";
        }
        return normalizedOwner + "#" + normalizedName;
    }

    private static String methodKey(String owner, String name, String desc, Integer jarId) {
        return safe(owner) + "|" + safe(name) + "|" + safe(desc) + "|" + normalizeJarId(jarId);
    }

    private static int normalizeJarId(Integer jarId) {
        return jarId == null ? -1 : jarId;
    }

    private static int normalizeOpcode(Integer opcode) {
        return opcode == null ? -1 : opcode;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record ParsedMethodBinding(MethodReference.Handle handle,
                                       BuildBytecodeWorkspace.ParsedMethod parsedMethod) {
    }

    private record LocalVarSlot(int start, int end, String name) {
        private boolean contains(int insnIndex) {
            if (insnIndex < 0) {
                return false;
            }
            if (start < 0 && end < 0) {
                return true;
            }
            if (start < 0) {
                return insnIndex < end;
            }
            if (end < 0) {
                return insnIndex >= start;
            }
            return insnIndex >= start && insnIndex < end;
        }
    }

    private record ReceiverVarCandidate(String name, int slot, int score) {
    }
}
