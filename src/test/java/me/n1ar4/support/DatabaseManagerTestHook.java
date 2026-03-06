package me.n1ar4.support;

import me.n1ar4.jar.analyzer.core.DatabaseManager;

import java.lang.reflect.Method;

public final class DatabaseManagerTestHook {
    private DatabaseManagerTestHook() {
    }

    public static void setBuilding(boolean building) {
        invoke("setBuilding", new Class[]{boolean.class}, building);
    }

    public static void markProjectBuildReady(long buildSeq) {
        invoke("markProjectBuildReady", new Class[]{long.class}, buildSeq);
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
