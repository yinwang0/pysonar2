package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Repr extends Node {

    public Node value;

    public Repr(Node n, String file, int start, int end, int line, int col) {
        super(NodeType.REPR, file, start, end, line, col);
        this.value = n;
        addChildren(n);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Repr:" + value + ">";
    }

}
