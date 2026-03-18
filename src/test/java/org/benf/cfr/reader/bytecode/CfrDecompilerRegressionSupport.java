package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CfrDecompilerRegressionSupport {
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");

    private CfrDecompilerRegressionSupport() {
    }

    static Path compileFixture(Path tempDir, String fixtureSuite, String className, String... javacArgs) throws IOException {
        return compileJava(tempDir, className, loadFixtureSource(fixtureSuite, className), javacArgs);
    }

    static Path compileJava(Path tempDir, String className, String source, String... javacArgs) throws IOException {
        Path sourceFile = tempDir.resolve(className + ".java");
        Files.writeString(sourceFile, source, StandardCharsets.UTF_8);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "JDK compiler is required for CFR regression tests");

        String[] args = new String[javacArgs.length + 5];
        System.arraycopy(javacArgs, 0, args, 0, javacArgs.length);
        args[javacArgs.length] = "-proc:none";
        args[javacArgs.length + 1] = "-g";
        args[javacArgs.length + 2] = "-d";
        args[javacArgs.length + 3] = tempDir.toString();
        args[javacArgs.length + 4] = sourceFile.toString();
        int compileExit = compiler.run(null, null, null, args);
        assertEquals(0, compileExit);
        Path compiledClassFile = resolveCompiledClassFile(tempDir, className, source);
        assertTrue(Files.exists(compiledClassFile), "Compiled class not found: " + compiledClassFile);
        return compiledClassFile;
    }

    private static String loadFixtureSource(String fixtureSuite, String className) throws IOException {
        Path fixturePath = Path.of("src", "test", "resources", "org", "benf", "cfr", "reader", "bytecode", "fixtures", fixtureSuite, className + ".java");
        assertTrue(Files.exists(fixturePath), "Missing CFR fixture: " + fixturePath);
        return Files.readString(fixturePath, StandardCharsets.UTF_8);
    }

    static String decompile(Path classFile) {
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

        Map<String, String> options = new HashMap<String, String>();
        options.put("showversion", "false");
        options.put("silent", "true");
        String currentClassPath = System.getProperty("java.class.path");
        if (currentClassPath != null && !currentClassPath.isBlank()) {
            options.put("extraclasspath", currentClassPath);
        }

        CfrDriver driver = new CfrDriver.Builder()
                .withOptions(options)
                .withOutputSink(sinkFactory)
                .build();
        driver.analyse(Collections.singletonList(classFile.toString()));
        return sinkOutput.toString();
    }

    static Path locateClassFile(Class<?> type) throws IOException {
        assertNotNull(type, "Class type is required");
        java.net.URL classResource = type.getResource(type.getSimpleName() + ".class");
        assertNotNull(classResource, "Class resource not found for " + type.getName());
        try {
            return Path.of(classResource.toURI());
        } catch (Exception ex) {
            throw new IOException("Resolve class file failed for " + type.getName(), ex);
        }
    }

    private static Path resolveCompiledClassFile(Path tempDir, String className, String source) {
        String packageName = extractPackageName(source);
        Path classFile = tempDir;
        if (!packageName.isEmpty()) {
            for (String packagePart : packageName.split("\\.")) {
                classFile = classFile.resolve(packagePart);
            }
        }
        return classFile.resolve(className + ".class");
    }

    private static String extractPackageName(String source) {
        Matcher matcher = PACKAGE_PATTERN.matcher(source);
        return matcher.find() ? matcher.group(1) : "";
    }
}
