/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.query;

public final class QueryErrorClassifier {
    public static final String CYPHER_FEATURE_NOT_SUPPORTED = "cypher_feature_not_supported";
    public static final String CYPHER_PARSE_ERROR = "cypher_parse_error";
    public static final String CYPHER_EMPTY_QUERY = "cypher_empty_query";
    public static final String CYPHER_QUERY_TIMEOUT = "cypher_query_timeout";
    public static final String CYPHER_EXPAND_BUDGET_EXCEEDED = "cypher_expand_budget_exceeded";
    public static final String CYPHER_PATH_BUDGET_EXCEEDED = "cypher_path_budget_exceeded";
    public static final String CYPHER_QUERY_INVALID = "cypher_query_invalid";
    public static final String CYPHER_QUERY_ERROR = "cypher_query_error";
    public static final String PROJECT_BUILD_IN_PROGRESS = "project_build_in_progress";
    public static final String PROJECT_MODEL_MISSING_REBUILD = "project_model_missing_rebuild";

    private static final String GRAPH_SNAPSHOT_MISSING_REBUILD = "graph_snapshot_missing_rebuild";
    private static final String GRAPH_SNAPSHOT_LOAD_FAILED = "graph_snapshot_load_failed";
    private static final String GRAPH_STORE_OPEN_FAIL = "graph_store_open_fail";
    private static final String PROJECT_NOT_READY_MESSAGE = "active project is not built, rebuild required";
    private static final String PROJECT_BUILDING_MESSAGE = "active project build in progress";

    private QueryErrorClassifier() {
    }

    public static String codeOf(String message) {
        String text = safe(message);
        if (text.startsWith(CYPHER_FEATURE_NOT_SUPPORTED)) {
            return CYPHER_FEATURE_NOT_SUPPORTED;
        }
        if (text.startsWith(CYPHER_PARSE_ERROR)) {
            return CYPHER_PARSE_ERROR;
        }
        if (text.startsWith(CYPHER_EMPTY_QUERY)) {
            return CYPHER_EMPTY_QUERY;
        }
        if (text.startsWith(CYPHER_QUERY_TIMEOUT)) {
            return CYPHER_QUERY_TIMEOUT;
        }
        if (text.startsWith(CYPHER_EXPAND_BUDGET_EXCEEDED)) {
            return CYPHER_EXPAND_BUDGET_EXCEEDED;
        }
        if (text.startsWith(CYPHER_PATH_BUDGET_EXCEEDED)) {
            return CYPHER_PATH_BUDGET_EXCEEDED;
        }
        if (text.startsWith(PROJECT_BUILD_IN_PROGRESS)) {
            return PROJECT_BUILD_IN_PROGRESS;
        }
        if (text.startsWith(PROJECT_MODEL_MISSING_REBUILD)
                || text.startsWith(GRAPH_SNAPSHOT_MISSING_REBUILD)
                || text.startsWith(GRAPH_SNAPSHOT_LOAD_FAILED)
                || text.startsWith(GRAPH_STORE_OPEN_FAIL)) {
            return PROJECT_MODEL_MISSING_REBUILD;
        }
        return CYPHER_QUERY_INVALID;
    }

    public static boolean isProjectBuilding(String message) {
        return PROJECT_BUILD_IN_PROGRESS.equals(codeOf(message));
    }

    public static boolean isProjectNotReady(String message) {
        return PROJECT_MODEL_MISSING_REBUILD.equals(codeOf(message));
    }

    public static String publicMessage(String message, String fallback) {
        String code = codeOf(message);
        if (PROJECT_BUILD_IN_PROGRESS.equals(code)) {
            return PROJECT_BUILDING_MESSAGE;
        }
        if (PROJECT_MODEL_MISSING_REBUILD.equals(code)) {
            return PROJECT_NOT_READY_MESSAGE;
        }
        String text = safe(message);
        if (!text.isBlank()) {
            return text;
        }
        return safe(fallback);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
