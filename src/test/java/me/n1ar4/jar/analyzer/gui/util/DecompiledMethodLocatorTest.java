/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DecompiledMethodLocatorTest {

    @Test
    void overloadMethod_shouldJumpToCorrectDeclarationName() {
        String code = ""
                + "public class A {\n"
                + "    public void foo(int x) {\n"
                + "        System.out.println(x);\n"
                + "    }\n"
                + "\n"
                + "    public void foo(String s) {\n"
                + "        System.out.println(s);\n"
                + "    }\n"
                + "}\n";

        // Hint line inside the second overload body ("println(s);").
        DecompiledMethodLocator.RangeHint hint = new DecompiledMethodLocator.RangeHint(7, 7);
        DecompiledMethodLocator.JumpTarget target =
                DecompiledMethodLocator.locate(code, "A", "foo", "(Ljava/lang/String;)V", hint);

        assertNotNull(target);
        assertEquals(DecompiledMethodLocator.Confidence.AST_NAME, target.confidence);

        int expected = code.indexOf("foo(String");
        assertTrue(expected >= 0);
        assertEquals(expected, target.startOffset);
        assertEquals("foo", code.substring(target.startOffset, target.endOffset));
    }

    @Test
    void overloadConstructor_shouldJumpToCorrectDeclarationName() {
        String code = ""
                + "public class B {\n"
                + "    public B() {\n"
                + "    }\n"
                + "\n"
                + "    public B(int x) {\n"
                + "        this();\n"
                + "    }\n"
                + "}\n";

        // Hint line inside the second constructor body ("this();").
        DecompiledMethodLocator.RangeHint hint = new DecompiledMethodLocator.RangeHint(6, 6);
        DecompiledMethodLocator.JumpTarget target =
                DecompiledMethodLocator.locate(code, "B", "<init>", "(I)V", hint);

        assertNotNull(target);
        assertEquals(DecompiledMethodLocator.Confidence.AST_NAME, target.confidence);

        int expected = code.indexOf("B(int");
        assertTrue(expected >= 0);
        assertEquals(expected, target.startOffset);
        assertEquals("B", code.substring(target.startOffset, target.endOffset));
    }

    @Test
    void staticInitializer_shouldJumpToStaticKeyword() {
        String code = ""
                + "public class C {\n"
                + "    static {\n"
                + "        int x = 1;\n"
                + "    }\n"
                + "}\n";

        DecompiledMethodLocator.RangeHint hint = new DecompiledMethodLocator.RangeHint(3, 3);
        DecompiledMethodLocator.JumpTarget target =
                DecompiledMethodLocator.locate(code, "C", "<clinit>", "()V", hint);

        assertNotNull(target);
        assertEquals(DecompiledMethodLocator.Confidence.AST_NAME, target.confidence);

        int expected = code.indexOf("static {");
        assertTrue(expected >= 0);
        assertEquals(expected, target.startOffset);
        assertEquals("static", code.substring(target.startOffset, target.endOffset));
    }

    @Test
    void parseFail_shouldUseLocalTextSignatureWhenHintExists() {
        // Invalid Java on purpose (anonymous class-like name), to force JavaParser parse failure.
        String code = ""
                + "class 1 implements Runnable {\n"
                + "    public void run() {\n"
                + "        System.out.println(\"x\");\n"
                + "    }\n"
                + "}\n";

        DecompiledMethodLocator.RangeHint hint = new DecompiledMethodLocator.RangeHint(3, 3);
        DecompiledMethodLocator.JumpTarget target =
                DecompiledMethodLocator.locate(code, "pkg/Outer$1", "run", "()V", hint);

        assertNotNull(target);
        assertEquals(DecompiledMethodLocator.Confidence.TEXT_SIGNATURE, target.confidence);

        int expected = code.indexOf("run()");
        assertTrue(expected >= 0);
        assertEquals(expected, target.startOffset);
        assertEquals("run", code.substring(target.startOffset, target.endOffset));
    }

    @Test
    void parseFail_shouldFallbackToMappingLineWhenSignatureNotFound() {
        String code = ""
                + "class 1 implements Runnable {\n"
                + "    public void run() {\n"
                + "        System.out.println(\"x\");\n"
                + "    }\n"
                + "}\n";

        // Ask for a non-existing symbol, but hint exists.
        DecompiledMethodLocator.RangeHint hint = new DecompiledMethodLocator.RangeHint(3, 3);
        DecompiledMethodLocator.JumpTarget target =
                DecompiledMethodLocator.locate(code, "pkg/Outer$1", "missing", "()V", hint);

        assertNotNull(target);
        assertEquals(DecompiledMethodLocator.Confidence.MAPPING_LINE, target.confidence);

        int expected = code.indexOf("        System.out.println");
        assertTrue(expected >= 0);
        assertEquals(expected, target.startOffset);
    }
}

