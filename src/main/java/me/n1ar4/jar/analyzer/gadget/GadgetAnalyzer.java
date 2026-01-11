/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gadget;

import me.n1ar4.jar.analyzer.utils.DirUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class GadgetAnalyzer {
    private static final Logger logger = LogManager.getLogger();

    private static class JarMeta {
        private final String name;
        private final String path;
        private final String version;

        private JarMeta(String name, String path, String version) {
            this.name = name;
            this.path = path;
            this.version = version;
        }
    }
    private final String dir;
    private final boolean enableNative;
    private final boolean enableHessian;
    private final boolean enableFastjson;
    private final boolean enableJdbc;

    public GadgetAnalyzer(String dir, boolean enableN, boolean enableH, boolean enableF, boolean enableJ) {
        this.dir = dir;
        this.enableNative = enableN;
        this.enableHessian = enableH;
        this.enableFastjson = enableF;
        this.enableJdbc = enableJ;
    }

    public List<GadgetInfo> process() {
        logger.info("start gadget analyzer");
        logger.info("n -> {} h -> {} f -> {} j -> {}",
                this.enableNative, this.enableHessian, this.enableFastjson, this.enableJdbc);
        List<String> files = DirUtil.GetFiles(this.dir);
        if (files == null || files.isEmpty()) {
            logger.warn("no files found");
            return new ArrayList<>();
        }
        List<Path> exiFiles = new ArrayList<>();
        for (String file : files) {
            Path tmp = Paths.get(file);
            if (Files.exists(tmp)) {
                exiFiles.add(tmp);
            }
        }
        List<String> finalFiles = new ArrayList<>();
        List<JarMeta> jarMetas = new ArrayList<>();
        for (Path exiFile : exiFiles) {
            String filename = exiFile.toFile().getName();
            if (!filename.endsWith(".jar")) {
                continue;
            }
            finalFiles.add(filename);
            jarMetas.add(new JarMeta(filename,
                    exiFile.toAbsolutePath().toString(),
                    guessVersion(filename)));
        }
        List<GadgetInfo> result = new ArrayList<>();
        // 匹配分析
        for (GadgetInfo rule : GadgetRule.rules) {
            String ruleType = rule.getType();
            if (ruleType.equals(GadgetInfo.NATIVE_TYPE)) {
                if (!this.enableNative) {
                    continue;
                }
            }
            if (ruleType.equals(GadgetInfo.HESSIAN_TYPE)) {
                if (!this.enableHessian) {
                    continue;
                }
            }
            if (ruleType.equals(GadgetInfo.FASTJSON_TYPE)) {
                if (!this.enableFastjson) {
                    continue;
                }
            }
            if (ruleType.equals(GadgetInfo.JDBC_TYPE)) {
                if (!this.enableJdbc) {
                    continue;
                }
            }
            logger.info("processing rule : " + rule.getJarsName());
            List<String> jarsName = rule.getJarsName();
            boolean[] successArray = new boolean[jarsName.size()];
            java.util.LinkedHashMap<String, JarMeta> matched = new java.util.LinkedHashMap<>();
            for (int i = 0; i < successArray.length; i++) {
                String jarName = jarsName.get(i);
                if (jarName.contains("!")) {
                    String temp = jarName.split("!")[0];
                    String whiteList = jarName.split("!")[1].split("\\.jar")[0];
                    for (JarMeta meta : jarMetas) {
                        if (!meta.name.startsWith(temp)) {
                            continue;
                        }
                        String ver = extractVersionWithPrefix(meta.name, temp);
                        if (ver != null && !ver.equals(whiteList)) {
                            successArray[i] = true;
                            addMatch(matched, meta, ver);
                        }
                    }
                } else {
                    if (!jarName.contains("*")) {
                        for (JarMeta meta : jarMetas) {
                            if (jarName.equals(meta.name)) {
                                successArray[i] = true;
                                addMatch(matched, meta, meta.version);
                            }
                        }
                    } else {
                        String regex = jarName.replace("*", ".*");
                        for (JarMeta meta : jarMetas) {
                            if (meta.name.matches(regex)) {
                                successArray[i] = true;
                                addMatch(matched, meta, meta.version);
                            }
                        }
                    }
                }
            }
            boolean success = true;
            for (boolean b : successArray) {
                if (!b) {
                    success = false;
                    break;
                }
            }
            if (success) {
                result.add(copyRule(rule, matched));
            }
        }
        // 补充输出
        if (this.enableNative) {

        }
        if (this.enableHessian) {

        }
        if (this.enableFastjson) {

        }
        if (this.enableJdbc) {

        }
        return result;
    }

    private static void addMatch(java.util.LinkedHashMap<String, JarMeta> matched,
                                 JarMeta meta,
                                 String version) {
        if (meta == null || meta.path == null) {
            return;
        }
        if (!matched.containsKey(meta.path)) {
            matched.put(meta.path, new JarMeta(meta.name, meta.path, version));
        }
    }

    private static GadgetInfo copyRule(GadgetInfo rule, java.util.LinkedHashMap<String, JarMeta> matched) {
        GadgetInfo out = new GadgetInfo();
        out.setID(rule.getID());
        out.setType(rule.getType());
        out.setJarsName(rule.getJarsName());
        out.setResult(rule.getResult());
        if (!matched.isEmpty()) {
            List<String> names = new ArrayList<>();
            List<String> paths = new ArrayList<>();
            List<String> versions = new ArrayList<>();
            for (JarMeta meta : matched.values()) {
                names.add(meta.name);
                paths.add(meta.path);
                versions.add(meta.version == null ? "" : meta.version);
            }
            out.setMatchedJarNames(names);
            out.setMatchedJarPaths(paths);
            out.setMatchedJarVersions(versions);
        }
        return out;
    }

    private static String extractVersionWithPrefix(String fileName, String prefix) {
        if (fileName == null || prefix == null) {
            return null;
        }
        if (!fileName.startsWith(prefix) || !fileName.endsWith(".jar")) {
            return null;
        }
        String tail = fileName.substring(prefix.length(), fileName.length() - 4);
        if (tail.startsWith("-")) {
            tail = tail.substring(1);
        }
        return tail.isEmpty() ? null : tail;
    }

    private static String guessVersion(String fileName) {
        if (fileName == null || !fileName.endsWith(".jar")) {
            return null;
        }
        String base = fileName.substring(0, fileName.length() - 4);
        int idx = base.lastIndexOf('-');
        if (idx <= 0 || idx >= base.length() - 1) {
            return null;
        }
        String candidate = base.substring(idx + 1);
        if (candidate.isEmpty()) {
            return null;
        }
        char first = candidate.charAt(0);
        if (first < '0' || first > '9') {
            return null;
        }
        return candidate;
    }
}
