/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.asm;

import me.n1ar4.jar.analyzer.core.reference.MethodReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class MethodReferenceLookup {
    private static final Map<Map<MethodReference.Handle, MethodReference>, SignatureIndex> INDEX_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private MethodReferenceLookup() {
    }

    public static MethodReference resolve(Map<MethodReference.Handle, MethodReference> methodMap,
                                          MethodReference.Handle target,
                                          Integer preferredJarId) {
        if (methodMap == null || methodMap.isEmpty() || target == null || target.getClassReference() == null) {
            return null;
        }
        MethodReference direct = methodMap.get(target);
        if (direct != null) {
            return direct;
        }
        String className = target.getClassReference().getName();
        String methodName = target.getName();
        String methodDesc = target.getDesc();
        if (className == null || methodName == null || methodDesc == null) {
            return null;
        }
        return index(methodMap).resolve(className, methodName, methodDesc, preferredJarId);
    }

    public static MethodReference resolve(Map<MethodReference.Handle, MethodReference> methodMap,
                                          MethodReference.Handle target) {
        return resolve(methodMap, target, target == null ? null : target.getJarId());
    }

    public static MethodReference.Handle resolveHandle(Map<MethodReference.Handle, MethodReference> methodMap,
                                                       MethodReference.Handle target,
                                                       Integer preferredJarId) {
        MethodReference method = resolve(methodMap, target, preferredJarId);
        return method == null ? null : method.getHandle();
    }

    public static MethodReference.Handle resolveHandle(Map<MethodReference.Handle, MethodReference> methodMap,
                                                       MethodReference.Handle target) {
        MethodReference method = resolve(methodMap, target);
        return method == null ? null : method.getHandle();
    }

    public static boolean exists(Map<MethodReference.Handle, MethodReference> methodMap,
                                 MethodReference.Handle target,
                                 Integer preferredJarId) {
        return resolve(methodMap, target, preferredJarId) != null;
    }

    public static Integer resolveJarId(Map<MethodReference.Handle, MethodReference> methodMap,
                                       String className,
                                       String methodName,
                                       String methodDesc,
                                       Integer preferredJarId) {
        if (methodMap == null || methodMap.isEmpty()) {
            return null;
        }
        MethodReference method = index(methodMap).resolve(className, methodName, methodDesc, preferredJarId);
        if (method == null || method.getJarId() == null || method.getJarId() < 0) {
            return null;
        }
        return method.getJarId();
    }

    private static SignatureIndex index(Map<MethodReference.Handle, MethodReference> methodMap) {
        synchronized (INDEX_CACHE) {
            SignatureIndex cached = INDEX_CACHE.get(methodMap);
            if (cached != null && cached.sourceSize() == methodMap.size()) {
                return cached;
            }
            SignatureIndex built = SignatureIndex.build(methodMap.values());
            INDEX_CACHE.put(methodMap, built);
            return built;
        }
    }

    private static int normalizeJarId(Integer jarId) {
        if (jarId == null || jarId < 0) {
            return Integer.MAX_VALUE;
        }
        return jarId;
    }

    private static String key(String className, String methodName, String methodDesc) {
        if (className == null || methodName == null || methodDesc == null) {
            return "";
        }
        return className + '\u0001' + methodName + '\u0001' + methodDesc;
    }

    private static final class SignatureIndex {
        private static final Comparator<MethodReference> CANDIDATE_ORDER = (left, right) -> {
            int cmp = Integer.compare(normalizeJarId(left == null ? null : left.getJarId()),
                    normalizeJarId(right == null ? null : right.getJarId()));
            if (cmp != 0) {
                return cmp;
            }
            return Integer.compare(System.identityHashCode(left), System.identityHashCode(right));
        };

        private final Map<String, List<MethodReference>> bySignature;
        private final int sourceSize;

        private SignatureIndex(Map<String, List<MethodReference>> bySignature, int sourceSize) {
            this.bySignature = bySignature;
            this.sourceSize = sourceSize;
        }

        private static SignatureIndex build(Collection<MethodReference> methods) {
            Map<String, List<MethodReference>> tmp = new HashMap<>();
            int rawSize = methods == null ? 0 : methods.size();
            if (methods != null) {
                for (MethodReference method : methods) {
                    if (method == null || method.getClassReference() == null) {
                        continue;
                    }
                    String className = method.getClassReference().getName();
                    String methodName = method.getName();
                    String methodDesc = method.getDesc();
                    String key = key(className, methodName, methodDesc);
                    if (key.isEmpty()) {
                        continue;
                    }
                    tmp.computeIfAbsent(key, ignore -> new ArrayList<>()).add(method);
                }
            }
            for (Map.Entry<String, List<MethodReference>> entry : tmp.entrySet()) {
                List<MethodReference> rows = entry.getValue();
                rows.sort(CANDIDATE_ORDER);
                entry.setValue(Collections.unmodifiableList(new ArrayList<>(rows)));
            }
            return new SignatureIndex(Collections.unmodifiableMap(tmp), rawSize);
        }

        private int sourceSize() {
            return sourceSize;
        }

        private MethodReference resolve(String className,
                                        String methodName,
                                        String methodDesc,
                                        Integer preferredJarId) {
            String key = key(className, methodName, methodDesc);
            if (key.isEmpty()) {
                return null;
            }
            List<MethodReference> rows = bySignature.get(key);
            if (rows == null || rows.isEmpty()) {
                return null;
            }
            int preferred = normalizeJarId(preferredJarId);
            MethodReference best = null;
            for (MethodReference candidate : rows) {
                if (candidate == null) {
                    continue;
                }
                if (best == null) {
                    best = candidate;
                    continue;
                }
                int candidateJar = normalizeJarId(candidate.getJarId());
                int bestJar = normalizeJarId(best.getJarId());
                if (preferred != Integer.MAX_VALUE && candidateJar == preferred && bestJar != preferred) {
                    best = candidate;
                    continue;
                }
                if (candidateJar < bestJar) {
                    best = candidate;
                }
            }
            return best;
        }
    }
}
