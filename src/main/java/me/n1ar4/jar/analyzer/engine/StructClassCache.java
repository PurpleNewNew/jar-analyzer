/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.engine;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.utils.RuntimeClassResolver;
import org.jetbrains.java.decompiler.struct.StructClass;

import java.util.LinkedHashMap;
import java.util.Map;

public final class StructClassCache {
    private static final int MAX_ENTRIES = 8192;
    private static final Map<String, StructClass> CACHE =
            new LinkedHashMap<String, StructClass>(MAX_ENTRIES, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, StructClass> eldest) {
                    return size() > MAX_ENTRIES;
                }
            };
    private static final Object LOCK = new Object();
    private static volatile long lastBuildSeq = -1L;
    private static volatile long lastRootSeq = -1L;

    private StructClassCache() {
    }

    public static StructClass get(String name) {
        if (name == null) {
            return null;
        }
        ensureFresh();
        synchronized (LOCK) {
            return CACHE.get(name);
        }
    }

    public static void put(StructClass clazz) {
        if (clazz == null || clazz.qualifiedName == null) {
            return;
        }
        ensureFresh();
        synchronized (LOCK) {
            CACHE.put(clazz.qualifiedName, clazz);
        }
    }

    private static void ensureFresh() {
        long buildSeq = DatabaseManager.getBuildSeq();
        long rootSeq = RuntimeClassResolver.getRootSeq();
        if (buildSeq == lastBuildSeq && rootSeq == lastRootSeq) {
            return;
        }
        synchronized (LOCK) {
            if (buildSeq == lastBuildSeq && rootSeq == lastRootSeq) {
                return;
            }
            CACHE.clear();
            lastBuildSeq = buildSeq;
            lastRootSeq = rootSeq;
        }
    }
}
