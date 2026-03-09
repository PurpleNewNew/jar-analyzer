/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoreRunnerClassNameNormalizeTest {
    @Test
    void normalizeDiscoveredClassNameShouldStripSupportedArchivePrefixesOnly() {
        assertEquals("demo/App.class",
                CoreRunner.normalizeDiscoveredClassName("BOOT-INF/classes/demo/App.class"));
        assertEquals("demo/Web.class",
                CoreRunner.normalizeDiscoveredClassName("WEB-INF/classes/demo/Web.class"));
        assertEquals("BOOT-INF/lib/demo/App.class",
                CoreRunner.normalizeDiscoveredClassName("BOOT-INF/lib/demo/App.class"));
        assertEquals("WEB-INF/lib/demo/Web.class",
                CoreRunner.normalizeDiscoveredClassName("WEB-INF/lib/demo/Web.class"));
    }
}
