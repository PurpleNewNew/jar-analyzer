/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.server.handler;

import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.entity.AnnoMethodResult;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.entity.SemanticHintResult;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.server.handler.api.ApiBaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.CommonFilterUtil;
import me.n1ar4.jar.analyzer.utils.SemanticHintUtil;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.*;

public class GetSemanticHintsHandler extends ApiBaseHandler implements HttpHandler {
    private static final int DEFAULT_LIMIT = 60;
    private static final int MAX_LIMIT = 300;
    private static final int DEFAULT_STR_LIMIT = 30;
    private static final int MAX_STR_LIMIT = 80;

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = EngineContext.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        Integer jarId = getIntParamNullable(session, "jarId");
        int limit = clamp(getIntParam(session, "limit", DEFAULT_LIMIT), 1, MAX_LIMIT);
        int strLimit = clamp(getIntParam(session, "strLimit", DEFAULT_STR_LIMIT), 1, MAX_STR_LIMIT);
        boolean includeJdk = includeJdk(session);

        List<SemanticHintResult> out = new ArrayList<>();
        for (SemanticHintUtil.SemanticCategory category : SemanticHintUtil.getCategories()) {
            SemanticHintResult result = new SemanticHintResult();
            result.setName(category.name);
            result.setDescription(category.description);
            if (category.stringKeywords != null) {
                result.setKeywords(category.stringKeywords);
            }

            LinkedHashMap<String, SemanticHintResult.HintMethod> dedup = new LinkedHashMap<>();

            if (category.annotations != null && !category.annotations.isEmpty()) {
                ArrayList<AnnoMethodResult> annos = engine.getMethodsByAnno(
                        category.annotations, "contains", "any", jarId, 0, limit);
                for (AnnoMethodResult ar : annos) {
                    MethodResult method = toMethodResult(ar);
                    if (isNoisy(method, includeJdk)) {
                        continue;
                    }
                    String key = methodKey(method);
                    if (dedup.containsKey(key)) {
                        continue;
                    }
                    SemanticHintResult.HintMethod hint = new SemanticHintResult.HintMethod();
                    hint.setMethod(method);
                    hint.setSource("annotation");
                    hint.setEvidence(normalizeAnno(ar.getAnnoName(), ar.getAnnoScope()));
                    dedup.put(key, hint);
                }
            }

            if (category.stringKeywords != null && !category.stringKeywords.isEmpty()) {
                for (String kw : category.stringKeywords) {
                    if (StringUtil.isNull(kw)) {
                        continue;
                    }
                    ArrayList<MethodResult> hits = engine.getMethodsByStr(
                            kw, jarId, null, strLimit, "auto");
                    List<MethodResult> filtered = filterMethods(hits, includeJdk);
                    for (MethodResult method : filtered) {
                        String key = methodKey(method);
                        if (dedup.containsKey(key)) {
                            continue;
                        }
                        SemanticHintResult.HintMethod hint = new SemanticHintResult.HintMethod();
                        hint.setMethod(method);
                        hint.setSource("string");
                        hint.setEvidence(kw);
                        dedup.put(key, hint);
                        if (dedup.size() >= limit) {
                            break;
                        }
                    }
                    if (dedup.size() >= limit) {
                        break;
                    }
                }
            }

            List<SemanticHintResult.HintMethod> methods = new ArrayList<>(dedup.values());
            result.setMethods(methods);
            result.setCount(methods.size());
            out.add(result);
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("count", out.size());
        return ok(out, meta);
    }

    private boolean isNoisy(MethodResult method, boolean includeJdk) {
        if (method == null) {
            return true;
        }
        if (!includeJdk) {
            if (CommonFilterUtil.isFilteredClass(method.getClassName())) {
                return true;
            }
            return CommonFilterUtil.isFilteredJar(method.getJarName());
        }
        return false;
    }

    private MethodResult toMethodResult(AnnoMethodResult ar) {
        MethodResult m = new MethodResult();
        m.setClassName(ar.getClassName());
        m.setMethodName(ar.getMethodName());
        m.setMethodDesc(ar.getMethodDesc());
        m.setJarId(ar.getJarId() == null ? 0 : ar.getJarId());
        m.setJarName(ar.getJarName());
        m.setLineNumber(ar.getLineNumber());
        return m;
    }

    private String methodKey(MethodResult method) {
        if (method == null) {
            return "";
        }
        return method.getClassName() + "#" + method.getMethodName() + "#" + method.getMethodDesc();
    }

    private String normalizeAnno(String annoName, String scope) {
        if (StringUtil.isNull(annoName)) {
            return "";
        }
        String norm = SemanticHintUtil.normalizeAnno(annoName);
        if (StringUtil.isNull(scope)) {
            return norm;
        }
        return norm + " (" + scope + ")";
    }

    private int clamp(int v, int min, int max) {
        if (v < min) {
            return min;
        }
        if (v > max) {
            return max;
        }
        return v;
    }
}
