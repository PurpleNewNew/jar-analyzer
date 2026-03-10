package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.ProjectStateUtil;
import me.n1ar4.jar.analyzer.engine.CFRDecompileEngine;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSettingsDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSnapshotDto;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.taint.TaintCache;
import me.n1ar4.jar.analyzer.utils.ClassIndex;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

final class BuildRuntimeFacade implements BuildFacade {
    private static final Logger logger = LogManager.getLogger();
    private static final String DEFAULT_HEAP_HINT = "-Xms2g -Xmx6g";
    private static final String LARGE_PROJECT_HEAP_HINT = "-Xms4g -Xmx8g";

    private final BuildState state;
    private final BuildWorkflowSupport buildWorkflow;
    private final BiFunction<String, String, String> translator;
    private final Supplier<CoreEngine> engineSupplier;
    private final Supplier<String> engineStatusSupplier;
    private final BuildExecutor buildExecutor;

    BuildRuntimeFacade(BuildState state,
                       BuildWorkflowSupport buildWorkflow,
                       Supplier<CoreEngine> engineSupplier,
                       BiFunction<String, String, String> translator,
                       Supplier<String> engineStatusSupplier) {
        this(state, buildWorkflow, engineSupplier, translator, engineStatusSupplier, CoreRunner::run);
    }

    BuildRuntimeFacade(BuildState state,
                       BuildWorkflowSupport buildWorkflow,
                       Supplier<CoreEngine> engineSupplier,
                       BiFunction<String, String, String> translator,
                       Supplier<String> engineStatusSupplier,
                       BuildExecutor buildExecutor) {
        this.state = state;
        this.buildWorkflow = buildWorkflow == null ? new BuildWorkflowSupport((zh, en) -> safe(zh)) : buildWorkflow;
        this.engineSupplier = engineSupplier == null ? () -> null : engineSupplier;
        this.translator = translator == null ? (zh, en) -> safe(zh) : translator;
        this.engineStatusSupplier = engineStatusSupplier == null ? () -> "CLOSED" : engineStatusSupplier;
        this.buildExecutor = buildExecutor == null ? CoreRunner::run : buildExecutor;
    }

    @Override
    public BuildSnapshotDto snapshot() {
        return new BuildSnapshotDto(
                state.buildSettings(),
                safe(engineStatusSupplier.get()),
                state.buildProgress(),
                state.totalJar(),
                state.totalClass(),
                state.totalMethod(),
                state.totalEdge(),
                state.databaseSize(),
                state.buildStatusText(),
                ActiveProjectContext.getActiveProjectKey(),
                ActiveProjectContext.getActiveProjectAlias(),
                ProjectStateUtil.runtimeBuildSeq()
        );
    }

    @Override
    public void apply(BuildSettingsDto settings) {
        if (settings == null) {
            return;
        }
        state.setBuildSettings(settings);
        state.setBuildStatusText(tr("构建设置已更新", "build settings updated"));
    }

    @Override
    public void startBuild() {
        if (!state.tryStartBuild()) {
            return;
        }
        Thread.ofVirtual().name("gui-runtime-build").start(() -> {
            try {
                doBuild();
            } finally {
                state.finishBuild();
            }
        });
    }

    @Override
    public void clearCache() {
        Thread.ofVirtual().name("gui-runtime-clear-cache").start(() -> {
            clearCachesBestEffort();
            state.setBuildProgress(0);
            refreshBuildMetricsSafely(tr("缓存已清理", "cache cleaned"));
        });
    }

    private void doBuild() {
        BuildSettingsDto settings = state.buildSettings();
        BuildWorkflowSupport.SdkResolution sdkResolution = buildWorkflow.resolveSdk(settings);
        if (!sdkResolution.error().isBlank()) {
            state.setBuildStatusText(sdkResolution.error());
            return;
        }
        Path workspaceSdkPath = sdkResolution.sdkPath();
        BuildWorkflowSupport.BuildInputResolution inputResolution = buildWorkflow.resolveBuildInput(settings);
        if (!inputResolution.error().isBlank()) {
            state.setBuildStatusText(inputResolution.error());
            return;
        }
        Path input = inputResolution.inputPath();
        state.setBuildProgress(0);
        state.setBuildStatusText(tr("构建中...", "building..."));
        synchronized (ActiveProjectContext.mutationLock()) {
            buildWorkflow.ensureActiveProject(settings, inputResolution, workspaceSdkPath);
            String previousExtra = buildWorkflow.pushBuildClasspathProperties(inputResolution.extraClasspath());
            try {
                CoreRunner.BuildResult result = buildExecutor.run(
                        input,
                        workspaceSdkPath,
                        settings.fixClassPath(),
                        settings.quickMode(),
                        state::setBuildProgress,
                        settings.resolveNestedJars()
                );
                if (result == null) {
                    state.setBuildStatusText(tr("构建失败", "build failed"));
                    return;
                }
                state.setTotalJar(String.valueOf(result.getJarCount()));
                state.setTotalClass(String.valueOf(result.getClassCount()));
                state.setTotalMethod(String.valueOf(result.getMethodCount()));
                state.setTotalEdge(String.valueOf(result.getEdgeCount()));
                state.setDatabaseSize(result.getDbSizeLabel());
                state.setBuildProgress(100);
                state.setBuildStatusText(tr("构建完成", "build finished"));
                buildWorkflow.saveBuildConfig((inputResolution.selectedInputPath() == null
                        ? settings.activeInputPath()
                        : inputResolution.selectedInputPath().toString()), result, state.language());
            } catch (OutOfMemoryError oom) {
                handleOutOfMemory(oom);
            } catch (Throwable ex) {
                state.setBuildProgress(0);
                logger.error("runtime build failed: {}", stackTrace(ex));
                refreshBuildMetricsSafely(tr("构建异常: ", "build error: ") + safe(ex.getMessage()));
            } finally {
                buildWorkflow.restoreBuildClasspathProperties(previousExtra);
            }
        }
    }

    private void handleOutOfMemory(OutOfMemoryError oom) {
        Runtime runtime = Runtime.getRuntime();
        long maxHeap = runtime.maxMemory();
        long usedHeap = runtime.totalMemory() - runtime.freeMemory();
        String buildStage = safe(CoreRunner.currentBuildStage());
        clearCachesBestEffort();
        System.gc();
        state.setBuildProgress(0);
        refreshBuildMetricsSafely(buildOutOfMemoryStatus(buildStage, maxHeap));
        logger.error(
                "runtime build failed: out of heap (stage={}, maxHeap={}, usedHeap={}): {}",
                buildStage,
                formatMemory(maxHeap),
                formatMemory(usedHeap),
                safe(oom.getMessage())
        );
    }

    private void clearCachesBestEffort() {
        try {
            CFRDecompileEngine.cleanCache();
        } catch (Throwable ex) {
            logger.debug("clear cfr cache failed: {}", ex.toString());
        }
        try {
            CoreEngine engine = engineSupplier.get();
            if (engine != null) {
                engine.clearCallGraphCache();
            }
        } catch (Throwable ex) {
            logger.debug("clear call graph cache failed: {}", ex.toString());
        }
        try {
            GraphStore.invalidateCache();
        } catch (Throwable ex) {
            logger.debug("invalidate graph cache failed: {}", ex.toString());
        }
        try {
            DatabaseManager.clearSemanticCache();
        } catch (Throwable ex) {
            logger.debug("clear semantic cache failed: {}", ex.toString());
        }
        try {
            TaintCache.dfsCache.clear();
            TaintCache.cache.clear();
        } catch (Throwable ex) {
            logger.debug("clear taint cache failed: {}", ex.toString());
        }
        try {
            ClassIndex.refresh();
        } catch (Throwable ex) {
            logger.debug("refresh class index failed: {}", ex.toString());
        }
    }

    private String buildOutOfMemoryStatus(String buildStage, long maxHeap) {
        String stage = safe(buildStage);
        if (stage.isBlank() || "idle".equalsIgnoreCase(stage)) {
            stage = "unknown";
        }
        return tr("构建失败：JVM 堆内存不足（阶段 ",
                "build failed: JVM heap exhausted (stage ")
                + stage
                + tr("，当前最大堆 ", ", current max heap ")
                + formatMemory(maxHeap)
                + tr("）。请使用 ", "); restart with ")
                + DEFAULT_HEAP_HINT
                + tr(" 重新启动；大项目建议 ", "; for larger targets try ")
                + LARGE_PROJECT_HEAP_HINT;
    }

    private void refreshBuildMetrics(String statusText) {
        state.setTotalJar(String.valueOf(DatabaseManager.getJarsMeta().size()));
        state.setTotalClass(String.valueOf(DatabaseManager.getClassReferences().size()));
        state.setTotalMethod(String.valueOf(DatabaseManager.getMethodReferences().size()));
        state.setTotalEdge(buildWorkflow.resolveCurrentEdgeCount());
        state.setDatabaseSize(buildWorkflow.formatDatabaseSize(buildWorkflow.resolveActiveStoreSize()));
        state.setBuildStatusText(statusText);
    }

    private void refreshBuildMetricsSafely(String statusText) {
        try {
            refreshBuildMetrics(statusText);
        } catch (Throwable ex) {
            state.setBuildStatusText(statusText);
            logger.error("refresh build metrics failed: {}", stackTrace(ex));
        }
    }

    private String tr(String zh, String en) {
        return safe(translator.apply(zh, en));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String stackTrace(Throwable ex) {
        if (ex == null) {
            return "";
        }
        StringWriter writer = new StringWriter();
        PrintWriter printer = new PrintWriter(writer);
        ex.printStackTrace(printer);
        printer.flush();
        return writer.toString();
    }

    private static String formatMemory(long bytes) {
        if (bytes <= 0L) {
            return "0 MiB";
        }
        double gib = bytes / (1024.0 * 1024.0 * 1024.0);
        if (gib >= 1.0) {
            return String.format(Locale.ROOT, "%.1f GiB", gib);
        }
        double mib = bytes / (1024.0 * 1024.0);
        return String.format(Locale.ROOT, "%.0f MiB", mib);
    }

    interface BuildState {
        BuildSettingsDto buildSettings();

        void setBuildSettings(BuildSettingsDto settings);

        int buildProgress();

        void setBuildProgress(int progress);

        String buildStatusText();

        void setBuildStatusText(String statusText);

        String totalJar();

        void setTotalJar(String value);

        String totalClass();

        void setTotalClass(String value);

        String totalMethod();

        void setTotalMethod(String value);

        String totalEdge();

        void setTotalEdge(String value);

        String databaseSize();

        void setDatabaseSize(String value);

        int language();

        boolean tryStartBuild();

        void finishBuild();
    }

    interface BuildExecutor {
        CoreRunner.BuildResult run(Path input,
                                   Path runtimeArchive,
                                   boolean fixClassPath,
                                   boolean quickMode,
                                   IntConsumer progressConsumer,
                                   boolean resolveNestedJars);
    }
}
