package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

final class SwitchPatternCaseEmitter {
    void apply(SwitchPatternCasePlanner.CaseRewritePlan plan) {
        if (plan.getRewrittenValues() != null) {
            plan.getStructuredCase().setValues(plan.getRewrittenValues());
        }
        if (plan.getBodyBlock() != null && plan.getRewrittenStatements() != null) {
            plan.getBodyBlock().replaceBlockStatements(plan.getRewrittenStatements());
        }
    }
}
