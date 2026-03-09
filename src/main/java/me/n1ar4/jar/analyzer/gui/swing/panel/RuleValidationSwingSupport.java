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

import me.n1ar4.jar.analyzer.gui.swing.SwingI18n;
import me.n1ar4.jar.analyzer.rules.ModelRegistry;
import me.n1ar4.jar.analyzer.rules.RuleValidationViews;
import me.n1ar4.jar.analyzer.rules.SinkRuleRegistry;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RuleValidationSwingSupport {
    private RuleValidationSwingSupport() {
    }

    static void showDialog(Component parent) {
        showDialog(parent, Map.of(), List.of());
    }

    static void showDialog(Component parent, Map<String, Object> initialSummary, List<Map<String, Object>> initialIssues) {
        JDialog dialog = createDialog(parent, SwingI18n.tr("规则校验", "Rule Validation"));
        dialog.setLayout(new java.awt.BorderLayout(8, 8));

        JLabel summaryLabel = new JLabel(buildStatusText(initialSummary));
        summaryLabel.setForeground(resolveStatusColor(initialSummary));
        JComboBox<ScopeOption> scopeBox = new JComboBox<>(new ScopeOption[]{
                new ScopeOption("all", SwingI18n.tr("全部", "all")),
                new ScopeOption("model", SwingI18n.tr("model", "model")),
                new ScopeOption("source", SwingI18n.tr("source", "source")),
                new ScopeOption("sink", SwingI18n.tr("sink", "sink"))
        });
        JTextArea issueArea = new JTextArea();
        issueArea.setEditable(false);
        issueArea.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(issueArea);
        scrollPane.setPreferredSize(new Dimension(820, 420));
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        JButton refreshButton = new JButton(SwingI18n.tr("刷新", "Refresh"));
        JButton closeButton = new JButton(SwingI18n.tr("关闭", "Close"));

        final Map<String, Object>[] summaryRef = new Map[]{normalizeSummary(initialSummary)};
        final List<Map<String, Object>>[] issueRef = new List[]{normalizeIssues(initialIssues)};

        Runnable repaintView = () -> {
            summaryLabel.setText(buildStatusText(summaryRef[0]));
            summaryLabel.setForeground(resolveStatusColor(summaryRef[0]));
            issueArea.setText(buildDetailsText(summaryRef[0], issueRef[0]));
            issueArea.setCaretPosition(0);
        };

        Runnable reloadCurrentScope = () -> loadScope(scopeBox, summaryRef, issueRef, repaintView, false);
        refreshButton.addActionListener(e -> loadScope(scopeBox, summaryRef, issueRef, repaintView, true));
        closeButton.addActionListener(e -> dialog.dispose());
        scopeBox.addActionListener(e -> reloadCurrentScope.run());

        JPanel top = new JPanel(new java.awt.BorderLayout(8, 8));
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        top.add(summaryLabel, java.awt.BorderLayout.CENTER);
        JPanel scopePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        scopePanel.add(new JLabel(SwingI18n.tr("范围", "Scope")));
        scopePanel.add(scopeBox);
        top.add(scopePanel, java.awt.BorderLayout.EAST);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        bottom.add(refreshButton);
        bottom.add(closeButton);

        dialog.add(top, java.awt.BorderLayout.NORTH);
        dialog.add(scrollPane, java.awt.BorderLayout.CENTER);
        dialog.add(bottom, java.awt.BorderLayout.SOUTH);
        dialog.setSize(860, 560);
        dialog.setLocationRelativeTo(parent);

        if (summaryRef[0].isEmpty() && issueRef[0].isEmpty()) {
            loadScope(scopeBox, summaryRef, issueRef, repaintView, false);
        } else {
            repaintView.run();
        }
        dialog.setVisible(true);
    }

    static String buildStatusText(Map<String, Object> summary) {
        if (summary == null || summary.isEmpty()) {
            return SwingI18n.tr("规则校验: 加载中", "Rule Validation: loading");
        }
        int compiled = intValue(summary.get("compiledRules"));
        int errors = intValue(summary.get("errorCount"));
        int warnings = intValue(summary.get("warningCount"));
        String state;
        if (errors > 0) {
            state = SwingI18n.tr("错误", "error");
        } else if (warnings > 0) {
            state = SwingI18n.tr("警告", "warning");
        } else {
            state = "OK";
        }
        return SwingI18n.tr("规则校验", "Rule Validation")
                + ": " + state
                + " | " + SwingI18n.tr("已编译", "compiled") + " " + compiled
                + " | " + SwingI18n.tr("错误", "errors") + " " + errors
                + " | " + SwingI18n.tr("警告", "warnings") + " " + warnings;
    }

    static String buildDetailsText(Map<String, Object> summary, List<Map<String, Object>> issues) {
        StringBuilder out = new StringBuilder();
        out.append(buildStatusText(summary)).append('\n');
        appendScopeSummary(out, summary, "model", SwingI18n.tr("model", "model"));
        appendScopeSummary(out, summary, "source", SwingI18n.tr("source", "source"));
        appendScopeSummary(out, summary, "modelSource", SwingI18n.tr("model/source", "model/source"));
        appendScopeSummary(out, summary, "sink", SwingI18n.tr("sink", "sink"));
        out.append('\n').append(SwingI18n.tr("问题列表", "Issues")).append('\n');
        if (issues == null || issues.isEmpty()) {
            out.append(SwingI18n.tr("无规则校验问题", "No rule validation issues")).append('\n');
            return out.toString();
        }
        int idx = 1;
        for (Map<String, Object> issue : issues) {
            if (issue == null || issue.isEmpty()) {
                continue;
            }
            out.append(idx++).append(". [")
                    .append(textValue(issue.get("level"), "-"))
                    .append("] ")
                    .append(textValue(issue.get("scope"), "all"))
                    .append(" | ")
                    .append(textValue(issue.get("code"), "-"));
            String path = textValue(issue.get("path"), "");
            if (!path.isBlank()) {
                out.append(" | ").append(path);
            }
            Integer index = nullableInt(issue.get("index"));
            if (index != null && index > 0) {
                out.append(" #").append(index);
            }
            String id = textValue(issue.get("id"), "");
            if (!id.isBlank()) {
                out.append(" | id=").append(id);
            }
            String kind = textValue(issue.get("kind"), "");
            if (!kind.isBlank()) {
                out.append(" | kind=").append(kind);
            }
            out.append('\n')
                    .append("   ")
                    .append(textValue(issue.get("message"), "-"))
                    .append('\n');
        }
        return out.toString();
    }

    static Color resolveStatusColor(Map<String, Object> summary) {
        if (summary == null || summary.isEmpty()) {
            return Color.DARK_GRAY;
        }
        int errors = intValue(summary.get("errorCount"));
        int warnings = intValue(summary.get("warningCount"));
        if (errors > 0) {
            return new Color(170, 40, 40);
        }
        if (warnings > 0) {
            return new Color(170, 120, 0);
        }
        return new Color(0, 128, 0);
    }

    private static void appendScopeSummary(StringBuilder out,
                                           Map<String, Object> summary,
                                           String key,
                                           String label) {
        if (out == null || summary == null || summary.isEmpty()) {
            return;
        }
        Object value = summary.get(key);
        if (!(value instanceof Map<?, ?> scopeMap)) {
            return;
        }
        out.append(label)
                .append(": ok=")
                .append(boolValue(scopeMap.get("ok")))
                .append(", compiled=")
                .append(intValue(scopeMap.get("compiledRules")))
                .append(", errors=")
                .append(intValue(scopeMap.get("errorCount")))
                .append(", warnings=")
                .append(intValue(scopeMap.get("warningCount")))
                .append('\n');
    }

    private static int intValue(Object value) {
        Integer parsed = nullableInt(value);
        return parsed == null ? 0 : parsed;
    }

    private static void loadScope(JComboBox<ScopeOption> scopeBox,
                                  Map<String, Object>[] summaryRef,
                                  List<Map<String, Object>>[] issueRef,
                                  Runnable repaintView,
                                  boolean forceRefresh) {
        String scope = selectedScope(scopeBox);
        Thread.ofVirtual().name("gui-rule-validation-dialog").start(() -> {
            if (forceRefresh) {
                ModelRegistry.checkNow();
                SinkRuleRegistry.checkNow();
            }
            Map<String, Object> summary = RuleValidationViews.combinedValidationMap();
            List<Map<String, Object>> issues = RuleValidationViews.issueMaps(scope);
            SwingUtilities.invokeLater(() -> {
                summaryRef[0] = normalizeSummary(summary);
                issueRef[0] = normalizeIssues(issues);
                repaintView.run();
            });
        });
    }

    private static String selectedScope(JComboBox<ScopeOption> scopeBox) {
        Object selected = scopeBox == null ? null : scopeBox.getSelectedItem();
        if (selected instanceof ScopeOption option) {
            return option.scope();
        }
        return "all";
    }

    private static Map<String, Object> normalizeSummary(Map<String, Object> summary) {
        if (summary == null || summary.isEmpty()) {
            return Map.of();
        }
        return new LinkedHashMap<>(summary);
    }

    private static List<Map<String, Object>> normalizeIssues(List<Map<String, Object>> issues) {
        if (issues == null || issues.isEmpty()) {
            return List.of();
        }
        return List.copyOf(issues);
    }

    private static JDialog createDialog(Component parent, String title) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        if (owner instanceof java.awt.Frame frame) {
            return new JDialog(frame, title, true);
        }
        if (owner instanceof java.awt.Dialog dialog) {
            return new JDialog(dialog, title, true);
        }
        return new JDialog((java.awt.Frame) null, title, true);
    }

    private static Integer nullableInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = textValue(value, "");
        if (text.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean boolValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return "true".equalsIgnoreCase(textValue(value, ""));
    }

    private static String textValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    private record ScopeOption(String scope, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
