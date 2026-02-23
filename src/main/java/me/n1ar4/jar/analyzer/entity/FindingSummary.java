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

public class FindingSummary {
    private String findingId;
    private String rule;
    private String severity;
    private FindingPathNode entrypoint;
    private FindingPathNode firstAppFrame;
    private FindingPathNode sink;
    private double score;
    private int pathLen;

    public String getFindingId() {
        return findingId;
    }

    public void setFindingId(String findingId) {
        this.findingId = findingId;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public FindingPathNode getEntrypoint() {
        return entrypoint;
    }

    public void setEntrypoint(FindingPathNode entrypoint) {
        this.entrypoint = entrypoint;
    }

    public FindingPathNode getFirstAppFrame() {
        return firstAppFrame;
    }

    public void setFirstAppFrame(FindingPathNode firstAppFrame) {
        this.firstAppFrame = firstAppFrame;
    }

    public FindingPathNode getSink() {
        return sink;
    }

    public void setSink(FindingPathNode sink) {
        this.sink = sink;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int getPathLen() {
        return pathLen;
    }

    public void setPathLen(int pathLen) {
        this.pathLen = pathLen;
    }
}
