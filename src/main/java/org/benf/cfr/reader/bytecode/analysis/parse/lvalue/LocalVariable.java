package org.benf.cfr.reader.bytecode.analysis.parse.lvalue;

import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.types.annotated.JavaAnnotatedTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.variables.Ident;
import org.benf.cfr.reader.bytecode.analysis.variables.NamedVariable;
import org.benf.cfr.reader.bytecode.analysis.variables.NamedVariableDefault;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableNamer;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.*;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericBaseInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.Objects;

public class LocalVariable extends AbstractLValue {
    private final NamedVariable name;
    // We keep this so we don't confuse two variables with the same name, tricksy.
    private final int idx;
    private final Ident ident;
    private boolean guessedFinal;
    private boolean guessedVar;
    private boolean ignored;
    private boolean capturedByLambda;
    private boolean suppressDefaultInitializer;
    private boolean conflictingGenericDeclaration;
    private final int originalRawOffset;
    private JavaAnnotatedTypeInstance customCreationType;
    private JavaTypeInstance customCreationJavaType;

    public LocalVariable(int stackPosition, Ident ident, VariableNamer variableNamer, int originalRawOffset, boolean clashed, InferredJavaType inferredJavaType) {
        super(inferredJavaType);
        this.name = variableNamer.getName(originalRawOffset, ident, stackPosition, clashed);
        this.idx = stackPosition;
        this.ident = ident;
        this.guessedFinal = false;
        this.guessedVar = false;
        this.capturedByLambda = false;
        this.suppressDefaultInitializer = false;
        this.conflictingGenericDeclaration = false;
        this.originalRawOffset = originalRawOffset;
    }

    public LocalVariable(String name, InferredJavaType inferredJavaType) {
        super(inferredJavaType);
        this.name = new NamedVariableDefault(name);
        this.idx = -1;
        this.ident = null;
        this.guessedFinal = false;
        this.guessedVar = false;
        this.capturedByLambda = false;
        this.suppressDefaultInitializer = false;
        this.conflictingGenericDeclaration = false;
        this.originalRawOffset = -1;
    }

    public int getOriginalRawOffset() {
        return originalRawOffset;
    }

    @Override
    public int getNumberOfCreators() {
        throw new ConfusedCFRException("NYI");
    }

    @Override
    public boolean isFinal() {
        return guessedFinal;
    }

    @Override
    public void markFinal() {
        guessedFinal = true;
    }

    @Override
    public void markVar() {
        guessedVar = true;
    }

    public void markIgnored() { ignored = true; }

    public void markCapturedByLambda() {
        this.capturedByLambda = true;
    }

    public boolean isCapturedByLambda() {
        return capturedByLambda;
    }

    public void suppressDefaultInitializer() {
        this.suppressDefaultInitializer = true;
    }

    public boolean shouldSuppressDefaultInitializer() {
        return suppressDefaultInitializer;
    }

    public void markConflictingGenericDeclaration() {
        this.conflictingGenericDeclaration = true;
    }

    public void clearConflictingGenericDeclaration() {
        this.conflictingGenericDeclaration = false;
    }

    public boolean hasConflictingGenericDeclaration() {
        return conflictingGenericDeclaration;
    }

    public boolean matchesReadableAlias(LocalVariable other) {
        if (other == null) {
            return false;
        }
        if (this == other || this.equals(other)) {
            return true;
        }
        JavaTypeInstance thisType = getInferredJavaType().getJavaTypeInstance();
        JavaTypeInstance otherType = other.getInferredJavaType().getJavaTypeInstance();
        if (!Objects.equals(thisType, otherType)) {
            return false;
        }
        if (idx >= 0 && other.idx >= 0) {
            if (idx != other.idx) {
                return false;
            }
            if (ident != null && other.ident != null) {
                return ident.equals(other.ident);
            }
            if (originalRawOffset >= 0 && other.originalRawOffset >= 0) {
                return originalRawOffset == other.originalRawOffset;
            }
            return !hasStableLocalIdentity(this)
                    || !hasStableLocalIdentity(other)
                    ? Objects.equals(name.getStringName(), other.name.getStringName())
                    : false;
        }
        if (idx == other.idx && Objects.equals(name.getStringName(), other.name.getStringName())) {
            return true;
        }
        return idx == -1
                && other.idx == -1
                && ident == null
                && other.ident == null
                && Objects.equals(name.getStringName(), other.name.getStringName());
    }

    private static boolean hasStableLocalIdentity(LocalVariable localVariable) {
        return localVariable != null
                && localVariable.idx >= 0
                && (localVariable.ident != null || localVariable.originalRawOffset >= 0);
    }

    @Override
    public boolean isFakeIgnored() {
        return ignored;
    }

    @Override
    public boolean isVar() {
        return guessedVar;
    }

    // Mutation hack :( we need to be able (without affecting any analysis) to inform the creation
    // about annotations.
    public void setCustomCreationType(JavaAnnotatedTypeInstance customCreationType) {
        this.customCreationType = customCreationType;
    }

    public void setCustomCreationJavaType(JavaTypeInstance customCreationJavaType) {
        if (shouldPreserveExistingCreationType(this.customCreationJavaType, customCreationJavaType)) {
            return;
        }
        this.customCreationJavaType = customCreationJavaType;
    }

    @Override
    public JavaAnnotatedTypeInstance getAnnotatedCreationType() {
        return customCreationType;
    }

    public JavaTypeInstance getCustomCreationJavaType() {
        return customCreationJavaType;
    }

    /*
     * Can't modify, so deep clone is this.
     */
    @Override
    public LValue deepClone(CloneHelper cloneHelper) {
        return this;
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.HIGHEST;
    }

    @Override
    public Dumper dump(Dumper d, boolean defines) {
        return name.dump(d, defines); // todo pass lv, lvt, start offset
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        name.dump(d);
        // Note that this print is only decorating when we have bad data.
        d.print(typeToString());
        return d;
    }

    public NamedVariable getName() {
        return name;
    }

    public int getIdx() {
        return idx;
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        return false;
    }

    @Override
    public <T> void collectLValueAssignments(Expression assignedTo, StatementContainer<T> statementContainer, LValueAssignmentCollector<T> lValueAssigmentCollector) {
        lValueAssigmentCollector.collectLocalVariableAssignment(this, statementContainer, assignedTo);
    }

    @Override
    public LValue replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        return this;
    }

    @Override
    public SSAIdentifiers<LValue> collectVariableMutation(SSAIdentifierFactory<LValue, ?> ssaIdentifierFactory) {
        return new SSAIdentifiers<LValue>(this, ssaIdentifierFactory);
    }

    @Override
    public LValue applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocalVariable)) return false;

        LocalVariable that = (LocalVariable) o;

        if (!name.equals(that.name)) return false;
        if (idx != that.idx) {
            return false;
        }
        if (ident == null) {
            if (that.ident != null) return false;
        } else {
            if (!ident.equals(that.ident)) return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + idx;
        if (ident != null) result = 31 * result + ident.hashCode();
        return result;
    }

    // fabric
    public Ident getIdent() {
        return ident;
    }

    private boolean shouldPreserveExistingCreationType(JavaTypeInstance currentType, JavaTypeInstance candidateType) {
        if (currentType == null || candidateType == null || currentType.equals(candidateType)) {
            return false;
        }
        JavaTypeInstance currentBaseType = currentType.getDeGenerifiedType();
        JavaTypeInstance candidateBaseType = candidateType.getDeGenerifiedType();
        if (currentBaseType == null || !currentBaseType.equals(candidateBaseType)) {
            return false;
        }
        if (!(currentType instanceof JavaGenericBaseInstance)) {
            return false;
        }
        if (candidateType instanceof JavaGenericBaseInstance) {
            return false;
        }
        return !((JavaGenericBaseInstance) currentType).hasUnbound();
    }
}
