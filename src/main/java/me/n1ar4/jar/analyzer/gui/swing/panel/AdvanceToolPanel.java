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
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingConfigSnapshotDto;
import me.n1ar4.jar.analyzer.gui.swing.SwingI18n;
import me.n1ar4.jar.analyzer.gui.swing.SwingUiApplyGuard;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;

public final class AdvanceToolPanel extends JPanel {
    private final JCheckBox showInnerClassBox = new JCheckBox("show inner class");
    private final JCheckBox fixClassPathBox = new JCheckBox("fix class path");
    private final JCheckBox sortByMethodBox = new JCheckBox("search sort by method");
    private final JCheckBox sortByClassBox = new JCheckBox("search sort by class");
    private final JCheckBox logAllSqlBox = new JCheckBox("log all sql");
    private final JCheckBox groupTreeByJarBox = new JCheckBox("group tree by jar");
    private final JCheckBox mergePackageRootBox = new JCheckBox("merge package root");
    private final JCheckBox fixMethodImplBox = new JCheckBox("fix method impl");
    private final JCheckBox quickModeBox = new JCheckBox("quick mode");
    private final JCheckBox stripeShowNamesBox = new JCheckBox("stripe show names");
    private final JSpinner stripeWidthSpin = new JSpinner(new SpinnerNumberModel(40, 40, 100, 1));
    private final JLabel statusValue = new JLabel(SwingI18n.tr("就绪", "ready"));
    private final SwingUiApplyGuard.Throttle snapshotThrottle = new SwingUiApplyGuard.Throttle();

    private volatile boolean syncing;
    private boolean hasSnapshot;
    private String snapshotLang = "";
    private String snapshotTheme = "";

    public AdvanceToolPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        initUi();
    }

    private void initUi() {
        JPanel pluginPanel = new JPanel(new GridLayout(5, 4, 4, 4));
        pluginPanel.setBorder(BorderFactory.createTitledBorder("Plugins / Tools"));
        pluginPanel.add(button("Global Search", () -> RuntimeFacades.tooling().openGlobalSearchTool()));
        pluginPanel.add(button("All Strings", () -> RuntimeFacades.tooling().openAllStringsTool()));
        pluginPanel.add(button("Cypher Console", () -> RuntimeFacades.tooling().openCypherConsoleTool()));
        pluginPanel.add(button("Encode", () -> RuntimeFacades.tooling().openEncodeTool()));
        pluginPanel.add(button("Socket Listener", () -> RuntimeFacades.tooling().openListenerTool()));
        pluginPanel.add(button("EL Search", () -> RuntimeFacades.tooling().openElSearchTool()));
        pluginPanel.add(button("Serialization", () -> RuntimeFacades.tooling().openSerializationTool()));
        pluginPanel.add(button("BCEL", () -> RuntimeFacades.tooling().openBcelTool()));
        pluginPanel.add(button("Repeater", () -> RuntimeFacades.tooling().openRepeaterTool()));
        pluginPanel.add(button("Obfuscation", () -> RuntimeFacades.tooling().openObfuscationTool()));
        pluginPanel.add(button("Remote Load", () -> RuntimeFacades.tooling().openRemoteLoadTool()));
        pluginPanel.add(button("Proxy", () -> RuntimeFacades.tooling().openProxyTool()));
        pluginPanel.add(button("Partition", () -> RuntimeFacades.tooling().openPartitionTool()));
        pluginPanel.add(button("System Monitor", () -> RuntimeFacades.tooling().openSystemMonitorTool()));
        pluginPanel.add(button("Remote Tomcat", () -> RuntimeFacades.tooling().openRemoteTomcatAnalyzer()));
        pluginPanel.add(button("Bytecode Debugger", () -> RuntimeFacades.tooling().openBytecodeDebugger()));

        JPanel analysisPanel = new JPanel(new GridLayout(2, 4, 4, 4));
        analysisPanel.setBorder(BorderFactory.createTitledBorder("Analysis"));
        analysisPanel.add(button("CFG", () -> RuntimeFacades.tooling().openCfgTool()));
        analysisPanel.add(button("Frame", () -> RuntimeFacades.tooling().openFrameTool(false)));
        analysisPanel.add(button("Full Frame", () -> RuntimeFacades.tooling().openFrameTool(true)));
        analysisPanel.add(button("Opcode", () -> RuntimeFacades.tooling().openOpcodeTool()));
        analysisPanel.add(button("ASM", () -> RuntimeFacades.tooling().openAsmTool()));
        analysisPanel.add(button("HTML Graph", () -> RuntimeFacades.tooling().openHtmlGraph()));
        analysisPanel.add(button("JD-GUI", () -> RuntimeFacades.tooling().openJdGui()));
        analysisPanel.add(button("Docs", () -> RuntimeFacades.tooling().openDocs()));

        JPanel actionPanel = new JPanel(new GridLayout(2, 4, 4, 4));
        actionPanel.setBorder(BorderFactory.createTitledBorder("Actions"));
        actionPanel.add(button("Export", () -> RuntimeFacades.tooling().openExportTool()));
        actionPanel.add(button("Version", () -> RuntimeFacades.tooling().openVersionInfo()));
        actionPanel.add(button("Changelog", () -> RuntimeFacades.tooling().openChangelog()));
        actionPanel.add(button("Thanks", () -> RuntimeFacades.tooling().openThanks()));
        actionPanel.add(button("Report Bug", () -> RuntimeFacades.tooling().openReportBug()));
        actionPanel.add(button("Project Site", () -> RuntimeFacades.tooling().openProjectSite()));
        actionPanel.add(button("Flappy", () -> RuntimeFacades.tooling().openFlappyGame()));
        actionPanel.add(button("Pocker", () -> RuntimeFacades.tooling().openPockerGame()));

        JPanel configPanel = new JPanel(new GridLayout(6, 2, 4, 4));
        configPanel.setBorder(BorderFactory.createTitledBorder("Runtime Config"));
        configPanel.add(showInnerClassBox);
        configPanel.add(fixClassPathBox);
        configPanel.add(sortByMethodBox);
        configPanel.add(sortByClassBox);
        configPanel.add(logAllSqlBox);
        configPanel.add(groupTreeByJarBox);
        configPanel.add(mergePackageRootBox);
        configPanel.add(fixMethodImplBox);
        configPanel.add(quickModeBox);
        configPanel.add(stripeShowNamesBox);
        configPanel.add(new JLabel("stripe width"));
        configPanel.add(stripeWidthSpin);

        showInnerClassBox.addItemListener(e -> toggleIfUser(e, RuntimeFacades.tooling()::toggleShowInnerClass));
        fixClassPathBox.addItemListener(e -> toggleIfUser(e, RuntimeFacades.tooling()::toggleFixClassPath));
        sortByMethodBox.addItemListener(e -> toggleIfUser(e, RuntimeFacades.tooling()::setSortByMethod));
        sortByClassBox.addItemListener(e -> toggleIfUser(e, RuntimeFacades.tooling()::setSortByClass));
        logAllSqlBox.addItemListener(e -> toggleIfUser(e, RuntimeFacades.tooling()::toggleLogAllSql));
        groupTreeByJarBox.addItemListener(e -> toggleIfUser(e, RuntimeFacades.tooling()::toggleGroupTreeByJar));
        mergePackageRootBox.addItemListener(e -> toggleIfUser(e, RuntimeFacades.tooling()::toggleMergePackageRoot));
        fixMethodImplBox.addItemListener(e -> toggleIfUser(e, RuntimeFacades.tooling()::toggleFixMethodImpl));
        quickModeBox.addItemListener(e -> toggleIfUser(e, RuntimeFacades.tooling()::toggleQuickMode));
        stripeShowNamesBox.addItemListener(e -> {
            if (!syncing && (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED)) {
                RuntimeFacades.tooling().setStripeShowNames(stripeShowNamesBox.isSelected());
            }
        });
        stripeWidthSpin.addChangeListener(e -> {
            if (!syncing) {
                RuntimeFacades.tooling().setStripeWidth((Integer) stripeWidthSpin.getValue());
            }
        });

        JPanel languageTheme = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        languageTheme.setBorder(BorderFactory.createTitledBorder("Language / Theme"));
        languageTheme.add(button("ZH", () -> RuntimeFacades.tooling().setLanguageChinese()));
        languageTheme.add(button("EN", () -> RuntimeFacades.tooling().setLanguageEnglish()));
        languageTheme.add(button("Theme Default", () -> RuntimeFacades.tooling().useThemeDefault()));
        languageTheme.add(button("Theme Dark", () -> RuntimeFacades.tooling().useThemeDark()));

        JPanel top = new JPanel(new BorderLayout(6, 6));
        top.add(pluginPanel, BorderLayout.NORTH);
        top.add(analysisPanel, BorderLayout.CENTER);
        top.add(actionPanel, BorderLayout.SOUTH);

        JPanel bottom = new JPanel(new BorderLayout(6, 6));
        bottom.add(configPanel, BorderLayout.NORTH);
        bottom.add(languageTheme, BorderLayout.CENTER);

        JPanel status = new JPanel(new BorderLayout());
        status.add(new JLabel("status"), BorderLayout.WEST);
        status.add(statusValue, BorderLayout.CENTER);
        bottom.add(status, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(bottom, BorderLayout.CENTER);
        applyLanguage();
    }

    public void applySnapshot(ToolingConfigSnapshotDto snapshot) {
        if (snapshot == null) {
            return;
        }
        if (!SwingUiApplyGuard.ensureEdt("AdvanceToolPanel.applySnapshot", () -> applySnapshot(snapshot))) {
            return;
        }
        if (!snapshotThrottle.allow(SwingUiApplyGuard.fingerprint(snapshot))) {
            return;
        }
        syncing = true;
        try {
            showInnerClassBox.setSelected(snapshot.showInnerClass());
            fixClassPathBox.setSelected(snapshot.fixClassPath());
            sortByMethodBox.setSelected(snapshot.sortByMethod());
            sortByClassBox.setSelected(snapshot.sortByClass());
            logAllSqlBox.setSelected(snapshot.logAllSql());
            groupTreeByJarBox.setSelected(snapshot.groupTreeByJar());
            mergePackageRootBox.setSelected(snapshot.mergePackageRoot());
            fixMethodImplBox.setSelected(snapshot.fixMethodImpl());
            quickModeBox.setSelected(snapshot.quickMode());
            stripeShowNamesBox.setSelected(snapshot.stripeShowNames());
            stripeWidthSpin.setValue(snapshot.stripeWidth());
            hasSnapshot = true;
            snapshotLang = snapshot.language();
            snapshotTheme = snapshot.theme();
            updateStatusText();
        } finally {
            syncing = false;
        }
    }

    private void toggleIfUser(ItemEvent e, Runnable action) {
        if (!syncing && (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED)) {
            action.run();
        }
    }

    private JButton button(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> action.run());
        return button;
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
        statusValue.setText(SwingI18n.tr("语言=", "lang=") + safe(snapshotLang)
                + ", " + SwingI18n.tr("主题=", "theme=") + safe(snapshotTheme));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
