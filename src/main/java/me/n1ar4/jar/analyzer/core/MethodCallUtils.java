/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import org.objectweb.asm.Opcodes;

import java.util.HashSet;

public final class MethodCallUtils {
    private MethodCallUtils() {
    }

    public static boolean addCallee(HashSet<MethodReference.Handle> callees,
                                    MethodReference.Handle callee) {
        if (callees == null || callee == null) {
            return false;
        }
        if (callees.add(callee)) {
            return true;
        }
        MethodReference.Handle existing = null;
        for (MethodReference.Handle handle : callees) {
            if (handle != null && handle.equals(callee)) {
                existing = handle;
                break;
            }
        }
        if (existing == null) {
            return false;
        }
        int newRank = opcodeRank(callee.getOpcode());
        int oldRank = opcodeRank(existing.getOpcode());
        if (newRank > oldRank) {
            callees.remove(existing);
            callees.add(callee);
            return true;
        }
        return false;
    }

    private static int opcodeRank(Integer opcode) {
        if (opcode == null || opcode < 0) {
            return 0;
        }
        if (opcode == Opcodes.INVOKEDYNAMIC) {
            return 1;
        }
        if (opcode == Opcodes.INVOKEVIRTUAL
                || opcode == Opcodes.INVOKEINTERFACE
                || opcode == Opcodes.INVOKESPECIAL
                || opcode == Opcodes.INVOKESTATIC) {
            return 2;
        }
        return 1;
    }
}
