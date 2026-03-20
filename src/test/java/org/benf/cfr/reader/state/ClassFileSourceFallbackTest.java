package org.benf.cfr.reader.state;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.util.AnalysisType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassFileSourceFallbackTest {
    @Test
    void shouldFallbackToDefaultClassFileSourceWhenOverrideMissesClass(@TempDir Path tempDir) throws IOException {
        Path classFile = compileJava(tempDir, "Sample",
                "public class Sample {\n" +
                        "    public int value() {\n" +
                        "        return 1;\n" +
                        "    }\n" +
                        "}\n");

        String decompiled = decompileWithOverride(classFile.toString(), new ThrowingLegacySource());

        assertTrue(decompiled.contains("class Sample"), decompiled);
        assertTrue(decompiled.contains("int value()"), decompiled);
    }

    @Test
    void shouldFallbackToDefaultJarSourceWhenLegacyOverrideReturnsNull(@TempDir Path tempDir) throws IOException {
        Path classFile = compileJava(tempDir, "JarSample",
                "public class JarSample {\n" +
                        "    public String value() {\n" +
                        "        return \"ok\";\n" +
                        "    }\n" +
                        "}\n");
        Path jarFile = createJar(tempDir.resolve("sample.jar"), classFile, "JarSample.class");

        String decompiled = decompileWithOverride(jarFile.toString(), new NullJarLegacySource());

        assertTrue(decompiled.contains("class JarSample"), decompiled);
        assertTrue(decompiled.contains("String value()"), decompiled);
    }

    @Test
    void wrapperShouldReturnNullForNullJarPathWithoutCallingLegacySource() {
        AtomicInteger addJarCalls = new AtomicInteger();
        ClassFileSourceWrapper wrapper = new ClassFileSourceWrapper(new ClassFileSource() {
            @Override
            public void informAnalysisRelativePathDetail(String usePath, String classFilePath) {
            }

            @Override
            public Collection<String> addJar(String jarPath) {
                addJarCalls.incrementAndGet();
                return Collections.singletonList("Example.class");
            }

            @Override
            public String getPossiblyRenamedPath(String path) {
                return path;
            }

            @Override
            public Pair<byte[], String> getClassFileContent(String path) {
                throw new IllegalStateException("not used");
            }
        });

        assertNull(wrapper.addJar(null));
        assertNull(wrapper.addJarContent(null, AnalysisType.JAR));
        assertEquals(0, addJarCalls.get());
    }

    private static String decompileWithOverride(String path, ClassFileSource source) {
        StringBuilder sinkOutput = new StringBuilder();
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
                    return t -> sinkOutput.append(((SinkReturns.Decompiled) t).getJava());
                }
                return t -> {
                    if (t != null) {
                        sinkOutput.append(String.valueOf(t)).append('\n');
                    }
                };
            }
        };

        Map<String, String> options = new HashMap<>();
        options.put("showversion", "false");
        options.put("silent", "true");
        String currentClassPath = System.getProperty("java.class.path");
        if (currentClassPath != null && !currentClassPath.isBlank()) {
            options.put("extraclasspath", currentClassPath);
        }

        CfrDriver driver = new CfrDriver.Builder()
                .withOptions(options)
                .withOverrideClassFileSource(source)
                .withOutputSink(sinkFactory)
                .build();
        driver.analyse(Collections.singletonList(path));
        return sinkOutput.toString();
    }

    private static Path compileJava(Path tempDir, String className, String source) throws IOException {
        Path sourceFile = tempDir.resolve(className + ".java");
        Files.writeString(sourceFile, source, StandardCharsets.UTF_8);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "JDK compiler is required for source fallback tests");
        int compileExit = compiler.run(null, null, null,
                "-proc:none",
                "-g",
                "-d",
                tempDir.toString(),
                sourceFile.toString());
        assertEquals(0, compileExit);

        Path classFile = tempDir.resolve(className + ".class");
        assertTrue(Files.exists(classFile), "Compiled class not found: " + classFile);
        return classFile;
    }

    private static Path createJar(Path jarFile, Path classFile, String entryName) throws IOException {
        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile))) {
            jarOutputStream.putNextEntry(new JarEntry(entryName));
            jarOutputStream.write(Files.readAllBytes(classFile));
            jarOutputStream.closeEntry();
        }
        return jarFile;
    }

    private static class ThrowingLegacySource implements ClassFileSource {
        @Override
        public void informAnalysisRelativePathDetail(String usePath, String classFilePath) {
        }

        @Override
        public Collection<String> addJar(String jarPath) {
            return null;
        }

        @Override
        public String getPossiblyRenamedPath(String path) {
            return null;
        }

        @Override
        public Pair<byte[], String> getClassFileContent(String path) throws IOException {
            throw new IOException("delegate miss for " + path);
        }
    }

    private static final class NullJarLegacySource extends ThrowingLegacySource {
    }
}
