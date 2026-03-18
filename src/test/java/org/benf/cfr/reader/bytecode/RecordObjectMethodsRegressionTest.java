package org.benf.cfr.reader.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordObjectMethodsRegressionTest {
    @Test
    void shouldResugarSimpleRecordsWithoutObjectMethodsBootstrap(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "record-object-methods",
                "SimpleRecordSample",
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);

        assertTrue(decompiled.contains("public record SimpleRecordSample(String name, int age)"), decompiled);
        assertFalse(decompiled.contains("ObjectMethods.bootstrap"), decompiled);
        assertFalse(decompiled.contains("public final String toString()"), decompiled);
        assertFalse(decompiled.contains("public final int hashCode()"), decompiled);
        assertFalse(decompiled.contains("public final boolean equals(Object object)"), decompiled);

        CfrDecompilerRegressionSupport.compileJava(tempDir, "SimpleRecordSample", decompiled, "--release", "21");
    }

    @Test
    void shouldKeepCanonicalValidationWithoutSyntheticRecordAssignments(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "record-object-methods",
                "ValidatedRecordSample",
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);

        assertTrue(decompiled.contains("public record ValidatedRecordSample(String name, int age)"), decompiled);
        assertTrue(decompiled.contains("throw new IllegalArgumentException(\"name\");"), decompiled);
        assertFalse(decompiled.contains("ObjectMethods.bootstrap"), decompiled);
        assertFalse(decompiled.contains("this.name = name;"), decompiled);
        assertFalse(decompiled.contains("this.age = age;"), decompiled);

        CfrDecompilerRegressionSupport.compileJava(tempDir, "ValidatedRecordSample", decompiled, "--release", "21");
    }
}
