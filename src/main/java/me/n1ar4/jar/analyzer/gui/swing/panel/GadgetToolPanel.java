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
import me.n1ar4.jar.analyzer.gui.runtime.model.GadgetRowDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.GadgetSettingsDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.GadgetSnapshotDto;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.List;

public final class GadgetToolPanel extends JPanel {
    private final JTextField inputDirText = new JTextField();
    private final JCheckBox nativeBox = new JCheckBox("Native");
    private final JCheckBox hessianBox = new JCheckBox("Hessian");
    private final JCheckBox jdbcBox = new JCheckBox("JDBC");
    private final JCheckBox fastjsonBox = new JCheckBox("Fastjson");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Definition", "Risk"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable resultTable = new JTable(tableModel);
    private final JLabel statusValue = new JLabel("ready");

    private volatile boolean syncing;

    public GadgetToolPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        initUi();
    }

    private void initUi() {
        JPanel inputPanel = new JPanel(new BorderLayout(6, 6));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Input"));
        JPanel pathRow = new JPanel(new BorderLayout(6, 0));
        pathRow.add(new JLabel("dependency dir"), BorderLayout.WEST);
        pathRow.add(inputDirText, BorderLayout.CENTER);
        JButton chooseBtn = new JButton("...");
        chooseBtn.addActionListener(e -> chooseDir());
        pathRow.add(chooseBtn, BorderLayout.EAST);
        inputPanel.add(pathRow, BorderLayout.NORTH);

        JPanel options = new JPanel(new GridLayout(1, 4, 4, 4));
        options.add(nativeBox);
        options.add(hessianBox);
        options.add(jdbcBox);
        options.add(fastjsonBox);
        inputPanel.add(options, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton applyBtn = new JButton("Apply");
        applyBtn.addActionListener(e -> applySettings());
        JButton startBtn = new JButton("Start");
        startBtn.addActionListener(e -> {
            applySettings();
            RuntimeFacades.gadget().start();
        });
        actions.add(applyBtn);
        actions.add(startBtn);
        inputPanel.add(actions, BorderLayout.SOUTH);

        JScrollPane tableScroll = new JScrollPane(resultTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Results"));
        resultTable.setAutoCreateRowSorter(true);

        JPanel status = new JPanel(new BorderLayout());
        status.add(new JLabel("status"), BorderLayout.WEST);
        status.add(statusValue, BorderLayout.CENTER);

        add(inputPanel, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
    }

    public void applySnapshot(GadgetSnapshotDto snapshot) {
        if (snapshot == null) {
            return;
        }
        GadgetSettingsDto settings = snapshot.settings();
        if (settings != null) {
            syncing = true;
            try {
                setTextIfIdle(inputDirText, settings.inputDir());
                nativeBox.setSelected(settings.nativeMode());
                hessianBox.setSelected(settings.hessian());
                jdbcBox.setSelected(settings.jdbc());
                fastjsonBox.setSelected(settings.fastjson());
            } finally {
                syncing = false;
            }
        }

        tableModel.setRowCount(0);
        List<GadgetRowDto> rows = snapshot.rows();
        if (rows != null) {
            for (GadgetRowDto row : rows) {
                tableModel.addRow(new Object[]{row.id(), row.definition(), row.risk()});
            }
        }
        statusValue.setText("rows=" + tableModel.getRowCount());
    }

    private void chooseDir() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Dependency Directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        String current = inputDirText.getText();
        if (current != null && !current.isBlank()) {
            chooser.setSelectedFile(new File(current));
        }
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            inputDirText.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void applySettings() {
        if (syncing) {
            return;
        }
        RuntimeFacades.gadget().apply(new GadgetSettingsDto(
                safe(inputDirText.getText()),
                nativeBox.isSelected(),
                hessianBox.isSelected(),
                jdbcBox.isSelected(),
                fastjsonBox.isSelected()
        ));
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
}

