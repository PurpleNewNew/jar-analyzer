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
import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.entity.MethodResult;
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
    private static final Logger logger = LogManager.getLogger();
    public String getClassName(NanoHTTPD.IHTTPSession session) {
        List<String> clazz = session.getParameters().get("class");
        if (clazz == null || clazz.isEmpty()) {
            return "";
        }
        String className = clazz.get(0);
        return className.replace('.', '/');
    }

    public String getMethodName(NanoHTTPD.IHTTPSession session) {
        List<String> m = session.getParameters().get("method");
        if (m == null || m.isEmpty()) {
            return "";
        }
        return m.get(0);
    }

    public String getMethodDesc(NanoHTTPD.IHTTPSession session) {
        List<String> d = session.getParameters().get("desc");
        if (d == null || d.isEmpty()) {
            return "";
        }
        return d.get(0);
    }

    public boolean getIncludeFull(NanoHTTPD.IHTTPSession session) {
        List<String> includeFull = session.getParameters().get("includeFull");
        if (includeFull == null || includeFull.isEmpty()) {
            includeFull = session.getParameters().get("full");
        }
        if (includeFull == null || includeFull.isEmpty()) {
            return false;
        }
        String value = includeFull.get(0);
        if (StringUtil.isNull(value)) {
            return false;
        }
        String v = value.trim().toLowerCase();
        return "1".equals(v) || "true".equals(v) || "yes".equals(v) || "on".equals(v);
    }

    public String getStr(NanoHTTPD.IHTTPSession session) {
        List<String> d = session.getParameters().get("str");
        if (d == null || d.isEmpty()) {
            return "";
        }
        return d.get(0);
    }

    protected String getParam(NanoHTTPD.IHTTPSession session, String key) {
        List<String> data = session.getParameters().get(key);
        if (data == null || data.isEmpty()) {
            return "";
        }
        return data.get(0);
    }

    protected ArrayList<MethodResult> filterJdkMethods(List<MethodResult> results,
                                                       NanoHTTPD.IHTTPSession session) {
        if (results == null) {
            return new ArrayList<>();
        }
        ArrayList<MethodResult> out = new ArrayList<>();
        for (MethodResult m : results) {
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

    public NanoHTTPD.Response error() {
        return buildError(
                NanoHTTPD.Response.Status.SERVICE_UNAVAILABLE,
                "engine_not_ready",
                "core engine is null");
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
            if (!StringUtil.isNull(methodDesc) && "null".equalsIgnoreCase(methodDesc.trim())) {
                methodDesc = null;
            }
            String[] lines = classCode.split("\n");
            StringBuilder methodCode = new StringBuilder();
            boolean inMethod = false;
            int braceCount = 0;
            boolean foundMethod = false;

            // 构建方法匹配的正则表达式
            // 匹配方法声明，考虑各种修饰符和参数
            String methodPattern;
            boolean isClinit = "<clinit>".equals(methodName);
            String matchMethodName = resolveMethodName(classCode, methodName);
            if (isClinit) {
                methodPattern = "^\\s*static\\s*\\{.*";
            } else {
                methodPattern = ".*\\b" + Pattern.quote(matchMethodName) + "\\s*\\(.*\\).*\\{?";
            }

            Pattern pattern = Pattern.compile(methodPattern);

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (!inMethod) {
                    Matcher matcher = pattern.matcher(line.trim());
                    boolean nameCandidate = !isClinit
                            && !StringUtil.isNull(methodDesc)
                            && containsWord(line, matchMethodName)
                            && line.indexOf('(') >= 0
                            && !line.trim().startsWith("@");
                    if (matcher.matches() || nameCandidate) {
                        if (!isClinit && !StringUtil.isNull(methodDesc)) {
                            // 支持方法签名跨行（长泛型/注解参数等）
                            if (!methodDescMatches(lines, i, methodDesc)) {     
                                continue;
                            }
                        }
                        // 找到方法开始
                        inMethod = true;
                        foundMethod = true;
                        if (!isClinit) {
                            int annStart = findAnnotationStart(lines, i);
                            for (int j = annStart; j < i; j++) {
                                methodCode.append(lines[j]).append("\n");
                            }
                        }
                        methodCode.append(line).append("\n");

                        // 计算大括号
                        braceCount += countChar(line, '{') - countChar(line, '}');

                        // 如果方法声明在一行内完成且没有大括号，可能是抽象方法或接口方法
                        if (line.trim().endsWith(";")) {
                            break;
                        }
                    }
                } else {
                    // 在方法内部
                    methodCode.append(line).append("\n");
                    braceCount += countChar(line, '{') - countChar(line, '}');

                    // 如果大括号平衡，方法结束
                    if (braceCount <= 0) {
                        break;
                    }
                }
            }

            if (foundMethod) {
                return methodCode.toString().trim();
            } else {
                if (!StringUtil.isNull(methodDesc)) {
                    return null;
                }
                return findMethodByFuzzyMatch(classCode, methodName);
            }

        } catch (Exception e) {
            logger.warn("Error extracting method code: " + e.getMessage());
            return null;
        }
    }

    /**
     * 模糊匹配方法
     */
    protected String findMethodByFuzzyMatch(String classCode, String methodName) {
        try {
            String[] lines = classCode.split("\n");
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (line.contains(methodName) &&
                        (line.contains("(") && line.contains(")"))) {

                    // 找到可能的方法，收集完整的方法代码
                    result.append(line).append("\n");

                    if (line.trim().endsWith(";")) {
                        // 抽象方法或接口方法
                        break;
                    }

                    int braceCount = countChar(line, '{') - countChar(line, '}');

                    for (int j = i + 1; j < lines.length && braceCount > 0; j++) {
                        String nextLine = lines[j];
                        result.append(nextLine).append("\n");
                        braceCount += countChar(nextLine, '{') - countChar(nextLine, '}');
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
        Pattern classPattern = Pattern.compile("\\b(class|interface|enum)\\s+([A-Za-z_$][A-Za-z0-9_$]*)");
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
}
