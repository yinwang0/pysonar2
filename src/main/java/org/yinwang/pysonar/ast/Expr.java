package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.Type;


/**
 * Expression statement.
 */
public class Expr extends Node {

    public Node value;


    public Expr(Node n, int start, int end) {
        super(start, end);
        this.value = n;
        addChildren(n);
    }


    @NotNull
    @Override
    public Type transform(State s) {
        if (value != null) {
            transformExpr(value, s);
        }
        return Type.CONT;
    }


    @NotNull
    @Override
    public String toString() {
        return "<Expr:" + value + ">";
    }

}
