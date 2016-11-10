package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PyList extends Sequence {

    public PyList(@NotNull List<Node> elts, String file, int start, int end, int line, int col) {
        super(NodeType.PYLIST, elts, file, start, end, line, col);
    }

    @NotNull
    @Override
    public String toString() {
        return "<List:" + start + ":" + elts + ">";
    }

}
