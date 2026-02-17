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
import me.n1ar4.jar.analyzer.gui.runtime.model.ApiInfoDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.McpConfigDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.McpLineConfigDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.McpLineKey;
import me.n1ar4.jar.analyzer.gui.swing.SwingI18n;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ApiMcpToolPanel extends JPanel {
    private final JTextField apiBindText = readonly();
    private final JTextField apiPortText = readonly();
    private final JTextField apiAuthText = readonly();
    private final JTextField apiTokenText = readonly();

    private final JTextField mcpBindText = new JTextField();
    private final JCheckBox mcpAuthBox = new JCheckBox("auth enabled");
    private final JTextField mcpTokenText = new JTextField();
    private final JCheckBox reportWebEnabledBox = new JCheckBox("report web enabled");
    private final JTextField reportWebHostText = new JTextField();
    private final JSpinner reportWebPortSpin = new JSpinner(new SpinnerNumberModel(20080, 1, 65535, 1));
    private final JLabel reportWebRunningValue = new JLabel("false");

    private final Map<McpLineKey, LineEditors> lineEditors = new EnumMap<>(McpLineKey.class);
    private final JTextArea statusArea = new JTextArea();
    private volatile boolean syncing;

    public ApiMcpToolPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        initUi();
    }

    private void initUi() {
        JPanel apiPanel = new JPanel(new GridLayout(4, 2, 4, 4));
        apiPanel.setBorder(BorderFactory.createTitledBorder("API Server"));
        apiPanel.add(new JLabel("bind"));
        apiPanel.add(apiBindText);
        apiPanel.add(new JLabel("port"));
        apiPanel.add(apiPortText);
        apiPanel.add(new JLabel("auth"));
        apiPanel.add(apiAuthText);
        apiPanel.add(new JLabel("token"));
        apiPanel.add(apiTokenText);

        JPanel mcpPanel = new JPanel(new GridLayout(6, 2, 4, 4));
        mcpPanel.setBorder(BorderFactory.createTitledBorder("MCP Config"));
        mcpPanel.add(new JLabel("bind"));
        mcpPanel.add(mcpBindText);
        mcpPanel.add(new JLabel("auth"));
        mcpPanel.add(mcpAuthBox);
        mcpPanel.add(new JLabel("token"));
        mcpPanel.add(mcpTokenText);
        mcpPanel.add(new JLabel("report web"));
        mcpPanel.add(reportWebEnabledBox);
        mcpPanel.add(new JLabel("report host"));
        mcpPanel.add(reportWebHostText);
        mcpPanel.add(new JLabel("report port / running"));
        JPanel reportRow = new JPanel(new BorderLayout(6, 0));
        reportRow.add(reportWebPortSpin, BorderLayout.CENTER);
        reportRow.add(reportWebRunningValue, BorderLayout.EAST);
        mcpPanel.add(reportRow);

        JPanel linesPanel = new JPanel(new GridLayout(McpLineKey.values().length, 1, 4, 4));
        linesPanel.setBorder(BorderFactory.createTitledBorder("MCP Lines"));
        for (McpLineKey key : McpLineKey.values()) {
            LineEditors editors = new LineEditors();
            lineEditors.put(key, editors);
            JPanel row = new JPanel(new BorderLayout(6, 0));
            row.add(new JLabel(key.name()), BorderLayout.WEST);
            JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            center.add(editors.enabled);
            center.add(new JLabel("port"));
            center.add(editors.port);
            center.add(new JLabel("running"));
            center.add(editors.running);
            row.add(center, BorderLayout.CENTER);
            linesPanel.add(row);
        }

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton applyRestartBtn = new JButton("Apply & Restart");
        applyRestartBtn.addActionListener(e -> applyAndRestart());
        JButton startConfiguredBtn = new JButton("Start Configured");
        startConfiguredBtn.addActionListener(e -> {
            List<String> msgs = RuntimeFacades.apiMcp().startConfigured();
            setStatus(msgs);
        });
        JButton stopAllBtn = new JButton("Stop All");
        stopAllBtn.addActionListener(e -> {
            RuntimeFacades.apiMcp().stopAll();
            setStatus(List.of(SwingI18n.tr("MCP 全部已停止", "MCP all stopped")));
        });
        JButton openApiDocBtn = new JButton("API Doc");
        openApiDocBtn.addActionListener(e -> RuntimeFacades.apiMcp().openApiDoc());
        JButton openMcpDocBtn = new JButton("MCP Doc");
        openMcpDocBtn.addActionListener(e -> RuntimeFacades.apiMcp().openMcpDoc());
        JButton openN8nDocBtn = new JButton("N8N Doc");
        openN8nDocBtn.addActionListener(e -> RuntimeFacades.apiMcp().openN8nDoc());
        JButton openReportBtn = new JButton("Open Report Web");
        openReportBtn.addActionListener(e -> RuntimeFacades.apiMcp().openReportWeb(
                safe(reportWebHostText.getText()),
                (Integer) reportWebPortSpin.getValue()
        ));
        actions.add(applyRestartBtn);
        actions.add(startConfiguredBtn);
        actions.add(stopAllBtn);
        actions.add(openApiDocBtn);
        actions.add(openMcpDocBtn);
        actions.add(openN8nDocBtn);
        actions.add(openReportBtn);

        statusArea.setEditable(false);
        statusArea.setRows(8);
        JScrollPane statusScroll = new JScrollPane(statusArea);
        statusScroll.setBorder(BorderFactory.createTitledBorder("Status"));

        JPanel north = new JPanel(new BorderLayout(6, 6));
        north.add(apiPanel, BorderLayout.NORTH);
        north.add(mcpPanel, BorderLayout.CENTER);
        north.add(linesPanel, BorderLayout.SOUTH);

        add(north, BorderLayout.NORTH);
        add(actions, BorderLayout.CENTER);
        add(statusScroll, BorderLayout.SOUTH);
        applyLanguage();
    }

    public void applySnapshot(ApiInfoDto apiInfo, McpConfigDto mcpConfig) {
        if (apiInfo != null) {
            apiBindText.setText(safe(apiInfo.bind()));
            apiPortText.setText(String.valueOf(apiInfo.port()));
            apiAuthText.setText(String.valueOf(apiInfo.authEnabled()));
            apiTokenText.setText(safe(apiInfo.maskedToken()));
        }
        if (mcpConfig != null) {
            syncing = true;
            try {
                setTextIfIdle(mcpBindText, mcpConfig.bind());
                mcpAuthBox.setSelected(mcpConfig.authEnabled());
                setTextIfIdle(mcpTokenText, mcpConfig.token());
                reportWebEnabledBox.setSelected(mcpConfig.reportWebEnabled());
                setTextIfIdle(reportWebHostText, mcpConfig.reportWebHost());
                reportWebPortSpin.setValue(normalizePort(mcpConfig.reportWebPort()));
                reportWebRunningValue.setText(String.valueOf(mcpConfig.reportWebRunning()));
                if (mcpConfig.lines() != null) {
                    for (McpLineConfigDto line : mcpConfig.lines()) {
                        LineEditors editors = lineEditors.get(line.key());
                        if (editors == null) {
                            continue;
                        }
                        editors.enabled.setSelected(line.enabled());
                        editors.port.setValue(normalizePort(line.port()));
                        editors.running.setText(String.valueOf(line.running()));
                    }
                }
            } finally {
                syncing = false;
            }
        }
    }

    private void applyAndRestart() {
        if (syncing) {
            return;
        }
        List<McpLineConfigDto> lines = new ArrayList<>();
        for (Map.Entry<McpLineKey, LineEditors> entry : lineEditors.entrySet()) {
            McpLineKey key = entry.getKey();
            LineEditors editors = entry.getValue();
            lines.add(new McpLineConfigDto(
                    key,
                    editors.enabled.isSelected(),
                    (Integer) editors.port.getValue(),
                    false
            ));
        }
        McpConfigDto config = new McpConfigDto(
                safe(mcpBindText.getText()),
                mcpAuthBox.isSelected(),
                safe(mcpTokenText.getText()),
                lines,
                reportWebEnabledBox.isSelected(),
                safe(reportWebHostText.getText()),
                (Integer) reportWebPortSpin.getValue(),
                false
        );
        List<String> msgs = RuntimeFacades.apiMcp().applyAndRestart(config);
        setStatus(msgs);
    }

    private void setStatus(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        if (lines != null) {
            for (String line : lines) {
                sb.append(safe(line)).append('\n');
            }
        }
        statusArea.setText(sb.toString());
        statusArea.setCaretPosition(statusArea.getDocument().getLength());
    }

    public void applyLanguage() {
        SwingI18n.localizeComponentTree(this);
    }

    private static JTextField readonly() {
        JTextField field = new JTextField();
        field.setEditable(false);
        return field;
    }

    private static int normalizePort(int port) {
        if (port < 1) {
            return 1;
        }
        if (port > 65535) {
            return 65535;
        }
        return port;
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

    private static final class LineEditors {
        private final JCheckBox enabled = new JCheckBox("enabled");
        private final JSpinner port = new JSpinner(new SpinnerNumberModel(18080, 1, 65535, 1));
        private final JLabel running = new JLabel("false");
    }
}
