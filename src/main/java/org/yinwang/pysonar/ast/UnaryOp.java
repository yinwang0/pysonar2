package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class UnaryOp extends Node {

    public Op op;
    public Node operand;

    public UnaryOp(Op op, Node operand, String file, int start, int end) {
        super(NodeType.UNARYOP, file, start, end);
        this.op = op;
        this.operand = operand;
        addChildren(operand);
    }

    @NotNull
    @Override
    public String toString() {
        return "(" + op + " " + operand + ")";
    }

}
