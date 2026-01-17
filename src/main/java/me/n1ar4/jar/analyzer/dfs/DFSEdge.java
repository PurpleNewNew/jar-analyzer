/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.dfs;

import me.n1ar4.jar.analyzer.core.reference.MethodReference;

public class DFSEdge {
    private MethodReference.Handle from;
    private MethodReference.Handle to;
    private String type;
    private String confidence;
    private String evidence;

    public MethodReference.Handle getFrom() {
        return from;
    }

    public void setFrom(MethodReference.Handle from) {
        this.from = from;
    }

    public MethodReference.Handle getTo() {
        return to;
    }

    public void setTo(MethodReference.Handle to) {
        this.to = to;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }
}
