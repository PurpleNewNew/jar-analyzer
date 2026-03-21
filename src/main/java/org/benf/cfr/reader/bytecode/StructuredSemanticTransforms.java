package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.BadCastChainRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.ClashDeclarationReducer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.NarrowingAssignmentRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.RedundantSuperRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.VarArgsRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.checker.IllegalReturnChecker;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.checker.LooseCatchChecker;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.checker.Op04Checker;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.checker.VoidVariableChecker;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.ExpressionRewriterTransformer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.HexLiteralTidier;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.InvalidBooleanCastCleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.InvalidExpressionStatementCleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.LambdaCleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.LocalVariableMetadataTransformer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.NakedNullCaster;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.ObjectTypeUsageRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.RedundantIntersectionCastTransformer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.SwitchExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.TernaryCastCleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.TryResourcesCollapser;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.TryResourcesTransformerJ12;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.TryResourcesTransformerJ7;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.TryResourcesTransformerJ9;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.VariableNameTidier;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ConstantFoldingRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.LiteralRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.AbstractLValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverImpl;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LocalClassScopeDiscoverImpl;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.expression.StructuredStatementExpression;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredDefinition;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableFactory;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.attributes.AttributeCode;
import org.benf.cfr.reader.entities.attributes.AttributeLocalVariableTable;
import org.benf.cfr.reader.entities.attributes.AttributeLocalVariableTypeTable;
import org.benf.cfr.reader.entities.attributes.AttributeTypeAnnotations;
import org.benf.cfr.reader.state.ClassCache;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;

import java.util.Set;
import java.util.SortedMap;
import java.util.List;

final class StructuredSemanticTransforms {
    private static final String OUTPUT_STAGE = "output-stage";
    private static final String FULLY_STRUCTURED_INPUT = "fully-structured";
    private static final List<ModernSemanticPlan> EMPTY_MODERN_PLANS = List.of();
    private static final StructuredPassEntry EXPRESSION_POLISH_PASS = StructuredPassEntry.of(
            "output-stage",
            OUTPUT_STAGE,
            FULLY_STRUCTURED_INPUT,
            StructuredPassDescriptor.of(
                    "expression-polish-stage",
                    "Applies expression-only output polish after structural and semantic recovery are complete.",
                    true,
                    false
            )
    );
    private static final StructuredPassEntry VALIDATION_METADATA_PASS = StructuredPassEntry.of(
            "output-stage",
            OUTPUT_STAGE,
            FULLY_STRUCTURED_INPUT,
            StructuredPassDescriptor.of(
                    "validation-and-metadata-stage",
                    "Runs output validation and metadata annotation without changing structure.",
                    true,
                    false
            )
    );

    private StructuredSemanticTransforms() {
    }

    static List<StructuredPassEntry> describePasses() {
        return List.of(
                entry("rewrite-explicit-type-usages", "modern-semantics", "fully-structured", "Rewrites explicit type usage to match modern anonymous and pattern semantics.", true, false),
                entry("remove-end-resource", "modern-semantics", "fully-structured", "Collapses try-with-resources synthetic release scaffolding.", true, true),
                entry("switch-expression", "modern-semantics", "fully-structured", "Rewrites switch scaffolding into switch-expression form when supported.", true, true),
                entry("rewrite-lambdas", "modern-semantics", "fully-structured", "Rewrites indy and synthetic lambda bodies into Java lambda syntax.", true, true),
                entry("remove-redundant-intersection-casts", "modern-semantics", "fully-structured", "Removes redundant intersection casts after semantic rewrites.", true, false, "rewrite-lambdas"),
                entry("tidy-variable-names", "output-polish.expression-polish", "fully-structured", "Renames locals to stable printable names without changing structure.", true, false),
                entry("apply-local-variable-metadata", "output-polish.validation-and-metadata", "fully-structured", "Applies LVT/LVTT and type-annotation metadata after structure is finalized.", false, false)
        );
    }

    static List<StructuredPassEntry> describeOutputPasses() {
        return List.of(
                outputEntry("remove-constructor-boilerplate", "output-polish.expression-polish", "Removes redundant constructor super boilerplate.", true, false),
                outputEntry("remove-unnecessary-vararg-arrays", "output-polish.expression-polish", "Elides synthetic vararg array wrappers.", true, false),
                outputEntry("rewrite-bad-cast-chains", "output-polish.expression-polish", "Simplifies cast chains produced by earlier rewrites.", true, false),
                outputEntry("rewrite-narrowing-assignments", "output-polish.expression-polish", "Rewrites explicit narrowing assignment scaffolding.", true, false),
                outputEntry("tidy-obfuscation", "output-polish.expression-polish", "Folds obfuscation constants when configured.", true, false),
                outputEntry("misc-keyhole-transforms", "output-polish.expression-polish", "Applies keyhole expression cleanups that preserve structure.", true, false),
                outputEntry("apply-checker", "output-polish.validation-and-metadata", "Runs output validators and records any decompiler comments.", false, false)
        );
    }

    static List<ModernSemanticPlan> modernSemanticPlans(StructureRecoveryPipeline structureRecoveryPipeline,
                                                        PatternSemanticsRewriter patternSemanticsRewriter) {
        if (structureRecoveryPipeline == null || patternSemanticsRewriter == null) {
            return EMPTY_MODERN_PLANS;
        }
        return List.of(
                modernPlan((block, context) -> rewriteExplicitTypeUsages(block, context.anonymousClassUsage, context.modernFeatures)),
                modernPlan(
                        context -> context.options.getOption(OptionsImpl.REWRITE_TRY_RESOURCES, context.classFileVersion),
                        (block, context) -> removeEndResource(context.classFile, block, context.comments)
                ),
                modernPlan((block, context) -> patternSemanticsRewriter.rewrite(block, context.bytecodeMeta, context.structureRecoveryTrace)),
                modernPlan(
                        context -> context.modernFeatures.supportsSwitchExpressions(),
                        (block, context) -> switchExpression(
                                context.method,
                                block,
                                context.comments,
                                context.modernFeatures.shouldEmitPreviewSwitchExpressionComment()
                        )
                ),
                modernPlan((block, context) -> structureRecoveryPipeline.cleanupAfterModernSemantics(block, context)),
                modernPlan((block, context) -> rewriteLambdas(context.commonState, context.method, block)),
                modernPlan((block, context) -> removeRedundantIntersectionCasts(block))
        );
    }

    static List<OutputPolishPlan> outputPolishPlans(StructureRecoveryPipeline structureRecoveryPipeline) {
        return List.of(
                outputPlan(EXPRESSION_POLISH_PASS, (block, context) -> applyExpressionPolish(block, context, structureRecoveryPipeline)),
                outputPlan(VALIDATION_METADATA_PASS, StructuredSemanticTransforms::applyValidationAndMetadata)
        );
    }

    static void rewriteExplicitTypeUsages(Op04StructuredStatement root,
                                          AnonymousClassUsage anonymousClassUsage,
                                          ModernFeatureStrategy modernFeatures) {
        transformStructured(root, new ObjectTypeUsageRewriter(anonymousClassUsage, modernFeatures));
    }

    static void reduceClashDeclarations(Op04StructuredStatement root, BytecodeMeta bytecodeMeta) {
        if (bytecodeMeta.getLivenessClashes().isEmpty()) {
            return;
        }
        root.transform(new ClashDeclarationReducer(bytecodeMeta.getLivenessClashes()), new StructuredScope());
    }

    static void tidyVariableNames(Method method,
                                  Op04StructuredStatement root,
                                  BytecodeMeta bytecodeMeta,
                                  DecompilerComments comments,
                                  ClassCache classCache,
                                  ModernFeatureStrategy modernFeatures) {
        VariableNameTidier variableNameTidier = new VariableNameTidier(
                method,
                VariableNameTidier.NameDiscoverer.getUsedLambdaNames(bytecodeMeta, root),
                classCache,
                modernFeatures
        );
        variableNameTidier.transform(root);
        if (variableNameTidier.isClassRenamed()) {
            comments.addComment(DecompilerComment.CLASS_RENAMED);
        }
    }

    static void applyLocalVariableMetadata(AttributeCode code,
                                           Op04StructuredStatement root,
                                           SortedMap<Integer, Integer> instrsByOffset,
                                           DecompilerComments comments) {
        AttributeLocalVariableTable localVariableTable = code.getLocalVariableTable();
        AttributeLocalVariableTypeTable localVariableTypeTable = code.getLocalVariableTypeTable();
        AttributeTypeAnnotations vis = code.getRuntimeVisibleTypeAnnotations();
        AttributeTypeAnnotations invis = code.getRuntimeInvisibleTypeAnnotations();
        if (vis == null && invis == null && localVariableTypeTable == null && localVariableTable == null) {
            return;
        }
        LocalVariableMetadataTransformer transformer = new LocalVariableMetadataTransformer(
                localVariableTable,
                localVariableTypeTable,
                vis,
                invis,
                instrsByOffset,
                comments,
                code.getConstantPool()
        );
        transformStructured(root, transformer);
    }

    static void removeEndResource(ClassFile classFile, Op04StructuredStatement root, DecompilerComments comments) {
        boolean transformed = new TryResourcesTransformerJ9(classFile).transform(root);
        transformed |= new TryResourcesTransformerJ7(classFile).transform(root);
        transformed |= new TryResourcesTransformerJ12(classFile).transform(root);
        if (transformed) {
            new TryResourcesCollapser().transform(root);
            if (comments != null) {
                // LOOPING_EXCEPTIONS is emitted before try-with-resources recovery, when the synthetic release
                // scaffolding still looks like a suspicious self-catching loop. Once we have successfully
                // resugared the construct, keeping that warning becomes misleading noise.
                comments.removeComment(DecompilerComment.LOOPING_EXCEPTIONS);
            }
        }
    }

    static void switchExpression(Method method,
                                 Op04StructuredStatement root,
                                 DecompilerComments comments,
                                 boolean emitPreviewComment) {
        new SwitchExpressionRewriter(comments, method, emitPreviewComment).transform(root);
    }

    static void markLambdaCapturedVariables(Op04StructuredStatement root) {
        LambdaCaptureCollector collector = new LambdaCaptureCollector();
        new ExpressionRewriterTransformer(collector).transform(root);
        if (collector.getCaptured().isEmpty()) {
            return;
        }
        root.transform(new LambdaCaptureMarker(collector.getCaptured()), new StructuredScope());
    }

    static void discoverVariableScopes(Method method,
                                       Op04StructuredStatement root,
                                       VariableFactory variableFactory,
                                       Options options,
                                       ClassFileVersion classFileVersion,
                                       BytecodeMeta bytecodeMeta) {
        LValueScopeDiscoverImpl scopeDiscoverer =
                new LValueScopeDiscoverImpl(options, method.getMethodPrototype(), variableFactory, classFileVersion);
        scopeDiscoverer.processOp04Statement(root);
        scopeDiscoverer.markDiscoveredCreations();
        if (scopeDiscoverer.didDetectInstanceOfMatching()) {
            bytecodeMeta.set(BytecodeMeta.CodeInfoFlag.INSTANCE_OF_MATCHES);
        }
    }

    static void discoverLocalClassScopes(Method method,
                                         Op04StructuredStatement root,
                                         VariableFactory variableFactory,
                                         Options options) {
        AbstractLValueScopeDiscoverer scopeDiscoverer = new LocalClassScopeDiscoverImpl(options, method, variableFactory);
        scopeDiscoverer.processOp04Statement(root);
        scopeDiscoverer.markDiscoveredCreations();
    }

    static void rewriteLambdas(DCCommonState state, Method method, Op04StructuredStatement root) {
        if (!state.getOptions().getOption(OptionsImpl.REWRITE_LAMBDAS, method.getClassFile().getClassFileVersion())) {
            return;
        }
        new LambdaRewriter(state, method).rewrite(root);
    }

    static void removeRedundantIntersectionCasts(Op04StructuredStatement root) {
        transformStructured(root, new RedundantIntersectionCastTransformer());
    }

    static void removeConstructorBoilerplate(Op04StructuredStatement root) {
        new RedundantSuperRewriter().rewrite(root);
    }

    private static void applyExpressionPolish(Op04StructuredStatement block,
                                             MethodAnalysisContext context,
                                             StructureRecoveryPipeline structureRecoveryPipeline) {
        if (context.options.getOption(OptionsImpl.REMOVE_BOILERPLATE) && context.method.isConstructor()) {
            removeConstructorBoilerplate(block);
        }
        new VarArgsRewriter().rewrite(block);
        rewriteExpressions(block, new BadCastChainRewriter());
        new NarrowingAssignmentRewriter().rewrite(block);
        tidyVariableNames(
                context.method,
                block,
                context.bytecodeMeta,
                context.comments,
                context.constantPool.getClassCache(),
                context.modernFeatures
        );
        if (context.options.getOption(OptionsImpl.CONST_OBF)) {
            rewriteExpressions(block, ConstantFoldingRewriter.INSTANCE);
        }
        transformStructured(block, new NakedNullCaster());
        transformStructured(block, new LambdaCleaner());
        transformStructured(block, new TernaryCastCleaner());
        transformStructured(block, new InvalidBooleanCastCleaner());
        transformStructured(block, new HexLiteralTidier());
        rewriteExpressions(block, LiteralRewriter.INSTANCE);
        transformStructured(block, new InvalidExpressionStatementCleaner(context.variableFactory));
        structureRecoveryPipeline.applyOutputPolish(block, context);
    }

    private static void applyValidationAndMetadata(Op04StructuredStatement block, MethodAnalysisContext context) {
        applyChecker(new LooseCatchChecker(), block, context.comments);
        applyChecker(new VoidVariableChecker(), block, context.comments);
        applyChecker(new IllegalReturnChecker(), block, context.comments);
        applyLocalVariableMetadata(
                context.originalCodeAttribute,
                block,
                context.lutByOffset,
                context.comments
        );
    }

    private static void applyChecker(Op04Checker checker, Op04StructuredStatement root, DecompilerComments comments) {
        transformStructured(root, checker);
        checker.commentInto(comments);
    }

    private static void transformStructured(Op04StructuredStatement root, StructuredStatementTransformer transformer) {
        root.transform(transformer, new StructuredScope());
    }

    private static void rewriteExpressions(Op04StructuredStatement root, ExpressionRewriter rewriter) {
        new ExpressionRewriterTransformer(rewriter).transform(root);
    }

    private static final class LambdaCaptureCollector extends AbstractExpressionRewriter {
        private final Set<LocalVariable> captured = SetFactory.newSet();

        private Set<LocalVariable> getCaptured() {
            return captured;
        }

        @Override
        public Expression rewriteExpression(Expression expression,
                                            SSAIdentifiers ssaIdentifiers,
                                            StatementContainer statementContainer,
                                            ExpressionRewriterFlags flags) {
            if (expression instanceof LambdaExpression) {
                LambdaExpression lambdaExpression = (LambdaExpression) expression;
                LValueCollectingRewriter collector = new LValueCollectingRewriter();
                Expression lambdaResult = lambdaExpression.getResult();
                if (lambdaResult instanceof StructuredStatementExpression) {
                    ((StructuredStatementExpression) lambdaResult).getContent().rewriteExpressions(collector);
                } else {
                    lambdaResult.applyExpressionRewriter(collector, null, null, ExpressionRewriterFlags.RVALUE);
                }
                for (LValue used : collector.getUsedLValues()) {
                    if (!(used instanceof LocalVariable)
                            || lambdaExpression.getArgs().contains(used)
                            || !((LocalVariable) used).getName().isGoodName()) {
                        continue;
                    }
                    captured.add((LocalVariable) used);
                }
            }
            return expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        }
    }

    private static final class LValueCollectingRewriter extends AbstractExpressionRewriter {
        private final Set<LValue> usedLValues = SetFactory.newSet();

        private Set<LValue> getUsedLValues() {
            return usedLValues;
        }

        @Override
        public LValue rewriteExpression(LValue lValue,
                                        SSAIdentifiers ssaIdentifiers,
                                        StatementContainer statementContainer,
                                        ExpressionRewriterFlags flags) {
            usedLValues.add(lValue);
            return lValue;
        }
    }

    private static final class LambdaCaptureMarker extends AbstractExpressionRewriter implements StructuredStatementTransformer {
        private final Set<LocalVariable> captured;

        private LambdaCaptureMarker(Set<LocalVariable> captured) {
            this.captured = captured;
        }

        @Override
        public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
            in.transformStructuredChildren(this, scope);
            if (in instanceof StructuredDefinition) {
                LValue lValue = ((StructuredDefinition) in).getLvalue();
                if (lValue instanceof LocalVariable && isCapturedLocal((LocalVariable) lValue)) {
                    ((LocalVariable) lValue).markCapturedByLambda();
                }
            }
            in.rewriteExpressions(this);
            return in;
        }

        @Override
        public LValue rewriteExpression(LValue lValue,
                                        SSAIdentifiers ssaIdentifiers,
                                        StatementContainer statementContainer,
                                        ExpressionRewriterFlags flags) {
            if (lValue instanceof LocalVariable && isCapturedLocal((LocalVariable) lValue)) {
                ((LocalVariable) lValue).markCapturedByLambda();
            }
            return lValue;
        }

        private boolean isCapturedLocal(LocalVariable localVariable) {
            for (LocalVariable candidate : captured) {
                if (localVariable.matchesReadableAlias(candidate)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static StructuredPassEntry entry(String name,
                                             String stage,
                                             String inputRequirement,
                                             String outputPromise,
                                             boolean idempotent,
                                             boolean allowsStructuralChange,
                                             String... dependencies) {
        return StructuredPassEntry.of(
                "semantic-transform",
                stage,
                inputRequirement,
                StructuredPassDescriptor.of(name, outputPromise, idempotent, allowsStructuralChange, dependencies)
        );
    }

    private static StructuredPassEntry outputEntry(String name,
                                                   String stage,
                                                   String outputPromise,
                                                   boolean idempotent,
                                                   boolean allowsStructuralChange) {
        return StructuredPassEntry.of(
                "output-transform",
                stage,
                FULLY_STRUCTURED_INPUT,
                StructuredPassDescriptor.of(name, outputPromise, idempotent, allowsStructuralChange)
        );
    }

    private static ModernSemanticPlan modernPlan(ModernSemanticAction action) {
        return modernPlan(context -> true, action);
    }

    private static ModernSemanticPlan modernPlan(ModernSemanticPredicate enabled,
                                                 ModernSemanticAction action) {
        return new ModernSemanticPlan(enabled, action);
    }

    private static OutputPolishPlan outputPlan(StructuredPassEntry pass, OutputPolishAction action) {
        return new OutputPolishPlan(pass, action);
    }

    @FunctionalInterface
    interface ModernSemanticPredicate {
        boolean test(MethodAnalysisContext context);
    }

    @FunctionalInterface
    interface ModernSemanticAction {
        void apply(Op04StructuredStatement block, MethodAnalysisContext context);
    }

    record ModernSemanticPlan(ModernSemanticPredicate enabled, ModernSemanticAction action) {
        boolean enabled(MethodAnalysisContext context) {
            return enabled.test(context);
        }

        void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            action.apply(block, context);
        }
    }

    @FunctionalInterface
    interface OutputPolishAction {
        void apply(Op04StructuredStatement block, MethodAnalysisContext context);
    }

    record OutputPolishPlan(StructuredPassEntry pass, OutputPolishAction action) {
        void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            action.apply(block, context);
        }
    }
}
