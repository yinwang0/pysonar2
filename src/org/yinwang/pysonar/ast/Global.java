package org.yinwang.pysonar.ast;

import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

import java.util.List;

public class Global extends Node {

    static final long serialVersionUID = 5978320165592263568L;

    private List<Name> names;


    public Global(List<Name> names, int start, int end) {
        super(start, end);
        this.names = names;
        addChildren(names);
    }

    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        // Do nothing here because global names are processed by NBlock
        return Indexer.idx.builtins.Cont;
    }

    public List<Name> getNames() {
        return names;
    }
    
    @Override
    public String toString() {
        return "<Global:" + names + ">";
    }

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            visitNodeList(names, v);
        }
    }
}
