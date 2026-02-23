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

public class JarEntity {
    private int jid;
    private String jarName;
    private String jarAbsPath;
    private String jarSha256;
    private String jarRole;
    private int depth;

    public int getJid() {
        return jid;
    }

    public void setJid(int jid) {
        this.jid = jid;
    }

    public String getJarName() {
        return jarName;
    }

    public void setJarName(String jarName) {
        this.jarName = jarName;
    }

    public String getJarAbsPath() {
        return jarAbsPath;
    }

    public void setJarAbsPath(String jarAbsPath) {
        this.jarAbsPath = jarAbsPath;
    }

    public String getJarSha256() {
        return jarSha256;
    }

    public void setJarSha256(String jarSha256) {
        this.jarSha256 = jarSha256;
    }

    public String getJarRole() {
        return jarRole;
    }

    public void setJarRole(String jarRole) {
        this.jarRole = jarRole;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    @Override
    public String toString() {
        return "JarEntity{" +
                "jid=" + jid +
                ", jarName='" + jarName + '\'' +
                ", jarAbsPath='" + jarAbsPath + '\'' +
                ", jarSha256='" + jarSha256 + '\'' +
                ", jarRole='" + jarRole + '\'' +
                ", depth=" + depth +
                '}';
    }
}
