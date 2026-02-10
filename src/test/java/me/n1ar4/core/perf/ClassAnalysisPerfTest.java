/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.core.perf;

import me.n1ar4.jar.analyzer.analyze.spring.SpringController;
import me.n1ar4.jar.analyzer.core.ClassAnalysisRunner;
import me.n1ar4.jar.analyzer.core.DiscoveryRunner;
import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.utils.BytecodeCache;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassAnalysisPerfTest {
    private static final String JAR_PROP = "perf.jar";
    private static final String JAR_PROP_ALT = "perfJar";
    private static final String RT_PROP = "perf.rt";
    private static final String RT_PROP_ALT = "perfRt";
    private static final String DEFAULT_RT = "test/rt.jar";
    private static final String ANALYSIS_THREADS_PROP = "jar.analyzer.class.analysis.threads";
    private static final String THREADS_PROP = "perf.threads";

    @Test
    @SuppressWarnings("all")
    public void compareSerialParallel() throws Exception {
        String jarPathRaw = System.getProperty(JAR_PROP);
        if (jarPathRaw == null || jarPathRaw.trim().isEmpty()) {
            jarPathRaw = System.getProperty(JAR_PROP_ALT);
        }
        Assumptions.assumeTrue(jarPathRaw != null && !jarPathRaw.trim().isEmpty(),
                "set -D" + JAR_PROP_ALT + "=path_to_jar");
        Path jarPath = Paths.get(jarPathRaw);
        Assumptions.assumeTrue(Files.exists(jarPath), "jar not exist: " + jarPath);

        List<Path> jars = new ArrayList<>();
        jars.add(jarPath);
        Path rtPath = resolveRtPath();
        if (rtPath != null && Files.exists(rtPath)) {
            jars.add(rtPath);
        }
        Path tempDir = Paths.get("target", "perf-temp");
        List<ClassFileEntity> classFiles = extractClasses(jars, tempDir);
        System.out.println("class files: " + classFiles.size() + " (jars=" + jars.size() + ")");

        Set<ClassReference> discoveredClasses = new HashSet<>();
        Set<MethodReference> discoveredMethods = new HashSet<>();
        Map<ClassReference.Handle, ClassReference> classMap = new HashMap<>();
        Map<MethodReference.Handle, MethodReference> methodMap = new HashMap<>();
        Map<MethodReference.Handle, List<String>> stringAnnoMap = new HashMap<>();

        long discoveryMs = measureMs(() -> DiscoveryRunner.start(new HashSet<>(classFiles),
                discoveredClasses, discoveredMethods, classMap, methodMap, stringAnnoMap));
        System.out.println("discovery ms: " + discoveryMs);

        BytecodeCache.clear();
        PerfResult serial = runSerial(classFiles, methodMap, classMap);
        System.out.println(serial.format("serial"));

        BytecodeCache.clear();
        int threads = resolveThreads(classFiles.size());
        PerfResult parallel = runParallel(classFiles, methodMap, classMap, threads);
        System.out.println(parallel.format("parallel"));

        printEdgeDiff(serial.methodCalls, parallel.methodCalls, 50);
    }

    private static PerfResult runSerial(List<ClassFileEntity> classFiles,
                                        Map<MethodReference.Handle, MethodReference> methodMap,
                                        Map<ClassReference.Handle, ClassReference> classMap) {
        HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls = new HashMap<>();
        Map<MethodCallKey, MethodCallMeta> methodCallMeta = new HashMap<>();
        Map<MethodReference.Handle, List<String>> strMap = new HashMap<>();
        List<SpringController> controllers = new ArrayList<>();
        ArrayList<String> interceptors = new ArrayList<>();
        ArrayList<String> servlets = new ArrayList<>();
        ArrayList<String> filters = new ArrayList<>();
        ArrayList<String> listeners = new ArrayList<>();

        String prev = System.getProperty(ANALYSIS_THREADS_PROP);
        System.setProperty(ANALYSIS_THREADS_PROP, "1");
        long ms = measureMs(() -> ClassAnalysisRunner.start(new HashSet<>(classFiles),
                methodCalls,
                methodMap,
                methodCallMeta,
                new HashSet<>(),
                strMap,
                classMap,
                controllers,
                interceptors,
                servlets,
                filters,
                listeners,
                true,
                true,
                true));
        restoreProperty(ANALYSIS_THREADS_PROP, prev);

        return new PerfResult(ms, methodCalls, methodCallMeta, strMap,
                controllers, interceptors, servlets, filters, listeners);
    }

    private static PerfResult runParallel(List<ClassFileEntity> classFiles,
                                          Map<MethodReference.Handle, MethodReference> methodMap,
                                          Map<ClassReference.Handle, ClassReference> classMap,
                                          int threads) {
        HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls = new HashMap<>();
        Map<MethodCallKey, MethodCallMeta> methodCallMeta = new HashMap<>();
        Map<MethodReference.Handle, List<String>> strMap = new HashMap<>();
        List<SpringController> controllers = new ArrayList<>();
        ArrayList<String> interceptors = new ArrayList<>();
        ArrayList<String> servlets = new ArrayList<>();
        ArrayList<String> filters = new ArrayList<>();
        ArrayList<String> listeners = new ArrayList<>();

        String prev = System.getProperty(ANALYSIS_THREADS_PROP);
        System.setProperty(ANALYSIS_THREADS_PROP, String.valueOf(threads));
        long ms = measureMs(() -> ClassAnalysisRunner.start(new HashSet<>(classFiles),
                methodCalls,
                methodMap,
                methodCallMeta,
                new HashSet<>(),
                strMap,
                classMap,
                controllers,
                interceptors,
                servlets,
                filters,
                listeners,
                true,
                true,
                true));
        restoreProperty(ANALYSIS_THREADS_PROP, prev);

        return new PerfResult(ms, methodCalls, methodCallMeta, strMap,
                controllers, interceptors, servlets, filters, listeners);
    }

    private static int resolveThreads(int classCount) {
        int cpu = Runtime.getRuntime().availableProcessors();
        String raw = System.getProperty(THREADS_PROP);
        if (raw != null) {
            try {
                int val = Integer.parseInt(raw.trim());
                if (val > 0) {
                    return Math.min(val, Math.max(1, classCount));
                }
            } catch (Exception ignored) {
            }
        }
        return Math.min(cpu, Math.max(1, classCount));
    }

    private static long measureMs(Runnable r) {
        long start = System.nanoTime();
        r.run();
        return (System.nanoTime() - start) / 1_000_000L;
    }

    private static List<ClassFileEntity> extractClasses(List<Path> jarPaths, Path tempDir) throws IOException {
        recreateDir(tempDir);
        List<ClassFileEntity> classFiles = new ArrayList<>();
        int jarId = 1;
        for (Path jarPath : jarPaths) {
            String jarName = jarPath.getFileName().toString();
            try (JarFile jarFile = new JarFile(jarPath.toFile())) {
                for (JarEntry entry : Collections.list(jarFile.entries())) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    String name = entry.getName();
                    if (!name.endsWith(".class")) {
                        continue;
                    }
                    Path out = tempDir.resolve(jarName).resolve(name);
                    Path parent = out.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    try (InputStream in = jarFile.getInputStream(entry)) {
                        Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                    }
                    ClassFileEntity entity = new ClassFileEntity();
                    entity.setClassName(name);
                    entity.setPath(out);
                    entity.setJarName(jarName);
                    entity.setJarId(jarId);
                    classFiles.add(entity);
                }
            }
            jarId++;
        }
        return classFiles;
    }

    private static void recreateDir(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        Files.createDirectories(dir);
    }

    private static Path resolveRtPath() {
        String rtRaw = System.getProperty(RT_PROP);
        if (rtRaw == null || rtRaw.trim().isEmpty()) {
            rtRaw = System.getProperty(RT_PROP_ALT);
        }
        if (rtRaw != null && !rtRaw.trim().isEmpty()) {
            return Paths.get(rtRaw);
        }
        Path fallback = Paths.get(DEFAULT_RT);
        if (Files.exists(fallback)) {
            return fallback;
        }
        return null;
    }

    private static void restoreProperty(String key, String previous) {
        if (previous == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, previous);
        }
    }

    private static final class PerfResult {
        private final long ms;
        private final HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls;
        private final Map<MethodCallKey, MethodCallMeta> methodCallMeta;
        private final Map<MethodReference.Handle, List<String>> strMap;
        private final List<SpringController> controllers;
        private final ArrayList<String> interceptors;
        private final ArrayList<String> servlets;
        private final ArrayList<String> filters;
        private final ArrayList<String> listeners;

        private PerfResult(long ms,
                           HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
                           Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                           Map<MethodReference.Handle, List<String>> strMap,
                           List<SpringController> controllers,
                           ArrayList<String> interceptors,
                           ArrayList<String> servlets,
                           ArrayList<String> filters,
                           ArrayList<String> listeners) {
            this.ms = ms;
            this.methodCalls = methodCalls;
            this.methodCallMeta = methodCallMeta;
            this.strMap = strMap;
            this.controllers = controllers;
            this.interceptors = interceptors;
            this.servlets = servlets;
            this.filters = filters;
            this.listeners = listeners;
        }

        private String format(String label) {
            return label + " ms: " + ms +
                    ", edges: " + countEdges(methodCalls) +
                    ", meta: " + methodCallMeta.size() +
                    ", strings: " + countStrings(strMap) +
                    ", controllers: " + controllers.size() +
                    ", web: " + (interceptors.size() + servlets.size() + filters.size() + listeners.size());
        }

        private static int countEdges(HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls) {
            int total = 0;
            for (HashSet<MethodReference.Handle> value : methodCalls.values()) {
                total += value.size();
            }
            return total;
        }

        private static int countStrings(Map<MethodReference.Handle, List<String>> strMap) {
            int total = 0;
            for (List<String> value : strMap.values()) {
                total += value.size();
            }
            return total;
        }
    }

    private static void printEdgeDiff(HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> serial,
                                      HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> parallel,
                                      int limit) {
        Set<String> serialEdges = toEdgeSet(serial);
        Set<String> parallelEdges = toEdgeSet(parallel);
        List<String> missingInParallel = new ArrayList<>();
        for (String edge : serialEdges) {
            if (!parallelEdges.contains(edge)) {
                missingInParallel.add(edge);
            }
        }
        List<String> extraInParallel = new ArrayList<>();
        for (String edge : parallelEdges) {
            if (!serialEdges.contains(edge)) {
                extraInParallel.add(edge);
            }
        }
        Collections.sort(missingInParallel);
        Collections.sort(extraInParallel);
        System.out.println("diff missingInParallel: " + missingInParallel.size());
        System.out.println("diff extraInParallel: " + extraInParallel.size());
        if (!missingInParallel.isEmpty()) {
            System.out.println("missingInParallel samples:");
            for (int i = 0; i < Math.min(limit, missingInParallel.size()); i++) {
                System.out.println(missingInParallel.get(i));
            }
        }
        if (!extraInParallel.isEmpty()) {
            System.out.println("extraInParallel samples:");
            for (int i = 0; i < Math.min(limit, extraInParallel.size()); i++) {
                System.out.println(extraInParallel.get(i));
            }
        }
    }

    private static Set<String> toEdgeSet(HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls) {
        Set<String> edges = new HashSet<>();
        for (Map.Entry<MethodReference.Handle, HashSet<MethodReference.Handle>> entry : methodCalls.entrySet()) {
            MethodReference.Handle caller = entry.getKey();
            if (caller == null) {
                continue;
            }
            String callerKey = formatMethod(caller);
            for (MethodReference.Handle callee : entry.getValue()) {
                if (callee == null) {
                    continue;
                }
                edges.add(callerKey + " -> " + formatMethod(callee));
            }
        }
        return edges;
    }

    private static String formatMethod(MethodReference.Handle handle) {
        String owner = handle.getClassReference() == null ? "?" : handle.getClassReference().getName();
        return owner + "." + handle.getName() + handle.getDesc();
    }
}
