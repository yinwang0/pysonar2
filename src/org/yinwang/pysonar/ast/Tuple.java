package org.yinwang.pysonar.ast;

import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.TupleType;
import org.yinwang.pysonar.types.Type;

import java.util.List;

public class Tuple extends Sequence {

    static final long serialVersionUID = -7647425038559142921L;

    public Tuple(List<Node> elts) {
        this(elts, 0, 1);
    }

    public Tuple(List<Node> elts, int start, int end) {
        super(elts, start, end);
    }

    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        TupleType t = new TupleType();
        for (Node e : elts) {
            t.add(resolveExpr(e, s, tag));
        }
        return t;
    }

    @Override
    public String toString() {
        return "<Tuple:" + start + ":" + elts + ">";
    }

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            visitNodeList(elts, v);
        }
    }
}
