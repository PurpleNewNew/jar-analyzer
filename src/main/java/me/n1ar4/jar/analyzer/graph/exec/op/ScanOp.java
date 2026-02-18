/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.exec.op;

public record ScanOp(String labelHint) implements GraphOp {
    @Override
    public String name() {
        return "Scan";
    }
}
