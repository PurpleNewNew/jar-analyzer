package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.gui.runtime.model.TreeNodeDto;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

final class ProjectTreeRuntimeFacade implements ProjectTreeFacade {
    private final TreeState state;
    private final ProjectTreeSupport treeSupport;
    private final Supplier<CoreEngine> engineSupplier;
    private final Runnable classIndexRefresh;

    ProjectTreeRuntimeFacade(TreeState state,
                             ProjectTreeSupport treeSupport,
                             Supplier<CoreEngine> engineSupplier,
                             Runnable classIndexRefresh) {
        this.state = state;
        this.treeSupport = treeSupport == null
                ? new ProjectTreeSupport(ProjectTreeSupport.UiActions.noop())
                : treeSupport;
        this.engineSupplier = engineSupplier == null ? () -> null : engineSupplier;
        this.classIndexRefresh = classIndexRefresh == null ? () -> {
        } : classIndexRefresh;
    }

    @Override
    public List<TreeNodeDto> snapshot() {
        CoreEngine engine = engineSupplier.get();
        if (engine == null || !engine.isEnabled()) {
            return List.of();
        }
        return treeSupport.buildTree(null, currentTreeSettings());
    }

    @Override
    public List<TreeNodeDto> search(String keyword) {
        CoreEngine engine = engineSupplier.get();
        if (engine == null || !engine.isEnabled()) {
            return List.of();
        }
        String key = safe(keyword).trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) {
            return treeSupport.buildTree(null, currentTreeSettings());
        }
        return treeSupport.buildTree(key, currentTreeSettings());
    }

    @Override
    public void openNode(String value) {
        treeSupport.openNode(value);
    }

    @Override
    public void refresh() {
        classIndexRefresh.run();
    }

    private ProjectTreeSupport.TreeSettings currentTreeSettings() {
        return new ProjectTreeSupport.TreeSettings(
                state.showInnerClass(),
                state.groupTreeByJar(),
                state.mergePackageRoot()
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    interface TreeState {
        boolean showInnerClass();

        boolean groupTreeByJar();

        boolean mergePackageRoot();
    }
}
