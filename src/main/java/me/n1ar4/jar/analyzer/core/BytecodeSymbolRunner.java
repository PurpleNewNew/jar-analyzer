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

import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.LocalVarEntity;
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
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class BytecodeSymbolRunner {
    private static final Logger logger = LogManager.getLogger();
    private static final String THREADS_PROP = "jar.analyzer.symbol.threads";

    private BytecodeSymbolRunner() {
    }

    public static Result start(Set<ClassFileEntity> classFileList) {
        if (classFileList == null || classFileList.isEmpty()) {
            return Result.empty();
        }
        List<ClassFileEntity> files = new ArrayList<>(classFileList);
        int threads = resolveThreads(files.size());
        if (threads <= 1) {
            return analyzeChunk(files);
        }
        List<List<ClassFileEntity>> partitions = partition(files, threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<Result>> futures = new ArrayList<>();
        Throwable asyncFailure = null;
        try {
            for (List<ClassFileEntity> chunk : partitions) {
                futures.add(pool.submit(new LocalTask(chunk)));
            }
            List<CallSiteEntity> callSites = new ArrayList<>();
            List<LocalVarEntity> localVars = new ArrayList<>();
            for (int i = 0; i < futures.size(); i++) {
                Future<Result> future = futures.get(i);
                try {
                    Result result = future.get();
                    callSites.addAll(result.callSites);
                    localVars.addAll(result.localVars);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("symbol index interrupted");
                    cancelRemaining(futures, i + 1);
                    break;
                } catch (ExecutionException e) {
                    asyncFailure = e.getCause() == null ? e : e.getCause();
                    logger.debug("symbol index task failed: {}", asyncFailure.toString(), asyncFailure);
                    cancelRemaining(futures, i + 1);
                    break;
                }
            }
            if (asyncFailure != null) {
                throw new IllegalStateException("symbol index task failed", asyncFailure);
            }
            return new Result(callSites, localVars);
        } finally {
            shutdownAndAwait(pool);
        }
    }

    private static Result analyzeChunk(List<ClassFileEntity> classFileList) {
        List<CallSiteEntity> callSites = new ArrayList<>();
        List<LocalVarEntity> localVars = new ArrayList<>();
        for (ClassFileEntity file : classFileList) {
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
                cr.accept(cn, ClassReader.SKIP_FRAMES);
                if (cn.methods == null || cn.methods.isEmpty()) {
                    continue;
                }
                for (MethodNode mn : cn.methods) {
                    if (mn == null || mn.instructions == null) {
                        continue;
                    }
                    MethodResultBundle bundle = analyzeMethod(cn, mn, file.getJarId());
                    callSites.addAll(bundle.callSites);
                    localVars.addAll(bundle.localVars);
                }
            } catch (Exception ex) {
                throw new IllegalStateException("symbol index failed for class file: "
                        + (file.getClassName() == null ? "" : file.getClassName()), ex);
            }
        }
        return new Result(callSites, localVars);
    }

    private static MethodResultBundle analyzeMethod(ClassNode cn, MethodNode mn, Integer jarId) {
        InsnList instructions = mn.instructions;
        int size = instructions.size();
        Map<LabelNode, Integer> labelIndex = new IdentityHashMap<>();
        Map<LabelNode, Integer> lineByLabel = new IdentityHashMap<>();
        for (int i = 0; i < size; i++) {
            AbstractInsnNode insn = instructions.get(i);
            if (insn instanceof LabelNode) {
                labelIndex.put((LabelNode) insn, i);
            } else if (insn instanceof LineNumberNode) {
                LineNumberNode ln = (LineNumberNode) insn;
                if (ln.start != null) {
                    lineByLabel.put(ln.start, ln.line);
                }
            }
        }
        Map<Integer, List<VarRange>> localsByIndex = buildLocalVarRanges(mn, labelIndex);
        List<LocalVarEntity> localVars = buildLocalVarEntities(cn, mn, jarId, lineByLabel);

        boolean needsInfer = containsVirtualCalls(instructions);
        Frame<SourceValue>[] frames = null;
        if (needsInfer) {
            try {
                Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());
                frames = analyzer.analyze(cn.name, mn);
            } catch (Exception ex) {
                logger.debug("symbol frame analyze failed: {}", ex.toString());
                frames = null;
            }
        }
        List<CallSiteEntity> callSites = new ArrayList<>();
        int currentLine = -1;
        int callIndex = 0;
        for (int i = 0; i < size; i++) {
            AbstractInsnNode insn = instructions.get(i);
            if (insn instanceof LineNumberNode) {
                currentLine = ((LineNumberNode) insn).line;
                continue;
            }
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode mi = (MethodInsnNode) insn;
                CallSiteEntity site = new CallSiteEntity();
                site.setCallerClassName(cn.name);
                site.setCallerMethodName(mn.name);
                site.setCallerMethodDesc(mn.desc);
                site.setCalleeOwner(mi.owner);
                site.setCalleeMethodName(mi.name);
                site.setCalleeMethodDesc(mi.desc);
                site.setOpCode(mi.getOpcode());
                site.setLineNumber(currentLine);
                site.setCallIndex(callIndex++);
                site.setJarId(jarId == null ? -1 : jarId);
                if (frames != null && isVirtualOpcode(mi.getOpcode())) {
                    String receiver = inferReceiverType(cn, mn, mi, i, frames, localsByIndex);
                    site.setReceiverType(receiver);
                }
                site.setCallSiteKey(CallSiteKeyUtil.buildCallSiteKey(site, i));
                callSites.add(site);
                continue;
            }
            if (insn instanceof InvokeDynamicInsnNode) {
                InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                Handle target = resolveInvokeDynamicTarget(indy);
                if (target != null) {
                    CallSiteEntity site = new CallSiteEntity();
                    site.setCallerClassName(cn.name);
                    site.setCallerMethodName(mn.name);
                    site.setCallerMethodDesc(mn.desc);
                    site.setCalleeOwner(target.getOwner());
                    site.setCalleeMethodName(target.getName());
                    site.setCalleeMethodDesc(target.getDesc());
                    site.setOpCode(Opcodes.INVOKEDYNAMIC);
                    site.setLineNumber(currentLine);
                    site.setCallIndex(-1);
                    site.setJarId(jarId == null ? -1 : jarId);
                    site.setCallSiteKey(CallSiteKeyUtil.buildCallSiteKey(site, i));
                    callSites.add(site);
                }
            }
        }
        return new MethodResultBundle(callSites, localVars);
    }

    private static boolean containsVirtualCalls(InsnList instructions) {
        if (instructions == null || instructions.size() == 0) {
            return false;
        }
        for (int i = 0; i < instructions.size(); i++) {
            AbstractInsnNode insn = instructions.get(i);
            if (!(insn instanceof MethodInsnNode)) {
                continue;
            }
            int opcode = insn.getOpcode();
            if (isVirtualOpcode(opcode)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isVirtualOpcode(int opcode) {
        return opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKEINTERFACE;
    }

    private static Handle resolveInvokeDynamicTarget(InvokeDynamicInsnNode indy) {
        if (indy == null || indy.bsm == null) {
            return null;
        }
        String owner = indy.bsm.getOwner();
        String name = indy.bsm.getName();
        if ("java/lang/invoke/StringConcatFactory".equals(owner)) {
            return null;
        }
        if ("java/lang/invoke/LambdaMetafactory".equals(owner)
                && ("metafactory".equals(name) || "altMetafactory".equals(name))) {
            Object[] args = indy.bsmArgs;
            if (args != null) {
                for (Object arg : args) {
                    if (arg instanceof Handle) {
                        return (Handle) arg;
                    }
                }
            }
        }
        return null;
    }

    private static String inferReceiverType(ClassNode cn,
                                            MethodNode mn,
                                            MethodInsnNode mi,
                                            int insnIndex,
                                            Frame<SourceValue>[] frames,
                                            Map<Integer, List<VarRange>> localsByIndex) {
        if (frames == null || insnIndex < 0 || insnIndex >= frames.length) {
            return null;
        }
        Frame<SourceValue> frame = frames[insnIndex];
        if (frame == null) {
            return null;
        }
        int argCount = Type.getArgumentTypes(mi.desc).length;
        int recvIndex = frame.getStackSize() - argCount - 1;
        if (recvIndex < 0) {
            return null;
        }
        SourceValue value = frame.getStack(recvIndex);
        if (value == null || value.insns == null || value.insns.isEmpty()) {
            return null;
        }
        String best = null;
        int bestScore = -1;
        for (AbstractInsnNode src : value.insns) {
            TypeCandidate candidate = resolveCandidateType(cn, mn, src, insnIndex, localsByIndex);
            if (candidate == null || candidate.type == null) {
                continue;
            }
            if (candidate.score > bestScore) {
                best = candidate.type;
                bestScore = candidate.score;
            }
        }
        return best;
    }

    private static TypeCandidate resolveCandidateType(ClassNode cn,
                                                      MethodNode mn,
                                                      AbstractInsnNode insn,
                                                      int insnIndex,
                                                      Map<Integer, List<VarRange>> localsByIndex) {
        if (insn == null) {
            return null;
        }
        if (insn instanceof TypeInsnNode) {
            TypeInsnNode tn = (TypeInsnNode) insn;
            if (tn.desc != null && !tn.desc.isEmpty()) {
                int score = (tn.getOpcode() == Opcodes.NEW || tn.getOpcode() == Opcodes.CHECKCAST) ? 5 : 2;
                return new TypeCandidate(tn.desc, score);
            }
        }
        if (insn instanceof MethodInsnNode) {
            MethodInsnNode mi = (MethodInsnNode) insn;
            String type = normalizeTypeDesc(Type.getReturnType(mi.desc));
            if (type != null) {
                return new TypeCandidate(type, 3);
            }
        }
        if (insn instanceof FieldInsnNode) {
            FieldInsnNode fi = (FieldInsnNode) insn;
            String type = normalizeTypeDesc(Type.getType(fi.desc));
            if (type != null) {
                return new TypeCandidate(type, 2);
            }
        }
        if (insn instanceof VarInsnNode) {
            VarInsnNode vi = (VarInsnNode) insn;
            String type = resolveLocalVarType(cn, mn, vi.var, insnIndex, localsByIndex);
            if (type != null) {
                return new TypeCandidate(type, 4);
            }
        }
        return null;
    }

    private static String resolveLocalVarType(ClassNode cn,
                                              MethodNode mn,
                                              int varIndex,
                                              int insnIndex,
                                              Map<Integer, List<VarRange>> localsByIndex) {
        if (localsByIndex != null) {
            List<VarRange> ranges = localsByIndex.get(varIndex);
            if (ranges != null) {
                for (VarRange range : ranges) {
                    if (range.contains(insnIndex)) {
                        String type = normalizeTypeDesc(Type.getType(range.desc));
                        if (type != null) {
                            return type;
                        }
                    }
                }
            }
        }
        boolean isStatic = (mn.access & Opcodes.ACC_STATIC) != 0;
        if (!isStatic && varIndex == 0 && cn != null) {
            return cn.name;
        }
        return null;
    }

    private static String normalizeTypeDesc(Type type) {
        if (type == null) {
            return null;
        }
        if (type.getSort() == Type.OBJECT) {
            return type.getInternalName();
        }
        if (type.getSort() == Type.ARRAY) {
            return null;
        }
        return null;
    }

    private static Map<Integer, List<VarRange>> buildLocalVarRanges(MethodNode mn,
                                                                    Map<LabelNode, Integer> labelIndex) {
        if (mn.localVariables == null || mn.localVariables.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, List<VarRange>> out = new HashMap<>();
        for (LocalVariableNode lv : mn.localVariables) {
            if (lv == null) {
                continue;
            }
            int start = labelIndex.getOrDefault(lv.start, -1);
            int end = labelIndex.getOrDefault(lv.end, -1);
            VarRange range = new VarRange(start, end, lv.desc);
            out.computeIfAbsent(lv.index, k -> new ArrayList<>()).add(range);
        }
        return out;
    }

    private static List<LocalVarEntity> buildLocalVarEntities(ClassNode cn,
                                                              MethodNode mn,
                                                              Integer jarId,
                                                              Map<LabelNode, Integer> lineByLabel) {
        if (mn.localVariables == null || mn.localVariables.isEmpty()) {
            return Collections.emptyList();
        }
        List<LocalVarEntity> out = new ArrayList<>();
        for (LocalVariableNode lv : mn.localVariables) {
            if (lv == null) {
                continue;
            }
            LocalVarEntity entity = new LocalVarEntity();
            entity.setClassName(cn.name);
            entity.setMethodName(mn.name);
            entity.setMethodDesc(mn.desc);
            entity.setVarIndex(lv.index);
            entity.setVarName(lv.name);
            entity.setVarDesc(lv.desc);
            entity.setVarSignature(lv.signature);
            Integer startLine = lineByLabel.get(lv.start);
            Integer endLine = lineByLabel.get(lv.end);
            entity.setStartLine(startLine == null ? -1 : startLine);
            entity.setEndLine(endLine == null ? -1 : endLine);
            entity.setJarId(jarId == null ? -1 : jarId);
            out.add(entity);
        }
        return out;
    }

    private static int resolveThreads(int classCount) {
        String raw = System.getProperty(THREADS_PROP);
        if (raw != null && !raw.trim().isEmpty()) {
            try {
                int val = Integer.parseInt(raw.trim());
                if (val > 0) {
                    return Math.min(val, Math.max(1, classCount));
                }
            } catch (NumberFormatException ex) {
                logger.debug("invalid threads: {}", raw);
            }
        }
        int cpu = Runtime.getRuntime().availableProcessors();
        if (cpu <= 1 || classCount < 200) {
            return 1;
        }
        return Math.min(cpu, Math.max(1, classCount / 200));
    }

    private static List<List<ClassFileEntity>> partition(List<ClassFileEntity> items, int parts) {
        if (parts <= 1) {
            return Collections.singletonList(items);
        }
        List<List<ClassFileEntity>> buckets = new ArrayList<>();
        for (int i = 0; i < parts; i++) {
            buckets.add(new ArrayList<>());
        }
        for (int i = 0; i < items.size(); i++) {
            buckets.get(i % parts).add(items.get(i));
        }
        return buckets;
    }

    private static void cancelRemaining(List<Future<Result>> futures, int fromIndex) {
        if (futures == null || futures.isEmpty()) {
            return;
        }
        for (int i = Math.max(0, fromIndex); i < futures.size(); i++) {
            Future<Result> pending = futures.get(i);
            if (pending != null) {
                pending.cancel(true);
            }
        }
    }

    private static void shutdownAndAwait(ExecutorService pool) {
        if (pool == null) {
            return;
        }
        pool.shutdown();
        try {
            if (pool.awaitTermination(30, TimeUnit.SECONDS)) {
                return;
            }
            logger.warn("symbol index pool did not terminate in 30s, force shutdown");
            pool.shutdownNow();
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("symbol index pool still running after force shutdown");
            }
        } catch (InterruptedException ex) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static final class LocalTask implements Callable<Result> {
        private final List<ClassFileEntity> classFileList;

        private LocalTask(List<ClassFileEntity> classFileList) {
            this.classFileList = classFileList;
        }

        @Override
        public Result call() {
            return analyzeChunk(classFileList);
        }
    }

    private static final class MethodResultBundle {
        private final List<CallSiteEntity> callSites;
        private final List<LocalVarEntity> localVars;

        private MethodResultBundle(List<CallSiteEntity> callSites, List<LocalVarEntity> localVars) {
            this.callSites = callSites;
            this.localVars = localVars;
        }
    }

    private static final class VarRange {
        private final int start;
        private final int end;
        private final String desc;

        private VarRange(int start, int end, String desc) {
            this.start = start;
            this.end = end;
            this.desc = desc;
        }

        private boolean contains(int index) {
            if (index < 0) {
                return false;
            }
            if (start < 0 || end < 0) {
                return false;
            }
            return index >= start && index <= end;
        }
    }

    private static final class TypeCandidate {
        private final String type;
        private final int score;

        private TypeCandidate(String type, int score) {
            this.type = type;
            this.score = score;
        }
    }

    public static final class Result {
        private final List<CallSiteEntity> callSites;
        private final List<LocalVarEntity> localVars;

        private Result(List<CallSiteEntity> callSites, List<LocalVarEntity> localVars) {
            this.callSites = callSites == null ? new ArrayList<>() : callSites;
            this.localVars = localVars == null ? new ArrayList<>() : localVars;
        }

        public List<CallSiteEntity> getCallSites() {
            return callSites;
        }

        public List<LocalVarEntity> getLocalVars() {
            return localVars;
        }

        public static Result empty() {
            return new Result(Collections.emptyList(), Collections.emptyList());
        }
    }
}
