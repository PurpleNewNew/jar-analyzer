package org.benf.cfr.reader.bytecode.analysis.parse.pattern;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.List;

public interface MatchPattern {
    InferredJavaType getInferredJavaType();

    BytecodeLoc getCombinedLoc();

    void collectTypeUsages(TypeUsageCollector collector);

    MatchPattern deepClone(CloneHelper cloneHelper);

    Dumper dump(Dumper d);

    MatchPattern replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer);

    MatchPattern applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags);

    void collectUsedLValues(LValueUsageCollector lValueUsageCollector);

    List<LValue> getCreatedLValues();

    boolean matches(LValue lValue);

    boolean equivalentUnder(MatchPattern other, EquivalenceConstraint constraint);
}
