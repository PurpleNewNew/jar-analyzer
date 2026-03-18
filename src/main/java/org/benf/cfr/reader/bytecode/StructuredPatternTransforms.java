package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.InstanceofMatchTidyingRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.InstanceOfTreeTransformer;

import java.util.List;

final class StructuredPatternTransforms {
    private StructuredPatternTransforms() {
    }

    static List<StructuredPassEntry> describePasses() {
        return List.of(
                entry(
                        "normalize-instanceof",
                        "pattern-semantics.normalize",
                        "any-structure-state",
                        "Normalizes instanceof trees so pattern recovery sees leaf-instanceof conditions.",
                        true,
                        false
                ),
                entry(
                        "tidy-instance-matches",
                        "pattern-semantics.recover",
                        "any-structure-state",
                        "Recovers instanceof match scaffolding after variable-scope discovery flagged matches.",
                        true,
                        true,
                        "normalize-instanceof"
                )
        );
    }

    static void normalizeInstanceOf(Op04StructuredStatement root) {
        new InstanceOfTreeTransformer().transform(root);
    }

    static void tidyInstanceMatches(Op04StructuredStatement root) {
        InstanceofMatchTidyingRewriter.rewrite(root);
    }

    static StructuredPassEntry normalizeEntry() {
        return describePasses().get(0);
    }

    static StructuredPassEntry tidyEntry() {
        return describePasses().get(1);
    }

    private static StructuredPassEntry entry(String name,
                                             String stage,
                                             String inputRequirement,
                                             String outputPromise,
                                             boolean idempotent,
                                             boolean allowsStructuralChange,
                                             String... dependencies) {
        return StructuredPassEntry.of(
                "pattern-semantics",
                stage,
                inputRequirement,
                StructuredPassDescriptor.of(name, outputPromise, idempotent, allowsStructuralChange, dependencies)
        );
    }
}
