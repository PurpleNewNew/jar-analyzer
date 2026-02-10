package me.n1ar4.cb;

import java.util.function.Supplier;

public class MySupplier implements Supplier<Object> {
    @Override
    public Object get() {
        return "ok";
    }
}

