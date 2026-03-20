package org.benf.cfr.reader.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BridgeAndSyntheticSemanticsRegressionTest {
    @Test
    void shouldHideBridgeMethodsWhileKeepingGenericOverride(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "bridge-synthetic",
                "BridgeMethodSample",
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);

        assertTrue(decompiled.contains("static final class Impl"), decompiled);
        assertTrue(decompiled.contains("extends Base<String>"), decompiled);
        assertTrue(decompiled.contains("public String get()"), decompiled);
        assertFalse(decompiled.contains("public Object get()"), decompiled);
        assertFalse(decompiled.contains("/* bridge */"), decompiled);

        CfrDecompilerRegressionSupport.compileJava(tempDir, "BridgeMethodSample", decompiled, "--release", "21");
    }

    @Test
    void shouldInlineSyntheticAccessorsFromLegacyInnerClassPatterns(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "bridge-synthetic",
                "SyntheticAccessorSample",
                "--release", "8");

        String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);

        assertFalse(decompiled.contains("access$"), decompiled);
        assertFalse(decompiled.contains("this$0"), decompiled);

        CfrDecompilerRegressionSupport.compileJava(tempDir, "SyntheticAccessorSample", decompiled, "--release", "21");
    }

    @Test
    void shouldInlineLegacySyntheticMutatingAccessors(@TempDir Path tempDir) throws IOException {
        String source = """
                public class SyntheticAccessorMutationSample {
                    private int counter;

                    class Inner {
                        void bump() {
                            ++counter;
                        }

                        int read() {
                            return counter;
                        }
                    }

                    static int run() {
                        SyntheticAccessorMutationSample outer = new SyntheticAccessorMutationSample();
                        Inner inner = outer.new Inner();
                        inner.bump();
                        inner.bump();
                        return inner.read();
                    }
                }
                """;

        Path classFile = CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                "SyntheticAccessorMutationSample",
                source,
                "--release", "8");

        String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);

        assertFalse(decompiled.contains("access$"), decompiled);
        assertTrue(decompiled.contains("++this.this$0.counter;")
                || decompiled.contains("++counter;")
                || decompiled.contains("++SyntheticAccessorMutationSample.this.counter;")
                || decompiled.contains("this.this$0.counter++;")
                || decompiled.contains("SyntheticAccessorMutationSample.this.counter++;")
                || decompiled.contains("counter++;"), decompiled);

        CfrDecompilerRegressionSupport.compileJava(tempDir, "SyntheticAccessorMutationSample", decompiled, "--release", "21");
    }
}
