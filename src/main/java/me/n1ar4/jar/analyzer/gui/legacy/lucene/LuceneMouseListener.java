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

import cn.hutool.core.util.StrUtil;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.gui.legacy.engine.CoreHelper;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.entity.LuceneSearchResult;
import me.n1ar4.jar.analyzer.gui.LuceneSearchForm;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.adapter.SearchInputListener;
import me.n1ar4.jar.analyzer.gui.util.DecompileSelector;
import me.n1ar4.jar.analyzer.gui.util.ProcessDialog;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;
import me.n1ar4.jar.analyzer.utils.JarUtil;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Paths;

public class LuceneMouseListener extends MouseAdapter {
    @Override
    public void mouseClicked(MouseEvent evt) {
        JList<?> list = (JList<?>) evt.getSource();
        if (evt.getClickCount() == 2) {
            int index = list.locationToIndex(evt.getPoint());
            LuceneSearchResult res = null;
            try {
                res = (LuceneSearchResult) list.getModel().getElementAt(index);
            } catch (Exception ignored) {
            }
            if (res == null) {
                return;
            }

            String searchKey = res.getSearchKey().trim().replace("\n", "\r\n");

            String finalClassPath = res.getAbsPathStr();
            String className = JarUtil.resolveClassNameFromPath(finalClassPath);
            if (className == null || className.trim().isEmpty()) {
                return;
            }

            String finalSearchKey = searchKey;
            String finalClassName = className;
            String finalAbsPath = finalClassPath;

            UiExecutor.runAsync(() -> {
                String code = DecompileSelector.decompile(Paths.get(finalAbsPath));
                UiExecutor.runOnEdt(() -> {
                    SearchInputListener.getFileTree().searchPathTarget(finalClassName);
                    MainForm.getCodeArea().setText(code);
                    MainForm.getCodeArea().setCaretPosition(0);

                    if (StrUtil.isNotBlank(code) && StrUtil.isNotBlank(finalSearchKey)) {
                        int idx;
                        if (LuceneSearchForm.useCaseSensitive()) {
                            idx = code.indexOf(finalSearchKey);
                        } else {
                            idx = code.toLowerCase().indexOf(finalSearchKey.toLowerCase());
                        }
                        if (idx != -1) {
                            MainForm.getCodeArea().setSelectionStart(idx);
                            MainForm.getCodeArea().setSelectionEnd(idx + finalSearchKey.length());
                        }
                    }
                });
            });

            JDialog dialog = UiExecutor.callOnEdt(() ->
                    ProcessDialog.createProgressDialog(MainForm.getInstance().getMasterPanel()));
            UiExecutor.runAsyncWithDialog(dialog, () -> CoreHelper.refreshAllMethods(finalClassName));

            UiExecutor.runAsync(() -> {
                CoreHelper.refreshSpringM(finalClassName);
                CoreEngine engine = EngineContext.getEngine();
                String jarName = engine == null ? "" : engine.getJarByClass(finalClassName);
                UiExecutor.runOnEdt(() -> {
                    MainForm.getInstance().getCurClassText().setText(finalClassName);
                    MainForm.getInstance().getCurJarText().setText(jarName);
                    MainForm.getInstance().getCurMethodText().setText(null);
                    MainForm.setCurMethod(null);

                    MainForm.getInstance().getMethodImplList().setModel(new DefaultListModel<>());
                    MainForm.getInstance().getSuperImplList().setModel(new DefaultListModel<>());
                    MainForm.getInstance().getCalleeList().setModel(new DefaultListModel<>());
                    MainForm.getInstance().getCallerList().setModel(new DefaultListModel<>());
                });
            });
        }
    }
}
