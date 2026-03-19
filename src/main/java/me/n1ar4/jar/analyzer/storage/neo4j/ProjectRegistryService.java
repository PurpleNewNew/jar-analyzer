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
import me.n1ar4.jar.analyzer.core.CallGraphPlan;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.core.runtime.JdkArchiveResolver;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.gui.runtime.api.ProjectScopedRuntimeCleaner;
import me.n1ar4.jar.analyzer.utils.ClassIndex;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.taint.TaintPropagationMode;
import me.n1ar4.jar.analyzer.utils.OSUtil;
import me.n1ar4.jar.analyzer.utils.ProjectPathNormalizer;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ProjectRegistryService {
    private static final Logger logger = LogManager.getLogger();
    private static final Path REGISTRY_FILE = Paths.get(".jar-analyzer-projects.json");
    private static final ProjectGraphStoreFacade PROJECT_STORE = ProjectGraphStoreFacade.getInstance();
    private static final ProjectRegistryService INSTANCE = new ProjectRegistryService();
    private static final String REGISTRY_STATE_OK = "ok";
    private static final String REGISTRY_STATE_MISSING = "missing";
    private static final String REGISTRY_STATE_UNAVAILABLE = "unavailable";
    private static final int REGISTRY_REPLACE_RETRY_COUNT = 5;
    private static final long REGISTRY_REPLACE_RETRY_DELAY_MS = 100L;

    private final Object lock = new Object();

    private String activeProjectKey = ActiveProjectContext.temporaryProjectKey();
    private final List<ProjectRegistryEntry> entries = new ArrayList<>();
    private String registryState = REGISTRY_STATE_MISSING;
    private String registryMessage = "";

    private String tempInputPath = "";
    private String tempRuntimePath = "";
    private boolean tempResolveNestedJars = false;
    private String tempJdkModules = JdkArchiveResolver.DEFAULT_MODULE_POLICY;
    private String tempCallGraphProfile = CallGraphPlan.PROFILE_BALANCED;
    private String tempTaintPropagationMode = TaintPropagationMode.BALANCED.name().toLowerCase(Locale.ROOT);
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
            return new ProjectRegistrySnapshot(out, activeProjectKey, alias, registryState, registryMessage);
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
        return register(
                alias,
                inputPath,
                runtimePath,
                resolveNestedJars,
                JdkArchiveResolver.DEFAULT_MODULE_POLICY,
                CallGraphPlan.PROFILE_BALANCED,
                TaintPropagationMode.BALANCED.name().toLowerCase(Locale.ROOT)
        );
    }

    public ProjectRegistryEntry register(String alias,
                                         String inputPath,
                                         String runtimePath,
                                         boolean resolveNestedJars,
                                         String jdkModules) {
        return register(
                alias,
                inputPath,
                runtimePath,
                resolveNestedJars,
                jdkModules,
                CallGraphPlan.PROFILE_BALANCED,
                TaintPropagationMode.BALANCED.name().toLowerCase(Locale.ROOT)
        );
    }

    public ProjectRegistryEntry register(String alias,
                                         String inputPath,
                                         String runtimePath,
                                         boolean resolveNestedJars,
                                         String jdkModules,
                                         String callGraphProfile) {
        return register(
                alias,
                inputPath,
                runtimePath,
                resolveNestedJars,
                jdkModules,
                callGraphProfile,
                TaintPropagationMode.BALANCED.name().toLowerCase(Locale.ROOT)
        );
    }

    public ProjectRegistryEntry register(String alias,
                                         String inputPath,
                                         String runtimePath,
                                         boolean resolveNestedJars,
                                         String jdkModules,
                                         String callGraphProfile,
                                         String taintPropagationMode) {
        ensureNoBuildInProgress();
        synchronized (ActiveProjectContext.mutationLock()) {
            ensureNoBuildInProgress();
            String normalizedInput = normalizePath(inputPath);
            String normalizedRuntime = normalizePath(runtimePath);
            String normalizedJdkModules = normalizeJdkModules(jdkModules);
            String normalizedCallGraphProfile = normalizeCallGraphProfile(callGraphProfile);
            String normalizedTaintPropagationMode = normalizeTaintPropagationMode(taintPropagationMode);
            if (normalizedInput.isBlank()) {
                throw new IllegalArgumentException("project_input_required");
            }
            String projectKey = buildProjectKey(
                    normalizedInput,
                    normalizedRuntime,
                    resolveNestedJars,
                    normalizedJdkModules,
                    normalizedCallGraphProfile
            );
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
                        normalizedJdkModules,
                        normalizedCallGraphProfile,
                        normalizedTaintPropagationMode,
                        current == null || current.createdAt() <= 0L ? now : current.createdAt(),
                        now
                );
            }
            boolean storeExistedBefore = projectStoreExists(next.projectKey());
            ensureProjectStore(next.projectKey());
            String previousProjectKey;
            String previousAlias;
            List<ProjectRegistryEntry> previousEntries;
            try {
                synchronized (lock) {
                    previousEntries = new ArrayList<>(entries);
                    upsertEntryLocked(next);
                    previousProjectKey = activeProjectKey;
                    previousAlias = resolveAlias(previousProjectKey);
                    try {
                        persistLocked();
                    } catch (RuntimeException ex) {
                        entries.clear();
                        entries.addAll(previousEntries);
                        throw ex;
                    }
                }
            } catch (RuntimeException ex) {
                throw attachPreparedProjectCleanupFailure(ex, next.projectKey(), storeExistedBefore);
            }
            try {
                if (!Objects.equals(previousProjectKey, next.projectKey())) {
                    onActiveProjectChanged(previousProjectKey, next.projectKey(), next.alias());
                } else {
                    synchronized (lock) {
                        setActiveStateLocked(next.projectKey(), next.alias());
                    }
                }
            } catch (RuntimeException ex) {
                try {
                    rollbackCommittedRegistryMutation(previousEntries, previousProjectKey, previousAlias, ex);
                } catch (RuntimeException rollbackEx) {
                    throw attachPreparedProjectCleanupFailure(rollbackEx, next.projectKey(), storeExistedBefore);
                }
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
                    persistLocked();
                }
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
                tempJdkModules = JdkArchiveResolver.DEFAULT_MODULE_POLICY;
                tempCallGraphProfile = CallGraphPlan.PROFILE_BALANCED;
                tempTaintPropagationMode = TaintPropagationMode.BALANCED.name().toLowerCase(Locale.ROOT);
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
                        () -> deleteProjectStoreOrThrow(temporaryKey, "temporary_project_store_delete_failed"),
                        "cleanup_temporary_project"
                );
            } else {
                deleteProjectStoreOrThrow(temporaryKey, "temporary_project_store_delete_failed");
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
                        JdkArchiveResolver.DEFAULT_MODULE_POLICY,
                        CallGraphPlan.PROFILE_BALANCED,
                        TaintPropagationMode.BALANCED.name().toLowerCase(Locale.ROOT),
                        now,
                        now
                );
            }
            boolean storeExistedBefore = projectStoreExists(next.projectKey());
            ensureProjectStore(next.projectKey());
            String previousProjectKey;
            String previousAlias;
            List<ProjectRegistryEntry> previousEntries;
            try {
                synchronized (lock) {
                    previousEntries = new ArrayList<>(entries);
                    entries.add(next);
                    previousProjectKey = activeProjectKey;
                    previousAlias = resolveAlias(previousProjectKey);
                    try {
                        persistLocked();
                    } catch (RuntimeException ex) {
                        entries.clear();
                        entries.addAll(previousEntries);
                        throw ex;
                    }
                }
            } catch (RuntimeException ex) {
                throw attachPreparedProjectCleanupFailure(ex, next.projectKey(), storeExistedBefore);
            }
            try {
                if (!Objects.equals(previousProjectKey, next.projectKey())) {
                    onActiveProjectChanged(previousProjectKey, next.projectKey(), next.alias());
                } else {
                    synchronized (lock) {
                        setActiveStateLocked(next.projectKey(), next.alias());
                    }
                }
            } catch (RuntimeException ex) {
                try {
                    rollbackCommittedRegistryMutation(previousEntries, previousProjectKey, previousAlias, ex);
                } catch (RuntimeException rollbackEx) {
                    throw attachPreparedProjectCleanupFailure(rollbackEx, next.projectKey(), storeExistedBefore);
                }
            }
            return next;
        }
    }

    public ProjectRegistryEntry upsertActiveProjectBuildSettings(String alias,
                                                                 String inputPath,
                                                                 String runtimePath,
                                                                 boolean resolveNestedJars) {
        return upsertActiveProjectBuildSettings(
                alias,
                inputPath,
                runtimePath,
                resolveNestedJars,
                JdkArchiveResolver.DEFAULT_MODULE_POLICY,
                CallGraphPlan.PROFILE_BALANCED,
                TaintPropagationMode.BALANCED.name().toLowerCase(Locale.ROOT)
        );
    }

    public ProjectRegistryEntry upsertActiveProjectBuildSettings(String alias,
                                                                 String inputPath,
                                                                 String runtimePath,
                                                                 boolean resolveNestedJars,
                                                                 String jdkModules) {
        return upsertActiveProjectBuildSettings(
                alias,
                inputPath,
                runtimePath,
                resolveNestedJars,
                jdkModules,
                CallGraphPlan.PROFILE_BALANCED,
                TaintPropagationMode.BALANCED.name().toLowerCase(Locale.ROOT)
        );
    }

    public ProjectRegistryEntry upsertActiveProjectBuildSettings(String alias,
                                                                 String inputPath,
                                                                 String runtimePath,
                                                                 boolean resolveNestedJars,
                                                                 String jdkModules,
                                                                 String callGraphProfile) {
        return upsertActiveProjectBuildSettings(
                alias,
                inputPath,
                runtimePath,
                resolveNestedJars,
                jdkModules,
                callGraphProfile,
                TaintPropagationMode.BALANCED.name().toLowerCase(Locale.ROOT)
        );
    }

    public ProjectRegistryEntry upsertActiveProjectBuildSettings(String alias,
                                                                 String inputPath,
                                                                 String runtimePath,
                                                                 boolean resolveNestedJars,
                                                                 String jdkModules,
                                                                 String callGraphProfile,
                                                                 String taintPropagationMode) {
        ensureNoBuildInProgress();
        synchronized (ActiveProjectContext.mutationLock()) {
            ensureNoBuildInProgress();
            String normalizedInput = normalizePath(inputPath);
            String normalizedRuntime = normalizePath(runtimePath);
            String normalizedJdkModules = normalizeJdkModules(jdkModules);
            String normalizedCallGraphProfile = normalizeCallGraphProfile(callGraphProfile);
            String normalizedTaintPropagationMode = normalizeTaintPropagationMode(taintPropagationMode);
            long now = System.currentTimeMillis();
            ProjectRegistryEntry next;
            String currentProjectKey;
            synchronized (lock) {
                currentProjectKey = ActiveProjectContext.resolveRequestedOrActive(activeProjectKey);
            }
            ensureProjectStore(currentProjectKey);
            synchronized (lock) {
                if (ActiveProjectContext.isTemporaryProjectKey(currentProjectKey)) {
                    String previousInputPath = tempInputPath;
                    String previousRuntimePath = tempRuntimePath;
                    boolean previousResolveNested = tempResolveNestedJars;
                    String previousJdkModules = tempJdkModules;
                    String previousCallGraphProfile = tempCallGraphProfile;
                    String previousTaintPropagationMode = tempTaintPropagationMode;
                    long previousUpdatedAt = tempUpdatedAt;
                    tempInputPath = normalizedInput;
                    tempRuntimePath = normalizedRuntime;
                    tempResolveNestedJars = resolveNestedJars;
                    tempJdkModules = normalizedJdkModules;
                    tempCallGraphProfile = normalizedCallGraphProfile;
                    tempTaintPropagationMode = normalizedTaintPropagationMode;
                    tempUpdatedAt = now;
                    try {
                        persistLocked();
                    } catch (RuntimeException ex) {
                        tempInputPath = previousInputPath;
                        tempRuntimePath = previousRuntimePath;
                        tempResolveNestedJars = previousResolveNested;
                        tempJdkModules = previousJdkModules;
                        tempCallGraphProfile = previousCallGraphProfile;
                        tempTaintPropagationMode = previousTaintPropagationMode;
                        tempUpdatedAt = previousUpdatedAt;
                        throw ex;
                    }
                    setActiveStateLocked(currentProjectKey, ActiveProjectContext.temporaryProjectAlias());
                    next = temporaryEntryLocked();
                } else {
                    List<ProjectRegistryEntry> previousEntries = new ArrayList<>(entries);
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
                            normalizedJdkModules,
                            normalizedCallGraphProfile,
                            normalizedTaintPropagationMode,
                            current == null ? now : (current.createdAt() <= 0L ? now : current.createdAt()),
                            now
                    );
                    upsertEntryLocked(next);
                    try {
                        persistLocked();
                    } catch (RuntimeException ex) {
                        entries.clear();
                        entries.addAll(previousEntries);
                        throw ex;
                    }
                    setActiveStateLocked(currentProjectKey, effectiveAlias);
                }
            }
            return next;
        }
    }

    public void publishActiveProjectModel(ProjectModel projectModel) {
        synchronized (ActiveProjectContext.mutationLock()) {
            String currentProjectKey;
            synchronized (lock) {
                currentProjectKey = ActiveProjectContext.resolveRequestedOrActive(activeProjectKey);
            }
            ProjectRuntimeContext.restoreProjectRuntime(
                    currentProjectKey,
                    DatabaseManager.getProjectBuildSeq(currentProjectKey),
                    projectModel == null ? ProjectModel.empty() : projectModel
            );
        }
    }

    public void publishActiveProjectRuntime(String projectKey,
                                            ProjectRuntimeSnapshot snapshot,
                                            ProjectModel projectModel) {
        synchronized (ActiveProjectContext.mutationLock()) {
            String resolvedProjectKey = ActiveProjectContext.resolveRequestedOrActive(projectKey);
            DatabaseManager.restoreProjectRuntime(resolvedProjectKey, snapshot);
            ProjectModel effectiveProjectModel = projectModel;
            if (effectiveProjectModel == null) {
                effectiveProjectModel = DatabaseManager.getProjectModel();
            }
            ProjectRuntimeContext.restoreProjectRuntime(
                    resolvedProjectKey,
                    DatabaseManager.getProjectBuildSeq(resolvedProjectKey),
                    effectiveProjectModel == null ? ProjectModel.empty() : effectiveProjectModel
            );
        }
    }

    public void publishBuiltActiveProjectRuntime(String projectKey,
                                                 ProjectRuntimeSnapshot snapshot,
                                                 ProjectModel projectModel) {
        synchronized (ActiveProjectContext.mutationLock()) {
            String resolvedProjectKey = ActiveProjectContext.resolveRequestedOrActive(projectKey);
            publishActiveProjectRuntime(resolvedProjectKey, snapshot, projectModel);
            resetProjectScopedRuntimeState();
            activateProjectServices(resolvedProjectKey);
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
                    persistLocked();
                }
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
            String previousAlias;
            String removedProjectKey = "";
            List<ProjectRegistryEntry> previousEntries;
            synchronized (lock) {
                previousEntries = new ArrayList<>(entries);
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
                previousAlias = resolveAlias(previousProjectKey);
                removedFlag = true;
                try {
                    persistLocked();
                } catch (RuntimeException ex) {
                    entries.clear();
                    entries.addAll(previousEntries);
                    throw ex;
                }
            }
            try {
                if (!nextActiveProjectKey.isBlank() && !Objects.equals(previousProjectKey, nextActiveProjectKey)) {
                    onActiveProjectChanged(previousProjectKey, nextActiveProjectKey, nextActiveAlias);
                } else if (!nextActiveProjectKey.isBlank()) {
                    synchronized (lock) {
                        setActiveStateLocked(nextActiveProjectKey, nextActiveAlias);
                    }
                }
            } catch (RuntimeException ex) {
                rollbackCommittedRegistryMutation(previousEntries, previousProjectKey, previousAlias, ex);
            }
            if (deleteStore && !removedProjectKey.isBlank()) {
                try {
                    deleteProjectStoreOrThrow(removedProjectKey, "project_store_delete_failed");
                } catch (RuntimeException ex) {
                    rollbackCommittedRegistryMutation(previousEntries, previousProjectKey, previousAlias, ex);
                }
            }
            return removedFlag;
        }
    }

    public static String buildProjectKey(String normalizedInputPath) {
        return buildProjectKey(
                normalizedInputPath,
                "",
                false,
                JdkArchiveResolver.DEFAULT_MODULE_POLICY,
                CallGraphPlan.PROFILE_BALANCED
        );
    }

    public static String buildProjectKey(String normalizedInputPath,
                                         String normalizedRuntimePath,
                                         boolean resolveNestedJars) {
        return buildProjectKey(
                normalizedInputPath,
                normalizedRuntimePath,
                resolveNestedJars,
                JdkArchiveResolver.DEFAULT_MODULE_POLICY,
                CallGraphPlan.PROFILE_BALANCED
        );
    }

    public static String buildProjectKey(String normalizedInputPath,
                                         String normalizedRuntimePath,
                                         boolean resolveNestedJars,
                                         String jdkModules) {
        return buildProjectKey(
                normalizedInputPath,
                normalizedRuntimePath,
                resolveNestedJars,
                jdkModules,
                CallGraphPlan.PROFILE_BALANCED
        );
    }

    public static String buildProjectKey(String normalizedInputPath,
                                         String normalizedRuntimePath,
                                         boolean resolveNestedJars,
                                         String jdkModules,
                                         String callGraphProfile) {
        String safeInput = normalizedInputPath == null ? "" : normalizedInputPath.trim();
        if (safeInput.isBlank()) {
            return "";
        }
        String safeRuntime = normalizedRuntimePath == null ? "" : normalizedRuntimePath.trim();
        String safeModules = normalizeJdkModules(jdkModules);
        String safeCallGraphProfile = normalizeCallGraphProfile(callGraphProfile);
        String signature = (safeRuntime.isBlank()
                && !resolveNestedJars
                && JdkArchiveResolver.DEFAULT_MODULE_POLICY.equals(safeModules)
                && CallGraphPlan.PROFILE_BALANCED.equals(safeCallGraphProfile))
                ? safeInput
                : safeInput + "\n" + safeRuntime + "\n" + resolveNestedJars + "\n"
                + safeModules + "\n" + safeCallGraphProfile;
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
        String loadedActiveProjectKey = "";
        boolean fileExists = Files.exists(REGISTRY_FILE);
        boolean parsed = !fileExists;
        String nextRegistryState = fileExists ? REGISTRY_STATE_OK : REGISTRY_STATE_MISSING;
        String nextRegistryMessage = fileExists
                ? ""
                : "project registry file not found: " + REGISTRY_FILE.toAbsolutePath().normalize();
        if (fileExists) {
            try {
                String raw = Files.readString(REGISTRY_FILE);
                JSONObject obj = JSON.parseObject(raw);
                loadedActiveProjectKey = safe(obj == null ? null : obj.getString("activeProjectKey"));
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
                                normalizeJdkModules(item.getString("jdkModules")),
                                safe(item.getString("callGraphProfile")),
                                safe(item.getString("taintPropagationMode")),
                                item.getLongValue("createdAt"),
                                item.getLongValue("updatedAt")
                        ));
                    }
                }
                parsed = true;
            } catch (Exception ex) {
                nextRegistryState = REGISTRY_STATE_UNAVAILABLE;
                nextRegistryMessage = "project registry file is unreadable; persisted projects may be temporarily hidden: "
                        + REGISTRY_FILE.toAbsolutePath().normalize();
                logger.error("load project registry fail: path={} err={} - persisted projects may be temporarily hidden",
                        REGISTRY_FILE.toAbsolutePath().normalize(),
                        ex.toString(),
                        ex);
            }
        }
        boolean preserveCurrent = fileExists && !parsed;
        String restoreProjectKey = ActiveProjectContext.temporaryProjectKey();
        String restoreAlias = ActiveProjectContext.temporaryProjectAlias();
        synchronized (lock) {
            setRegistryStatusLocked(nextRegistryState, nextRegistryMessage);
            if (preserveCurrent) {
                ActiveProjectContext.setActiveProject(activeProjectKey, resolveAlias(activeProjectKey));
            } else {
                entries.clear();
                entries.addAll(loadedEntries);
                activeProjectKey = resolvePersistedActiveProjectKey(loadedActiveProjectKey, loadedEntries);
                tempInputPath = "";
                tempRuntimePath = "";
                tempResolveNestedJars = false;
                tempJdkModules = JdkArchiveResolver.DEFAULT_MODULE_POLICY;
                tempCallGraphProfile = CallGraphPlan.PROFILE_BALANCED;
                tempTaintPropagationMode = TaintPropagationMode.BALANCED.name().toLowerCase(Locale.ROOT);
                tempUpdatedAt = 0L;
                restoreProjectKey = activeProjectKey;
                restoreAlias = resolveAlias(activeProjectKey);
                if (restoreAlias.isBlank()) {
                    restoreAlias = ActiveProjectContext.isTemporaryProjectKey(activeProjectKey)
                            ? ActiveProjectContext.temporaryProjectAlias()
                            : activeProjectKey;
                }
                ActiveProjectContext.setActiveProject(activeProjectKey, restoreAlias);
            }
        }
        if (!preserveCurrent) {
            restoreLoadedActiveProject(restoreProjectKey, restoreAlias);
            return;
        }
        try {
            ensureProjectStore(activeProjectKey);
        } catch (Exception ex) {
            logger.warn("initialize active project store fail: key={} err={}",
                    safe(activeProjectKey), ex.toString());
        }
    }

    private void persistLocked() {
        Path temp = null;
        try {
            JSONObject root = new JSONObject();
            root.put("activeProjectKey", activeProjectKey);
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
                row.put("jdkModules", entry.jdkModules());
                row.put("callGraphProfile", entry.callGraphProfile());
                row.put("taintPropagationMode", entry.taintPropagationMode());
                row.put("createdAt", entry.createdAt());
                row.put("updatedAt", entry.updatedAt());
                arr.add(row);
            }
            root.put("projects", arr);
            String json = JSON.toJSONString(root);
            Path target = REGISTRY_FILE.toAbsolutePath().normalize();
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            temp = Files.createTempFile(parent, target.getFileName() + ".tmp-", ".json");
            Files.writeString(temp, json, StandardCharsets.UTF_8);
            replaceRegistryFile(temp, target);
            setRegistryStatusLocked(REGISTRY_STATE_OK, "");
        } catch (Exception ex) {
            logger.error("save project registry fail: {}", ex.toString(), ex);
            throw new IllegalStateException("project_registry_persist_failed", ex);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (Exception ex) {
                    logger.debug("cleanup project registry temp file fail: {}", ex.toString());
                }
            }
        }
    }

    static void replaceRegistryFile(Path temp, Path target) throws Exception {
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= REGISTRY_REPLACE_RETRY_COUNT; attempt++) {
            try {
                try {
                    Files.move(temp, target,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (Exception ex) {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                }
                return;
            } catch (Exception ex) {
                lastFailure = ex;
                if (attempt >= REGISTRY_REPLACE_RETRY_COUNT) {
                    break;
                }
                try {
                    Thread.sleep(REGISTRY_REPLACE_RETRY_DELAY_MS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    IllegalStateException failure = new IllegalStateException("project_registry_persist_failed", interrupted);
                    if (lastFailure != null) {
                        failure.addSuppressed(lastFailure);
                    }
                    throw failure;
                }
            }
        }
        throw lastFailure == null ? new IllegalStateException("project_registry_persist_failed") : lastFailure;
    }

    private void setRegistryStatusLocked(String state, String message) {
        registryState = normalizeRegistryState(state);
        registryMessage = safe(message);
    }

    private static void ensureNoBuildInProgress() {
        if (DatabaseManager.isBuilding()) {
            throw new IllegalStateException("project_build_in_progress");
        }
    }

    private void rollbackCommittedRegistryMutation(List<ProjectRegistryEntry> previousEntries,
                                                   String previousProjectKey,
                                                   String previousAlias,
                                                   RuntimeException cause) {
        RuntimeException failure = cause == null ? new IllegalStateException("project_registry_rollback_failed") : cause;
        try {
            synchronized (lock) {
                entries.clear();
                if (previousEntries != null && !previousEntries.isEmpty()) {
                    entries.addAll(previousEntries);
                }
                persistLocked();
            }
        } catch (RuntimeException rollbackEx) {
            failure.addSuppressed(rollbackEx);
        }
        try {
            String rollbackProjectKey = ActiveProjectContext.normalizeProjectKey(previousProjectKey);
            if (rollbackProjectKey.isBlank()) {
                rollbackProjectKey = ActiveProjectContext.temporaryProjectKey();
            }
            String rollbackAlias = safe(previousAlias);
            if (rollbackAlias.isBlank()) {
                rollbackAlias = ActiveProjectContext.isTemporaryProjectKey(rollbackProjectKey)
                        ? ActiveProjectContext.temporaryProjectAlias()
                        : rollbackProjectKey;
            }
            String currentProjectKey = ActiveProjectContext.normalizeProjectKey(
                    ActiveProjectContext.getPublishedActiveProjectKey());
            if (!rollbackProjectKey.equals(currentProjectKey)) {
                onActiveProjectChanged(currentProjectKey, rollbackProjectKey, rollbackAlias);
            } else {
                synchronized (lock) {
                    setActiveStateLocked(rollbackProjectKey, rollbackAlias);
                    persistLocked();
                }
            }
        } catch (RuntimeException rollbackEx) {
            failure.addSuppressed(rollbackEx);
        }
        throw failure;
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
            RuntimeRestorePlan restorePlan = prepareRuntimeRestorePlan(nextProjectKey);
            ActiveSelection next;
            synchronized (lock) {
                next = setActiveStateLocked(nextProjectKey, nextAlias);
                persistLocked();
            }
            try {
                if (previousProjectKey != null && !previousProjectKey.isBlank()) {
                    PROJECT_STORE.closeProject(previousProjectKey);
                }
            } catch (Exception ex) {
                logger.debug("close previous project runtime fail: {}", ex.toString());
            }
            boolean restored = restoreAndActivateProjectRuntime(next.projectKey(), restorePlan);
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
                PROJECT_STORE.closeProject(projectKey);
            } catch (Exception ex) {
                logger.debug("close current project runtime fail: {}", ex.toString());
            }
            RuntimeException failure = null;
            try {
                if (projectMutation != null) {
                    projectMutation.run();
                }
                RuntimeRestorePlan restorePlan = prepareRuntimeRestorePlan(projectKey);
                boolean restored = restoreAndActivateProjectRuntime(projectKey, restorePlan);
                logger.info("project refreshed in place: key={} reason={} (metadataRestored={})",
                        safe(projectKey), safe(reason), restored);
            } catch (RuntimeException ex) {
                failure = ex;
                throw ex;
            } finally {
                if (failure != null) {
                    recoverActiveProjectRefresh(projectKey, alias, reason, failure);
                }
            }
        } finally {
            ActiveProjectContext.endProjectMutation(projectKey);
        }
    }

    private CoreEngine createProjectEngine(String projectKey) {
        try {
            ConfigFile cfg = new ConfigFile();
            cfg.setDbPath(PROJECT_STORE
                    .resolveProjectHome(projectKey)
                    .toString());
            cfg.setTempPath(Const.tempDir);
            cfg.setLang("en");
            return new CoreEngine(cfg);
        } catch (Exception ex) {
            logger.debug("init project core engine fail: key={} err={}", safe(projectKey), ex.toString());
            return null;
        }
    }

    private RuntimeRestorePlan prepareRuntimeRestorePlan(String projectKey) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        ProjectMetadataSnapshotStore store = ProjectMetadataSnapshotStore.getInstance();
        ProjectMetadataSnapshotStore.Availability availability = store.readAvailability(normalized);
        ProjectRuntimeSnapshot.ProjectModelData modelData = toProjectModelData(buildEntryProjectModel(projectEntrySnapshot(normalized)));
        if (availability.unavailable()) {
            modelData = store.readProjectModelRegardlessOfAvailability(normalized);
            if (modelData == null) {
                modelData = toProjectModelData(buildEntryProjectModel(projectEntrySnapshot(normalized)));
            }
            if (availability.corrupt()) {
                throw new IllegalStateException("project_runtime_snapshot_corrupt");
            }
            return RuntimeRestorePlan.unavailable(emptyRuntimeSnapshot(modelData));
        }
        Path snapshotFile = store.resolveSnapshotFile(normalized);
        if (!Files.exists(snapshotFile)) {
            return RuntimeRestorePlan.empty(emptyRuntimeSnapshot(modelData));
        }
        ProjectRuntimeSnapshot snapshot = store.read(normalized);
        if (snapshot == null) {
            ProjectMetadataSnapshotStore.Availability refreshed = store.readAvailability(normalized);
            if (refreshed.corrupt()) {
                throw new IllegalStateException("project_runtime_snapshot_corrupt");
            }
            throw new IllegalStateException("project_runtime_snapshot_restore_failed");
        }
        return RuntimeRestorePlan.snapshot(snapshot);
    }

    private boolean applyRuntimeRestorePlan(String projectKey, RuntimeRestorePlan plan) {
        if (plan == null || plan.snapshot() == null) {
            DatabaseManager.clearAllData();
            ProjectRuntimeContext.clear();
            return false;
        }
        publishActiveProjectRuntime(projectKey, plan.snapshot(), null);
        return plan.metadataRestored();
    }

    private void restoreLoadedActiveProject(String projectKey, String alias) {
        String normalized = ActiveProjectContext.normalizeProjectKey(projectKey);
        if (normalized.isBlank()) {
            normalized = ActiveProjectContext.temporaryProjectKey();
        }
        String effectiveAlias = safe(alias);
        if (effectiveAlias.isBlank()) {
            effectiveAlias = ActiveProjectContext.isTemporaryProjectKey(normalized)
                    ? ActiveProjectContext.temporaryProjectAlias()
                    : resolveAlias(normalized);
        }
        if (effectiveAlias.isBlank()) {
            effectiveAlias = ActiveProjectContext.isTemporaryProjectKey(normalized)
                    ? ActiveProjectContext.temporaryProjectAlias()
                    : normalized;
        }
        try {
            ensureProjectStore(normalized);
        } catch (Exception ex) {
            logger.warn("initialize active project store fail: key={} err={}",
                    safe(normalized), ex.toString());
        }
        try {
            refreshActiveProjectInPlace(normalized, effectiveAlias, null, "load_project_registry");
        } catch (RuntimeException ex) {
            logger.warn("restore active project on load fail: key={} err={}",
                    safe(normalized), ex.toString());
            restoreTemporaryProjectAfterLoad();
        }
    }

    private void restoreTemporaryProjectAfterLoad() {
        String temporaryKey = ActiveProjectContext.temporaryProjectKey();
        String temporaryAlias = ActiveProjectContext.temporaryProjectAlias();
        synchronized (lock) {
            setActiveStateLocked(temporaryKey, temporaryAlias);
        }
        try {
            ensureProjectStore(temporaryKey);
        } catch (Exception ex) {
            logger.warn("initialize temporary project store fail: key={} err={}",
                    safe(temporaryKey), ex.toString());
        }
        try {
            refreshActiveProjectInPlace(temporaryKey, temporaryAlias, null, "load_project_registry_fallback");
        } catch (RuntimeException ex) {
            logger.warn("fallback temporary project restore fail: {}", ex.toString());
            DatabaseManager.clearAllData();
            ProjectRuntimeContext.clear();
            activateProjectServices(temporaryKey);
        }
    }

    private boolean restoreAndActivateProjectRuntime(String projectKey, RuntimeRestorePlan restorePlan) {
        resetProjectScopedRuntimeState();
        boolean restored = applyRuntimeRestorePlan(projectKey, restorePlan);
        activateProjectServices(projectKey);
        return restored;
    }

    private void resetProjectScopedRuntimeState() {
        ProjectScopedRuntimeCleaner.resetProjectScopedRuntime("", "", EngineContext::getEngine);
    }

    private void activateProjectServices(String projectKey) {
        EngineContext.setEngine(createProjectEngine(projectKey));
        try {
            ClassIndex.refresh();
        } catch (Exception ex) {
            logger.debug("refresh class index fail: {}", ex.toString());
        }
    }

    private static String resolvePersistedActiveProjectKey(String rawActiveProjectKey,
                                                           List<ProjectRegistryEntry> loadedEntries) {
        String normalized = ActiveProjectContext.normalizeProjectKey(rawActiveProjectKey);
        if (normalized.isBlank() || ActiveProjectContext.isTemporaryProjectKey(normalized)) {
            return ActiveProjectContext.temporaryProjectKey();
        }
        if (loadedEntries != null) {
            for (ProjectRegistryEntry entry : loadedEntries) {
                if (entry != null && Objects.equals(entry.projectKey(), normalized)) {
                    return normalized;
                }
            }
        }
        return ActiveProjectContext.temporaryProjectKey();
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

    private ProjectRegistryEntry projectEntrySnapshot(String projectKey) {
        synchronized (lock) {
            if (ActiveProjectContext.isTemporaryProjectKey(projectKey)) {
                return temporaryEntryLocked();
            }
            return findByKey(projectKey).orElse(null);
        }
    }

    private static ProjectModel buildEntryProjectModel(ProjectRegistryEntry entry) {
        return buildEntryProjectModel(entry, JdkArchiveResolver.DEFAULT_MODULE_POLICY);
    }

    private static ProjectModel buildEntryProjectModel(ProjectRegistryEntry entry, String fallback) {
        if (entry == null) {
            return ProjectModel.empty();
        }
        Path inputPath = toPath(entry.inputPath());
        Path runtimePath = toPath(entry.runtimePath());
        List<Path> analyzedArchives = inputPath == null ? List.of() : List.of(inputPath);
        if (inputPath == null && runtimePath == null) {
            return ProjectModel.empty();
        }
        String modules = safe(entry.jdkModules());
        if (modules.isBlank()) {
            modules = normalizeJdkModules(fallback);
        }
        return ProjectModel.artifact(
                inputPath,
                runtimePath,
                analyzedArchives,
                entry.resolveNestedJars(),
                modules,
                entry.callGraphProfile(),
                entry.taintPropagationMode()
        );
    }

    private static ProjectRuntimeSnapshot.ProjectModelData toProjectModelData(ProjectModel model) {
        if (model == null) {
            return null;
        }
        return new ProjectRuntimeSnapshot.ProjectModelData(
                model.buildMode().name(),
                model.primaryInputPath() == null ? "" : model.primaryInputPath().toString(),
                model.runtimePath() == null ? "" : model.runtimePath().toString(),
                List.of(),
                stringifyPaths(model.analyzedArchives()),
                model.resolveInnerJars(),
                model.jdkModules(),
                model.callGraphProfile(),
                model.taintPropagationMode()
        );
    }

    private static List<String> stringifyPaths(List<Path> paths) {
        if (paths == null || paths.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>(paths.size());
        for (Path path : paths) {
            if (path != null) {
                out.add(path.toString());
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static ProjectRuntimeSnapshot emptyRuntimeSnapshot(ProjectRuntimeSnapshot.ProjectModelData modelData) {
        return new ProjectRuntimeSnapshot(
                ProjectRuntimeSnapshot.CURRENT_SCHEMA_VERSION,
                0L,
                modelData,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                java.util.Map.of(),
                java.util.Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                java.util.Set.of(),
                java.util.Set.of(),
                java.util.Set.of(),
                java.util.Set.of()
        );
    }

    private static Path toPath(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ProjectPathNormalizer.normalizeNullablePath(Paths.get(value));
        } catch (Exception ex) {
            return Paths.get(value);
        }
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
                tempJdkModules,
                tempCallGraphProfile,
                tempTaintPropagationMode,
                ts,
                ts
        );
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String value = path.trim();
        if (OSUtil.isWindows() && value.startsWith("/") && !value.startsWith("//")) {
            return normalizePosixAbsolutePath(value);
        }
        try {
            Path p = Paths.get(value).toAbsolutePath().normalize();
            return p.toString();
        } catch (Exception ex) {
            return value;
        }
    }

    private static String normalizeCallGraphProfile(String profile) {
        return CallGraphPlan.normalizeProfile(profile);
    }

    private static String normalizeTaintPropagationMode(String value) {
        return TaintPropagationMode.parse(value).name().toLowerCase(Locale.ROOT);
    }

    private static String normalizePosixAbsolutePath(String path) {
        ArrayDeque<String> parts = new ArrayDeque<>();
        for (String part : path.split("/")) {
            if (part == null || part.isBlank() || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                if (!parts.isEmpty()) {
                    parts.removeLast();
                }
                continue;
            }
            parts.addLast(part);
        }
        if (parts.isEmpty()) {
            return "/";
        }
        return "/" + String.join("/", parts);
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

    private static String normalizeJdkModules(String value) {
        return JdkArchiveResolver.normalizePolicy(value);
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
            PROJECT_STORE.database(projectKey);
        } catch (Exception ex) {
            logger.error("ensure project store fail: key={} err={}",
                    safe(projectKey), ex.toString(), ex);
            throw new IllegalStateException("project_store_open_failed", ex);
        }
    }

    private static boolean projectStoreExists(String projectKey) {
        try {
            Path home = PROJECT_STORE.resolveProjectHome(projectKey);
            return home != null && Files.exists(home);
        } catch (Exception ex) {
            logger.debug("resolve project store home fail: key={} err={}", safe(projectKey), ex.toString());
            return false;
        }
    }

    private static void cleanupPreparedProjectStore(String projectKey, boolean existedBefore) {
        if (existedBefore || projectKey == null || projectKey.isBlank()) {
            return;
        }
        deleteProjectStoreOrThrow(projectKey, "prepared_project_store_cleanup_failed");
    }

    private static RuntimeException attachPreparedProjectCleanupFailure(RuntimeException failure,
                                                                       String projectKey,
                                                                       boolean existedBefore) {
        if (failure == null) {
            failure = new IllegalStateException("prepared_project_store_cleanup_failed");
        }
        try {
            cleanupPreparedProjectStore(projectKey, existedBefore);
        } catch (RuntimeException cleanupEx) {
            failure.addSuppressed(cleanupEx);
        }
        return failure;
    }

    private static void deleteProjectStoreOrThrow(String projectKey, String code) {
        String normalized = ActiveProjectContext.normalizeProjectKey(projectKey);
        if (normalized.isBlank()) {
            return;
        }
        try {
            PROJECT_STORE.deleteProjectStore(normalized);
        } catch (RuntimeException ex) {
            throw new IllegalStateException(safe(code), ex);
        }
    }

    private void recoverActiveProjectRefresh(String projectKey,
                                             String alias,
                                             String reason,
                                             RuntimeException failure) {
        try {
            synchronized (lock) {
                setActiveStateLocked(projectKey, alias);
            }
            RuntimeRestorePlan restorePlan = prepareRuntimeRestorePlan(projectKey);
            boolean restored = applyRuntimeRestorePlan(projectKey, restorePlan);
            activateProjectServices(projectKey);
            logger.warn("project refresh recovered after failure: key={} reason={} (metadataRestored={})",
                    safe(projectKey), safe(reason), restored);
        } catch (RuntimeException recoverEx) {
            failure.addSuppressed(recoverEx);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeRegistryState(String state) {
        String value = safe(state).toLowerCase();
        if (REGISTRY_STATE_OK.equals(value)
                || REGISTRY_STATE_MISSING.equals(value)
                || REGISTRY_STATE_UNAVAILABLE.equals(value)) {
            return value;
        }
        return REGISTRY_STATE_OK;
    }

    private record ActiveSelection(String projectKey, String alias) {
    }

    private record RuntimeRestorePlan(ProjectRuntimeSnapshot snapshot, boolean metadataRestored) {
        private static RuntimeRestorePlan empty(ProjectRuntimeSnapshot snapshot) {
            return new RuntimeRestorePlan(snapshot, false);
        }

        private static RuntimeRestorePlan snapshot(ProjectRuntimeSnapshot snapshot) {
            return new RuntimeRestorePlan(snapshot, true);
        }

        private static RuntimeRestorePlan unavailable(ProjectRuntimeSnapshot snapshot) {
            return new RuntimeRestorePlan(snapshot, false);
        }
    }
}
