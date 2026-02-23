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

import me.n1ar4.jar.analyzer.chains.SinkModel;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.SqlSessionFactoryUtil;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.entity.FindingDetail;
import me.n1ar4.jar.analyzer.entity.FindingPathNode;
import me.n1ar4.jar.analyzer.entity.FindingSummary;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.rules.VulnerabilityRegistry;
import me.n1ar4.jar.analyzer.utils.StringUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.apache.ibatis.session.SqlSession;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

final class FindingEngineV2 {
    private static final Logger logger = LogManager.getLogger();
    private static final int MAX_DEPTH = 12;
    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 1000;
    private static volatile long cacheBuildSeq = -1L;
    private static final Map<String, FindingDetail> DETAIL_CACHE = new ConcurrentHashMap<>();

    private FindingEngineV2() {
    }

    static List<FindingSummary> search(CoreEngine engine,
                                       Set<String> nameFilter,
                                       String levelFilter,
                                       int offset,
                                       int limit) {
        if (engine == null) {
            return Collections.emptyList();
        }
        ensureCacheFresh();
        Set<String> entryClasses = collectEntrypointClasses(engine);
        Set<String> entryMethods = collectEntrypointMethods(engine);

        List<FindingDetail> details = new ArrayList<>();
        Set<String> dedup = new HashSet<>();
        List<SinkModel> sinkModels = VulnerabilityRegistry.getSinkModels();
        for (SinkModel sinkModel : sinkModels) {
            if (sinkModel == null) {
                continue;
            }
            String rule = sinkModel.getCategory();
            String severity = normalizeSeverity(sinkModel.getSeverity());
            if (!nameFilter.isEmpty() && !nameFilter.contains(rule)) {
                continue;
            }
            if (!StringUtil.isNull(levelFilter) && !severity.equalsIgnoreCase(levelFilter.trim())) {
                continue;
            }
            String sinkDesc = normalizeSinkDesc(sinkModel.getMethodDesc());
            ArrayList<MethodResult> sinkCallers = engine.getCallers(
                    sinkModel.getClassName(),
                    sinkModel.getMethodName(),
                    sinkDesc);
            if (sinkCallers == null || sinkCallers.isEmpty()) {
                continue;
            }
            FindingPathNode sinkNode = resolveSinkNode(engine, sinkModel);
            for (MethodResult caller : sinkCallers) {
                if (caller == null) {
                    continue;
                }
                Trace nearestApp = findNearestApp(engine, caller);
                if (nearestApp == null || nearestApp.path == null || nearestApp.path.isEmpty()) {
                    continue;
                }
                MethodResult firstApp = nearestApp.target;
                Trace entryTrace = findEntrypoint(engine, firstApp, entryClasses, entryMethods);
                if (entryTrace == null || entryTrace.target == null) {
                    continue;
                }
                MethodResult entry = entryTrace.target;
                List<MethodResult> full = mergePath(entryTrace.path, nearestApp.path);
                FindingDetail detail = buildDetail(engine, rule, severity, entry, firstApp, sinkNode, full);
                if (detail == null || detail.getSummary() == null) {
                    continue;
                }
                String dedupKey = rule + "|"
                        + key(entry) + "|"
                        + key(firstApp) + "|"
                        + key(caller) + "|"
                        + sinkNode.getClassName() + "#" + sinkNode.getMethodName() + "#" + sinkNode.getMethodDesc();
                if (!dedup.add(dedupKey)) {
                    continue;
                }
                details.add(detail);
            }
        }

        details.sort((a, b) -> Double.compare(
                b.getSummary().getScore(), a.getSummary().getScore()));
        storeDetails(details);
        persistFindingCache(details);

        int safeOffset = Math.max(0, offset);
        int safeLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        int from = Math.min(safeOffset, details.size());
        int to = Math.min(from + safeLimit, details.size());
        List<FindingSummary> out = new ArrayList<>();
        for (int i = from; i < to; i++) {
            out.add(details.get(i).getSummary());
        }
        return out;
    }

    static FindingDetail getDetail(String findingId) {
        if (StringUtil.isNull(findingId)) {
            return null;
        }
        ensureCacheFresh();
        String id = findingId.trim();
        FindingDetail detail = DETAIL_CACHE.get(id);
        if (detail != null) {
            return detail;
        }
        return loadDetailFromDb(id);
    }

    private static void ensureCacheFresh() {
        long buildSeq = DatabaseManager.getBuildSeq();
        if (cacheBuildSeq == buildSeq) {
            return;
        }
        synchronized (FindingEngineV2.class) {
            if (cacheBuildSeq == buildSeq) {
                return;
            }
            DETAIL_CACHE.clear();
            cacheBuildSeq = buildSeq;
        }
    }

    private static Set<String> collectEntrypointClasses(CoreEngine engine) {
        Set<String> out = new HashSet<>();
        addClasses(out, engine.getAllSpringC());
        addClasses(out, engine.getAllSpringI());
        addClasses(out, engine.getAllServlets());
        addClasses(out, engine.getAllFilters());
        addClasses(out, engine.getAllListeners());
        return out;
    }

    private static void addClasses(Set<String> out, List<ClassResult> classes) {
        if (out == null || classes == null) {
            return;
        }
        for (ClassResult item : classes) {
            if (item == null || StringUtil.isNull(item.getClassName())) {
                continue;
            }
            out.add(item.getClassName());
        }
    }

    private static Set<String> collectEntrypointMethods(CoreEngine engine) {
        Set<String> out = new HashSet<>();
        List<MethodResult> mappings = engine.getSpringMappingsAll(null, null, null, null);
        if (mappings == null) {
            return out;
        }
        for (MethodResult item : mappings) {
            if (item == null || StringUtil.isNull(item.getClassName())
                    || StringUtil.isNull(item.getMethodName())) {
                continue;
            }
            out.add(key(item));
        }
        return out;
    }

    private static Trace findNearestApp(CoreEngine engine, MethodResult start) {
        if (start == null) {
            return null;
        }
        Queue<Node> queue = new ArrayDeque<>();
        Map<String, MethodResult> seen = new HashMap<>();
        Map<String, String> parent = new HashMap<>();
        String startKey = key(start);
        queue.add(new Node(start, 0));
        seen.put(startKey, start);
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            MethodResult current = node.method;
            String currentKey = key(current);
            if (isApp(engine, current)) {
                return new Trace(current, buildPath(currentKey, parent, seen));
            }
            if (node.depth >= MAX_DEPTH) {
                continue;
            }
            ArrayList<MethodResult> callers = engine.getCallers(
                    current.getClassName(), current.getMethodName(), current.getMethodDesc());
            if (callers == null || callers.isEmpty()) {
                continue;
            }
            for (MethodResult caller : callers) {
                if (caller == null) {
                    continue;
                }
                String callerKey = key(caller);
                if (seen.containsKey(callerKey)) {
                    continue;
                }
                seen.put(callerKey, caller);
                parent.put(callerKey, currentKey);
                queue.add(new Node(caller, node.depth + 1));
            }
        }
        return null;
    }

    private static Trace findEntrypoint(CoreEngine engine,
                                        MethodResult start,
                                        Set<String> entryClasses,
                                        Set<String> entryMethods) {
        if (start == null) {
            return null;
        }
        Queue<Node> queue = new ArrayDeque<>();
        Map<String, MethodResult> seen = new HashMap<>();
        Map<String, String> parent = new HashMap<>();
        String startKey = key(start);
        queue.add(new Node(start, 0));
        seen.put(startKey, start);
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            MethodResult current = node.method;
            String currentKey = key(current);
            if (isEntrypoint(current, entryClasses, entryMethods)) {
                return new Trace(current, buildPath(currentKey, parent, seen));
            }
            if (node.depth >= MAX_DEPTH) {
                continue;
            }
            ArrayList<MethodResult> callers = engine.getCallers(
                    current.getClassName(), current.getMethodName(), current.getMethodDesc());
            if (callers == null || callers.isEmpty()) {
                continue;
            }
            for (MethodResult caller : callers) {
                if (caller == null || !isApp(engine, caller)) {
                    continue;
                }
                String callerKey = key(caller);
                if (seen.containsKey(callerKey)) {
                    continue;
                }
                seen.put(callerKey, caller);
                parent.put(callerKey, currentKey);
                queue.add(new Node(caller, node.depth + 1));
            }
        }
        return null;
    }

    private static List<MethodResult> buildPath(String targetKey,
                                                Map<String, String> parent,
                                                Map<String, MethodResult> seen) {
        List<MethodResult> path = new ArrayList<>();
        String current = targetKey;
        while (current != null) {
            MethodResult method = seen.get(current);
            if (method == null) {
                break;
            }
            path.add(method);
            current = parent.get(current);
        }
        return path;
    }

    private static List<MethodResult> mergePath(List<MethodResult> entryToApp,
                                                List<MethodResult> appToCaller) {
        List<MethodResult> out = new ArrayList<>();
        if (entryToApp != null) {
            out.addAll(entryToApp);
        }
        if (appToCaller != null && !appToCaller.isEmpty()) {
            for (int i = 1; i < appToCaller.size(); i++) {
                out.add(appToCaller.get(i));
            }
        }
        return out;
    }

    private static FindingDetail buildDetail(CoreEngine engine,
                                             String rule,
                                             String severity,
                                             MethodResult entry,
                                             MethodResult firstApp,
                                             FindingPathNode sinkNode,
                                             List<MethodResult> pathMethods) {
        if (entry == null || firstApp == null || sinkNode == null) {
            return null;
        }
        FindingSummary summary = new FindingSummary();
        summary.setRule(rule == null ? "unknown" : rule);
        summary.setSeverity(severity);
        summary.setEntrypoint(toPathNode(engine, entry));
        summary.setFirstAppFrame(toPathNode(engine, firstApp));
        summary.setSink(sinkNode);

        FindingDetail detail = new FindingDetail();
        detail.setSummary(summary);
        List<FindingPathNode> path = new ArrayList<>();
        if (pathMethods != null) {
            for (MethodResult method : pathMethods) {
                path.add(toPathNode(engine, method));
            }
        }
        path.add(sinkNode);
        detail.setPath(path);
        int pathLen = path.size();
        summary.setPathLen(pathLen);
        summary.setScore(score(summary.getSeverity(), pathLen));
        summary.setFindingId(buildFindingId(summary, path));
        return detail;
    }

    private static String normalizeSinkDesc(String desc) {
        if (desc == null) {
            return null;
        }
        String value = desc.trim();
        if (value.isEmpty() || "*".equals(value) || "null".equalsIgnoreCase(value)) {
            return null;
        }
        return value;
    }

    private static String normalizeSeverity(String severity) {
        if (StringUtil.isNull(severity)) {
            return "medium";
        }
        String value = severity.trim().toLowerCase(Locale.ROOT);
        if ("high".equals(value) || "medium".equals(value) || "low".equals(value)) {
            return value;
        }
        return "medium";
    }

    private static double score(String severity, int pathLen) {
        int base;
        if ("high".equalsIgnoreCase(severity)) {
            base = 100;
        } else if ("medium".equalsIgnoreCase(severity)) {
            base = 70;
        } else {
            base = 45;
        }
        int bonus = Math.max(0, 20 - Math.max(pathLen - 1, 0) * 2);
        return base + bonus;
    }

    private static boolean isEntrypoint(MethodResult method,
                                        Set<String> entryClasses,
                                        Set<String> entryMethods) {
        if (method == null) {
            return false;
        }
        if (entryMethods.contains(key(method))) {
            return true;
        }
        return entryClasses.contains(method.getClassName());
    }

    private static boolean isApp(CoreEngine engine, MethodResult method) {
        if (engine == null || method == null) {
            return false;
        }
        String role = engine.getClassRole(method.getClassName(), method.getJarId());
        return "APP".equalsIgnoreCase(role);
    }

    private static FindingPathNode toPathNode(CoreEngine engine, MethodResult method) {
        FindingPathNode node = new FindingPathNode();
        if (method == null) {
            return node;
        }
        node.setClassName(method.getClassName());
        node.setMethodName(method.getMethodName());
        node.setMethodDesc(method.getMethodDesc());
        node.setJarId(method.getJarId());
        node.setJarName(method.getJarName());
        if (engine != null) {
            node.setClassRole(engine.getClassRole(method.getClassName(), method.getJarId()));
        }
        return node;
    }

    private static FindingPathNode resolveSinkNode(CoreEngine engine, SinkModel sinkModel) {
        FindingPathNode node = new FindingPathNode();
        node.setClassName(sinkModel.getClassName());
        node.setMethodName(sinkModel.getMethodName());
        node.setMethodDesc(sinkModel.getMethodDesc());
        ArrayList<MethodResult> sinkMethods = engine.getMethod(
                sinkModel.getClassName(),
                sinkModel.getMethodName(),
                normalizeSinkDesc(sinkModel.getMethodDesc()));
        if (sinkMethods != null && !sinkMethods.isEmpty()) {
            MethodResult method = sinkMethods.get(0);
            node.setJarId(method.getJarId());
            node.setJarName(method.getJarName());
            node.setClassRole(engine.getClassRole(method.getClassName(), method.getJarId()));
        } else {
            node.setClassRole(engine.getClassRole(sinkModel.getClassName(), null));
        }
        return node;
    }

    private static String key(MethodResult method) {
        if (method == null) {
            return "";
        }
        return method.getClassName() + "#"
                + method.getMethodName() + "#"
                + method.getMethodDesc() + "#"
                + method.getJarId();
    }

    private static String buildFindingId(FindingSummary summary, List<FindingPathNode> path) {
        StringBuilder sb = new StringBuilder();
        sb.append(summary.getRule()).append("|")
                .append(summary.getSeverity()).append("|")
                .append(nodeKey(summary.getEntrypoint())).append("|")
                .append(nodeKey(summary.getFirstAppFrame())).append("|")
                .append(nodeKey(summary.getSink()));
        if (path != null) {
            for (FindingPathNode node : path) {
                sb.append("|").append(nodeKey(node));
            }
        }
        return sha256Hex(sb.toString());
    }

    private static String nodeKey(FindingPathNode node) {
        if (node == null) {
            return "";
        }
        return node.getClassName() + "#"
                + node.getMethodName() + "#"
                + node.getMethodDesc() + "#"
                + node.getJarId();
    }

    private static String sha256Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] data = digest.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(data.length * 2);
            for (byte b : data) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    private static void storeDetails(List<FindingDetail> details) {
        DETAIL_CACHE.clear();
        if (details == null || details.isEmpty()) {
            return;
        }
        for (FindingDetail detail : details) {
            if (detail == null || detail.getSummary() == null
                    || StringUtil.isNull(detail.getSummary().getFindingId())) {
                continue;
            }
            DETAIL_CACHE.put(detail.getSummary().getFindingId(), detail);
        }
    }

    private static void persistFindingCache(List<FindingDetail> details) {
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(false)) {
            Connection conn = session.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement clear = conn.prepareStatement("DELETE FROM finding_cache");
                 PreparedStatement insert = conn.prepareStatement(
                         "INSERT INTO finding_cache(fid, rule_name, severity, sink_mid, app_mid, entry_mid, score, path_len, evidence_json) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                clear.executeUpdate();
                if (details != null) {
                    for (FindingDetail detail : details) {
                        if (detail == null || detail.getSummary() == null) {
                            continue;
                        }
                        FindingSummary s = detail.getSummary();
                        insert.setString(1, s.getFindingId());
                        insert.setString(2, s.getRule());
                        insert.setString(3, s.getSeverity());
                        insert.setInt(4, -1);
                        insert.setInt(5, -1);
                        insert.setInt(6, -1);
                        insert.setDouble(7, s.getScore());
                        insert.setInt(8, s.getPathLen());
                        insert.setString(9, com.alibaba.fastjson2.JSON.toJSONString(detail));
                        insert.addBatch();
                    }
                }
                insert.executeBatch();
                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                logger.debug("persist finding cache fail: {}", ex.toString());
            }
        } catch (Exception ex) {
            logger.debug("persist finding cache fail: {}", ex.toString());
        }
    }

    private static FindingDetail loadDetailFromDb(String findingId) {
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true);
             PreparedStatement ps = session.getConnection().prepareStatement(
                     "SELECT evidence_json FROM finding_cache WHERE fid = ? LIMIT 1")) {
            ps.setString(1, findingId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String json = rs.getString("evidence_json");
                if (StringUtil.isNull(json)) {
                    return null;
                }
                FindingDetail detail = com.alibaba.fastjson2.JSON.parseObject(json, FindingDetail.class);
                if (detail != null && detail.getSummary() != null
                        && !StringUtil.isNull(detail.getSummary().getFindingId())) {
                    DETAIL_CACHE.put(detail.getSummary().getFindingId(), detail);
                }
                return detail;
            }
        } catch (Exception ex) {
            logger.debug("load finding detail from db fail: {}", ex.toString());
            return null;
        }
    }

    private static final class Node {
        private final MethodResult method;
        private final int depth;

        private Node(MethodResult method, int depth) {
            this.method = method;
            this.depth = depth;
        }
    }

    private static final class Trace {
        private final MethodResult target;
        private final List<MethodResult> path;

        private Trace(MethodResult target, List<MethodResult> path) {
            this.target = target;
            this.path = path == null ? Collections.emptyList() : path;
        }
    }
}
