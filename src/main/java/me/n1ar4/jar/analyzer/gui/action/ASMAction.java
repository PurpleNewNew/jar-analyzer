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

import me.n1ar4.jar.analyzer.analyze.asm.ASMPrint;
import me.n1ar4.jar.analyzer.analyze.asm.IdentifyCallEngine;
import me.n1ar4.jar.analyzer.analyze.cfg.CFGForm;
import me.n1ar4.jar.analyzer.analyze.frame.FrameForm;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.OpcodeForm;
import me.n1ar4.jar.analyzer.gui.util.ProcessDialog;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;
import me.n1ar4.jar.analyzer.gui.util.LogUtil;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import javax.swing.*;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@SuppressWarnings("all")
public class ASMAction {
    public static void run() {
        MainForm instance = MainForm.getInstance();
        JButton opcodeBtn = instance.getOpcodeBtn();
        JButton asmBtn = instance.getJavaAsmBtn();
        JButton cfgBtn = instance.getCfgBtn();
        JButton frameBtn = instance.getFrameBtn();
        JButton simpleFrameBtn = instance.getSimpleFrameButton();

        cfgBtn.addActionListener(e -> {
            MethodResult curMethod = MainForm.getCurMethod();
            if (curMethod == null) {
                JOptionPane.showMessageDialog(instance.getMasterPanel(), "current method is null");
                return;
            }
            if (StringUtil.isNull(curMethod.getMethodName()) ||
                    StringUtil.isNull(curMethod.getMethodDesc()) ||
                    StringUtil.isNull(curMethod.getClassName())) {
                JOptionPane.showMessageDialog(instance.getMasterPanel(), "current method data error");
                return;
            }
            CFGForm.start();
        });

        frameBtn.addActionListener(e -> {
            MethodResult curMethod = MainForm.getCurMethod();
            if (curMethod == null) {
                JOptionPane.showMessageDialog(instance.getMasterPanel(), "current method is null");
                return;
            }
            if (StringUtil.isNull(curMethod.getMethodName()) ||
                    StringUtil.isNull(curMethod.getMethodDesc()) ||
                    StringUtil.isNull(curMethod.getClassName())) {
                JOptionPane.showMessageDialog(instance.getMasterPanel(), "current method data error");
                return;
            }
            FrameForm.start(true);
        });

        simpleFrameBtn.addActionListener(e -> {
            MethodResult curMethod = MainForm.getCurMethod();
            if (curMethod == null) {
                JOptionPane.showMessageDialog(instance.getMasterPanel(), "current method is null");
                return;
            }
            if (StringUtil.isNull(curMethod.getMethodName()) ||
                    StringUtil.isNull(curMethod.getMethodDesc()) ||
                    StringUtil.isNull(curMethod.getClassName())) {
                JOptionPane.showMessageDialog(instance.getMasterPanel(), "current method data error");
                return;
            }
            FrameForm.start(false);
        });

        opcodeBtn.addActionListener(e -> {
            try {
                MethodResult curMethod = MainForm.getCurMethod();
                if (curMethod == null) {
                    JOptionPane.showMessageDialog(instance.getMasterPanel(), "current method is null");
                    return;
                }
                if (StringUtil.isNull(curMethod.getMethodName()) ||
                        StringUtil.isNull(curMethod.getMethodDesc()) ||
                        StringUtil.isNull(curMethod.getClassName())) {
                    JOptionPane.showMessageDialog(instance.getMasterPanel(), "current method data error");
                    return;
                }
                String absPath = curMethod.getClassPath().toAbsolutePath().toString();
                JDialog dialog = UiExecutor.callOnEdt(() ->
                        ProcessDialog.createProgressDialog(instance.getMasterPanel()));
                if (dialog != null) {
                    UiExecutor.runOnEdt(() -> dialog.setVisible(true));
                }
                UiExecutor.runAsync(() -> {
                    try {
                        String test = IdentifyCallEngine.run(
                                absPath, curMethod.getMethodName(), curMethod.getMethodDesc());
                        UiExecutor.runOnEdt(() -> OpcodeForm.start(test));
                    } catch (Exception ex) {
                        LogUtil.warn("parse opcode error");
                    } finally {
                        if (dialog != null) {
                            UiExecutor.runOnEdt(dialog::dispose);
                        }
                    }
                });
            } catch (Exception ex) {
                LogUtil.warn("parse opcode error");
            }
        });

        asmBtn.addActionListener(e -> {
            try {
                MethodResult curMethod = MainForm.getCurMethod();
                if (curMethod == null) {
                    JOptionPane.showMessageDialog(instance.getMasterPanel(), "current method is null");
                    return;
                }
                if (StringUtil.isNull(curMethod.getMethodName()) ||
                        StringUtil.isNull(curMethod.getMethodDesc()) ||
                        StringUtil.isNull(curMethod.getClassName())) {
                    JOptionPane.showMessageDialog(instance.getMasterPanel(), "current method data error");
                    return;
                }
                String absPath = curMethod.getClassPath().toAbsolutePath().toString();
                JDialog dialog = UiExecutor.callOnEdt(() ->
                        ProcessDialog.createProgressDialog(instance.getMasterPanel()));
                if (dialog != null) {
                    UiExecutor.runOnEdt(() -> dialog.setVisible(true));
                }
                UiExecutor.runAsync(() -> {
                    try (InputStream is = Files.newInputStream(Paths.get(absPath))) {
                        String data = ASMPrint.getPrint(is, true);
                        UiExecutor.runOnEdt(() -> OpcodeForm.start(data));
                    } catch (Exception ex) {
                        LogUtil.warn("parse opcode error");
                    } finally {
                        if (dialog != null) {
                            UiExecutor.runOnEdt(dialog::dispose);
                        }
                    }
                });
            } catch (Exception ex) {
                LogUtil.warn("parse opcode error");
            }
        });
    }
}
