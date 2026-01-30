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

import me.n1ar4.jar.analyzer.engine.CoreHelper;
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.state.State;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public final class NavigationHelper {
    private static final Logger logger = LogManager.getLogger();
    private static final long HEAVY_CLASS_FILE_BYTES = 512L * 1024L;
    private static final String MISSING_CLASS_BRIEF =
            "<html><p>need dependency or class file not found</p></html>";

    private NavigationHelper() {
    }

    public static void openMethod(MethodResult res) {
        openMethod(res, false, true, true);
    }

    public static void openMethod(MethodResult res,
                                  boolean openInNewTab,
                                  boolean refreshContext,
                                  boolean recordState) {
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
                showMissingClassMessage(MISSING_CLASS_BRIEF);
                return;
            }
            boolean alreadyLoaded = SyntaxAreaHelper.hasLoadedClass(className, !openInNewTab);
            boolean showProgress = shouldShowProgress(classPath, alreadyLoaded);
            JDialog dialog = null;
            if (showProgress) {
                dialog = UiExecutor.callOnEdt(() ->
                        ProcessDialog.createProgressDialog(MainForm.getInstance().getMasterPanel()));
                if (dialog != null) {
                    JDialog finalDialog = dialog;
                    UiExecutor.runOnEdt(() -> finalDialog.setVisible(true));
                }
            }
            boolean opened = SyntaxAreaHelper.openClassInEditor(
                    className,
                    resolved.getMethodName(),
                    resolved.getMethodDesc(),
                    true,
                    false,
                    openInNewTab,
                    recordState);
            if (!opened) {
                disposeDialog(dialog);
                return;
            }
            if (refreshContext) {
                CoreHelper.refreshMethodContextAsync(
                        className,
                        resolved.getMethodName(),
                        resolved.getMethodDesc(),
                        dialog);
            } else {
                disposeDialog(dialog);
            }
        });
    }

    public static void openMethod(State state,
                                  boolean openInNewTab,
                                  boolean refreshContext,
                                  boolean recordState) {
        if (state == null) {
            return;
        }
        MethodResult m = new MethodResult();
        m.setJarName(state.getJarName());
        m.setMethodName(state.getMethodName());
        m.setMethodDesc(state.getMethodDesc());
        m.setClassName(state.getClassName());
        m.setClassPath(state.getClassPath());
        openMethod(m, openInNewTab, refreshContext, recordState);
    }

    public static void openClass(ClassResult res, boolean openInNewTab) {
        if (res == null) {
            return;
        }
        openClass(res.getClassName(), res.getJarName(), openInNewTab, null);
    }

    public static void openClass(String className,
                                 String jarName,
                                 boolean openInNewTab) {
        openClass(className, jarName, openInNewTab, null);
    }

    public static void openClass(String className,
                                 String jarName,
                                 boolean openInNewTab,
                                 String missingMessage) {
        if (className == null || className.trim().isEmpty()) {
            return;
        }
        UiExecutor.runAsync(() -> {
            String classPath = SyntaxAreaHelper.resolveClassPath(className);
            if (classPath == null || !Files.exists(Paths.get(classPath))) {
                showMissingClassMessage(missingMessage);
                return;
            }
            boolean alreadyLoaded = SyntaxAreaHelper.hasLoadedClass(className, !openInNewTab);
            boolean showProgress = shouldShowProgress(classPath, alreadyLoaded);
            JDialog dialog = null;
            if (showProgress) {
                dialog = UiExecutor.callOnEdt(() ->
                        ProcessDialog.createProgressDialog(MainForm.getInstance().getMasterPanel()));
                if (dialog != null) {
                    JDialog finalDialog = dialog;
                    UiExecutor.runOnEdt(() -> finalDialog.setVisible(true));
                }
            }
            boolean opened = SyntaxAreaHelper.openClassInEditor(
                    className,
                    null,
                    null,
                    true,
                    false,
                    openInNewTab,
                    true);
            if (!opened) {
                disposeDialog(dialog);
                return;
            }
            resetMethodContext();
            CoreHelper.refreshAllMethods(className);
            if (dialog != null) {
                disposeDialog(dialog);
            }
        });
    }

    public static JDialog maybeShowProgressDialog(String className, boolean activeOnly) {
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        String classPath = SyntaxAreaHelper.resolveClassPath(className);
        if (classPath == null || !Files.exists(Paths.get(classPath))) {
            return null;
        }
        boolean alreadyLoaded = SyntaxAreaHelper.hasLoadedClass(className, activeOnly);
        if (!shouldShowProgress(classPath, alreadyLoaded)) {
            return null;
        }
        JDialog dialog = UiExecutor.callOnEdt(() ->
                ProcessDialog.createProgressDialog(MainForm.getInstance().getMasterPanel()));
        if (dialog != null) {
            UiExecutor.runOnEdt(() -> dialog.setVisible(true));
        }
        return dialog;
    }

    public static void disposeDialog(JDialog dialog) {
        if (dialog == null) {
            return;
        }
        UiExecutor.runOnEdt(dialog::dispose);
    }

    private static boolean shouldShowProgress(String classPath, boolean alreadyLoaded) {
        if (alreadyLoaded) {
            return false;
        }
        if (classPath == null || classPath.trim().isEmpty()) {
            return false;
        }
        try {
            return Files.size(Paths.get(classPath)) >= HEAVY_CLASS_FILE_BYTES;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static MethodResult resolveMethodResult(MethodResult res) {
        if (res == null) {
            return null;
        }
        if (MainForm.getEngine() == null) {
            return res;
        }
        MethodResult current = res;
        ClassResult nowClass = MainForm.getEngine().getClassByClass(res.getClassName());
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

    private static void resetMethodContext() {
        UiExecutor.runOnEdt(() -> {
            MainForm.getInstance().getCurMethodText().setText("");
            MainForm.setCurMethod(null);
            MainForm.getInstance().getMethodImplList().setModel(new DefaultListModel<>());
            MainForm.getInstance().getSuperImplList().setModel(new DefaultListModel<>());
            MainForm.getInstance().getCalleeList().setModel(new DefaultListModel<>());
            MainForm.getInstance().getCallerList().setModel(new DefaultListModel<>());
        });
    }

    private static void showMissingClassMessage(String message) {
        String finalMessage = message;
        if (finalMessage == null || finalMessage.trim().isEmpty()) {
            finalMessage = MISSING_CLASS_BRIEF;
        }
        UiExecutor.showMessage(MainForm.getInstance().getMasterPanel(), finalMessage);
    }
}
