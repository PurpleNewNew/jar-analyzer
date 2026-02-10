/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.action;

import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.gui.legacy.engine.CoreHelper;
import me.n1ar4.jar.analyzer.engine.DecompileEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.ModeSelector;
import me.n1ar4.jar.analyzer.gui.legacy.lucene.LuceneSearchListener;
import me.n1ar4.jar.analyzer.gui.legacy.lucene.LuceneSearchWrapper;
import me.n1ar4.jar.analyzer.gui.util.IconManager;
import me.n1ar4.jar.analyzer.gui.util.LogUtil;
import me.n1ar4.jar.analyzer.gui.util.MenuUtil;
import me.n1ar4.jar.analyzer.gui.util.ProcessDialog;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.ClasspathResolver;
import me.n1ar4.jar.analyzer.utils.DirUtil;
import me.n1ar4.jar.analyzer.utils.InterruptUtil;
import me.n1ar4.jar.analyzer.utils.StringUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

public class BuildAction {
    private static final Logger logger = LogManager.getLogger();

    public static void start(String path) {
        MainForm.getInstance().getFileText().setText(path);
        MainForm.getInstance().syncCommonBlacklistFromText(
                MainForm.getInstance().getClassBlackArea().getText());

        if (StringUtil.isNull(path)) {
            JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                    "cannot start build - jar is null");
            return;
        }

        boolean fixClass = MenuUtil.isFixClassPathEnabled();
        boolean deleteTemp = MainForm.getInstance().getDeleteTempCheckBox().isSelected();
        boolean addRtJar = MainForm.getInstance().getAddRtJarWhenCheckBox().isSelected();
        String rtText = MainForm.getInstance().getRtText().getText();
        boolean enableFixMethodImpl = MenuUtil.enableFixMethodImpl();

        UiExecutor.runAsync(() -> {
            // Headless-friendly workspace context for classpath/runtime resolution.
            try {
                WorkspaceContext.setInputPath(Paths.get(path));
            } catch (Throwable t) {
                InterruptUtil.restoreInterruptIfNeeded(t);
                if (t instanceof Error) {
                    throw (Error) t;
                }
                logger.debug("set workspace input path failed: {}: {}", path, t.toString());
            }
            Path od = Paths.get(Const.dbFile);
            if (Files.exists(od)) {
                LogUtil.info("jar-analyzer database exist");
                Integer res = UiExecutor.callOnEdt(() -> JOptionPane.showConfirmDialog(
                        MainForm.getInstance().getMasterPanel(),
                        "<html>" +
                                "file <b>jar-analyzer.db</b> exist<br>" +
                                "do you want to delete the old db file?" +
                                "</html>"));
                if (res == null || res == JOptionPane.CANCEL_OPTION) {
                    LogUtil.info("cancel build process");
                    return;
                }
                if (res == JOptionPane.OK_OPTION) {
                    LogUtil.info("delete old db");
                    try {
                        Files.delete(od);
                        LogUtil.info("delete old db success");
                    } catch (Exception ex) {
                        LogUtil.error("cannot delete db : " + ex.getMessage());
                        UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(
                                MainForm.getInstance().getMasterPanel(),
                                "<html>" +
                                        "<p>无法删除旧的 <strong>jar-analyzer.db</strong> 文件</p>" +
                                        "<p>" + ex.getMessage().trim() + "</p>" +
                                        "</html>"));
                        return;
                    }
                } else {
                    LogUtil.info("overwrite database");
                    try {
                        DatabaseManager.clearAllData();
                        LogUtil.info("clear old db data success");
                    } catch (Exception ex) {
                        LogUtil.error("clear old db data fail: " + ex.getMessage());
                        UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(
                                MainForm.getInstance().getMasterPanel(),
                                "<html>" +
                                        "<p>cannot clear old db data</p>" +
                                        "<p>" + ex.getMessage().trim() + "</p>" +
                                        "</html>"));
                        return;
                    }
                }
            }

            Path rtJarPath = null;
            if (addRtJar) {
                if (StringUtil.isNull(rtText)) {
                    UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(
                            MainForm.getInstance().getMasterPanel(),
                            "rt.jar file is null"));
                    return;
                }
                rtJarPath = Paths.get(rtText);
                if (!Files.exists(rtJarPath)) {
                    UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(
                            MainForm.getInstance().getMasterPanel(),
                            "rt.jar file not exist"));
                    return;
                }
            }
            try {
                WorkspaceContext.setRuntimeJarPath(rtJarPath);
            } catch (Throwable t) {
                InterruptUtil.restoreInterruptIfNeeded(t);
                if (t instanceof Error) {
                    throw (Error) t;
                }
                logger.debug("set workspace runtime jar path failed: {}: {}", rtJarPath, t.toString());
            }

            // Large input confirm + mode selection are GUI responsibilities.
            if (!confirmInputSize(Paths.get(path), rtJarPath)) {
                UiExecutor.runOnEdt(() -> MainForm.getInstance().getStartBuildDatabaseButton().setEnabled(true));
                return;
            }
            Integer mode = UiExecutor.callOnEdt(ModeSelector::show);
            if (mode == null || mode == 0) {
                UiExecutor.runOnEdt(() -> MainForm.getInstance().getStartBuildDatabaseButton().setEnabled(true));
                return;
            }
            boolean quickMode = mode == 2;

            JDialog dialog = UiExecutor.callOnEdt(() ->
                    ProcessDialog.createProgressDialog(MainForm.getInstance().getMasterPanel()));
            UiExecutor.runOnEdt(() -> {
                MainForm.getInstance().getStartBuildDatabaseButton().setEnabled(false);
                MainForm.getInstance().getBuildBar().setValue(0);
                if (dialog != null) {
                    dialog.setVisible(true);
                }
            });

            if (deleteTemp) {
                LogUtil.info("start delete temp");
                DirUtil.removeDir(new File(Const.tempDir));
                UiExecutor.runOnEdt(() -> MainForm.getInstance().getFileTree().refresh());
                LogUtil.info("delete temp success");
            }

            CoreRunner.BuildResult result = null;
            try {
                result = CoreRunner.run(Paths.get(path), rtJarPath, fixClass,
                        quickMode, enableFixMethodImpl,
                        p -> UiExecutor.runOnEdt(() -> MainForm.getInstance().getBuildBar().setValue(p)));
            } finally {
                JDialog finalDialog = dialog;
                UiExecutor.runOnEdt(() -> {
                    if (finalDialog != null) {
                        finalDialog.setVisible(false);
                        finalDialog.dispose();
                    }
                });
            }

            CoreRunner.BuildResult finalResult = result;
            UiExecutor.runOnEdt(() -> applyBuildResultToUi(finalResult, path));
        });
    }

    private static boolean confirmInputSize(Path jarPath, Path rtJarPath) {
        try {
            long totalSize = 0L;
            boolean includeNested = false;
            List<String> beforeJarList = ClasspathResolver.resolveInputArchives(
                    jarPath, rtJarPath, true, includeNested);
            for (String s : beforeJarList) {
                if (s == null || s.trim().isEmpty()) {
                    continue;
                }
                String lower = s.toLowerCase(Locale.ROOT);
                if (lower.endsWith(".jar") || lower.endsWith(".war") || lower.endsWith(".class")) {
                    totalSize += Paths.get(s).toFile().length();
                }
            }
            int totalM = (int) (totalSize / 1024 / 1024);
            String html;
            if (totalM > 1024) {
                html = "<html>加载 JAR/WAR 总大小 <strong>" + totalM + "</strong> MB<br>" +
                        "文件内容过大，可能产生巨大的临时文件和数据库，可能非常消耗内存<br>" +
                        "请确认是否要继续进行分析</html>";
            } else if (totalM == 0) {
                html = "加载 JAR/WAR 总大小不足 1MB 是否继续";
            } else {
                html = "加载 JAR/WAR 总大小 " + totalM + " MB 是否继续";
            }
            Integer chose = UiExecutor.callOnEdt(() -> JOptionPane.showConfirmDialog(
                    MainForm.getInstance().getMasterPanel(),
                    html,
                    "Jar Analyzer",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    IconManager.auIcon));
            return chose != null && chose == JOptionPane.OK_OPTION;
        } catch (Throwable t) {
            InterruptUtil.restoreInterruptIfNeeded(t);
            if (t instanceof Error) {
                throw (Error) t;
            }
            logger.debug("confirm input size failed: {}", t.toString());
            return true;
        }
    }

    private static void applyBuildResultToUi(CoreRunner.BuildResult result, String jarPath) {
        if (result == null) {
            MainForm.getInstance().getStartBuildDatabaseButton().setEnabled(true);
            return;
        }
        try {
            MainForm.getInstance().getTotalJarVal().setText(String.valueOf(result.getJarCount()));
            MainForm.getInstance().getTotalClassVal().setText(String.valueOf(result.getClassCount()));
            MainForm.getInstance().getTotalMethodVal().setText(String.valueOf(result.getMethodCount()));
            MainForm.getInstance().getTotalEdgeVal().setText(String.valueOf(result.getEdgeCount()));
            MainForm.getInstance().getDatabaseSizeVal().setText(result.getDbSizeLabel());
        } catch (Throwable t) {
            InterruptUtil.restoreInterruptIfNeeded(t);
            if (t instanceof Error) {
                throw (Error) t;
            }
            logger.debug("apply build metrics to ui failed: {}", t.toString());
        }
        try {
            MainForm.getInstance().getEngineVal().setText("RUNNING");
            MainForm.getInstance().getEngineVal().setForeground(Color.GREEN);
        } catch (Throwable t) {
            InterruptUtil.restoreInterruptIfNeeded(t);
            if (t instanceof Error) {
                throw (Error) t;
            }
            logger.debug("apply build status to ui failed: {}", t.toString());
        }
        try {
            MainForm.getInstance().getLoadDBText().setText(Const.dbFile);
        } catch (Throwable t) {
            InterruptUtil.restoreInterruptIfNeeded(t);
            if (t instanceof Error) {
                throw (Error) t;
            }
            logger.debug("apply db path to ui failed: {}", t.toString());
        }
        try {
            me.n1ar4.jar.analyzer.config.ConfigFile config = MainForm.getConfig();
            if (config == null) {
                config = new me.n1ar4.jar.analyzer.config.ConfigFile();
            }
            config.setTotalJar(String.valueOf(result.getJarCount()));
            config.setTotalClass(String.valueOf(result.getClassCount()));
            config.setTotalMethod(String.valueOf(result.getMethodCount()));
            config.setTotalEdge(String.valueOf(result.getEdgeCount()));
            config.setTempPath(Const.tempDir);
            config.setDbPath(Const.dbFile);
            config.setJarPath(jarPath);
            config.setDbSize(result.getDbSizeLabel());
            config.setLang("en");
            config.setDecompileCacheSize(String.valueOf(DecompileEngine.getCacheCapacity()));
            MainForm.setConfig(config);
            // CoreRunner already set EngineContext; keep GUI state consistent.
            if (EngineContext.getEngine() != null) {
                MainForm.setEngine(EngineContext.getEngine());
            }
            if (MainForm.getInstance().getAutoSaveCheckBox().isSelected()) {
                me.n1ar4.jar.analyzer.config.ConfigEngine.saveConfig(config);
                LogUtil.info("auto save finish");
            }
        } catch (Throwable t) {
            InterruptUtil.restoreInterruptIfNeeded(t);
            if (t instanceof Error) {
                throw (Error) t;
            }
            logger.debug("apply build config to ui failed: {}", t.toString());
        }
        try {
            MainForm.getInstance().getFileTree().refresh();
        } catch (Throwable t) {
            InterruptUtil.restoreInterruptIfNeeded(t);
            if (t instanceof Error) {
                throw (Error) t;
            }
            logger.debug("refresh file tree after build failed: {}", t.toString());
        }
        try {
            MainForm.getInstance().getClassBlackArea().setEditable(false);
            MainForm.getInstance().getClassWhiteArea().setEditable(false);
        } catch (Throwable t) {
            InterruptUtil.restoreInterruptIfNeeded(t);
            if (t instanceof Error) {
                throw (Error) t;
            }
            logger.debug("lock blacklist/whitelist areas failed: {}", t.toString());
        }
        try {
            CoreHelper.refreshSpringC();
            CoreHelper.refreshSpringI();
            CoreHelper.refreshServlets();
            CoreHelper.refreshFilters();
            CoreHelper.refreshLiteners();
        } catch (Throwable t) {
            InterruptUtil.restoreInterruptIfNeeded(t);
            if (t instanceof Error) {
                throw (Error) t;
            }
            logger.debug("refresh core listeners after build failed: {}", t.toString());
        }

        // Refresh lucene filename snapshot + query cache after rebuilding temp/db.
        try {
            LuceneSearchListener.clearCache();
            LuceneSearchWrapper.initEnvAsync();
        } catch (Throwable t) {
            InterruptUtil.restoreInterruptIfNeeded(t);
            if (t instanceof Error) {
                throw (Error) t;
            }
            logger.debug("refresh lucene cache after build failed: {}", t.toString());
        }
    }

    public static void run() {
        MainForm.getInstance().getStartBuildDatabaseButton().addActionListener(e -> {
            String path = MainForm.getInstance().getFileText().getText();
            start(path);
        });
    }
}
