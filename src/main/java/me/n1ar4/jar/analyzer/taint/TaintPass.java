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

public final class TaintPass {
    public enum Kind {
        FAIL,
        THIS,
        PARAM
    }

    private static final TaintPass FAIL = new TaintPass(Kind.FAIL, -1);
    private static final TaintPass THIS = new TaintPass(Kind.THIS, Sanitizer.THIS_PARAM);

    private final Kind kind;
    private final int paramIndex;

    private TaintPass(Kind kind, int paramIndex) {
        this.kind = kind;
        this.paramIndex = paramIndex;
    }

    public static TaintPass fail() {
        return FAIL;
    }

    public static TaintPass thisRef() {
        return THIS;
    }

    public static TaintPass param(int paramIndex) {
        return new TaintPass(Kind.PARAM, paramIndex);
    }

    public static TaintPass fromParamIndex(int paramIndex) {
        if (paramIndex == Sanitizer.THIS_PARAM) {
            return thisRef();
        }
        return param(paramIndex);
    }

    public boolean isFail() {
        return this.kind == Kind.FAIL;
    }

    public Kind getKind() {
        return this.kind;
    }

    public int getParamIndex() {
        return this.paramIndex;
    }

    public int toParamIndex() {
        if (this.kind == Kind.THIS) {
            return Sanitizer.THIS_PARAM;
        }
        if (this.kind == Kind.PARAM) {
            return this.paramIndex;
        }
        throw new IllegalStateException("no param index for FAIL");
    }

    public String formatLabel() {
        if (this.kind == Kind.FAIL) {
            return "fail";
        }
        if (this.kind == Kind.THIS) {
            return "this";
        }
        return String.valueOf(this.paramIndex);
    }
}
