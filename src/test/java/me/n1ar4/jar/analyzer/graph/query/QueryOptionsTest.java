/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.query;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void legacyLongChainProfileShouldStillBoostDefaults() {
        QueryOptions options = QueryOptions.fromMap(Map.of("profile", "long-chain"));
        assertEquals(128, options.getMaxHops());
        assertEquals(32_000, options.getMaxMs());
        assertEquals(1_536_000, options.getExpandBudget());
        assertEquals(12_288, options.getPathBudget());
        assertEquals(1000, options.getMaxPaths());
    }

    @Test
    void explicitMaxPathsShouldRaiseDefaultPathBudgetWithoutProfile() {
        QueryOptions options = QueryOptions.fromMap(Map.of("maxPaths", 3000));
        assertEquals(8, options.getMaxHops());
        assertEquals(24_000, options.getPathBudget());
        assertEquals(3000, options.getMaxPaths());
    }

    @Test
    void explicitBudgetsShouldRemainAuthoritative() {
        QueryOptions options = QueryOptions.fromMap(Map.of(
                "maxHops", 200,
                "expandBudget", 180_000,
                "pathBudget", 1500,
                "maxPaths", 3000
        ));
        assertEquals(200, options.getMaxHops());
        assertEquals(180_000, options.getExpandBudget());
        assertEquals(1500, options.getPathBudget());
        assertEquals(1500, options.getMaxPaths());
    }

    @Test
    void procedureHintsShouldRaiseDerivedBudgetsWhenUiDoesNotSendProfile() {
        QueryOptions options = QueryOptions.defaults();
        assertEquals(32_000, options.effectiveBudgetMaxMs(128, null));
        assertEquals(1_536_000, options.effectiveExpandBudget(128));
        assertEquals(12_288, options.effectivePathBudget(128, 1000));
    }
}
