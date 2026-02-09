/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.support;

import org.junit.jupiter.api.Assertions;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Build and locate fixture jars used by integration tests.
 *
 * Keep tests self-contained: do not depend on external jars checked into the repo root.
 */
public final class FixtureJars {
    private static final Object LOCK = new Object();
    private static volatile Path springbootJar;

    private FixtureJars() {
    }

    public static Path springbootTestJar() {
        Path cached = springbootJar;
        if (cached != null && Files.exists(cached)) {
            return cached;
        }
        synchronized (LOCK) {
            cached = springbootJar;
            if (cached != null && Files.exists(cached)) {
                return cached;
            }
            Path projectDir = Paths.get("test", "springboot-test");
            Path targetDir = projectDir.resolve("target");
            Path jar = findMainJar(targetDir);
            if (jar == null) {
                runMavenPackage(projectDir);
                jar = findMainJar(targetDir);
            }
            if (jar == null) {
                Assertions.fail("fixture jar not found under: " + targetDir.toAbsolutePath());
            }
            springbootJar = jar;
            return jar;
        }
    }

    private static Path findMainJar(Path targetDir) {
        if (targetDir == null || !Files.isDirectory(targetDir)) {
            return null;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetDir, "*.jar")) {
            for (Path p : stream) {
                if (p == null) {
                    continue;
                }
                String name = p.getFileName().toString();
                if (name.endsWith(".jar.original")) {
                    continue;
                }
                if (Files.isRegularFile(p)) {
                    return p;
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private static void runMavenPackage(Path projectDir) {
        if (projectDir == null) {
            Assertions.fail("fixture project dir is null");
        }
        ProcessBuilder pb = new ProcessBuilder("mvn", "-q", "-DskipTests", "package");
        pb.directory(projectDir.toFile());
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = process.getInputStream()) {
                byte[] buf = new byte[8192];
                int read;
                while ((read = in.read(buf)) >= 0) {
                    out.write(buf, 0, read);
                }
            }
            int code = process.waitFor();
            if (code != 0) {
                String msg = new String(out.toByteArray(), StandardCharsets.UTF_8);
                Assertions.fail("mvn package failed for " + projectDir.toAbsolutePath()
                        + " (exit=" + code + ")\n" + msg);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            Assertions.fail("mvn package interrupted for " + projectDir.toAbsolutePath());
        } catch (Exception e) {
            Assertions.fail("mvn package failed for " + projectDir.toAbsolutePath() + ": " + e);
        }
    }
}
