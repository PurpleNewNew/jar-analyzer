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
import me.n1ar4.jar.analyzer.entity.LeakResult;
import me.n1ar4.jar.analyzer.leak.LeakScanService;
import me.n1ar4.jar.analyzer.utils.StringUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

class LeakApiUtil {
    private static final Logger logger = LogManager.getLogger();
    private static final LeakScanService LEAK_SCAN_SERVICE = new LeakScanService();

    static class ParseResult {
        private final LeakScanService.Request request;
        private final NanoHTTPD.Response error;

        ParseResult(LeakScanService.Request request, NanoHTTPD.Response error) {
            this.request = request;
            this.error = error;
        }

        LeakScanService.Request getRequest() {
            return request;
        }

        NanoHTTPD.Response getError() {
            return error;
        }
    }

    static ParseResult parse(NanoHTTPD.IHTTPSession session) {
        Set<String> types = parseTypes(session);
        Boolean base64 = getOptionalBool(session, "base64");
        Integer limit = getInt(session, "limit");
        if (limit != null && limit <= 0) {
            limit = null;
        }
        List<String> whitelist = parseList(resolveParam(session, "whitelist", "allowlist", "include", "pkg"));
        List<String> blacklist = parseList(resolveParam(session, "blacklist", "exclude"));
        Set<String> jarNames = parseNameList(resolveParam(session, "jar", "jarName"));
        Set<Integer> jarIds = parseIntSet(getParam(session, "jarId"));
        return new ParseResult(new LeakScanService.Request(
                types,
                Boolean.TRUE.equals(base64),
                limit,
                whitelist,
                blacklist,
                jarNames,
                jarIds
        ), null);
    }

    static List<LeakResult> scan(LeakScanService.Request req, CoreEngine engine) {
        if (req == null) {
            return new ArrayList<>();
        }
        return LEAK_SCAN_SERVICE.scan(engine, req);
    }

    private static Set<String> parseTypes(NanoHTTPD.IHTTPSession session) {
        Set<String> types = new HashSet<>();
        types.addAll(splitParam(getParam(session, "types")));
        types.addAll(splitParam(getParam(session, "type")));
        return types;
    }

    private static String resolveParam(NanoHTTPD.IHTTPSession session, String... keys) {
        for (String key : keys) {
            if (StringUtil.isNull(key)) {
                continue;
            }
            String value = getParam(session, key);
            if (!StringUtil.isNull(value)) {
                return value;
            }
        }
        return "";
    }

    private static String getParam(NanoHTTPD.IHTTPSession session, String key) {
        List<String> data = session.getParameters().get(key);
        if (data == null || data.isEmpty()) {
            return "";
        }
        return data.get(0);
    }

    private static Set<String> splitParam(String value) {
        if (StringUtil.isNull(value)) {
            return Collections.emptySet();
        }
        String[] parts = value.split("[,;\\r\\n]+");
        Set<String> result = new HashSet<>();
        for (String part : parts) {
            if (StringUtil.isNull(part)) {
                continue;
            }
            result.add(part.trim().toLowerCase());
        }
        return result;
    }

    private static List<String> parseList(String value) {
        if (StringUtil.isNull(value)) {
            return Collections.emptyList();
        }
        String[] parts = value.split("[,;\\r\\n]+");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (StringUtil.isNull(part)) {
                continue;
            }
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private static Set<String> parseNameList(String value) {
        if (StringUtil.isNull(value)) {
            return Collections.emptySet();
        }
        String[] parts = value.split("[,;\\r\\n]+");
        Set<String> result = new LinkedHashSet<>();
        for (String part : parts) {
            if (StringUtil.isNull(part)) {
                continue;
            }
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed.toLowerCase());
            }
        }
        return result;
    }

    private static Set<Integer> parseIntSet(String value) {
        if (StringUtil.isNull(value)) {
            return Collections.emptySet();
        }
        String[] parts = value.split("[,;\\r\\n]+");
        Set<Integer> result = new LinkedHashSet<>();
        for (String part : parts) {
            if (StringUtil.isNull(part)) {
                continue;
            }
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                result.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException ex) {
                logger.debug("ignore invalid integer in list: {}", trimmed);
            }
        }
        return result;
    }

    private static Integer getInt(NanoHTTPD.IHTTPSession session, String key) {
        String value = getParam(session, key);
        if (StringUtil.isNull(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            logger.debug("invalid int param {}={}", key, value);
            return null;
        }
    }

    private static Boolean getOptionalBool(NanoHTTPD.IHTTPSession session, String key) {
        String value = getParam(session, key);
        if (StringUtil.isNull(value)) {
            return null;
        }
        String v = value.trim().toLowerCase();
        return "1".equals(v) || "true".equals(v) || "yes".equals(v) || "on".equals(v);
    }
}
