package me.n1ar4.jar.analyzer.storage.neo4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectRegistryReplaceFileTest {
    @Test
    void replaceRegistryFileShouldIgnoreLegacyTmpSiblingDirectory(@TempDir Path tempDir) throws Exception {
        Path target = tempDir.resolve("registry.json");
        Path legacyTmpDir = tempDir.resolve("registry.json.tmp");
        Path source = Files.createTempFile(tempDir, "registry.json.tmp-", ".json");

        Files.writeString(target, "{\"old\":true}", StandardCharsets.UTF_8);
        Files.createDirectories(legacyTmpDir);
        Files.writeString(source, "{\"new\":true}", StandardCharsets.UTF_8);

        ProjectRegistryService.replaceRegistryFile(source, target);

        assertEquals("{\"new\":true}", Files.readString(target, StandardCharsets.UTF_8));
        assertTrue(Files.isDirectory(legacyTmpDir));
    }

    @Test
    void replaceRegistryFileShouldFailWhenTargetIsNonEmptyDirectory(@TempDir Path tempDir) throws Exception {
        Path target = tempDir.resolve("registry.json");
        Path source = Files.createTempFile(tempDir, "registry.json.tmp-", ".json");

        Files.createDirectories(target);
        Files.writeString(target.resolve("occupied.txt"), "occupied", StandardCharsets.UTF_8);
        Files.writeString(source, "{\"new\":true}", StandardCharsets.UTF_8);

        assertThrows(DirectoryNotEmptyException.class,
                () -> ProjectRegistryService.replaceRegistryFile(source, target));
    }
}
