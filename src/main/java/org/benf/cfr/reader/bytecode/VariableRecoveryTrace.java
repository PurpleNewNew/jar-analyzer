package org.benf.cfr.reader.bytecode;

import java.util.ArrayList;
import java.util.List;

public final class VariableRecoveryTrace {
    private final List<PassTrace> passes = new ArrayList<PassTrace>();

    void record(StructuredPassEntry pass,
                StructureRecoverySnapshot before,
                StructureRecoverySnapshot after) {
        passes.add(new PassTrace(pass, before, after));
    }

    public List<PassTrace> getPasses() {
        return List.copyOf(passes);
    }

    public static VariableRecoveryTrace empty() {
        return new VariableRecoveryTrace();
    }

    public static final class PassTrace {
        private final StructuredPassEntry pass;
        private final StructureRecoverySnapshot before;
        private final StructureRecoverySnapshot after;

        private PassTrace(StructuredPassEntry pass,
                          StructureRecoverySnapshot before,
                          StructureRecoverySnapshot after) {
            this.pass = pass;
            this.before = before;
            this.after = after;
        }

        public StructuredPassEntry getPass() {
            return pass;
        }

        public StructureRecoverySnapshot getBefore() {
            return before;
        }

        public StructureRecoverySnapshot getAfter() {
            return after;
        }

        public boolean isChanged() {
            return !before.equals(after);
        }

        public boolean hasStructuralDelta() {
            return before.hasStructuralDelta(after);
        }
    }
}
