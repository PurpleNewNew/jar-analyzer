/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import me.n1ar4.jar.analyzer.gui.action.BuildAction;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import okhttp3.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class RemoteHttp {
    private static final Logger logger = LogManager.getLogger();
    private JPanel remotePanel;
    private JPanel rootPanel;
    private JTextField urlText;
    private JProgressBar progressBar;
    private JButton downBtn;
    private JButton loadBtn;
    private JLabel urlLabel;
    private JPanel opPanel;
    private static RemoteHttp instance;
    private static JFrame globalFrame;
    private static volatile String filename = null;
    private static volatile boolean finish = false;

    public static void start() {
        JFrame frame = new JFrame(Const.RemoteForm);
        instance = new RemoteHttp();
        instance.init();
        frame.setContentPane(instance.rootPanel);
        frame.pack();
        frame.setLocationRelativeTo(MainForm.getInstance().getMasterPanel());
        frame.setVisible(true);
        frame.setResizable(false);
        globalFrame = frame;
    }

    private void init() {
        instance.progressBar.setValue(0);
        downBtn.addActionListener(e -> {
            finish = false;
            UiExecutor.runOnEdt(() -> loadBtn.setEnabled(false));
            OkHttpClient okHttpClient = new OkHttpClient();
            String url = urlText.getText();
            if (url == null || url.isBlank()) {
                UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(instance.rootPanel, "error url"));
                return;
            }
            UiExecutor.runOnEdt(() -> progressBar.setValue(2));
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Connection", "close")
                    .build();
            UiExecutor.runOnEdt(() -> progressBar.setValue(3));
            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                @SuppressWarnings("all")
                public void onFailure(Call call, IOException ignored) {
                    UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(instance.rootPanel, "download failed"));
                    finish = false;
                    UiExecutor.runOnEdt(() -> loadBtn.setEnabled(false));
                }

                @Override
                @SuppressWarnings("all")
                public void onResponse(Call call, Response response) {
                    boolean ok = false;
                    try {
                        try (response) {
                            ResponseBody body = response.body();
                            if (body == null) {
                                UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(instance.rootPanel, "empty response"));
                                return;
                            }

                            long total = body.contentLength();
                            filename = "temp" + UUID.randomUUID() + ".jar";

                            Path downDir = Paths.get(Const.downDir);
                            try {
                                Files.createDirectories(downDir);
                            } catch (Exception ignored) {
                            }
                            Path file = downDir.resolve(filename);

                            UiExecutor.runOnEdt(() -> progressBar.setValue(4));
                            byte[] buf = new byte[2048];
                            long sum = 0;
                            long lastUiUpdate = System.currentTimeMillis();
                            int lastProgress = 4;
                            try (InputStream is = body.byteStream();
                                 FileOutputStream fos = new FileOutputStream(file.toFile())) {
                                int len;
                                while ((len = is.read(buf)) != -1) {
                                    fos.write(buf, 0, len);
                                    sum += len;
                                    int progress;
                                    if (total > 0) {
                                        progress = (int) (sum * 1.0f / total * 100);
                                    } else {
                                        progress = 4;
                                    }
                                    if (progress < 4) {
                                        progress = 4;
                                    }
                                    long now = System.currentTimeMillis();
                                    if (progress >= 100 || (progress > lastProgress && now - lastUiUpdate >= 100)) {
                                        int finalProgress = progress;
                                        lastProgress = progress;
                                        lastUiUpdate = now;
                                        UiExecutor.runOnEdt(() -> progressBar.setValue(finalProgress));
                                    }
                                }
                                fos.flush();
                                ok = true;
                            }
                        }
                    } catch (Exception ex) {
                        logger.debug("download failed: {}", ex.toString());
                        UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(instance.rootPanel, "download failed"));
                    } finally {
                        finish = ok;
                        boolean finalOk = ok;
                        UiExecutor.runOnEdt(() -> loadBtn.setEnabled(finalOk));
                    }
                }
            });
        });

        loadBtn.addActionListener(e -> {
            if (finish) {
                Path down = Paths.get(Const.downDir);
                try {
                    Files.createDirectories(down);
                } catch (Exception ignored) {
                }
                Path finalPath = down.resolve(Paths.get(filename));
                logger.info("load {}", finalPath.toString());
                BuildAction.start(finalPath.toAbsolutePath().toString());
                globalFrame.setVisible(false);
            } else {
                JOptionPane.showMessageDialog(instance.rootPanel, "download first");
            }
        });
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        remotePanel = new JPanel();
        remotePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel = new JPanel();
        rootPanel.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 5), -1, -1));
        remotePanel.add(rootPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        urlLabel = new JLabel();
        urlLabel.setText("HTTP URL");
        rootPanel.add(urlLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        final Spacer spacer1 = new Spacer();
        rootPanel.add(spacer1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        urlText = new JTextField();
        rootPanel.add(urlText, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(400, -1), new Dimension(150, -1), null, 0, false));
        progressBar = new JProgressBar();
        progressBar.setBorderPainted(true);
        progressBar.setStringPainted(true);
        rootPanel.add(progressBar, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        opPanel = new JPanel();
        opPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(opPanel, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        downBtn = new JButton();
        downBtn.setText("DOWNLOAD");
        opPanel.add(downBtn, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        loadBtn = new JButton();
        loadBtn.setText("LOAD");
        opPanel.add(loadBtn, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return remotePanel;
    }

}
