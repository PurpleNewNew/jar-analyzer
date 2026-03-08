package me.n1ar4.jar.analyzer.engine;

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class EngineContextProjectOverrideTest {
    private final String originalProjectKey = ActiveProjectContext.getPublishedActiveProjectKey();
    private final String originalProjectAlias = ActiveProjectContext.getPublishedActiveProjectAlias();

    @AfterEach
    void cleanup() {
        ActiveProjectContext.setActiveProject(originalProjectKey, originalProjectAlias);
        EngineContext.setEngine(null);
    }

    @Test
    void shouldReuseGlobalEngineWhenProjectOverrideDiffersFromPublished() {
        ActiveProjectContext.setActiveProject("engine-active", "engine-active");

        ConfigFile config = new ConfigFile();
        config.setDbPath("/tmp/jar-analyzer-engine-active");
        CoreEngine global = new CoreEngine(config);
        EngineContext.setEngine(global);

        assertSame(global, EngineContext.getEngine());

        CoreEngine overridden = ActiveProjectContext.withProject("engine-other", EngineContext::getEngine);
        CoreEngine overriddenAgain = ActiveProjectContext.withProject("engine-other", EngineContext::getEngine);

        assertSame(global, overridden);
        assertSame(overridden, overriddenAgain);
    }
}
