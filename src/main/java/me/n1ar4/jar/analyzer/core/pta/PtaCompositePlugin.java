/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.pta;

import me.n1ar4.jar.analyzer.core.reference.MethodReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class PtaCompositePlugin implements PtaPlugin {
    private static final PtaCompositePlugin EMPTY = new PtaCompositePlugin(Collections.emptyList());
    private final List<PtaPlugin> plugins;

    private PtaCompositePlugin(List<PtaPlugin> plugins) {
        this.plugins = plugins;
    }

    static PtaCompositePlugin empty() {
        return EMPTY;
    }

    static PtaCompositePlugin of(List<PtaPlugin> source) {
        if (source == null || source.isEmpty()) {
            return empty();
        }
        ArrayList<PtaPlugin> out = new ArrayList<>(source.size());
        for (PtaPlugin plugin : source) {
            if (plugin != null) {
                out.add(plugin);
            }
        }
        if (out.isEmpty()) {
            return empty();
        }
        return new PtaCompositePlugin(Collections.unmodifiableList(out));
    }

    @Override
    public void setBridge(PtaPluginBridge bridge) {
        for (PtaPlugin plugin : plugins) {
            plugin.setBridge(bridge);
        }
    }

    @Override
    public void onStart() {
        for (PtaPlugin plugin : plugins) {
            plugin.onStart();
        }
    }

    @Override
    public void onFinish(ContextSensitivePtaEngine.Result result) {
        for (PtaPlugin plugin : plugins) {
            plugin.onFinish(result);
        }
    }

    @Override
    public void onNewMethod(MethodReference.Handle method) {
        for (PtaPlugin plugin : plugins) {
            plugin.onNewMethod(method);
        }
    }

    @Override
    public void onNewContextMethod(PtaContextMethod method) {
        for (PtaPlugin plugin : plugins) {
            plugin.onNewContextMethod(method);
        }
    }

    @Override
    public void onNewPointsToObject(PtaVarNode var, MethodReference.Handle ownerMethod) {
        for (PtaPlugin plugin : plugins) {
            plugin.onNewPointsToObject(var, ownerMethod);
        }
    }

    @Override
    public void onNewCallEdge(PtaContextMethod callerContext,
                              MethodReference.Handle caller,
                              MethodReference.Handle callee,
                              String edgeType,
                              String confidence,
                              int opcode) {
        for (PtaPlugin plugin : plugins) {
            plugin.onNewCallEdge(callerContext, caller, callee, edgeType, confidence, opcode);
        }
    }
}
