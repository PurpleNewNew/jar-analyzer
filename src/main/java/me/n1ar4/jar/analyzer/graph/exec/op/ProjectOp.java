/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.exec.op;

import java.util.List;

public record ProjectOp(List<String> columns) implements GraphOp {
    @Override
    public String name() {
        return "Project";
    }
}
