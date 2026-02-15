package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.SearchQueryDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchResultDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchSnapshotDto;

import java.util.List;

public interface SearchFacade {
    SearchSnapshotDto snapshot();

    void applyQuery(SearchQueryDto query);

    void runSearch();

    void openResult(int index);

    void publishExternalResults(List<SearchResultDto> results, String statusText);
}
