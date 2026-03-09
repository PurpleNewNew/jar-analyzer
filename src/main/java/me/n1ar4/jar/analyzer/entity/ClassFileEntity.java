/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.entity;

import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.file.Path;

public class ClassFileEntity {
    private static final Logger logger = LogManager.getLogger();
    // SAVE
    private int cfId;
    // SAVE
    private String className;
    // Single source of truth for class file location.
    private Path path;
    // SAVE
    private String jarName;
    // SAVE
    private Integer jarId;
    private transient volatile byte[] cachedBytes;

    public int getCfId() {
        return cfId;
    }

    public void setCfId(int cfId) {
        this.cfId = cfId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Path getPath() {
        return resolvePath();
    }

    public void setPath(Path path) {
        this.path = normalizePath(path);
    }

    public String getPathStr() {
        return resolvePathStr();
    }

    public void setPathStr(String pathStr) {
        String normalized = normalizePathStr(pathStr);
        if (normalized == null || normalized.isEmpty()) {
            this.path = null;
            return;
        }
        try {
            this.path = normalizePath(Path.of(normalized));
        } catch (Exception ex) {
            this.path = null;
            logger.debug("resolve class path fail: {}", ex.toString());
        }
    }

    public String getJarName() {
        return jarName;
    }

    public void setJarName(String jarName) {
        this.jarName = jarName;
    }

    public Integer getJarId() {
        return jarId;
    }

    public void setJarId(Integer jarId) {
        this.jarId = jarId;
    }

    public void setCachedBytes(byte[] cachedBytes) {
        this.cachedBytes = cachedBytes;
    }

    public void clearCachedBytes() {
        this.cachedBytes = null;
    }

    public ClassFileEntity() {
    }

    public ClassFileEntity(String className, Path path, Integer jarId) {
        this.className = className;
        setPath(path);
        this.jarId = jarId;
    }

    public byte[] getFile() {
        try {
            byte[] local = this.cachedBytes;
            if (local != null) {
                return local;
            }
            return me.n1ar4.jar.analyzer.utils.BytecodeCache.read(resolvePath());
        } catch (Exception e) {
            logger.error("get file error: {}", e.toString());
            return null;
        }
    }

    public Path resolvePath() {
        if (path == null) {
            return null;
        }
        Path normalized = normalizePath(path);
        if (normalized != null) {
            path = normalized;
        }
        return normalized;
    }

    public String resolvePathStr() {
        Path resolved = resolvePath();
        if (resolved != null) {
            return resolved.toString();
        }
        return null;
    }

    private static Path normalizePath(Path path) {
        if (path == null) {
            return null;
        }
        try {
            return path.toAbsolutePath().normalize();
        } catch (Exception ex) {
            try {
                return path.normalize();
            } catch (Exception ignored) {
                return path;
            }
        }
    }

    private static String normalizePathStr(String pathStr) {
        if (pathStr == null) {
            return null;
        }
        return pathStr.trim();
    }

    @Override
    public String toString() {
        return "ClassFileEntity{" +
                "cfId=" + cfId +
                ", className='" + className + '\'' +
                ", path=" + path +
                ", pathStr='" + getPathStr() + '\'' +
                ", jarName='" + jarName + '\'' +
                ", jarId=" + jarId +
                '}';
    }
}
