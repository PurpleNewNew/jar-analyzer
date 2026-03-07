package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.CallGraphScope;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchMatchMode;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchMode;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchQueryDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchWorkflowSupportTest {
    private final SearchWorkflowSupport support = new SearchWorkflowSupport((zh, en) -> en);

    @Test
    void contributorSearchRequiresAtLeastOneContributor() {
        SearchQueryDto query = new SearchQueryDto(
                SearchMode.GLOBAL_CONTRIBUTOR,
                SearchMatchMode.LIKE,
                "",
                "",
                "demo",
                false,
                "all",
                false,
                false,
                false,
                false,
                false
        );

        SearchWorkflowSupport.SearchRunResult result = support.runContributorSearch(
                null,
                query,
                CallGraphScope.ALL,
                ProjectJarOriginResolver.empty()
        );

        assertTrue(result.results().isEmpty());
        assertEquals("at least one contributor is required", result.statusText());
    }

    @Test
    void queryLanguageSearchRejectsBlankScript() {
        SearchQueryDto query = new SearchQueryDto(
                SearchMode.CYPHER_QUERY,
                SearchMatchMode.LIKE,
                "",
                "",
                "   ",
                false
        );

        SearchWorkflowSupport.SearchRunResult result = support.runQueryLanguageSearch(
                query,
                CallGraphScope.ALL,
                ProjectJarOriginResolver.empty()
        );

        assertTrue(result.results().isEmpty());
        assertEquals("cypher query is required", result.statusText());
    }
}
