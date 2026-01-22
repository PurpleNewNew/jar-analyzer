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

public class MethodCallResult {
    private String callerClassName;
    private String callerMethodName;
    private String callerMethodDesc;
    private Integer callerJarId;
    private String callerJarName;
    private String calleeClassName;
    private String calleeMethodName;
    private String calleeMethodDesc;
    private Integer calleeJarId;
    private String calleeJarName;
    private Integer opCode;
    private String edgeType;
    private String edgeConfidence;
    private String edgeEvidence;

    public String getCallerClassName() {
        return callerClassName;
    }

    public void setCallerClassName(String callerClassName) {
        this.callerClassName = callerClassName;
    }

    public String getCallerMethodName() {
        return callerMethodName;
    }

    public void setCallerMethodName(String callerMethodName) {
        this.callerMethodName = callerMethodName;
    }

    public String getCallerMethodDesc() {
        return callerMethodDesc;
    }

    public void setCallerMethodDesc(String callerMethodDesc) {
        this.callerMethodDesc = callerMethodDesc;
    }

    public Integer getCallerJarId() {
        return callerJarId;
    }

    public void setCallerJarId(Integer callerJarId) {
        this.callerJarId = callerJarId;
    }

    public String getCallerJarName() {
        return callerJarName;
    }

    public void setCallerJarName(String callerJarName) {
        this.callerJarName = callerJarName;
    }

    public String getCalleeClassName() {
        return calleeClassName;
    }

    public void setCalleeClassName(String calleeClassName) {
        this.calleeClassName = calleeClassName;
    }

    public String getCalleeMethodName() {
        return calleeMethodName;
    }

    public void setCalleeMethodName(String calleeMethodName) {
        this.calleeMethodName = calleeMethodName;
    }

    public String getCalleeMethodDesc() {
        return calleeMethodDesc;
    }

    public void setCalleeMethodDesc(String calleeMethodDesc) {
        this.calleeMethodDesc = calleeMethodDesc;
    }

    public Integer getCalleeJarId() {
        return calleeJarId;
    }

    public void setCalleeJarId(Integer calleeJarId) {
        this.calleeJarId = calleeJarId;
    }

    public String getCalleeJarName() {
        return calleeJarName;
    }

    public void setCalleeJarName(String calleeJarName) {
        this.calleeJarName = calleeJarName;
    }

    public Integer getOpCode() {
        return opCode;
    }

    public void setOpCode(Integer opCode) {
        this.opCode = opCode;
    }

    public String getEdgeType() {
        return edgeType;
    }

    public void setEdgeType(String edgeType) {
        this.edgeType = edgeType;
    }

    public String getEdgeConfidence() {
        return edgeConfidence;
    }

    public void setEdgeConfidence(String edgeConfidence) {
        this.edgeConfidence = edgeConfidence;
    }

    public String getEdgeEvidence() {
        return edgeEvidence;
    }

    public void setEdgeEvidence(String edgeEvidence) {
        this.edgeEvidence = edgeEvidence;
    }

    @Override
    public String toString() {
        return "MethodCallResult{" +
                "callerClassName='" + callerClassName + '\'' +
                ", callerMethodName='" + callerMethodName + '\'' +
                ", callerMethodDesc='" + callerMethodDesc + '\'' +
                ", callerJarId=" + callerJarId +
                ", callerJarName='" + callerJarName + '\'' +
                ", calleeClassName='" + calleeClassName + '\'' +
                ", calleeMethodName='" + calleeMethodName + '\'' +
                ", calleeMethodDesc='" + calleeMethodDesc + '\'' +
                ", calleeJarId=" + calleeJarId +
                ", calleeJarName='" + calleeJarName + '\'' +
                ", opCode=" + opCode +
                ", edgeType='" + edgeType + '\'' +
                ", edgeConfidence='" + edgeConfidence + '\'' +
                ", edgeEvidence='" + edgeEvidence + '\'' +
                '}';
    }
}
