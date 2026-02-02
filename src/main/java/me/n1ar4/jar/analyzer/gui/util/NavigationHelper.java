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
        openMethod(res, OpenMethodOptions.defaults());
    }

    public static void openMethod(MethodResult res,
                                  OpenMethodOptions options) {
        if (res == null) {
            return;
        }
        OpenMethodOptions methodOptions = options == null ? OpenMethodOptions.defaults() : options;
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
            boolean openInNewTab = methodOptions.openInNewTab;
            boolean refreshContext = methodOptions.refreshContext;
            boolean recordState = methodOptions.recordState;
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
            SyntaxAreaHelper.OpenClassOptions classOptions = SyntaxAreaHelper.OpenClassOptions.defaults()
                    .preferExisting(true)
                    .warnOnMissing(false)
                    .openInNewTab(openInNewTab)
                    .recordState(recordState);
            boolean opened = SyntaxAreaHelper.openClassInEditor(
                    className,
                    resolved.getMethodName(),
                    resolved.getMethodDesc(),
                    classOptions);
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

    public static final class OpenMethodOptions {
        private boolean openInNewTab;
        private boolean refreshContext = true;
        private boolean recordState = true;

        private OpenMethodOptions() {
        }

        public static OpenMethodOptions defaults() {
            return new OpenMethodOptions();
        }

        public OpenMethodOptions openInNewTab(boolean value) {
            this.openInNewTab = value;
            return this;
        }

        public OpenMethodOptions refreshContext(boolean value) {
            this.refreshContext = value;
            return this;
        }

        public OpenMethodOptions recordState(boolean value) {
            this.recordState = value;
            return this;
        }
    }

    public static final class OpenClassOptions {
        private boolean openInNewTab;
        private boolean refreshContext = true;
        private boolean recordState = true;

        private OpenClassOptions() {
        }

        public static OpenClassOptions defaults() {
            return new OpenClassOptions();
        }

        public OpenClassOptions openInNewTab(boolean value) {
            this.openInNewTab = value;
            return this;
        }

        public OpenClassOptions refreshContext(boolean value) {
            this.refreshContext = value;
            return this;
        }

        public OpenClassOptions recordState(boolean value) {
            this.recordState = value;
            return this;
        }
    }

    public static void openClass(ClassResult res, boolean openInNewTab) {
        if (res == null) {
            return;
        }
        OpenClassOptions options = OpenClassOptions.defaults().openInNewTab(openInNewTab);
        openClass(res.getClassName(), res.getJarName(), options, null);
    }

    public static void openClass(String className,
                                 String jarName,
                                 boolean openInNewTab) {
        OpenClassOptions options = OpenClassOptions.defaults().openInNewTab(openInNewTab);
        openClass(className, jarName, options, null);
    }

    public static void openClass(String className,
                                 String jarName,
                                 boolean openInNewTab,
                                 String missingMessage) {
        OpenClassOptions options = OpenClassOptions.defaults().openInNewTab(openInNewTab);
        openClass(className, jarName, options, missingMessage);
    }

    public static void openClass(String className,
                                 String jarName,
                                 OpenClassOptions options) {
        openClass(className, jarName, options, null);
    }

    private static void openClass(String className,
                                  String jarName,
                                  OpenClassOptions options,
                                  String missingMessage) {
        if (className == null || className.trim().isEmpty()) {
            return;
        }
        OpenClassOptions resolved = options == null ? OpenClassOptions.defaults() : options;
        UiExecutor.runAsync(() -> {
            String classPath = SyntaxAreaHelper.resolveClassPath(className);
            if (classPath == null || !Files.exists(Paths.get(classPath))) {
                showMissingClassMessage(missingMessage);
                return;
            }
            boolean openInNewTab = resolved.openInNewTab;
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
            SyntaxAreaHelper.OpenClassOptions classOptions = SyntaxAreaHelper.OpenClassOptions.defaults()
                    .preferExisting(true)
                    .warnOnMissing(false)
                    .openInNewTab(openInNewTab)
                    .recordState(resolved.recordState);
            boolean opened = SyntaxAreaHelper.openClassInEditor(
                    className,
                    null,
                    null,
                    classOptions);
            if (!opened) {
                disposeDialog(dialog);
                return;
            }
            resetMethodContext();
            if (resolved.refreshContext) {
                CoreHelper.refreshAllMethods(className);
            }
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
