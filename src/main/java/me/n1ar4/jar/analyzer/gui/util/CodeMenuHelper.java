/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.util;

import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.gui.LuceneSearchForm;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.*;
import java.util.List;

public class CodeMenuHelper {
    final static JPanel fileTreeSearchPanel = MainForm.getInstance().getFileTreeSearchPanel();
    final static JTextField fileTreeSearchTextField = MainForm.getInstance().getFileTreeSearchTextField();

    public static void run() {
        install((RSyntaxTextArea) MainForm.getCodeArea());
    }

    public static void install(RSyntaxTextArea rArea) {
        if (rArea == null) {
            return;
        }
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem selectItem = new JMenuItem("SELECT STRING (LDC)");
        selectItem.setIcon(IconManager.stringIcon);
        selectItem.addActionListener(e -> {
            String str = rArea.getSelectedText();

            if (str == null) {
                JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                        "SELECTED STRING IS NULL");
                return;
            }

            UiExecutor.runAsync(() -> {
                List<MethodResult> mrs = MainForm.getEngine().getMethodsByStr(str);
                DefaultListModel<MethodResult> searchData = new DefaultListModel<>();
                searchData.clear();
                for (MethodResult mr : mrs) {
                    searchData.addElement(mr);
                }
                UiExecutor.runOnEdt(() -> {
                    MainForm.getInstance().getSearchList().setModel(searchData);
                    MainForm.getInstance().getTabbedPanel().setSelectedIndex(1);
                });
            });
        });
        popupMenu.add(selectItem);

        JMenuItem searchCallItem = new JMenuItem("SEARCH CALL INFO");
        searchCallItem.setIcon(IconManager.callIcon);
        searchCallItem.addActionListener(e -> {
            String methodName = rArea.getSelectedText();

            if (methodName == null) {
                JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                        "SELECTED STRING IS NULL");
                return;
            }

            methodName = methodName.trim();

            String className = MainForm.getCurClass();

            String finalMethodName = methodName;
            JDialog dialog = UiExecutor.callOnEdt(() ->
                    ProcessDialog.createProgressDialog(MainForm.getInstance().getMasterPanel()));
            if (dialog != null) {
                UiExecutor.runOnEdt(() -> dialog.setVisible(true));
            }
            UiExecutor.runAsync(() -> {
                try {
                    List<MethodResult> rL = MainForm.getEngine().getCallers(className, finalMethodName, null);
                    List<MethodResult> eL = MainForm.getEngine().getCallee(className, finalMethodName, null);

                    DefaultListModel<MethodResult> calleeData = new DefaultListModel<>();
                    DefaultListModel<MethodResult> callerData = new DefaultListModel<>();

                    UiExecutor.runOnEdt(() -> {
                        for (MethodResult mr : rL) {
                            callerData.addElement(mr);
                        }
                        for (MethodResult mr : eL) {
                            calleeData.addElement(mr);
                        }
                        MainForm.getInstance().getCalleeList().setModel(calleeData);
                        MainForm.getInstance().getCallerList().setModel(callerData);
                        MainForm.getInstance().getTabbedPanel().setSelectedIndex(2);
                    });
                } finally {
                    if (dialog != null) {
                        UiExecutor.runOnEdt(dialog::dispose);
                    }
                }
            });
        });
        popupMenu.add(searchCallItem);

        JMenuItem classItem = new JMenuItem("SEARCH CLASS FROM JARS");
        classItem.setIcon(IconManager.pubIcon);
        classItem.addActionListener(e -> {
            String className = rArea.getSelectedText();

            if (className == null) {
                JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                        "SELECTED STRING IS NULL");
                return;
            }

            className = className.trim();

            if (className.isEmpty()) {
                JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                        "SELECTED STRING IS NULL");
                return;
            }

            fileTreeSearchPanel.setVisible(true);

            fileTreeSearchTextField.setText(className);
        });
        popupMenu.add(classItem);

        JMenuItem openItem = new JMenuItem("OPEN IN EXPLORER");
        openItem.setIcon(IconManager.fileIcon);
        openItem.addActionListener(e -> OpenUtil.openCurrent());
        popupMenu.add(openItem);

        JMenuItem luceneItem = new JMenuItem("OPEN GLOBAL SEARCH");
        luceneItem.setIcon(IconManager.luceneIcon);
        luceneItem.addActionListener(e -> LuceneSearchForm.start(1));
        popupMenu.add(luceneItem);

        rArea.setPopupMenu(popupMenu);
    }
}
