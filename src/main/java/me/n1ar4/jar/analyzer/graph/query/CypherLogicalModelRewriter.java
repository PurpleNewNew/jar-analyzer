/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.query;

import me.n1ar4.jar.analyzer.graph.model.GraphRelationType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class CypherLogicalModelRewriter {
    private CypherLogicalModelRewriter() {
    }

    public static String rewrite(String query) {
        return rewriteLogicalTypePredicates(rewriteRelationshipPatterns(query));
    }

    private static String rewriteRelationshipPatterns(String query) {
        if (query == null || query.isEmpty()) {
            return query == null ? "" : query;
        }
        StringBuilder out = new StringBuilder(query.length() + 64);
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            char next = i + 1 < query.length() ? query.charAt(i + 1) : '\0';
            if (inLineComment) {
                out.append(c);
                if (c == '\n' || c == '\r') {
                    inLineComment = false;
                }
                continue;
            }
            if (inBlockComment) {
                out.append(c);
                if (c == '*' && next == '/') {
                    out.append(next);
                    i++;
                    inBlockComment = false;
                }
                continue;
            }
            if (inSingleQuote) {
                out.append(c);
                if (c == '\'' && next == '\'') {
                    out.append(next);
                    i++;
                    continue;
                }
                if (c == '\\' && i + 1 < query.length()) {
                    out.append(next);
                    i++;
                    continue;
                }
                if (c == '\'') {
                    inSingleQuote = false;
                }
                continue;
            }
            if (inDoubleQuote) {
                out.append(c);
                if (c == '"' && next == '"') {
                    out.append(next);
                    i++;
                    continue;
                }
                if (c == '\\' && i + 1 < query.length()) {
                    out.append(next);
                    i++;
                    continue;
                }
                if (c == '"') {
                    inDoubleQuote = false;
                }
                continue;
            }
            if (inBacktick) {
                out.append(c);
                if (c == '`') {
                    inBacktick = false;
                }
                continue;
            }
            if (c == '/' && next == '/') {
                out.append(c).append(next);
                i++;
                inLineComment = true;
                continue;
            }
            if (c == '/' && next == '*') {
                out.append(c).append(next);
                i++;
                inBlockComment = true;
                continue;
            }
            PredicateRewrite reverseRewrite = parseLiteralTypePredicateRewrite(query, i);
            if (reverseRewrite != null) {
                out.append(reverseRewrite.replacement());
                i = reverseRewrite.endExclusive() - 1;
                continue;
            }
            if (c == '\'') {
                out.append(c);
                inSingleQuote = true;
                continue;
            }
            if (c == '"') {
                out.append(c);
                inDoubleQuote = true;
                continue;
            }
            if (c == '`') {
                out.append(c);
                inBacktick = true;
                continue;
            }
            if (c == '[' && isRelationshipPatternStart(query, i)) {
                int end = findRelationshipPatternEnd(query, i);
                if (end > i) {
                    out.append('[')
                            .append(rewriteRelationshipPatternSegment(query.substring(i + 1, end)))
                            .append(']');
                    i = end;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String rewriteLogicalTypePredicates(String query) {
        if (query == null || query.isEmpty()) {
            return query == null ? "" : query;
        }
        StringBuilder out = new StringBuilder(query.length() + 128);
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            char next = i + 1 < query.length() ? query.charAt(i + 1) : '\0';
            if (inLineComment) {
                out.append(c);
                if (c == '\n' || c == '\r') {
                    inLineComment = false;
                }
                continue;
            }
            if (inBlockComment) {
                out.append(c);
                if (c == '*' && next == '/') {
                    out.append(next);
                    i++;
                    inBlockComment = false;
                }
                continue;
            }
            if (inSingleQuote) {
                out.append(c);
                if (c == '\'' && next == '\'') {
                    out.append(next);
                    i++;
                    continue;
                }
                if (c == '\\' && i + 1 < query.length()) {
                    out.append(next);
                    i++;
                    continue;
                }
                if (c == '\'') {
                    inSingleQuote = false;
                }
                continue;
            }
            if (inDoubleQuote) {
                out.append(c);
                if (c == '"' && next == '"') {
                    out.append(next);
                    i++;
                    continue;
                }
                if (c == '\\' && i + 1 < query.length()) {
                    out.append(next);
                    i++;
                    continue;
                }
                if (c == '"') {
                    inDoubleQuote = false;
                }
                continue;
            }
            if (inBacktick) {
                out.append(c);
                if (c == '`') {
                    inBacktick = false;
                }
                continue;
            }
            if (c == '/' && next == '/') {
                out.append(c).append(next);
                i++;
                inLineComment = true;
                continue;
            }
            if (c == '/' && next == '*') {
                out.append(c).append(next);
                i++;
                inBlockComment = true;
                continue;
            }
            if (c == '\'') {
                out.append(c);
                inSingleQuote = true;
                continue;
            }
            if (c == '"') {
                out.append(c);
                inDoubleQuote = true;
                continue;
            }
            if (c == '`') {
                out.append(c);
                inBacktick = true;
                continue;
            }
            PredicateRewrite rewrite = parseTypePredicateRewrite(query, i);
            if (rewrite != null) {
                out.append(rewrite.replacement());
                i = rewrite.endExclusive() - 1;
                continue;
            }
            PredicateRewrite reverseLiteralRewrite = parseLiteralTypePredicateRewrite(query, i);
            if (reverseLiteralRewrite != null) {
                out.append(reverseLiteralRewrite.replacement());
                i = reverseLiteralRewrite.endExclusive() - 1;
                continue;
            }
            PredicateRewrite reverseParamRewrite = parseParameterTypePredicateRewrite(query, i);
            if (reverseParamRewrite != null) {
                out.append(reverseParamRewrite.replacement());
                i = reverseParamRewrite.endExclusive() - 1;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    private static boolean isRelationshipPatternStart(String query, int index) {
        int prev = previousNonWhitespace(query, index - 1);
        return prev >= 0 && query.charAt(prev) == '-';
    }

    private static int previousNonWhitespace(String value, int index) {
        int i = index;
        while (i >= 0) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return i;
            }
            i--;
        }
        return -1;
    }

    private static int findRelationshipPatternEnd(String query, int start) {
        int depth = 1;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        for (int i = start + 1; i < query.length(); i++) {
            char c = query.charAt(i);
            char next = i + 1 < query.length() ? query.charAt(i + 1) : '\0';
            if (inSingleQuote) {
                if (c == '\'' && next == '\'') {
                    i++;
                    continue;
                }
                if (c == '\\' && i + 1 < query.length()) {
                    i++;
                    continue;
                }
                if (c == '\'') {
                    inSingleQuote = false;
                }
                continue;
            }
            if (inDoubleQuote) {
                if (c == '"' && next == '"') {
                    i++;
                    continue;
                }
                if (c == '\\' && i + 1 < query.length()) {
                    i++;
                    continue;
                }
                if (c == '"') {
                    inDoubleQuote = false;
                }
                continue;
            }
            if (inBacktick) {
                if (c == '`') {
                    inBacktick = false;
                }
                continue;
            }
            if (c == '\'') {
                inSingleQuote = true;
                continue;
            }
            if (c == '"') {
                inDoubleQuote = true;
                continue;
            }
            if (c == '`') {
                inBacktick = true;
                continue;
            }
            if (c == '[') {
                depth++;
                continue;
            }
            if (c == ']') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String rewriteRelationshipPatternSegment(String segment) {
        if (segment == null || segment.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(segment.length() + 64);
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        boolean rewriteTypes = true;
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            char next = i + 1 < segment.length() ? segment.charAt(i + 1) : '\0';
            if (inSingleQuote) {
                out.append(c);
                if (c == '\'' && next == '\'') {
                    out.append(next);
                    i++;
                    continue;
                }
                if (c == '\\' && i + 1 < segment.length()) {
                    out.append(next);
                    i++;
                    continue;
                }
                if (c == '\'') {
                    inSingleQuote = false;
                }
                continue;
            }
            if (inDoubleQuote) {
                out.append(c);
                if (c == '"' && next == '"') {
                    out.append(next);
                    i++;
                    continue;
                }
                if (c == '\\' && i + 1 < segment.length()) {
                    out.append(next);
                    i++;
                    continue;
                }
                if (c == '"') {
                    inDoubleQuote = false;
                }
                continue;
            }
            if (inBacktick) {
                out.append(c);
                if (c == '`') {
                    inBacktick = false;
                }
                continue;
            }
            if (c == '\'') {
                out.append(c);
                inSingleQuote = true;
                continue;
            }
            if (c == '"') {
                out.append(c);
                inDoubleQuote = true;
                continue;
            }
            if (c == '`') {
                out.append(c);
                inBacktick = true;
                continue;
            }
            if (c == '*' || c == '{') {
                rewriteTypes = false;
                out.append(c);
                continue;
            }
            if (!rewriteTypes || (c != ':' && c != '|')) {
                out.append(c);
                continue;
            }
            out.append(c);
            int j = i + 1;
            while (j < segment.length() && Character.isWhitespace(segment.charAt(j))) {
                out.append(segment.charAt(j));
                j++;
            }
            if (j >= segment.length() || segment.charAt(j) == '`') {
                i = j - 1;
                continue;
            }
            int start = j;
            while (j < segment.length() && isIdentifierPart(segment.charAt(j))) {
                j++;
            }
            if (j == start) {
                i = j - 1;
                continue;
            }
            String identifier = segment.substring(start, j);
            out.append(expandRelationshipType(identifier));
            i = j - 1;
        }
        return out.toString();
    }

    private static boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static String expandRelationshipType(String identifier) {
        List<String> expanded = GraphRelationType.expandLogicalRelationType(identifier);
        if (expanded.isEmpty()) {
            return identifier;
        }
        if (expanded.size() == 1 && identifier.equals(expanded.get(0))) {
            return identifier;
        }
        return String.join("|", expanded);
    }

    private static PredicateRewrite parseTypePredicateRewrite(String query, int start) {
        TypeCall call = parseTypeCall(query, start, false);
        if (call == null) {
            return null;
        }
        Operator operator = parseOperator(query, call.endExclusive());
        if (operator == null) {
            return null;
        }
        return switch (operator.kind()) {
            case EQUALS, NOT_EQUALS -> buildComparisonRewrite(query, call, operator);
            case IN, NOT_IN -> buildListRewrite(query, call, operator);
        };
    }

    private static PredicateRewrite parseLiteralTypePredicateRewrite(String query, int start) {
        if (start < 0 || start >= query.length()) {
            return null;
        }
        char current = query.charAt(start);
        if (current != '\'' && current != '"') {
            return null;
        }
        StringLiteral literal = parseStringLiteral(query, start);
        if (literal == null) {
            return null;
        }
        Operator operator = parseOperator(query, literal.endExclusive());
        if (operator == null || (operator.kind() != OperatorKind.EQUALS && operator.kind() != OperatorKind.NOT_EQUALS)) {
            return null;
        }
        TypeCall call = parseTypeCall(query, operator.endExclusive(), true);
        if (call == null) {
            return null;
        }
        Expansion expansion = expandLogicalValues(List.of(literal.value()));
        if (!expansion.changed()) {
            return null;
        }
        String replacement;
        if (operator.kind() == OperatorKind.EQUALS) {
            if (expansion.values().size() == 1) {
                replacement = renderStringLiteral(expansion.values().get(0)) + " = " + call.rawText();
            } else {
                List<String> parts = new ArrayList<>();
                for (String value : expansion.values()) {
                    parts.add(renderStringLiteral(value) + " = " + call.rawText());
                }
                replacement = "(" + String.join(" OR ", parts) + ")";
            }
        } else {
            if (expansion.values().size() == 1) {
                replacement = renderStringLiteral(expansion.values().get(0)) + " " + operator.rawToken() + " " + call.rawText();
            } else {
                replacement = "NOT (" + call.rawText() + " IN " + renderStringList(expansion.values()) + ")";
            }
        }
        return new PredicateRewrite(replacement, call.endExclusive());
    }

    private static PredicateRewrite parseParameterTypePredicateRewrite(String query, int start) {
        ParameterRef parameter = parseParameterRef(query, start, false);
        if (parameter == null) {
            return null;
        }
        Operator operator = parseOperator(query, parameter.endExclusive());
        if (operator == null || (operator.kind() != OperatorKind.EQUALS && operator.kind() != OperatorKind.NOT_EQUALS)) {
            return null;
        }
        TypeCall call = parseTypeCall(query, operator.endExclusive(), true);
        if (call == null) {
            return null;
        }
        return new PredicateRewrite(
                parameterComparisonReplacement(call.rawText(), operator.kind(), parameter.rawText()),
                call.endExclusive()
        );
    }

    private static PredicateRewrite buildComparisonRewrite(String query, TypeCall call, Operator operator) {
        StringLiteral literal = parseStringLiteral(query, operator.endExclusive());
        if (literal != null) {
            Expansion expansion = expandLogicalValues(List.of(literal.value()));
            if (!expansion.changed()) {
                return null;
            }
            String replacement;
            if (operator.kind() == OperatorKind.EQUALS) {
                if (expansion.values().size() == 1) {
                    replacement = call.rawText() + " = " + renderStringLiteral(expansion.values().get(0));
                } else {
                    List<String> parts = new ArrayList<>();
                    for (String value : expansion.values()) {
                        parts.add(call.rawText() + " = " + renderStringLiteral(value));
                    }
                    replacement = "(" + String.join(" OR ", parts) + ")";
                }
            } else {
                if (expansion.values().size() == 1) {
                    replacement = call.rawText() + " " + operator.rawToken() + " " + renderStringLiteral(expansion.values().get(0));
                } else {
                    replacement = "NOT (" + call.rawText() + " IN " + renderStringList(expansion.values()) + ")";
                }
            }
            return new PredicateRewrite(replacement, literal.endExclusive());
        }
        ParameterRef parameter = parseParameterRef(query, operator.endExclusive(), true);
        if (parameter == null) {
            return null;
        }
        return new PredicateRewrite(
                parameterComparisonReplacement(call.rawText(), operator.kind(), parameter.rawText()),
                parameter.endExclusive()
        );
    }

    private static PredicateRewrite buildListRewrite(String query, TypeCall call, Operator operator) {
        StringListLiteral list = parseStringListLiteral(query, operator.endExclusive());
        if (list != null) {
            Expansion expansion = expandLogicalValues(list.values());
            if (!expansion.changed()) {
                return null;
            }
            String inToken = operator.kind() == OperatorKind.NOT_IN ? " NOT IN " : " IN ";
            return new PredicateRewrite(call.rawText() + inToken + renderStringList(expansion.values()), list.endExclusive());
        }
        ParameterRef parameter = parseParameterRef(query, operator.endExclusive(), true);
        if (parameter == null) {
            return null;
        }
        return new PredicateRewrite(
                parameterListReplacement(call.rawText(), operator.kind(), parameter.rawText()),
                parameter.endExclusive()
        );
    }

    private static TypeCall parseTypeCall(String query, int start, boolean allowLeadingWhitespace) {
        int cursor = allowLeadingWhitespace ? skipWhitespace(query, start) : start;
        if (!matchesKeyword(query, cursor, "type")) {
            return null;
        }
        int rawStart = cursor;
        cursor = cursor + 4;
        cursor = skipWhitespace(query, cursor);
        if (cursor >= query.length() || query.charAt(cursor) != '(') {
            return null;
        }
        cursor++;
        cursor = skipWhitespace(query, cursor);
        int identStart = cursor;
        while (cursor < query.length() && isIdentifierPart(query.charAt(cursor))) {
            cursor++;
        }
        if (cursor == identStart) {
            return null;
        }
        cursor = skipWhitespace(query, cursor);
        if (cursor >= query.length() || query.charAt(cursor) != ')') {
            return null;
        }
        int endExclusive = cursor + 1;
        return new TypeCall(query.substring(rawStart, endExclusive), endExclusive);
    }

    private static Operator parseOperator(String query, int start) {
        int cursor = skipWhitespace(query, start);
        if (cursor >= query.length()) {
            return null;
        }
        if (query.startsWith("<>", cursor) || query.startsWith("!=", cursor)) {
            return new Operator(OperatorKind.NOT_EQUALS, query.substring(cursor, cursor + 2), cursor + 2);
        }
        if (query.charAt(cursor) == '=') {
            return new Operator(OperatorKind.EQUALS, "=", cursor + 1);
        }
        if (matchesKeyword(query, cursor, "not")) {
            int afterNot = skipWhitespace(query, cursor + 3);
            if (matchesKeyword(query, afterNot, "in")) {
                return new Operator(OperatorKind.NOT_IN, "NOT IN", afterNot + 2);
            }
        }
        if (matchesKeyword(query, cursor, "in")) {
            return new Operator(OperatorKind.IN, "IN", cursor + 2);
        }
        return null;
    }

    private static StringLiteral parseStringLiteral(String query, int start) {
        int cursor = skipWhitespace(query, start);
        if (cursor >= query.length()) {
            return null;
        }
        char quote = query.charAt(cursor);
        if (quote != '\'' && quote != '"') {
            return null;
        }
        StringBuilder value = new StringBuilder();
        for (int i = cursor + 1; i < query.length(); i++) {
            char c = query.charAt(i);
            char next = i + 1 < query.length() ? query.charAt(i + 1) : '\0';
            if (c == quote && next == quote) {
                value.append(quote);
                i++;
                continue;
            }
            if (c == '\\' && i + 1 < query.length()) {
                value.append(next);
                i++;
                continue;
            }
            if (c == quote) {
                return new StringLiteral(value.toString(), i + 1);
            }
            value.append(c);
        }
        return null;
    }

    private static StringListLiteral parseStringListLiteral(String query, int start) {
        int cursor = skipWhitespace(query, start);
        if (cursor >= query.length() || query.charAt(cursor) != '[') {
            return null;
        }
        cursor++;
        List<String> values = new ArrayList<>();
        while (true) {
            cursor = skipWhitespace(query, cursor);
            if (cursor >= query.length()) {
                return null;
            }
            if (query.charAt(cursor) == ']') {
                return new StringListLiteral(List.copyOf(values), cursor + 1);
            }
            StringLiteral literal = parseStringLiteral(query, cursor);
            if (literal == null) {
                return null;
            }
            values.add(literal.value());
            cursor = skipWhitespace(query, literal.endExclusive());
            if (cursor >= query.length()) {
                return null;
            }
            if (query.charAt(cursor) == ',') {
                cursor++;
                continue;
            }
            if (query.charAt(cursor) == ']') {
                return new StringListLiteral(List.copyOf(values), cursor + 1);
            }
            return null;
        }
    }

    private static Expansion expandLogicalValues(List<String> values) {
        LinkedHashSet<String> expanded = new LinkedHashSet<>();
        boolean changed = false;
        for (String value : values) {
            List<String> mapped = GraphRelationType.expandLogicalRelationType(value);
            if (mapped.isEmpty()) {
                expanded.add(value);
                continue;
            }
            if (mapped.size() != 1 || !value.equals(mapped.get(0))) {
                changed = true;
            }
            expanded.addAll(mapped);
        }
        if (expanded.size() != values.size()) {
            changed = true;
        }
        return new Expansion(List.copyOf(expanded), changed);
    }

    private static String renderStringList(List<String> values) {
        List<String> rendered = new ArrayList<>();
        for (String value : values) {
            rendered.add(renderStringLiteral(value));
        }
        return "[" + String.join(", ", rendered) + "]";
    }

    private static String renderStringLiteral(String value) {
        return "'" + (value == null ? "" : value.replace("'", "''")) + "'";
    }

    private static String parameterComparisonReplacement(String typeCall,
                                                         OperatorKind kind,
                                                         String parameter) {
        String match = "(" + typeCall + " = " + parameter + " OR ja.relGroup(" + typeCall + ") = " + parameter + ")";
        return kind == OperatorKind.NOT_EQUALS ? "NOT " + match : match;
    }

    private static String parameterListReplacement(String typeCall,
                                                   OperatorKind kind,
                                                   String parameter) {
        String match = "(" + typeCall + " IN " + parameter + " OR ja.relGroup(" + typeCall + ") IN " + parameter + ")";
        return kind == OperatorKind.NOT_IN ? "NOT " + match : match;
    }

    private static int skipWhitespace(String query, int cursor) {
        int out = cursor;
        while (out < query.length() && Character.isWhitespace(query.charAt(out))) {
            out++;
        }
        return out;
    }

    private static boolean matchesKeyword(String query, int start, String keyword) {
        if (query == null || keyword == null || start < 0) {
            return false;
        }
        int end = start + keyword.length();
        if (end > query.length() || !query.regionMatches(true, start, keyword, 0, keyword.length())) {
            return false;
        }
        if (start > 0 && isIdentifierPart(query.charAt(start - 1))) {
            return false;
        }
        return end >= query.length() || !isIdentifierPart(query.charAt(end));
    }

    private static ParameterRef parseParameterRef(String query, int start, boolean allowLeadingWhitespace) {
        int cursor = allowLeadingWhitespace ? skipWhitespace(query, start) : start;
        if (cursor < 0 || cursor >= query.length() || query.charAt(cursor) != '$') {
            return null;
        }
        int rawStart = cursor;
        cursor++;
        int identStart = cursor;
        while (cursor < query.length() && isIdentifierPart(query.charAt(cursor))) {
            cursor++;
        }
        if (cursor == identStart) {
            return null;
        }
        return new ParameterRef(query.substring(rawStart, cursor), cursor);
    }

    private record TypeCall(String rawText, int endExclusive) {
    }

    private enum OperatorKind {
        EQUALS,
        NOT_EQUALS,
        IN,
        NOT_IN
    }

    private record Operator(OperatorKind kind, String rawToken, int endExclusive) {
    }

    private record StringLiteral(String value, int endExclusive) {
    }

    private record StringListLiteral(List<String> values, int endExclusive) {
    }

    private record ParameterRef(String rawText, int endExclusive) {
    }

    private record Expansion(List<String> values, boolean changed) {
    }

    private record PredicateRewrite(String replacement, int endExclusive) {
    }
}
