/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.SearchMatchMode;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchMode;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchQueryDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchFacadeParityTest {
    @Test
    void applyQueryShouldRoundTripSnapshot() {
        SearchQueryDto trueQuery = new SearchQueryDto(
                SearchMode.STRING_CONTAINS,
                SearchMatchMode.EQUALS,
                "a.b.C",
                "targetMethod",
                "secret",
                true
        );
        RuntimeFacades.search().applyQuery(trueQuery);
        SearchQueryDto trueSnapshot = RuntimeFacades.search().snapshot().query();
        assertEquals(SearchMode.STRING_CONTAINS, trueSnapshot.mode());
        assertEquals(SearchMatchMode.EQUALS, trueSnapshot.matchMode());
        assertEquals("a.b.C", trueSnapshot.className());
        assertEquals("targetMethod", trueSnapshot.methodName());
        assertEquals("secret", trueSnapshot.keyword());
        assertEquals(true, trueSnapshot.nullParamFilter());

        SearchQueryDto falseQuery = new SearchQueryDto(
                SearchMode.METHOD_DEFINITION,
                SearchMatchMode.LIKE,
                "",
                "run",
                "",
                false
        );
        RuntimeFacades.search().applyQuery(falseQuery);
        SearchQueryDto falseSnapshot = RuntimeFacades.search().snapshot().query();
        assertEquals(SearchMode.METHOD_DEFINITION, falseSnapshot.mode());
        assertEquals(SearchMatchMode.LIKE, falseSnapshot.matchMode());
        assertEquals(false, falseSnapshot.nullParamFilter());
        assertEquals("all", falseSnapshot.scope());
        assertTrue(falseSnapshot.contributorClass());
        assertTrue(falseSnapshot.contributorMethod());
        assertTrue(falseSnapshot.contributorString());
        assertTrue(falseSnapshot.contributorResource());
    }

    @Test
    void applyQueryShouldKeepScopeAndContributorToggles() {
        SearchQueryDto query = new SearchQueryDto(
                SearchMode.SQL_QUERY,
                SearchMatchMode.LIKE,
                "",
                "",
                "select * from class_table limit 5",
                false,
                "library",
                false,
                true,
                false,
                true
        );
        RuntimeFacades.search().applyQuery(query);
        SearchQueryDto snapshot = RuntimeFacades.search().snapshot().query();
        assertEquals(SearchMode.SQL_QUERY, snapshot.mode());
        assertEquals("library", snapshot.scope());
        assertFalse(snapshot.contributorClass());
        assertTrue(snapshot.contributorMethod());
        assertFalse(snapshot.contributorString());
        assertTrue(snapshot.contributorResource());
    }
}
