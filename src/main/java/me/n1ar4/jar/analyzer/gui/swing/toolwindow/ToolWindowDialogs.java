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

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.others.Proxy;
import me.n1ar4.jar.analyzer.engine.DecompileDispatcher;
import me.n1ar4.jar.analyzer.engine.DecompileType;
import me.n1ar4.jar.analyzer.gui.runtime.api.RuntimeFacades;
import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchMatchMode;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchMode;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchQueryDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchResultDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchSnapshotDto;
import me.n1ar4.jar.analyzer.starter.Const;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.management.ManagementFactory;

public final class ToolWindowDialogs {
    @FunctionalInterface
    public interface Translator {
        String tr(String zh, String en);
    }

    @FunctionalInterface
    public interface BuildInputLoader {
        void load(Path file);
    }

    private static final DateTimeFormatter LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final com.sun.management.OperatingSystemMXBean OS_BEAN = resolveOsBean();
    private static final GlobalMemory OSHI_MEMORY = resolveOshiMemory();
    private static long lastCpuSampleNanos = -1L;
    private static long lastProcessCpuNanos = -1L;

    private ToolWindowDialogs() {
    }

    public static void showExportDialog(JFrame owner, Translator translator) {
        JDialog dialog = createDialog(owner, tr(translator, "导出 Java 代码", "Export Java Code"));
        dialog.setLayout(new BorderLayout(8, 8));
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;

        JTextArea inputArea = new JTextArea(6, 60);
        BuildSnapshotDto build = RuntimeFacades.build().snapshot();
        if (build != null && build.settings() != null && !safe(build.settings().activeInputPath()).isBlank()) {
            inputArea.setText(build.settings().activeInputPath());
        }
        JScrollPane inputScroll = new JScrollPane(inputArea);

        JTextField outputField = new JTextField("jar-analyzer-export");
        JButton outputBrowse = new JButton(tr(translator, "浏览", "Browse"));
        outputBrowse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle(tr(translator, "选择导出目录", "Choose Output Directory"));
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (!safe(outputField.getText()).isBlank()) {
                chooser.setSelectedFile(Paths.get(outputField.getText()).toFile());
            }
            int result = chooser.showOpenDialog(dialog);
            if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
                outputField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        JRadioButton fernflower = new JRadioButton("FernFlower", true);
        JRadioButton cfr = new JRadioButton("CFR");
        ButtonGroup engineGroup = new ButtonGroup();
        engineGroup.add(fernflower);
        engineGroup.add(cfr);
        JCheckBox nestedLib = new JCheckBox(tr(translator, "导出嵌套 lib JAR", "Export Nested lib JAR"), false);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString(tr(translator, "等待开始", "Idle"));

        JButton startButton = new JButton(tr(translator, "开始导出", "Start Export"));
        startButton.addActionListener(e -> {
            List<String> jarInputs = collectExportJarInputs(inputArea.getText());
            if (jarInputs.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        tr(translator, "未发现可导出的 JAR 输入", "No valid jar input found."));
                return;
            }
            String outputDir = safe(outputField.getText()).trim();
            if (outputDir.isBlank()) {
                JOptionPane.showMessageDialog(dialog, tr(translator, "请先填写输出目录", "Output directory is required."));
                return;
            }
            startButton.setEnabled(false);
            progressBar.setIndeterminate(true);
            progressBar.setString(tr(translator, "导出中...", "Exporting..."));
            Thread.ofVirtual().name("swing-export-tool").start(() -> {
                boolean ok;
                String msg;
                try {
                    DecompileType type = cfr.isSelected() ? DecompileType.CFR : DecompileType.FERNFLOWER;
                    ok = DecompileDispatcher.decompileJars(jarInputs, outputDir, type, nestedLib.isSelected());
                    msg = ok
                            ? tr(translator, "导出完成", "Export completed.")
                            : tr(translator, "导出失败，请查看日志", "Export failed, check logs.");
                } catch (Throwable ex) {
                    ok = false;
                    msg = tr(translator, "导出异常: ", "Export error: ") + ex.getMessage();
                }
                boolean finalOk = ok;
                String finalMsg = msg;
                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(finalOk ? 100 : 0);
                    progressBar.setString(finalOk ? tr(translator, "完成", "Done") : tr(translator, "失败", "Failed"));
                    startButton.setEnabled(true);
                    JOptionPane.showMessageDialog(dialog, finalMsg);
                });
            });
        });

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        form.add(new JLabel(tr(translator, "输入（JAR/目录，每行一个）", "Input (jar/dir, one per line)")), c);
        c.gridy = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        form.add(inputScroll, c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        c.gridy = 2;
        c.weightx = 0;
        form.add(new JLabel(tr(translator, "输出目录", "Output Dir")), c);
        c.gridy = 3;
        c.weightx = 1;
        form.add(outputField, c);
        c.gridx = 1;
        c.weightx = 0;
        form.add(outputBrowse, c);
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        JPanel enginePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        enginePanel.add(new JLabel(tr(translator, "反编译引擎", "Decompiler")));
        enginePanel.add(fernflower);
        enginePanel.add(cfr);
        enginePanel.add(nestedLib);
        form.add(enginePanel, c);
        c.gridy = 5;
        form.add(progressBar, c);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(startButton);
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setSize(900, 500);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    public static void showRemoteLoadDialog(JFrame owner, Translator translator, BuildInputLoader loader) {
        JDialog dialog = createDialog(owner, tr(translator, "远程加载", "Remote Load"));
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTextField urlField = new JTextField();
        JTextField localFileField = new JTextField();
        localFileField.setEditable(false);
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString(tr(translator, "等待下载", "Idle"));

        JButton downloadButton = new JButton(tr(translator, "下载", "Download"));
        JButton loadButton = new JButton(tr(translator, "加载到输入", "Load To Input"));
        loadButton.setEnabled(false);
        final Path[] downloaded = new Path[1];

        downloadButton.addActionListener(e -> {
            String rawUrl = safe(urlField.getText()).trim();
            if (rawUrl.isBlank()) {
                JOptionPane.showMessageDialog(dialog, tr(translator, "请输入 URL", "Please input URL."));
                return;
            }
            URI uri;
            try {
                uri = URI.create(rawUrl);
            } catch (Throwable ex) {
                JOptionPane.showMessageDialog(dialog, tr(translator, "URL 格式错误", "Invalid URL."));
                return;
            }
            downloadButton.setEnabled(false);
            loadButton.setEnabled(false);
            progressBar.setIndeterminate(true);
            progressBar.setString(tr(translator, "下载中...", "Downloading..."));
            Thread.ofVirtual().name("swing-remote-load").start(() -> {
                Path outputPath = null;
                String result;
                try {
                    Files.createDirectories(Paths.get(Const.downDir));
                    String filename = safe(Paths.get(safe(uri.getPath())).getFileName() == null
                            ? ""
                            : Paths.get(safe(uri.getPath())).getFileName().toString()).trim();
                    if (filename.isBlank()) {
                        filename = "remote-" + UUID.randomUUID() + ".jar";
                    }
                    if (!filename.toLowerCase(Locale.ROOT).endsWith(".jar")) {
                        filename = filename + ".jar";
                    }
                    outputPath = Paths.get(Const.downDir, filename).toAbsolutePath();
                    HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
                    HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
                    HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                    if (response.statusCode() >= 400) {
                        throw new IOException("http status: " + response.statusCode());
                    }
                    long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
                    try (InputStream in = response.body();
                         OutputStream out = Files.newOutputStream(outputPath)) {
                        byte[] buf = new byte[8192];
                        long written = 0;
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                            written += len;
                            if (contentLength > 0) {
                                int p = (int) Math.max(0, Math.min(100, (written * 100) / contentLength));
                                int progress = p;
                                SwingUtilities.invokeLater(() -> progressBar.setValue(progress));
                            }
                        }
                    }
                    result = tr(translator, "下载成功: ", "Downloaded: ") + outputPath;
                } catch (Throwable ex) {
                    result = tr(translator, "下载失败: ", "Download failed: ") + ex.getMessage();
                }
                Path finalOutputPath = outputPath;
                String finalResult = result;
                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    downloadButton.setEnabled(true);
                    if (finalOutputPath != null && Files.exists(finalOutputPath)) {
                        downloaded[0] = finalOutputPath;
                        localFileField.setText(finalOutputPath.toString());
                        loadButton.setEnabled(true);
                        progressBar.setValue(100);
                        progressBar.setString(tr(translator, "下载完成", "Done"));
                    } else {
                        progressBar.setValue(0);
                        progressBar.setString(tr(translator, "下载失败", "Failed"));
                    }
                    JOptionPane.showMessageDialog(dialog, finalResult);
                });
            });
        });

        loadButton.addActionListener(e -> {
            Path file = downloaded[0];
            if (file == null || !Files.exists(file)) {
                JOptionPane.showMessageDialog(dialog, tr(translator, "请先下载文件", "Please download first."));
                return;
            }
            if (loader != null) {
                loader.load(file);
            }
            JOptionPane.showMessageDialog(dialog, tr(translator, "已加载到输入路径", "Loaded to input path."));
        });

        JPanel center = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        center.add(new JLabel(tr(translator, "下载地址 URL", "Download URL")), c);
        c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        center.add(urlField, c);
        c.gridy = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        center.add(new JLabel(tr(translator, "本地文件", "Local File")), c);
        c.gridy = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        center.add(localFileField, c);
        c.gridy = 4;
        center.add(progressBar, c);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(downloadButton);
        actions.add(loadButton);

        dialog.add(center, BorderLayout.CENTER);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setSize(760, 230);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    public static void showProxyDialog(JFrame owner, Translator translator) {
        JDialog dialog = createDialog(owner, tr(translator, "代理设置", "Proxy Settings"));
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTextField httpHost = new JTextField(safe(Proxy.getHttpHost()));
        JTextField httpPort = new JTextField(safe(Proxy.getHttpPort()));
        JCheckBox applyHttps = new JCheckBox("HTTPS", true);

        JTextField socksHost = new JTextField(safe(Proxy.getSocksProxyHost()));
        JTextField socksPort = new JTextField(safe(Proxy.getSocksProxyPort()));
        JCheckBox systemProxy = new JCheckBox(
                tr(translator, "使用系统代理", "Use System Proxy"),
                Proxy.isSystemProxyOpen());

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.gridy = 0;
        form.add(new JLabel(tr(translator, "HTTP 主机", "HTTP Host")), c);
        c.gridx = 1;
        c.weightx = 1;
        form.add(httpHost, c);
        c.gridx = 2;
        c.weightx = 0;
        form.add(new JLabel(tr(translator, "HTTP 端口", "HTTP Port")), c);
        c.gridx = 3;
        c.weightx = 0.5;
        form.add(httpPort, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        form.add(new JLabel(tr(translator, "SOCKS 主机", "SOCKS Host")), c);
        c.gridx = 1;
        c.weightx = 1;
        form.add(socksHost, c);
        c.gridx = 2;
        c.weightx = 0;
        form.add(new JLabel(tr(translator, "SOCKS 端口", "SOCKS Port")), c);
        c.gridx = 3;
        c.weightx = 0.5;
        form.add(socksPort, c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 4;
        JPanel flags = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        flags.add(applyHttps);
        flags.add(systemProxy);
        form.add(flags, c);

        JButton applyHttpButton = new JButton(tr(translator, "应用 HTTP/S", "Apply HTTP/S"));
        applyHttpButton.addActionListener(e -> {
            Proxy.setHttpProxy(safe(httpHost.getText()).trim(), safe(httpPort.getText()).trim(), applyHttps.isSelected());
            JOptionPane.showMessageDialog(dialog, tr(translator, "HTTP 代理已应用", "HTTP proxy applied."));
        });
        JButton applySocksButton = new JButton(tr(translator, "应用 SOCKS", "Apply SOCKS"));
        applySocksButton.addActionListener(e -> {
            Proxy.setSocks(safe(socksHost.getText()).trim(), safe(socksPort.getText()).trim());
            JOptionPane.showMessageDialog(dialog, tr(translator, "SOCKS 代理已应用", "SOCKS proxy applied."));
        });
        JButton applySystemButton = new JButton(tr(translator, "应用系统代理", "Apply System Proxy"));
        applySystemButton.addActionListener(e -> {
            if (systemProxy.isSelected()) {
                Proxy.setSystemProxy();
            }
            JOptionPane.showMessageDialog(dialog, tr(translator, "系统代理配置已更新", "System proxy updated."));
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(applyHttpButton);
        actions.add(applySocksButton);
        actions.add(applySystemButton);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setSize(760, 220);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    public static void showPartitionDialog(JFrame owner, Translator translator) {
        JDialog dialog = createDialog(owner, tr(translator, "分片批量配置", "Partition Batch Config"));
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JSpinner value = new JSpinner(new SpinnerNumberModel(DatabaseManager.PART_SIZE, 50, 5000, 10));
        JLabel hint = new JLabel(tr(translator, "该值会影响数据库写入批量大小", "Affects database write batch size."));
        JButton apply = new JButton(tr(translator, "应用", "Apply"));
        apply.addActionListener(e -> {
            int val = (Integer) value.getValue();
            DatabaseManager.PART_SIZE = val;
            System.setProperty("jar-analyzer.db.batch", String.valueOf(val));
            JOptionPane.showMessageDialog(dialog, tr(translator, "已更新分片大小: ", "Updated batch size: ") + val);
        });
        JPanel center = new JPanel(new BorderLayout(6, 6));
        center.add(value, BorderLayout.NORTH);
        center.add(hint, BorderLayout.CENTER);
        dialog.add(center, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(apply);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setSize(420, 150);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    public static void showSqlConsoleDialog(JFrame owner, Translator translator) {
        JOptionPane.showMessageDialog(
                owner,
                tr(translator, "SQL 控制台已移除，请使用 Cypher 工作台。", "SQL console has been removed. Use Cypher workbench."),
                tr(translator, "提示", "Notice"),
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    public static void showEncodeToolDialog(JFrame owner, Translator translator) {
        JDialog dialog = createDialog(owner, tr(translator, "编码工具", "Encode Tool"));
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTextArea inputArea = new JTextArea();
        JTextArea outputArea = new JTextArea();
        JScrollPane inputScroll = new JScrollPane(inputArea);
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputArea.setEditable(false);

        JButton b64Encode = new JButton("Base64 +");
        JButton b64Decode = new JButton("Base64 -");
        JButton urlEncode = new JButton("URL +");
        JButton urlDecode = new JButton("URL -");
        JButton md5 = new JButton("MD5");
        JButton sha1 = new JButton("SHA1");
        JButton sha256 = new JButton("SHA256");

        b64Encode.addActionListener(e -> outputArea.setText(Base64.getEncoder()
                .encodeToString(safe(inputArea.getText()).getBytes(StandardCharsets.UTF_8))));
        b64Decode.addActionListener(e -> {
            try {
                outputArea.setText(new String(Base64.getDecoder().decode(safe(inputArea.getText()).trim()), StandardCharsets.UTF_8));
            } catch (Throwable ex) {
                outputArea.setText(tr(translator, "Base64 解码失败: ", "Base64 decode failed: ") + ex.getMessage());
            }
        });
        urlEncode.addActionListener(e -> {
            try {
                outputArea.setText(java.net.URLEncoder.encode(safe(inputArea.getText()), StandardCharsets.UTF_8));
            } catch (Throwable ex) {
                outputArea.setText(tr(translator, "URL 编码失败: ", "URL encode failed: ") + ex.getMessage());
            }
        });
        urlDecode.addActionListener(e -> {
            try {
                outputArea.setText(java.net.URLDecoder.decode(safe(inputArea.getText()), StandardCharsets.UTF_8));
            } catch (Throwable ex) {
                outputArea.setText(tr(translator, "URL 解码失败: ", "URL decode failed: ") + ex.getMessage());
            }
        });
        md5.addActionListener(e -> outputArea.setText(digestHex(translator, "MD5", safe(inputArea.getText()))));
        sha1.addActionListener(e -> outputArea.setText(digestHex(translator, "SHA-1", safe(inputArea.getText()))));
        sha256.addActionListener(e -> outputArea.setText(digestHex(translator, "SHA-256", safe(inputArea.getText()))));

        JPanel center = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0.5;
        center.add(inputScroll, c);
        c.gridy = 1;
        center.add(outputScroll, c);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(b64Encode);
        actions.add(b64Decode);
        actions.add(urlEncode);
        actions.add(urlDecode);
        actions.add(md5);
        actions.add(sha1);
        actions.add(sha256);

        dialog.add(center, BorderLayout.CENTER);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setSize(960, 560);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static String digestHex(Translator translator, String algorithm, String input) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(safe(input).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Throwable ex) {
            return tr(translator, "哈希失败: ", "Hash failed: ") + ex.getMessage();
        }
    }

    public static void showSocketListenerDialog(JFrame owner, Translator translator) {
        JDialog dialog = createDialog(owner, tr(translator, "端口监听", "Socket Listener"));
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTextField portField = new JTextField("8001");
        JTextArea logArea = new JTextArea();
        JScrollPane logScroll = new JScrollPane(logArea);
        JButton startBtn = new JButton(tr(translator, "启动", "Start"));
        JButton stopBtn = new JButton(tr(translator, "停止", "Stop"));
        stopBtn.setEnabled(false);

        AtomicBoolean running = new AtomicBoolean(false);
        final ServerSocket[] holder = new ServerSocket[1];

        Runnable stopAction = () -> {
            running.set(false);
            ServerSocket server = holder[0];
            holder[0] = null;
            if (server != null) {
                try {
                    server.close();
                } catch (Throwable ignored) {
                }
            }
            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);
        };

        startBtn.addActionListener(e -> {
            int port;
            try {
                port = Integer.parseInt(safe(portField.getText()).trim());
            } catch (Throwable ex) {
                appendSocketLog(logArea, tr(translator, "端口格式错误", "Invalid port."));
                return;
            }
            try {
                ServerSocket server = new ServerSocket(port);
                holder[0] = server;
                running.set(true);
                startBtn.setEnabled(false);
                stopBtn.setEnabled(true);
                appendSocketLog(logArea, tr(translator, "监听启动: ", "Listening on: ") + port);
                Thread.ofVirtual().name("swing-socket-listener").start(() -> {
                    while (running.get()) {
                        try (Socket socket = server.accept();
                             InputStream in = socket.getInputStream()) {
                            byte[] data = in.readAllBytes();
                            String body = new String(data, StandardCharsets.UTF_8);
                            appendSocketLog(logArea, socket.getRemoteSocketAddress() + " -> " + body);
                        } catch (Throwable ex) {
                            if (running.get()) {
                                appendSocketLog(logArea, tr(translator, "监听异常: ", "Listener error: ") + ex.getMessage());
                            }
                        }
                    }
                });
            } catch (Throwable ex) {
                appendSocketLog(logArea, tr(translator, "启动失败: ", "Start failed: ") + ex.getMessage());
            }
        });
        stopBtn.addActionListener(e -> stopAction.run());

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopAction.run();
            }
        });

        JPanel north = new JPanel(new BorderLayout(6, 0));
        north.add(new JLabel(tr(translator, "端口", "Port")), BorderLayout.WEST);
        north.add(portField, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(startBtn);
        actions.add(stopBtn);
        north.add(actions, BorderLayout.EAST);

        dialog.add(north, BorderLayout.NORTH);
        dialog.add(logScroll, BorderLayout.CENTER);
        dialog.setSize(860, 500);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static void appendSocketLog(JTextArea logArea, String text) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + LocalDateTime.now().format(LOG_TIME_FORMATTER) + "] " + safe(text) + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void showSerializationDialog(JFrame owner, Translator translator) {
        JDialog dialog = createDialog(owner, tr(translator, "序列化工具", "Serialization Tool"));
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTextArea input = new JTextArea();
        JTextArea output = new JTextArea();
        output.setEditable(false);
        JButton serialize = new JButton(tr(translator, "字符串 -> Base64", "String -> Base64"));
        JButton deserialize = new JButton(tr(translator, "Base64 -> 对象", "Base64 -> Object"));

        serialize.addActionListener(e -> {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                    oos.writeObject(safe(input.getText()));
                }
                output.setText(Base64.getEncoder().encodeToString(baos.toByteArray()));
            } catch (Throwable ex) {
                output.setText(tr(translator, "序列化失败: ", "Serialize failed: ") + ex.getMessage());
            }
        });
        deserialize.addActionListener(e -> {
            try {
                byte[] bytes = Base64.getDecoder().decode(safe(input.getText()).trim());
                Object obj;
                try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                    obj = ois.readObject();
                }
                output.setText(tr(translator, "类型: ", "Type: ") + obj.getClass().getName() + "\n" + String.valueOf(obj));
            } catch (Throwable ex) {
                output.setText(tr(translator, "反序列化失败: ", "Deserialize failed: ") + ex.getMessage());
            }
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(serialize);
        actions.add(deserialize);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(input), new JScrollPane(output));
        split.setResizeWeight(0.45);
        split.setContinuousLayout(true);

        dialog.add(actions, BorderLayout.NORTH);
        dialog.add(split, BorderLayout.CENTER);
        dialog.setSize(900, 540);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    public static void showHttpRepeaterDialog(JFrame owner, Translator translator) {
        JDialog dialog = createDialog(owner, "HTTP Repeater");
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JComboBox<String> methodBox = new JComboBox<>(new String[]{"GET", "POST", "PUT", "DELETE", "PATCH"});
        JTextField urlField = new JTextField();
        JTextArea headersArea = new JTextArea();
        headersArea.setRows(4);
        JTextArea bodyArea = new JTextArea();
        JTextArea responseArea = new JTextArea();
        responseArea.setEditable(false);
        JButton sendBtn = new JButton(tr(translator, "发送", "Send"));

        sendBtn.addActionListener(e -> {
            String method = String.valueOf(methodBox.getSelectedItem());
            String rawUrl = safe(urlField.getText()).trim();
            if (rawUrl.isBlank()) {
                return;
            }
            sendBtn.setEnabled(false);
            responseArea.setText(tr(translator, "请求中...", "Requesting..."));
            Thread.ofVirtual().name("swing-http-repeater").start(() -> {
                String result;
                try {
                    URI uri = URI.create(rawUrl);
                    HttpRequest.Builder builder = HttpRequest.newBuilder(uri);
                    String body = safe(bodyArea.getText());
                    if ("GET".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
                        if (body.isBlank()) {
                            builder.method(method, HttpRequest.BodyPublishers.noBody());
                        } else {
                            builder.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
                        }
                    } else {
                        builder.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
                    }
                    for (String line : safe(headersArea.getText()).split("\\R")) {
                        String h = safe(line).trim();
                        if (h.isBlank()) {
                            continue;
                        }
                        int idx = h.indexOf(':');
                        if (idx <= 0) {
                            continue;
                        }
                        String k = h.substring(0, idx).trim();
                        String v = h.substring(idx + 1).trim();
                        if (!k.isEmpty()) {
                            builder.header(k, v);
                        }
                    }
                    HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
                    HttpResponse<String> response = client.send(builder.build(),
                            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    sb.append("HTTP ").append(response.statusCode()).append('\n');
                    response.headers().map().forEach((k, v) -> sb.append(k).append(": ")
                            .append(String.join(",", v)).append('\n'));
                    sb.append('\n').append(response.body());
                    result = sb.toString();
                } catch (Throwable ex) {
                    result = tr(translator, "请求失败: ", "Request failed: ") + ex.getMessage();
                }
                String finalResult = result;
                SwingUtilities.invokeLater(() -> {
                    responseArea.setText(finalResult);
                    responseArea.setCaretPosition(0);
                    sendBtn.setEnabled(true);
                });
            });
        });

        JPanel north = new JPanel(new BorderLayout(6, 6));
        JPanel urlLine = new JPanel(new BorderLayout(6, 0));
        urlLine.add(methodBox, BorderLayout.WEST);
        urlLine.add(urlField, BorderLayout.CENTER);
        urlLine.add(sendBtn, BorderLayout.EAST);
        north.add(urlLine, BorderLayout.NORTH);
        north.add(new JScrollPane(headersArea), BorderLayout.CENTER);

        JSplitPane middle = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(bodyArea), new JScrollPane(responseArea));
        middle.setResizeWeight(0.38);
        middle.setContinuousLayout(true);

        dialog.add(north, BorderLayout.NORTH);
        dialog.add(middle, BorderLayout.CENTER);
        dialog.setSize(980, 680);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    public static void showAnalysisTextDialog(JFrame owner, Translator translator, String title, String content) {
        JDialog dialog = createDialog(owner, safe(title).isBlank()
                ? tr(translator, "分析结果", "Analysis Result")
                : title);
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTextField keywordField = new JTextField();
        JButton prevBtn = new JButton(tr(translator, "上一个", "Prev"));
        JButton nextBtn = new JButton(tr(translator, "下一个", "Next"));
        JButton copyBtn = new JButton(tr(translator, "复制", "Copy"));
        JButton saveBtn = new JButton(tr(translator, "保存", "Save"));

        JTextArea textArea = new JTextArea(safe(content));
        textArea.setEditable(false);
        textArea.setLineWrap(false);
        textArea.setWrapStyleWord(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(textArea);

        JLabel status = new JLabel(tr(translator, "就绪", "ready"));
        Runnable findNext = () -> {
            if (selectTextMatch(textArea, keywordField.getText(), true)) {
                status.setText(tr(translator, "已定位", "matched"));
            } else {
                status.setText(tr(translator, "未找到", "not found"));
            }
        };
        Runnable findPrev = () -> {
            if (selectTextMatch(textArea, keywordField.getText(), false)) {
                status.setText(tr(translator, "已定位", "matched"));
            } else {
                status.setText(tr(translator, "未找到", "not found"));
            }
        };
        keywordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    findNext.run();
                }
            }
        });
        nextBtn.addActionListener(e -> findNext.run());
        prevBtn.addActionListener(e -> findPrev.run());
        copyBtn.addActionListener(e -> {
            String selected = safe(textArea.getSelectedText());
            if (selected.isBlank()) {
                selected = safe(textArea.getText());
            }
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(selected), null);
        });
        saveBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle(tr(translator, "保存文本", "Save Text"));
            int result = chooser.showSaveDialog(dialog);
            if (result != JFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null) {
                return;
            }
            try {
                Files.writeString(chooser.getSelectedFile().toPath(), safe(textArea.getText()), StandardCharsets.UTF_8);
                status.setText(tr(translator, "已保存", "saved"));
            } catch (Throwable ex) {
                status.setText(tr(translator, "保存失败: ", "save failed: ") + ex.getMessage());
            }
        });

        JPanel north = new JPanel(new BorderLayout(6, 0));
        north.add(keywordField, BorderLayout.CENTER);
        JPanel findButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        findButtons.add(prevBtn);
        findButtons.add(nextBtn);
        north.add(findButtons, BorderLayout.EAST);

        JPanel south = new JPanel(new BorderLayout(6, 0));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actions.add(copyBtn);
        actions.add(saveBtn);
        south.add(actions, BorderLayout.WEST);
        south.add(status, BorderLayout.CENTER);

        dialog.add(north, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(south, BorderLayout.SOUTH);
        dialog.setSize(980, 700);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    public static void showElSearchDialog(JFrame owner, Translator translator) {
        JDialog dialog = createDialog(owner, tr(translator, "EL 搜索", "EL Search"));
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTextField keywordField = new JTextField("${");
        JButton startBtn = new JButton(tr(translator, "开始", "Start"));
        JButton openBtn = new JButton(tr(translator, "打开", "Open"));
        JLabel statusLabel = new JLabel(tr(translator, "就绪", "ready"));

        DefaultListModel<SearchResultDto> resultModel = new DefaultListModel<>();
        JList<SearchResultDto> resultList = new JList<>(resultModel);
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultList.setCellRenderer(new DefaultListCellRenderer() {
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
                    String className = safe(item.className());
                    String methodName = safe(item.methodName());
                    if (methodName.isBlank()) {
                        setText(className);
                    } else {
                        setText(className + "#" + methodName + safe(item.methodDesc())
                                + " [" + safe(item.jarName()) + "]");
                    }
                }
                return this;
            }
        });
        resultList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    openSelectedSearchResult(resultList, statusLabel, translator);
                }
            }
        });

        Runnable runSearch = () -> {
            String keyword = safe(keywordField.getText()).trim();
            if (keyword.isBlank()) {
                statusLabel.setText(tr(translator, "关键字不能为空", "keyword is required"));
                return;
            }
            startBtn.setEnabled(false);
            statusLabel.setText(tr(translator, "搜索中...", "searching..."));
            Thread.ofVirtual().name("swing-el-search").start(() -> {
                RuntimeFacades.search().applyQuery(new SearchQueryDto(
                        SearchMode.STRING_CONTAINS,
                        SearchMatchMode.LIKE,
                        "",
                        "",
                        keyword,
                        false
                ));
                RuntimeFacades.search().runSearch();
                SearchSnapshotDto snapshot = waitForSearchResult();
                List<SearchResultDto> rows = snapshot == null || snapshot.results() == null
                        ? List.of()
                        : snapshot.results();
                String status = snapshot == null ? "done" : safe(snapshot.statusText());
                SwingUtilities.invokeLater(() -> {
                    resultModel.clear();
                    for (SearchResultDto row : rows) {
                        resultModel.addElement(row);
                    }
                    statusLabel.setText(status.isBlank()
                            ? tr(translator, "结果: ", "results: ") + rows.size()
                            : status);
                    startBtn.setEnabled(true);
                });
            });
        };
        startBtn.addActionListener(e -> runSearch.run());
        openBtn.addActionListener(e -> openSelectedSearchResult(resultList, statusLabel, translator));
        keywordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    runSearch.run();
                }
            }
        });

        JPanel north = new JPanel(new BorderLayout(6, 0));
        north.add(new JLabel(tr(translator, "关键字", "keyword")), BorderLayout.WEST);
        north.add(keywordField, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actions.add(startBtn);
        actions.add(openBtn);

        JPanel south = new JPanel(new BorderLayout(6, 0));
        south.add(actions, BorderLayout.WEST);
        south.add(statusLabel, BorderLayout.CENTER);

        dialog.add(north, BorderLayout.NORTH);
        dialog.add(new JScrollPane(resultList), BorderLayout.CENTER);
        dialog.add(south, BorderLayout.SOUTH);
        dialog.setSize(880, 580);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    public static void showSystemMonitorDialog(JFrame owner, Translator translator) {
        JDialog dialog = createDialog(owner, tr(translator, "系统监控", "System Monitor"));
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel cpuLabel = new JLabel();
        JLabel memLabel = new JLabel();
        JProgressBar cpuBar = new JProgressBar(0, 100);
        JProgressBar memBar = new JProgressBar(0, 100);
        cpuBar.setStringPainted(true);
        memBar.setStringPainted(true);
        cpuBar.setForeground(new Color(0x5AAAFB));
        memBar.setForeground(new Color(0x63C986));

        JPanel grid = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        grid.add(new JLabel("CPU"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        grid.add(cpuBar, c);
        c.gridx = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        grid.add(cpuLabel, c);
        c.gridx = 0;
        c.gridy = 1;
        grid.add(new JLabel("MEM"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        grid.add(memBar, c);
        c.gridx = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        grid.add(memLabel, c);

        Timer timer = new Timer(800, e -> updateSystemMonitor(cpuBar, memBar, cpuLabel, memLabel));
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                timer.start();
                updateSystemMonitor(cpuBar, memBar, cpuLabel, memLabel);
            }

            @Override
            public void windowClosing(WindowEvent e) {
                timer.stop();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                timer.stop();
            }
        });

        dialog.add(grid, BorderLayout.CENTER);
        dialog.setSize(460, 180);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static boolean selectTextMatch(JTextArea area, String keywordRaw, boolean forward) {
        String keyword = safe(keywordRaw).trim();
        if (keyword.isBlank()) {
            return false;
        }
        String source = safe(area.getText());
        String sourceLower = source.toLowerCase(Locale.ROOT);
        String target = keyword.toLowerCase(Locale.ROOT);
        int caret = area.getCaretPosition();
        int index;
        if (forward) {
            int start = Math.max(0, caret);
            index = sourceLower.indexOf(target, start);
            if (index < 0) {
                index = sourceLower.indexOf(target);
            }
        } else {
            int start = Math.max(0, caret - 1);
            index = sourceLower.lastIndexOf(target, start);
            if (index < 0) {
                index = sourceLower.lastIndexOf(target);
            }
        }
        if (index < 0) {
            return false;
        }
        area.requestFocusInWindow();
        area.setCaretPosition(index);
        area.moveCaretPosition(index + target.length());
        return true;
    }

    private static SearchSnapshotDto waitForSearchResult() {
        SearchSnapshotDto snapshot = RuntimeFacades.search().snapshot();
        long deadline = System.currentTimeMillis() + 7000L;
        while (System.currentTimeMillis() < deadline) {
            SearchSnapshotDto current = RuntimeFacades.search().snapshot();
            if (current != null) {
                snapshot = current;
                String status = safe(current.statusText()).toLowerCase(Locale.ROOT);
                boolean finished = status.startsWith("results:")
                        || status.contains("required")
                        || status.contains("error")
                        || status.contains("unsupported");
                if (finished) {
                    break;
                }
            }
            try {
                Thread.sleep(120L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return snapshot;
    }

    private static void openSelectedSearchResult(
            JList<SearchResultDto> resultList,
            JLabel statusLabel,
            Translator translator
    ) {
        SearchResultDto selected = resultList.getSelectedValue();
        if (selected == null) {
            return;
        }
        if (safe(selected.methodName()).isBlank()) {
            statusLabel.setText(tr(translator, "结果不支持方法跳转", "result has no method navigation"));
            return;
        }
        RuntimeFacades.editor().openMethod(
                selected.className(),
                selected.methodName(),
                selected.methodDesc(),
                selected.jarId()
        );
    }

    private static void updateSystemMonitor(
            JProgressBar cpuBar,
            JProgressBar memBar,
            JLabel cpuLabel,
            JLabel memLabel
    ) {
        double cpu = readCpuLoad();
        double mem = readMemoryLoad();
        int cpuPercent = (int) Math.round(cpu * 100.0);
        int memPercent = (int) Math.round(mem * 100.0);
        cpuBar.setValue(cpuPercent);
        memBar.setValue(memPercent);
        cpuBar.setString(cpuPercent + "%");
        memBar.setString(memPercent + "%");
        cpuLabel.setText(cpuPercent + "%");
        memLabel.setText(memPercent + "%");
    }

    private static double readCpuLoad() {
        try {
            if (OS_BEAN != null) {
                double value = OS_BEAN.getSystemCpuLoad();
                if (value < 0 || Double.isNaN(value)) {
                    value = OS_BEAN.getProcessCpuLoad();
                }
                if (!Double.isNaN(value) && value >= 0.0) {
                    return Math.max(0.0, Math.min(1.0, value));
                }

                // Fallback for platforms where cpu load APIs frequently return -1.
                long processCpu = OS_BEAN.getProcessCpuTime();
                long now = System.nanoTime();
                if (processCpu > 0L) {
                    long prevCpu = lastProcessCpuNanos;
                    long prevNow = lastCpuSampleNanos;
                    lastProcessCpuNanos = processCpu;
                    lastCpuSampleNanos = now;
                    if (prevCpu > 0L && prevNow > 0L && processCpu >= prevCpu && now > prevNow) {
                        long elapsed = now - prevNow;
                        int cores = Math.max(1, OS_BEAN.getAvailableProcessors());
                        double fallback = (double) (processCpu - prevCpu) / (double) (elapsed * cores);
                        if (!Double.isNaN(fallback) && !Double.isInfinite(fallback) && fallback >= 0.0) {
                            return Math.max(0.0, Math.min(1.0, fallback));
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return 0.0;
    }

    private static double readMemoryLoad() {
        try {
            if (OSHI_MEMORY != null) {
                long total = OSHI_MEMORY.getTotal();
                long available = OSHI_MEMORY.getAvailable();
                if (total > 0L && available >= 0L) {
                    double value = (double) (total - available) / (double) total;
                    return Math.max(0.0, Math.min(1.0, value));
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            if (OS_BEAN != null) {
                long total = OS_BEAN.getTotalMemorySize();
                long free = OS_BEAN.getFreeMemorySize();
                if (total > 0L) {
                    double value = (double) (total - free) / (double) total;
                    return Math.max(0.0, Math.min(1.0, value));
                }
            }
        } catch (Throwable ignored) {
        }
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long used = runtime.totalMemory() - runtime.freeMemory();
        if (max <= 0L) {
            return 0.0;
        }
        double value = (double) used / (double) max;
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static GlobalMemory resolveOshiMemory() {
        try {
            return new SystemInfo().getHardware().getMemory();
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static com.sun.management.OperatingSystemMXBean resolveOsBean() {
        try {
            java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof com.sun.management.OperatingSystemMXBean osBean) {
                return osBean;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static List<String> collectExportJarInputs(String raw) {
        List<String> jars = new ArrayList<>();
        for (String line : safe(raw).split("\\R")) {
            String item = safe(line).trim();
            if (item.isEmpty()) {
                continue;
            }
            Path path;
            try {
                path = Paths.get(item).toAbsolutePath();
            } catch (Throwable ignored) {
                continue;
            }
            if (!Files.exists(path)) {
                continue;
            }
            if (Files.isDirectory(path)) {
                try (java.util.stream.Stream<Path> walk = Files.walk(path)) {
                    walk.filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                            .forEach(p -> jars.add(p.toString()));
                } catch (Throwable ignored) {
                }
                continue;
            }
            if (path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                jars.add(path.toString());
            }
        }
        return jars.stream().distinct().toList();
    }

    private static JDialog createDialog(JFrame owner, String title) {
        Window window = owner == null ? null : SwingUtilities.getWindowAncestor(owner);
        if (window instanceof Frame frame) {
            return new JDialog(frame, safe(title), false);
        }
        if (window instanceof Dialog dialog) {
            return new JDialog(dialog, safe(title), false);
        }
        return new JDialog(owner, safe(title), false);
    }

    private static String tr(Translator translator, String zh, String en) {
        if (translator == null) {
            return safe(zh);
        }
        return safe(translator.tr(zh, en));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
