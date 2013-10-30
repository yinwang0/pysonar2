package org.yinwang.pysonar.ast;

import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.ListType;
import org.yinwang.pysonar.types.Type;

public class Yield extends Node {

    static final long serialVersionUID = 2639481204205358048L;

    public Node value;

    public Yield(Node n) {
        this(n, 0, 1);
    }

    public Yield(Node n, int start, int end) {
        super(start, end);
        this.value = n;
        addChildren(n);
    }

    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        if (value != null) {
            return new ListType(resolveExpr(value, s, tag));
        } else {
            return Indexer.idx.builtins.None;
        }
    }

    @Override
    public String toString() {
        return "<Yield:" + start + ":" + value + ">";
    }

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(value, v);
        }
    }
}
