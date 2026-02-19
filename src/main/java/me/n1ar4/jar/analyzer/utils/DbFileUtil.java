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

import me.n1ar4.jar.analyzer.meta.CompatibilityCode;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class DbFileUtil {
    private static final Logger logger = LogManager.getLogger();
    private static final String LEGACY_DB_FILE = "jar-analyzer.db";
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
            return;
        }
        migrateLegacyDbIfNeeded(dbPath);
    }

    @CompatibilityCode(
            primary = "Delete artifacts under Const.dbFile",
            reason = "Keep cleanup of historical default db path artifacts for backward compatibility"
    )
    public static int deleteDbFiles() {
        int deleted = deleteDbArtifacts(resolveDbPath(), true);
        Path legacy = resolveLegacyDbPath();
        if (!legacy.equals(resolveDbPath())) {
            deleted += deleteDbArtifacts(legacy, true);
        }
        return deleted;
    }

    @CompatibilityCode(
            primary = "Delete sidecars under Const.dbFile",
            reason = "Keep cleanup of historical default db sidecars for backward compatibility"
    )
    public static int deleteDbSidecars() {
        int deleted = deleteDbArtifacts(resolveDbPath(), false);
        Path legacy = resolveLegacyDbPath();
        if (!legacy.equals(resolveDbPath())) {
            deleted += deleteDbArtifacts(legacy, false);
        }
        return deleted;
    }

    private static Path resolveDbPath() {
        return Paths.get(Const.dbFile).toAbsolutePath().normalize();
    }

    private static Path resolveLegacyDbPath() {
        return Paths.get(LEGACY_DB_FILE).toAbsolutePath().normalize();
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

    @CompatibilityCode(
            primary = "Const.dbFile location",
            reason = "Older versions wrote DB beside process root; keep one-time migration bridge to configured db path"
    )
    private static void migrateLegacyDbIfNeeded(Path targetDbPath) {
        Path legacyDbPath = resolveLegacyDbPath();
        if (legacyDbPath.equals(targetDbPath)) {
            return;
        }
        if (!Files.exists(legacyDbPath)) {
            return;
        }
        if (Files.exists(targetDbPath)) {
            return;
        }
        Path targetParent = targetDbPath.getParent();
        if (targetParent == null || !Files.isDirectory(targetParent)) {
            return;
        }
        logger.info("compat migration: move legacy db artifacts from {} to {}", legacyDbPath, targetParent);
        for (Path oldFile : collectDbArtifacts(legacyDbPath, true)) {
            try {
                Path fileName = oldFile.getFileName();
                if (fileName == null) {
                    continue;
                }
                Path newFile = targetParent.resolve(fileName.toString());
                Files.move(oldFile, newFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                logger.debug("migrate legacy db file fail: {}: {}", oldFile, ex.toString());
            }
        }
    }
}
