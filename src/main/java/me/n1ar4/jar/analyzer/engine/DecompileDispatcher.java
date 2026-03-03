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
        return CFRDecompileEngine.decompile(path.toAbsolutePath().toString());
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

    public static String stripPrefix(String code) {
        if (code == null) {
            return null;
        }
        String cfrPrefix = CFRDecompileEngine.getCFR_PREFIX();
        if (cfrPrefix != null && code.startsWith(cfrPrefix)) {
            return code.substring(cfrPrefix.length());
        }
        return code;
    }

    private static boolean isModuleInfoPath(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        String name = path.getFileName().toString();
        return "module-info.class".equalsIgnoreCase(name);
    }

}
