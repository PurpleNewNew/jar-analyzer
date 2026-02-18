/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.graph.cypher;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AstNormalizer {
    private static final Pattern PROC_PATTERN = Pattern.compile("(?is)^\\s*call\\s+([a-zA-Z0-9_\\.]+)\\s*\\((.*)\\)\\s*(return.*)?$");
    private static final Pattern LIMIT_PATTERN = Pattern.compile("(?is)\\blimit\\s+([0-9]+)");
    private static final Pattern SKIP_PATTERN = Pattern.compile("(?is)\\b(skip|offset)\\s+([0-9]+)");
    private static final Pattern LABEL_PATTERN = Pattern.compile("\\(:([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern HOPS_PATTERN = Pattern.compile("\\*\\s*([0-9]*)\\s*\\.\\.\\s*([0-9]*)");

    private AstNormalizer() {
    }

    public static NormalizedCypher normalize(String query) {
        String raw = query == null ? "" : query.trim();
        if (raw.isEmpty()) {
            throw new IllegalArgumentException("cypher_empty_query");
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        if (containsUnsupported(lower)) {
            throw new IllegalArgumentException("cypher_feature_not_supported");
        }

        Matcher proc = PROC_PATTERN.matcher(raw);
        if (proc.matches()) {
            String procName = safe(proc.group(1));
            List<String> args = splitArgs(proc.group(2));
            int limit = extractInt(raw, LIMIT_PATTERN, 2_147_483_647);
            int skip = extractInt(raw, SKIP_PATTERN, 0, 2);
            return NormalizedCypher.procedure(raw, procName, args, limit, skip);
        }

        if (!lower.startsWith("match")) {
            throw new IllegalArgumentException("cypher_feature_not_supported");
        }
        String label = "";
        Matcher labelMatcher = LABEL_PATTERN.matcher(raw);
        if (labelMatcher.find()) {
            label = safe(labelMatcher.group(1));
        }
        boolean shortest = lower.contains("shortestpath(");
        boolean relPattern = raw.contains("-[") && raw.contains("]->") || raw.contains("--");
        int maxHops = 4;
        Matcher hops = HOPS_PATTERN.matcher(raw);
        if (hops.find()) {
            String to = safe(hops.group(2));
            if (!to.isEmpty()) {
                maxHops = toInt(to, maxHops);
            } else {
                String from = safe(hops.group(1));
                if (!from.isEmpty()) {
                    maxHops = toInt(from, maxHops);
                }
            }
        }
        int limit = extractInt(raw, LIMIT_PATTERN, 500);
        int skip = extractInt(raw, SKIP_PATTERN, 0, 2);
        return NormalizedCypher.match(raw, label, relPattern, shortest, limit, skip, maxHops);
    }

    private static boolean containsUnsupported(String lower) {
        return lower.contains(" create ")
                || lower.startsWith("create ")
                || lower.contains(" merge ")
                || lower.startsWith("merge ")
                || lower.contains(" delete ")
                || lower.startsWith("delete ")
                || lower.contains(" detach delete ")
                || lower.contains(" set ")
                || lower.contains(" remove ")
                || lower.contains(" insert ")
                || lower.contains(" unwind ")
                || lower.contains(" foreach ")
                || lower.contains(" load csv ")
                || lower.contains(" drop ");
    }

    private static int extractInt(String raw, Pattern pattern, int def) {
        return extractInt(raw, pattern, def, 1);
    }

    private static int extractInt(String raw, Pattern pattern, int def, int group) {
        Matcher matcher = pattern.matcher(raw);
        if (!matcher.find()) {
            return def;
        }
        String value = safe(matcher.group(group));
        if (value.isEmpty()) {
            return def;
        }
        return toInt(value, def);
    }

    private static int toInt(String value, int def) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return def;
        }
    }

    private static List<String> splitArgs(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return out;
        }
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inSingle = false;
        boolean inDouble = false;
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '\'' && !inDouble) {
                inSingle = !inSingle;
            } else if (ch == '"' && !inSingle) {
                inDouble = !inDouble;
            }
            if (!inSingle && !inDouble) {
                if (ch == '(') {
                    depth++;
                } else if (ch == ')') {
                    depth = Math.max(0, depth - 1);
                } else if (ch == ',' && depth == 0) {
                    String arg = current.toString().trim();
                    if (!arg.isEmpty()) {
                        out.add(arg);
                    }
                    current.setLength(0);
                    continue;
                }
            }
            current.append(ch);
        }
        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            out.add(tail);
        }
        return out;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class NormalizedCypher {
        private final boolean procedure;
        private final String rawQuery;
        private final String procedureName;
        private final List<String> procedureArgs;
        private final String labelHint;
        private final boolean relationshipPattern;
        private final boolean shortestPath;
        private final int limit;
        private final int skip;
        private final int maxHops;

        private NormalizedCypher(boolean procedure,
                                 String rawQuery,
                                 String procedureName,
                                 List<String> procedureArgs,
                                 String labelHint,
                                 boolean relationshipPattern,
                                 boolean shortestPath,
                                 int limit,
                                 int skip,
                                 int maxHops) {
            this.procedure = procedure;
            this.rawQuery = rawQuery;
            this.procedureName = procedureName;
            this.procedureArgs = procedureArgs;
            this.labelHint = labelHint;
            this.relationshipPattern = relationshipPattern;
            this.shortestPath = shortestPath;
            this.limit = limit;
            this.skip = skip;
            this.maxHops = maxHops;
        }

        public static NormalizedCypher procedure(String rawQuery,
                                                 String procedureName,
                                                 List<String> procedureArgs,
                                                 int limit,
                                                 int skip) {
            return new NormalizedCypher(true, rawQuery, procedureName, procedureArgs,
                    "", false, false, limit, skip, 1);
        }

        public static NormalizedCypher match(String rawQuery,
                                             String labelHint,
                                             boolean relationshipPattern,
                                             boolean shortestPath,
                                             int limit,
                                             int skip,
                                             int maxHops) {
            return new NormalizedCypher(false, rawQuery, "", List.of(),
                    labelHint, relationshipPattern, shortestPath, limit, skip, maxHops);
        }

        public boolean isProcedure() {
            return procedure;
        }

        public String getRawQuery() {
            return rawQuery;
        }

        public String getProcedureName() {
            return procedureName;
        }

        public List<String> getProcedureArgs() {
            return procedureArgs;
        }

        public String getLabelHint() {
            return labelHint;
        }

        public boolean isRelationshipPattern() {
            return relationshipPattern;
        }

        public boolean isShortestPath() {
            return shortestPath;
        }

        public int getLimit() {
            return limit;
        }

        public int getSkip() {
            return skip;
        }

        public int getMaxHops() {
            return maxHops;
        }
    }
}
