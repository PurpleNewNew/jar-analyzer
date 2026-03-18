package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.SwitchPatternRewriter;

import java.util.List;

final class PatternSemanticsRewriter {
    private final PatternSemanticEngine engine;

    PatternSemanticsRewriter(ModernFeatureStrategy modernFeatures) {
        this.engine = new PatternSemanticEngine(
                List.of(
                        new InstanceOfPatternPass(),
                        new SwitchPatternPass(new SwitchPatternRewriter())
                ),
                modernFeatures
        );
    }

    void rewrite(Op04StructuredStatement block,
                 BytecodeMeta bytecodeMeta,
                 StructureRecoveryTrace trace) {
        engine.rewrite(block, bytecodeMeta, trace);
    }

    private enum PatternSemanticPhase {
        NORMALIZE,
        RECOVER,
        EMIT
    }

    private interface PatternSemanticPass {
        StructuredPassEntry entry(PatternSemanticPhase phase);

        boolean enabled(PatternSemanticContext context);

        default void apply(PatternSemanticContext context, PatternSemanticPhase phase) {
        }
    }

    private static final class PatternSemanticContext {
        private final Op04StructuredStatement root;
        private final BytecodeMeta bytecodeMeta;
        private final boolean bindingPatternsEnabled;
        private final boolean patternOutputPreferred;
        private final boolean switchPatternsEnabled;
        private final StructureRecoveryTrace trace;

        private PatternSemanticContext(Op04StructuredStatement root,
                                       BytecodeMeta bytecodeMeta,
                                       boolean bindingPatternsEnabled,
                                       boolean patternOutputPreferred,
                                       boolean switchPatternsEnabled,
                                       StructureRecoveryTrace trace) {
            this.root = root;
            this.bytecodeMeta = bytecodeMeta;
            this.bindingPatternsEnabled = bindingPatternsEnabled;
            this.patternOutputPreferred = patternOutputPreferred;
            this.switchPatternsEnabled = switchPatternsEnabled;
            this.trace = trace;
        }

        private Op04StructuredStatement getRoot() {
            return root;
        }

        private boolean hasInstanceOfMatches() {
            return bytecodeMeta.has(BytecodeMeta.CodeInfoFlag.INSTANCE_OF_MATCHES);
        }

        private boolean hasSwitches() {
            return bytecodeMeta.has(BytecodeMeta.CodeInfoFlag.SWITCHES);
        }

        private StructureRecoveryTrace getTrace() {
            return trace;
        }
    }

    private static final class PatternSemanticEngine {
        private final List<PatternSemanticPass> passes;
        private final boolean bindingPatternsEnabled;
        private final boolean patternOutputPreferred;
        private final boolean switchPatternsEnabled;

        private PatternSemanticEngine(List<PatternSemanticPass> passes,
                                      ModernFeatureStrategy modernFeatures) {
            this.passes = passes;
            this.bindingPatternsEnabled = modernFeatures.supportsBindingPatterns();
            this.patternOutputPreferred = modernFeatures.prefersPatternOutput();
            this.switchPatternsEnabled = modernFeatures.supportsPatternSwitches();
        }

        private void rewrite(Op04StructuredStatement block,
                             BytecodeMeta bytecodeMeta,
                             StructureRecoveryTrace trace) {
            PatternSemanticContext context = new PatternSemanticContext(
                    block,
                    bytecodeMeta,
                    bindingPatternsEnabled,
                    patternOutputPreferred,
                    switchPatternsEnabled,
                    trace
            );
            runPhase(context, PatternSemanticPhase.NORMALIZE);
            runPhase(context, PatternSemanticPhase.RECOVER);
            runPhase(context, PatternSemanticPhase.EMIT);
        }

        private void runPhase(PatternSemanticContext context, PatternSemanticPhase phase) {
            StructureRecoverySnapshot before = StructureRecoverySnapshot.capture(context.getRoot());
            StructureRecoveryTrace.PhaseTrace phaseTrace = context.getTrace().beginPhase(
                    "pattern-semantics." + phase.name().toLowerCase(),
                    "any-structure-state",
                    before
            );
            phaseTrace.recordInvariant("input-requirement", true, "accepted any-structure-state with " + before);
            StructureRecoveryTrace.RoundTrace roundTrace = phaseTrace.beginRound(1, before);
            for (PatternSemanticPass pass : passes) {
                StructuredPassEntry entry = pass.entry(phase);
                if (entry == null) {
                    continue;
                }
                StructureRecoverySnapshot passBefore = StructureRecoverySnapshot.capture(context.getRoot());
                boolean enabled = pass.enabled(context);
                if (enabled) {
                    pass.apply(context, phase);
                }
                roundTrace.recordPass(entry, enabled, passBefore, StructureRecoverySnapshot.capture(context.getRoot()));
            }
            StructureRecoverySnapshot after = StructureRecoverySnapshot.capture(context.getRoot());
            roundTrace.finish(after);
            phaseTrace.finish(after);
        }
    }

    private static final class InstanceOfPatternPass implements PatternSemanticPass {
        @Override
        public StructuredPassEntry entry(PatternSemanticPhase phase) {
            switch (phase) {
                case NORMALIZE:
                    return StructuredPatternTransforms.normalizeEntry();
                case RECOVER:
                    return StructuredPatternTransforms.tidyEntry();
                default:
                    return null;
            }
        }

        @Override
        public boolean enabled(PatternSemanticContext context) {
            return context.bindingPatternsEnabled;
        }

        @Override
        public void apply(PatternSemanticContext context, PatternSemanticPhase phase) {
            switch (phase) {
                case NORMALIZE:
                    StructuredPatternTransforms.normalizeInstanceOf(context.getRoot());
                    return;
                case RECOVER:
                    if (context.hasInstanceOfMatches()) {
                        StructuredPatternTransforms.tidyInstanceMatches(context.getRoot());
                    }
                    return;
                default:
                    return;
            }
        }
    }

    private static final class SwitchPatternPass implements PatternSemanticPass {
        private static final StructuredPassEntry EMIT_ENTRY = StructuredPassEntry.of(
                "pattern-semantics",
                "pattern-semantics.emit",
                "any-structure-state",
                StructuredPassDescriptor.of(
                        "switch-pattern-emit",
                        "Emits recovered switch patterns once semantic recovery is complete.",
                        true,
                        true
                )
        );
        private final SwitchPatternRewriter switchPatternRewriter;

        private SwitchPatternPass(SwitchPatternRewriter switchPatternRewriter) {
            this.switchPatternRewriter = switchPatternRewriter;
        }

        @Override
        public StructuredPassEntry entry(PatternSemanticPhase phase) {
            return phase == PatternSemanticPhase.EMIT ? EMIT_ENTRY : null;
        }

        @Override
        public boolean enabled(PatternSemanticContext context) {
            return context.patternOutputPreferred && context.switchPatternsEnabled && context.hasSwitches();
        }

        @Override
        public void apply(PatternSemanticContext context, PatternSemanticPhase phase) {
            if (phase == PatternSemanticPhase.EMIT) {
                switchPatternRewriter.rewrite(context.getRoot());
            }
        }
    }
}
