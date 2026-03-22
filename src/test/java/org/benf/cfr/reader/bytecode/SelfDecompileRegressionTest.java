package org.benf.cfr.reader.bytecode;

import me.n1ar4.jar.analyzer.core.CallGraphPlan;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.DiscoveryRunner;
import me.n1ar4.jar.analyzer.core.DispatchCallResolver;
import me.n1ar4.jar.analyzer.core.JspCompileRunner;
import me.n1ar4.jar.analyzer.core.WebEntryMethods;
import me.n1ar4.jar.analyzer.core.bytecode.BuildBytecodeWorkspace;
import me.n1ar4.jar.analyzer.core.facts.BuildFactSnapshot;
import me.n1ar4.jar.analyzer.engine.CFRDecompileEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.graph.proc.ProcedureRegistry;
import me.n1ar4.jar.analyzer.analyze.asm.IdentifyCall;
import me.n1ar4.jar.analyzer.analyze.asm.IdentifyCallEngine;
import me.n1ar4.jar.analyzer.engine.ClassLookupService;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.LocalVariableMetadataTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ExpressionTypeHintHelper;
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
                        "DispatchCallResolver",
                        CfrDecompilerRegressionSupport.locateClassFile(DispatchCallResolver.class),
                        new String[]{
                                "int scaled = (int)Math.sqrt("
                        },
                        new String[]{
                                "int scaled = Math.sqrt("
                        }),
                () -> assertSelfDecompileCompilable(
                        tempDir,
                        "DatabaseManager",
                        CfrDecompilerRegressionSupport.locateClassFile(DatabaseManager.class),
                        new String[]{
                                "T t = supplier.get();"
                        },
                        new String[]{
                                "Object t = supplier.get();",
                                "return (List<ClassFileEntity>)((List)DatabaseManager.withReadLock(() -> {"
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
                        }),
                () -> assertSelfDecompileCompilable(
                        tempDir,
                        "JspCompileRunner",
                        CfrDecompilerRegressionSupport.locateClassFile(JspCompileRunner.class),
                        new String[]{
                                "private static List<CompileRoot> discoverRoots("
                        },
                        new String[0]),
                () -> assertSelfDecompileCompilable(
                        tempDir,
                        "WebEntryMethods",
                        CfrDecompilerRegressionSupport.locateClassFile(WebEntryMethods.class),
                        new String[]{
                                "public final class WebEntryMethods",
                                "SERVLET_ENTRY_METHODS = Set.of("
                        },
                        new String[0]),
                () -> assertSelfDecompileCompilable(
                        tempDir,
                        "BuildFactSnapshot",
                        CfrDecompilerRegressionSupport.locateClassFile(BuildFactSnapshot.class),
                        new String[]{
                                "private static <K, V> Map<K, List<V>> immutableMapOfLists("
                        },
                        new String[]{
                                "(Map<K, List<V>>)Map.of()"
                        }),
                () -> assertSelfDecompileCompilable(
                        tempDir,
                        "CFRDecompileEngine",
                        CfrDecompilerRegressionSupport.locateClassFile(CFRDecompileEngine.class),
                        new String[]{
                                "private static String decompileInternal("
                        },
                        new String[]{
                                "1.cfr$lambda$getSink$0",
                                "(OutputSinkFactory.Sink<Object>)"
                        }),
                () -> assertSelfDecompileCompilable(
                        tempDir,
                        "EngineContext",
                        CfrDecompilerRegressionSupport.locateClassFile(EngineContext.class),
                        new String[]{
                                "private static void runProjectPrewarm(",
                                "catch (Exception ex)",
                                "finally "
                        },
                        new String[]{
                                "WARNING - Removed try catching itself",
                                "Loose catch block",
                                "return;\r\n        catch",
                                "return;\n        catch",
                                "** GOTO"
                        }),
                () -> assertSelfDecompileCompilable(
                        tempDir,
                        "ProcedureRegistry",
                        CfrDecompilerRegressionSupport.locateClassFile(ProcedureRegistry.class),
                        new String[]{
                                "private static List<Object> toRow("
                        },
                        new String[]{
                                "(List<Object>)List.of("
                        }),
                () -> assertSelfDecompileCompilable(
                        tempDir,
                        "AbstractMemberFunctionInvokation",
                        CfrDecompilerRegressionSupport.locateClassFile(AbstractMemberFunctionInvokation.class),
                        new String[0],
                        new String[0]),
                () -> assertSelfDecompileCompilable(
                        tempDir,
                        "ExpressionTypeHintHelper",
                        CfrDecompilerRegressionSupport.locateClassFile(ExpressionTypeHintHelper.class),
                        new String[0],
                        new String[0]),
                () -> assertSelfDecompileCompilable(
                        tempDir,
                        "LocalVariableMetadataTransformer",
                        CfrDecompilerRegressionSupport.locateClassFile(LocalVariableMetadataTransformer.class),
                        new String[0],
                        new String[0])
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
