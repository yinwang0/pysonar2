package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;


public class While extends Node {

    public Node test;
    public Node body;
    public Node orelse;


    public While(Node test, Node body, Node orelse, int start, int end) {
        super(start, end);
        this.test = test;
        this.body = body;
        this.orelse = orelse;
        addChildren(test, body, orelse);
    }


    @NotNull
    @Override
    public Type transform(State s) {
        transformExpr(test, s);
        Type t = Analyzer.self.builtins.unknown;

        if (body != null) {
            t = transformExpr(body, s);
        }

        if (orelse != null) {
            t = UnionType.union(t, transformExpr(orelse, s));
        }

        return t;
    }


    @NotNull
    @Override
    public String toString() {
        return "<While:" + test + ":" + body + ":" + orelse + ":" + start + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(test, v);
            visitNode(body, v);
            visitNode(orelse, v);
        }
    }
}
