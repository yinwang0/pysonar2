package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;


public class Continue extends Node
{

    public Continue(int start, int end)
    {
        super(start, end);
    }


    @NotNull
    @Override
    public String toString()
    {
        return "<Continue>";
    }


    @NotNull
    @Override
    public Type resolve(Scope s)
    {
        return Indexer.idx.builtins.None;
    }


    @Override
    public void visit(@NotNull NodeVisitor v)
    {
        v.visit(this);
    }
}
