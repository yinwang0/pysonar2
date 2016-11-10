package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Tuple extends Sequence {

    public Tuple(List<Node> elts, String file, int start, int end, int line, int col) {
        super(NodeType.TUPLE, elts, file, start, end, line, col);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Tuple:" + start + ":" + elts + ">";
    }

    @NotNull
    @Override
    public String toDisplay() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");

        int idx = 0;
        for (Node n : elts) {
            if (idx != 0) {
                sb.append(", ");
            }
            idx++;
            sb.append(n.toDisplay());
        }

        sb.append(")");
        return sb.toString();
    }

}
