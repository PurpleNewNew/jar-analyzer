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

import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class CoreUtil {
    private static final Logger logger = LogManager.getLogger();
    private static final String CLASSPATH_NESTED_DIR = "classpath-nested";
    private static final String JMOD_CACHE_DIR = "jmod-cache";

    public static List<ClassFileEntity> getAllClassesFromJars(List<String> jarPathList,
                                                              Map<String, Integer> jarIdMap,
                                                              List<ResourceEntity> resources,
                                                              boolean resolveInnerJars) {
        logger.info("collect all class");
        Map<ClassFileKey, ClassFileEntity> classFilesByKey = new LinkedHashMap<>();
        Path temp = Paths.get(Const.tempDir);
        try {
            cleanupBuildTemp(temp);
        } catch (Exception ex) {
            logger.debug("cleanup temp dir failed: {}: {}", temp, ex.toString());
        }
        try {
            Files.createDirectories(temp);
        } catch (IOException ex) {
            logger.debug("create temp dir failed: {}: {}", temp, ex.toString());
        }
        boolean recursiveArchiveScan = shouldResolveInnerJars(jarPathList, resolveInnerJars);
        if (resolveInnerJars && !recursiveArchiveScan) {
            logger.info("skip recursive nested jar scan because classpath already contains extracted nested archives");
        }
        for (String jarPath : jarPathList) {
            JarUtil.ResolveResult result = JarUtil.resolveNormalJarFile(
                    jarPath,
                    jarIdMap.get(jarPath),
                    recursiveArchiveScan
            );
            mergeClassFiles(classFilesByKey, result.getClassFiles());
            if (resources != null) {
                resources.addAll(result.getResources());
            }
        }
        // 2025/08/01 解决黑名单生效但是会创建空的目录 误导用户 问题
        // 遍历 Const.tempDir 目录 如果目录（以及其子目录）里不包含任何文件 删除该目录
        return new ArrayList<>(classFilesByKey.values());
    }

    private static boolean shouldResolveInnerJars(List<String> jarPathList, boolean resolveInnerJars) {
        if (!resolveInnerJars) {
            return false;
        }
        if (jarPathList == null || jarPathList.isEmpty()) {
            return true;
        }
        Path nestedRoot = Paths.get(Const.tempDir, CLASSPATH_NESTED_DIR).toAbsolutePath().normalize();
        for (String jarPath : jarPathList) {
            Path normalized = normalizePath(jarPath);
            if (normalized != null && normalized.startsWith(nestedRoot)) {
                return false;
            }
        }
        return true;
    }

    private static void mergeClassFiles(Map<ClassFileKey, ClassFileEntity> classFilesByKey,
                                        Collection<ClassFileEntity> classFiles) {
        if (classFilesByKey == null || classFiles == null || classFiles.isEmpty()) {
            return;
        }
        for (ClassFileEntity classFile : classFiles) {
            if (classFile == null) {
                continue;
            }
            classFilesByKey.putIfAbsent(ClassFileKey.of(classFile), classFile);
        }
    }

    private static void cleanupBuildTemp(Path temp) {
        if (temp == null || !Files.exists(temp) || !Files.isDirectory(temp)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(temp)) {
            for (Path child : stream) {
                if (child == null) {
                    continue;
                }
                String name = child.getFileName() == null ? "" : child.getFileName().toString();
                if (CLASSPATH_NESTED_DIR.equals(name) || JMOD_CACHE_DIR.equals(name)) {
                    // Keep extracted classpath/runtime caches used as build inputs.
                    continue;
                }
                try {
                    DirUtil.removeDir(child.toFile());
                } catch (Exception ex) {
                    logger.debug("cleanup temp child failed: {}: {}", child, ex.toString());
                }
            }
        } catch (Exception ex) {
            logger.debug("scan temp dir for cleanup failed: {}: {}", temp, ex.toString());
        }
    }

    private static Path normalizePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }
        try {
            return Paths.get(rawPath).toAbsolutePath().normalize();
        } catch (Exception ex) {
            logger.debug("normalize temp archive path failed: {}: {}", rawPath, ex.toString());
            return null;
        }
    }

    public static void cleanupEmptyTempDirs() {
        Path temp = Paths.get(Const.tempDir);
        deleteEmptyDirectories(temp);
    }

    private static boolean deleteEmptyDirectories(Path directory) {
        if (!Files.isDirectory(directory)) {
            return false;
        }

        boolean isEmpty = true;
        try {
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
                for (Path path : dirStream) {
                    if (Files.isDirectory(path)) {
                        // 递归检查子目录
                        boolean childEmpty = deleteEmptyDirectories(path);
                        if (!childEmpty) {
                            isEmpty = false;
                        }
                    } else {
                        // 发现文件，目录非空
                        isEmpty = false;
                    }
                }
            }

            // 如果目录为空，删除它
            if (isEmpty) {
                Files.delete(directory);
            }

            return isEmpty;
        } catch (IOException e) {
            logger.error("delete null dir {} error {}", directory, e);
            return false;
        }
    }

    private record ClassFileKey(String className,
                                Path path,
                                Integer jarId) {
        private static ClassFileKey of(ClassFileEntity classFile) {
            String className = classFile.getClassName() == null ? "" : classFile.getClassName().trim();
            Path path = classFile.getPath();
            Integer jarId = classFile.getJarId() == null ? -1 : classFile.getJarId();
            return new ClassFileKey(className, path, jarId);
        }
    }
}
