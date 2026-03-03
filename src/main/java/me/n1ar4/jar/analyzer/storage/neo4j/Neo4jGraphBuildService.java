/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.storage.neo4j;

import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Graph write path is hard-switched to Neo4j bulk import.
 * Legacy direct write mode and snapshot assembly were removed.
 */
public final class Neo4jGraphBuildService {
    private final Neo4jBulkImportService bulkImportService = new Neo4jBulkImportService();

    public GraphBuildStats replaceFromAnalysis(long buildSeq,
                                               boolean quickMode,
                                               String callGraphMode,
                                               Set<MethodReference> methods,
                                               Map<MethodReference.Handle, ? extends Set<MethodReference.Handle>> methodCalls,
                                               Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                                               List<CallSiteEntity> callSites) {
        return bulkImportService.replaceFromAnalysis(
                buildSeq,
                quickMode,
                callGraphMode,
                methods,
                methodCalls,
                methodCallMeta,
                callSites
        );
    }

    public record GraphBuildStats(int methodNodes, int callSiteNodes, int edgeCount) {
    }
}
