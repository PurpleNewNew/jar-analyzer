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

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.rules.RuleValidationViews;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jProjectStore;
import me.n1ar4.jar.analyzer.storage.neo4j.procedure.ApocWhitelist;
import me.n1ar4.jar.analyzer.storage.neo4j.procedure.NativeJaQueryContext;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Neo4jQueryService {
    private static final Pattern WRITE_CLAUSE_PATTERN = Pattern.compile(
            "(?is)\\b(create|merge|delete|detach\\s+delete|remove|drop|load\\s+csv|schema)\\b");
    private static final Pattern SET_TOKEN_PATTERN = Pattern.compile("(?is)\\bset\\b");

    private final Neo4jProjectStore projectStore = Neo4jProjectStore.getInstance();

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
        if (containsWriteClause(cypher)) {
            throw new IllegalArgumentException("cypher_feature_not_supported");
        }
        return executeNative(CypherLogicalModelRewriter.rewrite(cypher), safeParams, safeOptions, projectKey);
    }

    public Map<String, Object> explain(String query) {
        return explain(query, null);
    }

    public Map<String, Object> explain(String query, String projectKey) {
        String cypher = query == null ? "" : query.trim();
        if (cypher.isEmpty()) {
            throw new IllegalArgumentException("cypher_empty_query");
        }
        if (containsWriteClause(cypher)) {
            throw new IllegalArgumentException("cypher_feature_not_supported");
        }
        return explainNative(CypherLogicalModelRewriter.rewrite(cypher), projectKey);
    }

    public Map<String, Object> capabilities() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("engine", "neo4j-embedded");
        out.put("readOnly", true);
        out.put("clauses", List.of("MATCH", "WHERE", "RETURN", "WITH", "ORDER BY", "LIMIT", "SKIP", "CALL", "UNION", "UNWIND", "subquery"));
        out.put("procedures", List.of(
                "ja.path.from_to",
                "ja.path.from_to_pruned",
                "ja.path.shortest",
                "ja.path.shortest_pruned",
                "ja.taint.track"
        ));
        out.put("functions", List.of(
                "ja.isSink",
                "ja.isSource",
                "ja.relGroup",
                "ja.relSubtype",
                "ja.sinkKind",
                "ja.ruleVersion",
                "ja.rulesFingerprint",
                "ja.ruleValidation",
                "ja.ruleValidationIssues"
        ));
        out.put("profiles", List.of(QueryOptions.PROFILE_DEFAULT, QueryOptions.PROFILE_LONG_CHAIN));
        out.put("options", List.of("maxRows", "maxMs", "maxHops", "maxPaths", "profile", "expandBudget", "pathBudget", "timeoutCheckInterval"));
        out.put("procedureMode", "native-only");
        out.put("apocMode", "read-only-whitelist");
        out.put("apocWhitelistMode", ApocWhitelist.whitelistMode());
        out.put("apocWhitelistProperty", ApocWhitelist.APOC_WHITELIST_PROP);
        out.put("apocWhitelist", ApocWhitelist.effectiveWhitelist());
        out.put("ruleValidation", RuleValidationViews.combinedValidationMap());
        out.put("graphModel", QueryDisplayModel.capabilitiesView());
        out.put("unsupportedErrorCode", "cypher_feature_not_supported");
        return out;
    }

    private QueryResult executeNative(String query,
                                      Map<String, Object> params,
                                      QueryOptions options,
                                      String projectKey) {
        String resolvedProject = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        DatabaseManager.ensureProjectReadable(resolvedProject);
        return ActiveProjectContext.withProject(resolvedProject, () -> {
            GraphDatabaseService db = projectStore.activeDatabase();
            List<String> warnings = new ArrayList<>();
            try {
                return NativeJaQueryContext.with(options, resolvedProject, () -> {
                    try (Transaction tx = db.beginTx(options.getMaxMs(), TimeUnit.MILLISECONDS);
                         Result result = tx.execute(query, params)) {
                        List<String> columns = result.columns();
                        List<List<Object>> rows = new ArrayList<>();
                        int maxRows = options.getMaxRows();
                        while (result.hasNext()) {
                            if (rows.size() >= maxRows) {
                                return new QueryResult(columns, rows, warnings, true);
                            }
                            Map<String, Object> row = result.next();
                            List<Object> values = new ArrayList<>(columns.size());
                            for (String column : columns) {
                                values.add(convertValue(row.get(column)));
                            }
                            rows.add(values);
                        }
                        tx.commit();
                        return new QueryResult(columns, rows, warnings, false);
                    }
                });
            } catch (QueryExecutionException ex) {
                String msg = safe(ex.getMessage()).toLowerCase(Locale.ROOT);
                if (msg.contains("write") || msg.contains("creating") || msg.contains("updating")) {
                    throw new IllegalArgumentException("cypher_feature_not_supported", ex);
                }
                if (msg.contains("timeout") || msg.contains("terminated")) {
                    throw new IllegalArgumentException("cypher_query_timeout", ex);
                }
                throw new IllegalArgumentException(safe(ex.getMessage()), ex);
            } catch (IllegalArgumentException ex) {
                throw ex;
            } catch (Exception ex) {
                String msg = safe(ex.getMessage());
                if (msg.toLowerCase(Locale.ROOT).contains("timeout")
                        || msg.toLowerCase(Locale.ROOT).contains("terminated")) {
                    throw new IllegalArgumentException("cypher_query_timeout", ex);
                }
                throw new IllegalArgumentException(msg.isBlank() ? "cypher_query_invalid" : msg, ex);
            }
        });
    }

    private Map<String, Object> explainNative(String query, String projectKey) {
        String resolvedProject = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        DatabaseManager.ensureProjectReadable(resolvedProject);
        return ActiveProjectContext.withProject(resolvedProject, () -> {
            GraphDatabaseService db = projectStore.activeDatabase();
            try {
                return NativeJaQueryContext.with(QueryOptions.defaults(), resolvedProject, () -> {
                    try (Transaction tx = db.beginTx();
                         Result result = tx.execute("EXPLAIN " + query)) {
                        ExecutionPlanDescription plan = result.getExecutionPlanDescription();
                        Map<String, Object> out = new LinkedHashMap<>();
                        out.put("engine", "neo4j");
                        out.put("kind", "read");
                        out.put("operators", flattenOperators(plan));
                        out.put("arguments", plan == null ? Map.of() : plan.getArguments());
                        tx.commit();
                        return out;
                    }
                });
            } catch (IllegalArgumentException ex) {
                throw ex;
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? "cypher_explain_error" : ex.getMessage();
                if (msg.toLowerCase(Locale.ROOT).contains("timeout")) {
                    throw new IllegalArgumentException("cypher_query_timeout");
                }
                throw new IllegalArgumentException(msg, ex);
            }
        });
    }

    private static boolean containsWriteClause(String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        String lower = query.toLowerCase(Locale.ROOT).trim();
        if (lower.startsWith("call ja.path.") || lower.startsWith("call ja.taint.")) {
            return false;
        }
        String sanitized = stripLiteralsAndComments(query);
        if (WRITE_CLAUSE_PATTERN.matcher(sanitized).find()) {
            return true;
        }
        return containsSetClause(sanitized);
    }

    private static boolean containsSetClause(String sanitized) {
        if (sanitized == null || sanitized.isBlank()) {
            return false;
        }
        Matcher matcher = SET_TOKEN_PATTERN.matcher(sanitized);
        while (matcher.find()) {
            int start = matcher.start();
            int prev = previousNonWhitespace(sanitized, start - 1);
            if (prev >= 0 && sanitized.charAt(prev) == '.') {
                // property access such as n.set
                continue;
            }
            String prevWord = previousWord(sanitized, start - 1);
            if ("as".equals(prevWord)) {
                continue;
            }
            int next = nextNonWhitespace(sanitized, matcher.end());
            if (next < 0) {
                continue;
            }
            char nextChar = sanitized.charAt(next);
            if (Character.isLetter(nextChar) || nextChar == '_' || nextChar == '`' || nextChar == '(') {
                return true;
            }
        }
        return false;
    }

    private static int previousNonWhitespace(String value, int index) {
        int i = index;
        while (i >= 0) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return i;
            }
            i--;
        }
        return -1;
    }

    private static int nextNonWhitespace(String value, int index) {
        int i = index;
        while (i < value.length()) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private static String previousWord(String value, int index) {
        int end = previousNonWhitespace(value, index);
        if (end < 0) {
            return "";
        }
        int start = end;
        while (start >= 0 && Character.isLetter(value.charAt(start))) {
            start--;
        }
        return value.substring(start + 1, end + 1).toLowerCase(Locale.ROOT);
    }

    private static String stripLiteralsAndComments(String query) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(query.length());
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        int n = query.length();
        for (int i = 0; i < n; i++) {
            char c = query.charAt(i);
            char next = i + 1 < n ? query.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (c == '\n' || c == '\r') {
                    inLineComment = false;
                    out.append(c);
                } else {
                    out.append(' ');
                }
                continue;
            }

            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    out.append(' ').append(' ');
                    i++;
                    inBlockComment = false;
                } else if (c == '\n' || c == '\r') {
                    out.append(c);
                } else {
                    out.append(' ');
                }
                continue;
            }

            if (inSingleQuote) {
                if (c == '\'' && next == '\'') {
                    out.append(' ').append(' ');
                    i++;
                    continue;
                }
                if (c == '\\' && i + 1 < n) {
                    out.append(' ').append(' ');
                    i++;
                    continue;
                }
                if (c == '\'') {
                    inSingleQuote = false;
                    out.append(' ');
                } else if (c == '\n' || c == '\r') {
                    out.append(c);
                } else {
                    out.append(' ');
                }
                continue;
            }

            if (inDoubleQuote) {
                if (c == '"' && next == '"') {
                    out.append(' ').append(' ');
                    i++;
                    continue;
                }
                if (c == '\\' && i + 1 < n) {
                    out.append(' ').append(' ');
                    i++;
                    continue;
                }
                if (c == '"') {
                    inDoubleQuote = false;
                    out.append(' ');
                } else if (c == '\n' || c == '\r') {
                    out.append(c);
                } else {
                    out.append(' ');
                }
                continue;
            }

            if (inBacktick) {
                if (c == '`') {
                    inBacktick = false;
                    out.append(' ');
                } else if (c == '\n' || c == '\r') {
                    out.append(c);
                } else {
                    out.append(' ');
                }
                continue;
            }

            if (c == '/' && next == '/') {
                out.append(' ').append(' ');
                i++;
                inLineComment = true;
                continue;
            }
            if (c == '/' && next == '*') {
                out.append(' ').append(' ');
                i++;
                inBlockComment = true;
                continue;
            }
            if (c == '\'') {
                out.append(' ');
                inSingleQuote = true;
                continue;
            }
            if (c == '"') {
                out.append(' ');
                inDoubleQuote = true;
                continue;
            }
            if (c == '`') {
                out.append(' ');
                inBacktick = true;
                continue;
            }
            out.append(c);
        }
        return out.toString();
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
            Map<String, Object> properties = QueryDisplayModel.projectNodeProperties(new LinkedHashMap<>(node.getAllProperties()));
            out.put("labels", QueryDisplayModel.displayLabels(stringValue(properties.get("kind")), labels));
            out.put("properties", properties);
            return out;
        }
        if (value instanceof Relationship relationship) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("id", relationship.getId());
            out.put("edge_id", relationship.getProperty("edge_id", relationship.getId()));
            out.put("type", relationship.getType().name());
            out.put("startNodeId", relationship.getStartNode().getProperty("node_id", relationship.getStartNodeId()));
            out.put("endNodeId", relationship.getEndNode().getProperty("node_id", relationship.getEndNodeId()));
            out.put("properties", QueryDisplayModel.projectRelationshipProperties(
                    relationship.getType().name(),
                    new LinkedHashMap<>(relationship.getAllProperties())
            ));
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

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
