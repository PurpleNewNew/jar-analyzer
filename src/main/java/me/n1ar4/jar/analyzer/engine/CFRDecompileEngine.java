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

import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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

        // 检查缓存
        String key = "cfr-" + classFilePath;
        String cached = lruCache.get(key);
        if (cached != null) {
            logger.debug("get from cache: " + classFilePath);
            return cached;
        }

        try {
            Path classPath = Paths.get(classFilePath);
            if (!Files.exists(classPath)) {
                logger.warn("class file not exists: " + classFilePath);
                return null;
            }

            // CFR反编译选项
            Map<String, String> options = new HashMap<>();
            options.put("showversion", "false");
            options.put("hidelongstrings", "false");
            options.put("hideutf", "false");
            options.put("innerclasses", "true");
            options.put("skipbatchinnerclasses", "false");

            // 创建输出收集器
            StringBuilder decompiledCode = new StringBuilder();
            Object outputSinkFactory = buildOutputSinkFactory(decompiledCode);
            if (outputSinkFactory == null) {
                logger.warn("init cfr output sink factory fail");
                return null;
            }

            // 执行CFR反编译
            runCfr(classFilePath, options, outputSinkFactory);

            String result = decompiledCode.toString();
            if (!result.trim().isEmpty()) {
                // 添加前缀
                result = CFR_PREFIX + result;
                // 保存到缓存
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

    private static Object buildOutputSinkFactory(StringBuilder decompiledCode) {
        try {
            ClassLoader cl = CFRDecompileEngine.class.getClassLoader();
            Class<?> outputSinkFactoryClass = Class.forName("org.benf.cfr.reader.api.OutputSinkFactory", false, cl);
            Class<?> sinkTypeClass = Class.forName("org.benf.cfr.reader.api.OutputSinkFactory$SinkType", false, cl);
            Class<?> sinkClassClass = Class.forName("org.benf.cfr.reader.api.OutputSinkFactory$SinkClass", false, cl);
            Class<?> sinkInterface = Class.forName("org.benf.cfr.reader.api.OutputSinkFactory$Sink", false, cl);

            final Object decompiledClass = enumValue(sinkClassClass, "DECOMPILED");
            final Object stringClass = enumValue(sinkClassClass, "STRING");
            if (decompiledClass == null || stringClass == null || enumValue(sinkTypeClass, "JAVA") == null) {
                logger.warn("cfr sink enum missing");
                return null;
            }

            return Proxy.newProxyInstance(
                    outputSinkFactoryClass.getClassLoader(),
                    new Class<?>[]{outputSinkFactoryClass},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        if ("getSupportedSinks".equals(name)) {
                            Object sinkType = args[0];
                            @SuppressWarnings("unchecked")
                            Collection<Object> available = (Collection<Object>) args[1];
                            if (isEnumName(sinkType, "JAVA")
                                    && available != null
                                    && available.contains(decompiledClass)) {
                                return Collections.singletonList(decompiledClass);
                            }
                            return Collections.singletonList(stringClass);
                        }
                        if ("getSink".equals(name)) {
                            Object sinkType = args[0];
                            Object sinkClass = args[1];
                            if (isEnumName(sinkType, "JAVA")) {
                                if (isEnumName(sinkClass, "DECOMPILED")) {
                                    return buildDecompiledSink(sinkInterface, decompiledCode);
                                }
                                if (isEnumName(sinkClass, "STRING")) {
                                    return buildStringSink(sinkInterface, decompiledCode);
                                }
                            }
                            return buildNoopSink(sinkInterface);
                        }
                        return null;
                    });
        } catch (Exception ex) {
            logger.warn("init cfr output sink factory fail: " + ex.getMessage());
            return null;
        }
    }

    private static Object buildDecompiledSink(Class<?> sinkInterface, StringBuilder decompiledCode) {
        return Proxy.newProxyInstance(
                sinkInterface.getClassLoader(),
                new Class<?>[]{sinkInterface},
                (proxy, method, args) -> {
                    if ("write".equals(method.getName()) && args != null && args.length > 0) {
                        Object decompiled = args[0];
                        if (decompiled != null) {
                            try {
                                Method getJava = decompiled.getClass().getMethod("getJava");
                                Object java = getJava.invoke(decompiled);
                                if (java != null) {
                                    decompiledCode.append(java.toString());
                                }
                            } catch (Exception ignored) {
                                return null;
                            }
                        }
                    }
                    return null;
                });
    }

    private static Object buildStringSink(Class<?> sinkInterface, StringBuilder decompiledCode) {
        return Proxy.newProxyInstance(
                sinkInterface.getClassLoader(),
                new Class<?>[]{sinkInterface},
                (proxy, method, args) -> {
                    if ("write".equals(method.getName()) && args != null && args.length > 0) {
                        Object value = args[0];
                        if (value != null) {
                            decompiledCode.append(value.toString());
                        }
                    }
                    return null;
                });
    }

    private static Object buildNoopSink(Class<?> sinkInterface) {
        return Proxy.newProxyInstance(
                sinkInterface.getClassLoader(),
                new Class<?>[]{sinkInterface},
                (proxy, method, args) -> null);
    }

    private static void runCfr(String classFilePath, Map<String, String> options, Object outputSinkFactory)
            throws Exception {
        Class<?> builderClass = Class.forName("org.benf.cfr.reader.api.CfrDriver$Builder");
        Object builder = builderClass.getDeclaredConstructor().newInstance();
        Method withOptions = builderClass.getMethod("withOptions", Map.class);
        Method withOutputSink = builderClass.getMethod(
                "withOutputSink",
                Class.forName("org.benf.cfr.reader.api.OutputSinkFactory"));
        Object optionsBuilder = withOptions.invoke(builder, options);
        Object sinkBuilder = withOutputSink.invoke(optionsBuilder, outputSinkFactory);
        Method build = builderClass.getMethod("build");
        Object driver = build.invoke(sinkBuilder);
        Method analyse = driver.getClass().getMethod("analyse", Collection.class);
        analyse.invoke(driver, Collections.singletonList(classFilePath));
    }

    private static Object enumValue(Class<?> enumClass, String name) {
        if (enumClass == null || !enumClass.isEnum()) {
            return null;
        }
        Object[] constants = enumClass.getEnumConstants();
        if (constants == null) {
            return null;
        }
        for (Object constant : constants) {
            if (name.equals(constant.toString())) {
                return constant;
            }
        }
        return null;
    }

    private static boolean isEnumName(Object value, String name) {
        return value != null && name.equals(value.toString());
    }

    /**
     * 检查CFR是否可用
     *
     * @return true if CFR is available
     */
    public static boolean isAvailable() {
        try {
            Class.forName("org.benf.cfr.reader.api.CfrDriver");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
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
}
