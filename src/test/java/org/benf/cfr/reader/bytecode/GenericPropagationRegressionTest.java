package org.benf.cfr.reader.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class GenericPropagationRegressionTest {
    @Test
    void shouldKeepIteratorAndEntryTypesBoundAcrossLoopAndCollectionViews(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "generic-propagation",
                "GenericPropagationSample",
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);

        assertFalse(decompiled.contains("Iterator<Object>"), decompiled);
        assertFalse(decompiled.contains("Iterator iterator"), decompiled);
        assertFalse(decompiled.contains("Map.Entry entry"), decompiled);
        assertFalse(decompiled.contains("List keys ="), decompiled);

        CfrDecompilerRegressionSupport.compileJava(tempDir, "GenericPropagationSample", decompiled, "--release", "21");
        CfrDecompilerRegressionSupport.assertMethodGenericSignaturesEquivalent(
                tempDir,
                "generic-propagation",
                "GenericPropagationSample",
                decompiled,
                "withoutNulls",
                "keys"
        );
    }
}
