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
import me.n1ar4.jar.analyzer.core.asm.ReflectionProbeClassVisitor;
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
import java.util.concurrent.TimeUnit;

public final class ClassAnalysisRunner {
    private static final Logger logger = LogManager.getLogger();
    private static final String THREADS_PROP = "jar.analyzer.class.analysis.threads";

    private ClassAnalysisRunner() {
    }

    public static void start(Set<ClassFileEntity> classFileList,
                             HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
                             Map<MethodReference.Handle, MethodReference> methodMap,
                             Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                             Set<ClassReference.Handle> instantiatedClasses,
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
                    analyzeStrings, analyzeSpring, analyzeWeb, instantiatedClasses != null);
            mergeMethodCalls(methodCalls, result.methodCalls);
            mergeMethodCallMeta(methodCallMeta, result.methodCallMeta);
            mergeInstantiated(instantiatedClasses, result.instantiatedClasses);
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
                    analyzeStrings, analyzeSpring, analyzeWeb, instantiatedClasses != null)));
        }
        boolean interrupted = false;
        try {
            for (Future<LocalResult> future : futures) {
                try {
                    LocalResult result = future.get();
                    mergeMethodCalls(methodCalls, result.methodCalls);
                    mergeMethodCallMeta(methodCallMeta, result.methodCallMeta);
                    mergeInstantiated(instantiatedClasses, result.instantiatedClasses);
                    mergeStrings(strMap, result.strMap);
                    mergeList(controllers, result.controllers);
                    mergeList(interceptors, result.interceptors);
                    mergeList(servlets, result.servlets);
                    mergeList(filters, result.filters);
                    mergeList(listeners, result.listeners);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    interrupted = true;
                    logger.warn("class analysis interrupted");
                    break;
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    logger.error("class analysis task error: {}", cause == null ? e.toString() : cause.toString());
                }
            }
        } finally {
            if (interrupted) {
                for (Future<LocalResult> future : futures) {
                    future.cancel(true);
                }
                pool.shutdownNow();
            } else {
                pool.shutdown();
            }
            try {
                pool.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
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
            } catch (NumberFormatException ex) {
                logger.debug("invalid int property {}={}", THREADS_PROP, raw);
            }
        }
        int cpu = Runtime.getRuntime().availableProcessors();
        if (cpu <= 1 || classCount < 200) {
            return 1;
        }
        if (classCount < cpu * 2) {
            return Math.max(1, classCount / 2);
        }
        return Math.min(cpu, Math.max(1, classCount));
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

    private static void mergeInstantiated(Set<ClassReference.Handle> target,
                                          Set<ClassReference.Handle> src) {
        if (target == null || src == null || src.isEmpty()) {
            return;
        }
        target.addAll(src);
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

    private static LocalResult analyzeChunk(List<ClassFileEntity> classFileList,
                                            Map<MethodReference.Handle, MethodReference> methodMap,
                                            Map<ClassReference.Handle, ClassReference> classMap,
                                            boolean analyzeStrings,
                                            boolean analyzeSpring,
                                            boolean analyzeWeb,
                                            boolean collectInstantiated) {
        HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls = new HashMap<>();
        Map<MethodCallKey, MethodCallMeta> methodCallMeta = new HashMap<>();
        Set<ClassReference.Handle> instantiatedClasses = collectInstantiated ? new HashSet<>() : null;
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
                ReflectionProbeClassVisitor probe = new ReflectionProbeClassVisitor();
                org.objectweb.asm.ClassVisitor chain = probe;
                if (analyzeWeb && (interceptors != null || servlets != null
                        || filters != null || listeners != null)) {
                    chain = new JavaWebClassVisitor(interceptors, servlets, filters, listeners, chain);
                }
                if (analyzeSpring && controllers != null) {
                    chain = new SpringClassVisitor(controllers, classMap, methodMap, chain);
                }
                if (analyzeStrings && strMap != null) {
                    chain = new StringClassVisitor(strMap, classMap, methodMap, chain);
                }
                chain = new MethodCallClassVisitor(methodCalls, methodCallMeta, methodMap,
                        instantiatedClasses, chain, file.getJarId());
                cr.accept(chain, Const.GlobalASMOptions);

                if (probe.hasReflection()) {
                    ClassNode cn = new ClassNode();
                    cr.accept(cn, Const.GlobalASMOptions);
                    ReflectionCallResolver.appendReflectionEdges(cn, methodCalls, methodMap, methodCallMeta, false);
                }
            } catch (Exception e) {
                logger.error("class analysis error: {}", e.toString());
            }
        }

        return new LocalResult(methodCalls, methodCallMeta, instantiatedClasses, strMap,
                controllers, interceptors, servlets, filters, listeners);
    }

    private static final class LocalTask implements Callable<LocalResult> {
        private final List<ClassFileEntity> classFileList;
        private final Map<MethodReference.Handle, MethodReference> methodMap;
        private final Map<ClassReference.Handle, ClassReference> classMap;
        private final boolean analyzeStrings;
        private final boolean analyzeSpring;
        private final boolean analyzeWeb;
        private final boolean collectInstantiated;

        private LocalTask(List<ClassFileEntity> classFileList,
                          Map<MethodReference.Handle, MethodReference> methodMap,
                          Map<ClassReference.Handle, ClassReference> classMap,
                          boolean analyzeStrings,
                          boolean analyzeSpring,
                          boolean analyzeWeb,
                          boolean collectInstantiated) {
            this.classFileList = classFileList;
            this.methodMap = methodMap;
            this.classMap = classMap;
            this.analyzeStrings = analyzeStrings;
            this.analyzeSpring = analyzeSpring;
            this.analyzeWeb = analyzeWeb;
            this.collectInstantiated = collectInstantiated;
        }

        @Override
        public LocalResult call() {
            return analyzeChunk(classFileList, methodMap, classMap,
                    analyzeStrings, analyzeSpring, analyzeWeb, collectInstantiated);
        }
    }

    private static final class LocalResult {
        private final HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls;
        private final Map<MethodCallKey, MethodCallMeta> methodCallMeta;
        private final Set<ClassReference.Handle> instantiatedClasses;
        private final Map<MethodReference.Handle, List<String>> strMap;
        private final List<SpringController> controllers;
        private final ArrayList<String> interceptors;
        private final ArrayList<String> servlets;
        private final ArrayList<String> filters;
        private final ArrayList<String> listeners;

        private LocalResult(HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
                            Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                            Set<ClassReference.Handle> instantiatedClasses,
                            Map<MethodReference.Handle, List<String>> strMap,
                            List<SpringController> controllers,
                            ArrayList<String> interceptors,
                            ArrayList<String> servlets,
                            ArrayList<String> filters,
                            ArrayList<String> listeners) {
            this.methodCalls = methodCalls;
            this.methodCallMeta = methodCallMeta;
            this.instantiatedClasses = instantiatedClasses;
            this.strMap = strMap;
            this.controllers = controllers;
            this.interceptors = interceptors;
            this.servlets = servlets;
            this.filters = filters;
            this.listeners = listeners;
        }
    }
}
