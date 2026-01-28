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

import me.n1ar4.jar.analyzer.core.FinderRunner;
import me.n1ar4.jar.analyzer.engine.CoreHelper;
import me.n1ar4.jar.analyzer.engine.DecompileEngine;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.adapter.SearchInputListener;
import me.n1ar4.jar.analyzer.gui.state.State;
import me.n1ar4.jar.analyzer.gui.util.IconManager;
import me.n1ar4.jar.analyzer.gui.util.ProcessDialog;
import me.n1ar4.jar.analyzer.gui.util.SyntaxAreaHelper;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;

@SuppressWarnings("all")
public class PrevNextAction {
    public static void run() {
        MainForm instance = MainForm.getInstance();
        MainForm.setCurStateIndex(-1);
        MainForm.getStateList().clear();
        instance.getPrevBtn().addActionListener(e -> {
            // 当前是 0 或者上一个是 null 不允许上一步
            if (MainForm.getCurStateIndex() <= 0 ||
                    MainForm.getStateList().get(MainForm.getCurStateIndex() - 1) == null) {
                JOptionPane.showMessageDialog(instance.getMasterPanel(), String.format("<html>" +
                                "<p style='color: red; font-weight: bold;'>You cannot do it</p>" +
                                "<p>Current idx: <span style='color: blue; font-weight: bold;'>%d</span></p>" +
                                "<p>Total length: <span style='color: blue; font-weight: bold;'>%d</span></p>" +
                                "</html>", MainForm.getCurStateIndex(), MainForm.getStateList().size()),
                        "prev next action", JOptionPane.INFORMATION_MESSAGE, IconManager.ausIcon);
            }
            if (MainForm.getCurMethod() == null) {
                JOptionPane.showMessageDialog(instance.getMasterPanel(), "current method is null");
                return;
            }

            // 改变指针不改变内容
            MainForm.setCurStateIndex(MainForm.getCurStateIndex() - 1);
            if (MainForm.getCurStateIndex() < 0) {
                MainForm.setCurStateIndex(0);
            }

            // 变更状态
            State prev = MainForm.getStateList().get(MainForm.getCurStateIndex());
            if (prev == null) {
                JOptionPane.showMessageDialog(instance.getMasterPanel(), "invalid previous state");
                return;
            }
            instance.getCurJarText().setText(prev.getJarName());
            instance.getCurClassText().setText(prev.getClassName());
            MainForm.setCurClass(prev.getClassName());
            instance.getCurMethodText().setText(prev.getMethodName());
            MethodResult m = new MethodResult();
            Path path = prev.getClassPath();
            m.setJarName(prev.getJarName());
            m.setMethodName(prev.getMethodName());
            m.setMethodDesc(prev.getMethodDesc());
            m.setClassName(prev.getClassName());
            m.setClassPath(path);
            MainForm.setCurMethod(m);

            // DECOMPILE
            String className = m.getClassName();
            String methodName = m.getMethodName();
            String methodDesc = m.getMethodDesc();
            UiExecutor.runAsync(() -> {
                String classPath = SyntaxAreaHelper.resolveClassPath(className);
                if (classPath == null) {
                    UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(
                            MainForm.getInstance().getMasterPanel(),
                            "<html><p>need dependency or class file not found</p></html>"));
                    return;
                }
                String code = DecompileEngine.decompile(Paths.get(classPath));
                if (code == null) {
                    return;
                }
                int pos = FinderRunner.find(code, methodName, methodDesc);
                UiExecutor.runOnEdt(() -> {
                    // SET FILE TREE HIGHLIGHT
                    SearchInputListener.getFileTree().searchPathTarget(className);
                    MainForm.getCodeArea().setText(code);
                    MainForm.getCodeArea().setCaretPosition(pos + 1);
                });
            });

            JDialog dialog = ProcessDialog.createProgressDialog(MainForm.getInstance().getMasterPanel());
            CoreHelper.refreshMethodContextAsync(className, m.getMethodName(), m.getMethodDesc(), dialog);
        });


        instance.getNextBtn().addActionListener(e -> {
            // 当前是最后一个元素 或 下一个元素是空
            if (MainForm.getCurStateIndex() >= MainForm.getStateList().size() - 1 ||
                    MainForm.getStateList().get(MainForm.getCurStateIndex() + 1) == null) {
                JOptionPane.showMessageDialog(instance.getMasterPanel(), String.format("<html>" +
                                "<p style='color: red; font-weight: bold;'>You cannot do it</p>" +
                                "<p>Current idx: <span style='color: blue; font-weight: bold;'>%d</span></p>" +
                                "<p>Total length: <span style='color: blue; font-weight: bold;'>%d</span></p>" +
                                "</html>", MainForm.getCurStateIndex(), MainForm.getStateList().size()),
                        "prev next action", JOptionPane.INFORMATION_MESSAGE, IconManager.ausIcon);
                return;
            }

            // 改变指针不改变内容
            MainForm.setCurStateIndex(MainForm.getCurStateIndex() + 1);
            if (MainForm.getCurStateIndex() >= MainForm.getStateList().size()) {
                MainForm.setCurStateIndex(MainForm.getStateList().size() - 1);
            }

            State next = MainForm.getStateList().get(MainForm.getCurStateIndex());
            if (next == null) {
                JOptionPane.showMessageDialog(instance.getMasterPanel(), "invalid next state");
                return;
            }
            instance.getCurJarText().setText(next.getJarName());
            instance.getCurClassText().setText(next.getClassName());
            MainForm.setCurClass(next.getClassName());
            instance.getCurMethodText().setText(next.getMethodName());
            MethodResult m = new MethodResult();
            Path path = next.getClassPath();
            m.setJarName(next.getJarName());
            m.setMethodName(next.getMethodName());
            m.setMethodDesc(next.getMethodDesc());
            m.setClassPath(path);
            m.setClassName(next.getClassName());
            MainForm.setCurMethod(m);

            // DECOMPILE
            String className = m.getClassName();
            String methodName = m.getMethodName();
            String methodDesc = m.getMethodDesc();
            UiExecutor.runAsync(() -> {
                String classPath = SyntaxAreaHelper.resolveClassPath(className);
                if (classPath == null) {
                    UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(
                            MainForm.getInstance().getMasterPanel(),
                            "<html><p>need dependency or class file not found</p></html>"));
                    return;
                }
                String code = DecompileEngine.decompile(Paths.get(classPath));
                if (code == null) {
                    return;
                }
                int pos = FinderRunner.find(code, methodName, methodDesc);
                UiExecutor.runOnEdt(() -> {
                    MainForm.getCodeArea().setText(code);
                    MainForm.getCodeArea().setCaretPosition(pos + 1);
                });
            });

            JDialog dialog = ProcessDialog.createProgressDialog(MainForm.getInstance().getMasterPanel());
            CoreHelper.refreshMethodContextAsync(className, m.getMethodName(), m.getMethodDesc(), dialog);
        });
    }
}
