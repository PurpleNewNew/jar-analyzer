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

import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.gui.legacy.engine.CoreHelper;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.util.ProcessDialog;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import javax.swing.*;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SearchAction {
    public static void run() {
        JButton searchBtn = MainForm.getInstance().getStartSearchButton();
        JTextField scText = MainForm.getInstance().getSearchClassText();
        JTextField smText = MainForm.getInstance().getSearchMethodText();
        JTextField ssText = MainForm.getInstance().getSearchStrText();
        JRadioButton methodDefRadio = MainForm.getInstance().getMethodDefinitionRadioButton();
        JRadioButton methodCallRadio = MainForm.getInstance().getMethodCallRadioButton();
        JRadioButton binaryRadio = MainForm.getInstance().getBinarySearchRadioButton();
        JRadioButton stringRadio = MainForm.getInstance().getStringContainsRadioButton();

        JRadioButton equalsRadio = MainForm.getInstance().getEqualsSearchRadioButton();
        JRadioButton likeRadio = MainForm.getInstance().getLikeSearchRadioButton();

        searchBtn.addActionListener(e -> {
            MainForm.getInstance().syncSearchFilterFromText(
                    MainForm.getInstance().getBlackArea().getText());
            // 2025/06/27 搜索的类名给出提示
            String searchClass = scText.getText();
            if (searchClass == null) {
                searchClass = "";
            }
            if (searchClass.trim().startsWith("#")) {
                searchClass = "";
            }
            String finalClass = searchClass;

            if (methodCallRadio.isSelected() || methodDefRadio.isSelected()) {
                if (StringUtil.isNull(smText.getText())) {
                    JOptionPane.showMessageDialog(
                            MainForm.getInstance().getMasterPanel(), "need method data");
                    return;
                }
            }
            if (stringRadio.isSelected() || binaryRadio.isSelected()) {
                if (StringUtil.isNull(ssText.getText())) {
                    JOptionPane.showMessageDialog(
                            MainForm.getInstance().getMasterPanel(), "need search data");
                    return;
                }
            }

            JDialog dialog = UiExecutor.callOnEdt(() ->
                    ProcessDialog.createProgressDialog(MainForm.getInstance().getMasterPanel()));

            if (methodCallRadio.isSelected()) {
                if (dialog != null) {
                    UiExecutor.runOnEdt(() -> dialog.setVisible(true));
                }
                if (equalsRadio.isSelected()) {
                    UiExecutor.runAsync(() -> CoreHelper.refreshCallSearch(
                            finalClass, smText.getText(), null, dialog));
                }
                if (likeRadio.isSelected()) {
                    UiExecutor.runAsync(() -> CoreHelper.refreshCallSearchLike(
                            finalClass, smText.getText(), null, dialog));
                }
            }

            if (methodDefRadio.isSelected()) {
                if (dialog != null) {
                    UiExecutor.runOnEdt(() -> dialog.setVisible(true));
                }
                if (equalsRadio.isSelected()) {
                    UiExecutor.runAsync(() -> CoreHelper.refreshDefSearch(
                            finalClass, smText.getText(), null, dialog));
                }
                if (likeRadio.isSelected()) {
                    UiExecutor.runAsync(() -> CoreHelper.refreshDefSearchLike(
                            finalClass, smText.getText(), null, dialog));
                }
            }

            if (stringRadio.isSelected()) {
                if (dialog != null) {
                    UiExecutor.runOnEdt(() -> dialog.setVisible(true));
                }
                if (equalsRadio.isSelected()) {
                    UiExecutor.runAsync(() ->
                            CoreHelper.refreshStrSearchEqual(finalClass, ssText.getText(), dialog));
                }
                if (likeRadio.isSelected()) {
                    UiExecutor.runAsync(() ->
                            CoreHelper.refreshStrSearch(finalClass, ssText.getText(), dialog));
                }
            }

            if (binaryRadio.isSelected()) {
                CoreEngine engineSnapshot = MainForm.getEngine();
                if (engineSnapshot == null) {
                    JOptionPane.showMessageDialog(
                            MainForm.getInstance().getMasterPanel(),
                            "PLEASE BUILD DATABASE FIRST");
                    return;
                }
                if (dialog != null) {
                    UiExecutor.runOnEdt(() -> dialog.setVisible(true));
                }
                String search = ssText.getText();
                UiExecutor.runAsync(() -> {
                    Set<String> result = new HashSet<>();
                    ArrayList<String> jars = engineSnapshot.getJarsPath();
                    for (String jarPath : jars) {
                        try {
                            Path path = Paths.get(jarPath);
                            if (Files.size(path) > 1024 * 1024 * 50) {
                                try (FileInputStream fis = new FileInputStream(path.toFile())) {
                                    byte[] searchContext = search.getBytes();
                                    byte[] data = new byte[16384];
                                    int read;
                                    while ((read = fis.read(data, 0, data.length)) != -1) {
                                        for (int i = 0; i < read - searchContext.length + 1; ++i) {
                                            boolean found = true;
                                            for (int j = 0; j < searchContext.length; ++j) {
                                                if (data[i + j] != searchContext[j]) {
                                                    found = false;
                                                    break;
                                                }
                                            }
                                            if (found) {
                                                // FIX 2024/11/19
                                                // 可能弹出一大堆很多次
                                                // 去重保证一次即可
                                                result.add(jarPath);
                                            }
                                        }
                                    }
                                }
                            } else {
                                byte[] searchContext = search.getBytes();
                                byte[] data = Files.readAllBytes(path);
                                for (int i = 0; i < data.length - searchContext.length + 1; ++i) {
                                    boolean found = true;
                                    for (int j = 0; j < searchContext.length; ++j) {
                                        if (data[i + j] != searchContext[j]) {
                                            found = false;
                                            break;
                                        }
                                    }
                                    if (found) {
                                        // FIX 2024/11/19
                                        // 可能弹出一大堆很多次
                                        // 去重保证一次即可
                                        result.add(jarPath);
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    UiExecutor.runOnEdt(() -> {
                        if (dialog != null) {
                            dialog.dispose();
                        }
                        if (result.isEmpty()) {
                            JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                                    "<html>not found</html>");
                            return;
                        }
                        StringBuilder jarBuilder = new StringBuilder();
                        for (String data : result) {
                            jarBuilder.append(data);
                            jarBuilder.append("<br>");
                        }
                        JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                                "<html>search string [" + search + "] result:<br>"
                                        + jarBuilder + "</html>");
                    });
                });
                return;
            }

            MainForm.getInstance().getTabbedPanel().setSelectedIndex(1);
        });
    }
}
