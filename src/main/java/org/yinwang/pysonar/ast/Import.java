package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Import extends Node {

    public List<Alias> names;

    public Import(List<Alias> names, String file, int start, int end) {
        super(NodeType.IMPORT, file, start, end);
        this.names = names;
        addChildren(names);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Import:" + names + ">";
    }

}
