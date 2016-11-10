package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Alias extends Node {

    public List<Name> name;
    public Name asname;

    public Alias(List<Name> name, Name asname, String file, int start, int end, int line, int col) {
        super(NodeType.ALIAS, file, start, end, line, col);
        this.name = name;
        this.asname = asname;
        addChildren(name);
        addChildren(asname);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Alias:" + name + " as " + asname + ">";
    }

}
