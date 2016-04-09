package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Assign extends Node {

    @NotNull
    public Node target;
    @NotNull
    public Node value;

    public Assign(@NotNull Node target, @NotNull Node value, String file, int start, int end) {
        super(NodeType.ASSIGN, file, start, end);
        this.target = target;
        this.value = value;
        addChildren(target);
        addChildren(value);
    }

    @NotNull
    @Override
    public String toString() {
        return "(" + target + " = " + value + ")";
    }
}
