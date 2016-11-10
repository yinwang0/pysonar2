package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

/**
 * Expression statement.
 */
public class Expr extends Node {

    public Node value;

    public Expr(Node n, String file, int start, int end, int line, int col) {
        super(NodeType.EXPR, file, start, end, line, col);
        this.value = n;
        addChildren(n);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Expr:" + value + ">";
    }

}
