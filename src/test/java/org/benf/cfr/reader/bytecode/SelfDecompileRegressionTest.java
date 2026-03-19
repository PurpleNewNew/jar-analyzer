package org.benf.cfr.reader.bytecode;

import me.n1ar4.jar.analyzer.analyze.asm.IdentifyCall;
import me.n1ar4.jar.analyzer.analyze.spring.asm.SpringClassVisitor;
import me.n1ar4.jar.analyzer.core.CallGraphPlan;
import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.core.InheritanceMap;
import me.n1ar4.jar.analyzer.core.JspCompileRunner;
import me.n1ar4.jar.analyzer.engine.CFRDecompileEngine;
import me.n1ar4.jar.analyzer.engine.ClassLookupService;
import me.n1ar4.jar.analyzer.core.asm.StringMethodVisitor;
import me.n1ar4.jar.analyzer.gui.swing.panel.ImplToolPanel;
import me.n1ar4.jar.analyzer.gui.swing.panel.LeakToolPanel;
import me.n1ar4.jar.analyzer.taint.AliasRuleSupport;
import me.n1ar4.jar.analyzer.utils.ClasspathRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelfDecompileRegressionTest {
    @Test
    void shouldKeepJarAnalyzerCfrEngineStructured(@TempDir Path tempDir) throws IOException {
        String decompiled = CfrDecompilerRegressionSupport.decompileJava(
                CfrDecompilerRegressionSupport.locateClassFile(CFRDecompileEngine.class));

        assertStructured(decompiled);
        assertTrue(decompiled.contains("private static List<Path> collectNestedLibJars(Path resourcesRoot)"), decompiled);
        assertTrue(
                decompiled.contains("stream.forEach(path -> {")
                        || decompiled.contains("stream.forEach((Path path) -> {"),
                decompiled);
        assertTrue(decompiled.contains("String name = path.getFileName().toString().toLowerCase(Locale.ROOT);"), decompiled);
        assertTrue(decompiled.contains("String rel = root.relativize((Path)path).toString().replace(\"\\\\\", \"/\");"), decompiled);
        assertTrue(decompiled.contains("isNestedLibPath(rel)"), decompiled);
        assertTrue(decompiled.contains("new BuildScopedLru<String, String>("), decompiled);
        assertTrue(decompiled.contains("new LinkedHashMap<Path, List<String>>()"), decompiled);
        assertTrue(decompiled.contains("new ArrayList<String>()"), decompiled);
        assertTrue(decompiled.contains("SinkReturns.Decompiled decompiled = (SinkReturns.Decompiled)obj;"), decompiled);
        assertTrue(decompiled.contains("int[] failures = new int[]{0};"), decompiled);
        assertTrue(decompiled.contains("IOException[] first = new IOException[]{null};"), decompiled);
        assertFalse(decompiled.contains("Stream<Path> name ="), decompiled);
        assertFalse(decompiled.contains("Sink<Object>"), decompiled);
        assertFalse(decompiled.contains("new BuildScopedLru<K, V>("), decompiled);
        assertFalse(decompiled.contains("new LinkedHashMap<Path, List>("), decompiled);
        assertFalse(decompiled.contains("new ArrayList<E>()"), decompiled);
        assertFalse(decompiled.contains("OutputSinkFactory.SinkClass decompiled ="), decompiled);
        assertFalse(decompiled.contains("int[] failures = null;"), decompiled);
        assertFalse(decompiled.contains("IOException[] first = null;"), decompiled);
        CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                "CFRDecompileEngine",
                decompiled,
                "-classpath",
                System.getProperty("java.class.path"));
    }

    @Test
    void shouldKeepJarAnalyzerClasspathRegistryStructured() throws IOException {
        String decompiled = CfrDecompilerRegressionSupport.decompile(
                CfrDecompilerRegressionSupport.locateClassFile(ClasspathRegistry.class));

        assertStructured(decompiled);
        assertTrue(decompiled.contains("private static boolean matchesAllowedHash(String dirName, Set<String> allowedHashes)"), decompiled);
        assertTrue(decompiled.contains("allowedHashes.isEmpty()"), decompiled);
        assertTrue(decompiled.contains("return allowedHashes.contains(suffix);"), decompiled);
    }

    @Test
    void shouldKeepJarAnalyzerClassLookupStructured(@TempDir Path tempDir) throws IOException {
        String decompiled = CfrDecompilerRegressionSupport.decompileJava(
                CfrDecompilerRegressionSupport.locateClassFile(ClassLookupService.class));

        assertStructured(decompiled);
        assertTrue(decompiled.contains("public static LookupResult find(String nameOrPath, Integer preferJarId)"), decompiled);
        assertTrue(decompiled.contains("ClassLookupService.findClassInternal("), decompiled);
        assertTrue(decompiled.contains("return new LookupResult(data, path.toString(), null, path.toString());"), decompiled);
        assertFalse(decompiled.contains("byte[] external = BytecodeCache.read(path);"), decompiled);
        assertFalse(decompiled.contains("LookupResult lookupResult = result;"), decompiled);
        assertFalse(decompiled.contains("InputStream inputStream = zipFile.getInputStream(entry);"), decompiled);
        CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                "ClassLookupService",
                decompiled,
                "-classpath",
                System.getProperty("java.class.path"));
    }

    @Test
    void shouldKeepJarAnalyzerCallGraphPlanCompilable(@TempDir Path tempDir) throws IOException {
        String decompiled = CfrDecompilerRegressionSupport.decompileJava(
                CfrDecompilerRegressionSupport.locateClassFile(CallGraphPlan.class));

        assertStructured(decompiled);
        assertTrue(decompiled.contains("public record CallGraphPlan("), decompiled);
        assertFalse(decompiled.contains("ObjectMethods.bootstrap"), decompiled);
        assertFalse(decompiled.contains("public final String toString()"), decompiled);
        CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                "CallGraphPlan",
                decompiled,
                "-classpath",
                System.getProperty("java.class.path"));
    }

    @Test
    void shouldKeepJarAnalyzerIdentifyCallCompilable(@TempDir Path tempDir) throws IOException {
        String decompiled = CfrDecompilerRegressionSupport.decompileJava(
                CfrDecompilerRegressionSupport.locateClassFile(IdentifyCall.class));

        assertStructured(decompiled);
        assertTrue(decompiled.contains("Comparator.comparingInt((int[] a) -> a[0]).thenComparingInt((int[] a) -> a[1])"), decompiled);
        CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                "IdentifyCall",
                decompiled,
                "-classpath",
                System.getProperty("java.class.path"));
    }

    @Test
    void shouldKeepJarAnalyzerStringMethodVisitorCompilable(@TempDir Path tempDir) throws IOException {
        String decompiled = CfrDecompilerRegressionSupport.decompileJava(
                CfrDecompilerRegressionSupport.locateClassFile(StringMethodVisitor.class));

        assertStructured(decompiled);
        assertFalse(decompiled.contains("new ArrayList<E>()"), decompiled);
        CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                "StringMethodVisitor",
                decompiled,
                "-classpath",
                System.getProperty("java.class.path"));
    }

    @Test
    void shouldKeepJarAnalyzerSearchWorkflowSupportCompilable(@TempDir Path tempDir) throws IOException {
        String decompiled = CfrDecompilerRegressionSupport.decompileJava(
                locateJarAnalyzerClassFile("gui", "runtime", "api", "SearchWorkflowSupport.class"));

        assertStructured(decompiled);
        assertFalse(decompiled.contains("LinkedHashMap<CallSite, SearchResultDto>"), decompiled);
        assertFalse(decompiled.contains("(CallSite)((Object)rowKey)"), decompiled);
        CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                "SearchWorkflowSupport",
                decompiled,
                "-classpath",
                System.getProperty("java.class.path"));
    }

    @Test
    void shouldKeepJarAnalyzerBytecodeMainlineSemanticEdgeRunnerCompilable(@TempDir Path tempDir) throws IOException {
        String decompiled = CfrDecompilerRegressionSupport.decompileJava(
                locateJarAnalyzerClassFile("core", "BytecodeMainlineSemanticEdgeRunner.class"));

        assertStructured(decompiled);
        assertFalse(decompiled.contains("Map.Entry entry;"), decompiled);
        assertFalse(decompiled.contains("HashSet callees ="), decompiled);
        CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                "BytecodeMainlineSemanticEdgeRunner",
                decompiled,
                "-classpath",
                System.getProperty("java.class.path"));
    }

    @Test
    void shouldKeepJarAnalyzerInheritanceMapCompilable(@TempDir Path tempDir) throws IOException {
        String decompiled = CfrDecompilerRegressionSupport.decompileJava(
                CfrDecompilerRegressionSupport.locateClassFile(InheritanceMap.class));

        assertStructured(decompiled);
        assertFalse(decompiled.contains("Iterator<Object>"), decompiled);
        assertFalse(decompiled.contains("Map.Entry entry;"), decompiled);
        CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                "InheritanceMap",
                decompiled,
                "-classpath",
                System.getProperty("java.class.path"));
    }

    @Test
    void shouldKeepJarAnalyzerConstraintFactAssemblerCompilable(@TempDir Path tempDir) throws IOException {
        String decompiled = CfrDecompilerRegressionSupport.decompileJava(
                locateJarAnalyzerClassFile("core", "facts", "ConstraintFactAssembler.class"));

        assertStructured(decompiled);
        assertFalse(decompiled.contains("Map.Entry entry;"), decompiled);
        assertFalse(decompiled.contains("LinkedHashMap receiverVarByCallSiteKey ="), decompiled);
        CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                "ConstraintFactAssembler",
                decompiled,
                "-classpath",
                System.getProperty("java.class.path"));
    }

    @Test
    void shouldKeepJarAnalyzerJspCompileRunnerCompilable(@TempDir Path tempDir) throws IOException {
        String decompiled = CfrDecompilerRegressionSupport.decompileJava(
                CfrDecompilerRegressionSupport.locateClassFile(JspCompileRunner.class));

        assertStructured(decompiled);
        assertFalse(decompiled.contains("Iterator<Object> iterator"), decompiled);
        CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                "JspCompileRunner",
                decompiled,
                "-classpath",
                System.getProperty("java.class.path"));
    }

    @Test
    void shouldKeepJarAnalyzerSpringClassVisitorCompilable(@TempDir Path tempDir) throws IOException {
        String decompiled = CfrDecompilerRegressionSupport.decompileJava(
                CfrDecompilerRegressionSupport.locateClassFile(SpringClassVisitor.class));

        assertStructured(decompiled);
        assertFalse(decompiled.contains("((Stream)"), decompiled);
        CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                "SpringClassVisitor",
                decompiled,
                "-classpath",
                System.getProperty("java.class.path"));
    }

    @Test
    void shouldKeepJarAnalyzerImplToolPanelCompilable(@TempDir Path tempDir) throws IOException {
        String decompiled = CfrDecompilerRegressionSupport.decompileJava(
                CfrDecompilerRegressionSupport.locateClassFile(ImplToolPanel.class));

        assertStructured(decompiled);
        assertFalse(decompiled.contains("MethodNavDto selected = null;"), decompiled);
        CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                "ImplToolPanel",
                decompiled,
                "-classpath",
                System.getProperty("java.class.path"));
    }

    @Test
    void shouldKeepJarAnalyzerLeakToolPanelCompilable(@TempDir Path tempDir) throws IOException {
        String decompiled = CfrDecompilerRegressionSupport.decompileJava(
                CfrDecompilerRegressionSupport.locateClassFile(LeakToolPanel.class));

        assertStructured(decompiled);
        assertFalse(decompiled.contains("LeakItemDto selected = null;"), decompiled);
        CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                "LeakToolPanel",
                decompiled,
                "-classpath",
                System.getProperty("java.class.path"));
    }

    @Test
    void shouldKeepJarAnalyzerAliasRuleSupportCompilable(@TempDir Path tempDir) throws IOException {
        String decompiled = CfrDecompilerRegressionSupport.decompileJava(
                CfrDecompilerRegressionSupport.locateClassFile(AliasRuleSupport.class));

        assertStructured(decompiled);
        assertFalse(decompiled.contains("TaintModelRule additionalRule = null;"), decompiled);
        CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                "AliasRuleSupport",
                decompiled,
                "-classpath",
                System.getProperty("java.class.path"));
    }

    @Test
    void shouldKeepJarAnalyzerCoreRunnerSnapshotTypedWithinLambda(@TempDir Path tempDir) throws IOException {
        String decompiled = CfrDecompilerRegressionSupport.decompileJava(
                CfrDecompilerRegressionSupport.locateClassFile(CoreRunner.class));

        assertStructured(decompiled);
        assertTrue(decompiled.contains("ProjectRuntimeSnapshot snapshot = DatabaseManager.buildProjectRuntimeSnapshot("), decompiled);
        assertFalse(decompiled.contains("int snapshot = (int)DatabaseManager.buildProjectRuntimeSnapshot("), decompiled);
        CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                "CoreRunner",
                decompiled,
                "-classpath",
                System.getProperty("java.class.path"));
    }

    private static void assertStructured(String decompiled) {
        assertFalse(decompiled.contains("Unable to fully structure code"), decompiled);
        assertFalse(decompiled.contains("** GOTO"), decompiled);
    }

    private static Path locateJarAnalyzerClassFile(String... relativeParts) {
        return Path.of("target", "classes", "me", "n1ar4", "jar", "analyzer").resolve(Path.of("", relativeParts));
    }
}
