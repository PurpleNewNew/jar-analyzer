package org.benf.cfr.reader.state;

import org.benf.cfr.reader.PluginRunner;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.util.AnalysisType;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void pluginRunnerShouldUseDefaultSourceWithoutPriorReset(@TempDir Path tempDir) throws IOException {
        Path classFile = compileJava(tempDir, "PluginSample",
                "public class PluginSample {\n" +
                        "    public int value() {\n" +
                        "        return 7;\n" +
                        "    }\n" +
                        "}\n");

        PluginRunner pluginRunner = new PluginRunner(Map.of("showversion", "false", "silent", "true"));
        String decompiled = pluginRunner.getDecompilationFor(classFile.toString());

        assertTrue(decompiled.contains("class PluginSample"), decompiled);
        assertTrue(decompiled.contains("int value()"), decompiled);
    }

    @Test
    void chainedSourceShouldTurnNullClassMissesIntoIOException() {
        ClassFileSourceChained chained = new ClassFileSourceChained(Collections.singletonList(new NullClassFileSource()));

        IOException exception = assertThrows(IOException.class, () -> chained.getClassFileContent("Missing.class"));

        assertEquals("No such file Missing.class", exception.getMessage());
    }

    @Test
    void chainedSourceShouldRejectJarMissAfterAllSourcesFail() {
        ClassFileSourceChained chained = new ClassFileSourceChained(Collections.singletonList(new NullClassFileSource()));

        ConfusedCFRException exception = assertThrows(ConfusedCFRException.class,
                () -> chained.addJarContent("missing.jar", AnalysisType.JAR));

        assertEquals("Failed to load jar missing.jar", exception.getMessage());
    }

    @Test
    void chainedSourceShouldReturnIdentityPathWhenRenameMisses() {
        ClassFileSourceChained chained = new ClassFileSourceChained(Collections.singletonList(new NullClassFileSource()));

        assertEquals("a/b/Missing", chained.getPossiblyRenamedPath("a/b/Missing"));
    }

    @Test
    void shouldResolveSingleClassInputThroughOneAnalysisEntry(@TempDir Path tempDir) throws IOException {
        Path compiledClass = compileJava(tempDir, "Sample",
                "public class Sample {\n" +
                        "    public int value() {\n" +
                        "        return 3;\n" +
                        "    }\n" +
                        "}\n");
        Path oddClass = tempDir.resolve("Odd.class");
        Files.move(compiledClass, oddClass);

        TrackingClassFileSource source = new TrackingClassFileSource(oddClass);
        DCCommonState state = new DCCommonState(new OptionsImpl(Map.of()), source);

        JavaRefTypeInstance classType = (JavaRefTypeInstance) state.getClassFileForAnalysis(oddClass.toString()).getClassType();

        assertEquals("Sample", classType.getRawName());
        assertEquals(List.of(oddClass.toString(), "Sample.class"), source.requestedPaths);
        assertEquals(oddClass.toString(), source.informedUsePath);
        assertEquals("Sample.class", source.informedClassFilePath);
    }

    @Test
    void explicitJarShouldOverrideRelocatedLooseClass(@TempDir Path tempDir) throws IOException {
        Path looseDir = Files.createDirectories(tempDir.resolve("loose"));
        Path jarDir = Files.createDirectories(tempDir.resolve("jar"));
        Path looseClass = compileJava(looseDir, "Sample",
                "public class Sample {\n" +
                        "    public int value() {\n" +
                        "        return 1;\n" +
                        "    }\n" +
                        "}\n");
        Path jarClass = compileJava(jarDir, "Sample",
                "public class Sample {\n" +
                        "    public int value() {\n" +
                        "        return 2;\n" +
                        "    }\n" +
                        "}\n");
        Path jarFile = createJar(tempDir.resolve("sample.jar"), jarClass, "Sample.class");

        ClassFileSourceImpl source = new ClassFileSourceImpl(new OptionsImpl(Map.of()));
        source.informAnalysisRelativePathDetail(looseClass.toString(), "Sample.class");
        source.addJarContent(jarFile.toString(), AnalysisType.JAR);

        Pair<byte[], String> content = source.getClassFileContent("Sample.class");

        assertArrayEquals(Files.readAllBytes(jarClass), content.getFirst());
    }

    @Test
    void classpathJarShouldStillAllowRelocatedLooseClassOverride(@TempDir Path tempDir) throws IOException {
        Path looseDir = Files.createDirectories(tempDir.resolve("loose"));
        Path jarDir = Files.createDirectories(tempDir.resolve("jar"));
        Path looseClass = compileJava(looseDir, "Sample",
                "public class Sample {\n" +
                        "    public int value() {\n" +
                        "        return 1;\n" +
                        "    }\n" +
                        "}\n");
        Path jarClass = compileJava(jarDir, "Sample",
                "public class Sample {\n" +
                        "    public int value() {\n" +
                        "        return 2;\n" +
                        "    }\n" +
                        "}\n");
        Path jarFile = createJar(tempDir.resolve("cp.jar"), jarClass, "Sample.class");

        ClassFileSourceImpl source = new ClassFileSourceImpl(new OptionsImpl(Map.of(
                OptionsImpl.EXTRA_CLASS_PATH.getName(), jarFile.toString()
        )));
        source.informAnalysisRelativePathDetail(looseClass.toString(), "Sample.class");

        Pair<byte[], String> content = source.getClassFileContent("Sample.class");

        assertArrayEquals(Files.readAllBytes(looseClass), content.getFirst());
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

    private static final class NullClassFileSource implements org.benf.cfr.reader.apiunreleased.ClassFileSource2 {
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
        public Pair<byte[], String> getClassFileContent(String path) {
            return null;
        }

        @Override
        public org.benf.cfr.reader.apiunreleased.JarContent addJarContent(String jarPath, AnalysisType analysisType) {
            return null;
        }
    }

    private static final class TrackingClassFileSource implements org.benf.cfr.reader.apiunreleased.ClassFileSource2 {
        private final Path oddClass;
        private String informedUsePath;
        private String informedClassFilePath;
        private final List<String> requestedPaths = new java.util.ArrayList<>();

        private TrackingClassFileSource(Path oddClass) {
            this.oddClass = oddClass;
        }

        @Override
        public void informAnalysisRelativePathDetail(String usePath, String classFilePath) {
            this.informedUsePath = usePath;
            this.informedClassFilePath = classFilePath;
        }

        @Override
        public Collection<String> addJar(String jarPath) {
            return null;
        }

        @Override
        public String getPossiblyRenamedPath(String path) {
            return path;
        }

        @Override
        public Pair<byte[], String> getClassFileContent(String path) throws IOException {
            requestedPaths.add(path);
            if (path.equals(oddClass.toString()) || (informedUsePath != null && path.equals(informedClassFilePath))) {
                return Pair.make(Files.readAllBytes(oddClass), oddClass.toString());
            }
            throw new IOException("No such file " + path);
        }

        @Override
        public org.benf.cfr.reader.apiunreleased.JarContent addJarContent(String jarPath, AnalysisType analysisType) {
            return null;
        }
    }
}
