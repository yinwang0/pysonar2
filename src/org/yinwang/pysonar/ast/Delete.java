package org.yinwang.pysonar.ast;

import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

import java.util.List;

public class Delete extends Node {

    static final long serialVersionUID = -2223255555054110766L;

    public List<Node> targets;


    public Delete(List<Node> elts, int start, int end) {
        super(start, end);
        this.targets = elts;
        addChildren(elts);
    }

    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        for (Node n : targets) {
            resolveExpr(n, s, tag);
            if (n instanceof Name) {
                s.remove(n.asName().getId());
            }
        }
        return Indexer.idx.builtins.Cont;
    }

    @Override
    public String toString() {
        return "<Delete:" + targets + ">";
    }

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            visitNodeList(targets, v);
        }
    }
}
