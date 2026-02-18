/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.exec.op;

public record LimitOp(int limit, int skip) implements GraphOp {
    @Override
    public String name() {
        return "Limit";
    }
}
