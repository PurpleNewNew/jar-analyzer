/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.engine;

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CoreEngineMetadataStrictnessTest {
    @BeforeEach
    @AfterEach
    public void resetDatabaseManager() {
        DatabaseManager.clearAllData();
    }

    @Test
    public void classLookupShouldNotSynthesizeClassFromMethodOnlyMetadata() {
        DatabaseManager.saveMethods(Set.of(new MethodReference(
                new ClassReference.Handle("demo/Missing", 1),
                "run",
                "()V",
                false,
                Set.of(),
                0,
                1,
                "demo.jar",
                1
        )));
        CoreEngine engine = newEngine();

        assertTrue(engine.getClassesByClass("demo/Missing").isEmpty());
        assertNull(engine.getClassByClass("demo/Missing"));
        assertNull(engine.getClassRef(new ClassReference.Handle("demo/Missing", 1), 1));
    }

    @Test
    public void webRoleLookupShouldSkipClassesWithoutClassMetadata() {
        DatabaseManager.saveServlets(new ArrayList<>(List.of("demo/Missing")));
        CoreEngine engine = newEngine();

        assertTrue(engine.getAllServlets().isEmpty());
    }

    private static CoreEngine newEngine() {
        ConfigFile config = new ConfigFile();
        config.setDbPath("test-db");
        return new CoreEngine(config);
    }
}
