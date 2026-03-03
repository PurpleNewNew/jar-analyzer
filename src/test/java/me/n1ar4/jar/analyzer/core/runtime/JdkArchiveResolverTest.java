package me.n1ar4.jar.analyzer.core.runtime;

import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdkArchiveResolverTest {

    @Test
    void shouldResolveRtJarOnJdk8Layout() throws Exception {
        Path javaHome = Files.createTempDirectory("ja-jdk8");
        Path lib = javaHome.resolve("lib");
        Files.createDirectories(lib);
        Files.createFile(lib.resolve("rt.jar"));
        Files.createFile(lib.resolve("jce.jar"));

        JdkArchiveResolver.JdkResolution result = JdkArchiveResolver.resolve(javaHome, "core");

        assertEquals("rt-jar", result.strategy());
        assertTrue(result.archives().stream().anyMatch(path -> path.getFileName().toString().equals("rt.jar")));
        assertTrue(result.archives().stream().anyMatch(path -> path.getFileName().toString().equals("jce.jar")));
        assertTrue(result.sdkEntryCount() >= 1);
    }

    @Test
    void shouldResolveAndConvertJmodsOnJdk9PlusLayout() throws Exception {
        Path javaHome = Files.createTempDirectory("ja-jdk21");
        Path jmods = javaHome.resolve("jmods");
        Files.createDirectories(jmods);
        writeMinimalJmod(jmods.resolve("java.base.jmod"));

        JdkArchiveResolver.JdkResolution result = JdkArchiveResolver.resolve(javaHome, "core");

        assertEquals("jmods", result.strategy());
        assertFalse(result.archives().isEmpty());
        assertTrue(result.archives().stream().allMatch(path -> path.getFileName().toString().endsWith(".jar")));
        assertTrue(result.archives().stream().allMatch(Files::exists));
    }

    private static void writeMinimalJmod(Path target) throws Exception {
        try (OutputStream out = Files.newOutputStream(target);
             ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("classes/java/lang/Object.class"));
            zip.write(new byte[]{0x00});
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("classes/module-info.class"));
            zip.write(new byte[]{0x00});
            zip.closeEntry();
        }
    }
}
