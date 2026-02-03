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
        dialog.setAutoRequestFocus(false);
        dialog.pack();
        dialog.setLocationRelativeTo(master);
        return dialog;
    }

    public static JDialog createDelayedProgressDialog(JPanel master, int delayMs) {
        JDialog dialog = createProgressDialog(master);
        if (dialog == null) {
            return null;
        }
        int delay = Math.max(0, delayMs);
        Timer timer = new Timer(delay, e -> {
            if (!dialog.isDisplayable()) {
                return;
            }
            if (!dialog.isVisible()) {
                dialog.setVisible(true);
            }
        });
        timer.setRepeats(false);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                timer.stop();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                timer.stop();
            }
        });
        timer.start();
        return dialog;
    }
}
