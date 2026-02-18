/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryOptionsTest {
    @Test
    void defaultProfileShouldClampHopsAndBudgets() {
        QueryOptions options = new QueryOptions(500, 15000, 10_000, 500);
        assertEquals(QueryOptions.PROFILE_DEFAULT, options.getProfile());
        assertEquals(64, options.getMaxHops());
        assertEquals(300_000, options.getExpandBudget());
        assertEquals(5000, options.getPathBudget());
    }

    @Test
    void longChainProfileShouldAllowHigherHopsWithStrongBudget() {
        QueryOptions options = QueryOptions.fromMap(java.util.Map.of(
                "profile", "long-chain",
                "maxHops", 10_000,
                "expandBudget", 180_000,
                "pathBudget", 1500,
                "maxPaths", 3000
        ));
        assertEquals(QueryOptions.PROFILE_LONG_CHAIN, options.getProfile());
        assertEquals(256, options.getMaxHops());
        assertEquals(180_000, options.getExpandBudget());
        assertEquals(1500, options.getPathBudget());
        assertEquals(1500, options.getMaxPaths());
    }

    @Test
    void longChainProfileShouldUseLongDefaultsWhenHopsNotProvided() {
        QueryOptions options = QueryOptions.fromMap(java.util.Map.of(
                "profile", "long-chain"
        ));
        assertEquals(QueryOptions.PROFILE_LONG_CHAIN, options.getProfile());
        assertEquals(128, options.getMaxHops());
        assertEquals(20_000, options.getMaxMs());
    }

    @Test
    void longChainBooleanAliasShouldEnableProfile() {
        QueryOptions options = QueryOptions.fromMap(java.util.Map.of(
                "longChain", true
        ));
        assertEquals(QueryOptions.PROFILE_LONG_CHAIN, options.getProfile());
        assertEquals(128, options.getMaxHops());
    }
}
