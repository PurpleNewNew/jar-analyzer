package org.benf.cfr.reader.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class VineflowerRealCaseRegressionTest {
    private static final Path VINEFLOWER_TESTDATA = Path.of("jadx-vineflower", "testData", "src");

    @Test
    void shouldKeepSelectedVineflowerCasesCompilableAndSemanticallyTyped(@TempDir Path tempDir) throws Exception {
        Path dumpRoot = Path.of("target", "vineflower-audit-dump");
        deleteRecursively(dumpRoot);
        Files.createDirectories(dumpRoot);

        List<AuditCase> cases = List.of(
                AuditCase.of(
                        "java8/pkg/TestWhileIterator.java",
                        new String[]{"--release", "8"},
                        List.of("testNested"),
                        List.of(
                                "Iterator<Object> it1 = list.iterator();",
                                "Iterator<Object> it2 = set.iterator();"
                        ),
                        List.of(
                                "Iterator it1 = list.iterator();",
                                "Iterator it2 = set.iterator();"
                        )
                ),
                AuditCase.of(
                        "java8nodebug/pkg/TestIterationOverGenericsWithoutLvt.java",
                        new String[]{"--release", "8"},
                        List.of("test1", "test2", "test3", "test4"),
                        List.of(
                                "for (Number ",
                                "for (T b : a)",
                                "for (Object ",
                                "T b = null;"
                        ),
                        List.of("for (Object b : a)", "for (Object b : a)")
                ),
                AuditCase.of(
                        "java8/pkg/TestVarMergeSupertype.java",
                        new String[]{"--release", "8"},
                        List.of("myMethod", "myMethod2", "getMyInterfaceImpl"),
                        List.of(
                                "MyInterface rmcp = null;",
                                "MyInterfaceImpl rmcp = null;"
                        ),
                        List.of("Object rmcp")
                ),
                AuditCase.of(
                        "java8/pkg/TestWhileLambda.java",
                        new String[]{"--release", "8"},
                        List.of("test"),
                        List.of("Supplier<Object>", "() -> o2"),
                        List.of("lambda$")
                ),
                AuditCase.of(
                        "java8/pkg/TestWhileConditionTernary.java",
                        new String[]{"--release", "8"},
                        List.of("test1", "blackBox", "blackBox2", "blackBox3"),
                        List.of(),
                        List.of()
                ),
                AuditCase.of(
                        "java8/pkg/TestTryLoopRecompile.java",
                        new String[]{"--release", "8"},
                        List.of("test"),
                        List.of(),
                        List.of()
                ),
                AuditCase.of(
                        "java8/pkg/TestUnionTypeAssign.java",
                        new String[]{"--release", "8"},
                        List.of("test", "test1", "test2", "test3", "test4", "test5"),
                        List.of(),
                        List.of("Supplier supplier")
                )
        );

        List<String> failures = new ArrayList<String>();
        for (AuditCase auditCase : cases) {
            try {
                runCase(tempDir, dumpRoot, auditCase);
            } catch (AssertionError | IOException ex) {
                failures.add(auditCase.relativeSource + " :: " + ex.getMessage());
            }
        }

        if (!failures.isEmpty()) {
            throw new AssertionError("Vineflower real-case audit failures:\n" + String.join("\n", failures));
        }
    }

    private static void runCase(Path tempDir, Path dumpRoot, AuditCase auditCase) throws IOException {
        Path sourcePath = VINEFLOWER_TESTDATA.resolve(auditCase.relativeSource);
        if (!Files.exists(sourcePath)) {
            throw new IOException("Missing vineflower source: " + sourcePath.toAbsolutePath());
        }
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);
        String className = stripJavaExtension(sourcePath.getFileName().toString());
        Path caseRoot = Files.createDirectories(tempDir.resolve(className));
        Path originalRoot = Files.createDirectories(caseRoot.resolve("original"));
        Path decompiledRoot = Files.createDirectories(caseRoot.resolve("decompiled"));
        Path signatureRoot = Files.createDirectories(caseRoot.resolve("signature-check"));
        Path compiledClass = CfrDecompilerRegressionSupport.compileJava(
                originalRoot,
                className,
                source,
                auditCase.javacArgs
        );
        String decompiled = CfrDecompilerRegressionSupport.decompileJava(compiledClass);
        String structuredFailure = getStructuredFailure(decompiled);
        if (structuredFailure != null) {
            dumpCase(dumpRoot, className, decompiled);
            throw new AssertionError(structuredFailure);
        }
        for (String expected : auditCase.expectedContains) {
            if (!decompiled.contains(expected)) {
                dumpCase(dumpRoot, className, decompiled);
                throw new AssertionError("missing expected snippet: " + expected);
            }
        }
        for (String unexpected : auditCase.unexpectedContains) {
            if (decompiled.contains(unexpected)) {
                dumpCase(dumpRoot, className, decompiled);
                throw new AssertionError("contains unexpected snippet: " + unexpected);
            }
        }
        try {
            CfrDecompilerRegressionSupport.compileJava(
                    decompiledRoot,
                    className,
                    decompiled,
                    auditCase.javacArgs
            );
            CfrDecompilerRegressionSupport.assertMethodGenericSignaturesEquivalent(
                    signatureRoot,
                    className,
                    source,
                    decompiled,
                    auditCase.javacArgs,
                    auditCase.methodNames.toArray(new String[0])
            );
        } catch (AssertionError ex) {
            dumpCase(dumpRoot, className, decompiled);
            throw ex;
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

    private static void dumpCase(Path dumpRoot, String className, String decompiled) throws IOException {
        Files.createDirectories(dumpRoot);
        Files.writeString(dumpRoot.resolve(className + ".java"), decompiled, StandardCharsets.UTF_8);
    }

    private static String stripJavaExtension(String fileName) {
        return fileName.endsWith(".java") ? fileName.substring(0, fileName.length() - 5) : fileName;
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

    private static final class AuditCase {
        private final String relativeSource;
        private final String[] javacArgs;
        private final List<String> methodNames;
        private final List<String> expectedContains;
        private final List<String> unexpectedContains;

        private AuditCase(String relativeSource,
                          String[] javacArgs,
                          List<String> methodNames,
                          List<String> expectedContains,
                          List<String> unexpectedContains) {
            this.relativeSource = relativeSource;
            this.javacArgs = javacArgs;
            this.methodNames = methodNames;
            this.expectedContains = expectedContains;
            this.unexpectedContains = unexpectedContains;
        }

        private static AuditCase of(String relativeSource,
                                    String[] javacArgs,
                                    List<String> methodNames,
                                    List<String> expectedContains,
                                    List<String> unexpectedContains) {
            return new AuditCase(relativeSource, javacArgs, methodNames, expectedContains, unexpectedContains);
        }
    }
}
