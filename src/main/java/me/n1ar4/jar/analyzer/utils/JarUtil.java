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
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.objectweb.asm.ClassReader;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

@SuppressWarnings("all")
public class JarUtil {
    private static final Logger logger = LogManager.getLogger();

    private static final String META_INF = "META-INF";
    private static final int MAX_PARENT_SEARCH = 20;
    private static final int TEXT_PROBE_BYTES = 4096;

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

    private static Path resolveJarRoot(Path tmpDir, Integer jarId, String jarPathStr) {
        int safeId = jarId == null ? -1 : jarId;
        String hash = jarPathStr == null ? "0" : Integer.toHexString(jarPathStr.hashCode());
        String dirName = "jar-" + safeId + "-" + hash;
        return tmpDir.resolve(dirName);
    }

    private static void ensureDir(Path dir, Set<Path> cache) {
        if (dir == null) {
            return;
        }
        try {
            if (cache == null || cache.add(dir)) {
                Files.createDirectories(dir);
            }
        } catch (Exception e) {
            logger.debug("mkdirs error: {}", e.toString());
        }
    }

    private static boolean isJarDirName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (name.startsWith("jar-")) {
            return true;
        }
        int start = 0;
        if (name.charAt(0) == '-') {
            if (name.length() == 1) {
                return false;
            }
            start = 1;
        }
        for (int i = start; i < name.length(); i++) {
            if (!Character.isDigit(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static String resolveClassNameFromPath(String classPath) {
        return resolveClassNameFromPath(classPath, false);
    }

    public static String resolveClassNameFromPath(String classPath, boolean dotStyle) {
        if (classPath == null || classPath.trim().isEmpty()) {
            return null;
        }
        String normalized = classPath.replace("\\", "/");
        String temp = Const.tempDir.replace("\\", "/");
        int idx = normalized.lastIndexOf(temp);
        if (idx >= 0) {
            normalized = normalized.substring(idx + temp.length());
        }
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isEmpty()) {
            return null;
        }
        normalized = stripJarPrefix(normalized);
        normalized = stripRuntimeCachePrefix(normalized);
        normalized = stripBootWebPrefix(normalized);
        if (normalized.endsWith(".class")) {
            normalized = normalized.substring(0, normalized.length() - ".class".length());
        }
        if (normalized.isEmpty()) {
            return null;
        }
        if (dotStyle) {
            normalized = normalized.replace("/", ".");
        }
        return normalized;
    }

    public static Path resolveClassFileInTemp(String className) {
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        String normalized = className.trim();
        if (normalized.endsWith(".class")) {
            normalized = normalized.substring(0, normalized.length() - ".class".length());
        }
        if (normalized.contains(".")) {
            normalized = normalized.replace('.', '/');
        }
        String rel = normalized + ".class";
        Path base = Paths.get(Const.tempDir);
        Path direct = base.resolve(rel);
        if (Files.exists(direct)) {
            return direct;
        }
        Path boot = base.resolve(Paths.get("BOOT-INF", "classes", rel));
        if (Files.exists(boot)) {
            return boot;
        }
        Path web = base.resolve(Paths.get("WEB-INF", "classes", rel));
        if (Files.exists(web)) {
            return web;
        }
        if (!Files.isDirectory(base)) {
            return null;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(base)) {
            for (Path dir : stream) {
                if (!Files.isDirectory(dir)) {
                    continue;
                }
                String name = dir.getFileName().toString();
                if (!isJarDirName(name)) {
                    continue;
                }
                Path candidate = dir.resolve(rel);
                if (Files.exists(candidate)) {
                    return candidate;
                }
                Path bootCandidate = dir.resolve(Paths.get("BOOT-INF", "classes", rel));
                if (Files.exists(bootCandidate)) {
                    return bootCandidate;
                }
                Path webCandidate = dir.resolve(Paths.get("WEB-INF", "classes", rel));
                if (Files.exists(webCandidate)) {
                    return webCandidate;
                }
            }
        } catch (Exception ex) {
            logger.debug("scan temp dir failed: {}: {}", base, ex.toString());
        }
        Path runtime = base.resolve(Paths.get("runtime-cache", rel));
        if (Files.exists(runtime)) {
            return runtime;
        }
        return null;
    }

    private static String stripJarPrefix(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        String trimmed = path.startsWith("/") ? path.substring(1) : path;
        int slash = trimmed.indexOf('/');
        if (slash < 0) {
            return trimmed;
        }
        String first = trimmed.substring(0, slash);
        if (isJarDirName(first)) {
            return trimmed.substring(slash + 1);
        }
        return trimmed;
    }

    private static String stripRuntimeCachePrefix(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        String prefix = "runtime-cache/";
        if (path.startsWith(prefix)) {
            return path.substring(prefix.length());
        }
        return path;
    }

    private static String stripBootWebPrefix(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        String bootPrefix = "BOOT-INF/classes/";
        int bootIdx = path.indexOf(bootPrefix);
        if (bootIdx >= 0) {
            return path.substring(bootIdx + bootPrefix.length());
        }
        String webPrefix = "WEB-INF/classes/";
        int webIdx = path.indexOf(webPrefix);
        if (webIdx >= 0) {
            return path.substring(webIdx + webPrefix.length());
        }
        return path;
    }

    public static ResolveResult resolveNormalJarFile(String jarPath, Integer jarId) {
        ResolveResult result = new ResolveResult();
        try {
            Path tmpDir = Paths.get(Const.tempDir);
            resolve(jarId, jarPath, tmpDir, result);
            return result;
        } catch (Exception e) {
            logger.error("error: {}", e.toString());
        }
        return result;
    }

    private static void resolve(Integer jarId, String jarPathStr, Path tmpDir, ResolveResult result) {
        Path jarPath = Paths.get(jarPathStr);
        if (!Files.exists(jarPath)) {
            logger.error("jar not exist");
            return;
        }
        Path jarRoot = resolveJarRoot(tmpDir, jarId, jarPathStr);
        try {
            if (jarPathStr.toLowerCase(Locale.ROOT).endsWith(".class")) {
                String fileText = null;
                try {
                    Path root = WorkspaceContext.getInputPath();
                    fileText = root == null ? null : root.toString();
                } catch (Throwable t) {
                    InterruptUtil.restoreInterruptIfNeeded(t);
                    if (t instanceof Error) {
                        throw (Error) t;
                    }
                    logger.debug("get workspace input path failed: {}", t.toString());
                }
                if (fileText != null) {
                    fileText = fileText.trim();
                }
                if (fileText != null && !fileText.isEmpty() && !jarPathStr.contains(fileText)) {
                    logger.warn("skip class file not under root: {}", jarPathStr);
                    return;
                }

                String backPath = jarPathStr;
                String saveClass = null;
                byte[] classBytes = null;

                // 2025/06/26: resolve class entry from META-INF if present
                Path parentPath = jarPath;
                Path resultPath = null;
                int index = 0;
                while ((parentPath = parentPath.getParent()) != null) {
                    Path metaPath = parentPath.resolve("META-INF");
                    if (Files.exists(metaPath)) {
                        resultPath = metaPath;
                        break;
                    }
                    index++;
                    if (index > MAX_PARENT_SEARCH) {
                        break;
                    }
                }
                if (resultPath != null) {
                    String finalPath = resultPath.toAbsolutePath().toString();
                    if (fileText != null && !fileText.isEmpty() && !finalPath.contains(fileText)) {
                        return;
                    }
                    if (finalPath.length() < META_INF.length()) {
                        logger.warn("path too short: {}", finalPath);
                    } else {
                        try {
                            jarPathStr = jarPathStr.substring(finalPath.length() - META_INF.length());
                            saveClass = jarPathStr.replace("\\", "/");
                        } catch (StringIndexOutOfBoundsException e) {
                            logger.error("class path cut error jarPathStr={}, finalPath={}", jarPathStr, finalPath, e);
                        }
                    }
                }
                if (saveClass == null) {
                    try {
                        if (classBytes == null) {
                            classBytes = Files.readAllBytes(jarPath);
                        }
                        ClassReader reader = new ClassReader(classBytes);
                        String internalName = reader.getClassName();
                        if (internalName != null && !internalName.trim().isEmpty()) {
                            saveClass = internalName + ".class";
                        }
                    } catch (Exception e) {
                        logger.warn("resolve class name fail: {}", e.toString());
                        return;
                    }
                }
                if (saveClass == null || saveClass.trim().isEmpty()) {
                    return;
                }
                if (shouldSkipBuildClassEntry(saveClass)) {
                    logger.info("skip build class by common list: {}", saveClass);
                    return;
                }
                logger.info("加载 CLASS 文件 {}", saveClass);

                ClassFileEntity classFile = new ClassFileEntity(saveClass, jarPath, jarId);
                classFile.setJarName("class");

                Path fullPath = jarRoot.resolve(saveClass);
                try {
                    if (classBytes == null) {
                        classBytes = Files.readAllBytes(Paths.get(backPath));
                    }
                    classFile.setCachedBytes(classBytes);
                    BytecodeCache.preload(jarPath, classBytes);
                    DeferredFileWriter.enqueue(fullPath, classBytes, classFile);
                } catch (Exception e) {
                    logger.error("write class file error: {}", e.toString());
                }
                result.classFiles.add(classFile);
            } else if (jarPathStr.toLowerCase(Locale.ROOT).endsWith(".jar") ||
                    jarPathStr.toLowerCase(Locale.ROOT).endsWith(".war")) {
                if (shouldSkipBuildJar(jarPathStr)) {
                    logger.info("skip build jar by common list: {}", jarPathStr);
                    return;
                }
                String jarName = resolveJarName(jarPathStr);
                Set<Path> dirCache = new HashSet<>();
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
                    // 再保证 entryPath 绝对路径必须以解压临时目录 jarRoot 开头
                    Path entryPath = jarRoot.resolve(jarEntryName).toAbsolutePath().normalize();
                    Path jarRootAbs = jarRoot.toAbsolutePath().normalize();
                    if (!entryPath.startsWith(jarRootAbs)) {
                        // 不抛出异常只跳过这个文件继续处理其他文件
                        logger.warn("detect zip slip vulnearbility");
                        continue;
                    }
                    // ============================================================
                    Path fullPath = jarRoot.resolve(jarEntryName);
                    if (!jarEntry.isDirectory()) {
                        if (!jarEntryName.endsWith(".class")) {
                            if (AnalyzeEnv.jarsInJar && jarEntryName.endsWith(".jar")) {
                                if (shouldSkipBuildJar(jarEntryName)) {
                                    logger.info("skip build nested jar by common list: {}", jarEntryName);
                                    continue;
                                }
                                logger.info("analyze jars in jar");
                                Path dirName = fullPath.getParent();
                                ensureDir(dirName, dirCache);
                                try (OutputStream outputStream = Files.newOutputStream(fullPath);
                                     InputStream temp = jarFile.getInputStream(jarEntry)) {
                                    IOUtil.copy(temp, outputStream);
                                }
                                doInternal(jarId, fullPath, tmpDir, result);
                            }
                            // 保存资源文件（包含配置/mapper/XML/任意资源）
                            saveResourceEntry(jarId, jarPathStr, jarEntryName, jarFile, jarEntry, tmpDir, result);
                            continue;
                        }

                        if (shouldSkipBuildClassEntry(jarEntryName)) {
                            continue;
                        }

                        byte[] classBytes;
                        try (InputStream temp = jarFile.getInputStream(jarEntry)) {
                            classBytes = IOUtil.readBytes(temp);
                        }
                        if (classBytes == null || classBytes.length == 0) {
                            continue;
                        }
                        ClassFileEntity classFile = new ClassFileEntity(jarEntryName, fullPath, jarId);
                        classFile.setJarName(jarName);
                        classFile.setCachedBytes(classBytes);
                        DeferredFileWriter.enqueue(fullPath, classBytes, classFile);

                        result.classFiles.add(classFile);
                    }
                }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("error: {}", e.toString());
        }
    }

    private static void doInternal(Integer jarId, Path jarPath, Path tmpDir, ResolveResult result) {
        if (jarPath == null) {
            return;
        }
        if (shouldSkipBuildJar(jarPath.toString())) {
            logger.info("skip build jar by common list: {}", jarPath);
            return;
        }
        String jarName = resolveJarName(jarPath.toString());
        Path jarRoot = resolveJarRoot(tmpDir, jarId, jarPath.toString());
        Set<Path> dirCache = new HashSet<>();
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
                // 再保证 entryPath 绝对路径必须以解压临时目录 jarRoot 开头
                Path entryPath = jarRoot.resolve(jarEntryName).toAbsolutePath().normalize();
                Path jarRootAbs = jarRoot.toAbsolutePath().normalize();
                if (!entryPath.startsWith(jarRootAbs)) {
                    // 不抛出异常只跳过这个文件继续处理其他文件
                    logger.warn("detect zip slip vulnearbility");
                    continue;
                }
                // ============================================================
                Path fullPath = jarRoot.resolve(jarEntryName);
                if (!jarEntry.isDirectory()) {
                    if (!jarEntryName.endsWith(".class")) {
                        // 保存资源文件（包含配置/mapper/XML/任意资源）
                        saveResourceEntry(jarId, jarPath.toString(), jarEntryName, jarFile, jarEntry, tmpDir, result);
                        continue;
                    }

                    if (shouldSkipBuildClassEntry(jarEntryName)) {
                        continue;
                    }

                    byte[] classBytes;
                    try (InputStream temp = jarFile.getInputStream(jarEntry)) {
                        classBytes = IOUtil.readBytes(temp);
                    }
                    if (classBytes == null || classBytes.length == 0) {
                        continue;
                    }
                    ClassFileEntity classFile = new ClassFileEntity(jarEntryName, fullPath, jarId);
                    classFile.setJarName(jarName);
                    classFile.setCachedBytes(classBytes);
                    DeferredFileWriter.enqueue(fullPath, classBytes, classFile);

                    result.classFiles.add(classFile);
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
        String normalized = entryName.replace('\\', '/');
        if (normalized.endsWith("/module-info.class") || "module-info.class".equalsIgnoreCase(normalized)) {
            return true;
        }
        String name = normalized;
        if (name.endsWith(".class")) {
            name = name.substring(0, name.length() - ".class".length());
        }
        boolean whitelistActive = CommonWhitelistUtil.hasClassPrefixes();
        boolean whitelisted = CommonWhitelistUtil.isWhitelistedClassNormalized(name);
        if (!whitelisted) {
            return true;
        }
        if (whitelistActive && whitelisted) {
            return false;
        }
        return CommonBlacklistUtil.isBlacklistedClassNormalized(name);
    }

    public static Integer parseJarIdFromResourcePath(String relativePath) {
        if (StringUtil.isNull(relativePath)) {
            return null;
        }
        String norm = relativePath.replace("\\", "/");
        String prefix = Const.resourceDir + "/";
        if (!norm.startsWith(prefix)) {
            return null;
        }
        String rest = norm.substring(prefix.length());
        if (rest.isEmpty()) {
            return null;
        }
        int slash = rest.indexOf('/');
        String key = slash >= 0 ? rest.substring(0, slash) : rest;
        Integer parsed = parseTrailingJarId(key);
        if (parsed != null) {
            return parsed;
        }
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException ex) {
            logger.debug("parse jar id from resource path failed: {} ({})", key, relativePath);
            return null;
        }
    }

    private static String resolveResourceJarKey(Integer jarId, String jarPathStr) {
        String jarName = resolveJarName(jarPathStr);
        String base = sanitizeResourceKey(jarName);
        if (base.isEmpty()) {
            base = "unknown";
        }
        if (jarId != null && jarId >= 0) {
            return base + "-" + jarId;
        }
        String seed = jarPathStr == null ? base : jarPathStr;
        return base + "-" + Integer.toHexString(seed.hashCode());
    }

    private static Integer parseTrailingJarId(String value) {
        if (StringUtil.isNull(value)) {
            return null;
        }
        int idx = value.lastIndexOf('-');
        if (idx < 0 || idx + 1 >= value.length()) {
            return null;
        }
        String tail = value.substring(idx + 1);
        if (tail.isEmpty()) {
            return null;
        }
        for (int i = 0; i < tail.length(); i++) {
            if (!Character.isDigit(tail.charAt(i))) {
                return null;
            }
        }
        try {
            return Integer.parseInt(tail);
        } catch (NumberFormatException ex) {
            logger.debug("parse trailing jar id failed: {}", value);
            return null;
        }
    }

    private static String sanitizeResourceKey(String name) {
        if (name == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '-') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        String value = sb.toString();
        while (value.startsWith("_")) {
            value = value.substring(1);
        }
        while (value.endsWith("_")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static void saveResourceEntry(Integer jarId,
                                          String jarPathStr,
                                          String jarEntryName,
                                          ZipFile jarFile,
                                          ZipArchiveEntry jarEntry,
                                          Path tmpDir,
                                          ResolveResult result) {
        if (CommonFilterUtil.isFilteredResourcePath(jarEntryName)) {
            return;
        }
        if (isNestedLibJarPath(jarPathStr)) {
            return;
        }
        try {
            String jarName = resolveJarName(jarPathStr);
            int finalJarId = jarId == null ? -1 : jarId;
            String jarKey = resolveResourceJarKey(jarId, jarPathStr);
            Path resourceRoot = tmpDir.resolve(Const.resourceDir).resolve(jarKey);
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
            ResourceCopyMeta meta;
            try (OutputStream outputStream = Files.newOutputStream(resourcePath,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                 InputStream temp = jarFile.getInputStream(jarEntry)) {
                meta = copyResourceWithProbe(temp, outputStream);
            }

            ResourceEntity resource = new ResourceEntity();
            resource.setJarId(finalJarId);
            resource.setJarName(jarName);
            resource.setResourcePath(jarEntryName);
            resource.setPathStr(resourcePath.toAbsolutePath().toString());
            resource.setFileSize(meta.size);
            resource.setIsText(meta.text ? 1 : 0);
            result.resources.add(resource);
        } catch (Exception e) {
            logger.error("save resource error: {}", e.getMessage());
        }
    }

    private static boolean isNestedLibJarPath(String jarPathStr) {
        if (jarPathStr == null || jarPathStr.trim().isEmpty()) {
            return false;
        }
        String lower = jarPathStr.replace("\\", "/").toLowerCase(Locale.ROOT);
        return lower.contains("/boot-inf/lib/") || lower.contains("/web-inf/lib/");
    }

    private static String resolveJarName(String jarPathStr) {
        if (jarPathStr == null) {
            return "unknown";
        }
        try {
            Path path = Paths.get(jarPathStr);
            Path fileName = path.getFileName();
            if (fileName != null) {
                return fileName.toString();
            }
        } catch (Exception ex) {
            logger.debug("resolve jar name failed: {}: {}", jarPathStr, ex.toString());
        }
        String name = jarPathStr.replace("\\", "/");
        int slash = name.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < name.length()) {
            return name.substring(slash + 1);
        }
        return name;
    }

    private static ResourceCopyMeta copyResourceWithProbe(InputStream inputStream, OutputStream outputStream)
            throws Exception {
        byte[] buffer = new byte[8192];
        long total = 0;
        int suspicious = 0;
        int inspected = 0;
        boolean binary = false;
        int n;
        while ((n = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, n);
            total += n;
            if (!binary && inspected < TEXT_PROBE_BYTES) {
                int limit = Math.min(n, TEXT_PROBE_BYTES - inspected);
                for (int i = 0; i < limit; i++) {
                    byte b = buffer[i];
                    if (b == 0) {
                        binary = true;
                        break;
                    }
                    if (b < 0x09) {
                        suspicious++;
                        continue;
                    }
                    if (b > 0x0D && b < 0x20) {
                        suspicious++;
                    }
                }
                inspected += limit;
            }
        }
        boolean text;
        if (binary) {
            text = false;
        } else if (inspected == 0) {
            text = true;
        } else {
            text = (suspicious * 100 / inspected) < 30;
        }
        return new ResourceCopyMeta(total, text);
    }

    public static final class ResolveResult {
        private final Set<ClassFileEntity> classFiles = new HashSet<>();
        private final Set<ResourceEntity> resources = new HashSet<>();

        public Set<ClassFileEntity> getClassFiles() {
            return classFiles;
        }

        public Set<ResourceEntity> getResources() {
            return resources;
        }
    }

    private static final class ResourceCopyMeta {
        private final long size;
        private final boolean text;

        private ResourceCopyMeta(long size, boolean text) {
            this.size = size;
            this.text = text;
        }
    }
}
