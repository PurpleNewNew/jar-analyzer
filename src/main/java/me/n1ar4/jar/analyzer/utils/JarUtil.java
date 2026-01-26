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

import me.n1ar4.jar.analyzer.core.AnalyzeEnv;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.util.LogUtil;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

@SuppressWarnings("all")
public class JarUtil {
    private static final Logger logger = LogManager.getLogger();
    private static final Set<ClassFileEntity> classFileSet = new HashSet<>();
    private static final Set<ResourceEntity> resourceFileSet = new HashSet<>();

    private static final String META_INF = "META-INF";
    private static final int MAX_PARENT_SEARCH = 20;

    // 配置文件扩展名列表
    public static final Set<String> CONFIG_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".yml", ".yaml", ".properties", ".xml", ".json", ".conf", ".config",
            ".ini", ".toml", "web.xml", "application.properties", "application.yml",
            "application-dev.properties", "application-prod.properties",
            "application-dev.yml", "application-prod.yml",
            ".env", ".dotenv"
    ));

    public static boolean isConfigFile(String fileName) {
        fileName = fileName.toLowerCase();
        for (String ext : CONFIG_EXTENSIONS) {
            if (fileName.endsWith(ext.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static List<ClassFileEntity> resolveNormalJarFile(String jarPath, Integer jarId) {
        try {
            Path tmpDir = Paths.get(Const.tempDir);
            classFileSet.clear();
            resourceFileSet.clear();
            resolve(jarId, jarPath, tmpDir);
            return new ArrayList<>(classFileSet);
        } catch (Exception e) {
            logger.error("error: {}", e.toString());
        }
        return new ArrayList<>();
    }

    public static List<ResourceEntity> getResourceFiles() {
        return new ArrayList<>(resourceFileSet);
    }

    private static void resolve(Integer jarId, String jarPathStr, Path tmpDir) {
        Path jarPath = Paths.get(jarPathStr);
        if (!Files.exists(jarPath)) {
            logger.error("jar not exist");
            return;
        }
        try {
            if (jarPathStr.toLowerCase(Locale.ROOT).endsWith(".class")) {
                String fileText = MainForm.getInstance().getFileText().getText().trim();
                if (jarPathStr.contains(fileText)) {
                    String backPath = jarPathStr;

                    // #################################################
                    // 2025/06/26 处理重大 BUG
                    // 加载单个 CLASS 时 CLASSNAME 按照 META-INF 决定
                    Path parentPath = jarPath;
                    Path resultPath = null;
                    // 循环找 META-INF 目录
                    int index = 0;
                    while ((parentPath = parentPath.getParent()) != null) {
                        Path metaPath = parentPath.resolve("META-INF");
                        if (Files.exists(metaPath)) {
                            resultPath = metaPath;
                            break;
                        }
                        index++;
                        // 防止一直循环
                        if (index > MAX_PARENT_SEARCH) {
                            break;
                        }
                    }
                    if (resultPath == null) {
                        return;
                    }
                    String finalPath = resultPath.toAbsolutePath().toString();
                    if (!finalPath.contains(fileText)) {
                        // 跨越目录除外
                        return;
                    }
                    // 防止预期外错误
                    if (finalPath.length() < META_INF.length()) {
                        logger.warn("路径长度不足: {}", finalPath);
                        return;
                    }
                    try {
                        jarPathStr = jarPathStr.substring(finalPath.length() - META_INF.length());
                    } catch (StringIndexOutOfBoundsException e) {
                        logger.error("字符串截取错误: jarPathStr={}, finalPath={}", jarPathStr, finalPath, e);
                        return;
                    }
                    String saveClass = jarPathStr.replace("\\", "/");
                    if (shouldSkipBuildClassEntry(saveClass)) {
                        logger.info("skip build class by common list: {}", saveClass);
                        return;
                    }
                    logger.info("加载 CLASS 文件 {}", saveClass);
                    // #################################################

                    ClassFileEntity classFile = new ClassFileEntity(saveClass, jarPath, jarId);
                    classFile.setJarName("class");
                    classFileSet.add(classFile);

                    Path fullPath = tmpDir.resolve(jarPathStr);
                    Path parPath = fullPath.getParent();
                    if (!Files.exists(parPath)) {
                        Files.createDirectories(parPath);
                    }
                    try {
                        Files.createFile(fullPath);
                    } catch (Exception ignored) {
                    }
                    InputStream fis = Files.newInputStream(Paths.get(backPath));
                    OutputStream outputStream = Files.newOutputStream(fullPath);
                    IOUtil.copy(fis, outputStream);
                    outputStream.close();
                    fis.close();
                } else {
                    logger.warn("skip class file not under root: {}", jarPathStr);
                    return;
                }
            } else if (jarPathStr.toLowerCase(Locale.ROOT).endsWith(".jar") ||
                    jarPathStr.toLowerCase(Locale.ROOT).endsWith(".war")) {
                if (shouldSkipBuildJar(jarPathStr)) {
                    logger.info("skip build jar by common list: {}", jarPathStr);
                    return;
                }
                try (ZipFile jarFile = new ZipFile(jarPath)) {
                    Enumeration<? extends ZipArchiveEntry> entries = jarFile.getEntries();
                    while (entries.hasMoreElements()) {
                        ZipArchiveEntry jarEntry = entries.nextElement();
                    // =============== 2024/04/26 修复 ZIP SLIP 漏洞 ===============
                    String jarEntryName = jarEntry.getName();
                    // 第一次检查是否包含 ../ ..\\ 绕过
                    if (jarEntryName.contains("../") || jarEntryName.contains("..\\")) {
                        logger.warn("detect zip slip vulnearbility");
                        // 不抛出异常只跳过这个文件继续处理其他文件
                        continue;
                    }
                    // 可能还有其他的绕过情况？
                    // 先 normalize 处理 ../ 情况
                    // 再保证 entryPath 绝对路径必须以解压临时目录 tmpDir 开头
                    Path entryPath = tmpDir.resolve(jarEntryName).toAbsolutePath().normalize();
                    Path tmpDirAbs = tmpDir.toAbsolutePath().normalize();
                    if (!entryPath.startsWith(tmpDirAbs)) {
                        // 不抛出异常只跳过这个文件继续处理其他文件
                        logger.warn("detect zip slip vulnearbility");
                        continue;
                    }
                    // ============================================================
                    Path fullPath = tmpDir.resolve(jarEntryName);
                    if (!jarEntry.isDirectory()) {
                        if (!jarEntry.getName().endsWith(".class")) {
                            if (AnalyzeEnv.jarsInJar && jarEntry.getName().endsWith(".jar")) {
                                if (shouldSkipBuildJar(jarEntry.getName())) {
                                    logger.info("skip build nested jar by common list: {}", jarEntry.getName());
                                    continue;
                                }
                                LogUtil.info("analyze jars in jar");
                                Path dirName = fullPath.getParent();
                                if (!Files.exists(dirName)) {
                                    Files.createDirectories(dirName);
                                }
                                try {
                                    Files.createFile(fullPath);
                                } catch (Exception ignored) {
                                }
                                OutputStream outputStream = Files.newOutputStream(fullPath);
                                InputStream temp = jarFile.getInputStream(jarEntry);
                                IOUtil.copy(temp, outputStream);
                                temp.close();
                                doInternal(jarId, fullPath, tmpDir);
                                outputStream.close();
                            }
                            // 保存资源文件（包含配置/mapper/XML/任意资源）
                            saveResourceEntry(jarId, jarPathStr, jarEntryName, jarFile, jarEntry, tmpDir);
                            continue;
                        }

                        if (shouldSkipBuildClassEntry(jarEntryName)) {
                            continue;
                        }

                        Path dirName = fullPath.getParent();
                        if (!Files.exists(dirName)) {
                            Files.createDirectories(dirName);
                        }
                        OutputStream outputStream = Files.newOutputStream(fullPath);
                        InputStream temp = jarFile.getInputStream(jarEntry);
                        IOUtil.copy(temp, outputStream);
                        temp.close();
                        outputStream.close();
                        ClassFileEntity classFile = new ClassFileEntity(jarEntry.getName(), fullPath, jarId);
                        String splitStr;
                        if (OSUtil.isWindows()) {
                            splitStr = "\\\\";
                        } else {
                            splitStr = "/";
                        }
                        String[] splits = jarPathStr.split(splitStr);
                        classFile.setJarName(splits[splits.length - 1]);

                        classFileSet.add(classFile);
                    }
                }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("error: {}", e.toString());
        }
    }

    private static void doInternal(Integer jarId, Path jarPath, Path tmpDir) {
        if (jarPath == null) {
            return;
        }
        if (shouldSkipBuildJar(jarPath.toString())) {
            logger.info("skip build jar by common list: {}", jarPath);
            return;
        }
        try (ZipFile jarFile = new ZipFile(jarPath)) {
            Enumeration<? extends ZipArchiveEntry> entries = jarFile.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry jarEntry = entries.nextElement();
                // =============== 2024/04/26 修复 ZIP SLIP 漏洞 ===============
                String jarEntryName = jarEntry.getName();
                // 第一次检查是否包含 ../ ..\\ 绕过
                if (jarEntryName.contains("../") || jarEntryName.contains("..\\")) {
                    logger.warn("detect zip slip vulnearbility");
                    // 不抛出异常只跳过这个文件继续处理其他文件
                    continue;
                }
                // 可能还有其他的绕过情况？
                // 先 normalize 处理 ../ 情况
                // 再保证 entryPath 绝对路径必须以解压临时目录 tmpDir 开头
                Path entryPath = tmpDir.resolve(jarEntryName).toAbsolutePath().normalize();
                Path tmpDirAbs = tmpDir.toAbsolutePath().normalize();
                if (!entryPath.startsWith(tmpDirAbs)) {
                    // 不抛出异常只跳过这个文件继续处理其他文件
                    logger.warn("detect zip slip vulnearbility");
                    continue;
                }
                // ============================================================
                Path fullPath = tmpDir.resolve(jarEntryName);
                if (!jarEntry.isDirectory()) {
                    if (!jarEntry.getName().endsWith(".class")) {
                        // 保存资源文件（包含配置/mapper/XML/任意资源）
                        saveResourceEntry(jarId, jarPath.toString(), jarEntryName, jarFile, jarEntry, tmpDir);
                        continue;
                    }

                    if (shouldSkipBuildClassEntry(jarEntryName)) {
                        continue;
                    }

                    Path dirName = fullPath.getParent();
                    if (!Files.exists(dirName)) {
                        Files.createDirectories(dirName);
                    }
                    OutputStream outputStream = Files.newOutputStream(fullPath);
                    InputStream temp = jarFile.getInputStream(jarEntry);
                    IOUtil.copy(temp, outputStream);
                    temp.close();
                    outputStream.close();
                    ClassFileEntity classFile = new ClassFileEntity(jarEntry.getName(), fullPath, jarId);
                    String splitStr;
                    if (OSUtil.isWindows()) {
                        splitStr = "\\\\";
                    } else {
                        splitStr = "/";
                    }
                    String[] splits = jarPath.toString().split(splitStr);
                    classFile.setJarName(splits[splits.length - 1]);

                    classFileSet.add(classFile);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("error: {}", e.toString());
        }
    }

    private static boolean shouldSkipBuildJar(String jarPathStr) {
        if (jarPathStr == null || jarPathStr.trim().isEmpty()) {
            return false;
        }
        boolean whitelistActive = CommonWhitelistUtil.hasJarPrefixes();
        boolean whitelisted = CommonWhitelistUtil.isWhitelistedJar(jarPathStr);
        if (!whitelisted) {
            return true;
        }
        if (whitelistActive && whitelisted) {
            return false;
        }
        return CommonBlacklistUtil.isBlacklistedJar(jarPathStr);
    }

    private static boolean shouldSkipBuildClassEntry(String entryName) {
        if (entryName == null || entryName.trim().isEmpty()) {
            return false;
        }
        String name = entryName.replace('\\', '/');
        if (name.endsWith(".class")) {
            name = name.substring(0, name.length() - ".class".length());
        }
        boolean whitelistActive = CommonWhitelistUtil.hasClassPrefixes();
        boolean whitelisted = CommonWhitelistUtil.isWhitelistedClass(name);
        if (!whitelisted) {
            return true;
        }
        if (whitelistActive && whitelisted) {
            return false;
        }
        return CommonBlacklistUtil.isBlacklistedClass(name);
    }

    private static void saveResourceEntry(Integer jarId,
                                          String jarPathStr,
                                          String jarEntryName,
                                          ZipFile jarFile,
                                          ZipArchiveEntry jarEntry,
                                          Path tmpDir) {
        try {
            String jarName = resolveJarName(jarPathStr);
            int finalJarId = jarId == null ? -1 : jarId;
            Path resourceRoot = tmpDir.resolve(Const.resourceDir).resolve(String.valueOf(finalJarId));
            Path resourcePath = resourceRoot.resolve(jarEntryName).toAbsolutePath().normalize();
            Path resourceRootAbs = resourceRoot.toAbsolutePath().normalize();
            if (!resourcePath.startsWith(resourceRootAbs)) {
                logger.warn("detect resource zip slip: {}", jarEntryName);
                return;
            }
            Path dirName = resourcePath.getParent();
            if (dirName != null) {
                if (Files.exists(dirName) && !Files.isDirectory(dirName)) {
                    logger.debug("skip resource, parent is file: {}", dirName);
                    return;
                }
                if (!Files.exists(dirName)) {
                    Files.createDirectories(dirName);
                }
            }
            if (Files.exists(resourcePath) && Files.isDirectory(resourcePath)) {
                logger.debug("skip resource, path is directory: {}", resourcePath);
                return;
            }
            OutputStream outputStream = Files.newOutputStream(resourcePath,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            InputStream temp = jarFile.getInputStream(jarEntry);
            IOUtil.copy(temp, outputStream);
            temp.close();
            outputStream.close();

            ResourceEntity resource = new ResourceEntity();
            resource.setJarId(finalJarId);
            resource.setJarName(jarName);
            resource.setResourcePath(jarEntryName);
            resource.setPathStr(resourcePath.toAbsolutePath().toString());
            try {
                resource.setFileSize(Files.size(resourcePath));
            } catch (Exception ignored) {
                resource.setFileSize(-1);
            }
            resource.setIsText(isTextFile(resourcePath) ? 1 : 0);
            resourceFileSet.add(resource);
        } catch (Exception e) {
            logger.error("save resource error: {}", e.getMessage());
        }
    }

    private static String resolveJarName(String jarPathStr) {
        if (jarPathStr == null) {
            return "unknown";
        }
        String splitStr;
        if (OSUtil.isWindows()) {
            splitStr = "\\\\";
        } else {
            splitStr = "/";
        }
        String[] splits = jarPathStr.split(splitStr);
        if (splits.length == 0) {
            return jarPathStr;
        }
        return splits[splits.length - 1];
    }

    private static boolean isTextFile(Path path) {
        int maxCheck = 4096;
        try (InputStream inputStream = Files.newInputStream(path)) {
            byte[] buffer = new byte[maxCheck];
            int n = inputStream.read(buffer);
            if (n <= 0) {
                return true;
            }
            int suspicious = 0;
            for (int i = 0; i < n; i++) {
                byte b = buffer[i];
                if (b == 0) {
                    return false;
                }
                if (b < 0x09) {
                    suspicious++;
                    continue;
                }
                if (b > 0x0D && b < 0x20) {
                    suspicious++;
                }
            }
            return (suspicious * 100 / n) < 30;
        } catch (Exception e) {
            return false;
        }
    }
}
