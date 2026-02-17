/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.swing;

import me.n1ar4.jar.analyzer.cli.StartCmd;
import me.n1ar4.jar.analyzer.gui.runtime.api.RuntimeFacades;
import me.n1ar4.jar.analyzer.gui.runtime.model.ApiInfoDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.CallGraphSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ChainsResultItemDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ChainsSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.EditorDocumentDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.GadgetSettingsDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.GadgetSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.LeakSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.McpConfigDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.MethodNavDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.NoteSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ScaSettingsDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ScaSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingConfigSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingWindowAction;
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingWindowPayload;
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingWindowRequest;
import me.n1ar4.jar.analyzer.gui.runtime.model.TreeNodeDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.WebSnapshotDto;
import me.n1ar4.jar.analyzer.gui.swing.panel.AdvanceToolPanel;
import me.n1ar4.jar.analyzer.gui.swing.panel.ApiMcpToolPanel;
import me.n1ar4.jar.analyzer.gui.swing.panel.CallToolPanel;
import me.n1ar4.jar.analyzer.gui.swing.panel.ChainsToolPanel;
import me.n1ar4.jar.analyzer.gui.swing.panel.GadgetToolPanel;
import me.n1ar4.jar.analyzer.gui.swing.panel.ImplToolPanel;
import me.n1ar4.jar.analyzer.gui.swing.panel.LeakToolPanel;
import me.n1ar4.jar.analyzer.gui.swing.panel.NoteToolPanel;
import me.n1ar4.jar.analyzer.gui.swing.panel.ScaToolPanel;
import me.n1ar4.jar.analyzer.gui.swing.panel.SearchToolPanel;
import me.n1ar4.jar.analyzer.gui.swing.panel.StartToolPanel;
import me.n1ar4.jar.analyzer.gui.swing.panel.WebToolPanel;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SwingMainFrame extends JFrame {
    private static final Logger logger = LogManager.getLogger();
    private static final int REFRESH_INTERVAL_MS = 700;
    private static final long TREE_REFRESH_INTERVAL_MS = 3000;
    private static final String EMPTY_CARD = "__EMPTY__";
    private static final Color SHELL_BG = new Color(0xECECEC);
    private static final Color SHELL_LINE = new Color(0xC8C8C8);
    private static final int TOP_TOOLBAR_BUTTON_SIZE = 22;
    private static final int ACTIVE_DIVIDER_SIZE = 6;
    private static final int LOCKED_DIVIDER_SIZE = 0;
    private static final int LEFT_PANE_DEFAULT_WIDTH = 286;
    private static final int LEFT_PANE_MIN_WIDTH = 220;
    private static final int RIGHT_PANE_CONTENT_MIN_WIDTH = 280;
    private static final double RIGHT_PANE_MAX_WIDTH_RATIO = 0.42D;
    private static final int LEFT_STRIPE_WIDTH = 26;
    private static final Pattern STRUCTURE_METHOD_PATTERN = Pattern.compile(
            "^\\s*(?:(?:public|protected|private|static|final|native|synchronized|abstract|default|strictfp|transient)\\s+)*"
                    + "[\\w$<>,\\[\\]\\.?\\s]+\\s+([A-Za-z_$][\\w$]*)\\s*\\([^;{}]*\\)\\s*(?:\\{|throws\\b).*"
    );
    private static final Pattern STRUCTURE_SKIP_PATTERN = Pattern.compile(
            "^\\s*(?:if|for|while|switch|catch|try|do|return|throw|new)\\b.*"
    );

    private final StartCmd startCmd;
    private final AtomicBoolean refreshBusy = new AtomicBoolean(false);
    private final AtomicBoolean forceTreeRefresh = new AtomicBoolean(true);

    private final StartToolPanel startPanel = new StartToolPanel();
    private final SearchToolPanel searchPanel = new SearchToolPanel();
    private final CallToolPanel callPanel = new CallToolPanel();
    private final ImplToolPanel implPanel = new ImplToolPanel();
    private final WebToolPanel webPanel = new WebToolPanel();
    private final NoteToolPanel notePanel = new NoteToolPanel();
    private final ScaToolPanel scaPanel = new ScaToolPanel();
    private final LeakToolPanel leakPanel = new LeakToolPanel();
    private final GadgetToolPanel gadgetPanel = new GadgetToolPanel();
    private final AdvanceToolPanel advancePanel = new AdvanceToolPanel();
    private final ChainsToolPanel chainsPanel = new ChainsToolPanel();
    private final ApiMcpToolPanel apiPanel = new ApiMcpToolPanel();

    private final Map<ToolTab, JPanel> topPanels = new EnumMap<>(ToolTab.class);
    private final Map<ToolTab, JToggleButton> stripeButtons = new EnumMap<>(ToolTab.class);

    private final JTextField treeSearchField = new JTextField();
    private final JLabel treeStatusValue = new JLabel("ready");
    private final DefaultTreeModel treeModel = new DefaultTreeModel(new DefaultMutableTreeNode("workspace"));
    private final JTree projectTree = new JTree(treeModel);
    private final CardLayout treeCardLayout = new CardLayout();
    private final JPanel treeCardPanel = new JPanel(treeCardLayout);
    private final JLabel leftEmptyLabel = new JLabel("请打开文件");
    private final DefaultListModel<StructureItem> structureModel = new DefaultListModel<>();
    private final JList<StructureItem> structureList = new JList<>(structureModel);
    private final JLabel structureStatusValue = new JLabel("0");
    private final JSplitPane leftToolSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    private final JToggleButton leftTreeStripeButton = new VerticalStripeToggleButton("目录树");
    private final JToggleButton leftStructureStripeButton = new VerticalStripeToggleButton("结构");

    private final JTextField currentJarField = readonlyField();
    private final JTextField currentClassField = readonlyField();
    private final JTextField currentMethodField = readonlyField();
    private final JTextField editorSearchField = new JTextField();
    private final JLabel editorStatusValue = new JLabel("ready");
    private final RSyntaxTextArea editorArea = new RSyntaxTextArea();
    private final JTabbedPane workbenchTabs = new JTabbedPane();
    private JPanel startPageView;
    private JPanel codePageView;
    private final JTextArea recentProjectArea = new JTextArea();

    private final JToolBar rightStripe = new JToolBar(JToolBar.VERTICAL);
    private final JPanel rightContentHost = new JPanel(new BorderLayout());
    private final JPanel topCards = new JPanel(new java.awt.CardLayout());
    private final JLabel topTitle = new JLabel("工具窗");

    private JSplitPane leftCenterSplit;
    private JSplitPane rootSplit;
    private Timer refreshTimer;

    private ToolTab topTab = ToolTab.START;
    private boolean leftCollapsed;
    private boolean treePanelCollapsed;
    private boolean structurePanelCollapsed;
    private boolean rightCollapsed;
    private int expandedLeftWidth = LEFT_PANE_DEFAULT_WIDTH;
    private int stripeWidth = 48;
    private int expandedWidth = 380;
    private boolean rootDividerAdjusting;
    private long suppressStartPageUntil;
    private String lastTreeKeyword = "";
    private long lastTreeRefreshAt;

    public SwingMainFrame(StartCmd startCmd) {
        super("*New Project - jadx-gui");
        this.startCmd = startCmd;
        initFrame();
        initLayout();
        initActions();
        registerToolingWindowConsumer();
        refreshAsync();
        refreshTimer.start();
    }

    private void initFrame() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeWithConfirm();
            }
        });
        setJMenuBar(createMenuBar());
        setMinimumSize(new Dimension(1280, 760));
        setSize(new Dimension(1500, 900));
        setLocationRelativeTo(null);
    }

    private void initLayout() {
        JPanel leftPanel = buildProjectTreePane();
        JPanel centerPanel = buildEditorPane();
        leftCenterSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, centerPanel);
        leftCenterSplit.setResizeWeight(0.22);
        leftCenterSplit.setDividerLocation(expandedLeftWidth);
        leftCenterSplit.setContinuousLayout(true);
        leftCenterSplit.setDividerSize(ACTIVE_DIVIDER_SIZE);

        JPanel rightHost = buildRightToolPane();
        rootSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftCenterSplit, rightHost);
        rootSplit.setResizeWeight(1.0);
        rootSplit.setContinuousLayout(true);
        rootSplit.setOneTouchExpandable(false);
        rootSplit.setDividerSize(ACTIVE_DIVIDER_SIZE);
        installRootDividerTracker();

        getContentPane().setLayout(new BorderLayout());
        JPanel shellRoot = new JPanel(new BorderLayout());
        shellRoot.setBackground(SHELL_BG);
        shellRoot.add(buildTopToolbar(), BorderLayout.NORTH);
        shellRoot.add(rootSplit, BorderLayout.CENTER);
        getContentPane().add(shellRoot, BorderLayout.CENTER);
        updateSplitDraggableState();
        requestRootDividerLocationUpdate();
    }

    private void installRootDividerTracker() {
        if (!(rootSplit.getUI() instanceof BasicSplitPaneUI splitPaneUi)) {
            return;
        }
        BasicSplitPaneDivider divider = splitPaneUi.getDivider();
        if (divider == null) {
            return;
        }
        divider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (rightCollapsed || rootDividerAdjusting) {
                    return;
                }
                rememberRightExpandedWidth();
            }
        });
    }

    private void initActions() {
        if (startCmd != null && startCmd.getFontSize() > 0) {
            Font current = editorArea.getFont();
            editorArea.setFont(current.deriveFont((float) startCmd.getFontSize()));
        }

        refreshTimer = new Timer(REFRESH_INTERVAL_MS, e -> refreshAsync());
        refreshTimer.setRepeats(true);

        treeSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                forceTreeRefresh.set(true);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                forceTreeRefresh.set(true);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                forceTreeRefresh.set(true);
            }
        });

        projectTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    openTreeSelection();
                }
            }
        });
        projectTree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open-tree-node");
        projectTree.getActionMap().put("open-tree-node", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openTreeSelection();
            }
        });
    }

    private JToolBar buildTopToolbar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, SHELL_LINE),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        bar.setBackground(new Color(0xF3F3F3));
        bar.setRollover(true);

        // Match jadx toolbar grouping order as closely as possible.
        addTopToolbarButton(bar, "icons/jadx/openDisk.svg", "打开文件", e -> openFileFromToolbar(false));
        addTopToolbarButton(bar, "icons/jadx/addFile.svg", "添加输入", e -> openFileFromToolbar(false));
        addTopToolbarButton(bar, "icons/jadx/projectDirectory.svg", "打开项目目录", e -> openFileFromToolbar(true));
        addTopToolbarSeparator(bar);
        addTopToolbarButton(bar, "icons/jadx/refresh.svg", "重新加载", e -> refreshTreeNow());
        addTopToolbarSeparator(bar);
        addTopToolbarButton(bar, "icons/jadx/export.svg", "导出", e -> RuntimeFacades.tooling().openExportTool());
        addTopToolbarSeparator(bar);
        addTopToolbarButton(bar, "icons/jadx/locate.svg", "同步项目树", e -> refreshAsync());
        addTopToolbarToggleButton(bar, "icons/jadx/abbreviatePackageNames.svg", "扁平包名（待迁移）", false, true, null);
        addTopToolbarToggleButton(bar, "icons/jadx/editorPreview.svg", "预览标签（待迁移）", false, true, null);
        addTopToolbarToggleButton(bar, "icons/jadx/pagination.svg", "快速标签（待迁移）", false, true, null);
        addTopToolbarSeparator(bar);
        addTopToolbarButton(bar, "icons/jadx/find.svg", "全局搜索", e -> RuntimeFacades.tooling().openGlobalSearchTool());
        addTopToolbarButton(bar, "icons/jadx/ejbFinderMethod.svg", "类搜索", e -> promptTreeSearchKeyword());
        addTopToolbarButton(bar, "icons/jadx/usagesFinder.svg", "注释搜索", e -> RuntimeFacades.tooling().openGlobalSearchTool());
        addTopToolbarButton(bar, "icons/jadx/home.svg", "打开 start 面板", e -> focusToolTab(ToolTab.START));
        addTopToolbarButton(bar, "icons/jadx/application.svg", "打开 web 面板", e -> focusToolTab(ToolTab.WEB));
        addTopToolbarButton(bar, "icons/jadx/androidManifest.svg", "打开 api 面板", e -> focusToolTab(ToolTab.API));
        addTopToolbarSeparator(bar);
        addTopToolbarButton(bar, "icons/jadx/left.svg", "后退", e -> RuntimeFacades.editor().goPrev());
        addTopToolbarButton(bar, "icons/jadx/right.svg", "前进", e -> RuntimeFacades.editor().goNext());
        addTopToolbarSeparator(bar);
        addTopToolbarToggleButton(bar, "icons/jadx/helmChartLock.svg", "反混淆（待迁移）", false, true, null);
        addTopToolbarButton(bar, "icons/jadx/quark.svg", "混淆分析", e -> RuntimeFacades.tooling().openObfuscationTool());
        addTopToolbarButton(bar, "icons/jadx/startDebugger.svg", "字节码调试", e -> RuntimeFacades.tooling().openBytecodeDebugger());
        addTopToolbarSeparator(bar);
        addTopToolbarButton(bar, "icons/jadx/logVerbose.svg", "系统监控", e -> RuntimeFacades.tooling().openSystemMonitorTool());
        addTopToolbarSeparator(bar);
        addTopToolbarButton(bar, "icons/jadx/settings.svg", "打开 advance 面板", e -> focusToolTab(ToolTab.ADVANCE));
        addTopToolbarSeparator(bar);
        bar.add(Box.createHorizontalGlue());
        return bar;
    }

    private void addTopToolbarButton(JToolBar bar, String iconPath, String tooltip, java.awt.event.ActionListener listener) {
        bar.add(toolbarIconButton(iconPath, tooltip, listener, 15, true));
    }

    private void addTopToolbarToggleButton(
            JToolBar bar,
            String iconPath,
            String tooltip,
            boolean selected,
            boolean enabled,
            java.awt.event.ActionListener listener) {
        JToggleButton button = new JToggleButton();
        button.setFocusable(false);
        button.setToolTipText(tooltip);
        button.setSelected(selected);
        button.setEnabled(enabled);
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setPreferredSize(new Dimension(TOP_TOOLBAR_BUTTON_SIZE, TOP_TOOLBAR_BUTTON_SIZE));
        button.setMinimumSize(new Dimension(TOP_TOOLBAR_BUTTON_SIZE, TOP_TOOLBAR_BUTTON_SIZE));
        button.setMaximumSize(new Dimension(TOP_TOOLBAR_BUTTON_SIZE, TOP_TOOLBAR_BUTTON_SIZE));
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        Icon icon = loadIcon(iconPath, 15);
        if (icon != null) {
            button.setIcon(icon);
        }
        if (listener != null) {
            button.addActionListener(listener);
        }
        bar.add(button);
    }

    private void addTopToolbarSeparator(JToolBar bar) {
        bar.addSeparator(new Dimension(8, TOP_TOOLBAR_BUTTON_SIZE));
    }

    private void focusToolTab(ToolTab tab) {
        topTab = tab;
        setRightCollapsed(false);
    }

    private void promptTreeSearchKeyword() {
        String kw = JOptionPane.showInputDialog(this, "输入类名关键字", safe(treeSearchField.getText()));
        if (kw == null) {
            return;
        }
        treeSearchField.setText(kw);
        forceTreeRefresh.set(true);
        refreshAsync();
    }

    private void closeStartPageTab() {
        if (startPageView == null) {
            return;
        }
        int index = workbenchTabs.indexOfComponent(startPageView);
        if (index >= 0) {
            workbenchTabs.removeTabAt(index);
        }
    }

    private void ensureStartPageTab() {
        if (startPageView == null) {
            return;
        }
        if (workbenchTabs.indexOfComponent(startPageView) < 0) {
            workbenchTabs.insertTab("开始页", null, startPageView, null, 0);
        }
    }

    private void selectCodeTab() {
        if (codePageView == null) {
            return;
        }
        int codeIndex = workbenchTabs.indexOfComponent(codePageView);
        if (codeIndex >= 0) {
            workbenchTabs.setSelectedIndex(codeIndex);
        }
    }

    private void selectStartTabIfVisible() {
        if (startPageView == null) {
            return;
        }
        int startIndex = workbenchTabs.indexOfComponent(startPageView);
        if (startIndex >= 0) {
            workbenchTabs.setSelectedIndex(startIndex);
        }
    }

    private void syncStartPageVisibility(BuildSnapshotDto buildSnapshot) {
        if (System.currentTimeMillis() < suppressStartPageUntil) {
            closeStartPageTab();
            selectCodeTab();
            return;
        }
        String inputPath = "";
        if (buildSnapshot != null && buildSnapshot.settings() != null) {
            inputPath = safe(buildSnapshot.settings().inputPath());
        }
        if (inputPath.isBlank()) {
            ensureStartPageTab();
            if (safe(editorArea.getText()).isBlank()) {
                selectStartTabIfVisible();
            }
        } else {
            closeStartPageTab();
            selectCodeTab();
            suppressStartPageUntil = 0L;
        }
    }

    private JButton toolbarIconButton(String iconPath, String tooltip, java.awt.event.ActionListener listener) {
        return toolbarIconButton(iconPath, tooltip, listener, 15, false);
    }

    private JButton toolbarIconButton(
            String iconPath,
            String tooltip,
            java.awt.event.ActionListener listener,
            int iconSize,
            boolean topToolbarStyle) {
        JButton button = new JButton();
        button.setFocusable(false);
        button.setToolTipText(tooltip);
        Icon icon = loadIcon(iconPath, iconSize);
        if (icon != null) {
            button.setIcon(icon);
        } else {
            button.setText("?");
        }
        button.addActionListener(listener);
        if (topToolbarStyle) {
            button.putClientProperty("JButton.buttonType", "toolBarButton");
            button.setBorder(BorderFactory.createEmptyBorder());
            button.setMargin(new Insets(0, 0, 0, 0));
            button.setPreferredSize(new Dimension(TOP_TOOLBAR_BUTTON_SIZE, TOP_TOOLBAR_BUTTON_SIZE));
            button.setMinimumSize(new Dimension(TOP_TOOLBAR_BUTTON_SIZE, TOP_TOOLBAR_BUTTON_SIZE));
            button.setMaximumSize(new Dimension(TOP_TOOLBAR_BUTTON_SIZE, TOP_TOOLBAR_BUTTON_SIZE));
            button.setContentAreaFilled(false);
            button.setOpaque(false);
        } else {
            button.setMargin(new Insets(2, 6, 2, 6));
        }
        return button;
    }

    private void openFileFromToolbar(boolean directoryOnly) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(directoryOnly ? "选择项目目录" : "选择 Jar/目录");
        chooser.setFileSelectionMode(directoryOnly ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_AND_DIRECTORIES);
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null) {
            return;
        }
        BuildSnapshotDto snapshot = RuntimeFacades.build().snapshot();
        if (snapshot == null || snapshot.settings() == null) {
            return;
        }
        String input = chooser.getSelectedFile().getAbsolutePath();
        RuntimeFacades.build().apply(new me.n1ar4.jar.analyzer.gui.runtime.model.BuildSettingsDto(
                input,
                snapshot.settings().runtimePath(),
                snapshot.settings().resolveNestedJars(),
                snapshot.settings().autoFindRuntimeJar(),
                snapshot.settings().addRuntimeJar(),
                snapshot.settings().deleteTempBeforeBuild(),
                snapshot.settings().fixClassPath(),
                snapshot.settings().fixMethodImpl(),
                snapshot.settings().quickMode()
        ));
        suppressStartPageUntil = System.currentTimeMillis() + 3000;
        focusToolTab(ToolTab.START);
        closeStartPageTab();
        selectCodeTab();
    }

    private JPanel buildProjectTreePane() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, SHELL_LINE));
        panel.setBackground(new Color(0xF4F4F4));
        panel.setPreferredSize(new Dimension(LEFT_PANE_DEFAULT_WIDTH, 0));

        leftToolSplit.setBorder(BorderFactory.createEmptyBorder());
        leftToolSplit.setResizeWeight(0.62);
        leftToolSplit.setTopComponent(buildProjectTreeToolWindow());
        leftToolSplit.setBottomComponent(buildStructureToolWindow());
        leftToolSplit.setContinuousLayout(true);

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder());
        content.setBackground(new Color(0xF4F4F4));
        content.add(leftToolSplit, BorderLayout.CENTER);

        panel.add(buildLeftStripe(), BorderLayout.WEST);
        panel.add(content, BorderLayout.CENTER);
        applyLeftToolWindowState();
        return panel;
    }

    private JPanel buildProjectTreeToolWindow() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBorder(BorderFactory.createEmptyBorder());
        panel.setBackground(new Color(0xF4F4F4));

        JToolBar leftBar = new JToolBar();
        leftBar.setFloatable(false);
        leftBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, SHELL_LINE));
        leftBar.setBackground(new Color(0xF7F7F7));
        leftBar.add(toolbarIconButton("icons/jadx/refresh.svg", "刷新", e -> refreshTreeNow()));
        leftBar.add(toolbarIconButton("icons/jadx/find.svg", "搜索类名", e -> promptTreeSearchKeyword()));
        panel.add(leftBar, BorderLayout.NORTH);

        projectTree.setRootVisible(false);
        projectTree.setShowsRootHandles(true);
        projectTree.setBackground(Color.WHITE);
        JScrollPane treeScroll = new JScrollPane(projectTree);
        treeScroll.setBorder(BorderFactory.createEmptyBorder());

        leftEmptyLabel.setBorder(BorderFactory.createEmptyBorder(10, 8, 8, 8));
        leftEmptyLabel.setVerticalAlignment(JLabel.TOP);
        leftEmptyLabel.setHorizontalAlignment(JLabel.LEFT);
        JPanel emptyPanel = new JPanel(new BorderLayout());
        emptyPanel.setBackground(Color.WHITE);
        emptyPanel.add(leftEmptyLabel, BorderLayout.NORTH);

        treeCardPanel.setBorder(BorderFactory.createEmptyBorder());
        treeCardPanel.add(emptyPanel, "empty");
        treeCardPanel.add(treeScroll, "tree");
        treeCardLayout.show(treeCardPanel, "empty");

        JPanel status = new JPanel(new BorderLayout());
        status.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, SHELL_LINE));
        status.setBackground(new Color(0xF7F7F7));
        status.add(treeStatusValue, BorderLayout.WEST);
        treeStatusValue.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 2));

        panel.add(treeCardPanel, BorderLayout.CENTER);
        panel.add(status, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildStructureToolWindow() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, SHELL_LINE));
        panel.setBackground(new Color(0xF4F4F4));

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, SHELL_LINE));
        header.setBackground(new Color(0xF7F7F7));
        JLabel title = new JLabel("结构");
        title.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 2));
        structureStatusValue.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 6));
        header.add(title, BorderLayout.WEST);
        header.add(structureStatusValue, BorderLayout.EAST);

        structureList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        structureList.setBackground(Color.WHITE);
        structureList.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        structureList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    StructureItem item = structureList.getSelectedValue();
                    if (item == null) {
                        return;
                    }
                    int caret = Math.max(0, Math.min(editorArea.getDocument().getLength(), item.caretOffset()));
                    editorArea.setCaretPosition(caret);
                    workbenchTabs.setSelectedIndex(1);
                }
            }
        });
        JScrollPane scroll = new JScrollPane(structureList);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildLeftStripe() {
        JPanel stripe = new JPanel();
        stripe.setLayout(new BoxLayout(stripe, BoxLayout.Y_AXIS));
        stripe.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, SHELL_LINE));
        stripe.setBackground(new Color(0xF3F3F3));
        stripe.setPreferredSize(new Dimension(LEFT_STRIPE_WIDTH, 0));

        leftTreeStripeButton.setToolTipText("目录树");
        leftTreeStripeButton.addActionListener(e -> {
            treePanelCollapsed = !leftTreeStripeButton.isSelected();
            applyLeftToolWindowState();
        });
        leftStructureStripeButton.setToolTipText("结构");
        leftStructureStripeButton.addActionListener(e -> {
            structurePanelCollapsed = !leftStructureStripeButton.isSelected();
            applyLeftToolWindowState();
        });

        stripe.add(Box.createVerticalStrut(6));
        stripe.add(leftTreeStripeButton);
        stripe.add(Box.createVerticalStrut(2));
        stripe.add(leftStructureStripeButton);
        stripe.add(Box.createVerticalGlue());
        return stripe;
    }

    private void applyLeftToolWindowState() {
        leftTreeStripeButton.setSelected(!treePanelCollapsed);
        leftStructureStripeButton.setSelected(!structurePanelCollapsed);

        Component top = leftToolSplit.getTopComponent();
        Component bottom = leftToolSplit.getBottomComponent();
        if (top != null) {
            top.setVisible(!treePanelCollapsed);
        }
        if (bottom != null) {
            bottom.setVisible(!structurePanelCollapsed);
        }

        boolean bothVisible = !treePanelCollapsed && !structurePanelCollapsed;
        if (bothVisible) {
            leftToolSplit.setDividerSize(ACTIVE_DIVIDER_SIZE);
            leftToolSplit.setEnabled(true);
            int height = leftToolSplit.getHeight();
            int location = leftToolSplit.getDividerLocation();
            if (location <= 0 || (height > 0 && location >= height - 32)) {
                if (height > 0) {
                    leftToolSplit.setDividerLocation((int) (height * 0.62));
                } else {
                    leftToolSplit.setDividerLocation(0.62);
                }
            }
        } else {
            leftToolSplit.setDividerSize(LOCKED_DIVIDER_SIZE);
            leftToolSplit.setEnabled(false);
            if (!treePanelCollapsed) {
                leftToolSplit.setDividerLocation(1.0);
            } else if (!structurePanelCollapsed) {
                leftToolSplit.setDividerLocation(0.0);
            }
        }

        if (!leftCollapsed && leftCenterSplit != null) {
            if (isLeftToolContentCollapsed()) {
                int current = leftCenterSplit.getDividerLocation();
                if (current > LEFT_STRIPE_WIDTH + 10) {
                    expandedLeftWidth = current;
                }
                leftCenterSplit.setDividerLocation(LEFT_STRIPE_WIDTH + 2);
            } else {
                int current = leftCenterSplit.getDividerLocation();
                if (current <= LEFT_STRIPE_WIDTH + 6) {
                    leftCenterSplit.setDividerLocation(Math.max(LEFT_PANE_MIN_WIDTH, expandedLeftWidth));
                }
            }
        }

        updateSplitDraggableState();
        leftToolSplit.revalidate();
        leftToolSplit.repaint();
    }

    private boolean isLeftToolContentCollapsed() {
        return treePanelCollapsed && structurePanelCollapsed;
    }

    private JPanel buildEditorPane() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder());
        panel.setBackground(SHELL_BG);

        startPageView = buildStartPagePanel();
        codePageView = buildCodePagePanel();
        workbenchTabs.setBorder(BorderFactory.createEmptyBorder());
        workbenchTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        workbenchTabs.addTab("开始页", startPageView);
        workbenchTabs.addTab("代码", codePageView);
        workbenchTabs.setSelectedIndex(0);

        panel.add(workbenchTabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStartPagePanel() {
        JPanel page = new JPanel(new GridBagLayout());
        page.setBackground(SHELL_BG);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JPanel startBox = new JPanel(new BorderLayout(8, 8));
        startBox.setBackground(new Color(0xEFEFEF));
        startBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SHELL_LINE),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        startBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel startLabel = new JLabel("开始");
        startLabel.setFont(startLabel.getFont().deriveFont(Font.BOLD));
        startBox.add(startLabel, BorderLayout.NORTH);
        JPanel startButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        startButtons.setOpaque(false);
        JButton openFile = new JButton("打开文件");
        openFile.addActionListener(e -> openFileFromToolbar(false));
        JButton openProject = new JButton("打开项目");
        openProject.addActionListener(e -> openFileFromToolbar(true));
        startButtons.add(openFile);
        startButtons.add(openProject);
        startBox.add(startButtons, BorderLayout.CENTER);

        JPanel recentBox = new JPanel(new BorderLayout(8, 8));
        recentBox.setBackground(new Color(0xEFEFEF));
        recentBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SHELL_LINE),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        recentBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel recentLabel = new JLabel("最近项目");
        recentLabel.setFont(recentLabel.getFont().deriveFont(Font.BOLD));
        recentBox.add(recentLabel, BorderLayout.NORTH);
        recentProjectArea.setEditable(false);
        recentProjectArea.setBackground(Color.WHITE);
        recentProjectArea.setBorder(BorderFactory.createLineBorder(new Color(0xDDDDDD)));
        recentProjectArea.setRows(8);
        recentProjectArea.setText("");
        recentBox.add(new JScrollPane(recentProjectArea), BorderLayout.CENTER);

        center.add(startBox);
        center.add(Box.createVerticalStrut(18));
        center.add(recentBox);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        page.add(center, gbc);
        installStartPageAdaptiveSizing(page, center, startBox, recentBox);
        return page;
    }

    private void installStartPageAdaptiveSizing(JPanel page, JPanel center, JPanel startBox, JPanel recentBox) {
        Runnable apply = () -> {
            int width = page.getWidth();
            int height = page.getHeight();
            if (width <= 0 && workbenchTabs != null) {
                width = workbenchTabs.getWidth();
            }
            if (height <= 0 && workbenchTabs != null) {
                height = workbenchTabs.getHeight();
            }
            int contentWidth = clamp(width - 48, 420, 980);
            int contentHeight = clamp(height - 96, 300, 760);
            int gap = 18;
            int startHeight = clamp((int) (contentHeight * 0.42), 130, 300);
            int recentHeight = Math.max(140, contentHeight - startHeight - gap);

            center.setPreferredSize(new Dimension(contentWidth, startHeight + gap + recentHeight));
            startBox.setPreferredSize(new Dimension(contentWidth, startHeight));
            recentBox.setPreferredSize(new Dimension(contentWidth, recentHeight));
            center.revalidate();
        };
        page.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                apply.run();
            }
        });
        SwingUtilities.invokeLater(apply);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private JPanel buildCodePagePanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel context = new JPanel();
        context.setLayout(new BoxLayout(context, BoxLayout.X_AXIS));
        context.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SHELL_LINE),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
        context.add(new JLabel("jar"));
        context.add(Box.createHorizontalStrut(6));
        context.add(currentJarField);
        context.add(Box.createHorizontalStrut(8));
        context.add(new JLabel("class"));
        context.add(Box.createHorizontalStrut(6));
        context.add(currentClassField);
        context.add(Box.createHorizontalStrut(8));
        context.add(new JLabel("method"));
        context.add(Box.createHorizontalStrut(6));
        context.add(currentMethodField);

        JPanel searchPanel = new JPanel(new BorderLayout(6, 0));
        searchPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SHELL_LINE),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
        JButton searchPrev = new JButton("Prev");
        searchPrev.addActionListener(e -> RuntimeFacades.editor().searchInCurrent(editorSearchField.getText(), false));
        JButton searchNext = new JButton("Next");
        searchNext.addActionListener(e -> RuntimeFacades.editor().searchInCurrent(editorSearchField.getText(), true));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actions.add(searchPrev);
        actions.add(searchNext);
        searchPanel.add(editorSearchField, BorderLayout.CENTER);
        searchPanel.add(actions, BorderLayout.EAST);
        editorSearchField.addActionListener(e -> RuntimeFacades.editor().searchInCurrent(editorSearchField.getText(), true));

        editorArea.setEditable(false);
        editorArea.setCodeFoldingEnabled(true);
        editorArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        editorArea.setAntiAliasingEnabled(true);
        editorArea.setHighlightCurrentLine(true);
        RTextScrollPane scrollPane = new RTextScrollPane(editorArea);
        scrollPane.setLineNumbersEnabled(true);
        scrollPane.setBorder(BorderFactory.createLineBorder(SHELL_LINE));

        JPanel status = new JPanel(new BorderLayout());
        status.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, SHELL_LINE));
        status.add(editorStatusValue, BorderLayout.WEST);

        JPanel north = new JPanel(new BorderLayout(6, 6));
        north.add(context, BorderLayout.NORTH);
        north.add(searchPanel, BorderLayout.SOUTH);
        panel.add(north, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(status, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildRightToolPane() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        root.setMinimumSize(new Dimension(collapsedRightWidth(), 0));

        JPanel stripeWrap = new JPanel(new BorderLayout(0, 2));
        stripeWrap.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, SHELL_LINE));

        rightStripe.setFloatable(false);
        rightStripe.setRollover(true);
        rightStripe.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        rightStripe.setBackground(new Color(0xF3F3F3));
        stripeWrap.add(rightStripe, BorderLayout.CENTER);

        initToolPanels();
        initToolTabs();
        initSingleToolHost();
        rightContentHost.setMinimumSize(new Dimension(0, 0));
        topCards.setMinimumSize(new Dimension(0, 0));
        root.add(rightContentHost, BorderLayout.CENTER);
        root.add(stripeWrap, BorderLayout.EAST);
        return root;
    }

    private void initToolPanels() {
        topPanels.put(ToolTab.START, startPanel);
        topPanels.put(ToolTab.SEARCH, searchPanel);
        topPanels.put(ToolTab.CALL, callPanel);
        topPanels.put(ToolTab.IMPL, implPanel);
        topPanels.put(ToolTab.WEB, webPanel);
        topPanels.put(ToolTab.NOTE, notePanel);
        topPanels.put(ToolTab.SCA, scaPanel);
        topPanels.put(ToolTab.LEAK, leakPanel);
        topPanels.put(ToolTab.GADGET, gadgetPanel);
        topPanels.put(ToolTab.ADVANCE, advancePanel);
        topPanels.put(ToolTab.CHAINS, chainsPanel);
        topPanels.put(ToolTab.API, apiPanel);
        for (JPanel panel : topPanels.values()) {
            panel.setMinimumSize(new Dimension(0, 0));
        }
    }

    private void initToolTabs() {
        topCards.add(emptyCard("Top slot is empty"), EMPTY_CARD);
        for (ToolTab tab : ToolTab.values()) {
            topCards.add(topPanels.get(tab), tab.card());
            JToggleButton button = new JToggleButton(tab.buttonText());
            button.setFocusable(false);
            button.setToolTipText(tab.title());
            Icon icon = loadTabIcon(tab);
            if (icon != null) {
                button.setIcon(icon);
            }
            button.addActionListener(e -> onStripeButtonClicked(tab, e));
            stripeButtons.put(tab, button);
            rightStripe.add(button);
        }
        updateStripeStyle(true, stripeWidth);
        updateStripeSelection();
    }

    private void initSingleToolHost() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, SHELL_LINE));
        header.setBackground(new Color(0xF5F5F5));
        topTitle.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
        header.add(topTitle, BorderLayout.WEST);

        rightContentHost.removeAll();
        rightContentHost.add(header, BorderLayout.NORTH);
        rightContentHost.add(topCards, BorderLayout.CENTER);
        applyRightPaneState();
    }

    private JPanel emptyCard(String text) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(text, JLabel.CENTER), BorderLayout.CENTER);
        return panel;
    }

    private void onStripeButtonClicked(ToolTab tab, ActionEvent e) {
        if (Objects.equals(topTab, tab)) {
            topTab = null;
        } else {
            topTab = tab;
        }
        setRightCollapsed(topTab == null);
        applyRightPaneState();
    }

    private void setLeftCollapsed(boolean collapsed) {
        if (leftCenterSplit == null || leftCenterSplit.getLeftComponent() == null) {
            leftCollapsed = collapsed;
            updateSplitDraggableState();
            return;
        }
        if (collapsed == leftCollapsed) {
            updateSplitDraggableState();
            return;
        }
        if (collapsed) {
            int currentWidth = leftCenterSplit.getDividerLocation();
            if (currentWidth > LEFT_PANE_MIN_WIDTH / 2) {
                expandedLeftWidth = currentWidth;
            }
            leftCollapsed = true;
            leftCenterSplit.getLeftComponent().setVisible(false);
            leftCenterSplit.setDividerLocation(0);
        } else {
            leftCollapsed = false;
            int targetWidth = Math.max(LEFT_PANE_MIN_WIDTH, expandedLeftWidth);
            leftCenterSplit.getLeftComponent().setVisible(true);
            leftCenterSplit.getLeftComponent().setPreferredSize(new Dimension(targetWidth, 0));
            leftCenterSplit.setDividerLocation(targetWidth);
            applyLeftToolWindowState();
        }
        updateSplitDraggableState();
        leftCenterSplit.revalidate();
        leftCenterSplit.repaint();
    }

    private void setRightCollapsed(boolean collapsed) {
        if (collapsed && !rightCollapsed) {
            rememberRightExpandedWidth();
        }
        if (!collapsed && topTab == null) {
            topTab = ToolTab.START;
        }
        rightCollapsed = collapsed;
        applyRightPaneState();
    }

    private void applyRightPaneState() {
        java.awt.CardLayout topLayout = (java.awt.CardLayout) topCards.getLayout();

        if (topTab == null) {
            topLayout.show(topCards, EMPTY_CARD);
            topTitle.setText("工具窗");
        } else {
            topLayout.show(topCards, topTab.card());
            topTitle.setText(topTab.title());
        }

        if (rightCollapsed) {
            rightContentHost.setVisible(false);
        } else {
            rightContentHost.setVisible(true);
        }

        updateStripeSelection();
        updateSplitDraggableState();
        requestRootDividerLocationUpdate();
    }

    private void requestRootDividerLocationUpdate() {
        if (rootSplit == null) {
            return;
        }
        if (rootSplit.getWidth() > 0) {
            updateRootDividerLocation();
            return;
        }
        SwingUtilities.invokeLater(this::updateRootDividerLocation);
    }

    private void updateRootDividerLocation() {
        if (rootSplit == null) {
            return;
        }
        int totalWidth = rootSplit.getWidth();
        if (totalWidth <= 0) {
            totalWidth = getContentPane().getWidth();
        }
        if (totalWidth <= 0) {
            totalWidth = getWidth();
        }
        int divider = Math.max(0, rootSplit.getDividerSize());
        int targetRightWidth = preferredRightWidth(totalWidth);
        int minLocation = Math.max(0, Math.min(LEFT_PANE_MIN_WIDTH, totalWidth - divider));
        int maxLocation = Math.max(minLocation, totalWidth - divider - collapsedRightWidth());
        int location = totalWidth - divider - targetRightWidth;
        location = Math.max(minLocation, Math.min(maxLocation, location));
        rootDividerAdjusting = true;
        try {
            rootSplit.setDividerLocation(location);
        } finally {
            rootDividerAdjusting = false;
        }
    }

    private void rememberRightExpandedWidth() {
        if (rootSplit == null) {
            return;
        }
        int totalWidth = rootSplit.getWidth();
        if (totalWidth <= 0) {
            return;
        }
        int dividerLocation = rootSplit.getDividerLocation();
        if (dividerLocation <= 0 || dividerLocation >= totalWidth) {
            return;
        }
        int divider = Math.max(0, rootSplit.getDividerSize());
        int rightTotalWidth = totalWidth - divider - dividerLocation;
        if (rightTotalWidth <= collapsedRightWidth()) {
            return;
        }
        int contentWidth = rightTotalWidth - collapsedRightWidth();
        int maxContent = maxRightContentWidth(totalWidth);
        expandedWidth = clamp(contentWidth, RIGHT_PANE_CONTENT_MIN_WIDTH, maxContent);
    }

    private void updateSplitDraggableState() {
        boolean leftDraggable = !leftCollapsed && !isLeftToolContentCollapsed();
        applySplitDraggable(leftCenterSplit, leftDraggable);
        applySplitDraggable(rootSplit, !rightCollapsed);
    }

    private void applySplitDraggable(JSplitPane splitPane, boolean draggable) {
        if (splitPane == null) {
            return;
        }
        splitPane.setDividerSize(draggable ? ACTIVE_DIVIDER_SIZE : LOCKED_DIVIDER_SIZE);
        splitPane.setContinuousLayout(draggable);
        if (splitPane.getUI() instanceof BasicSplitPaneUI splitPaneUi) {
            BasicSplitPaneDivider divider = splitPaneUi.getDivider();
            if (divider != null) {
                divider.setEnabled(draggable);
                int cursorType = splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT
                        ? Cursor.E_RESIZE_CURSOR
                        : Cursor.N_RESIZE_CURSOR;
                divider.setCursor(draggable
                        ? Cursor.getPredefinedCursor(cursorType)
                        : Cursor.getDefaultCursor());
            }
        }
    }

    private int preferredRightWidth(int totalWidth) {
        if (rightCollapsed) {
            return collapsedRightWidth();
        }
        int maxContent = maxRightContentWidth(totalWidth);
        int content = clamp(expandedWidth, RIGHT_PANE_CONTENT_MIN_WIDTH, maxContent);
        expandedWidth = content;
        return content + collapsedRightWidth();
    }

    private int collapsedRightWidth() {
        return stripeWidth + 8;
    }

    private int maxRightContentWidth(int totalWidth) {
        int divider = Math.max(0, rootSplit == null ? ACTIVE_DIVIDER_SIZE : rootSplit.getDividerSize());
        int maxByRatio = (int) (totalWidth * RIGHT_PANE_MAX_WIDTH_RATIO) - collapsedRightWidth();
        int maxByLeft = totalWidth - divider - LEFT_PANE_MIN_WIDTH - collapsedRightWidth();
        return Math.max(RIGHT_PANE_CONTENT_MIN_WIDTH, Math.min(maxByRatio, maxByLeft));
    }

    private void updateStripeSelection() {
        for (Map.Entry<ToolTab, JToggleButton> entry : stripeButtons.entrySet()) {
            ToolTab tab = entry.getKey();
            entry.getValue().setSelected(Objects.equals(topTab, tab));
        }
    }

    private void updateStripeStyle(boolean showNames, int width) {
        stripeWidth = Math.max(40, Math.min(100, width));
        rightStripe.setPreferredSize(new Dimension(stripeWidth, 0));
        for (Map.Entry<ToolTab, JToggleButton> entry : stripeButtons.entrySet()) {
            ToolTab tab = entry.getKey();
            JToggleButton button = entry.getValue();
            button.setText(showNames ? tab.buttonText() : "");
            button.setHorizontalAlignment(showNames ? JToggleButton.LEADING : JToggleButton.CENTER);
            button.setHorizontalTextPosition(JToggleButton.RIGHT);
            button.setIconTextGap(showNames ? 4 : 0);
            button.setToolTipText(tab.title());
            button.setMaximumSize(new Dimension(stripeWidth - 2, 32));
            button.setPreferredSize(new Dimension(stripeWidth - 2, 32));
            button.setMargin(new Insets(2, 4, 2, 4));
        }
        rightStripe.revalidate();
        rightStripe.repaint();
    }

    private Icon loadTabIcon(ToolTab tab) {
        String path = switch (tab) {
            case START -> "/svg/start.svg";
            case SEARCH -> "/svg/search.svg";
            case CALL -> "/svg/connect.svg";
            case IMPL -> "/svg/inherit.svg";
            case WEB -> "/svg/spring.svg";
            case NOTE -> "/svg/note.svg";
            case SCA -> "/svg/sca.svg";
            case LEAK -> "/svg/leak.svg";
            case GADGET -> "/svg/gadget.svg";
            case ADVANCE -> "/svg/advance.svg";
            case CHAINS -> "/svg/tomcat.svg";
            case API -> "/svg/dir.svg";
        };
        return loadIcon(path, 16);
    }

    private Icon loadIcon(String path, int size) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        if (normalized.endsWith(".svg")) {
            try {
                return new FlatSVGIcon(normalized, size, size);
            } catch (Throwable ignored) {
            }
        }
        URL url = getClass().getResource(path.startsWith("/") ? path : "/" + path);
        if (url != null) {
            return new ImageIcon(url);
        }
        return null;
    }

    private void refreshTreeNow() {
        RuntimeFacades.projectTree().refresh();
        forceTreeRefresh.set(true);
        refreshAsync();
    }

    private void openTreeSelection() {
        TreePath path = projectTree.getSelectionPath();
        if (path == null) {
            return;
        }
        Object end = path.getLastPathComponent();
        if (!(end instanceof DefaultMutableTreeNode node)) {
            return;
        }
        Object user = node.getUserObject();
        if (!(user instanceof TreeNodeUi item) || item.directory()) {
            return;
        }
        RuntimeFacades.projectTree().openNode(item.value());
    }

    private void refreshAsync() {
        if (!refreshBusy.compareAndSet(false, true)) {
            return;
        }
        final String treeKeyword = safe(treeSearchField.getText());
        final long now = System.currentTimeMillis();
        final boolean treeChanged = !Objects.equals(treeKeyword, lastTreeKeyword);
        final boolean loadTree = forceTreeRefresh.getAndSet(false)
                || treeChanged
                || now - lastTreeRefreshAt >= TREE_REFRESH_INTERVAL_MS;

        Thread.ofVirtual().name("swing-runtime-sync").start(() -> {
            UiSnapshot snapshot = collectSnapshot(loadTree, treeKeyword);
            SwingUtilities.invokeLater(() -> {
                try {
                    applySnapshot(snapshot, loadTree, treeKeyword);
                } finally {
                    refreshBusy.set(false);
                }
            });
        });
    }

    private UiSnapshot collectSnapshot(boolean includeTree, String treeKeyword) {
        BuildSnapshotDto build = snapshotSafe(RuntimeFacades.build()::snapshot, null);
        SearchSnapshotDto search = snapshotSafe(RuntimeFacades.search()::snapshot, null);
        CallGraphSnapshotDto call = snapshotSafe(RuntimeFacades.callGraph()::snapshot, null);
        WebSnapshotDto web = snapshotSafe(RuntimeFacades.web()::snapshot, null);
        NoteSnapshotDto note = snapshotSafe(RuntimeFacades.note()::snapshot, null);
        ScaSnapshotDto sca = snapshotSafe(RuntimeFacades.sca()::snapshot, null);
        LeakSnapshotDto leak = snapshotSafe(RuntimeFacades.leak()::snapshot, null);
        GadgetSnapshotDto gadget = snapshotSafe(RuntimeFacades.gadget()::snapshot, null);
        ChainsSnapshotDto chains = snapshotSafe(RuntimeFacades.chains()::snapshot, null);
        ToolingConfigSnapshotDto tooling = snapshotSafe(RuntimeFacades.tooling()::configSnapshot, null);
        EditorDocumentDto editor = snapshotSafe(RuntimeFacades.editor()::current, null);
        ApiInfoDto api = snapshotSafe(RuntimeFacades.apiMcp()::apiInfo, null);
        McpConfigDto mcp = snapshotSafe(RuntimeFacades.apiMcp()::currentConfig, null);

        List<TreeNodeDto> tree = null;
        if (includeTree) {
            tree = snapshotSafe(() -> {
                if (treeKeyword == null || treeKeyword.isBlank()) {
                    return RuntimeFacades.projectTree().snapshot();
                }
                return RuntimeFacades.projectTree().search(treeKeyword);
            }, List.of());
        }
        return new UiSnapshot(build, search, call, web, note, sca, leak, gadget, chains, tooling, editor, api, mcp, tree);
    }

    private void applySnapshot(UiSnapshot snapshot, boolean appliedTree, String treeKeyword) {
        if (snapshot == null) {
            return;
        }
        syncStartPageVisibility(snapshot.build());
        if (snapshot.build() != null) {
            startPanel.applySnapshot(snapshot.build());
        }
        if (snapshot.search() != null) {
            searchPanel.applySnapshot(snapshot.search());
        }
        if (snapshot.call() != null) {
            callPanel.applySnapshot(snapshot.call());
            implPanel.applySnapshot(snapshot.call());
        }
        if (snapshot.web() != null) {
            webPanel.applySnapshot(snapshot.web());
        }
        if (snapshot.note() != null) {
            notePanel.applySnapshot(snapshot.note());
        }
        if (snapshot.sca() != null) {
            scaPanel.applySnapshot(snapshot.sca());
        }
        if (snapshot.leak() != null) {
            leakPanel.applySnapshot(snapshot.leak());
        }
        if (snapshot.gadget() != null) {
            gadgetPanel.applySnapshot(snapshot.gadget());
        }
        if (snapshot.chains() != null) {
            chainsPanel.applySnapshot(snapshot.chains());
        }
        if (snapshot.tooling() != null) {
            advancePanel.applySnapshot(snapshot.tooling());
            updateStripeStyle(snapshot.tooling().stripeShowNames(), snapshot.tooling().stripeWidth());
        }
        if (snapshot.apiInfo() != null || snapshot.mcp() != null) {
            apiPanel.applySnapshot(snapshot.apiInfo(), snapshot.mcp());
        }
        if (snapshot.editor() != null) {
            applyEditor(snapshot.editor());
        }
        if (appliedTree && snapshot.tree() != null) {
            applyTree(snapshot.tree());
            lastTreeKeyword = safe(treeKeyword);
            lastTreeRefreshAt = System.currentTimeMillis();
        }
    }

    private void applyEditor(EditorDocumentDto doc) {
        currentJarField.setText(safe(doc.jarName()));
        currentClassField.setText(safe(doc.className()));
        currentMethodField.setText(safe(doc.methodName()) + safe(doc.methodDesc()));
        String nextText = safe(doc.content());
        if (!Objects.equals(editorArea.getText(), nextText)) {
            editorArea.setText(nextText);
        }
        refreshStructureOutline(doc.className(), nextText);
        int target = Math.max(0, Math.min(editorArea.getDocument().getLength(), doc.caretOffset()));
        editorArea.setCaretPosition(target);
        editorStatusValue.setText(safe(doc.statusText()));
        if (!nextText.isBlank()) {
            selectCodeTab();
        }
    }

    private void refreshStructureOutline(String className, String content) {
        List<StructureItem> items = parseStructureItems(className, content);
        structureModel.clear();
        for (StructureItem item : items) {
            structureModel.addElement(item);
        }
        structureStatusValue.setText(String.valueOf(items.size()));
    }

    private List<StructureItem> parseStructureItems(String className, String content) {
        List<StructureItem> items = new ArrayList<>();
        String normalizedClass = safe(className);
        if (!normalizedClass.isBlank()) {
            items.add(new StructureItem("class " + normalizedClass, 0));
        }
        if (content == null || content.isBlank()) {
            return items;
        }
        int offset = 0;
        String[] lines = content.split("\n", -1);
        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine;
            String clean = line.replace("\r", "");
            String trimmed = clean.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("//") && !STRUCTURE_SKIP_PATTERN.matcher(trimmed).matches()) {
                Matcher matcher = STRUCTURE_METHOD_PATTERN.matcher(clean);
                if (matcher.matches()) {
                    String method = safe(matcher.group(1));
                    if (!method.isBlank()) {
                        items.add(new StructureItem(method + "()", offset));
                    }
                }
            }
            offset += line.length() + 1;
        }
        return items;
    }

    private void applyTree(List<TreeNodeDto> nodes) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("workspace");
        int count = 0;
        if (nodes != null) {
            for (TreeNodeDto node : nodes) {
                count += appendTreeNode(root, node);
            }
        }
        treeModel.setRoot(root);
        treeModel.reload();
        if (count > 0) {
            treeCardLayout.show(treeCardPanel, "tree");
            projectTree.expandRow(0);
            treeStatusValue.setText("类: " + count);
        } else {
            treeCardLayout.show(treeCardPanel, "empty");
            treeStatusValue.setText("无项目");
        }
    }

    private int appendTreeNode(DefaultMutableTreeNode parent, TreeNodeDto node) {
        if (node == null) {
            return 0;
        }
        TreeNodeUi ui = new TreeNodeUi(safe(node.label()), safe(node.value()), node.directory());
        DefaultMutableTreeNode current = new DefaultMutableTreeNode(ui);
        parent.add(current);
        int count = 1;
        if (node.children() != null) {
            for (TreeNodeDto child : node.children()) {
                count += appendTreeNode(current, child);
            }
        }
        return count;
    }

    private void registerToolingWindowConsumer() {
        RuntimeFacades.setToolingWindowConsumer(request -> {
            if (request == null) {
                return;
            }
            SwingUtilities.invokeLater(() -> handleToolingWindow(request));
        });
    }

    private void handleToolingWindow(ToolingWindowRequest request) {
        ToolingWindowAction action = request.action();
        ToolingWindowPayload payload = request.payload();
        if (action == null) {
            return;
        }
        if (payload instanceof ToolingWindowPayload.TextPayload text) {
            showTextDialog(safe(text.title()), safe(text.content()));
            return;
        }
        switch (action) {
            case MARKDOWN_VIEWER -> {
                if (payload instanceof ToolingWindowPayload.MarkdownPayload markdown) {
                    showTextDialog(safe(markdown.title()), loadMarkdownText(markdown.markdownResource()));
                } else {
                    showTextDialog("Markdown", "No markdown payload.");
                }
            }
            case SCA_INPUT_PICKER -> chooseScaInput(payload);
            case GADGET_DIR_PICKER -> chooseGadgetDir(payload);
            case CHAINS_RESULT -> showChainsResult(payload);
            case CHAINS_ADVANCED -> {
                setRightCollapsed(false);
                topTab = ToolTab.CHAINS;
                applyRightPaneState();
            }
            default -> showTextDialog("Tool Window", "Legacy dialog not migrated yet: " + action.name());
        }
    }

    private void chooseScaInput(ToolingWindowPayload payload) {
        String current = payload instanceof ToolingWindowPayload.PathPayload p ? safe(p.value()) : "";
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select SCA Input");
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        if (!current.isBlank()) {
            chooser.setSelectedFile(Paths.get(current).toFile());
        }
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null) {
            return;
        }
        ScaSnapshotDto snapshot = RuntimeFacades.sca().snapshot();
        if (snapshot == null || snapshot.settings() == null) {
            return;
        }
        ScaSettingsDto old = snapshot.settings();
        RuntimeFacades.sca().apply(new ScaSettingsDto(
                old.scanLog4j(),
                old.scanShiro(),
                old.scanFastjson(),
                chooser.getSelectedFile().getAbsolutePath(),
                old.outputMode(),
                old.outputFile()
        ));
    }

    private void chooseGadgetDir(ToolingWindowPayload payload) {
        String current = payload instanceof ToolingWindowPayload.PathPayload p ? safe(p.value()) : "";
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Gadget Dependency Directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (!current.isBlank()) {
            chooser.setSelectedFile(Paths.get(current).toFile());
        }
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null) {
            return;
        }
        GadgetSnapshotDto snapshot = RuntimeFacades.gadget().snapshot();
        if (snapshot == null || snapshot.settings() == null) {
            return;
        }
        GadgetSettingsDto old = snapshot.settings();
        RuntimeFacades.gadget().apply(new GadgetSettingsDto(
                chooser.getSelectedFile().getAbsolutePath(),
                old.nativeMode(),
                old.hessian(),
                old.jdbc(),
                old.fastjson()
        ));
    }

    private void showChainsResult(ToolingWindowPayload payload) {
        if (!(payload instanceof ToolingWindowPayload.ChainsResultPayload chains)) {
            showTextDialog("Chains", "No result payload.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(safe(chains.title())).append('\n');
        sb.append("total: ").append(chains.items() == null ? 0 : chains.items().size()).append('\n');
        sb.append('\n');
        List<ChainsResultItemDto> items = chains.items();
        if (items == null || items.isEmpty()) {
            sb.append(safe(chains.emptyHint()));
        } else {
            for (ChainsResultItemDto item : items) {
                if (item == null) {
                    continue;
                }
                sb.append("#").append(item.index())
                        .append(" ").append(safe(item.sink()))
                        .append(" <= ").append(safe(item.source())).append('\n');
                sb.append("depth=").append(item.depth())
                        .append(", path=").append(item.pathCount())
                        .append(", node=").append(item.nodeCount())
                        .append(", edge=").append(item.edgeCount())
                        .append(", ms=").append(item.elapsedMs()).append('\n');
                if (!safe(item.recommend()).isBlank()) {
                    sb.append("recommend: ").append(item.recommend()).append('\n');
                }
                if (!safe(item.taintDetail()).isBlank()) {
                    sb.append(item.taintDetail()).append('\n');
                }
                if (!safe(item.sanitizerDetail()).isBlank()) {
                    sb.append("sanitizer:\n").append(item.sanitizerDetail()).append('\n');
                }
                if (item.methods() != null && !item.methods().isEmpty()) {
                    sb.append("methods:\n");
                    for (MethodNavDto method : item.methods()) {
                        sb.append("  ").append(safe(method.className()))
                                .append('#').append(safe(method.methodName()))
                                .append(safe(method.methodDesc())).append('\n');
                    }
                }
                sb.append('\n');
            }
        }
        showTextDialog(safe(chains.title()), sb.toString());
    }

    private void showTextDialog(String title, String content) {
        JDialog dialog = new JDialog(this, safe(title), false);
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(false);
        area.setWrapStyleWord(false);
        area.setText(safe(content));
        area.setCaretPosition(0);
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(new JScrollPane(area), BorderLayout.CENTER);
        dialog.setSize(new Dimension(980, 640));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private String loadMarkdownText(String markdownResource) {
        String raw = safe(markdownResource).trim();
        if (raw.isBlank()) {
            return "";
        }
        try {
            Path path = Paths.get(raw);
            if (Files.exists(path)) {
                return Files.readString(path, StandardCharsets.UTF_8);
            }
        } catch (Throwable ignored) {
        }
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(raw)) {
            if (in != null) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Throwable ignored) {
        }
        if (raw.startsWith("src/main/resources/")) {
            String cp = raw.substring("src/main/resources/".length());
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(cp)) {
                if (in != null) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            } catch (Throwable ignored) {
            }
        }
        return "markdown not found: " + raw;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("文件");
        JMenuItem refreshTree = new JMenuItem("刷新项目");
        refreshTree.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        refreshTree.addActionListener(e -> refreshTreeNow());
        JMenuItem exit = new JMenuItem("退出");
        exit.addActionListener(e -> closeWithConfirm());
        fileMenu.add(refreshTree);
        fileMenu.add(exit);

        JMenu viewMenu = new JMenu("视图");
        JCheckBoxMenuItem toggleProjectTree = new JCheckBoxMenuItem("左侧栏", true);
        toggleProjectTree.addActionListener(e -> setLeftCollapsed(!toggleProjectTree.isSelected()));
        viewMenu.add(toggleProjectTree);
        viewMenu.addSeparator();
        for (ToolTab tab : ToolTab.values()) {
            JMenuItem item = new JMenuItem(tab.title());
            item.addActionListener(e -> {
                topTab = tab;
                setRightCollapsed(false);
                applyRightPaneState();
            });
            viewMenu.add(item);
        }

        JMenu navMenu = new JMenu("导航");
        navMenu.add(menuItem("后退", e -> RuntimeFacades.editor().goPrev()));
        navMenu.add(menuItem("前进", e -> RuntimeFacades.editor().goNext()));

        JMenu toolsMenu = new JMenu("工具");
        toolsMenu.add(menuItem("导出", e -> RuntimeFacades.tooling().openExportTool()));
        toolsMenu.add(menuItem("全局搜索", e -> RuntimeFacades.tooling().openGlobalSearchTool()));
        toolsMenu.add(menuItem("系统监控", e -> RuntimeFacades.tooling().openSystemMonitorTool()));
        toolsMenu.add(menuItem("SQL 控制台", e -> RuntimeFacades.tooling().openSqlConsoleTool()));
        toolsMenu.add(menuItem("版本", e -> RuntimeFacades.tooling().openVersionInfo()));

        JMenu pluginMenu = new JMenu("插件");
        pluginMenu.add(menuItem("远程加载", e -> RuntimeFacades.tooling().openRemoteLoadTool()));
        pluginMenu.add(menuItem("代理", e -> RuntimeFacades.tooling().openProxyTool()));
        pluginMenu.add(menuItem("字节码调试", e -> RuntimeFacades.tooling().openBytecodeDebugger()));

        JMenu helpMenu = new JMenu("帮助");
        helpMenu.add(menuItem("文档", e -> RuntimeFacades.tooling().openDocs()));
        helpMenu.add(menuItem("项目主页", e -> RuntimeFacades.tooling().openProjectSite()));
        helpMenu.add(menuItem("报告问题", e -> RuntimeFacades.tooling().openReportBug()));

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(navMenu);
        menuBar.add(toolsMenu);
        menuBar.add(pluginMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private JMenuItem menuItem(String title, java.awt.event.ActionListener action) {
        JMenuItem item = new JMenuItem(title);
        item.addActionListener(action);
        return item;
    }

    private void closeWithConfirm() {
        int resp = JOptionPane.showConfirmDialog(
                this,
                "CONFIRM EXIT?",
                "EXIT",
                JOptionPane.OK_CANCEL_OPTION
        );
        if (resp != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            RuntimeFacades.apiMcp().stopAll();
        } catch (Throwable ex) {
            logger.debug("stop mcp failed: {}", ex.toString());
        }
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        RuntimeFacades.setToolingWindowConsumer(null);
        dispose();
        System.exit(0);
    }

    private <T> T snapshotSafe(Supplier<T> supplier, T fallback) {
        try {
            T value = supplier.get();
            return value == null ? fallback : value;
        } catch (Throwable ex) {
            logger.debug("snapshot failed: {}", ex.toString());
            return fallback;
        }
    }

    private static JTextField readonlyField() {
        JTextField field = new JTextField();
        field.setEditable(false);
        return field;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private enum ToolTab {
        START("start"),
        SEARCH("search"),
        CALL("call"),
        IMPL("impl"),
        WEB("web"),
        NOTE("note"),
        SCA("sca"),
        LEAK("leak"),
        GADGET("gadget"),
        ADVANCE("advance"),
        CHAINS("chains"),
        API("api");

        private final String code;

        ToolTab(String code) {
            this.code = code;
        }

        String card() {
            return code;
        }

        String title() {
            return code;
        }

        String buttonText() {
            return code;
        }
    }

    private static final class VerticalStripeToggleButton extends JToggleButton {
        private VerticalStripeToggleButton(String text) {
            super(text);
            setFocusable(false);
            setBorder(BorderFactory.createEmptyBorder());
            setOpaque(false);
            setPreferredSize(new Dimension(LEFT_STRIPE_WIDTH - 2, 84));
            setMinimumSize(new Dimension(LEFT_STRIPE_WIDTH - 2, 66));
            setMaximumSize(new Dimension(LEFT_STRIPE_WIDTH - 2, 180));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Color bg;
            if (isSelected()) {
                bg = new Color(0xDCEBFF);
            } else if (getModel().isRollover()) {
                bg = new Color(0xE8E8E8);
            } else {
                bg = new Color(0xF3F3F3);
            }
            g2.setColor(bg);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(SHELL_LINE);
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.rotate(-Math.PI / 2);
            FontMetrics fm = g2.getFontMetrics(getFont());
            String text = getText() == null ? "" : getText();
            int textWidth = fm.stringWidth(text);
            int textX = -getHeight() + Math.max(0, (getHeight() - textWidth) / 2);
            int textY = (getWidth() + fm.getAscent() - fm.getDescent()) / 2;
            g2.setColor(Color.DARK_GRAY);
            g2.drawString(text, textX, textY);
            g2.dispose();
        }
    }

    private record StructureItem(String label, int caretOffset) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record TreeNodeUi(String label, String value, boolean directory) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record UiSnapshot(
            BuildSnapshotDto build,
            SearchSnapshotDto search,
            CallGraphSnapshotDto call,
            WebSnapshotDto web,
            NoteSnapshotDto note,
            ScaSnapshotDto sca,
            LeakSnapshotDto leak,
            GadgetSnapshotDto gadget,
            ChainsSnapshotDto chains,
            ToolingConfigSnapshotDto tooling,
            EditorDocumentDto editor,
            ApiInfoDto apiInfo,
            McpConfigDto mcp,
            List<TreeNodeDto> tree
    ) {
    }
}
