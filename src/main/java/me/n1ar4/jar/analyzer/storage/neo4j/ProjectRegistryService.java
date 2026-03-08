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
import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.CFRDecompileEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.ClassIndex;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ProjectRegistryService {
    private static final Logger logger = LogManager.getLogger();
    private static final Path REGISTRY_FILE = Paths.get(".jar-analyzer-projects.json");
    private static final ProjectRegistryService INSTANCE = new ProjectRegistryService();

    private final Object lock = new Object();

    private String activeProjectKey = ActiveProjectContext.temporaryProjectKey();
    private final List<ProjectRegistryEntry> entries = new ArrayList<>();

    private String tempInputPath = "";
    private String tempRuntimePath = "";
    private boolean tempResolveNestedJars = false;
    private long tempUpdatedAt = 0L;

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
            if (ActiveProjectContext.isTemporaryProjectKey(activeProjectKey)) {
                return temporaryEntryLocked();
            }
            ProjectRegistryEntry active = findByKey(activeProjectKey).orElse(null);
            if (active != null) {
                return active;
            }
            return temporaryEntryLocked();
        }
    }

    public ProjectRegistryEntry register(String alias,
                                         String inputPath,
                                         String runtimePath,
                                         boolean resolveNestedJars) {
        ensureNoBuildInProgress();
        synchronized (ActiveProjectContext.mutationLock()) {
            ensureNoBuildInProgress();
            String normalizedInput = normalizePath(inputPath);
            String normalizedRuntime = normalizePath(runtimePath);
            if (normalizedInput.isBlank()) {
                throw new IllegalArgumentException("project_input_required");
            }
            String projectKey = buildProjectKey(normalizedInput, normalizedRuntime, resolveNestedJars);
            String effectiveAlias = normalizeAlias(alias, normalizedInput);
            long now = System.currentTimeMillis();
            ProjectRegistryEntry next;
            synchronized (lock) {
                ProjectRegistryEntry current = findByKey(projectKey).orElse(null);
                next = new ProjectRegistryEntry(
                        projectKey,
                        ProjectType.PERSISTENT,
                        effectiveAlias,
                        normalizedInput,
                        normalizedRuntime,
                        resolveNestedJars,
                        current == null || current.createdAt() <= 0L ? now : current.createdAt(),
                        now
                );
            }
            ensureProjectStore(next.projectKey());
            String previousProjectKey;
            synchronized (lock) {
                upsertEntryLocked(next);
                previousProjectKey = activeProjectKey;
            }
            if (!Objects.equals(previousProjectKey, next.projectKey())) {
                onActiveProjectChanged(previousProjectKey, next.projectKey(), next.alias());
            } else {
                synchronized (lock) {
                    setActiveStateLocked(next.projectKey(), next.alias());
                }
            }
            synchronized (lock) {
                persistLocked();
            }
            return next;
        }
    }

    public ProjectRegistryEntry activateTemporaryProject() {
        ensureNoBuildInProgress();
        synchronized (ActiveProjectContext.mutationLock()) {
            ensureNoBuildInProgress();
            String projectKey = ActiveProjectContext.temporaryProjectKey();
            ensureProjectStore(projectKey);
            String previousProjectKey;
            ProjectRegistryEntry temporary;
            synchronized (lock) {
                previousProjectKey = activeProjectKey;
                temporary = temporaryEntryLocked();
            }
            if (!Objects.equals(previousProjectKey, projectKey)) {
                onActiveProjectChanged(previousProjectKey, projectKey, ActiveProjectContext.temporaryProjectAlias());
            } else {
                synchronized (lock) {
                    setActiveStateLocked(projectKey, ActiveProjectContext.temporaryProjectAlias());
                }
            }
            synchronized (lock) {
                persistLocked();
            }
            return temporary;
        }
    }

    public void cleanupTemporaryProject() {
        ensureNoBuildInProgress();
        synchronized (ActiveProjectContext.mutationLock()) {
            ensureNoBuildInProgress();
            String temporaryKey = ActiveProjectContext.temporaryProjectKey();
            synchronized (lock) {
                tempInputPath = "";
                tempRuntimePath = "";
                tempResolveNestedJars = false;
                tempUpdatedAt = 0L;
                if (Objects.equals(activeProjectKey, temporaryKey)) {
                    setActiveStateLocked(temporaryKey, ActiveProjectContext.temporaryProjectAlias());
                }
                persistLocked();
            }
            boolean refreshActiveTemp = false;
            synchronized (lock) {
                refreshActiveTemp = Objects.equals(activeProjectKey, temporaryKey);
            }
            if (refreshActiveTemp) {
                refreshActiveProjectInPlace(
                        temporaryKey,
                        ActiveProjectContext.temporaryProjectAlias(),
                        () -> {
                            try {
                                Neo4jProjectStore.getInstance().deleteProjectStore(temporaryKey);
                            } catch (Exception ex) {
                                logger.debug("cleanup temporary project store fail: {}", ex.toString());
                            }
                        },
                        "cleanup_temporary_project"
                );
            } else {
                try {
                    Neo4jProjectStore.getInstance().deleteProjectStore(temporaryKey);
                } catch (Exception ex) {
                    logger.debug("cleanup temporary project store fail: {}", ex.toString());
                }
            }
        }
    }

    public ProjectRegistryEntry createProject(String alias) {
        ensureNoBuildInProgress();
        synchronized (ActiveProjectContext.mutationLock()) {
            ensureNoBuildInProgress();
            long now = System.currentTimeMillis();
            ProjectRegistryEntry next;
            synchronized (lock) {
                String projectKey = nextProjectKeyLocked();
                String effectiveAlias = safe(alias);
                if (effectiveAlias.isBlank()) {
                    effectiveAlias = "project-" + projectKey;
                }
                next = new ProjectRegistryEntry(
                        projectKey,
                        ProjectType.PERSISTENT,
                        effectiveAlias,
                        "",
                        "",
                        false,
                        now,
                        now
                );
            }
            ensureProjectStore(next.projectKey());
            String previousProjectKey;
            synchronized (lock) {
                entries.add(next);
                previousProjectKey = activeProjectKey;
            }
            if (!Objects.equals(previousProjectKey, next.projectKey())) {
                onActiveProjectChanged(previousProjectKey, next.projectKey(), next.alias());
            } else {
                synchronized (lock) {
                    setActiveStateLocked(next.projectKey(), next.alias());
                }
            }
            synchronized (lock) {
                persistLocked();
            }
            return next;
        }
    }

    public ProjectRegistryEntry upsertActiveProjectBuildSettings(String alias,
                                                                 String inputPath,
                                                                 String runtimePath,
                                                                 boolean resolveNestedJars) {
        ensureNoBuildInProgress();
        synchronized (ActiveProjectContext.mutationLock()) {
            ensureNoBuildInProgress();
            String normalizedInput = normalizePath(inputPath);
            String normalizedRuntime = normalizePath(runtimePath);
            long now = System.currentTimeMillis();
            ProjectRegistryEntry next;
            String currentProjectKey;
            synchronized (lock) {
                currentProjectKey = ActiveProjectContext.resolveRequestedOrActive(activeProjectKey);
            }
            ensureProjectStore(currentProjectKey);
            synchronized (lock) {
                if (ActiveProjectContext.isTemporaryProjectKey(currentProjectKey)) {
                    tempInputPath = normalizedInput;
                    tempRuntimePath = normalizedRuntime;
                    tempResolveNestedJars = resolveNestedJars;
                    tempUpdatedAt = now;
                    setActiveStateLocked(currentProjectKey, ActiveProjectContext.temporaryProjectAlias());
                    persistLocked();
                    next = temporaryEntryLocked();
                } else {
                    ProjectRegistryEntry current = findByKey(currentProjectKey).orElse(null);
                    String effectiveAlias = safe(alias);
                    if (effectiveAlias.isBlank() && current != null) {
                        effectiveAlias = current.alias();
                    }
                    if (effectiveAlias.isBlank()) {
                        effectiveAlias = normalizeAlias("", normalizedInput);
                    }
                    if (effectiveAlias.isBlank()) {
                        effectiveAlias = currentProjectKey;
                    }
                    next = new ProjectRegistryEntry(
                            currentProjectKey,
                            ProjectType.PERSISTENT,
                            effectiveAlias,
                            normalizedInput,
                            normalizedRuntime,
                            resolveNestedJars,
                            current == null ? now : (current.createdAt() <= 0L ? now : current.createdAt()),
                            now
                    );
                    upsertEntryLocked(next);
                    setActiveStateLocked(currentProjectKey, effectiveAlias);
                    persistLocked();
                }
            }
            return next;
        }
    }

    public ProjectRegistryEntry switchActive(String projectKey) {
        ensureNoBuildInProgress();
        synchronized (ActiveProjectContext.mutationLock()) {
            ensureNoBuildInProgress();
            String normalized = ActiveProjectContext.normalizeProjectKey(projectKey);
            if (normalized.isBlank()) {
                throw new IllegalArgumentException("project_not_found");
            }
            if (ActiveProjectContext.isTemporaryProjectKey(normalized)) {
                return activateTemporaryProject();
            }
            ProjectRegistryEntry entry;
            String previousProjectKey;
            synchronized (lock) {
                entry = findByKey(normalized).orElse(null);
                if (entry == null) {
                    throw new IllegalArgumentException("project_not_found");
                }
                previousProjectKey = activeProjectKey;
            }
            ensureProjectStore(entry.projectKey());
            if (!Objects.equals(previousProjectKey, entry.projectKey())) {
                onActiveProjectChanged(previousProjectKey, entry.projectKey(), entry.alias());
            } else {
                synchronized (lock) {
                    setActiveStateLocked(entry.projectKey(), entry.alias());
                }
            }
            synchronized (lock) {
                persistLocked();
            }
            return entry;
        }
    }

    public boolean remove(String projectKey, boolean deleteStore) {
        ensureNoBuildInProgress();
        synchronized (ActiveProjectContext.mutationLock()) {
            ensureNoBuildInProgress();
            String normalized = ActiveProjectContext.normalizeProjectKey(projectKey);
            if (normalized.isBlank()) {
                return false;
            }
            if (ActiveProjectContext.isTemporaryProjectKey(normalized)) {
                if (deleteStore) {
                    cleanupTemporaryProject();
                }
                return true;
            }
            String nextActiveProjectKey = "";
            String nextActiveAlias = "";
            synchronized (lock) {
                if (findByKey(normalized).isEmpty()) {
                    return false;
                }
                if (Objects.equals(activeProjectKey, normalized)) {
                    ProjectRegistryEntry first = null;
                    for (ProjectRegistryEntry entry : entries) {
                        if (entry == null || Objects.equals(entry.projectKey(), normalized)) {
                            continue;
                        }
                        first = entry;
                        break;
                    }
                    if (first != null) {
                        nextActiveProjectKey = first.projectKey();
                        nextActiveAlias = first.alias();
                    } else {
                        nextActiveProjectKey = ActiveProjectContext.temporaryProjectKey();
                        nextActiveAlias = ActiveProjectContext.temporaryProjectAlias();
                    }
                }
            }
            if (!nextActiveProjectKey.isBlank() && !Objects.equals(normalized, nextActiveProjectKey)) {
                ensureProjectStore(nextActiveProjectKey);
            }
            boolean removedFlag;
            String previousProjectKey;
            String removedProjectKey = "";
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
                removedProjectKey = removed.projectKey();
                previousProjectKey = activeProjectKey;
                removedFlag = true;
            }
            if (!nextActiveProjectKey.isBlank() && !Objects.equals(previousProjectKey, nextActiveProjectKey)) {
                onActiveProjectChanged(previousProjectKey, nextActiveProjectKey, nextActiveAlias);
            } else if (!nextActiveProjectKey.isBlank()) {
                synchronized (lock) {
                    setActiveStateLocked(nextActiveProjectKey, nextActiveAlias);
                }
            }
            synchronized (lock) {
                persistLocked();
            }
            if (deleteStore && !removedProjectKey.isBlank()) {
                try {
                    Neo4jProjectStore.getInstance().deleteProjectStore(removedProjectKey);
                } catch (Exception ex) {
                    logger.debug("delete removed project store fail: key={} err={}",
                            safe(removedProjectKey), ex.toString());
                }
            }
            return removedFlag;
        }
    }

    public static String buildProjectKey(String normalizedInputPath) {
        return buildProjectKey(normalizedInputPath, "", false);
    }

    public static String buildProjectKey(String normalizedInputPath,
                                         String normalizedRuntimePath,
                                         boolean resolveNestedJars) {
        String safeInput = normalizedInputPath == null ? "" : normalizedInputPath.trim();
        if (safeInput.isBlank()) {
            return "";
        }
        String safeRuntime = normalizedRuntimePath == null ? "" : normalizedRuntimePath.trim();
        String signature = (safeRuntime.isBlank() && !resolveNestedJars)
                ? safeInput
                : safeInput + "\n" + safeRuntime + "\n" + resolveNestedJars;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(signature.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (Exception ex) {
            int hash = Math.abs(signature.hashCode());
            return Integer.toHexString(hash);
        }
    }

    private void load() {
        List<ProjectRegistryEntry> loadedEntries = new ArrayList<>();
        boolean fileExists = Files.exists(REGISTRY_FILE);
        boolean parsed = !fileExists;
        if (fileExists) {
            try {
                String raw = Files.readString(REGISTRY_FILE);
                JSONObject obj = JSON.parseObject(raw);
                JSONArray arr = obj == null ? null : obj.getJSONArray("projects");
                if (arr != null) {
                    for (int i = 0; i < arr.size(); i++) {
                        JSONObject item = arr.getJSONObject(i);
                        if (item == null) {
                            continue;
                        }
                        String projectKey = safe(item.getString("projectKey"));
                        if (projectKey.isBlank() || ActiveProjectContext.isTemporaryProjectKey(projectKey)
                                || "default".equalsIgnoreCase(projectKey)) {
                            continue;
                        }
                        ProjectType type = ProjectType.fromValue(item.getString("type"));
                        if (type == ProjectType.TEMP) {
                            continue;
                        }
                        loadedEntries.add(new ProjectRegistryEntry(
                                projectKey,
                                ProjectType.PERSISTENT,
                                safe(item.getString("alias")),
                                safe(item.getString("inputPath")),
                                safe(item.getString("runtimePath")),
                                item.getBooleanValue("resolveNestedJars"),
                                item.getLongValue("createdAt"),
                                item.getLongValue("updatedAt")
                        ));
                    }
                }
                parsed = true;
            } catch (Exception ex) {
                logger.warn("load project registry fail: {}", ex.toString());
            }
        }
        boolean preserveCurrent = fileExists && !parsed;
        synchronized (lock) {
            if (preserveCurrent) {
                ActiveProjectContext.setActiveProject(activeProjectKey, resolveAlias(activeProjectKey));
            } else {
                entries.clear();
                entries.addAll(loadedEntries);
                activeProjectKey = ActiveProjectContext.temporaryProjectKey();
                tempInputPath = "";
                tempRuntimePath = "";
                tempResolveNestedJars = false;
                tempUpdatedAt = 0L;
                ActiveProjectContext.setActiveProject(activeProjectKey, ActiveProjectContext.temporaryProjectAlias());
            }
        }
        try {
            ensureProjectStore(activeProjectKey);
        } catch (Exception ex) {
            logger.warn("initialize active project store fail: key={} err={}",
                    safe(activeProjectKey), ex.toString());
        }
    }

    private void persistLocked() {
        try {
            JSONObject root = new JSONObject();
            JSONArray arr = new JSONArray();
            for (ProjectRegistryEntry entry : entries) {
                if (entry == null || entry.type() != ProjectType.PERSISTENT) {
                    continue;
                }
                JSONObject row = new JSONObject();
                row.put("projectKey", entry.projectKey());
                row.put("type", entry.type().name());
                row.put("alias", entry.alias());
                row.put("inputPath", entry.inputPath());
                row.put("runtimePath", entry.runtimePath());
                row.put("resolveNestedJars", entry.resolveNestedJars());
                row.put("createdAt", entry.createdAt());
                row.put("updatedAt", entry.updatedAt());
                arr.add(row);
            }
            root.put("projects", arr);
            String json = JSON.toJSONString(root);
            Path target = REGISTRY_FILE.toAbsolutePath().normalize();
            Path temp = target.resolveSibling(target.getFileName() + ".tmp");
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(temp, json, StandardCharsets.UTF_8);
            try {
                Files.move(temp, target,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception ex) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ex) {
            logger.error("save project registry fail: {}", ex.toString(), ex);
            throw new IllegalStateException("project_registry_persist_failed", ex);
        }
    }

    private static void ensureNoBuildInProgress() {
        if (DatabaseManager.isBuilding()) {
            throw new IllegalStateException("project_build_in_progress");
        }
    }

    private void upsertEntryLocked(ProjectRegistryEntry entry) {
        if (entry == null || entry.projectKey().isBlank() || entry.type() != ProjectType.PERSISTENT) {
            return;
        }
        for (int i = 0; i < entries.size(); i++) {
            ProjectRegistryEntry current = entries.get(i);
            if (!Objects.equals(current.projectKey(), entry.projectKey())) {
                continue;
            }
            entries.set(i, entry);
            return;
        }
        entries.add(entry);
    }

    private ActiveSelection setActiveStateLocked(String projectKey, String alias) {
        String normalized = ActiveProjectContext.normalizeProjectKey(projectKey);
        if (normalized.isBlank()) {
            normalized = ActiveProjectContext.temporaryProjectKey();
        }
        activeProjectKey = normalized;
        String effectiveAlias;
        if (ActiveProjectContext.isTemporaryProjectKey(activeProjectKey)) {
            effectiveAlias = ActiveProjectContext.temporaryProjectAlias();
        } else {
            effectiveAlias = alias == null || alias.isBlank() ? resolveAlias(activeProjectKey) : alias.trim();
            if (effectiveAlias.isBlank()) {
                effectiveAlias = activeProjectKey;
            }
        }
        ActiveProjectContext.setActiveProject(activeProjectKey, effectiveAlias);
        return new ActiveSelection(activeProjectKey, effectiveAlias);
    }

    private void onActiveProjectChanged(String previousProjectKey, String nextProjectKey, String nextAlias) {
        ActiveProjectContext.beginProjectMutation(previousProjectKey, nextProjectKey);
        try {
            ActiveSelection next;
            synchronized (lock) {
                next = setActiveStateLocked(nextProjectKey, nextAlias);
            }
            try {
                if (previousProjectKey != null && !previousProjectKey.isBlank()) {
                    Neo4jProjectStore.getInstance().closeProject(previousProjectKey);
                }
            } catch (Exception ex) {
                logger.debug("close previous project runtime fail: {}", ex.toString());
            }
            try {
                CFRDecompileEngine.cleanCache();
            } catch (Exception ex) {
                logger.debug("clean cfr cache fail: {}", ex.toString());
            }
            try {
                var engine = EngineContext.getEngine();
                if (engine != null) {
                    engine.clearCallGraphCache();
                }
            } catch (Exception ex) {
                logger.debug("clear core engine cache fail: {}", ex.toString());
            }
            boolean restored = false;
            try {
                restored = ProjectMetadataSnapshotStore.getInstance().restoreIntoRuntime(next.projectKey());
            } catch (Exception ex) {
                logger.warn("restore project runtime snapshot fail: key={} err={}",
                        safe(next.projectKey()), ex.toString());
            }
            EngineContext.setEngine(createProjectEngine(next.projectKey()));
            try {
                ClassIndex.refresh();
            } catch (Exception ex) {
                logger.debug("refresh class index fail: {}", ex.toString());
            }
            logger.info("project switched: {} -> {} (metadataRestored={})",
                    safe(previousProjectKey), safe(next.projectKey()), restored);
        } finally {
            ActiveProjectContext.endProjectMutation(previousProjectKey, nextProjectKey);
        }
    }

    private void refreshActiveProjectInPlace(String projectKey,
                                             String alias,
                                             Runnable projectMutation,
                                             String reason) {
        ActiveProjectContext.beginProjectMutation(projectKey);
        try {
            synchronized (lock) {
                setActiveStateLocked(projectKey, alias);
            }
            ActiveProjectContext.bumpProjectEpoch();
            try {
                Neo4jProjectStore.getInstance().closeProject(projectKey);
            } catch (Exception ex) {
                logger.debug("close current project runtime fail: {}", ex.toString());
            }
            try {
                CFRDecompileEngine.cleanCache();
            } catch (Exception ex) {
                logger.debug("clean cfr cache fail: {}", ex.toString());
            }
            try {
                var engine = EngineContext.getEngine();
                if (engine != null) {
                    engine.clearCallGraphCache();
                }
            } catch (Exception ex) {
                logger.debug("clear core engine cache fail: {}", ex.toString());
            }
            if (projectMutation != null) {
                projectMutation.run();
            }
            boolean restored = false;
            try {
                restored = ProjectMetadataSnapshotStore.getInstance().restoreIntoRuntime(projectKey);
            } catch (Exception ex) {
                logger.warn("refresh project runtime snapshot fail: key={} err={}",
                        safe(projectKey), ex.toString());
            }
            EngineContext.setEngine(createProjectEngine(projectKey));
            try {
                ClassIndex.refresh();
            } catch (Exception ex) {
                logger.debug("refresh class index fail: {}", ex.toString());
            }
            logger.info("project refreshed in place: key={} reason={} (metadataRestored={})",
                    safe(projectKey), safe(reason), restored);
        } finally {
            ActiveProjectContext.endProjectMutation(projectKey);
        }
    }

    private CoreEngine createProjectEngine(String projectKey) {
        try {
            ConfigFile cfg = new ConfigFile();
            cfg.setDbPath(Neo4jProjectStore.getInstance()
                    .resolveProjectHome(projectKey)
                    .toString());
            cfg.setTempPath(Const.tempDir);
            cfg.setLang("en");
            cfg.setDecompileCacheSize(String.valueOf(CFRDecompileEngine.getCacheCapacity()));
            return new CoreEngine(cfg);
        } catch (Exception ex) {
            logger.debug("init project core engine fail: key={} err={}", safe(projectKey), ex.toString());
            return null;
        }
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
        if (ActiveProjectContext.isTemporaryProjectKey(projectKey)) {
            return ActiveProjectContext.temporaryProjectAlias();
        }
        for (ProjectRegistryEntry entry : entries) {
            if (Objects.equals(entry.projectKey(), projectKey)) {
                return entry.alias();
            }
        }
        if (Objects.equals(activeProjectKey, projectKey)) {
            String activeAlias = safe(ActiveProjectContext.getActiveProjectAlias());
            if (!activeAlias.isBlank()) {
                return activeAlias;
            }
        }
        return "";
    }

    private ProjectRegistryEntry temporaryEntryLocked() {
        long now = System.currentTimeMillis();
        long ts = tempUpdatedAt <= 0L ? now : tempUpdatedAt;
        return new ProjectRegistryEntry(
                ActiveProjectContext.temporaryProjectKey(),
                ProjectType.TEMP,
                ActiveProjectContext.temporaryProjectAlias(),
                tempInputPath,
                tempRuntimePath,
                tempResolveNestedJars,
                ts,
                ts
        );
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
            logger.debug("normalize project alias from path fail: {} ({})",
                    normalizedInput, ignored.toString());
        }
        return "project";
    }

    private String nextProjectKeyLocked() {
        while (true) {
            String key = UUID.randomUUID().toString().replace("-", "");
            if (key.length() > 12) {
                key = key.substring(0, 12);
            }
            if (findByKey(key).isEmpty()) {
                return key;
            }
        }
    }

    private static void ensureProjectStore(String projectKey) {
        try {
            Neo4jProjectStore.getInstance().database(projectKey);
        } catch (Exception ex) {
            logger.error("ensure project store fail: key={} err={}",
                    safe(projectKey), ex.toString(), ex);
            throw new IllegalStateException("project_store_open_failed", ex);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record ActiveSelection(String projectKey, String alias) {
    }
}
