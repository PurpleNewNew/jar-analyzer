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

public class SemanticHintResult {
    private String name;
    private String description;
    private int count;
    private List<HintMethod> methods = new ArrayList<>();
    private List<String> keywords = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<HintMethod> getMethods() {
        return methods;
    }

    public void setMethods(List<HintMethod> methods) {
        this.methods = methods;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public static class HintMethod {
        private MethodResult method;
        private String source;
        private String evidence;

        public MethodResult getMethod() {
            return method;
        }

        public void setMethod(MethodResult method) {
            this.method = method;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getEvidence() {
            return evidence;
        }

        public void setEvidence(String evidence) {
            this.evidence = evidence;
        }
    }
}
