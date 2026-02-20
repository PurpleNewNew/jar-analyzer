/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.cypher;

import me.n1ar4.jar.analyzer.gui.swing.cypher.model.SaveScriptRequest;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.ScriptItem;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.ScriptListResponse;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jStore;
import me.n1ar4.jar.analyzer.storage.neo4j.repo.Neo4jIdSequenceRepository;
import me.n1ar4.jar.analyzer.storage.neo4j.repo.Neo4jReadRepository;
import me.n1ar4.jar.analyzer.storage.neo4j.repo.Neo4jWriteRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CypherScriptService {
    private final Neo4jStore store = Neo4jStore.getInstance();
    private final Neo4jReadRepository readRepository = new Neo4jReadRepository(store);
    private final Neo4jWriteRepository writeRepository = new Neo4jWriteRepository(store);
    private final Neo4jIdSequenceRepository idSequenceRepository = new Neo4jIdSequenceRepository(store);

    public ScriptListResponse list() {
        List<Map<String, Object>> rows = readRepository.list(
                "MATCH (s:SavedCypher) " +
                        "RETURN s.scriptId AS scriptId, coalesce(s.title,'') AS title, coalesce(s.body,'') AS body, " +
                        "coalesce(s.tags,'') AS tags, coalesce(s.pinned,0) AS pinned, " +
                        "coalesce(s.createdAt,0) AS createdAt, coalesce(s.updatedAt,0) AS updatedAt " +
                        "ORDER BY pinned DESC, updatedAt DESC, scriptId DESC",
                Collections.emptyMap(),
                30_000L
        );
        List<ScriptItem> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            items.add(toItem(row));
        }
        return new ScriptListResponse(items);
    }

    public ScriptItem save(SaveScriptRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("invalid_script_request");
        }
        String body = safe(request.body());
        if (body.isBlank()) {
            throw new IllegalArgumentException("script_body_required");
        }
        String title = safe(request.title());
        if (title.isBlank()) {
            title = deriveTitle(body);
        }
        String tags = safe(request.tags());
        int pinned = request.pinned() ? 1 : 0;

        Long scriptId = request.scriptId();
        long id = scriptId == null || scriptId <= 0 ? idSequenceRepository.next("saved_cypher") : scriptId;

        Map<String, Object> params = new HashMap<>();
        params.put("scriptId", id);
        params.put("title", title);
        params.put("body", body);
        params.put("tags", tags);
        params.put("pinned", pinned);

        writeRepository.run(
                "MERGE (s:SavedCypher {scriptId:$scriptId}) " +
                        "ON CREATE SET s.createdAt = timestamp() " +
                        "SET s.title = $title, s.body = $body, s.tags = $tags, s.pinned = $pinned, s.updatedAt = timestamp()",
                params,
                30_000L
        );

        Map<String, Object> row = readRepository.one(
                "MATCH (s:SavedCypher {scriptId:$scriptId}) " +
                        "RETURN s.scriptId AS scriptId, coalesce(s.title,'') AS title, coalesce(s.body,'') AS body, " +
                        "coalesce(s.tags,'') AS tags, coalesce(s.pinned,0) AS pinned, " +
                        "coalesce(s.createdAt,0) AS createdAt, coalesce(s.updatedAt,0) AS updatedAt LIMIT 1",
                Map.of("scriptId", id),
                30_000L
        );
        return toItem(row);
    }

    public void delete(long scriptId) {
        if (scriptId <= 0L) {
            return;
        }
        writeRepository.run(
                "MATCH (s:SavedCypher {scriptId:$scriptId}) DETACH DELETE s",
                Map.of("scriptId", scriptId),
                30_000L
        );
    }

    private static ScriptItem toItem(Map<String, Object> row) {
        if (row == null || row.isEmpty()) {
            return new ScriptItem(0L, "", "", "", false, 0L, 0L);
        }
        return new ScriptItem(
                asLong(row.get("scriptId"), 0L),
                safe(row.get("title")),
                safe(row.get("body")),
                safe(row.get("tags")),
                asInt(row.get("pinned"), 0) > 0,
                asLong(row.get("createdAt"), 0L),
                asLong(row.get("updatedAt"), 0L)
        );
    }

    private static String deriveTitle(String body) {
        if (body == null || body.isBlank()) {
            return "script";
        }
        String[] lines = body.split("\\R", 2);
        String first = lines.length == 0 ? "" : safe(lines[0]);
        if (first.isBlank()) {
            return "script";
        }
        if (first.length() <= 80) {
            return first;
        }
        return first.substring(0, 80);
    }

    private static String safe(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static long asLong(Object value, long def) {
        if (value == null) {
            return def;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return def;
        }
    }

    private static int asInt(Object value, int def) {
        if (value == null) {
            return def;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return def;
        }
    }
}
