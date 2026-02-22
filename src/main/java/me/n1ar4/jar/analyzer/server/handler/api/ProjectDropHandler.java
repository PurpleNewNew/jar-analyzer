/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.server.handler.api;

import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ProjectDropHandler extends ApiBaseHandler implements HttpHandler {
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
        boolean deleted = DatabaseManager.deleteProjectStore(project);
        if (!deleted) {
            return buildError(
                    NanoHTTPD.Response.Status.CONFLICT,
                    "project_drop_failed",
                    "cannot delete project store: " + project);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("project", project);
        out.put("deleted", true);
        out.put("activeProject", DatabaseManager.activeProjectKey());
        return ok(out);
    }
}

