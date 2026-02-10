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

import me.n1ar4.jar.analyzer.gui.LuceneSearchForm;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.SearchForm;
import me.n1ar4.jar.analyzer.gui.legacy.lucene.LuceneSearchWrapper;

import javax.swing.JOptionPane;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class GlobalKeyListener extends KeyAdapter {
    private static final long SHIFT_DOUBLE_TAP_WINDOW_MS = 400L;
    private static boolean shiftDown = false;
    private static int shiftTapCount = 0;
    private static long lastShiftTapAt = 0L;

    private static void triggerGlobalSearch() {
        LuceneSearchWrapper.initEnvAsync();
        LuceneSearchForm.start(0);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0 ||
                (e.getModifiersEx() & KeyEvent.META_DOWN_MASK) != 0) {
            if (e.getKeyCode() == KeyEvent.VK_X) {
                if (MainForm.getCurMethod() == null) {
                    JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                            "<html>ctrl+x<br>" +
                                    "<b>you should select a method first</b></html>");
                    return;
                }
                MainForm.getInstance().getTabbedPanel().setSelectedIndex(2);
            }
        }
        if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0 ||
                (e.getModifiersEx() & KeyEvent.META_DOWN_MASK) != 0) {
            if (e.getKeyCode() == KeyEvent.VK_F) {
                SearchForm.start();
            }
        }
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            if (!shiftDown) {
                shiftDown = true;
                long now = System.currentTimeMillis();
                if (now - lastShiftTapAt <= SHIFT_DOUBLE_TAP_WINDOW_MS) {
                    shiftTapCount++;
                } else {
                    shiftTapCount = 1;
                }
                lastShiftTapAt = now;
                if (shiftTapCount >= 2) {
                    shiftTapCount = 0;
                    triggerGlobalSearch();
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            shiftDown = false;
        }
    }
}
