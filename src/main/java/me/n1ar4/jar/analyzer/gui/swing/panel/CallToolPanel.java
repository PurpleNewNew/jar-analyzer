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
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

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
import java.util.concurrent.atomic.AtomicLong;

public final class CallToolPanel extends JPanel {
    private static final Logger logger = LogManager.getLogger();
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
    private final AtomicLong scopeChangeSeq = new AtomicLong(0L);
    private boolean scopeUpdating = false;
    private String currentClassToken = "";
    private String currentMethodToken = "";

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
        refresh.addActionListener(e -> refreshCurrentContextAsync());
        JButton openAll = new JButton("Open All");
        openAll.addActionListener(e -> openSelectedAllMethod());
        JButton openCaller = new JButton("Open Caller");
        openCaller.addActionListener(e -> openSelectedCaller());
        JButton openCallee = new JButton("Open Callee");
        openCallee.addActionListener(e -> openSelectedCallee());
        scopeBox.setSelectedIndex(0);
        scopeBox.addActionListener(e -> {
            if (scopeUpdating) {
                return;
            }
            ScopeItem selected = (ScopeItem) scopeBox.getSelectedItem();
            if (selected == null) {
                return;
            }
            long seq = scopeChangeSeq.incrementAndGet();
            runCallGraphAsync("swing-call-scope", () -> {
                if (seq != scopeChangeSeq.get()) {
                    return;
                }
                RuntimeFacades.callGraph().setScope(selected.value());
                if (seq != scopeChangeSeq.get()) {
                    return;
                }
                RuntimeFacades.callGraph().refreshCurrentContext();
            });
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
            openSelectedAllMethod();
        });
        bindOpenOnEnter(callerList, () -> {
            openSelectedCaller();
        });
        bindOpenOnEnter(calleeList, () -> {
            openSelectedCallee();
        });

        allMethodList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    openSelectedAllMethod();
                }
            }
        });
        callerList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    openSelectedCaller();
                }
            }
        });
        calleeList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    openSelectedCallee();
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
        currentClassToken = safeToken(snapshot.currentClass());
        currentMethodToken = safeToken(snapshot.currentMethod());
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
        MethodNavDto selectedValue = list.getSelectedValue();
        int selectedIndex = list.getSelectedIndex();
        List<MethodNavDto> next = values == null ? List.of() : values;
        syncModel(model, next);
        if (selectedValue != null) {
            int index = indexOf(model, selectedValue);
            if (index >= 0) {
                list.setSelectedIndex(index);
                return;
            }
        }
        if (selectedIndex >= 0 && selectedIndex < model.getSize()) {
            list.setSelectedIndex(selectedIndex);
        }
    }

    private static void syncModel(DefaultListModel<MethodNavDto> model, List<MethodNavDto> values) {
        int targetSize = values == null ? 0 : values.size();
        int currentSize = model.getSize();
        int common = Math.min(currentSize, targetSize);
        for (int i = 0; i < common; i++) {
            MethodNavDto next = values.get(i);
            MethodNavDto current = model.get(i);
            if (!java.util.Objects.equals(current, next)) {
                model.set(i, next);
            }
        }
        for (int i = currentSize - 1; i >= targetSize; i--) {
            model.remove(i);
        }
        for (int i = common; i < targetSize; i++) {
            model.add(i, values.get(i));
        }
    }

    private static int indexOf(DefaultListModel<MethodNavDto> model, MethodNavDto target) {
        if (model == null || target == null) {
            return -1;
        }
        for (int i = 0; i < model.getSize(); i++) {
            if (java.util.Objects.equals(model.get(i), target)) {
                return i;
            }
        }
        return -1;
    }

    private void refreshCurrentContextAsync() {
        runCallGraphAsync("swing-call-refresh", () -> RuntimeFacades.callGraph().refreshCurrentContext());
    }

    private void openSelectedAllMethod() {
        MethodNavDto selected = allMethodList.getSelectedValue();
        if (selected == null) {
            return;
        }
        runCallGraphAsync("swing-call-open-all", () -> openMethod(selected));
    }

    private void openSelectedCaller() {
        MethodNavDto selected = callerList.getSelectedValue();
        if (selected == null) {
            return;
        }
        runCallGraphAsync("swing-call-open-caller", () -> openMethod(selected));
    }

    private void openSelectedCallee() {
        MethodNavDto selected = calleeList.getSelectedValue();
        if (selected == null) {
            return;
        }
        runCallGraphAsync("swing-call-open-callee", () -> openMethod(selected));
    }

    private void runCallGraphAsync(String threadName, Runnable action) {
        if (action == null) {
            return;
        }
        String name = threadName == null || threadName.isBlank() ? "swing-call-action" : threadName;
        Thread.ofVirtual().name(name).start(() -> {
            try {
                action.run();
            } catch (Throwable ex) {
                logger.warn("{} failed: {}", name, ex.toString());
            }
        });
    }

    private void openMethod(MethodNavDto item) {
        if (item == null) {
            return;
        }
        RuntimeFacades.editor().openMethod(
                item.className(),
                item.methodName(),
                item.methodDesc(),
                item.jarId()
        );
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String safeToken(String value) {
        return value == null ? "" : value.trim();
    }

    private List<String> collectHighlightTokens() {
        return SwingResultHtml.collectTokens(currentClassToken, currentMethodToken);
    }

    public void applyLanguage() {
        SwingI18n.localizeComponentTree(this);
    }

    private final class MethodRenderer extends DefaultListCellRenderer {
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
                        0,
                        collectHighlightTokens()
                ));
                setToolTipText(SwingResultHtml.normalizeClassName(method.className())
                        + "#" + safeToken(method.methodName()) + safeToken(method.methodDesc())
                        + " [" + safeToken(method.jarName()) + "]");
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
