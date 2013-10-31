package org.yinwang.pysonar.ast;

import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

public class Continue extends Node {

    static final long serialVersionUID = 1646681898280823606L;


    public Continue(int start, int end) {
        super(start, end);
    }

    @Override
    public String toString() {
        return "<Continue>";
    }
    
    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        return Indexer.idx.builtins.None;
    }

    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }
}
