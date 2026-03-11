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

import me.n1ar4.jar.analyzer.core.reference.ClassReference;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class InheritanceMap {
    private final Map<ClassReference.Handle, Set<ClassReference.Handle>> directParentsByChild;
    private final Map<ClassReference.Handle, Set<ClassReference.Handle>> allParentsByChild;
    private final Map<ClassReference.Handle, Set<ClassReference.Handle>> allChildrenByParent;
    private final Map<String, List<ClassReference.Handle>> handlesByName;

    private InheritanceMap(Map<ClassReference.Handle, Set<ClassReference.Handle>> directParentsByChild,
                           Map<ClassReference.Handle, Set<ClassReference.Handle>> allParentsByChild,
                           Map<ClassReference.Handle, Set<ClassReference.Handle>> allChildrenByParent,
                           Map<String, List<ClassReference.Handle>> handlesByName) {
        this.directParentsByChild = freezeSetMap(directParentsByChild);
        this.allParentsByChild = freezeSetMap(allParentsByChild);
        this.allChildrenByParent = freezeSetMap(allChildrenByParent);
        this.handlesByName = freezeListMap(handlesByName);
    }

    public static InheritanceMap fromClasses(Map<ClassReference.Handle, ClassReference> classMap) {
        if (classMap == null || classMap.isEmpty()) {
            return new InheritanceMap(Map.of(), Map.of(), Map.of(), Map.of());
        }
        ClassLookup lookup = new ClassLookup(classMap.values());
        Map<ClassReference.Handle, Set<ClassReference.Handle>> directParents = new LinkedHashMap<>();
        for (ClassReference clazz : classMap.values()) {
            if (clazz == null) {
                continue;
            }
            ClassReference.Handle child = clazz.getHandle();
            if (child == null) {
                continue;
            }
            LinkedHashSet<ClassReference.Handle> parents = new LinkedHashSet<>();
            addResolvedParent(parents, clazz.getSuperClass(), child.getJarId(), lookup);
            List<String> interfaces = clazz.getInterfaces();
            if (interfaces != null && !interfaces.isEmpty()) {
                for (String iface : interfaces) {
                    addResolvedParent(parents, iface, child.getJarId(), lookup);
                }
            }
            if (!parents.isEmpty()) {
                directParents.put(child, parents);
            }
        }

        Map<ClassReference.Handle, Set<ClassReference.Handle>> allParents = new LinkedHashMap<>();
        for (ClassReference.Handle handle : classMap.keySet()) {
            collectAllParents(handle, directParents, allParents);
        }
        Map<ClassReference.Handle, Set<ClassReference.Handle>> allChildren = new LinkedHashMap<>();
        for (Map.Entry<ClassReference.Handle, Set<ClassReference.Handle>> entry : allParents.entrySet()) {
            ClassReference.Handle child = entry.getKey();
            if (child == null) {
                continue;
            }
            for (ClassReference.Handle parent : entry.getValue()) {
                if (parent == null) {
                    continue;
                }
                allChildren.computeIfAbsent(parent, ignore -> new LinkedHashSet<>()).add(child);
            }
        }
        return new InheritanceMap(directParents, allParents, allChildren, lookup.handlesByName());
    }

    public Set<ClassReference.Handle> getDirectParents(ClassReference.Handle clazz) {
        Set<ClassReference.Handle> parents = directParentsByChild.get(clazz);
        return parents == null ? Collections.emptySet() : parents;
    }

    public Set<ClassReference.Handle> getSuperClasses(ClassReference.Handle clazz) {
        Set<ClassReference.Handle> parents = allParentsByChild.get(clazz);
        return parents == null ? Collections.emptySet() : parents;
    }

    public Set<ClassReference.Handle> getSubClasses(ClassReference.Handle clazz) {
        Set<ClassReference.Handle> children = allChildrenByParent.get(clazz);
        return children == null ? Collections.emptySet() : children;
    }

    public boolean isSubclassOf(ClassReference.Handle clazz, ClassReference.Handle superClass) {
        if (clazz == null || superClass == null) {
            return false;
        }
        if (clazz.equals(superClass)) {
            return true;
        }
        return getSuperClasses(clazz).contains(superClass);
    }

    public ClassReference.Handle resolveClass(String className, Integer preferredJarId) {
        String normalized = normalizeClassName(className);
        if (normalized.isEmpty()) {
            return null;
        }
        List<ClassReference.Handle> candidates = handlesByName.get(normalized);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        int normalizedJarId = normalizeJarId(preferredJarId);
        if (normalizedJarId >= 0) {
            for (ClassReference.Handle handle : candidates) {
                if (handle != null && normalizedJarId == normalizeJarId(handle.getJarId())) {
                    return handle;
                }
            }
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        for (ClassReference.Handle handle : candidates) {
            if (handle != null && normalizeJarId(handle.getJarId()) < 0) {
                return handle;
            }
        }
        return null;
    }

    private static Set<ClassReference.Handle> collectAllParents(
            ClassReference.Handle child,
            Map<ClassReference.Handle, Set<ClassReference.Handle>> directParents,
            Map<ClassReference.Handle, Set<ClassReference.Handle>> memo) {
        Set<ClassReference.Handle> cached = memo.get(child);
        if (cached != null) {
            return cached;
        }
        LinkedHashSet<ClassReference.Handle> all = new LinkedHashSet<>();
        Set<ClassReference.Handle> direct = directParents.get(child);
        if (direct != null && !direct.isEmpty()) {
            Deque<ClassReference.Handle> queue = new ArrayDeque<>(direct);
            while (!queue.isEmpty()) {
                ClassReference.Handle parent = queue.removeFirst();
                if (parent == null || !all.add(parent)) {
                    continue;
                }
                Set<ClassReference.Handle> parentParents = directParents.get(parent);
                if (parentParents != null && !parentParents.isEmpty()) {
                    queue.addAll(parentParents);
                }
            }
        }
        Set<ClassReference.Handle> frozen = all.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<>(all));
        memo.put(child, frozen);
        return frozen;
    }

    private static void addResolvedParent(Set<ClassReference.Handle> parents,
                                          String className,
                                          Integer preferredJarId,
                                          ClassLookup lookup) {
        if (parents == null || lookup == null) {
            return;
        }
        ClassReference.Handle parent = lookup.resolve(className, preferredJarId);
        if (parent != null) {
            parents.add(parent);
        }
    }

    private static Map<ClassReference.Handle, Set<ClassReference.Handle>> freezeSetMap(
            Map<ClassReference.Handle, Set<ClassReference.Handle>> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<ClassReference.Handle, Set<ClassReference.Handle>> out = new LinkedHashMap<>();
        for (Map.Entry<ClassReference.Handle, Set<ClassReference.Handle>> entry : input.entrySet()) {
            if (entry == null || entry.getKey() == null) {
                continue;
            }
            Set<ClassReference.Handle> value = entry.getValue();
            if (value == null || value.isEmpty()) {
                continue;
            }
            out.put(entry.getKey(), Collections.unmodifiableSet(new LinkedHashSet<>(value)));
        }
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    private static Map<String, List<ClassReference.Handle>> freezeListMap(
            Map<String, List<ClassReference.Handle>> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, List<ClassReference.Handle>> out = new LinkedHashMap<>();
        for (Map.Entry<String, List<ClassReference.Handle>> entry : input.entrySet()) {
            if (entry == null) {
                continue;
            }
            String key = normalizeClassName(entry.getKey());
            List<ClassReference.Handle> value = entry.getValue();
            if (key.isEmpty() || value == null || value.isEmpty()) {
                continue;
            }
            out.put(key, List.copyOf(value));
        }
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    private static String normalizeClassName(String className) {
        if (className == null) {
            return "";
        }
        String normalized = className.trim();
        if (normalized.isEmpty() || normalized.startsWith("[")) {
            return "";
        }
        if (normalized.startsWith("L") && normalized.endsWith(";")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (normalized.indexOf('.') >= 0) {
            normalized = normalized.replace('.', '/');
        }
        return normalized;
    }

    private static int normalizeJarId(Integer jarId) {
        if (jarId == null || jarId < 0) {
            return -1;
        }
        return jarId;
    }

    private static final class ClassLookup {
        private final Map<String, List<ClassReference.Handle>> handlesByName = new LinkedHashMap<>();

        private ClassLookup(Collection<ClassReference> classes) {
            if (classes == null || classes.isEmpty()) {
                return;
            }
            for (ClassReference clazz : classes) {
                if (clazz == null) {
                    continue;
                }
                ClassReference.Handle handle = clazz.getHandle();
                String name = normalizeClassName(handle == null ? null : handle.getName());
                if (name.isEmpty() || handle == null) {
                    continue;
                }
                handlesByName.computeIfAbsent(name, ignore -> new ArrayList<>()).add(handle);
            }
        }

        private Map<String, List<ClassReference.Handle>> handlesByName() {
            return handlesByName;
        }

        private ClassReference.Handle resolve(String className, Integer preferredJarId) {
            String normalized = normalizeClassName(className);
            if (normalized.isEmpty()) {
                return null;
            }
            List<ClassReference.Handle> candidates = handlesByName.get(normalized);
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }
            int normalizedJarId = normalizeJarId(preferredJarId);
            if (normalizedJarId >= 0) {
                for (ClassReference.Handle handle : candidates) {
                    if (handle != null && normalizedJarId == normalizeJarId(handle.getJarId())) {
                        return handle;
                    }
                }
            }
            if (candidates.size() == 1) {
                return candidates.get(0);
            }
            for (ClassReference.Handle handle : candidates) {
                if (handle != null && normalizeJarId(handle.getJarId()) < 0) {
                    return handle;
                }
            }
            return null;
        }
    }
}
