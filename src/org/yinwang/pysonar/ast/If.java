package org.yinwang.pysonar.ast;

import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

public class If extends Node {

    static final long serialVersionUID = -3744458754599083332L;

    public Node test;
    public Block body;
    public Block orelse;


    public If(Node test, Block body, Block orelse, int start, int end) {
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

        if (body != null && !body.isEmpty()) {
            type1 = resolveExpr(body, s, newTag);
        } else {
            type1 = Indexer.idx.builtins.Cont;
        }

        Scope s2 = new Scope(s);
        if (orelse != null && !orelse.isEmpty()) {
            type2 = resolveExpr(orelse, s2, -newTag);
        } else {
            type2 = Indexer.idx.builtins.Cont;
        }

        return UnionType.union(type1, type2);
    }

    @Override
    public String toString() {
        return "<If:" + start + ":" + test + ":" + body + ":" + orelse + ">";
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
