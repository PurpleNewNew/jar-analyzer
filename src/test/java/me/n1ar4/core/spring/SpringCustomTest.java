/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.core.spring;

import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.support.FixtureJars;
import me.n1ar4.support.Neo4jTestGraph;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class SpringCustomTest {
    @Test
    @SuppressWarnings("all")
    public void testRun() {
        try {
            Path file = FixtureJars.springbootTestJar();

            WorkspaceContext.updateResolveInnerJars(false);
            CoreRunner.BuildResult build = CoreRunner.run(file, null, false, false, true, null, true);

            int springControllers = DatabaseManager.getSpringControllers().size();
            assertEquals(4, springControllers,
                    "spring controller count should be 4");

            int classCount = DatabaseManager.getClassReferences().size();
            System.out.println("class reference count: " + classCount);
            assertEquals(77, classCount, "class count should be 77");

            long methodCallCount = build.getEdgeCount();
            System.out.println("build edge count: " + methodCallCount);
            assertTrue(methodCallCount >= 1931,
                    "build edge count should be at least 1931");

            int lambdaCount = Neo4jTestGraph.countCallEdges(call ->
                    call.edge().getEvidence().contains("lambda"));
            System.out.println("lambda inferred call edges: " + lambdaCount);
            assertTrue(lambdaCount > 0, "graph should contain lambda inferred call edges");

            System.out.println("所有数据库验证通过！");

        } catch (Exception e) {
            e.printStackTrace();
            fail("测试执行失败: " + e.getMessage());
        }
    }
}
