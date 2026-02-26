/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.swing;

import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class SwingResultHtml {
    private SwingResultHtml() {
    }

    public static String wrapHtml(String body) {
        return "<html>" + safe(body) + "</html>";
    }

    public static String renderKindTag(String kind) {
        String text = safe(kind).trim();
        if (text.isEmpty()) {
            return "";
        }
        return "<span style=\"color:#4f73c8;font-weight:600;\">[" + escapeHtml(text) + "]</span>";
    }

    public static String renderClassRow(String className, String jarName, List<String> tokens) {
        return wrapHtml(renderClassBody(className, jarName, tokens));
    }

    public static String renderMethodRow(String className,
                                         String methodName,
                                         String methodDesc,
                                         String jarName,
                                         int lineNumber,
                                         List<String> tokens) {
        return wrapHtml(renderMethodBody(className, methodName, methodDesc, jarName, lineNumber, tokens));
    }

    public static String renderClassBody(String className, String jarName, List<String> tokens) {
        String normalizedClass = normalizeClassName(className).trim();
        String safeJar = safe(jarName).trim();
        StringBuilder html = new StringBuilder(128);
        if (!normalizedClass.isEmpty()) {
            html.append(span(highlightText(normalizedClass, tokens), "#d48806", true));
        } else {
            html.append("-");
        }
        if (!safeJar.isEmpty()) {
            html.append("<span style=\"color:#6f6f6f;\"> [")
                    .append(escapeHtml(safeJar))
                    .append("]</span>");
        }
        return html.toString();
    }

    public static String renderMethodBody(String className,
                                          String methodName,
                                          String methodDesc,
                                          String jarName,
                                          int lineNumber,
                                          List<String> tokens) {
        String normalizedClass = normalizeClassName(className).trim();
        String methodSig = renderMethodSignature(methodName, methodDesc).trim();
        String safeJar = safe(jarName).trim();
        StringBuilder html = new StringBuilder(160);
        if (!normalizedClass.isEmpty()) {
            html.append(span(highlightText(normalizedClass, tokens), "#d48806", true))
                    .append("<span style=\"color:#7a7a7a;\">#</span>");
        }
        if (methodSig.isEmpty()) {
            methodSig = safe(methodName) + safe(methodDesc);
        }
        if (methodSig.isEmpty()) {
            methodSig = "-";
        }
        html.append(highlightText(methodSig, tokens));
        if (lineNumber > 0) {
            html.append("<span style=\"color:#6f6f6f;\"> : ")
                    .append(lineNumber)
                    .append("</span>");
        }
        if (!safeJar.isEmpty()) {
            html.append("<span style=\"color:#6f6f6f;\"> [")
                    .append(escapeHtml(safeJar))
                    .append("]</span>");
        }
        return html.toString();
    }

    public static List<String> collectTokens(String... values) {
        Set<String> out = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                if (value == null) {
                    continue;
                }
                String[] parts = value.trim().split("\\s+");
                for (String part : parts) {
                    String token = safe(part).trim();
                    if (!token.isEmpty() && token.length() <= 64) {
                        out.add(token);
                    }
                }
            }
        }
        return new ArrayList<>(out);
    }

    public static String normalizeClassName(String className) {
        return safe(className).replace('/', '.');
    }

    public static String renderMethodSignature(String methodName, String methodDesc) {
        String name = safe(methodName);
        if ("<init>".equals(name)) {
            name = "new";
        } else if ("<clinit>".equals(name)) {
            return "static{}";
        }
        String desc = safe(methodDesc);
        if (desc.isBlank()) {
            return name;
        }
        try {
            Type[] args = Type.getArgumentTypes(desc);
            StringBuilder sb = new StringBuilder();
            sb.append(name).append('(');
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(simpleTypeName(args[i]));
            }
            sb.append(')');
            return sb.toString();
        } catch (Throwable ignored) {
            return name + desc;
        }
    }

    public static String highlightText(String text, List<String> tokens) {
        String raw = safe(text);
        if (raw.isEmpty()) {
            return "";
        }
        if (tokens == null || tokens.isEmpty()) {
            return escapeHtml(raw);
        }
        boolean[] marks = new boolean[raw.length()];
        String lower = raw.toLowerCase(Locale.ROOT);
        for (String token : tokens) {
            String needle = safe(token).trim().toLowerCase(Locale.ROOT);
            if (needle.isEmpty()) {
                continue;
            }
            int from = 0;
            while (from < lower.length()) {
                int idx = lower.indexOf(needle, from);
                if (idx < 0) {
                    break;
                }
                int end = Math.min(marks.length, idx + needle.length());
                for (int i = idx; i < end; i++) {
                    marks[i] = true;
                }
                from = end;
            }
        }
        StringBuilder out = new StringBuilder(raw.length() + 48);
        boolean open = false;
        for (int i = 0; i < raw.length(); i++) {
            if (marks[i] && !open) {
                out.append("<span style=\"background:#ffe08a;\">");
                open = true;
            } else if (!marks[i] && open) {
                out.append("</span>");
                open = false;
            }
            appendEscaped(out, raw.charAt(i));
        }
        if (open) {
            out.append("</span>");
        }
        return out.toString();
    }

    public static String escapeHtml(String text) {
        String raw = safe(text);
        StringBuilder sb = new StringBuilder(raw.length() + 8);
        for (int i = 0; i < raw.length(); i++) {
            appendEscaped(sb, raw.charAt(i));
        }
        return sb.toString();
    }

    private static String span(String htmlContent, String color, boolean bold) {
        String body = safe(htmlContent);
        if (body.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(body.length() + 48);
        sb.append("<span style=\"color:").append(color).append(';');
        if (bold) {
            sb.append("font-weight:600;");
        }
        sb.append("\">").append(body).append("</span>");
        return sb.toString();
    }

    private static String simpleTypeName(Type type) {
        if (type == null) {
            return "?";
        }
        if (type.getSort() == Type.ARRAY) {
            StringBuilder sb = new StringBuilder(simpleTypeName(type.getElementType()));
            for (int i = 0; i < type.getDimensions(); i++) {
                sb.append("[]");
            }
            return sb.toString();
        }
        String name = safe(type.getClassName());
        int dot = name.lastIndexOf('.');
        if (dot >= 0 && dot < name.length() - 1) {
            name = name.substring(dot + 1);
        }
        int dollar = name.lastIndexOf('$');
        if (dollar >= 0 && dollar < name.length() - 1) {
            name = name.substring(dollar + 1);
        }
        return name.isBlank() ? "?" : name;
    }

    private static void appendEscaped(StringBuilder out, char ch) {
        switch (ch) {
            case '&' -> out.append("&amp;");
            case '<' -> out.append("&lt;");
            case '>' -> out.append("&gt;");
            case '"' -> out.append("&quot;");
            case '\'' -> out.append("&#39;");
            default -> out.append(ch);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
