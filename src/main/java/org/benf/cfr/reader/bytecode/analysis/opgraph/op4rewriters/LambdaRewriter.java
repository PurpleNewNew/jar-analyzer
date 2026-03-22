package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractNewArray;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.DynamicInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ExpressionTypeHintHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpressionCommon;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpressionFallback;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpressionNewArray;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.NewAnonymousArray;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.NewObjectArray;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LambdaParameter;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollectorSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.expression.StructuredStatementExpression;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredDefinition;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredReturn;
import org.benf.cfr.reader.bytecode.analysis.types.DynamicInvokeType;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaIntersectionTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.AccessFlagMethod;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.classfilehelpers.OverloadMethodSet;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodHandle;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodRef;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.lambda.LambdaUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LambdaRewriter implements Op04Rewriter, ExpressionRewriter {

    private final DCCommonState state;
    private final ClassFile thisClassFile;
    private final JavaTypeInstance typeInstance;
    private final Method method;
    private final LinkedList<Expression> processingStack = ListFactory.newLinkedList();

    public LambdaRewriter(DCCommonState state, Method method) {
        this.state = state;
        this.method = method;
        this.thisClassFile = method.getClassFile();
        this.typeInstance = thisClassFile.getClassType().getDeGenerifiedType();
    }

    @Override
    public void rewrite(Op04StructuredStatement root) {
        List<StructuredStatement> structuredStatements = MiscStatementTools.linearise(root);
        if (structuredStatements == null) return;

        /*
         * Lambdas come in two forms - the lambda which has been produced by the java compiler,
         * which will involve an invokedynamic call, and the lambda which has been produced by
         * an anonymous inner class - this wasn't a lambda in the original code, but we should
         * consider transforming back into lambdas because we can ;)
         */

        for (StructuredStatement statement : structuredStatements) {
            statement.rewriteExpressions(this);
        }
    }

    @Override
    public void handleStatement(StatementContainer statementContainer) {
    }

    /*
     * Expression rewriter boilerplate - note that we can't expect ssaIdentifiers to be non-null.
     */
    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        try {
            processingStack.push(expression);
            expression = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
            if (expression instanceof DynamicInvokation) {
                expression = rewriteDynamicExpression((DynamicInvokation) expression);
            }
            Expression res = expression;
            if (res instanceof CastExpression) {
                Expression child = ((CastExpression) res).getChild();
                if (child instanceof LambdaExpressionCommon) {
                    JavaTypeInstance resType = res.getInferredJavaType().getJavaTypeInstance();
                    JavaTypeInstance childType = child.getInferredJavaType().getJavaTypeInstance();

                    // If the child type is an intersection type it cannot be removed safely, at least not if
                    // it includes Serializable since that makes the complete lambda serializable.
                    if (childType instanceof JavaIntersectionTypeInstance) {
                        child = new CastExpression(BytecodeLoc.NONE, child.getInferredJavaType(), child, true);
                    }

                    if (childType.implicitlyCastsTo(resType, null)) {
                        return child;
                    } else {
                        /*
                         * This is more interesting - the cast doesn't work?  This means we might need to explicitly label
                         * the lambda expression type.
                         */
                        Expression tmp = new CastExpression(BytecodeLoc.NONE, child.getInferredJavaType(), child, true);
                        res = new CastExpression(BytecodeLoc.NONE, res.getInferredJavaType(), tmp);
                        return res;
                    }
                }
            } else if (res instanceof MemberFunctionInvokation) {
                MemberFunctionInvokation invoke = (MemberFunctionInvokation) res;
                if (invoke.getObject() instanceof LambdaExpressionCommon) {
                    res = invoke.withReplacedObject(new CastExpression(BytecodeLoc.NONE, invoke.getObject().getInferredJavaType(), invoke.getObject()));
                }
            }
            return res;
        } finally {
            processingStack.pop();
        }
    }

    @Override
    public ConditionalExpression rewriteExpression(ConditionalExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        Expression res = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        return (ConditionalExpression) res;
    }

    @Override
    public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return lValue;
    }

    @Override
    public StackSSALabel rewriteExpression(StackSSALabel lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return lValue;
    }

    /*
     * Back to the main event.
     *
     */
    private Expression rewriteDynamicExpression(DynamicInvokation dynamicExpression) {
        List<Expression> curriedArgs = dynamicExpression.getDynamicArgs();
        Expression functionCall = dynamicExpression.getInnerInvokation();
        if (functionCall instanceof StaticFunctionInvokation) {
            Expression res = rewriteDynamicExpression(dynamicExpression, (StaticFunctionInvokation) functionCall, curriedArgs);
            if (res == dynamicExpression) {
                return dynamicExpression;
            }
            // If the direct container is a member function invokation, and by NOT stating the argument type, we're
            // making it ambiguous, we need to force an explicit argument type.
            if (res instanceof LambdaExpression && processingStack.size() > 1) {
                Expression e = processingStack.get(1);
                couldBeAmbiguous(e, dynamicExpression, (LambdaExpression)res);
            }
            return res;
        }
        return dynamicExpression;
    }

    private void couldBeAmbiguous(Expression fn, Expression arg, LambdaExpression res) {
        if (!(fn instanceof AbstractFunctionInvokation) || thisClassFile == null) return;
        AbstractFunctionInvokation afi = (AbstractFunctionInvokation)fn;
        OverloadMethodSet oms = thisClassFile.getOverloadMethodSet(afi.getMethodPrototype());
        if (oms.size() < 2) return;
        // Find the index of this argument in the original invokation.
        List<Expression> args = afi.getArgs();
        int idx = args.indexOf(arg);
        if (idx == -1) return;
        // We may have very limited information about what the possible argument types are.
        // err on the side of caution, unless we can prove we aren't ambiguous.
        List<JavaTypeInstance> types = Functional.filter(oms.getPossibleArgTypes(idx, arg.getInferredJavaType().getJavaTypeInstance()),
                new Predicate<JavaTypeInstance>() {
                    @Override
                    public boolean test(JavaTypeInstance in) {
                        return in instanceof JavaRefTypeInstance || in instanceof JavaGenericRefTypeInstance;
                    }
                });
        if (types.size() == 1) return;
        JavaTypeInstance functionArgType = afi.getMethodPrototype().getArgs().get(idx);
        res.setExplicitArgTypes(getExplicitLambdaTypes(functionArgType));
    }

    private List<JavaTypeInstance> getExplicitLambdaTypes(JavaTypeInstance functionArgType) {
        ClassFile classFile = null;
        try {
            classFile = state.getClassFile(functionArgType.getDeGenerifiedType());
        } catch (CannotLoadClassException ignore) {
        }
        if (classFile == null || !classFile.isInterface()) return null;
        // Find the one method which has no body.
        List<Method> methods = Functional.filter(classFile.getMethods(), new Predicate<Method>() {
            @Override
            public boolean test(Method in) {
                return in.getCodeAttribute() == null;
            }
        });
        if (methods.size() != 1) return null;
        Method method = methods.get(0);
        List<JavaTypeInstance> args = method.getMethodPrototype().getArgs();
        if (functionArgType instanceof JavaGenericRefTypeInstance) {
            final GenericTypeBinder genericTypeBinder = classFile.getGenericTypeBinder((JavaGenericRefTypeInstance) functionArgType);
            args = Functional.map(args, new UnaryFunction<JavaTypeInstance, JavaTypeInstance>() {
                @Override
                public JavaTypeInstance invoke(JavaTypeInstance arg) {
                    return genericTypeBinder.getBindingFor(arg);
                }
            });
        }
        for (JavaTypeInstance arg : args) {
            if (arg == null) return null;
        }
        return args;
    }

    private static class CannotInlineLambdaException extends IllegalStateException {
    }

    private static Expression asInlineLambdaArgument(Expression e) {
        if (e instanceof LValueExpression) {
            LValueExpression lValueExpression = (LValueExpression) e;
            LValue lValue = lValueExpression.getLValue();
            return new LValueExpression(lValue);
        }
        if (e instanceof NewObjectArray) return e;
        if (e instanceof CastExpression) return e;
        throw new CannotInlineLambdaException();
    }

    private Expression rewriteDynamicExpression(DynamicInvokation dynamicExpression, StaticFunctionInvokation functionInvokation, List<Expression> curriedArgs) {
        JavaTypeInstance typeInstance = functionInvokation.getClazz();
        if (!typeInstance.getRawName().equals(TypeConstants.lambdaMetaFactoryName)) return dynamicExpression;
        String functionName = functionInvokation.getName();

        if (DynamicInvokeType.lookup(functionName) == DynamicInvokeType.UNKNOWN) return dynamicExpression;

        List<Expression> metaFactoryArgs = functionInvokation.getArgs();
        if (metaFactoryArgs.size() != 6) return dynamicExpression;
        /*
         * Right, it's the 6 argument form of LambdaMetafactory.metaFactory, which we understand.
         *
         */
        Expression arg = metaFactoryArgs.get(3);

        List<JavaTypeInstance> targetFnArgTypes = LambdaUtils.getLiteralProto(arg).getArgs();

        ConstantPoolEntryMethodHandle lambdaFnHandle = LambdaUtils.getHandle(metaFactoryArgs.get(4));
        ConstantPoolEntryMethodRef lambdaMethRef = lambdaFnHandle.getMethodRef();
        JavaTypeInstance lambdaTypeLocation = lambdaMethRef.getClassEntry().getTypeInstance();
        MethodPrototype lambdaFn = lambdaMethRef.getMethodPrototype();
        List<JavaTypeInstance> lambdaFnArgTypes = lambdaFn.getArgs();

        if (!(lambdaTypeLocation instanceof JavaRefTypeInstance)) {
            return dynamicExpression;
        }
        JavaRefTypeInstance lambdaTypeRefLocation = (JavaRefTypeInstance) lambdaTypeLocation;
        ClassFile classFile = resolveLambdaTargetClassFile(lambdaTypeRefLocation);

        // We can't ask the prototype for instance behaviour, we have to get it from the
        // handle, as it will point to a ref.
        boolean instance = false;
        switch (lambdaFnHandle.getReferenceKind()) {
            case INVOKE_INTERFACE:
            case INVOKE_SPECIAL:
            case INVOKE_VIRTUAL:
                instance = true;
                break;
        }

        /*
         * If we don't have the classfile (let's say we're looking at java8's consumer in java6) we can still GUESS
         * what it was going to do....
         */
        if (classFile == null) {
            return buildLambdaFallback(dynamicExpression, lambdaTypeRefLocation, lambdaFn, targetFnArgTypes, curriedArgs, instance);
        }

        if (curriedArgs.size() + targetFnArgTypes.size() - (instance ? 1 : 0) != lambdaFnArgTypes.size()) {
            throw new IllegalStateException("Bad argument counts!");
        }

        /* Now, we can call the synthetic function directly and emit it, or we could inline the synthetic, and no
         * longer emit it.
         */
        Method lambdaMethod;
        try {
            lambdaMethod = classFile.getMethodByPrototype(lambdaFn);
        } catch (NoSuchMethodException ignore) {
            // This might happen if you're using a JRE which doesn't have support classes, etc.
            return dynamicExpression;
        }
        normalizeCurriedArgs(curriedArgs);
        recoverSyntheticLambdaParameterTypes(lambdaMethod, curriedArgs, targetFnArgTypes, instance);
        Expression inlined = tryInlineSyntheticLambda(dynamicExpression, curriedArgs, targetFnArgTypes, lambdaTypeRefLocation, lambdaMethod, instance);
        if (inlined != null) {
            return inlined;
        }

        // Ok, just call the synthetic method directly.
        renameFallbackLambdaMethod(lambdaMethod, lambdaFn);
        return buildLambdaFallback(dynamicExpression, lambdaTypeRefLocation, lambdaFn, targetFnArgTypes, curriedArgs, instance);
    }

    private ClassFile resolveLambdaTargetClassFile(JavaRefTypeInstance lambdaTypeRefLocation) {
        if (this.typeInstance.equals(lambdaTypeRefLocation)) {
            return thisClassFile;
        }
        try {
            return state.getClassFile(lambdaTypeRefLocation);
        } catch (CannotLoadClassException ignore) {
            return null;
        }
    }

    private LambdaExpressionFallback buildLambdaFallback(DynamicInvokation dynamicExpression,
                                                         JavaRefTypeInstance lambdaTypeRefLocation,
                                                         MethodPrototype lambdaFn,
                                                         List<JavaTypeInstance> targetFnArgTypes,
                                                         List<Expression> curriedArgs,
                                                         boolean instance) {
        return new LambdaExpressionFallback(BytecodeLoc.TODO, lambdaTypeRefLocation, dynamicExpression.getInferredJavaType(), lambdaFn, targetFnArgTypes, curriedArgs, instance);
    }

    private void normalizeCurriedArgs(List<Expression> curriedArgs) {
        for (int x = 0, len = curriedArgs.size(); x < len; ++x) {
            Expression curriedArg = curriedArgs.get(x);
            JavaTypeInstance curriedArgType = curriedArg.getInferredJavaType().getJavaTypeInstance();
            if (curriedArgType.getDeGenerifiedType().equals(TypeConstants.SUPPLIER)) {
                if (curriedArg instanceof CastExpression) {
                    CastExpression castExpression = (CastExpression)curriedArg;
                    curriedArg = new CastExpression(BytecodeLoc.NONE, curriedArg.getInferredJavaType(), castExpression.getChild(), true);
                } else if (!(curriedArg instanceof LValueExpression)) {
                    curriedArg = new CastExpression(BytecodeLoc.NONE, curriedArg.getInferredJavaType(), curriedArg, true);
                }
            }
            curriedArgs.set(x, CastExpression.removeImplicit(curriedArg));
        }
    }

    private void recoverSyntheticLambdaParameterTypes(Method lambdaMethod,
                                                      List<Expression> curriedArgs,
                                                      List<JavaTypeInstance> targetFnArgTypes,
                                                      boolean instance) {
        if (lambdaMethod == null || !lambdaMethod.testAccessFlag(AccessFlagMethod.ACC_SYNTHETIC)) {
            return;
        }
        MethodPrototype methodPrototype = lambdaMethod.getMethodPrototype();
        List<LocalVariable> parameters = methodPrototype.getComputedParameters();
        List<JavaTypeInstance> args = methodPrototype.getArgs();
        int parameterIdx = 0;
        for (int curriedIdx = instance ? 1 : 0; curriedIdx < curriedArgs.size() && parameterIdx < parameters.size() && parameterIdx < args.size(); ++curriedIdx, ++parameterIdx) {
            JavaTypeInstance recoveredType = ExpressionTypeHintHelper.getDisplayType(curriedArgs.get(curriedIdx), args.get(parameterIdx));
            applyRecoveredLambdaParameterType(parameters.get(parameterIdx), args, parameterIdx, recoveredType);
        }
        for (int targetIdx = 0; targetIdx < targetFnArgTypes.size() && parameterIdx < parameters.size() && parameterIdx < args.size(); ++targetIdx, ++parameterIdx) {
            applyRecoveredLambdaParameterType(parameters.get(parameterIdx), args, parameterIdx, targetFnArgTypes.get(targetIdx));
        }
    }

    private void applyRecoveredLambdaParameterType(LocalVariable parameter,
                                                   List<JavaTypeInstance> args,
                                                   int argIndex,
                                                   JavaTypeInstance recoveredType) {
        if (parameter == null || recoveredType == null || argIndex < 0 || argIndex >= args.size()) {
            return;
        }
        JavaTypeInstance currentType = args.get(argIndex);
        if (currentType != null && !currentType.getDeGenerifiedType().equals(recoveredType.getDeGenerifiedType())) {
            return;
        }
        if (currentType != null && !ExpressionTypeHintHelper.shouldPreferResolvedType(currentType, recoveredType)) {
            return;
        }
        args.set(argIndex, recoveredType);
        parameter.getInferredJavaType().forceType(recoveredType, true);
        parameter.setCustomCreationJavaType(recoveredType);
    }

    private void renameFallbackLambdaMethod(Method lambdaMethod, MethodPrototype lambdaFn) {
        if (lambdaMethod == null || lambdaFn == null) {
            return;
        }
        String methodName = lambdaMethod.getName();
        if (!methodName.startsWith("lambda$")) {
            return;
        }
        String safeName = "cfr$" + methodName;
        lambdaMethod.getMethodPrototype().setFixedName(safeName);
        lambdaFn.setFixedName(safeName);
    }

    private Expression tryInlineSyntheticLambda(DynamicInvokation dynamicExpression,
                                                List<Expression> curriedArgs,
                                                List<JavaTypeInstance> targetFnArgTypes,
                                                JavaRefTypeInstance lambdaTypeRefLocation,
                                                Method lambdaMethod,
                                                boolean instance) {
        if (!this.typeInstance.equals(lambdaTypeRefLocation) || !lambdaMethod.testAccessFlag(AccessFlagMethod.ACC_SYNTHETIC)) {
            return null;
        }
        try {
            Op04StructuredStatement lambdaCode;
            try {
                lambdaCode = lambdaMethod.getAnalysis();
            } catch (Exception e) {
                throw new CannotInlineLambdaException();
            }

            int lambdaArgCount = targetFnArgTypes.size();
            List<Expression> replacementParameters = ListFactory.newList();
            for (int idx = instance ? 1 : 0, len = curriedArgs.size(); idx < len; ++idx) {
                replacementParameters.add(asInlineLambdaArgument(curriedArgs.get(idx)));
            }

            List<LValue> anonymousLambdaArgs = ListFactory.newList();
            List<LocalVariable> originalParameters = lambdaMethod.getMethodPrototype().getComputedParameters();
            int offset = replacementParameters.size();
            for (int idx = 0; idx < lambdaArgCount; ++idx) {
                LocalVariable original = originalParameters.get(idx + offset);
                String name = original.getName().getStringName();
                LocalVariable tmp = new LambdaParameter(name, new InferredJavaType(targetFnArgTypes.get(idx), InferredJavaType.Source.EXPRESSION),
                        lambdaMethod.getMethodPrototype(), idx + offset);
                anonymousLambdaArgs.add(tmp);
                replacementParameters.add(new LValueExpression(tmp));
            }

            if (originalParameters.size() != replacementParameters.size()) throw new CannotInlineLambdaException();

            Map<LValue, Expression> rewrites = MapFactory.newMap();
            for (int idx = 0; idx < originalParameters.size(); ++idx) {
                rewrites.put(originalParameters.get(idx), replacementParameters.get(idx));
            }

            List<StructuredStatement> structuredLambdaStatements = MiscStatementTools.linearise(lambdaCode);
            if (structuredLambdaStatements == null) {
                throw new CannotInlineLambdaException();
            }
            if (!shouldInlineStructuredLambda(structuredLambdaStatements, anonymousLambdaArgs)) {
                return null;
            }
            ExpressionRewriter variableRenamer = new LambdaInternalRewriter(rewrites);
            for (StructuredStatement lambdaStatement : structuredLambdaStatements) {
                lambdaStatement.rewriteExpressions(variableRenamer);
            }
            StructuredStatement lambdaStatement = lambdaCode.getStatement();

            lambdaMethod.hideSynthetic();

            if (structuredLambdaStatements.size() == 3 && structuredLambdaStatements.get(1) instanceof StructuredReturn) {
                StructuredReturn structuredReturn = (StructuredReturn) structuredLambdaStatements.get(1);
                Expression expression = structuredReturn.getValue();
                if (isNewArrayLambda(expression, curriedArgs, anonymousLambdaArgs)) {
                    return new LambdaExpressionNewArray(structuredReturn.getCombinedLoc(), dynamicExpression.getInferredJavaType(), expression.getInferredJavaType());
                }
                lambdaStatement = new StructuredExpressionStatement(structuredReturn.getCombinedLoc(), expression, true);
            }

            method.copyLocalClassesFrom(lambdaMethod);
            return new LambdaExpression(lambdaStatement.getCombinedLoc(), dynamicExpression.getInferredJavaType(), anonymousLambdaArgs, null,
                    new StructuredStatementExpression(new InferredJavaType(lambdaMethod.getMethodPrototype().getReturnType(), InferredJavaType.Source.EXPRESSION), lambdaStatement));
        } catch (CannotInlineLambdaException ignore) {
            return null;
        }
    }

    private boolean shouldInlineStructuredLambda(List<StructuredStatement> statements, List<LValue> lambdaArgs) {
        Set<LValue> lambdaParameters = org.benf.cfr.reader.util.collections.SetFactory.newSet();
        lambdaParameters.addAll(lambdaArgs);
        for (StructuredStatement statement : statements) {
            if (statement instanceof StructuredComment
                    || statement instanceof Block
                    || statement instanceof StructuredReturn) {
                continue;
            }
            if (statement instanceof StructuredDefinition) {
                if (!lambdaParameters.contains(((StructuredDefinition) statement).getLvalue())) {
                    return false;
                }
                continue;
            }
            if (statement instanceof StructuredAssignment) {
                LValue lValue = ((StructuredAssignment) statement).getLvalue();
                if (lValue instanceof LocalVariable && !lambdaParameters.contains(lValue)) {
                    return false;
                }
                continue;
            }
            if (statement instanceof StructuredExpressionStatement
                    && ((StructuredExpressionStatement) statement).getExpression() instanceof AssignmentExpression) {
                LValue updated = ((AssignmentExpression) ((StructuredExpressionStatement) statement).getExpression()).getUpdatedLValue();
                if (updated instanceof LocalVariable && !lambdaParameters.contains(updated)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isNewArrayLambda(Expression e, List<Expression> curriedArgs, List<LValue> anonymousLambdaArgs) {
        if (!curriedArgs.isEmpty()) return false;
        if (anonymousLambdaArgs.size() != 1) return false;

        if (!(e instanceof AbstractNewArray)) return false;
		if (e instanceof NewAnonymousArray) return true;
        AbstractNewArray ana = (AbstractNewArray)e;
        if (ana.getNumDims() != 1) return false;
        return ana.getDimSize(0).equals(new LValueExpression(anonymousLambdaArgs.get(0)));
    }

    public static class LambdaInternalRewriter implements ExpressionRewriter {
        private final Map<LValue, Expression> rewrites;

        LambdaInternalRewriter(Map<LValue, Expression> rewrites) {
            this.rewrites = rewrites;
        }

        @Override
        public void handleStatement(StatementContainer statementContainer) {

        }

        @Override
        public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (expression instanceof LValueExpression) {
                LValue lv = ((LValueExpression) expression).getLValue();
                Expression rewrite = rewrites.get(lv);
                if (rewrite != null) return rewrite;
            }
            return expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        }

        @Override
        public ConditionalExpression rewriteExpression(ConditionalExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            Expression res = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
            return (ConditionalExpression) res;
        }

        @Override
        public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            Expression replacement = rewrites.get(lValue);
            if (replacement instanceof LValueExpression) {
                return ((LValueExpression) replacement).getLValue();
            }
            return lValue;
        }

        @Override
        public StackSSALabel rewriteExpression(StackSSALabel lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            return lValue;
        }
    }

}
