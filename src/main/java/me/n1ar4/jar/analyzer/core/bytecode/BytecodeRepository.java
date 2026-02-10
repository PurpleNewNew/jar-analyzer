/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.bytecode;

import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.utils.BytecodeCache;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.nio.file.Paths;

/**
 * A thin abstraction around class-byte retrieval with caching.
 * <p>
 * This keeps byte loading/ASM parsing consistent across build and analysis paths.
 */
public interface BytecodeRepository {
    byte[] getBytes(String internalClassName);

    default byte[] getBytes(ClassReference.Handle handle) {
        if (handle == null) {
            return null;
        }
        return getBytes(handle.getName());
    }

    default ClassReader getClassReader(String internalClassName) {
        byte[] bytes = getBytes(internalClassName);
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return new ClassReader(bytes);
    }

    default ClassNode getClassNode(String internalClassName, int options) {
        ClassReader cr = getClassReader(internalClassName);
        if (cr == null) {
            return null;
        }
        ClassNode cn = new ClassNode();
        cr.accept(cn, options);
        return cn;
    }

    static BytecodeRepository current() {
        return new EngineBytecodeRepository();
    }

    final class EngineBytecodeRepository implements BytecodeRepository {
        @Override
        public byte[] getBytes(String internalClassName) {
            if (internalClassName == null || internalClassName.trim().isEmpty()) {
                return null;
            }
            CoreEngine engine = EngineContext.getEngine();
            if (engine == null) {
                return null;
            }
            String absPath = engine.getAbsPath(internalClassName);
            if (absPath == null || absPath.trim().isEmpty()) {
                return null;
            }
            return BytecodeCache.read(Paths.get(absPath));
        }
    }
}

