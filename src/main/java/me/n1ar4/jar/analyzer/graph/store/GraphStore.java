/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.graph.store;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jGraphSnapshotLoader;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

public final class GraphStore {
    private static final Logger logger = LogManager.getLogger();
    private static final Neo4jGraphSnapshotLoader NEO4J_LOADER = new Neo4jGraphSnapshotLoader();

    public GraphSnapshot loadSnapshot() {
        return loadSnapshot(ActiveProjectContext.getActiveProjectKey());
    }

    public GraphSnapshot loadSnapshot(String projectKey) {
        String resolvedProjectKey = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        DatabaseManager.ensureProjectReadable(resolvedProjectKey);
        try {
            GraphSnapshot neo4jSnapshot = NEO4J_LOADER.load(resolvedProjectKey);
            if (neo4jSnapshot == null || neo4jSnapshot.getNodeCount() <= 0) {
                throw new IllegalStateException("graph_snapshot_missing_rebuild");
            }
            return neo4jSnapshot;
        } catch (Exception ex) {
            logger.warn("load neo4j graph snapshot fail: key={} err={}", resolvedProjectKey, ex.toString());
            if (ex instanceof IllegalStateException state) {
                String message = state.getMessage();
                if ("graph_snapshot_missing_rebuild".equals(message)) {
                    throw state;
                }
                throw state;
            }
            throw new IllegalStateException("graph_snapshot_load_failed", ex);
        }
    }

    public GraphSnapshot loadFlowSnapshot() {
        return loadFlowSnapshot(ActiveProjectContext.getActiveProjectKey());
    }

    public GraphSnapshot loadFlowSnapshot(String projectKey) {
        String resolvedProjectKey = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        DatabaseManager.ensureProjectReadable(resolvedProjectKey);
        try {
            GraphSnapshot flowSnapshot = NEO4J_LOADER.loadFlow(resolvedProjectKey);
            if (flowSnapshot == null || flowSnapshot.getNodeCount() <= 0) {
                throw new IllegalStateException("graph_flow_snapshot_missing_rebuild");
            }
            return flowSnapshot;
        } catch (Exception ex) {
            logger.warn("load neo4j flow snapshot fail: key={} err={}", resolvedProjectKey, ex.toString());
            if (ex instanceof IllegalStateException state) {
                String message = state.getMessage();
                if ("graph_snapshot_missing_rebuild".equals(message)) {
                    throw new IllegalStateException("graph_flow_snapshot_missing_rebuild", state);
                }
                throw state;
            }
            throw new IllegalStateException("graph_flow_snapshot_load_failed", ex);
        }
    }

    public GraphSnapshot loadQuerySnapshot() {
        return loadQuerySnapshot(ActiveProjectContext.getActiveProjectKey());
    }

    public GraphSnapshot loadQuerySnapshot(String projectKey) {
        String resolvedProjectKey = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        DatabaseManager.ensureProjectReadable(resolvedProjectKey);
        try {
            GraphSnapshot querySnapshot = NEO4J_LOADER.loadQuery(resolvedProjectKey);
            if (querySnapshot == null || querySnapshot.getNodeCount() <= 0) {
                throw new IllegalStateException("graph_query_snapshot_missing_rebuild");
            }
            return querySnapshot;
        } catch (Exception ex) {
            logger.warn("load neo4j query snapshot fail: key={} err={}", resolvedProjectKey, ex.toString());
            if (ex instanceof IllegalStateException state) {
                String message = state.getMessage();
                if ("graph_snapshot_missing_rebuild".equals(message)) {
                    throw new IllegalStateException("graph_query_snapshot_missing_rebuild", state);
                }
                throw state;
            }
            throw new IllegalStateException("graph_query_snapshot_load_failed", ex);
        }
    }

    public static void invalidateCache() {
        Neo4jGraphSnapshotLoader.invalidateAll();
    }
}
