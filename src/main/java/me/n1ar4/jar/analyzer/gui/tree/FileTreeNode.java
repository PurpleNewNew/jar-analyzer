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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileTreeNode {
    public final File file;
    private final String displayName;
    private final List<File> virtualDirs;
    private final String virtualName;

    public FileTreeNode(File file) {
        this(file, null);
    }

    public FileTreeNode(File file, String displayName) {
        this(file, displayName, null, null);
    }

    private FileTreeNode(File file, String displayName, List<File> virtualDirs, String virtualName) {
        if (file == null && (virtualDirs == null || virtualDirs.isEmpty())) {
            throw new IllegalArgumentException("null file not allowed");
        }
        String name = displayName == null ? null : displayName.trim();
        this.displayName = (name == null || name.isEmpty()) ? null : name;
        this.virtualDirs = virtualDirs == null ? null : new ArrayList<>(virtualDirs);
        this.virtualName = virtualName == null ? null : virtualName.trim();
        this.file = file == null && virtualDirs != null && !virtualDirs.isEmpty()
                ? virtualDirs.get(0)
                : file;
    }

    public static FileTreeNode virtualDir(String name, List<File> dirs) {
        String trimmed = name == null ? null : name.trim();
        List<File> safeDirs = dirs == null ? Collections.emptyList() : dirs;
        File primary = safeDirs.isEmpty() ? null : safeDirs.get(0);
        return new FileTreeNode(primary, trimmed, safeDirs, trimmed);
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isVirtualDir() {
        return virtualDirs != null && !virtualDirs.isEmpty();
    }

    public List<File> getVirtualDirs() {
        if (virtualDirs == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(virtualDirs);
    }

    public String toString() {
        if (virtualName != null && !virtualName.isEmpty()) {
            return virtualName;
        }
        return file.getName();
    }
}
