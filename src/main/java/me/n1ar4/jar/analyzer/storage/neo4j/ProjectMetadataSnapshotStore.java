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
import com.alibaba.fastjson2.TypeReference;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ProjectMetadataSnapshotStore {
    private static final Logger logger = LogManager.getLogger();
    private static final ProjectMetadataSnapshotStore INSTANCE = new ProjectMetadataSnapshotStore();
    private static final String SNAPSHOT_FILE = "runtime-metadata.json";

    private ProjectMetadataSnapshotStore() {
    }

    public static ProjectMetadataSnapshotStore getInstance() {
        return INSTANCE;
    }

    public void write(String projectKey, ProjectRuntimeSnapshot snapshot) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        writeToHome(Neo4jProjectStore.getInstance().resolveProjectHome(normalized), snapshot);
    }

    public void writeToHome(Path projectHome, ProjectRuntimeSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("project_runtime_snapshot_missing");
        }
        Path target = resolveSnapshotFile(projectHome);
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.createDirectories(target.getParent());
            String json = JSON.toJSONString(snapshot);
            Files.writeString(temp, json, StandardCharsets.UTF_8);
            try {
                Files.move(temp, target,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception ex) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ex) {
            try {
                Files.deleteIfExists(temp);
            } catch (Exception ignored) {
                // best effort
            }
            logger.error("persist project runtime snapshot fail: home={} err={}", projectHome, ex.toString(), ex);
            throw new IllegalStateException("project_runtime_snapshot_write_failed", ex);
        }
    }

    public boolean restoreIntoRuntime(String projectKey) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        ProjectRuntimeSnapshot snapshot = read(normalized);
        if (snapshot == null) {
            WorkspaceContext.clear();
            return false;
        }
        DatabaseManager.restoreProjectRuntime(snapshot);
        ProjectModel model = DatabaseManager.getProjectModel();
        WorkspaceContext.setProjectModel(model == null ? ProjectModel.empty() : model);
        return true;
    }

    public ProjectRuntimeSnapshot read(String projectKey) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        Path target = resolveSnapshotFile(normalized);
        if (!Files.exists(target)) {
            return null;
        }
        try {
            String raw = Files.readString(target, StandardCharsets.UTF_8);
            if (raw == null || raw.isBlank()) {
                return null;
            }
            ProjectRuntimeSnapshot snapshot = JSON.parseObject(
                    raw,
                    new TypeReference<ProjectRuntimeSnapshot>() {
                    }
            );
            if (snapshot == null) {
                return null;
            }
            if (snapshot.schemaVersion() > ProjectRuntimeSnapshot.CURRENT_SCHEMA_VERSION) {
                logger.warn("skip project runtime snapshot with newer schema: key={} version={}",
                        normalized, snapshot.schemaVersion());
                return null;
            }
            return snapshot;
        } catch (Exception ex) {
            logger.warn("read project runtime snapshot fail: key={} err={}", normalized, ex.toString());
            return null;
        }
    }

    Path resolveSnapshotFile(String projectKey) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        return resolveSnapshotFile(Neo4jProjectStore.getInstance().resolveProjectHome(normalized));
    }

    Path resolveSnapshotFile(Path projectHome) {
        if (projectHome == null) {
            throw new IllegalArgumentException("project_home_missing");
        }
        return projectHome.resolve(SNAPSHOT_FILE);
    }
}
