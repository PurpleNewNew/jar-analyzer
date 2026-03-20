package org.benf.cfr.reader.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssertRewriteRegressionTest {
    @Test
    void shouldResugarSimpleAssertion(@TempDir Path tempDir) throws IOException {
        String source = """
                public class AssertRewriteSample {
                    static int check(int value) {
                        assert value > 0 : value;
                        return value;
                    }
                }
                """;

        Path classFile = CfrDecompilerRegressionSupport.compileJava(tempDir, "AssertRewriteSample", source, "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);

        assertTrue(decompiled.contains("assert (value > 0) : value;"), decompiled);
        assertFalse(decompiled.contains("$assertionsDisabled"), decompiled);
        assertFalse(decompiled.contains("throw new AssertionError"), decompiled);

        Path decompiledDir = Files.createDirectories(tempDir.resolve("decompiled"));
        CfrDecompilerRegressionSupport.compileJava(decompiledDir, "AssertRewriteSample", decompiled, "--release", "21");
    }
}
