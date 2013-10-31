package org.yinwang.pysonar.ast;

import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

import java.util.List;

public class Print extends Node {

    static final long serialVersionUID = 689872588518071148L;

    public Node dest;
    public List<Node> values;


    public Print(Node dest, List<Node> elts, int start, int end) {
        super(start, end);
        this.dest = dest;
        this.values = elts;
        addChildren(dest);
        addChildren(elts);
    }

    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        if (dest != null) resolveExpr(dest, s, tag);
        if (values != null) resolveList(values, s, tag);
        return Indexer.idx.builtins.Cont;
    }

    @Override
    public String toString() {
        return "<Print:" + values + ">";
    }

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(dest, v);
            visitNodeList(values, v);
        }
    }
}
