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
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class ImplToolPanel extends JPanel {
    private static final Logger logger = LogManager.getLogger();
    private final JLabel jarValue = new JLabel("-");
    private final JLabel classValue = new JLabel("-");
    private final JLabel methodValue = new JLabel("-");
    private final DefaultListModel<MethodNavDto> implModel = new DefaultListModel<>();
    private final DefaultListModel<MethodNavDto> superImplModel = new DefaultListModel<>();
    private final JList<MethodNavDto> implList = new JList<>(implModel);
    private final JList<MethodNavDto> superImplList = new JList<>(superImplModel);
    private final JComboBox<ScopeItem> scopeBox = new JComboBox<>(ScopeItem.defaultItems());
    private final JLabel statusValue = new JLabel(SwingI18n.tr("就绪", "ready"));
    private final SwingUiApplyGuard.Throttle snapshotThrottle = new SwingUiApplyGuard.Throttle();
    private final AtomicLong scopeChangeSeq = new AtomicLong(0L);
    private boolean scopeUpdating = false;
    private String currentClassToken = "";
    private String currentMethodToken = "";

    public ImplToolPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        initUi();
    }

    private void initUi() {
        JPanel contextPanel = new JPanel(new BorderLayout(6, 6));
        contextPanel.setBorder(BorderFactory.createTitledBorder("Current Context"));
        JPanel values = new JPanel(new BorderLayout(4, 4));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row1.add(new JLabel("jar"));
        row1.add(jarValue);
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row2.add(new JLabel("class"));
        row2.add(classValue);
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row3.add(new JLabel("method"));
        row3.add(methodValue);
        JPanel wrap = new JPanel(new BorderLayout(2, 2));
        wrap.add(row1, BorderLayout.NORTH);
        wrap.add(row2, BorderLayout.CENTER);
        wrap.add(row3, BorderLayout.SOUTH);
        values.add(wrap, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshCurrentContextAsync());
        JButton openImplBtn = new JButton("Open Impl");
        openImplBtn.addActionListener(e -> openSelectedImpl());
        JButton openSuperImplBtn = new JButton("Open Super");
        openSuperImplBtn.addActionListener(e -> openSelectedSuperImpl());
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
            runCallGraphAsync("swing-impl-scope", () -> {
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
        actions.add(refreshBtn);
        actions.add(openImplBtn);
        actions.add(openSuperImplBtn);
        actions.add(new JLabel("Scope"));
        actions.add(scopeBox);

        contextPanel.add(values, BorderLayout.CENTER);
        contextPanel.add(actions, BorderLayout.SOUTH);

        implList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        superImplList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        implList.setCellRenderer(new MethodCellRenderer());
        superImplList.setCellRenderer(new MethodCellRenderer());
        bindOpenOnEnter(implList, this::openSelectedImpl);
        bindOpenOnEnter(superImplList, this::openSelectedSuperImpl);

        implList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    openSelectedImpl();
                }
            }
        });
        superImplList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    openSelectedSuperImpl();
                }
            }
        });

        JPanel implPanel = new JPanel(new BorderLayout());
        implPanel.setBorder(BorderFactory.createTitledBorder("Impl Methods"));
        implPanel.add(new JScrollPane(implList), BorderLayout.CENTER);

        JPanel superImplPanel = new JPanel(new BorderLayout());
        superImplPanel.setBorder(BorderFactory.createTitledBorder("Super Impl Methods"));
        superImplPanel.add(new JScrollPane(superImplList), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, implPanel, superImplPanel);
        split.setResizeWeight(0.56);
        split.setDividerLocation(0.56);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        statusPanel.add(new JLabel("status"));
        statusPanel.add(statusValue);

        add(contextPanel, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
        applyLanguage();
    }

    public void applySnapshot(CallGraphSnapshotDto snapshot) {
        if (snapshot == null) {
            return;
        }
        if (!SwingUiApplyGuard.ensureEdt("ImplToolPanel.applySnapshot", () -> applySnapshot(snapshot))) {
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
        resetModelKeepingSelection(implModel, implList, snapshot.impls());
        resetModelKeepingSelection(superImplModel, superImplList, snapshot.superImpls());
        statusValue.setText(SwingI18n.tr("实现=", "impl=") + implModel.getSize()
                + ", " + SwingI18n.tr("父实现=", "super=") + superImplModel.getSize());
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

    private void openSelectedImpl() {
        MethodNavDto selected = implList.getSelectedValue();
        if (selected != null) {
            runCallGraphAsync("swing-impl-open", () -> openMethod(selected));
        }
    }

    private void openSelectedSuperImpl() {
        MethodNavDto selected = superImplList.getSelectedValue();
        if (selected != null) {
            runCallGraphAsync("swing-impl-open-super", () -> openMethod(selected));
        }
    }

    private static void resetModelKeepingSelection(
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
        runCallGraphAsync("swing-impl-refresh", () -> RuntimeFacades.callGraph().refreshCurrentContext());
    }

    private void runCallGraphAsync(String threadName, Runnable action) {
        if (action == null) {
            return;
        }
        String name = threadName == null || threadName.isBlank() ? "swing-impl-action" : threadName;
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

    private final class MethodCellRenderer extends DefaultListCellRenderer {
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
