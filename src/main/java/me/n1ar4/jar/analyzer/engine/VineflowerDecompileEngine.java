/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.engine;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.cache.BuildScopedLru;
import me.n1ar4.jar.analyzer.core.notify.NotifierContext;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.project.ProjectRuntimeState;
import me.n1ar4.jar.analyzer.utils.ClasspathRegistry;
import me.n1ar4.jar.analyzer.utils.IOUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.jetbrains.java.decompiler.main.decompiler.BaseDecompiler;
import org.jetbrains.java.decompiler.main.decompiler.DirectoryResultSaver;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class VineflowerDecompileEngine {
    public static final String INFO = "<html>" +
            "<b>VineFlower</b> - modern Java decompiler" +
            "</html>";
    private static final Logger logger = LogManager.getLogger();
    private static final String DECOMPILER_PREFIX = "//\n" +
            "// Jar Analyzer by 4ra1n\n" +
            "// (powered by VineFlower decompiler)\n" +
            "//\n";
    private static final Object CACHE_LOCK = new Object();
    private static final int CACHE_CAPACITY = 2000;
    private static final AtomicLong LAST_RUNTIME_STATE_VERSION =
            new AtomicLong(ProjectRuntimeContext.stateVersion());
    private static volatile BuildScopedLru<String, String> codeCache =
            new BuildScopedLru<>(CACHE_CAPACITY, ProjectRuntimeContext::stateVersion);
    private static volatile BuildScopedLru<String, List<LineMapping>> lineMappingCache =
            new BuildScopedLru<>(CACHE_CAPACITY, ProjectRuntimeContext::stateVersion);

    /**
     * 使用 VineFlower 反编译指定的 class 文件。
     *
     * @param classFilePath class 文件绝对路径
     * @return 反编译后的 Java 源代码
     */
    public static String decompile(String classFilePath) {
        if (classFilePath == null || classFilePath.trim().isEmpty()) {
            logger.warn("class file path is null or empty");
            return null;
        }

        ensureFreshCaches();
        ProjectRuntimeState runtimeState = ProjectRuntimeContext.getState();
        String key = buildCacheKey(classFilePath, runtimeState);
        String cached = codeCache.get(key);
        if (cached != null) {
            logger.debug("get from cache: {}", classFilePath);
            return cached;
        }

        Path classPath;
        try {
            classPath = Paths.get(classFilePath);
        } catch (Exception ex) {
            logger.warn("class file path invalid: {}", ex.getMessage());
            return null;
        }
        if (!Files.exists(classPath)) {
            logger.warn("class file not exists: {}", classFilePath);
            return null;
        }
        return DecompileLookupContext.withClassPath(classPath,
                () -> decompileInternal(classPath, key));
    }

    private static String decompileInternal(Path classPath, String cacheKey) {
        try {
            VineflowerSingleResult result = runSingleClassDecompile(classPath, false);
            if (result == null || result.code == null || result.code.trim().isEmpty()) {
                logger.warn("vineflower decompile result is empty: {}", classPath);
                return null;
            }
            String code = DECOMPILER_PREFIX + result.code;
            codeCache.put(cacheKey, code);
            logger.debug("vineflower decompile success: {}", classPath);
            return code;
        } catch (Exception ex) {
            logger.warn("vineflower decompile fail: {}", ex.getMessage());
            return null;
        }
    }

    public static DecompileResult decompileWithLineMapping(String classFilePath) {
        if (classFilePath == null || classFilePath.trim().isEmpty()) {
            logger.warn("class file path is null or empty");
            return null;
        }
        ensureFreshCaches();
        ProjectRuntimeState runtimeState = ProjectRuntimeContext.getState();
        String key = buildCacheKey(classFilePath, runtimeState);
        String cached = codeCache.get(key);
        List<LineMapping> cachedMappings = lineMappingCache.get(key);
        if (cached != null && cachedMappings != null) {
            logger.debug("get from cache: {}", classFilePath);
            return new DecompileResult(cached, cachedMappings);
        }
        Path classPath;
        try {
            classPath = Paths.get(classFilePath);
        } catch (Exception ex) {
            logger.warn("class file path invalid: {}", ex.getMessage());
            return null;
        }
        if (!Files.exists(classPath)) {
            logger.warn("class file not exists: {}", classFilePath);
            return null;
        }
        return DecompileLookupContext.withClassPath(classPath,
                () -> decompileWithLineMappingInternal(classPath, key));
    }

    private static DecompileResult decompileWithLineMappingInternal(Path classPath, String cacheKey) {
        try {
            VineflowerSingleResult result = runSingleClassDecompile(classPath, true);
            if (result == null || result.code == null || result.code.trim().isEmpty()) {
                logger.warn("vineflower decompile result is empty: {}", classPath);
                return null;
            }
            int prefixLines = countLines(DECOMPILER_PREFIX);
            String code = DECOMPILER_PREFIX + result.code;
            List<LineMapping> builtMappings = buildLineMappings(classPath, result.mapping, prefixLines);
            codeCache.put(cacheKey, code);
            lineMappingCache.put(cacheKey, builtMappings);
            logger.debug("vineflower decompile success: {}", classPath);
            return new DecompileResult(code, builtMappings);
        } catch (Exception ex) {
            logger.warn("vineflower decompile fail: {}", ex.getMessage());
            return null;
        }
    }

    public static boolean decompileJars(List<String> jarsPath, String outputDir) {
        return decompileJars(jarsPath, outputDir, false);
    }

    public static boolean decompileJars(List<String> jarsPath,
                                        String outputDir,
                                        boolean decompileNested) {
        if (jarsPath == null || jarsPath.isEmpty()) {
            return false;
        }
        for (String jarPath : jarsPath) {
            if (!jarPath.toLowerCase(Locale.ROOT).endsWith(".jar")) {
                NotifierContext.get().warn("Jar Analyzer",
                        "ONLY SUPPORT JAR FILE\n只支持 JAR 文件（其他类型的文件可以手动压缩成 JAR 后尝试）");
                return false;
            }

            Path jarPathPath = Paths.get(jarPath);
            Path outBase = Paths.get(outputDir);
            String jarName = jarPathPath.getFileName().toString();
            String folderName = jarName.replaceAll("(?i)\\.jar$", "");
            Path exportRoot = outBase.resolve(folderName);
            Path srcDir = exportRoot.resolve("src");
            Path resDir = exportRoot.resolve("resources");
            Path libSrcDir = exportRoot.resolve("lib-src");
            try {
                if (Files.exists(exportRoot)) {
                    deleteDirectory(exportRoot);
                }
                Files.createDirectories(srcDir);
                Files.createDirectories(resDir);
            } catch (Exception ex) {
                logger.warn("create export dir failed: {}", ex.getMessage());
                return false;
            }

            logger.info("decompile jar: {}", jarPath);
            logger.info("output dir: {}", exportRoot.toAbsolutePath());

            try {
                decompileJarWithVineflower(jarPathPath, srcDir);
                copyResourcesFromJar(jarPathPath, resDir);
                if (decompileNested) {
                    Files.createDirectories(libSrcDir);
                    decompileNestedJars(resDir, libSrcDir);
                }
            } catch (Exception ex) {
                logger.warn("vineflower decompile jar fail: {}", ex.getMessage());
                return false;
            }
        }
        return true;
    }

    static Map<String, String> buildCommonOptions(boolean trackBytecodeLoc) {
        Map<String, String> options = new LinkedHashMap<>();
        options.put(IFernflowerPreferences.DECOMPILE_INNER, "1");
        options.put(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, "1");
        options.put(IFernflowerPreferences.DECOMPILE_PREVIEW, "1");
        options.put(IFernflowerPreferences.PATTERN_MATCHING, "1");
        options.put(IFernflowerPreferences.SWITCH_EXPRESSIONS, "1");
        options.put(IFernflowerPreferences.USE_DEBUG_VAR_NAMES, "1");
        options.put(IFernflowerPreferences.USE_METHOD_PARAMETERS, "1");
        options.put(IFernflowerPreferences.REMOVE_BRIDGE, "0");
        options.put(IFernflowerPreferences.REMOVE_SYNTHETIC, "0");
        if (trackBytecodeLoc) {
            options.put(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, "1");
            options.put(IFernflowerPreferences.DUMP_CODE_LINES, "1");
        }
        return options;
    }

    static Map<String, String> buildBatchOptions(Path srcDir) {
        Map<String, String> options = buildCommonOptions(false);
        options.put(IFernflowerPreferences.THREADS, "0");
        options.put("outputdir", srcDir.toAbsolutePath().toString());
        options.put("clobber", "true");
        options.put("outputencoding", "UTF-8");
        options.put("silent", "true");
        options.put("threadcount", "0");
        return options;
    }

    public static void cleanCache() {
        synchronized (CACHE_LOCK) {
            resetCaches();
            LAST_RUNTIME_STATE_VERSION.set(ProjectRuntimeContext.stateVersion());
        }
    }

    private static void ensureFreshCaches() {
        long version = ProjectRuntimeContext.stateVersion();
        if (version == LAST_RUNTIME_STATE_VERSION.get()) {
            return;
        }
        synchronized (CACHE_LOCK) {
            version = ProjectRuntimeContext.stateVersion();
            if (version == LAST_RUNTIME_STATE_VERSION.get()) {
                return;
            }
            resetCaches();
            LAST_RUNTIME_STATE_VERSION.set(version);
        }
    }

    private static void resetCaches() {
        codeCache = new BuildScopedLru<>(CACHE_CAPACITY, ProjectRuntimeContext::stateVersion);
        lineMappingCache = new BuildScopedLru<>(CACHE_CAPACITY, ProjectRuntimeContext::stateVersion);
    }

    private static String buildCacheKey(String classFilePath, ProjectRuntimeState runtimeState) {
        String runtimeKey = runtimeState == null ? "" : runtimeState.cacheKey();
        return "vineflower-" + runtimeKey + "|" + classFilePath;
    }

    private static VineflowerSingleResult runSingleClassDecompile(Path classPath, boolean trackBytecodeLoc) throws IOException {
        InMemoryResultSaver saver = new InMemoryResultSaver();
        BaseDecompiler decompiler = new BaseDecompiler(
                saver,
                toVineflowerOptions(buildCommonOptions(trackBytecodeLoc)),
                IFernflowerLogger.NO_OP
        );
        try {
            decompiler.addSource(classPath.toFile());
            addProjectLibraries(decompiler, classPath);
            decompiler.decompileContext();
            if (saver.content == null || saver.content.isBlank()) {
                return null;
            }
            return new VineflowerSingleResult(normalizeLineEndings(saver.content), saver.mapping);
        } finally {
            saver.close();
        }
    }

    private static void decompileJarWithVineflower(Path jarPath, Path srcDir) throws IOException {
        DirectoryResultSaver saver = new DirectoryResultSaver(srcDir.toFile());
        BaseDecompiler decompiler = new BaseDecompiler(
                saver,
                toVineflowerOptions(buildBatchOptions(srcDir)),
                IFernflowerLogger.NO_OP
        );
        decompiler.addSource(jarPath.toFile());
        addProjectLibraries(decompiler, jarPath);
        decompiler.decompileContext();
    }

    private static void addProjectLibraries(BaseDecompiler decompiler, Path sourcePath) {
        if (decompiler == null) {
            return;
        }
        Set<Path> seen = new LinkedHashSet<>();
        Path normalizedSource = safeNormalize(sourcePath);
        for (Path entry : ClasspathRegistry.getDecompilerClasspathEntries()) {
            Path normalized = safeNormalize(entry);
            if (normalized == null || !Files.exists(normalized)) {
                continue;
            }
            if (normalizedSource != null && normalized.equals(normalizedSource)) {
                continue;
            }
            if (!seen.add(normalized)) {
                continue;
            }
            try {
                decompiler.addLibrary(normalized.toFile());
            } catch (Throwable ex) {
                logger.debug("add vineflower library failed: {}: {}", normalized, ex.toString());
            }
        }
    }

    private static Path safeNormalize(Path path) {
        if (path == null) {
            return null;
        }
        try {
            return path.toAbsolutePath().normalize();
        } catch (Exception ex) {
            return null;
        }
    }

    private static Map<String, Object> toVineflowerOptions(Map<String, String> options) {
        Map<String, Object> result = new HashMap<>();
        if (options == null || options.isEmpty()) {
            return result;
        }
        result.putAll(options);
        return result;
    }

    private static String normalizeLineEndings(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.replace("\r\n", "\n");
    }

    private static int countLines(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }

    private static List<LineMapping> buildLineMappings(Path classPath, int[] originalToDecompiled, int lineOffset) {
        if (originalToDecompiled == null || originalToDecompiled.length < 2) {
            return Collections.emptyList();
        }
        String className = readClassName(classPath);
        if (className == null || className.isBlank()) {
            return Collections.emptyList();
        }
        NavigableMap<Integer, Integer> originalToDecompiledMap = toOriginalToDecompiledMap(originalToDecompiled);
        if (originalToDecompiledMap.isEmpty()) {
            return Collections.emptyList();
        }
        List<MethodReference> methods = DatabaseManager.getMethodReferencesByClass(className);
        if (methods == null || methods.isEmpty()) {
            return Collections.emptyList();
        }
        List<LineMapping> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (MethodReference method : methods) {
            if (method == null || method.getLineNumber() <= 0) {
                continue;
            }
            Integer decompiledLine = findClosestDecompiledLine(originalToDecompiledMap, method.getLineNumber());
            if (decompiledLine == null || decompiledLine <= 0) {
                continue;
            }
            String methodName = method.getName();
            String methodDesc = method.getDesc();
            String key = safe(methodName) + "#" + safe(methodDesc) + "#" + String.valueOf(method.getJarId());
            if (!seen.add(key)) {
                continue;
            }
            NavigableMap<Integer, Integer> decompiledToSource = new TreeMap<>();
            decompiledToSource.put(decompiledLine + lineOffset, method.getLineNumber());
            result.add(new LineMapping(methodName, methodDesc, decompiledToSource));
        }
        return result;
    }

    private static NavigableMap<Integer, Integer> toOriginalToDecompiledMap(int[] mapping) {
        NavigableMap<Integer, Integer> out = new TreeMap<>();
        for (int i = 0; i + 1 < mapping.length; i += 2) {
            int originalLine = mapping[i];
            int decompiledLine = mapping[i + 1];
            if (originalLine <= 0 || decompiledLine <= 0) {
                continue;
            }
            out.putIfAbsent(originalLine, decompiledLine);
        }
        return out;
    }

    private static Integer findClosestDecompiledLine(NavigableMap<Integer, Integer> originalToDecompiled,
                                                     int sourceLine) {
        Integer exact = originalToDecompiled.get(sourceLine);
        if (exact != null) {
            return exact;
        }
        Map.Entry<Integer, Integer> floor = originalToDecompiled.floorEntry(sourceLine);
        Map.Entry<Integer, Integer> ceil = originalToDecompiled.ceilingEntry(sourceLine);
        if (floor == null && ceil == null) {
            return null;
        }
        if (floor == null) {
            return ceil.getValue();
        }
        if (ceil == null) {
            return floor.getValue();
        }
        int diffFloor = Math.abs(sourceLine - floor.getKey());
        int diffCeil = Math.abs(ceil.getKey() - sourceLine);
        return diffCeil < diffFloor ? ceil.getValue() : floor.getValue();
    }

    private static String readClassName(Path classPath) {
        if (classPath == null || !Files.exists(classPath)) {
            return null;
        }
        try (InputStream in = Files.newInputStream(classPath)) {
            ClassReader reader = new ClassReader(in);
            return reader.getClassName();
        } catch (Exception ex) {
            logger.debug("read class name failed: {}: {}", classPath, ex.toString());
            return null;
        }
    }

    private static void deleteDirectory(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        final int[] failures = {0};
        final IOException[] first = {null};
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            failures[0]++;
                            if (first[0] == null) {
                                first[0] = ex;
                            }
                        }
                    });
        } catch (IOException ex) {
            failures[0]++;
            if (first[0] == null) {
                first[0] = ex;
            }
        }
        if (failures[0] > 0) {
            logger.debug("delete directory failed: {}: failures={} first={}",
                    dir, failures[0], first[0] == null ? "" : first[0].toString());
        }
    }

    private static void copyResourcesFromJar(Path jarPath, Path outputDir) {
        if (jarPath == null || outputDir == null || !Files.exists(jarPath)) {
            return;
        }
        try (ZipFile zipFile = new ZipFile(jarPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            Path root = outputDir.toAbsolutePath().normalize();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name == null || name.isEmpty() || name.endsWith(".class")) {
                    continue;
                }
                Path target = root.resolve(name).toAbsolutePath().normalize();
                if (!target.startsWith(root)) {
                    logger.warn("detect zip slip vulnerability: {}", name);
                    continue;
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                    continue;
                }
                if (Files.exists(target)) {
                    continue;
                }
                Path parent = target.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                try (InputStream inputStream = zipFile.getInputStream(entry);
                     OutputStream outputStream = Files.newOutputStream(target,
                             StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    IOUtil.copy(inputStream, outputStream);
                }
            }
        } catch (Exception ex) {
            logger.warn("copy jar resources failed: {}", ex.getMessage());
        }
    }

    private static void decompileNestedJars(Path resourcesRoot, Path libSrcRoot) {
        if (resourcesRoot == null || libSrcRoot == null) {
            return;
        }
        List<Path> nestedJars = collectNestedLibJars(resourcesRoot);
        if (nestedJars.isEmpty()) {
            return;
        }
        Map<Path, List<String>> grouped = new LinkedHashMap<>();
        for (Path jar : nestedJars) {
            if (jar == null) {
                continue;
            }
            Path parent = jar.getParent();
            if (parent == null) {
                continue;
            }
            Path relParent;
            try {
                relParent = resourcesRoot.relativize(parent);
            } catch (Exception ex) {
                relParent = Paths.get(".");
            }
            Path outBase = libSrcRoot.resolve(relParent);
            grouped.computeIfAbsent(outBase, k -> new ArrayList<>())
                    .add(jar.toAbsolutePath().toString());
        }
        for (Map.Entry<Path, List<String>> entry : grouped.entrySet()) {
            Path outBase = entry.getKey();
            List<String> jars = entry.getValue();
            if (jars == null || jars.isEmpty()) {
                continue;
            }
            try {
                Files.createDirectories(outBase);
            } catch (Exception ex) {
                logger.debug("create nested lib output dir failed: {}: {}", outBase, ex.toString());
            }
            decompileJars(jars, outBase.toString(), false);
        }
    }

    private static List<Path> collectNestedLibJars(Path resourcesRoot) {
        List<Path> jars = new ArrayList<>();
        Path root = resourcesRoot.toAbsolutePath().normalize();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.forEach(path -> {
                if (path == null || !Files.isRegularFile(path)) {
                    return;
                }
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                if (!name.endsWith(".jar")) {
                    return;
                }
                String rel = root.relativize(path).toString().replace("\\", "/");
                if (isNestedLibPath(rel)) {
                    jars.add(path);
                }
            });
        } catch (Exception ex) {
            logger.warn("collect nested jars failed: {}", ex.getMessage());
        }
        return jars;
    }

    private static boolean isNestedLibPath(String relativePath) {
        if (relativePath == null) {
            return false;
        }
        String rel = relativePath.replace("\\", "/");
        if (rel.startsWith("BOOT-INF/lib/")) {
            return true;
        }
        if (rel.startsWith("WEB-INF/lib/")) {
            return true;
        }
        return rel.startsWith("lib/");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class VineflowerSingleResult {
        private final String code;
        private final int[] mapping;

        private VineflowerSingleResult(String code, int[] mapping) {
            this.code = code;
            this.mapping = mapping;
        }
    }

    private static final class InMemoryResultSaver implements IResultSaver {
        private String content;
        private int[] mapping;

        @Override
        public void saveFolder(String path) {
        }

        @Override
        public void copyFile(String source, String path, String entryName) {
        }

        @Override
        public void saveClassFile(String path,
                                  String qualifiedName,
                                  String entryName,
                                  String content,
                                  int[] mapping) {
            this.content = content;
            this.mapping = mapping;
        }

        @Override
        public void createArchive(String path, String archiveName, Manifest manifest) {
        }

        @Override
        public void saveDirEntry(String path, String archiveName, String entryName) {
        }

        @Override
        public void copyEntry(String source, String path, String archiveName, String entry) {
        }

        @Override
        public void saveClassEntry(String path,
                                   String archiveName,
                                   String qualifiedName,
                                   String entryName,
                                   String content) {
            this.content = content;
        }

        @Override
        public void saveClassEntry(String path,
                                   String archiveName,
                                   String qualifiedName,
                                   String entryName,
                                   String content,
                                   int[] mapping) {
            this.content = content;
            this.mapping = mapping;
        }

        @Override
        public void closeArchive(String path, String archiveName) {
        }
    }

    public static final class DecompileResult {
        private final String code;
        private final List<LineMapping> lineMappings;

        private DecompileResult(String code, List<LineMapping> lineMappings) {
            this.code = code;
            this.lineMappings = lineMappings;
        }

        public String getCode() {
            return code;
        }

        public List<LineMapping> getLineMappings() {
            return lineMappings;
        }
    }

    public static final class LineMapping {
        private final String methodName;
        private final String methodDesc;
        private final NavigableMap<Integer, Integer> decompiledToSource;

        private LineMapping(String methodName,
                            String methodDesc,
                            NavigableMap<Integer, Integer> decompiledToSource) {
            this.methodName = methodName;
            this.methodDesc = methodDesc;
            this.decompiledToSource = decompiledToSource;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getMethodDesc() {
            return methodDesc;
        }

        public NavigableMap<Integer, Integer> getDecompiledToSource() {
            return decompiledToSource;
        }
    }
}
