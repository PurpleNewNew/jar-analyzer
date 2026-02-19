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

import me.n1ar4.jar.analyzer.core.reference.MethodReference;

import java.util.Objects;

public final class MethodCallKey {
    private final String callerClass;
    private final String callerMethod;
    private final String callerDesc;
    private final Integer callerJarId;
    private final String calleeClass;
    private final String calleeMethod;
    private final String calleeDesc;
    private final Integer calleeJarId;

    public MethodCallKey(String callerClass, String callerMethod, String callerDesc,
                         String calleeClass, String calleeMethod, String calleeDesc) {
        this(callerClass, callerMethod, callerDesc, -1,
                calleeClass, calleeMethod, calleeDesc, -1);
    }

    public MethodCallKey(String callerClass, String callerMethod, String callerDesc, Integer callerJarId,
                         String calleeClass, String calleeMethod, String calleeDesc, Integer calleeJarId) {
        this.callerClass = callerClass;
        this.callerMethod = callerMethod;
        this.callerDesc = callerDesc;
        this.callerJarId = normalizeJarId(callerJarId);
        this.calleeClass = calleeClass;
        this.calleeMethod = calleeMethod;
        this.calleeDesc = calleeDesc;
        this.calleeJarId = normalizeJarId(calleeJarId);
    }

    public static MethodCallKey of(MethodReference.Handle caller, MethodReference.Handle callee) {
        if (caller == null || callee == null) {
            return null;
        }
        return new MethodCallKey(
                caller.getClassReference().getName(),
                caller.getName(),
                caller.getDesc(),
                -1,
                callee.getClassReference().getName(),
                callee.getName(),
                callee.getDesc(),
                -1
        );
    }

    public String getCallerClass() {
        return callerClass;
    }

    public String getCallerMethod() {
        return callerMethod;
    }

    public String getCallerDesc() {
        return callerDesc;
    }

    public Integer getCallerJarId() {
        return callerJarId;
    }

    public String getCalleeClass() {
        return calleeClass;
    }

    public String getCalleeMethod() {
        return calleeMethod;
    }

    public String getCalleeDesc() {
        return calleeDesc;
    }

    public Integer getCalleeJarId() {
        return calleeJarId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MethodCallKey that = (MethodCallKey) o;
        return Objects.equals(callerClass, that.callerClass) &&
                Objects.equals(callerMethod, that.callerMethod) &&
                Objects.equals(callerDesc, that.callerDesc) &&
                Objects.equals(callerJarId, that.callerJarId) &&
                Objects.equals(calleeClass, that.calleeClass) &&
                Objects.equals(calleeMethod, that.calleeMethod) &&
                Objects.equals(calleeDesc, that.calleeDesc) &&
                Objects.equals(calleeJarId, that.calleeJarId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(callerClass, callerMethod, callerDesc, callerJarId,
                calleeClass, calleeMethod, calleeDesc, calleeJarId);
    }

    private static Integer normalizeJarId(Integer jarId) {
        if (jarId == null) {
            return -1;
        }
        return jarId;
    }
}
