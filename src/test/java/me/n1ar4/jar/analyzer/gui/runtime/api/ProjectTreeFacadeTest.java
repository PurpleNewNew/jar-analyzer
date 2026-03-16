/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.core.facts.ClassFileEntity;
import me.n1ar4.jar.analyzer.core.facts.JarEntity;
import me.n1ar4.jar.analyzer.core.facts.ResourceEntity;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.gui.runtime.model.NavigationTargetDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingWindowPayload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectTreeFacadeTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
        ProjectRuntimeContext.clear();
        EngineContext.setEngine(null);
    }

    @Test
    void snapshotShouldBeEmptyWhenEngineMissing() {
        EngineContext.setEngine(null);
        assertTrue(RuntimeFacades.projectTree().snapshot().isEmpty());
        assertTrue(RuntimeFacades.projectTree().search("abc").isEmpty());
    }

    @Test
    void supportShouldBuildSemanticRootTreeWhenProjectModelReady() {
        ProjectModel model = ProjectModel.artifact(
                tempDir,
                null,
                List.of(tempDir),
                false
        );
        ProjectRuntimeSnapshot snapshot = DatabaseManager.buildProjectRuntimeSnapshot(
                9L,
                model,
                List.of(tempDir.toString()),
                java.util.Set.of(),
                java.util.Set.of(),
                java.util.Set.of(),
                java.util.Map.of(),
                java.util.Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        DatabaseManager.restoreProjectRuntime(snapshot);
        ProjectRuntimeContext.restoreProjectRuntime("", snapshot.buildSeq(), model);

        ProjectTreeSupport support = new ProjectTreeSupport(ProjectTreeSupport.UiActions.noop());
        List<me.n1ar4.jar.analyzer.gui.runtime.model.TreeNodeDto> nodes = support.buildTree(
                null,
                new ProjectTreeSupport.TreeSettings(false, false, false)
        );

        assertTrue(!nodes.isEmpty());
        assertEquals("App", nodes.get(0).label());
        assertTrue(nodes.get(0).directory());
        assertTrue(nodes.get(0).children().stream().anyMatch(node -> "Roots".equals(node.label())));
    }

    @Test
    void resourceTargetShouldOpenTextPreviewInEditorContent() throws IOException {
        Path file = tempDir.resolve("application.properties");
        String text = "server.port=8080\n";
        Files.writeString(file, text, StandardCharsets.UTF_8);

        ResourceEntity resource = new ResourceEntity();
        resource.setRid(12);
        resource.setResourcePath("BOOT-INF/classes/application.properties");
        resource.setPathStr(file.toString());
        resource.setJarName("app.jar");
        resource.setJarId(3);
        resource.setFileSize(Files.size(file));
        resource.setIsText(1);

        AtomicReference<ToolingWindowPayload.EditorContentPayload> opened = new AtomicReference<>();
        ProjectTreeSupport support = new ProjectTreeSupport(
                contentActions(opened),
                supportServices(List.of(resource), List.of())
        );

        support.openTarget(NavigationTargetDto.resourceTarget(12));

        ToolingWindowPayload.EditorContentPayload payload = opened.get();
        assertNotNull(payload);
        assertEquals("resource:12", payload.tabKey());
        assertEquals("application.properties", payload.title());
        assertEquals(text, payload.content());
        assertFalse(payload.image());
        assertTrue(payload.statusText().contains("BOOT-INF/classes/application.properties"));
        assertTrue(payload.statusText().contains("[app.jar]"));
        assertEquals(file.toAbsolutePath().normalize().toString(), payload.filePath());
    }

    @Test
    void filePathImageShouldOpenImagePreviewInEditorContent() throws IOException {
        Path file = tempDir.resolve("preview.png");
        BufferedImage image = new BufferedImage(4, 3, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(image, "png", file.toFile());

        AtomicReference<ToolingWindowPayload.EditorContentPayload> opened = new AtomicReference<>();
        ProjectTreeSupport support = new ProjectTreeSupport(
                contentActions(opened),
                supportServices(List.of(), List.of())
        );

        support.openTarget(NavigationTargetDto.filePathTarget(file.toString()));

        ToolingWindowPayload.EditorContentPayload payload = opened.get();
        assertNotNull(payload);
        assertEquals("path:" + file.toAbsolutePath().normalize(), payload.tabKey());
        assertEquals("preview.png", payload.title());
        assertTrue(payload.image());
        assertEquals(file.toAbsolutePath().normalize().toString(), payload.filePath());
        assertTrue(payload.statusText().contains(file.toAbsolutePath().normalize().toString()));
    }

    private static ProjectTreeSupport.UiActions contentActions(AtomicReference<ToolingWindowPayload.EditorContentPayload> opened) {
        return new ProjectTreeSupport.UiActions() {
            @Override
            public void openClass(String className, Integer jarId) {
            }

            @Override
            public void openContent(ToolingWindowPayload.EditorContentPayload payload) {
                opened.set(payload);
            }
        };
    }

    private static ProjectTreeSupport.Services supportServices(List<ResourceEntity> resources, List<JarEntity> jars) {
        ConfigFile config = new ConfigFile();
        config.setDbPath("target/test-db");
        CoreEngine engine = new CoreEngine(config) {
            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public ResourceEntity getResourceById(int rid) {
                return resources.stream()
                        .filter(row -> row != null && row.getRid() == rid)
                        .findFirst()
                        .orElse(null);
            }
        };
        return new ProjectTreeSupport.Services() {
            @Override
            public List<ClassFileEntity> classFiles() {
                return List.of();
            }

            @Override
            public List<ResourceEntity> resources() {
                return resources;
            }

            @Override
            public List<JarEntity> jarMeta() {
                return jars;
            }

            @Override
            public ProjectModel runtimeProjectModel() {
                return null;
            }

            @Override
            public long runtimeBuildSeq() {
                return 0L;
            }

            @Override
            public CoreEngine currentEngine() {
                return engine;
            }

            @Override
            public boolean isForceTargetJar(String fileName) {
                return false;
            }

            @Override
            public boolean isSdkJar(String fileName) {
                return false;
            }

            @Override
            public boolean isCommonLibraryJar(String fileName) {
                return false;
            }
        };
    }
}
