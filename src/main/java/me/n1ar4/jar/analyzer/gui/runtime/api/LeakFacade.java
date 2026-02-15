package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.LeakRulesDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.LeakSnapshotDto;

public interface LeakFacade {
    LeakSnapshotDto snapshot();

    void apply(LeakRulesDto rules);

    void start();

    void clear();

    void export();

    void openResult(int index);
}
