/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.rules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RuleValidationSummary {
    private final String name;
    private final String path;
    private final Map<String, Integer> compiledByKind;
    private final List<RuleValidationIssue> issues;
    private final List<RuleValidationSummary> files;

    private RuleValidationSummary(String name,
                                  String path,
                                  Map<String, Integer> compiledByKind,
                                  List<RuleValidationIssue> issues,
                                  List<RuleValidationSummary> files) {
        this.name = safe(name);
        this.path = safe(path);
        this.compiledByKind = compiledByKind == null || compiledByKind.isEmpty()
                ? Map.of()
                : Map.copyOf(compiledByKind);
        this.issues = issues == null || issues.isEmpty() ? List.of() : List.copyOf(issues);
        this.files = files == null || files.isEmpty() ? List.of() : List.copyOf(files);
    }

    public static Builder builder(String name, String path) {
        return new Builder(name, path);
    }

    public static RuleValidationSummary combine(String name, RuleValidationSummary... children) {
        List<RuleValidationSummary> files = new ArrayList<>();
        LinkedHashMap<String, Integer> compiled = new LinkedHashMap<>();
        if (children != null) {
            for (RuleValidationSummary child : children) {
                if (child == null) {
                    continue;
                }
                files.add(child);
                mergeCompiled(compiled, child.compiledByKind);
            }
        }
        return new RuleValidationSummary(name, "", compiled, List.of(), files);
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public Map<String, Integer> getCompiledByKind() {
        return compiledByKind;
    }

    public List<RuleValidationIssue> getIssues() {
        return issues;
    }

    public List<RuleValidationSummary> getFiles() {
        return files;
    }

    public int getCompiledRules() {
        if (!files.isEmpty()) {
            int total = 0;
            for (RuleValidationSummary file : files) {
                total += file.getCompiledRules();
            }
            return total;
        }
        int total = 0;
        for (Integer value : compiledByKind.values()) {
            if (value != null && value > 0) {
                total += value;
            }
        }
        return total;
    }

    public int getRejectedRules() {
        if (!files.isEmpty()) {
            int total = 0;
            for (RuleValidationSummary file : files) {
                total += file.getRejectedRules();
            }
            return total;
        }
        int total = 0;
        for (RuleValidationIssue issue : issues) {
            if (issue != null && issue.isRejected()) {
                total++;
            }
        }
        return total;
    }

    public int getErrorCount() {
        if (!files.isEmpty()) {
            int total = 0;
            for (RuleValidationSummary file : files) {
                total += file.getErrorCount();
            }
            return total;
        }
        int total = 0;
        for (RuleValidationIssue issue : issues) {
            if (issue != null && "error".equalsIgnoreCase(issue.getLevel())) {
                total++;
            }
        }
        return total;
    }

    public int getWarningCount() {
        if (!files.isEmpty()) {
            int total = 0;
            for (RuleValidationSummary file : files) {
                total += file.getWarningCount();
            }
            return total;
        }
        int total = 0;
        for (RuleValidationIssue issue : issues) {
            if (issue != null && "warning".equalsIgnoreCase(issue.getLevel())) {
                total++;
            }
        }
        return total;
    }

    public boolean isOk() {
        return getErrorCount() == 0;
    }

    public boolean hasIssues() {
        return getErrorCount() > 0 || getWarningCount() > 0;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", name);
        if (!path.isBlank()) {
            out.put("path", path);
        }
        out.put("ok", isOk());
        out.put("compiledRules", getCompiledRules());
        out.put("compiledByKind", compiledByKind);
        out.put("rejectedRules", getRejectedRules());
        out.put("errorCount", getErrorCount());
        out.put("warningCount", getWarningCount());
        if (!files.isEmpty()) {
            List<Map<String, Object>> fileMaps = new ArrayList<>(files.size());
            for (RuleValidationSummary file : files) {
                fileMaps.add(file.toMap());
            }
            out.put("files", fileMaps);
        }
        if (!issues.isEmpty()) {
            List<Map<String, Object>> issueMaps = new ArrayList<>(issues.size());
            for (RuleValidationIssue issue : issues) {
                issueMaps.add(issue.toMap());
            }
            out.put("issues", issueMaps);
        }
        return out;
    }

    private static void mergeCompiled(Map<String, Integer> target, Map<String, Integer> source) {
        if (target == null || source == null || source.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            int incoming = entry.getValue() == null ? 0 : entry.getValue();
            if (incoming <= 0) {
                continue;
            }
            target.merge(entry.getKey(), incoming, Integer::sum);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Builder {
        private final String name;
        private final String path;
        private final LinkedHashMap<String, Integer> compiledByKind = new LinkedHashMap<>();
        private final List<RuleValidationIssue> issues = new ArrayList<>();

        private Builder(String name, String path) {
            this.name = safe(name);
            this.path = safe(path);
        }

        public Builder compiled(String kind) {
            String normalized = safe(kind);
            if (normalized.isBlank()) {
                return this;
            }
            compiledByKind.merge(normalized, 1, Integer::sum);
            return this;
        }

        public Builder error(int index, String id, String kind, String code, String message) {
            issues.add(new RuleValidationIssue("error", code, message, index, id, kind, true));
            return this;
        }

        public Builder warning(int index, String id, String kind, String code, String message) {
            issues.add(new RuleValidationIssue("warning", code, message, index, id, kind, false));
            return this;
        }

        public RuleValidationSummary build() {
            return new RuleValidationSummary(name, path, compiledByKind, issues, List.of());
        }
    }
}
