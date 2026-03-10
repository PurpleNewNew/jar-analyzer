package ysoserial.payloads.util;

import ysoserial.secmgr.ExecSink;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class MemoizedInvocationHandler implements InvocationHandler, Serializable {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        return ExecSink.exec(method == null ? "invoke" : method.getName());
    }
}
