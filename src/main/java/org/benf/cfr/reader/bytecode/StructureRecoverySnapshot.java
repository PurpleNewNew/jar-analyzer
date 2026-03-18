package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractUnStructuredStatement;

import java.util.List;
import java.util.Objects;

public final class StructureRecoverySnapshot {
    private final boolean fullyStructured;
    private final int totalStatements;
    private final int unstructuredStatements;
    private final int nopStatements;
    private final int contentHash;

    private StructureRecoverySnapshot(boolean fullyStructured,
                                      int totalStatements,
                                      int unstructuredStatements,
                                      int nopStatements,
                                      int contentHash) {
        this.fullyStructured = fullyStructured;
        this.totalStatements = totalStatements;
        this.unstructuredStatements = unstructuredStatements;
        this.nopStatements = nopStatements;
        this.contentHash = contentHash;
    }

    static StructureRecoverySnapshot capture(Op04StructuredStatement block) {
        List<StructuredStatement> statements = MiscStatementTools.linearise(block);
        if (statements == null) {
            return new StructureRecoverySnapshot(block.isFullyStructured(), -1, -1, -1, block.toString().hashCode());
        }
        int unstructured = 0;
        int nops = 0;
        int contentHash = 1;
        for (StructuredStatement statement : statements) {
            if (statement instanceof AbstractUnStructuredStatement) {
                ++unstructured;
            }
            boolean effectivelyNop;
            try {
                effectivelyNop = statement.isEffectivelyNOP();
            } catch (UnsupportedOperationException ignored) {
                effectivelyNop = false;
            }
            if (effectivelyNop) {
                ++nops;
            }
            contentHash = 31 * contentHash + statement.getClass().getName().hashCode();
            contentHash = 31 * contentHash + (effectivelyNop ? 1 : 0);
            contentHash = 31 * contentHash + statement.toString().hashCode();
        }
        return new StructureRecoverySnapshot(block.isFullyStructured(), statements.size(), unstructured, nops, contentHash);
    }

    public boolean isFullyStructured() {
        return fullyStructured;
    }

    public int getTotalStatements() {
        return totalStatements;
    }

    public int getUnstructuredStatements() {
        return unstructuredStatements;
    }

    public int getNopStatements() {
        return nopStatements;
    }

    public int getContentHash() {
        return contentHash;
    }

    public boolean hasStructuralDelta(StructureRecoverySnapshot other) {
        return fullyStructured != other.fullyStructured
                || totalStatements != other.totalStatements
                || unstructuredStatements != other.unstructuredStatements
                || nopStatements != other.nopStatements;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StructureRecoverySnapshot)) {
            return false;
        }
        StructureRecoverySnapshot other = (StructureRecoverySnapshot) o;
        return fullyStructured == other.fullyStructured
                && totalStatements == other.totalStatements
                && unstructuredStatements == other.unstructuredStatements
                && nopStatements == other.nopStatements
                && contentHash == other.contentHash;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullyStructured, totalStatements, unstructuredStatements, nopStatements, contentHash);
    }

    @Override
    public String toString() {
        return "StructureRecoverySnapshot{" +
                "fullyStructured=" + fullyStructured +
                ", totalStatements=" + totalStatements +
                ", unstructuredStatements=" + unstructuredStatements +
                ", nopStatements=" + nopStatements +
                ", contentHash=" + contentHash +
                '}';
    }
}
