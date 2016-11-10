package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Slice extends Node {

    public Node lower;
    public Node step;
    public Node upper;

    public Slice(Node lower, Node step, Node upper, String file, int start, int end, int line, int col) {
        super(NodeType.SLICE, file, start, end, line, col);
        this.lower = lower;
        this.step = step;
        this.upper = upper;
        addChildren(lower, step, upper);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Slice:" + lower + ":" + step + ":" + upper + ">";
    }

}
