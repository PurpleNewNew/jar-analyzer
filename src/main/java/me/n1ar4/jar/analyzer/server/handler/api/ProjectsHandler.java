/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.server.handler.api;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectRegistryEntry;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectRegistryService;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectRegistrySnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ProjectsHandler extends ApiBaseHandler implements HttpHandler {
    private static final String ROOT = "/api/projects";
    private static final String ACTIVE = "/api/projects/active";
    private static final String REGISTER = "/api/projects/register";
    private static final String SWITCH = "/api/projects/switch";

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        String uri = session == null ? "" : session.getUri();
        if (uri == null || uri.isBlank()) {
            return buildError(NanoHTTPD.Response.Status.BAD_REQUEST, "invalid_request", "uri required");
        }
        if (ROOT.equals(uri)) {
            if (session.getMethod() != NanoHTTPD.Method.GET) {
                return buildError(NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED, "method_not_allowed", "GET required");
            }
            return listProjects();
        }
        if (ACTIVE.equals(uri)) {
            if (session.getMethod() != NanoHTTPD.Method.GET) {
                return buildError(NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED, "method_not_allowed", "GET required");
            }
            return activeProject();
        }
        if (REGISTER.equals(uri)) {
            if (session.getMethod() != NanoHTTPD.Method.POST) {
                return buildError(NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED, "method_not_allowed", "POST required");
            }
            return registerProject(session);
        }
        if (SWITCH.equals(uri)) {
            if (session.getMethod() != NanoHTTPD.Method.POST) {
                return buildError(NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED, "method_not_allowed", "POST required");
            }
            return switchProject(session);
        }
        if (uri.startsWith(ROOT + "/")) {
            if (session.getMethod() != NanoHTTPD.Method.DELETE) {
                return buildError(NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED, "method_not_allowed", "DELETE required");
            }
            String key = uri.substring((ROOT + "/").length()).trim();
            if (key.isBlank()) {
                return needParam("projectKey");
            }
            return removeProject(session, key);
        }
        return buildError(NanoHTTPD.Response.Status.NOT_FOUND, "project_api_not_found", "project api not found");
    }

    private NanoHTTPD.Response listProjects() {
        ProjectRegistrySnapshot snapshot = ProjectRegistryService.getInstance().snapshot();
        Map<String, Object> out = new HashMap<>();
        out.put("projects", snapshot.projects());
        out.put("activeProjectKey", snapshot.activeProjectKey());
        out.put("activeProjectAlias", snapshot.activeProjectAlias());
        return ok(out);
    }

    private NanoHTTPD.Response activeProject() {
        ProjectRegistryEntry active = ProjectRegistryService.getInstance().active();
        return ok(active);
    }

    private NanoHTTPD.Response registerProject(NanoHTTPD.IHTTPSession session) {
        JSONObject body = parseBodyObject(session);
        String alias = safe(body.getString("alias"));
        String inputPath = safe(body.getString("inputPath"));
        String runtimePath = safe(body.getString("runtimePath"));
        boolean resolveNested = body.getBooleanValue("resolveNestedJars");
        if (inputPath.isBlank()) {
            return needParam("inputPath");
        }
        try {
            ProjectRegistryEntry entry = ProjectRegistryService.getInstance()
                    .register(alias, inputPath, runtimePath, resolveNested);
            return ok(entry);
        } catch (IllegalArgumentException ex) {
            return buildError(NanoHTTPD.Response.Status.BAD_REQUEST, "project_register_invalid", safe(ex.getMessage()));
        } catch (Exception ex) {
            return buildError(NanoHTTPD.Response.Status.INTERNAL_ERROR, "project_register_error", safe(ex.getMessage()));
        }
    }

    private NanoHTTPD.Response switchProject(NanoHTTPD.IHTTPSession session) {
        JSONObject body = parseBodyObject(session);
        String projectKey = safe(body.getString("projectKey"));
        if (projectKey.isBlank()) {
            return needParam("projectKey");
        }
        try {
            ProjectRegistryEntry entry = ProjectRegistryService.getInstance().switchActive(projectKey);
            return ok(entry);
        } catch (IllegalArgumentException ex) {
            return buildError(NanoHTTPD.Response.Status.NOT_FOUND, "project_not_found", safe(ex.getMessage()));
        } catch (Exception ex) {
            return buildError(NanoHTTPD.Response.Status.INTERNAL_ERROR, "project_switch_error", safe(ex.getMessage()));
        }
    }

    private NanoHTTPD.Response removeProject(NanoHTTPD.IHTTPSession session, String key) {
        boolean deleteStore = getBoolParam(session, "deleteStore");
        boolean removed = ProjectRegistryService.getInstance().remove(key, deleteStore);
        if (!removed) {
            return buildError(NanoHTTPD.Response.Status.NOT_FOUND, "project_not_found", "project not found");
        }
        Map<String, Object> out = new HashMap<>();
        out.put("projectKey", key);
        out.put("deleteStore", deleteStore);
        out.put("removed", true);
        return ok(out);
    }

    private static JSONObject parseBodyObject(NanoHTTPD.IHTTPSession session) {
        if (session == null) {
            return new JSONObject();
        }
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String raw = files.get("postData");
            if (raw == null || raw.isBlank()) {
                return new JSONObject();
            }
            JSONObject obj = JSON.parseObject(raw);
            return obj == null ? new JSONObject() : obj;
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
