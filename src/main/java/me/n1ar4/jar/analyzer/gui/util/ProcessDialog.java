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

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

@SuppressWarnings("all")
public class ProcessDialog {
    public static JDialog createProgressDialog(JPanel master) {
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);

        JLabel label = new JLabel("<html>" +
                "<p>running please wait ... </p>" +
                "<p>请耐心等待任务完成 ... </p>" +
                "<p>关闭该进度条不会影响任务执行</p>" +
                "</html>");

        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 0, 10, 0);
        contentPanel.add(progressBar, gbc);
        gbc.gridy++;
        gbc.weighty = 0;
        contentPanel.add(label, gbc);

        JOptionPane optionPane = new JOptionPane(contentPanel, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION, IconManager.showIcon, new Object[]{}, null);

        Window owner = SwingUtilities.getWindowAncestor(master);
        JDialog dialog;
        if (owner instanceof Frame) {
            dialog = new JDialog((Frame) owner);
        } else if (owner instanceof Dialog) {
            dialog = new JDialog((Dialog) owner);
        } else {
            dialog = new JDialog();
        }
        dialog.setTitle("Jar Analyzer");
        dialog.setModalityType(Dialog.ModalityType.MODELESS);
        dialog.setContentPane(optionPane);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setLocationRelativeTo(master);
        if (owner != null) {
            dialog.getRootPane().putClientProperty("ownerRef", owner);
            dialog.getRootPane().putClientProperty("ownerEnabled", owner.isEnabled());
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    Object ref = dialog.getRootPane().getClientProperty("ownerRef");
                    Object enabled = dialog.getRootPane().getClientProperty("ownerEnabled");
                    if (ref instanceof Window && Boolean.TRUE.equals(enabled)) {
                        ((Window) ref).setEnabled(false);
                    }
                }

                @Override
                public void windowClosed(WindowEvent e) {
                    Object ref = dialog.getRootPane().getClientProperty("ownerRef");
                    Object enabled = dialog.getRootPane().getClientProperty("ownerEnabled");
                    if (ref instanceof Window && Boolean.TRUE.equals(enabled)) {
                        Window win = (Window) ref;
                        boolean hasVisibleChild = false;
                        for (Window child : win.getOwnedWindows()) {
                            if (child.isShowing()) {
                                hasVisibleChild = true;
                                break;
                            }
                        }
                        if (!hasVisibleChild) {
                            win.setEnabled(true);
                        }
                    }
                }
            });
        }
        return dialog;
    }
}
