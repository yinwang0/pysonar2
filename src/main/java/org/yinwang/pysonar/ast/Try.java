package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

import java.util.List;


public class Try extends Node {

    public List<ExceptHandler> handlers;
    public Block body;
    public Block orelse;
    public Block finalbody;

    public Try(List<ExceptHandler> handlers, Block body, Block orelse, Block finalbody,
               int start, int end) {
        super(start, end);
        this.handlers = handlers;
        this.body = body;
        this.orelse = orelse;
        this.finalbody = finalbody;
        addChildren(handlers);
        addChildren(body, orelse);
    }


    @NotNull
    @Override
    public Type resolve(Scope s) {
        Type tp1 = Analyzer.self.builtins.unknown;
        Type tp2 = Analyzer.self.builtins.unknown;
        Type tph = Analyzer.self.builtins.unknown;
        Type tpFinal = Analyzer.self.builtins.unknown;

        if (handlers != null) {
            for (ExceptHandler h : handlers) {
                tph = UnionType.union(tph, resolveExpr(h, s));
            }
        }

        if (body != null) {
            tp1 = resolveExpr(body, s);
        }

        if (orelse != null) {
            tp2 = resolveExpr(orelse, s);
        }

        if (finalbody != null) {
            tpFinal = resolveExpr(finalbody, s);
        }

        return new UnionType(tp1, tp2, tph, tpFinal);
    }


    @NotNull
    @Override
    public String toString() {
        return "<Try:" + handlers + ":" + body + ":" + orelse + ">";
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
