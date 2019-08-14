package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PySet extends Sequence {

    public PySet(List<Node> elts, String file, int start, int end, int line, int col) {
        super(NodeType.PYSET, elts, file, start, end, line, col);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Set:" + start + ":" + elts + ">";
    }

}
