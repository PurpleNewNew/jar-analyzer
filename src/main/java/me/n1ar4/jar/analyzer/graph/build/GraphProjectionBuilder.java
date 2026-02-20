/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.graph.build;

import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

public final class GraphProjectionBuilder {
    private static final Logger logger = LogManager.getLogger();

    private GraphProjectionBuilder() {
    }

    public static void projectCurrentBuild(long buildSeq, boolean quickMode, String callGraphMode) {
        GraphStore.invalidateCache();
        logger.info("graph projection skipped (neo4j direct mode): buildSeq={} quickMode={} callGraphMode={}",
                buildSeq, quickMode, callGraphMode);
    }
}
