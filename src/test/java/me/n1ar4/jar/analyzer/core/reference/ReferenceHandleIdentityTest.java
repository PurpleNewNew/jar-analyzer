/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.reference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ReferenceHandleIdentityTest {
    @Test
    public void classHandleShouldIncludeJarIdInIdentity() {
        ClassReference.Handle left = new ClassReference.Handle("a/b/C", 1);
        ClassReference.Handle right = new ClassReference.Handle("a/b/C", 2);
        ClassReference.Handle same = new ClassReference.Handle("a/b/C", 1);

        assertNotEquals(left, right);
        assertEquals(left, same);
        assertNotEquals(left.hashCode(), right.hashCode());
    }

    @Test
    public void methodHandleShouldIncludeJarIdInIdentity() {
        MethodReference.Handle left = new MethodReference.Handle(
                new ClassReference.Handle("a/b/C", 1),
                "call",
                "()V"
        );
        MethodReference.Handle right = new MethodReference.Handle(
                new ClassReference.Handle("a/b/C", 2),
                "call",
                "()V"
        );
        MethodReference.Handle same = new MethodReference.Handle(
                new ClassReference.Handle("a/b/C", 1),
                "call",
                "()V"
        );

        assertNotEquals(left, right);
        assertEquals(left, same);
        assertNotEquals(left.hashCode(), right.hashCode());
    }
}

