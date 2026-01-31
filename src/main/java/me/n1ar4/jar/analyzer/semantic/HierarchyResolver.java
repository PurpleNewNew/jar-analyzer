/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.semantic;

import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.entity.ClassResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HierarchyResolver {
    private final CoreEngine engine;
    private final Map<String, Boolean> subtypeCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> distanceCache = new ConcurrentHashMap<>();

    public HierarchyResolver(CoreEngine engine) {
        this.engine = engine;
    }

    public boolean isSubtype(String child, String parent) {
        if (child == null || parent == null) {
            return false;
        }
        if (child.equals(parent)) {
            return true;
        }
        if ("java/lang/Object".equals(parent)) {
            return true;
        }
        String key = child + "->" + parent;
        Boolean cached = subtypeCache.get(key);
        if (cached != null) {
            return cached;
        }
        boolean result = isSubtypeInternal(child, parent);
        subtypeCache.put(key, result);
        return result;
    }

    public int distance(String child, String parent) {
        if (child == null || parent == null) {
            return Integer.MAX_VALUE;
        }
        if (child.equals(parent)) {
            return 0;
        }
        String key = child + "=>" + parent;
        Integer cached = distanceCache.get(key);
        if (cached != null) {
            return cached;
        }
        int result = distanceInternal(child, parent);
        distanceCache.put(key, result);
        return result;
    }

    private boolean isSubtypeInternal(String child, String parent) {
        if (engine == null) {
            return false;
        }
        int depthLimit = 20;
        List<String> queue = new ArrayList<>();
        queue.add(child);
        int idx = 0;
        while (idx < queue.size() && depthLimit-- > 0) {
            String cur = queue.get(idx++);
            if (cur == null || cur.trim().isEmpty()) {
                continue;
            }
            if (parent.equals(cur)) {
                return true;
            }
            ClassResult result = engine.getClassByClass(cur);
            if (result != null && result.getSuperClassName() != null
                    && !result.getSuperClassName().trim().isEmpty()) {
                String sup = result.getSuperClassName();
                if (!queue.contains(sup)) {
                    queue.add(sup);
                }
            }
            List<String> interfaces = engine.getInterfacesByClass(cur);
            if (interfaces != null) {
                for (String itf : interfaces) {
                    if (itf == null || itf.trim().isEmpty()) {
                        continue;
                    }
                    if (!queue.contains(itf)) {
                        queue.add(itf);
                    }
                }
            }
        }
        return false;
    }

    private int distanceInternal(String child, String parent) {
        if (engine == null) {
            return Integer.MAX_VALUE;
        }
        List<String> queue = new ArrayList<>();
        List<Integer> depth = new ArrayList<>();
        queue.add(child);
        depth.add(0);
        int idx = 0;
        while (idx < queue.size()) {
            String cur = queue.get(idx);
            int curDepth = depth.get(idx);
            idx++;
            if (cur == null) {
                continue;
            }
            if (cur.equals(parent)) {
                return curDepth;
            }
            if (curDepth > 20) {
                continue;
            }
            ClassResult result = engine.getClassByClass(cur);
            if (result != null && result.getSuperClassName() != null
                    && !result.getSuperClassName().trim().isEmpty()) {
                String sup = result.getSuperClassName();
                if (!queue.contains(sup)) {
                    queue.add(sup);
                    depth.add(curDepth + 1);
                }
            }
            List<String> interfaces = engine.getInterfacesByClass(cur);
            if (interfaces != null) {
                for (String itf : interfaces) {
                    if (itf == null || itf.trim().isEmpty()) {
                        continue;
                    }
                    if (!queue.contains(itf)) {
                        queue.add(itf);
                        depth.add(curDepth + 1);
                    }
                }
            }
        }
        return Integer.MAX_VALUE;
    }
}
