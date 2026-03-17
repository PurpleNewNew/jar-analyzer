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

/**
 * Locate a method/constructor/static initializer in decompiled Java source.
 *
 * Main path is:
 * 1. AST + descriptor match
 * 2. exact decompiled line fallback from CFR mapping
 */
public final class DecompiledMethodLocator {
    private static final int FULL_SIGNATURE_SCORE = 30_000;
    private static final Object PARSE_LOCK = new Object();
    private static final JavaParser PARSER = createParser();
    private static String lastParsedCode = null;
    private static CompilationUnit lastParsedUnit = null;

    private DecompiledMethodLocator() {
    }

    public enum Confidence {
        AST_NAME,
        AST_RANGE,
        MAPPING_LINE,
        NONE
    }

    public static final class RangeHint {
        public final int line;

        public RangeHint(int line) {
            this.line = line;
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
        CompilationUnit cu = tryParse(code);
        if (cu != null) {
            JumpTarget target = locateByAstSignature(index, cu, internalClassName, trimmed, methodDesc);
            if (target != null && target.confidence != Confidence.NONE) {
                return target;
            }
        }
        if (hint != null) {
            return locateByMappingLine(index, hint);
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

    private static JumpTarget locateByAstSignature(LineIndex index,
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
            return best == null ? null : buildStaticInitTarget(index, best);
        }
        if ("<init>".equals(methodName)) {
            return selectByDescriptor(index, cu.findAll(ConstructorDeclaration.class), null, methodDesc);
        }

        List<MethodDeclaration> named = new ArrayList<>();
        for (MethodDeclaration md : cu.findAll(MethodDeclaration.class)) {
            if (md != null && methodName.equals(md.getNameAsString())) {
                named.add(md);
            }
        }
        JumpTarget exact = selectByDescriptor(index, named, methodName, methodDesc);
        if (exact != null) {
            return exact;
        }
        if (internalClassName != null) {
            String ctorName = pickCtorNameCandidates(internalClassName);
            if (ctorName != null && ctorName.equals(methodName)) {
                return selectByDescriptor(index, cu.findAll(ConstructorDeclaration.class), null, methodDesc);
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
        if (expected == null) {
            if (declarations.size() != 1) {
                return null;
            }
            return buildDeclarationTarget(index, declarations.get(0), "single-decl");
        }

        int bestScore = Integer.MIN_VALUE;
        Node best = null;
        int candidateCount = 0;
        for (Node decl : declarations) {
            if (decl == null) {
                continue;
            }
            int score;
            if (decl instanceof MethodDeclaration) {
                MethodDeclaration md = (MethodDeclaration) decl;
                if (methodName != null && !methodName.equals(md.getNameAsString())) {
                    continue;
                }
                score = scoreSignature(expected, md.getParameters());
            } else if (decl instanceof ConstructorDeclaration) {
                ConstructorDeclaration cd = (ConstructorDeclaration) decl;
                score = scoreSignature(expected, cd.getParameters());
            } else {
                continue;
            }
            candidateCount++;
            if (score > bestScore) {
                bestScore = score;
                best = decl;
            }
        }
        if (best == null) {
            return null;
        }
        if (bestScore < FULL_SIGNATURE_SCORE && candidateCount > 1) {
            return null;
        }
        return buildDeclarationTarget(index, best, bestScore >= FULL_SIGNATURE_SCORE ? "ast-desc" : "ast-partial");
    }

    private static JumpTarget buildDeclarationTarget(LineIndex index, Node declaration, String debug) {
        if (index == null || declaration == null) {
            return null;
        }
        if (declaration instanceof MethodDeclaration) {
            Range nameRange = ((MethodDeclaration) declaration).getName().getRange().orElse(null);
            JumpTarget byName = buildNameTarget(index, nameRange);
            if (byName != null) {
                return JumpTarget.of(byName.startOffset, byName.endOffset, Confidence.AST_NAME, debug);
            }
        } else if (declaration instanceof ConstructorDeclaration) {
            Range nameRange = ((ConstructorDeclaration) declaration).getName().getRange().orElse(null);
            JumpTarget byName = buildNameTarget(index, nameRange);
            if (byName != null) {
                return JumpTarget.of(byName.startOffset, byName.endOffset, Confidence.AST_NAME, debug);
            }
        }
        Range fallback = declaration.getRange().orElse(null);
        return buildLineTarget(index, fallback, Confidence.AST_RANGE, debug + "-line");
    }

    private static int scoreSignature(ExpectedSignature expected,
                                      List<com.github.javaparser.ast.body.Parameter> params) {
        if (expected == null) {
            return Integer.MIN_VALUE / 4;
        }
        if (params == null) {
            params = Collections.emptyList();
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
        if (matched == expected.params.size()) {
            return FULL_SIGNATURE_SCORE;
        }
        return 10_000 + matched * 100 - unknown * 10;
    }

    private static JumpTarget locateByMappingLine(LineIndex index, RangeHint hint) {
        if (index == null || hint == null) {
            return null;
        }
        int total = index.lineCount();
        int line = clampLine(hint.line, total);
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
        return buildLineTarget(index, range, Confidence.AST_RANGE, "ast-static-line");
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
            return this.innerBase != null
                    && (this.innerBase.equals(expected.base) || this.innerBase.equals(expected.innerBase));
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
