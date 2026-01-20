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

import me.n1ar4.jar.analyzer.analyze.spring.SpringController;
import me.n1ar4.jar.analyzer.analyze.spring.asm.SpringClassVisitor;
import me.n1ar4.jar.analyzer.core.asm.JavaWebClassVisitor;
import me.n1ar4.jar.analyzer.core.asm.MethodCallClassVisitor;
import me.n1ar4.jar.analyzer.core.asm.ReflectionCallResolver;
import me.n1ar4.jar.analyzer.core.asm.StringClassVisitor;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class ClassAnalysisRunner {
    private static final Logger logger = LogManager.getLogger();
    private static final String THREADS_PROP = "jar.analyzer.class.analysis.threads";
    private static final int PROBE_MIN_CLASSES = 400;
    private static final int PROBE_CLASSES_PER_THREAD = 200;
    private static final double PARALLEL_GAIN_THRESHOLD = 0.90;

    private ClassAnalysisRunner() {
    }

    public static void start(Set<ClassFileEntity> classFileList,
                             HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
                             Map<MethodReference.Handle, MethodReference> methodMap,
                             Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                             Map<MethodReference.Handle, List<String>> strMap,
                             Map<ClassReference.Handle, ClassReference> classMap,
                             List<SpringController> controllers,
                             ArrayList<String> interceptors,
                             ArrayList<String> servlets,
                             ArrayList<String> filters,
                             ArrayList<String> listeners,
                             boolean analyzeStrings,
                             boolean analyzeSpring,
                             boolean analyzeWeb) {
        logger.info("start class analysis pipeline");
        if (classFileList == null || classFileList.isEmpty()) {
            return;
        }
        List<ClassFileEntity> files = new ArrayList<>(classFileList);
        int threads = resolveThreads(files, methodMap, classMap, analyzeStrings, analyzeSpring, analyzeWeb);
        logger.info("class analysis threads: {}", threads);
        if (threads <= 1) {
            LocalResult result = analyzeChunk(files, methodMap, classMap,
                    analyzeStrings, analyzeSpring, analyzeWeb);
            mergeMethodCalls(methodCalls, result.methodCalls);
            mergeMethodCallMeta(methodCallMeta, result.methodCallMeta);
            mergeStrings(strMap, result.strMap);
            mergeList(controllers, result.controllers);
            mergeList(interceptors, result.interceptors);
            mergeList(servlets, result.servlets);
            mergeList(filters, result.filters);
            mergeList(listeners, result.listeners);
            return;
        }
        List<List<ClassFileEntity>> partitions = partition(files, threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<LocalResult>> futures = new ArrayList<>();
        for (List<ClassFileEntity> chunk : partitions) {
            futures.add(pool.submit(new LocalTask(chunk, methodMap, classMap,
                    analyzeStrings, analyzeSpring, analyzeWeb)));
        }
        for (Future<LocalResult> future : futures) {
            try {
                LocalResult result = future.get();
                mergeMethodCalls(methodCalls, result.methodCalls);
                mergeMethodCallMeta(methodCallMeta, result.methodCallMeta);
                mergeStrings(strMap, result.strMap);
                mergeList(controllers, result.controllers);
                mergeList(interceptors, result.interceptors);
                mergeList(servlets, result.servlets);
                mergeList(filters, result.filters);
                mergeList(listeners, result.listeners);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("class analysis interrupted");
            } catch (ExecutionException e) {
                logger.error("class analysis task error: {}", e.toString());
            }
        }
        pool.shutdown();
    }

    private static int resolveThreads(List<ClassFileEntity> files,
                                      Map<MethodReference.Handle, MethodReference> methodMap,
                                      Map<ClassReference.Handle, ClassReference> classMap,
                                      boolean analyzeStrings,
                                      boolean analyzeSpring,
                                      boolean analyzeWeb) {
        int classCount = files.size();
        String raw = System.getProperty(THREADS_PROP);
        if (raw != null && !raw.trim().isEmpty()) {
            try {
                int val = Integer.parseInt(raw.trim());
                if (val > 0) {
                    return Math.min(val, Math.max(1, classCount));
                }
            } catch (Exception ignored) {
            }
        }
        int cpu = Runtime.getRuntime().availableProcessors();
        if (cpu <= 1 || classCount < 200) {
            return 1;
        }
        if (classCount < cpu * 2) {
            return Math.max(1, classCount / 2);
        }
        int probeSize = Math.min(classCount, Math.max(PROBE_MIN_CLASSES, cpu * PROBE_CLASSES_PER_THREAD));
        if (probeSize < PROBE_MIN_CLASSES) {
            return Math.min(cpu, Math.max(1, classCount));
        }
        List<ClassFileEntity> probe = files.subList(0, probeSize);
        long serialMs = measureMs(() -> analyzeChunk(probe, methodMap, classMap,
                analyzeStrings, analyzeSpring, analyzeWeb));
        long parallelMs = measureMs(() -> runParallelProbe(probe, methodMap, classMap,
                analyzeStrings, analyzeSpring, analyzeWeb, cpu));
        if (parallelMs <= 0 || serialMs <= 0) {
            return Math.min(cpu, Math.max(1, classCount));
        }
        if (parallelMs < serialMs * PARALLEL_GAIN_THRESHOLD) {
            return Math.min(cpu, Math.max(1, classCount));
        }
        return Math.max(1, cpu / 2);
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

    private static long measureMs(Runnable runnable) {
        long start = System.nanoTime();
        runnable.run();
        return (System.nanoTime() - start) / 1_000_000L;
    }

    private static void mergeMethodCalls(HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> target,
                                         HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> src) {
        if (target == null || src == null || src.isEmpty()) {
            return;
        }
        for (Map.Entry<MethodReference.Handle, HashSet<MethodReference.Handle>> entry : src.entrySet()) {
            HashSet<MethodReference.Handle> existing = target.get(entry.getKey());
            if (existing == null) {
                target.put(entry.getKey(), new HashSet<>(entry.getValue()));
            } else {
                existing.addAll(entry.getValue());
            }
        }
    }

    private static void mergeMethodCallMeta(Map<MethodCallKey, MethodCallMeta> target,
                                            Map<MethodCallKey, MethodCallMeta> src) {
        if (target == null || src == null || src.isEmpty()) {
            return;
        }
        for (Map.Entry<MethodCallKey, MethodCallMeta> entry : src.entrySet()) {
            MethodCallMeta existing = target.get(entry.getKey());
            if (existing == null) {
                target.put(entry.getKey(), entry.getValue());
            } else {
                existing.mergeFrom(entry.getValue());
            }
        }
    }

    private static void mergeStrings(Map<MethodReference.Handle, List<String>> target,
                                     Map<MethodReference.Handle, List<String>> src) {
        if (target == null || src == null || src.isEmpty()) {
            return;
        }
        for (Map.Entry<MethodReference.Handle, List<String>> entry : src.entrySet()) {
            List<String> existing = target.get(entry.getKey());
            if (existing == null) {
                target.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            } else {
                existing.addAll(entry.getValue());
            }
        }
    }

    private static <T> void mergeList(List<T> target, List<T> src) {
        if (target == null || src == null || src.isEmpty()) {
            return;
        }
        target.addAll(src);
    }

    private static void runParallelProbe(List<ClassFileEntity> files,
                                         Map<MethodReference.Handle, MethodReference> methodMap,
                                         Map<ClassReference.Handle, ClassReference> classMap,
                                         boolean analyzeStrings,
                                         boolean analyzeSpring,
                                         boolean analyzeWeb,
                                         int threads) {
        List<List<ClassFileEntity>> partitions = partition(files, threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<LocalResult>> futures = new ArrayList<>();
        for (List<ClassFileEntity> chunk : partitions) {
            futures.add(pool.submit(new LocalTask(chunk, methodMap, classMap,
                    analyzeStrings, analyzeSpring, analyzeWeb)));
        }
        for (Future<LocalResult> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("class analysis probe interrupted");
            } catch (ExecutionException e) {
                logger.error("class analysis probe task error: {}", e.toString());
            }
        }
        pool.shutdown();
    }

    private static LocalResult analyzeChunk(List<ClassFileEntity> classFileList,
                                            Map<MethodReference.Handle, MethodReference> methodMap,
                                            Map<ClassReference.Handle, ClassReference> classMap,
                                            boolean analyzeStrings,
                                            boolean analyzeSpring,
                                            boolean analyzeWeb) {
        HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls = new HashMap<>();
        Map<MethodCallKey, MethodCallMeta> methodCallMeta = new HashMap<>();
        Map<MethodReference.Handle, List<String>> strMap = analyzeStrings ? new HashMap<>() : null;
        List<SpringController> controllers = analyzeSpring ? new ArrayList<>() : null;
        ArrayList<String> interceptors = analyzeWeb ? new ArrayList<>() : null;
        ArrayList<String> servlets = analyzeWeb ? new ArrayList<>() : null;
        ArrayList<String> filters = analyzeWeb ? new ArrayList<>() : null;
        ArrayList<String> listeners = analyzeWeb ? new ArrayList<>() : null;

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
                cr.accept(cn, Const.GlobalASMOptions);

                MethodCallClassVisitor mcv = new MethodCallClassVisitor(methodCalls, methodCallMeta, methodMap);
                cn.accept(mcv);

                ReflectionCallResolver.appendReflectionEdges(cn, methodCalls, methodMap, methodCallMeta, false);

                if (analyzeStrings && strMap != null) {
                    StringClassVisitor scv = new StringClassVisitor(strMap, classMap, methodMap);
                    cn.accept(scv);
                }
                if (analyzeSpring && controllers != null) {
                    SpringClassVisitor spv = new SpringClassVisitor(controllers, classMap, methodMap);
                    cn.accept(spv);
                }
                if (analyzeWeb && (interceptors != null || servlets != null
                        || filters != null || listeners != null)) {
                    JavaWebClassVisitor jcv = new JavaWebClassVisitor(interceptors, servlets, filters, listeners);
                    cn.accept(jcv);
                }
            } catch (Exception e) {
                logger.error("class analysis error: {}", e.toString());
            }
        }

        return new LocalResult(methodCalls, methodCallMeta, strMap,
                controllers, interceptors, servlets, filters, listeners);
    }

    private static final class LocalTask implements Callable<LocalResult> {
        private final List<ClassFileEntity> classFileList;
        private final Map<MethodReference.Handle, MethodReference> methodMap;
        private final Map<ClassReference.Handle, ClassReference> classMap;
        private final boolean analyzeStrings;
        private final boolean analyzeSpring;
        private final boolean analyzeWeb;

        private LocalTask(List<ClassFileEntity> classFileList,
                          Map<MethodReference.Handle, MethodReference> methodMap,
                          Map<ClassReference.Handle, ClassReference> classMap,
                          boolean analyzeStrings,
                          boolean analyzeSpring,
                          boolean analyzeWeb) {
            this.classFileList = classFileList;
            this.methodMap = methodMap;
            this.classMap = classMap;
            this.analyzeStrings = analyzeStrings;
            this.analyzeSpring = analyzeSpring;
            this.analyzeWeb = analyzeWeb;
        }

        @Override
        public LocalResult call() {
            return analyzeChunk(classFileList, methodMap, classMap, analyzeStrings, analyzeSpring, analyzeWeb);
        }
    }

    private static final class LocalResult {
        private final HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls;
        private final Map<MethodCallKey, MethodCallMeta> methodCallMeta;
        private final Map<MethodReference.Handle, List<String>> strMap;
        private final List<SpringController> controllers;
        private final ArrayList<String> interceptors;
        private final ArrayList<String> servlets;
        private final ArrayList<String> filters;
        private final ArrayList<String> listeners;

        private LocalResult(HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
                            Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                            Map<MethodReference.Handle, List<String>> strMap,
                            List<SpringController> controllers,
                            ArrayList<String> interceptors,
                            ArrayList<String> servlets,
                            ArrayList<String> filters,
                            ArrayList<String> listeners) {
            this.methodCalls = methodCalls;
            this.methodCallMeta = methodCallMeta;
            this.strMap = strMap;
            this.controllers = controllers;
            this.interceptors = interceptors;
            this.servlets = servlets;
            this.filters = filters;
            this.listeners = listeners;
        }
    }
}
