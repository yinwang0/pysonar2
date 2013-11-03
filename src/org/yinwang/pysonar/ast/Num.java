package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

public class Num extends Node {

    static final long serialVersionUID = -425866329526788376L;

    public Object n;


    public Num(Object n, int start, int end) {
        super(start, end);
        this.n = n;
    }

    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        return Indexer.idx.builtins.BaseNum;
    }

    @NotNull
    @Override
    public String toString() {
        return "<Num:" + n + ">";
    }

    @Override
    public void visit(@NotNull NodeVisitor v) {
        v.visit(this);
    }
}
