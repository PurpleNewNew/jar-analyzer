/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.adapter;

import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.util.NavigationHelper;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class FavMouseAdapter extends MouseAdapter {
    @SuppressWarnings("all")
    public void mouseClicked(MouseEvent evt) {
        JList<?> list = (JList<?>) evt.getSource();
        // 双击打开
        if (evt.getClickCount() == 2) {
            int index = list.locationToIndex(evt.getPoint());
            MethodResult res = null;
            try {
                res = (MethodResult) list.getModel().getElementAt(index);
            } catch (Exception ignored) {
            }
            if (res == null) {
                return;
            }
            NavigationHelper.openMethod(res);
        } else if (SwingUtilities.isRightMouseButton(evt)) {
            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem cleanAllFavorite = new JMenuItem("clean all favorite");
            JMenuItem cleanCurItems = new JMenuItem("clean this favorite");
            JMenuItem sendToSink = new JMenuItem("send to chains sink");
            JMenuItem sendToSource = new JMenuItem("send to chains source");
            popupMenu.add(cleanAllFavorite);
            popupMenu.add(cleanCurItems);
            popupMenu.add(sendToSink);
            popupMenu.add(sendToSource);
            cleanAllFavorite.addActionListener(e -> {
                MainForm.getFavData().clear();
                JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                        "CLEAN ALL FAVORIATES FINISH");
                UiExecutor.runAsync(() -> MainForm.getEngine().cleanFav());
            });
            cleanCurItems.addActionListener(e -> {
                MethodResult selectedItem = (MethodResult) list.getSelectedValue();
                if (MainForm.getFavData().removeElement(selectedItem)) {
                    JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                            "CLEAN FAVORIATE " + selectedItem.getMethodName() + " FINISH");
                    UiExecutor.runAsync(() -> MainForm.getEngine().cleanFavItem(selectedItem));
                }
            });
            sendToSink.addActionListener(e -> {
                MethodResult selectedItem = (MethodResult) list.getSelectedValue();
                MainForm.getInstance().setSink(
                        selectedItem.getClassName(), selectedItem.getMethodName(), selectedItem.getMethodDesc());
                JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                        "SEND SINK " + selectedItem.getMethodName() + " FINISH");
            });
            sendToSource.addActionListener(e -> {
                MethodResult selectedItem = (MethodResult) list.getSelectedValue();
                MainForm.getInstance().setSource(
                        selectedItem.getClassName(), selectedItem.getMethodName(), selectedItem.getMethodDesc());
                JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                        "SEND SOURCE " + selectedItem.getMethodName() + " FINISH");
            });
            int index = list.locationToIndex(evt.getPoint());
            list.setSelectedIndex(index);
            popupMenu.show(list, evt.getX(), evt.getY());
        }
    }
}
