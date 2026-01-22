/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.entity;

public class AnnoMethodResult {
    private String className;
    private String methodName;
    private String methodDesc;
    private int lineNumber;
    private Integer jarId;
    private String jarName;
    private String annoName;
    private String annoScope;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodDesc() {
        return methodDesc;
    }

    public void setMethodDesc(String methodDesc) {
        this.methodDesc = methodDesc;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public Integer getJarId() {
        return jarId;
    }

    public void setJarId(Integer jarId) {
        this.jarId = jarId;
    }

    public String getJarName() {
        return jarName;
    }

    public void setJarName(String jarName) {
        this.jarName = jarName;
    }

    public String getAnnoName() {
        return annoName;
    }

    public void setAnnoName(String annoName) {
        this.annoName = annoName;
    }

    public String getAnnoScope() {
        return annoScope;
    }

    public void setAnnoScope(String annoScope) {
        this.annoScope = annoScope;
    }

    @Override
    public String toString() {
        return "AnnoMethodResult{" +
                "className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", methodDesc='" + methodDesc + '\'' +
                ", lineNumber=" + lineNumber +
                ", jarId=" + jarId +
                ", jarName='" + jarName + '\'' +
                ", annoName='" + annoName + '\'' +
                ", annoScope='" + annoScope + '\'' +
                '}';
    }
}
