/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.plugins.sqlite;

import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.gui.util.ProcessDialog;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;

public class ConnectAction {
    public static void register() {
        SQLiteForm instance = SQLiteForm.getInstance();
        instance.getConnectButton().addActionListener(e -> {
            UiExecutor.runAsync(() -> {
                Path dbPath = Paths.get(Const.dbFile);
                if (!Files.exists(dbPath)) {
                    UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(
                            instance.getMasterPanel(),
                            String.format("need %s file", Const.dbFile)));
                    return;
                }
                UiExecutor.runOnEdt(() ->
                        instance.getSqliteText().setText(dbPath.toAbsolutePath().toString()));
                JDialog dialog = UiExecutor.callOnEdt(() ->
                        ProcessDialog.createProgressDialog(instance.getMasterPanel()));
                if (dialog != null) {
                    UiExecutor.runOnEdt(() -> dialog.setVisible(true));
                }
                DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
                String errMsg = null;
                SQLiteHelper helper = new SQLiteHelper();
                SQLiteForm.setHelper(helper);
                try {
                    helper.connect();
                    String getAllTablesSQL = "SELECT tbl_name FROM sqlite_master";
                    try (ResultSet rs = helper.executeQuery(getAllTablesSQL)) {
                        while (rs.next()) {
                            String tblName = rs.getString("tbl_name");
                            model.addElement(tblName);
                        }
                    }
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
                    instance.getTablesBox().setModel(model);
                    if (dialog != null) {
                        dialog.dispose();
                    }
                });
            });
        });
    }
}
