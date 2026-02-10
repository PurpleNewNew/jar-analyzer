/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.sca.utils;

import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class SCAMultiUtil {
    private static final Logger logger = LogManager.getLogger();

    public static Map<String, byte[]> exploreJarEx(File file, Map<String, String> hashMap) {
        Map<String, byte[]> resultMap = new HashMap<>();
        for (Map.Entry<String, String> mapEntry : hashMap.entrySet()) {
            String keyClass = mapEntry.getKey();
            List<File> nestedJars = new ArrayList<>();
            // 处理直接 CLASS
            try (JarFile jarFile = new JarFile(file)) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().endsWith(".class")) {
                        if (entry.getName().contains(keyClass) && !entry.getName().contains("$")) {
                            byte[] data = SCASingleUtil.getClassBytes(jarFile, entry);
                            resultMap.put(keyClass, data);
                            break;
                        }
                    } else if (entry.getName().endsWith(".jar")) {
                        // 这里别递归了 要不然变成一坨
                        File nestedJarFile = SCAExtractor.extractNestedJar(jarFile, entry);
                        nestedJars.add(nestedJarFile);
                    }
                }
            } catch (IOException ex) {
                logger.debug("explore jar failed: {}: {}", file, ex.toString());
            }
            if (nestedJars.isEmpty()) {
                continue;
            }
            // 处理内嵌 CLASS
            for (File nest : nestedJars) {
                try (JarFile jarFile = new JarFile(nest)) {
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.getName().endsWith(".class")) {
                            if (entry.getName().contains(keyClass) && !entry.getName().contains("$")) {
                                byte[] data = SCASingleUtil.getClassBytes(jarFile, entry);
                                resultMap.put(keyClass, data);
                                break;
                            }
                        }
                    }
                } catch (IOException ex) {
                    logger.debug("explore nested jar failed: {}: {}", nest, ex.toString());
                }
            }
            for (File nestedJar : nestedJars) {
                if (nestedJar == null) {
                    continue;
                }
                boolean success = nestedJar.delete();
                if (!success) {
                    logger.debug("delete temp jar failed: {}", nestedJar.getAbsolutePath());
                }
            }
        }
        return resultMap;
    }
}
