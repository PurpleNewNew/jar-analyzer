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
    private final String calleeClass;
    private final String calleeMethod;
    private final String calleeDesc;

    public MethodCallKey(String callerClass, String callerMethod, String callerDesc,
                         String calleeClass, String calleeMethod, String calleeDesc) {
        this.callerClass = callerClass;
        this.callerMethod = callerMethod;
        this.callerDesc = callerDesc;
        this.calleeClass = calleeClass;
        this.calleeMethod = calleeMethod;
        this.calleeDesc = calleeDesc;
    }

    public static MethodCallKey of(MethodReference.Handle caller, MethodReference.Handle callee) {
        if (caller == null || callee == null) {
            return null;
        }
        return new MethodCallKey(
                caller.getClassReference().getName(),
                caller.getName(),
                caller.getDesc(),
                callee.getClassReference().getName(),
                callee.getName(),
                callee.getDesc()
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

    public String getCalleeClass() {
        return calleeClass;
    }

    public String getCalleeMethod() {
        return calleeMethod;
    }

    public String getCalleeDesc() {
        return calleeDesc;
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
                Objects.equals(calleeClass, that.calleeClass) &&
                Objects.equals(calleeMethod, that.calleeMethod) &&
                Objects.equals(calleeDesc, that.calleeDesc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(callerClass, callerMethod, callerDesc, calleeClass, calleeMethod, calleeDesc);
    }
}
