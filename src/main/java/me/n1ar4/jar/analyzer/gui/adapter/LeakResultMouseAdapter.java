/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.adapter;

import me.n1ar4.jar.analyzer.engine.CoreHelper;
import me.n1ar4.jar.analyzer.entity.LeakResult;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.util.DecompileSelector;
import me.n1ar4.jar.analyzer.gui.util.SyntaxAreaHelper;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.JarUtil;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LeakResultMouseAdapter extends MouseAdapter {
    @SuppressWarnings("all")
    public void mouseClicked(MouseEvent evt) {
        JList<?> list = (JList<?>) evt.getSource();
        if (evt.getClickCount() == 2) {
            int index = list.locationToIndex(evt.getPoint());
            LeakResult res = (LeakResult) list.getModel().getElementAt(index);

            String className = res.getClassName();
            String finalValue = res.getValue();

            UiExecutor.runAsync(() -> {
                String tempPath = className.replace("/", File.separator);
                Path directPath = Paths.get(Const.tempDir, tempPath);
                if (Files.exists(directPath) && Files.isRegularFile(directPath)) {
                    String code;
                    try {
                        code = new String(Files.readAllBytes(directPath), StandardCharsets.UTF_8);
                    } catch (Exception ex) {
                        UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(
                                MainForm.getInstance().getMasterPanel(),
                                "read file failed: " + ex.getMessage()));
                        return;
                    }

                    int idx = finalValue == null ? -1 : code.indexOf(finalValue);

                    String jarName = null;
                    Integer jarId = JarUtil.parseJarIdFromResourcePath(className);
                    if (jarId != null) {
                        jarName = MainForm.getEngine().getJarNameById(jarId);
                    }
                    String finalJarName = jarName == null ? "" : jarName;

                    UiExecutor.runOnEdt(() -> {
                        SearchInputListener.getFileTree().searchPathTarget(className);

                        MainForm.getCodeArea().setText(code);
                        if (idx != -1) {
                            MainForm.getCodeArea().setSelectionStart(idx);
                            MainForm.getCodeArea().setSelectionEnd(idx + finalValue.length());
                            MainForm.getCodeArea().setCaretPosition(idx);
                        } else {
                            MainForm.getCodeArea().setCaretPosition(0);
                        }

                        MainForm.getInstance().getCurClassText().setText(className);
                        MainForm.getInstance().getCurJarText().setText(finalJarName);
                        MainForm.getInstance().getCurMethodText().setText(null);
                        MainForm.setCurMethod(null);
                        MainForm.setCurClass(className);

                        MainForm.getInstance().getMethodImplList().setModel(new DefaultListModel<>());
                        MainForm.getInstance().getSuperImplList().setModel(new DefaultListModel<>());
                        MainForm.getInstance().getCalleeList().setModel(new DefaultListModel<>());
                        MainForm.getInstance().getCallerList().setModel(new DefaultListModel<>());
                    });
                    return;
                }

                String classPath = SyntaxAreaHelper.resolveClassPath(className);
                if (classPath == null) {
                    UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                                "<html>" +
                                        "<p>need dependency or class file not found</p>" +
                                        "<p>缺少依赖或者文件找不到（考虑加载 rt.jar 并检查你的 JAR 是否合法）</p>" +
                                        "<p>默认以三种方式找类：</p>" +
                                        "<p>1.根据类名直接从根目录找（例如 <strong>com/a/b/Demo</strong> ）</p>" +
                                        "<p>2.从 <strong>BOOT-INF</strong> 找（例如 <strong>BOOT-INF/classes/com/a/Demo</strong> ）</p>" +
                                        "<p>3.从 <strong>WEB-INF</strong> 找（例如 <strong>WEB-INF/classes/com/a/Demo</strong> ）</p>" +
                                        "</html>"));
                    return;
                }

                String code = DecompileSelector.decompile(Paths.get(classPath));
                int idx = finalValue == null ? -1 : code.indexOf(finalValue);
                UiExecutor.runOnEdt(() -> {
                    SearchInputListener.getFileTree().searchPathTarget(className);
                    if (idx != -1) {
                        MainForm.getCodeArea().setText(code);
                        MainForm.getCodeArea().setSelectionStart(idx);
                        MainForm.getCodeArea().setSelectionEnd(idx + finalValue.length());
                        MainForm.getCodeArea().setCaretPosition(idx);
                    } else {
                        MainForm.getCodeArea().setText(code);
                        MainForm.getCodeArea().setCaretPosition(0);
                    }
                });

                CoreHelper.refreshAllMethods(className);

                String jarName = MainForm.getEngine().getJarByClass(className);
                if (StringUtil.isNull(jarName)) {
                    jarName = MainForm.getEngine().getJarByClass(className);
                }
                String finalJarName = jarName;
                UiExecutor.runOnEdt(() -> {
                    MainForm.getInstance().getCurClassText().setText(className);
                    MainForm.getInstance().getCurJarText().setText(finalJarName);
                    MainForm.getInstance().getCurMethodText().setText(null);
                    MainForm.setCurMethod(null);
                    MainForm.setCurClass(className);

                    MainForm.getInstance().getMethodImplList().setModel(new DefaultListModel<>());
                    MainForm.getInstance().getSuperImplList().setModel(new DefaultListModel<>());
                    MainForm.getInstance().getCalleeList().setModel(new DefaultListModel<>());
                    MainForm.getInstance().getCallerList().setModel(new DefaultListModel<>());
                });
            });
        }
    }
}
