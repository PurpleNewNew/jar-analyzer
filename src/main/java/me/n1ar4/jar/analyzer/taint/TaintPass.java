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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class TaintPass {
    private static final TaintPass FAIL = new TaintPass(Collections.emptySet());

    private final Set<Integer> paramIndices;

    private TaintPass(Set<Integer> paramIndices) {
        if (paramIndices == null || paramIndices.isEmpty()) {
            this.paramIndices = Collections.emptySet();
        } else {
            this.paramIndices = Collections.unmodifiableSet(new LinkedHashSet<>(paramIndices));
        }
    }

    public static TaintPass fail() {
        return FAIL;
    }

    public static TaintPass fromParamIndex(int paramIndex) {
        if (paramIndex == Sanitizer.NO_PARAM) {
            return FAIL;
        }
        Set<Integer> set = new LinkedHashSet<>();
        set.add(paramIndex);
        return new TaintPass(set);
    }

    public static TaintPass fromParamIndices(Collection<Integer> indices) {
        if (indices == null || indices.isEmpty()) {
            return FAIL;
        }
        Set<Integer> set = new LinkedHashSet<>();
        for (Integer idx : indices) {
            if (idx == null || idx == Sanitizer.NO_PARAM) {
                continue;
            }
            set.add(idx);
        }
        if (set.isEmpty()) {
            return FAIL;
        }
        return new TaintPass(set);
    }

    public boolean isFail() {
        return this.paramIndices.isEmpty();
    }

    public boolean hasAllParams() {
        return this.paramIndices.contains(Sanitizer.ALL_PARAMS);
    }

    public Set<Integer> getParamIndices() {
        return this.paramIndices;
    }

    public int toParamIndex() {
        if (this.paramIndices.isEmpty()) {
            throw new IllegalStateException("no param index for FAIL");
        }
        if (this.paramIndices.contains(Sanitizer.ALL_PARAMS)) {
            return Sanitizer.ALL_PARAMS;
        }
        return this.paramIndices.iterator().next();
    }

    public TaintPass addParamIndex(int paramIndex) {
        if (paramIndex == Sanitizer.NO_PARAM) {
            return this;
        }
        if (this.paramIndices.contains(paramIndex)) {
            return this;
        }
        Set<Integer> set = new LinkedHashSet<>(this.paramIndices);
        set.add(paramIndex);
        return new TaintPass(set);
    }

    public TaintPass merge(TaintPass other) {
        if (other == null || other.paramIndices.isEmpty()) {
            return this;
        }
        if (this.paramIndices.isEmpty()) {
            return other;
        }
        Set<Integer> set = new LinkedHashSet<>(this.paramIndices);
        set.addAll(other.paramIndices);
        return new TaintPass(set);
    }

    public String formatLabel() {
        if (this.paramIndices.isEmpty()) {
            return "fail";
        }
        if (this.paramIndices.contains(Sanitizer.ALL_PARAMS)) {
            return "all";
        }
        List<Integer> ordered = new ArrayList<>(this.paramIndices);
        ordered.sort((a, b) -> {
            if (a == Sanitizer.THIS_PARAM && b != Sanitizer.THIS_PARAM) {
                return -1;
            }
            if (b == Sanitizer.THIS_PARAM && a != Sanitizer.THIS_PARAM) {
                return 1;
            }
            return Integer.compare(a, b);
        });
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ordered.size(); i++) {
            int idx = ordered.get(i);
            if (i > 0) {
                sb.append(",");
            }
            if (idx == Sanitizer.THIS_PARAM) {
                sb.append("this");
            } else {
                sb.append(idx);
            }
        }
        return sb.toString();
    }
}
