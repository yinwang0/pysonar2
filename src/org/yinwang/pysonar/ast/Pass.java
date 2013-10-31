package org.yinwang.pysonar.ast;

import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

public class Pass extends Node {

    static final long serialVersionUID = 3668786487029793620L;


    public Pass(int start, int end) {
        super(start, end);
    }
    
    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        return Indexer.idx.builtins.Cont;
    }

    @Override
    public String toString() {
        return "<Pass>";
    }

    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }
}
