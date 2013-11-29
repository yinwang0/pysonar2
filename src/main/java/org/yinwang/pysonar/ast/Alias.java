package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

import java.util.List;


public class Alias extends Node {

    public List<Name> name;
    public Name asname;


    public Alias(List<Name> name, Name asname, int start, int end) {
        super(start, end);
        this.name = name;
        this.asname = asname;
        addChildren(name);
        addChildren(asname);
    }


    @NotNull
    @Override
    public Type resolve(Scope s) {
        return Analyzer.self.builtins.unknown;
    }


    @NotNull
    @Override
    public String toString() {
        return "<Alias:" + name + " as " + asname + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            for (Name n : name) {
                visitNode(n, v);
            }
            visitNode(asname, v);
        }
    }
}
