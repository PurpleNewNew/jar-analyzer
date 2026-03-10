/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.graph.store;

public final class GraphEdge {
    private final long edgeId;
    private final long srcId;
    private final long dstId;
    private final String relType;
    private final String confidence;
    private final String evidence;
    private final String aliasKind;
    private final int opCode;
    private final String callSiteKey;
    private final int lineNumber;
    private final int callIndex;
    private final int semanticFlags;

    public GraphEdge(long edgeId,
                     long srcId,
                     long dstId,
                     String relType,
                     String confidence,
                     String evidence,
                     int opCode) {
        this(edgeId, srcId, dstId, relType, confidence, evidence, "", opCode, "", -1, -1, 0);
    }

    public GraphEdge(long edgeId,
                     long srcId,
                     long dstId,
                     String relType,
                     String confidence,
                     String evidence,
                     String aliasKind,
                     int opCode,
                     String callSiteKey,
                     int lineNumber,
                     int callIndex) {
        this(edgeId, srcId, dstId, relType, confidence, evidence, aliasKind, opCode, callSiteKey, lineNumber, callIndex, 0);
    }

    public GraphEdge(long edgeId,
                     long srcId,
                     long dstId,
                     String relType,
                     String confidence,
                     String evidence,
                     String aliasKind,
                     int opCode,
                     String callSiteKey,
                     int lineNumber,
                     int callIndex,
                     int semanticFlags) {
        this.edgeId = edgeId;
        this.srcId = srcId;
        this.dstId = dstId;
        this.relType = safe(relType);
        this.confidence = safe(confidence);
        this.evidence = safe(evidence);
        this.aliasKind = safe(aliasKind);
        this.opCode = opCode;
        this.callSiteKey = safe(callSiteKey);
        this.lineNumber = lineNumber;
        this.callIndex = callIndex;
        this.semanticFlags = Math.max(0, semanticFlags);
    }

    public long getEdgeId() {
        return edgeId;
    }

    public long getSrcId() {
        return srcId;
    }

    public long getDstId() {
        return dstId;
    }

    public String getRelType() {
        return relType;
    }

    public String getConfidence() {
        return confidence;
    }

    public String getEvidence() {
        return evidence;
    }

    public String getAliasKind() {
        return aliasKind;
    }

    public int getOpCode() {
        return opCode;
    }

    public String getCallSiteKey() {
        return callSiteKey;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getCallIndex() {
        return callIndex;
    }

    public int getSemanticFlags() {
        return semanticFlags;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
