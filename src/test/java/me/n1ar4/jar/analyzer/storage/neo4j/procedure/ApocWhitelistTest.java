/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.storage.neo4j.procedure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApocWhitelistTest {
    private final String originalValue = System.getProperty(ApocWhitelist.APOC_WHITELIST_PROP);

    @AfterEach
    void restoreProperty() {
        if (originalValue == null) {
            System.clearProperty(ApocWhitelist.APOC_WHITELIST_PROP);
        } else {
            System.setProperty(ApocWhitelist.APOC_WHITELIST_PROP, originalValue);
        }
    }

    @Test
    void shouldUseDefaultReadOnlyWhitelistWhenPropertyMissing() {
        System.clearProperty(ApocWhitelist.APOC_WHITELIST_PROP);
        List<String> whitelist = ApocWhitelist.effectiveWhitelist();
        assertEquals("default", ApocWhitelist.whitelistMode());
        assertTrue(whitelist.contains("apoc.text.join"));
        assertTrue(whitelist.contains("apoc.map.fromPairs"));
        assertTrue(whitelist.contains("apoc.coll.contains"));
    }

    @Test
    void shouldSupportCategoryAndExplicitFunctionTokens() {
        System.setProperty(ApocWhitelist.APOC_WHITELIST_PROP, "text, apoc.map.fromPairs");
        List<String> whitelist = ApocWhitelist.effectiveWhitelist();
        assertEquals("custom", ApocWhitelist.whitelistMode());
        assertTrue(whitelist.contains("apoc.text.join"));
        assertTrue(whitelist.contains("apoc.text.clean"));
        assertTrue(whitelist.contains("apoc.map.fromPairs"));
        assertFalse(whitelist.contains("apoc.coll.contains"));
        assertFalse(whitelist.contains("apoc.map.merge"));
    }

    @Test
    void shouldDisableWhitelistWhenPropertyExplicitlyTurnsItOff() {
        System.setProperty(ApocWhitelist.APOC_WHITELIST_PROP, "none");
        assertEquals("off", ApocWhitelist.whitelistMode());
        assertEquals(List.of(), ApocWhitelist.effectiveWhitelist());
    }

    @Test
    void shouldKeepAllModeConsistentWithAllWhitelistExpansion() {
        System.setProperty(ApocWhitelist.APOC_WHITELIST_PROP, "all");
        List<String> whitelist = ApocWhitelist.effectiveWhitelist();
        assertEquals("all", ApocWhitelist.whitelistMode());
        assertTrue(whitelist.contains("apoc.text.join"));
        assertTrue(whitelist.contains("apoc.map.merge"));
        assertTrue(whitelist.contains("apoc.coll.flatten"));
    }
}
