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

import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.function.Supplier;

final class BuildRuntimeFacade implements BuildFacade {
    private static final Logger logger = LogManager.getLogger();

    private final BuildState state;
    private final BuildWorkflowSupport buildWorkflow;
    private final BiFunction<String, String, String> translator;
    private final Supplier<CoreEngine> engineSupplier;
    private final Supplier<String> engineStatusSupplier;

    BuildRuntimeFacade(BuildState state,
                       BuildWorkflowSupport buildWorkflow,
                       Supplier<CoreEngine> engineSupplier,
                       BiFunction<String, String, String> translator,
                       Supplier<String> engineStatusSupplier) {
        this.state = state;
        this.buildWorkflow = buildWorkflow == null ? new BuildWorkflowSupport((zh, en) -> safe(zh)) : buildWorkflow;
        this.engineSupplier = engineSupplier == null ? () -> null : engineSupplier;
        this.translator = translator == null ? (zh, en) -> safe(zh) : translator;
        this.engineStatusSupplier = engineStatusSupplier == null ? () -> "CLOSED" : engineStatusSupplier;
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
            state.setBuildProgress(0);
            refreshBuildMetrics(tr("缓存已清理", "cache cleaned"));
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
        Path rtPath = sdkResolution.runtimeArchivePath();
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
                CoreRunner.BuildResult result = CoreRunner.run(
                        input,
                        rtPath,
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
            } catch (Throwable ex) {
                refreshBuildMetrics(tr("构建异常: ", "build error: ") + safe(ex.getMessage()));
                logger.error("runtime build failed: {}", ex.toString());
            } finally {
                buildWorkflow.restoreBuildClasspathProperties(previousExtra);
            }
        }
    }

    private void refreshBuildMetrics(String statusText) {
        state.setTotalJar(String.valueOf(DatabaseManager.getJarsMeta().size()));
        state.setTotalClass(String.valueOf(DatabaseManager.getClassReferences().size()));
        state.setTotalMethod(String.valueOf(DatabaseManager.getMethodReferences().size()));
        state.setTotalEdge(buildWorkflow.resolveCurrentEdgeCount());
        state.setDatabaseSize(buildWorkflow.formatDatabaseSize(buildWorkflow.resolveActiveStoreSize()));
        state.setBuildStatusText(statusText);
    }

    private String tr(String zh, String en) {
        return safe(translator.apply(zh, en));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
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
}
