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

import me.n1ar4.jar.analyzer.core.build.BuildContext;
import me.n1ar4.jar.analyzer.core.bytecode.BuildBytecodeWorkspace;
import me.n1ar4.jar.analyzer.core.edge.BuildEdgeAccumulator;
import me.n1ar4.jar.analyzer.core.scope.ArchiveScopeClassifier.ScopeSummary;
import me.n1ar4.jar.analyzer.engine.project.ProjectOrigin;
import me.n1ar4.jar.analyzer.entity.LocalVarEntity;

import java.util.List;
import java.util.Map;

public final class BytecodeFactRunner {
    private BytecodeFactRunner() {
    }

    public static Result collect(BuildContext context,
                                 ScopeSummary scopeSummary,
                                 Map<Integer, ProjectOrigin> jarOriginsById,
                                 List<LocalVarEntity> localVars) {
        return collect(context, scopeSummary, jarOriginsById, localVars, null);
    }

    public static Result collect(BuildContext context,
                                 ScopeSummary scopeSummary,
                                 Map<Integer, ProjectOrigin> jarOriginsById,
                                 List<LocalVarEntity> localVars,
                                 BuildBytecodeWorkspace workspace) {
        BuildFactSnapshot snapshot = BuildFactAssembler.from(context, scopeSummary, jarOriginsById, localVars, workspace);
        BuildEdgeAccumulator edges = BuildEdgeAccumulator.fromContext(context);
        return new Result(snapshot, edges, localVars == null ? List.of() : List.copyOf(localVars));
    }

    public record Result(BuildFactSnapshot snapshot,
                         BuildEdgeAccumulator edges,
                         List<LocalVarEntity> localVars) {
        public Result {
            snapshot = snapshot == null ? BuildFactSnapshot.empty() : snapshot;
            edges = edges == null ? BuildEdgeAccumulator.empty() : edges;
            localVars = localVars == null ? List.of() : List.copyOf(localVars);
        }
    }
}
