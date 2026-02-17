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
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public final class ImplToolPanel extends JPanel {
    private final JLabel jarValue = new JLabel("-");
    private final JLabel classValue = new JLabel("-");
    private final JLabel methodValue = new JLabel("-");
    private final DefaultListModel<MethodNavDto> implModel = new DefaultListModel<>();
    private final DefaultListModel<MethodNavDto> superImplModel = new DefaultListModel<>();
    private final JList<MethodNavDto> implList = new JList<>(implModel);
    private final JList<MethodNavDto> superImplList = new JList<>(superImplModel);
    private final JLabel statusValue = new JLabel(SwingI18n.tr("就绪", "ready"));

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
        refreshBtn.addActionListener(e -> RuntimeFacades.callGraph().refreshCurrentContext());
        JButton openImplBtn = new JButton("Open Impl");
        openImplBtn.addActionListener(e -> openSelectedImpl());
        JButton openSuperImplBtn = new JButton("Open Super");
        openSuperImplBtn.addActionListener(e -> openSelectedSuperImpl());
        actions.add(refreshBtn);
        actions.add(openImplBtn);
        actions.add(openSuperImplBtn);

        contextPanel.add(values, BorderLayout.CENTER);
        contextPanel.add(actions, BorderLayout.SOUTH);

        implList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        superImplList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        implList.setCellRenderer(new MethodCellRenderer());
        superImplList.setCellRenderer(new MethodCellRenderer());

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
        jarValue.setText(safe(snapshot.currentJar()));
        classValue.setText(safe(snapshot.currentClass()));
        methodValue.setText(safe(snapshot.currentMethod()));
        resetModelKeepingSelection(implModel, implList, snapshot.impls());
        resetModelKeepingSelection(superImplModel, superImplList, snapshot.superImpls());
        statusValue.setText(SwingI18n.tr("实现=", "impl=") + implModel.getSize()
                + ", " + SwingI18n.tr("父实现=", "super=") + superImplModel.getSize());
    }

    private void openSelectedImpl() {
        int index = implList.getSelectedIndex();
        if (index >= 0) {
            RuntimeFacades.callGraph().openImpl(index);
        }
    }

    private void openSelectedSuperImpl() {
        int index = superImplList.getSelectedIndex();
        if (index >= 0) {
            RuntimeFacades.callGraph().openSuperImpl(index);
        }
    }

    private static void resetModelKeepingSelection(
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

    private static final class MethodCellRenderer extends DefaultListCellRenderer {
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
                setText(method.className() + "#" + method.methodName() + method.methodDesc() + " [" + method.jarName() + "]");
            }
            return this;
        }
    }
}
