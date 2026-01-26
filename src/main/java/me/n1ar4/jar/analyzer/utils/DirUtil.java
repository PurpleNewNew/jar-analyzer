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

import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("all")
public class DirUtil {
    private static final Logger logger = LogManager.getLogger();

    public static List<String> GetFiles(String path) {
        if (path == null || path.trim().isEmpty()) {
            return new ArrayList<>();
        }
        Path root = Paths.get(path);
        List<String> results = new ArrayList<>();
        try {
            if (Files.isDirectory(root)) {
                try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
                    stream.filter(Files::isRegularFile)
                            .map(p -> p.toAbsolutePath().toString())
                            .forEach(results::add);
                }
            } else if (Files.exists(root)) {
                results.add(root.toAbsolutePath().toString());
            }
        } catch (Exception e) {
            logger.warn("get files error: {}", e.toString());
        }
        return results;
    }

    public static boolean removeDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = removeDir(new File(dir, child));
                    if (!success) {
                        logger.debug("remove dir {} not success", dir.toString());
                        // 由于 DLL 文件不能删除
                        // 这里应该继续删除不能返回
                    }
                }
            }
        }
        if (!dir.delete()) {
            logger.debug("remove dir {} not success", dir.toString());
            return false;
        } else {
            return true;
        }
    }
}
