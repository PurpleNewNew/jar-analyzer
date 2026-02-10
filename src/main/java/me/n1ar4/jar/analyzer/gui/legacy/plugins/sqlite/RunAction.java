/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.legacy.plugins.sqlite;

import me.n1ar4.jar.analyzer.gui.util.ProcessDialog;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Vector;

public class RunAction {
    public static void register() {
        SQLiteForm instance = SQLiteForm.getInstance();
        instance.getRunButton().addActionListener(e -> {
            instance.getErrArea().setText(null);
            if (SQLiteForm.getHelper() == null) {
                JOptionPane.showMessageDialog(instance.getMasterPanel(),
                        "please connect first");
                return;
            }
            JTextArea area = SQLiteForm.getSqlArea();
            if (area == null || area.getText() == null || area.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(instance.getMasterPanel(),
                        "please input sql first");
                return;
            }
            String sql = area.getText();
            JDialog dialog = UiExecutor.callOnEdt(() ->
                    ProcessDialog.createProgressDialog(instance.getMasterPanel()));
            if (dialog != null) {
                UiExecutor.runOnEdt(() -> dialog.setVisible(true));
            }
            UiExecutor.runAsync(() -> {
                DefaultTableModel model = new DefaultTableModel();
                String errMsg = null;
                SQLiteHelper helper = SQLiteForm.getHelper();
                helper.connect();
                try (ResultSet rs = helper.executeQuery(sql)) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    Vector<String> columnNames = new Vector<>();
                    int columnCount = metaData.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        columnNames.add(metaData.getColumnName(i));
                    }
                    Vector<Vector<Object>> data = new Vector<>();
                    while (rs.next()) {
                        Vector<Object> vector = new Vector<>();
                        for (int i = 1; i <= columnCount; i++) {
                            vector.add(rs.getObject(i));
                        }
                        data.add(vector);
                    }
                    model.setDataVector(data, columnNames);
                } catch (Exception ex) {
                    errMsg = ex.getMessage();
                } finally {
                    helper.close();
                }
                String finalErr = errMsg;
                UiExecutor.runOnEdt(() -> {
                    if (finalErr != null) {
                        instance.getErrArea().setText(finalErr);
                    }
                    instance.getResultTable().setModel(model);
                    if (dialog != null) {
                        dialog.dispose();
                    }
                });
            });
        });
    }
}
