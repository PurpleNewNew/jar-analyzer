/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.taie;

import me.n1ar4.jar.analyzer.analyze.spring.SpringController;
import me.n1ar4.jar.analyzer.analyze.spring.SpringMapping;
import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.WebEntryMethodSpec;
import me.n1ar4.jar.analyzer.core.WebEntryMethods;
import me.n1ar4.jar.analyzer.core.build.BuildContext;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.project.ProjectOrigin;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.Type;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

public final class TaieOracleCallGraphRunner {
    private static final Logger logger = LogManager.getLogger();

    private TaieOracleCallGraphRunner() {
    }

    public static Result run(Path primaryInput,
                             BuildContext context,
                             List<Path> appArchives,
                             List<Path> classpathArchives,
                             Map<Integer, ProjectOrigin> jarOriginsById,
                             TaieAnalysisRunner.AnalysisProfile profile,
                             boolean collectEndpointAliasStats) {
        if (context == null || context.methodMap == null || context.methodMap.isEmpty()) {
            throw new IllegalStateException("tai-e oracle build context is empty");
        }
        TaieAnalysisRunner.AnalysisProfile resolvedProfile = profile == null
                ? TaieAnalysisRunner.resolveProfile()
                : profile;
        List<String> explicitEntryMethods = collectExplicitEntryMethods(context, jarOriginsById);
        String mainClass = resolveMainClass(primaryInput, appArchives, context.discoveredMethods);
        TaieAnalysisRunner.TaieRunResult taieResult = TaieAnalysisRunner.run(
                appArchives,
                classpathArchives,
                resolvedProfile,
                mainClass,
                explicitEntryMethods,
                collectEndpointAliasStats
        );
        if (!taieResult.success() || taieResult.callGraph() == null) {
            String reason = taieResult.reason() == null ? "" : taieResult.reason().trim();
            throw new IllegalStateException("tai-e call graph failed: " + reason);
        }
        TaieEdgeMapper.MappingResult mapped = TaieEdgeMapper.map(
                taieResult.callGraph(),
                context.methodMap,
                jarOriginsById,
                TaieEdgeMapper.resolveEdgePolicy()
        );
        return new Result(
                mapped.syntheticMethods() == null ? List.of() : List.copyOf(mapped.syntheticMethods()),
                mapped.methodCalls() == null ? Map.of() : mapped.methodCalls(),
                mapped.methodCallMeta() == null ? Map.of() : mapped.methodCallMeta(),
                taieResult.elapsedMs(),
                resolvedProfile.value(),
                mapped.edgePolicy(),
                mapped.totalEdges(),
                mapped.keptEdges(),
                mapped.skippedByPolicy(),
                mapped.unresolvedCaller(),
                mapped.unresolvedCallee(),
                explicitEntryMethods.size(),
                taieResult.entryMethodCount(),
                taieResult.reachableMethodCount(),
                taieResult.pointsToVarCount(),
                taieResult.pointsToObjectCount(),
                taieResult.endpointThisVarCount(),
                taieResult.endpointMayAliasPairs(),
                safe(taieResult.reflectionInference()),
                safe(taieResult.reflectionLog())
        );
    }

    public record Result(List<MethodReference> syntheticMethods,
                         Map<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
                         Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                         long elapsedMs,
                         String profile,
                         String edgePolicy,
                         int totalEdges,
                         int keptEdges,
                         int skippedByPolicy,
                         int unresolvedCaller,
                         int unresolvedCallee,
                         int explicitEntryCount,
                         int entryMethodCount,
                         int reachableMethodCount,
                         int pointsToVarCount,
                         int pointsToObjectCount,
                         int endpointThisVarCount,
                         long endpointMayAliasPairs,
                         String reflectionInference,
                         String reflectionLog) {
        public void mergeInto(BuildContext context) {
            if (context == null) {
                return;
            }
            if (syntheticMethods != null && !syntheticMethods.isEmpty()) {
                context.discoveredMethods.addAll(syntheticMethods);
            }
            if (methodCalls != null && !methodCalls.isEmpty()) {
                context.methodCalls.putAll(methodCalls);
            }
            if (methodCallMeta != null && !methodCallMeta.isEmpty()) {
                context.methodCallMeta.putAll(methodCallMeta);
            }
        }
    }

    private static String resolveMainClass(Path primaryInput,
                                           List<Path> appArchives,
                                           Set<MethodReference> discoveredMethods) {
        String fromManifest = resolveMainClassFromManifests(primaryInput, appArchives);
        if (!fromManifest.isBlank()) {
            logger.info("resolved Tai-e main class from manifest: {}", fromManifest);
            return fromManifest;
        }
        String fromMethods = selectMainClassFromDiscoveredMethods(discoveredMethods);
        if (!fromMethods.isBlank()) {
            logger.info("resolved Tai-e main class from discovered methods: {}", fromMethods);
            return fromMethods;
        }
        logger.debug("Tai-e main class unresolved; analysis may fallback to implicit entries only");
        return "";
    }

    private static String resolveMainClassFromManifests(Path primaryInput,
                                                        List<Path> appArchives) {
        LinkedHashSet<Path> candidates = new LinkedHashSet<>();
        if (primaryInput != null && Files.isRegularFile(primaryInput)) {
            candidates.add(primaryInput.toAbsolutePath().normalize());
        }
        if (appArchives != null && !appArchives.isEmpty()) {
            for (Path archive : appArchives) {
                if (archive != null && Files.isRegularFile(archive)) {
                    candidates.add(archive.toAbsolutePath().normalize());
                }
            }
        }
        for (Path archive : candidates) {
            String mainClass = readMainClassFromManifest(archive);
            if (!mainClass.isBlank()) {
                return mainClass;
            }
        }
        return "";
    }

    private static String readMainClassFromManifest(Path archive) {
        if (archive == null || Files.notExists(archive)) {
            return "";
        }
        String fileName = archive.getFileName() == null ? "" : archive.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(".jar") && !fileName.endsWith(".war")) {
            return "";
        }
        try (JarFile jarFile = new JarFile(archive.toFile())) {
            if (jarFile.getManifest() == null) {
                return "";
            }
            Attributes attrs = jarFile.getManifest().getMainAttributes();
            if (attrs == null) {
                return "";
            }
            String startClass = normalizeMainClassName(attrs.getValue("Start-Class"));
            if (!startClass.isBlank()) {
                return startClass;
            }
            String mainClass = normalizeMainClassName(attrs.getValue(Attributes.Name.MAIN_CLASS));
            if (mainClass.startsWith("org.springframework.boot.loader.")) {
                return "";
            }
            return mainClass;
        } catch (Exception ex) {
            logger.debug("read manifest main class failed for {}: {}", archive, ex.toString());
            return "";
        }
    }

    private static String selectMainClassFromDiscoveredMethods(Set<MethodReference> discoveredMethods) {
        if (discoveredMethods == null || discoveredMethods.isEmpty()) {
            return "";
        }
        String selected = "";
        for (MethodReference method : discoveredMethods) {
            if (method == null || method.getClassReference() == null) {
                continue;
            }
            if (!"main".equals(method.getName())) {
                continue;
            }
            if (!"([Ljava/lang/String;)V".equals(method.getDesc())) {
                continue;
            }
            if ((method.getAccess() & 0x0008) == 0) {
                continue;
            }
            if ((method.getAccess() & 0x0001) == 0) {
                continue;
            }
            String candidate = normalizeMainClassName(method.getClassReference().getName());
            if (candidate.isBlank()) {
                continue;
            }
            if (selected.isBlank() || candidate.compareTo(selected) < 0) {
                selected = candidate;
            }
        }
        return selected;
    }

    private static String normalizeMainClassName(String className) {
        if (className == null) {
            return "";
        }
        String normalized = className.trim();
        if (normalized.isEmpty()) {
            return "";
        }
        if (normalized.startsWith("L") && normalized.endsWith(";") && normalized.length() > 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        normalized = normalized.replace('/', '.').replace('\\', '.');
        if (normalized.endsWith(".class")) {
            normalized = normalized.substring(0, normalized.length() - ".class".length());
        }
        while (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static List<String> collectExplicitEntryMethods(BuildContext context,
                                                            Map<Integer, ProjectOrigin> jarOriginsById) {
        if (context == null || context.methodMap == null || context.methodMap.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> signatures = new LinkedHashSet<>();
        LinkedHashSet<String> entryOwners = new LinkedHashSet<>();

        if (context.controllers != null && !context.controllers.isEmpty()) {
            for (SpringController controller : context.controllers) {
                if (controller == null || controller.getMappings() == null || controller.getMappings().isEmpty()) {
                    continue;
                }
                for (SpringMapping mapping : controller.getMappings()) {
                    if (mapping == null) {
                        continue;
                    }
                    MethodReference method = mapping.getMethodReference();
                    if (method == null) {
                        method = resolveMethodByHandle(mapping.getMethodName(), context.methodMap);
                    }
                    addEntrySignature(signatures, entryOwners, method, jarOriginsById);
                }
            }
        }

        addEntryMethodsByClass(
                signatures,
                entryOwners,
                context.servlets,
                WebEntryMethods.SERVLET_ENTRY_METHODS,
                context.methodMap,
                jarOriginsById
        );
        addEntryMethodsByClass(
                signatures,
                entryOwners,
                context.filters,
                WebEntryMethods.FILTER_ENTRY_METHODS,
                context.methodMap,
                jarOriginsById
        );
        addEntryMethodsByClass(
                signatures,
                entryOwners,
                context.interceptors,
                WebEntryMethods.INTERCEPTOR_ENTRY_METHODS,
                context.methodMap,
                jarOriginsById
        );
        addEntryMethodsByClass(
                signatures,
                entryOwners,
                context.listeners,
                WebEntryMethods.LISTENER_ENTRY_METHODS,
                context.methodMap,
                jarOriginsById
        );
        if (context.explicitSourceMethodFlags != null && !context.explicitSourceMethodFlags.isEmpty()) {
            for (MethodReference.Handle handle : context.explicitSourceMethodFlags.keySet()) {
                addEntrySignature(signatures, entryOwners, resolveMethodByHandle(handle, context.methodMap), jarOriginsById);
            }
        }
        addEntryConstructors(signatures, entryOwners, context.methodMap, jarOriginsById);

        if (!signatures.isEmpty()) {
            logger.info("tai-e explicit entry methods resolved: {}", signatures.size());
        }
        return signatures.isEmpty() ? List.of() : List.copyOf(signatures);
    }

    private static void addEntryMethodsByClass(Set<String> out,
                                               Set<String> ownerOut,
                                               List<String> classNames,
                                               Set<WebEntryMethodSpec> methodSpecs,
                                               Map<MethodReference.Handle, MethodReference> methodMap,
                                               Map<Integer, ProjectOrigin> jarOriginsById) {
        if (out == null || classNames == null || classNames.isEmpty()
                || methodSpecs == null || methodSpecs.isEmpty()
                || methodMap == null || methodMap.isEmpty()) {
            return;
        }
        Set<String> normalizedClasses = new HashSet<>();
        for (String className : classNames) {
            String normalized = normalizeInternalClassName(className);
            if (!normalized.isBlank()) {
                normalizedClasses.add(normalized);
            }
        }
        if (normalizedClasses.isEmpty()) {
            return;
        }
        for (MethodReference method : methodMap.values()) {
            if (method == null || method.getClassReference() == null) {
                continue;
            }
            String owner = normalizeInternalClassName(method.getClassReference().getName());
            if (!normalizedClasses.contains(owner)) {
                continue;
            }
            WebEntryMethodSpec spec = new WebEntryMethodSpec(safe(method.getName()), safe(method.getDesc()));
            if (!methodSpecs.contains(spec)) {
                continue;
            }
            addEntrySignature(out, ownerOut, method, jarOriginsById);
        }
    }

    private static MethodReference resolveMethodByHandle(MethodReference.Handle handle,
                                                         Map<MethodReference.Handle, MethodReference> methodMap) {
        if (handle == null || methodMap == null || methodMap.isEmpty()) {
            return null;
        }
        MethodReference direct = methodMap.get(handle);
        if (direct != null) {
            return direct;
        }
        String targetOwner = normalizeInternalClassName(
                handle.getClassReference() == null ? "" : handle.getClassReference().getName()
        );
        String targetName = safe(handle.getName());
        String targetDesc = safe(handle.getDesc());
        if (targetOwner.isBlank() || targetName.isBlank() || targetDesc.isBlank()) {
            return null;
        }
        for (MethodReference method : methodMap.values()) {
            if (method == null || method.getClassReference() == null) {
                continue;
            }
            if (!targetName.equals(method.getName()) || !targetDesc.equals(method.getDesc())) {
                continue;
            }
            String owner = normalizeInternalClassName(method.getClassReference().getName());
            if (targetOwner.equals(owner)) {
                return method;
            }
        }
        return null;
    }

    private static void addEntrySignature(Set<String> out,
                                          Set<String> ownerOut,
                                          MethodReference method,
                                          Map<Integer, ProjectOrigin> jarOriginsById) {
        if (out == null || method == null || !isAppMethod(method, jarOriginsById)) {
            return;
        }
        String signature = toTaieMethodSignature(method);
        if (!signature.isBlank()) {
            out.add(signature);
        }
        if (ownerOut == null || method.getClassReference() == null) {
            return;
        }
        String owner = normalizeInternalClassName(method.getClassReference().getName());
        if (!owner.isBlank()) {
            ownerOut.add(owner);
        }
    }

    private static void addEntryConstructors(Set<String> out,
                                             Set<String> owners,
                                             Map<MethodReference.Handle, MethodReference> methodMap,
                                             Map<Integer, ProjectOrigin> jarOriginsById) {
        if (out == null || owners == null || owners.isEmpty()
                || methodMap == null || methodMap.isEmpty()) {
            return;
        }
        Map<String, List<MethodReference>> constructorsByOwner = new LinkedHashMap<>();
        for (MethodReference method : methodMap.values()) {
            if (method == null || method.getClassReference() == null || !isAppMethod(method, jarOriginsById)) {
                continue;
            }
            if (!"<init>".equals(method.getName())) {
                continue;
            }
            String owner = normalizeInternalClassName(method.getClassReference().getName());
            if (owner.isBlank() || !owners.contains(owner)) {
                continue;
            }
            constructorsByOwner.computeIfAbsent(owner, ignored -> new ArrayList<>()).add(method);
        }
        for (String owner : owners) {
            List<MethodReference> constructors = constructorsByOwner.get(owner);
            if (constructors == null || constructors.isEmpty()) {
                continue;
            }
            boolean addedDefault = false;
            for (MethodReference constructor : constructors) {
                if (!"()V".equals(constructor.getDesc())) {
                    continue;
                }
                String signature = toTaieMethodSignature(constructor);
                if (!signature.isBlank()) {
                    out.add(signature);
                    addedDefault = true;
                }
            }
            if (addedDefault) {
                continue;
            }
            for (MethodReference constructor : constructors) {
                String signature = toTaieMethodSignature(constructor);
                if (!signature.isBlank()) {
                    out.add(signature);
                }
            }
        }
    }

    private static boolean isAppMethod(MethodReference method,
                                       Map<Integer, ProjectOrigin> jarOriginsById) {
        if (method == null || method.getJarId() == null) {
            return false;
        }
        Integer jarId = method.getJarId();
        if (jarId <= 0) {
            return false;
        }
        ProjectOrigin origin = jarOriginsById == null ? null : jarOriginsById.get(jarId);
        return origin == ProjectOrigin.APP;
    }

    private static String toTaieMethodSignature(MethodReference method) {
        if (method == null || method.getClassReference() == null) {
            return "";
        }
        String owner = normalizeMainClassName(method.getClassReference().getName());
        String methodName = safe(method.getName());
        String desc = safe(method.getDesc());
        if (owner.isBlank() || methodName.isBlank() || desc.isBlank()) {
            return "";
        }
        try {
            Type mType = Type.getMethodType(desc);
            String returnType = toTaieTypeName(mType.getReturnType());
            Type[] args = mType.getArgumentTypes();
            StringBuilder params = new StringBuilder();
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) {
                        params.append(',');
                    }
                    params.append(toTaieTypeName(args[i]));
                }
            }
            return "<" + owner + ": " + returnType + " " + methodName + "(" + params + ")>";
        } catch (Exception ex) {
            logger.debug("build Tai-e method signature failed: {}#{}{} ({})",
                    owner, methodName, desc, ex.toString());
            return "";
        }
    }

    private static String toTaieTypeName(Type type) {
        if (type == null) {
            return "java.lang.Object";
        }
        return switch (type.getSort()) {
            case Type.VOID -> "void";
            case Type.BOOLEAN -> "boolean";
            case Type.CHAR -> "char";
            case Type.BYTE -> "byte";
            case Type.SHORT -> "short";
            case Type.INT -> "int";
            case Type.FLOAT -> "float";
            case Type.LONG -> "long";
            case Type.DOUBLE -> "double";
            default -> type.getClassName();
        };
    }

    private static String normalizeInternalClassName(String className) {
        if (className == null || className.isBlank()) {
            return "";
        }
        String normalized = className.trim().replace('.', '/').replace('\\', '/');
        if (normalized.startsWith("L") && normalized.endsWith(";") && normalized.length() > 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (normalized.endsWith(".class")) {
            normalized = normalized.substring(0, normalized.length() - ".class".length());
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
