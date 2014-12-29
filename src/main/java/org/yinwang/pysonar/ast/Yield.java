package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.ListType;
import org.yinwang.pysonar.types.Type;


public class Yield extends Node {

    public Node value;


    public Yield(Node n, String file, int start, int end) {
        super(file, start, end);
        this.value = n;
        addChildren(n);
    }


    @NotNull
    @Override
    public Type transform(State s) {
        if (value != null) {
            return new ListType(transformExpr(value, s));
        } else {
            return Type.NONE;
        }
    }

    @Override
    protected void unify(@NotNull Type other, @NotNull State env) {

    }


    @NotNull
    @Override
    public String toString() {
        return "<Yield:" + start + ":" + value + ">";
    }

}
