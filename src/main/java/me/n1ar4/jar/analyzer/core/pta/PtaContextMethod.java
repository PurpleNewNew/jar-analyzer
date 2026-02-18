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

import java.util.Objects;

final class PtaContextMethod {
    private final MethodReference.Handle method;
    private final PtaContext context;

    PtaContextMethod(MethodReference.Handle method, PtaContext context) {
        this.method = method;
        this.context = context == null ? PtaContext.root() : context;
    }

    MethodReference.Handle getMethod() {
        return method;
    }

    PtaContext getContext() {
        return context;
    }

    String id() {
        if (method == null || method.getClassReference() == null) {
            return "unknown@" + context.render();
        }
        return method.getClassReference().getName() + "." + method.getName()
                + method.getDesc() + "@" + context.render();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PtaContextMethod that = (PtaContextMethod) o;
        return Objects.equals(method, that.method) && Objects.equals(context, that.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, context);
    }
}
