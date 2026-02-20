/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.swing.panel;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import me.n1ar4.jar.analyzer.gui.swing.cypher.CypherWorkbenchService;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.DeleteScriptRequest;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.ExplainRequest;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.ProjectGraphRequest;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.QueryFrameRequest;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.SaveScriptRequest;
import me.n1ar4.jar.analyzer.gui.swing.jcef.JcefAssetLoader;
import me.n1ar4.jar.analyzer.gui.swing.jcef.JcefBridgeRouter;
import me.n1ar4.jar.analyzer.gui.swing.jcef.JcefFrontendRequestHandler;
import me.n1ar4.jar.analyzer.gui.swing.jcef.JcefRuntime;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.cef.CefSettings;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.network.CefRequest;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class CypherToolPanel extends JPanel {
    private static final Logger logger = LogManager.getLogger();
    private static final String CHANNEL_QUERY_EXECUTE = "ja.query.execute";
    private static final String CHANNEL_QUERY_EXPLAIN = "ja.query.explain";
    private static final String CHANNEL_QUERY_CAPABILITIES = "ja.query.capabilities";
    private static final String CHANNEL_GRAPH_PROJECT = "ja.graph.project";
    private static final String CHANNEL_SCRIPT_LIST = "ja.script.list";
    private static final String CHANNEL_SCRIPT_SAVE = "ja.script.save";
    private static final String CHANNEL_SCRIPT_DELETE = "ja.script.delete";
    private static final String CHANNEL_UI_CONTEXT = "ja.ui.context";
    private static final String CHANNEL_UI_FULLSCREEN = "ja.ui.fullscreen";

    private final CypherWorkbenchService service = new CypherWorkbenchService();

    private CefClient cefClient;
    private CefBrowser cefBrowser;
    private JcefBridgeRouter bridgeRouter;
    private boolean jcefReady;
    private boolean jcefInitAttempted;
    private boolean browserStartAttempted;
    private String entryUrl = "";
    private String language = "zh";
    private String theme = "default";
    private boolean fullscreen;

    public CypherToolPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder());
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (!jcefInitAttempted) {
            jcefInitAttempted = true;
            initPanel();
        }
    }

    public void applyLanguage() {
        pushUiContextToFrontend();
    }

    public void updateUiContext(String language, String theme) {
        this.language = normalizeLanguage(language);
        this.theme = normalizeTheme(theme);
        service.updateUiContext(this.language, this.theme);
        pushUiContextToFrontend();
    }

    public void setFullscreenAction(Consumer<Boolean> action) {
        service.setFullscreenConsumer(action);
    }

    public void setFullscreenState(boolean fullscreen) {
        this.fullscreen = fullscreen;
        pushFullscreenToFrontend();
    }

    private void initPanel() {
        if (!JcefRuntime.isAvailable()) {
            disableWithReason(JcefRuntime.failureReason());
            return;
        }
        try {
            CefApp app = JcefRuntime.requireApp();
            cefClient = app.createClient();
            bridgeRouter = new JcefBridgeRouter();
            registerBridgeChannels(bridgeRouter);
            cefClient.addMessageRouter(bridgeRouter.router());
            cefClient.addRequestHandler(new JcefFrontendRequestHandler());
            entryUrl = JcefAssetLoader.resolveEntryUrl();
            cefClient.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
                @Override
                public void onAfterCreated(CefBrowser browser) {
                    logger.info("cypher browser created: id={}", browser == null ? -1 : browser.getIdentifier());
                    if (browser == null) {
                        return;
                    }
                    try {
                        browser.loadURL(entryUrl);
                        logger.info("cypher workbench load from onAfterCreated: {}", entryUrl);
                    } catch (Exception ex) {
                        logger.error("cypher workbench load in onAfterCreated failed: {}", ex.toString());
                    }
                }

                @Override
                public void onAfterParentChanged(CefBrowser browser) {
                    if (browser != null) {
                        logger.info("cypher browser parent changed: id={}, url={}",
                                browser.getIdentifier(), safe(browser.getURL()));
                    }
                }

                @Override
                public void onBeforeClose(CefBrowser browser) {
                    if (browser != null) {
                        logger.info("cypher browser before close: id={}", browser.getIdentifier());
                    }
                }
            });
            cefClient.addDisplayHandler(new CefDisplayHandlerAdapter() {
                @Override
                public boolean onConsoleMessage(CefBrowser browser,
                                                CefSettings.LogSeverity level,
                                                String message,
                                                String source,
                                                int line) {
                    String normalized = safe(message);
                    if (!normalized.isBlank()) {
                        if (normalized.contains("__JA_WORKBENCH_READY__")) {
                            logger.info("cypher workbench ready: {}", normalized);
                        } else if (normalized.contains("__JA_WORKBENCH")) {
                            logger.error("cypher workbench bootstrap issue: {} ({}, line={})",
                                    normalized, safe(source), line);
                        } else {
                            logger.debug("cypher console [{}] {}:{} {}", String.valueOf(level), safe(source), line, normalized);
                        }
                    }
                    return false;
                }
            });
            cefClient.addLoadHandler(new CefLoadHandlerAdapter() {
                @Override
                public void onLoadStart(CefBrowser browser, CefFrame frame, CefRequest.TransitionType transitionType) {
                    if (frame != null && frame.isMain()) {
                        logger.info("cypher workbench load start: {}", safe(frame.getURL()));
                    }
                }

                @Override
                public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                    if (frame != null && frame.isMain()) {
                        logger.info("cypher workbench load end: status={}, url={}",
                                httpStatusCode, safe(frame.getURL()));
                        SwingUtilities.invokeLater(() -> {
                            pushUiContextToFrontend();
                            pushFullscreenToFrontend();
                            pushHostVisibilityToFrontend();
                            scheduleBootstrapProbe();
                        });
                    }
                }

                @Override
                public void onLoadError(CefBrowser browser,
                                        CefFrame frame,
                                        CefLoadHandler.ErrorCode errorCode,
                                        String errorText,
                                        String failedUrl) {
                    if (frame != null && frame.isMain()) {
                        logger.error("cypher workbench load error: code={}, text={}, url={}",
                                String.valueOf(errorCode), safe(errorText), safe(failedUrl));
                    } else {
                        logger.warn("cypher sub-resource load error: code={}, text={}, url={}",
                                String.valueOf(errorCode), safe(errorText), safe(failedUrl));
                    }
                }
            });
            showLoadingPlaceholder();
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentShown(ComponentEvent e) {
                    logger.info("cypher panel shown, ensure browser started");
                    ensureBrowserStarted();
                    if (cefBrowser != null) {
                        cefBrowser.setWindowVisibility(true);
                    }
                    pushHostVisibilityToFrontend();
                }

                @Override
                public void componentHidden(ComponentEvent e) {
                    if (cefBrowser != null) {
                        cefBrowser.setWindowVisibility(false);
                    }
                    pushHostVisibilityToFrontend();
                }
            });
            addHierarchyListener(e -> {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0L) {
                    return;
                }
                if (isShowing()) {
                    logger.info("cypher panel hierarchy showing=true, ensure browser started");
                    ensureBrowserStarted();
                }
            });
            jcefReady = true;
            SwingUtilities.invokeLater(this::ensureBrowserStarted);
        } catch (Throwable ex) {
            logger.error("init cypher jcef panel failed: {}", ex.toString());
            disableWithReason("JCEF init failed: " + safe(ex.getMessage()));
        }
    }

    private void ensureBrowserStarted() {
        if (!jcefReady || cefClient == null) {
            return;
        }
        if (cefBrowser == null) {
            try {
                browserStartAttempted = true;
                cefBrowser = cefClient.createBrowser(entryUrl, false, false);
                Component uiComponent = cefBrowser.getUIComponent();
                logger.info("cypher browser ui component: class={}",
                        uiComponent == null ? "null" : uiComponent.getClass().getName());
                if (uiComponent == null) {
                    throw new IllegalStateException("jcef browser ui component is null");
                }
                uiComponent.setVisible(true);
                uiComponent.setMinimumSize(new Dimension(200, 120));
                removeAll();
                add(uiComponent, BorderLayout.CENTER);
                revalidate();
                repaint();
                SwingUtilities.invokeLater(() -> {
                    try {
                        cefBrowser.createImmediately();
                        logger.info("cypher browser createImmediately invoked after ui attached");
                    } catch (Throwable ex) {
                        logger.warn("cypher browser createImmediately after attach failed: {}", ex.toString());
                    }
                });
            } catch (Throwable ex) {
                logger.error("cypher browser create failed: {}", ex.toString());
                disableWithReason("JCEF browser create failed: " + safe(ex.getMessage()));
                return;
            }
        }
        SwingUtilities.invokeLater(this::forceLoadEntryUrl);
        startWatchdog();
    }

    private void forceLoadEntryUrl() {
        if (cefBrowser == null || entryUrl.isBlank()) {
            return;
        }
        try {
            cefBrowser.loadURL(entryUrl);
            logger.info("cypher workbench force load url: {}", entryUrl);
        } catch (Exception ex) {
            logger.error("cypher workbench load url failed: {}", ex.toString());
        }
    }

    private void startWatchdog() {
        Timer watchdog = new Timer(2500, e -> {
            if (cefBrowser == null) {
                return;
            }
            String currentUrl = safe(cefBrowser.getURL());
            boolean hasDoc = false;
            try {
                hasDoc = cefBrowser.hasDocument();
            } catch (Throwable ignored) {
            }
            logger.info("cypher workbench watchdog: hasDocument={}, url={}", hasDoc, currentUrl);
            if (!hasDoc || currentUrl.isBlank() || "about:blank".equalsIgnoreCase(currentUrl)) {
                if (!browserStartAttempted && isShowing()) {
                    ensureBrowserStarted();
                    return;
                }
                forceLoadEntryUrl();
                logger.warn("cypher workbench watchdog reload: {}", entryUrl);
            }
        });
        watchdog.setRepeats(false);
        watchdog.start();
    }

    private void showLoadingPlaceholder() {
        removeAll();
        JLabel loading = new JLabel("Graph Console loading...", SwingConstants.CENTER);
        loading.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(loading, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void registerBridgeChannels(JcefBridgeRouter router) {
        router.register(CHANNEL_QUERY_EXECUTE, payload -> service.execute(parseExecuteRequest(payload)));
        router.register(CHANNEL_QUERY_EXPLAIN, payload -> service.explain(parseExplainRequest(payload)));
        router.register(CHANNEL_QUERY_CAPABILITIES, payload -> service.capabilities());
        router.register(CHANNEL_GRAPH_PROJECT, payload -> service.projectGraph(parseProjectRequest(payload)));
        router.register(CHANNEL_SCRIPT_LIST, payload -> service.listScripts());
        router.register(CHANNEL_SCRIPT_SAVE, payload -> service.saveScript(parseSaveScriptRequest(payload)));
        router.register(CHANNEL_SCRIPT_DELETE, payload -> {
            service.deleteScript(parseDeleteScriptRequest(payload));
            return Map.of("ok", true);
        });
        router.register(CHANNEL_UI_CONTEXT, payload -> service.uiContext());
        router.register(CHANNEL_UI_FULLSCREEN, payload -> {
            boolean value = parseBoolean(payload, "fullscreen", false);
            SwingUtilities.invokeLater(() -> service.requestFullscreen(value));
            return Map.of("ok", true, "fullscreen", value);
        });
    }

    private QueryFrameRequest parseExecuteRequest(JSONObject payload) {
        String query = safe(payload == null ? null : payload.getString("query"));
        Map<String, Object> params = asMap(payload == null ? null : payload.getJSONObject("params"));
        Map<String, Object> options = asMap(payload == null ? null : payload.getJSONObject("options"));
        return new QueryFrameRequest(query, params, options);
    }

    private ExplainRequest parseExplainRequest(JSONObject payload) {
        String query = safe(payload == null ? null : payload.getString("query"));
        return new ExplainRequest(query);
    }

    private ProjectGraphRequest parseProjectRequest(JSONObject payload) {
        if (payload == null) {
            return new ProjectGraphRequest("", List.of(), List.of(), List.of(), false);
        }
        String query = safe(payload.getString("query"));
        List<String> columns = toStringList(payload.getJSONArray("columns"));
        List<List<Object>> rows = toRows(payload.getJSONArray("rows"));
        List<String> warnings = toStringList(payload.getJSONArray("warnings"));
        boolean truncated = parseBoolean(payload, "truncated", false);
        return new ProjectGraphRequest(query, columns, rows, warnings, truncated);
    }

    private SaveScriptRequest parseSaveScriptRequest(JSONObject payload) {
        if (payload == null) {
            return new SaveScriptRequest(null, "", "", "", false);
        }
        Long scriptId = payload.containsKey("scriptId") ? payload.getLong("scriptId") : null;
        String title = safe(payload.getString("title"));
        String body = safe(payload.getString("body"));
        String tags = safe(payload.getString("tags"));
        boolean pinned = parseBoolean(payload, "pinned", false);
        return new SaveScriptRequest(scriptId, title, body, tags, pinned);
    }

    private DeleteScriptRequest parseDeleteScriptRequest(JSONObject payload) {
        if (payload == null) {
            return new DeleteScriptRequest(0L);
        }
        Long scriptId = payload.getLong("scriptId");
        return new DeleteScriptRequest(scriptId == null ? 0L : scriptId);
    }

    private static Map<String, Object> asMap(JSONObject object) {
        if (object == null || object.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<String, Object> entry : object.entrySet()) {
            out.put(entry.getKey(), entry.getValue());
        }
        return out;
    }

    private static List<String> toStringList(JSONArray array) {
        if (array == null || array.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>(array.size());
        for (Object item : array) {
            out.add(item == null ? "" : String.valueOf(item));
        }
        return out;
    }

    private static List<List<Object>> toRows(JSONArray array) {
        if (array == null || array.isEmpty()) {
            return List.of();
        }
        List<List<Object>> out = new ArrayList<>(array.size());
        for (Object row : array) {
            if (row instanceof JSONArray arr) {
                out.add(new ArrayList<>(arr));
                continue;
            }
            if (row instanceof List<?> list) {
                out.add(new ArrayList<>(list));
                continue;
            }
            out.add(List.of(row));
        }
        return out;
    }

    private void disableWithReason(String reason) {
        removeAll();
        JLabel label = new JLabel("Graph Console disabled: " + safe(reason), SwingConstants.CENTER);
        label.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(label, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void pushUiContextToFrontend() {
        if (!jcefReady || cefBrowser == null) {
            return;
        }
        service.updateUiContext(language, theme);
        String payload = JSON.toJSONString(service.uiContext());
        evalJs("window.JA_WORKBENCH&&window.JA_WORKBENCH.updateUiContext(" + payload + ");");
    }

    private void pushFullscreenToFrontend() {
        if (!jcefReady || cefBrowser == null) {
            return;
        }
        evalJs("window.JA_WORKBENCH&&window.JA_WORKBENCH.setFullscreen(" + fullscreen + ");");
    }

    private void pushHostVisibilityToFrontend() {
        if (!jcefReady || cefBrowser == null) {
            return;
        }
        evalJs("window.JA_WORKBENCH&&window.JA_WORKBENCH.onHostVisibility(" + isShowing() + ");");
    }

    private void evalJs(String script) {
        if (script == null || script.isBlank() || cefBrowser == null) {
            return;
        }
        try {
            cefBrowser.executeJavaScript(script, cefBrowser.getURL(), 0);
        } catch (Exception ex) {
            logger.debug("execute cypher jcef js fail: {}", ex.toString());
        }
    }

    private void scheduleBootstrapProbe() {
        evalJs("""
                setTimeout(function () {
                  try {
                    var app = document.getElementById('app');
                    var children = app ? app.children.length : -1;
                    var marker = (window.__JA_BOOT_ERROR || '');
                    if (!app || children <= 0) {
                      console.error('__JA_WORKBENCH_EMPTY__ children=' + children + ' ready=' + document.readyState + ' err=' + marker);
                    } else {
                      console.info('__JA_WORKBENCH_READY__ children=' + children);
                    }
                  } catch (e) {
                    console.error('__JA_WORKBENCH_PROBE_ERROR__ ' + String(e));
                  }
                }, 700);
                """);
    }

    private static boolean parseBoolean(JSONObject obj, String key, boolean def) {
        if (obj == null || key == null) {
            return def;
        }
        Object value = obj.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        if (value == null) {
            return def;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return def;
        }
        if ("1".equals(text)) {
            return true;
        }
        if ("0".equals(text)) {
            return false;
        }
        return Boolean.parseBoolean(text);
    }

    private static String normalizeLanguage(String value) {
        return "en".equalsIgnoreCase(safe(value)) ? "en" : "zh";
    }

    private static String normalizeTheme(String value) {
        return Objects.equals("dark", safe(value).toLowerCase()) ? "dark" : "default";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
