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

/**
 * Output sink for DFS analysis.
 * <p>
 * Engine/server code should depend on this interface instead of Swing components.
 */
public interface DfsOutput {
    void clear();

    void onMessage(String msg);

    void onChainFound(String chainId,
                      String title,
                      List<String> methods,
                      List<DFSEdge> edges,
                      boolean showEdgeMeta);
}

