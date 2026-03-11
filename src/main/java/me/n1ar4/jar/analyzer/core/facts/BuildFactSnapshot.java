/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.facts;

import me.n1ar4.jar.analyzer.analyze.spring.SpringController;
import me.n1ar4.jar.analyzer.core.InheritanceMap;
import me.n1ar4.jar.analyzer.core.bytecode.BuildBytecodeWorkspace;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.project.ProjectOrigin;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.LocalVarEntity;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record BuildFactSnapshot(ArchiveFacts archives,
                                TypeFacts types,
                                MethodFacts methods,
                                SymbolFacts symbols,
                                ResourceFacts resources,
                                SemanticFacts semantics,
                                BytecodeFacts bytecode,
                                ConstraintFacts constraints) {
    public BuildFactSnapshot {
        archives = archives == null ? ArchiveFacts.empty() : archives;
        types = types == null ? TypeFacts.empty() : types;
        methods = methods == null ? MethodFacts.empty() : methods;
        symbols = symbols == null ? SymbolFacts.empty() : symbols;
        resources = resources == null ? ResourceFacts.empty() : resources;
        semantics = semantics == null ? SemanticFacts.empty() : semantics;
        bytecode = bytecode == null ? BytecodeFacts.empty() : bytecode;
        constraints = constraints == null ? ConstraintFacts.empty() : constraints;
    }

    public static BuildFactSnapshot empty() {
        return new BuildFactSnapshot(
                ArchiveFacts.empty(),
                TypeFacts.empty(),
                MethodFacts.empty(),
                SymbolFacts.empty(),
                ResourceFacts.empty(),
                SemanticFacts.empty(),
                BytecodeFacts.empty(),
                ConstraintFacts.empty()
        );
    }

    public record ArchiveFacts(List<String> allArchives,
                               int targetArchiveCount,
                               int libraryArchiveCount,
                               int sdkEntryCount,
                               Map<Integer, ProjectOrigin> jarOriginsById,
                               boolean projectMode) {
        public ArchiveFacts {
            allArchives = allArchives == null ? List.of() : List.copyOf(allArchives);
            jarOriginsById = jarOriginsById == null || jarOriginsById.isEmpty()
                    ? Map.of()
                    : Map.copyOf(jarOriginsById);
            targetArchiveCount = Math.max(0, targetArchiveCount);
            libraryArchiveCount = Math.max(0, libraryArchiveCount);
            sdkEntryCount = Math.max(0, sdkEntryCount);
        }

        public static ArchiveFacts empty() {
            return new ArchiveFacts(List.of(), 0, 0, 0, Map.of(), false);
        }
    }

    public record TypeFacts(Map<ClassReference.Handle, ClassReference> classesByHandle,
                            InheritanceMap inheritanceMap,
                            Set<ClassReference.Handle> instantiatedClasses) {
        public TypeFacts {
            classesByHandle = classesByHandle == null || classesByHandle.isEmpty()
                    ? Map.of()
                    : Map.copyOf(classesByHandle);
            inheritanceMap = inheritanceMap == null
                    ? InheritanceMap.fromClasses(Map.of())
                    : inheritanceMap;
            instantiatedClasses = instantiatedClasses == null || instantiatedClasses.isEmpty()
                    ? Set.of()
                    : Set.copyOf(instantiatedClasses);
        }

        public static TypeFacts empty() {
            return new TypeFacts(Map.of(), InheritanceMap.fromClasses(Map.of()), Set.of());
        }
    }

    public record MethodFacts(Map<MethodReference.Handle, MethodReference> methodsByHandle,
                              Map<ClassReference.Handle, List<MethodReference.Handle>> methodsByClass) {
        public MethodFacts {
            methodsByHandle = methodsByHandle == null || methodsByHandle.isEmpty()
                    ? Map.of()
                    : Map.copyOf(methodsByHandle);
            methodsByClass = methodsByClass == null || methodsByClass.isEmpty()
                    ? Map.of()
                    : Map.copyOf(methodsByClass);
        }

        public static MethodFacts empty() {
            return new MethodFacts(Map.of(), Map.of());
        }
    }

    public record SymbolFacts(List<CallSiteEntity> callSites,
                              Map<MethodReference.Handle, List<CallSiteEntity>> callSitesByCaller,
                              Map<String, CallSiteEntity> callSitesByKey,
                              List<LocalVarEntity> localVars,
                              Map<MethodReference.Handle, List<LocalVarEntity>> localVarsByMethod,
                              Map<MethodReference.Handle, List<String>> stringLiteralsByMethod,
                              Map<MethodReference.Handle, List<String>> annotationStringsByMethod) {
        public SymbolFacts {
            callSites = callSites == null ? List.of() : List.copyOf(callSites);
            callSitesByCaller = callSitesByCaller == null || callSitesByCaller.isEmpty()
                    ? Map.of()
                    : Map.copyOf(callSitesByCaller);
            callSitesByKey = callSitesByKey == null || callSitesByKey.isEmpty()
                    ? Map.of()
                    : Map.copyOf(callSitesByKey);
            localVars = localVars == null ? List.of() : List.copyOf(localVars);
            localVarsByMethod = localVarsByMethod == null || localVarsByMethod.isEmpty()
                    ? Map.of()
                    : Map.copyOf(localVarsByMethod);
            stringLiteralsByMethod = stringLiteralsByMethod == null || stringLiteralsByMethod.isEmpty()
                    ? Map.of()
                    : Map.copyOf(stringLiteralsByMethod);
            annotationStringsByMethod = annotationStringsByMethod == null || annotationStringsByMethod.isEmpty()
                    ? Map.of()
                    : Map.copyOf(annotationStringsByMethod);
        }

        public static SymbolFacts empty() {
            return new SymbolFacts(List.of(), Map.of(), Map.of(), List.of(), Map.of(), Map.of(), Map.of());
        }
    }

    public record ResourceFacts(List<ResourceEntity> resources) {
        public ResourceFacts {
            resources = resources == null ? List.of() : List.copyOf(resources);
        }

        public static ResourceFacts empty() {
            return new ResourceFacts(List.of());
        }
    }

    public record SemanticFacts(List<SpringController> controllers,
                                List<String> interceptors,
                                List<String> servlets,
                                List<String> filters,
                                List<String> listeners,
                                Map<MethodReference.Handle, Integer> explicitSourceMethodFlags,
                                Map<MethodReference.Handle, Integer> methodSemanticFlags) {
        public SemanticFacts {
            controllers = controllers == null ? List.of() : List.copyOf(controllers);
            interceptors = interceptors == null ? List.of() : List.copyOf(interceptors);
            servlets = servlets == null ? List.of() : List.copyOf(servlets);
            filters = filters == null ? List.of() : List.copyOf(filters);
            listeners = listeners == null ? List.of() : List.copyOf(listeners);
            explicitSourceMethodFlags = explicitSourceMethodFlags == null || explicitSourceMethodFlags.isEmpty()
                    ? Map.of()
                    : Map.copyOf(explicitSourceMethodFlags);
            methodSemanticFlags = methodSemanticFlags == null || methodSemanticFlags.isEmpty()
                    ? Map.of()
                    : Map.copyOf(methodSemanticFlags);
        }

        public static SemanticFacts empty() {
            return new SemanticFacts(List.of(), List.of(), List.of(), List.of(), List.of(), Map.of(), Map.of());
        }
    }

    public record BytecodeFacts(Set<ClassFileEntity> classFiles,
                                BuildBytecodeWorkspace workspace) {
        public BytecodeFacts {
            classFiles = classFiles == null || classFiles.isEmpty()
                    ? Set.of()
                    : Set.copyOf(classFiles);
            workspace = workspace == null ? BuildBytecodeWorkspace.empty() : workspace;
        }

        public static BytecodeFacts empty() {
            return new BytecodeFacts(Set.of(), BuildBytecodeWorkspace.empty());
        }
    }

    public record ConstraintFacts(Map<String, String> receiverVarByCallSiteKey) {
        public ConstraintFacts {
            receiverVarByCallSiteKey = receiverVarByCallSiteKey == null || receiverVarByCallSiteKey.isEmpty()
                    ? Map.of()
                    : Map.copyOf(receiverVarByCallSiteKey);
        }

        public static ConstraintFacts empty() {
            return new ConstraintFacts(Map.of());
        }
    }
}
