package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.engine.CFRDecompileEngine;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.utils.ClassIndex;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.function.Supplier;

public final class ProjectScopedRuntimeCleaner {
    private static final Logger logger = LogManager.getLogger();

    private ProjectScopedRuntimeCleaner() {
    }

    public static void clearFlowResults() {
        try {
            FlowResultStore.getInstance().clear();
        } catch (Throwable ex) {
            logger.debug("clear flow results failed: {}", ex.toString());
        }
    }

    public static void clearDerivedCaches(Supplier<CoreEngine> engineSupplier) {
        try {
            CFRDecompileEngine.cleanCache();
        } catch (Throwable ex) {
            logger.debug("clear cfr cache failed: {}", ex.toString());
        }
        try {
            CoreEngine engine = engineSupplier == null ? null : engineSupplier.get();
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
            ClassIndex.refresh();
        } catch (Throwable ex) {
            logger.debug("refresh class index failed: {}", ex.toString());
        }
    }

    public static void resetProjectScopedRuntime(String editorStatusText,
                                                 String flowStatusText,
                                                 Supplier<CoreEngine> engineSupplier) {
        RuntimeFacades.clearProjectScopedEditorState(editorStatusText);
        RuntimeFacades.clearProjectScopedFlowState(flowStatusText);
        clearDerivedCaches(engineSupplier);
    }
}
