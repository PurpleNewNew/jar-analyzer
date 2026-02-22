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

import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jGraphSnapshotLoader;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

public final class GraphStore {
    private static final Logger logger = LogManager.getLogger();
    private static final Neo4jGraphSnapshotLoader NEO4J_LOADER = new Neo4jGraphSnapshotLoader();

    public GraphSnapshot loadSnapshot() {
        String projectKey = ActiveProjectContext.getActiveProjectKey();
        try {
            GraphSnapshot neo4jSnapshot = NEO4J_LOADER.load(projectKey);
            if (neo4jSnapshot == null || neo4jSnapshot.getNodeCount() <= 0) {
                throw new IllegalStateException("graph_snapshot_missing_rebuild");
            }
            return neo4jSnapshot;
        } catch (Exception ex) {
            logger.warn("load neo4j graph snapshot fail: key={} err={}", projectKey, ex.toString());
            throw ex instanceof IllegalStateException
                    ? (IllegalStateException) ex
                    : new IllegalStateException("graph_snapshot_load_failed", ex);
        }
    }

    public static void invalidateCache() {
        Neo4jGraphSnapshotLoader.invalidateAll();
    }
}
