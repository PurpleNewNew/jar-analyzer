/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.action;

import me.n1ar4.jar.analyzer.config.ConfigEngine;
import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.gui.GlobalOptions;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;
import me.n1ar4.jar.analyzer.mcp.McpLine;
import me.n1ar4.jar.analyzer.mcp.McpManager;
import me.n1ar4.jar.analyzer.mcp.McpReportWebConfig;
import me.n1ar4.jar.analyzer.mcp.McpServiceConfig;
import me.n1ar4.jar.analyzer.server.ServerConfig;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.URI;
import java.util.EnumMap;
import java.util.List;

/**
 * GUI integration for embedded MCP servers (placed under the "API" tab).
 */
public final class McpAction {
    private McpAction() {
    }

    public static void register() {
        MainForm instance = MainForm.getInstance();
        if (instance == null) {
            return;
        }
        JPanel apiPanel = instance.getApiPanel();
        JPanel apiShowPanel = instance.getApiShowPanel();
        if (apiPanel == null || apiShowPanel == null) {
            return;
        }

        // Rebuild API tab: keep existing API info panel, append MCP settings below.
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(apiShowPanel, BorderLayout.NORTH);
        wrapper.add(buildMcpPanel(instance), BorderLayout.CENTER);

        apiPanel.removeAll();
        apiPanel.setLayout(new BorderLayout());
        apiPanel.add(wrapper, BorderLayout.CENTER);
        apiPanel.revalidate();
        apiPanel.repaint();
    }

    private static JComponent buildMcpPanel(MainForm instance) {
        ConfigFile cfg = MainForm.getConfig();
        if (cfg == null) {
            cfg = new ConfigFile();
            MainForm.setConfig(cfg);
        }
        final ConfigFile initialCfg = cfg;

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(null, "MCP",
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        JTextField bindField = new JTextField(nvl(cfg.getMcpBind(), "0.0.0.0"));
        JCheckBox authBox = new JCheckBox("Enable MCP Auth (Token required for tools/call)", cfg.isMcpAuth());
        JTextField tokenField = new JTextField(nvl(cfg.getMcpToken(), "JAR-ANALYZER-MCP-TOKEN"));

        EnumMap<McpLine, JCheckBox> enabled = new EnumMap<>(McpLine.class);
        EnumMap<McpLine, JTextField> port = new EnumMap<>(McpLine.class);
        EnumMap<McpLine, JLabel> status = new EnumMap<>(McpLine.class);

        int row = 0;
        row = addRow(form, c, row, new JLabel("Bind"), bindField);
        row = addRow(form, c, row, new JLabel("Token"), tokenField);
        row = addFullRow(form, c, row, authBox);

        row = addFullRow(form, c, row, new JLabel("MCP Lines"));

        row = addLineRow(form, c, row, McpLine.AUDIT_FAST, "audit-fast", cfg.isMcpAuditFastEnabled(),
                String.valueOf(cfg.getMcpAuditFastPort()), enabled, port, status);
        row = addLineRow(form, c, row, McpLine.GRAPH_LITE, "graph-lite", cfg.isMcpGraphLiteEnabled(),
                String.valueOf(cfg.getMcpGraphLitePort()), enabled, port, status);
        row = addLineRow(form, c, row, McpLine.DFS_TAINT, "dfs", cfg.isMcpDfsTaintEnabled(),
                String.valueOf(cfg.getMcpDfsTaintPort()), enabled, port, status);
        row = addLineRow(form, c, row, McpLine.SCA_LEAK, "sca-leak", cfg.isMcpScaLeakEnabled(),
                String.valueOf(cfg.getMcpScaLeakPort()), enabled, port, status);
        row = addLineRow(form, c, row, McpLine.VUL_RULES, "vul-rules", cfg.isMcpVulRulesEnabled(),
                String.valueOf(cfg.getMcpVulRulesPort()), enabled, port, status);
        row = addLineRow(form, c, row, McpLine.REPORT, "report", cfg.isMcpReportEnabled(),
                String.valueOf(cfg.getMcpReportPort()), enabled, port, status);

        row = addFullRow(form, c, row, new JLabel("Report Web UI"));
        JCheckBox reportWebEnabled = new JCheckBox("Enable Report Web UI", cfg.isMcpReportWebEnabled());
        JTextField reportWebHost = new JTextField(nvl(cfg.getMcpReportWebHost(), "127.0.0.1"));
        JTextField reportWebPort = new JTextField(String.valueOf(cfg.getMcpReportWebPort()));
        JLabel reportWebStatus = new JLabel();
        setStatus(reportWebStatus, McpManager.get().isReportWebRunning());
        row = addRow(form, c, row, reportWebEnabled, reportWebStatus);
        row = addRow(form, c, row, new JLabel("Host"), reportWebHost);
        row = addRow(form, c, row, new JLabel("Port"), reportWebPort);

        JButton applyStart = new JButton("Apply + Start Enabled");
        JButton stopAll = new JButton("Stop All");
        JButton openReport = new JButton("Open Report UI");
        JPanel btns = new JPanel();
        btns.add(applyStart);
        btns.add(stopAll);
        btns.add(openReport);

        JPanel body = new JPanel(new BorderLayout());
        body.add(form, BorderLayout.NORTH);
        body.add(btns, BorderLayout.SOUTH);

        panel.add(new JScrollPane(body), BorderLayout.CENTER);

        Runnable refresh = () -> {
            for (McpLine line : McpLine.values()) {
                if (line == null) {
                    continue;
                }
                JLabel st = status.get(line);
                if (st == null) {
                    continue;
                }
                setStatus(st, McpManager.get().isRunning(line));
            }
            setStatus(reportWebStatus, McpManager.get().isReportWebRunning());
        };

        // Auto start on GUI load if enabled in config.
        UiExecutor.runAsync(() -> {
            List<String> errors = McpManager.get().startEnabled(
                    readLineConfigs(initialCfg),
                    readReportWebConfig(initialCfg),
                    safeApiConfig()
            );
            UiExecutor.runOnEdt(() -> {
                refresh.run();
                if (errors != null && !errors.isEmpty()) {
                    UiExecutor.showMessage(instance.getMasterPanel(),
                            String.join("\n", errors),
                            "MCP Start Failed",
                            javax.swing.JOptionPane.WARNING_MESSAGE);
                }
            });
        });

        applyStart.addActionListener(e -> UiExecutor.runAsync(() -> {
            ConfigFile config = MainForm.getConfig();
            if (config == null) {
                config = new ConfigFile();
            }
            applyUiToConfig(config, bindField, authBox, tokenField, enabled, port,
                    reportWebEnabled, reportWebHost, reportWebPort);
            MainForm.setConfig(config);
            ConfigEngine.saveConfig(config);

            List<String> errors = McpManager.get().restartAll(
                    readLineConfigs(config),
                    readReportWebConfig(config),
                    safeApiConfig()
            );
            UiExecutor.runOnEdt(() -> {
                refresh.run();
                if (errors != null && !errors.isEmpty()) {
                    UiExecutor.showMessage(instance.getMasterPanel(),
                            String.join("\n", errors),
                            "MCP Start Failed",
                            javax.swing.JOptionPane.WARNING_MESSAGE);
                } else {
                    UiExecutor.showMessage(instance.getMasterPanel(), "MCP servers started");
                }
            });
        }));

        stopAll.addActionListener(e -> UiExecutor.runAsync(() -> {
            McpManager.get().stopAll();
            UiExecutor.runOnEdt(() -> {
                refresh.run();
                UiExecutor.showMessage(instance.getMasterPanel(), "MCP servers stopped");
            });
        }));

        openReport.addActionListener(e -> {
            String host = reportWebHost.getText() == null ? "127.0.0.1" : reportWebHost.getText().trim();
            String p = reportWebPort.getText() == null ? "20080" : reportWebPort.getText().trim();
            try {
                Desktop.getDesktop().browse(new URI("http://" + host + ":" + p + "/"));
            } catch (Exception ignored) {
            }
        });

        // Keep status correct if user switches tabs quickly.
        SwingUtilities.invokeLater(refresh);
        return panel;
    }

    private static EnumMap<McpLine, McpServiceConfig> readLineConfigs(ConfigFile cfg) {
        EnumMap<McpLine, McpServiceConfig> map = new EnumMap<>(McpLine.class);
        if (cfg == null) {
            return map;
        }
        String bind = nvl(cfg.getMcpBind(), "0.0.0.0");
        boolean auth = cfg.isMcpAuth();
        String token = nvl(cfg.getMcpToken(), "JAR-ANALYZER-MCP-TOKEN");

        map.put(McpLine.AUDIT_FAST, service(cfg.isMcpAuditFastEnabled(), bind, cfg.getMcpAuditFastPort(), auth, token));
        map.put(McpLine.GRAPH_LITE, service(cfg.isMcpGraphLiteEnabled(), bind, cfg.getMcpGraphLitePort(), auth, token));
        map.put(McpLine.DFS_TAINT, service(cfg.isMcpDfsTaintEnabled(), bind, cfg.getMcpDfsTaintPort(), auth, token));
        map.put(McpLine.SCA_LEAK, service(cfg.isMcpScaLeakEnabled(), bind, cfg.getMcpScaLeakPort(), auth, token));
        map.put(McpLine.VUL_RULES, service(cfg.isMcpVulRulesEnabled(), bind, cfg.getMcpVulRulesPort(), auth, token));
        map.put(McpLine.REPORT, service(cfg.isMcpReportEnabled(), bind, cfg.getMcpReportPort(), auth, token));
        return map;
    }

    private static McpReportWebConfig readReportWebConfig(ConfigFile cfg) {
        McpReportWebConfig c = new McpReportWebConfig();
        if (cfg == null) {
            return c;
        }
        c.setEnabled(cfg.isMcpReportWebEnabled());
        c.setHost(cfg.getMcpReportWebHost());
        c.setPort(cfg.getMcpReportWebPort());
        return c;
    }

    private static McpServiceConfig service(boolean enabled, String bind, int port, boolean auth, String token) {
        McpServiceConfig c = new McpServiceConfig();
        c.setEnabled(enabled);
        c.setBind(bind);
        c.setPort(port);
        c.setAuth(auth);
        c.setToken(token);
        return c;
    }

    private static ServerConfig safeApiConfig() {
        ServerConfig cfg = GlobalOptions.getServerConfig();
        return cfg == null ? new ServerConfig() : cfg;
    }

    private static void applyUiToConfig(ConfigFile cfg,
                                        JTextField bindField,
                                        JCheckBox authBox,
                                        JTextField tokenField,
                                        EnumMap<McpLine, JCheckBox> enabled,
                                        EnumMap<McpLine, JTextField> port,
                                        JCheckBox reportWebEnabled,
                                        JTextField reportWebHost,
                                        JTextField reportWebPort) {
        if (cfg == null) {
            return;
        }
        cfg.setMcpBind(bindField.getText());
        cfg.setMcpAuth(authBox.isSelected());
        cfg.setMcpToken(tokenField.getText());

        cfg.setMcpAuditFastEnabled(isSelected(enabled.get(McpLine.AUDIT_FAST)));
        cfg.setMcpAuditFastPort(parsePort(port.get(McpLine.AUDIT_FAST), cfg.getMcpAuditFastPort()));

        cfg.setMcpGraphLiteEnabled(isSelected(enabled.get(McpLine.GRAPH_LITE)));
        cfg.setMcpGraphLitePort(parsePort(port.get(McpLine.GRAPH_LITE), cfg.getMcpGraphLitePort()));

        cfg.setMcpDfsTaintEnabled(isSelected(enabled.get(McpLine.DFS_TAINT)));
        cfg.setMcpDfsTaintPort(parsePort(port.get(McpLine.DFS_TAINT), cfg.getMcpDfsTaintPort()));

        cfg.setMcpScaLeakEnabled(isSelected(enabled.get(McpLine.SCA_LEAK)));
        cfg.setMcpScaLeakPort(parsePort(port.get(McpLine.SCA_LEAK), cfg.getMcpScaLeakPort()));

        cfg.setMcpVulRulesEnabled(isSelected(enabled.get(McpLine.VUL_RULES)));
        cfg.setMcpVulRulesPort(parsePort(port.get(McpLine.VUL_RULES), cfg.getMcpVulRulesPort()));

        cfg.setMcpReportEnabled(isSelected(enabled.get(McpLine.REPORT)));
        cfg.setMcpReportPort(parsePort(port.get(McpLine.REPORT), cfg.getMcpReportPort()));

        cfg.setMcpReportWebEnabled(reportWebEnabled.isSelected());
        cfg.setMcpReportWebHost(reportWebHost.getText());
        cfg.setMcpReportWebPort(parsePort(reportWebPort, cfg.getMcpReportWebPort()));
    }

    private static boolean isSelected(JCheckBox box) {
        return box != null && box.isSelected();
    }

    private static int parsePort(JTextField field, int def) {
        if (field == null) {
            return def;
        }
        String v = field.getText();
        if (v == null || v.trim().isEmpty()) {
            return def;
        }
        try {
            int p = Integer.parseInt(v.trim());
            if (p < 1 || p > 65535) {
                return def;
            }
            return p;
        } catch (NumberFormatException ignored) {
            return def;
        }
    }

    private static void setStatus(JLabel label, boolean running) {
        if (label == null) {
            return;
        }
        label.setText(running ? "RUNNING" : "STOPPED");
        label.setForeground(running ? new Color(0, 128, 0) : Color.RED);
    }

    private static int addRow(JPanel form, GridBagConstraints c, int row, JComponent left, JComponent right) {
        c.gridy = row;
        c.gridx = 0;
        c.weightx = 0.0;
        form.add(left, c);
        c.gridx = 1;
        c.weightx = 1.0;
        form.add(right, c);
        return row + 1;
    }

    private static int addFullRow(JPanel form, GridBagConstraints c, int row, JComponent comp) {
        c.gridy = row;
        c.gridx = 0;
        c.gridwidth = 2;
        c.weightx = 1.0;
        form.add(comp, c);
        c.gridwidth = 1;
        return row + 1;
    }

    private static int addLineRow(JPanel form,
                                  GridBagConstraints c,
                                  int row,
                                  McpLine line,
                                  String label,
                                  boolean enabledVal,
                                  String portVal,
                                  EnumMap<McpLine, JCheckBox> enabled,
                                  EnumMap<McpLine, JTextField> port,
                                  EnumMap<McpLine, JLabel> status) {
        JCheckBox box = new JCheckBox(label, enabledVal);
        JTextField portField = new JTextField(portVal);
        JLabel st = new JLabel();
        setStatus(st, McpManager.get().isRunning(line));
        enabled.put(line, box);
        port.put(line, portField);
        status.put(line, st);

        JPanel right = new JPanel(new BorderLayout());
        right.add(portField, BorderLayout.CENTER);
        right.add(st, BorderLayout.EAST);
        return addRow(form, c, row, box, right);
    }

    private static String nvl(String v, String def) {
        if (v == null) {
            return def;
        }
        String s = v.trim();
        return s.isEmpty() ? def : s;
    }
}
