package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.ClashDeclarationReducer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.ExpressionRewriterTransformer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.LocalVariableMetadataTransformer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.ObjectTypeUsageRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.RedundantIntersectionCastTransformer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.SwitchExpressionRewriter;
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

final class StructuredSemanticTransforms {
    private StructuredSemanticTransforms() {
    }

    static void rewriteExplicitTypeUsages(Op04StructuredStatement root,
                                          AnonymousClassUsage anonymousClassUsage,
                                          ModernFeatureStrategy modernFeatures) {
        new ObjectTypeUsageRewriter(anonymousClassUsage, modernFeatures).transform(root);
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
        AttributeLocalVariableTypeTable localVariableTypeTable = code.getLocalVariableTypeTable();
        AttributeTypeAnnotations vis = code.getRuntimeVisibleTypeAnnotations();
        AttributeTypeAnnotations invis = code.getRuntimeInvisibleTypeAnnotations();
        if (vis == null && invis == null && localVariableTypeTable == null) {
            return;
        }
        LocalVariableMetadataTransformer transformer = new LocalVariableMetadataTransformer(
                localVariableTypeTable,
                vis,
                invis,
                instrsByOffset,
                comments,
                code.getConstantPool()
        );
        transformer.transform(root);
    }

    static void removeEndResource(ClassFile classFile, Op04StructuredStatement root) {
        boolean transformed = new TryResourcesTransformerJ9(classFile).transform(root);
        transformed |= new TryResourcesTransformerJ7(classFile).transform(root);
        transformed |= new TryResourcesTransformerJ12(classFile).transform(root);
        if (transformed) {
            new TryResourcesCollapser().transform(root);
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
        new RedundantIntersectionCastTransformer().transform(root);
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
}
