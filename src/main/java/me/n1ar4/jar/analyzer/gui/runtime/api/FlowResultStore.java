package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.core.ProjectStateUtil;
import me.n1ar4.jar.analyzer.graph.flow.model.FlowPath;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.taint.TaintResult;

import java.util.ArrayList;
import java.util.List;

final class FlowResultStore {
    private static final FlowResultStore INSTANCE = new FlowResultStore();

    private volatile Snapshot snapshot = Snapshot.empty();

    private FlowResultStore() {
    }

    static FlowResultStore getInstance() {
        return INSTANCE;
    }

    static Context captureCurrentContext() {
        String projectKey = ActiveProjectContext.normalizeProjectKey(
                ActiveProjectContext.getPublishedActiveProjectKey());
        long buildSeq = projectKey.isBlank() ? 0L : ProjectStateUtil.projectBuildSnapshot(projectKey);
        long projectEpoch = ActiveProjectContext.currentEpoch();
        return new Context(projectKey, buildSeq, projectEpoch);
    }

    Snapshot snapshot() {
        Snapshot current = snapshot;
        if (isCurrent(current.context())) {
            return current;
        }
        synchronized (this) {
            current = snapshot;
            if (!isCurrent(current.context())) {
                snapshot = Snapshot.empty();
                return snapshot;
            }
            return current;
        }
    }

    synchronized boolean replaceDfs(Context context, List<FlowPath> dfsResults) {
        if (!isCurrent(context)) {
            snapshot = Snapshot.empty();
            return false;
        }
        snapshot = new Snapshot(
                context.projectKey(),
                context.buildSeq(),
                context.projectEpoch(),
                copyFlowPaths(dfsResults),
                List.of()
        );
        return true;
    }

    synchronized boolean replaceTaint(Context context, List<TaintResult> taintResults) {
        if (!isCurrent(context)) {
            snapshot = Snapshot.empty();
            return false;
        }
        List<FlowPath> dfsResults = matchesContext(snapshot, context) ? snapshot.dfsResults() : List.of();
        snapshot = new Snapshot(
                context.projectKey(),
                context.buildSeq(),
                context.projectEpoch(),
                dfsResults,
                copyTaintResults(taintResults)
        );
        return true;
    }

    synchronized void clear() {
        snapshot = Snapshot.empty();
    }

    private static boolean matchesContext(Snapshot current, Context context) {
        return current != null
                && context != null
                && current.projectKey().equals(context.projectKey())
                && current.buildSeq() == context.buildSeq()
                && current.projectEpoch() == context.projectEpoch();
    }

    private static boolean isCurrent(Context context) {
        if (context == null) {
            return false;
        }
        String projectKey = ActiveProjectContext.normalizeProjectKey(context.projectKey());
        if (projectKey.isBlank()) {
            return false;
        }
        String activeProjectKey = ActiveProjectContext.normalizeProjectKey(
                ActiveProjectContext.getPublishedActiveProjectKey());
        if (!projectKey.equals(activeProjectKey)) {
            return false;
        }
        if (context.projectEpoch() > 0L && context.projectEpoch() != ActiveProjectContext.currentEpoch()) {
            return false;
        }
        return context.buildSeq() <= 0L || !ProjectStateUtil.isProjectBuildStale(projectKey, context.buildSeq());
    }

    private static List<FlowPath> copyFlowPaths(List<FlowPath> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<FlowPath> out = new ArrayList<>(rows.size());
        for (FlowPath row : rows) {
            if (row != null) {
                out.add(row);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static List<TaintResult> copyTaintResults(List<TaintResult> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<TaintResult> out = new ArrayList<>(rows.size());
        for (TaintResult row : rows) {
            if (row != null) {
                out.add(row);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    record Context(String projectKey, long buildSeq, long projectEpoch) {
    }

    record Snapshot(String projectKey,
                    long buildSeq,
                    long projectEpoch,
                    List<FlowPath> dfsResults,
                    List<TaintResult> taintResults) {
        private static Snapshot empty() {
            return new Snapshot("", 0L, 0L, List.of(), List.of());
        }

        Context context() {
            return new Context(projectKey, buildSeq, projectEpoch);
        }
    }
}
