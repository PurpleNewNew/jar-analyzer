package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.ResourceReleaseDetector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.*;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.*;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.*;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.BeginBlock;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.EndBlock;
import org.benf.cfr.reader.entities.ClassFile;

import java.util.List;

public class TryResourcesTransformerJ7 extends TryResourceTransformerFinally {
    public TryResourcesTransformerJ7(ClassFile classFile) {
        super(classFile);
    }

    @Override
    protected ResourceMatch findResourceFinally(Op04StructuredStatement finallyBlock) {
        if (finallyBlock == null) return null;
        StructuredFinally finalli = (StructuredFinally)finallyBlock.getStatement();
        Op04StructuredStatement content = finalli.getCatchBlock();

        WildcardMatch wcm = new WildcardMatch();
        WildcardMatch.LValueWildcard throwableLValue = wcm.getLValueWildCard("throwable");
        WildcardMatch.LValueWildcard autoclose = wcm.getLValueWildCard("resource");
        TryResourcesMatchResultCollector collector = matchTryResourcesPattern(
                content,
                1,
                wcm,
                buildFinallyResourceReleaseMatcher(wcm, throwableLValue, autoclose)
        );
        if (collector == null) return null;

        LValue resource = collector.resource;
        LValue throwable = collector.throwable;

        // Because we don't have an explicit close method, we need to check types of arguments.
        // resource must cast back to AutoClosable.
        // except, prior to J9, closable didn't inherit from Autoclosable, so test for closable.
        return new ResourceMatch(null, resource, throwable);
    }

    private Matcher<StructuredStatement> buildFinallyResourceReleaseMatcher(WildcardMatch wcm,
                                                                            WildcardMatch.LValueWildcard throwableLValue,
                                                                            WildcardMatch.LValueWildcard autoclose) {
        Matcher<StructuredStatement> releaseMatcher = ResourceReleaseDetector.buildStructuredStatementMatcher(wcm, throwableLValue, autoclose);
        return buildOptionalGuardedBlockMatcher(autoclose, releaseMatcher);
    }
}
