package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.FlowSettingsDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.FlowSnapshotDto;

public interface FlowFacade {
    FlowSnapshotDto snapshot();

    void apply(FlowSettingsDto settings);

    void startDfs();

    void startTaint();

    void clearResults();

    void openAdvanceSettings();

    void setSource(String className, String methodName, String methodDesc);

    void setSink(String className, String methodName, String methodDesc);
}
