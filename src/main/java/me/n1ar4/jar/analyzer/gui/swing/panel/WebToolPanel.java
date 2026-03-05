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
import me.n1ar4.jar.analyzer.gui.runtime.model.ClassNavDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.MethodNavDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.WebSnapshotDto;
import me.n1ar4.jar.analyzer.gui.swing.SwingI18n;
import me.n1ar4.jar.analyzer.gui.swing.SwingResultHtml;
import me.n1ar4.jar.analyzer.gui.swing.SwingTextSync;
import me.n1ar4.jar.analyzer.gui.swing.SwingUiApplyGuard;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Supplier;

public final class WebToolPanel extends JPanel {
    private static final Logger logger = LogManager.getLogger();
    private final JTextField pathKeywordField = new JTextField();
    private final JLabel statusValue = new JLabel(SwingI18n.tr("就绪", "ready"));

    private final DefaultListModel<ClassNavDto> controllerModel = new DefaultListModel<>();
    private final DefaultListModel<MethodNavDto> mappingModel = new DefaultListModel<>();
    private final DefaultListModel<ClassNavDto> interceptorModel = new DefaultListModel<>();
    private final DefaultListModel<ClassNavDto> servletModel = new DefaultListModel<>();
    private final DefaultListModel<ClassNavDto> filterModel = new DefaultListModel<>();
    private final DefaultListModel<ClassNavDto> listenerModel = new DefaultListModel<>();

    private final JList<ClassNavDto> controllerList = new JList<>(controllerModel);
    private final JList<MethodNavDto> mappingList = new JList<>(mappingModel);
    private final JList<ClassNavDto> interceptorList = new JList<>(interceptorModel);
    private final JList<ClassNavDto> servletList = new JList<>(servletModel);
    private final JList<ClassNavDto> filterList = new JList<>(filterModel);
    private final JList<ClassNavDto> listenerList = new JList<>(listenerModel);
    private final SwingUiApplyGuard.Throttle snapshotThrottle = new SwingUiApplyGuard.Throttle();
    private boolean hasSnapshot;

    public WebToolPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        initUi();
    }

    private void initUi() {
        JPanel top = new JPanel(new BorderLayout(6, 0));
        top.setBorder(BorderFactory.createTitledBorder("Web Path Search"));
        top.add(pathKeywordField, BorderLayout.CENTER);
        JButton search = new JButton("Search");
        search.addActionListener(e -> applyPathAndRefresh());
        JButton refresh = new JButton("Refresh All");
        refresh.addActionListener(e -> runWebAsync("swing-web-refresh", () -> RuntimeFacades.web().refreshAll()));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actions.add(search);
        actions.add(refresh);
        top.add(actions, BorderLayout.EAST);

        pathKeywordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    applyPathAndRefresh();
                }
            }
        });

        for (JList<?> list : List.of(controllerList, mappingList, interceptorList, servletList, filterList, listenerList)) {
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }
        controllerList.setCellRenderer(new ClassRenderer(this::collectHighlightTokens));
        mappingList.setCellRenderer(new MethodRenderer(this::collectHighlightTokens));
        interceptorList.setCellRenderer(new ClassRenderer(this::collectHighlightTokens));
        servletList.setCellRenderer(new ClassRenderer(this::collectHighlightTokens));
        filterList.setCellRenderer(new ClassRenderer(this::collectHighlightTokens));
        listenerList.setCellRenderer(new ClassRenderer(this::collectHighlightTokens));

        controllerList.addMouseListener(new ClassOpenAdapter(controllerList));
        interceptorList.addMouseListener(new ClassOpenAdapter(interceptorList));
        servletList.addMouseListener(new ClassOpenAdapter(servletList));
        filterList.addMouseListener(new ClassOpenAdapter(filterList));
        listenerList.addMouseListener(new ClassOpenAdapter(listenerList));
        mappingList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    MethodNavDto selected = mappingList.getSelectedValue();
                    if (selected != null) {
                        MethodNavDto item = selected;
                        runWebAsync("swing-web-open-mapping", () -> openMethod(item));
                    }
                }
            }
        });

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("controller", new JScrollPane(controllerList));
        tabs.addTab("mapping", new JScrollPane(mappingList));
        tabs.addTab("interceptor", new JScrollPane(interceptorList));
        tabs.addTab("servlet", new JScrollPane(servletList));
        tabs.addTab("filter", new JScrollPane(filterList));
        tabs.addTab("listener", new JScrollPane(listenerList));

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(new JLabel("status"), BorderLayout.WEST);
        statusPanel.add(statusValue, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
        applyLanguage();
    }

    public void applySnapshot(WebSnapshotDto snapshot) {
        if (snapshot == null) {
            return;
        }
        if (!SwingUiApplyGuard.ensureEdt("WebToolPanel.applySnapshot", () -> applySnapshot(snapshot))) {
            return;
        }
        if (!snapshotThrottle.allow(SwingUiApplyGuard.fingerprint(snapshot))) {
            return;
        }
        SwingTextSync.setTextIfIdle(pathKeywordField, snapshot.pathKeyword());
        resetClassModel(controllerModel, controllerList, snapshot.controllers());
        resetMethodModel(mappingModel, mappingList, snapshot.mappings());
        resetClassModel(interceptorModel, interceptorList, snapshot.interceptors());
        resetClassModel(servletModel, servletList, snapshot.servlets());
        resetClassModel(filterModel, filterList, snapshot.filters());
        resetClassModel(listenerModel, listenerList, snapshot.listeners());
        hasSnapshot = true;
        updateStatusText();
    }

    private void applyPathAndRefresh() {
        String keyword = safe(pathKeywordField.getText());
        runWebAsync("swing-web-search", () -> {
            RuntimeFacades.web().pathSearch(keyword);
            RuntimeFacades.web().refreshAll();
        });
    }

    private List<String> collectHighlightTokens() {
        return SwingResultHtml.collectTokens(pathKeywordField.getText());
    }

    private static void resetClassModel(
            DefaultListModel<ClassNavDto> model,
            JList<ClassNavDto> list,
            List<ClassNavDto> values
    ) {
        ClassNavDto selectedValue = list.getSelectedValue();
        int selected = list.getSelectedIndex();
        List<ClassNavDto> next = values == null ? List.of() : values;
        syncClassModel(model, next);
        if (selectedValue != null) {
            int index = indexOfClass(model, selectedValue);
            if (index >= 0) {
                list.setSelectedIndex(index);
                return;
            }
        }
        if (selected >= 0 && selected < model.getSize()) {
            list.setSelectedIndex(selected);
        }
    }

    private static void resetMethodModel(
            DefaultListModel<MethodNavDto> model,
            JList<MethodNavDto> list,
            List<MethodNavDto> values
    ) {
        MethodNavDto selectedValue = list.getSelectedValue();
        int selected = list.getSelectedIndex();
        List<MethodNavDto> next = values == null ? List.of() : values;
        syncMethodModel(model, next);
        if (selectedValue != null) {
            int index = indexOfMethod(model, selectedValue);
            if (index >= 0) {
                list.setSelectedIndex(index);
                return;
            }
        }
        if (selected >= 0 && selected < model.getSize()) {
            list.setSelectedIndex(selected);
        }
    }

    private static void syncClassModel(DefaultListModel<ClassNavDto> model, List<ClassNavDto> values) {
        int targetSize = values == null ? 0 : values.size();
        int currentSize = model.getSize();
        int common = Math.min(currentSize, targetSize);
        for (int i = 0; i < common; i++) {
            ClassNavDto next = values.get(i);
            ClassNavDto current = model.get(i);
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

    private static void syncMethodModel(DefaultListModel<MethodNavDto> model, List<MethodNavDto> values) {
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

    private static int indexOfClass(DefaultListModel<ClassNavDto> model, ClassNavDto target) {
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

    private static int indexOfMethod(DefaultListModel<MethodNavDto> model, MethodNavDto target) {
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

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public void applyLanguage() {
        SwingI18n.localizeComponentTree(this);
        if (hasSnapshot) {
            updateStatusText();
        } else {
            statusValue.setText(SwingI18n.tr("就绪", "ready"));
        }
    }

    private void updateStatusText() {
        statusValue.setText(SwingI18n.tr("控制器=", "controller=") + controllerModel.size()
                + ", " + SwingI18n.tr("映射=", "mapping=") + mappingModel.size()
                + ", " + SwingI18n.tr("拦截器=", "interceptor=") + interceptorModel.size()
                + ", " + SwingI18n.tr("Servlet=", "servlet=") + servletModel.size()
                + ", " + SwingI18n.tr("过滤器=", "filter=") + filterModel.size()
                + ", " + SwingI18n.tr("监听器=", "listener=") + listenerModel.size());
    }

    private static final class ClassRenderer extends DefaultListCellRenderer {
        private final Supplier<List<String>> tokenSupplier;

        private ClassRenderer(Supplier<List<String>> tokenSupplier) {
            this.tokenSupplier = tokenSupplier;
        }

        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
        ) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof ClassNavDto item) {
                List<String> tokens = tokenSupplier == null ? List.of() : tokenSupplier.get();
                setText(SwingResultHtml.renderClassRow(item.className(), tokens));
                setToolTipText(SwingResultHtml.normalizeClassName(item.className())
                        + " [" + safe(item.jarName()) + "]");
            }
            return this;
        }
    }

    private static final class MethodRenderer extends DefaultListCellRenderer {
        private final Supplier<List<String>> tokenSupplier;

        private MethodRenderer(Supplier<List<String>> tokenSupplier) {
            this.tokenSupplier = tokenSupplier;
        }

        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
        ) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof MethodNavDto item) {
                List<String> tokens = tokenSupplier == null ? List.of() : tokenSupplier.get();
                setText(SwingResultHtml.renderMethodRow(
                        item.className(),
                        item.methodName(),
                        item.methodDesc(),
                        0,
                        tokens
                ));
                setToolTipText(SwingResultHtml.normalizeClassName(item.className())
                        + "#" + safe(item.methodName()) + safe(item.methodDesc())
                        + " [" + safe(item.jarName()) + "]");
            }
            return this;
        }
    }

    private static final class ClassOpenAdapter extends MouseAdapter {
        private final JList<ClassNavDto> list;

        private ClassOpenAdapter(JList<ClassNavDto> list) {
            this.list = list;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                ClassNavDto selected = list.getSelectedValue();
                if (selected != null) {
                    ClassNavDto item = selected;
                    Thread.ofVirtual().name("swing-web-open-class").start(() -> {
                        try {
                            RuntimeFacades.editor().openClass(item.className(), item.jarId());
                        } catch (Throwable ex) {
                            logger.warn("swing-web-open-class failed: {}", ex.toString());
                        }
                    });
                }
            }
        }
    }

    private void runWebAsync(String threadName, Runnable action) {
        if (action == null) {
            return;
        }
        String name = threadName == null || threadName.isBlank() ? "swing-web-action" : threadName;
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
}
