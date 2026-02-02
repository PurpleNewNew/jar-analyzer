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

import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.util.LogUtil;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;
import me.n1ar4.jar.analyzer.utils.ClasspathRegistry;
import me.n1ar4.jar.analyzer.utils.IOUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.decompiler.BaseDecompiler;
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.jetbrains.java.decompiler.util.InterpreterUtil;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static javax.swing.JOptionPane.ERROR_MESSAGE;

/**
 * Decompile Engine
 */
public class DecompileEngine {
    public static final String INFO = "<html>" +
            "<b>FernFlower</b> - A great plugin from <b>JetBrains intellij-community</b>" +
            "</html>";
    private static final Logger logger = LogManager.getLogger();
    private static final String JAVA_FILE = ".java";
    private static final String EXPORT_SRC_DIR = "src";
    private static final String EXPORT_RES_DIR = "resources";
    private static final String EXPORT_LIB_SRC_DIR = "lib-src";
    private static final String EXPORT_TMP_DIR = ".tmp-ff";
    private static final String FERN_PREFIX = "//\n" +
            "// Jar Analyzer by 4ra1n\n" +
            "// (powered by FernFlower decompiler)\n" +
            "//\n";
    private static volatile int cacheCapacity = DecompileCacheConfig.resolveCapacity();
    private static LRUCache lruCache = new LRUCache(cacheCapacity);
    private static final Map<String, List<FernLineMapping>> lineMappingCache = new ConcurrentHashMap<>();

    public static String getFERN_PREFIX() {
        return FERN_PREFIX;
    }

    public static void cleanCache() {
        lruCache = new LRUCache(cacheCapacity);
        lineMappingCache.clear();
    }

    public static int getCacheCapacity() {
        return cacheCapacity;
    }

    public static void setCacheCapacity(int capacity) {
        int normalized = DecompileCacheConfig.normalize(capacity, cacheCapacity);
        if (normalized == cacheCapacity) {
            return;
        }
        cacheCapacity = normalized;
        lruCache = new LRUCache(cacheCapacity);
        lineMappingCache.clear();
    }

    public static void setCacheCapacity(String capacity) {
        Integer parsed = DecompileCacheConfig.parseOptional(capacity);
        if (parsed == null) {
            return;
        }
        setCacheCapacity(parsed);
    }

    public static boolean decompileJars(List<String> jarsPath, String outputDir) {
        return decompileJars(jarsPath, outputDir, false);
    }

    public static boolean decompileJars(List<String> jarsPath,
                                        String outputDir,
                                        boolean decompileNested) {
        for (String jarPath : jarsPath) {
            // 2024/08/21
            // 对于非 JAR 文件不进行处理（仅支持 JAR 文件）
            if (!jarPath.toLowerCase().endsWith(".jar")) {
                    UiExecutor.showMessage(MainForm.getInstance().getMasterPanel(),
                        "<html>" +
                                "<p>ONLY SUPPORT <strong>JAR</strong> FILE</p>" +
                                "<p>只支持 JAR 文件（其他类型的文件可以手动压缩成 JAR 后尝试）</p>" +
                                "</html>");
                return false;
            }

            Path jarPathPath = Paths.get(jarPath);
            String jarName = jarPathPath.getFileName().toString();
            String baseName = jarName.replaceAll("(?i)\\.jar$", "");
            Path outBase = Paths.get(outputDir);
            Path exportRoot = outBase.resolve(baseName);
            Path srcDir = exportRoot.resolve(EXPORT_SRC_DIR);
            Path resDir = exportRoot.resolve(EXPORT_RES_DIR);
            Path libSrcDir = exportRoot.resolve(EXPORT_LIB_SRC_DIR);
            Path tmpDir = exportRoot.resolve(EXPORT_TMP_DIR);

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

            logger.info("decompile jar: " + jarPath);
            LogUtil.info("decompile jar: " + jarPath);
            logger.info("output dir: " + exportRoot.toAbsolutePath());

            List<String> cmd = new ArrayList<>();
            cmd.add(jarPathPath.toAbsolutePath().toString());
            cmd.add(tmpDir.toAbsolutePath().toString());

            try {
                Files.createDirectories(tmpDir);
            } catch (Exception ignored) {
            }

            // FERN FLOWER API
            ConsoleDecompiler.main(cmd.toArray(new String[0]));

            // extract fernflower output zip into src
            try {
                Path zipPath = resolveFernflowerZip(tmpDir, jarName, baseName);
                if (zipPath != null && Files.exists(zipPath)) {
                    if (extractZip(zipPath, srcDir)) {
                        try {
                            Files.deleteIfExists(zipPath);
                        } catch (Exception ignored) {
                        }
                    }
                } else {
                    logger.warn("fernflower output zip not found: {}", tmpDir);
                }
            } catch (Exception ex) {
                logger.warn("extract fernflower output failed: {}", ex.getMessage());
            } finally {
                deleteDirectory(tmpDir);
            }

            // copy non-class resources into resources dir
            try {
                copyResourcesFromJar(jarPathPath, resDir);
            } catch (Exception ex) {
                logger.warn("copy jar resources failed: {}", ex.getMessage());
            }

            if (decompileNested) {
                try {
                    Files.createDirectories(libSrcDir);
                    decompileNestedJars(resDir, libSrcDir, DecompileType.FERNFLOWER);
                } catch (Exception ex) {
                    logger.warn("decompile nested jars failed: {}", ex.getMessage());
                }
            }
        }
        return true;
    }

    /**
     * Decompile Any Class
     *
     * @param classFilePath Class File Path
     * @return Java Source Code
     */
    public static String decompile(Path classFilePath) {
        if (classFilePath == null) {
            return null;
        }
        try {
            // USE LRU CACHE
            String key = classFilePath.toAbsolutePath().toString();
            String data = lruCache.get(key);
            if (data != null && !data.isEmpty()) {
                logger.debug("use cache");
                return data;
            }
            if (!Files.exists(classFilePath)) {
                return null;
            }
            String fileName = classFilePath.getFileName().toString();

            if (!fileName.endsWith(".class")) {
                UiExecutor.showMessage(MainForm.getInstance().getMasterPanel(),
                        "<html>" +
                                "<p>你选择的目标不是 class 文件，无法反编译</p>" +
                                "<p>文件名：" + fileName + "</p>" +
                                "</html>",
                        "Jar Analyzer V2 Error", ERROR_MESSAGE);
                return null;
            }

            // RESOLVE $ CLASS
            Path classDirPath = classFilePath.getParent();

            // BUG FIX 2025/02/27
            // 临时目录不存在时，避免继续反编译造成异常
            if (classDirPath == null || !Files.exists(classDirPath)) {
                UiExecutor.showMessage(MainForm.getInstance().getMasterPanel(),
                        "<html>" +
                                "<p>临时目录不存在，可能因为没有导出（请检查 jars in jar 选项和 add rt.jar 设置）</p>" +
                                "<p>你选择的目标不是 class 文件，无法反编译</p>" +
                        "</html>",
                        "Jar Analyzer V2 Error", ERROR_MESSAGE);
                return null;
            }

            String targetClassName = readClassInternalName(classFilePath);
            MemoryResultSaver saver = new MemoryResultSaver(targetClassName);
            BaseDecompiler decompiler = new BaseDecompiler(new FileBytecodeProvider(),
                    saver, new HashMap<>(), new PrintStreamLogger(System.out));
            decompiler.addSource(classFilePath.toFile());

            List<String> extraClassList = collectInnerClassFiles(classFilePath, classDirPath);
            for (String extra : extraClassList) {
                decompiler.addSource(Paths.get(extra).toFile());
            }
            addLibraries(decompiler, classFilePath);

            LogUtil.info("decompile class: " + classFilePath.getFileName().toString());
            decompiler.decompileContext();
            String code = saver.getContent();
            if (code == null || code.trim().isEmpty()) {
                return null;
            }
            String codeStr = FERN_PREFIX + code;
            logger.debug("save cache");
            lruCache.put(key, codeStr);
            return codeStr;
        } catch (Throwable t) {
            logger.warn("decompile fail: " + t.getMessage());
        }
        return null;
    }

    public static FernDecompileResult decompileWithLineMapping(Path classFilePath) {
        if (classFilePath == null) {
            return null;
        }
        try {
            String key = classFilePath.toAbsolutePath().toString();
            String cached = lruCache.get(key);
            List<FernLineMapping> cachedMappings = lineMappingCache.get(key);
            if (cached != null && cachedMappings != null) {
                logger.debug("use cache");
                return new FernDecompileResult(cached, cachedMappings);
            }
            if (!Files.exists(classFilePath)) {
                logger.warn("class file not exists: " + classFilePath);
                return null;
            }
            String targetClassName = readClassInternalName(classFilePath);
            MemoryResultSaver saver = new MemoryResultSaver(targetClassName);
            Map<String, Object> options = new HashMap<>();
            options.put(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, "1");
            BaseDecompiler decompiler = new BaseDecompiler(new FileBytecodeProvider(),
                    saver, options, new PrintStreamLogger(System.out));
            Path classDirPath = classFilePath.getParent();
            if (classDirPath == null || !Files.exists(classDirPath)) {
                return null;
            }
            decompiler.addSource(classFilePath.toFile());
            List<String> extraClassList = collectInnerClassFiles(classFilePath, classDirPath);
            for (String extra : extraClassList) {
                decompiler.addSource(Paths.get(extra).toFile());
            }
            addLibraries(decompiler, classFilePath);
            decompiler.decompileContext();
            String code = saver.getContent();
            if (code == null || code.trim().isEmpty()) {
                return null;
            }
            String codeStr = FERN_PREFIX + code;
            int prefixLines = countLines(FERN_PREFIX);
            List<FernLineMapping> lineMappings =
                    buildLineMappings(saver.getMapping(), saver.getMappingByMethod(), prefixLines);
            lruCache.put(key, codeStr);
            lineMappingCache.put(key, lineMappings);
            return new FernDecompileResult(codeStr, lineMappings);
        } catch (Throwable t) {
            logger.warn("decompile fail: " + t.getMessage());
            return null;
        }
    }

    private static void addLibraries(BaseDecompiler decompiler, Path classFilePath) {
        if (decompiler == null) {
            return;
        }
        Set<Path> libraries = new LinkedHashSet<>();
        Path classRoot = resolveClassRoot(classFilePath);
        if (classRoot != null) {
            libraries.add(classRoot);
        }
        List<Path> archives = ClasspathRegistry.getClasspathEntriesForFernflower();
        if (archives != null && !archives.isEmpty()) {
            libraries.addAll(archives);
        }
        if (libraries.isEmpty()) {
            return;
        }
        Path sourcePath = null;
        try {
            if (classFilePath != null) {
                sourcePath = classFilePath.toAbsolutePath().normalize();
            }
        } catch (Exception ignored) {
        }
        for (Path lib : libraries) {
            if (lib == null) {
                continue;
            }
            Path candidate;
            try {
                candidate = lib.toAbsolutePath().normalize();
            } catch (Exception ex) {
                continue;
            }
            if (!Files.exists(candidate)) {
                continue;
            }
            if (sourcePath != null && sourcePath.equals(candidate)) {
                continue;
            }
            if (candidate.toString().toLowerCase(Locale.ROOT).endsWith(".class")) {
                continue;
            }
            if (Files.isDirectory(candidate) && !looksLikeClassRoot(candidate)) {
                continue;
            }
            try {
                decompiler.addLibrary(candidate.toFile());
            } catch (Throwable ignored) {
            }
        }
    }

    private static Path resolveClassRoot(Path classFilePath) {
        if (classFilePath == null || !Files.exists(classFilePath)) {
            return null;
        }
        String internal = readClassInternalName(classFilePath);
        if (internal == null || internal.isEmpty()) {
            return null;
        }
        int segments = internal.split("/").length - 1;
        Path root = classFilePath.toAbsolutePath().normalize().getParent();
        for (int i = 0; i < segments && root != null; i++) {
            root = root.getParent();
        }
        return root;
    }

    private static boolean looksLikeClassRoot(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return false;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path child : stream) {
                if (Files.isDirectory(child)) {
                    String name = child.getFileName().toString().toLowerCase(Locale.ROOT);
                    if ("com".equals(name) || "org".equals(name) || "net".equals(name) || "io".equals(name)
                            || "me".equals(name) || "cn".equals(name) || "edu".equals(name) || "gov".equals(name)
                            || "java".equals(name) || "javax".equals(name) || "jakarta".equals(name)
                            || "sun".equals(name) || "jdk".equals(name) || "android".equals(name)
                            || "androidx".equals(name) || "kotlin".equals(name) || "scala".equals(name)) {
                        return true;
                    }
                } else if (child.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".class")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static Path findDecompiledFile(Path outputDir, String baseName) throws IOException {
        if (outputDir == null || !Files.exists(outputDir)) {
            return null;
        }
        Path direct = outputDir.resolve(baseName + JAVA_FILE);
        if (Files.exists(direct)) {
            return direct;
        }
        try (Stream<Path> stream = Files.walk(outputDir)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(JAVA_FILE))
                .findFirst()
                .orElse(null);
        }
    }

    static void deleteDirectory(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
        } catch (IOException ignored) {
        }
    }

    private static Path resolveFernflowerZip(Path tmpDir, String jarName, String baseName) {
        if (tmpDir == null) {
            return null;
        }
        if (jarName != null && !jarName.isEmpty()) {
            Path raw = tmpDir.resolve(jarName);
            if (Files.exists(raw)) {
            String rawName = raw.getFileName().toString().toLowerCase(Locale.ROOT);
            if (rawName.endsWith(".zip") || rawName.endsWith(".jar")) {
                return raw;
            }
            Path zip = tmpDir.resolve(baseName + ".zip");
            try {
                Files.move(raw, zip, StandardCopyOption.REPLACE_EXISTING);
                return zip;
            } catch (Exception ignored) {
                return raw;
            }
            }
        }
        Path zip = tmpDir.resolve(baseName + ".zip");
        if (Files.exists(zip)) {
            return zip;
        }
        Path jar = tmpDir.resolve(baseName + ".jar");
        if (Files.exists(jar)) {
            return jar;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tmpDir, "*.zip")) {
            for (Path entry : stream) {
            return entry;
            }
        } catch (Exception ignored) {
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tmpDir, "*.jar")) {
            for (Path entry : stream) {
            return entry;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static boolean extractZip(Path zipPath, Path outputDir) {
        if (zipPath == null || outputDir == null) {
            return false;
        }
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            Path root = outputDir.toAbsolutePath().normalize();
            while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name == null || name.isEmpty()) {
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
            return true;
        } catch (Exception ex) {
            logger.warn("extract zip failed: {}", ex.getMessage());
            return false;
        }
    }

    static void copyResourcesFromJar(Path jarPath, Path outputDir) {
        if (jarPath == null || outputDir == null) {
            return;
        }
        if (!Files.exists(jarPath)) {
            return;
        }
        try (ZipFile zipFile = new ZipFile(jarPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            Path root = outputDir.toAbsolutePath().normalize();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name == null || name.isEmpty()) {
                    continue;
                }
                if (name.endsWith(".class")) {
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

    static void decompileNestedJars(Path resourcesRoot, Path libSrcRoot, DecompileType type) {
        if (resourcesRoot == null || libSrcRoot == null || type == null) {
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
            } catch (Exception ignored) {
            }
            if (type == DecompileType.CFR) {
                CFRDecompileEngine.decompileJars(jars, outBase.toString(), false);
            } else {
                decompileJars(jars, outBase.toString(), false);
            }
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

    private static List<String> collectInnerClassFiles(Path classFilePath, Path classDirPath) throws IOException {
        List<String> extraClassList = new ArrayList<>();
        String classNamePrefix = classFilePath.getFileName().toString();
        classNamePrefix = classNamePrefix.split("\\.")[0];
        String finalClassNamePrefix = classNamePrefix;
        Files.walkFileTree(classDirPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String fileName = file.getFileName().toString();
                if (fileName.startsWith(finalClassNamePrefix + "$")) {
                    extraClassList.add(file.toAbsolutePath().toString());
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return extraClassList;
    }

    private static String readClassInternalName(Path classFilePath) {
        if (classFilePath == null) {
            return null;
        }
        try {
            byte[] data = Files.readAllBytes(classFilePath);
            ClassReader reader = new ClassReader(data);
            return reader.getClassName();
        } catch (Throwable ignored) {
            return null;
        }
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

    private static List<FernLineMapping> buildLineMappings(int[] mapping,
                                                       Map<String, int[]> mappingByMethod,
                                                       int lineOffset) {
        int offset = Math.max(0, lineOffset);
        if (mappingByMethod != null && !mappingByMethod.isEmpty()) {
            return buildLineMappingsByMethod(mappingByMethod, offset);
        }
        if (mapping == null || mapping.length < 2) {
            return new ArrayList<>();
        }
        NavigableMap<Integer, Integer> decompiledToSource = new TreeMap<>();
        for (int i = 0; i + 1 < mapping.length; i += 2) {
            int original = mapping[i];
            int decompiled = mapping[i + 1];
            if (original <= 0 || decompiled <= 0) {
            continue;
            }
            int adjusted = decompiled + offset;
            decompiledToSource.putIfAbsent(adjusted, original);
        }
        if (decompiledToSource.isEmpty()) {
            return new ArrayList<>();
        }
        List<FernLineMapping> out = new ArrayList<>();
        out.add(new FernLineMapping(null, null, decompiledToSource));
        return out;
    }

    private static List<FernLineMapping> buildLineMappingsByMethod(Map<String, int[]> mappingByMethod,
                                                               int lineOffset) {
        List<FernLineMapping> out = new ArrayList<>();
        int offset = Math.max(0, lineOffset);
        for (Map.Entry<String, int[]> entry : mappingByMethod.entrySet()) {
            String methodKey = entry.getKey();
            int[] mapping = entry.getValue();
            if (mapping == null || mapping.length < 2) {
            continue;
            }
            String methodName = null;
            String methodDesc = null;
            if (methodKey != null) {
            int idx = methodKey.indexOf(' ');
            if (idx > 0) {
                methodName = methodKey.substring(0, idx);
                methodDesc = methodKey.substring(idx + 1);
            } else {
                methodName = methodKey;
            }
            }
            NavigableMap<Integer, Integer> decompiledToSource = new TreeMap<>();
            for (int i = 0; i + 1 < mapping.length; i += 2) {
            int original = mapping[i];
            int decompiled = mapping[i + 1];
            if (original <= 0 || decompiled <= 0) {
                continue;
            }
            int adjusted = decompiled + offset;
            decompiledToSource.putIfAbsent(adjusted, original);
            }
            if (!decompiledToSource.isEmpty()) {
            out.add(new FernLineMapping(methodName, methodDesc, decompiledToSource));
            }
        }
        return out;
    }

    private static final class MemoryResultSaver implements IResultSaver {
        private final String targetClassName;
        private String content;
        private int[] mapping;
        private Map<String, int[]> mappingByMethod;

        private MemoryResultSaver(String targetClassName) {
            this.targetClassName = targetClassName;
        }

        public String getContent() {
            return content;
        }

        public int[] getMapping() {
            return mapping;
        }

        public Map<String, int[]> getMappingByMethod() {
            return mappingByMethod;
        }

        @Override
        public void saveFolder(String path) {
        }

        @Override
        public void copyFile(String source, String path, String entryName) {
        }

        @Override
        public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
            if (content == null) {
            return;
            }
            if (targetClassName != null && qualifiedName != null && !targetClassName.equals(qualifiedName)) {
            return;
            }
            if (this.content == null) {
            this.content = content;
            this.mapping = mapping;
            try {
                mappingByMethod = DecompilerContext.getBytecodeSourceMapper()
                        .getOriginalLinesMappingByMethod(qualifiedName);
            } catch (Throwable ignored) {
                mappingByMethod = null;
            }
            }
        }

        @Override
        public void createArchive(String path, String archiveName, java.util.jar.Manifest manifest) {
        }

        @Override
        public void saveDirEntry(String path, String archiveName, String entryName) {
        }

        @Override
        public void copyEntry(String source, String path, String archiveName, String entry) {
        }

        @Override
        public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
        }

        @Override
        public void closeArchive(String path, String archiveName) {
        }
    }

    private static final class FileBytecodeProvider implements IBytecodeProvider {
        @Override
        public byte[] getBytecode(String externalPath, String internalPath) throws IOException {
            if (externalPath == null) {
            return new byte[0];
            }
            if (internalPath == null) {
            return InterpreterUtil.getBytes(Paths.get(externalPath).toFile());
            }
            try (ZipFile archive = new ZipFile(externalPath)) {
            ZipEntry entry = archive.getEntry(internalPath);
            if (entry == null) {
                return new byte[0];
            }
            return InterpreterUtil.getBytes(archive, entry);
            }
        }
    }

    public static final class FernDecompileResult {
        private final String code;
        private final List<FernLineMapping> lineMappings;

        private FernDecompileResult(String code, List<FernLineMapping> lineMappings) {
            this.code = code;
            this.lineMappings = lineMappings;
        }

        public String getCode() {
            return code;
        }

        public List<FernLineMapping> getLineMappings() {
            return lineMappings;
        }
    }

    public static final class FernLineMapping {
        private final String methodName;
        private final String methodDesc;
        private final NavigableMap<Integer, Integer> decompiledToSource;

        private FernLineMapping(String methodName,
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
