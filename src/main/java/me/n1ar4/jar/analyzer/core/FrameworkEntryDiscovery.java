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

import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;
import me.n1ar4.jar.analyzer.rules.FrameworkXmlSourceIndex;
import me.n1ar4.jar.analyzer.rules.SourceRuleSupport;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public final class FrameworkEntryDiscovery {
    private static final Logger logger = LogManager.getLogger();

    private FrameworkEntryDiscovery() {
    }

    public static Result discover(Collection<ResourceEntity> resources,
                                  Map<ClassReference.Handle, ClassReference> classMap,
                                  Map<MethodReference.Handle, MethodReference> methodMap) {
        if ((resources == null || resources.isEmpty())
                && (classMap == null || classMap.isEmpty())
                && (methodMap == null || methodMap.isEmpty())) {
            return Result.empty();
        }
        FrameworkXmlSourceIndex.Result xmlIndex = FrameworkXmlSourceIndex.fromResources(resources);
        Map<ClassReference.Handle, ClassReference> classIndex =
                classMap == null ? Map.of() : classMap;
        Map<String, ClassReference> classByName = buildClassByName(classIndex.values());
        BiFunction<String, Integer, ClassReference> resolver =
                (name, jarId) -> resolveClassReference(name, jarId, classIndex, classByName);

        Map<MethodReference.Handle, Integer> explicitSourceMethodFlags = new LinkedHashMap<>();
        if (methodMap != null && !methodMap.isEmpty()) {
            for (MethodReference method : methodMap.values()) {
                if (method == null) {
                    continue;
                }
                String owner = method.getClassReference() == null ? "" : method.getClassReference().getName();
                int jarId = method.getJarId() == null ? -1 : method.getJarId();
                int flags = xmlIndex.resolveFlags(owner, method.getName(), method.getDesc());
                ClassReference ownerClass = resolveOwnerClass(method, classIndex, classByName);
                flags |= SourceRuleSupport.resolveFrameworkFlags(method, ownerClass, resolver);
                if (flags != 0) {
                    explicitSourceMethodFlags.merge(method.getHandle(), flags, (left, right) -> left | right);
                }
            }
        }
        logger.info("framework entry discovery: xmlServlets={}, xmlFilters={}, xmlListeners={}, explicitMethods={}",
                xmlIndex.servletClasses().size(),
                xmlIndex.filterClasses().size(),
                xmlIndex.listenerClasses().size(),
                explicitSourceMethodFlags.size());
        return new Result(
                new ArrayList<>(xmlIndex.servletClasses()),
                new ArrayList<>(xmlIndex.filterClasses()),
                new ArrayList<>(xmlIndex.listenerClasses()),
                explicitSourceMethodFlags.isEmpty() ? Map.of() : Map.copyOf(explicitSourceMethodFlags)
        );
    }

    private static ClassReference resolveOwnerClass(MethodReference method,
                                                   Map<ClassReference.Handle, ClassReference> classIndex,
                                                   Map<String, ClassReference> classByName) {
        if (method == null || method.getClassReference() == null) {
            return null;
        }
        return resolveClassReference(
                method.getClassReference().getName(),
                method.getClassReference().getJarId(),
                classIndex,
                classByName
        );
    }

    private static ClassReference resolveClassReference(String className,
                                                        Integer jarId,
                                                        Map<ClassReference.Handle, ClassReference> classIndex,
                                                        Map<String, ClassReference> classByName) {
        if (className == null || className.isBlank()) {
            return null;
        }
        ClassReference direct = classIndex.get(new ClassReference.Handle(className, jarId));
        if (direct != null) {
            return direct;
        }
        return classByName.get(className);
    }

    private static Map<String, ClassReference> buildClassByName(Collection<ClassReference> classReferences) {
        Map<String, ClassReference> out = new HashMap<>();
        if (classReferences == null || classReferences.isEmpty()) {
            return out;
        }
        for (ClassReference classReference : classReferences) {
            if (classReference == null || classReference.getName() == null || classReference.getName().isBlank()) {
                continue;
            }
            out.putIfAbsent(classReference.getName(), classReference);
        }
        return out;
    }

    public record Result(List<String> servlets,
                         List<String> filters,
                         List<String> listeners,
                         Map<MethodReference.Handle, Integer> explicitSourceMethodFlags) {
        public static Result empty() {
            return new Result(List.of(), List.of(), List.of(), Map.of());
        }
    }
}
