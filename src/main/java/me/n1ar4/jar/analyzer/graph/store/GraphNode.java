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

public final class GraphNode {
    public static final int SOURCE_FLAG_ANY = 1;
    public static final int SOURCE_FLAG_WEB = 1 << 1;
    public static final int SOURCE_FLAG_MODEL = 1 << 2;
    public static final int SOURCE_FLAG_ANNOTATION = 1 << 3;
    public static final int SOURCE_FLAG_RPC = 1 << 4;

    private final long nodeId;
    private final String kind;
    private final int jarId;
    private final String className;
    private final String methodName;
    private final String methodDesc;
    private final String callSiteKey;
    private final int lineNumber;
    private final int callIndex;
    private final int sourceFlags;

    public GraphNode(long nodeId,
                     String kind,
                     int jarId,
                     String className,
                     String methodName,
                     String methodDesc,
                     String callSiteKey,
                     int lineNumber,
                     int callIndex) {
        this(nodeId, kind, jarId, className, methodName, methodDesc, callSiteKey, lineNumber, callIndex, 0);
    }

    public GraphNode(long nodeId,
                     String kind,
                     int jarId,
                     String className,
                     String methodName,
                     String methodDesc,
                     String callSiteKey,
                     int lineNumber,
                     int callIndex,
                     int sourceFlags) {
        this.nodeId = nodeId;
        this.kind = safe(kind);
        this.jarId = jarId;
        this.className = safe(className);
        this.methodName = safe(methodName);
        this.methodDesc = safe(methodDesc);
        this.callSiteKey = safe(callSiteKey);
        this.lineNumber = lineNumber;
        this.callIndex = callIndex;
        this.sourceFlags = Math.max(0, sourceFlags);
    }

    public long getNodeId() {
        return nodeId;
    }

    public String getKind() {
        return kind;
    }

    public int getJarId() {
        return jarId;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodDesc() {
        return methodDesc;
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

    public int getSourceFlags() {
        return sourceFlags;
    }

    public boolean hasSourceFlag(int flag) {
        if (flag <= 0) {
            return false;
        }
        return (sourceFlags & flag) != 0;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
