package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Return extends Node {

    public Node value;

    public Return(Node n, String file, int start, int end) {
        super(NodeType.RETURN, file, start, end);
        this.value = n;
        addChildren(n);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Return:" + value + ">";
    }

}
