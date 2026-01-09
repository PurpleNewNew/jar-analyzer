/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.taint;

import com.alibaba.fastjson2.annotation.JSONField;

public class Sanitizer {
    // 特殊标记：匹配 this 引用（仅实例方法）
    public static final int THIS_PARAM = -2;
    // 特殊标记：匹配所有参数（避免与真实参数索引冲突）
    public static final int ALL_PARAMS = -1;
    @JSONField
    private String className;
    @JSONField
    private String methodName;
    @JSONField
    private String methodDesc;
    @JSONField
    private int paramIndex;

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

    public int getParamIndex() {
        return paramIndex;
    }

    public void setParamIndex(int paramIndex) {
        this.paramIndex = paramIndex;
    }

    @Override
    public String toString() {
        return "Sanitizer{" +
                "className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", methodDesc='" + methodDesc + '\'' +
                ", paramIndex=" + paramIndex +
                '}';
    }
}
