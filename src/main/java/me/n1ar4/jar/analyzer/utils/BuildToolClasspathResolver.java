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

import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Resolve project classpath from build tools (Maven / Gradle).
 */
public final class BuildToolClasspathResolver {
    private static final Logger logger = LogManager.getLogger();
    private static final long MAVEN_TIMEOUT_SEC = 120L;
    private static final long GRADLE_TIMEOUT_SEC = 180L;
    private static final int MAX_OUTPUT_LINES = 20_000;

    private BuildToolClasspathResolver() {
    }

    public static List<Path> resolveProjectClasspath(Path projectRoot) {
        if (projectRoot == null || !Files.isDirectory(projectRoot)) {
            return List.of();
        }
        LinkedHashSet<Path> out = new LinkedHashSet<>();
        boolean hasPom = Files.exists(projectRoot.resolve("pom.xml"));
        boolean hasGradle = Files.exists(projectRoot.resolve("build.gradle"))
                || Files.exists(projectRoot.resolve("build.gradle.kts"))
                || Files.exists(projectRoot.resolve("settings.gradle"))
                || Files.exists(projectRoot.resolve("settings.gradle.kts"));
        if (hasPom) {
            out.addAll(resolveWithMaven(projectRoot));
        }
        if (hasGradle) {
            out.addAll(resolveWithGradle(projectRoot));
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static List<Path> resolveWithMaven(Path projectRoot) {
        List<String> executable = resolveMavenExecutable(projectRoot);
        if (executable.isEmpty()) {
            return List.of();
        }
        Path outputFile = createTempFile("maven-classpath", ".txt");
        if (outputFile == null) {
            return List.of();
        }
        List<String> command = new ArrayList<>(executable);
        command.add("-q");
        command.add("-DskipTests");
        command.add("-Dmdep.includeScope=test");
        command.add("-Dmdep.outputAbsoluteArtifactFilename=true");
        command.add("-Dmdep.pathSeparator=" + java.io.File.pathSeparator);
        command.add("-Dmdep.outputFile=" + outputFile.toAbsolutePath());
        command.add("dependency:build-classpath");

        CommandResult result = runCommand(projectRoot, command, MAVEN_TIMEOUT_SEC);
        List<Path> parsed = parseClasspathFile(outputFile);
        tryDelete(outputFile);
        if (!parsed.isEmpty()) {
            logger.info("resolved maven classpath: {}", parsed.size());
            return parsed;
        }
        if (result.timedOut) {
            logger.warn("maven classpath resolve timed out");
        } else if (result.exitCode != 0) {
            logger.debug("maven classpath resolve failed (code={}): {}", result.exitCode, result.outputPreview());
        }
        return List.of();
    }

    private static List<Path> resolveWithGradle(Path projectRoot) {
        List<String> executable = resolveGradleExecutable(projectRoot);
        if (executable.isEmpty()) {
            return List.of();
        }
        Path initScript = createTempFile("gradle-classpath-init", ".gradle");
        if (initScript == null) {
            return List.of();
        }
        try {
            Files.writeString(initScript, gradleInitScript(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            logger.debug("write gradle init script failed: {}", ex.toString());
            tryDelete(initScript);
            return List.of();
        }

        List<String> command = new ArrayList<>(executable);
        command.add("-q");
        command.add("--no-daemon");
        command.add("--console=plain");
        command.add("-I");
        command.add(initScript.toAbsolutePath().toString());
        command.add("jaPrintClasspath");

        CommandResult result = runCommand(projectRoot, command, GRADLE_TIMEOUT_SEC);
        tryDelete(initScript);
        List<Path> parsed = parseGradleOutput(result.output);
        if (!parsed.isEmpty()) {
            logger.info("resolved gradle classpath: {}", parsed.size());
            return parsed;
        }
        if (result.timedOut) {
            logger.warn("gradle classpath resolve timed out");
        } else if (result.exitCode != 0) {
            logger.debug("gradle classpath resolve failed (code={}): {}", result.exitCode, result.outputPreview());
        }
        return List.of();
    }

    private static List<String> resolveMavenExecutable(Path projectRoot) {
        boolean windows = OSUtil.isWindows();
        if (windows) {
            Path wrapperCmd = projectRoot.resolve("mvnw.cmd");
            if (Files.isRegularFile(wrapperCmd)) {
                return List.of(wrapperCmd.toAbsolutePath().toString());
            }
            Path wrapper = projectRoot.resolve("mvnw");
            if (Files.isRegularFile(wrapper)) {
                return List.of(wrapper.toAbsolutePath().toString());
            }
            return List.of("mvn");
        }
        Path wrapper = projectRoot.resolve("mvnw");
        if (Files.isRegularFile(wrapper)) {
            if (Files.isExecutable(wrapper)) {
                return List.of(wrapper.toAbsolutePath().toString());
            }
            return List.of("sh", wrapper.toAbsolutePath().toString());
        }
        return List.of("mvn");
    }

    private static List<String> resolveGradleExecutable(Path projectRoot) {
        boolean windows = OSUtil.isWindows();
        if (windows) {
            Path wrapperCmd = projectRoot.resolve("gradlew.bat");
            if (Files.isRegularFile(wrapperCmd)) {
                return List.of(wrapperCmd.toAbsolutePath().toString());
            }
            return List.of("gradle");
        }
        Path wrapper = projectRoot.resolve("gradlew");
        if (Files.isRegularFile(wrapper)) {
            if (Files.isExecutable(wrapper)) {
                return List.of(wrapper.toAbsolutePath().toString());
            }
            return List.of("sh", wrapper.toAbsolutePath().toString());
        }
        return List.of("gradle");
    }

    private static CommandResult runCommand(Path workDir, List<String> command, long timeoutSec) {
        Path outputFile = createTempFile("build-tool-cp", ".log");
        if (outputFile == null) {
            return new CommandResult(-1, false, "");
        }
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(true);
            pb.redirectOutput(outputFile.toFile());
            process = pb.start();
            boolean finished = process.waitFor(timeoutSec, TimeUnit.SECONDS);
            if (!finished) {
                process.destroy();
                try {
                    process.waitFor(3, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
                return new CommandResult(-1, true, readTextLimited(outputFile));
            }
            int code = process.exitValue();
            return new CommandResult(code, false, readTextLimited(outputFile));
        } catch (Exception ex) {
            return new CommandResult(-1, false, ex.toString());
        } finally {
            tryDelete(outputFile);
        }
    }

    private static List<Path> parseClasspathFile(Path outputFile) {
        if (outputFile == null || Files.notExists(outputFile)) {
            return List.of();
        }
        String raw;
        try {
            raw = Files.readString(outputFile, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            logger.debug("read classpath file failed: {}", ex.toString());
            return List.of();
        }
        return parsePathList(raw, java.io.File.pathSeparator);
    }

    private static List<Path> parseGradleOutput(String output) {
        if (output == null || output.isBlank()) {
            return List.of();
        }
        LinkedHashSet<Path> out = new LinkedHashSet<>();
        String[] lines = output.split("\\R");
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String trimmed = line.trim();
            if (!trimmed.startsWith("JA_CP=")) {
                continue;
            }
            String value = trimmed.substring("JA_CP=".length()).trim();
            Path p = normalizePath(value);
            if (p != null && Files.exists(p)) {
                out.add(p);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static List<Path> parsePathList(String raw, String separator) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String text = raw.trim();
        LinkedHashSet<Path> out = new LinkedHashSet<>();
        String[] parts = text.split(java.util.regex.Pattern.quote(separator));
        for (String part : parts) {
            Path p = normalizePath(part);
            if (p != null && Files.exists(p)) {
                out.add(p);
            }
        }
        if (out.isEmpty() && text.contains("Dependencies classpath:")) {
            int idx = text.indexOf("Dependencies classpath:");
            String tail = text.substring(idx + "Dependencies classpath:".length()).trim();
            String[] lines = tail.split("\\R");
            if (lines.length > 0) {
                return parsePathList(lines[0], separator);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static Path normalizePath(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.startsWith("[INFO]")) {
            value = value.substring("[INFO]".length()).trim();
        }
        if (value.startsWith("JA_CP=")) {
            value = value.substring("JA_CP=".length()).trim();
        }
        if (value.isEmpty()) {
            return null;
        }
        if (value.startsWith("file:")) {
            value = value.substring("file:".length());
        }
        try {
            return Paths.get(value).toAbsolutePath().normalize();
        } catch (Exception ex) {
            return null;
        }
    }

    private static Path createTempFile(String prefix, String suffix) {
        try {
            Path base = Paths.get(Const.tempDir, "build-tool");
            Files.createDirectories(base);
            return Files.createTempFile(base, prefix + "-", suffix);
        } catch (Exception ex) {
            logger.debug("create temp file failed: {}", ex.toString());
            return null;
        }
    }

    private static String readTextLimited(Path path) {
        if (path == null || Files.notExists(path)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int lines = 0;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (lines >= MAX_OUTPUT_LINES) {
                    sb.append("\n...(truncated)");
                    break;
                }
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(line);
                lines++;
            }
        } catch (Exception ex) {
            return ex.toString();
        }
        return sb.toString();
    }

    private static void tryDelete(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
        }
    }

    private static String gradleInitScript() {
        return "allprojects {\n"
                + "  tasks.register(\"jaPrintClasspath\") {\n"
                + "    doLast {\n"
                + "      def confNames = [\"compileClasspath\", \"runtimeClasspath\", \"testCompileClasspath\", \"testRuntimeClasspath\"]\n"
                + "      def files = new LinkedHashSet<File>()\n"
                + "      confNames.each { name ->\n"
                + "        def conf = configurations.findByName(name)\n"
                + "        if (conf != null && conf.canBeResolved) {\n"
                + "          try {\n"
                + "            files.addAll(conf.resolve())\n"
                + "          } catch (Exception ignored) {\n"
                + "          }\n"
                + "        }\n"
                + "      }\n"
                + "      files.each { f -> println(\"JA_CP=\" + f.absolutePath) }\n"
                + "    }\n"
                + "  }\n"
                + "}\n";
    }

    private static final class CommandResult {
        private final int exitCode;
        private final boolean timedOut;
        private final String output;

        private CommandResult(int exitCode, boolean timedOut, String output) {
            this.exitCode = exitCode;
            this.timedOut = timedOut;
            this.output = output == null ? "" : output;
        }

        private String outputPreview() {
            if (output.isBlank()) {
                return "";
            }
            String text = output.trim();
            if (text.length() <= 500) {
                return text;
            }
            return text.substring(0, 500) + "...";
        }
    }
}
