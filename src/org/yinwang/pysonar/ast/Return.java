package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

public class Return extends Node {

    static final long serialVersionUID = 5795610129307339141L;

    public Node value;


    public Return(Node n, int start, int end) {
        super(start, end);
        this.value = n;
        addChildren(n);
    }

    @Nullable
    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        if (value == null) {
            return Indexer.idx.builtins.None;
        } else {
            return resolveExpr(value, s, tag);
        }
    }

    @NotNull
    @Override
    public String toString() {
        return "<Return:" + value + ">";
    }

    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(value, v);
        }
    }
}
