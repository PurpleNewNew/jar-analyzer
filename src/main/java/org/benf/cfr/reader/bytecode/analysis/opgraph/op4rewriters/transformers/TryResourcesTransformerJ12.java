package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.ResourceReleaseDetector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.*;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.*;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.BeginBlock;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.EndBlock;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.functors.Predicate;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/*
 * Java 12 generates significantly smaller resource blocks for statements where
 * no actual finally statement is necessary.
 *
 *     public void testEnhancedTryEmpty() throws IOException {
        StringWriter writer = new StringWriter();
        try {
            writer.write("This is only a test.");
        }
        catch (Throwable throwable) {
            try {
                writer.close();
            }
            catch (Throwable throwable2) {
                throwable.addSuppressed(throwable2);
            }
            throw throwable;
        }
        writer.close(); <-- note, this is originally BEFORE the catch
                            block, but not covered by the try, so moved out.
    }
 */
public class TryResourcesTransformerJ12 extends TryResourcesTransformerBase {
    public TryResourcesTransformerJ12(ClassFile classFile) {
        super(classFile);
    }


    @Override
    protected boolean rewriteTry(StructuredTry structuredTry, StructuredScope scope, ResourceMatch resourceMatch) {
        if (!super.rewriteTry(structuredTry, scope, resourceMatch)) return false;
        structuredTry.getCatchBlocks().clear();
        if (resourceMatch.replacementCatchBlocks != null && !resourceMatch.replacementCatchBlocks.isEmpty()) {
            structuredTry.getCatchBlocks().addAll(resourceMatch.replacementCatchBlocks);
        }
        for (Op04StructuredStatement remove : resourceMatch.removeThese) {
            remove.nopOut();
        }
        return true;
    }

    @Override
    protected ResourceMatch getResourceMatch(StructuredTry structuredTry, StructuredScope scope) {
        if (structuredTry.getFinallyBlock() == null) {
            return getComplexResourceMatch(structuredTry, scope);
        } else {
            return getSimpleResourceMatch(structuredTry, scope);
        }
    }

    private ResourceMatch getSimpleResourceMatch(StructuredTry structuredTry, StructuredScope scope) {
        Op04StructuredStatement finallyBlock = structuredTry.getFinallyBlock();

        WildcardMatch wcm = new WildcardMatch();
        WildcardMatch.LValueWildcard throwableLValue = wcm.getLValueWildCard("throwable");
        WildcardMatch.LValueWildcard autoclose = wcm.getLValueWildCard("resource");
        TryResourcesMatchResultCollector collector = matchTryResourcesPattern(
                finallyBlock,
                2,
                wcm,
                wrapBlockMatcher(ResourceReleaseDetector.buildSimpleStructuredStatementMatcher(wcm, throwableLValue, autoclose))
        );
        if (collector == null) return null;
        return new ResourceMatch(null, collector.resource, collector.throwable, false, Collections.<Op04StructuredStatement>emptyList());
    }

    private ResourceMatch getComplexResourceMatch(StructuredTry structuredTry, StructuredScope scope) {
        if (structuredTry.getCatchBlocks().size() != 1) return null;
        Op04StructuredStatement catchBlock = structuredTry.getCatchBlocks().get(0);
        StructuredStatement catchStm = catchBlock.getStatement();
        if (!(catchStm instanceof StructuredCatch)) return null;
        StructuredCatch catchStatement = (StructuredCatch)catchStm;

        if (catchStatement.getCatchTypes().size() != 1) return null;
        JavaTypeInstance caughtType = catchStatement.getCatchTypes().get(0);
        if (!TypeConstants.THROWABLE.equals(caughtType)) return null;

        MatchedResourceRelease directMatch = matchNonTestingResourceRelease(catchBlock, 2);
        if (directMatch == null) {
            return getWrappedComplexResourceMatch(structuredTry, scope, catchStatement);
        }

        List<Op04StructuredStatement> toRemove = findCloseStatements(structuredTry, scope, directMatch.wcm, directMatch.collector);
        if (toRemove == null) return null;
        return new ResourceMatch(null, directMatch.collector.resource, directMatch.collector.throwable, false, toRemove);
    }

    private List<Op04StructuredStatement> findTrailingCloseStatementInTry(StructuredTry structuredTry, WildcardMatch wcm, TryResourcesMatchResultCollector collector) {
        Op04StructuredStatement tryb = structuredTry.getTryBlock();
        StructuredStatement tryStm = tryb.getStatement();
        if (!(tryStm instanceof Block)) return null;
        Block block = (Block)tryStm;
        Op04StructuredStatement lastInBlock = block.getLast();
        if (matchesCloseStatement(wcm, collector, lastInBlock.getStatement())) {
            return Collections.singletonList(lastInBlock);
        }
        return null;
    }

    private List<Op04StructuredStatement> findCloseStatementsAfterTry(StructuredTry structuredTry, StructuredScope scope, WildcardMatch wcm, TryResourcesMatchResultCollector collector) {
        Set<Op04StructuredStatement> next = scope.getNextFallThrough(structuredTry);

        List<Op04StructuredStatement> toRemove = Functional.filter(next, new Predicate<Op04StructuredStatement>() {
            @Override
            public boolean test(Op04StructuredStatement in) {
                return !(in.getStatement() instanceof StructuredComment);
            }
        });
        if (toRemove.size() != 1) return null;

        StructuredStatement statement = toRemove.get(0).getStatement();

        if (matchesCloseStatement(wcm, collector, statement)) {
            return toRemove;
        }
        return null;
    }

    private List<Op04StructuredStatement> findCloseStatements(StructuredTry structuredTry,
                                                              StructuredScope scope,
                                                              WildcardMatch wcm,
                                                              TryResourcesMatchResultCollector collector) {
        List<Op04StructuredStatement> toRemove = findCloseStatementsAfterTry(structuredTry, scope, wcm, collector);
        if (toRemove != null) {
            return toRemove;
        }
        return findTrailingCloseStatementInTry(structuredTry, wcm, collector);
    }

    private ResourceMatch getWrappedComplexResourceMatch(StructuredTry structuredTry,
                                                         StructuredScope scope,
                                                         StructuredCatch outerCatch) {
        StructuredTry wrappedTry = getSingleWrappedTry(outerCatch);
        if (wrappedTry == null) return null;

        MatchedResourceRelease wrappedMatch = matchNonTestingResourceRelease(wrappedTry.getTryBlock(), 1);
        if (wrappedMatch == null) return null;

        List<Op04StructuredStatement> toRemove = findCloseStatements(structuredTry, scope, wrappedMatch.wcm, wrappedMatch.collector);
        if (toRemove == null) return null;
        return new ResourceMatch(
                null,
                wrappedMatch.collector.resource,
                wrappedMatch.collector.throwable,
                false,
                toRemove,
                wrappedTry.getCatchBlocks()
        );
    }

    private StructuredTry getSingleWrappedTry(StructuredCatch outerCatch) {
        Op04StructuredStatement catchBlock = outerCatch.getCatchBlock();
        StructuredStatement catchBlockStatement = catchBlock.getStatement();
        if (!(catchBlockStatement instanceof Block)) {
            return null;
        }
        Block block = (Block) catchBlockStatement;
        org.benf.cfr.reader.util.Optional<Op04StructuredStatement> maybeOnly = block.getMaybeJustOneStatement();
        if (!maybeOnly.isSet()) {
            return null;
        }
        StructuredStatement onlyStatement = maybeOnly.getValue().getStatement();
        if (!(onlyStatement instanceof StructuredTry)) {
            return null;
        }
        StructuredTry wrappedTry = (StructuredTry) onlyStatement;
        return wrappedTry.getCatchBlocks().isEmpty() ? null : wrappedTry;
    }

    private MatchedResourceRelease matchNonTestingResourceRelease(Op04StructuredStatement statement, int advanceCount) {
        WildcardMatch wcm = new WildcardMatch();
        WildcardMatch.LValueWildcard throwableLValue = wcm.getLValueWildCard("throwable");
        WildcardMatch.LValueWildcard autoclose = wcm.getLValueWildCard("resource");
        TryResourcesMatchResultCollector collector = matchTryResourcesPattern(
                statement,
                advanceCount,
                wcm,
                wrapBlockMatcher(ResourceReleaseDetector.buildNonTestingStructuredStatementMatcher(wcm, throwableLValue, autoclose))
        );
        if (collector == null) {
            return null;
        }
        return new MatchedResourceRelease(wcm, collector);
    }

    private static class MatchedResourceRelease {
        private final WildcardMatch wcm;
        private final TryResourcesMatchResultCollector collector;

        private MatchedResourceRelease(WildcardMatch wcm, TryResourcesMatchResultCollector collector) {
            this.wcm = wcm;
            this.collector = collector;
        }
    }
}
