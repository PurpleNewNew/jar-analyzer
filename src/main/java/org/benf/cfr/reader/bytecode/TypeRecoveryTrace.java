package org.benf.cfr.reader.bytecode;

import java.util.ArrayList;
import java.util.List;

public final class TypeRecoveryTrace {
    private final List<PassTrace> passes = new ArrayList<PassTrace>();

    void record(StructuredPassEntry pass,
                String expressionKind,
                String expectedType,
                String beforeType,
                String afterType) {
        record(pass, expressionKind, expectedType, beforeType, afterType, null);
    }

    void record(StructuredPassEntry pass,
                String expressionKind,
                String expectedType,
                String beforeType,
                String afterType,
                String detail) {
        passes.add(new PassTrace(pass, expressionKind, expectedType, beforeType, afterType, detail));
    }

    public List<PassTrace> getPasses() {
        return List.copyOf(passes);
    }

    public static TypeRecoveryTrace empty() {
        return new TypeRecoveryTrace();
    }

    public static final class PassTrace {
        private final StructuredPassEntry pass;
        private final String expressionKind;
        private final String expectedType;
        private final String beforeType;
        private final String afterType;
        private final String detail;

        private PassTrace(StructuredPassEntry pass,
                          String expressionKind,
                          String expectedType,
                          String beforeType,
                          String afterType,
                          String detail) {
            this.pass = pass;
            this.expressionKind = expressionKind;
            this.expectedType = expectedType;
            this.beforeType = beforeType;
            this.afterType = afterType;
            this.detail = detail;
        }

        public StructuredPassEntry getPass() {
            return pass;
        }

        public String getExpressionKind() {
            return expressionKind;
        }

        public String getExpectedType() {
            return expectedType;
        }

        public String getBeforeType() {
            return beforeType;
        }

        public String getAfterType() {
            return afterType;
        }

        public String getDetail() {
            return detail;
        }

        public boolean isChanged() {
            if (beforeType == null) {
                return afterType != null;
            }
            return !beforeType.equals(afterType);
        }
    }
}
