package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.*;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.*;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredFinally;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredIf;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.BeginBlock;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.EndBlock;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.AccessFlagMethod;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;

import java.util.List;

public class TryResourcesTransformerJ9 extends TryResourceTransformerFinally {
    public TryResourcesTransformerJ9(ClassFile classFile) {
        super(classFile);
    }

    @Override
    protected ResourceMatch findResourceFinally(Op04StructuredStatement finallyBlock) {
        if (finallyBlock == null) return null;
        StructuredFinally finalli = (StructuredFinally)finallyBlock.getStatement();
        Op04StructuredStatement content = finalli.getCatchBlock();

        WildcardMatch wcm = new WildcardMatch();
        List<StructuredStatement> structuredStatements = linearizeStatements(content);
        if (structuredStatements == null) return null;

        Matcher<StructuredStatement> m = buildFinallyResourceReleaseMatcher(wcm);
        TryResourcesMatchResultCollector collector = new TryResourcesMatchResultCollector();
        boolean res = matchesStatements(structuredStatements, 1, m, collector);
        if (!res) return null;

        Method resourceMethod = resolveFakeEndResourceMethod(collector);
        if (resourceMethod == null) return null;

        return new ResourceMatch(resourceMethod, collector.resource, collector.throwable);
    }

    private Matcher<StructuredStatement> buildFinallyResourceReleaseMatcher(WildcardMatch wcm) {
        return new ResetAfterTest(wcm, buildOptionalGuardedBlockMatcher(wcm.getLValueWildCard("resource"), buildBlockResourceReleaseMatcher(wcm)));
    }

    private Matcher<StructuredStatement> buildBlockResourceReleaseMatcher(WildcardMatch wcm) {
        return new MatchSequence(
                new BeginBlock(null),
                buildFakeEndResourceInvocationMatcher(wcm),
                new EndBlock(null)
        );
    }

    private MatchOneOf buildFakeEndResourceInvocationMatcher(WildcardMatch wcm) {
        InferredJavaType inferredThrowable = new InferredJavaType(TypeConstants.THROWABLE, InferredJavaType.Source.LITERAL, true);
        InferredJavaType inferredAutoclosable = new InferredJavaType(TypeConstants.AUTO_CLOSEABLE, InferredJavaType.Source.LITERAL, true);
        JavaTypeInstance clazzType = getClassFile().getClassType();
        return new MatchOneOf(
                new StructuredExpressionStatement(BytecodeLoc.NONE,
                        wcm.getStaticFunction("fn", clazzType, RawJavaType.VOID, null,
                                new CastExpression(BytecodeLoc.NONE, inferredThrowable, new LValueExpression(wcm.getLValueWildCard("throwable"))),
                                new CastExpression(BytecodeLoc.NONE, inferredAutoclosable, new LValueExpression(wcm.getLValueWildCard("resource")))),
                        false),
                new StructuredExpressionStatement(BytecodeLoc.NONE,
                        wcm.getStaticFunction("fn2", clazzType, RawJavaType.VOID, null,
                                new LValueExpression(wcm.getLValueWildCard("throwable")),
                                new CastExpression(BytecodeLoc.NONE, inferredAutoclosable, new LValueExpression(wcm.getLValueWildCard("resource")))),
                        false),
                new StructuredExpressionStatement(BytecodeLoc.NONE,
                        wcm.getStaticFunction("fn3", clazzType, RawJavaType.VOID, null,
                                new LValueExpression(wcm.getLValueWildCard("throwable")),
                                new LValueExpression(wcm.getLValueWildCard("resource"))),
                        false)
        );
    }

    private Method resolveFakeEndResourceMethod(TryResourcesMatchResultCollector collector) {
        MethodPrototype prototype = collector.fn.getMethodPrototype();
        Method resourceMethod = getClassFile().getMethodByPrototypeOrNull(prototype);
        if (resourceMethod == null) {
            return null;
        }
        return resourceMethod.getAccessFlags().contains(AccessFlagMethod.ACC_FAKE_END_RESOURCE) ? resourceMethod : null;
    }

}
