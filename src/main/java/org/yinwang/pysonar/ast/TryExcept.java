package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

import java.util.List;


public class TryExcept extends Node {

    public List<ExceptHandler> handlers;
    public Block body;
    public Block orelse;


    public TryExcept(List<ExceptHandler> handlers, Block body, Block orelse,
                     int start, int end)
    {
        super(start, end);
        this.handlers = handlers;
        this.body = body;
        this.orelse = orelse;
        addChildren(handlers);
        addChildren(body, orelse);
    }


    @NotNull
    @Override
    public Type resolve(Scope s) {
        Type tp1 = Analyzer.self.builtins.unknown;
        Type tp2 = Analyzer.self.builtins.unknown;
        Type tph = Analyzer.self.builtins.unknown;

        for (ExceptHandler h : handlers) {
            tph = UnionType.union(tph, resolveExpr(h, s));
        }

        if (body != null) {
            tp1 = resolveExpr(body, s);
        }
        if (orelse != null) {
            tp2 = resolveExpr(orelse, s);
        }

        return UnionType.union(tp1, UnionType.union(tp2, tph));
    }


    @NotNull
    @Override
    public String toString() {
        return "<TryExcept:" + handlers + ":" + body + ":" + orelse + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNodeList(handlers, v);
            visitNode(body, v);
            visitNode(orelse, v);
        }
    }
}
