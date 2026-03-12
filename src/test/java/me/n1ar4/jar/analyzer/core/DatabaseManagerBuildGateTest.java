package me.n1ar4.jar.analyzer.core;

import me.n1ar4.support.DatabaseManagerTestHook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseManagerBuildGateTest {
    @AfterEach
    void cleanup() {
        DatabaseManagerTestHook.finishBuild(false);
        DatabaseManager.clearAllData();
    }

    @Test
    void beginBuildShouldRejectOverlappingBuilds() {
        long firstBuildSeq = DatabaseManager.beginBuild("build-gate-first");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> DatabaseManager.beginBuild("build-gate-second"));

        assertEquals("project_build_in_progress", ex.getMessage());
        assertTrue(DatabaseManager.isBuilding("build-gate-first"));
        assertFalse(DatabaseManager.isBuilding("build-gate-second"));

        DatabaseManagerTestHook.finishBuild("build-gate-first", firstBuildSeq, false);
        assertFalse(DatabaseManager.isBuilding());
    }

    @Test
    void finishBuildShouldIgnoreStaleSessionToken() {
        long firstBuildSeq = DatabaseManager.beginBuild("build-gate-owner");

        DatabaseManagerTestHook.finishBuild("build-gate-other", firstBuildSeq, false);

        assertTrue(DatabaseManager.isBuilding("build-gate-owner"));
        assertFalse(DatabaseManager.isBuilding("build-gate-other"));

        DatabaseManagerTestHook.finishBuild("build-gate-owner", firstBuildSeq, false);
        assertFalse(DatabaseManager.isBuilding());
    }
}
