package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class YieldFrom extends Node {

    public Node value;

    public YieldFrom(Node n, String file, int start, int end) {
        super(NodeType.YIELDFROM, file, start, end);
        this.value = n;
        addChildren(n);
    }

    @NotNull
    @Override
    public String toString() {
        return "<YieldFrom:" + start + ":" + value + ">";
    }

}
