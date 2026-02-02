/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.utils;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ExternalSymbolResolver {
    private static final Logger logger = LogManager.getLogger();
    private static final Map<String, ExternalClassInfo> CACHE = new ConcurrentHashMap<>();
    private static volatile long lastRootSeq = -1;
    private static volatile long lastBuildSeq = -1;

    private ExternalSymbolResolver() {
    }

    public static List<MethodResult> resolveMethods(String className) {
        ExternalClassInfo info = resolveInfo(className);
        if (info == null) {
            return Collections.emptyList();
        }
        return info.methods;
    }

    public static List<MethodResult> resolveCallees(String className,
                                                    String methodName,
                                                    String methodDesc) {
        ExternalClassInfo info = resolveInfo(className);
        if (info == null || methodName == null || methodDesc == null) {
            return Collections.emptyList();
        }
        String key = methodKey(methodName, methodDesc);
        List<MethodResult> cached = info.calleesCache.get(key);
        if (cached != null) {
            return cached;
        }
        List<MethodResult> resolved = buildCallees(info, methodName, methodDesc);
        info.calleesCache.put(key, resolved);
        return resolved;
    }

    public static List<MethodResult> resolveCallersInClass(String className,
                                                           String methodName,
                                                           String methodDesc) {
        ExternalClassInfo info = resolveInfo(className);
        if (info == null || methodName == null || methodDesc == null) {
            return Collections.emptyList();
        }
        String key = methodKey(methodName, methodDesc);
        List<MethodResult> cached = info.callersCache.get(key);
        if (cached != null) {
            return cached;
        }
        List<MethodResult> resolved = buildCallers(info, methodName, methodDesc);
        info.callersCache.put(key, resolved);
        return resolved;
    }

    public static boolean isExternalClass(String className) {
        if (className == null || className.trim().isEmpty()) {
            return false;
        }
        CoreEngine engine = MainForm.getEngine();
        if (engine == null) {
            return true;
        }
        String normalized = normalizeClassName(className);
        String path = engine.getAbsPath(normalized);
        return path == null || path.trim().isEmpty() || !Files.exists(java.nio.file.Paths.get(path));
    }

    private static ExternalClassInfo resolveInfo(String className) {
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        String normalized = normalizeClassName(className);
        if (normalized.isEmpty() || CommonFilterUtil.isModuleInfoClassName(normalized)) {
            return null;
        }
        ensureCacheValid();
        ExternalClassInfo cached = CACHE.get(normalized);
        if (cached != null) {
            return cached;
        }
        RuntimeClassResolver.ResolvedClass resolved = RuntimeClassResolver.resolve(normalized);
        if (resolved == null || resolved.getClassFile() == null) {
            return null;
        }
        Path classPath = resolved.getClassFile();
        if (classPath == null || !Files.exists(classPath)) {
            return null;
        }
        ClassNode node = new ClassNode();
        try {
            ClassReader reader = new ClassReader(Files.readAllBytes(classPath));
            reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } catch (Exception ex) {
            logger.debug("external class parse failed: {}", ex.toString());
            return null;
        }
        String jarName = resolveJarName(normalized, resolved);
        ExternalClassInfo info = buildInfo(normalized, classPath, jarName, node);
        CACHE.put(normalized, info);
        return info;
    }

    private static ExternalClassInfo buildInfo(String className,
                                               Path classPath,
                                               String jarName,
                                               ClassNode node) {
        List<MethodResult> methods = new ArrayList<>();
        Map<String, MethodNode> methodNodes = new LinkedHashMap<>();
        Map<String, MethodResult> methodResults = new LinkedHashMap<>();
        if (node != null && node.methods != null) {
            for (MethodNode method : node.methods) {
                if (method == null) {
                    continue;
                }
                MethodResult result = new MethodResult();
                result.setClassName(className);
                result.setMethodName(method.name);
                result.setMethodDesc(method.desc);
                result.setAccessInt(method.access);
                result.setIsStaticInt((method.access & Opcodes.ACC_STATIC) != 0 ? 1 : 0);
                result.setJarName(jarName);
                result.setJarId(-1);
                result.setClassPath(classPath);
                methods.add(result);
                String key = methodKey(method.name, method.desc);
                methodNodes.put(key, method);
                methodResults.put(key, result);
            }
        }
        methods.sort((a, b) -> {
            String na = a == null ? "" : a.getMethodName();
            String nb = b == null ? "" : b.getMethodName();
            return na.compareToIgnoreCase(nb);
        });
        return new ExternalClassInfo(className, classPath, jarName, methods, methodNodes, methodResults);
    }

    private static List<MethodResult> buildCallees(ExternalClassInfo info,
                                                   String methodName,
                                                   String methodDesc) {
        MethodNode node = info.methodNodes.get(methodKey(methodName, methodDesc));
        if (node == null || node.instructions == null) {
            return Collections.emptyList();
        }
        Map<String, MethodResult> out = new LinkedHashMap<>();
        for (AbstractInsnNode insn = node.instructions.getFirst();
             insn != null;
             insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode call = (MethodInsnNode) insn;
            String owner = call.owner;
            if (owner == null || owner.isEmpty()) {
                continue;
            }
            String key = methodKey(owner, call.name, call.desc);
            if (out.containsKey(key)) {
                continue;
            }
            MethodResult result = new MethodResult();
            result.setClassName(owner);
            result.setMethodName(call.name);
            result.setMethodDesc(call.desc);
            result.setJarName(resolveJarName(owner, null));
            result.setJarId(-1);
            out.put(key, result);
        }
        return new ArrayList<>(out.values());
    }

    private static List<MethodResult> buildCallers(ExternalClassInfo info,
                                                   String methodName,
                                                   String methodDesc) {
        if (info.methodNodes.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, MethodResult> out = new LinkedHashMap<>();
        String targetOwner = info.className;
        for (Map.Entry<String, MethodNode> entry : info.methodNodes.entrySet()) {
            MethodNode node = entry.getValue();
            if (node == null || node.instructions == null) {
                continue;
            }
            boolean matched = false;
            for (AbstractInsnNode insn = node.instructions.getFirst();
                 insn != null;
                 insn = insn.getNext()) {
                if (!(insn instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) insn;
                if (targetOwner.equals(call.owner)
                        && methodName.equals(call.name)
                        && methodDesc.equals(call.desc)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                continue;
            }
            MethodResult result = info.methodResults.get(entry.getKey());
            if (result == null) {
                continue;
            }
            String key = methodKey(result.getMethodName(), result.getMethodDesc());
            out.putIfAbsent(key, result);
        }
        return new ArrayList<>(out.values());
    }

    private static void ensureCacheValid() {
        long rootSeq = RuntimeClassResolver.getRootSeq();
        long buildSeq = DatabaseManager.getBuildSeq();
        if (rootSeq == lastRootSeq && buildSeq == lastBuildSeq) {
            return;
        }
        synchronized (ExternalSymbolResolver.class) {
            if (rootSeq == lastRootSeq && buildSeq == lastBuildSeq) {
                return;
            }
            CACHE.clear();
            lastRootSeq = rootSeq;
            lastBuildSeq = buildSeq;
        }
    }

    private static String resolveJarName(String className, RuntimeClassResolver.ResolvedClass resolved) {
        CoreEngine engine = MainForm.getEngine();
        String normalized = normalizeClassName(className);
        if (engine != null) {
            String jar = engine.getJarByClass(normalized);
            if (jar != null && !jar.trim().isEmpty()) {
                return jar;
            }
        }
        if (resolved != null && resolved.getJarName() != null) {
            return resolved.getJarName();
        }
        return RuntimeClassResolver.getJarName(normalized);
    }

    private static String normalizeClassName(String className) {
        String normalized = className.trim();
        if (normalized.endsWith(".class")) {
            normalized = normalized.substring(0, normalized.length() - 6);
        }
        normalized = normalized.replace('.', '/');
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static String methodKey(String name, String desc) {
        return name + desc;
    }

    private static String methodKey(String owner, String name, String desc) {
        return owner + "#" + name + desc;
    }

    private static final class ExternalClassInfo {
        private final String className;
        private final Path classPath;
        private final String jarName;
        private final List<MethodResult> methods;
        private final Map<String, MethodNode> methodNodes;
        private final Map<String, MethodResult> methodResults;
        private final Map<String, List<MethodResult>> calleesCache = new ConcurrentHashMap<>();
        private final Map<String, List<MethodResult>> callersCache = new ConcurrentHashMap<>();

        private ExternalClassInfo(String className,
                                  Path classPath,
                                  String jarName,
                                  List<MethodResult> methods,
                                  Map<String, MethodNode> methodNodes,
                                  Map<String, MethodResult> methodResults) {
            this.className = className;
            this.classPath = classPath;
            this.jarName = jarName;
            this.methods = methods == null ? Collections.emptyList() : methods;
            this.methodNodes = methodNodes == null ? Collections.emptyMap() : methodNodes;
            this.methodResults = methodResults == null ? Collections.emptyMap() : methodResults;
        }
    }
}
