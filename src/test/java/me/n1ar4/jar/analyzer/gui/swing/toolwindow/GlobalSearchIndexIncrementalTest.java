/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.toolwindow;

import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.core.facts.JarEntity;
import me.n1ar4.jar.analyzer.core.facts.ResourceEntity;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalSearchIndexIncrementalTest {
    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
        ProjectRuntimeContext.clear();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void incrementalSearchShouldPickUpDatabaseManagerMutations() throws Exception {
        Path jar = FixtureJars.springbootTestJar();
        ProjectRuntimeContext.updateResolveInnerJars(false);
        CoreRunner.run(jar, null, false, null);

        Class<?> indexClass = Class.forName(
                "me.n1ar4.jar.analyzer.gui.swing.toolwindow.GlobalSearchDialog$GlobalSearchIndex");
        Class<Enum> categoryClass = (Class<Enum>) Class.forName(
                "me.n1ar4.jar.analyzer.gui.swing.toolwindow.GlobalSearchDialog$CategoryItem");
        Class<?> runClass = Class.forName(
                "me.n1ar4.jar.analyzer.gui.swing.toolwindow.GlobalSearchDialog$SearchRun");

        Field instanceField = indexClass.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        Object index = instanceField.get(null);

        Method searchMethod = indexClass.getDeclaredMethod(
                "search", String.class, categoryClass, int.class, boolean.class);
        searchMethod.setAccessible(true);
        Method hitsMethod = runClass.getDeclaredMethod("hits");
        hitsMethod.setAccessible(true);

        Enum allCategory = Enum.valueOf(categoryClass, "ALL");
        Enum resourceCategory = Enum.valueOf(categoryClass, "RESOURCE");

        searchMethod.invoke(index, "CallbackEntry", allCategory, 256, true);

        String marker = "incmarker" + Long.toUnsignedString(System.nanoTime(), 36);
        insertSyntheticResourceMarker(marker);
        Object incrementalRun = searchMethod.invoke(index, marker, resourceCategory, 64, false);
        List<?> hits = (List<?>) hitsMethod.invoke(incrementalRun);

        assertFalse(hits == null || hits.isEmpty());
    }

    private static void insertSyntheticResourceMarker(String marker) {
        List<ResourceEntity> resources = new ArrayList<>(DatabaseManager.getResources());
        int nextRid = 1;
        for (ResourceEntity row : resources) {
            if (row == null) {
                continue;
            }
            nextRid = Math.max(nextRid, row.getRid() + 1);
        }
        int jarId = 0;
        String jarName = "";
        List<JarEntity> jars = DatabaseManager.getJarsMeta();
        if (jars != null && !jars.isEmpty()) {
            JarEntity jar = jars.get(0);
            if (jar != null) {
                jarId = jar.getJid();
                jarName = safe(jar.getJarName());
            }
        }
        ResourceEntity markerRow = new ResourceEntity();
        markerRow.setRid(nextRid);
        markerRow.setResourcePath("test/marker/" + marker + ".txt");
        markerRow.setPathStr("");
        markerRow.setJarId(jarId);
        markerRow.setJarName(jarName);
        markerRow.setFileSize(marker.length());
        markerRow.setIsText(1);
        resources.add(markerRow);
        DatabaseManager.saveResources(resources);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
