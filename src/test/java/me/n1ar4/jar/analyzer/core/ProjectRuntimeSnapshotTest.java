package me.n1ar4.jar.analyzer.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ProjectRuntimeSnapshotTest {
    @Test
    void plainCollectionsShouldStillBeCopiedForImmutability() {
        List<ProjectRuntimeSnapshot.ProjectRootData> roots = new ArrayList<>();
        roots.add(new ProjectRuntimeSnapshot.ProjectRootData(
                "CONTENT_ROOT",
                "APP",
                "demo.jar",
                "demo.jar",
                true,
                false,
                0
        ));
        List<String> archives = new ArrayList<>(List.of("demo.jar"));
        ProjectRuntimeSnapshot.ProjectModelData modelData = new ProjectRuntimeSnapshot.ProjectModelData(
                "ARTIFACT",
                "demo.jar",
                "",
                roots,
                archives,
                false
        );
        List<ProjectRuntimeSnapshot.JarData> jars = new ArrayList<>();
        jars.add(new ProjectRuntimeSnapshot.JarData(1, "demo.jar", "demo.jar"));
        Map<String, List<String>> methodStrings = new LinkedHashMap<>();
        methodStrings.put("demo#run()V@1", new ArrayList<>(List.of("alpha")));
        Set<String> servlets = new LinkedHashSet<>(Set.of("demo/Servlet"));

        ProjectRuntimeSnapshot snapshot = new ProjectRuntimeSnapshot(
                ProjectRuntimeSnapshot.CURRENT_SCHEMA_VERSION,
                7L,
                modelData,
                jars,
                List.of(),
                List.of(),
                List.of(),
                methodStrings,
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Set.of(),
                servlets,
                Set.of(),
                Set.of()
        );

        roots.add(new ProjectRuntimeSnapshot.ProjectRootData("CONTENT_ROOT", "APP", "extra.jar", "extra.jar", true, false, 1));
        archives.add("extra.jar");
        jars.add(new ProjectRuntimeSnapshot.JarData(2, "extra.jar", "extra.jar"));
        methodStrings.put("demo#extra()V@1", List.of("beta"));
        servlets.add("demo/OtherServlet");

        assertEquals(1, snapshot.projectModel().roots().size());
        assertEquals(1, snapshot.projectModel().analyzedArchives().size());
        assertEquals(1, snapshot.jars().size());
        assertEquals(1, snapshot.methodStrings().size());
        assertEquals(1, snapshot.servlets().size());
    }

    @Test
    void ownedCollectionsShouldBeReusedWithoutSecondCopy() {
        List<ProjectRuntimeSnapshot.ProjectRootData> roots = ProjectRuntimeSnapshot.ownedList(new ArrayList<>(List.of(
                new ProjectRuntimeSnapshot.ProjectRootData(
                        "CONTENT_ROOT",
                        "APP",
                        "demo.jar",
                        "demo.jar",
                        true,
                        false,
                        0
                )
        )));
        List<String> archives = ProjectRuntimeSnapshot.ownedList(new ArrayList<>(List.of("demo.jar")));
        ProjectRuntimeSnapshot.ProjectModelData modelData = new ProjectRuntimeSnapshot.ProjectModelData(
                "ARTIFACT",
                "demo.jar",
                "",
                roots,
                archives,
                false
        );
        List<ProjectRuntimeSnapshot.JarData> jars = ProjectRuntimeSnapshot.ownedList(new ArrayList<>(List.of(
                new ProjectRuntimeSnapshot.JarData(1, "demo.jar", "demo.jar")
        )));
        Map<String, List<String>> methodStrings = new LinkedHashMap<>();
        methodStrings.put("demo#run()V@1", List.of("alpha"));
        Map<String, List<String>> ownedMethodStrings = ProjectRuntimeSnapshot.ownedMap(methodStrings);
        Set<String> servlets = ProjectRuntimeSnapshot.ownedSet(new LinkedHashSet<>(Set.of("demo/Servlet")));

        ProjectRuntimeSnapshot snapshot = new ProjectRuntimeSnapshot(
                ProjectRuntimeSnapshot.CURRENT_SCHEMA_VERSION,
                7L,
                modelData,
                jars,
                List.of(),
                List.of(),
                List.of(),
                ownedMethodStrings,
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Set.of(),
                servlets,
                Set.of(),
                Set.of()
        );

        assertSame(roots, snapshot.projectModel().roots());
        assertSame(archives, snapshot.projectModel().analyzedArchives());
        assertSame(jars, snapshot.jars());
        assertSame(ownedMethodStrings, snapshot.methodStrings());
        assertSame(servlets, snapshot.servlets());
    }
}
