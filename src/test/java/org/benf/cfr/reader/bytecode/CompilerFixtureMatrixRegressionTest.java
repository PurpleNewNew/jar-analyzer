package org.benf.cfr.reader.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompilerFixtureMatrixRegressionTest {
    @Test
    void shouldRoundTripTypicalSwitchFixturesAcrossCompilerAndReleaseBands(@TempDir Path tempDir) throws IOException {
        assertLegacySwitchFixture(tempDir, fixture("compiler-matrix", "LegacySwitchSample", CfrDecompilerRegressionSupport.SourceCompiler.JAVAC, "8"));
        assertLegacySwitchFixture(tempDir, fixture("compiler-matrix", "LegacySwitchSample", CfrDecompilerRegressionSupport.SourceCompiler.ECJ, "17"));
        assertEnumSwitchFixture(tempDir, fixture("compiler-matrix", "EnumSwitchMatrixSample", CfrDecompilerRegressionSupport.SourceCompiler.ECJ, "17"));
        assertModernSwitchExpressionFixture(tempDir, fixture("compiler-matrix", "ModernSwitchExpressionSample", CfrDecompilerRegressionSupport.SourceCompiler.JAVAC, "21"));
        assertLabelledSwitchFixture(tempDir, fixture("compiler-matrix", "LabelledSwitchLoopMatrixSample", CfrDecompilerRegressionSupport.SourceCompiler.JAVAC, "8"));
        assertLabelledSwitchFixture(tempDir, fixture("compiler-matrix", "LabelledSwitchLoopMatrixSample", CfrDecompilerRegressionSupport.SourceCompiler.ECJ, "17"));
    }

    @Test
    void shouldRoundTripPatternAndRecordFixturesAcrossModernReleaseBands(@TempDir Path tempDir) throws IOException {
        assertPatternFixture(tempDir, fixture("pattern-recovery", "SwitchPatternSample", CfrDecompilerRegressionSupport.SourceCompiler.JAVAC, "21"));
        assertRecordFixture(tempDir, fixture("record-object-methods", "SimpleRecordSample", CfrDecompilerRegressionSupport.SourceCompiler.JAVAC, "17"));
        assertRecordFixture(tempDir, fixture("record-object-methods", "SimpleRecordSample", CfrDecompilerRegressionSupport.SourceCompiler.ECJ, "17"));
        assertLocalEnumFixture(tempDir, fixture("modern-output-polish", "TestLocalEnum", CfrDecompilerRegressionSupport.SourceCompiler.JAVAC, "21"));
    }

    @Test
    void shouldRoundTripSyntheticAccessorAndTryResourcesFixturesAcrossLegacyAndModernBands(@TempDir Path tempDir) throws IOException {
        assertSyntheticAccessorFixture(tempDir, fixture("bridge-synthetic", "SyntheticAccessorSample", CfrDecompilerRegressionSupport.SourceCompiler.JAVAC, "8"), false);
        assertSyntheticAccessorFixture(tempDir, fixture("compiler-matrix", "SyntheticAccessorMutationMatrixSample", CfrDecompilerRegressionSupport.SourceCompiler.ECJ, "8"), true);
        assertTryResourcesFixture(tempDir, fixture("compiler-matrix", "LegacyTryResourcesMatrixSample", CfrDecompilerRegressionSupport.SourceCompiler.JAVAC, "8"), "ByteArrayOutputStream", false, true);
        assertTryResourcesFixture(tempDir, fixture("compiler-matrix", "LegacyTryResourcesMatrixSample", CfrDecompilerRegressionSupport.SourceCompiler.ECJ, "8"), "ByteArrayOutputStream", false, false);
        assertTryResourcesFixture(tempDir, fixture("compiler-matrix", "ModernTryResourcesMatrixSample", CfrDecompilerRegressionSupport.SourceCompiler.JAVAC, "21"), "StringWriter", false, true);
        assertTryResourcesFixture(tempDir, fixture("compiler-matrix", "ModernTryResourcesMatrixSample", CfrDecompilerRegressionSupport.SourceCompiler.ECJ, "17"), "StringWriter", false, false);
        assertTryResourcesFinallyFixture(tempDir, fixture("compiler-matrix", "TryResourcesFinallyMatrixSample", CfrDecompilerRegressionSupport.SourceCompiler.JAVAC, "17"), "Scanner");
    }

    @Test
    void shouldKeepPrecisionAndOutputContractFixturesStableAcrossCompilerBands(@TempDir Path tempDir) throws IOException {
        assertFloatPrecisionFixture(tempDir, fixture("compiler-matrix", "FloatPrecisionMatrixSample", CfrDecompilerRegressionSupport.SourceCompiler.JAVAC, "8"));
        assertFloatPrecisionFixture(tempDir, fixture("compiler-matrix", "FloatPrecisionMatrixSample", CfrDecompilerRegressionSupport.SourceCompiler.ECJ, "17"));
    }

    private void assertLegacySwitchFixture(Path tempDir, FixtureVariant variant) throws IOException {
        String decompiled = decompileFixture(tempDir, variant);
        assertTrue(decompiled.contains("switch (value)"), decompiled);
        assertTrue(decompiled.contains("case \"open\":") || decompiled.contains("case \"open\" ->"), decompiled);
        assertTrue(decompiled.contains("case \"closed\":") || decompiled.contains("case \"closed\" ->"), decompiled);
        assertFalse(decompiled.contains(".hashCode()"), decompiled);
        assertFalse(decompiled.contains("lookupswitch"), decompiled);
        roundTripDecompiled(tempDir, variant, decompiled);
    }

    private void assertEnumSwitchFixture(Path tempDir, FixtureVariant variant) throws IOException {
        String decompiled = decompileFixture(tempDir, variant);
        assertTrue(decompiled.contains("switch (mode)"), decompiled);
        assertTrue(decompiled.contains("case OPEN:") || decompiled.contains("case OPEN ->"), decompiled);
        assertTrue(decompiled.contains("case CLOSED:") || decompiled.contains("case CLOSED ->"), decompiled);
        assertFalse(decompiled.contains("$SWITCH_TABLE$"), decompiled);
        assertFalse(decompiled.contains("ordinal()"), decompiled);
        roundTripDecompiled(tempDir, variant, decompiled);
    }

    private void assertModernSwitchExpressionFixture(Path tempDir, FixtureVariant variant) throws IOException {
        String decompiled = decompileFixture(tempDir, variant);
        assertTrue(decompiled.contains("switch (value)"), decompiled);
        assertTrue(decompiled.contains("case \"open\" ->"), decompiled);
        assertTrue(decompiled.contains("case \"closed\" ->"), decompiled);
        assertFalse(decompiled.contains(" cfr_switch_"), decompiled);
        roundTripDecompiled(tempDir, variant, decompiled);
    }

    private void assertLabelledSwitchFixture(Path tempDir, FixtureVariant variant) throws IOException {
        String decompiled = decompileFixture(tempDir, variant);
        assertTrue(decompiled.contains("switch (i)"), decompiled);
        assertFalse(Pattern.compile("(?m)^\\s*block\\d+:").matcher(decompiled).find(), decompiled);
        assertFalse(Pattern.compile("\\bbreak block\\d+;").matcher(decompiled).find(), decompiled);
        assertFalse(decompiled.contains("** GOTO"), decompiled);
        roundTripDecompiled(tempDir, variant, decompiled);
    }

    private void assertPatternFixture(Path tempDir, FixtureVariant variant) throws IOException {
        String decompiled = decompileFixture(tempDir, variant);
        assertTrue(decompiled.contains("case String s:" ) || decompiled.contains("case String s ->"), decompiled);
        assertTrue(decompiled.contains("case Integer i when i > 10:" ) || decompiled.contains("case Integer i when i > 10 ->"), decompiled);
        assertTrue(decompiled.contains("case Point(int x, int y):" ) || decompiled.contains("case Point(int x, int y) ->"), decompiled);
        assertFalse(decompiled.contains("SwitchBootstraps"), decompiled);
    }

    private void assertRecordFixture(Path tempDir, FixtureVariant variant) throws IOException {
        String decompiled = decompileFixture(tempDir, variant);
        assertTrue(decompiled.contains("record SimpleRecordSample(String name, int age)"), decompiled);
        assertFalse(decompiled.contains("ObjectMethods.bootstrap"), decompiled);
        assertFalse(decompiled.contains("public String name()"), decompiled);
        roundTripDecompiled(tempDir, variant, decompiled);
    }

    private void assertLocalEnumFixture(Path tempDir, FixtureVariant variant) throws IOException {
        String decompiled = decompileFixture(tempDir, variant);
        assertTrue(decompiled.contains("enum Type"), decompiled);
        assertFalse(decompiled.contains("static enum Type"), decompiled);
        roundTripDecompiled(tempDir, variant, decompiled);
    }

    private void assertSyntheticAccessorFixture(Path tempDir, FixtureVariant variant, boolean mutating) throws IOException {
        String decompiled = decompileFixture(tempDir, variant);
        assertFalse(decompiled.contains("access$"), decompiled);
        assertFalse(decompiled.contains("this$0"), decompiled);
        if (mutating) {
            assertTrue(decompiled.contains("++SyntheticAccessorMutationMatrixSample.this.counter;")
                    || decompiled.contains("SyntheticAccessorMutationMatrixSample.this.counter++;")
                    || decompiled.contains("syntheticAccessorMutationMatrixSample.counter = syntheticAccessorMutationMatrixSample.counter + 1;")
                    || decompiled.contains("++this.this$0.counter;")
                    || decompiled.contains("this.this$0.counter++;")
                    || decompiled.contains("++counter;")
                    || decompiled.contains("counter++;"), decompiled);
        }
        roundTripDecompiled(tempDir, variant, decompiled);
    }

    private void assertTryResourcesFixture(Path tempDir,
                                           FixtureVariant variant,
                                           String resourceType,
                                           boolean requireSuppressedCleanup,
                                           boolean requireRoundTrip) throws IOException {
        String decompiled = decompileFixture(tempDir, variant);
        assertTrue(Pattern.compile("try \\(" + resourceType + " \\w+ = new " + resourceType + "\\(\\);?\\)").matcher(decompiled).find(), decompiled);
        if (requireSuppressedCleanup) {
            assertFalse(decompiled.contains("addSuppressed"), decompiled);
        }
        assertFalse(decompiled.contains(".close();"), decompiled);
        if (requireRoundTrip) {
            roundTripDecompiled(tempDir, variant, decompiled);
        }
    }

    private void assertTryResourcesFinallyFixture(Path tempDir,
                                                  FixtureVariant variant,
                                                  String resourceType) throws IOException {
        String decompiled = decompileFixture(tempDir, variant);
        assertTrue(Pattern.compile("try \\(" + resourceType + " \\w+ = new " + resourceType + "\\([^)]*\\)\\)\\s*\\{").matcher(decompiled).find(), decompiled);
        assertFalse(decompiled.contains("Removed try catching itself"), decompiled);
        assertFalse(decompiled.contains(";){"), decompiled);
        roundTripDecompiled(tempDir, variant, decompiled);
    }

    private void assertFloatPrecisionFixture(Path tempDir, FixtureVariant variant) throws IOException {
        String decompiled = decompileFixture(tempDir, variant);
        assertTrue(decompiled.contains("double x"), decompiled);
        assertFalse(decompiled.contains("float x = 0.2f;"), decompiled);
        roundTripDecompiled(tempDir, variant, decompiled);
    }

    private String decompileFixture(Path tempDir, FixtureVariant variant) throws IOException {
        Path compiledDir = Files.createDirectories(tempDir.resolve(variant.id()).resolve("compiled"));
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                compiledDir,
                variant.fixtureSuite,
                variant.className,
                variant.compiler,
                variant.release);
        return CfrDecompilerRegressionSupport.decompileJava(classFile);
    }

    private void roundTripDecompiled(Path tempDir, FixtureVariant variant, String decompiled) throws IOException {
        Path decompiledDir = Files.createDirectories(tempDir.resolve(variant.id()).resolve("decompiled"));
        CfrDecompilerRegressionSupport.compileJava(decompiledDir, variant.className, decompiled, "--release", variant.release);
    }

    private static FixtureVariant fixture(String fixtureSuite,
                                          String className,
                                          CfrDecompilerRegressionSupport.SourceCompiler compiler,
                                          String release) {
        return new FixtureVariant(fixtureSuite, className, compiler, release);
    }

    private record FixtureVariant(String fixtureSuite,
                                  String className,
                                  CfrDecompilerRegressionSupport.SourceCompiler compiler,
                                  String release) {
        private String id() {
            return fixtureSuite + "-" + className + "-" + compiler.name().toLowerCase() + "-" + release;
        }
    }
}
