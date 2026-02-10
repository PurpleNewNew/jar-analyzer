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

public class MethodResultEdge extends MethodResult {
    private String edgeType;
    private String edgeConfidence;
    private String edgeEvidence;
    // Optional UI-only evidence derived from bytecode_call_site_table (line/index/receiver).
    private String callSiteEvidence;

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

    public String getCallSiteEvidence() {
        return callSiteEvidence;
    }

    public void setCallSiteEvidence(String callSiteEvidence) {
        this.callSiteEvidence = callSiteEvidence;
    }
}
