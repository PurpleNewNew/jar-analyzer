/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.taie;

import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import pascal.taie.World;
import pascal.taie.analysis.pta.core.solver.DeclaredParamProvider;
import pascal.taie.analysis.pta.core.solver.EntryPoint;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Adds explicit framework entry methods discovered by bytecode scanners.
 */
public final class TaieExtraEntryPointPlugin implements Plugin {
    private static final Logger logger = LogManager.getLogger();
    private static final ThreadLocal<List<String>> ENTRY_SIGNATURES =
            ThreadLocal.withInitial(List::of);
    private Solver solver;

    public static void install(List<String> signatures) {
        if (signatures == null || signatures.isEmpty()) {
            ENTRY_SIGNATURES.set(List.of());
            return;
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String raw : signatures) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String signature = raw.trim();
            if (!signature.isEmpty()) {
                normalized.add(signature);
            }
        }
        ENTRY_SIGNATURES.set(normalized.isEmpty() ? List.of() : List.copyOf(normalized));
    }

    public static void clear() {
        ENTRY_SIGNATURES.remove();
    }

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
    }

    @Override
    public void onStart() {
        Solver localSolver = solver;
        if (localSolver == null) {
            return;
        }
        List<String> entries = snapshotEntries();
        if (entries.isEmpty()) {
            return;
        }

        ClassHierarchy hierarchy = World.get().getClassHierarchy();
        int resolved = 0;
        int ignored = 0;
        for (String signature : entries) {
            try {
                JMethod method = hierarchy.getMethod(signature);
                if (method == null || method.isAbstract() || method.isNative()) {
                    ignored++;
                    continue;
                }
                if (method.getDeclaringClass() != null && method.getDeclaringClass().isPhantom()) {
                    ignored++;
                    continue;
                }
                localSolver.addEntryPoint(new EntryPoint(
                        method,
                        new DeclaredParamProvider(method, localSolver.getHeapModel(), 1)
                ));
                resolved++;
            } catch (Exception ex) {
                ignored++;
                logger.debug("resolve extra entry failed: {} ({})", signature, ex.toString());
            }
        }
        logger.info("tai-e explicit entry plugin: configured={} resolved={} ignored={}",
                entries.size(), resolved, ignored);
    }

    private static List<String> snapshotEntries() {
        List<String> entries = ENTRY_SIGNATURES.get();
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(entries);
    }
}
