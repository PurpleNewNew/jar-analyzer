package me.n1ar4.cb;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class MySpringMethodInterceptor implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation invocation) {
        return null;
    }
}
