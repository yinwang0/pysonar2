package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.Binder;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.Type;

import java.util.List;


public class Handler extends Node {

    public List<Node> exceptions;
    public Node binder;
    public Block body;


    public Handler(List<Node> exceptions, Node binder, Block body, int start, int end) {
        super(start, end);
        this.binder = binder;
        this.exceptions = exceptions;
        this.body = body;
        addChildren(binder, body);
        addChildren(exceptions);
    }


    @NotNull
    @Override
    public Type transform(@NotNull State s) {
        Type typeval = Analyzer.self.builtins.unknown;
        if (exceptions != null) {
            typeval = resolveUnion(exceptions, s);
        }
        if (binder != null) {
            Binder.bind(s, binder, typeval);
        }
        if (body != null) {
            return transformExpr(body, s);
        } else {
            return Analyzer.self.builtins.unknown;
        }
    }


    @NotNull
    @Override
    public String toString() {
        return "(handler:" + start + ":" + exceptions + ":" + binder + ")";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(binder, v);
            visitNodes(exceptions, v);
            visitNode(body, v);
        }
    }
}
