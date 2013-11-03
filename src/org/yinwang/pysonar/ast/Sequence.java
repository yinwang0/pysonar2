package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class Sequence extends Node {

    static final long serialVersionUID = 7996591535766850065L;

    @Nullable
    public List<Node> elts;


    public Sequence(@Nullable List<Node> elts, int start, int end) {
        super(start, end);
        this.elts = (elts != null) ? elts : new ArrayList<Node>();
        addChildren(elts);
    }

    @Nullable
    public List<Node> getElements() {
        return elts;
    }
}
