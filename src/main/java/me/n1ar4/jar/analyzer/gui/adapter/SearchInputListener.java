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

import cn.hutool.core.util.StrUtil;
import me.n1ar4.jar.analyzer.gui.GlobalOptions;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.tree.FileTree;
import me.n1ar4.jar.analyzer.gui.util.LogUtil;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SearchInputListener implements DocumentListener {
    private static final FileTree fileTree = MainForm.getInstance().getFileTree();
    private static final JTextField fileTreeSearchTextField = MainForm.getInstance().getFileTreeSearchTextField();
    private static final JLabel fileTreeSearchLabel = MainForm.getInstance().getFileTreeSearchLabel();
    private static List<String> collect;
    private static int count = 0;
    private static boolean refresh = false;
    private static final AtomicInteger SEARCH_SEQ = new AtomicInteger(0);
    private static final Timer SEARCH_TIMER = new Timer(180, e -> runSearch());

    static {
        SEARCH_TIMER.setRepeats(false);
    }

    public static FileTree getFileTree() {
        return fileTree;
    }

    public static void refreshLabelLang() {
        if (fileTreeSearchLabel == null) {
            return;
        }
        if (collect == null || collect.isEmpty()) {
            fileTreeSearchLabel.setVisible(false);
            return;
        }
        int idx = count == 0 ? 0 : count - 1;
        if (idx < 0) {
            idx = 0;
        }
        if (idx >= collect.size()) {
            idx = collect.size() - 1;
        }
        String className = collect.get(idx);
        boolean innerClass = className.contains("$");
        String[] temp = className.split("/");
        fileTreeSearchLabel.setText(buildLabelText(idx + 1, collect.size(), innerClass, temp[temp.length - 1]));
        fileTreeSearchLabel.setToolTipText(temp[temp.length - 1]);
        fileTreeSearchLabel.setVisible(true);
    }

    public static void search(String string, boolean isInner) {
        if (!isInner) {
            if (collect == null || collect.isEmpty()) {
                return;
            }
            if (count == collect.size()) {
                count = 0;
            }
            if (count != 0 && refresh) {
                count++;
            }
            String className = collect.get(count++);
            boolean innerClass = className.contains("$");
            String[] temp = className.split("/");
            fileTree.searchPathTarget(className);
            refresh = false;
            fileTreeSearchLabel.setText(buildLabelText(count, collect.size(), innerClass, temp[temp.length - 1]));
            fileTreeSearchLabel.setToolTipText(temp[temp.length - 1]);
            return;
        }
        if (!StrUtil.isNotBlank(string)) {
            UiExecutor.runOnEdt(() -> fileTreeSearchTextField.setText(""));
            return;
        }
        scheduleSearch();
    }

    private void filterInput() {
        String text = fileTreeSearchTextField.getText();
        // check invalid chars
        if (text.contains("'") || text.contains("\"")) {
            LogUtil.warn("check your input (invalid chars)");
            UiExecutor.runOnEdt(() -> fileTreeSearchTextField.setText(""));
            return;
        }
        scheduleSearch();
    }

    private static void scheduleSearch() {
        if (SEARCH_TIMER.isRunning()) {
            SEARCH_TIMER.restart();
        } else {
            SEARCH_TIMER.start();
        }
    }

    private static void runSearch() {
        String keyword = fileTreeSearchTextField.getText();
        if (!StrUtil.isNotBlank(keyword)) {
            if (fileTreeSearchLabel != null) {
                fileTreeSearchLabel.setToolTipText(null);
                fileTreeSearchLabel.setVisible(false);
            }
            collect = Collections.emptyList();
            return;
        }
        int seq = SEARCH_SEQ.incrementAndGet();
        UiExecutor.runAsync(() -> {
            List<String> results = queryClasses(keyword);
            UiExecutor.runOnEdt(() -> {
                if (seq != SEARCH_SEQ.get()) {
                    return;
                }
                applySearchResults(results);
            });
        });
    }

    private static List<String> queryClasses(String keyword) {
        if (MainForm.getEngine() == null) {
            return Collections.emptyList();
        }
        return MainForm.getEngine().includeClassByClassName(keyword, false);
    }

    private static void applySearchResults(List<String> results) {
        if (fileTreeSearchLabel == null) {
            return;
        }
        collect = results == null ? Collections.emptyList() : results;
        count = 0;
        refresh = true;
        if (collect.isEmpty()) {
            fileTreeSearchLabel.setToolTipText(null);
            fileTreeSearchLabel.setVisible(false);
            return;
        }
        String className = collect.get(0);
        boolean innerClass = className.contains("$");
        String[] temp = className.split("/");
        fileTree.searchPathTarget(className);
        fileTreeSearchLabel.setText(buildLabelText(1, collect.size(), innerClass, temp[temp.length - 1]));
        fileTreeSearchLabel.setToolTipText(temp[temp.length - 1]);
        fileTreeSearchLabel.setVisible(true);
    }

    private static String buildLabelText(int index, int total, boolean innerClass, String className) {
        if (GlobalOptions.getLang() == GlobalOptions.CHINESE) {
            return StrUtil.format("<html><p> 结果: {} / {} ({}) </p>" +
                            "<p> 类: {} </p>" +
                            "</html>",
                    index, total, innerClass ? "内部类" : "普通类", className);
        }
        return StrUtil.format("<html><p> result: {} / {} ({}) </p>" +
                        "<p> class: {} </p>" +
                        "</html>",
                index, total, innerClass ? "inner class" : "normal", className);
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        filterInput();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        filterInput();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        filterInput();
    }
}
