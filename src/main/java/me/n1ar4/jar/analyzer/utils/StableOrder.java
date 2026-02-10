/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.utils;

import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.entity.AnnoMethodResult;
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.entity.MethodCallResult;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;
import me.n1ar4.jar.analyzer.taint.TaintResult;

import java.util.Comparator;
import java.util.List;

/**
 * Central place for stable ordering of results shown via API/GUI/exporters.
 * This reduces ordering drift from DB iteration order and concurrent traversal.
 */
public final class StableOrder {
    private StableOrder() {
    }

    public static final Comparator<ClassResult> CLASS_RESULT =
            Comparator.comparingInt((ClassResult r) -> r == null ? Integer.MAX_VALUE : r.getJarId())
                    .thenComparing(r -> n(r == null ? null : r.getClassName()))
                    .thenComparing(r -> n(r == null ? null : r.getJarName()))
                    .thenComparing(r -> n(r == null ? null : r.getSuperClassName()))
                    .thenComparingInt(r -> r == null ? Integer.MAX_VALUE : r.getIsInterfaceInt());

    public static final Comparator<MethodResult> METHOD_RESULT =
            Comparator.comparingInt((MethodResult r) -> r == null ? Integer.MAX_VALUE : r.getJarId())
                    .thenComparing(r -> n(r == null ? null : r.getClassName()))
                    .thenComparing(r -> n(r == null ? null : r.getMethodName()))
                    .thenComparing(r -> n(r == null ? null : r.getMethodDesc()))
                    .thenComparing(r -> n(r == null ? null : r.getActualPath()))
                    .thenComparing(r -> n(r == null ? null : r.getRestfulType()))
                    .thenComparingInt(r -> r == null ? Integer.MAX_VALUE : r.getLineNumber());

    public static final Comparator<ResourceEntity> RESOURCE_ENTITY =
            Comparator.comparingInt((ResourceEntity r) -> r == null || r.getJarId() == null
                            ? Integer.MAX_VALUE
                            : r.getJarId())
                    .thenComparing(r -> n(r == null ? null : r.getResourcePath()), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(r -> n(r == null ? null : r.getPathStr()), String.CASE_INSENSITIVE_ORDER)
                    .thenComparingLong(r -> r == null ? Long.MAX_VALUE : r.getFileSize())
                    .thenComparingInt(r -> r == null ? Integer.MAX_VALUE : r.getRid());

    public static final Comparator<AnnoMethodResult> ANNO_METHOD_RESULT =
            Comparator.comparingInt((AnnoMethodResult r) -> r == null || r.getJarId() == null
                            ? Integer.MAX_VALUE
                            : r.getJarId())
                    .thenComparing(r -> n(r == null ? null : r.getClassName()))
                    .thenComparing(r -> n(r == null ? null : r.getMethodName()))
                    .thenComparing(r -> n(r == null ? null : r.getMethodDesc()))
                    .thenComparingInt(r -> r == null ? Integer.MAX_VALUE : r.getLineNumber())
                    .thenComparing(r -> n(r == null ? null : r.getAnnoName()))
                    .thenComparing(r -> n(r == null ? null : r.getAnnoScope()));

    public static final Comparator<MethodCallResult> METHOD_CALL_RESULT =
            Comparator.comparingInt((MethodCallResult r) -> r == null || r.getCallerJarId() == null
                            ? Integer.MAX_VALUE
                            : r.getCallerJarId())
                    .thenComparing(r -> n(r == null ? null : r.getCallerClassName()))
                    .thenComparing(r -> n(r == null ? null : r.getCallerMethodName()))
                    .thenComparing(r -> n(r == null ? null : r.getCallerMethodDesc()))
                    .thenComparingInt(r -> r == null || r.getCalleeJarId() == null ? Integer.MAX_VALUE : r.getCalleeJarId())
                    .thenComparing(r -> n(r == null ? null : r.getCalleeClassName()))
                    .thenComparing(r -> n(r == null ? null : r.getCalleeMethodName()))
                    .thenComparing(r -> n(r == null ? null : r.getCalleeMethodDesc()))
                    .thenComparingInt(r -> r == null || r.getOpCode() == null ? Integer.MAX_VALUE : r.getOpCode())
                    .thenComparing(r -> n(r == null ? null : r.getEdgeType()))
                    .thenComparing(r -> n(r == null ? null : r.getEdgeConfidence()))
                    .thenComparing(r -> n(r == null ? null : r.getEdgeEvidence()));

    public static final Comparator<DFSResult> DFS_RESULT =
            Comparator.comparingInt((DFSResult r) -> r == null ? Integer.MAX_VALUE : safeSize(r.getMethodList()))
                    .thenComparing(StableOrder::dfsPathKey);

    public static final Comparator<TaintResult> TAINT_RESULT =
            Comparator.comparing((TaintResult r) -> dfsPathKey(r == null ? null : r.getDfsResult()))
                    .thenComparing(r -> r != null && r.isSuccess() ? 0 : 1)
                    .thenComparing(r -> r != null && r.isLowConfidence() ? 1 : 0)
                    .thenComparing(r -> n(r == null ? null : r.getTaintText()));

    public static String dfsPathKey(DFSResult r) {
        if (r == null) {
            return "";
        }
        List<MethodReference.Handle> list = r.getMethodList();
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(list.size() * 64);
        for (MethodReference.Handle h : list) {
            sb.append(handleKey(h)).append("->");
        }
        return sb.toString();
    }

    private static int safeSize(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private static String handleKey(MethodReference.Handle h) {
        if (h == null) {
            return "";
        }
        String cls = "";
        if (h.getClassReference() != null) {
            cls = n(h.getClassReference().getName());
        }
        return cls + "." + n(h.getName()) + n(h.getDesc());
    }

    private static String n(String s) {
        return s == null ? "" : s;
    }
}

