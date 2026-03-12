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

import me.n1ar4.jar.analyzer.core.bytecode.BuildBytecodeWorkspace;
import me.n1ar4.jar.analyzer.core.facts.BuildFactSnapshot;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;

import java.util.Map;

public final class ReflectionConstraintAnalyzer {
    private ReflectionConstraintAnalyzer() {
    }

    public static Map<MethodReference.Handle, BuildFactSnapshot.MethodReflectionHints> collect(
            BuildBytecodeWorkspace workspace,
            Map<MethodReference.Handle, MethodReference> methodMap,
            Map<String, BuildFactSnapshot.AliasValueFact> instanceFieldFactsByKey) {
        return BytecodeMainlineReflectionResolver.collectReflectionHints(workspace, methodMap, instanceFieldFactsByKey);
    }

    public static Map<String, BuildFactSnapshot.AliasValueFact> collectInstanceFieldFacts(
            BuildBytecodeWorkspace workspace) {
        return BytecodeMainlineReflectionResolver.collectInstanceFieldFacts(workspace);
    }
}
