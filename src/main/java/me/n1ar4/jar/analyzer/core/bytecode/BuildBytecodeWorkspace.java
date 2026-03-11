/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.bytecode;

import me.n1ar4.jar.analyzer.core.InheritanceMap;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class BuildBytecodeWorkspace {
    private static final Logger logger = LogManager.getLogger();
    private static final int MIN_CLASSES = 200;

    private final List<ParsedClass> parsedClasses;
    private final Map<ClassReference.Handle, ParsedClass> classesByHandle;
    private volatile Set<ClassReference.Handle> instantiatedClasses;

    private BuildBytecodeWorkspace(List<ParsedClass> parsedClasses) {
        List<ParsedClass> sorted = parsedClasses == null ? List.of() : new ArrayList<>(parsedClasses);
        sorted.sort(Comparator
                .comparing((ParsedClass item) -> safe(item == null ? null : item.className()))
                .thenComparingInt(item -> normalizeJarId(item == null ? null : item.jarId())));
        this.parsedClasses = sorted.isEmpty() ? List.of() : List.copyOf(sorted);
        Map<ClassReference.Handle, ParsedClass> index = new LinkedHashMap<>();
        for (ParsedClass parsedClass : this.parsedClasses) {
            if (parsedClass == null || parsedClass.handle() == null) {
                continue;
            }
            index.put(parsedClass.handle(), parsedClass);
        }
        this.classesByHandle = index.isEmpty() ? Map.of() : Collections.unmodifiableMap(index);
    }

    public static BuildBytecodeWorkspace empty() {
        return new BuildBytecodeWorkspace(List.of());
    }

    public static BuildBytecodeWorkspace parse(Collection<ClassFileEntity> classFiles) {
        if (classFiles == null || classFiles.isEmpty()) {
            return empty();
        }
        List<ClassFileEntity> files = new ArrayList<>(classFiles);
        int threads = resolveThreads(files.size());
        if (threads <= 1) {
            return new BuildBytecodeWorkspace(parseChunk(files));
        }
        List<List<ClassFileEntity>> partitions = partition(files, threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<List<ParsedClass>>> futures = new ArrayList<>();
        Throwable asyncFailure = null;
        try {
            for (List<ClassFileEntity> chunk : partitions) {
                futures.add(pool.submit(new ParseTask(chunk)));
            }
            List<ParsedClass> out = new ArrayList<>();
            for (int i = 0; i < futures.size(); i++) {
                Future<List<ParsedClass>> future = futures.get(i);
                try {
                    out.addAll(future.get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    cancelRemaining(futures, i + 1);
                    break;
                } catch (ExecutionException ex) {
                    asyncFailure = ex.getCause() == null ? ex : ex.getCause();
                    cancelRemaining(futures, i + 1);
                    break;
                }
            }
            if (asyncFailure != null) {
                throw new IllegalStateException("bytecode workspace parse failed", asyncFailure);
            }
            return new BuildBytecodeWorkspace(out);
        } finally {
            shutdownAndAwait(pool);
        }
    }

    public List<ParsedClass> parsedClasses() {
        return parsedClasses;
    }

    public Map<ClassReference.Handle, ParsedClass> classesByHandle() {
        return classesByHandle;
    }

    public ParsedClass findClass(String className, Integer jarId) {
        String normalizedName = safe(className);
        if (normalizedName.isBlank()) {
            return null;
        }
        ParsedClass exact = classesByHandle.get(new ClassReference.Handle(normalizedName, jarId));
        if (exact != null) {
            return exact;
        }
        return classesByHandle.get(new ClassReference.Handle(normalizedName, -1));
    }

    public Set<ClassReference.Handle> collectInstantiatedClasses(InheritanceMap inheritanceMap) {
        Set<ClassReference.Handle> cached = instantiatedClasses;
        if (cached != null) {
            return cached;
        }
        LinkedHashSet<ClassReference.Handle> out = new LinkedHashSet<>();
        for (ParsedClass parsedClass : parsedClasses) {
            if (parsedClass == null || parsedClass.classNode() == null || parsedClass.methods().isEmpty()) {
                continue;
            }
            for (ParsedMethod method : parsedClass.methods()) {
                if (method == null || !method.hasInstructions()) {
                    continue;
                }
                for (int i = 0; i < method.methodNode().instructions.size(); i++) {
                    AbstractInsnNode insn = method.methodNode().instructions.get(i);
                    if (!(insn instanceof TypeInsnNode tin) || tin.getOpcode() != Opcodes.NEW) {
                        continue;
                    }
                    String desc = safe(tin.desc);
                    if (desc.isBlank()) {
                        continue;
                    }
                    ClassReference.Handle resolved = inheritanceMap == null
                            ? new ClassReference.Handle(desc, parsedClass.jarId())
                            : inheritanceMap.resolveClass(desc, parsedClass.jarId());
                    out.add(resolved == null ? new ClassReference.Handle(desc, parsedClass.jarId()) : resolved);
                }
            }
        }
        Set<ClassReference.Handle> resolved = out.isEmpty() ? Set.of() : Set.copyOf(out);
        instantiatedClasses = resolved;
        return resolved;
    }

    private static List<ParsedClass> parseChunk(List<ClassFileEntity> classFiles) {
        List<ParsedClass> out = new ArrayList<>();
        for (ClassFileEntity file : classFiles) {
            if (file == null) {
                continue;
            }
            byte[] bytes = file.getFile();
            if (bytes == null || bytes.length == 0) {
                continue;
            }
            try {
                ClassNode classNode = new ClassNode();
                new ClassReader(bytes).accept(classNode, Const.DiscoveryASMOptions);
                out.add(new ParsedClass(file, classNode));
            } catch (Exception ex) {
                throw new IllegalStateException("parse class node failed for class file: "
                        + safe(file.getClassName()), ex);
            }
        }
        return out;
    }

    private static int resolveThreads(int classCount) {
        int cpu = Runtime.getRuntime().availableProcessors();
        if (cpu <= 1 || classCount < MIN_CLASSES) {
            return 1;
        }
        return Math.min(cpu, Math.max(1, classCount / MIN_CLASSES));
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

    private static void cancelRemaining(List<Future<List<ParsedClass>>> futures, int fromIndex) {
        if (futures == null || futures.isEmpty()) {
            return;
        }
        for (int i = Math.max(0, fromIndex); i < futures.size(); i++) {
            Future<List<ParsedClass>> pending = futures.get(i);
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
            pool.shutdownNow();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static int normalizeJarId(Integer jarId) {
        if (jarId == null || jarId < 0) {
            return Integer.MAX_VALUE;
        }
        return jarId;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class ParseTask implements Callable<List<ParsedClass>> {
        private final List<ClassFileEntity> classFiles;

        private ParseTask(List<ClassFileEntity> classFiles) {
            this.classFiles = classFiles;
        }

        @Override
        public List<ParsedClass> call() {
            return parseChunk(classFiles);
        }
    }

    public static final class ParsedClass {
        private final ClassFileEntity file;
        private final ClassNode classNode;
        private final List<ParsedMethod> methods;

        private ParsedClass(ClassFileEntity file, ClassNode classNode) {
            this.file = file;
            this.classNode = classNode;
            List<ParsedMethod> localMethods = new ArrayList<>();
            if (classNode != null && classNode.methods != null) {
                for (MethodNode methodNode : classNode.methods) {
                    localMethods.add(new ParsedMethod(this, methodNode));
                }
            }
            this.methods = localMethods.isEmpty() ? List.of() : List.copyOf(localMethods);
        }

        public ClassFileEntity file() {
            return file;
        }

        public ClassNode classNode() {
            return classNode;
        }

        public List<ParsedMethod> methods() {
            return methods;
        }

        public Integer jarId() {
            if (file == null || file.getJarId() == null) {
                return -1;
            }
            return file.getJarId();
        }

        public String className() {
            return classNode == null ? "" : safe(classNode.name);
        }

        public ClassReference.Handle handle() {
            return new ClassReference.Handle(className(), jarId());
        }
    }

    public static final class ParsedMethod {
        private final ParsedClass owner;
        private final MethodNode methodNode;
        private volatile Frame<SourceValue>[] sourceFrames;
        private volatile boolean sourceFramesLoaded;

        private ParsedMethod(ParsedClass owner, MethodNode methodNode) {
            this.owner = owner;
            this.methodNode = methodNode;
        }

        public ParsedClass owner() {
            return owner;
        }

        public MethodNode methodNode() {
            return methodNode;
        }

        public boolean hasInstructions() {
            return methodNode != null
                    && methodNode.instructions != null
                    && methodNode.instructions.size() > 0;
        }

        public Frame<SourceValue>[] sourceFrames() {
            if (sourceFramesLoaded) {
                return sourceFrames;
            }
            synchronized (this) {
                if (sourceFramesLoaded) {
                    return sourceFrames;
                }
                sourceFrames = analyzeSourceFrames(owner, methodNode);
                sourceFramesLoaded = true;
                return sourceFrames;
            }
        }

        private static Frame<SourceValue>[] analyzeSourceFrames(ParsedClass owner, MethodNode methodNode) {
            if (owner == null || owner.classNode() == null || methodNode == null) {
                return null;
            }
            try {
                Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());
                return analyzer.analyze(owner.classNode().name, methodNode);
            } catch (Exception ex) {
                logger.debug("source frame analyze failed: {}", ex.toString());
                return null;
            }
        }
    }
}
