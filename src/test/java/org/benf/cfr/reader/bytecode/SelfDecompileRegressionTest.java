package org.benf.cfr.reader.bytecode;

import me.n1ar4.jar.analyzer.core.CallGraphPlan;
import me.n1ar4.jar.analyzer.core.DiscoveryRunner;
import me.n1ar4.jar.analyzer.core.bytecode.BuildBytecodeWorkspace;
import me.n1ar4.jar.analyzer.analyze.asm.IdentifyCall;
import me.n1ar4.jar.analyzer.analyze.asm.IdentifyCallEngine;
import me.n1ar4.jar.analyzer.engine.ClassLookupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelfDecompileRegressionTest {
    @Test
    void shouldKeepSelectedJarAnalyzerEngineCasesCompilable(@TempDir Path tempDir) {
        assertAll(
                () -> assertSelfDecompileCompilable(
                        tempDir,
                        "ClassLookupService",
                        CfrDecompilerRegressionSupport.locateClassFile(ClassLookupService.class),
                        new String[]{
                                "public static LookupResult find(String nameOrPath, Integer preferJarId)",
                                "ClassLookupService.findClassInternal(",
                                "return new LookupResult(data, path.toString(), null, path.toString());"
                        },
                        new String[]{
                                "byte[] external = BytecodeCache.read(path);",
                                "LookupResult lookupResult = result;",
                                "InputStream inputStream = zipFile.getInputStream(entry);"
                        })
        );
    }

    @Test
    void shouldKeepSelectedJarAnalyzerModelCasesCompilable(@TempDir Path tempDir) {
        assertAll(
                () -> assertSelfDecompileCompilable(
                        tempDir,
                        "CallGraphPlan",
                        CfrDecompilerRegressionSupport.locateClassFile(CallGraphPlan.class),
                        new String[]{
                                "public record CallGraphPlan("
                        },
                        new String[]{
                                "ObjectMethods.bootstrap",
                                "public final String toString()"
                        }),
                () -> assertSelfDecompileCompilable(
                        tempDir,
                        "BuildBytecodeWorkspace",
                        CfrDecompilerRegressionSupport.locateClassFile(BuildBytecodeWorkspace.class),
                        new String[]{
                                "cancelRemaining(",
                                "Future<List<ParsedClass>> future = futures.get(",
                                "Future<List<ParsedClass>> pending = futures.get(i);"
                        },
                        new String[]{
                                "+ true",
                                "Future future =",
                                "Future pending ="
                        }),
                () -> assertSelfDecompileCompilable(
                        tempDir,
                        "DiscoveryRunner",
                        CfrDecompilerRegressionSupport.locateClassFile(DiscoveryRunner.class),
                        new String[]{
                                "List<Future<LocalResult>> futures"
                        },
                        new String[]{
                                "Exception decompiling",
                                "throw new IllegalStateException(\"Decompilation failed\")",
                                "ArrayList futures = new ArrayList();"
                        }),
                () -> assertSelfDecompileCompilable(
                        tempDir,
                        "IdentifyCall",
                        CfrDecompilerRegressionSupport.locateClassFile(IdentifyCall.class),
                        new String[]{
                                "List<SourceValue> pending",
                                "SourceValue sv = pending.get(pIx);",
                                "Set<SourceValue> dep = sources.get(instruction);",
                                "Analyzer<SourceValue> analyzer = new Analyzer<SourceValue>(i)"
                        },
                        new String[]{
                                "List pending =",
                                "(List<SourceValue>)new ArrayList<SourceValue>(",
                                "Set dep =",
                                "(Interpreter)i"
                        }),
                () -> assertSelfDecompileCompilable(
                        tempDir,
                        "IdentifyCallEngine",
                        CfrDecompilerRegressionSupport.locateClassFile(IdentifyCallEngine.class),
                        new String[]{
                                "return Collections.emptyList();"
                        },
                        new String[]{
                                "(List<Level>)Collections.emptyList()"
                        })
        );
    }

    private static void assertSelfDecompileCompilable(Path tempDir,
                                                      String className,
                                                      Path classFile,
                                                      String[] requiredSnippets,
                                                      String[] forbiddenSnippets) throws IOException {
        String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);
        assertStructured(decompiled);
        for (String requiredSnippet : requiredSnippets) {
            assertTrue(decompiled.contains(requiredSnippet), decompiled);
        }
        for (String forbiddenSnippet : forbiddenSnippets) {
            assertFalse(decompiled.contains(forbiddenSnippet), decompiled);
        }
        CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                className,
                decompiled,
                "-classpath",
                System.getProperty("java.class.path"));
    }

    private static void assertStructured(String decompiled) {
        assertFalse(decompiled.contains("Unable to fully structure code"), decompiled);
        assertFalse(decompiled.contains("** GOTO"), decompiled);
    }

}
