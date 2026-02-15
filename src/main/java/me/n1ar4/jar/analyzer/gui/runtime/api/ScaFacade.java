package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.ScaSettingsDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ScaSnapshotDto;

public interface ScaFacade {
    ScaSnapshotDto snapshot();

    void apply(ScaSettingsDto settings);

    void chooseInput();

    void start();

    void openResult();
}
