package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.*;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.*;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.ArrayVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StaticVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.*;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.BeginBlock;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.EndBlock;
import org.benf.cfr.reader.bytecode.analysis.types.JavaArrayTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.*;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SwitchEnumRewriter implements Op04Rewriter {
    private static final String MATCH_SWITCH = "switch";
    private static final String MATCH_BODYLESS_SWITCH = "bodylessswitch";

    private final DCCommonState dcCommonState;
    private final ClassFile classFile;
    private final ClassFileVersion classFileVersion;
    private final BlockIdentifierFactory blockIdentifierFactory;
    private final static JavaTypeInstance expectedLUTType = new JavaArrayTypeInstance(1, RawJavaType.INT);

    public SwitchEnumRewriter(DCCommonState dcCommonState, ClassFile classFile, BlockIdentifierFactory blockIdentifierFactory) {
        this.dcCommonState = dcCommonState;
        this.classFile = classFile;
        this.classFileVersion = classFile.getClassFileVersion();
        this.blockIdentifierFactory = blockIdentifierFactory;
    }

    @Override
    public void rewrite(Op04StructuredStatement root) {
        Options options = dcCommonState.getOptions();
        if (!options.getOption(OptionsImpl.ENUM_SWITCH, classFileVersion)) return;

        List<StructuredStatement> structuredStatements = MiscStatementTools.linearise(root);
        if (structuredStatements == null) return;

        rewriteMatchedEnumSwitches(
                Functional.filter(structuredStatements, new Predicate<StructuredStatement>() {
                    @Override
                    public boolean test(StructuredStatement in) {
                        return in.getClass() == StructuredSwitch.class;
                    }
                }),
                buildIndexedSwitchMatcher(new WildcardMatch()),
                false
        );

        // We also have the vanishingly unlikely but quite silly case of switching on a literal with no content.
        // See switchTest23
        rewriteMatchedEnumSwitches(
                Functional.filter(structuredStatements, new Predicate<StructuredStatement>() {
                    @Override
                    public boolean test(StructuredStatement in) {
                        return in.getClass() == StructuredExpressionStatement.class;
                    }
                }),
                buildBodylessIndexedSwitchMatcher(new WildcardMatch()),
                true
        );
    }

    private void rewriteMatchedEnumSwitches(List<StructuredStatement> candidateStatements,
                                            Matcher<StructuredStatement> matcher,
                                            boolean expression) {
        if (candidateStatements.isEmpty()) {
            return;
        }
        MatchIterator<StructuredStatement> iterator = new MatchIterator<StructuredStatement>(candidateStatements);
        SwitchEnumMatchResultCollector collector = new SwitchEnumMatchResultCollector();
        while (iterator.hasNext()) {
            iterator.advance();
            collector.clear();
            if (matcher.match(iterator, collector)) {
                rewriteMatchedEnumSwitch(collector, expression);
                iterator.rewind1();
            }
        }
    }

    private Matcher<StructuredStatement> buildIndexedSwitchMatcher(WildcardMatch wcm) {
        return new ResetAfterTest(wcm,
                new CollectMatch(MATCH_SWITCH, new StructuredSwitch(BytecodeLoc.NONE,
                        new ArrayIndex(BytecodeLoc.NONE,
                                wcm.getExpressionWildCard("lut"),
                                wcm.getMemberFunction("fncall", "ordinal", wcm.getExpressionWildCard("object"))),
                        null, wcm.getBlockIdentifier("block"))));
    }

    private Matcher<StructuredStatement> buildBodylessIndexedSwitchMatcher(WildcardMatch wcm) {
        return new ResetAfterTest(wcm,
                new CollectMatch(MATCH_BODYLESS_SWITCH, new StructuredExpressionStatement(
                        BytecodeLoc.NONE,
                        new ArrayIndex(BytecodeLoc.NONE,
                                wcm.getExpressionWildCard("lut"),
                                wcm.getMemberFunction("fncall", "ordinal", wcm.getExpressionWildCard("object"))),
                        true)));
    }


    /*

     we expect the 'foreign class' to have a static initialiser which looks much like this.

     static void <clinit>()
{
    EnumSwitchTest1$1.$SwitchMap$org$benf$cfr$tests$EnumSwitchTest1$enm = new int[EnumSwitchTest1$enm.values().length];

    try {
        EnumSwitchTest1$1.$SwitchMap$org$benf$cfr$tests$EnumSwitchTest1$enm[EnumSwitchTest1$enm.ONE.ordinal()] = 1;
    }
    catch (NoSuchFieldError unnamed_local_ex_0) {
    }
    try {
        EnumSwitchTest1$1.$SwitchMap$org$benf$cfr$tests$EnumSwitchTest1$enm[EnumSwitchTest1$enm.TWO.ordinal()] = 2;
    }
    catch (NoSuchFieldError unnamed_local_ex_0) {
    }
}

     */

    private void rewriteMatchedEnumSwitch(SwitchEnumMatchResultCollector matchResultCollector, boolean expression) {
        LookupRewritePlan plan = createLookupRewritePlan(matchResultCollector);
        if (plan == null) {
            return;
        }
        if (!rewriteLookupSwitch(matchResultCollector, expression, plan.enumObject, plan.lookupTable, plan.lookupStatements, plan.sharedWildcardMatch)) {
            return;
        }
        applyLookupCleanup(plan);
    }

    private LookupRewritePlan createLookupRewritePlan(SwitchEnumMatchResultCollector matchResultCollector) {
        Expression lookup = matchResultCollector.getLookupTable();
        if (lookup instanceof LValueExpression) {
            return createJavacLookupRewritePlan(matchResultCollector.getEnumObject(), ((LValueExpression) lookup).getLValue());
        }
        if (lookup instanceof StaticFunctionInvokation) {
            return createEclipseLookupRewritePlan(matchResultCollector.getEnumObject(), (StaticFunctionInvokation) lookup);
        }
        return null;
    }

    private LookupRewritePlan createEclipseLookupRewritePlan(Expression enumObject, StaticFunctionInvokation lookupFn) {
        Literal literalValue = enumObject.getComputedLiteral(MapFactory.<LValue, Literal>newMap());
        boolean enumObjectIsNullLiteral = Literal.NULL.equals(literalValue);
        if (lookupFn.getClazz() != this.classFile.getClassType()) {
            return null;
        }
        if (!lookupFn.getArgs().isEmpty()) {
            return null;
        }

        Method lookupMethod = resolveUniqueNoArgMethod(lookupFn.getName());
        if (lookupMethod == null || !isSyntheticStaticLookupMethod(lookupMethod)) {
            return null;
        }
        if (!lookupMethod.getMethodPrototype().getReturnType().equals(expectedLUTType)) {
            return null;
        }

        List<StructuredStatement> lookupStatements = getLookupMethodStatements(lookupMethod);
        if (lookupStatements == null) return null;

        WildcardMatch sharedWildcardMatch = new WildcardMatch();
        EclipseVarResultCollector assignment = matchEclipseLookupAssignment(lookupStatements, enumObject, enumObjectIsNullLiteral, sharedWildcardMatch);
        if (assignment == null) {
            return null;
        }

        ClassFileField hiddenField = getSyntheticStaticLookupField(assignment.field);
        if (hiddenField == null) {
            return null;
        }

        Expression rewrittenEnumObject = enumObject;
        if (enumObjectIsNullLiteral) {
            JavaTypeInstance enumType = assignment.arrayLen.getClazz();
            rewrittenEnumObject = new CastExpression(BytecodeLoc.NONE, new InferredJavaType(enumType, InferredJavaType.Source.TRANSFORM), enumObject);
        }

        return new LookupRewritePlan(rewrittenEnumObject, assignment.lookup, lookupStatements, sharedWildcardMatch, hiddenField, lookupMethod, null);
    }

    private static class EclipseVarResultCollector implements MatchResultCollector {
        LValue lookup;
        LValue field;
        StaticFunctionInvokation arrayLen;

        @Override
        public void clear() {
            lookup = null;
            field = null;
            arrayLen = null;
        }

        @Override
        public void collectStatement(String name, StructuredStatement statement) {

        }

        @Override
        public void collectMatches(String name, WildcardMatch wcm) {
            lookup = wcm.getLValueWildCard("lookup").getMatch();
            field = wcm.getLValueWildCard("static").getMatch();
            arrayLen = wcm.getStaticFunction("func").getMatch();
        }
    }

    private static class LookupRewritePlan {
        private final Expression enumObject;
        private final LValue lookupTable;
        private final List<StructuredStatement> lookupStatements;
        private final WildcardMatch sharedWildcardMatch;
        private final ClassFileField hiddenField;
        private final Method hiddenMethod;
        private final ClassFile hiddenInnerClass;

        private LookupRewritePlan(Expression enumObject,
                                  LValue lookupTable,
                                  List<StructuredStatement> lookupStatements,
                                  WildcardMatch sharedWildcardMatch,
                                  ClassFileField hiddenField,
                                  Method hiddenMethod,
                                  ClassFile hiddenInnerClass) {
            this.enumObject = enumObject;
            this.lookupTable = lookupTable;
            this.lookupStatements = lookupStatements;
            this.sharedWildcardMatch = sharedWildcardMatch;
            this.hiddenField = hiddenField;
            this.hiddenMethod = hiddenMethod;
            this.hiddenInnerClass = hiddenInnerClass;
        }
    }

    private LookupRewritePlan createJavacLookupRewritePlan(Expression enumObject, LValue lookupTable) {
        if (!(lookupTable instanceof StaticVariable)) {
            return null;
        }

        StaticVariable staticLookupTable = (StaticVariable) lookupTable;
        ClassFile enumLookupOwner = resolveLookupOwnerClass(staticLookupTable);
        if (enumLookupOwner == null) {
            return null;
        }
        if (!hasExpectedLookupField(enumLookupOwner, staticLookupTable)) {
            return null;
        }

        Method lutStaticInit;
        try {
            lutStaticInit = enumLookupOwner.getMethodByName("<clinit>").get(0);
        } catch (NoSuchMethodException e) {
            return null;
        }
        List<StructuredStatement> lookupStatements = getLookupMethodStatements(lutStaticInit);
        if (lookupStatements == null) return null;
        return new LookupRewritePlan(enumObject, lookupTable, lookupStatements, null, null, null, enumLookupOwner);
    }

    private Method resolveUniqueNoArgMethod(String name) {
        try {
            List<Method> methods = this.classFile.getMethodByName(name);
            if (methods.size() != 1) {
                return null;
            }
            Method method = methods.get(0);
            return method.getMethodPrototype().getArgs().isEmpty() ? method : null;
        } catch (NoSuchMethodException ignore) {
            return null;
        }
    }

    private boolean isSyntheticStaticLookupMethod(Method method) {
        return method.testAccessFlag(AccessFlagMethod.ACC_SYNTHETIC)
                && method.testAccessFlag(AccessFlagMethod.ACC_STATIC);
    }

    private EclipseVarResultCollector matchEclipseLookupAssignment(List<StructuredStatement> lookupStatements,
                                                                  Expression enumObject,
                                                                  boolean enumObjectIsNullLiteral,
                                                                  WildcardMatch wcm) {
        JavaTypeInstance enumType = enumObjectIsNullLiteral ? null : enumObject.getInferredJavaType().getJavaTypeInstance();
        Matcher<StructuredStatement> matcher =
                new ResetAfterTest(wcm,
                        new MatchSequence(
                                new MatchOneOf(
                                        new MatchSequence(
                                                new StructuredAssignment(BytecodeLoc.NONE, wcm.getLValueWildCard("res"), new LValueExpression(wcm.getLValueWildCard("static"))),
                                                new StructuredIf(BytecodeLoc.NONE, new ComparisonOperation(BytecodeLoc.TODO, new LValueExpression(wcm.getLValueWildCard("res")), Literal.NULL,CompOp.NE), null),
                                                new BeginBlock(null),
                                                new StructuredReturn(BytecodeLoc.NONE, new LValueExpression(wcm.getLValueWildCard("res")), null),
                                                new EndBlock(null)
                                        ),
                                        new MatchSequence(
                                                new StructuredIf(BytecodeLoc.NONE, new ComparisonOperation(BytecodeLoc.TODO, new LValueExpression(wcm.getLValueWildCard("static")), Literal.NULL,CompOp.NE), null),
                                                new BeginBlock(null),
                                                new StructuredReturn(BytecodeLoc.NONE, new LValueExpression(wcm.getLValueWildCard("static")), null),
                                                new EndBlock(null)
                                        )
                                ),
                                new StructuredAssignment(BytecodeLoc.NONE, wcm.getLValueWildCard("lookup"), new NewPrimitiveArray(BytecodeLoc.TODO,
                                        new ArrayLength(BytecodeLoc.NONE, wcm.getStaticFunction("func", enumType, null, "values")),
                                        RawJavaType.INT)))
                );
        MatchIterator<StructuredStatement> iterator = new MatchIterator<StructuredStatement>(lookupStatements);
        EclipseVarResultCollector collector = new EclipseVarResultCollector();
        while (iterator.hasNext()) {
            iterator.advance();
            collector.clear();
            if (matcher.match(iterator, collector)) {
                return collector;
            }
        }
        return null;
    }

    private ClassFileField getSyntheticStaticLookupField(LValue fieldLValue) {
        if (!(fieldLValue instanceof StaticVariable)) {
            return null;
        }
        StaticVariable staticVariable = (StaticVariable) fieldLValue;
        ClassFileField classFileField = staticVariable.getClassFileField();
        Field field = classFileField.getField();
        if (!field.testAccessFlag(AccessFlag.ACC_SYNTHETIC) || !field.testAccessFlag(AccessFlag.ACC_STATIC)) {
            return null;
        }
        return classFileField;
    }

    private ClassFile resolveLookupOwnerClass(StaticVariable staticLookupTable) {
        try {
            return dcCommonState.getClassFile(staticLookupTable.getOwningClassType());
        } catch (CannotLoadClassException e) {
            return null;
        }
    }

    private boolean hasExpectedLookupField(ClassFile enumLookupOwner, StaticVariable staticLookupTable) {
        try {
            Field field = enumLookupOwner
                    .getFieldByName(staticLookupTable.getFieldName(), staticLookupTable.getInferredJavaType().getJavaTypeInstance())
                    .getField();
            return field.getJavaTypeInstance().equals(expectedLUTType);
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    private void applyLookupCleanup(LookupRewritePlan plan) {
        if (plan.hiddenField != null) {
            plan.hiddenField.markHidden();
        }
        if (plan.hiddenMethod != null) {
            plan.hiddenMethod.hideSynthetic();
        }
        if (plan.hiddenInnerClass != null) {
            plan.hiddenInnerClass.markHiddenInnerClass();
        }
    }

    private boolean rewriteLookupSwitch(SwitchEnumMatchResultCollector mrc,
                                        boolean expression,
                                        Expression enumObject,
                                        LValue lookupTable,
                                        List<StructuredStatement> structuredStatements,
                                        WildcardMatch sharedWildcardMatch) {
        WildcardMatch caseMatch = new WildcardMatch();
        Matcher<StructuredStatement> matcher = buildEnumLookupMatcher(lookupTable, enumObject, sharedWildcardMatch, caseMatch);
        SwitchForeignEnumMatchResultCollector matchResultCollector = new SwitchForeignEnumMatchResultCollector(caseMatch);
        if (!matchLookupInitialisation(structuredStatements, matcher, matchResultCollector)) {
            return false;
        }
        return rewriteIndexedSwitch(mrc, expression, enumObject, matchResultCollector);
    }

    private Matcher<StructuredStatement> buildEnumLookupMatcher(LValue lookupTable,
                                                                Expression enumObject,
                                                                WildcardMatch sharedWildcardMatch,
                                                                WildcardMatch caseMatch) {
        WildcardMatch lookupArrayMatch = sharedWildcardMatch == null ? new WildcardMatch() : sharedWildcardMatch;
        return new ResetAfterTest(lookupArrayMatch,
                new MatchSequence(
                        new StructuredAssignment(BytecodeLoc.NONE, lookupTable, new NewPrimitiveArray(BytecodeLoc.TODO,
                                new ArrayLength(BytecodeLoc.NONE, lookupArrayMatch.getStaticFunction("func", enumObject.getInferredJavaType().getJavaTypeInstance(), null, "values")),
                                RawJavaType.INT)),
                        getEnumSugarKleeneStar(lookupTable, enumObject, caseMatch)
                )
        );
    }

    private boolean matchLookupInitialisation(List<StructuredStatement> structuredStatements,
                                              Matcher<StructuredStatement> matcher,
                                              SwitchForeignEnumMatchResultCollector collector) {
        MatchIterator<StructuredStatement> iterator = new MatchIterator<StructuredStatement>(structuredStatements);
        while (iterator.hasNext()) {
            iterator.advance();
            collector.clear();
            if (matcher.match(iterator, collector)) {
                return true;
            }
        }
        return false;
    }

    private boolean rewriteIndexedSwitch(SwitchEnumMatchResultCollector mrc, boolean expression, Expression enumObject, SwitchForeignEnumMatchResultCollector matchResultCollector) {
        Map<Integer, StaticVariable> reverseLut = matchResultCollector.getLUT();
        /*
         * Now, we rewrite the statement in the FIRST match (i.e in our original source file)
         * to use the reverse map of integer to enum value.
         *
         * We can only do the rewrite if ALL the entries in the case list are in the map we found above.
         */
        StructuredStatement structuredStatement;
        StructuredSwitch newSwitch;
        if (!expression) {
            StructuredSwitch structuredSwitch = mrc.getStructuredSwitch();
            structuredStatement = structuredSwitch;
            Op04StructuredStatement switchBlock = structuredSwitch.getBody();
            StructuredStatement switchBlockStatement = switchBlock.getStatement();
            if (!(switchBlockStatement instanceof Block)) {
                throw new IllegalStateException("Inside switch should be a block");
            }

            Block block = (Block) switchBlockStatement;
            List<Op04StructuredStatement> caseStatements = block.getBlockStatements();

            /*
             * If we can match every one of the ordinals, we replace the statement.
             */
            LinkedList<Op04StructuredStatement> newBlockContent = ListFactory.newLinkedList();
            InferredJavaType inferredJavaType = enumObject.getInferredJavaType();
            for (Op04StructuredStatement caseOuter : caseStatements) {
                StructuredStatement caseInner = caseOuter.getStatement();
                if (!(caseInner instanceof StructuredCase)) {
                    return true;
                }
                StructuredCase caseStmt = (StructuredCase) caseInner;
                List<Expression> values = caseStmt.getValues();
                List<Expression> newValues = ListFactory.newList();
                for (Expression value : values) {
                    Integer iVal = getIntegerFromLiteralExpression(value);
                    if (iVal == null) {
                        return false;
                    }
                    StaticVariable enumVal = reverseLut.get(iVal);
                    if (enumVal == null) {
                        return false;
                    }
                    newValues.add(new LValueExpression(enumVal));
                }
                StructuredCase replacement = new StructuredCase(BytecodeLoc.TODO, newValues, inferredJavaType, caseStmt.getBody(), caseStmt.getBlockIdentifier(), true);
                newBlockContent.add(new Op04StructuredStatement(replacement));
            }
            Block replacementBlock = new Block(newBlockContent, block.isIndenting());
            newSwitch = new StructuredSwitch(
                    BytecodeLoc.TODO,
                    enumObject,
                    new Op04StructuredStatement(replacementBlock),
                    structuredSwitch.getBlockIdentifier());
        } else {
            structuredStatement = mrc.getStructuredExpressionStatement();
            LinkedList<Op04StructuredStatement> tmp = new LinkedList<Op04StructuredStatement>();
            tmp.add(new Op04StructuredStatement(new StructuredComment("Empty switch")));
            newSwitch = new StructuredSwitch(
                    BytecodeLoc.TODO,
                    enumObject,
                    new Op04StructuredStatement(new Block(tmp, true)),
                    blockIdentifierFactory.getNextBlockIdentifier(BlockType.SWITCH));
        }


        structuredStatement.getContainer().replaceStatement(newSwitch);
        return true;
    }

    private KleeneStar getEnumSugarKleeneStar(LValue lookupTable, Expression enumObject, WildcardMatch wcm) {
        return new KleeneStar(new ResetAfterTest(wcm,
                new MatchSequence(
                        new StructuredTry(null, null),
                        new BeginBlock(null),
                        new StructuredAssignment(
                                BytecodeLoc.NONE,
                                new ArrayVariable(
                                        new ArrayIndex(BytecodeLoc.NONE,
                                                new LValueExpression(lookupTable),
                                                wcm.getMemberFunction("ordinal", "ordinal",
                                                        new LValueExpression(
                                                                wcm.getStaticVariable("enumval", enumObject.getInferredJavaType().getJavaTypeInstance(), enumObject.getInferredJavaType())
                                                        )
                                                )
                                        )
                                ),
                                wcm.getExpressionWildCard("literal")
                        ),
                        new EndBlock(null),
                        new StructuredCatch(null, null, null, null),
                        new BeginBlock(null),
                        new EndBlock(null)
                )
        ));
    }

    private List<StructuredStatement> getLookupMethodStatements(Method lutStaticInit) {
        Op04StructuredStatement lutStaticInitCode = lutStaticInit.getAnalysis();

        List<StructuredStatement> structuredStatements = MiscStatementTools.linearise(lutStaticInitCode);
        if (structuredStatements == null) return null;
        // Filter out the comments.
        structuredStatements = Functional.filter(structuredStatements, new Predicate<StructuredStatement>() {
            @Override
            public boolean test(StructuredStatement in) {
                return !(in instanceof StructuredComment);
            }
        });
        return structuredStatements;
    }

    private Integer getIntegerFromLiteralExpression(Expression exp) {
        if (!(exp instanceof Literal)) {
            return null;
        }
        Literal literal = (Literal) exp;
        TypedLiteral typedLiteral = literal.getValue();
        if (typedLiteral.getType() != TypedLiteral.LiteralType.Integer) {
            return null;
        }
        return (Integer) typedLiteral.getValue();
    }

    private static class SwitchEnumMatchResultCollector extends AbstractMatchResultIterator {

        private Expression lookupTable;
        private Expression enumObject;
        private StructuredSwitch structuredSwitch;
        private StructuredExpressionStatement structuredExpressionStatement;

        private SwitchEnumMatchResultCollector() {
        }

        @Override
        public void clear() {
            lookupTable = null;
            enumObject = null;
            structuredSwitch = null;
            structuredExpressionStatement = null;
        }

        @Override
        public void collectStatement(String name, StructuredStatement statement) {
            if (name.equals(MATCH_SWITCH)) {
                structuredSwitch = (StructuredSwitch) statement;
            } else if (name.equals(MATCH_BODYLESS_SWITCH)) {
                structuredExpressionStatement = (StructuredExpressionStatement)statement;
            }
        }

        @Override
        public void collectMatches(String name, WildcardMatch wcm) {
            lookupTable = wcm.getExpressionWildCard("lut").getMatch();
            enumObject = wcm.getExpressionWildCard("object").getMatch();
        }

        Expression getLookupTable() {
            return lookupTable;
        }

        Expression getEnumObject() {
            return enumObject;
        }

        StructuredSwitch getStructuredSwitch() {
            return structuredSwitch;
        }

        StructuredExpressionStatement getStructuredExpressionStatement() {
            return structuredExpressionStatement;
        }
    }

    private class SwitchForeignEnumMatchResultCollector extends AbstractMatchResultIterator {
        private final WildcardMatch wcmCase;
        private final Map<Integer, StaticVariable> lutValues = MapFactory.newMap();

        private SwitchForeignEnumMatchResultCollector(WildcardMatch wcmCase) {
            this.wcmCase = wcmCase;
        }

        Map<Integer, StaticVariable> getLUT() {
            return lutValues;
        }

        @Override
        public void clear() {
            lutValues.clear();
        }

        @Override
        public void collectMatches(String name, WildcardMatch wcm) {
            if (wcm == wcmCase) {
                StaticVariable staticVariable = wcm.getStaticVariable("enumval").getMatch();

                Expression exp = wcm.getExpressionWildCard("literal").getMatch();
                Integer literalInt = getIntegerFromLiteralExpression(exp);
                if (literalInt == null) {
                    return;
                }
                lutValues.put(literalInt, staticVariable);
            }
        }
    }
}
