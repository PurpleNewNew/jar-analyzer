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
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.HashSet;
import java.util.Set;

final class PtaConstraintCheckerPlugin implements PtaPlugin {
    private static final Logger logger = LogManager.getLogger();
    private final Set<MethodReference.Handle> reachedMethods = new HashSet<>();
    private final Set<PtaContextMethod> reachedContextMethods = new HashSet<>();

    @Override
    public void onNewMethod(MethodReference.Handle method) {
        if (method != null) {
            reachedMethods.add(method);
        }
    }

    @Override
    public void onNewContextMethod(PtaContextMethod method) {
        if (method == null || method.getMethod() == null) {
            return;
        }
        if (!reachedMethods.contains(method.getMethod())) {
            logger.warn("pta-plugin-constraint: context method reached before method {}", method.id());
        }
        reachedContextMethods.add(method);
    }

    @Override
    public void onNewPointsToObject(PtaVarNode var, MethodReference.Handle ownerMethod) {
        if (ownerMethod != null && !reachedMethods.contains(ownerMethod)) {
            logger.warn("pta-plugin-constraint: points-to reached before method {} var={}",
                    ownerMethod.getName(), var == null ? "?" : var.getLocalId());
        }
        if (var == null || var.getOwner() == null || !reachedContextMethods.contains(var.getOwner())) {
            logger.warn("pta-plugin-constraint: points-to reached before context method {}",
                    var == null || var.getOwner() == null ? "?" : var.getOwner().id());
        }
    }
}
