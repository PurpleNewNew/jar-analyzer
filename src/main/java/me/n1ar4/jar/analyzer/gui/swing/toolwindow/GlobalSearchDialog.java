/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.swing.toolwindow;

import me.n1ar4.jar.analyzer.core.SqlSessionFactoryUtil;
import me.n1ar4.jar.analyzer.gui.runtime.api.RuntimeFacades;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.apache.ibatis.session.SqlSession;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.lucene103.Lucene103Codec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GlobalSearchDialog extends JDialog {
    private static final Logger logger = LogManager.getLogger();
    private static final int MAX_RESULT = 300;
    private static final int PAGE_STEP = 8;
    private static final long serialVersionUID = 1L;
    private static volatile GlobalSearchDialog instance;

    private final ToolWindowDialogs.Translator translator;
    private final JTextField keywordField = new JTextField();
    private final Map<CategoryItem, JToggleButton> categoryTabs = new EnumMap<>(CategoryItem.class);
    private final ButtonGroup categoryTabGroup = new ButtonGroup();
    private final JButton searchButton = new JButton();
    private final JButton rebuildButton = new JButton();
    private final JButton openButton = new JButton();
    private final JLabel statusLabel = new JLabel();
    private final DefaultListModel<HitItem> resultModel = new DefaultListModel<>();
    private final JList<HitItem> resultList = new JList<>(resultModel);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile CategoryItem activeCategory = CategoryItem.ALL;
    private volatile SearchRun lastRun = new SearchRun(List.of(), "", "");
    private volatile String lastKeyword = "";

    public static void show(JFrame owner, ToolWindowDialogs.Translator translator) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> show(owner, translator));
            return;
        }
        if (instance == null || !instance.isDisplayable()) {
            instance = new GlobalSearchDialog(owner, translator);
        }
        instance.setVisible(true);
        instance.toFront();
        instance.requestSearchFocus();
    }

    private GlobalSearchDialog(JFrame owner, ToolWindowDialogs.Translator translator) {
        super(owner, "Global Search", false);
        this.translator = translator;
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        initUi();
        applyLanguage();
        setSize(960, 620);
        setLocationRelativeTo(owner);
    }

    private void initUi() {
        JPanel north = new JPanel(new BorderLayout(0, 6));
        JPanel tabsLine = new JPanel(new BorderLayout(6, 0));
        JPanel tabsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        initCategoryTabs(tabsPanel);
        tabsLine.add(tabsPanel, BorderLayout.WEST);
        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        topActions.add(rebuildButton);
        tabsLine.add(topActions, BorderLayout.EAST);

        JPanel queryLine = new JPanel(new BorderLayout(6, 0));
        queryLine.add(keywordField, BorderLayout.CENTER);
        queryLine.add(searchButton, BorderLayout.EAST);
        north.add(tabsLine, BorderLayout.NORTH);
        north.add(queryLine, BorderLayout.SOUTH);

        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultList.setCellRenderer(new ResultRenderer());
        resultList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    openSelected();
                }
            }
        });

        JPanel south = new JPanel(new BorderLayout(6, 0));
        JPanel action = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        action.add(openButton);
        south.add(action, BorderLayout.WEST);
        south.add(statusLabel, BorderLayout.CENTER);

        add(north, BorderLayout.NORTH);
        add(new JScrollPane(resultList), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> searchAsync(false));
        rebuildButton.addActionListener(e -> searchAsync(true));
        openButton.addActionListener(e -> openSelected());
        keywordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeywordKeyPressed(e);
            }
        });
        resultList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleResultListKeyPressed(e);
            }
        });
    }

    private void applyLanguage() {
        setTitle(tr("全局搜索", "Global Search"));
        searchButton.setText(tr("搜索", "Search"));
        rebuildButton.setText(tr("重建索引", "Rebuild Index"));
        openButton.setText(tr("打开", "Open"));
        statusLabel.setText(tr("就绪", "ready"));
        refreshTabPresentation();
    }

    private void requestSearchFocus() {
        SwingUtilities.invokeLater(() -> {
            keywordField.requestFocusInWindow();
            keywordField.selectAll();
        });
    }

    private void searchAsync(boolean forceRebuild) {
        String keyword = safe(keywordField.getText()).trim();
        if (keyword.isBlank()) {
            statusLabel.setText(tr("请输入关键字", "keyword is required"));
            return;
        }
        if (!running.compareAndSet(false, true)) {
            statusLabel.setText(tr("搜索进行中...", "search in progress..."));
            return;
        }
        searchButton.setEnabled(false);
        rebuildButton.setEnabled(false);
        openButton.setEnabled(false);
        statusLabel.setText(forceRebuild
                ? tr("重建索引并搜索中...", "rebuilding index and searching...")
                : tr("搜索中...", "searching..."));
        String queryKeyword = keyword;
        Thread.ofVirtual().name("gui-global-search").start(() -> {
            SearchRun run;
            try {
                run = GlobalSearchIndex.INSTANCE.search(queryKeyword, CategoryItem.ALL, MAX_RESULT, forceRebuild);
            } catch (Exception ex) {
                run = SearchRun.error("search error: " + safe(ex.getMessage()));
            }
            SearchRun finalRun = run;
            SwingUtilities.invokeLater(() -> {
                lastRun = finalRun;
                lastKeyword = queryKeyword;
                int visible = applyFilteredResults(finalRun);
                String summary = finalRun.error().isBlank()
                        ? buildResultSummary(visible, finalRun.hits().size())
                        : finalRun.error();
                if (!safe(finalRun.buildInfo()).isBlank()) {
                    summary = summary + " | " + finalRun.buildInfo();
                }
                statusLabel.setText(summary);
                searchButton.setEnabled(true);
                rebuildButton.setEnabled(true);
                openButton.setEnabled(true);
                running.set(false);
            });
        });
    }

    private void handleKeywordKeyPressed(KeyEvent e) {
        if (e == null) {
            return;
        }
        if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_RIGHT) {
            switchCategory(1);
            e.consume();
            return;
        }
        if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_LEFT) {
            switchCategory(-1);
            e.consume();
            return;
        }
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ENTER -> {
                String currentKeyword = safe(keywordField.getText()).trim();
                if (!currentKeyword.equals(lastKeyword)) {
                    searchAsync(false);
                } else if (selectedHit() != null) {
                    openSelected();
                } else {
                    searchAsync(false);
                }
                e.consume();
            }
            case KeyEvent.VK_DOWN -> {
                moveSelection(1);
                e.consume();
            }
            case KeyEvent.VK_UP -> {
                moveSelection(-1);
                e.consume();
            }
            case KeyEvent.VK_PAGE_DOWN -> {
                moveSelection(PAGE_STEP);
                e.consume();
            }
            case KeyEvent.VK_PAGE_UP -> {
                moveSelection(-PAGE_STEP);
                e.consume();
            }
            case KeyEvent.VK_ESCAPE -> {
                setVisible(false);
                e.consume();
            }
            default -> {
            }
        }
    }

    private void handleResultListKeyPressed(KeyEvent e) {
        if (e == null) {
            return;
        }
        if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_RIGHT) {
            switchCategory(1);
            e.consume();
            return;
        }
        if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_LEFT) {
            switchCategory(-1);
            e.consume();
            return;
        }
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ENTER -> {
                openSelected();
                e.consume();
            }
            case KeyEvent.VK_ESCAPE -> {
                setVisible(false);
                e.consume();
            }
            case KeyEvent.VK_UP -> {
                moveSelection(-1);
                e.consume();
            }
            case KeyEvent.VK_DOWN -> {
                moveSelection(1);
                e.consume();
            }
            case KeyEvent.VK_PAGE_DOWN -> {
                moveSelection(PAGE_STEP);
                e.consume();
            }
            case KeyEvent.VK_PAGE_UP -> {
                moveSelection(-PAGE_STEP);
                e.consume();
            }
            default -> {
                char ch = e.getKeyChar();
                if (!Character.isISOControl(ch) && !e.isAltDown() && !e.isControlDown() && !e.isMetaDown()) {
                    keywordField.requestFocusInWindow();
                    keywordField.setCaretPosition(keywordField.getText().length());
                }
            }
        }
    }

    private void moveSelection(int delta) {
        if (resultModel.isEmpty()) {
            return;
        }
        int current = resultList.getSelectedIndex();
        if (current < 0) {
            current = delta >= 0 ? 0 : resultModel.size() - 1;
        } else {
            current += delta;
        }
        int next = Math.max(0, Math.min(resultModel.size() - 1, current));
        resultList.setSelectedIndex(next);
        resultList.ensureIndexIsVisible(next);
    }

    private void switchCategory(int delta) {
        CategoryItem[] items = CategoryItem.values();
        int index = activeCategory == null ? 0 : activeCategory.ordinal();
        int target = index + (delta >= 0 ? 1 : -1);
        if (target < 0 || target >= items.length) {
            return;
        }
        CategoryItem next = items[target];
        JToggleButton button = categoryTabs.get(next);
        if (button != null) {
            button.setSelected(true);
        }
        onCategoryChanged(next);
    }

    private HitItem selectedHit() {
        return resultList.getSelectedValue();
    }

    private void openSelected() {
        HitItem item = selectedHit();
        if (item == null) {
            return;
        }
        String navigate = safe(item.navigateValue());
        if (!navigate.isBlank()) {
            RuntimeFacades.projectTree().openNode(navigate);
            statusLabel.setText(tr("已打开", "opened"));
            return;
        }
        if (!safe(item.methodName()).isBlank()) {
            RuntimeFacades.editor().openMethod(
                    item.className(),
                    item.methodName(),
                    item.methodDesc(),
                    item.jarId()
            );
            statusLabel.setText(tr("已打开", "opened"));
            return;
        }
        if (!safe(item.className()).isBlank()) {
            RuntimeFacades.editor().openClass(item.className(), item.jarId());
            statusLabel.setText(tr("已打开", "opened"));
            return;
        }
        statusLabel.setText(tr("当前结果无法跳转", "result has no navigation"));
    }

    private void initCategoryTabs(JPanel tabsPanel) {
        for (CategoryItem item : CategoryItem.values()) {
            JToggleButton button = new JToggleButton();
            button.setFocusable(false);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            button.addActionListener(e -> onCategoryChanged(item));
            categoryTabGroup.add(button);
            categoryTabs.put(item, button);
            tabsPanel.add(button);
        }
        JToggleButton all = categoryTabs.get(CategoryItem.ALL);
        if (all != null) {
            all.setSelected(true);
        }
        refreshTabPresentation();
    }

    private void onCategoryChanged(CategoryItem category) {
        if (category == null) {
            return;
        }
        activeCategory = category;
        refreshTabPresentation();
        int visible = applyFilteredResults(lastRun);
        if (safe(lastKeyword).isBlank()) {
            statusLabel.setText(tr("就绪", "ready"));
            return;
        }
        if (safe(lastRun.error()).isBlank()) {
            statusLabel.setText(buildResultSummary(visible, lastRun.hits().size()));
        } else {
            statusLabel.setText(lastRun.error());
        }
    }

    private int applyFilteredResults(SearchRun run) {
        resultModel.clear();
        List<HitItem> source = run == null || run.hits() == null ? List.of() : run.hits();
        for (HitItem hit : source) {
            if (hit == null) {
                continue;
            }
            if (activeCategory != CategoryItem.ALL
                    && !activeCategory.code.equals(normalizeKind(hit.kind()))) {
                continue;
            }
            resultModel.addElement(hit);
        }
        if (!resultModel.isEmpty()) {
            resultList.setSelectedIndex(0);
            resultList.ensureIndexIsVisible(0);
        }
        return resultModel.size();
    }

    private String buildResultSummary(int visible, int total) {
        if (activeCategory == CategoryItem.ALL) {
            return tr("结果", "results") + ": " + visible;
        }
        return tr("结果", "results") + ": " + visible + " / " + total;
    }

    private String normalizeKind(String kind) {
        String value = safe(kind).trim().toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            return "other";
        }
        return value;
    }

    private void refreshTabPresentation() {
        for (CategoryItem item : CategoryItem.values()) {
            JToggleButton button = categoryTabs.get(item);
            if (button == null) {
                continue;
            }
            button.setText(tabTitle(item));
            if (item == activeCategory) {
                button.setContentAreaFilled(true);
                button.setOpaque(true);
                button.setBackground(new java.awt.Color(0xDCE8FF));
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new java.awt.Color(0x8AA6E0)),
                        BorderFactory.createEmptyBorder(4, 10, 4, 10)
                ));
            } else {
                button.setContentAreaFilled(false);
                button.setOpaque(false);
                button.setBorder(BorderFactory.createEmptyBorder(5, 11, 5, 11));
            }
        }
    }

    private String tabTitle(CategoryItem item) {
        if (item == null) {
            return "";
        }
        return switch (item) {
            case ALL -> tr("所有", "All");
            case CLASS -> tr("类", "Class");
            case METHOD -> tr("方法", "Method");
            case STRING -> tr("文本", "Text");
            case RESOURCE -> tr("文件", "File");
            case CALL -> tr("操作", "Action");
            case GRAPH -> tr("符号", "Symbol");
        };
    }

    private String tr(String zh, String en) {
        if (translator == null) {
            return safe(zh);
        }
        return safe(translator.tr(zh, en));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static int parseInt(String value, int def) {
        try {
            return Integer.parseInt(safe(value).trim());
        } catch (Exception ignored) {
            return def;
        }
    }

    private static String trimText(String value, int maxLen) {
        String text = safe(value);
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLen)) + "...";
    }

    private static String normalizeSearchText(String value) {
        String text = safe(value)
                .replace('\n', ' ')
                .replace('\r', ' ')
                .toLowerCase(Locale.ROOT)
                .trim();
        if (text.length() > 2048) {
            text = text.substring(0, 2048);
        }
        return text;
    }

    private final class ResultRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
        ) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (!(value instanceof HitItem item)) {
                return this;
            }
            String kind = safe(item.kind()).toUpperCase(Locale.ROOT);
            String text = safe(item.preview());
            if (text.isBlank()) {
                if (safe(item.methodName()).isBlank()) {
                    text = safe(item.className());
                } else {
                    text = item.className() + "#" + item.methodName() + safe(item.methodDesc());
                }
            }
            setText("[" + kind + "] " + text);
            return this;
        }
    }

    private enum CategoryItem {
        ALL("all"),
        CLASS("class"),
        METHOD("method"),
        STRING("string"),
        RESOURCE("resource"),
        CALL("call"),
        GRAPH("graph");

        private final String code;

        CategoryItem(String code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return switch (this) {
                case ALL -> "All";
                case CLASS -> "Class";
                case METHOD -> "Method";
                case STRING -> "String";
                case RESOURCE -> "Resource";
                case CALL -> "Call";
                case GRAPH -> "Graph";
            };
        }
    }

    private record HitItem(
            String kind,
            String className,
            String methodName,
            String methodDesc,
            String jarName,
            int jarId,
            String preview,
            String navigateValue
    ) {
    }

    private record SearchRun(List<HitItem> hits, String buildInfo, String error) {
        private static SearchRun error(String message) {
            return new SearchRun(List.of(), "", safe(message));
        }
    }

    private static final class GlobalSearchIndex {
        private static final GlobalSearchIndex INSTANCE = new GlobalSearchIndex();
        private static final int BATCH_COMMIT_STEP = 10_000;
        private final Path indexPath = Paths.get(Const.dbDir, "global-search-index");
        private volatile long indexedBuildSeq = Long.MIN_VALUE;
        private volatile long indexedDbMtime = Long.MIN_VALUE;
        private volatile String buildInfo = "";
        private Directory directory;
        private DirectoryReader reader;
        private IndexSearcher searcher;

        private synchronized SearchRun search(String keyword,
                                              CategoryItem category,
                                              int limit,
                                              boolean forceRebuild) throws Exception {
            ensureReady(forceRebuild);
            Query query = buildQuery(keyword, category);
            if (query instanceof MatchNoDocsQuery || searcher == null || reader == null) {
                return new SearchRun(List.of(), buildInfo, "");
            }
            int top = Math.max(1, Math.min(limit, 1000));
            TopDocs docs = searcher.search(query, top);
            StoredFields storedFields = reader.storedFields();
            List<HitItem> hits = new ArrayList<>();
            for (ScoreDoc scoreDoc : docs.scoreDocs) {
                Document doc = storedFields.document(scoreDoc.doc);
                hits.add(new HitItem(
                        safe(doc.get("kind")),
                        safe(doc.get("class_name")),
                        safe(doc.get("method_name")),
                        safe(doc.get("method_desc")),
                        safe(doc.get("jar_name")),
                        parseInt(doc.get("jar_id"), 0),
                        safe(doc.get("preview")),
                        safe(doc.get("navigate"))
                ));
            }
            return new SearchRun(hits, buildInfo, "");
        }

        private void ensureReady(boolean forceRebuild) throws Exception {
            Fingerprint fp = readFingerprint();
            boolean useBuildSeq = fp.buildSeq() > 0L;
            boolean needsBuild = forceRebuild
                    || searcher == null
                    || (useBuildSeq && fp.buildSeq() != indexedBuildSeq)
                    || (!useBuildSeq && fp.dbMtime() != indexedDbMtime);
            if (needsBuild) {
                rebuild(fp);
            } else {
                refreshSearcher();
            }
        }

        private void rebuild(Fingerprint fp) throws Exception {
            closeReader();
            Files.createDirectories(indexPath);
            if (directory == null) {
                directory = FSDirectory.open(indexPath);
            }
            IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            config.setCodec(new Lucene103Codec(Lucene103Codec.Mode.BEST_COMPRESSION));
            int total = 0;
            try (IndexWriter writer = new IndexWriter(directory, config);
                 SqlSession session = SqlSessionFactoryUtil.sqlSessionFactory.openSession(true)) {
                Connection connection = session.getConnection();
                Map<Integer, String> jarNames = loadJarNames(connection);
                total += indexClassTable(connection, writer, jarNames);
                total += indexMethodTable(connection, writer, jarNames);
                total += indexStringTable(connection, writer, jarNames);
                total += indexResourceTable(connection, writer, jarNames);
                total += indexMethodCallTable(connection, writer, jarNames);
                total += indexGraphNodeTable(connection, writer, jarNames);
                writer.commit();
            }
            indexedBuildSeq = fp.buildSeq();
            indexedDbMtime = fp.dbMtime();
            buildInfo = "build_seq=" + indexedBuildSeq + ", docs=" + total;
            refreshSearcher();
        }

        private int indexClassTable(Connection connection,
                                    IndexWriter writer,
                                    Map<Integer, String> jarNames) {
            String sql = "SELECT class_name, jar_id FROM class_table";
            int count = 0;
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String className = safe(rs.getString("class_name"));
                    int jarId = rs.getInt("jar_id");
                    String jarName = safe(jarNames.get(jarId));
                    String preview = className + (jarName.isBlank() ? "" : " [" + jarName + "]");
                    String navigate = "cls:" + className + "|" + jarId;
                    addDoc(writer, "class", className, "", "", jarId, jarName, preview, navigate, preview);
                    count++;
                    maybeCommit(writer, count);
                }
            } catch (Exception ex) {
                logger.debug("index class_table failed: {}", ex.toString());
            }
            return count;
        }

        private int indexMethodTable(Connection connection,
                                     IndexWriter writer,
                                     Map<Integer, String> jarNames) {
            String sql = "SELECT class_name, method_name, method_desc, jar_id FROM method_table";
            int count = 0;
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String className = safe(rs.getString("class_name"));
                    String methodName = safe(rs.getString("method_name"));
                    String methodDesc = safe(rs.getString("method_desc"));
                    int jarId = rs.getInt("jar_id");
                    String jarName = safe(jarNames.get(jarId));
                    String preview = className + "#" + methodName + methodDesc;
                    String navigate = "cls:" + className + "|" + jarId;
                    String searchable = preview + " " + jarName;
                    addDoc(writer, "method", className, methodName, methodDesc, jarId, jarName,
                            preview, navigate, searchable);
                    count++;
                    maybeCommit(writer, count);
                }
            } catch (Exception ex) {
                logger.debug("index method_table failed: {}", ex.toString());
            }
            return count;
        }

        private int indexStringTable(Connection connection,
                                     IndexWriter writer,
                                     Map<Integer, String> jarNames) {
            String sql = "SELECT class_name, method_name, method_desc, value, jar_id, jar_name FROM string_table";
            int count = 0;
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String className = safe(rs.getString("class_name"));
                    String methodName = safe(rs.getString("method_name"));
                    String methodDesc = safe(rs.getString("method_desc"));
                    String value = safe(rs.getString("value"));
                    int jarId = rs.getInt("jar_id");
                    String jarName = safe(rs.getString("jar_name"));
                    if (jarName.isBlank()) {
                        jarName = safe(jarNames.get(jarId));
                    }
                    String preview = className + "#" + methodName + methodDesc + " :: " + trimText(value, 220);
                    String navigate = "cls:" + className + "|" + jarId;
                    String searchable = preview + " " + value;
                    addDoc(writer, "string", className, methodName, methodDesc, jarId, jarName,
                            preview, navigate, searchable);
                    count++;
                    maybeCommit(writer, count);
                }
            } catch (Exception ex) {
                logger.debug("index string_table failed: {}", ex.toString());
            }
            return count;
        }

        private int indexResourceTable(Connection connection,
                                       IndexWriter writer,
                                       Map<Integer, String> jarNames) {
            String sql = "SELECT rid, resource_path, jar_id, jar_name, file_size FROM resource_table";
            int count = 0;
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    int rid = rs.getInt("rid");
                    String path = safe(rs.getString("resource_path"));
                    int jarId = rs.getInt("jar_id");
                    String jarName = safe(rs.getString("jar_name"));
                    if (jarName.isBlank()) {
                        jarName = safe(jarNames.get(jarId));
                    }
                    long fileSize = rs.getLong("file_size");
                    String preview = path + " (" + fileSize + " bytes)";
                    String navigate = "res:" + rid;
                    String searchable = path + " " + jarName;
                    addDoc(writer, "resource", path, "", "", jarId, jarName, preview, navigate, searchable);
                    count++;
                    maybeCommit(writer, count);
                }
            } catch (Exception ex) {
                logger.debug("index resource_table failed: {}", ex.toString());
            }
            return count;
        }

        private int indexMethodCallTable(Connection connection,
                                         IndexWriter writer,
                                         Map<Integer, String> jarNames) {
            String sql = "SELECT caller_class_name, caller_method_name, caller_method_desc, caller_jar_id, " +
                    "callee_class_name, callee_method_name, callee_method_desc, edge_type, edge_confidence " +
                    "FROM method_call_table";
            int count = 0;
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String callerClass = safe(rs.getString("caller_class_name"));
                    String callerMethod = safe(rs.getString("caller_method_name"));
                    String callerDesc = safe(rs.getString("caller_method_desc"));
                    int callerJarId = rs.getInt("caller_jar_id");
                    String calleeClass = safe(rs.getString("callee_class_name"));
                    String calleeMethod = safe(rs.getString("callee_method_name"));
                    String calleeDesc = safe(rs.getString("callee_method_desc"));
                    String edgeType = safe(rs.getString("edge_type"));
                    String confidence = safe(rs.getString("edge_confidence"));
                    String jarName = safe(jarNames.get(callerJarId));
                    String preview = callerClass + "#" + callerMethod + callerDesc
                            + " -> " + calleeClass + "#" + calleeMethod + calleeDesc
                            + " [" + edgeType + "/" + confidence + "]";
                    String searchable = preview + " " + jarName;
                    String navigate = "cls:" + callerClass + "|" + callerJarId;
                    addDoc(writer, "call", callerClass, callerMethod, callerDesc,
                            callerJarId, jarName, preview, navigate, searchable);
                    count++;
                    maybeCommit(writer, count);
                }
            } catch (Exception ex) {
                logger.debug("index method_call_table failed: {}", ex.toString());
            }
            return count;
        }

        private int indexGraphNodeTable(Connection connection,
                                        IndexWriter writer,
                                        Map<Integer, String> jarNames) {
            String sql = "SELECT node_id, kind, class_name, method_name, method_desc, jar_id FROM graph_node";
            int count = 0;
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    long nodeId = rs.getLong("node_id");
                    String kind = safe(rs.getString("kind"));
                    String className = safe(rs.getString("class_name"));
                    String methodName = safe(rs.getString("method_name"));
                    String methodDesc = safe(rs.getString("method_desc"));
                    int jarId = rs.getInt("jar_id");
                    String jarName = safe(jarNames.get(jarId));
                    String signature = className + (methodName.isBlank() ? "" : "#" + methodName + methodDesc);
                    String preview = "node#" + nodeId + " " + kind + " " + signature;
                    String navigate = className.isBlank() ? "" : "cls:" + className + "|" + jarId;
                    String searchable = preview + " " + jarName;
                    addDoc(writer, "graph", className, methodName, methodDesc,
                            jarId, jarName, preview, navigate, searchable);
                    count++;
                    maybeCommit(writer, count);
                }
            } catch (Exception ex) {
                logger.debug("index graph_node failed: {}", ex.toString());
            }
            return count;
        }

        private void addDoc(IndexWriter writer,
                            String kind,
                            String className,
                            String methodName,
                            String methodDesc,
                            int jarId,
                            String jarName,
                            String preview,
                            String navigate,
                            String searchable) throws Exception {
            Document doc = new Document();
            doc.add(new StringField("kind", safe(kind), Field.Store.YES));
            doc.add(new StringField("class_name", safe(className), Field.Store.YES));
            doc.add(new StringField("method_name", safe(methodName), Field.Store.YES));
            doc.add(new StringField("method_desc", safe(methodDesc), Field.Store.YES));
            doc.add(new StringField("jar_name", safe(jarName), Field.Store.YES));
            doc.add(new StringField("jar_id", String.valueOf(jarId), Field.Store.YES));
            doc.add(new StringField("navigate", safe(navigate), Field.Store.YES));
            doc.add(new StringField("preview", trimText(safe(preview), 400), Field.Store.YES));
            String searchableLower = normalizeSearchText(searchable + " " + className + " " + methodName + " " + methodDesc);
            doc.add(new StringField("content_lower", searchableLower, Field.Store.NO));
            writer.addDocument(doc);
        }

        private void maybeCommit(IndexWriter writer, int count) throws Exception {
            if (count > 0 && count % BATCH_COMMIT_STEP == 0) {
                writer.commit();
            }
        }

        private void refreshSearcher() throws Exception {
            if (directory == null) {
                directory = FSDirectory.open(indexPath);
            }
            if (reader == null) {
                reader = DirectoryReader.open(directory);
                searcher = new IndexSearcher(reader);
                return;
            }
            DirectoryReader next = DirectoryReader.openIfChanged(reader);
            if (next != null) {
                DirectoryReader old = reader;
                reader = next;
                searcher = new IndexSearcher(reader);
                old.close();
            }
        }

        private void closeReader() {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception ex) {
                logger.debug("close lucene reader failed: {}", ex.toString());
            }
            reader = null;
            searcher = null;
        }

        private Query buildQuery(String keyword, CategoryItem category) {
            List<String> tokens = splitTokens(keyword);
            if (tokens.isEmpty()) {
                return new MatchNoDocsQuery();
            }
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            if (category != null && category != CategoryItem.ALL) {
                builder.add(new TermQuery(new Term("kind", category.code)), BooleanClause.Occur.MUST);
            }
            for (String token : tokens) {
                String escaped = escapeWildcard(token);
                if (escaped.isBlank()) {
                    continue;
                }
                builder.add(new WildcardQuery(new Term("content_lower", "*" + escaped + "*")),
                        BooleanClause.Occur.MUST);
            }
            BooleanQuery query = builder.build();
            if (query.clauses().isEmpty()) {
                return new MatchNoDocsQuery();
            }
            return query;
        }

        private List<String> splitTokens(String keyword) {
            String lower = safe(keyword).toLowerCase(Locale.ROOT).trim();
            if (lower.isBlank()) {
                return List.of();
            }
            String[] parts = lower.split("\\s+");
            List<String> out = new ArrayList<>();
            for (String part : parts) {
                String token = safe(part).trim();
                if (!token.isBlank()) {
                    out.add(token);
                }
                if (out.size() >= 6) {
                    break;
                }
            }
            return out;
        }

        private String escapeWildcard(String value) {
            StringBuilder sb = new StringBuilder();
            for (char ch : safe(value).toCharArray()) {
                if (ch == '\\' || ch == '*' || ch == '?') {
                    sb.append('\\');
                }
                sb.append(ch);
            }
            return sb.toString();
        }

        private Map<Integer, String> loadJarNames(Connection connection) {
            Map<Integer, String> out = new HashMap<>();
            String sql = "SELECT jid, jar_name FROM jar_table";
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getInt("jid"), safe(rs.getString("jar_name")));
                }
            } catch (Exception ex) {
                logger.debug("load jar names failed: {}", ex.toString());
            }
            return out;
        }

        private Fingerprint readFingerprint() {
            long buildSeq = 0L;
            if (SqlSessionFactoryUtil.sqlSessionFactory != null) {
                try (SqlSession session = SqlSessionFactoryUtil.sqlSessionFactory.openSession(true)) {
                    Connection connection = session.getConnection();
                    buildSeq = Math.max(buildSeq, queryBuildSeq(connection, "project_model_meta"));
                    buildSeq = Math.max(buildSeq, queryBuildSeq(connection, "graph_meta"));
                } catch (Exception ex) {
                    logger.debug("read build_seq failed: {}", ex.toString());
                }
            }
            long dbMtime = 0L;
            try {
                Path db = Paths.get(Const.dbFile).toAbsolutePath();
                if (Files.exists(db)) {
                    dbMtime = Files.getLastModifiedTime(db).toMillis();
                }
            } catch (Exception ex) {
                logger.debug("read db mtime failed: {}", ex.toString());
            }
            return new Fingerprint(buildSeq, dbMtime);
        }

        private long queryBuildSeq(Connection connection, String table) {
            String sql = "SELECT COALESCE(MAX(build_seq), 0) AS build_seq FROM " + table;
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("build_seq");
                }
            } catch (Exception ignored) {
                return 0L;
            }
            return 0L;
        }
    }

    private record Fingerprint(long buildSeq, long dbMtime) {
    }
}
