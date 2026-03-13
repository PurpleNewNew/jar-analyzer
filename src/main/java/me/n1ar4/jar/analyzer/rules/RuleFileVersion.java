/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.rules;

import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HexFormat;

final class RuleFileVersion {
    private static final Logger logger = LogManager.getLogger();

    private RuleFileVersion() {
    }

    static State probe(String rawPath, String label) {
        if (rawPath == null || rawPath.isBlank()) {
            return State.missing();
        }
        try {
            Path path = Paths.get(rawPath);
            if (!Files.exists(path)) {
                return State.missing();
            }
            return new State(
                    true,
                    Files.getLastModifiedTime(path).toMillis(),
                    Files.size(path),
                    ""
            );
        } catch (Exception ex) {
            logger.debug("probe {} rule file failed: path={} err={}", safe(label), rawPath, ex.toString());
            return State.missing();
        }
    }

    static State stamp(String rawPath, String label) {
        if (rawPath == null || rawPath.isBlank()) {
            return State.missing();
        }
        try {
            Path path = Paths.get(rawPath);
            if (!Files.exists(path)) {
                return State.missing();
            }
            byte[] data = Files.readAllBytes(path);
            return new State(
                    true,
                    Files.getLastModifiedTime(path).toMillis(),
                    data.length,
                    sha256Hex(data)
            );
        } catch (Exception ex) {
            logger.debug("stamp {} rule file failed: path={} err={}", safe(label), rawPath, ex.toString());
            return State.missing();
        }
    }

    private static String sha256Hex(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (Exception ex) {
            logger.debug("compute rule file hash failed: {}", ex.toString());
            return "";
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    static final class State {
        private final boolean exists;
        private final long modifiedTime;
        private final long size;
        private final String contentHash;

        private State(boolean exists, long modifiedTime, long size, String contentHash) {
            this.exists = exists;
            this.modifiedTime = modifiedTime;
            this.size = size;
            this.contentHash = contentHash == null ? "" : contentHash;
        }

        static State missing() {
            return new State(false, -1L, -1L, "");
        }

        boolean sameProbe(State other) {
            return other != null
                    && exists == other.exists
                    && modifiedTime == other.modifiedTime
                    && size == other.size;
        }

        boolean sameStamped(State other) {
            return sameProbe(other) && contentHash.equals(other.contentHash);
        }

        String fingerprint() {
            if (!exists) {
                return "missing";
            }
            if (!contentHash.isBlank()) {
                return contentHash;
            }
            return modifiedTime + ":" + size;
        }
    }
}
