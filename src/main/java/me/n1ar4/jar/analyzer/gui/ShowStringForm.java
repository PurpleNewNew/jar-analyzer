/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class ShowStringForm {
    private static final int LARGE_TEXT_THRESHOLD = 100000;
    private JPanel masterPanel;
    private JScrollPane stringScroll;
    private JTextArea stringArea;
    private JButton prevBtn;
    private JButton nextBtn;
    private JPanel opPanel;
    private JLabel totalLabel;
    private JLabel curLabel;
    private int curPage;
    private int totalPage;

    public static void start(int total, ArrayList<String> list, JDialog dialog) {
        ArrayList<String> snapshot = list == null ? new ArrayList<>() : new ArrayList<>(list);
        UiExecutor.runAsync(() -> {
            long estimatedLen = estimateLength(snapshot);
            Boolean proceed = UiExecutor.callOnEdt(() -> {
                if (estimatedLen > LARGE_TEXT_THRESHOLD) {
                    int resp = JOptionPane.showConfirmDialog(MainForm.getInstance().getMasterPanel(),
                            "<html>" +
                                    "<p>你的 STRING LIST 第一页数据大于 10 0000 长度</p>" +
                                    "<p>不方便展示和查看，建议您不要直接打开而是自行搜索</p>" +
                                    "<p>如果你坚持要打开可以点击 确认 按钮</p>" +
                                    "</html>",
                            "STRING TOO LARGE", JOptionPane.OK_CANCEL_OPTION);
                    return resp == JOptionPane.OK_OPTION;
                }
                return Boolean.TRUE;
            });
            if (!Boolean.TRUE.equals(proceed)) {
                UiExecutor.runOnEdt(() -> {
                    if (dialog != null) {
                        dialog.dispose();
                    }
                });
                return;
            }
            String data = buildString(snapshot);
            UiExecutor.runOnEdt(() -> {
                JFrame frame = new JFrame(Const.StringForm);
                ShowStringForm instance = new ShowStringForm();
                instance.applyData(total, data);
                frame.setContentPane(instance.masterPanel);
                if (dialog != null) {
                    dialog.dispose();
                }
                frame.pack();
                frame.setLocationRelativeTo(MainForm.getInstance().getMasterPanel());
                frame.setVisible(true);
            });
        });
    }

    private static long estimateLength(ArrayList<String> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        long len = 0;
        for (String s : list) {
            len += s == null ? 4 : s.length();
            len += 1;
        }
        return len;
    }

    private static String buildString(ArrayList<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            sb.append(s);
            sb.append("\n");
        }
        return sb.toString();
    }

    private void applyData(int total, String data) {
        stringArea.setText(data == null ? "" : data);
        stringArea.setCaretPosition(0);

        int totalPage = total / 100 + 1;
        int curPage = 1;

        totalLabel.setText(String.format("Total: %d", total));
        curLabel.setText(String.format("Page: %d/%d", curPage, totalPage));
        this.curPage = 1;
        this.totalPage = totalPage;

        nextBtn.addActionListener(e -> {
            if (this.curPage == this.totalPage) {
                JOptionPane.showMessageDialog(this.masterPanel, "you cannot do it");
                return;
            }
            UiExecutor.runAsync(() -> {
                ArrayList<String> nextList = MainForm.getEngine().getStrings(this.curPage + 1);
                String nextData = buildString(nextList);
                UiExecutor.runOnEdt(() -> {
                    stringArea.setText(nextData);
                    stringArea.setCaretPosition(0);
                    this.curPage += 1;
                    curLabel.setText(String.format("Page: %d/%d", this.curPage, this.totalPage));
                });
            });
        });

        prevBtn.addActionListener(e -> {
            if (this.curPage == 1) {
                JOptionPane.showMessageDialog(this.masterPanel, "you cannot do it");
                return;
            }
            UiExecutor.runAsync(() -> {
                ArrayList<String> nextList = MainForm.getEngine().getStrings(this.curPage - 1);
                String nextData = buildString(nextList);
                UiExecutor.runOnEdt(() -> {
                    stringArea.setText(nextData);
                    stringArea.setCaretPosition(0);
                    this.curPage -= 1;
                    curLabel.setText(String.format("Page: %d/%d", this.curPage, this.totalPage));
                });
            });
        });
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        masterPanel = new JPanel();
        masterPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        stringScroll = new JScrollPane();
        masterPanel.add(stringScroll, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(500, 600), null, 0, false));
        stringArea = new JTextArea();
        stringArea.setEditable(true);
        stringScroll.setViewportView(stringArea);
        opPanel = new JPanel();
        opPanel.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 5, 0), -1, -1));
        masterPanel.add(opPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        prevBtn = new JButton();
        prevBtn.setText("Prev");
        opPanel.add(prevBtn, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        opPanel.add(spacer1, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        nextBtn = new JButton();
        nextBtn.setText("Next");
        opPanel.add(nextBtn, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        totalLabel = new JLabel();
        totalLabel.setText("Label");
        opPanel.add(totalLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        curLabel = new JLabel();
        curLabel.setText("Label");
        opPanel.add(curLabel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return masterPanel;
    }

}
