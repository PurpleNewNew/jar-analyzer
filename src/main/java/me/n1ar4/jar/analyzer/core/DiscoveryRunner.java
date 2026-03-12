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

import me.n1ar4.jar.analyzer.core.asm.DiscoveryClassVisitor;
import me.n1ar4.jar.analyzer.core.asm.StringAnnoClassVisitor;
import me.n1ar4.jar.analyzer.core.bytecode.BuildBytecodeWorkspace;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.ArrayList;
import java.util.Comparator;
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

public class DiscoveryRunner {
    private static final Logger logger = LogManager.getLogger();
    private static final int MIN_CLASSES = 200;

    public static void start(BuildBytecodeWorkspace workspace,
                             Set<ClassReference> discoveredClasses,
                             Set<MethodReference> discoveredMethods,
                             Map<ClassReference.Handle, ClassReference> classMap,
                             Map<MethodReference.Handle, MethodReference> methodMap,
                             Map<MethodReference.Handle, List<String>> stringAnnoMap) {
        logger.info("start class analyze");
        if (workspace == null || workspace.parsedClasses().isEmpty()) {
            return;
        }
        List<BuildBytecodeWorkspace.ParsedClass> parsedClasses = new ArrayList<>(workspace.parsedClasses());
        int threads = resolveThreads(parsedClasses.size());
        if (threads <= 1) {
            LocalResult result = analyzeParsedClasses(parsedClasses);
            mergeInto(discoveredClasses, discoveredMethods, stringAnnoMap, result);
        } else {
            List<List<BuildBytecodeWorkspace.ParsedClass>> partitions = partitionParsedClasses(parsedClasses, threads);
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            List<Future<LocalResult>> futures = new ArrayList<>();
            boolean interrupted = false;
            Throwable asyncFailure = null;
            try {
                for (List<BuildBytecodeWorkspace.ParsedClass> chunk : partitions) {
                    futures.add(pool.submit(new ParsedLocalTask(chunk)));
                }
                for (Future<LocalResult> future : futures) {
                    try {
                        mergeInto(discoveredClasses, discoveredMethods, stringAnnoMap, future.get());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        interrupted = true;
                        logger.warn("discovery interrupted");
                        break;
                    } catch (ExecutionException e) {
                        Throwable cause = e.getCause() == null ? e : e.getCause();
                        asyncFailure = cause;
                        logger.error("discovery task error", cause);
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
                throw new IllegalStateException("discovery task failed", asyncFailure);
            }
        }
        mergeDiscoveredRows(discoveredClasses, discoveredMethods, classMap, methodMap);
        logger.info("string annotation analyze merged");
    }

    private static void mergeDiscoveredRows(Set<ClassReference> discoveredClasses,
                                            Set<MethodReference> discoveredMethods,
                                            Map<ClassReference.Handle, ClassReference> classMap,
                                            Map<MethodReference.Handle, MethodReference> methodMap) {
        List<ClassReference> classRows = new ArrayList<>(discoveredClasses);
        classRows.sort(Comparator
                .comparing((ClassReference row) -> safe(row == null ? null : row.getName()))
                .thenComparingInt(row -> normalizeJarId(row == null ? null : row.getJarId())));
        for (ClassReference clazz : classRows) {
            if (clazz == null || clazz.getName() == null || clazz.getName().isBlank()) {
                continue;
            }
            classMap.put(clazz.getHandle(), clazz);
        }
        List<MethodReference> methodRows = new ArrayList<>(discoveredMethods);
        methodRows.sort(Comparator
                .comparing((MethodReference row) -> safe(className(row)))
                .thenComparing(row -> safe(row == null ? null : row.getName()))
                .thenComparing(row -> safe(row == null ? null : row.getDesc()))
                .thenComparingInt(row -> normalizeJarId(row == null ? null : row.getJarId())));
        for (MethodReference method : methodRows) {
            if (method == null || method.getClassReference() == null) {
                continue;
            }
            methodMap.put(method.getHandle(), method);
        }
    }

    private static int resolveThreads(int classCount) {
        int cpu = Runtime.getRuntime().availableProcessors();
        if (cpu <= 1 || classCount < MIN_CLASSES) {
            return 1;
        }
        return Math.min(cpu, Math.max(1, classCount / MIN_CLASSES));
    }

    private static List<List<BuildBytecodeWorkspace.ParsedClass>> partitionParsedClasses(
            List<BuildBytecodeWorkspace.ParsedClass> items,
            int parts) {
        if (parts <= 1) {
            return Collections.singletonList(items);
        }
        List<List<BuildBytecodeWorkspace.ParsedClass>> buckets = new ArrayList<>();
        for (int i = 0; i < parts; i++) {
            buckets.add(new ArrayList<>());
        }
        for (int i = 0; i < items.size(); i++) {
            buckets.get(i % parts).add(items.get(i));
        }
        return buckets;
    }

    private static void mergeInto(Set<ClassReference> discoveredClasses,
                                  Set<MethodReference> discoveredMethods,
                                  Map<MethodReference.Handle, List<String>> stringAnnoMap,
                                  LocalResult result) {
        if (result == null) {
            return;
        }
        discoveredClasses.addAll(result.discoveredClasses);
        discoveredMethods.addAll(result.discoveredMethods);
        if (stringAnnoMap != null && result.stringAnnoMap != null) {
            for (Map.Entry<MethodReference.Handle, List<String>> entry : result.stringAnnoMap.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                    continue;
                }
                List<String> existing = stringAnnoMap.get(entry.getKey());
                if (existing == null) {
                    stringAnnoMap.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                } else {
                    existing.addAll(entry.getValue());
                }
            }
        }
    }

    private static LocalResult analyzeParsedClasses(List<BuildBytecodeWorkspace.ParsedClass> parsedClasses) {
        Set<ClassReference> discoveredClasses = new HashSet<>();
        Set<MethodReference> discoveredMethods = new HashSet<>();
        Map<MethodReference.Handle, List<String>> stringAnnoMap = new HashMap<>();
        for (BuildBytecodeWorkspace.ParsedClass parsedClass : parsedClasses) {
            if (parsedClass == null || parsedClass.classNode() == null) {
                continue;
            }
            try {
                StringAnnoClassVisitor sav = new StringAnnoClassVisitor(stringAnnoMap, parsedClass.jarId());
                DiscoveryClassVisitor dcv = new DiscoveryClassVisitor(
                        discoveredClasses,
                        discoveredMethods,
                        parsedClass.file() == null ? null : parsedClass.file().getJarName(),
                        parsedClass.jarId(),
                        sav
                );
                parsedClass.classNode().accept(dcv);
            } catch (Exception ex) {
                throw new IllegalStateException("discovery failed for parsed class: "
                        + safe(parsedClass.className()), ex);
            }
        }
        return new LocalResult(discoveredClasses, discoveredMethods, stringAnnoMap);
    }

    private static int normalizeJarId(Integer jarId) {
        if (jarId == null || jarId < 0) {
            return Integer.MAX_VALUE;
        }
        return jarId;
    }

    private static String className(MethodReference method) {
        if (method == null || method.getClassReference() == null) {
            return "";
        }
        return method.getClassReference().getName();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class ParsedLocalTask implements Callable<LocalResult> {
        private final List<BuildBytecodeWorkspace.ParsedClass> parsedClasses;

        private ParsedLocalTask(List<BuildBytecodeWorkspace.ParsedClass> parsedClasses) {
            this.parsedClasses = parsedClasses;
        }

        @Override
        public LocalResult call() {
            return analyzeParsedClasses(parsedClasses);
        }
    }

    private static final class LocalResult {
        private final Set<ClassReference> discoveredClasses;
        private final Set<MethodReference> discoveredMethods;
        private final Map<MethodReference.Handle, List<String>> stringAnnoMap;

        private LocalResult(Set<ClassReference> discoveredClasses,
                            Set<MethodReference> discoveredMethods,
                            Map<MethodReference.Handle, List<String>> stringAnnoMap) {
            this.discoveredClasses = discoveredClasses;
            this.discoveredMethods = discoveredMethods;
            this.stringAnnoMap = stringAnnoMap;
        }
    }
}
