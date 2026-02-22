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

import me.n1ar4.jar.analyzer.analyze.spring.SpringController;
import me.n1ar4.jar.analyzer.analyze.spring.SpringMapping;
import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class SpringCoreTest {
    @Test
    @SuppressWarnings("all")
    public void testRun() {
        try {
            Path file = FixtureJars.springbootTestJar();

            WorkspaceContext.updateResolveInnerJars(false);
            CoreRunner.run(file, null, false, false, true, null, true);

            int controllers = DatabaseManager.getSpringControllers().size();
            System.out.println("spring controller count: " + controllers);
            assertEquals(4, controllers, "spring controller count should be 4");

            long mappingsWithPath = 0L;
            for (SpringController controller : DatabaseManager.getSpringControllers()) {
                if (controller == null) {
                    continue;
                }
                for (SpringMapping mapping : controller.getMappings()) {
                    if (mapping == null) {
                        continue;
                    }
                    String path = mapping.getPathRestful();
                    if (path == null || path.isBlank()) {
                        path = mapping.getPath();
                    }
                    if (path != null && !path.isBlank()) {
                        mappingsWithPath++;
                    }
                }
            }
            System.out.println("spring mapping count with path: " + mappingsWithPath);
            assertTrue(mappingsWithPath >= 6L,
                    "spring mapping count with path should be at least 6");

            System.out.println("所有数据库验证通过！");

        } catch (Exception e) {
            e.printStackTrace();
            fail("测试执行失败: " + e.getMessage());
        }
    }
}
