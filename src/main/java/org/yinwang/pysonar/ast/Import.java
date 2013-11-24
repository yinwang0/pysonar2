package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.types.ModuleType;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;

import java.util.List;

public class Import extends Node {

    public List<Alias> names;

    public Import(List<Alias> names, int start, int end) {
        super(start, end);
        this.names = names;
        addChildren(names);
    }


    @NotNull
    @Override
    public Type resolve(@NotNull Scope s) {
        for (Alias a : names) {
            ModuleType mod = Indexer.idx.loadModule(a.name, s);
            if (mod == null) {
                Indexer.idx.putProblem(this, "Cannot load module");
            } else if (a.asname != null) {
                s.put(a.asname.id, a.asname, mod, Binding.Kind.VARIABLE);
            }
        }
        return Indexer.idx.builtins.Cont;
    }


    @NotNull
    @Override
    public String toString() {
        return "<Import:" + names + ">";
    }

    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNodeList(names, v);
        }
    }
}
