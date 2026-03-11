package me.n1ar4.cb;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class MyInvocationHandler implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        Task task = new FastTask();
        task.run();
        return null;
    }
}
