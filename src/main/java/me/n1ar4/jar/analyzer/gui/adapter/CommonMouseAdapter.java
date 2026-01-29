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
import me.n1ar4.jar.analyzer.engine.index.IndexPluginsSupport;
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.gui.LuceneSearchForm;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.PreviewForm;
import me.n1ar4.jar.analyzer.gui.state.State;
import me.n1ar4.jar.analyzer.gui.util.DecompileSelector;
import me.n1ar4.jar.analyzer.gui.util.ProcessDialog;
import me.n1ar4.jar.analyzer.gui.util.SyntaxAreaHelper;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;
import me.n1ar4.jar.analyzer.utils.StringUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CommonMouseAdapter extends MouseAdapter {
    private static final Logger logger = LogManager.getLogger();

    private static JFrame frameIns = null;

    @SuppressWarnings("all")
    public void mouseClicked(MouseEvent evt) {
        if (frameIns != null) {
            frameIns.dispose();
        }
        JList<?> list = (JList<?>) evt.getSource();
        // 左键双击
        if (evt.getClickCount() == 2) {
            int index = list.locationToIndex(evt.getPoint());
            if (!isValidIndex(list, index, evt.getPoint())) {
                return;
            }
            MethodResult res = null;
            try {
                res = (MethodResult) list.getModel().getElementAt(index);
            } catch (Exception ignored) {
            }
            if (res == null) {
                return;
            }
            openMethodResult(res);
        } else if (SwingUtilities.isRightMouseButton(evt)) {
            int index = list.locationToIndex(evt.getPoint());
            if (!isValidIndex(list, index, evt.getPoint())) {
                return;
            }
            list.setSelectedIndex(index);
            JPopupMenu popupMenu = new JPopupMenu();

            JMenuItem addToFavorite = new JMenuItem("add to favorite");
            popupMenu.add(addToFavorite);
            addToFavorite.addActionListener(e -> {
                MethodResult selectedItem = (MethodResult) list.getSelectedValue();
                if (selectedItem == null) {
                    JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                            "SELECTED METHOD IS NULL");
                    return;
                }
                addToFavoriteIfMissing(selectedItem);
            });

            JMenuItem sendToSink = new JMenuItem("send to chains sink");
            popupMenu.add(sendToSink);
            sendToSink.addActionListener(e -> {
                MethodResult selectedItem = (MethodResult) list.getSelectedValue();
                if (selectedItem == null) {
                    JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                            "SELECTED METHOD IS NULL");
                    return;
                }
                addToFavoriteIfMissing(selectedItem);
                MainForm.getInstance().setSink(
                        selectedItem.getClassName(), selectedItem.getMethodName(), selectedItem.getMethodDesc());
                switchToChainsTab();
                JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                        "SEND SINK " + selectedItem.getMethodName() + " FINISH");
            });

            JMenuItem sendToSource = new JMenuItem("send to chains source");
            popupMenu.add(sendToSource);
            sendToSource.addActionListener(e -> {
                MethodResult selectedItem = (MethodResult) list.getSelectedValue();
                if (selectedItem == null) {
                    JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                            "SELECTED METHOD IS NULL");
                    return;
                }
                addToFavoriteIfMissing(selectedItem);
                MainForm.getInstance().setSource(
                        selectedItem.getClassName(), selectedItem.getMethodName(), selectedItem.getMethodDesc());
                switchToChainsTab();
                JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                        "SEND SOURCE " + selectedItem.getMethodName() + " FINISH");
            });

            JMenuItem findUsages = new JMenuItem("find usages (approx)");
            popupMenu.add(findUsages);
            findUsages.addActionListener(e -> {
                MethodResult selectedItem = (MethodResult) list.getSelectedValue();
                if (selectedItem == null) {
                    JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                            "SELECTED METHOD IS NULL");
                    return;
                }
                JDialog dialog = ProcessDialog.createProgressDialog(MainForm.getInstance().getMasterPanel());
                UiExecutor.runOnEdt(() -> dialog.setVisible(true));
                UiExecutor.runAsync(() -> CoreHelper.refreshFindUsagesApprox(
                        selectedItem.getClassName(),
                        selectedItem.getMethodName(),
                        selectedItem.getMethodDesc(),
                        dialog));
            });

            JMenuItem goToImpl = new JMenuItem("go to implementation");
            popupMenu.add(goToImpl);
            goToImpl.addActionListener(e -> {
                MethodResult selectedItem = (MethodResult) list.getSelectedValue();
                if (selectedItem == null) {
                    JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                            "SELECTED METHOD IS NULL");
                    return;
                }
                UiExecutor.runAsync(() -> {
                    List<MethodResult> impls = MainForm.getEngine().getImpls(
                            selectedItem.getClassName(),
                            selectedItem.getMethodName(),
                            selectedItem.getMethodDesc());
                    UiExecutor.runOnEdt(() -> {
                        MethodResult picked = pickFromList(impls, "Select Implementation");
                        if (picked == null) {
                            JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                                    "result is null");
                            return;
                        }
                        openMethodResult(picked);
                    });
                });
            });

            JMenuItem goToSuper = new JMenuItem("go to super method");
            popupMenu.add(goToSuper);
            goToSuper.addActionListener(e -> {
                MethodResult selectedItem = (MethodResult) list.getSelectedValue();
                if (selectedItem == null) {
                    JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                            "SELECTED METHOD IS NULL");
                    return;
                }
                UiExecutor.runAsync(() -> {
                    List<MethodResult> supers = MainForm.getEngine().getSuperImpls(
                            selectedItem.getClassName(),
                            selectedItem.getMethodName(),
                            selectedItem.getMethodDesc());
                    UiExecutor.runOnEdt(() -> {
                        MethodResult picked = pickFromList(supers, "Select Super Method");
                        if (picked == null) {
                            JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                                    "result is null");
                            return;
                        }
                        openMethodResult(picked);
                    });
                });
            });

            JMenuItem copyThis = new JMenuItem("copy this");
            popupMenu.add(copyThis);
            copyThis.addActionListener(e -> {
                MethodResult selectedItem = (MethodResult) list.getSelectedValue();
                if (selectedItem == null) {
                    JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                            "SELECTED METHOD IS NULL");
                    return;
                }
                StringSelection stringSelection = new StringSelection(selectedItem.getCopyString());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
                JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(), "COPY OK");
            });

            JMenuItem copyAll = new JMenuItem("copy all");
            popupMenu.add(copyAll);
            copyAll.addActionListener(e -> {
                ListModel<?> all = list.getModel();
                if (all.getSize() == 0) {
                    return;
                }
                java.util.List<String> snapshot = UiExecutor.callOnEdt(() -> {
                    java.util.List<String> items = new java.util.ArrayList<>(all.getSize());
                    for (int i = 0; i < all.getSize(); i++) {
                        MethodResult mr = (MethodResult) all.getElementAt(i);
                        items.add(mr.getCopyString());
                    }
                    return items;
                });
                if (snapshot == null || snapshot.isEmpty()) {
                    return;
                }
                UiExecutor.runAsync(() -> {
                    StringBuilder sb = new StringBuilder();
                    for (String item : snapshot) {
                        sb.append(item);
                        sb.append("\n");
                    }
                    StringSelection stringSelection = new StringSelection(sb.toString());
                    UiExecutor.runOnEdt(() -> {
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(stringSelection, null);
                        JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(), "COPY OK");
                    });
                });
            });

            JMenuItem previewItem = new JMenuItem("预览 / preview");
            popupMenu.add(previewItem);
            previewItem.addActionListener(e -> {
                MethodResult selectedItem = (MethodResult) list.getSelectedValue();
                if (selectedItem == null) {
                    JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                            "SELECTED METHOD IS NULL");
                    return;
                }
                String className = selectedItem.getClassName();
                MethodResult finalSelected = selectedItem;
                UiExecutor.runAsync(() -> {
                    String classPath = SyntaxAreaHelper.resolveClassPath(className);
                    if (classPath == null) {
                        UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(
                                MainForm.getInstance().getMasterPanel(),
                                "<html><p>need dependency or class file not found</p></html>"));
                        return;
                    }
                    String code = DecompileSelector.decompile(Paths.get(classPath));
                    String methodName = finalSelected.getMethodName();
                    int pos = FinderRunner.find(code, methodName, finalSelected.getMethodDesc());
                    UiExecutor.runOnEdt(() -> {
                        if (code == null || code.isEmpty()) {
                            JOptionPane.showMessageDialog(null, "Preview failed: decompile error");
                            return;
                        }
                        frameIns = PreviewForm.start(code, pos);
                    });
                });
            });

            JMenuItem clearHis = new JMenuItem("清除历史 / clear history");
            popupMenu.add(clearHis);
            clearHis.addActionListener(e -> {
                UiExecutor.runAsync(() -> MainForm.getEngine().cleanHistory());
                MainForm.getInstance().getHistoryListData().clear();
                MainForm.getInstance().getHistoryList().revalidate();
                MainForm.getInstance().getHistoryList().repaint();
                JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(), "CLEAR OK");
            });
            popupMenu.show(list, evt.getX(), evt.getY());
        }
    }

    private static void openMethodResult(MethodResult res) {
        if (res == null) {
            return;
        }
        UiExecutor.runAsync(() -> {
            MethodResult resolved = resolveMethodResult(res);
            if (resolved == null) {
                return;
            }
            String className = resolved.getClassName();
            String classPath = SyntaxAreaHelper.resolveClassPath(className);
            if (classPath == null || !Files.exists(Paths.get(classPath))) {
                UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(
                        MainForm.getInstance().getMasterPanel(),
                        "<html><p>need dependency or class file not found</p></html>"));
                return;
            }
            String reuseCode = UiExecutor.callOnEdt(() -> {
                String curClass = MainForm.getCurClass();
                if (curClass != null && curClass.equals(className)) {
                    String existing = MainForm.getCodeArea().getText();
                    if (existing != null && !existing.trim().isEmpty() && looksLikeJava(existing)) {
                        return existing;
                    }
                }
                return null;
            });
            String code = reuseCode;
            if (code == null) {
                if (LuceneSearchForm.getInstance() != null && LuceneSearchForm.usePaLucene()) {
                    IndexPluginsSupport.addIndex(Paths.get(classPath).toFile());
                }
                code = DecompileSelector.decompile(Paths.get(classPath));
            }
            if (code == null) {
                return;
            }
            String methodName = resolved.getMethodName();
            int pos = FinderRunner.find(code, methodName, resolved.getMethodDesc());
            int caretPos = Math.max(0, pos + 1);
            String jarName = resolved.getJarName();
            if (StringUtil.isNull(jarName)) {
                jarName = MainForm.getEngine().getJarByClass(className);
            }
            State newState = new State();
            newState.setClassPath(Paths.get(classPath));
            newState.setJarName(jarName);
            newState.setClassName(resolved.getClassName());
            newState.setMethodDesc(resolved.getMethodDesc());
            newState.setMethodName(resolved.getMethodName());
            String finalCode = code;
            String finalClassName = className;
            String finalJarName = jarName;
            MethodResult finalRes = resolved;
            int finalCaretPos = caretPos;
            UiExecutor.runOnEdt(() -> {
                SearchInputListener.getFileTree().searchPathTarget(finalClassName);
                MainForm.getCodeArea().setText(finalCode);
                MainForm.getCodeArea().setCaretPosition(finalCaretPos);
                MainForm.getInstance().getCurClassText().setText(finalClassName);
                MainForm.setCurClass(finalClassName);
                MainForm.getInstance().getCurJarText().setText(finalJarName);
                MainForm.getInstance().getCurMethodText().setText(finalRes.getMethodName());
                finalRes.setClassPath(Paths.get(classPath));
                MainForm.setCurMethod(finalRes);
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
            });
            JDialog dialog = UiExecutor.callOnEdt(() ->
                    ProcessDialog.createProgressDialog(MainForm.getInstance().getMasterPanel()));
            CoreHelper.refreshMethodContextAsync(finalClassName, finalRes.getMethodName(), finalRes.getMethodDesc(), dialog);
        });
    }

    private static MethodResult resolveMethodResult(MethodResult res) {
        ClassResult nowClass = MainForm.getEngine().getClassByClass(res.getClassName());
        MethodResult current = res;
        while (nowClass != null) {
            ArrayList<MethodResult> method = MainForm.getEngine().getMethod(
                    nowClass.getClassName(),
                    current.getMethodName(),
                    current.getMethodDesc());
            if (!method.isEmpty()) {
                current = method.get(0);
                logger.debug("find target method in class: {}", nowClass.getClassName());
                break;
            }
            nowClass = MainForm.getEngine().getClassByClass(nowClass.getSuperClassName());
        }
        return current;
    }

    private static MethodResult pickFromList(List<MethodResult> list, String title) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        String[] options = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            MethodResult mr = list.get(i);
            StringBuilder label = new StringBuilder();
            label.append(mr.getClassName()).append("#").append(mr.getMethodName());
            if (mr.getMethodDesc() != null) {
                label.append(mr.getMethodDesc());
            }
            String jar = mr.getJarName();
            if (jar != null && !jar.trim().isEmpty()) {
                label.append(" [").append(jar.trim()).append("]");
            }
            options[i] = label.toString();
        }
        Object choice = JOptionPane.showInputDialog(
                MainForm.getInstance().getMasterPanel(),
                "select method",
                title,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]);
        if (choice == null) {
            return null;
        }
        String selected = choice.toString();
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(selected)) {
                return list.get(i);
            }
        }
        return list.get(0);
    }

    private static void runOnEdt(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    private static boolean isValidIndex(JList<?> list, int index, Point point) {
        if (index < 0) {
            return false;
        }
        Rectangle bounds = list.getCellBounds(index, index);
        return bounds != null && bounds.contains(point);
    }

    private static void switchToChainsTab() {
        JTabbedPane tabbed = MainForm.getInstance().getTabbedPanel();
        int idx = tabbed.indexOfTab("chains");
        if (idx >= 0) {
            tabbed.setSelectedIndex(idx);
        }
    }

    private static void addToFavoriteIfMissing(MethodResult selectedItem) {
        if (selectedItem == null) {
            return;
        }
        DefaultListModel<MethodResult> favData = MainForm.getFavData();
        if (favData == null) {
            return;
        }
        if (!containsMethod(favData, selectedItem)) {
            favData.addElement(selectedItem);
            if (MainForm.getEngine() != null) {
                UiExecutor.runAsync(() -> MainForm.getEngine().addFav(selectedItem));
            }
        }
    }

    private static boolean containsMethod(DefaultListModel<MethodResult> model, MethodResult target) {
        if (model == null || target == null) {
            return false;
        }
        for (int i = 0; i < model.size(); i++) {
            MethodResult item = model.getElementAt(i);
            if (sameMethod(item, target)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameMethod(MethodResult a, MethodResult b) {
        if (a == null || b == null) {
            return false;
        }
        return safeEquals(a.getClassName(), b.getClassName())
                && safeEquals(a.getMethodName(), b.getMethodName())
                && safeEquals(a.getMethodDesc(), b.getMethodDesc());
    }

    private static boolean safeEquals(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    private static boolean looksLikeJava(String text) {
        String t = text.trim();
        if (t.isEmpty()) {
            return false;
        }
        if (t.contains("class ") || t.contains("interface ") || t.contains("enum ")) {
            return true;
        }
        return t.contains("package ") && t.contains(";");
    }
}
