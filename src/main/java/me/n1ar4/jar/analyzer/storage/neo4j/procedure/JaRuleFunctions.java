/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.storage.neo4j.procedure;

import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

public final class JaRuleFunctions {
    @UserFunction(name = "ja.isSource")
    @Description("Return true when the target method node is marked as a source.")
    public Boolean isSource(@Name("target") Object target) {
        return JaNativeBridge.isSource(target);
    }

    @UserFunction(name = "ja.isSink")
    @Description("Return true when the target method node matches the current sink rules.")
    public Boolean isSink(@Name("target") Object target) {
        return JaNativeBridge.isSink(target);
    }

    @UserFunction(name = "ja.sinkKind")
    @Description("Resolve the sink category for the target method node.")
    public String sinkKind(@Name("target") Object target) {
        return JaNativeBridge.sinkKind(target);
    }

    @UserFunction(name = "ja.ruleVersion")
    @Description("Return the current effective rule version.")
    public Long ruleVersion() {
        return JaNativeBridge.ruleVersion();
    }

    @UserFunction(name = "ja.rulesFingerprint")
    @Description("Return the current effective rules fingerprint.")
    public String rulesFingerprint() {
        return JaNativeBridge.rulesFingerprint();
    }

    @UserFunction(name = "ja.ruleValidation")
    @Description("Return the current rule validation summary for model/source and sink rules.")
    public java.util.Map<String, Object> ruleValidation() {
        return JaNativeBridge.ruleValidation();
    }

    @UserFunction(name = "ja.ruleValidationIssues")
    @Description("Return flattened rule validation issues. Scope supports all|model|source|sink.")
    public java.util.List<java.util.Map<String, Object>> ruleValidationIssues(@Name("scope") String scope) {
        return JaNativeBridge.ruleValidationIssues(scope);
    }
}
