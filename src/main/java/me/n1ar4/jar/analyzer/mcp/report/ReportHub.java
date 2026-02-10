/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.mcp.report;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import fi.iki.elonen.NanoWSD;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.entity.VulReportEntity;
import me.n1ar4.jar.analyzer.utils.InterruptUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ReportHub {
    private static final Logger logger = LogManager.getLogger();
    private final Set<NanoWSD.WebSocket> clients = ConcurrentHashMap.newKeySet();

    public void addClient(NanoWSD.WebSocket socket) {
        if (socket == null) {
            return;
        }
        clients.add(socket);
    }

    public void removeClient(NanoWSD.WebSocket socket) {
        if (socket == null) {
            return;
        }
        clients.remove(socket);
    }

    public void broadcast(ReportData data) {
        if (data == null) {
            return;
        }
        String json;
        try {
            json = JSON.toJSONString(data);
        } catch (Exception ex) {
            logger.debug("serialize report failed: {}", ex.toString());
            return;
        }
        List<NanoWSD.WebSocket> toRemove = new ArrayList<>();
        for (NanoWSD.WebSocket ws : clients) {
            if (ws == null || !ws.isOpen()) {
                toRemove.add(ws);
                continue;
            }
            try {
                ws.send(json);
            } catch (IOException ex) {
                toRemove.add(ws);
            }
        }
        for (NanoWSD.WebSocket ws : toRemove) {
            if (ws != null) {
                clients.remove(ws);
            }
        }
    }

    public void save(ReportData data) {
        if (data == null) {
            return;
        }
        try {
            VulReportEntity e = new VulReportEntity();
            e.setType(data.getType());
            e.setReason(data.getReason());
            e.setScore(data.getScore());
            e.setTrace(JSON.toJSONString(data.getTrace()));
            DatabaseManager.saveVulReport(e);
        } catch (Throwable t) {
            InterruptUtil.restoreInterruptIfNeeded(t);
            logger.debug("save report failed: {}", t.toString());
        }
    }

    public JSONArray loadHistory() {
        List<VulReportEntity> list = DatabaseManager.getVulReports();
        JSONArray out = new JSONArray();
        if (list == null || list.isEmpty()) {
            return out;
        }
        for (VulReportEntity e : list) {
            if (e == null) {
                continue;
            }
            JSONObject o = new JSONObject();
            o.put("type", e.getType());
            o.put("reason", e.getReason());
            o.put("score", e.getScore() == null ? 0 : e.getScore());
            String trace = e.getTrace();
            if (trace != null && !trace.trim().isEmpty()) {
                try {
                    o.put("trace", JSON.parseArray(trace));
                } catch (Exception ex) {
                    o.put("trace", new JSONArray());
                }
            } else {
                o.put("trace", new JSONArray());
            }
            out.add(o);
        }
        return out;
    }
}

