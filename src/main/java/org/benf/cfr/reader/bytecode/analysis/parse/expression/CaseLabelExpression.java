package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import org.benf.cfr.reader.bytecode.analysis.parse.LValue;

import java.util.List;

public interface CaseLabelExpression {
    List<LValue> getCreatedLValues();

    boolean matches(LValue lValue);
}
