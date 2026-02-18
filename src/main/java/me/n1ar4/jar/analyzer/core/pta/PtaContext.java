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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

final class PtaContext {
    private static final PtaContext ROOT = new PtaContext(Collections.emptyList());
    private final List<String> callChain;

    private PtaContext(List<String> callChain) {
        this.callChain = callChain == null ? Collections.emptyList() : callChain;
    }

    static PtaContext root() {
        return ROOT;
    }

    PtaContext extend(String callSiteToken, int depth) {
        if (depth <= 0) {
            return this;
        }
        String token = normalizeToken(callSiteToken);
        if (token == null) {
            return this;
        }
        ArrayList<String> next = new ArrayList<>(Math.max(1, depth));
        next.add(token);
        for (int i = 0; i < callChain.size() && next.size() < depth; i++) {
            next.add(callChain.get(i));
        }
        return new PtaContext(Collections.unmodifiableList(next));
    }

    String render() {
        if (callChain.isEmpty()) {
            return "root";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < callChain.size(); i++) {
            if (i > 0) {
                sb.append("->");
            }
            sb.append(callChain.get(i));
        }
        return sb.toString();
    }

    String renderDepth(int depth) {
        if (depth <= 0 || callChain.isEmpty()) {
            return "root";
        }
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(depth, callChain.size());
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                sb.append("->");
            }
            sb.append(callChain.get(i));
        }
        return sb.length() == 0 ? "root" : sb.toString();
    }

    private static String normalizeToken(String token) {
        if (token == null) {
            return null;
        }
        String value = token.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.length() > 80) {
            value = value.substring(0, 80);
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PtaContext that = (PtaContext) o;
        return Objects.equals(callChain, that.callChain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(callChain);
    }
}
