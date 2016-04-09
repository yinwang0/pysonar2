package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Await extends Node {

    public Node value;

    public Await(Node n, String file, int start, int end) {
        super(NodeType.AWAIT, file, start, end);
        this.value = n;
        addChildren(n);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Await:" + value + ">";
    }

}
