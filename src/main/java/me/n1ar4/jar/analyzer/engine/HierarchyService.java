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

import me.n1ar4.jar.analyzer.core.BuildSeqUtil;
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.utils.InterruptUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Resolve type hierarchy from the built database.
 * <p>
 * This avoids relying on {@code AnalyzeEnv} (build-time in-memory state),
 * making DFS/Taint analysis re-entrant and stable across multiple jobs.
 */
public final class HierarchyService {
    private static final Logger logger = LogManager.getLogger();
    private static final String CACHE_MAX_PROP = "jar.analyzer.hierarchy.cache.max";
    private static final int DEFAULT_CACHE_MAX = 8192;
    private static final int MIN_CACHE_MAX = 256;
    private static final int MAX_CACHE_MAX = 65536;
    private static volatile int cacheMax = resolveCacheMax();

    private static final Object EPOCH_LOCK = new Object();
    private static final AtomicLong LAST_BUILD_SEQ = new AtomicLong(-1);

    private static final Object CACHE_LOCK = new Object();
    private static final LinkedHashMap<String, Set<String>> SUPER_CACHE =
            new LinkedHashMap<String, Set<String>>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Set<String>> eldest) {
                    return size() > cacheMax;
                }
            };

    private HierarchyService() {
    }

    public static Set<String> getSuperTypes(String className) {
        String normalized = normalize(className);
        if (normalized == null) {
            return Collections.emptySet();
        }
        ensureFresh();
        Set<String> cached;
        synchronized (CACHE_LOCK) {
            cached = SUPER_CACHE.get(normalized);
        }
        if (cached != null) {
            return cached;
        }
        Set<String> computed = computeSuperTypes(normalized);
        Set<String> frozen = computed.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(computed);
        synchronized (CACHE_LOCK) {
            Set<String> prev = SUPER_CACHE.get(normalized);
            if (prev != null) {
                return prev;
            }
            SUPER_CACHE.put(normalized, frozen);
            return frozen;
        }
    }

    public static boolean isSubclassOf(String clazz, String superClass) {
        String a = normalize(clazz);
        String b = normalize(superClass);
        if (a == null || b == null) {
            return false;
        }
        if (a.equals(b)) {
            return true;
        }
        return getSuperTypes(a).contains(b);
    }

    private static void ensureFresh() {
        BuildSeqUtil.ensureFresh(LAST_BUILD_SEQ, EPOCH_LOCK, () -> {
            synchronized (CACHE_LOCK) {
                SUPER_CACHE.clear();
            }
        });
    }

    private static Set<String> computeSuperTypes(String root) {
        CoreEngine engine = EngineContext.getEngine();
        if (engine == null) {
            return Collections.emptySet();
        }
        Set<String> out = new LinkedHashSet<>();
        Set<String> visited = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        visited.add(root);
        stack.push(root);
        while (!stack.isEmpty()) {
            String current = stack.pop();
            if (current == null || current.isEmpty()) {
                continue;
            }
            try {
                ClassResult clazz = engine.getClassByClass(current);
                if (clazz != null) {
                    String superClass = normalize(clazz.getSuperClassName());
                    if (superClass != null && out.add(superClass) && visited.add(superClass)) {
                        stack.push(superClass);
                    }
                }
                List<String> interfaces = engine.getInterfacesByClass(current);
                if (interfaces != null && !interfaces.isEmpty()) {
                    for (String iface : interfaces) {
                        String normalized = normalize(iface);
                        if (normalized == null) {
                            continue;
                        }
                        if (out.add(normalized) && visited.add(normalized)) {
                            stack.push(normalized);
                        }
                    }
                }
            } catch (Exception ex) {
                InterruptUtil.restoreInterruptIfNeeded(ex);
                logger.debug("resolve hierarchy failed for {}: {}", current, ex.toString());
            }
        }
        out.remove(root);
        return out;
    }

    private static int resolveCacheMax() {
        String raw = System.getProperty(CACHE_MAX_PROP);
        if (raw != null && !raw.trim().isEmpty()) {
            try {
                int val = Integer.parseInt(raw.trim());
                if (val > 0) {
                    return Math.max(MIN_CACHE_MAX, Math.min(MAX_CACHE_MAX, val));
                }
            } catch (NumberFormatException ex) {
                logger.debug("invalid hierarchy cache max: {}", raw);
            }
        }
        return DEFAULT_CACHE_MAX;
    }

    private static String normalize(String name) {
        if (name == null) {
            return null;
        }
        String v = name.trim();
        if (v.isEmpty()) {
            return null;
        }
        // Array types are not handled (they are not normal internal names).
        if (v.startsWith("[")) {
            return null;
        }
        if (v.startsWith("L") && v.endsWith(";") && v.length() > 2) {
            v = v.substring(1, v.length() - 1);
        }
        return v.replace('.', '/');
    }
}
