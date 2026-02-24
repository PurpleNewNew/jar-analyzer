/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.tree;

import me.n1ar4.jar.analyzer.gui.util.MenuUtil;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.CommonFilterUtil;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class TreeFileFilter {
    private static final String INNER = "$";
    private static final String DECOMPILE_DIR = "jar-analyzer-decompile";
    private static final String CLASSPATH_NESTED_DIR = "classpath-nested";
    private static final String CONSOLE_DLL = "console.dll";
    private static final String MODULE_INFO = "module-info.class";
    private static final String META_INF = "META-INF";
    private static final Set<String> CONFIG_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".properties", ".yml", ".yaml", ".xml", ".json", ".ini", ".cfg", ".conf", ".toml"
    ));
    private static final Set<String> RESOURCE_NOISE_DIRS = new HashSet<>(Arrays.asList(
            "static", "public", "templates", "webjars", "assets", "i18n"
    ));
    private static final String[] CONFIG_NAME_HINTS = new String[]{
            "application", "bootstrap", "mybatis", "spring", "logback", "log4j",
            "dubbo", "nacos", "apollo", "datasource", "jdbc", "redis",
            "rabbit", "kafka", "security", "shiro", "sa-token", "jwt",
            "quartz", "xxl-job", "flowable", "camunda"
    };
    private final File file;
    private final boolean showFiles;
    private final boolean showHiddenFiles;

    public TreeFileFilter(File file,
                          boolean showFiles,
                          boolean showHiddenFiles) {
        this.file = file;
        this.showFiles = showFiles;
        this.showHiddenFiles = showHiddenFiles;
    }

    public static TreeFileFilter defaults(File file) {
        return new TreeFileFilter(file, true, true);
    }

    @SuppressWarnings("all")
    public boolean shouldFilter() {
        String path = file.getPath().replace('\\', '/').toLowerCase(Locale.ROOT);
        if (isMetaInfOnlyDir(file)) {
            return true;
        }
        if (path.contains("/boot-inf/lib") || path.contains("/web-inf/lib")) {
            return true;
        }
        boolean showInner = MenuUtil.isShowInnerEnabled();
        if (!showInner && file.getName().contains(INNER)) {
            return true;
        }
        if (file.isFile() && !showFiles) {
            return true;
        }
        if (!showHiddenFiles && file.isHidden()) {
            return true;
        }
        if (file.getName().equals(DECOMPILE_DIR)) {
            return true;
        }
        if (file.getName().equals(CLASSPATH_NESTED_DIR)) {
            return true;
        }
        if (file.getName().equals(CONSOLE_DLL)) {
            return true;
        }
        if (file.isFile() && file.getName().equalsIgnoreCase(MODULE_INFO)) {
            return true;
        }
        if (file.isDirectory() && isResourceNoiseDirectory(path)) {
            return true;
        }
        if (file.isFile() && !file.getName().toLowerCase().endsWith(".class")) {
            if (CommonFilterUtil.isFilteredResourcePath(file.getPath())) {
                return true;
            }
            if (isResourcesTreePath(path) && !isBusinessConfigFile(path, file.getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isResourcesTreePath(String lowerPath) {
        String marker = buildResourceTreeMarker();
        return lowerPath.startsWith(marker) || lowerPath.contains("/" + marker);
    }

    private String extractResourceRelativePath(String lowerPath) {
        if (lowerPath == null || lowerPath.isEmpty()) {
            return "";
        }
        String marker = buildResourceTreeMarker();
        int idx = lowerPath.indexOf(marker);
        if (idx < 0) {
            idx = lowerPath.indexOf("/" + marker);
            if (idx < 0) {
                return "";
            }
            idx += 1;
        }
        int start = idx + marker.length();
        if (start >= lowerPath.length()) {
            return "";
        }
        return lowerPath.substring(start);
    }

    private String buildResourceTreeMarker() {
        return (Const.tempDir + "/" + Const.resourceDir + "/").toLowerCase(Locale.ROOT);
    }

    private boolean isResourceNoiseDirectory(String lowerPath) {
        if (!isResourcesTreePath(lowerPath)) {
            return false;
        }
        String relative = extractResourceRelativePath(lowerPath);
        if (relative.isEmpty()) {
            return false;
        }
        String[] parts = relative.split("/");
        if (parts.length <= 1) {
            // resources root or jar root: keep
            return false;
        }
        String name = file.getName() == null ? "" : file.getName().toLowerCase(Locale.ROOT);
        if (name.isEmpty()) {
            return false;
        }
        return RESOURCE_NOISE_DIRS.contains(name);
    }

    private boolean isBusinessConfigFile(String lowerPath, String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }
        String name = fileName.toLowerCase(Locale.ROOT);
        if (isConfigExtension(name)) {
            return true;
        }
        if (!hasConfigNameHint(name)) {
            return false;
        }
        String relative = extractResourceRelativePath(lowerPath);
        if (relative.isEmpty()) {
            return true;
        }
        return relative.contains("/config/")
                || relative.contains("/configs/")
                || relative.contains("/mapper/")
                || relative.contains("/mappers/")
                || relative.contains("/mybatis/");
    }

    private boolean isConfigExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        for (String ext : CONFIG_EXTENSIONS) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasConfigNameHint(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        for (String hint : CONFIG_NAME_HINTS) {
            if (hint == null || hint.isEmpty()) {
                continue;
            }
            if (fileName.startsWith(hint) || fileName.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMetaInfOnlyDir(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return false;
        }
        File[] children = dir.listFiles();
        if (children == null || children.length == 0) {
            return false;
        }
        boolean hasMetaInf = false;
        for (File child : children) {
            if (child == null) {
                continue;
            }
            String name = child.getName();
            if (name.equalsIgnoreCase(META_INF)) {
                hasMetaInf = true;
                continue;
            }
            if (child.isHidden()) {
                continue;
            }
            return false;
        }
        return hasMetaInf;
    }
}
