package org.springframework.cglib.proxy;

public final class Enhancer {
    private Enhancer() {
    }

    public static Object create(Object callback) {
        return callback;
    }
}
