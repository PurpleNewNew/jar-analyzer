/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.taint;

import me.n1ar4.jar.analyzer.core.reference.MethodReference;

/**
 * Resolve sink kind for taint verification.
 *
 * <p>Design boundary: MUST NOT depend on rules/sink.json.
 * The sink kind can be provided explicitly via a system property, otherwise
 * a small set of conservative heuristics are applied.</p>
 */
public final class SinkKindResolver {
    /**
     * Optional override. Example: -Djar.analyzer.taint.sinkKind=sql
     */
    public static final String SINK_KIND_PROP = "jar.analyzer.taint.sinkKind";

    /**
     * Disable heuristics. Example: -Djar.analyzer.taint.sinkKindHeuristic=false
     */
    private static final String HEURISTIC_ENABLE_PROP = "jar.analyzer.taint.sinkKindHeuristic";

    public enum Origin {
        OVERRIDE,
        HEURISTIC,
        UNKNOWN
    }

    public static final class Result {
        private final String kind;
        private final Origin origin;

        private Result(String kind, Origin origin) {
            this.kind = kind;
            this.origin = origin == null ? Origin.UNKNOWN : origin;
        }

        public String getKind() {
            return kind;
        }

        public Origin getOrigin() {
            return origin;
        }
    }

    private SinkKindResolver() {
    }

    public static Result resolve(MethodReference.Handle sink) {
        String override = System.getProperty(SINK_KIND_PROP);
        String normalized = normalizeKind(override);
        if (normalized != null) {
            return new Result(normalized, Origin.OVERRIDE);
        }
        if (!isHeuristicEnabled()) {
            return new Result(null, Origin.UNKNOWN);
        }
        String guessed = guessBySignature(sink);
        if (guessed != null) {
            return new Result(guessed, Origin.HEURISTIC);
        }
        return new Result(null, Origin.UNKNOWN);
    }

    private static boolean isHeuristicEnabled() {
        String raw = System.getProperty(HEURISTIC_ENABLE_PROP);
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        return !"false".equalsIgnoreCase(raw.trim());
    }

    private static String normalizeKind(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim().toLowerCase();
        if (v.isEmpty()) {
            return null;
        }
        if (v.contains("sql")) {
            return "sql";
        }
        if (v.contains("xss")) {
            return "xss";
        }
        if (v.contains("ssrf")) {
            return "ssrf";
        }
        if (v.contains("file") || v.contains("path")) {
            return "file";
        }
        if (v.contains("rpc")) {
            return "rpc";
        }
        // Allow custom kinds (future-proof); model rules will simply not match if unused.
        return v;
    }

    private static String guessBySignature(MethodReference.Handle sink) {
        if (sink == null) {
            return null;
        }
        String owner = sink.getClassReference() == null ? null : sink.getClassReference().getName();
        String name = sink.getName();
        if (owner == null || name == null) {
            return null;
        }
        String cls = owner.replace('.', '/');

        // SQL: JDK / common frameworks
        if (cls.startsWith("java/sql/") || cls.startsWith("javax/sql/")) {
            return "sql";
        }
        if (cls.startsWith("org/springframework/jdbc/")) {
            return "sql";
        }

        // SSRF: java.net.URL openConnection/openStream is a common sink
        if ("java/net/URL".equals(cls)) {
            if ("openConnection".equals(name) || "openStream".equals(name) || "getContent".equals(name)) {
                return "ssrf";
            }
        }

        // XSS: servlet response output / PrintWriter output
        if ("javax/servlet/ServletOutputStream".equals(cls) || "jakarta/servlet/ServletOutputStream".equals(cls)) {
            if ("print".equals(name) || "println".equals(name) || "write".equals(name)) {
                return "xss";
            }
        }
        if ("java/io/PrintWriter".equals(cls)) {
            if ("print".equals(name) || "println".equals(name) || "write".equals(name) || "append".equals(name)) {
                return "xss";
            }
        }
        if ("javax/servlet/http/HttpServletResponse".equals(cls) || "jakarta/servlet/http/HttpServletResponse".equals(cls)) {
            // getWriter/getOutputStream are often used as the last hop before output.
            if ("getWriter".equals(name) || "getOutputStream".equals(name)) {
                return "xss";
            }
        }

        // File: common filesystem APIs
        if ("java/io/File".equals(cls)
                || "java/nio/file/Files".equals(cls)
                || "java/nio/file/Path".equals(cls)
                || "java/nio/file/Paths".equals(cls)
                || "java/io/FileInputStream".equals(cls)
                || "java/io/FileOutputStream".equals(cls)
                || "java/io/RandomAccessFile".equals(cls)
                || "java/io/FileReader".equals(cls)
                || "java/io/FileWriter".equals(cls)) {
            return "file";
        }

        return null;
    }
}
