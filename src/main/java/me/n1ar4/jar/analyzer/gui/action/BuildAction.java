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

import me.n1ar4.jar.analyzer.core.AnalyzeEnv;
import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.util.LogUtil;
import me.n1ar4.jar.analyzer.gui.util.MenuUtil;
import me.n1ar4.jar.analyzer.gui.util.ProcessDialog;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.DirUtil;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import javax.swing.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BuildAction {
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

        UiExecutor.runAsync(() -> {
            CoreEngine oldEngine = MainForm.getEngine();
            if (oldEngine != null) {
                try {
                    oldEngine.close();
                } catch (Throwable ignored) {
                }
                MainForm.setEngine(null);
            }
            Path od = Paths.get(Const.dbFile);
            if (Files.exists(od)) {
                LogUtil.info("jar-analyzer database exist");
                LogUtil.info("use rebuild+atomic replace mode; old db stays available until publish");
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

            JDialog dialog = UiExecutor.callOnEdt(() ->
                    ProcessDialog.createProgressDialog(MainForm.getInstance().getMasterPanel()));
            UiExecutor.runOnEdt(() ->
                    MainForm.getInstance().getStartBuildDatabaseButton().setEnabled(false));

            if (deleteTemp) {
                LogUtil.info("start delete temp");
                DirUtil.removeDir(new File(Const.tempDir));
                UiExecutor.runOnEdt(() -> MainForm.getInstance().getFileTree().refresh());
                LogUtil.info("delete temp success");
            }
            CoreRunner.run(Paths.get(path), rtJarPath, fixClass, dialog);
        });
    }

    public static void run() {
        MainForm.getInstance().getStartBuildDatabaseButton().addActionListener(e -> {
            AnalyzeEnv.isCli = false;
            String path = MainForm.getInstance().getFileText().getText();
            start(path);
        });
    }
}
