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

public class ConfigFile {
    private String jarPath;
    private String dbPath;
    private String tempPath;
    private String dbSize;
    private String totalJar;
    private String totalClass;
    private String totalMethod;
    private String totalEdge;
    private String lang;
    private String theme;
    private String decompileCacheSize;

    // --- MCP (embedded) ---
    private boolean mcpAuth;
    private String mcpToken = "JAR-ANALYZER-MCP-TOKEN";
    private String mcpBind = "0.0.0.0";

    private boolean mcpAuditFastEnabled;
    private int mcpAuditFastPort = 20033;

    private boolean mcpGraphLiteEnabled;
    private int mcpGraphLitePort = 20034;

    private boolean mcpDfsTaintEnabled;
    private int mcpDfsTaintPort = 20035;

    private boolean mcpScaLeakEnabled;
    private int mcpScaLeakPort = 20036;

    private boolean mcpVulRulesEnabled;
    private int mcpVulRulesPort = 20037;

    private boolean mcpReportEnabled;
    private int mcpReportPort = 20081;

    private boolean mcpReportWebEnabled;
    private String mcpReportWebHost = "127.0.0.1";
    private int mcpReportWebPort = 20080;

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getJarPath() {
        return jarPath;
    }

    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }

    public String getDbPath() {
        return dbPath;
    }

    public void setDbPath(String dbPath) {
        this.dbPath = dbPath;
    }

    public String getTempPath() {
        return tempPath;
    }

    public void setTempPath(String tempPath) {
        this.tempPath = tempPath;
    }

    public String getDbSize() {
        return dbSize;
    }

    public void setDbSize(String dbSize) {
        this.dbSize = dbSize;
    }

    public String getTotalJar() {
        return totalJar;
    }

    public void setTotalJar(String totalJar) {
        this.totalJar = totalJar;
    }

    public String getTotalClass() {
        return totalClass;
    }

    public void setTotalClass(String totalClass) {
        this.totalClass = totalClass;
    }

    public String getTotalMethod() {
        return totalMethod;
    }

    public void setTotalMethod(String totalMethod) {
        this.totalMethod = totalMethod;
    }

    public String getTotalEdge() {
        return totalEdge;
    }

    public void setTotalEdge(String totalEdge) {
        this.totalEdge = totalEdge;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getDecompileCacheSize() {
        return decompileCacheSize;
    }

    public void setDecompileCacheSize(String decompileCacheSize) {
        this.decompileCacheSize = decompileCacheSize;
    }

    public boolean isMcpAuth() {
        return mcpAuth;
    }

    public void setMcpAuth(boolean mcpAuth) {
        this.mcpAuth = mcpAuth;
    }

    public String getMcpToken() {
        return mcpToken;
    }

    public void setMcpToken(String mcpToken) {
        if (mcpToken == null) {
            return;
        }
        String v = mcpToken.trim();
        if (!v.isEmpty()) {
            this.mcpToken = v;
        }
    }

    public String getMcpBind() {
        return mcpBind;
    }

    public void setMcpBind(String mcpBind) {
        if (mcpBind == null) {
            return;
        }
        String v = mcpBind.trim();
        if (!v.isEmpty()) {
            this.mcpBind = v;
        }
    }

    public boolean isMcpAuditFastEnabled() {
        return mcpAuditFastEnabled;
    }

    public void setMcpAuditFastEnabled(boolean mcpAuditFastEnabled) {
        this.mcpAuditFastEnabled = mcpAuditFastEnabled;
    }

    public int getMcpAuditFastPort() {
        return mcpAuditFastPort;
    }

    public void setMcpAuditFastPort(int mcpAuditFastPort) {
        this.mcpAuditFastPort = mcpAuditFastPort;
    }

    public boolean isMcpGraphLiteEnabled() {
        return mcpGraphLiteEnabled;
    }

    public void setMcpGraphLiteEnabled(boolean mcpGraphLiteEnabled) {
        this.mcpGraphLiteEnabled = mcpGraphLiteEnabled;
    }

    public int getMcpGraphLitePort() {
        return mcpGraphLitePort;
    }

    public void setMcpGraphLitePort(int mcpGraphLitePort) {
        this.mcpGraphLitePort = mcpGraphLitePort;
    }

    public boolean isMcpDfsTaintEnabled() {
        return mcpDfsTaintEnabled;
    }

    public void setMcpDfsTaintEnabled(boolean mcpDfsTaintEnabled) {
        this.mcpDfsTaintEnabled = mcpDfsTaintEnabled;
    }

    public int getMcpDfsTaintPort() {
        return mcpDfsTaintPort;
    }

    public void setMcpDfsTaintPort(int mcpDfsTaintPort) {
        this.mcpDfsTaintPort = mcpDfsTaintPort;
    }

    public boolean isMcpScaLeakEnabled() {
        return mcpScaLeakEnabled;
    }

    public void setMcpScaLeakEnabled(boolean mcpScaLeakEnabled) {
        this.mcpScaLeakEnabled = mcpScaLeakEnabled;
    }

    public int getMcpScaLeakPort() {
        return mcpScaLeakPort;
    }

    public void setMcpScaLeakPort(int mcpScaLeakPort) {
        this.mcpScaLeakPort = mcpScaLeakPort;
    }

    public boolean isMcpVulRulesEnabled() {
        return mcpVulRulesEnabled;
    }

    public void setMcpVulRulesEnabled(boolean mcpVulRulesEnabled) {
        this.mcpVulRulesEnabled = mcpVulRulesEnabled;
    }

    public int getMcpVulRulesPort() {
        return mcpVulRulesPort;
    }

    public void setMcpVulRulesPort(int mcpVulRulesPort) {
        this.mcpVulRulesPort = mcpVulRulesPort;
    }

    public boolean isMcpReportEnabled() {
        return mcpReportEnabled;
    }

    public void setMcpReportEnabled(boolean mcpReportEnabled) {
        this.mcpReportEnabled = mcpReportEnabled;
    }

    public int getMcpReportPort() {
        return mcpReportPort;
    }

    public void setMcpReportPort(int mcpReportPort) {
        this.mcpReportPort = mcpReportPort;
    }

    public boolean isMcpReportWebEnabled() {
        return mcpReportWebEnabled;
    }

    public void setMcpReportWebEnabled(boolean mcpReportWebEnabled) {
        this.mcpReportWebEnabled = mcpReportWebEnabled;
    }

    public String getMcpReportWebHost() {
        return mcpReportWebHost;
    }

    public void setMcpReportWebHost(String mcpReportWebHost) {
        if (mcpReportWebHost == null) {
            return;
        }
        String v = mcpReportWebHost.trim();
        if (!v.isEmpty()) {
            this.mcpReportWebHost = v;
        }
    }

    public int getMcpReportWebPort() {
        return mcpReportWebPort;
    }

    public void setMcpReportWebPort(int mcpReportWebPort) {
        this.mcpReportWebPort = mcpReportWebPort;
    }

    @Override
    public String toString() {
        return "ConfigFile{" +
                "jarPath='" + jarPath + '\'' +
                ", dbPath='" + dbPath + '\'' +
                ", tempPath='" + tempPath + '\'' +
                ", dbSize='" + dbSize + '\'' +
                ", totalJar='" + totalJar + '\'' +
                ", totalClass='" + totalClass + '\'' +
                ", totalMethod='" + totalMethod + '\'' +
                ", totalEdge='" + totalEdge + '\'' +
                ", lang='" + lang + '\'' +
                ", theme='" + theme + '\'' +
                ", decompileCacheSize='" + decompileCacheSize + '\'' +
                ", mcpAuth=" + mcpAuth +
                ", mcpToken='" + mcpToken + '\'' +
                ", mcpBind='" + mcpBind + '\'' +
                ", mcpAuditFastEnabled=" + mcpAuditFastEnabled +
                ", mcpAuditFastPort=" + mcpAuditFastPort +
                ", mcpGraphLiteEnabled=" + mcpGraphLiteEnabled +
                ", mcpGraphLitePort=" + mcpGraphLitePort +
                ", mcpDfsTaintEnabled=" + mcpDfsTaintEnabled +
                ", mcpDfsTaintPort=" + mcpDfsTaintPort +
                ", mcpScaLeakEnabled=" + mcpScaLeakEnabled +
                ", mcpScaLeakPort=" + mcpScaLeakPort +
                ", mcpVulRulesEnabled=" + mcpVulRulesEnabled +
                ", mcpVulRulesPort=" + mcpVulRulesPort +
                ", mcpReportEnabled=" + mcpReportEnabled +
                ", mcpReportPort=" + mcpReportPort +
                ", mcpReportWebEnabled=" + mcpReportWebEnabled +
                ", mcpReportWebHost='" + mcpReportWebHost + '\'' +
                ", mcpReportWebPort=" + mcpReportWebPort +
                '}';
    }
}
