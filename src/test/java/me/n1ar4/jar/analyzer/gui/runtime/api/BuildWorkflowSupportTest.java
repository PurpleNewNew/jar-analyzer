package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSettingsDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildWorkflowSupportTest {
    private final BuildWorkflowSupport support = new BuildWorkflowSupport((zh, en) -> en);

    @TempDir
    Path tempDir;

    @Test
    void resolveBuildInputRejectsUnsupportedFiles() throws Exception {
        Path textFile = Files.createFile(tempDir.resolve("notes.txt"));

        BuildWorkflowSupport.BuildInputResolution result = support.resolveBuildInput(
                new BuildSettingsDto(textFile.toString(), "", false, false, false)
        );

        assertFalse(result.error().isBlank());
        assertTrue(result.error().contains("input file must be .jar/.war/.class"));
        assertNull(result.inputPath());
    }

    @Test
    void resolveBuildInputAcceptsProjectDirectoryWithBytecode() throws Exception {
        Files.writeString(tempDir.resolve("pom.xml"), "<project/>");
        Path classFile = Files.createDirectories(tempDir.resolve("target/classes/demo"))
                .resolve("App.class");
        Files.write(classFile, new byte[]{0x01});

        BuildWorkflowSupport.BuildInputResolution result = support.resolveBuildInput(
                new BuildSettingsDto(tempDir.toString(), "", true, false, false)
        );

        assertTrue(result.error().isBlank());
        assertEquals(tempDir.toAbsolutePath().normalize(), result.inputPath());
        assertEquals(tempDir.toAbsolutePath().normalize(), result.projectRootPath());
        assertTrue(result.projectLayout());
        assertFalse(result.extraClasspath().isEmpty());
    }
}
