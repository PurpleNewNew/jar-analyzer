/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.pta;

import me.n1ar4.jar.analyzer.core.reference.MethodReference;

final class PtaSyntheticCall {
    private final MethodReference.Handle target;
    private final String type;
    private final String confidence;
    private final String reason;
    private final int opcode;

    PtaSyntheticCall(MethodReference.Handle target,
                     String type,
                     String confidence,
                     String reason,
                     int opcode) {
        this.target = target;
        this.type = type;
        this.confidence = confidence;
        this.reason = reason;
        this.opcode = opcode;
    }

    MethodReference.Handle getTarget() {
        return target;
    }

    String getType() {
        return type;
    }

    String getConfidence() {
        return confidence;
    }

    String getReason() {
        return reason;
    }

    int getOpcode() {
        return opcode;
    }
}
