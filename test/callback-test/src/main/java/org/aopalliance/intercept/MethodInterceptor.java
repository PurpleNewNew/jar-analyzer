package org.aopalliance.intercept;

public interface MethodInterceptor {
    Object invoke(MethodInvocation invocation) throws Throwable;
}
