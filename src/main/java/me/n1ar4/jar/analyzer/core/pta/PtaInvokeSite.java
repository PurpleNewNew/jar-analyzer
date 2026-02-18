/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.pta;

import java.util.Objects;

final class PtaInvokeSite {
    private final String callSiteKey;
    private final String declaredOwner;
    private final String calleeName;
    private final String calleeDesc;
    private final int opcode;
    private final String receiverVarId;
    private final String receiverType;
    private final int callIndex;
    private final int lineNumber;

    PtaInvokeSite(String callSiteKey,
                  String declaredOwner,
                  String calleeName,
                  String calleeDesc,
                  int opcode,
                  String receiverVarId,
                  String receiverType,
                  int callIndex,
                  int lineNumber) {
        this.callSiteKey = callSiteKey == null ? "" : callSiteKey;
        this.declaredOwner = declaredOwner;
        this.calleeName = calleeName;
        this.calleeDesc = calleeDesc;
        this.opcode = opcode;
        this.receiverVarId = normalizeReceiverVarId(receiverVarId, callSiteKey, declaredOwner, calleeName, calleeDesc,
                callIndex, lineNumber);
        this.receiverType = receiverType;
        this.callIndex = callIndex;
        this.lineNumber = lineNumber;
    }

    String getDeclaredOwner() {
        return declaredOwner;
    }

    String getCalleeName() {
        return calleeName;
    }

    String getCalleeDesc() {
        return calleeDesc;
    }

    int getOpcode() {
        return opcode;
    }

    String getReceiverType() {
        return receiverType;
    }

    String receiverVarId() {
        return receiverVarId;
    }

    String siteToken() {
        if (!callSiteKey.isEmpty()) {
            return callSiteKey;
        }
        return (declaredOwner == null ? "?" : declaredOwner)
                + "#" + (calleeName == null ? "?" : calleeName)
                + "#" + (calleeDesc == null ? "?" : calleeDesc)
                + "#" + callIndex + "#" + lineNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PtaInvokeSite that = (PtaInvokeSite) o;
        return opcode == that.opcode
                && callIndex == that.callIndex
                && lineNumber == that.lineNumber
                && Objects.equals(callSiteKey, that.callSiteKey)
                && Objects.equals(declaredOwner, that.declaredOwner)
                && Objects.equals(calleeName, that.calleeName)
                && Objects.equals(calleeDesc, that.calleeDesc)
                && Objects.equals(receiverVarId, that.receiverVarId)
                && Objects.equals(receiverType, that.receiverType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(callSiteKey, declaredOwner, calleeName,
                calleeDesc, opcode, receiverVarId, receiverType, callIndex, lineNumber);
    }

    private static String normalizeReceiverVarId(String receiverVarId,
                                                 String callSiteKey,
                                                 String declaredOwner,
                                                 String calleeName,
                                                 String calleeDesc,
                                                 int callIndex,
                                                 int lineNumber) {
        if (receiverVarId != null) {
            String value = receiverVarId.trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        String key = callSiteKey == null ? "" : callSiteKey;
        if (!key.isEmpty()) {
            return "recv@" + key;
        }
        return "recv@" + (declaredOwner == null ? "?" : declaredOwner)
                + "#" + (calleeName == null ? "?" : calleeName)
                + "#" + (calleeDesc == null ? "?" : calleeDesc)
                + "#" + callIndex + "#" + lineNumber;
    }
}
