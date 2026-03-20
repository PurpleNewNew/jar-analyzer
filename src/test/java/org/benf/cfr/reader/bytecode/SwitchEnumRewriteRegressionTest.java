package org.benf.cfr.reader.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwitchEnumRewriteRegressionTest {
    @Test
    void shouldResugarEcjEnumSwitchWithoutLookupScaffolding(@TempDir Path tempDir) throws IOException {
        String source = """
                public class SwitchEnumRewriteSample {
                    enum Mode { OPEN, CLOSED, UNKNOWN }

                    static int map(Mode mode) {
                        switch (mode) {
                            case OPEN:
                                return 1;
                            case CLOSED:
                                return 2;
                            default:
                                return 3;
                        }
                    }
                }
                """;

        Path classFile = CfrDecompilerRegressionSupport.compileJavaWithEcj(
                tempDir,
                "SwitchEnumRewriteSample",
                source,
                "-17");

        String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);

        assertTrue(decompiled.contains("switch (mode)"), decompiled);
        assertTrue(decompiled.contains("case OPEN:") || decompiled.contains("case OPEN ->"), decompiled);
        assertTrue(decompiled.contains("case CLOSED:") || decompiled.contains("case CLOSED ->"), decompiled);
        assertFalse(decompiled.contains("$SWITCH_TABLE$"), decompiled);
        assertFalse(decompiled.contains("ordinal()"), decompiled);

        Path decompiledDir = Files.createDirectories(tempDir.resolve("decompiled-enum-switch"));
        CfrDecompilerRegressionSupport.compileJava(decompiledDir, "SwitchEnumRewriteSample", decompiled, "--release", "21");
    }
}
