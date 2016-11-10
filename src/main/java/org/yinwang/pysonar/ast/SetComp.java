package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SetComp extends Node {

    public Node elt;
    public List<Comprehension> generators;

    public SetComp(Node elt, List<Comprehension> generators, String file, int start, int end, int line, int col) {
        super(NodeType.SETCOMP, file, start, end, line, col);
        this.elt = elt;
        this.generators = generators;
        addChildren(elt);
        addChildren(generators);
    }

    @NotNull
    @Override
    public String toString() {
        return "<NSetComp:" + start + ":" + elt + ">";
    }

}
