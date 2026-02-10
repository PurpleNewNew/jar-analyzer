/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.engine.index;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.util.StrUtil;
import me.n1ar4.jar.analyzer.engine.DecompileDispatcher;
import me.n1ar4.jar.analyzer.engine.DecompileType;
import me.n1ar4.jar.analyzer.engine.index.entity.Result;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.apache.lucene.index.IndexWriterConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

public class IndexPluginsSupport {
    private static final Logger logger = LogManager.getLogger();

    public final static String CurrentPath = System.getProperty("user.dir");
    public final static String DocumentPath = CurrentPath + FileUtil.FILE_SEPARATOR + Const.indexDir;
    public final static String TempPath = CurrentPath + FileUtil.FILE_SEPARATOR + Const.tempDir;

    private static final Integer MAX_CORE = Runtime.getRuntime().availableProcessors();
    private static final Integer MAX_SIZE_GROUP = 40;
    private static final ExecutorService executorService = ExecutorBuilder.create()
            .setCorePoolSize(MAX_CORE * 2)
            .setMaxPoolSize(MAX_CORE * 3)
            .setWorkQueue(new LinkedBlockingQueue<>())
            .build();

    private static boolean useActive = false;

    public static void setUseActive(boolean useActive) {
        IndexPluginsSupport.useActive = useActive;
    }

    static {
        Path curPath = Paths.get(CurrentPath);
        Path indexPath = curPath.resolve(Const.indexDir);
        Path tempPath = curPath.resolve(Const.tempDir);
        // MKDIR TEMP DIR
        if (!Files.exists(tempPath)) {
            try {
                Files.createDirectories(tempPath);
            } catch (Exception ex) {
                logger.debug("create temp dir failed: {}: {}", tempPath, ex.toString());
            }
        }
        // MKDIR INDEX DIR
        if (!Files.exists(indexPath)) {
            try {
                Files.createDirectories(indexPath);
            } catch (Exception ex) {
                logger.debug("create index dir failed: {}: {}", indexPath, ex.toString());
            }
        }
    }

    public static List<File> getJarAnalyzerPluginsSupportAllFiles() {
        return FileUtil.loopFiles(TempPath, pathname -> pathname.getName().endsWith(".class"));
    }

    /**
     * 清除生成标识
     *
     * @param file jar文件
     * @return code
     */
    public static String getCode(File file, DecompileType type) {
        String decompile = null;
        try {
            decompile = DecompileDispatcher.decompile(file.toPath(), type);
        } catch (Exception ex) {
            logger.debug("decompile failed for index: {}: {}", file, ex.toString());
        }
        if (!StrUtil.isNotBlank(decompile)) {
            return null;
        }
        return DecompileDispatcher.stripPrefix(decompile, type);
    }


    public static boolean addIndex(File file) {
        if (useActive) {
            return true;
        }
        DecompileType type = DecompileDispatcher.resolvePreferred();
        Map<String, String> codeMap = new HashMap<>();
        String code = getCode(file, type);
        if (StrUtil.isNotBlank(code)) {
            codeMap.put(file.getPath(), code);
        }

        try {
            IndexEngine.initIndex(codeMap);
            IndexEngine.refreshSearcher();
            logger.info("add index {} ok", FileUtil.getName(file));
            return true;
        } catch (IOException ex) {
            logger.error("add index error: {}", ex.getMessage());
            return false;
        }
    }

    public static boolean initIndex() throws IOException, InterruptedException {
        IndexEngine.closeAll();
        FileUtil.del(DocumentPath);
        int size = MAX_SIZE_GROUP;
        List<File> jarAnalyzerPluginsSupportAllFiles = getJarAnalyzerPluginsSupportAllFiles();
        if (jarAnalyzerPluginsSupportAllFiles.isEmpty()) {
            logger.info("no class files found, cannot build lucene index");
            return false;
        }
        DecompileType type = DecompileDispatcher.resolvePreferred();
        IndexEngine.ensureWriter(IndexWriterConfig.OpenMode.CREATE);
        List<List<File>> split = CollUtil.split(jarAnalyzerPluginsSupportAllFiles, size);
        CountDownLatch latch = new CountDownLatch(split.size());
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
        for (List<File> files : split) {
            executorService.execute(() -> {
                Map<String, String> codeMap = new HashMap<>();
                files.forEach(file -> {
                    String code = getCode(file, type);
                    if (StrUtil.isNotBlank(code)) {
                        codeMap.put(file.getPath(), code);
                    }
                });
                try {
                    IndexEngine.initIndex(codeMap);
                } catch (Throwable t) {
                    me.n1ar4.jar.analyzer.utils.InterruptUtil.restoreInterruptIfNeeded(t);
                    errors.add(t);
                    logger.error("init index error: {}", t.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        IndexEngine.refreshSearcher();
        if (!errors.isEmpty()) {
            Throwable first = errors.peek();
            if (first instanceof Error) {
                throw (Error) first;
            }
            if (first instanceof IOException) {
                throw (IOException) first;
            }
            IOException ex = new IOException("init index failed");
            ex.initCause(first);
            throw ex;
        }
        return true;
    }

    public static Result search(String keyword) throws IOException {
        return IndexEngine.searchNormal(keyword);
    }
}
