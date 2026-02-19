/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.utils;

import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class DbFileUtil {
    private static final Logger logger = LogManager.getLogger();
    private static final String[] SIDECAR_SUFFIXES = new String[]{"-wal", "-shm", "-journal"};
    private static final String MASTER_JOURNAL_GLOB = "-mj*";

    private DbFileUtil() {
    }

    public static void ensureDbDirectory() {
        Path dbPath = Paths.get(Const.dbFile).toAbsolutePath().normalize();
        Path parent = dbPath.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException ex) {
            logger.warn("create db dir fail: {}", ex.toString());
        }
    }

    public static int deleteDbFiles() {
        return deleteDbArtifacts(resolveDbPath(), true);
    }

    public static int deleteDbSidecars() {
        return deleteDbArtifacts(resolveDbPath(), false);
    }

    private static Path resolveDbPath() {
        return Paths.get(Const.dbFile).toAbsolutePath().normalize();
    }

    private static int deleteDbArtifacts(Path dbPath, boolean includeMain) {
        int deleted = 0;
        for (Path file : collectDbArtifacts(dbPath, includeMain)) {
            try {
                if (Files.deleteIfExists(file)) {
                    deleted++;
                }
            } catch (IOException ex) {
                logger.debug("delete db artifact fail: {}: {}", file, ex.toString());
            }
        }
        return deleted;
    }

    private static List<Path> collectDbArtifacts(Path dbPath, boolean includeMain) {
        List<Path> files = new ArrayList<>();
        Path fileName = dbPath.getFileName();
        if (fileName == null) {
            return files;
        }
        String baseName = fileName.toString();
        Path parent = dbPath.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            return files;
        }
        if (includeMain) {
            files.add(parent.resolve(baseName));
        }
        for (String suffix : SIDECAR_SUFFIXES) {
            files.add(parent.resolve(baseName + suffix));
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent, baseName + MASTER_JOURNAL_GLOB)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    files.add(path);
                }
            }
        } catch (IOException ex) {
            logger.debug("scan db master journal fail: {}", ex.toString());
        }
        return files;
    }

}
