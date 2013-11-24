package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

import java.util.List;

public class Global extends Node {

    private List<Name> names;


    public Global(List<Name> names, int start, int end) {
        super(start, end);
        this.names = names;
        addChildren(names);
    }

    @NotNull
    @Override
    public Type resolve(Scope s) {
        // Do nothing here because global names are processed by NBlock
        return Indexer.idx.builtins.Cont;
    }

    public List<Name> getNames() {
        return names;
    }
    
    @NotNull
    @Override
    public String toString() {
        return "<Global:" + names + ">";
    }

    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNodeList(names, v);
        }
    }
}
