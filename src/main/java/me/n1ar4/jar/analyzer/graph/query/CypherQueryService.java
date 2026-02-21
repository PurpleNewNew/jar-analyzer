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

import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jStore;
import org.neo4j.graphdb.ExecutionPlanDescription;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class CypherQueryService implements QueryService {
    private static final Pattern WRITE_KEYWORD_PATTERN = Pattern.compile(
            "(?is)\\b(create|merge|set|delete|detach|remove|drop|alter|load\\s+csv|start\\s+database|stop\\s+database)\\b"
    );

    private final Neo4jStore store;

    public CypherQueryService(Neo4jStore store) {
        this.store = store;
    }

    @Override
    public QueryResult execute(String query, Map<String, Object> params, QueryOptions options) {
        String cypher = normalize(query);
        if (cypher.isEmpty()) {
            return QueryResult.empty();
        }
        assertReadOnly(cypher);

        QueryOptions opts = options == null ? QueryOptions.defaults() : options;
        Map<String, Object> queryParams = params == null ? Map.of() : params;

        try {
            return store.read(opts.getMaxMs(), tx -> {
                Result result = tx.execute(cypher, queryParams);
                List<String> columns = new ArrayList<>(result.columns());
                List<List<Object>> rows = new ArrayList<>();
                List<String> warnings = new ArrayList<>();
                boolean truncated = false;

                while (result.hasNext()) {
                    if (rows.size() >= opts.getMaxRows()) {
                        truncated = true;
                        warnings.add("result_truncated_by_max_rows");
                        break;
                    }
                    Map<String, Object> row = result.next();
                    List<Object> out = new ArrayList<>(columns.size());
                    for (String col : columns) {
                        out.add(normalizeValue(row.get(col)));
                    }
                    rows.add(out);
                }
                return new QueryResult(columns, rows, warnings, truncated);
            });
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw mapNeo4jException(ex);
        }
    }

    public Map<String, Object> explain(String query) {
        String cypher = normalize(query);
        if (cypher.isEmpty()) {
            throw new IllegalArgumentException("cypher_empty_query");
        }
        assertReadOnly(cypher);

        try {
            return store.read(QueryOptions.defaults().getMaxMs(), tx -> {
                Result result = tx.execute("EXPLAIN " + stripPlanPrefix(cypher));
                ExecutionPlanDescription plan = result.getExecutionPlanDescription();
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("kind", "neo4j-explain");
                out.put("operators", flattenOperators(plan));
                out.put("plan", mapPlan(plan));
                return out;
            });
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw mapNeo4jException(ex);
        }
    }

    public Map<String, Object> capabilities() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("engine", "neo4j-embedded-lite");
        out.put("readOnly", true);
        out.put("clauses", List.of(
                "MATCH",
                "OPTIONAL MATCH",
                "WHERE",
                "WITH",
                "UNWIND",
                "CALL {}",
                "RETURN",
                "ORDER BY",
                "LIMIT",
                "SKIP"
        ));
        out.put("profiles", List.of(QueryOptions.PROFILE_DEFAULT, QueryOptions.PROFILE_LONG_CHAIN));
        out.put("options", List.of(
                "maxRows",
                "maxMs",
                "maxHops",
                "maxPaths",
                "profile",
                "expandBudget",
                "pathBudget",
                "timeoutCheckInterval"
        ));
        out.put("unsupportedErrorCode", "cypher_read_only");
        return out;
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim();
    }

    private static void assertReadOnly(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("cypher_empty_query");
        }
        String normalized = stripPlanPrefix(query).toLowerCase(Locale.ROOT);
        if (WRITE_KEYWORD_PATTERN.matcher(normalized).find()) {
            throw new IllegalArgumentException("cypher_read_only");
        }
    }

    private static String stripPlanPrefix(String query) {
        String normalized = normalize(query);
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.startsWith("explain ")) {
            return normalized.substring("explain ".length()).trim();
        }
        if (lower.startsWith("profile ")) {
            return normalized.substring("profile ".length()).trim();
        }
        return normalized;
    }

    private static IllegalArgumentException mapNeo4jException(Exception ex) {
        String message = ex.getMessage() == null ? ex.toString() : ex.getMessage();
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("feature disabled: load csv")) {
            return new IllegalArgumentException("feature disabled: LOAD CSV");
        }
        if (lower.contains("feature disabled: quantified path patterns")) {
            return new IllegalArgumentException("feature disabled: quantified path patterns");
        }
        if (lower.contains("syntax") || lower.contains("invalid input")) {
            return new IllegalArgumentException("cypher_parse_error: " + message);
        }
        if (lower.contains("timed out") || lower.contains("timeout")) {
            return new IllegalArgumentException("cypher_query_timeout");
        }
        return new IllegalArgumentException("cypher_query_invalid: " + message);
    }

    private static Map<String, Object> mapPlan(ExecutionPlanDescription plan) {
        if (plan == null) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", plan.getName());
        out.put("arguments", normalizeValue(plan.getArguments()));
        out.put("identifiers", normalizeValue(plan.getIdentifiers()));
        List<Map<String, Object>> children = new ArrayList<>();
        for (ExecutionPlanDescription child : plan.getChildren()) {
            children.add(mapPlan(child));
        }
        out.put("children", children);
        return out;
    }

    private static List<String> flattenOperators(ExecutionPlanDescription plan) {
        List<String> out = new ArrayList<>();
        flattenOperators(plan, out);
        return out;
    }

    private static void flattenOperators(ExecutionPlanDescription plan, List<String> out) {
        if (plan == null || out == null) {
            return;
        }
        out.add(plan.getName());
        for (ExecutionPlanDescription child : plan.getChildren()) {
            flattenOperators(child, out);
        }
    }

    private static Object normalizeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Node node) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("id", node.getId());
            List<String> labels = new ArrayList<>();
            node.getLabels().forEach(label -> labels.add(label.name()));
            out.put("labels", labels);
            out.put("properties", normalizeValue(node.getAllProperties()));
            return out;
        }
        if (value instanceof Relationship relationship) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("id", relationship.getId());
            out.put("type", relationship.getType().name());
            out.put("start", relationship.getStartNodeId());
            out.put("end", relationship.getEndNodeId());
            out.put("properties", normalizeValue(relationship.getAllProperties()));
            return out;
        }
        if (value instanceof Path path) {
            Map<String, Object> out = new LinkedHashMap<>();
            List<Object> nodes = new ArrayList<>();
            path.nodes().forEach(node -> nodes.add(normalizeValue(node)));
            List<Object> relationships = new ArrayList<>();
            path.relationships().forEach(rel -> relationships.add(normalizeValue(rel)));
            out.put("length", path.length());
            out.put("nodes", nodes);
            out.put("relationships", relationships);
            return out;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                out.put(String.valueOf(entry.getKey()), normalizeValue(entry.getValue()));
            }
            return out;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> out = new ArrayList<>();
            for (Object item : iterable) {
                out.add(normalizeValue(item));
            }
            return out;
        }
        Class<?> type = value.getClass();
        if (type.isArray()) {
            int len = Array.getLength(value);
            List<Object> out = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                out.add(normalizeValue(Array.get(value, i)));
            }
            return out;
        }
        if (type.isPrimitive()
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof String) {
            return value;
        }
        return String.valueOf(value);
    }
}
