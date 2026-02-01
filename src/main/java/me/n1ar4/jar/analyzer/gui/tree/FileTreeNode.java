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

public class FileTreeNode {
    public final File file;
    private final String displayName;

    public FileTreeNode(File file) {
        this(file, null);
    }

    public FileTreeNode(File file, String displayName) {
        if (file == null) {
            throw new IllegalArgumentException("null file not allowed");
        }
        String name = displayName == null ? null : displayName.trim();
        this.displayName = (name == null || name.isEmpty()) ? null : name;
        this.file = file;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String toString() {
        return file.getName();
    }
}
