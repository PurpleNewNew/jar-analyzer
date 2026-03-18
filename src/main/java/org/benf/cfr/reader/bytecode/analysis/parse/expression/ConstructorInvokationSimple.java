package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.VarArgsRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.rewriteinterface.FunctionProcessor;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.FieldVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.SentinelLocalClassLValue;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.*;
import org.benf.cfr.reader.bytecode.analysis.types.*;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.classfilehelpers.OverloadMethodSet;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.util.MiscConstants;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.List;

public class ConstructorInvokationSimple extends AbstractConstructorInvokation implements FunctionProcessor {

    private final MemberFunctionInvokation constructorInvokation;
    private InferredJavaType constructionType;

    public ConstructorInvokationSimple(BytecodeLoc loc,
                                       MemberFunctionInvokation constructorInvokation,
                                       InferredJavaType inferredJavaType, InferredJavaType constructionType,
                                       List<Expression> args) {
        super(loc, inferredJavaType, constructorInvokation.getFunction(), args);
        this.constructorInvokation = constructorInvokation;
        this.constructionType = constructionType;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, getArgs(), constructorInvokation);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new ConstructorInvokationSimple(getLoc(), constructorInvokation, getInferredJavaType(), constructionType, cloneHelper.replaceOrClone(getArgs()));
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.PAREN_SUB_MEMBER;
    }

    private JavaTypeInstance getFinalDisplayTypeInstance() {
        JavaTypeInstance res = recoverConstructionDisplayType(
                constructionType.getJavaTypeInstance(),
                getInferredJavaType().getJavaTypeInstance()
        );
        if (res instanceof JavaGenericBaseInstance && ((JavaGenericBaseInstance) res).hasL01Wildcard()) {
            res = ((JavaGenericBaseInstance) res).getWithoutL01Wildcard();
        }
        return ExpressionTypeHintHelper.sanitizeConstructorType(res);
    }

    JavaTypeInstance getDisplayTypeInstance() {
        return getFinalDisplayTypeInstance();
    }

    public void improveConstructionType(JavaTypeInstance targetType) {
        if (targetType == null) {
            return;
        }
        if (targetType instanceof JavaGenericBaseInstance && ((JavaGenericBaseInstance) targetType).hasL01Wildcard()) {
            targetType = ((JavaGenericBaseInstance) targetType).getWithoutL01Wildcard();
        }
        JavaTypeInstance improved = recoverConstructionDisplayType(constructionType.getJavaTypeInstance(), targetType);
        if (!improved.equals(constructionType.getJavaTypeInstance())) {
            constructionType.forceType(improved, true);
            getInferredJavaType().forceType(improved, true);
        }
        improveArgumentTypes(improved);
    }

    private JavaTypeInstance recoverConstructionDisplayType(JavaTypeInstance constructionDisplayType,
                                                            JavaTypeInstance candidateType) {
        if (candidateType == null) {
            return constructionDisplayType;
        }
        if (candidateType instanceof JavaGenericBaseInstance && ((JavaGenericBaseInstance) candidateType).hasL01Wildcard()) {
            candidateType = ((JavaGenericBaseInstance) candidateType).getWithoutL01Wildcard();
        }

        JavaTypeInstance rebound = reboundFromClassSignature(constructionDisplayType, candidateType);
        if (isImprovement(constructionDisplayType, rebound)) {
            return rebound;
        }

        if (!(constructionDisplayType instanceof JavaGenericBaseInstance)) {
            return constructionDisplayType;
        }
        JavaGenericBaseInstance genericConstructionType = (JavaGenericBaseInstance) constructionDisplayType;
        if (!genericConstructionType.hasUnbound()) {
            return constructionDisplayType;
        }

        JavaTypeInstance reboundDirect = maybeRebindWithBindings(genericConstructionType, constructionDisplayType, candidateType);
        if (isImprovement(constructionDisplayType, reboundDirect)) {
            return reboundDirect;
        }

        BindingSuperContainer constructionSupers = constructionDisplayType.getBindingSupers();
        if (constructionSupers != null && candidateType != null) {
            JavaGenericRefTypeInstance candidateBound = constructionSupers.getBoundSuperForBase(candidateType.getDeGenerifiedType());
            if (candidateBound != null) {
                JavaTypeInstance reboundFromBound = maybeRebindWithBindings(candidateBound, constructionDisplayType, candidateType);
                if (isImprovement(constructionDisplayType, reboundFromBound)) {
                    return reboundFromBound;
                }
            }
        }

        if (candidateType instanceof JavaGenericBaseInstance) {
            JavaTypeInstance reboundFromBase = GenericTypeBinder
                    .extractBaseBindings(genericConstructionType, candidateType)
                    .getBindingFor(constructionDisplayType);
            if (isImprovement(constructionDisplayType, reboundFromBase)) {
                return reboundFromBase;
            }
        }

        if (candidateType instanceof JavaGenericBaseInstance
                && constructionDisplayType.getDeGenerifiedType().equals(candidateType.getDeGenerifiedType())
                && !((JavaGenericBaseInstance) candidateType).hasUnbound()) {
            return candidateType;
        }
        return constructionDisplayType;
    }

    private JavaTypeInstance reboundFromClassSignature(JavaTypeInstance constructionDisplayType, JavaTypeInstance candidateType) {
        JavaTypeInstance rawConstructionType = constructionDisplayType.getDeGenerifiedType();
        if (!(rawConstructionType instanceof JavaRefTypeInstance)) {
            return constructionDisplayType;
        }
        ClassFile classFile = ((JavaRefTypeInstance) rawConstructionType).getClassFile();
        if (classFile == null) {
            return constructionDisplayType;
        }
        JavaTypeInstance unboundConstructionType =
                classFile.getClassSignature().getThisGeneralTypeClass(rawConstructionType, classFile.getConstantPool());
        if (!(unboundConstructionType instanceof JavaGenericRefTypeInstance)) {
            return constructionDisplayType;
        }
        BindingSuperContainer bindingSupers = unboundConstructionType.getBindingSupers();
        if (bindingSupers == null) {
            return constructionDisplayType;
        }
        JavaGenericRefTypeInstance unboundCandidate = bindingSupers.getBoundSuperForBase(candidateType.getDeGenerifiedType());
        if (unboundCandidate == null) {
            return constructionDisplayType;
        }
        return maybeRebindWithBindings((JavaGenericBaseInstance) unboundCandidate, unboundConstructionType, candidateType);
    }

    private JavaTypeInstance maybeRebindWithBindings(JavaGenericBaseInstance bindingSource,
                                                     JavaTypeInstance reboundTarget,
                                                     JavaTypeInstance candidateType) {
        JavaTypeInstance rebound = GenericTypeBinder
                .extractBindings(bindingSource, candidateType)
                .getBindingFor(reboundTarget);
        return rebound == null ? reboundTarget : rebound;
    }

    private boolean isImprovement(JavaTypeInstance currentType, JavaTypeInstance reboundType) {
        if (reboundType == null || reboundType.equals(currentType)) {
            return false;
        }
        if (!(reboundType instanceof JavaGenericBaseInstance)) {
            return true;
        }
        if (!(currentType instanceof JavaGenericBaseInstance)) {
            return true;
        }
        JavaGenericBaseInstance currentGeneric = (JavaGenericBaseInstance) currentType;
        JavaGenericBaseInstance reboundGeneric = (JavaGenericBaseInstance) reboundType;
        if (currentGeneric.hasUnbound() && !reboundGeneric.hasUnbound()) {
            return true;
        }
        if (currentGeneric.hasUnbound()
                && reboundGeneric.hasUnbound()
                && currentGeneric.getDeGenerifiedType().equals(reboundGeneric.getDeGenerifiedType())) {
            return true;
        }
        return !reboundGeneric.hasUnbound();
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        JavaTypeInstance clazz = getFinalDisplayTypeInstance();
        improveArgumentTypes(clazz);
        List<Expression> args = getArgs();
        MethodPrototype prototype = constructorInvokation.getMethodPrototype();

        if (prototype.isInnerOuterThis() && prototype.isHiddenArg(0) && args.size() > 0) {
            Expression a1 = args.get(0);

            test : if (!a1.toString().equals(MiscConstants.THIS)) {
                if (a1 instanceof LValueExpression) {
                    LValue lValue = ((LValueExpression) a1).getLValue();
                    if (lValue instanceof FieldVariable) {
                        ClassFileField classFileField = ((FieldVariable) lValue).getClassFileField();
                        if (classFileField.isSyntheticOuterRef()) break test;
                    }
                }
                d.dump(a1).print('.');
            }
        }

        d.keyword("new ").dump(clazz).separator("(");
        boolean first = true;
        for (int i = 0; i < args.size(); ++i) {
            if (prototype.isHiddenArg(i)) continue;
            Expression arg = args.get(i);
            if (!first) d.print(", ");
            first = false;
            d.dump(arg);
        }
        d.separator(")");
        return d;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null) return false;
        if (!(o instanceof ConstructorInvokationSimple)) return false;

        return super.equals(o);
    }

    public static boolean isAnonymousMethodType(JavaTypeInstance lValueType) {
        InnerClassInfo innerClassInfo = lValueType.getInnerClassHereInfo();
        if (innerClassInfo.isMethodScopedClass() && !innerClassInfo.isAnonymousClass()) {
            return true;
        }
        return false;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        JavaTypeInstance lValueType = constructorInvokation.getClassTypeInstance();
        if (isAnonymousMethodType(lValueType)) {
            lValueUsageCollector.collect(new SentinelLocalClassLValue(lValueType), ReadWrite.READ /* not strictly.. */);
        }
        super.collectUsedLValues(lValueUsageCollector);
    }


    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (!(o instanceof ConstructorInvokationSimple)) return false;
        if (!super.equivalentUnder(o, constraint)) return false;
        return true;
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        return caught.checkAgainst(constructorInvokation);
    }

    @Override
    public void rewriteVarArgs(VarArgsRewriter varArgsRewriter) {
        MethodPrototype methodPrototype = getMethodPrototype();
        if (!methodPrototype.isVarArgs()) return;
        OverloadMethodSet overloadMethodSet = getOverloadMethodSet();
        if (overloadMethodSet == null) return;
        GenericTypeBinder gtb = methodPrototype.getTypeBinderFor(getArgs());
        varArgsRewriter.rewriteVarArgsArg(overloadMethodSet, methodPrototype, getArgs(), gtb);
    }

    public MethodPrototype getConstructorPrototype() {
        return getMethodPrototype();
    }

    private void improveArgumentTypes(JavaTypeInstance targetType) {
        JavaTypeInstance normalizedTargetType = ExpressionTypeHintHelper.normalizeExpectedType(targetType);
        if (normalizedTargetType == null) {
            return;
        }
        MethodPrototype prototype = getConstructorPrototype();
        List<JavaTypeInstance> prototypeArgs = prototype.getArgs();
        GenericTypeBinder genericTypeBinder = null;
        JavaTypeInstance classType = prototype.getClassType();
        if (classType instanceof JavaGenericBaseInstance) {
            genericTypeBinder = GenericTypeBinder.extractBaseBindings((JavaGenericBaseInstance) classType, normalizedTargetType);
        }
        if (genericTypeBinder == null) {
            genericTypeBinder = prototype.getTypeBinderFor(getArgs());
        }
        OverloadMethodSet overloadMethodSet = getOverloadMethodSet();
        List<Expression> args = getArgs();
        for (int x = 0; x < args.size(); ++x) {
            if (prototype.isHiddenArg(x)) {
                continue;
            }
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
            if (expectedArgType == null) {
                expectedArgType = boundPrototypeArg;
            }
            ExpressionTypeHintHelper.improveExpressionType(arg, expectedArgType);
        }
    }
}
