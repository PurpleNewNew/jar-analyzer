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

import me.n1ar4.jar.analyzer.core.InheritanceMap;
import me.n1ar4.jar.analyzer.core.build.BuildContext;
import me.n1ar4.jar.analyzer.core.bytecode.BuildBytecodeWorkspace;
import me.n1ar4.jar.analyzer.core.edge.BuildEdgeAccumulator;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.core.scope.ArchiveScopeClassifier.ScopeSummary;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectBuildMode;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.engine.project.ProjectOrigin;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.entity.LocalVarEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BuildFactAssembler {
    private BuildFactAssembler() {
    }

    public static BuildFactSnapshot from(BuildContext context,
                                         ScopeSummary scopeSummary,
                                         Map<Integer, ProjectOrigin> jarOriginsById,
                                         List<LocalVarEntity> localVars) {
        return from(context, scopeSummary, jarOriginsById, localVars, null);
    }

    public static BuildFactSnapshot from(BuildContext context,
                                         ScopeSummary scopeSummary,
                                         Map<Integer, ProjectOrigin> jarOriginsById,
                                         List<LocalVarEntity> localVars,
                                         BuildBytecodeWorkspace workspace) {
        if (context == null) {
            return BuildFactSnapshot.empty();
        }
        Map<ClassReference.Handle, List<MethodReference.Handle>> methodsByClass = new LinkedHashMap<>();
        Map<MethodReference.Handle, Integer> methodSemanticFlags = new LinkedHashMap<>();
        for (MethodReference method : context.methodMap.values()) {
            if (method == null || method.getHandle() == null || method.getClassReference() == null) {
                continue;
            }
            methodsByClass.computeIfAbsent(method.getClassReference(), ignore -> new ArrayList<>())
                    .add(method.getHandle());
            if (method.getSemanticFlags() > 0) {
                methodSemanticFlags.put(method.getHandle(), method.getSemanticFlags());
            }
        }

        Map<MethodReference.Handle, List<CallSiteEntity>> callSitesByCaller = new LinkedHashMap<>();
        Map<String, CallSiteEntity> callSitesByKey = new LinkedHashMap<>();
        for (CallSiteEntity site : context.callSites) {
            if (site == null) {
                continue;
            }
            MethodReference.Handle caller = new MethodReference.Handle(
                    new ClassReference.Handle(site.getCallerClassName(), site.getJarId()),
                    site.getCallerMethodName(),
                    site.getCallerMethodDesc()
            );
            callSitesByCaller.computeIfAbsent(caller, ignore -> new ArrayList<>()).add(site);
            if (site.getCallSiteKey() != null && !site.getCallSiteKey().isBlank()) {
                callSitesByKey.put(site.getCallSiteKey(), site);
            }
        }

        Map<MethodReference.Handle, List<LocalVarEntity>> localVarsByMethod = new LinkedHashMap<>();
        if (localVars != null) {
            for (LocalVarEntity localVar : localVars) {
                if (localVar == null || localVar.getClassName() == null
                        || localVar.getMethodName() == null || localVar.getMethodDesc() == null) {
                    continue;
                }
                MethodReference.Handle owner = new MethodReference.Handle(
                        new ClassReference.Handle(localVar.getClassName(), localVar.getJarId()),
                        localVar.getMethodName(),
                        localVar.getMethodDesc()
                );
                localVarsByMethod.computeIfAbsent(owner, ignore -> new ArrayList<>()).add(localVar);
            }
        }

        ProjectModel projectModel = ProjectRuntimeContext.getProjectModel();
        boolean projectMode = projectModel != null && projectModel.buildMode() == ProjectBuildMode.PROJECT;
        InheritanceMap inheritanceMap = InheritanceMap.fromClasses(context.classMap);
        BuildFactSnapshot.ArchiveFacts archives = new BuildFactSnapshot.ArchiveFacts(
                snapshotArchiveList(scopeSummary),
                scopeSummary == null ? 0 : scopeSummary.targetArchiveCount(),
                scopeSummary == null ? 0 : scopeSummary.libraryArchiveCount(),
                scopeSummary == null ? 0 : scopeSummary.sdkArchiveCount(),
                jarOriginsById,
                projectMode
        );
        BuildFactSnapshot.TypeFacts types = new BuildFactSnapshot.TypeFacts(
                context.classMap,
                inheritanceMap,
                workspace == null ? java.util.Set.of() : workspace.collectInstantiatedClasses(inheritanceMap)
        );
        BuildFactSnapshot.MethodFacts methods = new BuildFactSnapshot.MethodFacts(
                context.methodMap,
                methodsByClass
        );
        BuildFactSnapshot.SymbolFacts symbols = new BuildFactSnapshot.SymbolFacts(
                context.callSites,
                callSitesByCaller,
                callSitesByKey,
                localVars,
                localVarsByMethod,
                context.strMap,
                context.stringAnnoMap
        );
        BuildFactSnapshot.ResourceFacts resources = new BuildFactSnapshot.ResourceFacts(context.resources);
        BuildFactSnapshot.SemanticFacts semantics = new BuildFactSnapshot.SemanticFacts(
                context.controllers,
                context.interceptors,
                context.servlets,
                context.filters,
                context.listeners,
                context.explicitSourceMethodFlags,
                methodSemanticFlags
        );
        BuildFactSnapshot.BytecodeFacts bytecode = new BuildFactSnapshot.BytecodeFacts(context.classFileList, workspace);
        BuildFactSnapshot.ConstraintFacts constraints = ConstraintFactAssembler.assemble(
                context.callSites,
                context.methodMap,
                workspace
        );
        return new BuildFactSnapshot(
                archives,
                types,
                methods,
                symbols,
                resources,
                semantics,
                bytecode,
                constraints
        );
    }

    public static BuildContext contextView(BuildFactSnapshot snapshot,
                                          BuildEdgeAccumulator edges) {
        BuildContext context = new BuildContext();
        if (snapshot == null) {
            if (edges != null) {
                edges.copyInto(context);
            }
            return context;
        }
        context.classFileList.addAll(snapshot.bytecode().classFiles());
        context.discoveredClasses.addAll(snapshot.types().classesByHandle().values());
        context.classMap.putAll(snapshot.types().classesByHandle());
        context.discoveredMethods.addAll(snapshot.methods().methodsByHandle().values());
        context.methodMap.putAll(snapshot.methods().methodsByHandle());
        context.callSites.addAll(snapshot.symbols().callSites());
        context.resources.addAll(snapshot.resources().resources());
        context.controllers.addAll(snapshot.semantics().controllers());
        context.interceptors.addAll(snapshot.semantics().interceptors());
        context.servlets.addAll(snapshot.semantics().servlets());
        context.filters.addAll(snapshot.semantics().filters());
        context.listeners.addAll(snapshot.semantics().listeners());
        context.explicitSourceMethodFlags.putAll(snapshot.semantics().explicitSourceMethodFlags());
        context.strMap.putAll(snapshot.symbols().stringLiteralsByMethod());
        context.stringAnnoMap.putAll(snapshot.symbols().annotationStringsByMethod());
        if (edges != null) {
            edges.copyInto(context);
        }
        return context;
    }

    private static List<String> snapshotArchiveList(ScopeSummary scopeSummary) {
        if (scopeSummary == null || scopeSummary.originsByArchive().isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Map.Entry<java.nio.file.Path, ProjectOrigin> entry : scopeSummary.originsByArchive().entrySet()) {
            if (entry == null || entry.getKey() == null) {
                continue;
            }
            out.add(entry.getKey().toString());
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }
}
