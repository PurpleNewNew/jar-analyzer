/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.taint.summary;

import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class ReachabilitySerde {
    private static final String PREFIX = "v1:";

    private ReachabilitySerde() {
    }

    public static String toCacheValue(ReachabilityIndex index) {
        if (index == null) {
            return null;
        }
        String payload = encodePayload(index);
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            byte[] compressed = gzip(payload.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(compressed);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static ReachabilityIndex fromCacheValue(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String text = raw.trim();
        if (!text.startsWith(PREFIX)) {
            return null;
        }
        String b64 = text.substring(PREFIX.length());
        if (b64.isEmpty()) {
            return null;
        }
        try {
            byte[] compressed = Base64.getDecoder().decode(b64);
            byte[] data = gunzip(compressed);
            String payload = new String(data, StandardCharsets.UTF_8);
            return decodePayload(payload);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String encodePayload(ReachabilityIndex index) {
        StringBuilder sb = new StringBuilder();
        sb.append("toSink\n");
        for (MethodReference.Handle handle : index.getReachableToSink()) {
            appendHandle(sb, handle);
        }
        sb.append("fromSource\n");
        for (MethodReference.Handle handle : index.getReachableFromSource()) {
            appendHandle(sb, handle);
        }
        return sb.toString();
    }

    private static void appendHandle(StringBuilder sb, MethodReference.Handle handle) {
        if (sb == null || handle == null || handle.getClassReference() == null) {
            return;
        }
        String c = handle.getClassReference().getName();
        String m = handle.getName();
        String d = handle.getDesc();
        if (c == null || m == null || d == null) {
            return;
        }
        sb.append(c).append('#').append(m).append('#').append(d).append('\n');
    }

    private static ReachabilityIndex decodePayload(String payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        Set<MethodReference.Handle> toSink = new HashSet<>();
        Set<MethodReference.Handle> fromSource = new HashSet<>();
        Set<MethodReference.Handle> current = null;
        String[] lines = payload.split("\n");
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String v = line.trim();
            if (v.isEmpty()) {
                continue;
            }
            if ("toSink".equals(v)) {
                current = toSink;
                continue;
            }
            if ("fromSource".equals(v)) {
                current = fromSource;
                continue;
            }
            if (current == null) {
                continue;
            }
            MethodReference.Handle handle = parseHandle(v);
            if (handle != null) {
                current.add(handle);
            }
        }
        return new ReachabilityIndex(toSink, fromSource);
    }

    private static MethodReference.Handle parseHandle(String line) {
        int a = line.indexOf('#');
        if (a <= 0) {
            return null;
        }
        int b = line.indexOf('#', a + 1);
        if (b <= a + 1) {
            return null;
        }
        String clazz = line.substring(0, a);
        String name = line.substring(a + 1, b);
        String desc = line.substring(b + 1);
        if (clazz.isEmpty() || name.isEmpty() || desc.isEmpty()) {
            return null;
        }
        return new MethodReference.Handle(new ClassReference.Handle(clazz), name, desc);
    }

    private static byte[] gzip(byte[] data) throws Exception {
        if (data == null || data.length == 0) {
            return new byte[0];
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(baos)) {
            out.write(data);
        }
        return baos.toByteArray();
    }

    private static byte[] gunzip(byte[] data) throws Exception {
        if (data == null || data.length == 0) {
            return new byte[0];
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(data))) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                baos.write(buf, 0, n);
            }
        }
        return baos.toByteArray();
    }
}

