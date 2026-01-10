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

public class ResourceSearchResult {
    private Integer jarId;
    private String jarName;
    private String resourcePath;
    private long fileSize;
    private int isText;
    private String matchType;
    private String preview;

    public Integer getJarId() {
        return jarId;
    }

    public void setJarId(Integer jarId) {
        this.jarId = jarId;
    }

    public String getJarName() {
        return jarName;
    }

    public void setJarName(String jarName) {
        this.jarName = jarName;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getIsText() {
        return isText;
    }

    public void setIsText(int isText) {
        this.isText = isText;
    }

    public String getMatchType() {
        return matchType;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }

    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }
}
