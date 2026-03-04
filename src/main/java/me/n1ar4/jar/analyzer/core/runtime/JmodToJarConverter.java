/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.runtime;

import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class JmodToJarConverter {
    private static final Logger logger = LogManager.getLogger();
    private static final ConcurrentMap<String, Path> CACHE = new ConcurrentHashMap<>();

    private JmodToJarConverter() {
    }

    public static List<Path> convertAll(List<Path> archives) {
        if (archives == null || archives.isEmpty()) {
            return List.of();
        }
        List<Path> out = new ArrayList<>();
        for (Path archive : archives) {
            if (archive == null) {
                continue;
            }
            Path converted = convert(archive);
            if (converted != null) {
                out.add(converted);
            }
        }
        return out;
    }

    public static Path convert(Path archive) {
        if (archive == null || Files.notExists(archive)) {
            return null;
        }
        Path normalized = normalizePath(archive);
        String fileName = normalized.getFileName() == null
                ? ""
                : normalized.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".jar")) {
            return normalized;
        }
        if (!fileName.endsWith(".jmod")) {
            return normalized;
        }
        String cacheKey = buildCacheKey(normalized);
        Path cached = CACHE.get(cacheKey);
        if (cached != null && Files.exists(cached)) {
            return cached;
        }
        Path output = buildOutputPath(normalized, cacheKey);
        try {
            Path parent = output.getParent();
            if (parent != null && Files.notExists(parent)) {
                Files.createDirectories(parent);
            }
            convertJmodToJar(normalized, output);
            CACHE.put(cacheKey, output);
            return output;
        } catch (Exception ex) {
            logger.warn("convert jmod failed: {} -> {} ({})", normalized, output, ex.toString());
            return null;
        }
    }

    private static void convertJmodToJar(Path source, Path output) throws Exception {
        try (ZipFile moduleFile = new ZipFile(source.toFile());
             OutputStream fileOut = Files.newOutputStream(output);
             ZipOutputStream zipOutput = new ZipOutputStream(fileOut)) {
            Enumeration<? extends ZipEntry> entries = moduleFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry == null || entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (name == null || name.isBlank()) {
                    continue;
                }
                String outEntryName = mapEntryName(name);
                if (outEntryName == null || outEntryName.isBlank()) {
                    continue;
                }
                ZipEntry outEntry = new ZipEntry(outEntryName);
                if (entry.getTime() > 0) {
                    outEntry.setTime(entry.getTime());
                }
                zipOutput.putNextEntry(outEntry);
                try (InputStream in = moduleFile.getInputStream(entry)) {
                    in.transferTo(zipOutput);
                }
                zipOutput.closeEntry();
            }
            zipOutput.flush();
        }
    }

    private static String mapEntryName(String name) {
        if (name.startsWith("classes/")) {
            String out = name.substring("classes/".length());
            if (out.equals("module-info.class") || out.endsWith("/module-info.class")) {
                return null;
            }
            return out;
        }
        if (name.startsWith("resources/")) {
            return name.substring("resources/".length());
        }
        if (name.startsWith("lib/")) {
            return name.substring("lib/".length());
        }
        return null;
    }

    private static String buildCacheKey(Path source) {
        long modified = -1L;
        try {
            modified = Files.getLastModifiedTime(source).toMillis();
        } catch (Exception ignored) {
            logger.debug("read jmod lastModified fail: {} ({})", source, ignored.toString());
        }
        return Integer.toHexString((source.toString() + "@" + modified).hashCode());
    }

    private static Path buildOutputPath(Path source, String key) {
        String moduleName = source.getFileName() == null ? "module" : source.getFileName().toString();
        if (moduleName.toLowerCase(Locale.ROOT).endsWith(".jmod")) {
            moduleName = moduleName.substring(0, moduleName.length() - ".jmod".length());
        }
        return Path.of(Const.tempDir, "jmod-cache", key + "-" + moduleName + ".jar")
                .toAbsolutePath()
                .normalize();
    }

    private static Path normalizePath(Path path) {
        if (path == null) {
            return null;
        }
        try {
            return path.toAbsolutePath().normalize();
        } catch (Exception ex) {
            return path.normalize();
        }
    }
}
