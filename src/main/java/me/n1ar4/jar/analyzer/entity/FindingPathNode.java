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

public class FindingPathNode {
    private String className;
    private String methodName;
    private String methodDesc;
    private Integer jarId;
    private String jarName;
    private String classRole;

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

    public String getClassRole() {
        return classRole;
    }

    public void setClassRole(String classRole) {
        this.classRole = classRole;
    }
}
