package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;


public abstract class Sequence extends Node
{

    @NotNull
    public List<Node> elts;


    public Sequence(@NotNull List<Node> elts, int start, int end)
    {
        super(start, end);
        this.elts = elts;
        addChildren(elts);
    }


    @NotNull
    public List<Node> getElements()
    {
        return elts;
    }
}
