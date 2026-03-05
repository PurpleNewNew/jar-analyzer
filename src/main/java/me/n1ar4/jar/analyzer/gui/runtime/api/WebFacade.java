package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.WebSnapshotDto;

public interface WebFacade {
    WebSnapshotDto snapshot();

    void refreshAll();

    void pathSearch(String keyword);
}
