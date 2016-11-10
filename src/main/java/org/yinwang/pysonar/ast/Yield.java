package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Yield extends Node {

    public Node value;

    public Yield(Node n, String file, int start, int end, int line, int col) {
        super(NodeType.YIELD, file, start, end, line, col);
        this.value = n;
        addChildren(n);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Yield:" + start + ":" + value + ">";
    }

}
