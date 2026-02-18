/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.exec.op;

public record ExpandOp(int maxHops, boolean shortestPath) implements GraphOp {
    @Override
    public String name() {
        return "Expand";
    }
}
