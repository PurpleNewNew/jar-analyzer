package org.benf.cfr.reader.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TernaryTypeMergeRegressionTest {
    @Test
    void shouldPreserveGenericBranchTypesInsideTernaryExpressions(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "type-recovery",
                "TernaryTypeMergeSample",
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);

        assertTrue(Pattern.compile("return flag \\? new ArrayList<String>\\(\\) : new LinkedList<String>\\(\\);").matcher(decompiled).find(), decompiled);
        assertTrue(Pattern.compile("return flag \\? new ArrayList<String>\\(input\\) : new LinkedList<String>\\(input\\);").matcher(decompiled).find(), decompiled);
        assertFalse(Pattern.compile("new ArrayList\\(\\) : new LinkedList\\(\\)").matcher(decompiled).find(), decompiled);

        CfrDecompilerRegressionSupport.compileJava(tempDir, "TernaryTypeMergeSample", decompiled, "--release", "21");
    }
}
