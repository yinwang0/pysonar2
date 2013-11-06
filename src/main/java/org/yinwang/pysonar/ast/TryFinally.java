package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

public class TryFinally extends Node {

    static final long serialVersionUID = 136428581711609107L;

    public Block body;
    public Block finalbody;


    public TryFinally(Block body, Block orelse, int start, int end) {
        super(start, end);
        this.body = body;
        this.finalbody = orelse;
        addChildren(body, orelse);
    }

    @Nullable
    @Override
    public Type resolve(Scope s, int tag) {
        Type tFinal = Indexer.idx.builtins.unknown;
        if (body != null) resolveExpr(body, s, tag);
        if (finalbody != null) tFinal = resolveExpr(finalbody, s, tag);
        return tFinal;
    }

    @NotNull
    @Override
    public String toString() {
        return "<TryFinally:" + body + ":" + finalbody + ">";
    }

    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(body, v);
            visitNode(finalbody, v);
        }
    }
}
