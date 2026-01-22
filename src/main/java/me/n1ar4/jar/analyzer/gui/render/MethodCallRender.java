/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.render;

import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.entity.MethodResultEdge;
import me.n1ar4.jar.analyzer.utils.ASMUtil;

import javax.swing.*;
import java.awt.*;

public class MethodCallRender extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof MethodResult) {
            MethodResult result = (MethodResult) value;
            String className = result.getClassName().replace("/", ".");
            className = "<font style=\"color: orange; font-weight: bold;\">" + className + "</font>";
            String m = ASMUtil.convertMethodDesc(result.getMethodName(), result.getMethodDesc());
            String edgeInfo = "";
            String edgeTip = null;
            if (result instanceof MethodResultEdge) {
                MethodResultEdge edge = (MethodResultEdge) result;
                String type = edge.getEdgeType();
                String conf = edge.getEdgeConfidence();
                if (type != null || conf != null) {
                    String typeText = type == null || type.isEmpty() ? "direct" : type;
                    String confText = conf == null || conf.isEmpty() ? "high" : conf;
                    edgeInfo = " <font style=\"color: #888888;\">[" + typeText + "/" + confText + "]</font>";
                }
                String evidence = edge.getEdgeEvidence();
                if (evidence != null && !evidence.trim().isEmpty()) {
                    edgeTip = evidence;
                }
            }
            setText("<html>" + className + "   " + m + edgeInfo + "</html>");
            setToolTipText(edgeTip);
        } else {
            return null;
        }
        return component;
    }
}
