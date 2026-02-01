/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.entity;

import me.n1ar4.jar.analyzer.utils.JarUtil;

public class LuceneSearchResult {
    // 反编译后的代码的文件内容
    public static final int TYPE_CONTENT = 0xf0;
    // 类名：也可以理解成文件名 是一个东西
    public static final int TYPE_CLASS_NAME = 0xf1;

    private int type;
    private String absPathStr;
    private String fileName;
    private String contentStr;
    private String title;
    private String searchKey;

    public String getSearchKey() {
        return searchKey;
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getAbsPathStr() {
        return absPathStr;
    }

    public void setAbsPathStr(String absPathStr) {
        this.absPathStr = absPathStr;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentStr() {
        return contentStr;
    }

    public void setContentStr(String contentStr) {
        this.contentStr = contentStr;
    }

    public String getClassName() {
        String className = JarUtil.resolveClassNameFromPath(this.getAbsPathStr(), true);
        if (className == null || className.trim().isEmpty()) {
            if (fileName != null && !fileName.trim().isEmpty()) {
                return fileName;
            }
            String path = this.getAbsPathStr();
            return path == null ? "" : path;
        }
        return className;
    }

    @Override
    public String toString() {
        return "LuceneSearchResult{" +
                "type=" + type +
                ", absPathStr='" + absPathStr + '\'' +
                ", fileName='" + fileName + '\'' +
                ", contentStr='" + contentStr + '\'' +
                '}';
    }
}
