package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.CallGraphSnapshotDto;

public interface CallGraphFacade {
    CallGraphSnapshotDto snapshot();

    void refreshCurrentContext();

    String scope();

    void setScope(String scope);
}
