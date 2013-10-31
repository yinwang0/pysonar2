package org.yinwang.pysonar.ast;

import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

public class While extends Node {

    static final long serialVersionUID = -2419753875936526587L;

    public Node test;
    public Block body;
    public Block orelse;


    public While(Node test, Block body, Block orelse, int start, int end) {
        super(start, end);
        this.test = test;
        this.body = body;
        this.orelse = orelse;
        addChildren(test, body, orelse);
    }

    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        resolveExpr(test, s, tag);
        Type t = Indexer.idx.builtins.unknown;

        if (body != null) {
            t = resolveExpr(body, s, tag);
        }

        if (orelse != null) {
            t = UnionType.union(t, resolveExpr(orelse, s, tag));
        }

        return t;
    }

    @Override
    public String toString() {
        return "<While:" + test + ":" + body + ":" + orelse + ":" + start + ">";
    }

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(test, v);
            visitNode(body, v);
            visitNode(orelse, v);
        }
    }
}
