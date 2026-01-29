/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.util;

import com.github.rjeschke.txtmark.Processor;
import me.n1ar4.games.flappy.FBMainFrame;
import me.n1ar4.games.pocker.Main;
import me.n1ar4.jar.analyzer.config.ConfigEngine;
import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.gui.*;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;
import me.n1ar4.jar.analyzer.os.SystemChart;
import me.n1ar4.jar.analyzer.plugins.jd.JDGUIStarter;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import me.n1ar4.shell.analyzer.form.ShellForm;

import javax.swing.*;
import java.awt.*;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class MenuUtil {
    private static final Logger logger = LogManager.getLogger();
    private static final JCheckBoxMenuItem showInnerConfig = new JCheckBoxMenuItem("show inner class");
    private static final JCheckBoxMenuItem fixClassPathConfig = new JCheckBoxMenuItem("fix class path");
    private static final JCheckBoxMenuItem sortedByMethodConfig = new JCheckBoxMenuItem("sort results by method name");
    private static final JCheckBoxMenuItem sortedByClassConfig = new JCheckBoxMenuItem("sort results by class name");
    private static final JCheckBoxMenuItem logAllSqlConfig = new JCheckBoxMenuItem("save all sql statement");
    private static final JCheckBoxMenuItem chineseConfig = new JCheckBoxMenuItem("Chinese");
    private static final JCheckBoxMenuItem englishConfig = new JCheckBoxMenuItem("English");
    private static final JCheckBoxMenuItem enableFixMethodImplConfig = new JCheckBoxMenuItem(
            "enable fix methods impl/override");
    private static final JCheckBoxMenuItem disableFixMethodImplConfig = new JCheckBoxMenuItem(
            "disable fix methods impl/override");

    private static final JCheckBoxMenuItem themeDarkItem = new JCheckBoxMenuItem("use dark ui");
    private static final JCheckBoxMenuItem themeOrangeItem = new JCheckBoxMenuItem("use orange ui");

    private static boolean isChinese() {
        return GlobalOptions.getLang() == GlobalOptions.CHINESE;
    }

    private static String t(String zh, String en) {
        return isChinese() ? zh : en;
    }

    private static void applyLangToStaticItems() {
        showInnerConfig.setText(t("显示内部类", "show inner class"));
        fixClassPathConfig.setText(t("修复类路径", "fix class path"));
        sortedByMethodConfig.setText(t("按方法名排序", "sort results by method name"));
        sortedByClassConfig.setText(t("按类名排序", "sort results by class name"));
        logAllSqlConfig.setText(t("保存全部 SQL", "save all sql statement"));
        chineseConfig.setText(t("中文", "Chinese"));
        englishConfig.setText(t("英文", "English"));
        enableFixMethodImplConfig.setText(t("启用方法实现/覆盖补全", "enable fix methods impl/override"));
        disableFixMethodImplConfig.setText(t("关闭方法实现/覆盖补全", "disable fix methods impl/override"));
        themeDarkItem.setText(t("深色主题", "use dark ui"));
        themeOrangeItem.setText(t("橙色主题", "use orange ui"));
    }

    public static void refreshMenuBar() {
        JFrame frame = MainForm.getFrame();
        if (frame == null) {
            return;
        }
        frame.setJMenuBar(createMenuBar());
        frame.revalidate();
        frame.repaint();
    }

    public static void setLangFlag() {
        if (GlobalOptions.getLang() == GlobalOptions.CHINESE) {
            chineseConfig.setState(true);
        } else if (GlobalOptions.getLang() == GlobalOptions.ENGLISH) {
            englishConfig.setState(true);
        }
    }

    public static void useDark() {
        themeDarkItem.setState(true);
        themeOrangeItem.setState(false);
        JarAnalyzerLaf.setupDark();
    }

    public static void useOrange() {
        themeDarkItem.setState(false);
        themeOrangeItem.setState(true);
        JarAnalyzerLaf.setupOrange();
    }

    public static void useDefault() {
        themeDarkItem.setState(false);
        themeOrangeItem.setState(false);
        JarAnalyzerLaf.setupLight(false);
    }

    static {
        showInnerConfig.setState(false);
        fixClassPathConfig.setState(false);
        sortedByMethodConfig.setState(false);
        sortedByClassConfig.setState(true);
        logAllSqlConfig.setSelected(false);
        enableFixMethodImplConfig.setSelected(true);

        chineseConfig.addActionListener(e -> {
            chineseConfig.setState(chineseConfig.getState());
            englishConfig.setState(!chineseConfig.getState());
            if (chineseConfig.getState()) {
                logger.info("use chinese language");
                GlobalOptions.setLang(GlobalOptions.CHINESE);
                MainForm.refreshLang(true);
                refreshMenuBar();
                JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                        "已切换到中文");
                ConfigFile cf = MainForm.getConfig();
                if (cf == null) {
                    return;
                }
                cf.setLang("zh");
                MainForm.setConfig(cf);
                ConfigEngine.saveConfig(cf);
            }
        });

        englishConfig.addActionListener(e -> {
            englishConfig.setState(englishConfig.getState());
            chineseConfig.setState(!englishConfig.getState());
            if (englishConfig.getState()) {
                logger.info("use english language");
                GlobalOptions.setLang(GlobalOptions.ENGLISH);
                MainForm.refreshLang(true);
                refreshMenuBar();
                JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                        "use english language");
                ConfigFile cf = MainForm.getConfig();
                if (cf == null) {
                    return;
                }
                cf.setLang("en");
                MainForm.setConfig(cf);
                ConfigEngine.saveConfig(cf);
            }
        });

        sortedByMethodConfig.addActionListener(e -> {
            sortedByMethodConfig.setState(sortedByMethodConfig.getState());
            sortedByClassConfig.setState(!sortedByMethodConfig.getState());
        });

        sortedByClassConfig.addActionListener(e -> {
            sortedByClassConfig.setState(sortedByClassConfig.getState());
            sortedByMethodConfig.setState(!sortedByClassConfig.getState());
        });

        enableFixMethodImplConfig.addActionListener(e -> {
            enableFixMethodImplConfig.setState(enableFixMethodImplConfig.getState());
            disableFixMethodImplConfig.setState(!enableFixMethodImplConfig.getState());
        });

        disableFixMethodImplConfig.addActionListener(e -> {
            disableFixMethodImplConfig.setState(disableFixMethodImplConfig.getState());
            enableFixMethodImplConfig.setState(!disableFixMethodImplConfig.getState());
        });
    }

    public static JCheckBoxMenuItem getShowInnerConfig() {
        return showInnerConfig;
    }

    public static JCheckBoxMenuItem getFixClassPathConfig() {
        return fixClassPathConfig;
    }

    public static JCheckBoxMenuItem getLogAllSqlConfig() {
        return logAllSqlConfig;
    }

    public static boolean isShowInnerEnabled() {
        return getStateOnEdt(showInnerConfig);
    }

    public static boolean isFixClassPathEnabled() {
        return getStateOnEdt(fixClassPathConfig);
    }

    public static boolean isLogAllSqlEnabled() {
        return getStateOnEdt(logAllSqlConfig);
    }

    public static boolean sortedByMethod() {
        return getStateOnEdt(sortedByMethodConfig);
    }

    public static boolean sortedByClass() {
        return getStateOnEdt(sortedByClassConfig);
    }

    public static boolean enableFixMethodImpl() {
        return getStateOnEdt(enableFixMethodImplConfig);
    }

    public static boolean disableFixMethodImpl() {
        return getStateOnEdt(disableFixMethodImplConfig);
    }

    private static boolean getStateOnEdt(JCheckBoxMenuItem item) {
        if (item == null) {
            return false;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            return item.getState();
        }
        AtomicBoolean state = new AtomicBoolean(false);
        try {
            SwingUtilities.invokeAndWait(() -> state.set(item.getState()));
        } catch (Exception ignored) {
            return item.getState();
        }
        return state.get();
    }

    public static JMenuBar createMenuBar() {
        applyLangToStaticItems();
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createAboutMenu());
        menuBar.add(createConfigMenu());
        menuBar.add(language());
        menuBar.add(loadRemote());
        menuBar.add(exportJava());
        menuBar.add(createGames());
        menuBar.add(createTheme());
        JMenu plugins = new JMenu(t("插件", "plugins"));
        JMenuItem systemItem = new JMenuItem(t("系统信息", "system info"));
        systemItem.setIcon(IconManager.systemIcon);
        systemItem.addActionListener(e -> SystemChart.start0());
        plugins.add(systemItem);
        JMenuItem luceneItem = new JMenuItem(t("全局搜索", "global search"));
        luceneItem.setIcon(IconManager.luceneIcon);
        luceneItem.addActionListener(e -> LuceneSearchForm.start(1));
        plugins.add(luceneItem);
        JMenuItem jdItem = new JMenuItem(t("启动 JD-GUI", "start jd-gui"));
        jdItem.setIcon(IconManager.jdIcon);
        jdItem.addActionListener(e -> JDGUIStarter.start());
        plugins.add(jdItem);
        menuBar.add(plugins);
        return menuBar;
    }

    private static JMenu createTheme() {
        JMenu theme = new JMenu(t("主题", "theme"));
        themeDarkItem.addActionListener(e -> {
            ConfigFile cf;
            if (themeDarkItem.getState()) {
                themeOrangeItem.setState(false);
                JarAnalyzerLaf.setupDark();
                cf = MainForm.getConfig();
                if (cf == null) {
                    return;
                }
                cf.setTheme("dark");
            } else {
                JarAnalyzerLaf.setupLight(false);
                cf = MainForm.getConfig();
                if (cf == null) {
                    return;
                }
                cf.setTheme("default");
            }
            MainForm.setConfig(cf);
            ConfigEngine.saveConfig(cf);
        });
        themeOrangeItem.addActionListener(e -> {
            ConfigFile cf;
            if (themeOrangeItem.getState()) {
                themeDarkItem.setState(false);
                JarAnalyzerLaf.setupOrange();
                cf = MainForm.getConfig();
                if (cf == null) {
                    return;
                }
                cf.setTheme("orange");
            } else {
                JarAnalyzerLaf.setupLight(false);
                cf = MainForm.getConfig();
                if (cf == null) {
                    return;
                }
                cf.setTheme("default");
            }
            MainForm.setConfig(cf);
            ConfigEngine.saveConfig(cf);
        });
        theme.add(themeDarkItem);
        theme.add(themeOrangeItem);
        return theme;
    }

    private static JMenu exportJava() {
        JMenu export = new JMenu(t("导出", "export"));
        JMenuItem proxyItem = new JMenuItem(t("反编译并导出", "decompile and export"));
        proxyItem.setIcon(IconManager.engineIcon);
        proxyItem.addActionListener(e -> ExportForm.start());
        export.add(proxyItem);
        return export;
    }

    private static JMenu loadRemote() {
        JMenu loadRemote = new JMenu(t("远程", "remote"));
        JMenuItem loadByHttp = new JMenuItem(t("HTTP 加载 JAR", "load jars (http)"));
        loadByHttp.setIcon(IconManager.remoteIcon);
        loadByHttp.addActionListener(e -> RemoteHttp.start());
        loadRemote.add(loadByHttp);
        JMenuItem start = new JMenuItem(t("启动 Tomcat 分析", "start tomcat analyzer"));
        start.setIcon(IconManager.tomcatIcon);
        start.addActionListener(e -> ShellForm.start0());
        loadRemote.add(start);
        JMenuItem dbgItem = new JMenuItem(t("打开字节码调试器", "open bytecode debugger"));
        dbgItem.setIcon(IconManager.debugIcon);
        dbgItem.addActionListener(e -> me.n1ar4.dbg.gui.MainForm.start());
        loadRemote.add(dbgItem);
        JMenuItem proxyItem = new JMenuItem(t("打开代理配置", "open proxy config"));
        proxyItem.setIcon(IconManager.proxyIcon);
        proxyItem.addActionListener(e -> ProxyForm.start());
        loadRemote.add(proxyItem);
        return loadRemote;
    }

    private static JMenu createGames() {
        try {
            JMenu gameMenu = new JMenu(t("游戏", "games"));
            JMenuItem flappyItem = new JMenuItem("Flappy Bird");
            URL iconUrl = MainForm.class.getClassLoader().getResource(
                    "game/flappy/flappy_bird/bird1_0.png");
            if (iconUrl == null) {
                return null;
            }
            ImageIcon flappyIcon = new ImageIcon(iconUrl);
            flappyItem.setIcon(flappyIcon);
            flappyItem.addActionListener(e -> new FBMainFrame().startGame());
            JMenuItem pokerItem = new JMenuItem("斗地主");
            iconUrl = MainForm.class.getClassLoader().getResource(
                    "game/pocker/images/logo.png");
            if (iconUrl == null) {
                return null;
            }
            ImageIcon pokerIcon = new ImageIcon(iconUrl);
            pokerItem.setIcon(pokerIcon);
            pokerItem.addActionListener(e -> new Thread(Main::new).start());

            gameMenu.add(flappyItem);
            gameMenu.add(pokerItem);
            return gameMenu;
        } catch (Exception ex) {
            logger.error("error: {}", ex.toString());
        }
        return null;
    }

    private static JMenu language() {
        try {
            JMenu configMenu = new JMenu(t("语言", "language"));
            configMenu.add(chineseConfig);
            configMenu.add(englishConfig);
            return configMenu;
        } catch (Exception ex) {
            logger.error("error: {}", ex.toString());
        }
        return null;
    }

    private static JMenu createConfigMenu() {
        try {
            JMenu configMenu = new JMenu(t("配置", "config"));
            configMenu.add(showInnerConfig);
            configMenu.add(fixClassPathConfig);
            configMenu.add(sortedByMethodConfig);
            configMenu.add(sortedByClassConfig);
            configMenu.add(enableFixMethodImplConfig);
            configMenu.add(disableFixMethodImplConfig);
            configMenu.add(logAllSqlConfig);
            JMenuItem partitionConfig = new JMenuItem(t("分区配置", "partition config"));
            partitionConfig.setIcon(IconManager.javaIcon);
            partitionConfig.addActionListener(e -> PartForm.start());
            configMenu.add(partitionConfig);
            return configMenu;
        } catch (Exception ex) {
            logger.error("error: {}", ex.toString());
        }
        return null;
    }

    @SuppressWarnings("all")
    private static JMenu createAboutMenu() {
        try {
            JMenu aboutMenu = new JMenu(t("帮助", "help"));

            // QUICK START
            JMenuItem docsItem = new JMenuItem(t("官方文档", "docs"));
            docsItem.setIcon(IconManager.ausIcon);
            docsItem.addActionListener(e -> {
                UiExecutor.runAsync(() -> {
                    try {
                        Desktop desktop = Desktop.getDesktop();
                        URI oURL = new URI(Const.docsUrl);
                        desktop.browse(oURL);
                    } catch (Exception ex) {
                        logger.error("error: {}", ex.toString());
                    }
                });
            });
            aboutMenu.add(docsItem);

            JMenuItem bugItem = new JMenuItem(t("报告问题", "report bug"));
            URL iconUrl = MainForm.class.getClassLoader().getResource("img/issue.png");
            if (iconUrl == null) {
                return null;
            }
            ImageIcon imageIcon = new ImageIcon(iconUrl);
            bugItem.setIcon(imageIcon);
            aboutMenu.add(bugItem);
            bugItem.addActionListener(e -> {
                UiExecutor.runAsync(() -> {
                    try {
                        Desktop desktop = Desktop.getDesktop();
                        URI oURL = new URI(Const.newIssueUrl);
                        desktop.browse(oURL);
                    } catch (Exception ex) {
                        logger.error("error: {}", ex.toString());
                    }
                });
            });

            JMenuItem projectItem = new JMenuItem(t("项目主页", "project"));
            iconUrl = MainForm.class.getClassLoader().getResource("img/address.png");
            if (iconUrl == null) {
                return null;
            }
            imageIcon = new ImageIcon(iconUrl);
            projectItem.setIcon(imageIcon);
            aboutMenu.add(projectItem);
            projectItem.addActionListener(e -> {
                UiExecutor.runAsync(() -> {
                    try {
                        Desktop desktop = Desktop.getDesktop();
                        URI oURL = new URI(Const.projectUrl);
                        desktop.browse(oURL);
                    } catch (Exception ex) {
                        logger.error("error: {}", ex.toString());
                    }
                });
            });
            JMenuItem jarItem = new JMenuItem("version: " + Const.version);
            iconUrl = MainForm.class.getClassLoader().getResource("img/ver.png");
            if (iconUrl == null) {
                return null;
            }
            imageIcon = new ImageIcon(iconUrl);
            jarItem.setIcon(imageIcon);
            aboutMenu.add(jarItem);
            JMenuItem changelogItem = new JMenuItem(t("更新日志", "changelogs"));
            iconUrl = MainForm.class.getClassLoader().getResource("img/update.png");
            if (iconUrl == null) {
                return null;
            }
            imageIcon = new ImageIcon(iconUrl);
            changelogItem.setIcon(imageIcon);
            changelogItem.addActionListener(e -> {
                UiExecutor.runAsync(() -> {
                    try (java.io.InputStream i = MenuUtil.class.getClassLoader().getResourceAsStream("CHANGELOG.MD")) {
                        if (i == null) {
                            return;
                        }
                        int bufferSize = 1024;
                        char[] buffer = new char[bufferSize];
                        StringBuilder out = new StringBuilder();
                        Reader in = new InputStreamReader(i, StandardCharsets.UTF_8);
                        for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0; ) {
                            out.append(buffer, 0, numRead);
                        }
                        String html = Processor.process(out.toString());
                        UiExecutor.runOnEdt(() -> ChangeLogForm.start(Const.ChangeLogForm, html));
                    } catch (Exception ex) {
                        logger.error("error: {}", ex.toString());
                    }
                });
            });
            aboutMenu.add(changelogItem);
            JMenuItem thanksItem = new JMenuItem(t("致谢", "thanks"));
            iconUrl = MainForm.class.getClassLoader().getResource("img/github.png");
            if (iconUrl == null) {
                return null;
            }
            imageIcon = new ImageIcon(iconUrl);
            thanksItem.setIcon(imageIcon);
            thanksItem.addActionListener(e -> {
                UiExecutor.runAsync(() -> {
                    try (java.io.InputStream i = MenuUtil.class.getClassLoader().getResourceAsStream("thanks.md")) {
                        if (i == null) {
                            return;
                        }
                        int bufferSize = 1024;
                        char[] buffer = new char[bufferSize];
                        StringBuilder out = new StringBuilder();
                        Reader in = new InputStreamReader(i, StandardCharsets.UTF_8);
                        for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0; ) {
                            out.append(buffer, 0, numRead);
                        }
                        String html = Processor.process(out.toString());
                        UiExecutor.runOnEdt(() -> ChangeLogForm.start("THANKS", html));
                    } catch (Exception ex) {
                        logger.error("error: {}", ex.toString());
                    }
                });
            });
            aboutMenu.add(thanksItem);
            return aboutMenu;
        } catch (Exception ex) {
            return null;
        }
    }
}
