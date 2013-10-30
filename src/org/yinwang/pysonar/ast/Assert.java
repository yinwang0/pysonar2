package org.yinwang.pysonar.ast;

import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

public class Assert extends Node {

    static final long serialVersionUID = 7574732756076428388L;

    public Node test;
    public Node msg;

    public Assert(Node test, Node msg) {
        this(test, msg, 0, 1);
    }

    public Assert(Node test, Node msg, int start, int end) {
        super(start, end);
        this.test = test;
        this.msg = msg;
        addChildren(test, msg);
    }

    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        if (test != null) resolveExpr(test, s, tag);
        if (msg != null) resolveExpr(msg, s, tag);
        return Indexer.idx.builtins.Cont;
    }

    @Override
    public String toString() {
        return "<Assert:" + test + ":" + msg + ">";
    }

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(test, v);
            visitNode(msg, v);
        }
    }
}
