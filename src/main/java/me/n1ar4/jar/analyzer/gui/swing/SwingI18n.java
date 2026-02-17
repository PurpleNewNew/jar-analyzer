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

import com.formdev.flatlaf.extras.FlatSVGIcon;
import me.n1ar4.jar.analyzer.gui.GlobalOptions;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;

public final class SwingI18n {
    private static final Map<String, Pair> DICT = new HashMap<>();
    private static final Icon BROWSE_ICON = loadBrowseIcon();
    private static volatile String currentLanguage = "";

    static {
        add("构建设置", "Build Settings");
        add("构建快照", "Build Snapshot");
        add("构建过滤", "Build Filters");
        add("构建进度", "progress");
        add("状态", "Status");
        add("状态", "status");
        add("编辑构建黑名单", "Edit Build Blacklist");
        add("编辑构建白名单", "Edit Build Whitelist");
        add("作者", "Authors");
        add("类 / 包 列表", "Class / Package List");
        add("Jar 前缀列表", "Jar Prefix List");
        add("保存", "Save");
        add("取消", "Cancel");
        add("输入", "input");
        add("运行时", "runtime");
        add("引擎", "engine");
        add("Jar 数", "jar");
        add("类数", "class");
        add("方法数", "method");
        add("边数", "edge");
        add("数据库", "db");
        add("应用", "Apply");
        add("开始构建", "Start Build");
        add("清理缓存", "Clear Cache");
        add("解析嵌套 Jar", "resolve nested jars");
        add("自动查找运行时 Jar", "auto find runtime jar");
        add("附加运行时 Jar", "add runtime jar");
        add("构建前删除临时目录", "delete temp before build");
        add("修复类路径", "fix class path");
        add("修复方法实现", "fix method impl");
        add("快速模式", "quick mode");
        add("查询条件", "Search Query");
        add("方法调用", "method call");
        add("方法定义", "method definition");
        add("字符串包含", "string contains");
        add("二进制包含", "binary contains");
        add("关键字", "keyword");
        add("过滤空参数方法", "exclude null-parameter method");
        add("匹配", "match");
        add("模糊", "like");
        add("精确", "equals");
        add("结果", "Results");
        add("应用", "Apply");
        add("开始", "Start");
        add("打开", "Open");
        add("当前上下文", "Current Context");
        add("刷新", "Refresh");
        add("打开全部", "Open All");
        add("打开调用者", "Open Caller");
        add("打开被调用", "Open Callee");
        add("全部方法", "all methods");
        add("调用者", "caller");
        add("被调用", "callee");
        add("打开实现", "Open Impl");
        add("打开父实现", "Open Super");
        add("实现方法", "Impl Methods");
        add("父实现方法", "Super Impl Methods");
        add("路径搜索", "Web Path Search");
        add("搜索", "Search");
        add("刷新全部", "Refresh All");
        add("控制器", "controller");
        add("映射", "mapping");
        add("拦截器", "interceptor");
        add("Servlet", "servlet");
        add("过滤器", "filter");
        add("监听器", "listener");
        add("历史", "history");
        add("收藏", "favorites");
        add("加载", "Load");
        add("清空历史", "Clear History");
        add("清空收藏", "Clear Fav");
        add("打开历史", "Open History");
        add("打开收藏", "Open Fav");
        add("模块", "Modules");
        add("输入 / 输出", "Input / Output");
        add("输出模式", "output mode");
        add("控制台", "Console");
        add("打开结果", "Open Result");
        add("SCA 日志", "SCA Log");
        add("泄露规则", "Leak Rules");
        add("配置", "Config");
        add("文件路径", "File Path");
        add("手机号", "Phone");
        add("身份证", "ID Card");
        add("银行卡", "Bank Card");
        add("云密钥", "Cloud AK/SK");
        add("加密密钥", "Crypto Key");
        add("AI 密钥", "AI Key");
        add("密码", "Password");
        add("检测 Base64", "Detect Base64");
        add("清空", "Clear");
        add("导出", "Export");
        add("泄露日志", "Leak Log");
        add("输入", "Input");
        add("依赖目录", "dependency dir");
        add("结果", "Results");
        add("编号", "ID");
        add("定义", "Definition");
        add("风险", "Risk");
        add("分析模式", "Analyze Mode");
        add("从 Sink 开始", "from sink");
        add("从 Source 开始", "from source");
        add("空 Source 列表", "null source list");
        add("指定 Source", "specified source");
        add("Source 仅来自 spring/servlet", "source only from spring/servlet");
        add("Sink", "Sink");
        add("Source", "Source");
        add("描述", "desc");
        add("最大深度", "max depth");
        add("最大结果数", "max results");
        add("最小边置信度", "min edge confidence");
        add("污点种子", "Taint Seed");
        add("种子参数序号", "seed param index");
        add("黑名单（';' 分隔）", "Blacklist (split by ';')");
        add("开始 DFS", "Start DFS");
        add("开始 Taint", "Start Taint");
        add("查看 DFS", "View DFS");
        add("查看 Taint", "View Taint");
        add("高级", "Advanced");
        add("提示", "Hint");
        add("在 search/note 中右键可将方法设置为 source/sink。", "Right click in search/note to send source or sink if needed.");
        add("汇总模式", "summary");
        add("启用污点校验", "taint verify");
        add("显示边元信息", "show edge meta");
        add("种子严格模式", "seed strict");
        add("API 服务", "API Server");
        add("绑定地址", "bind");
        add("端口", "port");
        add("鉴权", "auth");
        add("令牌", "token");
        add("MCP 配置", "MCP Config");
        add("启用鉴权", "auth enabled");
        add("启用报告 Web", "report web enabled");
        add("报告 Web", "report web");
        add("报告主机", "report host");
        add("报告端口 / 运行", "report port / running");
        add("MCP 线路", "MCP Lines");
        add("启用", "enabled");
        add("运行中", "running");
        add("应用并重启", "Apply & Restart");
        add("启动已配置线路", "Start Configured");
        add("停止全部", "Stop All");
        add("API 文档", "API Doc");
        add("MCP 文档", "MCP Doc");
        add("N8N 文档", "N8N Doc");
        add("打开报告页", "Open Report Web");
        add("MCP 全部已停止", "MCP all stopped");
        add("插件 / 工具", "Plugins / Tools");
        add("全局搜索", "Global Search");
        add("全部字符串", "All Strings");
        add("SQL 控制台", "SQL Console");
        add("编码", "Encode");
        add("Socket 监听", "Socket Listener");
        add("EL 搜索", "EL Search");
        add("序列化", "Serialization");
        add("混淆分析", "Obfuscation");
        add("远程加载", "Remote Load");
        add("代理", "Proxy");
        add("分区", "Partition");
        add("系统监控", "System Monitor");
        add("远程 Tomcat", "Remote Tomcat");
        add("字节码调试", "Bytecode Debugger");
        add("分析", "Analysis");
        add("完整 Frame", "Full Frame");
        add("操作码", "Opcode");
        add("HTML 图", "HTML Graph");
        add("文档", "Docs");
        add("动作", "Actions");
        add("版本", "Version");
        add("更新日志", "Changelog");
        add("致谢", "Thanks");
        add("问题反馈", "Report Bug");
        add("项目主页", "Project Site");
        add("运行时配置", "Runtime Config");
        add("显示内部类", "show inner class");
        add("按方法名排序", "search sort by method");
        add("按类名排序", "search sort by class");
        add("记录全部 SQL", "log all sql");
        add("按 Jar 分组树", "group tree by jar");
        add("合并包根", "merge package root");
        add("侧栏显示名称", "stripe show names");
        add("侧栏宽度", "stripe width");
        add("语言 / 主题", "Language / Theme");
        add("默认主题", "Theme Default");
        add("深色主题", "Theme Dark");
        add("橙色主题", "Theme Orange");
        add("就绪", "ready");
    }

    private SwingI18n() {
    }

    public static boolean isEnglish() {
        return "en".equals(resolveLanguage());
    }

    public static String tr(String zh, String en) {
        return isEnglish() ? safe(en) : safe(zh);
    }

    public static void setLanguage(String language) {
        currentLanguage = normalizeLanguage(language);
    }

    public static void setupBrowseButton(JButton button, JTextField textField, String zhTip, String enTip) {
        if (button == null || textField == null) {
            return;
        }
        button.setFocusable(false);
        button.setMargin(new Insets(0, 4, 0, 4));
        button.setText("");
        if (BROWSE_ICON != null) {
            button.setIcon(BROWSE_ICON);
        } else {
            button.setText("...");
        }
        int h = Math.max(22, textField.getPreferredSize().height);
        Dimension size = new Dimension(24, h);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        button.setToolTipText(tr(zhTip, enTip));
    }

    public static void localizeComponentTree(Component root) {
        if (root == null) {
            return;
        }
        translateComponent(root);
    }

    private static void translateComponent(Component component) {
        if (component == null) {
            return;
        }
        if (component instanceof AbstractButton button) {
            button.setText(translate(button.getText()));
        } else if (component instanceof JLabel label) {
            label.setText(translate(label.getText()));
        }
        if (component instanceof JTabbedPane tabs) {
            for (int i = 0; i < tabs.getTabCount(); i++) {
                tabs.setTitleAt(i, translate(tabs.getTitleAt(i)));
            }
        } else if (component instanceof JTable table) {
            TableColumnModel columnModel = table.getColumnModel();
            for (int i = 0; i < columnModel.getColumnCount(); i++) {
                TableColumn column = columnModel.getColumn(i);
                Object header = column.getHeaderValue();
                if (header != null) {
                    column.setHeaderValue(translate(String.valueOf(header)));
                }
            }
            if (table.getTableHeader() != null) {
                table.getTableHeader().repaint();
            }
        }
        if (component instanceof JComponent jComponent) {
            translateBorder(jComponent.getBorder());
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                translateComponent(child);
            }
        }
    }

    private static void translateBorder(Border border) {
        if (border instanceof TitledBorder titled) {
            titled.setTitle(translate(titled.getTitle()));
        }
    }

    private static String translate(String text) {
        String value = safe(text);
        if (value.isBlank()) {
            return value;
        }
        Pair pair = DICT.get(value);
        if (pair == null) {
            return value;
        }
        return isEnglish() ? pair.en() : pair.zh();
    }

    private static void add(String zh, String en) {
        if (zh == null || en == null) {
            return;
        }
        Pair pair = DICT.get(zh);
        if (pair == null) {
            pair = new Pair(zh, en);
            DICT.put(zh, pair);
        }
        DICT.put(en, pair);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String resolveLanguage() {
        String local = normalizeLanguage(currentLanguage);
        if (!local.isEmpty()) {
            return local;
        }
        return GlobalOptions.getLang() == GlobalOptions.ENGLISH ? "en" : "zh";
    }

    private static String normalizeLanguage(String language) {
        if ("en".equalsIgnoreCase(safe(language))) {
            return "en";
        }
        if ("zh".equalsIgnoreCase(safe(language))) {
            return "zh";
        }
        return "";
    }

    private static Icon loadBrowseIcon() {
        try {
            return new FlatSVGIcon("icons/jadx/openDisk.svg", 14, 14);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private record Pair(String zh, String en) {
    }
}
