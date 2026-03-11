/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.engine;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Global CoreEngine holder decoupled from GUI classes.
 * <p>
 * Historically many modules referenced a Swing UI singleton to reach the engine, which tightly coupled
 * server/engine code to the desktop UI. This holder provides the same capability without that dependency.
 */
public final class EngineContext {
    private static final Logger logger = LogManager.getLogger();
    private static final AtomicReference<CoreEngine> ENGINE = new AtomicReference<>();
    private static final AtomicLong PREWARM_REQUEST_SEQ = new AtomicLong(0L);
    private static final ExecutorService PREWARM_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "jar-analyzer-engine-prewarm");
        thread.setDaemon(true);
        return thread;
    });

    private EngineContext() {
    }

    public static CoreEngine getEngine() {
        return ENGINE.get();
    }

    public static void setEngine(CoreEngine engine) {
        ENGINE.set(engine);
    }

    public static boolean scheduleProjectPrewarm(String projectKey,
                                                 long buildSeq,
                                                 Consumer<PrewarmReport> completion) {
        String normalizedProjectKey = ActiveProjectContext.normalizeProjectKey(projectKey);
        if (normalizedProjectKey.isBlank() || buildSeq <= 0L) {
            return false;
        }
        long requestSeq = PREWARM_REQUEST_SEQ.incrementAndGet();
        PREWARM_EXECUTOR.execute(() -> runProjectPrewarm(requestSeq, normalizedProjectKey, buildSeq, completion));
        return true;
    }

    private static void runProjectPrewarm(long requestSeq,
                                          String projectKey,
                                          long buildSeq,
                                          Consumer<PrewarmReport> completion) {
        long totalStartNs = System.nanoTime();
        long querySnapshotMs = 0L;
        long flowSnapshotMs = 0L;
        long callGraphCacheMs = 0L;
        String status = "completed";
        String error = null;
        try {
            if (!isPrewarmCurrent(requestSeq, projectKey, buildSeq)) {
                status = "stale";
                return;
            }
            GraphStore graphStore = new GraphStore();
            long queryStartNs = System.nanoTime();
            ActiveProjectContext.withProject(projectKey, () -> graphStore.loadQuerySnapshot(projectKey));
            querySnapshotMs = elapsedMs(queryStartNs);
            if (!isPrewarmCurrent(requestSeq, projectKey, buildSeq)) {
                status = "stale";
                return;
            }
            long flowStartNs = System.nanoTime();
            ActiveProjectContext.withProject(projectKey, () -> graphStore.loadFlowSnapshot(projectKey));
            flowSnapshotMs = elapsedMs(flowStartNs);
            if (!isPrewarmCurrent(requestSeq, projectKey, buildSeq)) {
                status = "stale";
                return;
            }
            CoreEngine engine = getEngine();
            if (engine == null) {
                status = "no_engine";
                return;
            }
            long callGraphStartNs = System.nanoTime();
            ActiveProjectContext.withProject(projectKey, () -> engine.getCallGraphCache());
            callGraphCacheMs = elapsedMs(callGraphStartNs);
            if (!isPrewarmCurrent(requestSeq, projectKey, buildSeq)) {
                status = "stale";
                return;
            }
        } catch (Exception ex) {
            if (!isPrewarmCurrent(requestSeq, projectKey, buildSeq)) {
                status = "stale";
            } else {
                status = "failed";
                error = ex.toString();
                logger.warn("background prewarm failed: key={} buildSeq={} err={}", projectKey, buildSeq, ex.toString());
            }
        } finally {
            PrewarmReport report = new PrewarmReport(
                    projectKey,
                    buildSeq,
                    status,
                    querySnapshotMs,
                    flowSnapshotMs,
                    callGraphCacheMs,
                    elapsedMs(totalStartNs),
                    error
            );
            if (report.completed()) {
                logger.info("background prewarm finished: key={} buildSeq={} queryMs={} flowMs={} callGraphMs={} totalMs={}",
                        projectKey,
                        buildSeq,
                        report.querySnapshotMs(),
                        report.flowSnapshotMs(),
                        report.callGraphCacheMs(),
                        report.totalMs());
            } else if ("stale".equals(report.status())) {
                logger.debug("background prewarm stale: key={} buildSeq={} totalMs={}",
                        projectKey,
                        buildSeq,
                        report.totalMs());
            } else {
                logger.info("background prewarm skipped: key={} buildSeq={} status={} totalMs={}",
                        projectKey,
                        buildSeq,
                        report.status(),
                        report.totalMs());
            }
            if (completion != null) {
                try {
                    completion.accept(report);
                } catch (Exception ex) {
                    logger.debug("background prewarm completion callback fail: {}", ex.toString());
                }
            }
        }
    }

    private static boolean isPrewarmCurrent(long requestSeq,
                                            String projectKey,
                                            long buildSeq) {
        if (requestSeq != PREWARM_REQUEST_SEQ.get()) {
            return false;
        }
        String normalizedProjectKey = ActiveProjectContext.normalizeProjectKey(projectKey);
        if (normalizedProjectKey.isBlank()) {
            return false;
        }
        if (!normalizedProjectKey.equals(ActiveProjectContext.normalizeProjectKey(
                ActiveProjectContext.getPublishedActiveProjectKey()))) {
            return false;
        }
        if (ActiveProjectContext.isProjectMutationInProgress(normalizedProjectKey)
                || DatabaseManager.isBuilding(normalizedProjectKey)) {
            return false;
        }
        if (!normalizedProjectKey.equals(ActiveProjectContext.normalizeProjectKey(ProjectRuntimeContext.projectKey()))) {
            return false;
        }
        if (ProjectRuntimeContext.buildSeq() != Math.max(0L, buildSeq)) {
            return false;
        }
        return DatabaseManager.isProjectReady(normalizedProjectKey);
    }

    private static long elapsedMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000L;
    }

    public record PrewarmReport(String projectKey,
                                long buildSeq,
                                String status,
                                long querySnapshotMs,
                                long flowSnapshotMs,
                                long callGraphCacheMs,
                                long totalMs,
                                String error) {
        public boolean completed() {
            return "completed".equals(status);
        }
    }
}
