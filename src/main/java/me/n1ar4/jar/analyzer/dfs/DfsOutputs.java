/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.dfs;

import java.util.List;

public final class DfsOutputs {
    private static final DfsOutput NOOP = new DfsOutput() {
        @Override
        public void clear() {
        }

        @Override
        public void onMessage(String msg) {
        }

        @Override
        public void onChainFound(String chainId, String title, List<String> methods, List<DFSEdge> edges, boolean showEdgeMeta) {
        }
    };

    private DfsOutputs() {
    }

    public static DfsOutput noop() {
        return NOOP;
    }
}

