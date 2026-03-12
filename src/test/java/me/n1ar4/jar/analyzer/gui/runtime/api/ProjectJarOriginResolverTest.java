/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.engine.project.ProjectBuildMode;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.engine.project.ProjectOrigin;
import me.n1ar4.jar.analyzer.engine.project.ProjectRoot;
import me.n1ar4.jar.analyzer.engine.project.ProjectRootKind;
import me.n1ar4.jar.analyzer.core.facts.JarEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProjectJarOriginResolverTest {
    @TempDir
    Path tempDir;

    @Test
    public void shouldDeriveAppLibraryAndSdkOriginsFromProjectModel() {
        Path appRoot = tempDir.resolve("app");
        Path sdkRoot = tempDir.resolve("sdk");
        Path projectJar = appRoot.resolve("primary.jar");
        Path libraryJar = tempDir.resolve("libs").resolve("extra.jar");
        Path sdkJar = sdkRoot.resolve("custom-sdk.jar");
        ProjectModel model = ProjectModel.builder()
                .buildMode(ProjectBuildMode.PROJECT)
                .primaryInputPath(projectJar)
                .runtimePath(sdkRoot)
                .addRoot(new ProjectRoot(
                        ProjectRootKind.CONTENT_ROOT,
                        ProjectOrigin.APP,
                        appRoot,
                        "",
                        false,
                        false,
                        10
                ))
                .addRoot(new ProjectRoot(
                        ProjectRootKind.SDK,
                        ProjectOrigin.SDK,
                        sdkRoot,
                        "",
                        false,
                        false,
                        20
                ))
                .addAnalyzedArchive(projectJar)
                .addAnalyzedArchive(libraryJar)
                .addAnalyzedArchive(sdkJar)
                .build();

        ProjectJarOriginResolver resolver = ProjectJarOriginResolver.fromProjectModel(model, List.of(
                jar(1, "primary.jar", projectJar),
                jar(2, "extra.jar", libraryJar),
                jar(3, "custom-sdk.jar", sdkJar)
        ));

        assertEquals(ProjectOrigin.APP, resolver.resolve(1));
        assertEquals(ProjectOrigin.LIBRARY, resolver.resolve(2));
        assertEquals(ProjectOrigin.SDK, resolver.resolve(3));
    }

    @Test
    public void shouldDefaultToAppWhenJarIdIsMissing() {
        assertEquals(ProjectOrigin.APP, ProjectJarOriginResolver.empty().resolve((Integer) null));
        assertEquals(ProjectOrigin.APP, ProjectJarOriginResolver.empty().resolve(0));
    }

    private static JarEntity jar(int id, String name, Path path) {
        JarEntity jar = new JarEntity();
        jar.setJid(id);
        jar.setJarName(name);
        jar.setJarAbsPath(path.toString());
        return jar;
    }
}
