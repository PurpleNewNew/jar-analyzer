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
        Path classFile = CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                "UnifiedPatternSample",
                """
                public class UnifiedPatternSample {
                    static String describe(Object o) {
                        if (o instanceof String s && s.length() > 2) {
                            return s.trim();
                        }
                        return switch (o) {
                            case Integer i when i > 10 -> "big" + i;
                            case Integer i -> "small" + i;
                            case Point(int x, int y) -> x + ":" + y;
                            default -> "other";
                        };
                    }

                    record Point(int x, int y) {}
                }
                """,
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
        Path classFile = CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                "SwitchPatternSample",
                """
                public class SwitchPatternSample {
                    static String test(Object o) {
                        return switch (o) {
                            case String s -> s;
                            case Integer i when i > 10 -> "big" + i;
                            case Integer i -> "small" + i;
                            case Point(int x, int y) -> x + "," + y;
                            case null -> "nil";
                            default -> "other";
                        };
                    }
                    record Point(int x, int y) {}
                }
                """,
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
