/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.exec.op;

public record ProcedureCallOp(String procedureName) implements GraphOp {
    @Override
    public String name() {
        return "ProcedureCall";
    }
}
