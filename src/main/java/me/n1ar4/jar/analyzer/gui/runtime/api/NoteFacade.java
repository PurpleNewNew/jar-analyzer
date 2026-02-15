package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.NoteSnapshotDto;

public interface NoteFacade {
    NoteSnapshotDto snapshot();

    void load();

    void openHistory(int index);

    void openFavorite(int index);

    void clearHistory();

    void clearFavorites();
}
