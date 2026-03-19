package org.benf.cfr.reader.bytecode;

import java.util.ArrayList;
import java.util.List;

public final class MethodDecompileRecord {
    private final List<StageRecord> stages = new ArrayList<StageRecord>();

    void recordStage(String stage,
                     String inputRequirement,
                     StructureRecoverySnapshot before,
                     StructureRecoverySnapshot after,
                     int beforeStructurePhaseCount,
                     int afterStructurePhaseCount,
                     int beforeVariablePassCount,
                     int afterVariablePassCount,
                     int beforeTypePassCount,
                     int afterTypePassCount,
                     StructureRecoveryTrace structureRecoveryTrace,
                     VariableRecoveryTrace variableRecoveryTrace,
                     TypeRecoveryTrace typeRecoveryTrace) {
        stages.add(new StageRecord(
                stage,
                inputRequirement,
                before,
                after,
                beforeStructurePhaseCount,
                afterStructurePhaseCount,
                beforeVariablePassCount,
                afterVariablePassCount,
                beforeTypePassCount,
                afterTypePassCount,
                StageArtifacts.capture(
                        beforeStructurePhaseCount,
                        afterStructurePhaseCount,
                        beforeVariablePassCount,
                        afterVariablePassCount,
                        beforeTypePassCount,
                        afterTypePassCount,
                        structureRecoveryTrace,
                        variableRecoveryTrace,
                        typeRecoveryTrace
                ),
                false,
                null
        ));
    }

    void recordSkippedStage(String stage,
                            String inputRequirement,
                            StructureRecoverySnapshot before,
                            int beforeStructurePhaseCount,
                            int beforeVariablePassCount,
                            int beforeTypePassCount,
                            String reason) {
        stages.add(new StageRecord(
                stage,
                inputRequirement,
                before,
                before,
                beforeStructurePhaseCount,
                beforeStructurePhaseCount,
                beforeVariablePassCount,
                beforeVariablePassCount,
                beforeTypePassCount,
                beforeTypePassCount,
                StageArtifacts.empty(),
                true,
                reason
        ));
    }

    public List<StageRecord> getStages() {
        return List.copyOf(stages);
    }

    public static MethodDecompileRecord empty() {
        return new MethodDecompileRecord();
    }

    public static final class StageRecord {
        private final String stage;
        private final String inputRequirement;
        private final StructureRecoverySnapshot before;
        private final StructureRecoverySnapshot after;
        private final int beforeStructurePhaseCount;
        private final int afterStructurePhaseCount;
        private final int beforeVariablePassCount;
        private final int afterVariablePassCount;
        private final int beforeTypePassCount;
        private final int afterTypePassCount;
        private final StageArtifacts artifacts;
        private final boolean skipped;
        private final String skipReason;

        private StageRecord(String stage,
                            String inputRequirement,
                            StructureRecoverySnapshot before,
                            StructureRecoverySnapshot after,
                            int beforeStructurePhaseCount,
                            int afterStructurePhaseCount,
                            int beforeVariablePassCount,
                            int afterVariablePassCount,
                            int beforeTypePassCount,
                            int afterTypePassCount,
                            StageArtifacts artifacts,
                            boolean skipped,
                            String skipReason) {
            this.stage = stage;
            this.inputRequirement = inputRequirement;
            this.before = before;
            this.after = after;
            this.beforeStructurePhaseCount = beforeStructurePhaseCount;
            this.afterStructurePhaseCount = afterStructurePhaseCount;
            this.beforeVariablePassCount = beforeVariablePassCount;
            this.afterVariablePassCount = afterVariablePassCount;
            this.beforeTypePassCount = beforeTypePassCount;
            this.afterTypePassCount = afterTypePassCount;
            this.artifacts = artifacts;
            this.skipped = skipped;
            this.skipReason = skipReason;
        }

        public String getStage() {
            return stage;
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

        public int getBeforeStructurePhaseCount() {
            return beforeStructurePhaseCount;
        }

        public int getAfterStructurePhaseCount() {
            return afterStructurePhaseCount;
        }

        public int getBeforeVariablePassCount() {
            return beforeVariablePassCount;
        }

        public int getAfterVariablePassCount() {
            return afterVariablePassCount;
        }

        public int getBeforeTypePassCount() {
            return beforeTypePassCount;
        }

        public int getAfterTypePassCount() {
            return afterTypePassCount;
        }

        public boolean isSkipped() {
            return skipped;
        }

        public String getSkipReason() {
            return skipReason;
        }

        public boolean hasStructuralDelta() {
            return before != null && after != null && before.hasStructuralDelta(after);
        }

        public boolean hasValidationFailures() {
            return artifacts.hasFailedStructureInvariants();
        }

        public String getValidationSummary() {
            return artifacts.describe(stage, inputRequirement, skipped, skipReason);
        }

        public StageArtifacts getArtifacts() {
            return artifacts;
        }
    }

    public static final class StageArtifacts {
        private final List<String> structurePhaseNames;
        private final List<String> failedStructureInvariants;
        private final List<String> variablePassNames;
        private final int variablePassesWithStructuralDelta;
        private final List<String> typePassNames;
        private final int changedTypePassCount;

        private StageArtifacts(List<String> structurePhaseNames,
                               List<String> failedStructureInvariants,
                               List<String> variablePassNames,
                               int variablePassesWithStructuralDelta,
                               List<String> typePassNames,
                               int changedTypePassCount) {
            this.structurePhaseNames = List.copyOf(structurePhaseNames);
            this.failedStructureInvariants = List.copyOf(failedStructureInvariants);
            this.variablePassNames = List.copyOf(variablePassNames);
            this.variablePassesWithStructuralDelta = variablePassesWithStructuralDelta;
            this.typePassNames = List.copyOf(typePassNames);
            this.changedTypePassCount = changedTypePassCount;
        }

        private static StageArtifacts capture(int beforeStructurePhaseCount,
                                              int afterStructurePhaseCount,
                                              int beforeVariablePassCount,
                                              int afterVariablePassCount,
                                              int beforeTypePassCount,
                                              int afterTypePassCount,
                                              StructureRecoveryTrace structureRecoveryTrace,
                                              VariableRecoveryTrace variableRecoveryTrace,
                                              TypeRecoveryTrace typeRecoveryTrace) {
            List<String> structurePhases = new ArrayList<String>();
            List<String> failedInvariants = new ArrayList<String>();
            for (StructureRecoveryTrace.PhaseTrace phase : structureRecoveryTrace.getPhases()
                    .subList(beforeStructurePhaseCount, afterStructurePhaseCount)) {
                structurePhases.add(phase.getPhase());
                for (StructureRecoveryTrace.InvariantTrace invariant : phase.getInvariants()) {
                    if (!invariant.isPassed()) {
                        failedInvariants.add(phase.getPhase() + ":" + invariant.getName());
                    }
                }
                for (StructureRecoveryTrace.RoundTrace round : phase.getRounds()) {
                    for (StructureRecoveryTrace.InvariantTrace invariant : round.getInvariants()) {
                        if (!invariant.isPassed()) {
                            failedInvariants.add(phase.getPhase() + ":round-" + round.getRound() + ":" + invariant.getName());
                        }
                    }
                }
            }

            List<String> variablePassNames = new ArrayList<String>();
            int variablePassesWithStructuralDelta = 0;
            for (VariableRecoveryTrace.PassTrace pass : variableRecoveryTrace.getPasses()
                    .subList(beforeVariablePassCount, afterVariablePassCount)) {
                variablePassNames.add(pass.getPass().getDescriptor().getName());
                if (pass.hasStructuralDelta()) {
                    ++variablePassesWithStructuralDelta;
                }
            }

            List<String> typePassNames = new ArrayList<String>();
            int changedTypePassCount = 0;
            for (TypeRecoveryTrace.PassTrace pass : typeRecoveryTrace.getPasses()
                    .subList(beforeTypePassCount, afterTypePassCount)) {
                typePassNames.add(pass.getPass().getDescriptor().getName());
                if (pass.isChanged()) {
                    ++changedTypePassCount;
                }
            }

            return new StageArtifacts(
                    structurePhases,
                    failedInvariants,
                    variablePassNames,
                    variablePassesWithStructuralDelta,
                    typePassNames,
                    changedTypePassCount
            );
        }

        private static StageArtifacts empty() {
            return new StageArtifacts(List.of(), List.of(), List.of(), 0, List.of(), 0);
        }

        public List<String> getStructurePhaseNames() {
            return structurePhaseNames;
        }

        public List<String> getFailedStructureInvariants() {
            return failedStructureInvariants;
        }

        public List<String> getVariablePassNames() {
            return variablePassNames;
        }

        public int getVariablePassesWithStructuralDelta() {
            return variablePassesWithStructuralDelta;
        }

        public List<String> getTypePassNames() {
            return typePassNames;
        }

        public int getChangedTypePassCount() {
            return changedTypePassCount;
        }

        public boolean hasFailedStructureInvariants() {
            return !failedStructureInvariants.isEmpty();
        }

        public boolean hasVariableHotspots() {
            return variablePassesWithStructuralDelta > 0;
        }

        public boolean hasTypeHotspots() {
            return changedTypePassCount > 0;
        }

        public String describe(String stage,
                               String inputRequirement,
                               boolean skipped,
                               String skipReason) {
            StringBuilder builder = new StringBuilder();
            builder.append(stage).append('[').append(inputRequirement).append(']');
            if (skipped) {
                builder.append(" skipped=").append(skipReason);
            }
            builder.append(" structurePhases=").append(structurePhaseNames.size());
            builder.append(" failedStructureInvariants=").append(failedStructureInvariants.size());
            builder.append(" variablePasses=").append(variablePassNames.size());
            builder.append(" variableHotspots=").append(variablePassesWithStructuralDelta);
            builder.append(" typePasses=").append(typePassNames.size());
            builder.append(" typeHotspots=").append(changedTypePassCount);
            return builder.toString();
        }
    }
}
