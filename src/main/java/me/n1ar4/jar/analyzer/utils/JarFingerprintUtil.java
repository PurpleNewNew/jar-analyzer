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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;

public final class JarFingerprintUtil {
    private static final ConcurrentHashMap<String, String> CACHE = new ConcurrentHashMap<>();

    private JarFingerprintUtil() {
    }

    public static String sha256(String absPath) {
        if (StringUtil.isNull(absPath)) {
            return "";
        }
        String path = absPath.trim();
        if (path.isEmpty()) {
            return "";
        }
        return CACHE.computeIfAbsent(path, JarFingerprintUtil::sha256File);
    }

    private static String sha256File(String absPath) {
        try {
            Path path = Paths.get(absPath);
            if (!Files.exists(path)) {
                return "";
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return toHex(digest.digest());
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String toHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
