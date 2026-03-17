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

    void rewrite(Op04StructuredStatement block, BytecodeMeta bytecodeMeta) {
        engine.rewrite(block, bytecodeMeta);
    }

    private enum PatternSemanticPhase {
        NORMALIZE,
        RECOVER,
        EMIT
    }

    private interface PatternSemanticPass {
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

        private PatternSemanticContext(Op04StructuredStatement root,
                                       BytecodeMeta bytecodeMeta,
                                       boolean bindingPatternsEnabled,
                                       boolean patternOutputPreferred,
                                       boolean switchPatternsEnabled) {
            this.root = root;
            this.bytecodeMeta = bytecodeMeta;
            this.bindingPatternsEnabled = bindingPatternsEnabled;
            this.patternOutputPreferred = patternOutputPreferred;
            this.switchPatternsEnabled = switchPatternsEnabled;
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

        private void rewrite(Op04StructuredStatement block, BytecodeMeta bytecodeMeta) {
            PatternSemanticContext context = new PatternSemanticContext(
                    block,
                    bytecodeMeta,
                    bindingPatternsEnabled,
                    patternOutputPreferred,
                    switchPatternsEnabled
            );
            runPhase(context, PatternSemanticPhase.NORMALIZE);
            runPhase(context, PatternSemanticPhase.RECOVER);
            runPhase(context, PatternSemanticPhase.EMIT);
        }

        private void runPhase(PatternSemanticContext context, PatternSemanticPhase phase) {
            for (PatternSemanticPass pass : passes) {
                if (pass.enabled(context)) {
                    pass.apply(context, phase);
                }
            }
        }
    }

    private static final class InstanceOfPatternPass implements PatternSemanticPass {
        @Override
        public boolean enabled(PatternSemanticContext context) {
            return context.bindingPatternsEnabled;
        }

        @Override
        public void apply(PatternSemanticContext context, PatternSemanticPhase phase) {
            switch (phase) {
                case NORMALIZE:
                    Op04StructuredStatement.normalizeInstanceOf(context.getRoot(), true);
                    return;
                case RECOVER:
                    if (context.hasInstanceOfMatches()) {
                        Op04StructuredStatement.tidyInstanceMatches(context.getRoot());
                    }
                    return;
                default:
                    return;
            }
        }
    }

    private static final class SwitchPatternPass implements PatternSemanticPass {
        private final SwitchPatternRewriter switchPatternRewriter;

        private SwitchPatternPass(SwitchPatternRewriter switchPatternRewriter) {
            this.switchPatternRewriter = switchPatternRewriter;
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
