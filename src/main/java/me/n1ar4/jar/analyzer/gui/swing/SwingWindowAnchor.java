/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.swing;

import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Window;

/**
 * Shared Swing window anchor used by notifier/shell/dbg modules.
 */
public final class SwingWindowAnchor {
    private static volatile Window frame;
    private static volatile Component masterComponent = new JPanel();

    private SwingWindowAnchor() {
    }

    public static Window getFrame() {
        return frame;
    }

    public static Component getMasterComponent() {
        return masterComponent;
    }

    public static void setFrame(Window frame) {
        SwingWindowAnchor.frame = frame;
    }

    public static void setMasterComponent(Component masterComponent) {
        if (masterComponent != null) {
            SwingWindowAnchor.masterComponent = masterComponent;
        }
    }
}
