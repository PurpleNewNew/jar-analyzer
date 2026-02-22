/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.edge;

import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.support.Neo4jTestGraph;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpringFrameworkEdgeInferenceTest {
    @Test
    public void shouldInferSpringFrameworkEdges() throws Exception {
        Path jar = FixtureJars.springbootTestJar();
        WorkspaceContext.updateResolveInnerJars(false);
        CoreRunner.run(jar, null, false, false, true, null, true);

        int frameworkCount = Neo4jTestGraph.countCallEdges(call ->
                "framework".equals(Neo4jTestGraph.resolveEdgeType(call.edge())));
        assertTrue(frameworkCount > 0, "framework rule should add edges");

        int webEntryCount = Neo4jTestGraph.countCallEdges(call ->
                "framework".equals(Neo4jTestGraph.resolveEdgeType(call.edge()))
                        && call.edge().getEvidence().contains("spring_web_entry"));
        assertTrue(webEntryCount > 0, "framework rule should infer spring web entry edges");

        assertTrue(Neo4jTestGraph.existsCallEdge(
                        "me/n1ar4/test/TestApplication",
                        "main",
                        null,
                        "me/n1ar4/test/demos/web/DataController",
                        "getStatus",
                        null,
                        "framework",
                        null),
                "main should connect to controller mapping method");
    }
}
