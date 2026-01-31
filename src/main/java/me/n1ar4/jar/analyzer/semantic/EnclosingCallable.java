/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.semantic;

public final class EnclosingCallable {
    public final String className;
    public final String methodName;
    public final int argCount;

    public EnclosingCallable(String className, String methodName, int argCount) {
        this.className = className;
        this.methodName = methodName;
        this.argCount = argCount;
    }
}
