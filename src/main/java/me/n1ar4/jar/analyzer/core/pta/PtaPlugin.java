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

interface PtaPlugin {
    default void onStart() {
    }

    default void onFinish(ContextSensitivePtaEngine.Result result) {
    }

    default void onNewMethod(MethodReference.Handle method) {
    }

    default void onNewContextMethod(PtaContextMethod method) {
    }

    default void onNewPointsToObject(PtaVarNode var, MethodReference.Handle ownerMethod) {
    }

    default void onNewCallEdge(MethodReference.Handle caller,
                               MethodReference.Handle callee,
                               String edgeType,
                               String confidence,
                               int opcode) {
    }
}
