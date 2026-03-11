/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.edge;

import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.build.BuildContext;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BuildEdgeAccumulator {
    private final HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls;
    private final Map<MethodCallKey, MethodCallMeta> methodCallMeta;

    private BuildEdgeAccumulator(HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
                                 Map<MethodCallKey, MethodCallMeta> methodCallMeta) {
        this.methodCalls = methodCalls == null ? new HashMap<>() : methodCalls;
        this.methodCallMeta = methodCallMeta == null ? new LinkedHashMap<>() : methodCallMeta;
    }

    public static BuildEdgeAccumulator empty() {
        return new BuildEdgeAccumulator(new HashMap<>(), new LinkedHashMap<>());
    }

    public static BuildEdgeAccumulator fromContext(BuildContext context) {
        if (context == null) {
            return empty();
        }
        HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls = new HashMap<>();
        for (Map.Entry<MethodReference.Handle, HashSet<MethodReference.Handle>> entry : context.methodCalls.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            methodCalls.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return new BuildEdgeAccumulator(methodCalls, new LinkedHashMap<>(context.methodCallMeta));
    }

    public HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls() {
        return methodCalls;
    }

    public Map<MethodCallKey, MethodCallMeta> methodCallMeta() {
        return methodCallMeta;
    }

    public void copyInto(BuildContext context) {
        if (context == null) {
            return;
        }
        context.methodCalls.clear();
        context.methodCallMeta.clear();
        for (Map.Entry<MethodReference.Handle, HashSet<MethodReference.Handle>> entry : methodCalls.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            context.methodCalls.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        context.methodCallMeta.putAll(methodCallMeta);
    }
}
