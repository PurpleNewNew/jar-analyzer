package net.bytebuddy;

public class ByteBuddy {
    public ByteBuddy subclass(Class<?> type) {
        return this;
    }

    public ByteBuddy method(Object matcher) {
        return this;
    }

    public Object make() {
        return this;
    }
}
