package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.ExpressionRewriterTransformer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.ConstructorUtils;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.NewAnonymousArray;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.FieldVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.QuotingUtils;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredReturn;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.AccessFlagMethod;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodHandle;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.MiscConstants;
import org.benf.cfr.reader.util.MiscUtils;
import org.benf.cfr.reader.util.Optional;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.Predicate;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class RecordRewriter {
    private static final Set<AccessFlag> recordFieldFlags = SetFactory.newSet(AccessFlag.ACC_FINAL, AccessFlag.ACC_PRIVATE);
    private static final Set<AccessFlagMethod> recordGetterFlags = SetFactory.newSet(AccessFlagMethod.ACC_PUBLIC);

    public static void rewrite(ClassFile classFile, DCCommonState state) {
        if (!TypeConstants.RECORD.equals(classFile.getBaseClassType())) return;

        /* For a record, we can eliminate
         * * the constructor (if it only assigns fields, and assigns ALL fields)
         *    (but we must leave validation code in place).
         * * equals, hashcode and toString, if they exactly match the expected bootstrap pattern.
         * * getters (if they are exactly getters).
         */
        rewriteIfRecord(classFile, state);
    }

    private static boolean rewriteIfRecord(ClassFile classFile, DCCommonState state) {
        List<ClassFileField> instances = Functional.filter(classFile.getFields(), new Predicate<ClassFileField>() {
            @Override
            public boolean test(ClassFileField in) {
                return !in.getField().testAccessFlag(AccessFlag.ACC_STATIC);
            }
        });

        if (!Functional.filter(instances, new Predicate<ClassFileField>() {
            @Override
            public boolean test(ClassFileField in) {
                return !recordFieldFlags.equals(in.getField().getAccessFlags());
            }
        }).isEmpty()) {
            // we have some fields that don't look right.
            return false;
        }

        // getters don't have to be default - we can hide them if they ARE default getters, but
        // it's not required.
        // However, the 0 argument getter for each field has to exist, AND it has to be public.
        List<Method> getters = ListFactory.newList();
        for (ClassFileField cff : instances) {
            List<Method> methods = classFile.getMethodsByNameOrNull(cff.getFieldName());
            // TODO: Rather than search for the method to match the field, we could
            // use the method as the source of truth.  However, there's a problem here, as
            // we can add additional methods which are not related.
            if (methods == null) return false;
            methods = Functional.filter(methods, new Predicate<Method>() {
                @Override
                public boolean test(Method in) {
                    return in.getMethodPrototype().getArgs().isEmpty();
                }
            });
            if (methods.size() != 1) return false;
            Method method = methods.get(0);
            if (!recordGetterFlags.equals(method.getAccessFlags())) {
                return false;
            }
            getters.add(method);
        }

        // There must be one canonical constructor, and all other constructors must
        // call it (eventually)
        // (the latter can be cheekily verified by ensuring all other constructors are chained to SOMETHING).
        List<Method> constructors = classFile.getConstructors();
        Method canonicalCons = findCanonicalConstructor(constructors, instances);
        if (canonicalCons == null) {
            return false;
        }
        for (Method other : constructors) {
            if (other == canonicalCons) {
                continue;
            }
            if (ConstructorUtils.getDelegatingPrototype(other) == null) return false;
        }

        JavaRefTypeInstance thisType = classFile.getRefClassType();
        // At this point, we're going to proceed, so can make changes.

        // Assignments from the parameters to the members should be at the end, in which case
        // they can hidden / removed.
        // If there is no code after this, the canonical can be hidden as implicit.
        if (removeImplicitAssignments(canonicalCons, instances, thisType)) {
            // Mark the canonical constructor for later.
            canonicalCons.setConstructorFlag(Method.MethodConstructor.RECORD_CANONICAL_CONSTRUCTOR);
        }
        // Now if it's empty, hide altogether.
        Block canonicalBlock = getStructuredCodeBlock(canonicalCons);
        if (canonicalBlock != null && canonicalBlock.isEffectivelyNOP()) {
            canonicalCons.hideDead();
        }

        // If any of the getters are default getters, hide them also.
        for (int x=0;x<getters.size();++x) {
            hideDefaultGetter(getters.get(x), instances.get(x), thisType);
        }

        hideDefaultUtilityMethods(classFile, thisType, instances);
        classFile.rewriteAsRecord(state);
        return true;
    }

    /*
     * Default implementations of
     * equals
     * hashcode
     * toString
     *
     * can be hidden.
     *
     * NB: This default may change - this is the existing behaviour as of 14-ea+34-1452
     */
    private static void hideDefaultUtilityMethods(ClassFile classFile, JavaTypeInstance thisType, List<ClassFileField> instances) {
        Method equalsMethod = findPublicMethod(classFile, Collections.<JavaTypeInstance>singletonList(TypeConstants.OBJECT), MiscConstants.EQUALS);
        Method toStringMethod = findPublicMethod(classFile, Collections.<JavaTypeInstance>emptyList(), MiscConstants.TOSTRING);
        Method hashCodeMethod = findPublicMethod(classFile, Collections.<JavaTypeInstance>emptyList(), MiscConstants.HASHCODE);

        hideDefaultObjectMethod(
                thisType,
                instances,
                equalsMethod,
                MiscConstants.EQUALS,
                RawJavaType.BOOLEAN,
                equalsMethod == null ? null : new LValueExpression(equalsMethod.getMethodPrototype().getComputedParameters().get(0))
        );
        hideDefaultObjectMethod(
                thisType,
                instances,
                toStringMethod,
                MiscConstants.TOSTRING,
                TypeConstants.STRING
        );
        hideDefaultObjectMethod(
                thisType,
                instances,
                hashCodeMethod,
                MiscConstants.HASHCODE,
                RawJavaType.INT
        );
    }

    private static void hideDefaultObjectMethod(JavaTypeInstance thisType,
                                                List<ClassFileField> fields,
                                                Method method,
                                                String methodName,
                                                JavaTypeInstance resultType,
                                                Expression... extraBootstrapArgs) {
        if (method == null) {
            return;
        }

        WildcardMatch wcm = new WildcardMatch();
        List<Expression> bootstrapArgs = ListFactory.newList();
        bootstrapArgs.add(new Literal(TypedLiteral.getString(QuotingUtils.enquoteString(methodName))));
        bootstrapArgs.add(wcm.getExpressionWildCard("array"));
        bootstrapArgs.add(wcm.getExpressionWildCard("this"));
        Collections.addAll(bootstrapArgs, extraBootstrapArgs);

        StructuredStatement stm = new StructuredReturn(
                BytecodeLoc.NONE,
                new CastExpression(
                        BytecodeLoc.NONE,
                        new InferredJavaType(resultType, InferredJavaType.Source.TEST),
                        wcm.getStaticFunction(
                                "func",
                                TypeConstants.OBJECTMETHODS,
                                TypeConstants.OBJECT,
                                "bootstrap",
                                bootstrapArgs.toArray(new Expression[0])
                        )
                ),
                resultType
        );

        StructuredStatement item = getSingleStructuredStatement(method);
        if (!stm.equals(item)) return;
        if (!matchesObjectMethodBootstrapArgs(wcm.getExpressionWildCard("array").getMatch(), thisType, fields)) return;
        if (!MiscUtils.isThis(wcm.getExpressionWildCard("this").getMatch(), thisType)) return;
        method.hideDead();
    }

    private static boolean matchesStringBootstrapArg(Expression expression, String name) {
        Literal l = expression.getComputedLiteral(null);
        if (l == null) return false;
        TypedLiteral tl = l.getValue();
        if (tl.getType() != TypedLiteral.LiteralType.String) return false;
        String val = tl.toString();
        return val.equals(QuotingUtils.enquoteString(name));
    }

    private static boolean matchesMethodHandleBootstrapArg(Expression expression, String name) {
        Literal l = expression.getComputedLiteral(null);
        if (l == null) return false;
        TypedLiteral tl = l.getValue();
        if (tl.getType() != TypedLiteral.LiteralType.MethodHandle) return false;
        ConstantPoolEntryMethodHandle handle = tl.getMethodHandle();
        if (!handle.isFieldRef()) return false;
        String fName = handle.getFieldRef().getLocalName();
        return name.equals(fName);
    }

    private static boolean matchesClassBootstrapArg(Expression expression, JavaTypeInstance thisType) {
        Literal l = expression.getComputedLiteral(null);
        if (l == null) return false;
        TypedLiteral tl = l.getValue();
        if (tl.getType() != TypedLiteral.LiteralType.Class) return false;
        return thisType.equals(tl.getClassValue());
    }

    private static boolean matchesObjectMethodBootstrapArgs(Expression cmpArgs,
                                                            JavaTypeInstance thisType,
                                                            List<ClassFileField> instances) {
        if (!(cmpArgs instanceof NewAnonymousArray)) return false;
        List<Expression> cmpValues = ((NewAnonymousArray) cmpArgs).getValues();
        if (cmpValues.size() != instances.size() + 2) return false;

        if (!matchesClassBootstrapArg(cmpValues.get(0), thisType)) return false;

        StringBuilder semi = new StringBuilder();
        int idx = 2;
        for (ClassFileField field : instances) {
            if (idx != 2) semi.append(";");
            Expression arg = cmpValues.get(idx++);
            String name = field.getFieldName();
            if (!matchesMethodHandleBootstrapArg(arg, name)) return false;
            semi.append(name);
        }
        if (!matchesStringBootstrapArg(cmpValues.get(1), semi.toString())) return false;
        return true;
    }

    private static Method findPublicMethod(ClassFile classFile, final List<JavaTypeInstance> args, String name) {
        List<Method> methods = classFile.getMethodsByNameOrNull(name);
        if (methods == null) return null;
        methods = Functional.filter(methods, new Predicate<Method>() {
            @Override
            public boolean test(Method in) {
                if (!in.testAccessFlag(AccessFlagMethod.ACC_PUBLIC)) return false;
                return in.getMethodPrototype().getArgs().equals(args);
            }
        });
        return methods.size() == 1 ? methods.get(0) : null;
    }

    private static StructuredStatement getSingleStructuredStatement(Method method) {
        Block block = getStructuredCodeBlock(method);
        if (block == null) return null;
        Optional<Op04StructuredStatement> content = block.getMaybeJustOneStatement();
        if (!content.isSet()) return null;
        return content.getValue().getStatement();
    }

    private static void hideDefaultGetter(Method method, ClassFileField classFileField, JavaRefTypeInstance thisType) {
        StructuredStatement item = getSingleStructuredStatement(method);
        if (!(item instanceof StructuredReturn)) {
            return;
        }
        Expression value = ((StructuredReturn) item).getValue();
        if (!(value instanceof LValueExpression)) {
            return;
        }
        if (resolveThisFieldReference(((LValueExpression) value).getLValue(), thisType) != classFileField) {
            return;
        }
        classFileField.markHidden();

        /*
         * If method has annotations cannot hide it. This is due to ambiguities regarding whether
         * method was explicitly declared in source and annotated, or whether it was generated
         * by the compiler and annotations with target METHOD were propagated from annotated
         * record component. Class file currently does not contain enough information to determine
         * this (see https://bugs.openjdk.org/browse/JDK-8251375), so to avoid losing annotations
         * by accident or introducing unwanted side effects by placing them on record component,
         * just keep the method.
         */
        if (method.getMethodAnnotations().isEmpty()) {
            method.hideDead();
        }
    }

    private static boolean removeImplicitAssignments(Method canonicalCons, List<ClassFileField> instances, JavaRefTypeInstance thisType) {
        Block block = getStructuredCodeBlock(canonicalCons);
        if (block == null) return false;
        List<Op04StructuredStatement> statements = block.getBlockStatements();
        ImplicitAssignmentTail implicitTail = collectTrailingImplicitAssignments(statements, instances, canonicalCons.getMethodPrototype().getComputedParameters(), thisType);
        /*
         * If there are any remaining usages of 'this.' left, we can't use the 0 argument canonical constructor,
         * because since J14, it's no longer valid to refer to 'this.' inside it.
         *
         * If we're not using the 0 argument version, we must assign all fields, so can't use the nops above.
         */
        if (hasLeadingThisReferences(statements, implicitTail.nopFrom, thisType)) {
            return false;
        }

        for (Op04StructuredStatement nop : implicitTail.toNop) {
            nop.nopOut();
        }
        return true;
    }

    private static ImplicitAssignmentTail collectTrailingImplicitAssignments(List<Op04StructuredStatement> statements,
                                                                             List<ClassFileField> instances,
                                                                             List<LocalVariable> args,
                                                                             JavaRefTypeInstance thisType) {
        List<ClassFileField> remainingFields = ListFactory.newList(instances);
        List<Op04StructuredStatement> toNop = ListFactory.newList();
        int nopFrom = statements.size();
        for (int x = statements.size() - 1; x >= 0; --x) {
            Op04StructuredStatement stm = statements.get(x);
            StructuredStatement statement = stm.getStatement();
            if (statement.isEffectivelyNOP()) {
                continue;
            }
            int idx = -1;
            if (statement instanceof StructuredAssignment) {
                StructuredAssignment assignment = (StructuredAssignment) statement;
                ClassFileField field = resolveThisFieldReference(assignment.getLvalue(), thisType);
                if (field != null) {
                    idx = remainingFields.indexOf(field);
                    if (idx != -1) {
                        Expression rhs = assignment.getRvalue();
                        if (!(rhs instanceof LValueExpression) || ((LValueExpression) rhs).getLValue() != args.get(idx)) {
                            idx = -1;
                        }
                    }
                }
            }
            if (idx == -1) {
                break;
            }
            remainingFields.set(idx, null);
            toNop.add(stm);
            nopFrom = x;
        }
        return new ImplicitAssignmentTail(toNop, nopFrom);
    }

    private static boolean hasLeadingThisReferences(List<Op04StructuredStatement> statements,
                                                    int exclusiveEnd,
                                                    JavaTypeInstance thisType) {
        ThisCheck thisCheck = new ThisCheck(thisType);
        ExpressionRewriterTransformer check = new ExpressionRewriterTransformer(thisCheck);
        for (int x = 0; x < exclusiveEnd && !thisCheck.failed; ++x) {
            check.transform(statements.get(x));
        }
        return thisCheck.failed;
    }

    private static final class ImplicitAssignmentTail {
        private final List<Op04StructuredStatement> toNop;
        private final int nopFrom;

        private ImplicitAssignmentTail(List<Op04StructuredStatement> toNop, int nopFrom) {
            this.toNop = toNop;
            this.nopFrom = nopFrom;
        }
    }

    static class ThisCheck extends AbstractExpressionRewriter {
        private final JavaTypeInstance thisType;
        private boolean failed;

        ThisCheck(JavaTypeInstance thisType) {
            this.thisType = thisType;
        }

        @Override
        public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (failed) {
                return lValue;
            }
            if (MiscUtils.isThis(lValue, thisType)) {
                failed = true;
            }
            return super.rewriteExpression(lValue, ssaIdentifiers, statementContainer, flags);
        }
    }

    private static ClassFileField resolveThisFieldReference(LValue lhs, JavaRefTypeInstance thisType) {
        if (!(lhs instanceof FieldVariable)) return null;
        Expression obj = ((FieldVariable) lhs).getObject();
        if (!MiscUtils.isThis(obj, thisType)) return null;
        return ((FieldVariable) lhs).getClassFileField();
    }

    private static Block getStructuredCodeBlock(Method method) {
        if (method == null || method.getCodeAttribute() == null) return null;
        StructuredStatement topCode = method.getAnalysis().getStatement();
        return topCode instanceof Block ? (Block) topCode : null;
    }

    private static Method findCanonicalConstructor(List<Method> constructors, List<ClassFileField> fields) {
        Method canonical = null;
        for (Method constructor : constructors) {
            Method.MethodConstructor constructorFlag = constructor.getConstructorFlag();
            if (!constructorFlag.isConstructor() || constructorFlag.isEnumConstructor()) {
                continue;
            }
            MethodPrototype proto = constructor.getMethodPrototype();
            if (!proto.parametersComputed()) {
                continue;
            }
            List<JavaTypeInstance> protoArgs = proto.getArgs();
            if (protoArgs.size() != fields.size()) {
                continue;
            }
            List<LocalVariable> parameters = proto.getComputedParameters();
            if (parameters.size() != fields.size()) {
                continue;
            }
            boolean matches = true;
            for (int x = 0; x < fields.size(); ++x) {
                JavaTypeInstance fieldType = fields.get(x).getField().getJavaTypeInstance();
                if (!fieldType.equals(protoArgs.get(x))) {
                    matches = false;
                    break;
                }
            }
            if (!matches) {
                continue;
            }
            if (canonical != null) {
                return null;
            }
            canonical = constructor;
        }
        return canonical;
    }
}
