package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.CallGraphSnapshotDto;

public interface CallGraphFacade {
    CallGraphSnapshotDto snapshot();

    void refreshCurrentContext();

    void openAllMethod(int index);

    void openCaller(int index);

    void openCallee(int index);

    void openImpl(int index);

    void openSuperImpl(int index);
}
