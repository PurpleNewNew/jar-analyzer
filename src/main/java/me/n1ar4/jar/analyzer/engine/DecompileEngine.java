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
import me.n1ar4.jar.analyzer.starter.Const;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
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
    private static final String JAVA_DIR = "jar-analyzer-decompile";
    private static final String JAVA_FILE = ".java";
    private static final String FERN_PREFIX = "//\n" +
            "// Jar Analyzer by 4ra1n\n" +
            "// (powered by FernFlower decompiler)\n" +
            "//\n";
    private static volatile int cacheCapacity = DecompileCacheConfig.resolveCapacity();
    private static LRUCache lruCache = new LRUCache(cacheCapacity);
    private static final Map<String, List<FernLineMapping>> lineMappingCache = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> FORCE_FERN = new ThreadLocal<>();

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

            List<String> cmd = new ArrayList<>();
            Path jarPathPath = Paths.get(jarPath);
            cmd.add(jarPathPath.toAbsolutePath().toString());
            Path path = Paths.get(outputDir);
            try {
                Files.createDirectories(path);
            } catch (Exception ignored) {
            }
            cmd.add(path.toAbsolutePath().toString());

            logger.info("decompile jar: " + jarPath);
            LogUtil.info("decompile jar: " + jarPath);
            logger.info("output dir: " + outputDir);

            // FERN FLOWER API
            ConsoleDecompiler.main(cmd.toArray(new String[0]));

            // HACK NAME
            String jarName = jarPathPath.getFileName().toString();
            String zipName = jarName.replaceAll("\\.jar$", ".zip");
            Path oldPath = path.toAbsolutePath().resolve(jarName);
            Path newPath = path.toAbsolutePath().resolve(zipName);

            try {
                Files.move(oldPath, newPath);
                System.out.println("file renamed to: " + newPath);
            } catch (Exception ignored) {
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
        try {
            boolean fern = isFernSelected();
            if (fern) {
                // USE LRU CACHE
                String key = classFilePath.toAbsolutePath().toString();
                String data = lruCache.get(key);
                if (data != null && !data.isEmpty()) {
                    logger.debug("use cache");
                    return data;
                }
                Path dirPath = Paths.get(Const.tempDir);
                Path deDirPath = dirPath.resolve(Paths.get(JAVA_DIR));
                if (!Files.exists(deDirPath)) {
                    Files.createDirectory(deDirPath);
                }
                Path outputDir = null;
                try {
                    outputDir = Files.createTempDirectory(deDirPath, "ff-");
                    String javaDir = outputDir.toAbsolutePath().toString();
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

                    String baseName = fileName.substring(0, fileName.length() - ".class".length());

                    // RESOLVE $ CLASS
                    Path classDirPath = classFilePath.getParent();

                    // BUG FIX 2025/02/27
                    // 临时目录不存在时，避免继续反编译造成异常
                    if (!Files.exists(classDirPath)) {
                        UiExecutor.showMessage(MainForm.getInstance().getMasterPanel(),
                                "<html>" +
                                        "<p>临时目录不存在，可能因为没有导出（请检查 jars in jar 选项和 add rt.jar 设置）</p>" +
                                        "<p>你选择的目标不是 class 文件，无法反编译</p>" +
                                "</html>",
                                "Jar Analyzer V2 Error", ERROR_MESSAGE);
                        return null;
                    }

                    List<String> extraClassList = collectInnerClassFiles(classFilePath, classDirPath);

                    List<String> cmd = new ArrayList<>();
                    cmd.add(classFilePath.toAbsolutePath().toString());
                    cmd.addAll(extraClassList);
                    cmd.add(javaDir);

                    LogUtil.info("decompile class: " + classFilePath.getFileName().toString());

                    try {
                        // FERN FLOWER API
                        ConsoleDecompiler.main(cmd.toArray(new String[0]));
                    } catch (Throwable t) {
                        // 反编译异常通常不影响主流程
                        // 记录日志后继续
                        logger.warn("fern flower fail: " + t.getMessage());
                    }

                    Path javaFilePath = findDecompiledFile(outputDir, baseName);
                    if (javaFilePath != null && Files.exists(javaFilePath)) {
                        byte[] code = Files.readAllBytes(javaFilePath);
                        String codeStr = new String(code, StandardCharsets.UTF_8);
                        codeStr = FERN_PREFIX + codeStr;
                        logger.debug("save cache");
                        lruCache.put(key, codeStr);
                        return codeStr;
                    }
                    return null;
                } finally {
                    deleteDirectory(outputDir);
                }
            } else {
                LogUtil.warn("unknown error");
                return null;
            }
        } catch (Exception ex) {
            logger.warn("decompile fail: " + ex.getMessage());
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

    public static String decompile(Path classFilePath, boolean forceFern) {
        if (!forceFern) {
            return decompile(classFilePath);
        }
        Boolean prev = FORCE_FERN.get();
        FORCE_FERN.set(Boolean.TRUE);
        try {
            return decompile(classFilePath);
        } finally {
            if (prev == null) {
                FORCE_FERN.remove();
            } else {
                FORCE_FERN.set(prev);
            }
        }
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

    private static void deleteDirectory(Path dir) {
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

    private static boolean isFernSelected() {
        Boolean forced = FORCE_FERN.get();
        if (forced != null) {
            return forced;
        }
        try {
            MainForm instance = MainForm.getInstance();
            if (instance == null || instance.getFernRadio() == null) {
                return true;
            }
            return instance.getFernRadio().isSelected();
        } catch (Throwable t) {
            return true;
        }
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
