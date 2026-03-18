package org.benf.cfr.reader.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class RawReceiverSemanticsRegressionTest {
    @Test
    void shouldAvoidRawReceiverCastsAcrossGenericStreamChains(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "raw-receiver",
                "RawReceiverSample",
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);

        assertFalse(decompiled.contains("((Stream)"), decompiled);
        assertFalse(decompiled.contains("Stream stream"), decompiled);
        assertFalse(decompiled.contains("(List)"), decompiled);

        CfrDecompilerRegressionSupport.compileJava(tempDir, "RawReceiverSample", decompiled, "--release", "21");
    }
}
