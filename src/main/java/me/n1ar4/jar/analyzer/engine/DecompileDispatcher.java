/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.engine;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.notify.NotifierContext;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.file.Path;
import java.util.List;

public final class DecompileDispatcher {
    private static final Logger logger = LogManager.getLogger();

    private DecompileDispatcher() {
    }

    public static String decompile(Path path) {
        Path decompilePath = resolveDecompilePath(path);
        if (decompilePath == null) {
            return null;
        }
        return CFRDecompileEngine.decompile(decompilePath.toString());
    }

    public static CFRDecompileEngine.CfrDecompileResult decompileWithLineMapping(Path path) {
        Path decompilePath = resolveDecompilePath(path);
        if (decompilePath == null) {
            return null;
        }
        return CFRDecompileEngine.decompileWithLineMapping(decompilePath.toString());
    }

    public static boolean decompileJars(List<String> jarsPath, String outputDir) {
        return decompileJars(jarsPath, outputDir, false);
    }

    public static boolean decompileJars(List<String> jarsPath, String outputDir, boolean decompileNested) {
        if (jarsPath == null || jarsPath.isEmpty()) {
            return false;
        }
        return CFRDecompileEngine.decompileJars(jarsPath, outputDir, decompileNested);
    }

    private static Path resolveDecompilePath(Path path) {
        if (path == null) {
            return null;
        }
        if (DatabaseManager.isBuilding()) {
            logger.info("decompile blocked during build");
            NotifierContext.get().warn("Jar Analyzer",
                    "Build is running, index not ready.\n构建中索引未完成，已禁止反编译。");
            return null;
        }
        if (isModuleInfoPath(path)) {
            return null;
        }
        return path.toAbsolutePath();
    }

    private static boolean isModuleInfoPath(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        String name = path.getFileName().toString();
        return "module-info.class".equalsIgnoreCase(name);
    }

}
