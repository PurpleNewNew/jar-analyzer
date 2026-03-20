package org.benf.cfr.reader.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwitchStringRewriteRegressionTest {
    @Test
    void shouldResugarStringSwitchWithoutHashcodeScaffolding(@TempDir Path tempDir) throws IOException {
        String source = """
                public class SwitchStringRewriteSample {
                    static int pick(String value) {
                        return switch (value) {
                            case "a" -> 1;
                            case "bc" -> 2;
                            default -> 3;
                        };
                    }
                }
                """;

        Path classFile = CfrDecompilerRegressionSupport.compileJava(tempDir, "SwitchStringRewriteSample", source, "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);

        assertTrue(decompiled.contains("switch (value)"), decompiled);
        assertTrue(decompiled.contains("case \"a\":") || decompiled.contains("case \"a\" ->"), decompiled);
        assertTrue(decompiled.contains("case \"bc\":") || decompiled.contains("case \"bc\" ->"), decompiled);
        assertFalse(decompiled.contains("hashCode()"), decompiled);
        assertFalse(decompiled.contains("equals(value)"), decompiled);

        Path decompiledDir = Files.createDirectories(tempDir.resolve("decompiled-string-switch"));
        CfrDecompilerRegressionSupport.compileJava(decompiledDir, "SwitchStringRewriteSample", decompiled, "--release", "21");
    }
}
