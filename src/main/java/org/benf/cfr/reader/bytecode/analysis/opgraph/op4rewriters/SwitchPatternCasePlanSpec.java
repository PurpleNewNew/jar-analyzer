package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredCase;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;

import java.util.LinkedList;
import java.util.List;

final class SwitchPatternCasePlanSpec {
    enum Kind {
        UNCHANGED,
        NULL_LITERAL,
        BINDING,
        GUARDED_BINDING,
        RECORD
    }

    private final Kind kind;
    private final StructuredCase structuredCase;
    private final Block bodyBlock;
    private final LinkedList<Op04StructuredStatement> rewrittenStatements;
    private final JavaTypeInstance bindingType;
    private final LocalVariable binding;
    private final Expression guard;
    private final List<LValue> recordComponents;

    private SwitchPatternCasePlanSpec(Kind kind,
                                      StructuredCase structuredCase,
                                      Block bodyBlock,
                                      LinkedList<Op04StructuredStatement> rewrittenStatements,
                                      JavaTypeInstance bindingType,
                                      LocalVariable binding,
                                      Expression guard,
                                      List<LValue> recordComponents) {
        this.kind = kind;
        this.structuredCase = structuredCase;
        this.bodyBlock = bodyBlock;
        this.rewrittenStatements = rewrittenStatements;
        this.bindingType = bindingType;
        this.binding = binding;
        this.guard = guard;
        this.recordComponents = recordComponents;
    }

    static SwitchPatternCasePlanSpec unchanged(StructuredCase structuredCase) {
        return new SwitchPatternCasePlanSpec(Kind.UNCHANGED, structuredCase, null, null, null, null, null, null);
    }

    static SwitchPatternCasePlanSpec nullLiteral(StructuredCase structuredCase) {
        return new SwitchPatternCasePlanSpec(Kind.NULL_LITERAL, structuredCase, null, null, null, null, null, null);
    }

    static SwitchPatternCasePlanSpec binding(SwitchPatternCaseContext context,
                                             LocalVariable binding,
                                             LinkedList<Op04StructuredStatement> rewrittenStatements) {
        return new SwitchPatternCasePlanSpec(
                Kind.BINDING,
                context.getStructuredCase(),
                context.getBodyBlock(),
                rewrittenStatements,
                binding.getInferredJavaType().getJavaTypeInstance(),
                binding,
                null,
                null
        );
    }

    static SwitchPatternCasePlanSpec guardedBinding(SwitchPatternCaseContext context,
                                                    LocalVariable binding,
                                                    Expression guard,
                                                    LinkedList<Op04StructuredStatement> rewrittenStatements) {
        return new SwitchPatternCasePlanSpec(
                Kind.GUARDED_BINDING,
                context.getStructuredCase(),
                context.getBodyBlock(),
                rewrittenStatements,
                binding.getInferredJavaType().getJavaTypeInstance(),
                binding,
                guard,
                null
        );
    }

    static SwitchPatternCasePlanSpec record(SwitchPatternCaseContext context,
                                            LocalVariable binding,
                                            List<LValue> components,
                                            LinkedList<Op04StructuredStatement> rewrittenStatements) {
        return new SwitchPatternCasePlanSpec(
                Kind.RECORD,
                context.getStructuredCase(),
                context.getBodyBlock(),
                rewrittenStatements,
                binding.getInferredJavaType().getJavaTypeInstance(),
                binding,
                null,
                components
        );
    }

    Kind getKind() {
        return kind;
    }

    StructuredCase getStructuredCase() {
        return structuredCase;
    }

    Block getBodyBlock() {
        return bodyBlock;
    }

    LinkedList<Op04StructuredStatement> getRewrittenStatements() {
        return rewrittenStatements;
    }

    JavaTypeInstance getBindingType() {
        return bindingType;
    }

    LocalVariable getBinding() {
        return binding;
    }

    Expression getGuard() {
        return guard;
    }

    List<LValue> getRecordComponents() {
        return recordComponents;
    }
}
