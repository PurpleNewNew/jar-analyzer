package org.benf.cfr.reader.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayTypeReturnResolutionRegressionTest {
    @Test
    void shouldPreserveStaticAndMemberReturnGenericsInAssignedLocals(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "type-recovery",
                "DisplayTypeReturnSample",
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);

        assertTrue(Pattern.compile("List<String> fromStatic = DisplayTypeReturnSample\\.wrap\\(\"a\"\\);").matcher(decompiled).find(), decompiled);
        assertTrue(Pattern.compile("List<String> fromMember = builder\\.buildList\\(\\);").matcher(decompiled).find(), decompiled);
        assertFalse(Pattern.compile("List fromStatic = ").matcher(decompiled).find(), decompiled);
        assertFalse(Pattern.compile("List fromMember = ").matcher(decompiled).find(), decompiled);

        CfrDecompilerRegressionSupport.compileJava(tempDir, "DisplayTypeReturnSample", decompiled, "--release", "21");
    }
}
