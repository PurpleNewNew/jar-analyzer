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

import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.support.Neo4jTestGraph;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PtaSpringStabilityTest {
    private static final String MODE_PROP = "jar.analyzer.callgraph.mode";

    @Test
    public void shouldKeepSpringCoverageStableInPtaMode() throws Exception {
        String oldMode = System.getProperty(MODE_PROP);
        try {
            Path jar = FixtureJars.springbootTestJar();
            WorkspaceContext.updateResolveInnerJars(false);

            System.setProperty(MODE_PROP, "rta");
            CoreRunner.BuildResult rta = CoreRunner.run(jar, null, false, false, true, null, true);

            System.setProperty(MODE_PROP, "pta");
            CoreRunner.BuildResult pta = CoreRunner.run(jar, null, false, false, true, null, true);

            assertTrue(pta.getEdgeCount() >= rta.getEdgeCount(),
                    "pta mode should not regress edge coverage on spring sample");
            assertTrue(pta.getDispatchEdgesAdded() >= rta.getDispatchEdgesAdded(),
                    "pta mode should keep at least baseline dispatch coverage");
            assertTrue(pta.getPtaContextMethodsProcessed() > 0,
                    "pta mode should process context methods on spring sample");

            int framework = Neo4jTestGraph.countCallEdges(call ->
                    "framework".equals(Neo4jTestGraph.resolveEdgeType(call.edge())));
            assertTrue(framework > 0, "spring framework semantic edges should remain available");
        } finally {
            restoreProperty(oldMode);
        }
    }

    private static void restoreProperty(String old) {
        if (old == null) {
            System.clearProperty(MODE_PROP);
        } else {
            System.setProperty(MODE_PROP, old);
        }
    }
}
