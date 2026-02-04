/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.taint.summary;

import java.util.Set;

public interface SummaryCollector {
    void onReturnTaint(int seedParam, Set<String> markers);

    void onFieldTaint(int seedParam, String owner, String name, String desc, boolean isStatic, Set<String> markers);

    void onCallTaint(int seedParam,
                     me.n1ar4.jar.analyzer.core.reference.MethodReference.Handle callee,
                     FlowPort to,
                     Set<String> markers);

    void onLowConfidence(int seedParam, String reason);
}
