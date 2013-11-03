package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

public class IfExp extends Node {

    static final long serialVersionUID = 8516153579808365723L;

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

    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        Type type1, type2;
        resolveExpr(test, s, tag);
        int newTag = Indexer.idx.newThread();
        if (body != null) {
            type1 = resolveExpr(body, s, newTag);
        } else {
            type1 = Indexer.idx.builtins.Cont;
        }
        if (orelse != null) {
            type2 = resolveExpr(orelse, s, -newTag);
        } else {
            type2 = Indexer.idx.builtins.Cont;
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
