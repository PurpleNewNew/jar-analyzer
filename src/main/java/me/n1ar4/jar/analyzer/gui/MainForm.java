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

import java.awt.Component;
import java.awt.Panel;
import java.awt.Window;
import javax.swing.RootPaneContainer;

/**
 * Minimal compatibility holder kept for excluded modules
 * (shell/dbg) during the Swing runtime migration.
 */
public final class MainForm {
    private static final MainForm INSTANCE = new MainForm();
    private static volatile Window frame;
    private static volatile Component masterPanel = new Panel();

    private MainForm() {
    }

    public static MainForm getInstance() {
        return INSTANCE;
    }

    public Component getMasterPanel() {
        return masterPanel;
    }

    public static Window getFrame() {
        return frame;
    }

    public static void setFrame(Window frame) {
        MainForm.frame = frame;
        if (frame instanceof RootPaneContainer rootPaneContainer) {
            Component content = rootPaneContainer.getContentPane();
            if (content != null) {
                MainForm.masterPanel = content;
            }
        }
    }

    public static void setMasterPanel(Component panel) {
        if (panel != null) {
            masterPanel = panel;
        }
    }
}
