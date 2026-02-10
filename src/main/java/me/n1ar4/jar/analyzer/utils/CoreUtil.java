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

    public static List<ClassFileEntity> getAllClassesFromJars(List<String> jarPathList,
                                                              Map<String, Integer> jarIdMap,
                                                              List<ResourceEntity> resources) {
        logger.info("collect all class");
        Set<ClassFileEntity> classFileSet = new HashSet<>();
        Path temp = Paths.get(Const.tempDir);
        try {
            DirUtil.removeDir(temp.toFile());
        } catch (Exception ex) {
            logger.debug("cleanup temp dir failed: {}: {}", temp, ex.toString());
        }
        try {
            Files.createDirectories(temp);
        } catch (IOException ex) {
            logger.debug("create temp dir failed: {}: {}", temp, ex.toString());
        }
        for (String jarPath : jarPathList) {
            JarUtil.ResolveResult result = JarUtil.resolveNormalJarFile(jarPath, jarIdMap.get(jarPath));
            classFileSet.addAll(result.getClassFiles());
            if (resources != null) {
                resources.addAll(result.getResources());
            }
        }
        // 2025/08/01 解决黑名单生效但是会创建空的目录 误导用户 问题
        // 遍历 Const.tempDir 目录 如果目录（以及其子目录）里不包含任何文件 删除该目录
        return new ArrayList<>(classFileSet);
    }

    public static void cleanupEmptyTempDirs() {
        Path temp = Paths.get(Const.tempDir);
        // 2025/08/01 瑙ｅ喅榛戝悕鍗曠敓鏁堜絾鏄細鍒涘缓绌虹殑鐩綍 璇鐢ㄦ埛 闂
        // 閬嶅巻 Const.tempDir 鐩綍 濡傛灉鐩綍锛堜互鍙婂叾瀛愮洰褰曪級閲屼笉鍖呭惫浠讳綍鏂囦欢 鍒犻櫎璇ョ洰褰?
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
}
