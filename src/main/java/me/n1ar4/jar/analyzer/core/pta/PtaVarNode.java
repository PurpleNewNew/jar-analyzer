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

import java.util.Objects;

final class PtaVarNode {
    private final PtaContextMethod owner;
    private final String localId;

    PtaVarNode(PtaContextMethod owner, String localId) {
        this.owner = owner;
        this.localId = localId == null ? "" : localId;
    }

    PtaContextMethod getOwner() {
        return owner;
    }

    String getLocalId() {
        return localId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PtaVarNode ptaVarNode = (PtaVarNode) o;
        return Objects.equals(owner, ptaVarNode.owner)
                && Objects.equals(localId, ptaVarNode.localId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, localId);
    }
}
