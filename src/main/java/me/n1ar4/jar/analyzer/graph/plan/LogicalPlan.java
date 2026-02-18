/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.graph.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LogicalPlan {
    public enum Kind {
        MATCH,
        PROCEDURE
    }

    private final Kind kind;
    private final String rawQuery;
    private final String procedureName;
    private final List<String> procedureArgs;
    private final String labelHint;
    private final Integer limit;
    private final Integer skip;
    private final boolean relationshipPattern;
    private final boolean shortestPath;
    private final int maxHops;

    public LogicalPlan(Kind kind,
                       String rawQuery,
                       String procedureName,
                       List<String> procedureArgs,
                       String labelHint,
                       Integer limit,
                       Integer skip,
                       boolean relationshipPattern,
                       boolean shortestPath,
                       int maxHops) {
        this.kind = kind;
        this.rawQuery = rawQuery == null ? "" : rawQuery;
        this.procedureName = procedureName == null ? "" : procedureName;
        this.procedureArgs = procedureArgs == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(procedureArgs));
        this.labelHint = labelHint == null ? "" : labelHint;
        this.limit = limit;
        this.skip = skip;
        this.relationshipPattern = relationshipPattern;
        this.shortestPath = shortestPath;
        this.maxHops = Math.max(1, maxHops);
    }

    public Kind getKind() {
        return kind;
    }

    public String getRawQuery() {
        return rawQuery;
    }

    public String getProcedureName() {
        return procedureName;
    }

    public List<String> getProcedureArgs() {
        return procedureArgs;
    }

    public String getLabelHint() {
        return labelHint;
    }

    public Integer getLimit() {
        return limit;
    }

    public Integer getSkip() {
        return skip;
    }

    public boolean isRelationshipPattern() {
        return relationshipPattern;
    }

    public boolean isShortestPath() {
        return shortestPath;
    }

    public int getMaxHops() {
        return maxHops;
    }
}
