package org.benf.cfr.reader.bytecode;

import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SelfDecompileAuditTest {
    @Test
    void shouldKeepJarAnalyzerSelfDecompileCompilable() throws Exception {
        Path classRoot = Path.of("target", "classes", "me", "n1ar4", "jar", "analyzer");
        Path dumpRoot = Path.of("target", "self-audit-dump");
        if (!Files.isDirectory(classRoot)) {
            throw new IOException("Missing class root: " + classRoot.toAbsolutePath());
        }

        deleteRecursively(dumpRoot);
        Files.createDirectories(dumpRoot);

        List<Path> classFiles;
        try (Stream<Path> stream = Files.walk(classRoot)) {
            classFiles = stream
                    .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".class"))
                    .filter(path -> !path.getFileName().toString().contains("$"))
                    .sorted()
                    .collect(Collectors.toList());
        }

        List<String> failures = new ArrayList<String>();
        Path tempDir = Files.createTempDirectory("cfr-self-audit");
        try {
            for (Path classFile : classFiles) {
                String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);
                String structuredFailure = getStructuredFailure(decompiled);
                if (structuredFailure != null) {
                    dumpDecompiledSource(dumpRoot, classRoot, classFile, decompiled);
                    failures.add(classFile + " :: " + structuredFailure);
                    continue;
                }
                String compileFailure = compileDecompiledSource(tempDir, classRoot, classFile, decompiled);
                if (compileFailure != null) {
                    dumpDecompiledSource(dumpRoot, classRoot, classFile, decompiled);
                    failures.add(classFile + " :: " + compileFailure);
                }
            }
        } finally {
            deleteRecursively(tempDir);
        }

        if (!failures.isEmpty()) {
            throw new AssertionError("Self decompile audit failures:\n" + String.join("\n", failures));
        }
    }

    private static String getStructuredFailure(String decompiled) {
        if (decompiled.contains("Unable to fully structure code")) {
            return "contains 'Unable to fully structure code'";
        }
        if (decompiled.contains("** GOTO")) {
            return "contains '** GOTO'";
        }
        return null;
    }

    private static String compileDecompiledSource(Path tempDir,
                                                  Path classRoot,
                                                  Path classFile,
                                                  String decompiled) throws IOException {
        Path relativeClass = classRoot.relativize(classFile);
        Path sourceFile = tempDir.resolve(relativeClass.toString().replace(".class", ".java"));
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, decompiled, StandardCharsets.UTF_8);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "JDK compiler is required for self audit");

        Path outputDir = tempDir.resolve("classes");
        Files.createDirectories(outputDir);
        ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
        int exitCode = compiler.run(
                null,
                null,
                errorOutput,
                "-proc:none",
                "-classpath",
                System.getProperty("java.class.path"),
                "-d",
                outputDir.toString(),
                sourceFile.toString());
        if (exitCode == 0) {
            return null;
        }
        String details = errorOutput.toString(StandardCharsets.UTF_8);
        String firstLine = details == null ? "" : details.lines().findFirst().orElse("").trim();
        return "javac exit=" + exitCode + " for " + sourceFile.getFileName() + (firstLine.isEmpty() ? "" : " :: " + firstLine);
    }

    private static void dumpDecompiledSource(Path dumpRoot,
                                             Path classRoot,
                                             Path classFile,
                                             String decompiled) throws IOException {
        Path relativeClass = classRoot.relativize(classFile);
        Path dumpFile = dumpRoot.resolve(relativeClass.toString().replace(".class", ".java"));
        Files.createDirectories(dumpFile.getParent());
        Files.writeString(dumpFile, decompiled, StandardCharsets.UTF_8);
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                Files.deleteIfExists(path);
            }
        }
    }
}
