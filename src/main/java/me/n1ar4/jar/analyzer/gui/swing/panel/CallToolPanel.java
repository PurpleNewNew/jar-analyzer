/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.swing.panel;

import me.n1ar4.jar.analyzer.gui.runtime.api.RuntimeFacades;
import me.n1ar4.jar.analyzer.gui.runtime.model.CallGraphSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.MethodNavDto;
import me.n1ar4.jar.analyzer.gui.swing.SwingI18n;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public final class CallToolPanel extends JPanel {
    private final JLabel jarValue = new JLabel("-");
    private final JLabel classValue = new JLabel("-");
    private final JLabel methodValue = new JLabel("-");

    private final DefaultListModel<MethodNavDto> allMethodModel = new DefaultListModel<>();
    private final DefaultListModel<MethodNavDto> callerModel = new DefaultListModel<>();
    private final DefaultListModel<MethodNavDto> calleeModel = new DefaultListModel<>();

    private final JList<MethodNavDto> allMethodList = new JList<>(allMethodModel);
    private final JList<MethodNavDto> callerList = new JList<>(callerModel);
    private final JList<MethodNavDto> calleeList = new JList<>(calleeModel);

    public CallToolPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        initUi();
    }

    private void initUi() {
        JPanel context = new JPanel(new GridLayout(3, 1, 2, 2));
        context.setBorder(BorderFactory.createTitledBorder("Current Context"));
        context.add(pair("jar", jarValue));
        context.add(pair("class", classValue));
        context.add(pair("method", methodValue));

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> RuntimeFacades.callGraph().refreshCurrentContext());
        JButton openAll = new JButton("Open All");
        openAll.addActionListener(e -> {
            int index = allMethodList.getSelectedIndex();
            if (index >= 0) {
                RuntimeFacades.callGraph().openAllMethod(index);
            }
        });
        JButton openCaller = new JButton("Open Caller");
        openCaller.addActionListener(e -> {
            int index = callerList.getSelectedIndex();
            if (index >= 0) {
                RuntimeFacades.callGraph().openCaller(index);
            }
        });
        JButton openCallee = new JButton("Open Callee");
        openCallee.addActionListener(e -> {
            int index = calleeList.getSelectedIndex();
            if (index >= 0) {
                RuntimeFacades.callGraph().openCallee(index);
            }
        });
        actionPanel.add(refresh);
        actionPanel.add(openAll);
        actionPanel.add(openCaller);
        actionPanel.add(openCallee);

        for (JList<MethodNavDto> list : List.of(allMethodList, callerList, calleeList)) {
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setCellRenderer(new MethodRenderer());
        }

        allMethodList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    int index = allMethodList.getSelectedIndex();
                    if (index >= 0) {
                        RuntimeFacades.callGraph().openAllMethod(index);
                    }
                }
            }
        });
        callerList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    int index = callerList.getSelectedIndex();
                    if (index >= 0) {
                        RuntimeFacades.callGraph().openCaller(index);
                    }
                }
            }
        });
        calleeList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    int index = calleeList.getSelectedIndex();
                    if (index >= 0) {
                        RuntimeFacades.callGraph().openCallee(index);
                    }
                }
            }
        });

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("all methods", new JScrollPane(allMethodList));
        tabs.addTab("caller", new JScrollPane(callerList));
        tabs.addTab("callee", new JScrollPane(calleeList));

        add(context, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);
        applyLanguage();
    }

    public void applySnapshot(CallGraphSnapshotDto snapshot) {
        if (snapshot == null) {
            return;
        }
        jarValue.setText(safe(snapshot.currentJar()));
        classValue.setText(safe(snapshot.currentClass()));
        methodValue.setText(safe(snapshot.currentMethod()));
        resetModel(allMethodModel, allMethodList, snapshot.allMethods());
        resetModel(callerModel, callerList, snapshot.callers());
        resetModel(calleeModel, calleeList, snapshot.callees());
    }

    private static JPanel pair(String key, JLabel value) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.add(new JLabel(key));
        row.add(value);
        return row;
    }

    private static void resetModel(
            DefaultListModel<MethodNavDto> model,
            JList<MethodNavDto> list,
            List<MethodNavDto> values
    ) {
        int selected = list.getSelectedIndex();
        model.clear();
        if (values != null) {
            for (MethodNavDto item : values) {
                model.addElement(item);
            }
        }
        if (selected >= 0 && selected < model.getSize()) {
            list.setSelectedIndex(selected);
        }
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    public void applyLanguage() {
        SwingI18n.localizeComponentTree(this);
    }

    private static final class MethodRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
        ) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof MethodNavDto method) {
                setText(method.className()
                        + "#" + method.methodName() + method.methodDesc()
                        + " [" + method.jarName() + "]");
            }
            return this;
        }
    }
}
