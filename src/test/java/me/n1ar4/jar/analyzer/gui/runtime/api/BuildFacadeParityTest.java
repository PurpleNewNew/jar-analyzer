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
import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.gui.GlobalOptions;
import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSettingsDto;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectRegistryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class BuildFacadeParityTest {
    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
        ProjectRuntimeContext.clear();
        ProjectRegistryService.getInstance().cleanupTemporaryProject();
        ProjectRegistryService.getInstance().activateTemporaryProject();
    }

    @Test
    void applyShouldRoundTripSettings() {
        boolean[] bools = new boolean[]{false, true};
        for (boolean fixClassPath : bools) {
            BuildSettingsDto settings = new BuildSettingsDto(
                    "/tmp/input.jar",
                    "/tmp/rt.jar",
                    true,
                    fixClassPath
            );
            RuntimeFacades.build().apply(settings);
            BuildSettingsDto snapshot = RuntimeFacades.build().snapshot().settings();
            assertEquals("/tmp/input.jar", snapshot.activeInputPath());
            assertEquals("/tmp/rt.jar", snapshot.sdkPath());
            assertEquals(true, snapshot.resolveNestedJars());
            assertEquals(fixClassPath, snapshot.fixClassPath());
        }
    }

    @Test
    void clearCacheShouldKeepActiveProjectMetadata() throws Exception {
        DatabaseManager.clearAllData();
        MethodReference method = new MethodReference(
                new ClassReference.Handle("demo/Cached", 1),
                "run",
                "()V",
                false,
                java.util.Set.of(),
                1,
                10,
                "app.jar",
                1
        );
        ProjectRuntimeSnapshot snapshot = DatabaseManager.buildProjectRuntimeSnapshot(
                7L,
                me.n1ar4.jar.analyzer.engine.project.ProjectModel.artifact(
                        Path.of("/tmp/input.jar"),
                        null,
                        List.of(Path.of("/tmp/input.jar")),
                        false
                ),
                List.of("/tmp/input.jar"),
                java.util.Set.of(),
                java.util.Set.of(),
                java.util.Set.of(method),
                java.util.Map.of(),
                java.util.Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        DatabaseManager.restoreProjectRuntime(snapshot);
        ProjectRuntimeContext.restoreProjectRuntime(
                "",
                snapshot.buildSeq(),
                DatabaseManager.getProjectModel()
        );

        RuntimeFacades.build().clearCache();
        waitForCacheCleanup();

        assertEquals(1, DatabaseManager.getMethodReferences().size());
        assertEquals(7L, DatabaseManager.getProjectBuildSeq());
    }

    @Test
    void outOfMemoryShouldReportHeapHint() throws Exception {
        Path input = java.nio.file.Files.createTempFile("build-facade-oom", ".jar");
        try {
            TestBuildState state = new TestBuildState(new BuildSettingsDto(input.toString(), "", false, false));
            BuildRuntimeFacade facade = new BuildRuntimeFacade(
                    state,
                    new BuildWorkflowSupport((zh, en) -> zh),
                    () -> null,
                    (zh, en) -> zh,
                    () -> "CLOSED",
                    (jar, runtimeArchive, fixClassPath, progressConsumer, resolveNestedJars) -> {
                        throw new OutOfMemoryError("Java heap space");
                    }
            );

            facade.startBuild();
            assertTrue(state.awaitFinish(), "build did not finish in time");
            assertTrue(state.statusText().contains("堆内存不足"));
            assertTrue(state.statusText().contains("-Xms2g -Xmx6g"));
            assertTrue(state.statusText().contains("-Xms4g -Xmx8g"));
            assertEquals(0, state.buildProgress());
        } finally {
            java.nio.file.Files.deleteIfExists(input);
        }
    }

    @Test
    void blankSdkShouldNotInjectRuntimeIntoBuildExecutor() throws Exception {
        Path input = java.nio.file.Files.createTempFile("build-facade-no-sdk", ".jar");
        AtomicReference<Path> runtimeArchive = new AtomicReference<>(Path.of("sentinel"));
        try {
            TestBuildState state = new TestBuildState(new BuildSettingsDto(input.toString(), "", true, false));
            BuildRuntimeFacade facade = new BuildRuntimeFacade(
                    state,
                    new BuildWorkflowSupport((zh, en) -> en),
                    () -> null,
                    (zh, en) -> en,
                    () -> "CLOSED",
                    (jar, runtimePath, fixClassPath, progressConsumer, resolveNestedJars) -> {
                        runtimeArchive.set(runtimePath);
                        progressConsumer.accept(100);
                        return new CoreRunner.BuildResult(
                                1L,
                                1,
                                1,
                                1,
                                1,
                                0,
                                0L,
                                0L,
                                "0.00 MB",
                                "bytecode-mainline",
                                "balanced",
                                "balanced",
                                1,
                                0,
                                0,
                                0L,
                                0L,
                                0L,
                                0L,
                                Map.of()
                        );
                    }
            );

            facade.startBuild();
            assertTrue(state.awaitFinish(), "build did not finish in time");
            assertNull(runtimeArchive.get());
            assertEquals("build finished", state.statusText());
        } finally {
            java.nio.file.Files.deleteIfExists(input);
        }
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

    private static final class TestBuildState implements BuildRuntimeFacade.BuildState {
        private final CountDownLatch finished = new CountDownLatch(1);
        private final BuildSettingsDto settings;
        private volatile int progress;
        private volatile String statusText = "";
        private boolean started;

        private TestBuildState(BuildSettingsDto settings) {
            this.settings = settings;
        }

        @Override
        public BuildSettingsDto buildSettings() {
            return settings;
        }

        @Override
        public void setBuildSettings(BuildSettingsDto settings) {
        }

        @Override
        public int buildProgress() {
            return progress;
        }

        @Override
        public void setBuildProgress(int progress) {
            this.progress = progress;
        }

        @Override
        public String buildStatusText() {
            return statusText;
        }

        @Override
        public void setBuildStatusText(String statusText) {
            this.statusText = statusText;
        }

        @Override
        public String totalJar() {
            return "0";
        }

        @Override
        public void setTotalJar(String value) {
        }

        @Override
        public String totalClass() {
            return "0";
        }

        @Override
        public void setTotalClass(String value) {
        }

        @Override
        public String totalMethod() {
            return "0";
        }

        @Override
        public void setTotalMethod(String value) {
        }

        @Override
        public String totalEdge() {
            return "0";
        }

        @Override
        public void setTotalEdge(String value) {
        }

        @Override
        public String databaseSize() {
            return "0.00 MB";
        }

        @Override
        public void setDatabaseSize(String value) {
        }

        @Override
        public int language() {
            return GlobalOptions.CHINESE;
        }

        @Override
        public synchronized boolean tryStartBuild() {
            if (started) {
                return false;
            }
            started = true;
            return true;
        }

        @Override
        public void finishBuild() {
            finished.countDown();
        }

        private boolean awaitFinish() throws InterruptedException {
            return finished.await(2, TimeUnit.SECONDS);
        }

        private String statusText() {
            return statusText;
        }
    }
}
