package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.engine.project.ProjectBuildMode;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.engine.project.ProjectOrigin;
import me.n1ar4.jar.analyzer.engine.project.ProjectRoot;
import me.n1ar4.jar.analyzer.engine.project.ProjectRootKind;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JspCompileRunnerTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldCompileSimpleJspIntoClassFile() throws Exception {
        Path classesDir = tempDir.resolve(Paths.get("target", "classes"));
        Path webappDir = tempDir.resolve(Paths.get("src", "main", "webapp"));
        Files.createDirectories(classesDir);
        Files.createDirectories(webappDir);
        Files.writeString(webappDir.resolve("index.jsp"), """
                <%@ page contentType="text/html;charset=UTF-8" %>
                <html><body>Hello JSP</body></html>
                """, StandardCharsets.UTF_8);

        ProjectModel model = new ProjectModel(
                ProjectBuildMode.PROJECT,
                classesDir,
                null,
                List.of(
                        new ProjectRoot(ProjectRootKind.CONTENT_ROOT, ProjectOrigin.APP, tempDir, "", false, false, 10),
                        new ProjectRoot(ProjectRootKind.RESOURCE_ROOT, ProjectOrigin.APP, webappDir, "", false, false, 26),
                        new ProjectRoot(ProjectRootKind.GENERATED, ProjectOrigin.GENERATED, classesDir, "", false, false, 15)
                ),
                List.of(),
                false
        );

        List<ClassFileEntity> classFiles = JspCompileRunner.compile(model, List.of(), List.of(classesDir.toString()));

        assertFalse(classFiles.isEmpty());
        assertTrue(classFiles.stream().anyMatch(cf -> cf.getClassName() != null
                && cf.getClassName().contains("org/apache/jsp")
                && cf.getClassName().endsWith(".class")));
    }
}
