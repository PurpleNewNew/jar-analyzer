/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.cypher;

import me.n1ar4.jar.analyzer.gui.swing.cypher.model.SaveScriptRequest;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.ScriptItem;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.ScriptListResponse;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class CypherScriptService {
    private static final Logger logger = LogManager.getLogger();
    private static final Path STORE_FILE = Path.of(Const.dbDir, "cypher-scripts.json");
    private static final Object STORE_LOCK = new Object();

    public ScriptListResponse list() {
        synchronized (STORE_LOCK) {
            List<StoredScript> entities = readStore();
            entities.sort(Comparator.comparingInt(StoredScript::getPinned).reversed()
                    .thenComparingLong(StoredScript::getUpdatedAt).reversed()
                    .thenComparingLong(StoredScript::getScriptId).reversed());
            List<ScriptItem> items = new ArrayList<>();
            if (entities != null) {
                for (StoredScript entity : entities) {
                    if (entity == null) {
                        continue;
                    }
                    items.add(toItem(entity));
                }
            }
            return new ScriptListResponse(items);
        }
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

        synchronized (STORE_LOCK) {
            List<StoredScript> rows = readStore();
            long now = System.currentTimeMillis();
            long nextId = nextId(rows);
            Long scriptId = request.scriptId();
            if (scriptId == null || scriptId <= 0L) {
                StoredScript entity = new StoredScript();
                entity.setScriptId(nextId);
                entity.setTitle(title);
                entity.setBody(body);
                entity.setTags(tags);
                entity.setPinned(pinned);
                entity.setCreatedAt(now);
                entity.setUpdatedAt(now);
                rows.add(entity);
                writeStore(rows);
                return toItem(entity);
            }

            StoredScript existed = null;
            for (StoredScript row : rows) {
                if (row == null) {
                    continue;
                }
                if (row.getScriptId() == scriptId) {
                    existed = row;
                    break;
                }
            }
            if (existed == null) {
                StoredScript entity = new StoredScript();
                entity.setScriptId(nextId);
                entity.setTitle(title);
                entity.setBody(body);
                entity.setTags(tags);
                entity.setPinned(pinned);
                entity.setCreatedAt(now);
                entity.setUpdatedAt(now);
                rows.add(entity);
                writeStore(rows);
                return toItem(entity);
            }

            existed.setTitle(title);
            existed.setBody(body);
            existed.setTags(tags);
            existed.setPinned(pinned);
            existed.setUpdatedAt(now);
            writeStore(rows);
            return toItem(existed);
        }
    }

    public void delete(long scriptId) {
        if (scriptId <= 0L) {
            return;
        }
        synchronized (STORE_LOCK) {
            List<StoredScript> rows = readStore();
            rows.removeIf(item -> item != null && item.getScriptId() == scriptId);
            writeStore(rows);
        }
    }

    private static ScriptItem toItem(StoredScript entity) {
        if (entity == null) {
            return new ScriptItem(0L, "", "", "", false, 0L, 0L);
        }
        return new ScriptItem(
                entity.getScriptId(),
                safe(entity.getTitle()),
                safe(entity.getBody()),
                safe(entity.getTags()),
                entity.getPinned() > 0,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    static Path storeFilePath() {
        return STORE_FILE;
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

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static long nextId(List<StoredScript> rows) {
        long max = 0L;
        if (rows != null) {
            for (StoredScript row : rows) {
                if (row == null) {
                    continue;
                }
                max = Math.max(max, row.getScriptId());
            }
        }
        return max + 1L;
    }

    private static List<StoredScript> readStore() {
        if (!Files.exists(STORE_FILE)) {
            return new ArrayList<>();
        }
        try {
            String content = Files.readString(STORE_FILE, StandardCharsets.UTF_8);
            List<StoredScript> out = JSON.parseObject(content, new TypeReference<List<StoredScript>>() {
            });
            if (out == null) {
                return new ArrayList<>();
            }
            return new ArrayList<>(out);
        } catch (Exception ex) {
            logger.warn("read cypher script store failed: {}", ex.toString());
            return new ArrayList<>();
        }
    }

    private static void writeStore(List<StoredScript> rows) {
        try {
            Files.createDirectories(STORE_FILE.getParent());
            String content = JSON.toJSONString(rows == null ? List.of() : rows);
            Path tmp = STORE_FILE.resolveSibling(STORE_FILE.getFileName() + ".tmp");
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, STORE_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception moveEx) {
                logger.debug("atomic move not available for cypher script store: {}", moveEx.toString());
                Files.move(tmp, STORE_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("write_cypher_script_store_failed", ex);
        }
    }

    private static final class StoredScript {
        private long scriptId;
        private String title;
        private String body;
        private String tags;
        private int pinned;
        private long createdAt;
        private long updatedAt;

        public long getScriptId() {
            return scriptId;
        }

        public void setScriptId(long scriptId) {
            this.scriptId = scriptId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public String getTags() {
            return tags;
        }

        public void setTags(String tags) {
            this.tags = tags;
        }

        public int getPinned() {
            return pinned;
        }

        public void setPinned(int pinned) {
            this.pinned = pinned;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
        }

        public long getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(long updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
}
