/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.query;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryOptionsTest {
    @Test
    void explicitHopLimitsShouldScaleDerivedBudgets() {
        QueryOptions options = new QueryOptions(500, 15000, 200, 1000);
        assertEquals(200, options.getMaxHops());
        assertEquals(15_000, options.getMaxMs());
        assertEquals(2_400_000, options.getExpandBudget());
        assertEquals(19_200, options.getPathBudget());
        assertEquals(1000, options.getMaxPaths());
    }

    @Test
    void explicitBudgetsShouldRemainAuthoritative() {
        QueryOptions options = new QueryOptions(500, 15000, 200, 3000, 180_000, 1500, 64);
        assertEquals(200, options.getMaxHops());
        assertEquals(180_000, options.getExpandBudget());
        assertEquals(1500, options.getPathBudget());
        assertEquals(1500, options.getMaxPaths());
    }

    @Test
    void publicApiOptionsShouldOnlyAcceptMaxRows() {
        QueryOptions options = QueryOptions.fromMap(Map.of("maxRows", 2048));
        assertEquals(2048, options.getMaxRows());
        assertEquals(15_000, options.getMaxMs());
        assertEquals(8, options.getMaxHops());
        assertEquals(500, options.getMaxPaths());
    }

    @Test
    void publicApiOptionsShouldRejectUnsupportedKeys() {
        assertUnsupported("budgetHint", "wide");
        assertUnsupported("traceMode", "graph");
        assertUnsupported("timeoutPolicy", "strict");
    }

    @Test
    void procedureHintsShouldRaiseDerivedBudgets() {
        QueryOptions options = QueryOptions.defaults();
        assertEquals(32_000, options.effectiveBudgetMaxMs(128, null));
        assertEquals(1_536_000, options.effectiveExpandBudget(128));
        assertEquals(12_288, options.effectivePathBudget(128, 1000));
    }

    private static void assertUnsupported(String key, Object value) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> QueryOptions.fromMap(Map.of(key, value)));
        assertEquals("invalid_request: unsupported query option: " + key, ex.getMessage());
    }
}
