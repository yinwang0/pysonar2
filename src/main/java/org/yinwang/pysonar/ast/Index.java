package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Index extends Node {

    public Node value;

    public Index(Node n, String file, int start, int end) {
        super(NodeType.INDEX, file, start, end);
        this.value = n;
        addChildren(n);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Index:" + value + ">";
    }

}
