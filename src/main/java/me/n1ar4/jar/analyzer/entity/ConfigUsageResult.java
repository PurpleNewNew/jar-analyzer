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

import java.util.ArrayList;
import java.util.List;

public class ConfigUsageResult {
    private String key;
    private List<ConfigItem> items = new ArrayList<>();
    private List<Usage> usages = new ArrayList<>();
    private int resourceCount;
    private int methodCount;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<ConfigItem> getItems() {
        return items;
    }

    public void setItems(List<ConfigItem> items) {
        this.items = items;
    }

    public List<Usage> getUsages() {
        return usages;
    }

    public void setUsages(List<Usage> usages) {
        this.usages = usages;
    }

    public int getResourceCount() {
        return resourceCount;
    }

    public void setResourceCount(int resourceCount) {
        this.resourceCount = resourceCount;
    }

    public int getMethodCount() {
        return methodCount;
    }

    public void setMethodCount(int methodCount) {
        this.methodCount = methodCount;
    }

    public static class Usage {
        private MethodResult method;
        private List<EntryPoint> entrypoints = new ArrayList<>();
        private List<String> semanticTags = new ArrayList<>();
        private List<String> annoNames = new ArrayList<>();

        public MethodResult getMethod() {
            return method;
        }

        public void setMethod(MethodResult method) {
            this.method = method;
        }

        public List<EntryPoint> getEntrypoints() {
            return entrypoints;
        }

        public void setEntrypoints(List<EntryPoint> entrypoints) {
            this.entrypoints = entrypoints;
        }

        public List<String> getSemanticTags() {
            return semanticTags;
        }

        public void setSemanticTags(List<String> semanticTags) {
            this.semanticTags = semanticTags;
        }

        public List<String> getAnnoNames() {
            return annoNames;
        }

        public void setAnnoNames(List<String> annoNames) {
            this.annoNames = annoNames;
        }
    }

    public static class EntryPoint {
        private String type;
        private String className;
        private String methodName;
        private String methodDesc;
        private String path;
        private String restfulType;
        private Integer jarId;
        private String jarName;
        private int depth;
        private List<MethodTrace> trace = new ArrayList<>();

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public String getMethodDesc() {
            return methodDesc;
        }

        public void setMethodDesc(String methodDesc) {
            this.methodDesc = methodDesc;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getRestfulType() {
            return restfulType;
        }

        public void setRestfulType(String restfulType) {
            this.restfulType = restfulType;
        }

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

        public int getDepth() {
            return depth;
        }

        public void setDepth(int depth) {
            this.depth = depth;
        }

        public List<MethodTrace> getTrace() {
            return trace;
        }

        public void setTrace(List<MethodTrace> trace) {
            this.trace = trace;
        }
    }

    public static class MethodTrace {
        private String className;
        private String methodName;
        private String methodDesc;

        public MethodTrace() {
        }

        public MethodTrace(String className, String methodName, String methodDesc) {
            this.className = className;
            this.methodName = methodName;
            this.methodDesc = methodDesc;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public String getMethodDesc() {
            return methodDesc;
        }

        public void setMethodDesc(String methodDesc) {
            this.methodDesc = methodDesc;
        }
    }
}
