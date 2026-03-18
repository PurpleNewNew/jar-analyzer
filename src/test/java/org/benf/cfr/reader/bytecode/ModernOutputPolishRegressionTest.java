package org.benf.cfr.reader.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModernOutputPolishRegressionTest {
    @Test
    void shouldNotEmitIllegalStaticOnLocalEnums(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "modern-output-polish",
                "TestLocalEnum",
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompile(classFile);

        assertTrue(Pattern.compile("\\benum Type\\b").matcher(decompiled).find(), decompiled);
        assertFalse(Pattern.compile("\\bstatic enum Type\\b").matcher(decompiled).find(), decompiled);
    }

    @Test
    void shouldPreserveVarForNamelessLocalClasses(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "modern-output-polish",
                "TestAnonymousVar",
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompile(classFile);

        assertTrue(Pattern.compile("var printer = new Object\\(\\)\\s*\\{").matcher(decompiled).find(), decompiled);
        assertTrue(Pattern.compile("var printer = new TestAnonymousVar\\(\\)\\s*\\{").matcher(decompiled).find(), decompiled);
        assertTrue(Pattern.compile("TestAnonymousVar printer = new TestAnonymousVar\\(\\)\\s*\\{").matcher(decompiled).find(), decompiled);
    }
}
