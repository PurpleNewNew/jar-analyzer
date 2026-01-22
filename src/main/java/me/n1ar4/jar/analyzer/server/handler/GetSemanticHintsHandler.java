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

import com.alibaba.fastjson2.JSON;
import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.entity.AnnoMethodResult;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.entity.SemanticHintResult;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.server.handler.base.BaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.CommonFilterUtil;
import me.n1ar4.jar.analyzer.utils.SemanticHintUtil;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.*;

public class GetSemanticHintsHandler extends BaseHandler implements HttpHandler {
    private static final int DEFAULT_LIMIT = 60;
    private static final int MAX_LIMIT = 300;
    private static final int DEFAULT_STR_LIMIT = 30;
    private static final int MAX_STR_LIMIT = 80;

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = MainForm.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        Integer jarId = getIntParam(session, "jarId");
        int limit = clamp(getIntParam(session, "limit", DEFAULT_LIMIT), 1, MAX_LIMIT);
        int strLimit = clamp(getIntParam(session, "strLimit", DEFAULT_STR_LIMIT), 1, MAX_STR_LIMIT);
        boolean excludeNoise = shouldExcludeNoise(session);

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
                    if (excludeNoise && isNoisy(method)) {
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
                    hits = filterJdkMethods(hits, session);
                    for (MethodResult method : hits) {
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

        String json = JSON.toJSONString(out);
        return buildJSON(json);
    }

    private boolean isNoisy(MethodResult method) {
        if (method == null) {
            return true;
        }
        if (CommonFilterUtil.isFilteredClass(method.getClassName())) {
            return true;
        }
        return CommonFilterUtil.isFilteredJar(method.getJarName());
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

    private Integer getIntParam(NanoHTTPD.IHTTPSession session, String key) {
        String value = getParam(session, key);
        if (StringUtil.isNull(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private int getIntParam(NanoHTTPD.IHTTPSession session, String key, int def) {
        Integer v = getIntParam(session, key);
        return v == null ? def : v;
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
