package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.GadgetSettingsDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.GadgetSnapshotDto;

public interface GadgetFacade {
    GadgetSnapshotDto snapshot();

    void apply(GadgetSettingsDto settings);

    void chooseDir();

    void start();
}
