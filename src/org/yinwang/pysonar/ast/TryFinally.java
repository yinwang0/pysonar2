package org.yinwang.pysonar.ast;

import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

public class TryFinally extends Node {

    static final long serialVersionUID = 136428581711609107L;

    public Block body;
    public Block finalbody;

    public TryFinally(Block body, Block orelse) {
        this(body, orelse, 0, 1);
    }

    public TryFinally(Block body, Block orelse, int start, int end) {
        super(start, end);
        this.body = body;
        this.finalbody = orelse;
        addChildren(body, orelse);
    }

    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        Type tFinal = Indexer.idx.builtins.unknown;
        if (body != null) resolveExpr(body, s, tag);
        if (finalbody != null) tFinal = resolveExpr(finalbody, s, tag);
        return tFinal;
    }

    @Override
    public String toString() {
        return "<TryFinally:" + body + ":" + finalbody + ">";
    }

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(body, v);
            visitNode(finalbody, v);
        }
    }
}
