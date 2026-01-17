/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.taint;

import com.alibaba.fastjson2.annotation.JSONField;

import java.util.List;

public class TaintGuardRule {
    @JSONField
    private String kind;
    @JSONField
    private String type;
    @JSONField
    private String className;
    @JSONField
    private String methodName;
    @JSONField
    private String methodDesc;
    @JSONField
    private Integer paramIndex;
    @JSONField
    private List<String> allowlist;
    @JSONField
    private Boolean enabled;
    @JSONField
    private String mode;
    @JSONField
    private Boolean requireNormalized;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodDesc() {
        return methodDesc;
    }

    public void setMethodDesc(String methodDesc) {
        this.methodDesc = methodDesc;
    }

    public Integer getParamIndex() {
        return paramIndex;
    }

    public void setParamIndex(Integer paramIndex) {
        this.paramIndex = paramIndex;
    }

    public List<String> getAllowlist() {
        return allowlist;
    }

    public void setAllowlist(List<String> allowlist) {
        this.allowlist = allowlist;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Boolean getRequireNormalized() {
        return requireNormalized;
    }

    public void setRequireNormalized(Boolean requireNormalized) {
        this.requireNormalized = requireNormalized;
    }
}
