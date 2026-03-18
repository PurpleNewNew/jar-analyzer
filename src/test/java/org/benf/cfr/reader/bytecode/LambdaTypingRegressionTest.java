package org.benf.cfr.reader.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LambdaTypingRegressionTest {
    @Test
    void shouldPreserveLambdaReturnTargetTyping(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "type-recovery",
                "LambdaTypingSample",
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);

        assertTrue(Pattern.compile("return \\(String value\\) -> new ArrayList<String>\\(\\);").matcher(decompiled).find(), decompiled);
        assertTrue(Pattern.compile("Comparator\\.comparingInt\\(\\(String value\\) -> value\\.length\\(\\)\\)").matcher(decompiled).find(), decompiled);
        assertFalse(Pattern.compile("new ArrayList<E>\\(\\)").matcher(decompiled).find(), decompiled);

        CfrDecompilerRegressionSupport.compileJava(tempDir, "LambdaTypingSample", decompiled, "--release", "21");
    }
}
