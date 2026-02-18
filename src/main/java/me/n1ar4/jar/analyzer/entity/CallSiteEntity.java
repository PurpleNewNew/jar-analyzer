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

public class CallSiteEntity {
    private String callerClassName;
    private String callerMethodName;
    private String callerMethodDesc;
    private String calleeOwner;
    private String calleeMethodName;
    private String calleeMethodDesc;
    private Integer opCode;
    private Integer lineNumber;
    private Integer callIndex;
    private String receiverType;
    private Integer jarId;
    private String callSiteKey;

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

    public String getCalleeOwner() {
        return calleeOwner;
    }

    public void setCalleeOwner(String calleeOwner) {
        this.calleeOwner = calleeOwner;
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

    public Integer getOpCode() {
        return opCode;
    }

    public void setOpCode(Integer opCode) {
        this.opCode = opCode;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public Integer getCallIndex() {
        return callIndex;
    }

    public void setCallIndex(Integer callIndex) {
        this.callIndex = callIndex;
    }

    public String getReceiverType() {
        return receiverType;
    }

    public void setReceiverType(String receiverType) {
        this.receiverType = receiverType;
    }

    public Integer getJarId() {
        return jarId;
    }

    public void setJarId(Integer jarId) {
        this.jarId = jarId;
    }

    public String getCallSiteKey() {
        return callSiteKey;
    }

    public void setCallSiteKey(String callSiteKey) {
        this.callSiteKey = callSiteKey;
    }
}
