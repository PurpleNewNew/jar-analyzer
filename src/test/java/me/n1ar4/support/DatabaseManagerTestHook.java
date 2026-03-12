package me.n1ar4.support;

import me.n1ar4.jar.analyzer.core.DatabaseManager;

import java.lang.reflect.Method;

public final class DatabaseManagerTestHook {
    private DatabaseManagerTestHook() {
    }

    public static void finishBuild() {
        invoke("finishBuild", new Class[0]);
    }

    public static void finishBuild(boolean buildCommitted) {
        invoke("finishBuild", new Class[]{boolean.class}, buildCommitted);
    }

    public static void finishBuild(String projectKey, long buildSeq, boolean buildCommitted) {
        invoke("finishBuild", new Class[]{String.class, long.class, boolean.class}, projectKey, buildSeq, buildCommitted);
    }

    private static void invoke(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = DatabaseManager.class.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            method.invoke(null, args);
        } catch (Exception ex) {
            throw new IllegalStateException("database_manager_test_hook_failed: " + methodName, ex);
        }
    }
}
