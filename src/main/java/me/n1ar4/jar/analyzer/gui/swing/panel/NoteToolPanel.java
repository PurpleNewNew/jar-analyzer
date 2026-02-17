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
import me.n1ar4.jar.analyzer.gui.runtime.model.MethodNavDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.NoteSnapshotDto;
import me.n1ar4.jar.analyzer.gui.swing.SwingI18n;
import me.n1ar4.jar.analyzer.gui.swing.SwingUiApplyGuard;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public final class NoteToolPanel extends JPanel {
    private final DefaultListModel<MethodNavDto> historyModel = new DefaultListModel<>();
    private final DefaultListModel<MethodNavDto> favoriteModel = new DefaultListModel<>();
    private final JList<MethodNavDto> historyList = new JList<>(historyModel);
    private final JList<MethodNavDto> favoriteList = new JList<>(favoriteModel);
    private final JLabel statusValue = new JLabel(SwingI18n.tr("就绪", "ready"));
    private final JPopupMenu historyMenu = new JPopupMenu();
    private final JPopupMenu favoriteMenu = new JPopupMenu();
    private final JMenuItem historySetSourceItem = new JMenuItem();
    private final JMenuItem historySetSinkItem = new JMenuItem();
    private final JMenuItem favoriteSetSourceItem = new JMenuItem();
    private final JMenuItem favoriteSetSinkItem = new JMenuItem();
    private final SwingUiApplyGuard.Throttle snapshotThrottle = new SwingUiApplyGuard.Throttle();
    private boolean hasSnapshot;

    public NoteToolPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        initUi();
    }

    private void initUi() {
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        favoriteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setCellRenderer(new MethodRenderer());
        favoriteList.setCellRenderer(new MethodRenderer());

        historyList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    int index = historyList.getSelectedIndex();
                    if (index >= 0) {
                        RuntimeFacades.note().openHistory(index);
                    }
                }
                maybeShowHistoryMenu(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowHistoryMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowHistoryMenu(e);
            }
        });
        favoriteList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    int index = favoriteList.getSelectedIndex();
                    if (index >= 0) {
                        RuntimeFacades.note().openFavorite(index);
                    }
                }
                maybeShowFavoriteMenu(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowFavoriteMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowFavoriteMenu(e);
            }
        });

        historySetSourceItem.addActionListener(e -> setHistoryAsChainsPoint(true));
        historySetSinkItem.addActionListener(e -> setHistoryAsChainsPoint(false));
        favoriteSetSourceItem.addActionListener(e -> setFavoriteAsChainsPoint(true));
        favoriteSetSinkItem.addActionListener(e -> setFavoriteAsChainsPoint(false));
        historyMenu.add(historySetSourceItem);
        historyMenu.add(historySetSinkItem);
        favoriteMenu.add(favoriteSetSourceItem);
        favoriteMenu.add(favoriteSetSinkItem);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("history", new JScrollPane(historyList));
        tabs.addTab("favorites", new JScrollPane(favoriteList));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton loadBtn = new JButton("Load");
        loadBtn.addActionListener(e -> RuntimeFacades.note().load());
        JButton clearHisBtn = new JButton("Clear History");
        clearHisBtn.addActionListener(e -> RuntimeFacades.note().clearHistory());
        JButton clearFavBtn = new JButton("Clear Fav");
        clearFavBtn.addActionListener(e -> RuntimeFacades.note().clearFavorites());
        JButton openHisBtn = new JButton("Open History");
        openHisBtn.addActionListener(e -> {
            int index = historyList.getSelectedIndex();
            if (index >= 0) {
                RuntimeFacades.note().openHistory(index);
            }
        });
        JButton openFavBtn = new JButton("Open Fav");
        openFavBtn.addActionListener(e -> {
            int index = favoriteList.getSelectedIndex();
            if (index >= 0) {
                RuntimeFacades.note().openFavorite(index);
            }
        });
        actions.add(loadBtn);
        actions.add(clearHisBtn);
        actions.add(clearFavBtn);
        actions.add(openHisBtn);
        actions.add(openFavBtn);

        JPanel status = new JPanel(new BorderLayout());
        status.add(new JLabel("status"), BorderLayout.WEST);
        status.add(statusValue, BorderLayout.CENTER);

        add(tabs, BorderLayout.CENTER);
        add(actions, BorderLayout.NORTH);
        add(status, BorderLayout.SOUTH);
        applyLanguage();
    }

    public void applySnapshot(NoteSnapshotDto snapshot) {
        if (snapshot == null) {
            return;
        }
        if (!SwingUiApplyGuard.ensureEdt("NoteToolPanel.applySnapshot", () -> applySnapshot(snapshot))) {
            return;
        }
        if (!snapshotThrottle.allow(SwingUiApplyGuard.fingerprint(snapshot))) {
            return;
        }
        resetModel(historyModel, historyList, snapshot.history());
        resetModel(favoriteModel, favoriteList, snapshot.favorites());
        hasSnapshot = true;
        updateStatusText();
    }

    private static void resetModel(
            DefaultListModel<MethodNavDto> model,
            JList<MethodNavDto> list,
            List<MethodNavDto> values
    ) {
        int selected = list.getSelectedIndex();
        model.clear();
        if (values != null) {
            for (MethodNavDto item : values) {
                model.addElement(item);
            }
        }
        if (selected >= 0 && selected < model.getSize()) {
            list.setSelectedIndex(selected);
        }
    }

    public void applyLanguage() {
        SwingI18n.localizeComponentTree(this);
        historySetSourceItem.setText(SwingI18n.tr("设为 Source", "Set As Source"));
        historySetSinkItem.setText(SwingI18n.tr("设为 Sink", "Set As Sink"));
        favoriteSetSourceItem.setText(SwingI18n.tr("设为 Source", "Set As Source"));
        favoriteSetSinkItem.setText(SwingI18n.tr("设为 Sink", "Set As Sink"));
        if (hasSnapshot) {
            updateStatusText();
        } else {
            statusValue.setText(SwingI18n.tr("就绪", "ready"));
        }
    }

    private void updateStatusText() {
        statusValue.setText(SwingI18n.tr("历史=", "history=") + historyModel.size()
                + ", " + SwingI18n.tr("收藏=", "favorite=") + favoriteModel.size());
    }

    private static final class MethodRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
        ) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof MethodNavDto item) {
                setText(item.className() + "#" + item.methodName() + item.methodDesc() + " [" + item.jarName() + "]");
            }
            return this;
        }
    }

    private void maybeShowHistoryMenu(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        int idx = historyList.locationToIndex(e.getPoint());
        if (idx < 0) {
            return;
        }
        historyList.setSelectedIndex(idx);
        historyMenu.show(historyList, e.getX(), e.getY());
    }

    private void maybeShowFavoriteMenu(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        int idx = favoriteList.locationToIndex(e.getPoint());
        if (idx < 0) {
            return;
        }
        favoriteList.setSelectedIndex(idx);
        favoriteMenu.show(favoriteList, e.getX(), e.getY());
    }

    private void setHistoryAsChainsPoint(boolean source) {
        MethodNavDto item = historyList.getSelectedValue();
        setAsChainsPoint(item, source);
    }

    private void setFavoriteAsChainsPoint(boolean source) {
        MethodNavDto item = favoriteList.getSelectedValue();
        setAsChainsPoint(item, source);
    }

    private void setAsChainsPoint(MethodNavDto item, boolean source) {
        if (item == null) {
            return;
        }
        if (source) {
            RuntimeFacades.chains().setSource(item.className(), item.methodName(), item.methodDesc());
            statusValue.setText(SwingI18n.tr("已设为 source", "set as source"));
        } else {
            RuntimeFacades.chains().setSink(item.className(), item.methodName(), item.methodDesc());
            statusValue.setText(SwingI18n.tr("已设为 sink", "set as sink"));
        }
    }
}
