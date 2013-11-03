package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.*;
import org.yinwang.pysonar.types.ModuleType;
import org.yinwang.pysonar.types.Type;

import java.util.List;

public class Import extends Node {

    static final long serialVersionUID = -2180402676651342012L;

    public List<Alias> names;


    public Import(List<Alias> names, int start, int end) {
        super(start, end);
        this.names = names;
        for (Alias a : names) {
            for (Name n : a.name) {
                if (n == null) {
                    Util.msg("segment is null!");
                }
            }
        }
        addChildren(names);
    }


    @Override
    public Type resolve(@NotNull Scope s, int tag) {
        for (Alias a : names) {
            ModuleType mod = Indexer.idx.loadModule(a.name, s, tag);
            if (mod == null) {
                Indexer.idx.putProblem(this, "Can't load module");
            } else if (a.asname != null) {
                s.put(a.asname.id, a.asname, mod, Binding.Kind.MODULE, tag);
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
