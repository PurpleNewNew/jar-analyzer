/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.entity.CallSiteEntity;

public final class CallSiteKeyUtil {
    private static final int INSN_INDEX_POSITION = 10;

    private CallSiteKeyUtil() {
    }

    public static String buildCallSiteKey(CallSiteEntity site) {
        return buildCallSiteKey(site, null);
    }

    public static String buildCallSiteKey(CallSiteEntity site, Integer insnIndex) {
        if (site == null) {
            return "";
        }
        return safe(site.getJarId()) + "|" +
                safe(site.getCallerClassName()) + "|" +
                safe(site.getCallerMethodName()) + "|" +
                safe(site.getCallerMethodDesc()) + "|" +
                safe(site.getCalleeOwner()) + "|" +
                safe(site.getCalleeMethodName()) + "|" +
                safe(site.getCalleeMethodDesc()) + "|" +
                safe(site.getOpCode()) + "|" +
                safe(site.getCallIndex()) + "|" +
                safe(site.getLineNumber()) + "|" +
                safe(insnIndex);
    }

    public static int parseInsnIndex(String callSiteKey) {
        if (callSiteKey == null || callSiteKey.isBlank()) {
            return -1;
        }
        int start = 0;
        int part = 0;
        for (int i = 0; i <= callSiteKey.length(); i++) {
            if (i < callSiteKey.length() && callSiteKey.charAt(i) != '|') {
                continue;
            }
            if (part == INSN_INDEX_POSITION) {
                String raw = callSiteKey.substring(start, i).trim();
                if (raw.isEmpty()) {
                    return -1;
                }
                try {
                    return Integer.parseInt(raw);
                } catch (NumberFormatException ex) {
                    return -1;
                }
            }
            part++;
            start = i + 1;
        }
        return -1;
    }

    private static String safe(String v) {
        if (v == null) {
            return "";
        }
        return v;
    }

    private static String safe(Integer v) {
        if (v == null) {
            return "-1";
        }
        return String.valueOf(v);
    }
}
