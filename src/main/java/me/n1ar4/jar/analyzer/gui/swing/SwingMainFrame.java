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
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.others.Proxy;
import me.n1ar4.jar.analyzer.engine.DecompileDispatcher;
import me.n1ar4.jar.analyzer.engine.DecompileType;
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
import me.n1ar4.jar.analyzer.gui.swing.toolwindow.ToolWindowDialogs;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
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
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JToggleButton;
import javax.swing.JRadioButton;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
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
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URL;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SwingMainFrame extends JFrame {
    private static final Logger logger = LogManager.getLogger();
    private static final int REFRESH_INTERVAL_MS = 700;
    private static final long TREE_REFRESH_INTERVAL_MS = 3000;
    private static final long IDLE_FALLBACK_REFRESH_MS = 1200;
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
    private static final int BUILD_LOG_BUFFER_LIMIT = 60_000;
    private static final int EDITOR_TAB_LIMIT = 80;
    private static final String EDITOR_TAB_KEY_PROP = "editor.tab.key";
    private static final DateTimeFormatter BUILD_LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
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
    private final AtomicBoolean refreshRequested = new AtomicBoolean(true);

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
    private final JLabel leftEmptyLabel = new JLabel();
    private final DefaultListModel<StructureItem> structureModel = new DefaultListModel<>();
    private final JList<StructureItem> structureList = new JList<>(structureModel);
    private final JLabel structureStatusValue = new JLabel("0");
    private final JLabel structureTitleLabel = new JLabel();
    private final JSplitPane leftToolSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    private final JToggleButton leftTreeStripeButton = new VerticalStripeToggleButton("");
    private final JToggleButton leftStructureStripeButton = new VerticalStripeToggleButton("");
    private JButton treeRefreshButton;
    private JButton treeSearchButton;
    private final Icon treeCategoryInputIcon = loadIcon("icons/jadx/moduleDirectory.svg", 16);
    private final Icon treeCategorySourceIcon = loadIcon("icons/jadx/java.svg", 16);
    private final Icon treeCategoryResourceIcon = loadIcon("icons/jadx/resourcesRoot.svg", 16);
    private final Icon treeCategoryDependencyIcon = loadIcon("icons/jadx/archive.svg", 16);
    private final Icon treePackageIcon = loadIcon("icons/jadx/package.svg", 16);
    private final Icon treeClassIcon = loadIcon("icons/jadx/class.svg", 16);
    private final Icon treeFolderIcon = loadIcon("icons/jadx/folder.svg", 16);
    private final Icon treeFileIcon = loadIcon("icons/jadx/file_any_type.svg", 16);

    private final JTabbedPane editorClassTabs = new JTabbedPane();
    private final JLabel editorPathValue = new JLabel();
    private final Map<String, EditorTabRef> editorTabRefs = new java.util.LinkedHashMap<>();
    private final RSyntaxTextArea editorArea = new RSyntaxTextArea();
    private final JTabbedPane workbenchTabs = new JTabbedPane();
    private JPanel startPageView;
    private JPanel codePageView;
    private final JTextArea recentProjectArea = new JTextArea();
    private final JLabel startSectionLabel = new JLabel();
    private final JButton startOpenFileButton = new JButton();
    private final JButton startOpenProjectButton = new JButton();
    private final JLabel recentSectionLabel = new JLabel();

    private final JToolBar rightStripe = new JToolBar(JToolBar.VERTICAL);
    private final JToggleButton buildLogButton = new JToggleButton();
    private JToggleButton topToggleMergePackageRoot;
    private JToggleButton topToggleEditorTabs;
    private JToggleButton topToggleQuickMode;
    private JToggleButton topToggleFixMethodImpl;
    private final JPanel rightContentHost = new JPanel(new BorderLayout());
    private final JPanel topCards = new JPanel(new java.awt.CardLayout());
    private final JLabel topTitle = new JLabel();
    private final StringBuilder buildLogBuffer = new StringBuilder();
    private String lastBuildLogStatus = "";
    private int lastBuildLogProgress = -1;

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
    private String uiLanguage = "zh";
    private String uiTheme = "";
    private String initialTheme = "default";
    private boolean localizationReady;
    private boolean stripeNamesVisible = true;
    private boolean topToolbarToggleSyncing;
    private boolean editorTabsVisible = true;
    private boolean editorTabSelectionAdjusting;
    private String activeEditorTabKey = "";
    private String lastEditorStructureSignature = "";
    private String lastEditorCaretSyncSignature = "";
    private int lastTreeFingerprint;
    private boolean treeFingerprintReady;
    private long lastRefreshCompletedAt;
    private BuildSnapshotDto lastAppliedBuildSnapshot;
    private SearchSnapshotDto lastAppliedSearchSnapshot;
    private CallGraphSnapshotDto lastAppliedCallSnapshot;
    private WebSnapshotDto lastAppliedWebSnapshot;
    private NoteSnapshotDto lastAppliedNoteSnapshot;
    private ScaSnapshotDto lastAppliedScaSnapshot;
    private LeakSnapshotDto lastAppliedLeakSnapshot;
    private GadgetSnapshotDto lastAppliedGadgetSnapshot;
    private ChainsSnapshotDto lastAppliedChainsSnapshot;
    private ToolingConfigSnapshotDto lastAppliedToolingSnapshot;
    private ApiInfoDto lastAppliedApiInfoSnapshot;
    private McpConfigDto lastAppliedMcpSnapshot;
    private EditorDocumentDto lastAppliedEditorSnapshot;

    public SwingMainFrame(StartCmd startCmd) {
        super("*New Project - jadx-gui");
        this.startCmd = startCmd;
        ToolingConfigSnapshotDto initialTooling = snapshotSafe(RuntimeFacades.tooling()::configSnapshot, null);
        if (initialTooling != null) {
            uiLanguage = normalizeLanguage(initialTooling.language());
            initialTheme = normalizeTheme(initialTooling.theme());
        }
        SwingI18n.setLanguage(uiLanguage);
        initFrame();
        initLayout();
        initActions();
        applyLanguage(uiLanguage);
        applyTheme(initialTheme);
        registerToolingWindowConsumer();
        requestRefresh(true, true);
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

        refreshTimer = new Timer(REFRESH_INTERVAL_MS, e -> onRefreshTimerTick());
        refreshTimer.setRepeats(true);

        treeSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                requestRefresh(true, false);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                requestRefresh(true, false);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                requestRefresh(true, false);
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
        ToolingConfigSnapshotDto tooling = snapshotSafe(RuntimeFacades.tooling()::configSnapshot, null);
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
        addTopToolbarButton(bar, "icons/jadx/locate.svg", "同步项目树", e -> requestRefresh(false, true));
        topToggleMergePackageRoot = addTopToolbarToggleButton(
                bar,
                "icons/jadx/abbreviatePackageNames.svg",
                tr("包根合并", "Merge Package Root"),
                tooling != null && tooling.mergePackageRoot(),
                true,
                e -> {
                    if (topToolbarToggleSyncing) {
                        return;
                    }
                    RuntimeFacades.tooling().toggleMergePackageRoot();
                    requestRefresh(true, true);
                }
        );
        topToggleEditorTabs = addTopToolbarToggleButton(
                bar,
                "icons/jadx/editorPreview.svg",
                tr("代码标签栏", "Code Tabs"),
                editorTabsVisible,
                true,
                e -> {
                    if (topToolbarToggleSyncing) {
                        return;
                    }
                    editorTabsVisible = topToggleEditorTabs.isSelected();
                    applyEditorTabsVisibility();
                }
        );
        topToggleQuickMode = addTopToolbarToggleButton(
                bar,
                "icons/jadx/pagination.svg",
                tr("快速模式", "Quick Mode"),
                tooling != null && tooling.quickMode(),
                true,
                e -> {
                    if (topToolbarToggleSyncing) {
                        return;
                    }
                    RuntimeFacades.tooling().toggleQuickMode();
                    requestRefresh(false, true);
                }
        );
        addTopToolbarSeparator(bar);
        addTopToolbarButton(bar, "icons/jadx/find.svg", "全局搜索", e -> RuntimeFacades.tooling().openGlobalSearchTool());
        addTopToolbarButton(bar, "icons/jadx/ejbFinderMethod.svg", "类搜索", e -> promptTreeSearchKeyword());
        addTopToolbarButton(bar, "icons/jadx/usagesFinder.svg", "注释搜索", e -> RuntimeFacades.tooling().openGlobalSearchTool());
        addTopToolbarButton(bar, "icons/jadx/home.svg", "打开 start 面板", e -> focusToolTab(ToolTab.START));
        addTopToolbarButton(bar, "icons/jadx/application.svg", "打开 web 面板", e -> focusToolTab(ToolTab.WEB));
        addTopToolbarButton(bar, "icons/jadx/androidManifest.svg", "打开 api 面板", e -> focusToolTab(ToolTab.API));
        addTopToolbarSeparator(bar);
        addTopToolbarButton(bar, "icons/jadx/left.svg", "后退", e -> {
            RuntimeFacades.editor().goPrev();
            requestRefresh(false, true);
        });
        addTopToolbarButton(bar, "icons/jadx/right.svg", "前进", e -> {
            RuntimeFacades.editor().goNext();
            requestRefresh(false, true);
        });
        addTopToolbarButton(bar, "icons/jadx/addFile.svg", tr("收藏当前方法", "Add Current Method To Favorites"), e -> addCurrentMethodToFavorites());
        addTopToolbarSeparator(bar);
        topToggleFixMethodImpl = addTopToolbarToggleButton(
                bar,
                "icons/jadx/helmChartLock.svg",
                tr("修复方法实现", "Fix Method Impl"),
                tooling != null && tooling.fixMethodImpl(),
                true,
                e -> {
                    if (topToolbarToggleSyncing) {
                        return;
                    }
                    RuntimeFacades.tooling().toggleFixMethodImpl();
                    requestRefresh(false, true);
                }
        );
        addTopToolbarButton(bar, "icons/jadx/quark.svg", "混淆分析", e -> RuntimeFacades.tooling().openObfuscationTool());
        addTopToolbarButton(bar, "icons/jadx/startDebugger.svg", "字节码调试", e -> RuntimeFacades.tooling().openBytecodeDebugger());
        addTopToolbarSeparator(bar);
        addTopToolbarButton(bar, "icons/jadx/logVerbose.svg", "系统监控", e -> RuntimeFacades.tooling().openSystemMonitorTool());
        addTopToolbarSeparator(bar);
        addTopToolbarButton(bar, "icons/jadx/settings.svg", "打开 advance 面板", e -> focusToolTab(ToolTab.ADVANCE));
        addTopToolbarSeparator(bar);
        bar.add(Box.createHorizontalGlue());
        syncTopToolbarToggles(tooling);
        return bar;
    }

    private void addTopToolbarButton(JToolBar bar, String iconPath, String tooltip, java.awt.event.ActionListener listener) {
        bar.add(toolbarIconButton(iconPath, tooltip, listener, 15, true));
    }

    private JToggleButton addTopToolbarToggleButton(
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
        return button;
    }

    private void addTopToolbarSeparator(JToolBar bar) {
        bar.addSeparator(new Dimension(8, TOP_TOOLBAR_BUTTON_SIZE));
    }

    private void applyEditorTabsVisibility() {
        editorClassTabs.setVisible(editorTabsVisible);
        editorClassTabs.revalidate();
        editorClassTabs.repaint();
    }

    private void syncTopToolbarToggles(ToolingConfigSnapshotDto tooling) {
        topToolbarToggleSyncing = true;
        try {
            if (topToggleMergePackageRoot != null && tooling != null) {
                topToggleMergePackageRoot.setSelected(tooling.mergePackageRoot());
            }
            if (topToggleQuickMode != null && tooling != null) {
                topToggleQuickMode.setSelected(tooling.quickMode());
            }
            if (topToggleFixMethodImpl != null && tooling != null) {
                topToggleFixMethodImpl.setSelected(tooling.fixMethodImpl());
            }
            if (topToggleEditorTabs != null) {
                topToggleEditorTabs.setSelected(editorTabsVisible);
            }
        } finally {
            topToolbarToggleSyncing = false;
        }
        applyEditorTabsVisibility();
    }

    private void focusToolTab(ToolTab tab) {
        topTab = tab;
        setRightCollapsed(false);
    }

    private void addCurrentMethodToFavorites() {
        boolean ok = RuntimeFacades.editor().addCurrentToFavorites();
        if (!ok) {
            showTextDialog(
                    tr("收藏失败", "Favorite Failed"),
                    tr("当前没有可收藏的方法，或运行时尚未就绪。",
                            "No current method can be added, or runtime is not ready.")
            );
            return;
        }
        RuntimeFacades.note().load();
        requestRefresh(false, true);
    }

    private void promptTreeSearchKeyword() {
        String kw = JOptionPane.showInputDialog(
                this,
                tr("输入类名关键字", "Input Class Keyword"),
                safe(treeSearchField.getText())
        );
        if (kw == null) {
            return;
        }
        treeSearchField.setText(kw);
        requestRefresh(true, true);
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
            // Don't force tab selection on every refresh.
            // User can stay on "Code" even before opening a project.
            if (workbenchTabs.getSelectedComponent() == null
                    && safe(editorArea.getText()).isBlank()) {
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
        requestRefresh(true, true);
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
        treeRefreshButton = toolbarIconButton("icons/jadx/refresh.svg", tr("刷新", "Refresh"), e -> refreshTreeNow());
        treeSearchButton = toolbarIconButton("icons/jadx/find.svg", tr("搜索类名", "Search Class"), e -> promptTreeSearchKeyword());
        leftBar.add(treeRefreshButton);
        leftBar.add(treeSearchButton);
        panel.add(leftBar, BorderLayout.NORTH);

        projectTree.setRootVisible(false);
        projectTree.setShowsRootHandles(true);
        projectTree.setBackground(Color.WHITE);
        projectTree.setCellRenderer(new ProjectTreeRenderer());
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
        structureTitleLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 2));
        structureStatusValue.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 6));
        header.add(structureTitleLabel, BorderLayout.WEST);
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
                    selectCodeTab();
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

        leftTreeStripeButton.setToolTipText(tr("目录树", "Project"));
        leftTreeStripeButton.addActionListener(e -> {
            treePanelCollapsed = !leftTreeStripeButton.isSelected();
            applyLeftToolWindowState();
        });
        leftStructureStripeButton.setToolTipText(tr("结构", "Structure"));
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
        workbenchTabs.addTab(tr("开始页", "Start"), startPageView);
        workbenchTabs.addTab(tr("代码", "Code"), codePageView);
        workbenchTabs.setSelectedIndex(0);

        panel.add(workbenchTabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStartPagePanel() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(SHELL_BG);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.setMinimumSize(new Dimension(220, 220));
        center.setPreferredSize(new Dimension(740, 520));
        center.setMaximumSize(new Dimension(980, Integer.MAX_VALUE));

        JPanel startBox = new JPanel(new BorderLayout(8, 8));
        startBox.setBackground(new Color(0xEFEFEF));
        startBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SHELL_LINE),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        startSectionLabel.setFont(startSectionLabel.getFont().deriveFont(Font.BOLD));
        startBox.add(startSectionLabel, BorderLayout.NORTH);
        JPanel startButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        startButtons.setOpaque(false);
        startOpenFileButton.addActionListener(e -> openFileFromToolbar(false));
        startOpenProjectButton.addActionListener(e -> openFileFromToolbar(true));
        startButtons.add(startOpenFileButton);
        startButtons.add(startOpenProjectButton);
        startBox.add(startButtons, BorderLayout.CENTER);

        JPanel recentBox = new JPanel(new BorderLayout(8, 8));
        recentBox.setBackground(new Color(0xEFEFEF));
        recentBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SHELL_LINE),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        recentSectionLabel.setFont(recentSectionLabel.getFont().deriveFont(Font.BOLD));
        recentBox.add(recentSectionLabel, BorderLayout.NORTH);
        recentProjectArea.setEditable(false);
        recentProjectArea.setBackground(Color.WHITE);
        recentProjectArea.setBorder(BorderFactory.createLineBorder(new Color(0xDDDDDD)));
        recentProjectArea.setRows(8);
        recentProjectArea.setText("");
        recentBox.add(new JScrollPane(recentProjectArea), BorderLayout.CENTER);

        GridBagConstraints row = new GridBagConstraints();
        row.gridx = 0;
        row.weightx = 1.0;
        row.fill = GridBagConstraints.BOTH;
        row.gridy = 0;
        row.weighty = 0.38;
        row.insets = new Insets(0, 0, 18, 0);
        center.add(startBox, row);
        row.gridy = 1;
        row.weighty = 0.62;
        row.insets = new Insets(0, 0, 0, 0);
        center.add(recentBox, row);

        JPanel limit = new JPanel();
        limit.setOpaque(false);
        limit.setLayout(new BoxLayout(limit, BoxLayout.X_AXIS));
        limit.setBorder(BorderFactory.createEmptyBorder(28, 24, 28, 24));
        limit.add(Box.createHorizontalGlue());
        limit.add(center);
        limit.add(Box.createHorizontalGlue());

        page.add(limit, BorderLayout.CENTER);
        return page;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private JPanel buildCodePagePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        editorClassTabs.setBorder(BorderFactory.createLineBorder(SHELL_LINE));
        editorClassTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        editorClassTabs.addChangeListener(e -> onEditorTabChanged());
        installEditorTabInteractions();

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
        editorPathValue.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        status.add(editorPathValue, BorderLayout.WEST);

        panel.add(editorClassTabs, BorderLayout.NORTH);
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
        stripeWrap.setBackground(new Color(0xF3F3F3));

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

    private void initBuildLogButton() {
        buildLogButton.setFocusable(false);
        Icon logIcon = loadIcon("icons/jadx/logVerbose.svg", 16);
        if (logIcon != null) {
            buildLogButton.setIcon(logIcon);
        }
        buildLogButton.setToolTipText(tr("构建日志", "Build Log"));
        buildLogButton.addActionListener(e -> {
            showBuildLogDialog();
            buildLogButton.setSelected(false);
        });
        updateLogButtonStyle(stripeNamesVisible);
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
        initBuildLogButton();
        rightStripe.add(Box.createVerticalGlue());
        rightStripe.add(buildLogButton);
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
            topTitle.setText(tr("工具窗", "Tool Window"));
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
        stripeNamesVisible = showNames;
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
        updateLogButtonStyle(showNames);
        rightStripe.revalidate();
        rightStripe.repaint();
    }

    private void updateLogButtonStyle(boolean showNames) {
        buildLogButton.setText(showNames ? tr("日志", "Log") : "");
        buildLogButton.setHorizontalAlignment(showNames ? JToggleButton.LEADING : JToggleButton.CENTER);
        buildLogButton.setHorizontalTextPosition(JToggleButton.RIGHT);
        buildLogButton.setIconTextGap(showNames ? 4 : 0);
        int w = Math.max(38, stripeWidth - 2);
        Dimension size = new Dimension(w, 32);
        buildLogButton.setMinimumSize(size);
        buildLogButton.setPreferredSize(size);
        buildLogButton.setMaximumSize(size);
        buildLogButton.setMargin(new Insets(2, 4, 2, 4));
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
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshTreeNow);
            return;
        }
        RuntimeFacades.projectTree().refresh();
        requestRefresh(true, true);
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
        requestRefresh(false, true);
    }

    private void requestRefresh(boolean forceTree, boolean immediate) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> requestRefresh(forceTree, immediate));
            return;
        }
        if (forceTree) {
            forceTreeRefresh.set(true);
        }
        refreshRequested.set(true);
        if (immediate) {
            refreshAsync();
        }
    }

    private void onRefreshTimerTick() {
        long now = System.currentTimeMillis();
        if (refreshRequested.get() || now - lastRefreshCompletedAt >= fallbackRefreshIntervalMs()) {
            refreshAsync();
        }
    }

    private long fallbackRefreshIntervalMs() {
        BuildSnapshotDto build = lastAppliedBuildSnapshot;
        if (build != null) {
            int progress = build.buildProgress();
            if (progress > 0 && progress < 100) {
                return REFRESH_INTERVAL_MS;
            }
            String status = safe(build.statusText()).toLowerCase(Locale.ROOT);
            if (!status.isBlank() && !status.contains("ready")) {
                return REFRESH_INTERVAL_MS;
            }
        }
        return IDLE_FALLBACK_REFRESH_MS;
    }

    private void refreshAsync() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshAsync);
            return;
        }
        if (!refreshBusy.compareAndSet(false, true)) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean requested = refreshRequested.getAndSet(false);
        boolean dueFallback = now - lastRefreshCompletedAt >= fallbackRefreshIntervalMs();
        if (!requested && !dueFallback) {
            refreshBusy.set(false);
            return;
        }
        final String treeKeyword = safe(treeSearchField.getText());
        final long refreshNow = now;
        final boolean treeChanged = !Objects.equals(treeKeyword, lastTreeKeyword);
        final boolean loadTree = forceTreeRefresh.getAndSet(false)
                || treeChanged
                || refreshNow - lastTreeRefreshAt >= TREE_REFRESH_INTERVAL_MS;

        Thread.ofVirtual().name("swing-runtime-sync").start(() -> {
            UiSnapshot snapshot = null;
            try {
                snapshot = collectSnapshot(loadTree, treeKeyword);
            } catch (Throwable ex) {
                logger.warn("collect ui snapshot failed", ex);
            }
            UiSnapshot finalSnapshot = snapshot;
            SwingUtilities.invokeLater(() -> {
                try {
                    applySnapshot(finalSnapshot, loadTree, treeKeyword);
                } finally {
                    refreshBusy.set(false);
                    if (refreshRequested.get()) {
                        SwingUtilities.invokeLater(this::refreshAsync);
                    }
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
            lastRefreshCompletedAt = System.currentTimeMillis();
            return;
        }
        syncStartPageVisibility(snapshot.build());
        if (snapshot.build() != null && !Objects.equals(lastAppliedBuildSnapshot, snapshot.build())) {
            appendBuildLog(snapshot.build());
            startPanel.applySnapshot(snapshot.build());
            lastAppliedBuildSnapshot = snapshot.build();
        }
        startPanel.refreshResourceMonitor();
        if (snapshot.search() != null && !Objects.equals(lastAppliedSearchSnapshot, snapshot.search())) {
            searchPanel.applySnapshot(snapshot.search());
            lastAppliedSearchSnapshot = snapshot.search();
        }
        if (snapshot.call() != null && !Objects.equals(lastAppliedCallSnapshot, snapshot.call())) {
            callPanel.applySnapshot(snapshot.call());
            implPanel.applySnapshot(snapshot.call());
            lastAppliedCallSnapshot = snapshot.call();
        }
        if (snapshot.web() != null && !Objects.equals(lastAppliedWebSnapshot, snapshot.web())) {
            webPanel.applySnapshot(snapshot.web());
            lastAppliedWebSnapshot = snapshot.web();
        }
        if (snapshot.note() != null && !Objects.equals(lastAppliedNoteSnapshot, snapshot.note())) {
            notePanel.applySnapshot(snapshot.note());
            lastAppliedNoteSnapshot = snapshot.note();
        }
        if (snapshot.sca() != null && !Objects.equals(lastAppliedScaSnapshot, snapshot.sca())) {
            scaPanel.applySnapshot(snapshot.sca());
            lastAppliedScaSnapshot = snapshot.sca();
        }
        if (snapshot.leak() != null && !Objects.equals(lastAppliedLeakSnapshot, snapshot.leak())) {
            leakPanel.applySnapshot(snapshot.leak());
            lastAppliedLeakSnapshot = snapshot.leak();
        }
        if (snapshot.gadget() != null && !Objects.equals(lastAppliedGadgetSnapshot, snapshot.gadget())) {
            gadgetPanel.applySnapshot(snapshot.gadget());
            lastAppliedGadgetSnapshot = snapshot.gadget();
        }
        if (snapshot.chains() != null && !Objects.equals(lastAppliedChainsSnapshot, snapshot.chains())) {
            chainsPanel.applySnapshot(snapshot.chains());
            lastAppliedChainsSnapshot = snapshot.chains();
        }
        if (snapshot.tooling() != null && !Objects.equals(lastAppliedToolingSnapshot, snapshot.tooling())) {
            applyLanguage(snapshot.tooling().language());
            applyTheme(snapshot.tooling().theme());
            advancePanel.applySnapshot(snapshot.tooling());
            updateStripeStyle(snapshot.tooling().stripeShowNames(), snapshot.tooling().stripeWidth());
            syncTopToolbarToggles(snapshot.tooling());
            lastAppliedToolingSnapshot = snapshot.tooling();
        }
        boolean apiChanged = !Objects.equals(lastAppliedApiInfoSnapshot, snapshot.apiInfo());
        boolean mcpChanged = !Objects.equals(lastAppliedMcpSnapshot, snapshot.mcp());
        if ((snapshot.apiInfo() != null || snapshot.mcp() != null) && (apiChanged || mcpChanged)) {
            apiPanel.applySnapshot(snapshot.apiInfo(), snapshot.mcp());
            lastAppliedApiInfoSnapshot = snapshot.apiInfo();
            lastAppliedMcpSnapshot = snapshot.mcp();
        }
        if (snapshot.editor() != null && !Objects.equals(lastAppliedEditorSnapshot, snapshot.editor())) {
            applyEditor(snapshot.editor());
            lastAppliedEditorSnapshot = snapshot.editor();
        }
        if (appliedTree && snapshot.tree() != null) {
            String keyword = safe(treeKeyword);
            int fingerprint = fingerprintTree(snapshot.tree());
            boolean keywordChanged = !Objects.equals(keyword, lastTreeKeyword);
            boolean treeChanged = !treeFingerprintReady || fingerprint != lastTreeFingerprint;
            if (keywordChanged || treeChanged) {
                applyTree(snapshot.tree());
            }
            lastTreeKeyword = keyword;
            lastTreeFingerprint = fingerprint;
            treeFingerprintReady = true;
            lastTreeRefreshAt = System.currentTimeMillis();
        }
        lastRefreshCompletedAt = System.currentTimeMillis();
    }

    private void applyEditor(EditorDocumentDto doc) {
        syncEditorTabs(doc);
        String nextText = safe(doc.content());
        String structureSignature = safe(doc.className()) + "\u0000" + Integer.toHexString(nextText.hashCode());
        boolean contentChanged = !Objects.equals(editorArea.getText(), nextText);
        if (contentChanged) {
            editorArea.setText(nextText);
        }
        if (!Objects.equals(lastEditorStructureSignature, structureSignature)) {
            refreshStructureOutline(doc.className(), nextText);
            lastEditorStructureSignature = structureSignature;
        }
        int target = Math.max(0, Math.min(editorArea.getDocument().getLength(), doc.caretOffset()));
        String caretSignature = editorTabKey(doc)
                + "\u0000" + target
                + "\u0000" + safe(doc.methodName())
                + "\u0000" + safe(doc.methodDesc())
                + "\u0000" + safe(doc.statusText());
        boolean shouldSyncCaret = contentChanged || !Objects.equals(lastEditorCaretSyncSignature, caretSignature);
        if (shouldSyncCaret) {
            int currentCaret = Math.max(0, Math.min(editorArea.getDocument().getLength(), editorArea.getCaretPosition()));
            if (currentCaret != target) {
                editorArea.setCaretPosition(target);
            }
            lastEditorCaretSyncSignature = caretSignature;
        }
        editorPathValue.setText(formatEditorLocation(doc));
        editorPathValue.setToolTipText(safe(doc.statusText()));
        if (!safe(doc.className()).isBlank()) {
            selectCodeTab();
        }
    }

    private void syncEditorTabs(EditorDocumentDto doc) {
        if (doc == null) {
            return;
        }
        String key = editorTabKey(doc);
        if (key.isBlank()) {
            return;
        }
        EditorTabRef ref = editorTabRefs.get(key);
        String title = editorTabTitle(doc.className());
        String tooltip = formatEditorLocation(doc);
        Component marker;
        if (ref == null) {
            JPanel holder = new JPanel();
            holder.setOpaque(false);
            holder.setPreferredSize(new Dimension(0, 0));
            holder.putClientProperty(EDITOR_TAB_KEY_PROP, key);
            editorClassTabs.addTab(title, holder);
            int newIndex = editorClassTabs.indexOfComponent(holder);
            if (newIndex >= 0) {
                editorClassTabs.setTabComponentAt(newIndex, createEditorTabHeader(key, title));
            }
            marker = holder;
        } else {
            marker = ref.marker();
        }
        int index = editorClassTabs.indexOfComponent(marker);
        if (index >= 0) {
            if (!Objects.equals(editorClassTabs.getTitleAt(index), title)) {
                editorClassTabs.setTitleAt(index, title);
                updateEditorTabHeaderTitle(index, title);
            }
            editorClassTabs.setToolTipTextAt(index, tooltip);
            if (editorClassTabs.getSelectedIndex() != index) {
                editorTabSelectionAdjusting = true;
                try {
                    editorClassTabs.setSelectedIndex(index);
                } finally {
                    editorTabSelectionAdjusting = false;
                }
            }
        }
        editorTabRefs.put(key, new EditorTabRef(
                key,
                safe(doc.className()),
                doc.jarId(),
                safe(doc.jarName()),
                marker
        ));
        activeEditorTabKey = key;
        trimEditorTabs();
    }

    private void onEditorTabChanged() {
        if (editorTabSelectionAdjusting) {
            return;
        }
        int idx = editorClassTabs.getSelectedIndex();
        activateEditorTabAt(idx, false);
    }

    private void trimEditorTabs() {
        if (editorTabRefs.size() <= EDITOR_TAB_LIMIT) {
            return;
        }
        List<String> keys = new ArrayList<>(editorTabRefs.keySet());
        for (String key : keys) {
            if (editorTabRefs.size() <= EDITOR_TAB_LIMIT) {
                break;
            }
            if (Objects.equals(activeEditorTabKey, key)) {
                continue;
            }
            closeEditorTab(key, false);
        }
    }

    private void installEditorTabInteractions() {
        editorClassTabs.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleEditorTabMouse(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleEditorTabMouse(e);
            }
        });
    }

    private void handleEditorTabMouse(MouseEvent e) {
        if (e == null) {
            return;
        }
        int index = editorClassTabs.indexAtLocation(e.getX(), e.getY());
        if (index < 0) {
            return;
        }
        if (SwingUtilities.isMiddleMouseButton(e)) {
            closeEditorTabAt(index, true);
            return;
        }
        if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
            showEditorTabPopup(index, e.getX(), e.getY());
        }
    }

    private void showEditorTabPopup(int index, int x, int y) {
        String key = tabKeyAt(index);
        if (key.isBlank()) {
            return;
        }
        editorTabSelectionAdjusting = true;
        try {
            editorClassTabs.setSelectedIndex(index);
        } finally {
            editorTabSelectionAdjusting = false;
        }
        JPopupMenu menu = new JPopupMenu();
        JMenuItem closeOthers = new JMenuItem(tr("关闭其它", "Close Others"));
        closeOthers.addActionListener(e -> closeOtherEditorTabs(key));
        menu.add(closeOthers);
        menu.show(editorClassTabs, x, y);
    }

    private void showEditorTabPopupByKey(String key, Component invoker, int x, int y) {
        int index = indexOfTabKey(key);
        if (index < 0) {
            return;
        }
        editorTabSelectionAdjusting = true;
        try {
            editorClassTabs.setSelectedIndex(index);
        } finally {
            editorTabSelectionAdjusting = false;
        }
        JPopupMenu menu = new JPopupMenu();
        JMenuItem closeOthers = new JMenuItem(tr("关闭其它", "Close Others"));
        closeOthers.addActionListener(e -> closeOtherEditorTabs(key));
        menu.add(closeOthers);
        menu.show(invoker, x, y);
    }

    private void closeOtherEditorTabs(String keepKey) {
        if (safe(keepKey).isBlank()) {
            return;
        }
        List<String> keys = new ArrayList<>(editorTabRefs.keySet());
        for (String key : keys) {
            if (Objects.equals(key, keepKey)) {
                continue;
            }
            closeEditorTab(key, false);
        }
        int keepIndex = indexOfTabKey(keepKey);
        if (keepIndex >= 0) {
            editorTabSelectionAdjusting = true;
            try {
                editorClassTabs.setSelectedIndex(keepIndex);
            } finally {
                editorTabSelectionAdjusting = false;
            }
            activateEditorTabAt(keepIndex, false);
        }
    }

    private void closeEditorTabAt(int index, boolean activateNeighbor) {
        String key = tabKeyAt(index);
        if (key.isBlank()) {
            return;
        }
        closeEditorTab(key, activateNeighbor);
    }

    private void closeEditorTab(String key, boolean activateNeighbor) {
        if (safe(key).isBlank()) {
            return;
        }
        if (editorClassTabs.getTabCount() <= 1) {
            return;
        }
        EditorTabRef ref = editorTabRefs.remove(key);
        if (ref == null) {
            return;
        }
        int index = editorClassTabs.indexOfComponent(ref.marker());
        if (index < 0) {
            if (Objects.equals(activeEditorTabKey, key)) {
                activeEditorTabKey = "";
            }
            return;
        }
        boolean closingActive = Objects.equals(activeEditorTabKey, key);
        editorTabSelectionAdjusting = true;
        try {
            editorClassTabs.removeTabAt(index);
        } finally {
            editorTabSelectionAdjusting = false;
        }
        if (editorClassTabs.getTabCount() <= 0) {
            activeEditorTabKey = "";
            return;
        }
        if (closingActive && activateNeighbor) {
            int nextIndex = Math.max(0, Math.min(index, editorClassTabs.getTabCount() - 1));
            editorTabSelectionAdjusting = true;
            try {
                editorClassTabs.setSelectedIndex(nextIndex);
            } finally {
                editorTabSelectionAdjusting = false;
            }
            activateEditorTabAt(nextIndex, true);
        }
    }

    private void activateEditorTabAt(int index, boolean forceOpen) {
        if (index < 0 || index >= editorClassTabs.getTabCount()) {
            return;
        }
        String key = tabKeyAt(index);
        if (key.isBlank()) {
            return;
        }
        EditorTabRef ref = editorTabRefs.get(key);
        if (ref == null || safe(ref.className()).isBlank()) {
            return;
        }
        boolean shouldOpen = forceOpen || !Objects.equals(activeEditorTabKey, key);
        activeEditorTabKey = key;
        if (shouldOpen) {
            RuntimeFacades.editor().openClass(ref.className(), ref.jarId());
            requestRefresh(false, true);
        }
    }

    private int indexOfTabKey(String key) {
        for (int i = 0; i < editorClassTabs.getTabCount(); i++) {
            if (Objects.equals(tabKeyAt(i), key)) {
                return i;
            }
        }
        return -1;
    }

    private String tabKeyAt(int index) {
        if (index < 0 || index >= editorClassTabs.getTabCount()) {
            return "";
        }
        Component marker = editorClassTabs.getComponentAt(index);
        if (!(marker instanceof JPanel panel)) {
            return "";
        }
        Object keyObj = panel.getClientProperty(EDITOR_TAB_KEY_PROP);
        return keyObj == null ? "" : safe(String.valueOf(keyObj));
    }

    private Component createEditorTabHeader(String key, String title) {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        header.setOpaque(false);
        JLabel label = new JLabel(safe(title));
        label.putClientProperty("editor.tab.header.label", Boolean.TRUE);
        JButton close = new JButton("x");
        close.setFocusable(false);
        close.setMargin(new Insets(0, 2, 0, 2));
        close.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        close.setContentAreaFilled(false);
        close.setOpaque(false);
        close.setRolloverEnabled(true);
        close.setToolTipText(tr("关闭标签", "Close Tab"));
        close.addActionListener(e -> closeEditorTab(key, true));
        MouseAdapter headerMouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleHeaderMouse(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleHeaderMouse(e);
            }

            private void handleHeaderMouse(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    closeEditorTab(key, true);
                    return;
                }
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    showEditorTabPopupByKey(key, e.getComponent(), e.getX(), e.getY());
                }
            }
        };
        header.addMouseListener(headerMouse);
        label.addMouseListener(headerMouse);
        header.add(label);
        header.add(close);
        return header;
    }

    private void updateEditorTabHeaderTitle(int index, String title) {
        if (index < 0 || index >= editorClassTabs.getTabCount()) {
            return;
        }
        Component header = editorClassTabs.getTabComponentAt(index);
        if (!(header instanceof JPanel panel)) {
            return;
        }
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JLabel label) {
                label.setText(safe(title));
                return;
            }
        }
    }

    private String editorTabKey(EditorDocumentDto doc) {
        String className = safe(doc == null ? null : doc.className()).trim();
        if (className.isBlank()) {
            return "";
        }
        Integer jarId = doc.jarId();
        int id = jarId == null ? 0 : jarId;
        return className + "|" + id;
    }

    private String editorTabTitle(String className) {
        String normalized = safe(className).replace('\\', '/');
        if (normalized.isBlank()) {
            return tr("代码", "Code");
        }
        int idx = normalized.lastIndexOf('/');
        return idx >= 0 ? normalized.substring(idx + 1) : normalized;
    }

    private String formatEditorLocation(EditorDocumentDto doc) {
        if (doc == null) {
            return "";
        }
        String className = safe(doc.className()).trim();
        String methodName = safe(doc.methodName()).trim();
        String methodDesc = safe(doc.methodDesc()).trim();
        String jarName = safe(doc.jarName()).trim();
        if (className.isBlank() && jarName.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (!className.isBlank()) {
            sb.append(className);
        }
        if (!methodName.isBlank()) {
            sb.append('#').append(methodName).append(methodDesc);
        }
        if (!jarName.isBlank()) {
            if (sb.length() > 0) {
                sb.append("  ");
            }
            sb.append('[').append(jarName).append(']');
        }
        return sb.toString();
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
        Set<String> expandedKeys = captureExpandedTreeKeys();
        String selectedKey = captureSelectedTreeKey();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("workspace");
        int nodeCount = 0;
        if (nodes != null) {
            for (TreeNodeDto node : nodes) {
                nodeCount += appendTreeNode(root, node);
            }
        }
        int classCount = countClassLeaves(root);
        treeModel.setRoot(root);
        treeModel.reload();
        if (nodeCount > 0) {
            treeCardLayout.show(treeCardPanel, "tree");
            restoreTreeVisualState(root, expandedKeys, selectedKey);
            treeStatusValue.setText(tr("类: ", "Classes: ") + classCount);
        } else {
            treeCardLayout.show(treeCardPanel, "empty");
            treeStatusValue.setText(tr("无项目", "No Project"));
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

    private int countClassLeaves(DefaultMutableTreeNode node) {
        if (node == null) {
            return 0;
        }
        int total = 0;
        Object user = node.getUserObject();
        if (user instanceof TreeNodeUi ui && !ui.directory() && safe(ui.value()).startsWith("cls:")) {
            total++;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            Object childObj = node.getChildAt(i);
            if (childObj instanceof DefaultMutableTreeNode child) {
                total += countClassLeaves(child);
            }
        }
        return total;
    }

    private int fingerprintTree(List<TreeNodeDto> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return 1;
        }
        int hash = 1;
        for (TreeNodeDto node : nodes) {
            hash = 31 * hash + fingerprintNode(node);
        }
        return hash;
    }

    private int fingerprintNode(TreeNodeDto node) {
        if (node == null) {
            return 0;
        }
        int hash = 1;
        hash = 31 * hash + safe(node.label()).hashCode();
        hash = 31 * hash + safe(node.value()).hashCode();
        hash = 31 * hash + Boolean.hashCode(node.directory());
        if (node.children() != null) {
            for (TreeNodeDto child : node.children()) {
                hash = 31 * hash + fingerprintNode(child);
            }
        }
        return hash;
    }

    private Set<String> captureExpandedTreeKeys() {
        Set<String> keys = new LinkedHashSet<>();
        Object rootObj = treeModel.getRoot();
        if (!(rootObj instanceof DefaultMutableTreeNode root)) {
            return keys;
        }
        TreePath rootPath = new TreePath(root.getPath());
        Enumeration<TreePath> expanded = projectTree.getExpandedDescendants(rootPath);
        if (expanded == null) {
            return keys;
        }
        while (expanded.hasMoreElements()) {
            TreePath path = expanded.nextElement();
            keys.add(treePathKey(path));
        }
        return keys;
    }

    private String captureSelectedTreeKey() {
        TreePath selected = projectTree.getSelectionPath();
        if (selected == null) {
            return "";
        }
        return treePathKey(selected);
    }

    private void restoreTreeVisualState(DefaultMutableTreeNode root, Set<String> expandedKeys, String selectedKey) {
        TreePath rootPath = new TreePath(root.getPath());
        if (expandedKeys == null || expandedKeys.isEmpty()) {
            projectTree.expandPath(rootPath);
        } else {
            restoreExpandedPaths(root, rootPath, expandedKeys);
        }
        if (selectedKey == null || selectedKey.isBlank()) {
            return;
        }
        TreePath selectedPath = findTreePathByKey(root, rootPath, selectedKey);
        if (selectedPath != null) {
            projectTree.setSelectionPath(selectedPath);
            projectTree.scrollPathToVisible(selectedPath);
        }
    }

    private void restoreExpandedPaths(DefaultMutableTreeNode node, TreePath path, Set<String> expandedKeys) {
        String key = treePathKey(path);
        if (expandedKeys.contains(key)) {
            projectTree.expandPath(path);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            Object childObj = node.getChildAt(i);
            if (!(childObj instanceof DefaultMutableTreeNode child)) {
                continue;
            }
            restoreExpandedPaths(child, path.pathByAddingChild(child), expandedKeys);
        }
    }

    private TreePath findTreePathByKey(DefaultMutableTreeNode node, TreePath path, String targetKey) {
        if (targetKey.equals(treePathKey(path))) {
            return path;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            Object childObj = node.getChildAt(i);
            if (!(childObj instanceof DefaultMutableTreeNode child)) {
                continue;
            }
            TreePath found = findTreePathByKey(child, path.pathByAddingChild(child), targetKey);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private String treePathKey(TreePath path) {
        if (path == null) {
            return "";
        }
        Object[] parts = path.getPath();
        StringBuilder sb = new StringBuilder();
        for (Object part : parts) {
            if (!(part instanceof DefaultMutableTreeNode node)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('/');
            }
            sb.append(treeNodeKey(node));
        }
        return sb.toString();
    }

    private String treeNodeKey(DefaultMutableTreeNode node) {
        Object user = node == null ? null : node.getUserObject();
        if (user instanceof TreeNodeUi ui) {
            String stable = safe(ui.value()).isBlank() ? safe(ui.label()) : safe(ui.value());
            return (ui.directory() ? "D:" : "F:") + stable;
        }
        return "N:" + safe(String.valueOf(user));
    }

    private Icon resolveTreeNodeIcon(TreeNodeUi ui) {
        if (ui == null) {
            return treeFolderIcon;
        }
        String value = safe(ui.value());
        if (value.startsWith("cat:input")) {
            return treeCategoryInputIcon;
        }
        if (value.startsWith("cat:source")) {
            return treeCategorySourceIcon;
        }
        if (value.startsWith("cat:resource")) {
            return treeCategoryResourceIcon;
        }
        if (value.startsWith("cat:dependency")) {
            return treeCategoryDependencyIcon;
        }
        if (value.startsWith("jar:")) {
            return treeCategoryDependencyIcon;
        }
        if (value.startsWith("srcjar:")) {
            return treeCategoryDependencyIcon;
        }
        if (value.startsWith("srcpkg:")) {
            return treePackageIcon;
        }
        if (value.startsWith("cls:")) {
            return treeClassIcon;
        }
        if (value.startsWith("input:")) {
            return treeCategoryInputIcon;
        }
        if (value.startsWith("res:") || value.startsWith("jarpath:")) {
            return treeFileIcon;
        }
        if (ui.directory()) {
            return treeFolderIcon;
        }
        return treeFileIcon;
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
        switch (action) {
            case EXPORT -> ToolWindowDialogs.showExportDialog(this, this::tr);
            case REMOTE_LOAD -> ToolWindowDialogs.showRemoteLoadDialog(this, this::tr, this::applyBuildInputFromRemoteLoad);
            case PROXY -> ToolWindowDialogs.showProxyDialog(this, this::tr);
            case PARTITION -> ToolWindowDialogs.showPartitionDialog(this, this::tr);
            case GLOBAL_SEARCH -> {
                focusToolTab(ToolTab.SEARCH);
                requestRefresh(false, true);
            }
            case SYSTEM_MONITOR -> ToolWindowDialogs.showSystemMonitorDialog(this, this::tr);
            case MARKDOWN_VIEWER -> {
                if (payload instanceof ToolingWindowPayload.MarkdownPayload markdown) {
                    showTextDialog(safe(markdown.title()), loadMarkdownText(markdown.markdownResource()));
                } else {
                    showTextDialog("Markdown", "No markdown payload.");
                }
            }
            case CFG, FRAME, OPCODE, ASM, BCEL_TOOL, ALL_STRINGS -> showAnalysisToolWindow(action, payload);
            case TEXT_VIEWER -> {
                if (payload instanceof ToolingWindowPayload.TextPayload text) {
                    showTextDialog(safe(text.title()), safe(text.content()));
                } else {
                    showTextDialog(
                            tr("工具窗口", "Tool Window"),
                            tr("当前动作需要文本载荷，但未提供内容: ", "Text payload required but missing: ") + action.name()
                    );
                }
            }
            case EL_SEARCH -> ToolWindowDialogs.showElSearchDialog(this, this::tr);
            case SQL_CONSOLE -> ToolWindowDialogs.showSqlConsoleDialog(this, this::tr);
            case ENCODE_TOOL -> ToolWindowDialogs.showEncodeToolDialog(this, this::tr);
            case SOCKET_LISTENER -> ToolWindowDialogs.showSocketListenerDialog(this, this::tr);
            case SERIALIZATION -> ToolWindowDialogs.showSerializationDialog(this, this::tr);
            case REPEATER -> ToolWindowDialogs.showHttpRepeaterDialog(this, this::tr);
            case OBFUSCATION -> {
                focusToolTab(ToolTab.ADVANCE);
                showTextDialog(tr("混淆分析", "Obfuscation"),
                        tr("已切换到 advance 面板，可继续使用混淆分析、字节码分析和相关工具。",
                                "Switched to advance panel for obfuscation and bytecode related tools."));
            }
            case SCA_INPUT_PICKER -> chooseScaInput(payload);
            case GADGET_DIR_PICKER -> chooseGadgetDir(payload);
            case CHAINS_RESULT -> showChainsResult(payload);
            case CHAINS_ADVANCED -> {
                setRightCollapsed(false);
                topTab = ToolTab.CHAINS;
                applyRightPaneState();
            }
            case EXTERNAL_TOOLS -> {
                String preferred = payload instanceof ToolingWindowPayload.PathPayload path ? safe(path.value()) : "";
                if (!preferred.isBlank()) {
                    RuntimeFacades.launchExternalTool(preferred);
                }
            }
        }
    }

    private void showAnalysisToolWindow(ToolingWindowAction action, ToolingWindowPayload payload) {
        if (payload instanceof ToolingWindowPayload.TextPayload text) {
            ToolWindowDialogs.showAnalysisTextDialog(this, this::tr, safe(text.title()), safe(text.content()));
            return;
        }
        showTextDialog(
                tr("工具窗口", "Tool Window"),
                tr("当前动作需要文本载荷，但未提供内容: ", "Text payload required but missing: ") + action.name()
        );
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

    private void applyBuildInputFromRemoteLoad(Path file) {
        if (file == null || !Files.exists(file)) {
            return;
        }
        BuildSnapshotDto snapshot = RuntimeFacades.build().snapshot();
        if (snapshot == null || snapshot.settings() == null) {
            return;
        }
        var old = snapshot.settings();
        RuntimeFacades.build().apply(new me.n1ar4.jar.analyzer.gui.runtime.model.BuildSettingsDto(
                file.toAbsolutePath().toString(),
                old.runtimePath(),
                old.resolveNestedJars(),
                old.autoFindRuntimeJar(),
                old.addRuntimeJar(),
                old.deleteTempBeforeBuild(),
                old.fixClassPath(),
                old.fixMethodImpl(),
                old.quickMode()
        ));
        suppressStartPageUntil = System.currentTimeMillis() + 3000;
        focusToolTab(ToolTab.START);
        closeStartPageTab();
        selectCodeTab();
        requestRefresh(true, true);
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

    private void showBuildLogDialog() {
        String text = buildLogBuffer.length() == 0
                ? tr("暂无日志", "No logs yet")
                : buildLogBuffer.toString();
        JDialog dialog = new JDialog(this, tr("构建日志", "Build Log"), false);
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(false);
        area.setWrapStyleWord(false);
        area.setText(text);
        area.setCaretPosition(area.getDocument().getLength());
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(new JScrollPane(area), BorderLayout.CENTER);
        dialog.setSize(new Dimension(920, 420));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void appendBuildLog(BuildSnapshotDto build) {
        if (build == null) {
            return;
        }
        String status = safe(build.statusText()).trim();
        int progress = Math.max(0, Math.min(100, build.buildProgress()));
        if (status.isBlank()) {
            return;
        }
        if (Objects.equals(lastBuildLogStatus, status) && lastBuildLogProgress == progress) {
            return;
        }
        lastBuildLogStatus = status;
        lastBuildLogProgress = progress;
        buildLogBuffer
                .append('[')
                .append(LocalDateTime.now().format(BUILD_LOG_TIME_FORMATTER))
                .append("] [")
                .append(progress)
                .append("%] ")
                .append(status)
                .append('\n');
        trimBuildLogBuffer();
    }

    private void trimBuildLogBuffer() {
        int overflow = buildLogBuffer.length() - BUILD_LOG_BUFFER_LIMIT;
        if (overflow <= 0) {
            return;
        }
        int cut = buildLogBuffer.indexOf("\n", overflow);
        if (cut < 0) {
            buildLogBuffer.setLength(0);
            return;
        }
        buildLogBuffer.delete(0, cut + 1);
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
        ToolingConfigSnapshotDto tooling = snapshotSafe(RuntimeFacades.tooling()::configSnapshot, null);

        JMenu fileMenu = new JMenu(tr("文件", "File"));
        JMenuItem refreshTree = new JMenuItem(tr("刷新项目", "Refresh Project"));
        refreshTree.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        refreshTree.addActionListener(e -> refreshTreeNow());
        JMenuItem exit = new JMenuItem(tr("退出", "Exit"));
        exit.addActionListener(e -> closeWithConfirm());
        fileMenu.add(refreshTree);
        fileMenu.add(exit);

        JMenu viewMenu = new JMenu(tr("视图", "View"));
        JCheckBoxMenuItem toggleProjectTree = new JCheckBoxMenuItem(tr("左侧栏", "Left Sidebar"), !leftCollapsed);
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

        JMenu navMenu = new JMenu(tr("导航", "Navigate"));
        navMenu.add(menuItem(tr("后退", "Back"), e -> {
            RuntimeFacades.editor().goPrev();
            requestRefresh(false, true);
        }));
        navMenu.add(menuItem(tr("前进", "Forward"), e -> {
            RuntimeFacades.editor().goNext();
            requestRefresh(false, true);
        }));
        navMenu.addSeparator();
        navMenu.add(menuItem(tr("收藏当前方法", "Add Current Method To Favorites"), e -> addCurrentMethodToFavorites()));

        JMenu toolsMenu = new JMenu(tr("工具", "Tools"));
        toolsMenu.add(menuItem(tr("导出", "Export"), e -> RuntimeFacades.tooling().openExportTool()));
        toolsMenu.add(menuItem(tr("全局搜索", "Global Search"), e -> RuntimeFacades.tooling().openGlobalSearchTool()));
        toolsMenu.add(menuItem(tr("系统监控", "System Monitor"), e -> RuntimeFacades.tooling().openSystemMonitorTool()));
        toolsMenu.add(menuItem(tr("字符串总览", "All Strings"), e -> RuntimeFacades.tooling().openAllStringsTool()));
        toolsMenu.add(menuItem(tr("EL 搜索", "EL Search"), e -> RuntimeFacades.tooling().openElSearchTool()));
        toolsMenu.add(menuItem(tr("分片配置", "Partition"), e -> RuntimeFacades.tooling().openPartitionTool()));
        toolsMenu.add(menuItem(tr("SQL 控制台", "SQL Console"), e -> RuntimeFacades.tooling().openSqlConsoleTool()));
        toolsMenu.add(menuItem(tr("编码工具", "Encode Tool"), e -> RuntimeFacades.tooling().openEncodeTool()));
        toolsMenu.add(menuItem(tr("端口监听", "Socket Listener"), e -> RuntimeFacades.tooling().openListenerTool()));
        toolsMenu.add(menuItem(tr("序列化工具", "Serialization"), e -> RuntimeFacades.tooling().openSerializationTool()));
        toolsMenu.add(menuItem(tr("HTTP Repeater", "HTTP Repeater"), e -> RuntimeFacades.tooling().openRepeaterTool()));
        toolsMenu.add(menuItem(tr("BCEL 工具", "BCEL Tool"), e -> RuntimeFacades.tooling().openBcelTool()));
        toolsMenu.addSeparator();
        toolsMenu.add(menuItem(tr("CFG 分析", "CFG Analyze"), e -> RuntimeFacades.tooling().openCfgTool()));
        toolsMenu.add(menuItem(tr("Frame 分析", "Frame Analyze"), e -> RuntimeFacades.tooling().openFrameTool(false)));
        toolsMenu.add(menuItem(tr("Full Frame 分析", "Full Frame Analyze"), e -> RuntimeFacades.tooling().openFrameTool(true)));
        toolsMenu.add(menuItem(tr("Opcode 查看", "Opcode Viewer"), e -> RuntimeFacades.tooling().openOpcodeTool()));
        toolsMenu.add(menuItem(tr("ASM 查看", "ASM Viewer"), e -> RuntimeFacades.tooling().openAsmTool()));
        toolsMenu.add(menuItem(tr("HTML 调用图", "HTML Graph"), e -> RuntimeFacades.tooling().openHtmlGraph()));
        toolsMenu.add(menuItem(tr("版本", "Version"), e -> RuntimeFacades.tooling().openVersionInfo()));

        JMenu pluginMenu = new JMenu(tr("插件", "Plugin"));
        pluginMenu.add(menuItem(tr("远程加载", "Remote Load"), e -> RuntimeFacades.tooling().openRemoteLoadTool()));
        pluginMenu.add(menuItem(tr("代理", "Proxy"), e -> RuntimeFacades.tooling().openProxyTool()));
        pluginMenu.add(menuItem(tr("混淆分析", "Obfuscation"), e -> RuntimeFacades.tooling().openObfuscationTool()));
        pluginMenu.add(menuItem(tr("远程 Tomcat 分析", "Remote Tomcat Analyzer"), e -> RuntimeFacades.tooling().openRemoteTomcatAnalyzer()));
        pluginMenu.add(menuItem(tr("字节码调试", "Bytecode Debugger"), e -> RuntimeFacades.tooling().openBytecodeDebugger()));
        pluginMenu.add(menuItem("JD-GUI", e -> RuntimeFacades.tooling().openJdGui()));

        JMenu settingsMenu = new JMenu(tr("设置", "Settings"));
        settingsMenu.add(createConfigMenu(tooling));
        settingsMenu.add(createLanguageMenu());
        settingsMenu.add(createThemeMenu(tooling));

        JMenu helpMenu = new JMenu(tr("帮助", "Help"));
        helpMenu.add(menuItem(tr("文档", "Docs"), e -> RuntimeFacades.tooling().openDocs()));
        helpMenu.add(menuItem(tr("项目主页", "Project Site"), e -> RuntimeFacades.tooling().openProjectSite()));
        helpMenu.add(menuItem(tr("报告问题", "Report Bug"), e -> RuntimeFacades.tooling().openReportBug()));

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(navMenu);
        menuBar.add(toolsMenu);
        menuBar.add(pluginMenu);
        menuBar.add(settingsMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private JMenu createConfigMenu(ToolingConfigSnapshotDto tooling) {
        JMenu configMenu = new JMenu(tr("配置", "Config"));

        JCheckBoxMenuItem showInnerItem = new JCheckBoxMenuItem(
                tr("显示内部类", "Show Inner Class"),
                tooling != null && tooling.showInnerClass()
        );
        showInnerItem.addActionListener(e -> {
            RuntimeFacades.tooling().toggleShowInnerClass();
            requestRefresh(true, true);
        });
        configMenu.add(showInnerItem);

        JCheckBoxMenuItem fixClassPathItem = new JCheckBoxMenuItem(
                tr("修复类路径", "Fix Class Path"),
                tooling != null && tooling.fixClassPath()
        );
        fixClassPathItem.addActionListener(e -> {
            RuntimeFacades.tooling().toggleFixClassPath();
            requestRefresh(false, true);
        });
        configMenu.add(fixClassPathItem);

        JRadioButtonMenuItem sortMethodItem = new JRadioButtonMenuItem(
                tr("按方法名排序", "Sort By Method"),
                tooling != null && tooling.sortByMethod()
        );
        JRadioButtonMenuItem sortClassItem = new JRadioButtonMenuItem(
                tr("按类名排序", "Sort By Class"),
                tooling == null || tooling.sortByClass()
        );
        ButtonGroup sortGroup = new ButtonGroup();
        sortGroup.add(sortMethodItem);
        sortGroup.add(sortClassItem);
        sortMethodItem.addActionListener(e -> {
            RuntimeFacades.tooling().setSortByMethod();
            requestRefresh(false, true);
        });
        sortClassItem.addActionListener(e -> {
            RuntimeFacades.tooling().setSortByClass();
            requestRefresh(false, true);
        });
        configMenu.add(sortMethodItem);
        configMenu.add(sortClassItem);

        JCheckBoxMenuItem logSqlItem = new JCheckBoxMenuItem(
                tr("保存全部 SQL", "Save All SQL"),
                tooling != null && tooling.logAllSql()
        );
        logSqlItem.addActionListener(e -> {
            RuntimeFacades.tooling().toggleLogAllSql();
            requestRefresh(false, true);
        });
        configMenu.add(logSqlItem);

        JCheckBoxMenuItem groupTreeItem = new JCheckBoxMenuItem(
                tr("文件树按 JAR 分组", "Group Tree By Jar"),
                tooling != null && tooling.groupTreeByJar()
        );
        groupTreeItem.addActionListener(e -> {
            RuntimeFacades.tooling().toggleGroupTreeByJar();
            requestRefresh(true, true);
        });
        configMenu.add(groupTreeItem);

        JCheckBoxMenuItem mergeRootItem = new JCheckBoxMenuItem(
                tr("包根合并", "Merge Package Root"),
                tooling != null && tooling.mergePackageRoot()
        );
        mergeRootItem.addActionListener(e -> {
            RuntimeFacades.tooling().toggleMergePackageRoot();
            requestRefresh(true, true);
        });
        configMenu.add(mergeRootItem);

        JCheckBoxMenuItem fixImplItem = new JCheckBoxMenuItem(
                tr("方法实现补全", "Fix Method Impl"),
                tooling != null && tooling.fixMethodImpl()
        );
        fixImplItem.addActionListener(e -> {
            RuntimeFacades.tooling().toggleFixMethodImpl();
            requestRefresh(false, true);
        });
        configMenu.add(fixImplItem);

        JCheckBoxMenuItem quickModeItem = new JCheckBoxMenuItem(
                tr("快速模式", "Quick Mode"),
                tooling != null && tooling.quickMode()
        );
        quickModeItem.addActionListener(e -> {
            RuntimeFacades.tooling().toggleQuickMode();
            requestRefresh(false, true);
        });
        configMenu.add(quickModeItem);

        JCheckBoxMenuItem stripeNamesItem = new JCheckBoxMenuItem(
                tr("侧栏显示名称", "Show Stripe Labels"),
                tooling != null && tooling.stripeShowNames()
        );
        stripeNamesItem.addActionListener(e -> {
            RuntimeFacades.tooling().setStripeShowNames(stripeNamesItem.isSelected());
            requestRefresh(false, true);
        });
        configMenu.add(stripeNamesItem);
        return configMenu;
    }

    private JMenu createLanguageMenu() {
        JMenu languageMenu = new JMenu(tr("语言", "Language"));
        JRadioButtonMenuItem zh = new JRadioButtonMenuItem(tr("中文", "Chinese"), !"en".equalsIgnoreCase(uiLanguage));
        JRadioButtonMenuItem en = new JRadioButtonMenuItem(tr("英文", "English"), "en".equalsIgnoreCase(uiLanguage));
        ButtonGroup group = new ButtonGroup();
        group.add(zh);
        group.add(en);
        zh.addActionListener(e -> {
            RuntimeFacades.tooling().setLanguageChinese();
            applyLanguage("zh");
            requestRefresh(false, true);
        });
        en.addActionListener(e -> {
            RuntimeFacades.tooling().setLanguageEnglish();
            applyLanguage("en");
            requestRefresh(false, true);
        });
        languageMenu.add(zh);
        languageMenu.add(en);
        return languageMenu;
    }

    private JMenu createThemeMenu(ToolingConfigSnapshotDto tooling) {
        String theme = normalizeTheme(tooling == null ? uiTheme : tooling.theme());
        JMenu themeMenu = new JMenu(tr("主题", "Theme"));
        JRadioButtonMenuItem defaultItem = new JRadioButtonMenuItem(tr("默认", "Default"), "default".equals(theme));
        JRadioButtonMenuItem darkItem = new JRadioButtonMenuItem(tr("深色", "Dark"), "dark".equals(theme));
        JRadioButtonMenuItem orangeItem = new JRadioButtonMenuItem(tr("橙色", "Orange"), "orange".equals(theme));
        ButtonGroup group = new ButtonGroup();
        group.add(defaultItem);
        group.add(darkItem);
        group.add(orangeItem);

        defaultItem.addActionListener(e -> {
            RuntimeFacades.tooling().useThemeDefault();
            applyTheme("default");
        });
        darkItem.addActionListener(e -> {
            RuntimeFacades.tooling().useThemeDark();
            applyTheme("dark");
        });
        orangeItem.addActionListener(e -> {
            RuntimeFacades.tooling().useThemeOrange();
            applyTheme("orange");
        });
        themeMenu.add(defaultItem);
        themeMenu.add(darkItem);
        themeMenu.add(orangeItem);
        return themeMenu;
    }

    private JMenuItem menuItem(String title, java.awt.event.ActionListener action) {
        JMenuItem item = new JMenuItem(title);
        item.addActionListener(action);
        return item;
    }

    private void applyLanguage(String language) {
        String normalized = normalizeLanguage(language);
        if (Objects.equals(uiLanguage, normalized) && localizationReady) {
            return;
        }
        uiLanguage = normalized;
        SwingI18n.setLanguage(uiLanguage);
        localizationReady = true;
        refreshLocalizedTexts();
        setJMenuBar(createMenuBar());
        revalidate();
        repaint();
    }

    private void applyTheme(String theme) {
        String normalized = normalizeTheme(theme);
        if (Objects.equals(uiTheme, normalized)) {
            return;
        }
        uiTheme = normalized;
        try {
            if ("dark".equals(normalized)) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
                if ("orange".equals(normalized)) {
                    Color accent = new Color(0xF39C3D);
                    UIManager.put("Component.focusColor", accent);
                    UIManager.put("ProgressBar.foreground", accent);
                    UIManager.put("Button.default.background", accent);
                    UIManager.put("Button.default.foreground", Color.WHITE);
                }
            }
            SwingUtilities.updateComponentTreeUI(this);
            updateSplitDraggableState();
            requestRootDividerLocationUpdate();
        } catch (Throwable ex) {
            logger.warn("apply theme failed: {}", ex.toString());
        }
    }

    private void refreshLocalizedTexts() {
        leftEmptyLabel.setText(tr("请打开文件", "Please open file"));
        leftTreeStripeButton.setText(tr("目录树", "Project"));
        leftStructureStripeButton.setText(tr("结构", "Structure"));
        leftTreeStripeButton.setToolTipText(tr("目录树", "Project"));
        leftStructureStripeButton.setToolTipText(tr("结构", "Structure"));
        if (treeRefreshButton != null) {
            treeRefreshButton.setToolTipText(tr("刷新", "Refresh"));
        }
        if (treeSearchButton != null) {
            treeSearchButton.setToolTipText(tr("搜索类名", "Search Class"));
        }
        if (topToggleMergePackageRoot != null) {
            topToggleMergePackageRoot.setToolTipText(tr("包根合并", "Merge Package Root"));
        }
        if (topToggleEditorTabs != null) {
            topToggleEditorTabs.setToolTipText(tr("代码标签栏", "Code Tabs"));
        }
        if (topToggleQuickMode != null) {
            topToggleQuickMode.setToolTipText(tr("快速模式", "Quick Mode"));
        }
        if (topToggleFixMethodImpl != null) {
            topToggleFixMethodImpl.setToolTipText(tr("修复方法实现", "Fix Method Impl"));
        }
        updateLogButtonStyle(stripeNamesVisible);
        buildLogButton.setToolTipText(tr("构建日志", "Build Log"));
        structureTitleLabel.setText(tr("结构", "Structure"));
        startSectionLabel.setText(tr("开始", "Start"));
        startOpenFileButton.setText(tr("打开文件", "Open File"));
        startOpenProjectButton.setText(tr("打开项目", "Open Project"));
        recentSectionLabel.setText(tr("最近项目", "Recent Projects"));
        if (startPageView != null) {
            int startIndex = workbenchTabs.indexOfComponent(startPageView);
            if (startIndex >= 0) {
                workbenchTabs.setTitleAt(startIndex, tr("开始页", "Start"));
            }
        }
        if (codePageView != null) {
            int codeIndex = workbenchTabs.indexOfComponent(codePageView);
            if (codeIndex >= 0) {
                workbenchTabs.setTitleAt(codeIndex, tr("代码", "Code"));
            }
        }
        startPanel.applyLanguage();
        searchPanel.applyLanguage();
        callPanel.applyLanguage();
        implPanel.applyLanguage();
        webPanel.applyLanguage();
        notePanel.applyLanguage();
        scaPanel.applyLanguage();
        leakPanel.applyLanguage();
        gadgetPanel.applyLanguage();
        advancePanel.applyLanguage();
        chainsPanel.applyLanguage();
        apiPanel.applyLanguage();
        applyRightPaneState();
    }

    private String tr(String zh, String en) {
        return "en".equalsIgnoreCase(uiLanguage) ? safe(en) : safe(zh);
    }

    private static String normalizeLanguage(String language) {
        return "en".equalsIgnoreCase(safe(language)) ? "en" : "zh";
    }

    private static String normalizeTheme(String theme) {
        String value = safe(theme).trim().toLowerCase();
        if ("dark".equals(value) || "orange".equals(value)) {
            return value;
        }
        return "default";
    }

    private void closeWithConfirm() {
        int resp = JOptionPane.showConfirmDialog(
                this,
                tr("确认退出？", "Confirm exit?"),
                tr("退出", "Exit"),
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

    private record EditorTabRef(
            String key,
            String className,
            Integer jarId,
            String jarName,
            Component marker
    ) {
    }

    private final class ProjectTreeRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(
                JTree tree,
                Object value,
                boolean sel,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus
        ) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode node) {
                Object user = node.getUserObject();
                if (user instanceof TreeNodeUi ui) {
                    setText(ui.label());
                    Icon icon = resolveTreeNodeIcon(ui);
                    if (icon != null) {
                        setLeafIcon(icon);
                        setClosedIcon(icon);
                        setOpenIcon(icon);
                        setIcon(icon);
                    }
                }
            }
            return this;
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
