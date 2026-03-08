/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathMatcherRoutesTest {
    @Test
    void dfsJobBaseRouteShouldNotBeRegistered() {
        assertFalse(PathMatcher.handlers.containsKey("/api/flow/dfs/jobs"));
        assertTrue(PathMatcher.handlers.containsKey("/api/flow/dfs/jobs/*"));
    }
}
