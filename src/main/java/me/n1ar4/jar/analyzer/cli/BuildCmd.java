/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "build database")
public class BuildCmd {
    public static final String CMD = "build";
    @Parameter(names = {"-j", "--jar"}, description = "jar file/dir")
    private String jar;
    @Parameter(names = {"--del-cache"}, description = "delete old cache")
    private boolean delCache;
    @Parameter(names = {"--inner-jars"}, description = "resolve jars in jar")
    private boolean innerJars;
    @Parameter(names = {"--jdk-modules"}, description = "jdk modules policy: core|web|all|csv")
    private String jdkModules = "core";

    public BuildCmd() {

    }

    public String getJar() {
        return jar;
    }

    public boolean delCache() {
        return delCache;
    }

    public boolean enableInnerJars() {
        return innerJars;
    }

    public String getJdkModules() {
        return jdkModules;
    }
}
