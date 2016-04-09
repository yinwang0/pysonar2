package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PySet extends Sequence {

    public PySet(List<Node> elts, String file, int start, int end) {
        super(NodeType.PYSET, elts, file, start, end);
    }

    @NotNull
    @Override
    public String toString() {
        return "<List:" + start + ":" + elts + ">";
    }

}
