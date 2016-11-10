package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.Builtins;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.Type;

import java.util.List;

public class ClassDef extends Node {

    @NotNull
    public Name name;
    public List<Node> bases;
    public Node body;

    public ClassDef(@NotNull Name name, List<Node> bases, Node body, String file, int start, int end, int line, int col) {
        super(NodeType.CLASSDEF, file, start, end, line, col);
        this.name = name;
        this.bases = bases;
        this.body = body;
        addChildren(name, this.body);
        addChildren(bases);
    }

    public void addSpecialAttribute(@NotNull State s, String name, Type proptype) {
        Binding b = new Binding(name, Builtins.newTutUrl("classes.html"), proptype, Binding.Kind.ATTRIBUTE);
        s.update(name, b);
        b.markSynthetic();
        b.markStatic();

    }

    @NotNull
    @Override
    public String toString() {
        return "(class:" + name.id + ":" + start + ")";
    }

}
