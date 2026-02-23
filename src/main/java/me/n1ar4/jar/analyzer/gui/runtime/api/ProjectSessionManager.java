package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSettingsDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ProjectSessionSummaryDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ProjectStructureSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ProjectStructureUpdateDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.RuntimeProfileDto;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class ProjectSessionManager {
    private static final Logger logger = LogManager.getLogger();
    private static final String DEFAULT_PROJECT_ID = "default";
    private static final String DEFAULT_RUNTIME_ID = "default-runtime";
    private final Map<String, ProjectSession> sessions = new ConcurrentHashMap<>();
    private volatile String activeProjectId = DEFAULT_PROJECT_ID;

    ProjectSessionManager() {
        RuntimeProfileDto runtime = buildDefaultRuntimeProfile();
        BuildSettingsDto defaults = BuildSettingsDto.defaults();
        ProjectSession session = new ProjectSession(
                defaults.projectId(),
                "workspace",
                defaults,
                runtime.profileId(),
                mapProfiles(List.of(runtime)),
                System.currentTimeMillis()
        );
        sessions.put(session.projectId, session);
        activeProjectId = session.projectId;
    }

    synchronized BuildSettingsDto activateInput(BuildSettingsDto template, String inputPath, boolean projectMode) {
        String normalizedInput = normalizePathText(inputPath);
        String mode = projectMode ? BuildSettingsDto.INPUT_PROJECT : BuildSettingsDto.INPUT_FILE;
        String projectId = deriveProjectId(normalizedInput, mode);
        String projectName = deriveProjectName(normalizedInput, projectMode);
        BuildSettingsDto base = template == null ? BuildSettingsDto.defaults() : template;

        ProjectSession existing = sessions.get(projectId);
        ProjectSession session;
        if (existing == null) {
            RuntimeProfileDto runtime = buildDefaultRuntimeProfile();
            session = new ProjectSession(
                    projectId,
                    projectName,
                    new BuildSettingsDto(
                            projectId,
                            mode,
                            projectMode ? "" : normalizedInput,
                            projectMode ? normalizedInput : "",
                            runtime.profileId(),
                            base.resolveNestedJars(),
                            base.deleteTempBeforeBuild(),
                            base.fixClassPath(),
                            base.fixMethodImpl(),
                            base.quickMode()
                    ),
                    runtime.profileId(),
                    mapProfiles(List.of(runtime)),
                    System.currentTimeMillis()
            );
            sessions.put(projectId, session);
        } else {
            session = existing;
            BuildSettingsDto next = new BuildSettingsDto(
                    projectId,
                    mode,
                    projectMode ? "" : normalizedInput,
                    projectMode ? normalizedInput : "",
                    normalizeRuntimeId(session.selectedRuntimeProfileId),
                    base.resolveNestedJars(),
                    base.deleteTempBeforeBuild(),
                    base.fixClassPath(),
                    base.fixMethodImpl(),
                    base.quickMode()
            );
            session.settings = next;
            session.projectName = projectName;
            session.updatedAt = System.currentTimeMillis();
        }
        activeProjectId = projectId;
        touch(projectId);
        return session.settings;
    }

    synchronized BuildSettingsDto switchProject(String projectId, BuildSettingsDto fallback) {
        String id = normalizeProjectId(projectId);
        ProjectSession session = sessions.get(id);
        if (session == null) {
            return fallback == null ? BuildSettingsDto.defaults() : fallback;
        }
        activeProjectId = id;
        touch(id);
        return session.settings;
    }

    synchronized ProjectStructureSnapshotDto snapshot(String projectId) {
        ProjectSession session = resolveSession(projectId);
        if (session == null) {
            RuntimeProfileDto runtime = buildDefaultRuntimeProfile();
            return new ProjectStructureSnapshotDto(
                    DEFAULT_PROJECT_ID,
                    runtime.profileId(),
                    List.of(runtime)
            );
        }
        List<RuntimeProfileDto> profiles = new ArrayList<>(session.runtimeProfiles.values());
        profiles.sort(Comparator.comparing(RuntimeProfileDto::profileName, String.CASE_INSENSITIVE_ORDER));
        return new ProjectStructureSnapshotDto(
                session.projectId,
                normalizeRuntimeId(session.selectedRuntimeProfileId),
                profiles
        );
    }

    synchronized BuildSettingsDto saveStructure(ProjectStructureUpdateDto update, BuildSettingsDto fallback) {
        if (update == null) {
            return fallback == null ? BuildSettingsDto.defaults() : fallback;
        }
        String projectId = normalizeProjectId(update.projectId());
        ProjectSession session = sessions.get(projectId);
        if (session == null) {
            session = createSessionFromFallback(projectId, fallback);
            sessions.put(projectId, session);
        }

        LinkedHashMap<String, RuntimeProfileDto> nextProfiles = new LinkedHashMap<>();
        if (update.runtimeProfiles() != null) {
            for (RuntimeProfileDto profile : update.runtimeProfiles()) {
                RuntimeProfileDto normalized = normalizeProfile(profile);
                if (normalized == null) {
                    continue;
                }
                nextProfiles.put(normalized.profileId(), normalized);
            }
        }
        if (nextProfiles.isEmpty()) {
            RuntimeProfileDto def = buildDefaultRuntimeProfile();
            nextProfiles.put(def.profileId(), def);
        }
        String selected = normalizeRuntimeId(update.selectedRuntimeProfileId());
        if (!nextProfiles.containsKey(selected)) {
            selected = nextProfiles.keySet().iterator().next();
        }

        session.runtimeProfiles.clear();
        session.runtimeProfiles.putAll(nextProfiles);
        session.selectedRuntimeProfileId = selected;
        session.settings = session.settings.withRuntimeProfileId(selected);
        session.updatedAt = System.currentTimeMillis();
        touch(session.projectId);
        if (Objects.equals(activeProjectId, session.projectId)) {
            return session.settings;
        }
        return fallback == null ? session.settings : fallback;
    }

    synchronized List<ProjectSessionSummaryDto> recentSessions() {
        List<ProjectSession> values = new ArrayList<>(sessions.values());
        values.sort((a, b) -> Long.compare(b.updatedAt, a.updatedAt));
        List<ProjectSessionSummaryDto> out = new ArrayList<>();
        for (ProjectSession session : values) {
            if (session == null) {
                continue;
            }
            BuildSettingsDto settings = session.settings;
            String inputPath = settings == null ? "" : settings.activeInputPath();
            boolean projectMode = settings != null && settings.isProjectMode();
            out.add(new ProjectSessionSummaryDto(
                    session.projectId,
                    session.projectName,
                    inputPath,
                    projectMode,
                    session.updatedAt
            ));
        }
        return out;
    }

    synchronized String currentProjectId() {
        return normalizeProjectId(activeProjectId);
    }

    synchronized Path resolveRuntimePath(BuildSettingsDto settings) {
        BuildSettingsDto current = settings == null ? BuildSettingsDto.defaults() : settings;
        ProjectSession session = resolveSession(current.projectId());
        if (session == null) {
            RuntimeProfileDto profile = buildDefaultRuntimeProfile();
            return normalizePath(profile.runtimePath());
        }
        RuntimeProfileDto profile = session.runtimeProfiles.get(normalizeRuntimeId(current.runtimeProfileId()));
        if (profile == null) {
            profile = session.runtimeProfiles.get(normalizeRuntimeId(session.selectedRuntimeProfileId));
        }
        if (profile == null) {
            profile = buildDefaultRuntimeProfile();
        }
        return normalizePath(profile.runtimePath());
    }

    private ProjectSession createSessionFromFallback(String projectId, BuildSettingsDto fallback) {
        BuildSettingsDto base = fallback == null ? BuildSettingsDto.defaults() : fallback.withProjectId(projectId);
        RuntimeProfileDto runtime = buildDefaultRuntimeProfile();
        LinkedHashMap<String, RuntimeProfileDto> profiles = mapProfiles(List.of(runtime));
        return new ProjectSession(
                projectId,
                deriveProjectName(base.activeInputPath(), base.isProjectMode()),
                base.withRuntimeProfileId(runtime.profileId()),
                runtime.profileId(),
                profiles,
                System.currentTimeMillis()
        );
    }

    private ProjectSession resolveSession(String projectId) {
        String id = normalizeProjectId(projectId);
        ProjectSession session = sessions.get(id);
        if (session != null) {
            return session;
        }
        return sessions.get(normalizeProjectId(activeProjectId));
    }

    private void touch(String projectId) {
        ProjectSession session = sessions.get(normalizeProjectId(projectId));
        if (session != null) {
            session.updatedAt = System.currentTimeMillis();
        }
    }

    private static LinkedHashMap<String, RuntimeProfileDto> mapProfiles(List<RuntimeProfileDto> profiles) {
        LinkedHashMap<String, RuntimeProfileDto> out = new LinkedHashMap<>();
        if (profiles == null) {
            return out;
        }
        for (RuntimeProfileDto profile : profiles) {
            RuntimeProfileDto normalized = normalizeProfile(profile);
            if (normalized != null) {
                out.put(normalized.profileId(), normalized);
            }
        }
        return out;
    }

    private static RuntimeProfileDto normalizeProfile(RuntimeProfileDto profile) {
        if (profile == null) {
            return null;
        }
        String id = normalizeRuntimeId(profile.profileId());
        String name = normalizeLabel(profile.profileName(), id);
        String path = normalizePathText(profile.runtimePath());
        boolean valid = path.isBlank() || Files.exists(Paths.get(path));
        return new RuntimeProfileDto(id, name, path, profile.readOnly(), valid);
    }

    private static RuntimeProfileDto buildDefaultRuntimeProfile() {
        Path runtimePath = detectCurrentRuntimeArchive();
        String path = runtimePath == null ? "" : runtimePath.toString();
        String label = runtimePath == null ? "Current JVM Runtime" : "Current JVM Runtime (" + runtimePath.getFileName() + ")";
        return new RuntimeProfileDto(DEFAULT_RUNTIME_ID, label, path, true, runtimePath != null);
    }

    private static Path detectCurrentRuntimeArchive() {
        try {
            String javaHomeRaw = System.getProperty("java.home");
            if (javaHomeRaw == null || javaHomeRaw.isBlank()) {
                return null;
            }
            Path home = Paths.get(javaHomeRaw).toAbsolutePath().normalize();
            Path rtJar = home.resolve(Paths.get("lib", "rt.jar"));
            if (Files.isRegularFile(rtJar)) {
                return rtJar;
            }
            Path jreRt = home.resolve(Paths.get("jre", "lib", "rt.jar"));
            if (Files.isRegularFile(jreRt)) {
                return jreRt;
            }
            Path modules = home.resolve(Paths.get("lib", "modules"));
            if (Files.isRegularFile(modules)) {
                return modules;
            }
            return home;
        } catch (Exception ex) {
            logger.debug("detect runtime archive fail: {}", ex.toString());
            return null;
        }
    }

    private static String deriveProjectId(String inputPath, String mode) {
        String seed = normalizePathText(inputPath) + "#" + safe(mode).toLowerCase(Locale.ROOT);
        if (seed.isBlank()) {
            return DEFAULT_PROJECT_ID;
        }
        return UUID.nameUUIDFromBytes(seed.getBytes()).toString();
    }

    private static String deriveProjectName(String inputPath, boolean projectMode) {
        Path path = normalizePath(inputPath);
        if (path != null && path.getFileName() != null) {
            return path.getFileName().toString();
        }
        if (projectMode) {
            return "project";
        }
        return "artifact";
    }

    private static String normalizeProjectId(String projectId) {
        String id = safe(projectId);
        return id.isBlank() ? DEFAULT_PROJECT_ID : id;
    }

    private static String normalizeRuntimeId(String runtimeProfileId) {
        String id = safe(runtimeProfileId);
        return id.isBlank() ? DEFAULT_RUNTIME_ID : id;
    }

    private static String normalizeLabel(String value, String fallback) {
        String out = safe(value);
        return out.isBlank() ? fallback : out;
    }

    private static String normalizePathText(String value) {
        Path path = normalizePath(value);
        return path == null ? safe(value) : path.toString();
    }

    private static Path normalizePath(String value) {
        String text = safe(value);
        if (text.isBlank()) {
            return null;
        }
        try {
            return Paths.get(text).toAbsolutePath().normalize();
        } catch (Exception ex) {
            return null;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class ProjectSession {
        private final String projectId;
        private String projectName;
        private BuildSettingsDto settings;
        private String selectedRuntimeProfileId;
        private final LinkedHashMap<String, RuntimeProfileDto> runtimeProfiles;
        private long updatedAt;

        private ProjectSession(String projectId,
                               String projectName,
                               BuildSettingsDto settings,
                               String selectedRuntimeProfileId,
                               LinkedHashMap<String, RuntimeProfileDto> runtimeProfiles,
                               long updatedAt) {
            this.projectId = normalizeProjectId(projectId);
            this.projectName = normalizeLabel(projectName, this.projectId);
            this.settings = settings == null ? BuildSettingsDto.defaults().withProjectId(this.projectId) : settings.withProjectId(this.projectId);
            this.selectedRuntimeProfileId = normalizeRuntimeId(selectedRuntimeProfileId);
            this.runtimeProfiles = runtimeProfiles == null ? new LinkedHashMap<>() : runtimeProfiles;
            this.updatedAt = updatedAt;
        }
    }
}
