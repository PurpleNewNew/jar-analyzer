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
import me.n1ar4.jar.analyzer.engine.DecompileEngine;
import me.n1ar4.jar.analyzer.engine.index.IndexPluginsSupport;
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.gui.LuceneSearchForm;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.util.ProcessDialog;
import me.n1ar4.jar.analyzer.gui.util.SyntaxAreaHelper;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Paths;

public class ControllerMouseAdapter extends MouseAdapter {
    @SuppressWarnings("all")
    public void mouseClicked(MouseEvent evt) {
        JList<?> list = (JList<?>) evt.getSource();
        if (evt.getClickCount() == 2) {
            int index = list.locationToIndex(evt.getPoint());

            // 2025/04/06 处理预期外报错问题
            if (index < 0 || list == null || list.getModel() == null ||
                    list.getModel().getElementAt(index) == null) {
                return;
            }

            ClassResult res = (ClassResult) list.getModel().getElementAt(index);
            String className = res.getClassName();

            UiExecutor.runAsync(() -> {
                String classPath = SyntaxAreaHelper.resolveClassPath(className);
                if (classPath == null) {
                        UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                                "<html>" +
                                        "<p>need dependency or class file not found</p>" +
                                        "<p>缺少依赖或者文件找不到（考虑加载 rt.jar 并检查你的 JAR 是否合法）</p>" +
                                        "<p>默认以三种方式找类：</p>" +
                                        "<p>1.根据类名直接从根目录找（例如 <strong>com/a/b/Demo</strong> ）</p>" +
                                        "<p>2.从 <strong>BOOT-INF</strong> 找（" +
                                        "例如 <strong>BOOT-INF/classes/com/a/Demo</strong> ）</p>" +
                                        "<p>3.从 <strong>WEB-INF</strong> 找（" +
                                        "例如 <strong>WEB-INF/classes/com/a/Demo</strong> ）<p>" +
                                        "</html>"));
                        return;
                }

                if (LuceneSearchForm.getInstance() != null && LuceneSearchForm.usePaLucene()) {
                    IndexPluginsSupport.addIndex(Paths.get(classPath).toFile());
                }
                String code = DecompileEngine.decompile(Paths.get(classPath));
                UiExecutor.runOnEdt(() -> {
                    SearchInputListener.getFileTree().searchPathTarget(className);
                    MainForm.getCodeArea().setText(code);
                    MainForm.getCodeArea().setCaretPosition(0);
                });

                JDialog dialog = UiExecutor.callOnEdt(() ->
                        ProcessDialog.createProgressDialog(MainForm.getInstance().getMasterPanel()));
                UiExecutor.runAsyncWithDialog(dialog, () -> CoreHelper.refreshAllMethods(className));

                CoreHelper.refreshSpringM(className);
                String jarName = res.getJarName();
                if (StringUtil.isNull(jarName)) {
                    jarName = MainForm.getEngine().getJarByClass(className);
                }
                String finalJarName = jarName;
                UiExecutor.runOnEdt(() -> {
                    MainForm.getInstance().getCurClassText().setText(className);
                    MainForm.setCurClass(className);
                    MainForm.getInstance().getCurJarText().setText(finalJarName);
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
