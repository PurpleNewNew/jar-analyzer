/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.server.handler.api;

import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ProjectSelectHandler extends ApiBaseHandler implements HttpHandler {
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        if (session.getMethod() != NanoHTTPD.Method.POST) {
            return buildError(
                    NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED,
                    "method_not_allowed",
                    "POST required");
        }
        String project = ProjectApiUtil.resolveString(session, "project");
        if (project.isBlank()) {
            return needParam("project");
        }
        String previous = DatabaseManager.activeProjectKey();
        DatabaseManager.selectDatabase(project);
        String active = DatabaseManager.activeProjectKey();
        Path activeHome = DatabaseManager.activeProjectHome();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("project", active);
        out.put("previousProject", previous);
        out.put("activeHome", activeHome == null ? "" : activeHome.toAbsolutePath().normalize().toString());
        out.put("switched", !previous.equals(active));
        return ok(out);
    }
}

