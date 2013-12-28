package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;


public class BinOp extends Node {

    @NotNull
    public Node left;
    @NotNull
    public Node right;
    @NotNull
    public Op op;


    public BinOp(@NotNull Op op, @NotNull Node left, @NotNull Node right, String file, int start, int end) {
        super(file, start, end);
        this.left = left;
        this.right = right;
        this.op = op;
        addChildren(left, right);
    }


    @NotNull
    @Override
    public Type transform(State s) {
        Type ltype = transformExpr(left, s);
        Type rtype = transformExpr(right, s);

        // If either non-null operand is a string, assume the result is a string.
        if (ltype == Type.STR || rtype == Type.STR) {
            return Type.STR;
        }
        // If either non-null operand is a number, assume the result is a number.
        if (ltype == Type.INT || rtype == Type.INT) {
            return Type.INT;
        }

        return UnionType.union(ltype, rtype);
    }


    @NotNull
    @Override
    public String toString() {
        return "(" + left + " " + op + " " + right + ")";
    }

}
