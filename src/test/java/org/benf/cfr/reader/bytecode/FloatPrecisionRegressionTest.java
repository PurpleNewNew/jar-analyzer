package org.benf.cfr.reader.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FloatPrecisionRegressionTest {
    @Test
    void shouldKeepDoubleDeclarationForFloatLiteralPrecisionConstant(@TempDir Path tempDir) throws IOException {
        String source = """
                public class FloatPrecisionSample {
                    public void test() {
                        double x = 0.2F;
                    }
                }
                """;

        Path classFile = CfrDecompilerRegressionSupport.compileJava(tempDir, "FloatPrecisionSample", source, "--release", "8");

        String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);

        assertTrue(decompiled.contains("double x"), decompiled);
        assertFalse(decompiled.contains("float x = 0.2f;"), decompiled);

        Path decompiledDir = Files.createDirectories(tempDir.resolve("decompiled-float-precision"));
        CfrDecompilerRegressionSupport.compileJava(decompiledDir, "FloatPrecisionSample", decompiled, "--release", "21");
    }
}
