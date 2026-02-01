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
import me.n1ar4.jar.analyzer.engine.index.IndexPluginsSupport;
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.gui.LuceneSearchForm;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.util.DecompileSelector;
import me.n1ar4.jar.analyzer.gui.util.IconManager;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;
import me.n1ar4.jar.analyzer.utils.JarUtil;
import me.n1ar4.jar.analyzer.utils.OpenUtil;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class TreeRightMenuAdapter extends MouseAdapter {
    private static final String TIPS = "<html>" +
            "super class is missing (need <b>rt.jar</b>)<br>" +
            "maybe super class is <b>java.lang.Object</b> from rt.jar" +
            "</html>";
    private final JTree fileTree = MainForm.getInstance().getFileTree();
    private final JPopupMenu popupMenu;


    public TreeRightMenuAdapter() {
        popupMenu = new JPopupMenu();
        JMenuItem decompileItem = new JMenuItem("DECOMPILE");
        decompileItem.setIcon(IconManager.engineIcon);
        JMenuItem superClassItem = new JMenuItem("SUPER CLASS");
        superClassItem.setIcon(IconManager.pubIcon);
        JMenuItem openItem = new JMenuItem("OPEN IN EXPLORER");
        openItem.setIcon(IconManager.fileIcon);
        popupMenu.add(decompileItem);
        popupMenu.add(superClassItem);
        popupMenu.add(openItem);

        openItem.addActionListener(e -> {
            TreePath selectedPath = fileTree.getSelectionPath();
            if (selectedPath != null) {
                String sel = selectedPath.toString();
                sel = sel.substring(1, sel.length() - 1);
                String[] selArray = sel.split(",");
                ArrayList<String> pathList = new ArrayList<>();
                for (String s : selArray) {
                    s = s.trim();
                    pathList.add(s);
                }

                String[] path = pathList.toArray(new String[0]);
                String filePath = String.join(File.separator, path);

                OpenUtil.openFileInExplorer(Paths.get(filePath).toAbsolutePath().toString());
            }
        });

        decompileItem.addActionListener(e -> {
            TreePath selectedPath = fileTree.getSelectionPath();
            if (selectedPath != null) {
                DecompileHelper.decompile(selectedPath);
            }
        });

        superClassItem.addActionListener(e -> {
            TreePath selectedPath = fileTree.getSelectionPath();
            if (selectedPath != null) {
                String sel = selectedPath.toString();
                sel = sel.substring(1, sel.length() - 1);
                String[] selArray = sel.split(",");
                ArrayList<String> pathList = new ArrayList<>();
                for (String s : selArray) {
                    s = s.trim();
                    pathList.add(s);
                }

                String[] path = pathList.toArray(new String[0]);
                String filePath = String.join(File.separator, path);

                if (!filePath.endsWith(".class")) {
                    return;
                }

                String className = JarUtil.resolveClassNameFromPath(filePath);
                if (StringUtil.isNull(className)) {
                    return;
                }

                String finalClassName = className;
                UiExecutor.runAsync(() -> {
                    Path thePath = Paths.get(filePath);
                    if (!Files.exists(thePath)) {
                        UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(
                                MainForm.getInstance().getMasterPanel(),
                                "file not exist"));
                        return;
                    }
                    ClassResult classResult = MainForm.getEngine().getClassByClass(finalClassName);
                    String superClassName = classResult == null ? null : classResult.getSuperClassName();
                    if (StringUtil.isNull(superClassName)) {
                        UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(
                                MainForm.getInstance().getMasterPanel(), TIPS));
                        return;
                    }
                    String absPath = MainForm.getEngine().getAbsPath(superClassName);
                    if (StringUtil.isNull(absPath)) {
                        UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(
                                MainForm.getInstance().getMasterPanel(), TIPS));
                        return;
                    }

                    Path absPathPath = Paths.get(absPath);
                    if (!Files.exists(absPathPath)) {
                        UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(
                                MainForm.getInstance().getMasterPanel(), TIPS));
                        return;
                    }

                    if (LuceneSearchForm.getInstance() != null && LuceneSearchForm.usePaLucene()) {
                        IndexPluginsSupport.addIndex(absPathPath.toFile());
                    }
                    String code = DecompileSelector.decompile(absPathPath);
                    String jarName = MainForm.getEngine().getJarByClass(superClassName);

                    UiExecutor.runOnEdt(() -> {
                        SearchInputListener.getFileTree().searchPathTarget(superClassName);
                        MainForm.getCodeArea().setText(code);
                        MainForm.getCodeArea().setCaretPosition(0);

                        MainForm.getInstance().getCurClassText().setText(superClassName);
                        MainForm.setCurClass(superClassName);
                        MainForm.getInstance().getCurJarText().setText(jarName);
                        MainForm.getInstance().getCurMethodText().setText(null);
                        MainForm.setCurMethod(null);

                        MainForm.getInstance().getMethodImplList().setModel(new DefaultListModel<>());
                        MainForm.getInstance().getSuperImplList().setModel(new DefaultListModel<>());
                        MainForm.getInstance().getCalleeList().setModel(new DefaultListModel<>());
                        MainForm.getInstance().getCallerList().setModel(new DefaultListModel<>());
                    });

                    CoreHelper.refreshAllMethods(superClassName);
                });
            }
        });
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            int row = fileTree.getRowForLocation(e.getX(), e.getY());
            TreePath path = fileTree.getPathForRow(row);
            if (path == null || path.getLastPathComponent() == null) {
                return;
            }
            if (!path.getLastPathComponent().toString().endsWith(".class")) {
                return;
            }
            fileTree.setSelectionPath(path);
            if (row >= 0) {
                popupMenu.show(fileTree, e.getX(), e.getY());
            }
        }
    }
}
