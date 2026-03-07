/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.leak;

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.entity.LeakResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LeakScanServiceStringCoverageTest {
    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
    }

    @Test
    void shouldScanAllStoredStringsForAClass() {
        MethodReference method = new MethodReference(
                new ClassReference.Handle("demo/Secrets", 1),
                "load",
                "()V",
                false,
                Set.of(),
                0,
                1,
                "demo.jar",
                1
        );
        String sensitive = "https://api.example.com/private";
        DatabaseManager.runAtomicUpdate(() -> {
            DatabaseManager.saveMethods(Set.of(method));
            DatabaseManager.saveStrMap(
                    Map.of(method.getHandle(), List.of("safe", sensitive)),
                    Map.of()
            );
        });

        CoreEngine engine = newEngine();
        LeakScanService service = new LeakScanService();
        List<LeakResult> results = service.scan(
                engine,
                new LeakScanService.Request(Set.of("url"), false, null, List.of(), List.of(), Set.of(), Set.of())
        );

        assertTrue(results.stream().anyMatch(result ->
                "URL".equals(result.getTypeName()) && sensitive.equals(result.getValue())));
    }

    private static CoreEngine newEngine() {
        ConfigFile config = new ConfigFile();
        config.setDbPath("test-db");
        return new CoreEngine(config);
    }
}
