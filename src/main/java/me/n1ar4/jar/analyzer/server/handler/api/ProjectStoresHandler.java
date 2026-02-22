/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.server.handler.api;

import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProjectStoresHandler extends ApiBaseHandler implements HttpHandler {
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        if (session.getMethod() != NanoHTTPD.Method.GET) {
            return buildError(
                    NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED,
                    "method_not_allowed",
                    "GET required");
        }
        List<Map<String, Object>> stores = DatabaseManager.listProjectStores();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("activeProject", DatabaseManager.activeProjectKey());
        Path activeHome = DatabaseManager.activeProjectHome();
        out.put("activeHome", activeHome == null ? "" : activeHome.toAbsolutePath().normalize().toString());
        out.put("stores", stores);
        return ok(out);
    }
}

