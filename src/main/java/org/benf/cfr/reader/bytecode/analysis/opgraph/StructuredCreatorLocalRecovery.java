package org.benf.cfr.reader.bytecode.analysis.opgraph;

final class StructuredCreatorLocalRecovery {
    private StructuredCreatorLocalRecovery() {
    }

    static void restoreCreatorDependencyOrder(Op04StructuredStatement root) {
        StructuredCreatorOrderingRecovery.restoreCreatorDependencyOrder(root);
    }

    static void restoreCreatorsBeforeFirstUse(Op04StructuredStatement root) {
        StructuredCreatorOrderingRecovery.restoreCreatorsBeforeFirstUse(root);
    }

    static void restoreTrailingCreatorAssignments(Op04StructuredStatement root) {
        StructuredEmbeddedAssignmentRecovery.restoreTrailingCreatorAssignments(root);
    }

    static void restorePrefixEmbeddedAssignments(Op04StructuredStatement root) {
        StructuredEmbeddedAssignmentRecovery.restorePrefixEmbeddedAssignments(root);
    }
}
