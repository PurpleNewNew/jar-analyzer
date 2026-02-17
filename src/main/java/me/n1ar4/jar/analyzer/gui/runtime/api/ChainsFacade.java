package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.ChainsSettingsDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ChainsSnapshotDto;

public interface ChainsFacade {
    ChainsSnapshotDto snapshot();

    void apply(ChainsSettingsDto settings);

    void startDfs();

    void startTaint();

    void clearResults();

    void openAdvanceSettings();

    void setSource(String className, String methodName, String methodDesc);

    void setSink(String className, String methodName, String methodDesc);
}
