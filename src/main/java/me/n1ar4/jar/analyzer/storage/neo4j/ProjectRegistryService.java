/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.storage.neo4j;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.engine.CFRDecompileEngine;
import me.n1ar4.jar.analyzer.engine.DecompileEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.utils.ClassIndex;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ProjectRegistryService {
    private static final Logger logger = LogManager.getLogger();
    private static final Path REGISTRY_FILE = Paths.get(".jar-analyzer-projects.json");
    private static final ProjectRegistryService INSTANCE = new ProjectRegistryService();

    private final Object lock = new Object();

    private String activeProjectKey = ActiveProjectContext.normalizeProjectKey(null);
    private final List<ProjectRegistryEntry> entries = new ArrayList<>();

    private ProjectRegistryService() {
        load();
    }

    public static ProjectRegistryService getInstance() {
        return INSTANCE;
    }

    public ProjectRegistrySnapshot snapshot() {
        synchronized (lock) {
            List<ProjectRegistryEntry> out = new ArrayList<>(entries);
            out.sort(Comparator.comparing(ProjectRegistryEntry::alias, String.CASE_INSENSITIVE_ORDER));
            String alias = resolveAlias(activeProjectKey);
            return new ProjectRegistrySnapshot(out, activeProjectKey, alias);
        }
    }

    public List<ProjectRegistryEntry> list() {
        return snapshot().projects();
    }

    public ProjectRegistryEntry active() {
        synchronized (lock) {
            ProjectRegistryEntry active = findByKey(activeProjectKey).orElse(null);
            if (active != null) {
                return active;
            }
            return new ProjectRegistryEntry(
                    ActiveProjectContext.normalizeProjectKey(activeProjectKey),
                    ActiveProjectContext.getActiveProjectAlias(),
                    "",
                    "",
                    false,
                    0L,
                    0L
            );
        }
    }

    public ProjectRegistryEntry register(String alias,
                                         String inputPath,
                                         String runtimePath,
                                         boolean resolveNestedJars) {
        String normalizedInput = normalizePath(inputPath);
        if (normalizedInput.isBlank()) {
            throw new IllegalArgumentException("project_input_required");
        }
        String projectKey = buildProjectKey(normalizedInput);
        String effectiveAlias = normalizeAlias(alias, normalizedInput);
        long now = System.currentTimeMillis();
        ProjectRegistryEntry next;
        String previousProjectKey;
        String currentProjectKey;
        synchronized (lock) {
            next = null;
            for (int i = 0; i < entries.size(); i++) {
                ProjectRegistryEntry current = entries.get(i);
                if (!Objects.equals(current.projectKey(), projectKey)) {
                    continue;
                }
                next = new ProjectRegistryEntry(
                        current.projectKey(),
                        effectiveAlias,
                        normalizedInput,
                        normalizePath(runtimePath),
                        resolveNestedJars,
                        current.createdAt() <= 0L ? now : current.createdAt(),
                        now
                );
                entries.set(i, next);
                break;
            }
            if (next == null) {
                next = new ProjectRegistryEntry(
                        projectKey,
                        effectiveAlias,
                        normalizedInput,
                        normalizePath(runtimePath),
                        resolveNestedJars,
                        now,
                        now
                );
                entries.add(next);
            }
            previousProjectKey = activeProjectKey;
            setActiveLocked(next.projectKey(), next.alias());
            currentProjectKey = activeProjectKey;
            persistLocked();
        }
        if (!Objects.equals(previousProjectKey, currentProjectKey)) {
            onActiveProjectChanged(previousProjectKey, currentProjectKey);
        }
        return next;
    }

    public ProjectRegistryEntry switchActive(String projectKey) {
        String normalized = ActiveProjectContext.normalizeProjectKey(projectKey);
        ProjectRegistryEntry entry;
        String previousProjectKey;
        String currentProjectKey;
        synchronized (lock) {
            entry = findByKey(normalized).orElse(null);
            if (entry == null) {
                throw new IllegalArgumentException("project_not_found");
            }
            previousProjectKey = activeProjectKey;
            setActiveLocked(entry.projectKey(), entry.alias());
            currentProjectKey = activeProjectKey;
            persistLocked();
        }
        if (!Objects.equals(previousProjectKey, currentProjectKey)) {
            onActiveProjectChanged(previousProjectKey, currentProjectKey);
        }
        return entry;
    }

    public boolean remove(String projectKey, boolean deleteStore) {
        String normalized = ActiveProjectContext.normalizeProjectKey(projectKey);
        boolean removedFlag;
        String previousProjectKey;
        String currentProjectKey;
        synchronized (lock) {
            int index = -1;
            for (int i = 0; i < entries.size(); i++) {
                if (Objects.equals(entries.get(i).projectKey(), normalized)) {
                    index = i;
                    break;
                }
            }
            if (index < 0) {
                return false;
            }
            ProjectRegistryEntry removed = entries.remove(index);
            previousProjectKey = activeProjectKey;
            if (deleteStore) {
                Neo4jProjectStore.getInstance().deleteProjectStore(removed.projectKey());
            }
            if (Objects.equals(activeProjectKey, removed.projectKey())) {
                if (!entries.isEmpty()) {
                    ProjectRegistryEntry first = entries.get(0);
                    setActiveLocked(first.projectKey(), first.alias());
                } else {
                    setActiveLocked(ActiveProjectContext.normalizeProjectKey(null), "default");
                }
            }
            currentProjectKey = activeProjectKey;
            persistLocked();
            removedFlag = true;
        }
        if (!Objects.equals(previousProjectKey, currentProjectKey)) {
            onActiveProjectChanged(previousProjectKey, currentProjectKey);
        }
        return removedFlag;
    }

    public static String buildProjectKey(String normalizedInputPath) {
        String safeInput = normalizedInputPath == null ? "" : normalizedInputPath.trim();
        if (safeInput.isBlank()) {
            return ActiveProjectContext.normalizeProjectKey(null);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(safeInput.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (Exception ex) {
            int hash = Math.abs(safeInput.hashCode());
            return Integer.toHexString(hash);
        }
    }

    private void load() {
        synchronized (lock) {
            entries.clear();
            activeProjectKey = ActiveProjectContext.normalizeProjectKey(null);
            if (!Files.exists(REGISTRY_FILE)) {
                ActiveProjectContext.setActiveProject(activeProjectKey, "default");
                return;
            }
            try {
                String raw = Files.readString(REGISTRY_FILE);
                JSONObject obj = JSON.parseObject(raw);
                if (obj == null) {
                    ActiveProjectContext.setActiveProject(activeProjectKey, "default");
                    return;
                }
                String storedActive = safe(obj.getString("activeProjectKey"));
                if (!storedActive.isBlank()) {
                    activeProjectKey = ActiveProjectContext.normalizeProjectKey(storedActive);
                }
                JSONArray arr = obj.getJSONArray("projects");
                if (arr != null) {
                    for (int i = 0; i < arr.size(); i++) {
                        JSONObject item = arr.getJSONObject(i);
                        if (item == null) {
                            continue;
                        }
                        ProjectRegistryEntry entry = new ProjectRegistryEntry(
                                safe(item.getString("projectKey")),
                                safe(item.getString("alias")),
                                safe(item.getString("inputPath")),
                                safe(item.getString("runtimePath")),
                                item.getBooleanValue("resolveNestedJars"),
                                item.getLongValue("createdAt"),
                                item.getLongValue("updatedAt")
                        );
                        if (entry.projectKey().isBlank()) {
                            continue;
                        }
                        entries.add(entry);
                    }
                }
            } catch (Exception ex) {
                logger.warn("load project registry fail: {}", ex.toString());
            }
            String alias = resolveAlias(activeProjectKey);
            ActiveProjectContext.setActiveProject(activeProjectKey, alias.isBlank() ? "default" : alias);
        }
    }

    private void persistLocked() {
        try {
            JSONObject root = new JSONObject();
            root.put("activeProjectKey", activeProjectKey);
            JSONArray arr = new JSONArray();
            for (ProjectRegistryEntry entry : entries) {
                JSONObject row = new JSONObject();
                row.put("projectKey", entry.projectKey());
                row.put("alias", entry.alias());
                row.put("inputPath", entry.inputPath());
                row.put("runtimePath", entry.runtimePath());
                row.put("resolveNestedJars", entry.resolveNestedJars());
                row.put("createdAt", entry.createdAt());
                row.put("updatedAt", entry.updatedAt());
                arr.add(row);
            }
            root.put("projects", arr);
            Files.writeString(REGISTRY_FILE, JSON.toJSONString(root), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            logger.warn("save project registry fail: {}", ex.toString());
        }
    }

    private void setActiveLocked(String projectKey, String alias) {
        activeProjectKey = ActiveProjectContext.normalizeProjectKey(projectKey);
        String effectiveAlias = alias == null || alias.isBlank() ? resolveAlias(activeProjectKey) : alias.trim();
        if (effectiveAlias.isBlank()) {
            effectiveAlias = activeProjectKey;
        }
        ActiveProjectContext.setActiveProject(activeProjectKey, effectiveAlias);
    }

    private void onActiveProjectChanged(String previousProjectKey, String nextProjectKey) {
        try {
            DatabaseManager.clearAllData();
        } catch (Exception ex) {
            logger.debug("clear legacy runtime cache fail: {}", ex.toString());
        }
        try {
            WorkspaceContext.clear();
        } catch (Exception ex) {
            logger.debug("clear workspace context fail: {}", ex.toString());
        }
        try {
            DecompileEngine.cleanCache();
        } catch (Exception ex) {
            logger.debug("clean fernflower cache fail: {}", ex.toString());
        }
        try {
            CFRDecompileEngine.cleanCache();
        } catch (Exception ex) {
            logger.debug("clean cfr cache fail: {}", ex.toString());
        }
        try {
            ClassIndex.refresh();
        } catch (Exception ex) {
            logger.debug("refresh class index fail: {}", ex.toString());
        }
        try {
            var engine = EngineContext.getEngine();
            if (engine != null) {
                engine.clearCallGraphCache();
            }
            EngineContext.setEngine(null);
        } catch (Exception ex) {
            logger.debug("clear core engine cache fail: {}", ex.toString());
        }
        logger.info("project switched: {} -> {} (runtime cache invalidated)",
                safe(previousProjectKey), safe(nextProjectKey));
    }

    private Optional<ProjectRegistryEntry> findByKey(String projectKey) {
        if (projectKey == null || projectKey.isBlank()) {
            return Optional.empty();
        }
        for (ProjectRegistryEntry entry : entries) {
            if (Objects.equals(entry.projectKey(), projectKey)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    private String resolveAlias(String projectKey) {
        if (projectKey == null || projectKey.isBlank()) {
            return "";
        }
        for (ProjectRegistryEntry entry : entries) {
            if (Objects.equals(entry.projectKey(), projectKey)) {
                return entry.alias();
            }
        }
        return "";
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        try {
            Path p = Paths.get(path.trim()).toAbsolutePath().normalize();
            return p.toString();
        } catch (Exception ex) {
            return path.trim();
        }
    }

    private static String normalizeAlias(String alias, String normalizedInput) {
        String safeAlias = alias == null ? "" : alias.trim();
        if (!safeAlias.isBlank()) {
            return safeAlias;
        }
        if (normalizedInput == null || normalizedInput.isBlank()) {
            return "project";
        }
        try {
            Path p = Paths.get(normalizedInput);
            Path fileName = p.getFileName();
            if (fileName != null) {
                String value = fileName.toString().trim();
                if (!value.isBlank()) {
                    return value;
                }
            }
        } catch (Exception ignored) {
        }
        return "project";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
