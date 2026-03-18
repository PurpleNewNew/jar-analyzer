package org.benf.cfr.reader.bytecode;

import java.util.ArrayList;
import java.util.List;

public final class StructureRecoveryTrace {
    private final List<PhaseTrace> phases = new ArrayList<PhaseTrace>();

    void recordSkippedPhase(String phase,
                            String inputRequirement,
                            StructureRecoverySnapshot before,
                            String reason) {
        PhaseTrace phaseTrace = new PhaseTrace(phase, inputRequirement, before, before);
        phaseTrace.recordInvariant("input-requirement", false, reason + " for " + inputRequirement + " with " + before);
        phaseTrace.markSkipped(reason);
        phases.add(phaseTrace);
    }

    PhaseTrace beginPhase(String phase,
                          String inputRequirement,
                          StructureRecoverySnapshot before) {
        PhaseTrace phaseTrace = new PhaseTrace(phase, inputRequirement, before, before);
        phases.add(phaseTrace);
        return phaseTrace;
    }

    public List<PhaseTrace> getPhases() {
        return List.copyOf(phases);
    }

    public static StructureRecoveryTrace empty() {
        return new StructureRecoveryTrace();
    }

    public static final class PhaseTrace {
        private final String phase;
        private final String inputRequirement;
        private final StructureRecoverySnapshot before;
        private final List<RoundTrace> rounds = new ArrayList<RoundTrace>();
        private final List<InvariantTrace> invariants = new ArrayList<InvariantTrace>();
        private StructureRecoverySnapshot after;
        private boolean skipped;
        private String skipReason;

        private PhaseTrace(String phase,
                           String inputRequirement,
                           StructureRecoverySnapshot before,
                           StructureRecoverySnapshot after) {
            this.phase = phase;
            this.inputRequirement = inputRequirement;
            this.before = before;
            this.after = after;
        }

        RoundTrace beginRound(int round, StructureRecoverySnapshot before) {
            RoundTrace roundTrace = new RoundTrace(round, before, before);
            rounds.add(roundTrace);
            return roundTrace;
        }

        void finish(StructureRecoverySnapshot after) {
            this.after = after;
        }

        void recordInvariant(String name, boolean passed, String detail) {
            invariants.add(new InvariantTrace(name, passed, detail));
        }

        void markSkipped(String reason) {
            this.skipped = true;
            this.skipReason = reason;
        }

        public String getPhase() {
            return phase;
        }

        public String getInputRequirement() {
            return inputRequirement;
        }

        public StructureRecoverySnapshot getBefore() {
            return before;
        }

        public StructureRecoverySnapshot getAfter() {
            return after;
        }

        public boolean isSkipped() {
            return skipped;
        }

        public String getSkipReason() {
            return skipReason;
        }

        public List<RoundTrace> getRounds() {
            return List.copyOf(rounds);
        }

        public List<InvariantTrace> getInvariants() {
            return List.copyOf(invariants);
        }
    }

    public static final class RoundTrace {
        private final int round;
        private final StructureRecoverySnapshot before;
        private final List<PassTrace> passes = new ArrayList<PassTrace>();
        private final List<InvariantTrace> invariants = new ArrayList<InvariantTrace>();
        private StructureRecoverySnapshot after;

        private RoundTrace(int round,
                           StructureRecoverySnapshot before,
                           StructureRecoverySnapshot after) {
            this.round = round;
            this.before = before;
            this.after = after;
        }

        void recordPass(StructuredPassEntry pass,
                        boolean enabled,
                        StructureRecoverySnapshot before,
                        StructureRecoverySnapshot after) {
            passes.add(new PassTrace(pass, enabled, before, after));
        }

        void finish(StructureRecoverySnapshot after) {
            this.after = after;
        }

        void recordInvariant(String name, boolean passed, String detail) {
            invariants.add(new InvariantTrace(name, passed, detail));
        }

        public int getRound() {
            return round;
        }

        public StructureRecoverySnapshot getBefore() {
            return before;
        }

        public StructureRecoverySnapshot getAfter() {
            return after;
        }

        public List<PassTrace> getPasses() {
            return List.copyOf(passes);
        }

        public List<InvariantTrace> getInvariants() {
            return List.copyOf(invariants);
        }
    }

    public static final class PassTrace {
        private final StructuredPassEntry pass;
        private final boolean enabled;
        private final StructureRecoverySnapshot before;
        private final StructureRecoverySnapshot after;

        private PassTrace(StructuredPassEntry pass,
                          boolean enabled,
                          StructureRecoverySnapshot before,
                          StructureRecoverySnapshot after) {
            this.pass = pass;
            this.enabled = enabled;
            this.before = before;
            this.after = after;
        }

        public StructuredPassEntry getPass() {
            return pass;
        }

        public boolean isEnabled() {
            return enabled;
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

        public boolean respectsStructureContract() {
            return pass.getDescriptor().allowsStructuralChange() || !hasStructuralDelta();
        }
    }

    public static final class InvariantTrace {
        private final String name;
        private final boolean passed;
        private final String detail;

        private InvariantTrace(String name, boolean passed, String detail) {
            this.name = name;
            this.passed = passed;
            this.detail = detail;
        }

        public String getName() {
            return name;
        }

        public boolean isPassed() {
            return passed;
        }

        public String getDetail() {
            return detail;
        }
    }
}
