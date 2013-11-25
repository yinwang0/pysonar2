package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;


public class Repr extends Node
{

    public Node value;


    public Repr(Node n, int start, int end)
    {
        super(start, end);
        this.value = n;
        addChildren(n);
    }


    @NotNull
    @Override
    public Type resolve(Scope s)
    {
        if (value != null)
        {
            resolveExpr(value, s);
        }
        return Indexer.idx.builtins.BaseStr;
    }


    @NotNull
    @Override
    public String toString()
    {
        return "<Repr:" + value + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v)
    {
        if (v.visit(this))
        {
            visitNode(value, v);
        }
    }
}
