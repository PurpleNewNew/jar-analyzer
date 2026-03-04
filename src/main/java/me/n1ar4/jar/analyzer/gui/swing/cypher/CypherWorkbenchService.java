/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.cypher;

import me.n1ar4.jar.analyzer.graph.query.QueryOptions;
import me.n1ar4.jar.analyzer.graph.query.QueryResult;
import me.n1ar4.jar.analyzer.graph.query.QueryServices;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.DeleteScriptRequest;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.ExplainRequest;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.ExplainResponse;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.QueryFrameRequest;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.QueryFrameResponse;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.SaveScriptRequest;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.ScriptItem;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.ScriptListResponse;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.UiContextResponse;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class CypherWorkbenchService implements CypherWorkbenchBridge {
    private static final int FRAME_RING_LIMIT = 50;

    private final GraphStore graphStore;
    private final QueryResultProjector projector;
    private final CypherScriptService scriptService;
    private final Deque<String> frameIds = new ArrayDeque<>();
    private final AtomicReference<String> language = new AtomicReference<>("zh");
    private final AtomicReference<String> theme = new AtomicReference<>("default");
    private final AtomicReference<Consumer<Boolean>> fullscreenConsumer = new AtomicReference<>();

    public CypherWorkbenchService() {
        this(new GraphStore(), new QueryResultProjector(), new CypherScriptService());
    }

    CypherWorkbenchService(GraphStore graphStore,
                           QueryResultProjector projector,
                           CypherScriptService scriptService) {
        this.graphStore = graphStore == null ? new GraphStore() : graphStore;
        this.projector = projector == null ? new QueryResultProjector() : projector;
        this.scriptService = scriptService == null ? new CypherScriptService() : scriptService;
    }

    public void updateUiContext(String language, String theme) {
        this.language.set(normalizeLanguage(language));
        this.theme.set(normalizeTheme(theme));
    }

    public void setFullscreenConsumer(Consumer<Boolean> consumer) {
        this.fullscreenConsumer.set(consumer);
    }

    @Override
    public QueryFrameResponse execute(QueryFrameRequest request) {
        if (request == null || safe(request.query()).isBlank()) {
            return QueryFrameResponse.error("cypher_empty_query", "query required");
        }
        String query = safe(request.query());
        Map<String, Object> params = request.params() == null ? Map.of() : request.params();
        Map<String, Object> optionMap = request.options() == null ? Map.of() : request.options();
        QueryOptions options = QueryOptions.fromMap(optionMap);

        long start = System.nanoTime();
        try {
            QueryResult result = QueryServices.cypher().execute(query, params, options);
            GraphSnapshot snapshot = graphStore.loadSnapshot();
            long elapsedMs = Math.max(0L, (System.nanoTime() - start) / 1_000_000L);
            String frameId = "f-" + UUID.randomUUID();
            rememberFrame(frameId);
            return QueryFrameResponse.ok(projector.toFrame(frameId, query, result, elapsedMs, snapshot));
        } catch (IllegalArgumentException ex) {
            String message = safe(ex.getMessage());
            if (message.startsWith("cypher_feature_not_supported")) {
                return QueryFrameResponse.error("cypher_feature_not_supported", message);
            }
            if (message.startsWith("cypher_parse_error")) {
                return QueryFrameResponse.error("cypher_parse_error", message);
            }
            if (message.startsWith("cypher_query_timeout")) {
                return QueryFrameResponse.error("cypher_query_timeout", message);
            }
            if (message.startsWith("cypher_expand_budget_exceeded")) {
                return QueryFrameResponse.error("cypher_expand_budget_exceeded", message);
            }
            if (message.startsWith("cypher_path_budget_exceeded")) {
                return QueryFrameResponse.error("cypher_path_budget_exceeded", message);
            }
            return QueryFrameResponse.error("cypher_query_invalid", message.isBlank() ? "cypher query invalid" : message);
        } catch (Throwable ex) {
            String message = safe(ex.getMessage());
            return QueryFrameResponse.error("cypher_query_error", message.isBlank() ? "cypher query failed" : message);
        }
    }

    @Override
    public ExplainResponse explain(ExplainRequest request) {
        String query = request == null ? "" : safe(request.query());
        if (query.isBlank()) {
            throw new IllegalArgumentException("cypher_empty_query");
        }
        try {
            return new ExplainResponse(QueryServices.cypher().explain(query));
        } catch (IllegalArgumentException ex) {
            String message = safe(ex.getMessage());
            String code = resolveQueryErrorCode(message);
            String normalized = message.isBlank() ? code : code + ": " + message;
            throw new IllegalArgumentException(normalized, ex);
        } catch (Exception ex) {
            String message = safe(ex.getMessage());
            String code = message.toLowerCase().contains("timeout")
                    ? "cypher_query_timeout"
                    : "cypher_query_invalid";
            String normalized = message.isBlank() ? code : code + ": " + message;
            throw new IllegalArgumentException(normalized, ex);
        }
    }

    @Override
    public Map<String, Object> capabilities() {
        return QueryServices.cypher().capabilities();
    }

    @Override
    public ScriptListResponse listScripts() {
        return scriptService.list();
    }

    @Override
    public ScriptItem saveScript(SaveScriptRequest request) {
        return scriptService.save(request);
    }

    @Override
    public void deleteScript(DeleteScriptRequest request) {
        if (request == null || request.scriptId() <= 0L) {
            return;
        }
        scriptService.delete(request.scriptId());
    }

    @Override
    public UiContextResponse uiContext() {
        return new UiContextResponse(language.get(), theme.get());
    }

    @Override
    public void requestFullscreen(boolean fullscreen) {
        Consumer<Boolean> callback = fullscreenConsumer.get();
        if (callback != null) {
            callback.accept(fullscreen);
        }
    }

    public List<String> recentFrameIds() {
        synchronized (frameIds) {
            return List.copyOf(frameIds);
        }
    }

    private void rememberFrame(String frameId) {
        if (frameId == null || frameId.isBlank()) {
            return;
        }
        synchronized (frameIds) {
            frameIds.remove(frameId);
            frameIds.addFirst(frameId);
            while (frameIds.size() > FRAME_RING_LIMIT) {
                frameIds.removeLast();
            }
        }
    }

    private static String normalizeLanguage(String value) {
        return "en".equalsIgnoreCase(safe(value)) ? "en" : "zh";
    }

    private static String normalizeTheme(String value) {
        return "dark".equalsIgnoreCase(safe(value)) ? "dark" : "default";
    }

    private static String resolveQueryErrorCode(String message) {
        String text = safe(message);
        if (text.startsWith("cypher_feature_not_supported")) {
            return "cypher_feature_not_supported";
        }
        if (text.startsWith("cypher_parse_error")) {
            return "cypher_parse_error";
        }
        if (text.startsWith("cypher_empty_query")) {
            return "cypher_empty_query";
        }
        if (text.startsWith("cypher_query_timeout")) {
            return "cypher_query_timeout";
        }
        if (text.startsWith("cypher_expand_budget_exceeded")) {
            return "cypher_expand_budget_exceeded";
        }
        if (text.startsWith("cypher_path_budget_exceeded")) {
            return "cypher_path_budget_exceeded";
        }
        if (text.startsWith("project_model_missing_rebuild")
                || text.startsWith("graph_snapshot_missing_rebuild")
                || text.startsWith("graph_snapshot_load_failed")
                || text.startsWith("neo4j_store_open_fail")) {
            return "project_model_missing_rebuild";
        }
        return "cypher_query_invalid";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
