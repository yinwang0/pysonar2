package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;


public class Assert extends Node {

    public Node test;
    public Node msg;


    public Assert(Node test, Node msg, int start, int end) {
        super(start, end);
        this.test = test;
        this.msg = msg;
        addChildren(test, msg);
    }


    @NotNull
    @Override
    public Type resolve(Scope s) {
        if (test != null) {
            resolveExpr(test, s);
        }
        if (msg != null) {
            resolveExpr(msg, s);
        }
        return Analyzer.self.builtins.Cont;
    }


    @NotNull
    @Override
    public String toString() {
        return "<Assert:" + test + ":" + msg + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(test, v);
            visitNode(msg, v);
        }
    }
}
