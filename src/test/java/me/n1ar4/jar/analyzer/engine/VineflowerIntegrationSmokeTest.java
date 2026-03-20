package me.n1ar4.jar.analyzer.engine;

import org.jetbrains.java.decompiler.main.decompiler.BaseDecompiler;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VineflowerIntegrationSmokeTest {
    @Test
    void shouldDecompileSingleClassAndExposeLineMapping(@TempDir Path tempDir) throws IOException {
        Path sourceFile = tempDir.resolve("VineflowerSample.java");
        Files.writeString(sourceFile, ""
                + "public class VineflowerSample {\n"
                + "    static int test(int x) {\n"
                + "        int a = x + 1;\n"
                + "        int b = a * 2;\n"
                + "        return b;\n"
                + "    }\n"
                + "}\n", StandardCharsets.UTF_8);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "JDK compiler is required");
        int exitCode = compiler.run(null, null, null,
                "--release", "21",
                "-g",
                "-d", tempDir.toString(),
                sourceFile.toString());
        assertEquals(0, exitCode);

        Path classFile = tempDir.resolve("VineflowerSample.class");
        assertTrue(Files.exists(classFile));

        InMemorySaver saver = new InMemorySaver();
        Map<String, Object> options = new HashMap<>();
        options.put(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, "1");
        options.put(IFernflowerPreferences.DUMP_CODE_LINES, "1");
        options.put(IFernflowerPreferences.REMOVE_BRIDGE, "0");
        options.put(IFernflowerPreferences.REMOVE_SYNTHETIC, "0");

        BaseDecompiler decompiler = new BaseDecompiler(saver, options, IFernflowerLogger.NO_OP);
        decompiler.addSource(classFile.toFile());
        decompiler.decompileContext();

        assertNotNull(saver.content);
        assertTrue(saver.content.contains("class VineflowerSample"));
        assertTrue(saver.content.contains("static int test(int x)"));
        assertNotNull(saver.mapping);
        assertFalse(saver.mapping.length == 0);
        assertArrayEquals(new int[]{4, 4, 5, 4, 3, 3}, saver.mapping);
    }

    private static final class InMemorySaver implements IResultSaver {
        private String content;
        private int[] mapping;

        @Override
        public void saveFolder(String path) {
        }

        @Override
        public void copyFile(String source, String path, String entryName) {
        }

        @Override
        public void saveClassFile(String path,
                                  String qualifiedName,
                                  String entryName,
                                  String content,
                                  int[] mapping) {
            this.content = content;
            this.mapping = mapping;
        }

        @Override
        public void createArchive(String path, String archiveName, Manifest manifest) {
        }

        @Override
        public void saveDirEntry(String path, String archiveName, String entryName) {
        }

        @Override
        public void copyEntry(String source, String path, String archiveName, String entry) {
        }

        @Override
        public void saveClassEntry(String path,
                                   String archiveName,
                                   String qualifiedName,
                                   String entryName,
                                   String content) {
            this.content = content;
        }

        @Override
        public void saveClassEntry(String path,
                                   String archiveName,
                                   String qualifiedName,
                                   String entryName,
                                   String content,
                                   int[] mapping) {
            this.content = content;
            this.mapping = mapping;
        }

        @Override
        public void closeArchive(String path, String archiveName) {
        }
    }
}
