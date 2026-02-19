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
import com.github.javaparser.ast.expr.MethodCallExpr;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.entity.MemberEntity;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.gui.runtime.model.EditorDeclarationResultDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.EditorDeclarationTargetDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.EditorDocumentDto;
import me.n1ar4.jar.analyzer.semantic.CallContext;
import me.n1ar4.jar.analyzer.semantic.CallPosition;
import me.n1ar4.jar.analyzer.semantic.CallResolver;
import me.n1ar4.jar.analyzer.semantic.CallSiteSelection;
import me.n1ar4.jar.analyzer.semantic.EnclosingCallable;
import me.n1ar4.jar.analyzer.semantic.SemanticResolver;
import me.n1ar4.jar.analyzer.semantic.SymbolKind;
import me.n1ar4.jar.analyzer.semantic.SymbolRef;
import me.n1ar4.jar.analyzer.semantic.TypeRef;
import me.n1ar4.jar.analyzer.semantic.TypeSolver;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EditorDeclarationResolver {
    private static final Logger logger = LogManager.getLogger();
    private static final Object PARSE_LOCK = new Object();
    private static final JavaParser PARSER = createParser();
    private static String lastCode = null;
    private static CompilationUnit lastUnit = null;
    private static final int MAX_CANDIDATES = 64;

    private EditorDeclarationResolver() {
    }

    public static EditorDeclarationResultDto resolve(CoreEngine engine,
                                                     EditorDocumentDto doc,
                                                     int caretOffset) {
        if (engine == null || !engine.isEnabled()) {
            return EditorDeclarationResultDto.empty("engine is not ready");
        }
        if (doc == null) {
            return EditorDeclarationResultDto.empty("editor document is not ready");
        }
        String code = safe(doc.content());
        if (code.isBlank()) {
            return EditorDeclarationResultDto.empty("editor content is empty");
        }

        TextIndex index = new TextIndex(code);
        int offset = index.clampOffset(caretOffset);
        String token = extractToken(code, offset);

        CompilationUnit cu = parse(code);
        if (cu == null) {
            List<EditorDeclarationTargetDto> fallback = resolveTokenFallback(engine, doc, token);
            if (fallback.isEmpty()) {
                return new EditorDeclarationResultDto(token, "unknown", List.of(), "semantic parse failed");
            }
            return new EditorDeclarationResultDto(token, "unknown", fallback, "fallback declaration candidates: " + fallback.size());
        }

        TypeSolver typeSolver = new TypeSolver(engine);
        SemanticResolver semanticResolver = new SemanticResolver(engine, typeSolver);
        Position pos = index.toPosition(offset);
        SymbolRef symbol = semanticResolver.resolveSemanticTarget(cu, pos, token);
        String symbolKind = normalizeKind(symbol);

        List<EditorDeclarationTargetDto> targets = resolveSymbolTargets(
                engine, doc, cu, pos, symbol, typeSolver, index);
        if (targets.isEmpty()) {
            targets = resolveTokenFallback(engine, doc, token);
        }
        if (targets.isEmpty()) {
            return new EditorDeclarationResultDto(token, symbolKind, List.of(), "declaration not found");
        }
        return new EditorDeclarationResultDto(
                token,
                symbolKind,
                targets,
                "declaration candidates: " + targets.size()
        );
    }

    private static List<EditorDeclarationTargetDto> resolveSymbolTargets(CoreEngine engine,
                                                                         EditorDocumentDto doc,
                                                                         CompilationUnit cu,
                                                                         Position pos,
                                                                         SymbolRef symbol,
                                                                         TypeSolver typeSolver,
                                                                         TextIndex index) {
        List<EditorDeclarationTargetDto> targets = new ArrayList<>();
        if (symbol == null) {
            return targets;
        }

        Range declarationRange = symbol.getDeclarationRange();
        if (declarationRange != null && declarationRange.begin != null) {
            int localOffset = index.toOffset(declarationRange.begin);
            if (localOffset >= 0) {
                targets.add(new EditorDeclarationTargetDto(
                        "",
                        "",
                        "",
                        "",
                        0,
                        localOffset,
                        "local",
                        "semantic"
                ));
            }
        }

        SymbolKind kind = symbol.getKind();
        if (kind == SymbolKind.CLASS
                || kind == SymbolKind.INTERFACE
                || kind == SymbolKind.ENUM
                || kind == SymbolKind.ANNOTATION
                || kind == SymbolKind.RECORD) {
            String owner = normalizeClassName(symbol.getOwner());
            String simpleName = safe(symbol.getName()).trim();
            if (owner == null && (simpleName.contains("/") || simpleName.contains("."))) {
                owner = normalizeClassName(simpleName);
            }
            if (owner == null && !simpleName.isBlank()) {
                List<String> classes = engine.includeClassByClassName(simpleName, true);
                int count = 0;
                for (String className : classes) {
                    if (className == null || !className.endsWith("/" + simpleName)) {
                        continue;
                    }
                    appendClassTargets(engine, targets, className, "semantic");
                    count++;
                    if (count >= 20) {
                        break;
                    }
                }
            }
            if (owner != null) {
                appendClassTargets(engine, targets, owner, "semantic");
            }
            return rankAndLimitTargets(targets, doc, null);
        }

        if (kind == SymbolKind.METHOD || kind == SymbolKind.CONSTRUCTOR) {
            appendMethodTargets(engine, doc, cu, pos, symbol, typeSolver, targets);
            return rankAndLimitTargets(targets, doc, null);
        }

        if (kind == SymbolKind.FIELD || kind == SymbolKind.VARIABLE || kind == SymbolKind.TYPE_PARAM) {
            appendFieldTargets(engine, doc, symbol, targets);
            return rankAndLimitTargets(targets, doc, null);
        }
        return rankAndLimitTargets(targets, doc, null);
    }

    private static void appendMethodTargets(CoreEngine engine,
                                            EditorDocumentDto doc,
                                            CompilationUnit cu,
                                            Position pos,
                                            SymbolRef symbol,
                                            TypeSolver typeSolver,
                                            List<EditorDeclarationTargetDto> out) {
        String owner = normalizeClassName(symbol.getOwner());
        String currentClass = normalizeClassName(doc == null ? "" : doc.className());
        if (owner == null) {
            owner = currentClass;
        }
        if (owner == null) {
            return;
        }

        String methodName = safe(symbol.getName());
        if (methodName.isBlank()) {
            return;
        }
        if (symbol.getKind() == SymbolKind.CONSTRUCTOR) {
            methodName = "<init>";
        }
        int argCount = symbol.getArgCount();
        ResolvedMethodTarget exactTarget = resolveExactMethodTarget(
                cu, pos, owner, methodName, argCount, doc, typeSolver);
        if (exactTarget != null) {
            if (exactTarget.className != null && !exactTarget.className.isBlank()) {
                owner = normalizeClassName(exactTarget.className);
            }
            if (exactTarget.methodName != null && !exactTarget.methodName.isBlank()) {
                methodName = exactTarget.methodName;
            }
        }

        String exactDesc = exactTarget == null ? null : safe(exactTarget.methodDesc);
        List<MethodResult> methods = exactDesc.isBlank()
                ? engine.getMethod(owner, methodName, null)
                : engine.getMethod(owner, methodName, exactDesc);
        if ((methods == null || methods.isEmpty()) && !exactDesc.isBlank()) {
            methods = engine.getMethod(owner, methodName, null);
        }
        if (methods == null || methods.isEmpty()) {
            return;
        }
        List<MethodResult> filtered = filterMethodsByArgCount(methods, argCount);
        if (!filtered.isEmpty() && exactDesc.isBlank()) {
            methods = filtered;
        }
        if (exactDesc.isBlank()) {
            exactDesc = resolveExactMethodDesc(cu, pos, owner, methodName, argCount, doc, typeSolver);
        }
        for (MethodResult method : methods) {
            if (method == null) {
                continue;
            }
            String cls = normalizeClassName(method.getClassName());
            if (cls == null) {
                cls = owner;
            }
            out.add(new EditorDeclarationTargetDto(
                    cls == null ? "" : cls,
                    safe(method.getMethodName()),
                    safe(method.getMethodDesc()),
                    safe(method.getJarName()),
                    Math.max(0, method.getJarId()),
                    -1,
                    "method",
                    exactDesc != null && exactDesc.equals(method.getMethodDesc())
                            ? (exactTarget != null && exactTarget.fromCallSite ? "semantic-callsite" : "semantic-exact")
                            : "semantic"
            ));
        }
    }

    private static void appendFieldTargets(CoreEngine engine,
                                           EditorDocumentDto doc,
                                           SymbolRef symbol,
                                           List<EditorDeclarationTargetDto> out) {
        String owner = normalizeClassName(symbol.getOwner());
        if (owner == null) {
            owner = normalizeClassName(doc == null ? "" : doc.className());
        }
        if (owner == null) {
            return;
        }
        String name = safe(symbol.getName());
        if (name.isBlank()) {
            appendClassTargets(engine, out, owner, "semantic-field");
            return;
        }
        List<MemberEntity> members = engine.getMembersByClassAndName(owner, name);
        if (members == null || members.isEmpty()) {
            appendClassTargets(engine, out, owner, "semantic-field");
            return;
        }
        for (MemberEntity member : members) {
            if (member == null) {
                continue;
            }
            String cls = normalizeClassName(member.getClassName());
            if (cls == null) {
                cls = owner;
            }
            Integer jarId = member.getJarId();
            String jarName = jarId == null ? "" : safe(engine.getJarNameById(jarId));
            out.add(new EditorDeclarationTargetDto(
                    cls == null ? "" : cls,
                    "",
                    "",
                    jarName,
                    jarId == null ? 0 : Math.max(0, jarId),
                    -1,
                    "field",
                    "semantic"
            ));
        }
    }

    private static void appendClassTargets(CoreEngine engine,
                                           List<EditorDeclarationTargetDto> out,
                                           String className,
                                           String source) {
        String normalized = normalizeClassName(className);
        if (normalized == null) {
            return;
        }
        List<ClassResult> classes = engine.getClassesByClass(normalized);
        if (classes == null || classes.isEmpty()) {
            ClassResult item = engine.getClassByClass(normalized);
            if (item == null) {
                out.add(new EditorDeclarationTargetDto(
                        normalized,
                        "",
                        "",
                        "",
                        0,
                        -1,
                        "class",
                        source
                ));
                return;
            }
            classes = List.of(item);
        }
        for (ClassResult item : classes) {
            if (item == null) {
                continue;
            }
            out.add(new EditorDeclarationTargetDto(
                    safe(normalizeClassName(item.getClassName())),
                    "",
                    "",
                    safe(item.getJarName()),
                    Math.max(0, item.getJarId()),
                    -1,
                    "class",
                    source
            ));
        }
    }

    private static List<EditorDeclarationTargetDto> resolveTokenFallback(CoreEngine engine,
                                                                         EditorDocumentDto doc,
                                                                         String token) {
        String word = safe(token).trim();
        if (word.isBlank()) {
            return List.of();
        }
        List<EditorDeclarationTargetDto> out = new ArrayList<>();
        String currentClass = normalizeClassName(doc == null ? "" : doc.className());
        if (currentClass != null) {
            List<MethodResult> methods = engine.getMethod(currentClass, word, null);
            for (MethodResult method : methods) {
                if (method == null) {
                    continue;
                }
                out.add(new EditorDeclarationTargetDto(
                        safe(normalizeClassName(method.getClassName())),
                        safe(method.getMethodName()),
                        safe(method.getMethodDesc()),
                        safe(method.getJarName()),
                        Math.max(0, method.getJarId()),
                        -1,
                        "method",
                        "fallback-current-class"
                ));
            }
        }
        if (looksLikeClassToken(word)) {
            if (word.contains("/") || word.contains(".")) {
                appendClassTargets(engine, out, word, "fallback-class-token");
            } else {
                List<String> classes = engine.includeClassByClassName(word, true);
                int count = 0;
                for (String className : classes) {
                    if (className == null || !className.endsWith("/" + word)) {
                        continue;
                    }
                    appendClassTargets(engine, out, className, "fallback-class-search");
                    count++;
                    if (count >= 20) {
                        break;
                    }
                }
            }
        }
        return rankAndLimitTargets(out, doc, null);
    }

    private static String resolveExactMethodDesc(CompilationUnit cu,
                                                 Position pos,
                                                 String owner,
                                                 String methodName,
                                                 int argCount,
                                                 EditorDocumentDto doc,
                                                 TypeSolver typeSolver) {
        if (typeSolver == null || owner == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        CallResolver callResolver = typeSolver.getCallResolver();
        if (callResolver == null) {
            return null;
        }
        try {
            MethodCallExpr call = callResolver.findMethodCallAt(cu, pos);
            if (call != null && methodName.equals(call.getNameAsString())) {
                CallContext ctx = buildCallContext(cu, pos, doc, callResolver);
                boolean preferStatic = callResolver.isStaticScope(cu, call, ctx);
                String desc = callResolver.resolveMethodDescByCall(
                        cu, owner, call, ctx, null, preferStatic);
                if (desc != null && !desc.isBlank()) {
                    return desc;
                }
            }
            if (argCount != TypeSolver.ARG_COUNT_UNKNOWN) {
                String desc = callResolver.resolveMethodDescByArgCount(owner, methodName, argCount);
                if (desc != null && !desc.isBlank()) {
                    return desc;
                }
            }
        } catch (Throwable ex) {
            logger.debug("resolve exact method descriptor failed: {}", ex.toString());
        }
        return null;
    }

    private static ResolvedMethodTarget resolveExactMethodTarget(CompilationUnit cu,
                                                                 Position pos,
                                                                 String owner,
                                                                 String methodName,
                                                                 int argCount,
                                                                 EditorDocumentDto doc,
                                                                 TypeSolver typeSolver) {
        if (typeSolver == null || owner == null || owner.isBlank() || methodName == null || methodName.isBlank()) {
            return null;
        }
        CallResolver callResolver = typeSolver.getCallResolver();
        if (callResolver == null) {
            return null;
        }
        try {
            MethodCallExpr call = callResolver.findMethodCallAt(cu, pos);
            if (call == null || !methodName.equals(call.getNameAsString())) {
                return null;
            }
            CallContext ctx = buildCallContext(cu, pos, doc, callResolver);
            CallPosition callPosition = callResolver.resolveCallPosition(cu, call, ctx);
            String scopeHint = callResolver.resolveScopeClassName(cu, call, ctx);
            if (scopeHint == null || scopeHint.isBlank()) {
                scopeHint = owner;
            }
            CallSiteSelection selection = callResolver.resolveCallSiteTarget(
                    ctx, methodName, argCount, scopeHint, callPosition);
            if (selection != null && selection.methodDesc != null && !selection.methodDesc.isBlank()) {
                return new ResolvedMethodTarget(
                        normalizeClassName(selection.className),
                        safe(selection.methodName),
                        safe(selection.methodDesc),
                        true
                );
            }
            boolean preferStatic = callResolver.isStaticScope(cu, call, ctx);
            TypeRef scopeType = null;
            if (call.getScope().isPresent()) {
                scopeType = typeSolver.resolveExpressionType(cu, call.getScope().get(), ctx);
            }
            String desc = callResolver.resolveMethodDescByCall(cu, owner, call, ctx, scopeType, preferStatic);
            if (desc != null && !desc.isBlank()) {
                return new ResolvedMethodTarget(owner, methodName, desc, false);
            }
        } catch (Throwable ex) {
            logger.debug("resolve exact method target failed: {}", ex.toString());
        }
        return null;
    }

    private static CallContext buildCallContext(CompilationUnit cu,
                                                Position pos,
                                                EditorDocumentDto doc,
                                                CallResolver resolver) {
        String className = normalizeClassName(doc == null ? "" : doc.className());
        String methodName = safe(doc == null ? "" : doc.methodName());
        String methodDesc = safe(doc == null ? "" : doc.methodDesc());
        int methodArgCount = argCountFromDesc(methodDesc);

        if (resolver != null && cu != null && pos != null) {
            EnclosingCallable enclosing = resolver.resolveEnclosingCallable(cu, pos);
            if (enclosing != null) {
                if (className == null) {
                    className = normalizeClassName(enclosing.className);
                }
                if (methodName.isBlank()) {
                    methodName = safe(enclosing.methodName);
                    methodArgCount = enclosing.argCount;
                }
                if (methodDesc.isBlank()
                        && className != null
                        && !methodName.isBlank()
                        && methodArgCount != TypeSolver.ARG_COUNT_UNKNOWN) {
                    String guessed = resolver.resolveMethodDescByArgCount(className, methodName, methodArgCount);
                    if (guessed != null && !guessed.isBlank()) {
                        methodDesc = guessed;
                    }
                }
            }
        }
        if (methodArgCount == TypeSolver.ARG_COUNT_UNKNOWN && !methodDesc.isBlank()) {
            methodArgCount = argCountFromDesc(methodDesc);
        }
        return new CallContext(
                cu,
                pos,
                pos == null ? -1 : pos.line,
                className,
                methodName,
                methodDesc,
                methodArgCount
        );
    }

    private static List<MethodResult> filterMethodsByArgCount(List<MethodResult> methods, int argCount) {
        if (methods == null || methods.isEmpty() || argCount == TypeSolver.ARG_COUNT_UNKNOWN) {
            return List.of();
        }
        List<MethodResult> out = new ArrayList<>();
        for (MethodResult method : methods) {
            if (method == null) {
                continue;
            }
            String desc = method.getMethodDesc();
            if (argCountFromDesc(desc) == argCount) {
                out.add(method);
            }
        }
        return out;
    }

    private static int argCountFromDesc(String desc) {
        if (desc == null || desc.isBlank()) {
            return TypeSolver.ARG_COUNT_UNKNOWN;
        }
        try {
            return Type.getArgumentTypes(desc).length;
        } catch (Throwable ignored) {
            return TypeSolver.ARG_COUNT_UNKNOWN;
        }
    }

    private static List<EditorDeclarationTargetDto> rankAndLimitTargets(List<EditorDeclarationTargetDto> targets,
                                                                        EditorDocumentDto doc,
                                                                        String exactDesc) {
        if (targets == null || targets.isEmpty()) {
            return List.of();
        }
        String currentClass = normalizeClassName(doc == null ? "" : doc.className());
        int currentJar = doc == null || doc.jarId() == null ? 0 : Math.max(0, doc.jarId());
        Map<String, EditorDeclarationTargetDto> unique = new LinkedHashMap<>();
        for (EditorDeclarationTargetDto target : targets) {
            if (target == null) {
                continue;
            }
            String key = target.className() + "|" + target.methodName() + "|" + target.methodDesc()
                    + "|" + target.jarId() + "|" + target.caretOffset();
            unique.putIfAbsent(key, target);
        }
        List<EditorDeclarationTargetDto> out = new ArrayList<>(unique.values());
        out.sort(Comparator
                .comparingInt((EditorDeclarationTargetDto item) -> targetScore(item, currentClass, currentJar, exactDesc))
                .reversed()
                .thenComparing(EditorDeclarationTargetDto::className)
                .thenComparing(EditorDeclarationTargetDto::methodName)
                .thenComparing(EditorDeclarationTargetDto::methodDesc)
                .thenComparingInt(EditorDeclarationTargetDto::jarId));
        if (out.size() > MAX_CANDIDATES) {
            return List.copyOf(out.subList(0, MAX_CANDIDATES));
        }
        return List.copyOf(out);
    }

    private static int targetScore(EditorDeclarationTargetDto item,
                                   String currentClass,
                                   int currentJar,
                                   String exactDesc) {
        if (item == null) {
            return Integer.MIN_VALUE;
        }
        int score = 0;
        if (item.localTarget()) {
            score += 120;
        }
        if (currentClass != null && currentClass.equals(normalizeClassName(item.className()))) {
            score += 40;
        }
        if (currentJar > 0 && currentJar == item.jarId()) {
            score += 24;
        }
        if (exactDesc != null && !exactDesc.isBlank() && exactDesc.equals(item.methodDesc())) {
            score += 24;
        }
        if ("semantic-exact".equals(item.source())) {
            score += 12;
        } else if ("semantic".equals(item.source())) {
            score += 8;
        }
        if (item.methodTarget()) {
            score += 6;
        }
        return score;
    }

    private static String normalizeKind(SymbolRef symbol) {
        if (symbol == null || symbol.getKind() == null) {
            return "unknown";
        }
        return symbol.getKind().name().toLowerCase(Locale.ROOT);
    }

    private static boolean looksLikeClassToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        char first = token.charAt(0);
        return Character.isUpperCase(first) || token.contains("/") || token.contains(".");
    }

    private static String extractToken(String code, int caretOffset) {
        if (code == null || code.isEmpty()) {
            return "";
        }
        int n = code.length();
        int offset = Math.max(0, Math.min(caretOffset, n));
        int left = offset;
        if (left > 0 && left == n) {
            left = n - 1;
        }
        if (left > 0 && !isIdentifierChar(code.charAt(left)) && isIdentifierChar(code.charAt(left - 1))) {
            left--;
        }
        if (left < 0 || left >= n || !isIdentifierChar(code.charAt(left))) {
            return "";
        }
        int start = left;
        int end = left + 1;
        while (start > 0 && isIdentifierChar(code.charAt(start - 1))) {
            start--;
        }
        while (end < n && isIdentifierChar(code.charAt(end))) {
            end++;
        }
        return code.substring(start, end);
    }

    private static boolean isIdentifierChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    private static JavaParser createParser() {
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
        return new JavaParser(config);
    }

    private static CompilationUnit parse(String code) {
        synchronized (PARSE_LOCK) {
            if (code != null && code.equals(lastCode)) {
                return lastUnit;
            }
            try {
                ParseResult<CompilationUnit> result = PARSER.parse(code);
                CompilationUnit cu = result.getResult().orElse(null);
                lastCode = code;
                lastUnit = cu;
                return cu;
            } catch (Throwable ignored) {
                lastCode = code;
                lastUnit = null;
                return null;
            }
        }
    }

    private static String normalizeClassName(String className) {
        if (className == null) {
            return null;
        }
        String normalized = className.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.endsWith(".class")) {
            normalized = normalized.substring(0, normalized.length() - 6);
        }
        if (normalized.contains(".")) {
            normalized = normalized.replace('.', '/');
        }
        return normalized;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class ResolvedMethodTarget {
        private final String className;
        private final String methodName;
        private final String methodDesc;
        private final boolean fromCallSite;

        private ResolvedMethodTarget(String className,
                                     String methodName,
                                     String methodDesc,
                                     boolean fromCallSite) {
            this.className = className;
            this.methodName = methodName;
            this.methodDesc = methodDesc;
            this.fromCallSite = fromCallSite;
        }
    }

    private static final class TextIndex {
        private final String text;
        private final int[] lineStarts;

        private TextIndex(String text) {
            this.text = text == null ? "" : text;
            List<Integer> starts = new ArrayList<>();
            starts.add(0);
            for (int i = 0; i < this.text.length(); i++) {
                if (this.text.charAt(i) == '\n') {
                    starts.add(i + 1);
                }
            }
            lineStarts = new int[starts.size()];
            for (int i = 0; i < starts.size(); i++) {
                lineStarts[i] = starts.get(i);
            }
        }

        private int clampOffset(int offset) {
            return Math.max(0, Math.min(offset, text.length()));
        }

        private Position toPosition(int offset) {
            int value = clampOffset(offset);
            int index = locateLine(value);
            int lineStart = lineStarts[index];
            int line = index + 1;
            int column = Math.max(1, value - lineStart + 1);
            return new Position(line, column);
        }

        private int toOffset(Position pos) {
            if (pos == null || lineStarts.length == 0) {
                return -1;
            }
            int line = Math.max(1, Math.min(pos.line, lineStarts.length));
            int lineStart = lineStarts[line - 1];
            int lineEndExclusive = line >= lineStarts.length ? text.length() : lineStarts[line] - 1;
            int maxColumn = Math.max(1, lineEndExclusive - lineStart + 1);
            int column = Math.max(1, Math.min(pos.column, maxColumn));
            return Math.max(0, Math.min(text.length(), lineStart + column - 1));
        }

        private int locateLine(int offset) {
            int low = 0;
            int high = lineStarts.length - 1;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                int start = lineStarts[mid];
                int next = mid + 1 < lineStarts.length ? lineStarts[mid + 1] : Integer.MAX_VALUE;
                if (offset < start) {
                    high = mid - 1;
                    continue;
                }
                if (offset >= next) {
                    low = mid + 1;
                    continue;
                }
                return mid;
            }
            return Math.max(0, Math.min(lineStarts.length - 1, low));
        }
    }
}
