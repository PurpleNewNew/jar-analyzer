package com.sun.syndication.feed.impl;

import java.io.Serializable;
import java.lang.reflect.Method;

public class ToStringBean implements Serializable {
    private final Object bean;
    private final String getter;

    public ToStringBean(Object bean, String getter) {
        this.bean = bean;
        this.getter = getter;
    }

    @Override
    public String toString() {
        try {
            Method method = bean.getClass().getMethod(getter);
            return String.valueOf(method.invoke(bean));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
