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

import me.n1ar4.jar.analyzer.core.FinderRunner;
import me.n1ar4.jar.analyzer.engine.CoreHelper;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.state.State;
import me.n1ar4.jar.analyzer.gui.util.DecompileSelector;
import me.n1ar4.jar.analyzer.gui.util.ProcessDialog;
import me.n1ar4.jar.analyzer.gui.util.SyntaxAreaHelper;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;
import me.n1ar4.jar.analyzer.utils.StringUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Paths;

public class FavMouseAdapter extends MouseAdapter {
    private static final Logger logger = LogManager.getLogger();

    @SuppressWarnings("all")
    public void mouseClicked(MouseEvent evt) {
        JList<?> list = (JList<?>) evt.getSource();
        // ??????
        if (evt.getClickCount() == 2) {
            int index = list.locationToIndex(evt.getPoint());
            MethodResult res = null;
            try {
                res = (MethodResult) list.getModel().getElementAt(index);
            } catch (Exception ignored) {
            }
            if (res == null) {
                return;
            }
            String className = res.getClassName();
            MethodResult finalRes = res;

            UiExecutor.runAsync(() -> {
                String classPath = SyntaxAreaHelper.resolveClassPath(className);
                if (classPath == null) {
                        UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                                "<html>" +
                                        "<p>need dependency or class file not found</p>" +
                                        "<p>???????????????????????????? rt.jar ???????? JAR ???????</p>" +
                                        "<p>???????????????</p>" +
                                        "<p>1.?????????????????????? <strong>com/a/b/Demo</strong> ??</p>" +
                                        "<p>2.?? <strong>BOOT-INF</strong> ???" +
                                        "???? <strong>BOOT-INF/classes/com/a/Demo</strong> ??</p>" +
                                        "<p>3.?? <strong>WEB-INF</strong> ???" +
                                        "???? <strong>WEB-INF/classes/com/a/Demo</strong> ??<p>" +
                                        "</html>"));
                        return;
                }

                String code = DecompileSelector.decompile(Paths.get(classPath));
                String methodName = finalRes.getMethodName();
                int pos = FinderRunner.find(code, methodName, finalRes.getMethodDesc());
                UiExecutor.runOnEdt(() -> {
                    SearchInputListener.getFileTree().searchPathTarget(className);
                    MainForm.getCodeArea().setText(code);
                    MainForm.getCodeArea().setCaretPosition(pos + 1);
                });

                String jarName = finalRes.getJarName();
                if (StringUtil.isNull(jarName)) {
                    jarName = MainForm.getEngine().getJarByClass(className);
                }
                String finalJarName = jarName;
                String finalClassPath = classPath;
                UiExecutor.runOnEdt(() -> {
                    MainForm.getInstance().getCurClassText().setText(className);
                    MainForm.setCurClass(className);
                    MainForm.getInstance().getCurJarText().setText(finalJarName);
                    MainForm.getInstance().getCurMethodText().setText(finalRes.getMethodName());
                    finalRes.setClassPath(Paths.get(finalClassPath));
                    MainForm.setCurMethod(finalRes);

                    State newState = new State();
                    newState.setClassPath(Paths.get(finalClassPath));
                    newState.setJarName(finalJarName);
                    newState.setClassName(finalRes.getClassName());
                    newState.setMethodDesc(finalRes.getMethodDesc());
                    newState.setMethodName(finalRes.getMethodName());

                    int curSI = MainForm.getCurStateIndex();
                    if (curSI == -1) {
                        MainForm.getStateList().add(curSI + 1, newState);
                        MainForm.setCurStateIndex(curSI + 1);
                    } else {
                        if (curSI >= MainForm.getStateList().size()) {
                            curSI = MainForm.getStateList().size() - 1;
                        }
                        State state = MainForm.getStateList().get(curSI);
                        if (state != null) {
                            int a = MainForm.getStateList().size();
                            MainForm.getStateList().add(curSI + 1, newState);
                            int b = MainForm.getStateList().size();
                            if (a == b) {
                                MainForm.setCurStateIndex(curSI);
                            } else {
                                MainForm.setCurStateIndex(curSI + 1);
                            }
                        } else {
                            logger.warn("current state is null");
                        }
                    }
                    JDialog dialog = ProcessDialog.createProgressDialog(MainForm.getInstance().getMasterPanel());
                    CoreHelper.refreshMethodContextAsync(className,
                            finalRes.getMethodName(), finalRes.getMethodDesc(), dialog);
                });
            });
} else if (SwingUtilities.isRightMouseButton(evt)) {
            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem cleanAllFavorite = new JMenuItem("clean all favorite");
            JMenuItem cleanCurItems = new JMenuItem("clean this favorite");
            JMenuItem sendToSink = new JMenuItem("send to chains sink");
            JMenuItem sendToSource = new JMenuItem("send to chains source");
            popupMenu.add(cleanAllFavorite);
            popupMenu.add(cleanCurItems);
            popupMenu.add(sendToSink);
            popupMenu.add(sendToSource);
            cleanAllFavorite.addActionListener(e -> {
                MainForm.getFavData().clear();
                JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                        "CLEAN ALL FAVORIATES FINISH");
                UiExecutor.runAsync(() -> MainForm.getEngine().cleanFav());
            });
            cleanCurItems.addActionListener(e -> {
                MethodResult selectedItem = (MethodResult) list.getSelectedValue();
                if (MainForm.getFavData().removeElement(selectedItem)) {
                    JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                            "CLEAN FAVORIATE " + selectedItem.getMethodName() + " FINISH");
                    UiExecutor.runAsync(() -> MainForm.getEngine().cleanFavItem(selectedItem));
                }
            });
            sendToSink.addActionListener(e -> {
                MethodResult selectedItem = (MethodResult) list.getSelectedValue();
                MainForm.getInstance().setSink(
                        selectedItem.getClassName(), selectedItem.getMethodName(), selectedItem.getMethodDesc());
                JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                        "SEND SINK " + selectedItem.getMethodName() + " FINISH");
            });
            sendToSource.addActionListener(e -> {
                MethodResult selectedItem = (MethodResult) list.getSelectedValue();
                MainForm.getInstance().setSource(
                        selectedItem.getClassName(), selectedItem.getMethodName(), selectedItem.getMethodDesc());
                JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                        "SEND SOURCE " + selectedItem.getMethodName() + " FINISH");
            });
            int index = list.locationToIndex(evt.getPoint());
            list.setSelectedIndex(index);
            popupMenu.show(list, evt.getX(), evt.getY());
        }
    }
}
