package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSettingsDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSnapshotDto;

public interface BuildFacade {
    BuildSnapshotDto snapshot();

    void apply(BuildSettingsDto settings);

    void startBuild();

    void clearCache();
}
