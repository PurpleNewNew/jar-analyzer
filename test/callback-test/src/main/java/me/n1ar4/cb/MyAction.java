package me.n1ar4.cb;

import java.security.PrivilegedAction;

public class MyAction implements PrivilegedAction<Object> {
    @Override
    public Object run() {
        return "ok";
    }
}

