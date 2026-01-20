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

import me.n1ar4.jar.analyzer.core.asm.MethodCallClassVisitor;
import me.n1ar4.jar.analyzer.core.asm.ReflectionCallResolver;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class MethodCallRunner {
    private static final Logger logger = LogManager.getLogger();

    public static void start(Set<ClassFileEntity> classFileList, HashMap<MethodReference.Handle,
            HashSet<MethodReference.Handle>> methodCalls) {
        logger.info("start analyze method calls");
        for (ClassFileEntity file : classFileList) {
            try {
                byte[] bytes = file.getFile();
                if (bytes == null || bytes.length == 0) {
                    continue;
                }
                ClassReader cr = new ClassReader(bytes);
                ClassNode cn = new ClassNode();
                cr.accept(cn, Const.GlobalASMOptions);
                MethodCallClassVisitor mcv =
                        new MethodCallClassVisitor(methodCalls, AnalyzeEnv.methodCallMeta, AnalyzeEnv.methodMap);
                cn.accept(mcv);
                ReflectionCallResolver.appendReflectionEdges(
                        cn, methodCalls, AnalyzeEnv.methodMap, AnalyzeEnv.methodCallMeta, false);
            } catch (Exception e) {
                logger.warn("method call error: {}", e.toString());
            }
        }
    }
}
