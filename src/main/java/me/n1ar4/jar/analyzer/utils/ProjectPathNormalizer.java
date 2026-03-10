/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.utils;

import java.nio.file.Path;

public final class ProjectPathNormalizer {
    private ProjectPathNormalizer() {
    }

    public static Path normalizeNullablePath(Path path) {
        if (path == null) {
            return null;
        }
        try {
            if (OSUtil.isWindows() && isRootRelativeWithoutDrive(path)) {
                return path.normalize();
            }
            return path.toAbsolutePath().normalize();
        } catch (Exception ex) {
            return path.normalize();
        }
    }

    private static boolean isRootRelativeWithoutDrive(Path path) {
        String value = path.toString();
        if (value == null || value.isBlank()) {
            return false;
        }
        if (value.startsWith("\\\\") || value.startsWith("//")) {
            return false;
        }
        return value.startsWith("\\") || value.startsWith("/");
    }
}
