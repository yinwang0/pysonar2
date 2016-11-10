package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class BinOp extends Node {

    @NotNull
    public Node left;
    @NotNull
    public Node right;
    @NotNull
    public Op op;

    public BinOp(@NotNull Op op, @NotNull Node left, @NotNull Node right, String file, int start, int end, int line, int col) {
        super(NodeType.BINOP, file, start, end, line, col);
        this.left = left;
        this.right = right;
        this.op = op;
        addChildren(left, right);
    }

    @NotNull
    @Override
    public String toString() {
        return "(" + left + " " + op + " " + right + ")";
    }

}
