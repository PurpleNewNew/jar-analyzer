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
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.gui.util.NavigationHelper;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ControllerMouseAdapter extends MouseAdapter {
    @SuppressWarnings("all")
    public void mouseClicked(MouseEvent evt) {
        JList<?> list = (JList<?>) evt.getSource();
        if (evt.getClickCount() == 2) {
            int index = list.locationToIndex(evt.getPoint());

            // 2025/04/06 澶勭悊棰勬湡澶栨姤閿欓棶棰?
            if (index < 0 || list == null || list.getModel() == null ||
                    list.getModel().getElementAt(index) == null) {
                return;
            }

            ClassResult res = (ClassResult) list.getModel().getElementAt(index);
            String className = res.getClassName();

            NavigationHelper.openClass(res, false);
            CoreHelper.refreshSpringM(className);
        }
    }
}
