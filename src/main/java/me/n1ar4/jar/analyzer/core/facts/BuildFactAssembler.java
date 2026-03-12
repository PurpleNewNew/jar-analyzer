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
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BuildFactAssembler {
    private BuildFactAssembler() {
    }

    public static BuildFactSnapshot from(BuildContext context,
                                         BuildBytecodeWorkspace workspace) {
        if (context == null) {
            return BuildFactSnapshot.empty();
        }
        Map<MethodReference.Handle, Integer> methodSemanticFlags = new LinkedHashMap<>();
        for (MethodReference method : context.methodMap.values()) {
            if (method == null || method.getHandle() == null || method.getClassReference() == null) {
                continue;
            }
            if (method.getSemanticFlags() > 0) {
                methodSemanticFlags.put(method.getHandle(), method.getSemanticFlags());
            }
        }

        Map<MethodReference.Handle, List<CallSiteEntity>> callSitesByCaller = new LinkedHashMap<>();
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
        }
        InheritanceMap inheritanceMap = InheritanceMap.fromClasses(context.classMap);
        BuildFactSnapshot.TypeFacts types = new BuildFactSnapshot.TypeFacts(
                context.classMap,
                inheritanceMap,
                workspace == null ? java.util.Set.of() : workspace.collectInstantiatedClasses(inheritanceMap)
        );
        BuildFactSnapshot.MethodFacts methods = new BuildFactSnapshot.MethodFacts(context.methodMap);
        BuildFactSnapshot.SymbolFacts symbols = new BuildFactSnapshot.SymbolFacts(
                context.callSites,
                callSitesByCaller
        );
        BuildFactSnapshot.SemanticFacts semantics = new BuildFactSnapshot.SemanticFacts(
                context.explicitSourceMethodFlags,
                methodSemanticFlags
        );
        BuildFactSnapshot.BytecodeFacts bytecode = new BuildFactSnapshot.BytecodeFacts(workspace);
        BuildFactSnapshot.ConstraintFacts constraints = ConstraintFactAssembler.assemble(
                context.callSites,
                context.methodMap,
                workspace
        );
        return new BuildFactSnapshot(
                types,
                methods,
                symbols,
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
        context.classMap.putAll(snapshot.types().classesByHandle());
        context.methodMap.putAll(snapshot.methods().methodsByHandle());
        context.callSites.addAll(snapshot.symbols().callSites());
        if (edges != null) {
            edges.copyInto(context);
        }
        return context;
    }
}
