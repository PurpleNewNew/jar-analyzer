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

import java.util.ArrayList;
import java.util.List;

public class FindingDetail {
    private FindingSummary summary;
    private List<FindingPathNode> path = new ArrayList<>();

    public FindingSummary getSummary() {
        return summary;
    }

    public void setSummary(FindingSummary summary) {
        this.summary = summary;
    }

    public List<FindingPathNode> getPath() {
        return path;
    }

    public void setPath(List<FindingPathNode> path) {
        this.path = path;
    }
}
