package me.n1ar4.jar.analyzer.qa;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoTaieMigrationRegressionPackTest {
    @Test
    void shouldCoverRemainingPhase0Issues() {
        Map<String, NoTaieMigrationRegressionPack.IssuePack> packs = NoTaieMigrationRegressionPack.all();

        assertTrue(packs.containsKey("JA-NT-106"));
        assertTrue(packs.containsKey("JA-NT-107"));
        assertTrue(packs.containsKey("JA-NT-108"));
        assertEquals(6, packs.size());
    }

    @Test
    void mappedTestsShouldResolveAndExposeStableCommands() throws Exception {
        for (NoTaieMigrationRegressionPack.IssuePack pack : NoTaieMigrationRegressionPack.all().values()) {
            assertNotNull(pack);
            assertFalse(pack.requiredTests().isEmpty(), "required tests should not be empty for " + pack.issueKey());
            String command = pack.mavenCommand();
            assertTrue(command.startsWith("mvn -q -Dskip.npm=true -Dskip.installnodenpm=true -Dtest="));
            for (String testClass : pack.requiredTests()) {
                Class<?> resolved = Class.forName(testClass);
                assertNotNull(resolved, "missing mapped test class: " + testClass);
                String simpleName = resolved.getSimpleName();
                assertTrue(command.contains(simpleName),
                        () -> "command should include " + simpleName + " for " + pack.issueKey());
            }
        }
    }
}
