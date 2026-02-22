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

import me.n1ar4.jar.analyzer.core.pta.ContextSensitivePtaEngine;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.support.Neo4jTestGraph;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PtaOnTheFlySpringClosureTest {
    private static final String PROP_MODE = "jar.analyzer.callgraph.mode";
    private static final String PROP_EDGE_INFER = "jar.analyzer.edge.infer.enable";
    private static final String PROP_ONFLY = "jar.analyzer.pta.semantic.onfly.enable";
    private static final String PROP_INCREMENTAL = "jar.analyzer.pta.incremental";

    @Test
    public void shouldKeepSpringFrameworkSemanticsWhenPostEdgePipelineDisabled() throws Exception {
        String oldMode = System.getProperty(PROP_MODE);
        String oldInfer = System.getProperty(PROP_EDGE_INFER);
        String oldOnFly = System.getProperty(PROP_ONFLY);
        String oldIncremental = System.getProperty(PROP_INCREMENTAL);
        try {
            ContextSensitivePtaEngine.clearIncrementalCache();
            System.setProperty(PROP_MODE, "pta");
            System.setProperty(PROP_EDGE_INFER, "false");
            System.setProperty(PROP_ONFLY, "true");
            System.setProperty(PROP_INCREMENTAL, "false");

            Path jar = FixtureJars.springbootTestJar();
            WorkspaceContext.updateResolveInnerJars(false);
            CoreRunner.run(jar, null, false, false, true, null, true);

            int framework = Neo4jTestGraph.countCallEdges(call ->
                    "framework".equals(Neo4jTestGraph.resolveEdgeType(call.edge())));
            assertTrue(framework > 0, "framework semantic edges should be preserved by pta on-the-fly plugin");

            int webEntry = Neo4jTestGraph.countCallEdges(call ->
                    "framework".equals(Neo4jTestGraph.resolveEdgeType(call.edge()))
                            && call.edge().getEvidence().contains("spring_web_entry"));
            assertTrue(webEntry > 0, "spring web-entry semantics should remain available");

            assertTrue(Neo4jTestGraph.existsCallEdge(
                            "me/n1ar4/test/TestApplication",
                            "main",
                            null,
                            "me/n1ar4/test/demos/web/DataController",
                            "getStatus",
                            null,
                            "framework",
                            null),
                    "main should connect to controller mapping under on-the-fly mode");
        } finally {
            ContextSensitivePtaEngine.clearIncrementalCache();
            restoreProperty(PROP_MODE, oldMode);
            restoreProperty(PROP_EDGE_INFER, oldInfer);
            restoreProperty(PROP_ONFLY, oldOnFly);
            restoreProperty(PROP_INCREMENTAL, oldIncremental);
        }
    }

    private static void restoreProperty(String key, String oldValue) {
        if (oldValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, oldValue);
        }
    }
}
