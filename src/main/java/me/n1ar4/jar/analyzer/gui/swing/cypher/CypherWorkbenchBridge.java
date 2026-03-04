/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.cypher;

import me.n1ar4.jar.analyzer.gui.swing.cypher.model.DeleteScriptRequest;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.ExplainRequest;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.ExplainResponse;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.QueryFrameRequest;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.QueryFrameResponse;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.SaveScriptRequest;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.ScriptItem;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.ScriptListResponse;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.UiContextResponse;

import java.util.Map;

public interface CypherWorkbenchBridge {
    QueryFrameResponse execute(QueryFrameRequest request);

    ExplainResponse explain(ExplainRequest request);

    Map<String, Object> capabilities();

    ScriptListResponse listScripts();

    ScriptItem saveScript(SaveScriptRequest request);

    void deleteScript(DeleteScriptRequest request);

    UiContextResponse uiContext();

    void requestFullscreen(boolean fullscreen);
}
