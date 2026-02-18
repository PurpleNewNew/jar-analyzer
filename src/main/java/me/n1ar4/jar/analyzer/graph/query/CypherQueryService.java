/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.graph.query;

import me.n1ar4.jar.analyzer.graph.exec.GraphExecutor;
import me.n1ar4.jar.analyzer.graph.plan.LogicalPlan;
import me.n1ar4.jar.analyzer.graph.plan.Planner;
import me.n1ar4.jar.analyzer.graph.proc.ProcedureRegistry;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CypherQueryService implements QueryService {
    private final Planner planner = new Planner();
    private final GraphExecutor executor = new GraphExecutor(new GraphStore(), new ProcedureRegistry());

    @Override
    public QueryResult execute(String query, Map<String, Object> params, QueryOptions options) {
        LogicalPlan plan = planner.plan(query);
        return executor.execute(plan, params == null ? Map.of() : params, options == null ? QueryOptions.defaults() : options);
    }

    public Map<String, Object> explain(String query) {
        LogicalPlan plan = planner.plan(query);
        Map<String, Object> out = new HashMap<>();
        out.put("kind", plan.getKind().name().toLowerCase());
        out.put("procedure", plan.getProcedureName());
        out.put("labelHint", plan.getLabelHint());
        out.put("limit", plan.getLimit());
        out.put("skip", plan.getSkip());
        out.put("relationshipPattern", plan.isRelationshipPattern());
        out.put("shortestPath", plan.isShortestPath());
        out.put("maxHops", plan.getMaxHops());
        out.put("operators", operators(plan));
        return out;
    }

    public Map<String, Object> capabilities() {
        Map<String, Object> out = new HashMap<>();
        out.put("readOnly", true);
        out.put("clauses", List.of("MATCH", "WHERE", "RETURN", "ORDER BY", "LIMIT", "SKIP", "shortestPath", "CALL"));
        out.put("procedures", List.of("ja.path.from_to", "ja.path.shortest", "ja.taint.track"));
        out.put("profiles", List.of(QueryOptions.PROFILE_DEFAULT, QueryOptions.PROFILE_LONG_CHAIN));
        out.put("options", List.of("maxRows", "maxMs", "maxHops", "maxPaths", "profile", "expandBudget", "pathBudget", "timeoutCheckInterval"));
        out.put("unsupportedErrorCode", "cypher_feature_not_supported");
        return out;
    }

    private List<String> operators(LogicalPlan plan) {
        if (plan.getKind() == LogicalPlan.Kind.PROCEDURE) {
            return List.of("ProcedureCall", "Project", "Limit");
        }
        if (plan.isRelationshipPattern()) {
            return List.of("Scan", "Expand", "Filter", "Project", "Sort", "Limit");
        }
        return List.of("Scan", "Filter", "Project", "Sort", "Limit");
    }
}
