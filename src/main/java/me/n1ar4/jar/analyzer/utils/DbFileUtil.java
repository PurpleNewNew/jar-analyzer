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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class DbFileUtil {
    private static final Logger logger = LogManager.getLogger();
    private static final String[] TRANSIENT_DIRS = new String[]{"logs", "transactions", "tmp"};

    private DbFileUtil() {
    }

    public static void ensureDbDirectory() {
        try {
            Files.createDirectories(resolveNeo4jHomePath());
        } catch (IOException ex) {
            logger.warn("create neo4j home dir fail: {}", ex.toString());
        }
    }

    public static int deleteDbFiles() {
        return deleteRecursively(resolveNeo4jHomePath(), true);
    }

    public static int deleteDbSidecars() {
        int deleted = 0;
        Path home = resolveNeo4jHomePath();
        for (String dirName : TRANSIENT_DIRS) {
            deleted += deleteRecursively(home.resolve(dirName), true);
        }
        return deleted;
    }

    private static Path resolveNeo4jHomePath() {
        return Paths.get(Const.neo4jHome).toAbsolutePath().normalize();
    }

    private static int deleteRecursively(Path root, boolean includeRoot) {
        if (root == null || !Files.exists(root)) {
            return 0;
        }
        int deleted = 0;
        List<Path> paths = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder()).forEach(paths::add);
        } catch (IOException ex) {
            logger.debug("scan neo4j home fail: {}: {}", root, ex.toString());
            return 0;
        }
        for (Path path : paths) {
            if (!includeRoot && path.equals(root)) {
                continue;
            }
            try {
                if (Files.deleteIfExists(path)) {
                    deleted++;
                }
            } catch (IOException ex) {
                logger.debug("delete neo4j file fail: {}: {}", path, ex.toString());
            }
        }
        return deleted;
    }
}
