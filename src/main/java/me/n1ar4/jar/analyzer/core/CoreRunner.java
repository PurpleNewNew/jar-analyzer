/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.config.ConfigEngine;
import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.asm.FixClassVisitor;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.CFRDecompileEngine;
import me.n1ar4.jar.analyzer.engine.DecompileEngine;
import me.n1ar4.jar.analyzer.engine.CoreHelper;
import me.n1ar4.jar.analyzer.engine.index.IndexEngine;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.LocalVarEntity;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.ModeSelector;
import me.n1ar4.jar.analyzer.gui.util.LogUtil;
import me.n1ar4.jar.analyzer.gui.util.MenuUtil;
import me.n1ar4.jar.analyzer.lucene.LuceneSearchListener;
import me.n1ar4.jar.analyzer.lucene.LuceneSearchWrapper;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.BytecodeCache;
import me.n1ar4.jar.analyzer.utils.ClasspathResolver;
import me.n1ar4.jar.analyzer.utils.CoreUtil;
import me.n1ar4.jar.analyzer.utils.DeferredFileWriter;
import me.n1ar4.jar.analyzer.utils.IOUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassReader;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class CoreRunner {
    private static final Logger logger = LogManager.getLogger();

    private static boolean quickMode = false;

    private static void runOnEdt(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    private static <T> T callOnEdt(Supplier<T> supplier) {
        if (supplier == null) {
            return null;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            return supplier.get();
        }
        AtomicReference<T> ref = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> ref.set(supplier.get()));
        } catch (Exception ignored) {
            return null;
        }
        return ref.get();
    }

    private static void setBuildProgress(int value) {
        runOnEdt(() -> MainForm.getInstance().getBuildBar().setValue(value));
    }

    private static void refreshCachesAfterBuild() {
        try {
            IndexEngine.closeAll();
        } catch (Throwable t) {
            logger.debug("close index fail: {}", t.toString());
        }
        try {
            LuceneSearchListener.clearCache();
        } catch (Throwable t) {
            logger.debug("clear lucene cache fail: {}", t.toString());
        }
        try {
            LuceneSearchWrapper.initEnvAsync();
        } catch (Throwable t) {
            logger.debug("init lucene env fail: {}", t.toString());
        }
        try {
            DecompileEngine.cleanCache();
        } catch (Throwable t) {
            logger.debug("clean fern cache fail: {}", t.toString());
        }
        try {
            CFRDecompileEngine.cleanCache();
        } catch (Throwable t) {
            logger.debug("clean cfr cache fail: {}", t.toString());
        }
        try {
            me.n1ar4.jar.analyzer.utils.ClassIndex.refresh();
        } catch (Throwable t) {
            logger.debug("refresh class index fail: {}", t.toString());
        }
    }

    public static void run(Path jarPath, Path rtJarPath, boolean fixClass, JDialog dialog) {
        // 2024-12-30
        // 非 CLI 才会弹窗
        if (!AnalyzeEnv.isCli) {
            // 2024-07-05 不允许太大的 JAR 文件
            long totalSize = 0;
            // Nested jars are handled only via AnalyzeEnv.jarsInJar (JarUtil).
            boolean includeNested = false;
            List<String> beforeJarList = ClasspathResolver.resolveInputArchives(
                    jarPath, rtJarPath, true, includeNested);
            for (String s : beforeJarList) {
                if (s == null || s.trim().isEmpty()) {
                    continue;
                }
                String lower = s.toLowerCase();
                if (lower.endsWith(".jar") || lower.endsWith(".war") || lower.endsWith(".class")) {
                    totalSize += Paths.get(s).toFile().length();
                }
            }

            int totalM = (int) (totalSize / 1024 / 1024);

            Integer chose;
            if (totalM > 1024) {
                // 对于大于 1G 的 JAR 输入进行提示
                chose = callOnEdt(() -> JOptionPane.showConfirmDialog(MainForm.getInstance().getMasterPanel(),
                        "<html>加载 JAR/WAR 总大小 <strong>" + totalM + "</strong> MB<br>" +
                                "文件内容过大，可能产生巨大的临时文件和数据库，可能非常消耗内存<br>" +
                                "请确认是否要继续进行分析" +
                                "</html>"));
            } else if (totalM == 0) {
                chose = callOnEdt(() -> JOptionPane.showConfirmDialog(MainForm.getInstance().getMasterPanel(),
                        "加载 JAR/WAR 总大小不足 1MB 是否继续"));
            } else {
                chose = callOnEdt(() -> JOptionPane.showConfirmDialog(MainForm.getInstance().getMasterPanel(),
                        "加载 JAR/WAR 总大小 " + totalM + " MB 是否继续"));
            }
            if (chose == null || chose != 0) {
                runOnEdt(() -> MainForm.getInstance().getStartBuildDatabaseButton().setEnabled(true));
                return;
            }

            // 2025/04/06 FEAT
            // 允许选择标准模式和快速模式两种方式
            Integer res = callOnEdt(ModeSelector::show);
            if (res == null) {
                runOnEdt(() -> MainForm.getInstance().getStartBuildDatabaseButton().setEnabled(true));
                return;
            }
            switch (res) {
                case 0:
                    runOnEdt(() -> JOptionPane.showMessageDialog(null, "你必须选择一种模式"));
                    runOnEdt(() -> MainForm.getInstance().getStartBuildDatabaseButton().setEnabled(true));
                    return;
                case 1:
                    quickMode = false;
                    logger.info("use std mode");
                    break;
                case 2:
                    quickMode = true;
                    logger.info("use quick mode");
                    break;
                default:
                    logger.error("unknown mode: " + res);
                    runOnEdt(() -> MainForm.getInstance().getStartBuildDatabaseButton().setEnabled(true));
                    return;
            }

            if (dialog != null) {
                runOnEdt(() -> dialog.setVisible(true));
            }

            runOnEdt(() -> MainForm.getInstance().getStartBuildDatabaseButton().setEnabled(false));
        }

        if (AnalyzeEnv.isCli) {
            try {
                Path dbPath = Paths.get(Const.dbFile);
                if (Files.exists(dbPath)) {
                    DatabaseManager.clearAllData();
                }
            } catch (Exception e) {
                logger.warn("clear cli db fail: {}", e.toString());
            }
        }
        DatabaseManager.setBuilding(true);
        try {
            DatabaseManager.prepareBuild();
        } catch (Throwable t) {
            DatabaseManager.setBuilding(false);
            throw t;
        }
        BuildDbWriter dbWriter = new BuildDbWriter();
        boolean finalizePending = true;
        boolean cleaned = false;
        try {
            Map<String, Integer> jarIdMap = new HashMap<>();

            List<ClassFileEntity> cfs;
            setBuildProgress(10);
            // Nested jars are handled only via AnalyzeEnv.jarsInJar (JarUtil).
            boolean includeNested = false;
            List<String> jarList = ClasspathResolver.resolveInputArchives(
                    jarPath, rtJarPath, !quickMode, includeNested);
            if (Files.isDirectory(jarPath)) {
                logger.info("input is a dir");
                LogUtil.info("input is a dir");
            } else {
                logger.info("input is a jar file");
                LogUtil.info("input is a jar");
            }
            if (rtJarPath != null) {
                LogUtil.info("analyze with rt.jar file");
            }
            runOnEdt(() -> MainForm.getInstance().getTotalJarVal().setText(String.valueOf(jarList.size())));
            for (String s : jarList) {
                if (s == null || s.trim().isEmpty()) {
                    continue;
                }
                String lower = s.toLowerCase();
                if (lower.endsWith(".jar") || lower.endsWith(".war")) {
                    DatabaseManager.saveJar(s);
                    jarIdMap.put(s, DatabaseManager.getJarId(s).getJid());
                }
            }
            cfs = CoreUtil.getAllClassesFromJars(jarList, jarIdMap, AnalyzeEnv.resources);
            // BUG CLASS NAME
            for (ClassFileEntity cf : cfs) {
                String className = cf.getClassName();
                if (!fixClass) {
                    int i = className.indexOf("classes");
                    if (className.contains("BOOT-INF") || className.contains("WEB-INF")) {
                        // 从 BOOT-INF/classes 开始取
                        // 从 WEB-INF/classes 开始取
                        className = className.substring(i + 8);
                    }
                    // 如果 i 小于 0 (不包含 classes 目录) 直接设置
                    cf.setClassName(className);
                } else {
                    // fix class name
                    Path parPath = resolveJarRoot(cf.getPath());
                    FixClassVisitor cv = new FixClassVisitor();
                    byte[] classBytes = cf.getFile();
                    if (classBytes == null || classBytes.length == 0) {
                        continue;
                    }
                    ClassReader cr = new ClassReader(classBytes);
                    cr.accept(cv, Const.HeaderASMOptions);
                    // get actual class name
                    String actualName = cv.getName();
                    Path path = parPath.resolve(Paths.get(actualName));
                    File file = path.toFile();
                    // write file
                    File parent = file.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        logger.error("fix class mkdirs error");
                    }
                    className = file.getPath() + ".class";
                    try {
                        Path fixedPath = Paths.get(className);
                        Files.write(fixedPath, classBytes);
                        BytecodeCache.preload(fixedPath, classBytes);
                    } catch (Exception ignored) {
                        logger.error("fix path copy bytes error");
                    }
                    cf.setClassName(actualName + ".class");
                    cf.setPath(Paths.get(className));
                }
            }

            setBuildProgress(15);
            AnalyzeEnv.classFileList.addAll(cfs);
            logger.info("get all class");
            LogUtil.info("get all class");
            dbWriter.submit(() -> DatabaseManager.saveClassFiles(AnalyzeEnv.classFileList));
            dbWriter.submit(() -> DatabaseManager.saveResources(AnalyzeEnv.resources));
            setBuildProgress(20);
            DiscoveryRunner.start(AnalyzeEnv.classFileList, AnalyzeEnv.discoveredClasses,
                    AnalyzeEnv.discoveredMethods, AnalyzeEnv.classMap,
                    AnalyzeEnv.methodMap, AnalyzeEnv.stringAnnoMap);
            dbWriter.submit(() -> DatabaseManager.saveClassInfo(AnalyzeEnv.discoveredClasses));
            setBuildProgress(25);
            dbWriter.submit(() -> DatabaseManager.saveMethods(AnalyzeEnv.discoveredMethods));
            setBuildProgress(30);
            logger.info("analyze class finish");
            LogUtil.info("analyze class finish");
            for (MethodReference mr : AnalyzeEnv.discoveredMethods) {
                ClassReference.Handle ch = mr.getClassReference();
                AnalyzeEnv.methodsInClassMap
                        .computeIfAbsent(ch, k -> new ArrayList<>())
                        .add(mr);
            }
            setBuildProgress(35);
            ClassAnalysisRunner.start(AnalyzeEnv.classFileList,
                    AnalyzeEnv.methodCalls,
                    AnalyzeEnv.methodMap,
                    AnalyzeEnv.methodCallMeta,
                    AnalyzeEnv.strMap,
                    AnalyzeEnv.classMap,
                    AnalyzeEnv.controllers,
                    AnalyzeEnv.interceptors,
                    AnalyzeEnv.servlets,
                    AnalyzeEnv.filters,
                    AnalyzeEnv.listeners,
                    !quickMode,
                    !quickMode,
                    !quickMode);
            setBuildProgress(40);

            BytecodeSymbolRunner.Result symbolResult = null;
            if (!quickMode && BytecodeSymbolRunner.isEnabled()) {
                symbolResult = BytecodeSymbolRunner.start(AnalyzeEnv.classFileList);
                List<CallSiteEntity> callSites = symbolResult.getCallSites();
                List<LocalVarEntity> localVars = symbolResult.getLocalVars();
                dbWriter.submit(() -> DatabaseManager.saveCallSites(callSites));
                dbWriter.submit(() -> DatabaseManager.saveLocalVars(localVars));
            }

            if (!quickMode) {
                AnalyzeEnv.inheritanceMap = InheritanceRunner.derive(AnalyzeEnv.classMap);
                setBuildProgress(50);
                logger.info("build inheritance");
                LogUtil.info("build inheritance");
                Map<MethodReference.Handle, Set<MethodReference.Handle>> implMap =
                        InheritanceRunner.getAllMethodImplementations(AnalyzeEnv.inheritanceMap, AnalyzeEnv.methodMap);
                dbWriter.submit(() -> DatabaseManager.saveImpls(implMap));
                setBuildProgress(60);

                // 2024/09/02
                // 自动处理方法实现是可选的
                // 具体参考 doc/README-others.md
                if (MenuUtil.enableFixMethodImpl()) {
                    // 方法 -> [所有子类 override 方法列表]
                    for (Map.Entry<MethodReference.Handle, Set<MethodReference.Handle>> entry :
                            implMap.entrySet()) {
                        MethodReference.Handle k = entry.getKey();
                        Set<MethodReference.Handle> v = entry.getValue();
                        // 当前方法的所有 callee 列表
                        HashSet<MethodReference.Handle> calls =
                                AnalyzeEnv.methodCalls.computeIfAbsent(k, kk -> new HashSet<>());
                        // 增加所有的 override 方法
                        for (MethodReference.Handle impl : v) {
                            MethodCallUtils.addCallee(calls, impl);
                            String reason = resolveOverrideReason(k);
                            MethodCallMeta.record(AnalyzeEnv.methodCallMeta, MethodCallKey.of(k, impl),
                                    MethodCallMeta.TYPE_OVERRIDE, MethodCallMeta.CONF_LOW, reason);
                        }
                    }
                    Set<ClassReference.Handle> instantiated =
                            DispatchCallResolver.collectInstantiatedClasses(AnalyzeEnv.classFileList);
                    int dispatchAdded = DispatchCallResolver.expandVirtualCalls(
                            AnalyzeEnv.methodCalls,
                            AnalyzeEnv.methodCallMeta,
                            AnalyzeEnv.methodMap,
                            AnalyzeEnv.classMap,
                            AnalyzeEnv.inheritanceMap,
                            instantiated);
                    logger.info("dispatch edges added: {}", dispatchAdded);
                    if (symbolResult != null && !symbolResult.getCallSites().isEmpty()) {
                        int typedAdded = TypedDispatchResolver.expandWithTypes(
                                AnalyzeEnv.methodCalls,
                                AnalyzeEnv.methodCallMeta,
                                AnalyzeEnv.methodMap,
                                AnalyzeEnv.classMap,
                                AnalyzeEnv.inheritanceMap,
                                symbolResult.getCallSites());
                        logger.info("typed dispatch edges added: {}", typedAdded);
                    }
                } else {
                    logger.warn("enable fix method impl/override is recommend");
                }

                clearCachedBytes(AnalyzeEnv.classFileList);
                dbWriter.submit(() -> DatabaseManager.saveMethodCalls(AnalyzeEnv.methodCalls));
                setBuildProgress(70);
                logger.info("build extra inheritance");
                LogUtil.info("build extra inheritance");
                setBuildProgress(80);
                dbWriter.submit(() -> DatabaseManager.saveStrMap(AnalyzeEnv.strMap, AnalyzeEnv.stringAnnoMap));
                dbWriter.submit(() -> DatabaseManager.saveSpringController(AnalyzeEnv.controllers));
                dbWriter.submit(() -> DatabaseManager.saveSpringInterceptor(AnalyzeEnv.interceptors));
                dbWriter.submit(() -> DatabaseManager.saveServlets(AnalyzeEnv.servlets));
                dbWriter.submit(() -> DatabaseManager.saveFilters(AnalyzeEnv.filters));
                dbWriter.submit(() -> DatabaseManager.saveListeners(AnalyzeEnv.listeners));

                setBuildProgress(90);
            } else {
                setBuildProgress(70);
                clearCachedBytes(AnalyzeEnv.classFileList);
                dbWriter.submit(() -> DatabaseManager.saveMethodCalls(AnalyzeEnv.methodCalls));
            }

            DeferredFileWriter.awaitAndStop();
            CoreUtil.cleanupEmptyTempDirs();
            dbWriter.await();
            DatabaseManager.finalizeBuild();
            finalizePending = false;
            refreshCachesAfterBuild();
            logger.info("build database finish");
            LogUtil.info("build database finish");

            long edgeCount = countEdges(AnalyzeEnv.methodCalls);
            runOnEdt(() -> MainForm.getInstance().getTotalEdgeVal().setText(String.valueOf(edgeCount)));

            long fileSizeBytes = getFileSize();
            String fileSizeMB = formatSizeInMB(fileSizeBytes);
            runOnEdt(() -> MainForm.getInstance().getDatabaseSizeVal().setText(fileSizeMB));
            setBuildProgress(100);
            runOnEdt(() -> MainForm.getInstance().getStartBuildDatabaseButton().setEnabled(false));

            if (!AnalyzeEnv.isCli) {
                runOnEdt(() -> {
                    MainForm.getInstance().getEngineVal().setText("RUNNING");
                    MainForm.getInstance().getEngineVal().setForeground(Color.GREEN);
                });

                runOnEdt(() -> MainForm.getInstance().getLoadDBText().setText(Const.dbFile));

                ConfigFile config = MainForm.getConfig();
                if (config == null) {
                    config = new ConfigFile();
                }
                String totalMethod = callOnEdt(() -> MainForm.getInstance().getTotalMethodVal().getText());
                String totalClass = callOnEdt(() -> MainForm.getInstance().getTotalClassVal().getText());
                String totalJar = callOnEdt(() -> MainForm.getInstance().getTotalJarVal().getText());
                String totalEdge = callOnEdt(() -> MainForm.getInstance().getTotalEdgeVal().getText());
                config.setTotalMethod(totalMethod);
                config.setTotalClass(totalClass);
                config.setTotalJar(totalJar);
                config.setTotalEdge(totalEdge);
                config.setTempPath(Const.tempDir);
                config.setDbPath(Const.dbFile);
                String jarPathText = callOnEdt(() -> MainForm.getInstance().getFileText().getText());
                config.setJarPath(jarPathText);
                config.setDbSize(fileSizeMB);
                config.setLang("en");
                config.setDecompileCacheSize(String.valueOf(DecompileEngine.getCacheCapacity()));
                MainForm.setConfig(config);
                MainForm.setEngine(new CoreEngine(config));

                Boolean autoSave = callOnEdt(() -> MainForm.getInstance().getAutoSaveCheckBox().isSelected());
                if (Boolean.TRUE.equals(autoSave)) {
                    ConfigEngine.saveConfig(config);
                    logger.info("auto save finish");
                    LogUtil.info("auto save finish");
                }

                runOnEdt(() -> MainForm.getInstance().getFileTree().refresh());

                // DISABLE WHITE/BLACK LIST
                runOnEdt(() -> {
                    MainForm.getInstance().getClassBlackArea().setEditable(false);
                    MainForm.getInstance().getClassWhiteArea().setEditable(false);
                });

                CoreHelper.refreshSpringC();
                CoreHelper.refreshSpringI();
                CoreHelper.refreshServlets();
                CoreHelper.refreshFilters();
                CoreHelper.refreshLiteners();

                if (dialog != null) {
                    runOnEdt(() -> {
                        dialog.setVisible(false);
                        dialog.dispose();
                    });
                }
            }

            clearAnalyzeEnv();
            cleaned = true;
        } finally {
            DeferredFileWriter.awaitAndStop();
            CoreUtil.cleanupEmptyTempDirs();
            dbWriter.close();
            if (!cleaned) {
                clearAnalyzeEnv();
            }
            if (finalizePending) {
                DatabaseManager.finalizeBuild();
            }
            DatabaseManager.setBuilding(false);
        }
    }

    private static void clearAnalyzeEnv() {
        clearCachedBytes(AnalyzeEnv.classFileList);
        AnalyzeEnv.classFileList.clear();
        AnalyzeEnv.discoveredClasses.clear();
        AnalyzeEnv.discoveredMethods.clear();
        AnalyzeEnv.methodsInClassMap.clear();
        AnalyzeEnv.classMap.clear();
        AnalyzeEnv.methodMap.clear();
        AnalyzeEnv.methodCalls.clear();
        AnalyzeEnv.methodCallMeta.clear();
        AnalyzeEnv.strMap.clear();
        AnalyzeEnv.resources.clear();
        BytecodeCache.clear();
        if (!quickMode && AnalyzeEnv.inheritanceMap != null) {
            AnalyzeEnv.inheritanceMap.getInheritanceMap().clear();
            AnalyzeEnv.inheritanceMap.getSubClassMap().clear();
        }
        AnalyzeEnv.controllers.clear();
        AnalyzeEnv.interceptors.clear();
        AnalyzeEnv.servlets.clear();
        AnalyzeEnv.filters.clear();
        AnalyzeEnv.listeners.clear();
        AnalyzeEnv.stringAnnoMap.clear();
        System.gc();
    }

    private static void clearCachedBytes(Set<ClassFileEntity> classFileList) {
        if (classFileList == null || classFileList.isEmpty()) {
            return;
        }
        for (ClassFileEntity cf : classFileList) {
            if (cf != null) {
                cf.clearCachedBytes();
            }
        }
    }

    private static Path resolveJarRoot(Path classPath) {
        Path tempRoot = Paths.get(Const.tempDir).toAbsolutePath().normalize();
        if (classPath == null) {
            return tempRoot;
        }
        Path current = classPath.toAbsolutePath().normalize();
        while (current != null && current.getParent() != null && !current.getParent().equals(tempRoot)) {
            current = current.getParent();
        }
        if (current != null && current.getParent() != null && current.getParent().equals(tempRoot)) {
            return current;
        }
        return tempRoot;
    }

    private static long countEdges(Map<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls) {
        if (methodCalls == null || methodCalls.isEmpty()) {
            return 0L;
        }
        long count = 0L;
        for (Set<MethodReference.Handle> callees : methodCalls.values()) {
            if (callees != null) {
                count += callees.size();
            }
        }
        return count;
    }

    private static long getFileSize() {
        File file = new File(Const.dbFile);
        return file.length();
    }

    private static String formatSizeInMB(long fileSizeBytes) {
        double fileSizeMB = (double) fileSizeBytes / (1024 * 1024);
        return String.format("%.2f MB", fileSizeMB);
    }

    private static String resolveOverrideReason(MethodReference.Handle base) {
        if (base == null) {
            return "override";
        }
        ClassReference baseClass = AnalyzeEnv.classMap.get(base.getClassReference());
        if (baseClass == null) {
            return "override";
        }
        String name = baseClass.getName();
        if (baseClass.isInterface()) {
            return "interface";
        }
        if ("java/lang/Object".equals(name)) {
            return "object";
        }
        if ("java/io/Serializable".equals(name)) {
            return "serializable";
        }
        if ("java/lang/Iterable".equals(name)) {
            return "iterable";
        }
        if ("java/lang/Cloneable".equals(name)) {
            return "cloneable";
        }
        if (name != null && name.startsWith("java/util/") && baseClass.isInterface()) {
            return "util_interface";
        }
        return "inheritance";
    }

}
