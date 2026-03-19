package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.DecompilerComments;

public class AnalysisResultSuccessful implements AnalysisResult {
    private final DecompilerComments comments;
    private final Op04StructuredStatement code;
    private final AnonymousClassUsage anonymousClassUsage;
    private final StructureRecoveryTrace structureRecoveryTrace;
    private final VariableRecoveryTrace variableRecoveryTrace;
    private final TypeRecoveryTrace typeRecoveryTrace;
    private final MethodDecompileRecord methodDecompileRecord;
    private final boolean failed;
    private final boolean exception;

    AnalysisResultSuccessful(DecompilerComments comments,
                             Op04StructuredStatement code,
                             AnonymousClassUsage anonymousClassUsage,
                             StructureRecoveryTrace structureRecoveryTrace,
                             VariableRecoveryTrace variableRecoveryTrace,
                             TypeRecoveryTrace typeRecoveryTrace,
                             MethodDecompileRecord methodDecompileRecord) {
        this.anonymousClassUsage = anonymousClassUsage;
        this.comments = comments;
        this.code = code;
        this.structureRecoveryTrace = structureRecoveryTrace;
        this.variableRecoveryTrace = variableRecoveryTrace;
        this.typeRecoveryTrace = typeRecoveryTrace;
        this.methodDecompileRecord = methodDecompileRecord;
        boolean failed = false;
        boolean exception = false;
        for (DecompilerComment comment : comments.getCommentCollection()) {
            if (comment.isFailed()) {
                failed = true;
            }
            if (comment.isException()) {
                exception = true;
            }
        }
        this.failed = failed;
        this.exception = exception;
    }

    @Override
    public boolean isFailed() {
        return failed;
    }

    @Override
    public boolean isThrown() {
        return exception;
    }

    @Override
    public Op04StructuredStatement getCode() {
        return code;
    }

    @Override
    public DecompilerComments getComments() {
        return comments;
    }

    @Override
    public AnonymousClassUsage getAnonymousClassUsage() {
        return anonymousClassUsage;
    }

    @Override
    public StructureRecoveryTrace getStructureRecoveryTrace() {
        return structureRecoveryTrace;
    }

    @Override
    public VariableRecoveryTrace getVariableRecoveryTrace() {
        return variableRecoveryTrace;
    }

    @Override
    public TypeRecoveryTrace getTypeRecoveryTrace() {
        return typeRecoveryTrace;
    }

    @Override
    public MethodDecompileRecord getMethodDecompileRecord() {
        return methodDecompileRecord;
    }
}
