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

import me.n1ar4.jar.analyzer.core.build.BuildContext;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;

interface PtaPluginBridge {
    BuildContext getBuildContext();

    PtaSolverConfig getConfig();

    void addSemanticEdge(PtaContextMethod callerContext,
                         MethodReference.Handle target,
                         String edgeType,
                         String confidence,
                         String reason,
                         int opcode,
                         ClassReference.Handle receiverType,
                         String callSiteToken);
}
