package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

public class Ellipsis extends Node {

    static final long serialVersionUID = 4148534089952252511L;


    public Ellipsis(int start, int end) {
        super(start, end);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Ellipsis>";
    }
    
    @Override
    public Type resolve(Scope s, int tag) {
        return Indexer.idx.builtins.None;
    }

    @Override
    public void visit(@NotNull NodeVisitor v) {
        v.visit(this);
    }
}
