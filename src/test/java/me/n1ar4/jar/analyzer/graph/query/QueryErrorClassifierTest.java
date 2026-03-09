/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QueryErrorClassifierTest {
    @Test
    public void shouldNormalizeProjectNotReadyCodes() {
        assertEquals(
                QueryErrorClassifier.PROJECT_MODEL_MISSING_REBUILD,
                QueryErrorClassifier.codeOf("graph_snapshot_load_failed: store closed"));
        assertTrue(QueryErrorClassifier.isProjectNotReady("graph_store_open_fail"));
        assertEquals(
                "active project is not built, rebuild required",
                QueryErrorClassifier.publicMessage("graph_snapshot_missing_rebuild", ""));
    }

    @Test
    public void shouldKeepStableCypherCodes() {
        assertEquals(
                QueryErrorClassifier.CYPHER_QUERY_TIMEOUT,
                QueryErrorClassifier.codeOf("cypher_query_timeout: 3000ms"));
        assertEquals(
                QueryErrorClassifier.PROJECT_BUILD_IN_PROGRESS,
                QueryErrorClassifier.codeOf("project_build_in_progress"));
        assertEquals(
                "active project build in progress",
                QueryErrorClassifier.publicMessage("project_build_in_progress", ""));
    }

    @Test
    public void shouldNormalizeWrappedKnownQueryErrors() {
        String wrapped = "Failed to invoke function 'apoc.text.join': Caused by: java.lang.IllegalArgumentException: "
                + "cypher_feature_not_supported: apoc function not allowed: apoc.text.join";
        assertEquals(QueryErrorClassifier.CYPHER_FEATURE_NOT_SUPPORTED, QueryErrorClassifier.codeOf(wrapped));
        assertEquals(
                "cypher_feature_not_supported: apoc function not allowed: apoc.text.join",
                QueryErrorClassifier.publicMessage(wrapped, ""));
    }
}
