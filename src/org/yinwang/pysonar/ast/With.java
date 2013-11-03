package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

public class With extends Node {

    static final long serialVersionUID = 560128079414064421L;

    public Node optional_vars;
    public Node context_expr;
    public Block body;


    public With(Node optional_vars, Node context_expr, Block body, int start, int end) {
        super(start, end);
        this.optional_vars = optional_vars;
        this.context_expr = context_expr;
        this.body = body;
        addChildren(optional_vars, context_expr, body);
    }

    @Nullable
    @Override
    public Type resolve(@NotNull Scope s, int tag) {
        Type val = resolveExpr(context_expr, s, tag);
        NameBinder.bind(s, optional_vars, val, tag);
        return resolveExpr(body, s, tag);
    }

    @NotNull
    @Override
    public String toString() {
        return "<With:" + context_expr + ":" + optional_vars + ":" + body + ">";
    }

    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(optional_vars, v);
            visitNode(context_expr, v);
            visitNode(body, v);
        }
    }
}
