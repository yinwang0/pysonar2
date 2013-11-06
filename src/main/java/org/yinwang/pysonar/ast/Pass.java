package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

public class Pass extends Node {

    static final long serialVersionUID = 3668786487029793620L;


    public Pass(int start, int end) {
        super(start, end);
    }
    
    @Override
    public Type resolve(Scope s, int tag) {
        return Indexer.idx.builtins.Cont;
    }

    @NotNull
    @Override
    public String toString() {
        return "<Pass>";
    }

    @Override
    public void visit(@NotNull NodeVisitor v) {
        v.visit(this);
    }
}
