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
                     int afterTypePassCount) {
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
    }
}
