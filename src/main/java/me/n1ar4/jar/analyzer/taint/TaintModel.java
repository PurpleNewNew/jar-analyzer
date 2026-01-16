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

import java.util.List;

public class TaintModel {
    @JSONField
    private String className;
    @JSONField
    private String methodName;
    @JSONField
    private String methodDesc;
    @JSONField
    private List<TaintFlow> flows;

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

    public List<TaintFlow> getFlows() {
        return flows;
    }

    public void setFlows(List<TaintFlow> flows) {
        this.flows = flows;
    }
}
