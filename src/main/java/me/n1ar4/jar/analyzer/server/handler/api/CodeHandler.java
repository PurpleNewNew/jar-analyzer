/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.server.handler.api;

import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.DecompileDispatcher;
import me.n1ar4.jar.analyzer.engine.model.MethodView;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CodeHandler extends ApiBaseHandler implements HttpHandler {
    private static final Logger logger = LogManager.getLogger();

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = requireReadyEngine();
        if (engine == null) {
            return projectNotReady();
        }
        String className = getClassParam(session);
        String methodName = getStringParam(session, "method", "methodName");
        String methodDesc = getStringParam(session, "desc", "methodDesc");
        if (StringUtil.isNull(className)) {
            return needParam("class");
        }
        if (StringUtil.isNull(methodName)) {
            return needParam("method");
        }
        String engineName = getStringParam(session, "engine", "decompiler");
        if (!StringUtil.isNull(engineName) && !"cfr".equalsIgnoreCase(engineName.trim())) {
            return buildError(
                    NanoHTTPD.Response.Status.BAD_REQUEST,
                    "invalid_engine",
                    "engine must be cfr");
        }
        boolean includeFull = getBoolParam(session, "full") || getBoolParam(session, "includeFull");
        Integer jarId = getIntParamNullable(session, "jarId");
        try {
            MethodResolution resolution = resolveMethod(engine, className, methodName, methodDesc, jarId);
            if (resolution.ambiguous()) {
                return buildError(
                        NanoHTTPD.Response.Status.NOT_FOUND,
                        "method_not_found",
                        "method not found: " + className + "#" + methodName
                                + " (descriptor required to disambiguate overloads)");
            }
            String resolvedMethodDesc = resolution.methodDesc();
            String absPath = jarId == null
                    ? engine.getAbsPath(className)
                    : engine.getAbsPath(className, jarId);
            if (StringUtil.isNull(absPath)) {
                return buildError(
                        NanoHTTPD.Response.Status.NOT_FOUND,
                        "class_not_found",
                        "class file not found: " + className);
            }
            String decompiledCode = DecompileDispatcher.decompile(Paths.get(absPath));
            if (StringUtil.isNull(decompiledCode)) {
                return buildError(
                        NanoHTTPD.Response.Status.INTERNAL_ERROR,
                        "decompile_failed",
                        "failed to decompile class: " + className);
            }
            String methodCode = extractMethodCode(decompiledCode, methodName, resolvedMethodDesc);
            if (methodCode == null) {
                return buildError(
                        NanoHTTPD.Response.Status.NOT_FOUND,
                        "method_not_found",
                        "method not found: " + className + "#" + methodName + safeDesc(resolvedMethodDesc));
            }
            Map<String, Object> result = new HashMap<>();
            result.put("engine", "cfr");
            result.put("className", className);
            result.put("methodName", methodName);
            result.put("methodDesc", safeDesc(resolvedMethodDesc));
            result.put("methodCode", methodCode);
            if (includeFull) {
                result.put("fullClassCode", decompiledCode);
            }
            return ok(result);
        } catch (Exception e) {
            logger.error("error getting method code: " + e.getMessage(), e);
            return buildError(
                    NanoHTTPD.Response.Status.INTERNAL_ERROR,
                    "code_error",
                    "error: " + e.getMessage());
        }
    }

    private static String safeDesc(String methodDesc) {
        String desc = methodDesc == null ? "" : methodDesc.trim();
        return desc.isEmpty() ? "" : desc;
    }

    private static MethodResolution resolveMethod(CoreEngine engine,
                                                  String className,
                                                  String methodName,
                                                  String methodDesc,
                                                  Integer jarId) {
        String normalizedDesc = normalizeMethodDesc(methodDesc);
        if (normalizedDesc != null) {
            return MethodResolution.resolved(normalizedDesc);
        }
        if (engine == null || StringUtil.isNull(className) || StringUtil.isNull(methodName)) {
            return MethodResolution.resolved(null);
        }
        ArrayList<MethodView> matches = engine.getMethod(className, methodName, null);
        String resolvedDesc = null;
        for (MethodView match : matches) {
            if (match == null) {
                continue;
            }
            if (jarId != null && match.getJarId() != jarId) {
                continue;
            }
            String candidateDesc = normalizeMethodDesc(match.getMethodDesc());
            if (candidateDesc == null) {
                continue;
            }
            if (resolvedDesc == null) {
                resolvedDesc = candidateDesc;
                continue;
            }
            if (!resolvedDesc.equals(candidateDesc)) {
                return MethodResolution.ambiguousResolution();
            }
        }
        return MethodResolution.resolved(resolvedDesc);
    }

    private static String normalizeMethodDesc(String methodDesc) {
        String desc = safeDesc(methodDesc);
        if (desc.isEmpty() || "null".equalsIgnoreCase(desc)) {
            return null;
        }
        return desc;
    }

    private static final class MethodResolution {
        private final String methodDesc;
        private final boolean ambiguous;

        private MethodResolution(String methodDesc, boolean ambiguous) {
            this.methodDesc = methodDesc;
            this.ambiguous = ambiguous;
        }

        private String methodDesc() {
            return methodDesc;
        }

        private boolean ambiguous() {
            return ambiguous;
        }

        private static MethodResolution resolved(String methodDesc) {
            return new MethodResolution(methodDesc, false);
        }

        private static MethodResolution ambiguousResolution() {
            return new MethodResolution(null, true);
        }
    }
}
