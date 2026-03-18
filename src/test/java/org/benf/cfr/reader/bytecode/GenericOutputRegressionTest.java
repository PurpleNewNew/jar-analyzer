package org.benf.cfr.reader.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenericOutputRegressionTest {
    @Test
    void shouldPreserveWildcardCastsThatAreNotImplicit(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "generic-output",
                "TestGenericSuperCast",
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompile(classFile);

        assertTrue(Pattern.compile("Class<T> t = \\(Class<T>\\)inner\\.get\\(\\);").matcher(decompiled).find());
        assertTrue(Pattern.compile("Class<\\? extends T> t = \\(Class<\\? extends T>\\)inner\\.get\\(\\);").matcher(decompiled).find());
        assertTrue(Pattern.compile("return \\(Class<T>\\)inner\\.get\\(\\);").matcher(decompiled).find());
        assertTrue(Pattern.compile("return \\(Class<\\? extends T>\\)inner\\.get\\(\\);").matcher(decompiled).find());
        assertFalse(Pattern.compile("Class<\\? super T> t = inner\\.get\\(\\);").matcher(decompiled).find());
        assertFalse(Pattern.compile("Class<T> t = inner\\.get\\(\\);").matcher(decompiled).find());
        assertFalse(Pattern.compile("Class<\\? extends T> t = inner\\.get\\(\\);").matcher(decompiled).find());
    }
}
