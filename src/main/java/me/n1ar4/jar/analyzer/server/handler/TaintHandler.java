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
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.server.handler.base.BaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.taint.TaintAnalyzer;
import me.n1ar4.jar.analyzer.taint.TaintResult;

import java.util.List;

public class TaintHandler extends BaseHandler implements HttpHandler {
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = MainForm.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        DfsApiUtil.ParseResult parse = DfsApiUtil.parse(session);
        if (parse.getError() != null) {
            return parse.getError();
        }
        // 先跑 DFS，再进行污点分析验证
        DfsApiUtil.DfsRequest req = parse.getRequest();
        List<DFSResult> resultList = DfsApiUtil.run(req);
        Integer taintMax = req.maxPaths != null ? req.maxPaths : req.maxLimit;
        List<TaintResult> taintResults = TaintAnalyzer.analyze(resultList, req.timeoutMs, taintMax);
        String json = JSON.toJSONString(taintResults);
        return buildJSON(json);
    }
}
