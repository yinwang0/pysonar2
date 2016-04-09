package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PyList extends Sequence {

    public PyList(@NotNull List<Node> elts, String file, int start, int end) {
        super(NodeType.PYLIST, elts, file, start, end);
    }

    @NotNull
    @Override
    public String toString() {
        return "<List:" + start + ":" + elts + ">";
    }

}
