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

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ClasspathResolver {
    private static final Logger logger = LogManager.getLogger();
    private static final String EXTRA_PROP = "jar.analyzer.classpath.extra";
    private static final String INCLUDE_MANIFEST_PROP = "jar.analyzer.classpath.includeManifest";
    private static final String INCLUDE_NESTED_LIB_PROP = "jar.analyzer.classpath.includeNestedLib";
    private static final String SCAN_DEPTH_PROP = "jar.analyzer.classpath.scanDepth";
    static final String CONFLICT_PROP = "jar.analyzer.classpath.conflict";
    private static final String NESTED_CACHE_DIR = "classpath-nested";
    private static final Map<String, Path> NESTED_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private ClasspathResolver() {
    }

    public enum ConflictStrategy {
        FIRST,
        NEAREST
    }

    private enum SourceKind {
        ROOT(0),
        DEPENDENCY(1),
        EXTRA(2);

        private final int rank;

        SourceKind(int rank) {
            this.rank = rank;
        }
    }

    public static ConflictStrategy resolveConflictStrategy() {
        String raw = System.getProperty(CONFLICT_PROP);
        if (StringUtil.isBlank(raw)) {
            return ConflictStrategy.NEAREST;
        }
        String val = raw.strip().toLowerCase(Locale.ROOT);
        if ("first".equals(val)) {
            return ConflictStrategy.FIRST;
        }
        return ConflictStrategy.NEAREST;
    }

    public static List<String> resolveInputArchives(Path inputPath, Path rtPath, boolean extended) {
        return resolveInputArchives(inputPath, rtPath, extended, isNestedLibEnabled());
    }

    public static List<String> resolveInputArchives(Path inputPath,
                                                    Path rtPath,
                                                    boolean extended,
                                                    boolean includeNested) {
        if (inputPath == null) {
            return Collections.emptyList();
        }
        Set<Path> result = new LinkedHashSet<>();
        collectInputPath(inputPath, result, extended, includeNested);
        if (extended) {
            collectExtraClasspath(result);
        }
        if (rtPath != null && Files.exists(rtPath)) {
            result.add(rtPath.toAbsolutePath().normalize());
        }
        List<String> out = new ArrayList<>();
        for (Path p : result) {
            out.add(p.toAbsolutePath().toString());
        }
        return out;
    }

    public static List<Path> resolveUserArchives(String rootPath) {
        if (StringUtil.isBlank(rootPath)) {
            return Collections.emptyList();
        }
        Set<Path> result = new LinkedHashSet<>();
        collectInputPath(Paths.get(rootPath.strip()), result, true, isNestedLibEnabled());
        collectExtraClasspath(result);
        return new ArrayList<>(result);
    }

    public static ClasspathGraph resolveClasspathGraph(Path inputPath) {
        return resolveClasspathGraph(inputPath, isNestedLibEnabled());
    }

    public static ClasspathGraph resolveClasspathGraph(Path inputPath, boolean includeNested) {
        if (inputPath == null) {
            return new ClasspathGraph(Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
        }
        Map<Path, GraphNode> nodes = new LinkedHashMap<>();
        Deque<GraphNode> queue = new ArrayDeque<>();
        int order = 0;
        int maxDepth = resolveScanDepth();
        List<Path> roots = resolveRootArchives(inputPath, maxDepth);
        for (Path root : roots) {
            if (root == null) {
                continue;
            }
            GraphNode node = new GraphNode(root, 0, SourceKind.ROOT, order++);
            nodes.put(root, node);
            queue.add(node);
        }
        List<Path> extra = resolveExtraClasspath(maxDepth);
        for (Path path : extra) {
            if (path == null || nodes.containsKey(path)) {
                continue;
            }
            GraphNode node = new GraphNode(path, 1, SourceKind.EXTRA, order++);
            nodes.put(path, node);
            queue.add(node);
        }
        while (!queue.isEmpty()) {
            GraphNode current = queue.poll();
            if (current == null) {
                continue;
            }
            if (current.depth >= maxDepth) {
                continue;
            }
            if (!isArchive(current.path)) {
                continue;
            }
            Set<Path> deps = collectArchiveDependencies(current.path, includeNested);
            if (deps.isEmpty()) {
                continue;
            }
            for (Path dep : deps) {
                if (dep == null || nodes.containsKey(dep)) {
                    continue;
                }
                GraphNode node = new GraphNode(dep, current.depth + 1, SourceKind.DEPENDENCY, order++);
                nodes.put(dep, node);
                queue.add(node);
            }
        }
        List<GraphNode> sorted = new ArrayList<>(nodes.values());
        sorted.sort(Comparator.comparingInt((GraphNode node) -> node.depth)
                .thenComparingInt(node -> node.sourceKind.rank)
                .thenComparingInt(node -> node.order));
        List<Path> ordered = new ArrayList<>();
        Map<Path, Integer> depthByPath = new LinkedHashMap<>();
        Map<Path, Integer> orderIndex = new LinkedHashMap<>();
        int idx = 0;
        for (GraphNode node : sorted) {
            ordered.add(node.path);
            depthByPath.put(node.path, node.depth);
            orderIndex.put(node.path, idx++);
        }
        return new ClasspathGraph(ordered, depthByPath, orderIndex);
    }

    private static void collectInputPath(Path inputPath,
                                         Set<Path> result,
                                         boolean extended,
                                         boolean includeNested) {
        if (inputPath == null) {
            return;
        }
        Path normalized = inputPath.toAbsolutePath().normalize();
        if (!Files.exists(normalized)) {
            return;
        }
        if (Files.isDirectory(normalized)) {
            scanDirectory(normalized, result, resolveScanDepth());
            return;
        }
        if (isArchive(normalized) || isClassFile(normalized)) {
            result.add(normalized);
            if (extended && isArchive(normalized)) {
                if (isManifestEnabled()) {
                    collectManifestClasspath(normalized, result);
                }
                if (isSiblingLibEnabled()) {
                    collectSiblingLibs(normalized, result);
                }
                if (includeNested) {
                    collectNestedLibs(normalized, result);
                }
            }
        }
    }

    private static void collectExtraClasspath(Set<Path> result) {
        String extra = System.getProperty(EXTRA_PROP);
        if (StringUtil.isBlank(extra)) {
            return;
        }
        String sep = File.pathSeparator;
        String[] parts = extra.split(java.util.regex.Pattern.quote(sep));
        for (String part : parts) {
            String p = part == null ? null : part.strip();
            if (p == null || p.isEmpty()) {
                continue;
            }
            Path path = Paths.get(p);
            if (!Files.exists(path)) {
                continue;
            }
            if (Files.isDirectory(path)) {
                scanDirectory(path, result, resolveScanDepth());
            } else if (isArchive(path) || isClassFile(path)) {
                result.add(path.toAbsolutePath().normalize());
            }
        }
    }

    private static void collectManifestClasspath(Path archive, Set<Path> result) {
        try (JarFile jarFile = new JarFile(archive.toFile())) {
            Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                return;
            }
            String raw = manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
            if (StringUtil.isBlank(raw)) {
                return;
            }
            String[] parts = raw.strip().split("\\s+");
            Path base = archive.getParent();
            for (String part : parts) {
                String p = part == null ? null : part.strip();
                if (p == null || p.isEmpty()) {
                    continue;
                }
                if (p.startsWith("http:") || p.startsWith("https:") || p.startsWith("file:")) {
                    continue;
                }
                Path dep = base == null ? Paths.get(p) : base.resolve(p);
                if (Files.exists(dep) && (isArchive(dep) || isClassFile(dep))) {
                    result.add(dep.toAbsolutePath().normalize());
                }
            }
        } catch (Exception ex) {
            logger.debug("manifest classpath parse failed: {}", ex.toString());
        }
    }

    private static void collectSiblingLibs(Path archive, Set<Path> result) {
        Path parent = archive.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            return;
        }
        Path lib = parent.resolve("lib");
        Path libs = parent.resolve("libs");
        if (Files.isDirectory(lib)) {
            scanDirectory(lib, result, 2);
        }
        if (Files.isDirectory(libs)) {
            scanDirectory(libs, result, 2);
        }
    }

    private static void collectNestedLibs(Path archive, Set<Path> result) {
        if (archive == null || !Files.exists(archive)) {
            return;
        }
        try (ZipFile zipFile = new ZipFile(archive.toFile())) {
            java.util.Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry == null) {
                    continue;
                }
                String name = entry.getName();
                if (name == null || !name.endsWith(".jar")) {
                    continue;
                }
                if (!isLikelyNestedLib(name)) {
                    continue;
                }
                Path nested = extractNestedJar(archive, zipFile, entry);
                if (nested != null && Files.exists(nested)) {
                    result.add(nested.toAbsolutePath().normalize());
                }
            }
        } catch (Exception ex) {
            logger.debug("collect nested libs failed: {}", ex.toString());
        }
    }

    private static boolean isLikelyNestedLib(String name) {
        if (name.startsWith("BOOT-INF/lib/") || name.startsWith("WEB-INF/lib/")) {
            return true;
        }
        if (name.startsWith("lib/") || name.contains("/lib/")) {
            return true;
        }
        return name.startsWith("META-INF/lib/");
    }

    private static Path extractNestedJar(Path archive, ZipFile zipFile, ZipEntry entry) {
        String key = archive.toAbsolutePath() + "!" + entry.getName();
        Path cached = NESTED_CACHE.get(key);
        if (cached != null && Files.exists(cached) && isCacheFresh(cached, archive, entry)) {
            return cached;
        }
        Path out = buildNestedJarPath(key, entry.getName());
        try {
            Path parent = out.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                Files.copy(inputStream, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            NESTED_CACHE.put(key, out);
            return out;
        } catch (Exception ex) {
            logger.debug("extract nested jar failed: {}", ex.toString());
            return null;
        }
    }

    private static boolean isCacheFresh(Path cached, Path archive, ZipEntry entry) {
        if (cached == null || !Files.exists(cached)) {
            return false;
        }
        try {
            long cachedTime = Files.getLastModifiedTime(cached).toMillis();
            long sourceTime = resolveSourceTime(archive, entry);
            if (sourceTime <= 0) {
                return true;
            }
            return cachedTime >= sourceTime;
        } catch (Exception ex) {
            logger.debug("classpath cache freshness check failed: {}: {}", cached, ex.toString());
            return false;
        }
    }

    private static long resolveSourceTime(Path archive, ZipEntry entry) {
        long entryTime = entry == null ? -1L : entry.getTime();
        if (entryTime > 0) {
            return entryTime;
        }
        if (archive != null && Files.exists(archive)) {
            try {
                return Files.getLastModifiedTime(archive).toMillis();
            } catch (Exception ex) {
                logger.debug("resolve classpath source mtime failed: {}: {}", archive, ex.toString());
            }
        }
        return -1L;
    }

    private static Path buildNestedJarPath(String key, String entryName) {
        String fileName = entryName == null ? "nested.jar" : Paths.get(entryName).getFileName().toString();
        String safe = Integer.toHexString(key.hashCode());
        return Paths.get(Const.tempDir, NESTED_CACHE_DIR, safe + "-" + fileName);
    }

    private static void scanDirectory(Path root, Set<Path> result, int depth) {
        if (root == null || !Files.isDirectory(root)) {
            return;
        }
        try (java.util.stream.Stream<Path> stream = Files.walk(root, Math.max(1, depth))) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> isArchive(p) || isClassFile(p))
                    .map(p -> p.toAbsolutePath().normalize())
                    .forEach(result::add);
        } catch (Exception ex) {
            logger.debug("scan classpath dir failed: {}", ex.toString());
        }
    }

    private static boolean isArchive(Path path) {
        if (path == null) {
            return false;
        }
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".jar") || name.endsWith(".war");
    }

    private static boolean isClassFile(Path path) {
        if (path == null) {
            return false;
        }
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".class");
    }

    private static boolean isManifestEnabled() {
        String raw = System.getProperty(INCLUDE_MANIFEST_PROP);
        if (StringUtil.isBlank(raw)) {
            return true;
        }
        return Boolean.parseBoolean(raw.strip());
    }

    private static boolean isSiblingLibEnabled() {
        return false;
    }

    private static boolean isNestedLibEnabled() {
        String raw = System.getProperty(INCLUDE_NESTED_LIB_PROP);
        if (StringUtil.isBlank(raw)) {
            return true;
        }
        return Boolean.parseBoolean(raw.strip());
    }

    private static int resolveScanDepth() {
        String raw = System.getProperty(SCAN_DEPTH_PROP);
        if (StringUtil.isBlank(raw)) {
            return 6;
        }
        try {
            int val = Integer.parseInt(raw.strip());
            if (val < 1) {
                return 1;
            }
            if (val > 12) {
                return 12;
            }
            return val;
        } catch (NumberFormatException ex) {
            logger.debug("invalid classpath scan depth: {}", raw);
            return 6;
        }
    }

    private static List<Path> resolveRootArchives(Path inputPath, int scanDepth) {
        if (inputPath == null) {
            return Collections.emptyList();
        }
        Path normalized = inputPath.toAbsolutePath().normalize();
        if (!Files.exists(normalized)) {
            return Collections.emptyList();
        }
        if (Files.isDirectory(normalized)) {
            return scanDirectoryOrdered(normalized, scanDepth);
        }
        if (isArchive(normalized) || isClassFile(normalized)) {
            return Collections.singletonList(normalized);
        }
        return Collections.emptyList();
    }

    private static List<Path> resolveExtraClasspath(int scanDepth) {
        String extra = System.getProperty(EXTRA_PROP);
        if (StringUtil.isBlank(extra)) {
            return Collections.emptyList();
        }
        String sep = File.pathSeparator;
        String[] parts = extra.split(java.util.regex.Pattern.quote(sep));
        List<Path> out = new ArrayList<>();
        for (String part : parts) {
            String p = part == null ? null : part.strip();
            if (p == null || p.isEmpty()) {
                continue;
            }
            Path path = Paths.get(p);
            if (!Files.exists(path)) {
                continue;
            }
            if (Files.isDirectory(path)) {
                out.addAll(scanDirectoryOrdered(path, scanDepth));
            } else if (isArchive(path) || isClassFile(path)) {
                out.add(path.toAbsolutePath().normalize());
            }
        }
        return out;
    }

    private static Set<Path> collectArchiveDependencies(Path archive, boolean includeNested) {
        if (archive == null || !Files.exists(archive)) {
            return Collections.emptySet();
        }
        Set<Path> deps = new LinkedHashSet<>();
        if (isManifestEnabled()) {
            collectManifestClasspath(archive, deps);
        }
        if (isSiblingLibEnabled()) {
            collectSiblingLibs(archive, deps);
        }
        if (includeNested) {
            collectNestedLibs(archive, deps);
        }
        return deps;
    }

    private static List<Path> scanDirectoryOrdered(Path root, int depth) {
        if (root == null || !Files.isDirectory(root)) {
            return Collections.emptyList();
        }
        try (java.util.stream.Stream<Path> stream = Files.walk(root, Math.max(1, depth))) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> isArchive(p) || isClassFile(p))
                    .map(p -> p.toAbsolutePath().normalize())
                    .sorted(Comparator.comparing(Path::toString))
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception ex) {
            logger.debug("scan classpath dir failed: {}", ex.toString());
            return Collections.emptyList();
        }
    }

    public static final class ClasspathGraph {
        private final List<Path> orderedArchives;
        private final Map<Path, Integer> depthByPath;
        private final Map<Path, Integer> orderIndex;

        private ClasspathGraph(List<Path> orderedArchives,
                               Map<Path, Integer> depthByPath,
                               Map<Path, Integer> orderIndex) {
            this.orderedArchives = orderedArchives == null ? Collections.emptyList() : orderedArchives;
            this.depthByPath = depthByPath == null ? Collections.emptyMap() : depthByPath;
            this.orderIndex = orderIndex == null ? Collections.emptyMap() : orderIndex;
        }

        public List<Path> getOrderedArchives() {
            return orderedArchives;
        }

        public int getDepth(Path path) {
            if (path == null) {
                return Integer.MAX_VALUE;
            }
            Integer depth = depthByPath.get(path);
            return depth == null ? Integer.MAX_VALUE : depth;
        }

        public int getOrder(Path path) {
            if (path == null) {
                return Integer.MAX_VALUE;
            }
            Integer idx = orderIndex.get(path);
            return idx == null ? Integer.MAX_VALUE : idx;
        }
    }

    private static final class GraphNode {
        private final Path path;
        private final int depth;
        private final SourceKind sourceKind;
        private final int order;

        private GraphNode(Path path, int depth, SourceKind sourceKind, int order) {
            this.path = path;
            this.depth = depth;
            this.sourceKind = sourceKind == null ? SourceKind.DEPENDENCY : sourceKind;
            this.order = order;
        }
    }
}
