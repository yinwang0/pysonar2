package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Starred extends Node {

    public Node value;

    public Starred(Node n, String file, int start, int end) {
        super(NodeType.STARRED, file, start, end);
        this.value = n;
        addChildren(n);
    }

    @NotNull
    @Override
    public String toString() {
        return "<starred:" + value + ">";
    }

}
