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
import me.n1ar4.jar.analyzer.gui.runtime.model.WebClassBucket;
import me.n1ar4.jar.analyzer.gui.runtime.model.WebSnapshotDto;

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

public final class WebToolPanel extends JPanel {
    private final JTextField pathKeywordField = new JTextField();
    private final JLabel statusValue = new JLabel("ready");

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
        refresh.addActionListener(e -> RuntimeFacades.web().refreshAll());
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
        controllerList.setCellRenderer(new ClassRenderer());
        mappingList.setCellRenderer(new MethodRenderer());
        interceptorList.setCellRenderer(new ClassRenderer());
        servletList.setCellRenderer(new ClassRenderer());
        filterList.setCellRenderer(new ClassRenderer());
        listenerList.setCellRenderer(new ClassRenderer());

        controllerList.addMouseListener(new ClassOpenAdapter(WebClassBucket.CONTROLLER, controllerList));
        interceptorList.addMouseListener(new ClassOpenAdapter(WebClassBucket.INTERCEPTOR, interceptorList));
        servletList.addMouseListener(new ClassOpenAdapter(WebClassBucket.SERVLET, servletList));
        filterList.addMouseListener(new ClassOpenAdapter(WebClassBucket.FILTER, filterList));
        listenerList.addMouseListener(new ClassOpenAdapter(WebClassBucket.LISTENER, listenerList));
        mappingList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    int index = mappingList.getSelectedIndex();
                    if (index >= 0) {
                        RuntimeFacades.web().openMapping(index);
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
    }

    public void applySnapshot(WebSnapshotDto snapshot) {
        if (snapshot == null) {
            return;
        }
        setTextIfIdle(pathKeywordField, snapshot.pathKeyword());
        resetClassModel(controllerModel, controllerList, snapshot.controllers());
        resetMethodModel(mappingModel, mappingList, snapshot.mappings());
        resetClassModel(interceptorModel, interceptorList, snapshot.interceptors());
        resetClassModel(servletModel, servletList, snapshot.servlets());
        resetClassModel(filterModel, filterList, snapshot.filters());
        resetClassModel(listenerModel, listenerList, snapshot.listeners());
        statusValue.setText("controller=" + controllerModel.size()
                + ", mapping=" + mappingModel.size()
                + ", interceptor=" + interceptorModel.size()
                + ", servlet=" + servletModel.size()
                + ", filter=" + filterModel.size()
                + ", listener=" + listenerModel.size());
    }

    private void applyPathAndRefresh() {
        RuntimeFacades.web().pathSearch(safe(pathKeywordField.getText()));
        RuntimeFacades.web().refreshAll();
    }

    private static void resetClassModel(
            DefaultListModel<ClassNavDto> model,
            JList<ClassNavDto> list,
            List<ClassNavDto> values
    ) {
        int selected = list.getSelectedIndex();
        model.clear();
        if (values != null) {
            for (ClassNavDto value : values) {
                model.addElement(value);
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
        int selected = list.getSelectedIndex();
        model.clear();
        if (values != null) {
            for (MethodNavDto value : values) {
                model.addElement(value);
            }
        }
        if (selected >= 0 && selected < model.getSize()) {
            list.setSelectedIndex(selected);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static void setTextIfIdle(JTextField field, String value) {
        if (field == null || field.isFocusOwner()) {
            return;
        }
        String next = safe(value);
        if (!next.equals(field.getText())) {
            field.setText(next);
        }
    }

    private static final class ClassRenderer extends DefaultListCellRenderer {
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
                setText(item.className() + " [" + item.jarName() + "]");
            }
            return this;
        }
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
            if (value instanceof MethodNavDto item) {
                setText(item.className() + "#" + item.methodName() + item.methodDesc() + " [" + item.jarName() + "]");
            }
            return this;
        }
    }

    private static final class ClassOpenAdapter extends MouseAdapter {
        private final WebClassBucket bucket;
        private final JList<ClassNavDto> list;

        private ClassOpenAdapter(WebClassBucket bucket, JList<ClassNavDto> list) {
            this.bucket = bucket;
            this.list = list;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                int index = list.getSelectedIndex();
                if (index >= 0) {
                    RuntimeFacades.web().openClass(bucket, index);
                }
            }
        }
    }
}

