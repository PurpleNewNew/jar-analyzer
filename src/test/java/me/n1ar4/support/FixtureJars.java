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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Build and locate fixture jars used by integration tests.
 *
 * Keep tests self-contained: do not depend on external jars checked into the repo root.
 */
public final class FixtureJars {
    private static final Object LOCK = new Object();
    private static volatile Path springbootJar;
    private static volatile Path callbackJar;
    private static volatile Path frameworkStackJar;
    private static volatile Path gadgetFamilyJar;
    private static volatile Path ysoserialJar;
    private static volatile Path strutsSpringMyBatisArchive;

    private FixtureJars() {
    }

    public static Path springbootTestJar() {
        Path cached = springbootJar;
        if (cached != null && Files.exists(cached)) {
            return cached;
        }
        synchronized (LOCK) {
            springbootJar = resolveFixtureArchive(Paths.get("test", "springboot-test"));
            return springbootJar;
        }
    }

    public static Path callbackTestJar() {
        Path cached = callbackJar;
        if (cached != null && Files.exists(cached)) {
            return cached;
        }
        synchronized (LOCK) {
            callbackJar = resolveFixtureArchive(Paths.get("test", "callback-test"));
            return callbackJar;
        }
    }

    public static Path frameworkStackTestJar() {
        Path cached = frameworkStackJar;
        if (cached != null && Files.exists(cached)) {
            return cached;
        }
        synchronized (LOCK) {
            frameworkStackJar = resolveFixtureArchive(Paths.get("test", "framework-stack-test"));
            return frameworkStackJar;
        }
    }

    public static Path gadgetFamilyTestJar() {
        Path cached = gadgetFamilyJar;
        if (cached != null && Files.exists(cached)) {
            return cached;
        }
        synchronized (LOCK) {
            gadgetFamilyJar = resolveFixtureArchive(Paths.get("test", "gadget-family-test"));
            return gadgetFamilyJar;
        }
    }

    public static Path ysoserialPayloadTestJar() {
        Path cached = ysoserialJar;
        if (cached != null && Files.exists(cached)) {
            return cached;
        }
        synchronized (LOCK) {
            ysoserialJar = resolveFixtureArchive(Paths.get("test", "ysoserial-payload-test"));
            return ysoserialJar;
        }
    }

    public static Path strutsSpringMyBatisAppArchive() {
        Path cached = strutsSpringMyBatisArchive;
        if (cached != null && Files.exists(cached)) {
            return cached;
        }
        synchronized (LOCK) {
            strutsSpringMyBatisArchive = resolveFixtureArchive(Paths.get("test", "struts-spring-mybatis-app"));
            return strutsSpringMyBatisArchive;
        }
    }

    private static Path resolveFixtureArchive(Path projectDir) {
        if (projectDir == null) {
            Assertions.fail("fixture project dir is null");
        }
        Path targetDir = projectDir.resolve("target");
        Path archive = findMainArchive(targetDir);
        if (archive == null || isRebuildRequired(projectDir, archive)) {
            runMavenPackage(projectDir);
            archive = findMainArchive(targetDir);
        }
        if (archive == null) {
            Assertions.fail("fixture archive not found under: " + targetDir.toAbsolutePath());
        }
        return archive;
    }

    private static boolean isRebuildRequired(Path projectDir, Path jar) {
        if (projectDir == null || jar == null || !Files.exists(jar)) {
            return true;
        }
        try {
            long jarTime = Files.getLastModifiedTime(jar).toMillis();
            AtomicLong newest = new AtomicLong(0L);
            Path pom = projectDir.resolve("pom.xml");
            if (Files.exists(pom)) {
                newest.set(Math.max(newest.get(), Files.getLastModifiedTime(pom).toMillis()));
            }
            Path src = projectDir.resolve("src");
            if (Files.isDirectory(src)) {
                try (java.util.stream.Stream<Path> stream = Files.walk(src)) {
                    stream.filter(Files::isRegularFile).forEach(p -> {
                        try {
                            long t = Files.getLastModifiedTime(p).toMillis();
                            long prev = newest.get();
                            if (t > prev) {
                                newest.set(t);
                            }
                        } catch (Exception ignored) {
                        }
                    });
                }
            }
            return newest.get() > jarTime;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Path findMainArchive(Path targetDir) {
        Path war = findFirstArchive(targetDir, "*.war");
        if (war != null) {
            return war;
        }
        return findFirstArchive(targetDir, "*.jar");
    }

    private static Path findFirstArchive(Path targetDir, String glob) {
        if (targetDir == null || !Files.isDirectory(targetDir)) {
            return null;
        }
        Path best = null;
        long bestTime = Long.MIN_VALUE;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetDir, glob)) {
            for (Path p : stream) {
                if (p == null) {
                    continue;
                }
                String name = p.getFileName().toString();
                if (name.endsWith(".jar.original") || name.endsWith(".war.original")) {
                    continue;
                }
                if (Files.isRegularFile(p)) {
                    long modified = 0L;
                    try {
                        modified = Files.getLastModifiedTime(p).toMillis();
                    } catch (Exception ignored) {
                    }
                    if (best == null || modified >= bestTime) {
                        best = p;
                        bestTime = modified;
                    }
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return best;
    }

    private static void runMavenPackage(Path projectDir) {
        if (projectDir == null) {
            Assertions.fail("fixture project dir is null");
        }
        ProcessBuilder pb = new ProcessBuilder(resolveMavenCommand(projectDir));
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

    private static List<String> resolveMavenCommand(Path projectDir) {
        List<String> wrapper = resolveWrapperCommand(projectDir);
        if (!wrapper.isEmpty()) {
            return wrapper;
        }
        Path executable = resolveMavenExecutable();
        if (executable != null) {
            return List.of(executable.toString(), "-q", "-DskipTests", "package");
        }
        return List.of("mvn.cmd", "-q", "-DskipTests", "package");
    }

    private static List<String> resolveWrapperCommand(Path projectDir) {
        if (projectDir == null) {
            return List.of();
        }
        Path windowsWrapper = projectDir.resolve("mvnw.cmd");
        if (Files.isRegularFile(windowsWrapper)) {
            return List.of(windowsWrapper.toString(), "-q", "-DskipTests", "package");
        }
        Path wrapper = projectDir.resolve("mvnw");
        if (Files.isRegularFile(wrapper)) {
            return List.of(wrapper.toString(), "-q", "-DskipTests", "package");
        }
        return List.of();
    }

    private static Path resolveMavenExecutable() {
        Path fromProperty = resolveMavenHomeExecutable(System.getProperty("maven.home"));
        if (fromProperty != null) {
            return fromProperty;
        }
        Path fromEnvHome = resolveMavenHomeExecutable(System.getenv("MAVEN_HOME"));
        if (fromEnvHome != null) {
            return fromEnvHome;
        }
        Path fromEnvM2 = resolveMavenHomeExecutable(System.getenv("M2_HOME"));
        if (fromEnvM2 != null) {
            return fromEnvM2;
        }
        return resolveFromPath();
    }

    private static Path resolveMavenHomeExecutable(String home) {
        if (home == null || home.isBlank()) {
            return null;
        }
        Path bin = Paths.get(home).resolve("bin");
        Path windows = bin.resolve("mvn.cmd");
        if (Files.isRegularFile(windows)) {
            return windows;
        }
        Path unix = bin.resolve("mvn");
        if (Files.isRegularFile(unix)) {
            return unix;
        }
        return null;
    }

    private static Path resolveFromPath() {
        String pathValue = System.getenv("PATH");
        if (pathValue == null || pathValue.isBlank()) {
            return null;
        }
        String[] entries = pathValue.split(java.io.File.pathSeparator);
        List<String> candidates = new ArrayList<>();
        if (isWindows()) {
            candidates.add("mvn.cmd");
            candidates.add("mvn.bat");
            candidates.add("mvn.exe");
        }
        candidates.add("mvn");
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            Path dir = Paths.get(entry.trim());
            for (String candidate : candidates) {
                Path executable = dir.resolve(candidate);
                if (Files.isRegularFile(executable)) {
                    return executable;
                }
            }
        }
        return null;
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("win");
    }
}
