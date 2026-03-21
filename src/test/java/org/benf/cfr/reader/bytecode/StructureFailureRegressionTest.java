package org.benf.cfr.reader.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureFailureRegressionTest {
    @Test
    void shouldReduceDefiniteAssignmentGotoResidue(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "structure-failure",
                "TestDefiniteAssignment",
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);

        assertFalse(decompiled.contains("Unable to fully structure code"), decompiled);
        assertFalse(decompiled.contains("** GOTO"), decompiled);
        assertFalse(decompiled.contains("lbl-1000"), decompiled);
        assertFalse(decompiled.contains("block6:"), decompiled);
        assertFalse(decompiled.contains("block-1:"), decompiled);
        assertFalse(decompiled.contains("double d2;"), decompiled);
        assertFalse(decompiled.contains("double d3 = n;"), decompiled);
        assertFalse(decompiled.contains("if (bool) ** GOTO"), decompiled);
        assertFalse(decompiled.contains("lbl3:"), decompiled);
        assertTrue(decompiled.contains("if (!bool) {"), decompiled);
        assertTrue(decompiled.contains("} else {"), decompiled);
        assertTrue(decompiled.contains("double cFake = 0.01;"), decompiled);
        assertTrue(decompiled.contains("System.out.println(cFake);"), decompiled);
        assertTrue(decompiled.contains("c += 5.0;"), decompiled);
        assertTrue(decompiled.contains("boolean x = d > 0.0;") || decompiled.contains("if (x = d > 0.0)"), decompiled);
        assertTrue(decompiled.contains("System.out.println(d);"), decompiled);
        CfrDecompilerRegressionSupport.compileJava(tempDir, "TestDefiniteAssignment", decompiled);
    }

    @Test
    void shouldKeepLoopInSwitchWithLabelledBreakStructured(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "structure-failure",
                "TestSwitchLoop",
                "--release", "8");

        String decompiled = CfrDecompilerRegressionSupport.decompile(classFile);

        assertFalse(decompiled.contains("Unable to fully structure code"), decompiled);
        assertFalse(decompiled.contains("** GOTO"), decompiled);
        assertFalse(Pattern.compile("(?m)^\\s*block\\d+:").matcher(decompiled).find(), decompiled);
        assertFalse(Pattern.compile("\\bbreak block\\d+;").matcher(decompiled).find(), decompiled);
        assertTrue(decompiled.contains("switch (i)"), decompiled);
    }

    @Test
    void shouldKeepAssignedLocalBoundToFollowingIfCondition(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "structure-failure",
                "TestTail",
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);

        assertFalse(decompiled.contains("double d2 = n;"), decompiled);
        assertTrue(decompiled.contains("boolean x = d > 0.0;") || decompiled.contains("boolean x = false;"), decompiled);
        assertTrue(decompiled.contains("if (x) {") || decompiled.contains("if (x = d > 0.0)"), decompiled);
        assertTrue(decompiled.contains("System.out.println(d);"), decompiled);
        CfrDecompilerRegressionSupport.compileJava(tempDir, "TestTail", decompiled);
    }

    @Test
    void shouldCleanupEmptyIfResidueAfterSwitchExpressionRewrite(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "structure-failure",
                "InlineSwitchExpressionSample",
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompile(classFile);

        assertTrue(decompiled.contains("switch (++j)"));
        assertFalse(decompiled.contains("label3:"));
        assertFalse(decompiled.contains("empty if block"));
        assertFalse(decompiled.contains("Unable to fully structure code"));
    }

    @Test
    void shouldKeepGuardReturnChainStructured(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "structure-failure",
                "GuardReturnChainSample",
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);

        assertFalse(decompiled.contains("Unable to fully structure code"), decompiled);
        assertFalse(decompiled.contains("** GOTO"), decompiled);
        assertTrue(decompiled.contains("allowedHashes.isEmpty()"), decompiled);
        assertTrue(decompiled.contains("dirName.lastIndexOf(45)"), decompiled);
        assertTrue(decompiled.contains("return allowedHashes.contains(suffix);"), decompiled);
        CfrDecompilerRegressionSupport.compileJava(tempDir, "GuardReturnChainSample", decompiled);
    }

    @Test
    void shouldKeepLambdaCollectorStructured(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "structure-failure",
                "LambdaGuardCollectorSample",
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompile(classFile);

        assertFalse(decompiled.contains("Unable to fully structure code"), decompiled);
        assertFalse(decompiled.contains("** GOTO"), decompiled);
        assertFalse(decompiled.contains("lambda$"), decompiled);
        assertFalse(decompiled.contains("new List<Path>()"), decompiled);
        assertTrue(decompiled.contains("stream.forEach("), decompiled);
        assertTrue(decompiled.contains("Files.walk(root"), decompiled);
        assertTrue(decompiled.contains("new ArrayList<Path>()"), decompiled);
        assertTrue(decompiled.contains("jars.add"), decompiled);
    }
}
