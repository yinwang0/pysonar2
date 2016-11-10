package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GeneratorExp extends Node {

    public Node elt;
    public List<Comprehension> generators;

    public GeneratorExp(Node elt, List<Comprehension> generators, String file, int start, int end, int line, int col) {
        super(NodeType.GENERATOREXP, file, start, end, line, col);
        this.elt = elt;
        this.generators = generators;
        addChildren(elt);
        addChildren(generators);
    }

    @NotNull
    @Override
    public String toString() {
        return "<GeneratorExp:" + start + ":" + elt + ">";
    }

}
