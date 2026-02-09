/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.leak;

import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.entity.LeakResult;
import me.n1ar4.jar.analyzer.entity.MemberEntity;
import me.n1ar4.jar.analyzer.exporter.LeakCsvExporter;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.util.ProcessDialog;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.CommonFilterUtil;
import me.n1ar4.jar.analyzer.utils.DirUtil;
import me.n1ar4.jar.analyzer.utils.JarUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import javax.swing.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

public class LeakAction {
    private static final Logger logger = LogManager.getLogger();
    // 规则配置类
    private static class RuleConfig {
        private final boolean enabled;
        private final Function<String, List<String>> ruleFunction;
        private final String typeName;
        private final String logName;

        public RuleConfig(boolean enabled, Function<String, List<String>> ruleFunction,
                          String typeName, String logName) {
            this.enabled = enabled;
            this.ruleFunction = ruleFunction;
            this.typeName = typeName;
            this.logName = logName;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public Function<String, List<String>> getRuleFunction() {
            return ruleFunction;
        }

        public String getTypeName() {
            return typeName;
        }

        public String getLogName() {
            return logName;
        }
    }

    private static void log(String msg) {
        String line = "[LOG] " + msg + "\n";
        UiExecutor.runOnEdt(() -> {
            MainForm.getInstance().getLeakLogArea().append(line);
            MainForm.getInstance().getLeakLogArea().setCaretPosition(
                    MainForm.getInstance().getLeakLogArea().getDocument().getLength()
            );
        });
    }

    /**
     * 通用规则处理器
     *
     * @param config    规则配置
     * @param members   成员实体列表
     * @param stringMap 字符串映射
     * @param results   结果集合
     */
    private static void processRule(RuleConfig config, List<MemberEntity> members,
                                    Map<String, String> stringMap, Set<LeakResult> results) {
        if (!config.isEnabled()) {
            return;
        }

        log(config.getLogName() + " leak start");

        // 处理成员实体
        for (MemberEntity member : members) {
            if (isJdkClass(member.getClassName())) {
                continue;
            }
            List<String> data = config.getRuleFunction().apply(member.getValue());
            if (data.isEmpty()) {
                continue;
            }
            for (String s : data) {
                LeakResult leakResult = new LeakResult();
                leakResult.setClassName(member.getClassName());
                leakResult.setValue(s.trim());
                leakResult.setTypeName(config.getTypeName());
                results.add(leakResult);
            }
        }

        Path tempDir = Paths.get(Const.tempDir).toAbsolutePath();
        try {
            List<String> allFiles = DirUtil.GetFiles(tempDir.toString());
            for (String filePath : allFiles) {
                Path file = Paths.get(filePath);
                String fileName = file.getFileName().toString().toLowerCase();

                // 检查文件是否符合配置文件扩展名规则
                boolean isConfigFile = false;
                for (String ext : JarUtil.CONFIG_EXTENSIONS) {
                    if (fileName.endsWith(ext.toLowerCase())) {
                        isConfigFile = true;
                        break;
                    }
                }

                if (isConfigFile) {
                    try {
                        // 读取文件内容
                        byte[] fileBytes = Files.readAllBytes(file);
                        String fileContent = new String(fileBytes, StandardCharsets.UTF_8);

                        // 应用规则函数进行匹配
                        List<String> data = config.getRuleFunction().apply(fileContent);
                        if (!data.isEmpty()) {
                            for (String s : data) {
                                LeakResult leakResult = new LeakResult();
                                // 使用相对于 tempDir 的路径作为类名
                                String relativePath = tempDir.relativize(file).toString()
                                        .replace("\\", "/");
                                leakResult.setClassName(relativePath);
                                leakResult.setValue(s.trim());
                                leakResult.setTypeName(config.getTypeName());
                                results.add(leakResult);
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("读取配置文件失败: {} - {}", filePath, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("遍历静态文件时出错: {}", e.toString());
        }

        // 处理字符串映射
        for (Map.Entry<String, String> entry : stringMap.entrySet()) {
            String className = entry.getKey();
            String value = entry.getValue();
            if (isJdkClass(className)) {
                continue;
            }
            List<String> data = config.getRuleFunction().apply(value);
            if (data.isEmpty()) {
                continue;
            }
            for (String s : data) {
                LeakResult leakResult = new LeakResult();
                leakResult.setClassName(className);
                leakResult.setValue(s.trim());
                leakResult.setTypeName(config.getTypeName());
                results.add(leakResult);
            }
        }

        log(config.getLogName() + " leak finish");
    }

    public static void register() {
        MainForm instance = MainForm.getInstance();
        if (instance == null) {
            return;
        }

        JButton export = instance.getExportLeakBtn();

        // 获取所有复选框
        JCheckBox jwtBox = instance.getLeakJWTBox();
        JCheckBox idCardBox = instance.getLeakIdBox();
        JCheckBox ipAddrBox = instance.getLeakIpBox();
        JCheckBox emailBox = instance.getLeakEmailBox();
        JCheckBox urlBox = instance.getLeakUrlBox();
        JCheckBox jdbcBox = instance.getLeakJdbcBox();
        JCheckBox filePathBox = instance.getLeakFileBox();
        JCheckBox macAddrBox = instance.getLeakMacBox();
        JCheckBox phoneBox = instance.getLeakPhoneBox();
        JCheckBox apiKeyBox = instance.getAPIKeyCheckBox();
        JCheckBox bankBox = instance.getBankCardCheckBox();
        JCheckBox cloudAkSkBox = instance.getAKSKCheckBox();
        JCheckBox cryptoBox = instance.getCryptoKeyCheckBox();
        JCheckBox aiKeyBox = instance.getAIKeyCheckBox();
        JCheckBox passBox = instance.getPasswordCheckBox();

        // 设置默认选中状态
        jwtBox.setSelected(true);
        idCardBox.setSelected(true);
        ipAddrBox.setSelected(true);
        emailBox.setSelected(true);
        urlBox.setSelected(true);
        jdbcBox.setSelected(true);
        filePathBox.setSelected(true);
        macAddrBox.setSelected(true);
        phoneBox.setSelected(true);
        apiKeyBox.setSelected(true);
        bankBox.setSelected(true);
        cloudAkSkBox.setSelected(true);
        cryptoBox.setSelected(true);
        aiKeyBox.setSelected(true);
        passBox.setSelected(true);

        JList<LeakResult> leakList = instance.getLeakResultList();

        logger.info("registering leak action");

        // 添加导出功能
        export.addActionListener(e -> {
            DefaultListModel<LeakResult> model = (DefaultListModel<LeakResult>) leakList.getModel();
            if (model == null || model.isEmpty()) {
                JOptionPane.showMessageDialog(instance.getMasterPanel(), "没有泄露检测结果可以导出");
                return;
            }

            List<LeakResult> results = new ArrayList<>();
            for (int i = 0; i < model.getSize(); i++) {
                results.add(model.getElementAt(i));
            }

            JDialog dialog = UiExecutor.callOnEdt(() ->
                    ProcessDialog.createProgressDialog(instance.getMasterPanel()));
            if (dialog != null) {
                UiExecutor.runOnEdt(() -> dialog.setVisible(true));
            }
            UiExecutor.runAsync(() -> {
                LeakCsvExporter exporter = new LeakCsvExporter(results);
                boolean success = exporter.doExport();
                String fileName = exporter.getFileName();
                UiExecutor.runOnEdt(() -> {
                    if (success) {
                        JOptionPane.showMessageDialog(instance.getMasterPanel(), "导出成功: " + fileName);
                    } else {
                        JOptionPane.showMessageDialog(instance.getMasterPanel(), "导出失败");
                    }
                    if (dialog != null) {
                        dialog.dispose();
                    }
                });
            });
        });

        instance.getLeakStartBtn().addActionListener(e -> {
            boolean jwtEnabled = jwtBox.isSelected();
            boolean idCardEnabled = idCardBox.isSelected();
            boolean ipAddrEnabled = ipAddrBox.isSelected();
            boolean emailEnabled = emailBox.isSelected();
            boolean urlEnabled = urlBox.isSelected();
            boolean jdbcEnabled = jdbcBox.isSelected();
            boolean filePathEnabled = filePathBox.isSelected();
            boolean macAddrEnabled = macAddrBox.isSelected();
            boolean phoneEnabled = phoneBox.isSelected();
            boolean apiKeyEnabled = apiKeyBox.isSelected();
            boolean bankEnabled = bankBox.isSelected();
            boolean cloudAkSkEnabled = cloudAkSkBox.isSelected();
            boolean cryptoEnabled = cryptoBox.isSelected();
            boolean aiKeyEnabled = aiKeyBox.isSelected();
            boolean passEnabled = passBox.isSelected();

            UiExecutor.runAsync(() -> {
                CoreEngine engine = EngineContext.getEngine();
                if (engine == null || !engine.isEnabled()) {
                    return;
                }
                List<MemberEntity> members = engine.getAllMembersInfo();
                Map<String, String> stringMap = engine.getStringMap();

                Set<LeakResult> results = new LinkedHashSet<>();

                // 配置所有规则
                RuleConfig[] ruleConfigs = {
                        new RuleConfig(jwtEnabled, JWTRule::match, "JWT-TOKEN", "jwt-token"),
                        new RuleConfig(idCardEnabled, IDCardRule::match, "ID-CARD", "id-card"),
                        new RuleConfig(ipAddrEnabled, IPAddressRule::match, "IP-ADDR", "ip-addr"),
                        new RuleConfig(emailEnabled, EmailRule::match, "EMAIL", "email"),
                        new RuleConfig(urlEnabled, UrlRule::match, "URL", "url"),
                        new RuleConfig(jdbcEnabled, JDBCRule::match, "JDBC", "jdbc"),
                        new RuleConfig(filePathEnabled, FilePathRule::match, "FILE-PATH", "file-path"),
                        new RuleConfig(macAddrEnabled, MacAddressRule::match, "MAC-ADDR", "mac-addr"),
                        new RuleConfig(phoneEnabled, PhoneRule::match, "PHONE", "phone"),
                        new RuleConfig(apiKeyEnabled, ApiKeyRule::match, "API-KEY", "api-key"),
                        new RuleConfig(bankEnabled, BankCardRule::match, "BANK-CARD", "bank-card"),
                        new RuleConfig(cloudAkSkEnabled, CloudAKSKRule::match, "CLOUD-AKSK", "cloud-aksk"),
                        new RuleConfig(cryptoEnabled, CryptoKeyRule::match, "CRYPTO-KEY", "crypto-key"),
                        new RuleConfig(aiKeyEnabled, OpenAITokenRule::match, "AI-KEY", "ai-key"),
                        new RuleConfig(passEnabled, PasswordRule::match, "PASSWORD", "password")
                };

                // 处理所有规则
                for (RuleConfig config : ruleConfigs) {
                    processRule(config, members, stringMap, results);
                }

                // 更新UI
                DefaultListModel<LeakResult> model = new DefaultListModel<>();
                for (LeakResult leakResult : results) {
                    model.addElement(leakResult);
                }
                UiExecutor.runOnEdt(() -> leakList.setModel(model));
            });
        });

        instance.getLeakCleanBtn().addActionListener(e -> {
            leakList.setModel(new DefaultListModel<>());
            JOptionPane.showMessageDialog(instance.getMasterPanel(), "clean data finish");
        });
    }

    private static boolean isJdkClass(String className) {
        return CommonFilterUtil.isFilteredClass(className);
    }
}
