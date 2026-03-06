/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSettingsDto;
import me.n1ar4.support.DatabaseManagerTestHook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class BuildFacadeParityTest {
    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
    }

    @Test
    void applyShouldRoundTripSettings() {
        boolean[] bools = new boolean[]{false, true};
        for (boolean fixClassPath : bools) {
            for (boolean quickMode : bools) {
                BuildSettingsDto settings = new BuildSettingsDto(
                        "/tmp/input.jar",
                        "/tmp/rt.jar",
                        true,
                        fixClassPath,
                        quickMode
                );
                RuntimeFacades.build().apply(settings);
                BuildSettingsDto snapshot = RuntimeFacades.build().snapshot().settings();
                assertEquals("/tmp/input.jar", snapshot.activeInputPath());
                assertEquals("/tmp/rt.jar", snapshot.sdkPath());
                assertEquals(true, snapshot.resolveNestedJars());
                assertEquals(fixClassPath, snapshot.fixClassPath());
                assertEquals(quickMode, snapshot.quickMode());
            }
        }
    }

    @Test
    void clearCacheShouldKeepActiveProjectMetadata() throws Exception {
        DatabaseManager.clearAllData();
        DatabaseManager.runAtomicUpdate(() -> {
            DatabaseManager.saveMethods(java.util.Set.of(new MethodReference(
                    new ClassReference.Handle("demo/Cached", 1),
                    "run",
                    "()V",
                    false,
                    java.util.Set.of(),
                    1,
                    10,
                    "app.jar",
                    1
            )));
            DatabaseManagerTestHook.markProjectBuildReady(7L);
        });

        RuntimeFacades.build().clearCache();
        waitForCacheCleanup();

        assertEquals(1, DatabaseManager.getMethodReferences().size());
        assertEquals(7L, DatabaseManager.getProjectBuildSeq());
    }

    private static void waitForCacheCleanup() throws Exception {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            String status = RuntimeFacades.build().snapshot().statusText();
            if ("cache cleaned".equals(status) || "缓存已清理".equals(status)) {
                return;
            }
            Thread.sleep(20L);
        }
        fail("clearCache did not finish in time");
    }
}
