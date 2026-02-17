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
import me.n1ar4.jar.analyzer.gui.runtime.model.LeakItemDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.LeakRulesDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.LeakSnapshotDto;
import me.n1ar4.jar.analyzer.gui.swing.SwingI18n;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public final class LeakToolPanel extends JPanel {
    private final JCheckBox urlBox = new JCheckBox("URL");
    private final JCheckBox jdbcBox = new JCheckBox("JDBC");
    private final JCheckBox filePathBox = new JCheckBox("File Path");
    private final JCheckBox jwtBox = new JCheckBox("JWT");
    private final JCheckBox macBox = new JCheckBox("MAC");
    private final JCheckBox ipBox = new JCheckBox("IP");
    private final JCheckBox phoneBox = new JCheckBox("Phone");
    private final JCheckBox idCardBox = new JCheckBox("ID Card");
    private final JCheckBox emailBox = new JCheckBox("Email");
    private final JCheckBox apiKeyBox = new JCheckBox("API Key");
    private final JCheckBox bankCardBox = new JCheckBox("Bank Card");
    private final JCheckBox cloudAkSkBox = new JCheckBox("Cloud AK/SK");
    private final JCheckBox cryptoKeyBox = new JCheckBox("Crypto Key");
    private final JCheckBox aiKeyBox = new JCheckBox("AI Key");
    private final JCheckBox passwordBox = new JCheckBox("Password");
    private final JCheckBox detectBase64Box = new JCheckBox("Detect Base64");

    private final DefaultListModel<LeakItemDto> resultModel = new DefaultListModel<>();
    private final JList<LeakItemDto> resultList = new JList<>(resultModel);
    private final javax.swing.JTextArea logArea = new javax.swing.JTextArea();
    private final JLabel statusValue = new JLabel(SwingI18n.tr("就绪", "ready"));

    private volatile boolean syncing;
    private boolean hasSnapshot;

    public LeakToolPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        initUi();
    }

    private void initUi() {
        JPanel rulesPanel = new JPanel(new GridLayout(5, 3, 4, 4));
        rulesPanel.setBorder(BorderFactory.createTitledBorder("Leak Rules"));
        rulesPanel.add(urlBox);
        rulesPanel.add(jdbcBox);
        rulesPanel.add(filePathBox);
        rulesPanel.add(jwtBox);
        rulesPanel.add(macBox);
        rulesPanel.add(ipBox);
        rulesPanel.add(phoneBox);
        rulesPanel.add(idCardBox);
        rulesPanel.add(emailBox);
        rulesPanel.add(apiKeyBox);
        rulesPanel.add(bankCardBox);
        rulesPanel.add(cloudAkSkBox);
        rulesPanel.add(cryptoKeyBox);
        rulesPanel.add(aiKeyBox);
        rulesPanel.add(passwordBox);

        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        configPanel.setBorder(BorderFactory.createTitledBorder("Config"));
        configPanel.add(detectBase64Box);

        JPanel top = new JPanel(new BorderLayout(6, 6));
        top.add(rulesPanel, BorderLayout.CENTER);
        top.add(configPanel, BorderLayout.SOUTH);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton applyBtn = new JButton("Apply");
        applyBtn.addActionListener(e -> applyRules());
        JButton startBtn = new JButton("Start");
        startBtn.addActionListener(e -> {
            applyRules();
            RuntimeFacades.leak().start();
        });
        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> RuntimeFacades.leak().clear());
        JButton exportBtn = new JButton("Export");
        exportBtn.addActionListener(e -> RuntimeFacades.leak().export());
        actionPanel.add(applyBtn);
        actionPanel.add(startBtn);
        actionPanel.add(clearBtn);
        actionPanel.add(exportBtn);

        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultList.setCellRenderer(new LeakRenderer());
        resultList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    int index = resultList.getSelectedIndex();
                    if (index >= 0) {
                        RuntimeFacades.leak().openResult(index);
                    }
                }
            }
        });
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(BorderFactory.createTitledBorder("Results"));
        resultPanel.add(new JScrollPane(resultList), BorderLayout.CENTER);

        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Leak Log"));

        JPanel center = new JPanel(new GridLayout(2, 1, 6, 6));
        center.add(resultPanel);
        center.add(logScroll);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(new JLabel("status"), BorderLayout.WEST);
        statusPanel.add(statusValue, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(6, 6));
        south.add(actionPanel, BorderLayout.NORTH);
        south.add(statusPanel, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        applyLanguage();
    }

    public void applySnapshot(LeakSnapshotDto snapshot) {
        if (snapshot == null) {
            return;
        }
        LeakRulesDto rules = snapshot.rules();
        if (rules != null) {
            syncing = true;
            try {
                urlBox.setSelected(rules.url());
                jdbcBox.setSelected(rules.jdbc());
                filePathBox.setSelected(rules.filePath());
                jwtBox.setSelected(rules.jwt());
                macBox.setSelected(rules.mac());
                ipBox.setSelected(rules.ip());
                phoneBox.setSelected(rules.phone());
                idCardBox.setSelected(rules.idCard());
                emailBox.setSelected(rules.email());
                apiKeyBox.setSelected(rules.apiKey());
                bankCardBox.setSelected(rules.bankCard());
                cloudAkSkBox.setSelected(rules.cloudAkSk());
                cryptoKeyBox.setSelected(rules.cryptoKey());
                aiKeyBox.setSelected(rules.aiKey());
                passwordBox.setSelected(rules.password());
                detectBase64Box.setSelected(rules.detectBase64());
            } finally {
                syncing = false;
            }
        }
        int selected = resultList.getSelectedIndex();
        resultModel.clear();
        List<LeakItemDto> results = snapshot.results();
        if (results != null) {
            for (LeakItemDto item : results) {
                resultModel.addElement(item);
            }
        }
        if (selected >= 0 && selected < resultModel.size()) {
            resultList.setSelectedIndex(selected);
        }
        logArea.setText(safe(snapshot.logTail()));
        logArea.setCaretPosition(logArea.getDocument().getLength());
        hasSnapshot = true;
        updateStatusText();
    }

    private void applyRules() {
        if (syncing) {
            return;
        }
        RuntimeFacades.leak().apply(new LeakRulesDto(
                urlBox.isSelected(),
                jdbcBox.isSelected(),
                filePathBox.isSelected(),
                jwtBox.isSelected(),
                macBox.isSelected(),
                ipBox.isSelected(),
                phoneBox.isSelected(),
                idCardBox.isSelected(),
                emailBox.isSelected(),
                apiKeyBox.isSelected(),
                bankCardBox.isSelected(),
                cloudAkSkBox.isSelected(),
                cryptoKeyBox.isSelected(),
                aiKeyBox.isSelected(),
                passwordBox.isSelected(),
                detectBase64Box.isSelected()
        ));
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
        statusValue.setText(SwingI18n.tr("结果数=", "results=") + resultModel.size());
    }

    private static final class LeakRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
        ) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof LeakItemDto item) {
                setText("[" + safe(item.typeName()) + "] "
                        + safe(item.className()) + " -> "
                        + safe(item.value()));
            }
            return this;
        }
    }
}
