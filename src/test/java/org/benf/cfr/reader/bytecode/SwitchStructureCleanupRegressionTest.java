package org.benf.cfr.reader.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwitchStructureCleanupRegressionTest {
    @Test
    void shouldCleanupEmptyIfResidueAfterSwitchExpressionRewrite(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                "InlineSwitchExpressionSample",
                """
                public class InlineSwitchExpressionSample {
                  public void test(int i) {
                    int j = 0;
                    while (j < i) {
                      j++;
                      i = switch (j) {
                        case 1 -> 3;
                        default -> {
                          label3:
                          if (j == 4) {
                            break label3;
                          }
                          yield 2;
                        }
                      };
                    }
                  }
                }
                """,
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompile(classFile);

        assertTrue(decompiled.contains("switch (++j)"));
        assertFalse(decompiled.contains("label3:"));
        assertFalse(decompiled.contains("empty if block"));
        assertFalse(decompiled.contains("Unable to fully structure code"));
    }
}
