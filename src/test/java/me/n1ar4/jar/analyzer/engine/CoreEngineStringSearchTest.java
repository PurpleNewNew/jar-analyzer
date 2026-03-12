/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.engine;

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.model.MethodView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoreEngineStringSearchTest {
    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
    }

    @Test
    void shouldMatchAnnotationStringsEvenWhenMethodAlsoHasNormalLiterals() {
        MethodReference method = new MethodReference(
                new ClassReference.Handle("demo/Controller", 1),
                "index",
                "()V",
                false,
                Set.of(),
                0,
                1,
                "demo.jar",
                1
        );
        DatabaseManager.saveMethods(Set.of(method));
        DatabaseManager.saveStrMap(
                Map.of(method.getHandle(), List.of("jdbc:mysql://safe")),
                Map.of(method.getHandle(), List.of("/api/index"))
        );

        CoreEngine engine = newEngine();
        List<MethodView> results = engine.getMethodsByStr("/api/index", null, null, null, "equal");

        assertEquals(1, results.size());
        assertEquals("/api/index", results.get(0).getStrValue());
    }

    private static CoreEngine newEngine() {
        ConfigFile config = new ConfigFile();
        config.setDbPath("test-db");
        return new CoreEngine(config);
    }
}
