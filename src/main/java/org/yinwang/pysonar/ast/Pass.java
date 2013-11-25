package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;


public class Pass extends Node
{

    public Pass(int start, int end)
    {
        super(start, end);
    }


    @NotNull
    @Override
    public Type resolve(Scope s)
    {
        return Indexer.idx.builtins.Cont;
    }


    @NotNull
    @Override
    public String toString()
    {
        return "<Pass>";
    }


    @Override
    public void visit(@NotNull NodeVisitor v)
    {
        v.visit(this);
    }
}
