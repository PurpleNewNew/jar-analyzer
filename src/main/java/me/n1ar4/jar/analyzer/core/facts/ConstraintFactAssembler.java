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
import java.util.List;
import java.util.Map;

final class ConstraintFactAssembler {
    private ConstraintFactAssembler() {
    }

    static BuildFactSnapshot.ConstraintFacts assemble(List<CallSiteEntity> callSites,
                                                     BuildBytecodeWorkspace workspace) {
        if (workspace == null || workspace.parsedClasses().isEmpty()) {
            return BuildFactSnapshot.ConstraintFacts.empty();
        }
        Map<String, ParsedMethodBinding> methodsByKey = indexParsedMethods(workspace);
        LinkedHashMap<MethodReference.Handle, BuildFactSnapshot.MethodConstraintFacts> methodsByHandle = new LinkedHashMap<>();
        for (ParsedMethodBinding binding : methodsByKey.values()) {
            if (binding == null || binding.handle() == null || binding.parsedMethod() == null) {
                continue;
            }
            methodsByHandle.put(binding.handle(), buildMethodConstraints(binding.parsedMethod()));
        }
        LinkedHashMap<String, String> receiverVarByCallSiteKey = buildReceiverVarFacts(callSites, methodsByKey);
        if (receiverVarByCallSiteKey.isEmpty() && methodsByHandle.isEmpty()) {
            return BuildFactSnapshot.ConstraintFacts.empty();
        }
        return new BuildFactSnapshot.ConstraintFacts(receiverVarByCallSiteKey, methodsByHandle);
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

    private static BuildFactSnapshot.MethodConstraintFacts buildMethodConstraints(BuildBytecodeWorkspace.ParsedMethod parsedMethod) {
        MethodNode methodNode = parsedMethod == null ? null : parsedMethod.methodNode();
        if (methodNode == null || methodNode.instructions == null || methodNode.instructions.size() == 0) {
            return BuildFactSnapshot.MethodConstraintFacts.empty();
        }
        ArrayList<BuildFactSnapshot.AllocEdge> allocEdges = new ArrayList<>();
        LinkedHashMap<Integer, List<BuildFactSnapshot.AssignEdge>> objectAssignEdgesByVar = new LinkedHashMap<>();
        LinkedHashMap<Integer, List<BuildFactSnapshot.AssignEdge>> numericAssignEdgesByVar = new LinkedHashMap<>();
        LinkedHashMap<String, List<BuildFactSnapshot.FieldEdge>> fieldLoadEdgesByFieldKey = new LinkedHashMap<>();
        LinkedHashMap<String, List<BuildFactSnapshot.FieldEdge>> fieldStoreEdgesByFieldKey = new LinkedHashMap<>();
        ArrayList<BuildFactSnapshot.ArrayAccessEdge> arrayLoadEdges = new ArrayList<>();
        ArrayList<BuildFactSnapshot.ArrayAccessEdge> arrayStoreEdges = new ArrayList<>();
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
                arrayCopyEdges,
                returnEdges,
                nativeModelHints
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
