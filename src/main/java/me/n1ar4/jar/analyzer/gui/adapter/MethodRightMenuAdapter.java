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

import me.n1ar4.jar.analyzer.engine.CFRDecompileEngine;
import me.n1ar4.jar.analyzer.engine.DecompileEngine;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.util.DecompileSelector;
import me.n1ar4.jar.analyzer.gui.util.ProcessDialog;
import me.n1ar4.jar.analyzer.gui.util.SyntaxAreaHelper;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 现在不支持改方法名的操作了
 * 但是先保留 说不定未来某天会遇到
 */
@SuppressWarnings("all")
public class MethodRightMenuAdapter extends MouseAdapter {
    private static final Logger logger = LogManager.getLogger();
    private final JList<MethodResult> list;
    private final JPopupMenu popupMenu;

    public static byte[] renameMethod(String className,
                                      String methodName,
                                      String methodDesc,
                                      String newMethodName,
                                      Integer jarId) {
        try {
            Path finalFile = resolveClassFilePath(className, jarId);
            if (finalFile == null || !Files.exists(finalFile)) {
                logger.error("rename method file not found: {}", className);
                return new byte[]{};
            }
            ClassReader classReader = new ClassReader(Files.readAllBytes(finalFile));
            ClassWriter classWriter = new ClassWriter(classReader, 0);
            ClassVisitor classVisitor = new ClassVisitor(Const.ASMVersion, classWriter) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc,
                                                 String signature, String[] exceptions) {
                    if (name.equals(methodName) && desc.equals(methodDesc)) {
                        name = newMethodName;
                    }
                    return super.visitMethod(access, name, desc, signature, exceptions);
                }
            };
            classReader.accept(classVisitor, 0);
            return classWriter.toByteArray();
        } catch (Exception ex) {
            logger.error("rename method error: {}", ex.toString());
            return new byte[]{};
        }
    }


    @SuppressWarnings("all")
    public MethodRightMenuAdapter() {
        list = MainForm.getInstance().getAllMethodList();
        popupMenu = new JPopupMenu();
        JMenuItem renameItem = new JMenuItem("rename");
        popupMenu.add(renameItem);

        renameItem.addActionListener(e -> {
            int selectedIndex = list.getSelectedIndex();
            if (selectedIndex != -1) {
                DefaultListModel<MethodResult> model = (DefaultListModel<MethodResult>) list.getModel();
                MethodResult currentItem = model.getElementAt(selectedIndex);
                String newItem = JOptionPane.showInputDialog(MainForm.getInstance().getMasterPanel(),
                        "rename method: ", currentItem.getMethodName());
                if (newItem != null && !newItem.isEmpty()) {
                    JDialog dialog = UiExecutor.callOnEdt(() ->
                            ProcessDialog.createDelayedProgressDialog(MainForm.getInstance().getMasterPanel(), 200));
                    UiExecutor.runAsync(() -> {
                        try {
                            int res = MainForm.getEngine().updateMethod(currentItem.getClassName(),
                                    currentItem.getMethodName(),
                                    currentItem.getMethodDesc(), newItem);
                            if (res == 0) {
                                UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(
                                        MainForm.getInstance().getMasterPanel(),
                                        "update database error"));
                                return;
                            }
                            byte[] modifiedClass = renameMethod(currentItem.getClassName(),
                                    currentItem.getMethodName(), currentItem.getMethodDesc(), newItem,
                                    currentItem.getJarId());
                            try {
                                String originClass = currentItem.getClassName();
                                Path finalFile = resolveClassFilePath(originClass, currentItem.getJarId());
                                if (finalFile == null) {
                                    UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(
                                            MainForm.getInstance().getMasterPanel(),
                                            "class file not found"));
                                    return;
                                }
                                Files.deleteIfExists(finalFile);
                                Files.write(finalFile, modifiedClass);
                                DecompileEngine.cleanCache();
                                CFRDecompileEngine.cleanCache();
                                String code = DecompileSelector.decompile(finalFile);
                                UiExecutor.runOnEdt(() -> {
                                    MainForm.getCodeArea().setText(code);
                                    logger.info("refresh bytecode");
                                    currentItem.setMethodName(newItem);
                                    model.setElementAt(currentItem, selectedIndex);
                                });
                            } catch (Exception ignored) {
                                logger.error("write bytecode error");
                            }
                        } finally {
                            if (dialog != null) {
                                UiExecutor.runOnEdt(dialog::dispose);
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            if (e.isPopupTrigger()) {
                int index = list.locationToIndex(e.getPoint());
                list.setSelectedIndex(index);
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    private static Path resolveClassFilePath(String className, Integer jarId) {
        String classPath = SyntaxAreaHelper.resolveClassPath(className, jarId);
        if (classPath == null || classPath.trim().isEmpty()) {
            return null;
        }
        return Paths.get(classPath);
    }
}
