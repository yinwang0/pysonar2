package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;


public class IfExp extends Node {

    public Node test;
    public Node body;
    public Node orelse;


    public IfExp(Node test, Node body, Node orelse, int start, int end) {
        super(start, end);
        this.test = test;
        this.body = body;
        this.orelse = orelse;
        addChildren(test, body, orelse);
    }


    @NotNull
    @Override
    public Type transform(State s) {
        Type type1, type2;
        transformExpr(test, s);

        if (body != null) {
            type1 = transformExpr(body, s);
        } else {
            type1 = Analyzer.self.builtins.Cont;
        }
        if (orelse != null) {
            type2 = transformExpr(orelse, s);
        } else {
            type2 = Analyzer.self.builtins.Cont;
        }
        return UnionType.union(type1, type2);
    }


    @NotNull
    @Override
    public String toString() {
        return "<IfExp:" + start + ":" + test + ":" + body + ":" + orelse + ">";
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
