/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.util;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Best-effort locate a method/constructor/static initializer in a decompiled Java source string.
 *
 * This class intentionally does not depend on Swing UI components so it can be unit-tested.
 */
public final class DecompiledMethodLocator {
    private static final int WINDOW_LINES_ABOVE = 80;
    private static final int WINDOW_LINES_BELOW = 5;
    private static final Object PARSE_LOCK = new Object();
    private static final JavaParser PARSER = createParser();
    private static String lastParsedCode = null;
    private static CompilationUnit lastParsedUnit = null;

    private DecompiledMethodLocator() {
    }

    public enum Confidence {
        AST_NAME,
        TEXT_SIGNATURE,
        MAPPING_LINE,
        NONE
    }

    public static final class RangeHint {
        public final int minLine;
        public final int maxLine;

        public RangeHint(int minLine, int maxLine) {
            this.minLine = minLine;
            this.maxLine = maxLine;
        }
    }

    public static final class JumpTarget {
        public final int startOffset;
        public final int endOffset;
        public final Confidence confidence;
        public final String debug;

        private JumpTarget(int startOffset, int endOffset, Confidence confidence, String debug) {
            this.startOffset = Math.max(0, startOffset);
            this.endOffset = Math.max(this.startOffset, endOffset);
            this.confidence = confidence == null ? Confidence.NONE : confidence;
            this.debug = debug;
        }

        public static JumpTarget of(int startOffset, int endOffset, Confidence confidence, String debug) {
            return new JumpTarget(startOffset, endOffset, confidence, debug);
        }
    }

    public static JumpTarget locate(String code,
                                    String internalClassName,
                                    String methodName,
                                    String methodDesc,
                                    RangeHint hint) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        if (methodName == null || methodName.trim().isEmpty()) {
            return null;
        }
        String trimmed = methodName.trim();
        LineIndex index = new LineIndex(code);

        // For performance, prefer local text scan within a small mapping window.
        // This provides an IDEA-like caret placement without parsing the whole file.
        if (hint != null) {
            JumpTarget target = locateByLocalText(index, internalClassName, trimmed, hint);
            if (target != null && target.confidence != Confidence.NONE) {
                return target;
            }
        }

        CompilationUnit cu = tryParse(code);
        if (hint != null && cu != null) {
            JumpTarget target = locateByAstWithHint(index, cu, trimmed, methodDesc, hint);
            if (target != null && target.confidence != Confidence.NONE) {
                return target;
            }
        }
        if (cu != null) {
            JumpTarget target = locateByAstDescriptor(index, cu, internalClassName, trimmed, methodDesc);
            if (target != null && target.confidence != Confidence.NONE) {
                return target;
            }
        }
        if (hint != null) {
            JumpTarget mapped = locateByMappingLine(index, hint);
            if (mapped != null) {
                return mapped;
            }
        }
        return null;
    }

    private static JavaParser createParser() {
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
        return new JavaParser(config);
    }

    private static CompilationUnit tryParse(String code) {
        if (code == null) {
            return null;
        }
        synchronized (PARSE_LOCK) {
            if (lastParsedCode != null && lastParsedCode.equals(code)) {
                return lastParsedUnit;
            }
            try {
                ParseResult<CompilationUnit> result = PARSER.parse(code);
                CompilationUnit cu = result.getResult().orElse(null);
                lastParsedCode = code;
                lastParsedUnit = cu;
                return cu;
            } catch (Throwable ignored) {
                lastParsedCode = code;
                lastParsedUnit = null;
                return null;
            }
        }
    }

    private static JumpTarget locateByAstWithHint(LineIndex index,
                                                  CompilationUnit cu,
                                                  String methodName,
                                                  String methodDesc,
                                                  RangeHint hint) {
        if (index == null || cu == null || methodName == null || hint == null) {
            return null;
        }
        int total = index.lineCount();
        int minLine = clampLine(hint.minLine, total);
        int maxLine = clampLine(hint.maxLine, total);
        int midLine = clampLine((minLine + maxLine) / 2, total);
        List<Integer> anchors = new ArrayList<>(3);
        anchors.add(midLine);
        if (minLine != midLine) {
            anchors.add(minLine);
        }
        if (maxLine != midLine && maxLine != minLine) {
            anchors.add(maxLine);
        }

        for (int anchor : anchors) {
            JumpTarget target = locateByAstAnchor(index, cu, methodName, methodDesc, anchor);
            if (target != null && target.confidence != Confidence.NONE) {
                return target;
            }
        }

        // If we couldn't locate by a single anchor line, try overlap with the full hint span.
        JumpTarget target = locateByAstOverlap(index, cu, methodName, methodDesc, minLine, maxLine);
        if (target != null && target.confidence != Confidence.NONE) {
            return target;
        }
        return null;
    }

    private static JumpTarget locateByAstAnchor(LineIndex index,
                                                CompilationUnit cu,
                                                String methodName,
                                                String methodDesc,
                                                int anchorLine) {
        if ("<clinit>".equals(methodName)) {
            List<InitializerDeclaration> inits = cu.findAll(InitializerDeclaration.class);
            List<InitializerDeclaration> statics = new ArrayList<>();
            for (InitializerDeclaration init : inits) {
                if (init != null && init.isStatic() && init.getRange().isPresent()) {
                    statics.add(init);
                }
            }
            InitializerDeclaration hit = selectByAnchorLine(statics, anchorLine);
            if (hit != null) {
                return buildStaticInitTarget(index, hit);
            }
            return null;
        }

        if ("<init>".equals(methodName)) {
            List<ConstructorDeclaration> ctors = cu.findAll(ConstructorDeclaration.class);
            List<ConstructorDeclaration> candidates = new ArrayList<>();
            for (ConstructorDeclaration ctor : ctors) {
                if (ctor != null && ctor.getRange().isPresent()) {
                    candidates.add(ctor);
                }
            }
            ConstructorDeclaration hit = selectByAnchorLine(candidates, anchorLine);
            if (hit != null) {
                JumpTarget byName = buildNameTarget(index, hit.getName().getRange().orElse(null));
                if (byName != null) {
                    return byName;
                }
                Range fallback = hit.getRange().orElse(null);
                return buildLineTarget(index, fallback, Confidence.MAPPING_LINE, "ctor-range");
            }
            return null;
        }

        List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
        List<MethodDeclaration> candidates = new ArrayList<>();
        for (MethodDeclaration md : methods) {
            if (md == null || !md.getRange().isPresent()) {
                continue;
            }
            if (methodName.equals(md.getNameAsString())) {
                candidates.add(md);
            }
        }
        MethodDeclaration hit = selectByAnchorLine(candidates, anchorLine);
        if (hit != null) {
            JumpTarget byName = buildNameTarget(index, hit.getName().getRange().orElse(null));
            if (byName != null) {
                return byName;
            }
            Range fallback = hit.getRange().orElse(null);
            return buildLineTarget(index, fallback, Confidence.MAPPING_LINE, "method-range");
        }
        return null;
    }

    private static JumpTarget locateByAstOverlap(LineIndex index,
                                                 CompilationUnit cu,
                                                 String methodName,
                                                 String methodDesc,
                                                 int minLine,
                                                 int maxLine) {
        if (index == null || cu == null || methodName == null) {
            return null;
        }
        if (minLine <= 0 || maxLine <= 0) {
            return null;
        }
        int bestScore = Integer.MIN_VALUE;
        Node best = null;
        if ("<clinit>".equals(methodName)) {
            for (InitializerDeclaration init : cu.findAll(InitializerDeclaration.class)) {
                if (init == null || !init.isStatic()) {
                    continue;
                }
                Range r = init.getRange().orElse(null);
                int score = overlapScore(r, minLine, maxLine);
                if (score > bestScore) {
                    bestScore = score;
                    best = init;
                }
            }
            if (best instanceof InitializerDeclaration) {
                return buildStaticInitTarget(index, (InitializerDeclaration) best);
            }
            return null;
        }
        if ("<init>".equals(methodName)) {
            for (ConstructorDeclaration ctor : cu.findAll(ConstructorDeclaration.class)) {
                if (ctor == null) {
                    continue;
                }
                Range r = ctor.getRange().orElse(null);
                int score = overlapScore(r, minLine, maxLine);
                if (score > bestScore) {
                    bestScore = score;
                    best = ctor;
                }
            }
            if (best instanceof ConstructorDeclaration) {
                Range nameRange = ((ConstructorDeclaration) best).getName().getRange().orElse(null);
                JumpTarget byName = buildNameTarget(index, nameRange);
                if (byName != null) {
                    return byName;
                }
                return buildLineTarget(index, ((ConstructorDeclaration) best).getRange().orElse(null),
                        Confidence.MAPPING_LINE, "ctor-overlap");
            }
            return null;
        }
        for (MethodDeclaration md : cu.findAll(MethodDeclaration.class)) {
            if (md == null) {
                continue;
            }
            if (!methodName.equals(md.getNameAsString())) {
                continue;
            }
            Range r = md.getRange().orElse(null);
            int score = overlapScore(r, minLine, maxLine);
            if (score > bestScore) {
                bestScore = score;
                best = md;
            }
        }
        if (best instanceof MethodDeclaration) {
            Range nameRange = ((MethodDeclaration) best).getName().getRange().orElse(null);
            JumpTarget byName = buildNameTarget(index, nameRange);
            if (byName != null) {
                return byName;
            }
            return buildLineTarget(index, ((MethodDeclaration) best).getRange().orElse(null),
                    Confidence.MAPPING_LINE, "method-overlap");
        }
        return null;
    }

    private static int overlapScore(Range range, int hintMin, int hintMax) {
        if (range == null) {
            return Integer.MIN_VALUE;
        }
        int begin = range.begin.line;
        int end = range.end.line;
        int overlap = Math.max(0, Math.min(end, hintMax) - Math.max(begin, hintMin) + 1);
        int span = Math.max(1, end - begin + 1);
        // Prefer nodes that overlap more and span less.
        return overlap * 1000 - span;
    }

    private static <T extends Node> T selectByAnchorLine(List<T> nodes, int anchorLine) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        T bestContaining = null;
        int bestSpan = Integer.MAX_VALUE;
        T bestNearest = null;
        int bestDistance = Integer.MAX_VALUE;

        for (T node : nodes) {
            if (node == null) {
                continue;
            }
            Range range = node.getRange().orElse(null);
            if (range == null) {
                continue;
            }
            int begin = range.begin.line;
            int end = range.end.line;
            int span = Math.max(0, end - begin);
            if (anchorLine >= begin && anchorLine <= end) {
                if (span < bestSpan) {
                    bestSpan = span;
                    bestContaining = node;
                }
            } else {
                int dist = anchorLine < begin ? (begin - anchorLine) : (anchorLine - end);
                if (dist < bestDistance || (dist == bestDistance && span < bestSpan)) {
                    bestDistance = dist;
                    bestSpan = span;
                    bestNearest = node;
                }
            }
        }
        return bestContaining != null ? bestContaining : bestNearest;
    }

    private static JumpTarget locateByAstDescriptor(LineIndex index,
                                                    CompilationUnit cu,
                                                    String internalClassName,
                                                    String methodName,
                                                    String methodDesc) {
        if (index == null || cu == null || methodName == null || methodName.trim().isEmpty()) {
            return null;
        }
        if ("<clinit>".equals(methodName)) {
            List<InitializerDeclaration> inits = cu.findAll(InitializerDeclaration.class);
            InitializerDeclaration best = null;
            int bestLine = Integer.MAX_VALUE;
            for (InitializerDeclaration init : inits) {
                if (init == null || !init.isStatic()) {
                    continue;
                }
                Range r = init.getRange().orElse(null);
                if (r == null) {
                    continue;
                }
                int line = r.begin.line;
                if (line < bestLine) {
                    bestLine = line;
                    best = init;
                }
            }
            if (best != null) {
                return buildStaticInitTarget(index, best);
            }
            return null;
        }
        if ("<init>".equals(methodName)) {
            List<ConstructorDeclaration> ctors = cu.findAll(ConstructorDeclaration.class);
            JumpTarget exact = selectByDescriptor(index, ctors, null, methodDesc);
            if (exact != null) {
                return exact;
            }
            return null;
        }

        List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
        List<MethodDeclaration> named = new ArrayList<>();
        for (MethodDeclaration md : methods) {
            if (md != null && methodName.equals(md.getNameAsString())) {
                named.add(md);
            }
        }
        JumpTarget exact = selectByDescriptor(index, named, methodName, methodDesc);
        if (exact != null) {
            return exact;
        }
        // Special case: constructor call may show up as a method name equal to class simple name.
        if (internalClassName != null) {
            String ctorName = pickCtorNameCandidates(internalClassName);
            if (ctorName != null && ctorName.equals(methodName)) {
                List<ConstructorDeclaration> ctors = cu.findAll(ConstructorDeclaration.class);
                JumpTarget ctor = selectByDescriptor(index, ctors, null, methodDesc);
                if (ctor != null) {
                    return ctor;
                }
            }
        }
        return null;
    }

    private static JumpTarget selectByDescriptor(LineIndex index,
                                                 List<? extends Node> declarations,
                                                 String methodName,
                                                 String methodDesc) {
        if (index == null || declarations == null || declarations.isEmpty()) {
            return null;
        }
        ExpectedSignature expected = ExpectedSignature.fromDesc(methodDesc);
        int bestScore = Integer.MIN_VALUE;
        Node best = null;

        for (Node decl : declarations) {
            if (decl == null) {
                continue;
            }
            if (decl instanceof MethodDeclaration) {
                MethodDeclaration md = (MethodDeclaration) decl;
                if (methodName != null && !methodName.equals(md.getNameAsString())) {
                    continue;
                }
                int score = scoreSignature(expected, md.getParameters(), false);
                if (score > bestScore) {
                    bestScore = score;
                    best = md;
                }
            } else if (decl instanceof ConstructorDeclaration) {
                ConstructorDeclaration cd = (ConstructorDeclaration) decl;
                int score = scoreSignature(expected, cd.getParameters(), true);
                if (score > bestScore) {
                    bestScore = score;
                    best = cd;
                }
            }
        }

        if (best == null) {
            return null;
        }
        if (best instanceof MethodDeclaration) {
            Range nameRange = ((MethodDeclaration) best).getName().getRange().orElse(null);
            JumpTarget byName = buildNameTarget(index, nameRange);
            if (byName != null) {
                return byName;
            }
        } else if (best instanceof ConstructorDeclaration) {
            Range nameRange = ((ConstructorDeclaration) best).getName().getRange().orElse(null);
            JumpTarget byName = buildNameTarget(index, nameRange);
            if (byName != null) {
                return byName;
            }
        }
        Range fallback = best.getRange().orElse(null);
        return buildLineTarget(index, fallback, Confidence.MAPPING_LINE, "desc-fallback");
    }

    private static int scoreSignature(ExpectedSignature expected,
                                      List<com.github.javaparser.ast.body.Parameter> params,
                                      boolean ctor) {
        if (params == null) {
            params = Collections.emptyList();
        }
        if (expected == null) {
            // No descriptor: only arg count can be trusted.
            return 10_000 - params.size();
        }
        if (expected.argCount != params.size()) {
            return Integer.MIN_VALUE / 4;
        }
        if (expected.params == null || expected.params.isEmpty()) {
            return 20_000;
        }
        int matched = 0;
        int unknown = 0;
        for (int i = 0; i < params.size(); i++) {
            com.github.javaparser.ast.body.Parameter p = params.get(i);
            ExpectedParam ep = expected.params.get(i);
            ParamShape ps = ParamShape.fromParameter(p);
            if (ps == null || ep == null) {
                unknown++;
                continue;
            }
            if (ps.matches(ep)) {
                matched++;
            }
        }
        // Strongly prefer full matches; otherwise still consider it but keep it lower.
        if (matched == expected.params.size()) {
            return 30_000;
        }
        return 10_000 + matched * 100 - unknown * 10;
    }

    private static JumpTarget locateByLocalText(LineIndex index,
                                                String internalClassName,
                                                String methodName,
                                                RangeHint hint) {
        if (index == null || hint == null || methodName == null || methodName.trim().isEmpty()) {
            return null;
        }
        int total = index.lineCount();
        int minLine = clampLine(hint.minLine, total);
        int from = Math.max(1, minLine - WINDOW_LINES_ABOVE);
        int to = Math.min(total, minLine + WINDOW_LINES_BELOW);

        if ("<clinit>".equals(methodName)) {
            return findStaticBlockSignature(index, from, to, minLine);
        }
        if ("<init>".equals(methodName)) {
            List<String> names = ctorNameCandidates(internalClassName);
            JumpTarget best = null;
            int bestScore = Integer.MIN_VALUE;
            for (String name : names) {
                JumpTarget jt = findMethodLikeSignature(index, name, from, to, minLine);
                if (jt == null) {
                    continue;
                }
                int line = indexOffsetToLine(index, jt.startOffset);
                int score = scoreCandidateLine(line, minLine);
                if (score > bestScore) {
                    bestScore = score;
                    best = jt;
                }
            }
            return best;
        }
        return findMethodLikeSignature(index, methodName, from, to, minLine);
    }

    private static JumpTarget findStaticBlockSignature(LineIndex index,
                                                       int fromLine,
                                                       int toLine,
                                                       int anchorLine) {
        JumpTarget best = null;
        int bestScore = Integer.MIN_VALUE;
        for (int line = fromLine; line <= toLine; line++) {
            String text = index.lineText(line);
            if (text == null) {
                continue;
            }
            if (looksLikeCommentLine(text)) {
                continue;
            }
            int idx = indexOfWord(text, "static");
            if (idx < 0) {
                continue;
            }
            int brace = text.indexOf('{', idx);
            if (brace < 0) {
                continue;
            }
            // Filter out "static class" / "static interface" etc.
            String after = text.substring(idx).toLowerCase(Locale.ROOT);
            if (after.contains(" static class") || after.contains(" static interface")) {
                continue;
            }
            int score = scoreCandidateLine(line, anchorLine);
            if (score <= bestScore) {
                continue;
            }
            int start = index.lineStartOffset(line) + idx;
            int end = start + "static".length();
            bestScore = score;
            best = JumpTarget.of(start, Math.min(index.codeLength(), end),
                    Confidence.TEXT_SIGNATURE, "text-static");
        }
        return best;
    }

    private static JumpTarget findMethodLikeSignature(LineIndex index,
                                                      String name,
                                                      int fromLine,
                                                      int toLine,
                                                      int anchorLine) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String needle = name.trim();
        JumpTarget best = null;
        int bestScore = Integer.MIN_VALUE;
        for (int line = fromLine; line <= toLine; line++) {
            String text = index.lineText(line);
            if (text == null) {
                continue;
            }
            if (looksLikeCommentLine(text)) {
                continue;
            }
            int pos = 0;
            while (pos < text.length()) {
                int idx = indexOfWord(text, needle, pos);
                if (idx < 0) {
                    break;
                }
                int after = idx + needle.length();
                int p = skipSpaces(text, after);
                if (p < text.length() && text.charAt(p) == '(') {
                    if (looksLikeDeclarationAround(index, line, idx, p)) {
                        int score = scoreCandidateLine(line, anchorLine);
                        if (score > bestScore) {
                            int start = index.lineStartOffset(line) + idx;
                            int end = start + needle.length();
                            bestScore = score;
                            best = JumpTarget.of(start, Math.min(index.codeLength(), end),
                                    Confidence.TEXT_SIGNATURE, "text-sig");
                        }
                    }
                }
                pos = idx + needle.length();
            }
        }
        return best;
    }

    private static int scoreCandidateLine(int candidateLine, int anchorLine) {
        if (candidateLine <= 0 || anchorLine <= 0) {
            return Integer.MIN_VALUE / 4;
        }
        if (candidateLine <= anchorLine) {
            return 100_000 - (anchorLine - candidateLine);
        }
        return 50_000 - (candidateLine - anchorLine);
    }

    private static int indexOffsetToLine(LineIndex index, int offset) {
        if (index == null) {
            return 1;
        }
        int total = index.lineCount();
        int best = 1;
        int bestStart = 0;
        for (int line = 1; line <= total; line++) {
            int start = index.lineStartOffset(line);
            if (start <= offset && start >= bestStart) {
                bestStart = start;
                best = line;
            }
        }
        return best;
    }

    private static boolean looksLikeDeclarationAround(LineIndex index, int line, int nameIdx, int parenIdx) {
        String current = index.lineText(line);
        if (current == null) {
            return false;
        }
        // Avoid obvious call sites: "foo(" preceded by "." or "new " heuristics.
        int before = nameIdx - 1;
        if (before >= 0) {
            char c = current.charAt(before);
            if (c == '.' || c == '#') {
                return false;
            }
        }
        String left = current.substring(0, Math.min(nameIdx, current.length())).trim();
        if (left.endsWith("new")) {
            return false;
        }

        // If "{" exists on the same line after the paren, it's likely a declaration.
        int brace = current.indexOf('{', parenIdx);
        if (brace >= 0) {
            return true;
        }

        // Or within the next few lines.
        for (int i = 1; i <= 3; i++) {
            String next = index.lineText(line + i);
            if (next == null) {
                continue;
            }
            String t = next.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (looksLikeCommentLine(next)) {
                continue;
            }
            if (t.startsWith("throws") || t.startsWith("{")) {
                return true;
            }
            if (t.contains("{")) {
                return true;
            }
        }
        return false;
    }

    private static JumpTarget locateByMappingLine(LineIndex index, RangeHint hint) {
        if (index == null || hint == null) {
            return null;
        }
        int total = index.lineCount();
        int line = clampLine(hint.minLine, total);
        int start = index.lineStartOffset(line);
        int end = Math.min(index.codeLength(), start + 1);
        return JumpTarget.of(start, end, Confidence.MAPPING_LINE, "mapping-line");
    }

    private static JumpTarget buildNameTarget(LineIndex index, Range nameRange) {
        if (index == null || nameRange == null) {
            return null;
        }
        int start = index.positionToOffset(nameRange.begin);
        int end = index.positionToOffsetInclusiveEnd(nameRange.end);
        if (start < 0 || end <= start) {
            return null;
        }
        return JumpTarget.of(start, end, Confidence.AST_NAME, "ast-name");
    }

    private static JumpTarget buildStaticInitTarget(LineIndex index, InitializerDeclaration init) {
        if (index == null || init == null) {
            return null;
        }
        Range range = init.getRange().orElse(null);
        if (range == null) {
            return null;
        }
        int line = clampLine(range.begin.line, index.lineCount());
        String text = index.lineText(line);
        if (text != null) {
            int idx = indexOfWord(text, "static");
            if (idx >= 0) {
                int start = index.lineStartOffset(line) + idx;
                int end = Math.min(index.codeLength(), start + "static".length());
                return JumpTarget.of(start, end, Confidence.AST_NAME, "ast-static");
            }
        }
        return buildLineTarget(index, range, Confidence.MAPPING_LINE, "ast-static-fallback");
    }

    private static JumpTarget buildLineTarget(LineIndex index,
                                              Range range,
                                              Confidence confidence,
                                              String debug) {
        if (index == null || range == null) {
            return null;
        }
        int line = clampLine(range.begin.line, index.lineCount());
        int start = index.lineStartOffset(line);
        int end = Math.min(index.codeLength(), start + 1);
        return JumpTarget.of(start, end, confidence, debug);
    }

    private static int clampLine(int line, int totalLines) {
        if (totalLines <= 0) {
            return 1;
        }
        if (line <= 0) {
            return 1;
        }
        return Math.min(line, totalLines);
    }

    private static boolean looksLikeCommentLine(String text) {
        if (text == null) {
            return true;
        }
        String t = text.trim();
        return t.startsWith("//") || t.startsWith("/*") || t.startsWith("*") || t.startsWith("*/");
    }

    private static int skipSpaces(String text, int pos) {
        int p = Math.max(0, pos);
        while (p < text.length()) {
            char c = text.charAt(p);
            if (!Character.isWhitespace(c)) {
                break;
            }
            p++;
        }
        return p;
    }

    private static boolean isIdent(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    private static int indexOfWord(String text, String word) {
        return indexOfWord(text, word, 0);
    }

    private static int indexOfWord(String text, String word, int fromIndex) {
        if (text == null || word == null || word.isEmpty()) {
            return -1;
        }
        int idx = text.indexOf(word, Math.max(0, fromIndex));
        while (idx >= 0) {
            boolean leftOk = idx == 0 || !isIdent(text.charAt(idx - 1));
            int end = idx + word.length();
            boolean rightOk = end >= text.length() || !isIdent(text.charAt(end));
            if (leftOk && rightOk) {
                return idx;
            }
            idx = text.indexOf(word, end);
        }
        return -1;
    }

    private static List<String> ctorNameCandidates(String internalClassName) {
        if (internalClassName == null || internalClassName.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String name = internalClassName.trim();
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        String outer = name;
        String inner = name;
        int dollar = name.lastIndexOf('$');
        if (dollar >= 0 && dollar < name.length() - 1) {
            inner = name.substring(dollar + 1);
        }
        List<String> out = new ArrayList<>(2);
        if (!outer.isEmpty()) {
            out.add(outer);
        }
        if (!inner.isEmpty() && !inner.equals(outer)) {
            out.add(inner);
        }
        return out;
    }

    private static String pickCtorNameCandidates(String internalClassName) {
        List<String> names = ctorNameCandidates(internalClassName);
        return names.isEmpty() ? null : names.get(names.size() - 1);
    }

    private static final class ExpectedSignature {
        final int argCount;
        final List<ExpectedParam> params;

        private ExpectedSignature(int argCount, List<ExpectedParam> params) {
            this.argCount = argCount;
            this.params = params;
        }

        static ExpectedSignature fromDesc(String desc) {
            if (desc == null || desc.trim().isEmpty()) {
                return null;
            }
            try {
                Type[] args = Type.getArgumentTypes(desc);
                List<ExpectedParam> out = new ArrayList<>(args.length);
                for (Type t : args) {
                    out.add(ExpectedParam.fromAsmType(t));
                }
                return new ExpectedSignature(args.length, out);
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    private static final class ExpectedParam {
        final boolean primitive;
        final String base;
        final String innerBase;
        final int dims;

        private ExpectedParam(boolean primitive, String base, String innerBase, int dims) {
            this.primitive = primitive;
            this.base = base;
            this.innerBase = innerBase;
            this.dims = Math.max(0, dims);
        }

        static ExpectedParam fromAsmType(Type type) {
            if (type == null) {
                return null;
            }
            int dims = 0;
            Type t = type;
            if (t.getSort() == Type.ARRAY) {
                dims = t.getDimensions();
                t = t.getElementType();
            }
            if (t.getSort() >= Type.BOOLEAN && t.getSort() <= Type.DOUBLE) {
                return new ExpectedParam(true, t.getClassName(), t.getClassName(), dims);
            }
            if (t.getSort() == Type.OBJECT) {
                String internal = t.getInternalName();
                String base = internal == null ? t.getClassName() : internal;
                if (base.contains("/")) {
                    base = base.substring(base.lastIndexOf('/') + 1);
                }
                String inner = base;
                if (inner.contains("$")) {
                    inner = inner.substring(inner.lastIndexOf('$') + 1);
                }
                return new ExpectedParam(false, base, inner, dims);
            }
            return new ExpectedParam(false, t.getClassName(), t.getClassName(), dims);
        }
    }

    private static final class ParamShape {
        final boolean primitive;
        final String base;
        final String innerBase;
        final int dims;

        private ParamShape(boolean primitive, String base, String innerBase, int dims) {
            this.primitive = primitive;
            this.base = base;
            this.innerBase = innerBase;
            this.dims = Math.max(0, dims);
        }

        static ParamShape fromParameter(com.github.javaparser.ast.body.Parameter p) {
            if (p == null) {
                return null;
            }
            com.github.javaparser.ast.type.Type t = p.getType();
            if (t == null) {
                return null;
            }
            int dims = 0;
            com.github.javaparser.ast.type.Type baseType = t;
            while (baseType.isArrayType()) {
                dims++;
                baseType = baseType.asArrayType().getComponentType();
            }
            if (p.isVarArgs()) {
                dims++;
            }
            if (baseType.isPrimitiveType()) {
                String name = baseType.asPrimitiveType().toString();
                return new ParamShape(true, name, name, dims);
            }
            if (baseType.isClassOrInterfaceType()) {
                String name = baseType.asClassOrInterfaceType().getName().getIdentifier();
                String inner = name;
                if (inner.contains("$")) {
                    inner = inner.substring(inner.lastIndexOf('$') + 1);
                }
                return new ParamShape(false, name, inner, dims);
            }
            if (baseType.isVarType()) {
                return null;
            }
            // Fallback to a simple text-based base (strip generics).
            String raw = baseType.toString();
            String cleaned = raw.replaceAll("<.*>", "");
            if (cleaned.contains(".")) {
                cleaned = cleaned.substring(cleaned.lastIndexOf('.') + 1);
            }
            String inner = cleaned;
            if (inner.contains("$")) {
                inner = inner.substring(inner.lastIndexOf('$') + 1);
            }
            return new ParamShape(false, cleaned, inner, dims);
        }

        boolean matches(ExpectedParam expected) {
            if (expected == null) {
                return false;
            }
            if (this.dims != expected.dims) {
                return false;
            }
            if (this.primitive || expected.primitive) {
                return this.primitive == expected.primitive
                        && this.base != null
                        && this.base.equals(expected.base);
            }
            if (this.base == null || expected.base == null) {
                return false;
            }
            if (this.base.equals(expected.base) || this.base.equals(expected.innerBase)) {
                return true;
            }
            return this.innerBase != null && (this.innerBase.equals(expected.base) || this.innerBase.equals(expected.innerBase));
        }
    }

    private static final class LineIndex {
        private final String code;
        private final int[] starts;

        private LineIndex(String code) {
            this.code = code;
            int lines = 1;
            for (int i = 0; i < code.length(); i++) {
                if (code.charAt(i) == '\n') {
                    lines++;
                }
            }
            int[] tmp = new int[lines + 1];
            int line = 0;
            tmp[line++] = 0;
            for (int i = 0; i < code.length(); i++) {
                if (code.charAt(i) == '\n') {
                    tmp[line++] = i + 1;
                }
            }
            tmp[line] = code.length();
            this.starts = tmp;
        }

        int codeLength() {
            return code.length();
        }

        int lineCount() {
            return Math.max(1, starts.length - 1);
        }

        int lineStartOffset(int line) {
            int total = lineCount();
            int l = clampLine(line, total);
            return starts[l - 1];
        }

        int lineEndContentExclusive(int line) {
            int total = lineCount();
            int l = clampLine(line, total);
            int nextStart = starts[l];
            if (l < total && nextStart > 0 && nextStart <= code.length() && code.charAt(nextStart - 1) == '\n') {
                return nextStart - 1;
            }
            return nextStart;
        }

        String lineText(int line) {
            int total = lineCount();
            if (line <= 0 || line > total) {
                return null;
            }
            int start = lineStartOffset(line);
            int end = lineEndContentExclusive(line);
            if (start < 0 || end < start || start > code.length()) {
                return null;
            }
            end = Math.min(end, code.length());
            return code.substring(start, end);
        }

        int positionToOffset(Position position) {
            if (position == null) {
                return -1;
            }
            int total = lineCount();
            int line = position.line;
            if (line <= 0 || line > total) {
                return -1;
            }
            int start = lineStartOffset(line);
            int end = lineEndContentExclusive(line);
            int lineLen = Math.max(0, end - start);
            int column = Math.max(1, position.column);
            int offsetInLine = Math.min(column - 1, lineLen);
            return start + offsetInLine;
        }

        int positionToOffsetInclusiveEnd(Position position) {
            int off = positionToOffset(position);
            if (off < 0) {
                return off;
            }
            return Math.min(code.length(), off + 1);
        }
    }
}
