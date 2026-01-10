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

public class ResourceEntity {
    private int rid;
    private String resourcePath;
    private String pathStr;
    private String jarName;
    private Integer jarId;
    private long fileSize;
    private int isText;

    public int getRid() {
        return rid;
    }

    public void setRid(int rid) {
        this.rid = rid;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String getPathStr() {
        return pathStr;
    }

    public void setPathStr(String pathStr) {
        this.pathStr = pathStr;
    }

    public String getJarName() {
        return jarName;
    }

    public void setJarName(String jarName) {
        this.jarName = jarName;
    }

    public Integer getJarId() {
        return jarId;
    }

    public void setJarId(Integer jarId) {
        this.jarId = jarId;
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

    @Override
    public String toString() {
        return "ResourceEntity{" +
                "rid=" + rid +
                ", resourcePath='" + resourcePath + '\'' +
                ", pathStr='" + pathStr + '\'' +
                ", jarName='" + jarName + '\'' +
                ", jarId=" + jarId +
                ", fileSize=" + fileSize +
                ", isText=" + isText +
                '}';
    }
}
