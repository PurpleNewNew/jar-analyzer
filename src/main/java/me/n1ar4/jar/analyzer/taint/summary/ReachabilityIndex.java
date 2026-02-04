/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.taint.summary;

import me.n1ar4.jar.analyzer.core.reference.MethodReference;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ReachabilityIndex {
    private final Set<MethodReference.Handle> reachableToSink;
    private final Set<MethodReference.Handle> reachableFromSource;

    public ReachabilityIndex(Set<MethodReference.Handle> reachableToSink,
                             Set<MethodReference.Handle> reachableFromSource) {
        this.reachableToSink = reachableToSink == null ? new HashSet<>() : new HashSet<>(reachableToSink);
        this.reachableFromSource = reachableFromSource == null ? new HashSet<>() : new HashSet<>(reachableFromSource);
    }

    public boolean isReachableToSink(MethodReference.Handle handle) {
        return handle != null && reachableToSink.contains(handle);
    }

    public boolean isReachableFromSource(MethodReference.Handle handle) {
        return handle != null && reachableFromSource.contains(handle);
    }

    public Set<MethodReference.Handle> getReachableToSink() {
        return Collections.unmodifiableSet(reachableToSink);
    }

    public Set<MethodReference.Handle> getReachableFromSource() {
        return Collections.unmodifiableSet(reachableFromSource);
    }
}
