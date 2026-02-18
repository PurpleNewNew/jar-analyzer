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
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchMatchMode;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchMode;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchQueryDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchResultDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchSnapshotDto;
import me.n1ar4.jar.analyzer.gui.swing.SwingI18n;
import me.n1ar4.jar.analyzer.gui.swing.SwingTextSync;
import me.n1ar4.jar.analyzer.gui.swing.SwingUiApplyGuard;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public final class SearchToolPanel extends JPanel {
    private final JRadioButton callMode = new JRadioButton("method call");
    private final JRadioButton definitionMode = new JRadioButton("method definition");
    private final JRadioButton stringMode = new JRadioButton("string contains");
    private final JRadioButton binaryMode = new JRadioButton("binary contains");

    private final JRadioButton likeMatch = new JRadioButton("like");
    private final JRadioButton equalsMatch = new JRadioButton("equals");

    private final JTextField classText = new JTextField();
    private final JTextField methodText = new JTextField();
    private final JTextField keywordText = new JTextField();
    private final JCheckBox nullParamFilter = new JCheckBox("exclude null-parameter method");

    private final DefaultListModel<SearchResultDto> resultModel = new DefaultListModel<>();
    private final JList<SearchResultDto> resultList = new JList<>(resultModel);
    private final JLabel statusValue = new JLabel(SwingI18n.tr("就绪", "ready"));
    private final JPopupMenu resultMenu = new JPopupMenu();
    private final JMenuItem setAsSourceItem = new JMenuItem();
    private final JMenuItem setAsSinkItem = new JMenuItem();
    private final SwingUiApplyGuard.Throttle snapshotThrottle = new SwingUiApplyGuard.Throttle();

    private volatile boolean syncing;

    public SearchToolPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        initUi();
    }

    private void initUi() {
        JPanel queryPanel = new JPanel(new BorderLayout(6, 6));
        queryPanel.setBorder(BorderFactory.createTitledBorder("Search Query"));

        JPanel modePanel = new JPanel(new GridLayout(0, 2, 4, 4));
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(callMode);
        modeGroup.add(definitionMode);
        modeGroup.add(stringMode);
        modeGroup.add(binaryMode);
        callMode.setSelected(true);
        modePanel.add(callMode);
        modePanel.add(definitionMode);
        modePanel.add(stringMode);
        modePanel.add(binaryMode);
        queryPanel.add(modePanel, BorderLayout.NORTH);

        JPanel fieldsPanel = new JPanel(new GridLayout(4, 2, 4, 4));
        fieldsPanel.add(new JLabel("class"));
        fieldsPanel.add(classText);
        fieldsPanel.add(new JLabel("method"));
        fieldsPanel.add(methodText);
        fieldsPanel.add(new JLabel("keyword"));
        fieldsPanel.add(keywordText);
        fieldsPanel.add(nullParamFilter);
        fieldsPanel.add(new JLabel(""));
        queryPanel.add(fieldsPanel, BorderLayout.CENTER);

        JPanel matchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        ButtonGroup matchGroup = new ButtonGroup();
        matchGroup.add(likeMatch);
        matchGroup.add(equalsMatch);
        likeMatch.setSelected(true);
        matchPanel.add(new JLabel("match"));
        matchPanel.add(likeMatch);
        matchPanel.add(equalsMatch);
        queryPanel.add(matchPanel, BorderLayout.SOUTH);

        JPanel resultPanel = new JPanel(new BorderLayout(4, 4));
        resultPanel.setBorder(BorderFactory.createTitledBorder("Results"));
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultList.setCellRenderer(new SearchResultRenderer());
        resultList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    openSelected();
                }
                maybeShowResultMenu(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowResultMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowResultMenu(e);
            }
        });
        setAsSourceItem.addActionListener(e -> setSelectedAsChainsPoint(true));
        setAsSinkItem.addActionListener(e -> setSelectedAsChainsPoint(false));
        resultMenu.add(setAsSourceItem);
        resultMenu.add(setAsSinkItem);
        resultPanel.add(new JScrollPane(resultList), BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton applyBtn = new JButton("Apply");
        applyBtn.addActionListener(e -> applyQueryOnly());
        JButton startBtn = new JButton("Start");
        startBtn.addActionListener(e -> {
            applyQueryOnly();
            RuntimeFacades.search().runSearch();
        });
        JButton openBtn = new JButton("Open");
        openBtn.addActionListener(e -> openSelected());
        actionPanel.add(applyBtn);
        actionPanel.add(startBtn);
        actionPanel.add(openBtn);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(new JLabel("status"), BorderLayout.WEST);
        statusPanel.add(statusValue, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        bottom.add(actionPanel, BorderLayout.NORTH);
        bottom.add(statusPanel, BorderLayout.SOUTH);

        callMode.addActionListener(e -> updateFieldEnablement());
        definitionMode.addActionListener(e -> updateFieldEnablement());
        stringMode.addActionListener(e -> updateFieldEnablement());
        binaryMode.addActionListener(e -> updateFieldEnablement());
        keywordText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    applyQueryOnly();
                    RuntimeFacades.search().runSearch();
                }
            }
        });
        methodText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    applyQueryOnly();
                    RuntimeFacades.search().runSearch();
                }
            }
        });
        classText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    applyQueryOnly();
                    RuntimeFacades.search().runSearch();
                }
            }
        });

        updateFieldEnablement();

        add(queryPanel, BorderLayout.NORTH);
        add(resultPanel, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        applyLanguage();
    }

    public void applySnapshot(SearchSnapshotDto snapshot) {
        if (snapshot == null) {
            return;
        }
        if (!SwingUiApplyGuard.ensureEdt("SearchToolPanel.applySnapshot", () -> applySnapshot(snapshot))) {
            return;
        }
        if (!snapshotThrottle.allow(SwingUiApplyGuard.fingerprint(snapshot))) {
            return;
        }
        SearchQueryDto query = snapshot.query();
        if (query != null) {
            syncing = true;
            try {
                selectMode(query.mode());
                selectMatchMode(query.matchMode());
                setTextIfIdle(classText, query.className());
                setTextIfIdle(methodText, query.methodName());
                setTextIfIdle(keywordText, query.keyword());
                nullParamFilter.setSelected(query.nullParamFilter());
                updateFieldEnablement();
            } finally {
                syncing = false;
            }
        }

        int selected = resultList.getSelectedIndex();
        resultModel.clear();
        List<SearchResultDto> results = snapshot.results();
        if (results != null) {
            for (SearchResultDto dto : results) {
                resultModel.addElement(dto);
            }
        }
        if (selected >= 0 && selected < resultModel.getSize()) {
            resultList.setSelectedIndex(selected);
        }
        statusValue.setText(safe(snapshot.statusText()));
    }

    private void applyQueryOnly() {
        if (syncing) {
            return;
        }
        RuntimeFacades.search().applyQuery(new SearchQueryDto(
                selectedMode(),
                selectedMatchMode(),
                safe(classText.getText()),
                safe(methodText.getText()),
                safe(keywordText.getText()),
                nullParamFilter.isSelected()
        ));
    }

    private void openSelected() {
        int index = resultList.getSelectedIndex();
        if (index >= 0) {
            RuntimeFacades.search().openResult(index);
        }
    }

    private void maybeShowResultMenu(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        int idx = resultList.locationToIndex(e.getPoint());
        if (idx < 0) {
            return;
        }
        resultList.setSelectedIndex(idx);
        SearchResultDto selected = resultList.getSelectedValue();
        boolean methodReady = selected != null && !safe(selected.methodName()).isBlank();
        setAsSourceItem.setEnabled(methodReady);
        setAsSinkItem.setEnabled(methodReady);
        resultMenu.show(resultList, e.getX(), e.getY());
    }

    private void setSelectedAsChainsPoint(boolean source) {
        SearchResultDto selected = resultList.getSelectedValue();
        if (selected == null || safe(selected.methodName()).isBlank()) {
            return;
        }
        if (source) {
            RuntimeFacades.chains().setSource(selected.className(), selected.methodName(), selected.methodDesc());
            statusValue.setText(SwingI18n.tr("已设为 source", "set as source"));
        } else {
            RuntimeFacades.chains().setSink(selected.className(), selected.methodName(), selected.methodDesc());
            statusValue.setText(SwingI18n.tr("已设为 sink", "set as sink"));
        }
    }

    private void selectMode(SearchMode mode) {
        SearchMode safeMode = mode == null ? SearchMode.METHOD_CALL : mode;
        switch (safeMode) {
            case METHOD_CALL -> callMode.setSelected(true);
            case METHOD_DEFINITION -> definitionMode.setSelected(true);
            case STRING_CONTAINS -> stringMode.setSelected(true);
            case BINARY_CONTAINS -> binaryMode.setSelected(true);
            case GLOBAL_CONTRIBUTOR, SQL_QUERY, CYPHER_QUERY -> callMode.setSelected(true);
        }
    }

    private SearchMode selectedMode() {
        if (definitionMode.isSelected()) {
            return SearchMode.METHOD_DEFINITION;
        }
        if (stringMode.isSelected()) {
            return SearchMode.STRING_CONTAINS;
        }
        if (binaryMode.isSelected()) {
            return SearchMode.BINARY_CONTAINS;
        }
        return SearchMode.METHOD_CALL;
    }

    private void selectMatchMode(SearchMatchMode mode) {
        if (mode == SearchMatchMode.EQUALS) {
            equalsMatch.setSelected(true);
        } else {
            likeMatch.setSelected(true);
        }
    }

    private SearchMatchMode selectedMatchMode() {
        return equalsMatch.isSelected() ? SearchMatchMode.EQUALS : SearchMatchMode.LIKE;
    }

    private void updateFieldEnablement() {
        SearchMode mode = selectedMode();
        boolean methodMode = mode == SearchMode.METHOD_CALL || mode == SearchMode.METHOD_DEFINITION;
        boolean stringContainsMode = mode == SearchMode.STRING_CONTAINS;
        boolean binaryContainsMode = mode == SearchMode.BINARY_CONTAINS;

        classText.setEnabled(methodMode || stringContainsMode);
        methodText.setEnabled(methodMode);
        keywordText.setEnabled(stringContainsMode || binaryContainsMode);
        nullParamFilter.setEnabled(methodMode || stringContainsMode);
        likeMatch.setEnabled(true);
        equalsMatch.setEnabled(true);
    }

    public void applyLanguage() {
        SwingI18n.localizeComponentTree(this);
        setAsSourceItem.setText(SwingI18n.tr("设为 Source", "Set As Source"));
        setAsSinkItem.setText(SwingI18n.tr("设为 Sink", "Set As Sink"));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static void setTextIfIdle(JTextField field, String value) {
        SwingTextSync.setTextIfIdle(field, value);
    }

    private static final class SearchResultRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
        ) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof SearchResultDto item) {
                if (safe(item.methodName()).isBlank()) {
                    String text = safe(item.className());
                    if (text.isBlank()) {
                        text = safe(item.preview());
                    }
                    setText(text);
                } else {
                    setText(item.className()
                            + "#" + item.methodName() + safe(item.methodDesc())
                            + " [" + safe(item.jarName()) + "]");
                }
            }
            return this;
        }
    }
}
