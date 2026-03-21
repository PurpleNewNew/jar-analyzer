package org.benf.cfr.reader.util;

public enum DecompilationQuality {
    CLEAN,
    DEGRADED,
    FAILED;

    public static DecompilationQuality max(DecompilationQuality a, DecompilationQuality b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
