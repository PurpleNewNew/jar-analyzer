/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.cypher;

import me.n1ar4.jar.analyzer.core.SqlSessionFactoryUtil;
import me.n1ar4.jar.analyzer.core.mapper.CypherScriptMapper;
import me.n1ar4.jar.analyzer.entity.CypherScriptEntity;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.SaveScriptRequest;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.ScriptItem;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.ScriptListResponse;
import org.apache.ibatis.session.SqlSession;

import java.util.ArrayList;
import java.util.List;

public final class CypherScriptService {
    public ScriptListResponse list() {
        try (SqlSession session = SqlSessionFactoryUtil.sqlSessionFactory.openSession(true)) {
            CypherScriptMapper mapper = session.getMapper(CypherScriptMapper.class);
            List<CypherScriptEntity> entities = mapper.listScripts();
            List<ScriptItem> items = new ArrayList<>();
            if (entities != null) {
                for (CypherScriptEntity entity : entities) {
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

        try (SqlSession session = SqlSessionFactoryUtil.sqlSessionFactory.openSession(true)) {
            CypherScriptMapper mapper = session.getMapper(CypherScriptMapper.class);
            Long scriptId = request.scriptId();
            if (scriptId == null || scriptId <= 0L) {
                CypherScriptEntity entity = new CypherScriptEntity();
                entity.setTitle(title);
                entity.setBody(body);
                entity.setTags(tags);
                entity.setPinned(pinned);
                mapper.insertScript(entity);
                Long lastId = mapper.selectLastInsertRowId();
                if (lastId != null && lastId > 0L) {
                    entity.setScriptId(lastId);
                }
                CypherScriptEntity created = mapper.getScriptById(entity.getScriptId());
                return toItem(created == null ? entity : created);
            }

            CypherScriptEntity existed = mapper.getScriptById(scriptId);
            if (existed == null) {
                CypherScriptEntity entity = new CypherScriptEntity();
                entity.setTitle(title);
                entity.setBody(body);
                entity.setTags(tags);
                entity.setPinned(pinned);
                mapper.insertScript(entity);
                Long lastId = mapper.selectLastInsertRowId();
                if (lastId != null && lastId > 0L) {
                    entity.setScriptId(lastId);
                }
                CypherScriptEntity created = mapper.getScriptById(entity.getScriptId());
                return toItem(created == null ? entity : created);
            }

            existed.setTitle(title);
            existed.setBody(body);
            existed.setTags(tags);
            existed.setPinned(pinned);
            mapper.updateScript(existed);
            CypherScriptEntity updated = mapper.getScriptById(scriptId);
            return toItem(updated == null ? existed : updated);
        }
    }

    public void delete(long scriptId) {
        if (scriptId <= 0L) {
            return;
        }
        try (SqlSession session = SqlSessionFactoryUtil.sqlSessionFactory.openSession(true)) {
            CypherScriptMapper mapper = session.getMapper(CypherScriptMapper.class);
            mapper.deleteScript(scriptId);
        }
    }

    private static ScriptItem toItem(CypherScriptEntity entity) {
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
}
