/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.legacy.lucene;

import me.n1ar4.jar.analyzer.entity.LuceneSearchResult;
import me.n1ar4.jar.analyzer.gui.LuceneSearchForm;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LuceneSearchListener implements DocumentListener {
    private static final LuceneSearchCache GLOBAL_CACHE = new LuceneSearchCache();
    private final JTextArea textField;
    private final JList<LuceneSearchResult> resultList;
    private final DefaultListModel<LuceneSearchResult> resultModel;
    private final LuceneSearchCache searchCache;
    private final AtomicInteger searchSeq;
    private final Timer searchTimer;
    private volatile String pendingText;

    private void runSearch() {
        String text = pendingText;
        int seq = searchSeq.incrementAndGet();
        if (text == null || text.isEmpty()) {
            resultList.setModel(new DefaultListModel<>());
            return;
        }

        UiExecutor.runAsync(() -> {
            List<LuceneSearchResult> results;

            if (searchCache.containsKey(text)) {
                results = searchCache.get(text);
            } else {
                results = LuceneSearchWrapper.searchFileName(text);
                if (!LuceneSearchForm.useNoLucene()) {
                    results.addAll(LuceneSearchWrapper.searchLucene(text));
                }
                searchCache.put(text, results);
            }

            DefaultListModel<LuceneSearchResult> model = new DefaultListModel<>();
            for (LuceneSearchResult result : results) {
                if (!result.getFileName().endsWith(".class")) {
                    continue;
                }
                model.addElement(result);
            }
            SwingUtilities.invokeLater(() -> {
                if (seq != searchSeq.get()) {
                    return;
                }
                resultList.setModel(model);
            });
        });
    }

    private void scheduleSearch() {
        pendingText = textField.getText();
        if (searchTimer.isRunning()) {
            searchTimer.restart();
        } else {
            searchTimer.start();
        }
    }

    public LuceneSearchListener(JTextArea text, JList<LuceneSearchResult> res) {
        this.textField = text;
        this.resultList = res;
        this.resultModel = new DefaultListModel<>();
        this.searchCache = GLOBAL_CACHE;
        this.searchSeq = new AtomicInteger(0);
        this.searchTimer = new Timer(200, e -> runSearch());
        this.searchTimer.setRepeats(false);
        res.setModel(resultModel);
    }

    public static void clearCache() {
        GLOBAL_CACHE.clear();
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        scheduleSearch();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        scheduleSearch();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        scheduleSearch();
    }
}
