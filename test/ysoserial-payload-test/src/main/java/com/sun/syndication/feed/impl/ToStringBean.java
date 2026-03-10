package com.sun.syndication.feed.impl;

import ysoserial.secmgr.ExecSink;

import java.io.Serializable;
import java.lang.reflect.Method;

public class ToStringBean implements Serializable {
    private final Object bean;
    private final String methodName;

    public ToStringBean(Object bean, String methodName) {
        this.bean = bean;
        this.methodName = methodName;
    }

    @Override
    public String toString() {
        if (bean == null || methodName == null || methodName.isEmpty()) {
            return ExecSink.exec("rome-empty");
        }
        try {
            Method method = bean.getClass().getMethod(methodName);
            Object value = method.invoke(bean);
            return String.valueOf(value);
        } catch (Exception ex) {
            return ExecSink.exec("rome-fallback");
        }
    }
}
