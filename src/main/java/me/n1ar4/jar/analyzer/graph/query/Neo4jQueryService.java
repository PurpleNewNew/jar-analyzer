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

import me.n1ar4.jar.analyzer.graph.plan.LogicalPlan;
import me.n1ar4.jar.analyzer.graph.plan.Planner;
import me.n1ar4.jar.analyzer.graph.proc.ProcedureRegistry;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jProjectStore;
import org.neo4j.graphdb.ExecutionPlanDescription;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class Neo4jQueryService implements QueryService {
    private static final Pattern WRITE_CLAUSE_PATTERN = Pattern.compile(
            "(?is)\\b(create|merge|delete|detach\\s+delete|set|remove|drop|load\\s+csv|schema)\\b");

    private final Planner planner = new Planner();
    private final ProcedureRegistry procedures = new ProcedureRegistry();
    private final GraphStore graphStore = new GraphStore();
    private final Neo4jProjectStore projectStore = Neo4jProjectStore.getInstance();

    @Override
    public QueryResult execute(String query, Map<String, Object> params, QueryOptions options) throws Exception {
        return execute(query, params, options, null);
    }

    public QueryResult execute(String query,
                               Map<String, Object> params,
                               QueryOptions options,
                               String projectKey) throws Exception {
        String cypher = query == null ? "" : query.trim();
        if (cypher.isEmpty()) {
            throw new IllegalArgumentException("cypher_empty_query");
        }
        Map<String, Object> safeParams = params == null ? Map.of() : params;
        QueryOptions safeOptions = options == null ? QueryOptions.defaults() : options;

        if (isJaProcedure(cypher)) {
            return executeProcedure(cypher, safeParams, safeOptions, projectKey);
        }
        if (containsWriteClause(cypher)) {
            throw new IllegalArgumentException("cypher_feature_not_supported");
        }
        return executeNative(cypher, safeParams, safeOptions, projectKey);
    }

    public Map<String, Object> explain(String query) {
        return explain(query, null);
    }

    public Map<String, Object> explain(String query, String projectKey) {
        String cypher = query == null ? "" : query.trim();
        if (cypher.isEmpty()) {
            throw new IllegalArgumentException("cypher_empty_query");
        }
        if (isJaProcedure(cypher)) {
            LogicalPlan plan = planner.plan(cypher);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("engine", "ja-procedure");
            out.put("kind", plan.getKind().name().toLowerCase(Locale.ROOT));
            out.put("procedure", plan.getProcedureName());
            out.put("operators", List.of("ProcedureCall", "Project", "Limit"));
            return out;
        }
        if (containsWriteClause(cypher)) {
            throw new IllegalArgumentException("cypher_feature_not_supported");
        }
        String resolvedProject = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        return ActiveProjectContext.withProject(resolvedProject, () -> {
            GraphDatabaseService db = projectStore.activeDatabase();
            try (Transaction tx = db.beginTx();
                 Result result = tx.execute("EXPLAIN " + cypher)) {
                ExecutionPlanDescription plan = result.getExecutionPlanDescription();
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("engine", "neo4j");
                out.put("kind", "read");
                out.put("operators", flattenOperators(plan));
                out.put("arguments", plan == null ? Map.of() : plan.getArguments());
                tx.commit();
                return out;
            } catch (IllegalArgumentException ex) {
                throw ex;
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? "cypher_explain_error" : ex.getMessage();
                if (msg.toLowerCase(Locale.ROOT).contains("timeout")) {
                    throw new IllegalArgumentException("cypher_query_timeout");
                }
                throw new IllegalArgumentException(msg);
            }
        });
    }

    public Map<String, Object> capabilities() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("engine", "neo4j-embedded");
        out.put("readOnly", true);
        out.put("clauses", List.of("MATCH", "WHERE", "RETURN", "WITH", "ORDER BY", "LIMIT", "SKIP", "CALL", "UNION", "UNWIND", "subquery"));
        out.put("procedures", List.of("ja.path.from_to", "ja.path.shortest", "ja.taint.track"));
        out.put("profiles", List.of(QueryOptions.PROFILE_DEFAULT, QueryOptions.PROFILE_LONG_CHAIN));
        out.put("options", List.of("maxRows", "maxMs", "maxHops", "maxPaths", "profile", "expandBudget", "pathBudget", "timeoutCheckInterval"));
        out.put("unsupportedErrorCode", "cypher_feature_not_supported");
        return out;
    }

    private QueryResult executeProcedure(String query,
                                         Map<String, Object> params,
                                         QueryOptions options,
                                         String projectKey) {
        LogicalPlan plan = planner.plan(query);
        String resolvedProject = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        return ActiveProjectContext.withProject(resolvedProject, () -> {
            GraphSnapshot snapshot = graphStore.loadSnapshot();
            return procedures.execute(plan.getProcedureName(), plan.getProcedureArgs(), params, options, snapshot);
        });
    }

    private QueryResult executeNative(String query,
                                      Map<String, Object> params,
                                      QueryOptions options,
                                      String projectKey) {
        String resolvedProject = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        return ActiveProjectContext.withProject(resolvedProject, () -> {
            GraphDatabaseService db = projectStore.activeDatabase();
            List<String> warnings = new ArrayList<>();
            if (options.getMaxHops() > 0 || options.getMaxPaths() > 0
                    || options.getExpandBudget() > 0 || options.getPathBudget() > 0) {
                warnings.add("budget_options_apply_to_ja_procedures_only");
            }
            boolean truncated = false;
            try (Transaction tx = db.beginTx(options.getMaxMs(), TimeUnit.MILLISECONDS);
                 Result result = tx.execute(query, params)) {
                List<String> columns = result.columns();
                List<List<Object>> rows = new ArrayList<>();
                int maxRows = options.getMaxRows();
                while (result.hasNext()) {
                    if (rows.size() >= maxRows) {
                        truncated = true;
                        break;
                    }
                    Map<String, Object> row = result.next();
                    List<Object> values = new ArrayList<>(columns.size());
                    for (String column : columns) {
                        values.add(convertValue(row.get(column)));
                    }
                    rows.add(values);
                }
                tx.commit();
                return new QueryResult(columns, rows, warnings, truncated);
            } catch (QueryExecutionException ex) {
                String msg = safe(ex.getMessage()).toLowerCase(Locale.ROOT);
                if (msg.contains("write") || msg.contains("creating") || msg.contains("updating")) {
                    throw new IllegalArgumentException("cypher_feature_not_supported");
                }
                if (msg.contains("timeout") || msg.contains("terminated")) {
                    throw new IllegalArgumentException("cypher_query_timeout");
                }
                throw new IllegalArgumentException(safe(ex.getMessage()));
            } catch (IllegalArgumentException ex) {
                throw ex;
            } catch (Exception ex) {
                String msg = safe(ex.getMessage());
                if (msg.toLowerCase(Locale.ROOT).contains("timeout")
                        || msg.toLowerCase(Locale.ROOT).contains("terminated")) {
                    throw new IllegalArgumentException("cypher_query_timeout");
                }
                throw new IllegalArgumentException(msg.isBlank() ? "cypher_query_invalid" : msg);
            }
        });
    }

    private static boolean isJaProcedure(String query) {
        String value = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return value.startsWith("call ja.path.") || value.startsWith("call ja.taint.");
    }

    private static boolean containsWriteClause(String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        String lower = query.toLowerCase(Locale.ROOT).trim();
        if (lower.startsWith("call ja.path.") || lower.startsWith("call ja.taint.")) {
            return false;
        }
        return WRITE_CLAUSE_PATTERN.matcher(query).find();
    }

    private static Object convertValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Node node) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("id", node.getId());
            out.put("node_id", node.getProperty("node_id", node.getId()));
            List<String> labels = new ArrayList<>();
            for (var label : node.getLabels()) {
                labels.add(label.name());
            }
            out.put("labels", labels);
            out.put("properties", new LinkedHashMap<>(node.getAllProperties()));
            return out;
        }
        if (value instanceof Relationship relationship) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("id", relationship.getId());
            out.put("edge_id", relationship.getProperty("edge_id", relationship.getId()));
            out.put("type", relationship.getType().name());
            out.put("startNodeId", relationship.getStartNode().getProperty("node_id", relationship.getStartNodeId()));
            out.put("endNodeId", relationship.getEndNode().getProperty("node_id", relationship.getEndNodeId()));
            out.put("properties", new LinkedHashMap<>(relationship.getAllProperties()));
            return out;
        }
        if (value instanceof Path path) {
            Map<String, Object> out = new LinkedHashMap<>();
            List<Object> nodes = new ArrayList<>();
            for (Node node : path.nodes()) {
                nodes.add(convertValue(node));
            }
            List<Object> rels = new ArrayList<>();
            for (Relationship relationship : path.relationships()) {
                rels.add(convertValue(relationship));
            }
            out.put("nodes", nodes);
            out.put("relationships", rels);
            out.put("length", path.length());
            return out;
        }
        if (value instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object item : list) {
                out.add(convertValue(item));
            }
            return out;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                out.put(String.valueOf(entry.getKey()), convertValue(entry.getValue()));
            }
            return out;
        }
        return value;
    }

    private static List<String> flattenOperators(ExecutionPlanDescription root) {
        if (root == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        flatten(root, out);
        return out;
    }

    private static void flatten(ExecutionPlanDescription node, List<String> out) {
        if (node == null || out == null) {
            return;
        }
        out.add(node.getName());
        for (ExecutionPlanDescription child : node.getChildren()) {
            flatten(child, out);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
