package org.benf.cfr.reader.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetTypingRegressionTest {
    @Test
    void shouldPreserveTargetTypingForConstructorAndNestedLambdaInference(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "type-recovery",
                "TargetTypingConstructorSample",
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);

        assertTrue(Pattern.compile("new LinkedHashMap<String, List<String>>\\(\\)").matcher(decompiled).find(), decompiled);
        assertTrue(Pattern.compile("computeIfAbsent\\(value, \\(String ignored\\) -> new ArrayList<String>\\(\\)\\)").matcher(decompiled).find(), decompiled);
        assertFalse(Pattern.compile("new LinkedHashMap<String, List>\\(\\)").matcher(decompiled).find(), decompiled);
        assertFalse(Pattern.compile("new ArrayList<E>\\(\\)").matcher(decompiled).find(), decompiled);

        CfrDecompilerRegressionSupport.compileJava(tempDir, "TargetTypingConstructorSample", decompiled, "--release", "21");
    }
}
