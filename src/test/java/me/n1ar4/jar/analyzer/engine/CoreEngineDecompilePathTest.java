/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.engine;

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.facts.ClassFileEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CoreEngineDecompilePathTest {
    private final Set<Path> tempPaths = new LinkedHashSet<>();

    @AfterEach
    void cleanup() throws Exception {
        DatabaseManager.clearAllData();
        for (Path path : tempPaths) {
            Files.deleteIfExists(path);
        }
    }

    @Test
    void getAbsPathWithJarIdShouldNotFallbackAcrossJars() throws Exception {
        Path firstPath = newTempClassFile("jar-analyzer-shared-first");
        Path secondPath = newTempClassFile("jar-analyzer-shared-second");
        DatabaseManager.saveClassFiles(Set.of(
                classFile("demo/Shared", firstPath, 1),
                classFile("demo/Shared", secondPath, 2)
        ));

        Path dbPath = Files.createTempDirectory("jar-analyzer-core-engine");
        tempPaths.add(dbPath);
        ConfigFile config = new ConfigFile();
        config.setDbPath(dbPath.toString());
        CoreEngine engine = new CoreEngine(config);

        assertEquals(secondPath.toString(), engine.getAbsPath("demo/Shared", 2));
        assertNull(engine.getAbsPath("demo/Shared", 99));
    }

    private Path newTempClassFile(String prefix) throws Exception {
        Path file = Files.createTempFile(prefix, ".class");
        tempPaths.add(file);
        return file.toAbsolutePath().normalize();
    }

    private static ClassFileEntity classFile(String className, Path path, int jarId) {
        ClassFileEntity entity = new ClassFileEntity();
        entity.setClassName(className);
        entity.setJarId(jarId);
        entity.setPath(path);
        return entity;
    }
}
