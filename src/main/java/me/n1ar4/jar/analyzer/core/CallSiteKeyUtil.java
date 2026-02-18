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
    private CallSiteKeyUtil() {
    }

    public static String buildCallSiteKey(CallSiteEntity site) {
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
                safe(site.getCallIndex());
    }

    public static String buildEdgeLookupKey(Integer callerJarId,
                                            String callerClass,
                                            String callerMethod,
                                            String callerDesc,
                                            String calleeOwner,
                                            String calleeMethod,
                                            String calleeDesc,
                                            Integer opcode) {
        return safe(callerJarId) + "|" +
                safe(callerClass) + "|" +
                safe(callerMethod) + "|" +
                safe(callerDesc) + "|" +
                safe(calleeOwner) + "|" +
                safe(calleeMethod) + "|" +
                safe(calleeDesc) + "|" +
                safe(opcode);
    }

    public static String buildEdgeLookupKey(CallSiteEntity site) {
        if (site == null) {
            return "";
        }
        return buildEdgeLookupKey(
                site.getJarId(),
                site.getCallerClassName(),
                site.getCallerMethodName(),
                site.getCallerMethodDesc(),
                site.getCalleeOwner(),
                site.getCalleeMethodName(),
                site.getCalleeMethodDesc(),
                site.getOpCode()
        );
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
