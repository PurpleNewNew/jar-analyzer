/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.cypher;

import me.n1ar4.jar.analyzer.gui.swing.cypher.model.*;

public interface CypherWorkbenchBridge {
    QueryFrameResponse execute(QueryFrameRequest request);

    ExplainResponse explain(ExplainRequest request);

    CapabilitiesResponse capabilities();

    GraphFramePayload projectGraph(ProjectGraphRequest request);

    ScriptListResponse listScripts();

    ScriptItem saveScript(SaveScriptRequest request);

    void deleteScript(DeleteScriptRequest request);

    UiContextResponse uiContext();

    void requestFullscreen(boolean fullscreen);
}
