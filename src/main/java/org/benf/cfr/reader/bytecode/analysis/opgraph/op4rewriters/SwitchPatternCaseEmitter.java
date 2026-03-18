package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import org.benf.cfr.reader.util.collections.ListFactory;

final class SwitchPatternCaseEmitter {
    void apply(SwitchPatternCasePlanner.CaseRewritePlan plan) {
        if (plan.getRewrittenValue() != null) {
            plan.getStructuredCase().setValues(ListFactory.newImmutableList(plan.getRewrittenValue()));
        }
        if (plan.getBodyBlock() != null && plan.getRewrittenStatements() != null) {
            plan.getBodyBlock().replaceBlockStatements(plan.getRewrittenStatements());
        }
    }
}
