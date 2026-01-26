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
import me.n1ar4.jar.analyzer.engine.CFRDecompileEngine;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.DecompileEngine;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class CodeHandler extends ApiBaseHandler implements HttpHandler {
    private static final Logger logger = LogManager.getLogger();

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = MainForm.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
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
        if (StringUtil.isNull(engineName)) {
            engineName = "cfr";
        }
        String engineKey = engineName.trim().toLowerCase();
        if (!"cfr".equals(engineKey) && !"fernflower".equals(engineKey)) {
            return buildError(
                    NanoHTTPD.Response.Status.BAD_REQUEST,
                    "invalid_engine",
                    "engine must be cfr or fernflower");
        }
        boolean includeFull = getBoolParam(session, "full");
        if (!includeFull) {
            includeFull = getBoolParam(session, "includeFull");
        }
        try {
            String absPath = engine.getAbsPath(className);
            if (StringUtil.isNull(absPath)) {
                return buildError(
                        NanoHTTPD.Response.Status.NOT_FOUND,
                        "class_not_found",
                        "class file not found: " + className);
            }
            String decompiledCode = "cfr".equals(engineKey)
                    ? CFRDecompileEngine.decompile(absPath)
                    : DecompileEngine.decompile(Paths.get(absPath), true);
            if (StringUtil.isNull(decompiledCode)) {
                return buildError(
                        NanoHTTPD.Response.Status.INTERNAL_ERROR,
                        "decompile_failed",
                        "failed to decompile class: " + className);
            }
            String methodCode = extractMethodCode(decompiledCode, methodName, methodDesc);
            if (methodCode == null) {
                methodCode = "";
            }
            Map<String, Object> result = new HashMap<>();
            result.put("engine", engineKey);
            result.put("className", className);
            result.put("methodName", methodName);
            result.put("methodDesc", methodDesc);
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
}
