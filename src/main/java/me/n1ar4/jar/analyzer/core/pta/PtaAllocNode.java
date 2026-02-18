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

import me.n1ar4.jar.analyzer.core.reference.ClassReference;

import java.util.Objects;

final class PtaAllocNode {
    private final ClassReference.Handle type;
    private final String allocSite;

    PtaAllocNode(ClassReference.Handle type, String allocSite) {
        this.type = type;
        this.allocSite = allocSite == null ? "" : allocSite;
    }

    ClassReference.Handle getType() {
        return type;
    }

    String getAllocSite() {
        return allocSite;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PtaAllocNode that = (PtaAllocNode) o;
        return Objects.equals(type, that.type) && Objects.equals(allocSite, that.allocSite);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, allocSite);
    }
}
