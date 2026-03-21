package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.ResourceReleaseDetector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.*;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.*;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.*;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.BeginBlock;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.EndBlock;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.collections.SetUtil;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * Java 9 has made try-with-resources much cleaner.
 */
public abstract class TryResourcesTransformerBase implements StructuredStatementTransformer {

    private final ClassFile classFile;
    private final Map<Op04StructuredStatement, List<StructuredStatement>> linearizedStatements = new IdentityHashMap<Op04StructuredStatement, List<StructuredStatement>>();
    private boolean success = false;

    TryResourcesTransformerBase(ClassFile classFile) {
        this.classFile = classFile;
    }

    public boolean transform(Op04StructuredStatement root) {
        success = false;
        linearizedStatements.clear();
        try {
            StructuredScope structuredScope = new StructuredScope();
            root.transform(this, structuredScope);
            return success;
        } finally {
            linearizedStatements.clear();
        }
    }

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        if (in instanceof StructuredTry) {
            StructuredTry structuredTry = (StructuredTry)in;
            ResourceMatch match = getResourceMatch(structuredTry, scope);
            if (match != null) {
                // Ok, now we have to find the initialisation of the closable.
                if (rewriteTry(structuredTry, scope, match)) {
                    success = true;
                }
            }
        }
        in.transformStructuredChildren(this, scope);
        return in;
    }

    protected abstract ResourceMatch getResourceMatch(StructuredTry structuredTry, StructuredScope scope);

    // Now we have to walk back from the try statement, finding the declaration of the resource.
    // the declaration MUST not be initialised from something which is subsequently used before the try statement.
    protected boolean rewriteTry(StructuredTry structuredTry, StructuredScope scope, ResourceMatch resourceMatch) {
        List<Op04StructuredStatement> preceding = scope.getPrecedingInblock(1, 2);
        // seatch backwards for a definition of resource.
        LValue resource = resourceMatch.resource;
        Op04StructuredStatement autoAssign = findAutoclosableAssignment(preceding, resource);
        if (autoAssign == null) return false;
        StructuredAssignment assign = (StructuredAssignment)autoAssign.getStatement();
        autoAssign.nopOut();
        structuredTry.setFinally(null);
        structuredTry.addResources(Collections.singletonList(new Op04StructuredStatement(assign)));
        if (resourceMatch.resourceMethod != null) {
            resourceMatch.resourceMethod.hideSynthetic();
        }
        if (resourceMatch.reprocessException) {
            return rewriteException(structuredTry, preceding);
        }
        return true;
    }

    // And if this looks like
    // Exception exception
    // try() {
    //   X
    // }  catch (Exception e) {
    //   exception = e;
    //   throw e;
    // }
    // Then we can remove everything except try() { X }.
    private boolean rewriteException(StructuredTry structuredTry, List<Op04StructuredStatement> preceding) {
        List<Op04StructuredStatement> catchBlocks = structuredTry.getCatchBlocks();
        if (catchBlocks.size() != 1) return false;
        Op04StructuredStatement catchBlock = catchBlocks.get(0);

        Op04StructuredStatement exceptionDeclare = null;
        LValue tempThrowable = null;
        for (int x=preceding.size()-1;x >= 0;--x) {
            Op04StructuredStatement stm = preceding.get(x);
            StructuredStatement structuredStatement = stm.getStatement();
            if (structuredStatement.isScopeBlock()) return false;
            if (structuredStatement instanceof StructuredAssignment) {
                StructuredAssignment ass = (StructuredAssignment)structuredStatement;
                LValue lvalue = ass.getLvalue();
                if (!ass.isCreator(lvalue)) return false;
                if (!ass.getRvalue().equals(Literal.NULL)) return false;
                if (!lvalue.getInferredJavaType().getJavaTypeInstance().equals(TypeConstants.THROWABLE)) return false;
                exceptionDeclare = stm;
                tempThrowable = lvalue;
                break;
            }
        }
        if (exceptionDeclare == null) return false;

        List<StructuredStatement> catchContent = ListFactory.newList();
        catchBlock.linearizeStatementsInto(catchContent);

        WildcardMatch wcm = new WildcardMatch();

        WildcardMatch.LValueWildcard exceptionWildCard = wcm.getLValueWildCard("exception");
        Matcher<StructuredStatement> matcher = new MatchSequence(
                new StructuredCatch(null, null, exceptionWildCard, null),
                new BeginBlock(null),
                new StructuredAssignment(BytecodeLoc.NONE, tempThrowable, new LValueExpression(exceptionWildCard)),
                new StructuredThrow(BytecodeLoc.NONE, new LValueExpression(exceptionWildCard)),
                new EndBlock(null)
        );

        if (!matchesStatements(catchContent, 1, matcher, new EmptyMatchResultCollector())) return false;
        LValue caught = wcm.getLValueWildCard("exception").getMatch();
        if (!caught.getInferredJavaType().getJavaTypeInstance().equals(TypeConstants.THROWABLE)) return false;

        exceptionDeclare.nopOut();
        structuredTry.clearCatchBlocks();
        return true;
    }

    private Op04StructuredStatement findAutoclosableAssignment(List<Op04StructuredStatement> preceding, LValue resource) {
        LValueUsageCheckingRewriter usages = new LValueUsageCheckingRewriter();
        for (int x=preceding.size()-1;x >= 0;--x) {
            Op04StructuredStatement stm = preceding.get(x);
            StructuredStatement structuredStatement = stm.getStatement();
            if (structuredStatement.isScopeBlock()) return null;
            if (structuredStatement instanceof StructuredAssignment) {
                StructuredAssignment structuredAssignment = (StructuredAssignment)structuredStatement;

                if (structuredAssignment.isCreator(resource)) {
                    // get all values used in this, check they were not subsequently used.
                    LValueUsageCheckingRewriter check = new LValueUsageCheckingRewriter();
                    structuredAssignment.rewriteExpressions(check);
                    if (SetUtil.hasIntersection(usages.used, check.used)) return null;
                    return stm;
                }
                structuredStatement.rewriteExpressions(usages);
            }
        }
        return null;
    }

    protected ClassFile getClassFile() {
        return classFile;
    }

    protected List<StructuredStatement> linearizeStatements(Op04StructuredStatement statement) {
        if (statement == null) return null;
        return linearizedStatements.computeIfAbsent(
                statement,
                key -> org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools.linearise(key)
        );
    }

    protected boolean matchesStatements(List<StructuredStatement> statements,
                                        int advanceCount,
                                        Matcher<StructuredStatement> matcher,
                                        MatchResultCollector collector) {
        if (statements == null) return false;
        MatchIterator<StructuredStatement> iterator = new MatchIterator<StructuredStatement>(statements);
        for (int idx = 0; idx < advanceCount; ++idx) {
            if (!iterator.hasNext()) {
                return false;
            }
            iterator.advance();
        }
        return matcher.match(iterator, collector);
    }

    protected boolean matchesCloseStatement(WildcardMatch wcm,
                                            TryResourcesMatchResultCollector collector,
                                            StructuredStatement statement) {
        Matcher<StructuredStatement> checkClose = ResourceReleaseDetector.buildCloseExpressionMatcher(wcm, new LValueExpression(collector.resource));
        return matchesStatements(Collections.singletonList(statement), 1, checkClose, new EmptyMatchResultCollector());
    }

    protected Matcher<StructuredStatement> wrapBlockMatcher(Matcher<StructuredStatement> matcher) {
        return new MatchSequence(
                new BeginBlock(null),
                matcher,
                new EndBlock(null)
        );
    }

    protected TryResourcesMatchResultCollector matchTryResourcesPattern(Op04StructuredStatement statement,
                                                                        int advanceCount,
                                                                        WildcardMatch wcm,
                                                                        Matcher<StructuredStatement> matcher) {
        List<StructuredStatement> structuredStatements = linearizeStatements(statement);
        if (structuredStatements == null) {
            return null;
        }
        TryResourcesMatchResultCollector collector = new TryResourcesMatchResultCollector();
        Matcher<StructuredStatement> resetMatcher = new ResetAfterTest(wcm, matcher);
        if (!matchesStatements(structuredStatements, advanceCount, resetMatcher, collector)) {
            return null;
        }
        return collector;
    }

    protected Matcher<StructuredStatement> buildOptionalGuardedBlockMatcher(LValue resource, Matcher<StructuredStatement> blockMatcher) {
        return new MatchOneOf(
                new MatchSequence(
                        new BeginBlock(null),
                        new StructuredIf(BytecodeLoc.NONE, new ComparisonOperation(BytecodeLoc.NONE, new LValueExpression(resource), Literal.NULL, CompOp.NE), null),
                        blockMatcher,
                        new EndBlock(null)
                ),
                blockMatcher
        );
    }

    private static class LValueUsageCheckingRewriter extends AbstractExpressionRewriter {
        final Set<LValue> used = SetFactory.newSet();
        @Override
        public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            used.add(lValue);
            return lValue;
        }
    }

    static class ResourceMatch
    {
        final Method resourceMethod;
        final LValue resource;
        final LValue throwable;
        final boolean reprocessException;
        final List<Op04StructuredStatement> removeThese;
        final List<Op04StructuredStatement> replacementCatchBlocks;

        ResourceMatch(Method resourceMethod, LValue resource, LValue throwable) {
            this(resourceMethod, resource, throwable,  true, null, null);
        }

        ResourceMatch(Method resourceMethod, LValue resource, LValue throwable, boolean reprocessException, List<Op04StructuredStatement> removeThese) {
            this(resourceMethod, resource, throwable, reprocessException, removeThese, null);
        }

        ResourceMatch(Method resourceMethod,
                      LValue resource,
                      LValue throwable,
                      boolean reprocessException,
                      List<Op04StructuredStatement> removeThese,
                      List<Op04StructuredStatement> replacementCatchBlocks) {
            this.resourceMethod = resourceMethod;
            this.resource = resource;
            this.throwable = throwable;
            this.reprocessException = reprocessException;
            this.removeThese = removeThese;
            this.replacementCatchBlocks = replacementCatchBlocks;
        }
    }

    protected static class TryResourcesMatchResultCollector implements MatchResultCollector {
        StaticFunctionInvokation fn;
        LValue resource;
        LValue throwable;

        @Override
        public void clear() {
            fn = null;
            throwable = resource = null;
        }

        @Override
        public void collectStatement(String name, StructuredStatement statement) {

        }

        private StaticFunctionInvokation getFn(WildcardMatch wcm, String name) {
            WildcardMatch.StaticFunctionInvokationWildcard staticFunction = wcm.getStaticFunction(name);
            if (staticFunction == null) return null;
            return staticFunction.getMatch();
        }

        @Override
        public void collectMatches(String name, WildcardMatch wcm) {
            fn = getFn(wcm, "fn");
            if (fn == null) {
                fn = getFn(wcm, "fn2");
            }
            if (fn == null) {
                fn = getFn(wcm, "fn3");
            }
            resource = wcm.getLValueWildCard("resource").getMatch();
            throwable = wcm.getLValueWildCard("throwable").getMatch();
        }
    }
}
