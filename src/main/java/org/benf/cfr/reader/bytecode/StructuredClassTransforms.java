package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.AnonymousClassConstructorRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.InnerClassConstructorRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LValueReplacingRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.PointlessStructuredExpressions;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.ResourceReleaseDetector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.SyntheticAccessorRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.SyntheticOuterRefRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.ExpressionRewriterTransformer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationAnonymousInner;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.FieldVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetUtil;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StructuredClassTransforms {
    private StructuredClassTransforms() {
    }

    static void fixInnerClassConstructorSyntheticOuterArgs(ClassFile classFile,
                                                           Method method,
                                                           Op04StructuredStatement root,
                                                           Set<MethodPrototype> processed) {
        if (classFile.isInnerClass()) {
            removeAnonymousSyntheticConstructorOuterArgs(method, root);
            removeMethodScopedSyntheticConstructorOuterArgs(method, root, processed);
        }
    }

    static void tidyAnonymousConstructors(Op04StructuredStatement root) {
        root.transform(new ExpressionRewriterTransformer(new AnonymousClassConstructorRewriter()), new StructuredScope());
    }

    static void inlineSyntheticAccessors(DCCommonState state, Method method, Op04StructuredStatement root) {
        JavaTypeInstance classType = method.getClassFile().getClassType();
        new SyntheticAccessorRewriter(state, classType).rewrite(root);
    }

    static void replaceNestedSyntheticOuterRefs(Op04StructuredStatement root) {
        List<StructuredStatement> statements = MiscStatementTools.linearise(root);
        if (statements == null) {
            return;
        }
        SyntheticOuterRefRewriter syntheticOuterRefRewriter = new SyntheticOuterRefRewriter();
        for (StructuredStatement statement : statements) {
            statement.rewriteExpressions(syntheticOuterRefRewriter);
            PointlessStructuredExpressions.removePointlessExpression(statement);
        }
    }

    public static boolean isTryWithResourceSynthetic(Method method, Op04StructuredStatement root) {
        return ResourceReleaseDetector.isResourceRelease(method, root);
    }

    static FieldVariable findInnerClassOuterThis(Method method, Op04StructuredStatement root) {
        MethodPrototype prototype = method.getMethodPrototype();
        List<LocalVariable> vars = prototype.getComputedParameters();
        if (vars.isEmpty()) {
            return null;
        }

        LocalVariable outerThis = vars.get(0);
        InnerClassConstructorRewriter innerClassConstructorRewriter =
                new InnerClassConstructorRewriter(method.getClassFile(), outerThis);
        innerClassConstructorRewriter.rewrite(root);
        return innerClassConstructorRewriter.getMatchedField();
    }

    static void removeInnerClassOuterThis(Method method, Op04StructuredStatement root) {
        MethodPrototype prototype = method.getMethodPrototype();
        List<LocalVariable> vars = prototype.getComputedParameters();
        if (vars.isEmpty()) {
            return;
        }

        LocalVariable outerThis = vars.get(0);
        InnerClassConstructorRewriter innerClassConstructorRewriter =
                new InnerClassConstructorRewriter(method.getClassFile(), outerThis);
        innerClassConstructorRewriter.rewrite(root);
        FieldVariable matchedLValue = innerClassConstructorRewriter.getMatchedField();
        if (matchedLValue == null) {
            return;
        }

        Map<LValue, LValue> replacements = MapFactory.newMap();
        replacements.put(outerThis, matchedLValue);
        innerClassConstructorRewriter.getAssignmentStatement().getContainer().nopOut();
        prototype.setInnerOuterThis();
        prototype.hide(0);

        applyLValueReplacer(replacements, root);
    }

    private static void removeMethodScopedSyntheticConstructorOuterArgs(Method method,
                                                                        Op04StructuredStatement root,
                                                                        Set<MethodPrototype> processed) {
        MethodPrototype prototype = method.getMethodPrototype();
        if (!processed.add(prototype)) {
            return;
        }
        List<MethodPrototype.ParameterLValue> vars = prototype.getParameterLValues();
        if (vars.isEmpty()) {
            return;
        }
        List<ConstructorInvokationSimple> usages = method.getClassFile().getMethodUsages();
        if (usages.isEmpty()) {
            return;
        }

        class CaptureExpression {
            private final int idx;
            private final Set<Expression> captures = new HashSet<Expression>();

            private CaptureExpression(int idx) {
                this.idx = idx;
            }
        }

        Map<MethodPrototype, MethodPrototype> protos = new IdentityHashMap<MethodPrototype, MethodPrototype>();
        Map<MethodPrototype.ParameterLValue, CaptureExpression> captured = MapFactory.newIdentityMap();
        for (ConstructorInvokationSimple usage : usages) {
            List<Expression> args = usage.getArgs();
            MethodPrototype proto = usage.getConstructorPrototype();
            protos.put(proto, proto);
            for (int x = 0; x < vars.size(); ++x) {
                MethodPrototype.ParameterLValue var = vars.get(x);
                if (var.isHidden() || proto.isHiddenArg(x)) {
                    CaptureExpression capture = captured.get(var);
                    if (capture == null) {
                        capture = new CaptureExpression(x);
                        captured.put(var, capture);
                    }
                    capture.captures.add(args.get(x));
                }
            }
        }

        MethodPrototype callProto = resolveCallPrototype(protos, prototype);
        if (callProto == null) {
            return;
        }

        ClassFile owner = method.getClassFile();
        for (int x = 0; x < vars.size(); ++x) {
            MethodPrototype.ParameterLValue parameterLValue = vars.get(x);
            CaptureExpression captureExpression = captured.get(parameterLValue);
            if (captureExpression == null || captureExpression.captures.size() != 1) {
                continue;
            }
            Expression expr = captureExpression.captures.iterator().next();
            if (!(expr instanceof LValueExpression)) {
                continue;
            }

            LValue lValueArg = ((LValueExpression) expr).getLValue();
            String overrideName = getInnerClassOuterArgName(method, lValueArg);
            if (overrideName == null) {
                continue;
            }

            if (parameterLValue.hidden == MethodPrototype.HiddenReason.HiddenOuterReference) {
                if (prototype.isInnerOuterThis()) {
                    if (prototype.isHiddenArg(captureExpression.idx)) {
                        callProto.hide(captureExpression.idx);
                    }
                } else {
                    hideField(root, callProto, owner, captureExpression.idx, parameterLValue.localVariable, lValueArg, overrideName);
                }
            } else if (parameterLValue.hidden == MethodPrototype.HiddenReason.HiddenCapture || callProto.isHiddenArg(x)) {
                hideField(root, callProto, owner, captureExpression.idx, parameterLValue.localVariable, lValueArg, overrideName);
            }
        }
    }

    private static MethodPrototype resolveCallPrototype(Map<MethodPrototype, MethodPrototype> protos, MethodPrototype prototype) {
        switch (protos.size()) {
            case 0:
                return null;
            case 1:
                return SetUtil.getSingle(protos.keySet());
            default:
                MethodPrototype callProto = null;
                for (MethodPrototype proto : protos.keySet()) {
                    if (proto.equalsMatch(prototype)) {
                        if (callProto == null) {
                            callProto = proto;
                        } else {
                            return null;
                        }
                    }
                }
                return callProto;
        }
    }

    private static void removeAnonymousSyntheticConstructorOuterArgs(Method method, Op04StructuredStatement root) {
        MethodPrototype prototype = method.getMethodPrototype();
        List<LocalVariable> vars = prototype.getComputedParameters();
        if (vars.isEmpty()) {
            return;
        }

        Map<LValue, LValue> replacements = MapFactory.newMap();
        List<ConstructorInvokationAnonymousInner> usages = method.getClassFile().getAnonymousUsages();
        ConstructorInvokationAnonymousInner usage = usages.size() == 1 ? usages.get(0) : null;
        if (usage == null) {
            return;
        }

        List<Expression> actualArgs = usage.getArgs();
        if (actualArgs.size() != vars.size()) {
            return;
        }

        ClassFile owner = method.getClassFile();
        for (int x = 0, len = vars.size(); x < len; ++x) {
            LocalVariable protoVar = vars.get(x);
            Expression arg = CastExpression.removeImplicit(actualArgs.get(x));
            if (!(arg instanceof LValueExpression)) {
                continue;
            }
            LValue lValueArg = ((LValueExpression) arg).getLValue();
            String overrideName = getInnerClassOuterArgName(method, lValueArg);
            if (overrideName == null) {
                continue;
            }
            hideField(root, prototype, owner, x, protoVar, lValueArg, overrideName);
        }

        applyLValueReplacer(replacements, root);
    }

    private static String getInnerClassOuterArgName(Method method, LValue lValueArg) {
        if (lValueArg instanceof LocalVariable) {
            return ((LocalVariable) lValueArg).getName().getStringName();
        }
        if (lValueArg instanceof FieldVariable) {
            FieldVariable fieldVariable = (FieldVariable) lValueArg;
            JavaTypeInstance thisClass = method.getClassFile().getClassType();
            JavaTypeInstance fieldClass = fieldVariable.getOwningClassType();
            if (thisClass.getInnerClassHereInfo().isTransitiveInnerClassOf(fieldClass)) {
                return fieldVariable.getFieldName();
            }
        }
        return null;
    }

    private static void hideField(Op04StructuredStatement root,
                                  MethodPrototype prototype,
                                  ClassFile classFile,
                                  int index,
                                  LocalVariable protoVar,
                                  LValue lValueArg,
                                  String overrideName) {
        InnerClassConstructorRewriter innerClassConstructorRewriter = new InnerClassConstructorRewriter(classFile, protoVar);
        innerClassConstructorRewriter.rewrite(root);
        FieldVariable matchedField = innerClassConstructorRewriter.getMatchedField();
        if (matchedField == null) {
            return;
        }
        innerClassConstructorRewriter.getAssignmentStatement().getContainer().nopOut();
        ClassFileField classFileField = matchedField.getClassFileField();
        classFileField.overrideName(overrideName);
        classFileField.markSyntheticOuterRef();
        classFileField.markHidden();
        prototype.hide(index);
        lValueArg.markFinal();
    }

    private static void applyLValueReplacer(Map<LValue, LValue> replacements, Op04StructuredStatement root) {
        if (!replacements.isEmpty()) {
            MiscStatementTools.applyExpressionRewriter(root, new LValueReplacingRewriter(replacements));
        }
    }
}
