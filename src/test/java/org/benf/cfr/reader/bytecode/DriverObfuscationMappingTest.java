package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DriverObfuscationMappingTest {
    @Test
    void shouldApplyConfiguredObfuscationMappingForSingleClass(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileJava(tempDir, "a",
                "public class a {\n" +
                        "    public int b() {\n" +
                        "        return 1;\n" +
                        "    }\n" +
                        "}\n");
        Path mappingFile = writeMappingFile(tempDir);

        String decompiled = CfrDecompilerRegressionSupport.decompile(classFile,
                Map.of(OptionsImpl.OBFUSCATION_PATH.getName(), mappingFile.toString()));
        DecompiledResult result = decompileWithMetadata(classFile,
                Map.of(OptionsImpl.OBFUSCATION_PATH.getName(), mappingFile.toString()));

        assertTrue(decompiled.contains("class Real"), decompiled);
        assertTrue(decompiled.contains("int value()"), decompiled);
        assertEquals("Real", result.className);
    }

    @Test
    void shouldApplyConfiguredObfuscationMappingForJar(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileJava(tempDir, "a",
                "public class a {\n" +
                        "    public int b() {\n" +
                        "        return 2;\n" +
                        "    }\n" +
                        "}\n");
        Path jarFile = createJar(tempDir.resolve("sample.jar"), classFile, "a.class");
        Path mappingFile = writeMappingFile(tempDir);

        String decompiled = CfrDecompilerRegressionSupport.decompile(jarFile,
                Map.of(OptionsImpl.OBFUSCATION_PATH.getName(), mappingFile.toString()));

        assertTrue(decompiled.contains("class Real"), decompiled);
        assertTrue(decompiled.contains("int value()"), decompiled);
    }

    private static Path writeMappingFile(Path tempDir) throws IOException {
        Path mappingFile = tempDir.resolve("mapping.txt");
        Files.writeString(mappingFile,
                "Real -> a:\n" +
                        "    int value() -> b\n",
                StandardCharsets.UTF_8);
        return mappingFile;
    }

    private static Path createJar(Path jarFile, Path classFile, String entryName) throws IOException {
        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile))) {
            jarOutputStream.putNextEntry(new JarEntry(entryName));
            jarOutputStream.write(Files.readAllBytes(classFile));
            jarOutputStream.closeEntry();
        }
        return jarFile;
    }

    private static DecompiledResult decompileWithMetadata(Path classFile, Map<String, String> extraOptions) {
        DecompiledResult result = new DecompiledResult();
        OutputSinkFactory sinkFactory = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> available) {
                if (sinkType == SinkType.JAVA) {
                    return Arrays.asList(SinkClass.DECOMPILED, SinkClass.STRING);
                }
                return Arrays.asList(SinkClass.STRING, SinkClass.EXCEPTION_MESSAGE);
            }

            @Override
            public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                if (sinkType == SinkType.JAVA && sinkClass == SinkClass.DECOMPILED) {
                    return t -> {
                        SinkReturns.Decompiled decompiled = (SinkReturns.Decompiled) t;
                        result.className = decompiled.getClassName();
                        result.java = decompiled.getJava();
                    };
                }
                return t -> { };
            }
        };
        Map<String, String> options = new HashMap<>();
        options.put("showversion", "false");
        options.put("silent", "true");
        options.putAll(extraOptions);
        String currentClassPath = System.getProperty("java.class.path");
        if (currentClassPath != null && !currentClassPath.isBlank()) {
            options.put("extraclasspath", currentClassPath);
        }
        CfrDriver driver = new CfrDriver.Builder()
                .withOptions(options)
                .withOutputSink(sinkFactory)
                .build();
        driver.analyse(Collections.singletonList(classFile.toString()));
        return result;
    }

    private static final class DecompiledResult {
        private String className;
        private String java;
    }
}
