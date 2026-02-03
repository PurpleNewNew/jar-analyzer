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
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CFR Decompile Engine
 */
public class CFRDecompileEngine {
    public static final String INFO = "<html>" +
            "<b>CFR</b> - Another Java decompiler" +
            "</html>";
    private static final Logger logger = LogManager.getLogger();
    private static final String CFR_PREFIX = "//\n" +
            "// Jar Analyzer by 4ra1n\n" +
            "// (powered by CFR decompiler)\n" +
            "//\n";
    private static volatile int cacheCapacity = DecompileCacheConfig.resolveCapacity();
    private static LRUCache lruCache = new LRUCache(cacheCapacity);
    private static final Map<String, List<CfrLineMapping>> lineMappingCache = new ConcurrentHashMap<>();
    private static final JarAnalyzerClassFileSource CLASS_SOURCE = new JarAnalyzerClassFileSource();

    /**
     * 使用CFR反编译指定的class文件
     *
     * @param classFilePath class文件的绝对路径
     * @return 反编译后的Java源代码
     */
    public static String decompile(String classFilePath) {
        if (classFilePath == null || classFilePath.trim().isEmpty()) {
            logger.warn("class file path is null or empty");
            return null;
        }
        if (DatabaseManager.isBuilding()) {
            logger.info("decompile blocked during build");
            try {
                UiExecutor.showMessage(MainForm.getInstance().getMasterPanel(),
                        "<html>" +
                                "<p>Build is running, index not ready.</p>" +
                                "<p>构建中索引未完成，已禁止反编译。</p>" +
                                "</html>");
            } catch (Throwable ignored) {
            }
            return null;
        }

        // 检查缓存
        String key = "cfr-" + classFilePath;
        String cached = lruCache.get(key);
        if (cached != null) {
            logger.debug("get from cache: " + classFilePath);
            return cached;
        }

        Path classPath;
        try {
            classPath = Paths.get(classFilePath);
        } catch (Exception ex) {
            logger.warn("class file path invalid: " + ex.getMessage());
            return null;
        }
        if (!Files.exists(classPath)) {
            logger.warn("class file not exists: " + classFilePath);
            return null;
        }
        return DecompileLookupContext.withClassPath(classPath,
                () -> decompileInternal(classFilePath));
    }

    private static String decompileInternal(String classFilePath) {
        try {
            // CFR反编译选项
            Map<String, String> options = new HashMap<>();
            options.put("showversion", "false");
            options.put("hidelongstrings", "false");
            options.put("hideutf", "false");
            options.put("innerclasses", "true");
            options.put("skipbatchinnerclasses", "false");

            // 创建输出收集器
            StringBuilder decompiledCode = new StringBuilder();
            OutputSinkFactory outputSinkFactory = new OutputSinkFactory() {
                @Override
                public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> available) {
                    if (sinkType == SinkType.JAVA) {
                        return pickSinks(available,
                                SinkClass.DECOMPILED,
                                SinkClass.DECOMPILED_MULTIVER,
                                SinkClass.STRING);
                    }
                    return pickSinks(available, SinkClass.STRING, SinkClass.EXCEPTION_MESSAGE);
                }

                @Override
                public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                    if (sinkType == SinkType.JAVA) {
                        if (sinkClass == SinkClass.DECOMPILED || sinkClass == SinkClass.DECOMPILED_MULTIVER) {
                            return (T obj) -> {
                                SinkReturns.Decompiled decompiled = (SinkReturns.Decompiled) obj;
                                decompiledCode.append(decompiled.getJava());
                            };
                        } else if (sinkClass == SinkClass.STRING) {
                            return (T obj) -> decompiledCode.append(obj.toString());
                        }
                    }
                    return (T obj) -> {
                    };
                }
            };

            // 执行CFR反编译
            CfrDriver driver = new CfrDriver.Builder()
                    .withOptions(options)
                    .withClassFileSource(CLASS_SOURCE)
                    .withOutputSink(outputSinkFactory)
                    .build();

            List<String> toAnalyse = Collections.singletonList(classFilePath);
            driver.analyse(toAnalyse);

            String result = decompiledCode.toString();
            if (!result.trim().isEmpty()) {
                // 添加前缀
                result = CFR_PREFIX + result;
                // 保存到缓存
                String key = "cfr-" + classFilePath;
                lruCache.put(key, result);
                logger.debug("cfr decompile success: " + classFilePath);
                return result;
            } else {
                logger.warn("cfr decompile result is empty: " + classFilePath);
                return null;
            }
        } catch (Exception ex) {
            logger.warn("cfr decompile fail: " + ex.getMessage());
            return null;
        }
    }

    public static CfrDecompileResult decompileWithLineMapping(String classFilePath) {
        if (classFilePath == null || classFilePath.trim().isEmpty()) {
            logger.warn("class file path is null or empty");
            return null;
        }
        if (DatabaseManager.isBuilding()) {
            logger.info("decompile blocked during build");
            try {
                UiExecutor.showMessage(MainForm.getInstance().getMasterPanel(),
                        "<html>" +
                                "<p>Build is running, index not ready.</p>" +
                                "<p>构建中索引未完成，已禁止反编译。</p>" +
                                "</html>");
            } catch (Throwable ignored) {
            }
            return null;
        }
        String key = "cfr-" + classFilePath;
        String cached = lruCache.get(key);
        List<CfrLineMapping> cachedMappings = lineMappingCache.get(key);
        if (cached != null && cachedMappings != null) {
            logger.debug("get from cache: " + classFilePath);
            return new CfrDecompileResult(cached, cachedMappings);
        }
        Path classPath;
        try {
            classPath = Paths.get(classFilePath);
        } catch (Exception ex) {
            logger.warn("class file path invalid: " + ex.getMessage());
            return null;
        }
        if (!Files.exists(classPath)) {
            logger.warn("class file not exists: " + classFilePath);
            return null;
        }
        return DecompileLookupContext.withClassPath(classPath,
                () -> decompileWithLineMappingInternal(classFilePath));
    }

    private static CfrDecompileResult decompileWithLineMappingInternal(String classFilePath) {
        try {
            Map<String, String> options = new HashMap<>();
            options.put("showversion", "false");
            options.put("hidelongstrings", "false");
            options.put("hideutf", "false");
            options.put("innerclasses", "true");
            options.put("skipbatchinnerclasses", "false");
            options.put("trackbytecodeloc", "true");
            StringBuilder decompiledCode = new StringBuilder();
            List<SinkReturns.LineNumberMapping> lineMappings = new ArrayList<>();
            OutputSinkFactory outputSinkFactory = new OutputSinkFactory() {
                @Override
                public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> available) {
                    if (sinkType == SinkType.JAVA) {
                        return pickSinks(available,
                                SinkClass.DECOMPILED,
                                SinkClass.DECOMPILED_MULTIVER,
                                SinkClass.STRING);
                    }
                    if (sinkType == SinkType.LINENUMBER) {
                        return pickSinks(available, SinkClass.LINE_NUMBER_MAPPING, SinkClass.STRING);
                    }
                    return pickSinks(available, SinkClass.STRING, SinkClass.EXCEPTION_MESSAGE);
                }

                @Override
                public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                    if (sinkType == SinkType.JAVA) {
                        if (sinkClass == SinkClass.DECOMPILED || sinkClass == SinkClass.DECOMPILED_MULTIVER) {
                            return (T obj) -> {
                                SinkReturns.Decompiled decompiled = (SinkReturns.Decompiled) obj;
                                decompiledCode.append(decompiled.getJava());
                            };
                        } else if (sinkClass == SinkClass.STRING) {
                            return (T obj) -> decompiledCode.append(obj.toString());
                        }
                    }
                    if (sinkType == SinkType.LINENUMBER && sinkClass == SinkClass.LINE_NUMBER_MAPPING) {
                        return (T obj) -> lineMappings.add((SinkReturns.LineNumberMapping) obj);
                    }
                    return (T obj) -> {
                    };
                }
            };
            CfrDriver driver = new CfrDriver.Builder()
                    .withOptions(options)
                    .withClassFileSource(CLASS_SOURCE)
                    .withOutputSink(outputSinkFactory)
                    .build();
            driver.analyse(Collections.singletonList(classFilePath));
            String result = decompiledCode.toString();
            if (result.trim().isEmpty()) {
                logger.warn("cfr decompile result is empty: " + classFilePath);
                return null;
            }
            int prefixLines = countLines(CFR_PREFIX);
            List<CfrLineMapping> builtMappings = buildLineMappings(lineMappings, prefixLines);
            result = CFR_PREFIX + result;
            String key = "cfr-" + classFilePath;
            lruCache.put(key, result);
            if (builtMappings == null) {
                builtMappings = Collections.emptyList();
            }
            lineMappingCache.put(key, builtMappings);
            logger.debug("cfr decompile success: " + classFilePath);
            return new CfrDecompileResult(result, builtMappings);
        } catch (Exception ex) {
            logger.warn("cfr decompile fail: " + ex.getMessage());
            return null;
        }
    }

    public static String getCFR_PREFIX() {
        return CFR_PREFIX;
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
            if (!jarPath.toLowerCase().endsWith(".jar")) {
                UiExecutor.showMessage(MainForm.getInstance().getMasterPanel(),
                        "<html>" +
                                "<p>ONLY SUPPORT <strong>JAR</strong> FILE</p>" +
                                "<p>只支持 JAR 文件（其他类型的文件可以手动压缩成 JAR 后尝试）</p>" +
                                "</html>");
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
                    DecompileEngine.deleteDirectory(exportRoot);
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

            Map<String, String> options = new HashMap<>();
            options.put("showversion", "false");
            options.put("hidelongstrings", "false");
            options.put("hideutf", "false");
            options.put("innerclasses", "true");
            options.put("skipbatchinnerclasses", "false");
            options.put("outputdir", srcDir.toAbsolutePath().toString());
            options.put("clobber", "true");
            options.put("outputencoding", "UTF-8");
            options.put("silent", "true");

            try {
                CfrDriver driver = new CfrDriver.Builder()
                        .withOptions(options)
                        .build();
                driver.analyse(Collections.singletonList(jarPathPath.toAbsolutePath().toString()));
                DecompileEngine.copyResourcesFromJar(jarPathPath, resDir);
                if (decompileNested) {
                    Files.createDirectories(libSrcDir);
                    DecompileEngine.decompileNestedJars(resDir, libSrcDir, DecompileType.CFR);
                }
            } catch (Exception ex) {
                logger.warn("cfr decompile jar fail: " + ex.getMessage());
                return false;
            }
        }
        return true;
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

    private static List<CfrLineMapping> buildLineMappings(List<SinkReturns.LineNumberMapping> mappings, int lineOffset) {
        if (mappings == null || mappings.isEmpty()) {
            return Collections.emptyList();
        }
        List<CfrLineMapping> result = new ArrayList<>();
        for (SinkReturns.LineNumberMapping mapping : mappings) {
            if (mapping == null) {
                continue;
            }
            NavigableMap<Integer, Integer> offsetToDecompiled = mapping.getClassFileMappings();
            NavigableMap<Integer, Integer> offsetToSource = mapping.getMappings();
            if (offsetToDecompiled == null || offsetToDecompiled.isEmpty()
                    || offsetToSource == null || offsetToSource.isEmpty()) {
                continue;
            }
            NavigableMap<Integer, Integer> decompiledToSource = new TreeMap<>();
            for (Map.Entry<Integer, Integer> entry : offsetToDecompiled.entrySet()) {
                Integer offset = entry.getKey();
                Integer decompiledLine = entry.getValue();
                if (offset == null || decompiledLine == null) {
                    continue;
                }
                Integer sourceLine = offsetToSource.get(offset);
                if (sourceLine == null) {
                    Map.Entry<Integer, Integer> floor = offsetToSource.floorEntry(offset);
                    Map.Entry<Integer, Integer> ceil = offsetToSource.ceilingEntry(offset);
                    sourceLine = pickClosestLine(offset, floor, ceil);
                }
                if (sourceLine == null) {
                    continue;
                }
                int adjustedLine = decompiledLine + lineOffset;
                if (!decompiledToSource.containsKey(adjustedLine)) {
                    decompiledToSource.put(adjustedLine, sourceLine);
                }
            }
            if (!decompiledToSource.isEmpty()) {
                result.add(new CfrLineMapping(mapping.methodName(), mapping.methodDescriptor(), decompiledToSource));
            }
        }
        return result;
    }

    private static Integer pickClosestLine(int offset,
                                           Map.Entry<Integer, Integer> floor,
                                           Map.Entry<Integer, Integer> ceil) {
        if (floor == null && ceil == null) {
            return null;
        }
        if (floor == null) {
            return ceil.getValue();
        }
        if (ceil == null) {
            return floor.getValue();
        }
        int diffFloor = Math.abs(offset - floor.getKey());
        int diffCeil = Math.abs(ceil.getKey() - offset);
        return diffCeil < diffFloor ? ceil.getValue() : floor.getValue();
    }

    private static List<OutputSinkFactory.SinkClass> pickSinks(Collection<OutputSinkFactory.SinkClass> available,
                                                               OutputSinkFactory.SinkClass... preferred) {
        if (available == null || available.isEmpty()) {
            return Collections.singletonList(OutputSinkFactory.SinkClass.STRING);
        }
        List<OutputSinkFactory.SinkClass> sinks = new ArrayList<>();
        if (preferred != null) {
            for (OutputSinkFactory.SinkClass sink : preferred) {
                if (sink != null && available.contains(sink)) {
                    sinks.add(sink);
                }
            }
        }
        if (!sinks.isEmpty()) {
            return sinks;
        }
        return Collections.singletonList(available.iterator().next());
    }

    public static final class CfrDecompileResult {
        private final String code;
        private final List<CfrLineMapping> lineMappings;

        private CfrDecompileResult(String code, List<CfrLineMapping> lineMappings) {
            this.code = code;
            this.lineMappings = lineMappings;
        }

        public String getCode() {
            return code;
        }

        public List<CfrLineMapping> getLineMappings() {
            return lineMappings;
        }
    }

    public static final class CfrLineMapping {
        private final String methodName;
        private final String methodDesc;
        private final NavigableMap<Integer, Integer> decompiledToSource;

        private CfrLineMapping(String methodName,
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
