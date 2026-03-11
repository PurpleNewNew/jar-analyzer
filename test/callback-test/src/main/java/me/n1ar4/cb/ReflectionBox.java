package me.n1ar4.cb;

final class ReflectionBox {
    final String className;
    final String methodName;

    ReflectionBox(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }
}
