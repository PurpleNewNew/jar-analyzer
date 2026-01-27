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

import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.ShowStringForm;
import me.n1ar4.jar.analyzer.gui.util.ProcessDialog;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;

import javax.swing.*;
import java.util.ArrayList;

public class ShowStringAction {
    public static void run() {
        JButton showString = MainForm.getInstance().getShowStringListButton();
        showString.addActionListener(e -> {
            if (MainForm.getEngine() == null || !MainForm.getEngine().isEnabled()) {
                JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                        "please start engine first");
                return;
            }

            // 2025/06/26 optimize ALL STRING list display
            UiExecutor.runAsync(() -> {
                int allStringSize = MainForm.getEngine().getStringCount();
                if (allStringSize > 1000) {
                    String msg = "Too many strings to display. Use SEARCH for precise queries.";
                    UiExecutor.runOnEdt(() -> {
                        JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(), msg);
                        MainForm.getInstance().getTabbedPanel().setSelectedIndex(1);
                    });
                    return;
                }
                String msg = "This will display the string list. Use SEARCH for precise queries.";
                UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(
                        MainForm.getInstance().getMasterPanel(), msg));

                JDialog dialog = UiExecutor.callOnEdt(() ->
                        ProcessDialog.createProgressDialog(MainForm.getInstance().getMasterPanel()));
                UiExecutor.runOnEdt(() -> dialog.setVisible(true));
                ArrayList<String> stringList = MainForm.getEngine().getStrings(1);
                int total = MainForm.getEngine().getStringCount();
                UiExecutor.runOnEdt(() -> ShowStringForm.start(total, stringList, dialog));
            });
        });
    }
}
