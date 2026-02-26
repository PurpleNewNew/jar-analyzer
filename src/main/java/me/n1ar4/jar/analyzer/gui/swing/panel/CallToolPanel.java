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
import me.n1ar4.jar.analyzer.gui.swing.SwingResultHtml;
import me.n1ar4.jar.analyzer.gui.swing.SwingUiApplyGuard;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
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
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
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
    private final JComboBox<ScopeItem> scopeBox = new JComboBox<>(ScopeItem.defaultItems());
    private final SwingUiApplyGuard.Throttle snapshotThrottle = new SwingUiApplyGuard.Throttle();
    private static final List<String> EMPTY_TOKENS = List.of();
    private boolean scopeUpdating = false;

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
        scopeBox.setSelectedIndex(0);
        scopeBox.addActionListener(e -> {
            if (scopeUpdating) {
                return;
            }
            ScopeItem selected = (ScopeItem) scopeBox.getSelectedItem();
            if (selected == null) {
                return;
            }
            RuntimeFacades.callGraph().setScope(selected.value());
            RuntimeFacades.callGraph().refreshCurrentContext();
        });
        actionPanel.add(refresh);
        actionPanel.add(openAll);
        actionPanel.add(openCaller);
        actionPanel.add(openCallee);
        actionPanel.add(new JLabel("Scope"));
        actionPanel.add(scopeBox);

        for (JList<MethodNavDto> list : List.of(allMethodList, callerList, calleeList)) {
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setCellRenderer(new MethodRenderer());
        }
        bindOpenOnEnter(allMethodList, () -> {
            int index = allMethodList.getSelectedIndex();
            if (index >= 0) {
                RuntimeFacades.callGraph().openAllMethod(index);
            }
        });
        bindOpenOnEnter(callerList, () -> {
            int index = callerList.getSelectedIndex();
            if (index >= 0) {
                RuntimeFacades.callGraph().openCaller(index);
            }
        });
        bindOpenOnEnter(calleeList, () -> {
            int index = calleeList.getSelectedIndex();
            if (index >= 0) {
                RuntimeFacades.callGraph().openCallee(index);
            }
        });

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
        if (!SwingUiApplyGuard.ensureEdt("CallToolPanel.applySnapshot", () -> applySnapshot(snapshot))) {
            return;
        }
        if (!snapshotThrottle.allow(SwingUiApplyGuard.fingerprint(snapshot))) {
            return;
        }
        jarValue.setText(safe(snapshot.currentJar()));
        classValue.setText(safe(snapshot.currentClass()));
        methodValue.setText(safe(snapshot.currentMethod()));
        syncScope(snapshot.scope());
        resetModel(allMethodModel, allMethodList, snapshot.allMethods());
        resetModel(callerModel, callerList, snapshot.callers());
        resetModel(calleeModel, calleeList, snapshot.callees());
    }

    private void syncScope(String scope) {
        String value = safe(scope).trim();
        if (value.isEmpty()) {
            return;
        }
        for (int i = 0; i < scopeBox.getItemCount(); i++) {
            ScopeItem item = scopeBox.getItemAt(i);
            if (item != null && value.equalsIgnoreCase(item.value())) {
                Object current = scopeBox.getSelectedItem();
                if (current == item) {
                    return;
                }
                scopeUpdating = true;
                try {
                    scopeBox.setSelectedIndex(i);
                } finally {
                    scopeUpdating = false;
                }
                return;
            }
        }
    }

    private static JPanel pair(String key, JLabel value) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.add(new JLabel(key));
        row.add(value);
        return row;
    }

    private static void bindOpenOnEnter(JList<MethodNavDto> list, Runnable action) {
        if (list == null || action == null) {
            return;
        }
        list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open-selected-item");
        list.getActionMap().put("open-selected-item", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
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
                setText(SwingResultHtml.renderMethodRow(
                        method.className(),
                        method.methodName(),
                        method.methodDesc(),
                        method.jarName(),
                        0,
                        EMPTY_TOKENS
                ));
                setToolTipText(method.className()
                        + "#" + method.methodName() + safe(method.methodDesc()));
            }
            return this;
        }
    }

    private record ScopeItem(String label, String value) {
        private static ScopeItem[] defaultItems() {
            return new ScopeItem[]{
                    new ScopeItem("App", "app"),
                    new ScopeItem("All", "all"),
                    new ScopeItem("Libraries", "library"),
                    new ScopeItem("SDK", "sdk"),
                    new ScopeItem("Generated", "generated"),
                    new ScopeItem("Excluded", "excluded")
            };
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
