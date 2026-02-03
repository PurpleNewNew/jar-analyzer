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
import me.n1ar4.jar.analyzer.utils.CommonFilterUtil;

import java.io.File;
import java.util.Locale;

public class TreeFileFilter {
    private static final String INNER = "$";
    private static final String DECOMPILE_DIR = "jar-analyzer-decompile";
    private static final String CONSOLE_DLL = "console.dll";
    private static final String MODULE_INFO = "module-info.class";
    private static final String META_INF = "META-INF";
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
        if (file.getName().equals(CONSOLE_DLL)) {
            return true;
        }
        if (file.isFile() && file.getName().equalsIgnoreCase(MODULE_INFO)) {
            return true;
        }
        if (file.isFile() && !file.getName().toLowerCase().endsWith(".class")) {
            if (CommonFilterUtil.isFilteredResourcePath(file.getPath())) {
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
