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
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassReader;

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

public class DiscoveryRunner {
    private static final Logger logger = LogManager.getLogger();
    private static final String THREADS_PROP = "jar.analyzer.discovery.threads";
    private static final int MIN_CLASSES = 200;

    public static void start(Set<ClassFileEntity> classFileList,
                             Set<ClassReference> discoveredClasses,
                             Set<MethodReference> discoveredMethods,
                             Map<ClassReference.Handle, ClassReference> classMap,
                             Map<MethodReference.Handle, MethodReference> methodMap,
                             Map<MethodReference.Handle, List<String>> stringAnnoMap) {
        logger.info("start class analyze");
        if (classFileList == null || classFileList.isEmpty()) {
            return;
        }
        List<ClassFileEntity> files = new ArrayList<>(classFileList);
        int threads = resolveThreads(files.size());
        if (threads <= 1) {
            LocalResult result = analyzeChunk(files);
            mergeInto(discoveredClasses, discoveredMethods, stringAnnoMap, result);
        } else {
            List<List<ClassFileEntity>> partitions = partition(files, threads);
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            List<Future<LocalResult>> futures = new ArrayList<>();
            for (List<ClassFileEntity> chunk : partitions) {
                futures.add(pool.submit(new LocalTask(chunk)));
            }
            boolean interrupted = false;
            try {
                for (Future<LocalResult> future : futures) {
                    try {
                        mergeInto(discoveredClasses, discoveredMethods, stringAnnoMap, future.get());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        interrupted = true;
                        logger.warn("discovery interrupted");
                        break;
                    } catch (ExecutionException e) {
                        Throwable cause = e.getCause();
                        logger.error("discovery task error: {}", cause == null ? e.toString() : cause.toString());
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
        for (ClassReference clazz : discoveredClasses) {
            classMap.put(clazz.getHandle(), clazz);
        }
        for (MethodReference method : discoveredMethods) {
            methodMap.put(method.getHandle(), method);
        }
        logger.info("string annotation analyze merged");
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
                logger.debug("invalid int property {}={}", THREADS_PROP, raw);
            }
        }
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

    private static LocalResult analyzeChunk(List<ClassFileEntity> classFileList) {
        Set<ClassReference> discoveredClasses = new HashSet<>();
        Set<MethodReference> discoveredMethods = new HashSet<>();
        Map<MethodReference.Handle, List<String>> stringAnnoMap = new HashMap<>();
        for (ClassFileEntity file : classFileList) {
            try {
                byte[] bytes = file.getFile();
                if (bytes == null || bytes.length == 0) {
                    continue;
                }
                ClassReader cr = new ClassReader(bytes);
                StringAnnoClassVisitor sav = new StringAnnoClassVisitor(stringAnnoMap);
                DiscoveryClassVisitor dcv = new DiscoveryClassVisitor(discoveredClasses,
                        discoveredMethods, file.getJarName(), file.getJarId(), sav);
                cr.accept(dcv, Const.DiscoveryASMOptions);
            } catch (Exception e) {
                logger.error("discovery error: {}", e.toString());
            }
        }
        return new LocalResult(discoveredClasses, discoveredMethods, stringAnnoMap);
    }

    private static final class LocalTask implements Callable<LocalResult> {
        private final List<ClassFileEntity> classFileList;

        private LocalTask(List<ClassFileEntity> classFileList) {
            this.classFileList = classFileList;
        }

        @Override
        public LocalResult call() {
            return analyzeChunk(classFileList);
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
