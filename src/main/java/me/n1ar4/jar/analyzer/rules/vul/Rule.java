/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.rules.vul;

import me.n1ar4.jar.analyzer.engine.SearchCondition;

import java.util.List;
import java.util.Map;

/**
 * Vulnerability rule model loaded from {@code rules/vulnerability.yaml}.
 */
public class Rule {
    private String name;
    private Map<String, Map<String, List<SearchCondition>>> levels;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Map<String, List<SearchCondition>>> getLevels() {
        return levels;
    }

    public void setLevels(Map<String, Map<String, List<SearchCondition>>> levels) {
        this.levels = levels;
    }
}
