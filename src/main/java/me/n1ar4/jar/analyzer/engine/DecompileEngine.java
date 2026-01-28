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
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

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
    private static final ThreadLocal<Boolean> FORCE_FERN = new ThreadLocal<>();

    public static String getFERN_PREFIX() {
        return FERN_PREFIX;
    }

    public static void cleanCache() {
        lruCache = new LRUCache(cacheCapacity);
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
                    List<String> extraClassList = new ArrayList<>();
                    Path classDirPath = classFilePath.getParent();
                    String classNamePrefix = classFilePath.getFileName().toString();
                    classNamePrefix = classNamePrefix.split("\\.")[0];

                    String finalClassNamePrefix = classNamePrefix;

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
                        String codeStr = new String(code);
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
}
