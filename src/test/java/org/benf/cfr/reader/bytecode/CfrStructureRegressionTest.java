package org.benf.cfr.reader.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CfrStructureRegressionTest {
    @Test
    void shouldReduceDefiniteAssignmentGotoResidue(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                "TestDefiniteAssignment",
                """
                import java.io.IOException;

                public class TestDefiniteAssignment {
                  void testExample16$1$$1(int v) throws IOException {
                    int k;
                    if (v > 0 && (k = System.in.read()) >= 0)
                      System.out.println(k);
                  }

                  void testExample16$1$$2(int n) {
                    {
                      int k;
                      while (true) {
                        k = n;
                        if (k >= 5) break;
                        n = 6;
                      }
                      System.out.println(k);
                    }
                  }

                  void testExample16$1$$3modified(int n, int m) {
                    int k;
                    while (n < 4 || (k = m) < 5) {
                      k = n;
                      if (k >= 5) break;
                      n = 6;
                    }
                    System.out.println(k);
                  }

                  void testAssignments(int n, boolean bool) {
                    int a;
                    if (bool && ((a = n) > 0 || (a = -n) > 100)) {
                      System.out.println(a);
                    }

                    int b;
                    if (bool || (b = (b = n) * b) > 0) {
                      System.out.println("b");
                    } else {
                      System.out.println(b);
                    }

                    {
                      double cFake = 0.01;
                      System.out.println(cFake);
                    }

                    double c;
                    if (!((n < 1.0 - n) && (c = (n + 5)) > (c * c - c / 2)) ? n < 5.0 - (c = n) : n > c) {
                      System.out.println(c);
                      c += 2;
                    } else {
                      c += 5;
                    }
                    System.out.println(c);

                    boolean x;
                    double d;
                    if (x = ((d = n) > 0)) {
                      System.out.println(d);
                    }
                  }
                }
                """,
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompile(classFile);

        assertFalse(decompiled.contains("Unable to fully structure code"), decompiled);
        assertFalse(decompiled.contains("** GOTO"), decompiled);
        assertFalse(decompiled.contains("lbl-1000"), decompiled);
        assertFalse(decompiled.contains("block6:"), decompiled);
        assertFalse(decompiled.contains("block-1:"), decompiled);
        assertFalse(decompiled.contains("double d2;"), decompiled);
        assertFalse(decompiled.contains("double d3 = n;"), decompiled);
        assertFalse(decompiled.contains("if (x = d > 0.0)"), decompiled);
        assertFalse(decompiled.contains("if (bool) ** GOTO"), decompiled);
        assertFalse(decompiled.contains("lbl3:"), decompiled);
        assertTrue(decompiled.contains("if (!bool) {"), decompiled);
        assertTrue(decompiled.contains("} else {"), decompiled);
        assertTrue(decompiled.contains("c += 5.0;"), decompiled);
        assertTrue(decompiled.contains("if (x = (d = (double)n) > 0.0)"), decompiled);
    }

    @Test
    void shouldKeepLoopInSwitchWithLabelledBreakStructured(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                "TestSwitchLoop",
                """
                public class TestSwitchLoop {
                  public void test8(int i) {
                    switch (i) {
                      case 0:
                        label: {
                          for (int j = 0; j < 10; j++) {
                            if (j == 3) {
                              break label;
                            }
                          }

                          System.out.println(0);
                        }
                        System.out.println("after");
                      case 1:
                        System.out.println(1);
                    }

                    System.out.println("after2");
                  }
                }
                """,
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompile(classFile);

        assertFalse(decompiled.contains("Unable to fully structure code"), decompiled);
        assertFalse(decompiled.contains("** GOTO"), decompiled);
        assertTrue(decompiled.contains("switch (i)"), decompiled);
    }

    @Test
    void shouldKeepAssignedLocalBoundToFollowingIfCondition(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                "TestTail",
                """
                public class TestTail {
                  void test(int n) {
                    boolean x;
                    double d;
                    if (x = ((d = n) > 0)) {
                      System.out.println(d);
                    }
                  }
                }
                """,
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompile(classFile);

        assertFalse(decompiled.contains("double d2 = n;"), decompiled);
        assertFalse(decompiled.contains("if (x = d > 0.0)"), decompiled);
        assertTrue(decompiled.contains("if (x = (d = (double)n) > 0.0)"), decompiled);
        assertTrue(decompiled.contains("System.out.println(d);"), decompiled);
    }
}
