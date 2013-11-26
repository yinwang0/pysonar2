package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

import java.util.List;


public class Print extends Node {

    public Node dest;
    public List<Node> values;


    public Print(Node dest, List<Node> elts, int start, int end) {
        super(start, end);
        this.dest = dest;
        this.values = elts;
        addChildren(dest);
        addChildren(elts);
    }


    @NotNull
    @Override
    public Type resolve(Scope s) {
        if (dest != null) {
            resolveExpr(dest, s);
        }
        if (values != null) {
            resolveList(values, s);
        }
        return Indexer.idx.builtins.Cont;
    }


    @NotNull
    @Override
    public String toString() {
        return "<Print:" + values + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(dest, v);
            visitNodeList(values, v);
        }
    }
}
