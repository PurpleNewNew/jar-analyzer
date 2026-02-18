package org.aopalliance.intercept;

public interface MethodInvocation {
    Object proceed() throws Throwable;
}
