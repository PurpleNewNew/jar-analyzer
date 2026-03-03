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

import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class JdkArchiveResolver {
    private static final Logger logger = LogManager.getLogger();
    private static final String MODULES_PROP = "jar.analyzer.jdk.modules";
    private static final String DEFAULT_MODULE_POLICY = "core";
    private static final List<String> CORE_MODULES = List.of("java.base", "java.desktop", "java.logging");

    private JdkArchiveResolver() {
    }

    public static JdkResolution resolve(Path sdkPath) {
        String policy = System.getProperty(MODULES_PROP, DEFAULT_MODULE_POLICY);
        return resolve(sdkPath, policy);
    }

    public static JdkResolution resolve(Path sdkPath, String modulePolicy) {
        Path runtime = detectRuntimeHome(sdkPath);
        if (runtime == null) {
            return JdkResolution.none();
        }

        Set<Path> rawArchives = new LinkedHashSet<>();
        if (Files.isRegularFile(runtime)) {
            rawArchives.add(runtime);
        } else {
            Path rtJar = runtime.resolve(Paths.get("lib", "rt.jar"));
            Path jreRtJar = runtime.resolve(Paths.get("jre", "lib", "rt.jar"));
            if (Files.isRegularFile(rtJar) || Files.isRegularFile(jreRtJar)) {
                if (Files.isRegularFile(rtJar)) {
                    rawArchives.add(rtJar);
                }
                if (Files.isRegularFile(jreRtJar)) {
                    rawArchives.add(jreRtJar);
                }
                Path jceJar = runtime.resolve(Paths.get("lib", "jce.jar"));
                if (Files.isRegularFile(jceJar)) {
                    rawArchives.add(jceJar);
                }
                Path jreJceJar = runtime.resolve(Paths.get("jre", "lib", "jce.jar"));
                if (Files.isRegularFile(jreJceJar)) {
                    rawArchives.add(jreJceJar);
                }
                return new JdkResolution(runtime, new ArrayList<>(rawArchives), "rt-jar", policyOrDefault(modulePolicy));
            }

            Path jmodsDir = resolveJmodsDir(runtime);
            if (jmodsDir != null && Files.isDirectory(jmodsDir)) {
                List<Path> selected = selectJmods(jmodsDir, modulePolicy);
                rawArchives.addAll(selected);
                List<Path> converted = JmodToJarConverter.convertAll(new ArrayList<>(rawArchives));
                if (!converted.isEmpty()) {
                    return new JdkResolution(runtime, converted, "jmods", policyOrDefault(modulePolicy));
                }
                return new JdkResolution(runtime, new ArrayList<>(rawArchives), "jmods", policyOrDefault(modulePolicy));
            }
        }

        List<Path> fallback = new ArrayList<>();
        if (!rawArchives.isEmpty()) {
            fallback.addAll(rawArchives);
        }
        return fallback.isEmpty()
                ? JdkResolution.none()
                : new JdkResolution(runtime, fallback, "custom", policyOrDefault(modulePolicy));
    }

    private static List<Path> selectJmods(Path jmodsDir, String modulePolicy) {
        List<Path> allModules = listAllJmods(jmodsDir);
        if (allModules.isEmpty()) {
            return List.of();
        }
        String policy = policyOrDefault(modulePolicy).toLowerCase(Locale.ROOT);
        if ("all".equals(policy)) {
            return allModules;
        }
        Set<String> wanted = new LinkedHashSet<>();
        if ("core".equals(policy)) {
            wanted.addAll(CORE_MODULES);
        } else {
            String[] parts = policy.split(",");
            for (String part : parts) {
                if (part == null || part.isBlank()) {
                    continue;
                }
                String module = normalizeModuleName(part);
                if (!module.isBlank()) {
                    wanted.add(module);
                }
            }
            if (wanted.isEmpty()) {
                wanted.addAll(CORE_MODULES);
            }
        }

        List<Path> selected = new ArrayList<>();
        for (Path jmod : allModules) {
            String module = normalizeModuleName(jmod.getFileName() == null ? "" : jmod.getFileName().toString());
            if (wanted.contains(module)) {
                selected.add(jmod);
            }
        }
        if (selected.isEmpty()) {
            logger.warn("jdk modules policy {} matched nothing in {}, fallback core", policy, jmodsDir);
            for (Path jmod : allModules) {
                String module = normalizeModuleName(jmod.getFileName() == null ? "" : jmod.getFileName().toString());
                if (CORE_MODULES.contains(module)) {
                    selected.add(jmod);
                }
            }
        }
        return selected;
    }

    private static List<Path> listAllJmods(Path jmodsDir) {
        if (jmodsDir == null || !Files.isDirectory(jmodsDir)) {
            return List.of();
        }
        try (var stream = Files.list(jmodsDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.endsWith(".jmod");
                    })
                    .map(path -> path.toAbsolutePath().normalize())
                    .sorted()
                    .toList();
        } catch (Exception ex) {
            logger.warn("list jmods failed: {}", ex.toString());
            return List.of();
        }
    }

    private static Path resolveJmodsDir(Path runtimeHome) {
        if (runtimeHome == null) {
            return null;
        }
        Path normalized = runtimeHome.toAbsolutePath().normalize();
        if (safeName(normalized).equals("jmods") && Files.isDirectory(normalized)) {
            return normalized;
        }
        Path jmods = normalized.resolve("jmods");
        if (Files.isDirectory(jmods)) {
            return jmods;
        }
        Path parent = normalized.getParent();
        if (parent != null) {
            Path parentJmods = parent.resolve("jmods");
            if (Files.isDirectory(parentJmods)) {
                return parentJmods;
            }
        }
        return null;
    }

    private static Path detectRuntimeHome(Path sdkPath) {
        if (sdkPath != null) {
            Path normalized = normalizePath(sdkPath);
            if (normalized != null && Files.exists(normalized)) {
                return normalized;
            }
        }

        String javaHome = System.getProperty("java.home");
        if (javaHome != null && !javaHome.isBlank()) {
            Path home = Paths.get(javaHome).toAbsolutePath().normalize();
            if (Files.exists(home)) {
                return home;
            }
        }

        String envHome = System.getenv("JAVA_HOME");
        if (envHome != null && !envHome.isBlank()) {
            Path home = Paths.get(envHome).toAbsolutePath().normalize();
            if (Files.exists(home)) {
                return home;
            }
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
            return path.normalize();
        }
    }

    private static String safeName(Path path) {
        if (path == null || path.getFileName() == null) {
            return "";
        }
        return path.getFileName().toString().toLowerCase(Locale.ROOT);
    }

    private static String normalizeModuleName(String raw) {
        String module = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (module.endsWith(".jmod")) {
            module = module.substring(0, module.length() - ".jmod".length());
        }
        return module;
    }

    private static String policyOrDefault(String policy) {
        if (policy == null || policy.isBlank()) {
            return DEFAULT_MODULE_POLICY;
        }
        return policy.trim();
    }

    public record JdkResolution(Path runtimeHome,
                                List<Path> archives,
                                String strategy,
                                String modulesPolicy) {
        public JdkResolution {
            runtimeHome = runtimeHome == null ? null : runtimeHome.toAbsolutePath().normalize();
            if (archives == null || archives.isEmpty()) {
                archives = List.of();
            } else {
                List<Path> normalized = new ArrayList<>();
                for (Path archive : archives) {
                    if (archive == null) {
                        continue;
                    }
                    normalized.add(archive.toAbsolutePath().normalize());
                }
                archives = normalized.isEmpty() ? List.of() : List.copyOf(normalized);
            }
            strategy = strategy == null ? "none" : strategy;
            modulesPolicy = modulesPolicy == null ? DEFAULT_MODULE_POLICY : modulesPolicy;
        }

        public static JdkResolution none() {
            return new JdkResolution(null, Collections.emptyList(), "none", DEFAULT_MODULE_POLICY);
        }

        public int sdkEntryCount() {
            return archives.size();
        }
    }
}
