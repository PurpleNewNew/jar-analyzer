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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class SCASingleUtil {
    private static final Logger logger = LogManager.getLogger();

    public static byte[] exploreJar(File file, String keyClassName) {
        byte[] data = null;
        List<File> nestedJars = new ArrayList<>();
        try (JarFile jarFile = new JarFile(file)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    if (entry.getName().contains(keyClassName) && !entry.getName().contains("$")) {
                        data = getClassBytes(jarFile, entry);
                        break;
                    }
                } else if (entry.getName().endsWith(".jar")) {
                    File nestedJarFile = SCAExtractor.extractNestedJar(jarFile, entry);
                    nestedJars.add(nestedJarFile);
                }
            }
        } catch (IOException ex) {
            logger.debug("explore jar failed: {}: {}", file, ex.toString());
        }

        if (data == null && !nestedJars.isEmpty()) {
            // 处理内嵌 CLASS
            for (File nest : nestedJars) {
                try (JarFile jarFile = new JarFile(nest)) {
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.getName().endsWith(".class")) {
                            if (entry.getName().contains(keyClassName) && !entry.getName().contains("$")) {
                                data = SCASingleUtil.getClassBytes(jarFile, entry);
                                break;
                            }
                        }
                    }
                } catch (IOException ex) {
                    logger.debug("explore nested jar failed: {}: {}", nest, ex.toString());
                }
                if (data != null) {
                    break;
                }
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
        return data;
    }

    static byte[] getClassBytes(JarFile jarFile, JarEntry entry) throws IOException {
        try (InputStream is = jarFile.getInputStream(entry);
             ByteArrayOutputStream bao = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                bao.write(buffer, 0, bytesRead);
            }
            return bao.toByteArray();
        }
    }
}
