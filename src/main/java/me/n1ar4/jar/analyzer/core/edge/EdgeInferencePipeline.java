/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.edge;

import me.n1ar4.jar.analyzer.core.build.BuildContext;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Best-effort semantic edge inference.
 * <p>
 * This is intentionally conservative and stays within ASM + SQLite.
 */
public final class EdgeInferencePipeline {
    private static final Logger logger = LogManager.getLogger();
    private static final String PROP_ENABLE = "jar.analyzer.edge.infer.enable";

    private EdgeInferencePipeline() {
    }

    public static int infer(BuildContext ctx) {
        if (!isEnabled()) {
            return 0;
        }
        if (ctx == null || ctx.methodCalls == null || ctx.methodCalls.isEmpty()) {
            return 0;
        }
        List<EdgeInferRule> rules = new ArrayList<>();
        rules.add(new DoPrivilegedEdgeRule());
        rules.add(new ThreadStartEdgeRule());
        rules.add(new ExecutorCallbackEdgeRule());
        rules.add(new CompletableFutureEdgeRule());
        rules.add(new DynamicProxyEdgeRule());
        rules.add(new ReflectionLogEdgeRule());

        int added = 0;
        for (EdgeInferRule rule : rules) {
            if (rule == null) {
                continue;
            }
            try {
                int a = rule.apply(ctx);
                if (a > 0) {
                    added += a;
                    logger.info("edge infer rule {} added {}", rule.id(), a);
                }
            } catch (Exception ex) {
                logger.debug("edge infer rule {} failed: {}", rule.id(), ex.toString());
            }
        }
        return added;
    }

    private static boolean isEnabled() {
        String raw = System.getProperty(PROP_ENABLE);
        if (raw == null || raw.isBlank()) {
            return true;
        }
        return !"false".equalsIgnoreCase(raw.strip());
    }
}
