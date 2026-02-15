package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.SearchQueryDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchSnapshotDto;

public interface SearchFacade {
    SearchSnapshotDto snapshot();

    void applyQuery(SearchQueryDto query);

    void runSearch();

    void openResult(int index);
}
