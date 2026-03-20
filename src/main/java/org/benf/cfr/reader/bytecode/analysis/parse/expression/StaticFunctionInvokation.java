package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.PrimitiveBoxingRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.VarArgsRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.rewriteinterface.BoxingProcessor;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.rewriteinterface.FunctionProcessor;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.*;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericBaseInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodRef;
import org.benf.cfr.reader.entities.classfilehelpers.OverloadMethodSet;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.annotation.Nullable;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.TypeContext;

import java.util.List;

public class StaticFunctionInvokation extends AbstractFunctionInvokation implements FunctionProcessor, BoxingProcessor {
    protected final List<Expression> args;
    private final JavaTypeInstance clazz;
    // No - a static method doesn't require an object.
    // BUT - we could be an explicit static. :(
    private Expression object;
    private @Nullable
    List<JavaTypeInstance> explicitGenerics;

    private static InferredJavaType getTypeForFunction(ConstantPoolEntryMethodRef function, List<Expression> args) {
        return new InferredJavaType(
                function.getMethodPrototype().getReturnType(function.getClassEntry().getTypeInstance(), args),
                InferredJavaType.Source.FUNCTION, true);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new StaticFunctionInvokation(getLoc(), getFunction(), cloneHelper.replaceOrClone(args), cloneHelper.replaceOrClone(object));
    }

    private StaticFunctionInvokation(BytecodeLoc loc, ConstantPoolEntryMethodRef function, List<Expression> args, Expression object) {
        super(loc, function, getTypeForFunction(function, args));
        this.args = args;
        this.clazz = function.getClassEntry().getTypeInstance();
        this.object = object;
    }

    public StaticFunctionInvokation(BytecodeLoc loc, ConstantPoolEntryMethodRef function, List<Expression> args) {
        this(loc, function, args, null);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, args, object);
    }

    public void forceObject(Expression object) {
        this.object = object;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        if (object != null) {
            object.collectTypeUsages(collector);
        }
        collector.collect(clazz);
        for (Expression arg : args) {
            arg.collectTypeUsages(collector);
        }
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        LValueRewriter.Util.rewriteArgArray(lValueRewriter, ssaIdentifiers, statementContainer, args);
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        applyNonArgExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
        applyExpressionRewriterToArgs(expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        ExpressionRewriterHelper.applyBackwards(args, expressionRewriter, ssaIdentifiers, statementContainer, flags);
        applyNonArgExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public void applyExpressionRewriterToArgs(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        ExpressionRewriterHelper.applyForwards(args, expressionRewriter, ssaIdentifiers, statementContainer, flags);
    }

    @Override
    public void setExplicitGenerics(List<JavaTypeInstance> types) {
        explicitGenerics = types;
    }

    @Override
    public List<JavaTypeInstance> getExplicitGenerics() {
        return explicitGenerics;
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.PAREN_SUB_MEMBER;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        improveAgainstExpectedType(null);
        normalizeLateAssignedArgumentOrder();
        if (object != null) {
            d.dump(object).separator(".");
        } else {
            if (!d.getTypeUsageInformation().isStaticImport(clazz.getDeGenerifiedType(), getFixedName())) {
                d.dump(clazz, TypeContext.Static).separator(".");
            }
            if (explicitGenerics != null
                    && !explicitGenerics.isEmpty()
                    && !ExpressionTypeHintHelper.shouldSuppressExplicitGenerics(explicitGenerics)) {
                d.operator("<");
                boolean first = true;
                for (JavaTypeInstance typeInstance : explicitGenerics) {
                    first = StringUtils.comma(first, d);
                    d.dump(typeInstance);
                }
                d.operator(">");
            }
        }
        d.methodName(getFixedName(), getMethodPrototype(), false, false).separator("(");
        boolean first = true;
        for (Expression arg : args) {
            first = StringUtils.comma(first, d);
            d.dump(arg);
        }
        d.separator(")");
        return d;
    }

    private void normalizeLateAssignedArgumentOrder() {
        for (int targetIndex = 1; targetIndex < args.size(); ++targetIndex) {
            AssignmentCapture capture = AssignmentCapture.capture(args.get(targetIndex));
            if (capture == null) {
                continue;
            }
            for (int argIndex = 0; argIndex < targetIndex; ++argIndex) {
                Expression priorArg = args.get(argIndex);
                if (!isSimpleLocalReference(priorArg, capture.localVariable)) {
                    continue;
                }
                args.set(argIndex, capture.assignmentExpression);
                args.set(targetIndex, replaceAssignmentWithLocal(args.get(targetIndex), capture.assignmentExpression, capture.localVariable));
                return;
            }
        }
    }

    private boolean isSimpleLocalReference(Expression expression, LocalVariable localVariable) {
        if (!(expression instanceof LValueExpression) || localVariable == null) {
            return false;
        }
        LValue lValue = ((LValueExpression) expression).getLValue();
        return lValue instanceof LocalVariable
                && ((LocalVariable) lValue).matchesReadableAlias(localVariable);
    }

    private Expression replaceAssignmentWithLocal(Expression expression,
                                                  AssignmentExpression target,
                                                  LocalVariable localVariable) {
        return expression.applyExpressionRewriter(
                new LateAssignmentReferenceRewriter(target, localVariable),
                null,
                null,
                ExpressionRewriterFlags.RVALUE
        );
    }

    void improveAgainstExpectedType(JavaTypeInstance expectedType) {
        GenericTypeBinder genericTypeBinder = getExpectedTypeBinder(expectedType);
        improveArgumentTypes(genericTypeBinder);
        DisplayTypeResolution resolution = resolveDisplayReturnType(expectedType);
        JavaTypeInstance resolvedReturnType = resolution.getResolvedType();
        if (resolvedReturnType != null
                && !resolution.requiresExplicitCast()
                && !needsExpectedReturnCast(expectedType)) {
            getInferredJavaType().forceType(resolvedReturnType, true);
        }
        maybeDropRedundantExplicitGenerics();
    }

    JavaTypeInstance getDisplayReturnType() {
        return resolveDisplayReturnType(null).getResolvedType();
    }

    DisplayTypeResolution resolveDisplayReturnType(JavaTypeInstance expectedType) {
        MethodPrototype methodPrototype = getMethodPrototype();
        GenericTypeBinder expectedTypeBinder = getExpectedTypeBinder(expectedType);
        GenericTypeBinder argBinder = methodPrototype.getTypeBinderFor(args);
        JavaTypeInstance selectedReturnType = null;
        String selectedSource = null;
        JavaTypeInstance expectedBoundReturnType = null;
        if (expectedTypeBinder != null) {
            expectedBoundReturnType = expectedTypeBinder.getBindingFor(methodPrototype.getReturnType());
            if (ExpressionTypeHintHelper.shouldPreferResolvedType(selectedReturnType, expectedBoundReturnType)) {
                selectedReturnType = expectedBoundReturnType;
                selectedSource = "expected-bound-return";
            }
        }
        JavaTypeInstance argBoundReturnType = null;
        if (argBinder != null) {
            argBoundReturnType = argBinder.getBindingFor(methodPrototype.getReturnType());
            if (ExpressionTypeHintHelper.shouldPreferResolvedType(selectedReturnType, argBoundReturnType)) {
                selectedReturnType = argBoundReturnType;
                selectedSource = "arg-bound-return";
            }
        }
        JavaTypeInstance declaredReturnType = methodPrototype.getReturnType();
        if (ExpressionTypeHintHelper.shouldPreferResolvedType(selectedReturnType, declaredReturnType)) {
            selectedReturnType = declaredReturnType;
            selectedSource = "declared-return";
        }
        JavaTypeInstance fallbackReturnType = methodPrototype.getReturnType(clazz, args);
        if (ExpressionTypeHintHelper.shouldPreferResolvedType(selectedReturnType, fallbackReturnType)) {
            selectedReturnType = fallbackReturnType;
            selectedSource = "fallback-return";
        }
        JavaTypeInstance inferredReturnType = getInferredJavaType().getJavaTypeInstance();
        if (ExpressionTypeHintHelper.shouldPreferResolvedType(selectedReturnType, inferredReturnType)) {
            selectedReturnType = inferredReturnType;
            selectedSource = "inferred-return";
        }
        boolean requiresExplicitCast = normalizedExpectedCastRequired(
                expectedType,
                selectedReturnType,
                expectedBoundReturnType,
                argBoundReturnType,
                fallbackReturnType
        );
        return new DisplayTypeResolution(
                selectedReturnType,
                requiresExplicitCast,
                "expectedType=" + ExpressionTypeHintHelper.describeType(expectedType)
                        + ", expectedBinder=" + expectedTypeBinder
                        + ", expectedBoundReturn=" + ExpressionTypeHintHelper.describeType(expectedBoundReturnType)
                        + ", argBinder=" + argBinder
                        + ", argBoundReturn=" + ExpressionTypeHintHelper.describeType(argBoundReturnType)
                        + ", declaredReturn=" + ExpressionTypeHintHelper.describeType(declaredReturnType)
                        + ", fallbackReturn=" + ExpressionTypeHintHelper.describeType(fallbackReturnType)
                        + ", inferredReturn=" + ExpressionTypeHintHelper.describeType(inferredReturnType)
                        + ", explicitCast=" + requiresExplicitCast
                        + ", selectedSource=" + selectedSource
                        + ", selected=" + ExpressionTypeHintHelper.describeType(selectedReturnType)
        );
    }

    boolean needsExpectedReturnCast(JavaTypeInstance expectedType) {
        JavaTypeInstance normalizedExpectedType = ExpressionTypeHintHelper.normalizeExpectedType(expectedType);
        GenericTypeBinder genericTypeBinder = getExpectedTypeBinder(normalizedExpectedType);
        if (normalizedExpectedType == null || genericTypeBinder == null) {
            return false;
        }
        MethodPrototype methodPrototype = getMethodPrototype();
        List<JavaTypeInstance> prototypeArgs = methodPrototype.getArgs();
        for (int x = 0; x < args.size() && x < prototypeArgs.size(); ++x) {
            JavaTypeInstance expectedArgType = prototypeArgs.get(x);
            if (expectedArgType == null) {
                continue;
            }
            expectedArgType = genericTypeBinder.getBindingFor(expectedArgType);
            expectedArgType = ExpressionTypeHintHelper.normalizeExpectedType(expectedArgType);
            if (expectedArgType == null) {
                continue;
            }
            JavaTypeInstance argumentType = ExpressionTypeHintHelper.getDisplayType(args.get(x), expectedArgType);
            if (argumentType == null) {
                continue;
            }
            JavaTypeInstance argumentBaseType = argumentType.getDeGenerifiedType();
            JavaTypeInstance expectedBaseType = expectedArgType.getDeGenerifiedType();
            if (argumentBaseType != null
                    && expectedBaseType != null
                    && argumentBaseType.equals(expectedBaseType)
                    && ExpressionTypeHintHelper.shouldPreferResolvedType(argumentType, expectedArgType)) {
                return true;
            }
        }
        return false;
    }

    private boolean normalizedExpectedCastRequired(JavaTypeInstance expectedType,
                                                   JavaTypeInstance selectedReturnType,
                                                   JavaTypeInstance expectedBoundReturnType,
                                                   JavaTypeInstance argBoundReturnType,
                                                   JavaTypeInstance fallbackReturnType) {
        if (expectedType == null || selectedReturnType == null || expectedBoundReturnType == null) {
            return false;
        }
        if (!selectedReturnType.equals(expectedBoundReturnType)) {
            return false;
        }
        if (selectedReturnType.equals(argBoundReturnType) || selectedReturnType.equals(fallbackReturnType)) {
            return false;
        }
        return true;
    }

    private GenericTypeBinder getExpectedTypeBinder(JavaTypeInstance expectedType) {
        MethodPrototype methodPrototype = getMethodPrototype();
        GenericTypeBinder genericTypeBinder = null;
        JavaTypeInstance normalizedExpectedType = ExpressionTypeHintHelper.normalizeExpectedType(expectedType);
        JavaTypeInstance returnType = methodPrototype.getReturnType();
        if (normalizedExpectedType != null && returnType instanceof JavaGenericBaseInstance) {
            genericTypeBinder = GenericTypeBinder.extractBaseBindings((JavaGenericBaseInstance) returnType, normalizedExpectedType);
            if (genericTypeBinder != null && methodPrototype.hasFormalTypeParameters()) {
                List<JavaTypeInstance> explicitTypes = methodPrototype.getExplicitGenericUsage(genericTypeBinder);
                if (explicitTypes != null && !explicitTypes.isEmpty()) {
                    setExplicitGenerics(explicitTypes);
                }
            }
        }
        if (genericTypeBinder == null) {
            genericTypeBinder = methodPrototype.getTypeBinderFor(args);
        }
        return genericTypeBinder;
    }

    private void improveArgumentTypes(GenericTypeBinder genericTypeBinder) {
        MethodPrototype methodPrototype = getMethodPrototype();
        OverloadMethodSet overloadMethodSet = methodPrototype.getOverloadMethodSet();
        List<JavaTypeInstance> prototypeArgs = methodPrototype.getArgs();
        for (int x = 0; x < args.size(); ++x) {
            Expression arg = args.get(x);
            JavaTypeInstance boundPrototypeArg = null;
            if (x < prototypeArgs.size()) {
                boundPrototypeArg = prototypeArgs.get(x);
                if (genericTypeBinder != null) {
                    boundPrototypeArg = genericTypeBinder.getBindingFor(boundPrototypeArg);
                }
            }
            JavaTypeInstance expectedArgType = null;
            if (ExpressionTypeHintHelper.isSpecific(boundPrototypeArg)) {
                expectedArgType = boundPrototypeArg;
            }
            if (overloadMethodSet != null) {
                JavaTypeInstance overloadArgType = overloadMethodSet.getArgType(x, arg.getInferredJavaType().getJavaTypeInstance());
                if (!ExpressionTypeHintHelper.isSpecific(expectedArgType)) {
                    expectedArgType = overloadArgType;
                }
            }
            if (expectedArgType == null && x < prototypeArgs.size()) {
                expectedArgType = boundPrototypeArg;
            }
            ExpressionTypeHintHelper.improveExpressionType(arg, expectedArgType);
        }
    }

    private void maybeDropRedundantExplicitGenerics() {
        if (explicitGenerics == null || explicitGenerics.isEmpty()) {
            return;
        }
        if (ExpressionTypeHintHelper.shouldSuppressExplicitGenerics(explicitGenerics)) {
            explicitGenerics = null;
            return;
        }
        MethodPrototype methodPrototype = getMethodPrototype();
        GenericTypeBinder argOnlyBinder = methodPrototype.getTypeBinderFor(args);
        if (argOnlyBinder == null) {
            return;
        }
        List<JavaTypeInstance> inferredFromArgs = methodPrototype.getExplicitGenericUsage(argOnlyBinder);
        if (inferredFromArgs != null && inferredFromArgs.equals(explicitGenerics)) {
            explicitGenerics = null;
        }
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        for (Expression expression : args) {
            expression.collectUsedLValues(lValueUsageCollector);
        }
    }

    public JavaTypeInstance getClazz() {
        return clazz;
    }

    public List<Expression> getArgs() {
        return args;
    }

    @Override
    public void rewriteVarArgs(VarArgsRewriter varArgsRewriter) {
        MethodPrototype methodPrototype = getMethodPrototype();
        if (!methodPrototype.isVarArgs()) return;
        OverloadMethodSet overloadMethodSet = methodPrototype.getOverloadMethodSet();
        if (overloadMethodSet == null) return;
        GenericTypeBinder gtb = methodPrototype.getTypeBinderFor(args);
        varArgsRewriter.rewriteVarArgsArg(overloadMethodSet, methodPrototype, getArgs(), gtb);
    }


    public boolean rewriteBoxing(PrimitiveBoxingRewriter boxingRewriter) {
        OverloadMethodSet overloadMethodSet = getMethodPrototype().getOverloadMethodSet();
        if (overloadMethodSet == null) {
            return false;
        }
        for (int x = 0; x < args.size(); ++x) {
            /*
             * We can only remove explicit boxing if the target type is correct -
             * i.e. calling an object function with an explicit box can't have the box removed.
             *
             * This is fixed by a later pass which makes sure that the argument
             * can be passed to the target.
             */
            Expression arg = args.get(x);
            arg = boxingRewriter.rewriteExpression(arg, null, null, null);
            args.set(x, boxingRewriter.sugarParameterBoxing(arg, x, overloadMethodSet, null, getFunction().getMethodPrototype()));
        }
        return true;
    }

    @Override
    public void applyNonArgExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if (object != null) {
            object = expressionRewriter.rewriteExpression(object, ssaIdentifiers, statementContainer, flags);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (!(o instanceof StaticFunctionInvokation)) return false;
        StaticFunctionInvokation other = (StaticFunctionInvokation) o;
        if (!getName().equals(other.getName())) return false;
        if (!clazz.equals(other.clazz)) return false;
        if (!args.equals(other.args)) return false;
        return true;
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (!(o instanceof StaticFunctionInvokation)) return false;
        StaticFunctionInvokation other = (StaticFunctionInvokation) o;
        if (!constraint.equivalent(getName(), other.getName())) return false;
        if (!constraint.equivalent(clazz, other.clazz)) return false;
        if (!constraint.equivalent(args, other.args)) return false;
        return true;
    }

    private static final class AssignmentCapture extends AbstractExpressionRewriter {
        private AssignmentExpression assignmentExpression;
        private LocalVariable localVariable;
        private boolean duplicate;

        static AssignmentCapture capture(Expression expression) {
            AssignmentCapture capture = new AssignmentCapture();
            expression.applyExpressionRewriter(capture, null, null, ExpressionRewriterFlags.RVALUE);
            if (capture.duplicate || capture.assignmentExpression == null || capture.localVariable == null) {
                return null;
            }
            return capture;
        }

        @Override
        public Expression rewriteExpression(Expression expression,
                                            SSAIdentifiers ssaIdentifiers,
                                            StatementContainer statementContainer,
                                            ExpressionRewriterFlags flags) {
            if (expression instanceof AssignmentExpression) {
                AssignmentExpression assignmentExpression = (AssignmentExpression) expression;
                if (assignmentExpression.getUpdatedLValue() instanceof LocalVariable) {
                    if (this.assignmentExpression != null) {
                        duplicate = true;
                    } else {
                        this.assignmentExpression = assignmentExpression;
                        this.localVariable = (LocalVariable) assignmentExpression.getUpdatedLValue();
                    }
                }
            }
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }
    }

    private static final class LateAssignmentReferenceRewriter extends AbstractExpressionRewriter {
        private final AssignmentExpression target;
        private final LocalVariable replacement;

        private LateAssignmentReferenceRewriter(AssignmentExpression target, LocalVariable replacement) {
            this.target = target;
            this.replacement = replacement;
        }

        @Override
        public Expression rewriteExpression(Expression expression,
                                            SSAIdentifiers ssaIdentifiers,
                                            StatementContainer statementContainer,
                                            ExpressionRewriterFlags flags) {
            if (target.equals(expression)) {
                return new LValueExpression(replacement);
            }
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }
    }
}
