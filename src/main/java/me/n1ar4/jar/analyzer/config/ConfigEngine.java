/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.config;

import me.n1ar4.jar.analyzer.core.notify.NotifierContext;
import me.n1ar4.jar.analyzer.utils.InterruptUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ConfigEngine {
    private static final Logger logger = LogManager.getLogger();
    public static final String CONFIG_FILE_PATH = ".jar-analyzer";

    private static void setIfPresent(Properties properties, String key, String value) {
        if (properties == null || key == null) {
            return;
        }
        if (value == null) {
            return;
        }
        String v = value.trim();
        if (!v.isEmpty()) {
            properties.setProperty(key, v);
        }
    }

    private static boolean getBool(Properties properties, String key, boolean def) {
        if (properties == null || key == null) {
            return def;
        }
        String v = properties.getProperty(key);
        if (v == null) {
            return def;
        }
        v = v.trim().toLowerCase();
        if (v.isEmpty()) {
            return def;
        }
        return "true".equals(v) || "1".equals(v) || "yes".equals(v);
    }

    private static int getInt(Properties properties, String key, int def) {
        if (properties == null || key == null) {
            return def;
        }
        String v = properties.getProperty(key);
        if (v == null) {
            return def;
        }
        v = v.trim();
        if (v.isEmpty()) {
            return def;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException ignored) {
            return def;
        }
    }

    public static boolean exist() {
        Path configPath = Paths.get(CONFIG_FILE_PATH);
        return Files.exists(configPath);
    }

    public static ConfigFile parseConfig() {
        try {
            Path configPath = Paths.get(CONFIG_FILE_PATH);
            if (!Files.exists(configPath)) {
                return null;
            }

            byte[] data = Files.readAllBytes(configPath);
            if (new String(data, StandardCharsets.UTF_8).contains("!!me.n1ar4.")) {
                String msg = "config file changed in 2.5-beta+; the old config file will be deleted";
                logger.warn(msg);
                try {
                    NotifierContext.get().warn("Jar Analyzer", msg);
                } catch (Throwable t) {
                    InterruptUtil.restoreInterruptIfNeeded(t);
                    if (t instanceof Error) {
                        throw (Error) t;
                    }
                    logger.debug("notifier warn failed: {}", t.toString());
                }
                Files.delete(configPath);
                return null;
            }

            Properties properties = new Properties();
            properties.load(new ByteArrayInputStream(Files.readAllBytes(configPath)));
            ConfigFile obj = new ConfigFile();
            obj.setDbPath(properties.getProperty("db-path"));
            obj.setDbSize(properties.getProperty("db-size"));
            obj.setJarPath(properties.getProperty("jar-path"));
            obj.setTempPath(properties.getProperty("temp-path"));
            obj.setTotalClass(properties.getProperty("total-class"));
            obj.setTotalJar(properties.getProperty("total-jar"));
            obj.setTotalMethod(properties.getProperty("total-method"));
            obj.setTotalEdge(properties.getProperty("total-edge"));
            obj.setLang(properties.getProperty("lang"));
            obj.setTheme(properties.getProperty("theme"));
            obj.setDecompileCacheSize(properties.getProperty("decompile-cache-size"));
            obj.setStripeShowNames(getBool(properties, "stripe-show-names", false));
            obj.setStripeWidth(getInt(properties, "stripe-width", 40));

            // MCP (embedded)
            obj.setMcpBind(properties.getProperty("mcp-bind", "0.0.0.0"));
            obj.setMcpAuth(getBool(properties, "mcp-auth", false));
            obj.setMcpToken(properties.getProperty("mcp-token", "JAR-ANALYZER-MCP-TOKEN"));

            obj.setMcpAuditFastEnabled(getBool(properties, "mcp-audit-fast-enabled", false));
            obj.setMcpAuditFastPort(getInt(properties, "mcp-audit-fast-port", 20033));

            obj.setMcpGraphLiteEnabled(getBool(properties, "mcp-graph-lite-enabled", false));
            obj.setMcpGraphLitePort(getInt(properties, "mcp-graph-lite-port", 20034));

            obj.setMcpDfsTaintEnabled(getBool(properties, "mcp-dfs-taint-enabled", false));
            obj.setMcpDfsTaintPort(getInt(properties, "mcp-dfs-taint-port", 20035));

            obj.setMcpScaLeakEnabled(getBool(properties, "mcp-sca-leak-enabled", false));
            obj.setMcpScaLeakPort(getInt(properties, "mcp-sca-leak-port", 20036));

            obj.setMcpVulRulesEnabled(getBool(properties, "mcp-vul-rules-enabled", false));
            obj.setMcpVulRulesPort(getInt(properties, "mcp-vul-rules-port", 20037));

            obj.setMcpReportEnabled(getBool(properties, "mcp-report-enabled", false));
            obj.setMcpReportPort(getInt(properties, "mcp-report-port", 20081));

            obj.setMcpReportWebEnabled(getBool(properties, "mcp-report-web-enabled", false));
            obj.setMcpReportWebHost(properties.getProperty("mcp-report-web-host", "127.0.0.1"));
            obj.setMcpReportWebPort(getInt(properties, "mcp-report-web-port", 20080));
            return obj;
        } catch (Exception ex) {
            logger.error("parse config error: {}", ex.toString());
        }
        return null;
    }

    public static void saveConfig(ConfigFile configFile) {
        try {
            if (configFile == null) {
                return;
            }
            Path configPath = Paths.get(CONFIG_FILE_PATH);
            Properties properties = new Properties();

            // Core paths/metrics (optional; allow saving partial configs, e.g. MCP-only).
            setIfPresent(properties, "db-path", configFile.getDbPath());
            setIfPresent(properties, "db-size", configFile.getDbSize());
            setIfPresent(properties, "jar-path", configFile.getJarPath());
            setIfPresent(properties, "temp-path", configFile.getTempPath());
            setIfPresent(properties, "total-class", configFile.getTotalClass());
            setIfPresent(properties, "total-jar", configFile.getTotalJar());
            setIfPresent(properties, "total-method", configFile.getTotalMethod());
            setIfPresent(properties, "total-edge", configFile.getTotalEdge());
            setIfPresent(properties, "lang", configFile.getLang());
            setIfPresent(properties, "theme", configFile.getTheme() == null ? "default" : configFile.getTheme());
            setIfPresent(properties, "decompile-cache-size", configFile.getDecompileCacheSize());
            properties.setProperty("stripe-show-names", String.valueOf(configFile.isStripeShowNames()));
            properties.setProperty("stripe-width", String.valueOf(configFile.getStripeWidth()));

            // MCP (embedded)
            setIfPresent(properties, "mcp-bind", configFile.getMcpBind());
            properties.setProperty("mcp-auth", String.valueOf(configFile.isMcpAuth()));
            setIfPresent(properties, "mcp-token", configFile.getMcpToken());

            properties.setProperty("mcp-audit-fast-enabled", String.valueOf(configFile.isMcpAuditFastEnabled()));
            properties.setProperty("mcp-audit-fast-port", String.valueOf(configFile.getMcpAuditFastPort()));

            properties.setProperty("mcp-graph-lite-enabled", String.valueOf(configFile.isMcpGraphLiteEnabled()));
            properties.setProperty("mcp-graph-lite-port", String.valueOf(configFile.getMcpGraphLitePort()));

            properties.setProperty("mcp-dfs-taint-enabled", String.valueOf(configFile.isMcpDfsTaintEnabled()));
            properties.setProperty("mcp-dfs-taint-port", String.valueOf(configFile.getMcpDfsTaintPort()));

            properties.setProperty("mcp-sca-leak-enabled", String.valueOf(configFile.isMcpScaLeakEnabled()));
            properties.setProperty("mcp-sca-leak-port", String.valueOf(configFile.getMcpScaLeakPort()));

            properties.setProperty("mcp-vul-rules-enabled", String.valueOf(configFile.isMcpVulRulesEnabled()));
            properties.setProperty("mcp-vul-rules-port", String.valueOf(configFile.getMcpVulRulesPort()));

            properties.setProperty("mcp-report-enabled", String.valueOf(configFile.isMcpReportEnabled()));
            properties.setProperty("mcp-report-port", String.valueOf(configFile.getMcpReportPort()));

            properties.setProperty("mcp-report-web-enabled", String.valueOf(configFile.isMcpReportWebEnabled()));
            setIfPresent(properties, "mcp-report-web-host", configFile.getMcpReportWebHost());
            properties.setProperty("mcp-report-web-port", String.valueOf(configFile.getMcpReportWebPort()));

            properties.store(Files.newOutputStream(configPath), null);
        } catch (Exception ex) {
            logger.error("save config error: {}", ex.toString());
        }
    }
}
