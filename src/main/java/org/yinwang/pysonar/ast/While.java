package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;


public class While extends Node {

    public Node test;
    public Node body;
    public Node orelse;


    public While(Node test, Node body, Node orelse, String file, int start, int end) {
        super(file, start, end);
        this.test = test;
        this.body = body;
        this.orelse = orelse;
        addChildren(test, body, orelse);
    }


    @NotNull
    @Override
    public Type transform(State s) {
        transformExpr(test, s);
        Type t = Type.UNKNOWN;

        if (body != null) {
            t = transformExpr(body, s);
        }

        if (orelse != null) {
            t = UnionType.union(t, transformExpr(orelse, s));
        }

        return t;
    }

    @Override
    protected void unify(@NotNull Type other, @NotNull State env) {

    }


    @NotNull
    @Override
    public String toString() {
        return "<While:" + test + ":" + body + ":" + orelse + ":" + start + ">";
    }

}
