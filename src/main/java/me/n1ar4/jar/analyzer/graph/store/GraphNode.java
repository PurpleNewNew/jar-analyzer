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
    private final long nodeId;
    private final String kind;
    private final int jarId;
    private final String className;
    private final String methodName;
    private final String methodDesc;
    private final String callSiteKey;
    private final int lineNumber;
    private final int callIndex;

    public GraphNode(long nodeId,
                     String kind,
                     int jarId,
                     String className,
                     String methodName,
                     String methodDesc,
                     String callSiteKey,
                     int lineNumber,
                     int callIndex) {
        this.nodeId = nodeId;
        this.kind = safe(kind);
        this.jarId = jarId;
        this.className = safe(className);
        this.methodName = safe(methodName);
        this.methodDesc = safe(methodDesc);
        this.callSiteKey = safe(callSiteKey);
        this.lineNumber = lineNumber;
        this.callIndex = callIndex;
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

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
