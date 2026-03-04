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
import me.n1ar4.jar.analyzer.core.asm.StringClassVisitor;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
                             Map<MethodReference.Handle, MethodReference> methodMap,
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
        if (!analyzeStrings && !analyzeSpring && !analyzeWeb) {
            logger.debug("class analysis skip: all analyzers disabled");
            return;
        }

        List<ClassFileEntity> files = new ArrayList<>(classFileList);
        int threads = resolveThreads(files);
        logger.info("class analysis threads: {}", threads);
        if (threads <= 1) {
            LocalResult result = analyzeChunk(files, methodMap, classMap,
                    analyzeStrings, analyzeSpring, analyzeWeb);
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

        boolean interrupted = false;
        Throwable asyncFailure = null;
        try {
            for (Future<LocalResult> future : futures) {
                try {
                    LocalResult result = future.get();
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
                    asyncFailure = e.getCause() == null ? e : e.getCause();
                    logger.error("class analysis task error", asyncFailure);
                    break;
                }
            }
        } finally {
            if (interrupted || asyncFailure != null) {
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
        if (asyncFailure != null) {
            throw new IllegalStateException("class analysis task failed", asyncFailure);
        }
    }

    private static int resolveThreads(List<ClassFileEntity> files) {
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
                                            boolean analyzeWeb) {
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
                ClassVisitor chain = new ClassVisitor(Const.ASMVersion) {
                };
                if (analyzeWeb) {
                    chain = new JavaWebClassVisitor(interceptors, servlets, filters, listeners, chain);
                }
                if (analyzeSpring) {
                    chain = new SpringClassVisitor(controllers, classMap, methodMap, file.getJarId(), chain);
                }
                if (analyzeStrings) {
                    chain = new StringClassVisitor(strMap, methodMap, file.getJarId(), chain);
                }
                cr.accept(chain, Const.GlobalASMOptions);
            } catch (Exception e) {
                throw new IllegalStateException("class analysis failed for class file: "
                        + safe(file.getClassName()), e);
            }
        }

        return new LocalResult(strMap,
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
            return analyzeChunk(classFileList, methodMap, classMap,
                    analyzeStrings, analyzeSpring, analyzeWeb);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class LocalResult {
        private final Map<MethodReference.Handle, List<String>> strMap;
        private final List<SpringController> controllers;
        private final ArrayList<String> interceptors;
        private final ArrayList<String> servlets;
        private final ArrayList<String> filters;
        private final ArrayList<String> listeners;

        private LocalResult(Map<MethodReference.Handle, List<String>> strMap,
                            List<SpringController> controllers,
                            ArrayList<String> interceptors,
                            ArrayList<String> servlets,
                            ArrayList<String> filters,
                            ArrayList<String> listeners) {
            this.strMap = strMap;
            this.controllers = controllers;
            this.interceptors = interceptors;
            this.servlets = servlets;
            this.filters = filters;
            this.listeners = listeners;
        }
    }
}
