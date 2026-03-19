/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.server.handler.base;

import com.alibaba.fastjson2.JSON;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithParameters;
import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.model.MethodView;
import me.n1ar4.jar.analyzer.graph.query.QueryErrorClassifier;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.utils.CommonFilterUtil;
import me.n1ar4.jar.analyzer.utils.StringUtil;
import me.n1ar4.parser.DescInfo;
import me.n1ar4.parser.DescUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BaseHandler {
    protected static final Logger logger = LogManager.getLogger();
    private static final Object METHOD_PARSE_LOCK = new Object();
    private static final JavaParser METHOD_PARSER = createMethodParser();

    protected String getParam(NanoHTTPD.IHTTPSession session, String key) {
        List<String> data = session.getParameters().get(key);
        if (data == null || data.isEmpty()) {
            return "";
        }
        return data.get(0);
    }

    protected ArrayList<MethodView> filterJdkMethods(List<MethodView> results) {
        if (results == null) {
            return new ArrayList<>();
        }
        ArrayList<MethodView> out = new ArrayList<>();
        for (MethodView m : results) {
            if (m == null) {
                continue;
            }
            String className = m.getClassName();
            if (!isJdkClass(className) && !isNoisyJar(m.getJarName())) {
                out.add(m);
            }
        }
        return out;
    }

    protected boolean isNoisyJar(String jarName) {
        return CommonFilterUtil.isFilteredJar(jarName);
    }

    protected boolean isJdkClass(String className) {
        return CommonFilterUtil.isFilteredClass(className);
    }

    public NanoHTTPD.Response buildJSON(String json) {
        if (json == null || json.isEmpty()) {
            NanoHTTPD.Response resp = NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.OK,
                    "application/json",
                    "{}");
            return addCorsHeaders(resp);
        } else {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            int lengthInBytes = bytes.length;
            if (lengthInBytes > 3 * 1024 * 1024) {
                return buildError(
                        NanoHTTPD.Response.Status.PAYLOAD_TOO_LARGE,
                        "json_too_large",
                        "max size 3 MB");
            } else {
                NanoHTTPD.Response resp = NanoHTTPD.newFixedLengthResponse(
                        NanoHTTPD.Response.Status.OK,
                        "application/json",
                        json);
                return addCorsHeaders(resp);
            }
        }
    }

    public NanoHTTPD.Response needParam(String s) {
        return buildError(
                NanoHTTPD.Response.Status.BAD_REQUEST,
                "missing_param",
                "need param: " + s);
    }

    protected NanoHTTPD.Response projectNotReady() {
        if (DatabaseManager.isBuilding() || ActiveProjectContext.isProjectMutationInProgress()) {
            return projectBuilding();
        }
        return buildError(
                NanoHTTPD.Response.Status.SERVICE_UNAVAILABLE,
                QueryErrorClassifier.PROJECT_MODEL_MISSING_REBUILD,
                QueryErrorClassifier.publicMessage(
                        QueryErrorClassifier.PROJECT_MODEL_MISSING_REBUILD,
                        QueryErrorClassifier.PROJECT_MODEL_MISSING_REBUILD));
    }

    protected NanoHTTPD.Response projectBuilding() {
        return buildError(
                NanoHTTPD.Response.Status.SERVICE_UNAVAILABLE,
                QueryErrorClassifier.PROJECT_BUILD_IN_PROGRESS,
                QueryErrorClassifier.publicMessage(
                        QueryErrorClassifier.PROJECT_BUILD_IN_PROGRESS,
                        QueryErrorClassifier.PROJECT_BUILD_IN_PROGRESS));
    }

    protected NanoHTTPD.Response projectStateError(String message) {
        if (QueryErrorClassifier.isProjectBuilding(message)) {
            return projectBuilding();
        }
        if (QueryErrorClassifier.isProjectNotReady(message)) {
            return projectNotReady();
        }
        return null;
    }

    protected NanoHTTPD.Response requireActiveProjectRequest(String requestedProjectKey) {
        String normalized = ActiveProjectContext.normalizeProjectKey(requestedProjectKey);
        if (normalized.isBlank()) {
            return null;
        }
        String activeProjectKey = ActiveProjectContext.normalizeProjectKey(
                ActiveProjectContext.getPublishedActiveProjectKey());
        if (normalized.equals(activeProjectKey)) {
            return null;
        }
        return buildError(
                NanoHTTPD.Response.Status.CONFLICT,
                "project_switch_required",
                "switch active project first");
    }

    protected CoreEngine requireReadyEngine() {
        try {
            DatabaseManager.ensureProjectReadable();
        } catch (IllegalStateException ex) {
            return null;
        }
        CoreEngine engine = EngineContext.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return null;
        }
        return engine;
    }

    protected NanoHTTPD.Response buildError(NanoHTTPD.Response.Status status, String code, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", false);
        result.put("code", code);
        result.put("message", message);
        result.put("status", status.getRequestStatus());
        String json = JSON.toJSONString(result);
        NanoHTTPD.Response resp = NanoHTTPD.newFixedLengthResponse(status, "application/json", json);
        return addCorsHeaders(resp);
    }

    protected NanoHTTPD.Response addCorsHeaders(NanoHTTPD.Response resp) {
        if (resp == null) {
            return null;
        }
        resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE");
        resp.addHeader("Access-Control-Allow-Headers", "Token, Content-Type");
        resp.addHeader("Access-Control-Max-Age", "600");
        resp.addHeader("Vary", "Origin");
        return resp;
    }

    /**
     * 从反编译的类代码中提取指定方法的代码
     *
     * @param classCode  完整的类代码
     * @param methodName 方法名
     * @param methodDesc 方法描述符（可选）
     * @return 方法代码，如果未找到则返回null
     */
    protected String extractMethodCode(String classCode, String methodName, String methodDesc) {
        if (StringUtil.isNull(classCode) || StringUtil.isNull(methodName)) {
            return null;
        }

        try {
            String normalizedDesc = normalizeMethodDesc(methodDesc);
            String astMatch = extractMethodCodeByAst(classCode, methodName, normalizedDesc);
            if (!StringUtil.isNull(astMatch)) {
                return astMatch;
            }
            String scanned = extractMethodCodeByScan(classCode, methodName, normalizedDesc);
            if (!StringUtil.isNull(scanned)) {
                return scanned;
            }
            if (!StringUtil.isNull(normalizedDesc)) {
                return null;
            }
            return findMethodByFuzzyMatch(classCode, methodName);
        } catch (Exception e) {
            logger.warn("Error extracting method code: " + e.getMessage());
            return null;
        }
    }

    private String normalizeMethodDesc(String methodDesc) {
        if (StringUtil.isNull(methodDesc)) {
            return null;
        }
        String normalized = methodDesc.trim();
        if (normalized.isEmpty() || "null".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }

    private String extractMethodCodeByAst(String classCode, String methodName, String methodDesc) {
        CompilationUnit unit = parseCompilationUnit(classCode);
        if (unit == null) {
            return null;
        }
        if ("<clinit>".equals(methodName)) {
            for (InitializerDeclaration declaration : unit.findAll(InitializerDeclaration.class)) {
                if (declaration != null && declaration.isStatic()) {
                    return sliceNode(classCode, declaration);
                }
            }
            return null;
        }
        if ("<init>".equals(methodName)) {
            return extractConstructorCodeByAst(classCode, unit, methodDesc);
        }
        return extractNamedMethodCodeByAst(classCode, unit, methodName, methodDesc);
    }

    private CompilationUnit parseCompilationUnit(String classCode) {
        synchronized (METHOD_PARSE_LOCK) {
            ParseResult<CompilationUnit> result = METHOD_PARSER.parse(classCode);
            return result.getResult().orElse(null);
        }
    }

    private String extractConstructorCodeByAst(String classCode,
                                               CompilationUnit unit,
                                               String methodDesc) {
        ConstructorDeclaration constructor = selectCallable(unit.findAll(ConstructorDeclaration.class), methodDesc);
        if (constructor != null) {
            return sliceNode(classCode, constructor);
        }
        CompactConstructorDeclaration compact = selectCompactConstructor(unit.findAll(CompactConstructorDeclaration.class), methodDesc);
        if (compact != null) {
            return sliceNode(classCode, compact);
        }
        return null;
    }

    private String extractNamedMethodCodeByAst(String classCode,
                                               CompilationUnit unit,
                                               String methodName,
                                               String methodDesc) {
        List<MethodDeclaration> candidates = new ArrayList<>();
        for (MethodDeclaration declaration : unit.findAll(MethodDeclaration.class)) {
            if (declaration != null && methodName.equals(declaration.getNameAsString())) {
                candidates.add(declaration);
            }
        }
        MethodDeclaration selected = selectCallable(candidates, methodDesc);
        return selected == null ? null : sliceNode(classCode, selected);
    }

    private <T extends Node & NodeWithParameters<?>> T selectCallable(List<T> declarations, String methodDesc) {
        if (declarations == null || declarations.isEmpty()) {
            return null;
        }
        if (StringUtil.isNull(methodDesc)) {
            return declarations.size() == 1 ? declarations.get(0) : null;
        }
        for (T declaration : declarations) {
            if (declaration != null && descriptorMatchesParameters(declaration.getParameters(), methodDesc)) {
                return declaration;
            }
        }
        return null;
    }

    private CompactConstructorDeclaration selectCompactConstructor(List<CompactConstructorDeclaration> declarations,
                                                                  String methodDesc) {
        if (declarations == null || declarations.isEmpty()) {
            return null;
        }
        if (StringUtil.isNull(methodDesc)) {
            return declarations.size() == 1 ? declarations.get(0) : null;
        }
        for (CompactConstructorDeclaration declaration : declarations) {
            if (declaration != null && descriptorMatchesParameters(resolveCompactConstructorParameters(declaration), methodDesc)) {
                return declaration;
            }
        }
        return null;
    }

    private List<Parameter> resolveCompactConstructorParameters(CompactConstructorDeclaration declaration) {
        if (declaration == null) {
            return List.of();
        }
        Node parent = declaration.getParentNode().orElse(null);
        if (parent instanceof RecordDeclaration recordDeclaration) {
            return new ArrayList<>(recordDeclaration.getParameters());
        }
        return List.of();
    }

    private boolean descriptorMatchesParameters(List<Parameter> parameters, String methodDesc) {
        DescInfo descInfo = DescUtil.parseDesc(methodDesc);
        List<String> expectedParams = descInfo.getParams();
        List<Parameter> actualParams = parameters == null ? List.of() : parameters;
        if (expectedParams.size() != actualParams.size()) {
            return false;
        }
        for (int i = 0; i < expectedParams.size(); i++) {
            String expectedType = normalizeDescType(expectedParams.get(i));
            String actualType = normalizeParamType(actualParams.get(i).toString());
            if (!expectedType.equals(actualType)) {
                return false;
            }
        }
        return true;
    }

    private String sliceNode(String classCode, Node node) {
        if (node == null) {
            return null;
        }
        Range range = node.getRange().orElse(null);
        if (range == null) {
            return null;
        }
        SourceSliceIndex index = new SourceSliceIndex(classCode);
        int start = index.toOffset(range.begin);
        int end = index.toInclusiveEnd(range.end);
        if (start < 0 || end <= start || end > classCode.length()) {
            return null;
        }
        return classCode.substring(start, end).trim();
    }

    private String extractMethodCodeByScan(String classCode, String methodName, String methodDesc) {
        String[] lines = classCode.split("\n");
        StringBuilder methodCode = new StringBuilder();
        boolean inMethod = false;
        int braceCount = 0;
        boolean foundMethod = false;
        boolean isClinit = "<clinit>".equals(methodName);
        boolean isConstructor = "<init>".equals(methodName);
        String matchMethodName = resolveMethodName(classCode, methodName);
        String methodPattern;
        if (isClinit) {
            methodPattern = "^\\s*static\\s*\\{.*";
        } else if (isConstructor) {
            methodPattern = ".*\\b" + Pattern.quote(matchMethodName) + "\\s*(\\(.*\\))?\\s*\\{?";
        } else {
            methodPattern = ".*\\b" + Pattern.quote(matchMethodName) + "\\s*\\(.*\\).*\\{?";
        }

        Pattern pattern = Pattern.compile(methodPattern);
        BraceScanState braceState = new BraceScanState();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (!inMethod) {
                Matcher matcher = pattern.matcher(line.trim());
                boolean nameCandidate = !isClinit
                        && !StringUtil.isNull(methodDesc)
                        && containsWord(line, matchMethodName)
                        && (line.indexOf('(') >= 0 || (isConstructor && line.contains("{")))
                        && !line.trim().startsWith("@");
                if (matcher.matches() || nameCandidate) {
                    if (!isClinit && !StringUtil.isNull(methodDesc) && !methodDescMatches(lines, i, methodDesc)) {
                        continue;
                    }
                    inMethod = true;
                    foundMethod = true;
                    if (!isClinit) {
                        int annStart = findAnnotationStart(lines, i);
                        for (int j = annStart; j < i; j++) {
                            methodCode.append(lines[j]).append("\n");
                        }
                    }
                    methodCode.append(line).append("\n");
                    braceCount += countStructuralBraceDelta(line, braceState);
                    if (line.trim().endsWith(";")) {
                        break;
                    }
                }
            } else {
                methodCode.append(line).append("\n");
                braceCount += countStructuralBraceDelta(line, braceState);
                if (braceCount <= 0) {
                    break;
                }
            }
        }
        return foundMethod ? methodCode.toString().trim() : null;
    }

    /**
     * 模糊匹配方法
     */
    protected String findMethodByFuzzyMatch(String classCode, String methodName) {
        try {
            String[] lines = classCode.split("\n");
            StringBuilder result = new StringBuilder();
            String matchMethodName = resolveMethodName(classCode, methodName);
            boolean isConstructor = "<init>".equals(methodName);
            BraceScanState braceState = new BraceScanState();

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (line.contains(matchMethodName)
                        && ((line.contains("(") && line.contains(")")) || (isConstructor && line.contains("{")))) {

                    // 找到可能的方法，收集完整的方法代码
                    result.append(line).append("\n");

                    if (line.trim().endsWith(";")) {
                        // 抽象方法或接口方法
                        break;
                    }

                    int braceCount = countStructuralBraceDelta(line, braceState);

                    for (int j = i + 1; j < lines.length && braceCount > 0; j++) {
                        String nextLine = lines[j];
                        result.append(nextLine).append("\n");
                        braceCount += countStructuralBraceDelta(nextLine, braceState);
                    }
                    break;
                }
            }
            return result.length() > 0 ? result.toString().trim() : null;
        } catch (Exception e) {
            logger.warn("error in fuzzy method matching: " + e.getMessage());
            return null;
        }
    }

    /**
     * 计算字符串中指定字符的数量
     */
    protected int countChar(String str, char ch) {
        int count = 0;
        for (char c : str.toCharArray()) {
            if (c == ch) {
                count++;
            }
        }
        return count;
    }

    private int countStructuralBraceDelta(String line, BraceScanState state) {
        if (StringUtil.isNull(line)) {
            return 0;
        }
        BraceScanState active = state == null ? new BraceScanState() : state;
        int delta = 0;
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            char next = i + 1 < line.length() ? line.charAt(i + 1) : '\0';
            char next2 = i + 2 < line.length() ? line.charAt(i + 2) : '\0';
            if (active.inBlockComment) {
                if (current == '*' && next == '/') {
                    active.inBlockComment = false;
                    i++;
                }
                continue;
            }
            if (active.inTextBlock) {
                if (current == '"' && next == '"' && next2 == '"') {
                    active.inTextBlock = false;
                    i += 2;
                }
                continue;
            }
            if (active.inString) {
                if (current == '"' && !active.escaped) {
                    active.inString = false;
                }
                active.escaped = current == '\\' && !active.escaped;
                continue;
            }
            if (active.inChar) {
                if (current == '\'' && !active.escaped) {
                    active.inChar = false;
                }
                active.escaped = current == '\\' && !active.escaped;
                continue;
            }
            if (current == '/' && next == '/') {
                break;
            }
            if (current == '/' && next == '*') {
                active.inBlockComment = true;
                i++;
                continue;
            }
            if (current == '"' && next == '"' && next2 == '"') {
                active.inTextBlock = true;
                active.escaped = false;
                i += 2;
                continue;
            }
            if (current == '"') {
                active.inString = true;
                active.escaped = false;
                continue;
            }
            if (current == '\'') {
                active.inChar = true;
                active.escaped = false;
                continue;
            }
            if (current == '{') {
                delta++;
            } else if (current == '}') {
                delta--;
            }
        }
        if (!active.inString && !active.inChar) {
            active.escaped = false;
        }
        return delta;
    }

    // 处理 <init> 构造方法的显示名称
    private String resolveMethodName(String classCode, String methodName) {
        if ("<init>".equals(methodName)) {
            String className = extractClassSimpleName(classCode);
            if (!StringUtil.isNull(className)) {
                return className;
            }
        }
        return methodName;
    }

    // 从反编译源码中提取类名（用于构造方法匹配）
    private String extractClassSimpleName(String classCode) {
        if (StringUtil.isNull(classCode)) {
            return "";
        }
        Pattern classPattern = Pattern.compile("\\b(class|interface|enum|record)\\s+([A-Za-z_$][A-Za-z0-9_$]*)");
        Matcher matcher = classPattern.matcher(classCode);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return "";
    }

    // 根据 methodDesc 校验方法参数列表
    private boolean methodDescMatches(String line, String methodDesc) {
        int start = line.indexOf('(');
        int end = line.lastIndexOf(')');
        if (start < 0 || end < start) {
            return false;
        }
        String paramsSection = line.substring(start + 1, end).trim();
        DescInfo descInfo = DescUtil.parseDesc(methodDesc);
        List<String> expectedParams = descInfo.getParams();
        List<String> actualParams = splitParams(paramsSection);
        if (expectedParams.size() != actualParams.size()) {
            return false;
        }
        for (int i = 0; i < expectedParams.size(); i++) {
            String expectedType = normalizeDescType(expectedParams.get(i));
            String actualType = normalizeParamType(actualParams.get(i));
            if (!expectedType.equals(actualType)) {
                return false;
            }
        }
        return true;
    }

    // 支持多行方法签名的匹配（从 startIndex 向下拼接）
    private boolean methodDescMatches(String[] lines, int startIndex, String methodDesc) {
        StringBuilder signature = new StringBuilder();
        int parenBalance = 0;
        boolean sawParen = false;
        int limit = Math.min(lines.length, startIndex + 50);
        for (int i = startIndex; i < limit; i++) {
            String line = lines[i];
            signature.append(line).append("\n");
            if (line.indexOf('(') >= 0) {
                sawParen = true;
            }
            parenBalance += countChar(line, '(') - countChar(line, ')');
            if (sawParen && parenBalance <= 0 && line.contains(")")) {
                break;
            }
            if (!sawParen && line.contains(";")) {
                break;
            }
        }
        if (!sawParen) {
            return false;
        }
        return methodDescMatches(signature.toString(), methodDesc);
    }

    // 判断方法名是否是独立单词，避免误匹配
    private boolean containsWord(String line, String word) {
        if (StringUtil.isNull(line) || StringUtil.isNull(word)) {
            return false;
        }
        Pattern wordPattern = Pattern.compile("\\b" + Pattern.quote(word) + "\\b");
        return wordPattern.matcher(line).find();
    }

    // 向上查找紧邻方法签名前的注解行
    private int findAnnotationStart(String[] lines, int methodLine) {
        int start = methodLine;
        boolean seen = false;
        for (int i = methodLine - 1; i >= 0; i--) {
            String t = lines[i].trim();
            if (t.isEmpty()) {
                if (seen) {
                    start = i;
                    continue;
                }
                break;
            }
            if (t.startsWith("@")) {
                seen = true;
                start = i;
                continue;
            }
            break;
        }
        return start;
    }

    // 拆分参数列表，忽略泛型/嵌套括号中的逗号
    private List<String> splitParams(String params) {
        List<String> result = new ArrayList<>();
        if (StringUtil.isNull(params)) {
            return result;
        }
        StringBuilder current = new StringBuilder();
        int angleDepth = 0;
        int parenDepth = 0;
        for (int i = 0; i < params.length(); i++) {
            char c = params.charAt(i);
            if (c == '<') {
                angleDepth++;
            } else if (c == '>' && angleDepth > 0) {
                angleDepth--;
            } else if (c == '(') {
                parenDepth++;
            } else if (c == ')' && parenDepth > 0) {
                parenDepth--;
            }
            if (c == ',' && angleDepth == 0 && parenDepth == 0) {
                String part = current.toString().trim();
                if (!part.isEmpty()) {
                    result.add(part);
                }
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        String part = current.toString().trim();
        if (!part.isEmpty()) {
            result.add(part);
        }
        return result;
    }

    // 规范化 desc 类型（去泛型/空白/varargs）
    private String normalizeDescType(String type) {
        if (StringUtil.isNull(type)) {
            return "";
        }
        String cleaned = stripGenerics(type);
        cleaned = cleaned.replace("...", "[]");
        cleaned = cleaned.replaceAll("\\s+", "");
        return DescUtil.cleanJavaLang(cleaned);
    }

    // 规范化源码中的参数类型（去注解/参数名/泛型等）
    private String normalizeParamType(String param) {
        if (StringUtil.isNull(param)) {
            return "";
        }
        String cleaned = param;
        cleaned = cleaned.replaceAll("@[\\w.]+(\\([^)]*\\))?\\s*", "");
        cleaned = cleaned.replaceAll("\\bfinal\\b\\s*", "");
        cleaned = cleaned.trim();
        int lastSpace = lastSpaceOutsideGenerics(cleaned);
        if (lastSpace != -1) {
            cleaned = cleaned.substring(0, lastSpace).trim();
        }
        cleaned = stripGenerics(cleaned);
        cleaned = cleaned.replace("...", "[]");
        cleaned = cleaned.replaceAll("\\s+", "");
        return DescUtil.cleanJavaLang(cleaned);
    }

    // 获取泛型外的最后一个空格位置（用于分离类型与参数名）
    private int lastSpaceOutsideGenerics(String value) {
        int depth = 0;
        int last = -1;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>' && depth > 0) {
                depth--;
            } else if (depth == 0 && Character.isWhitespace(c)) {
                last = i;
            }
        }
        return last;
    }

    // 去掉泛型参数
    private String stripGenerics(String value) {
        if (StringUtil.isNull(value)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '<') {
                depth++;
                continue;
            }
            if (c == '>' && depth > 0) {
                depth--;
                continue;
            }
            if (depth == 0) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static JavaParser createMethodParser() {
        ParserConfiguration configuration = new ParserConfiguration();
        configuration.setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
        return new JavaParser(configuration);
    }

    private static final class BraceScanState {
        private boolean inBlockComment;
        private boolean inString;
        private boolean inChar;
        private boolean inTextBlock;
        private boolean escaped;
    }

    private static final class SourceSliceIndex {
        private final int[] lineStarts;
        private final int length;

        private SourceSliceIndex(String code) {
            String text = code == null ? "" : code;
            this.length = text.length();
            List<Integer> starts = new ArrayList<>();
            starts.add(0);
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == '\n' && i + 1 <= text.length()) {
                    starts.add(i + 1);
                }
            }
            this.lineStarts = new int[starts.size()];
            for (int i = 0; i < starts.size(); i++) {
                this.lineStarts[i] = starts.get(i);
            }
        }

        private int toOffset(Position position) {
            if (position == null || lineStarts.length == 0) {
                return -1;
            }
            int line = Math.max(1, Math.min(position.line, lineStarts.length));
            int lineStart = lineStarts[line - 1];
            int offset = lineStart + Math.max(0, position.column - 1);
            return Math.max(0, Math.min(length, offset));
        }

        private int toInclusiveEnd(Position position) {
            int start = toOffset(position);
            if (start < 0) {
                return -1;
            }
            return Math.max(0, Math.min(length, start + 1));
        }
    }
}
