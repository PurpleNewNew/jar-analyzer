package org.benf.cfr.reader.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatternRecoveryRegressionTest {
    @Test
    void shouldUseUnifiedPatternSemanticsForIfAndSwitch(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "pattern-recovery",
                "UnifiedPatternSample",
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompile(classFile);
        assertTrue(Pattern.compile("instanceof String \\w+").matcher(decompiled).find());
        assertTrue(Pattern.compile("case Integer (\\w+) when \\1 > 10:").matcher(decompiled).find());
        assertTrue(Pattern.compile("case Point\\(int \\w+, int \\w+\\):").matcher(decompiled).find());
        assertFalse(decompiled.contains("SwitchBootstraps.typeSwitch"));
        assertFalse(decompiled.contains("MatchException"));
        assertFalse(decompiled.contains("= (String)o"));
    }

    @Test
    void shouldRewriteJava21TypeSwitchIntoPatternCases(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "pattern-recovery",
                "SwitchPatternSample",
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompile(classFile);

        assertFalse(decompiled.contains("SwitchBootstraps.typeSwitch"));
        assertFalse(decompiled.contains("while (true)"));
        assertFalse(decompiled.contains("continue block"));
        assertFalse(decompiled.contains("MatchException"));
        assertTrue(Pattern.compile("case String \\w+:").matcher(decompiled).find());
        assertTrue(Pattern.compile("case Integer (\\w+) when \\1 > 10:").matcher(decompiled).find());
        assertTrue(Pattern.compile("case Integer \\w+:").matcher(decompiled).find());
        assertTrue(Pattern.compile("case Point\\(int \\w+, int \\w+\\):").matcher(decompiled).find());
        assertFalse(Pattern.compile("case Point \\w+:").matcher(decompiled).find());
        assertTrue(decompiled.contains("case null:"));
        assertTrue(Pattern.compile("switch \\([^\\n]*\\)").matcher(decompiled).find());
    }
}
