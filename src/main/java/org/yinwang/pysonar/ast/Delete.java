package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

import java.util.List;


public class Delete extends Node
{

    public List<Node> targets;


    public Delete(List<Node> elts, int start, int end)
    {
        super(start, end);
        this.targets = elts;
        addChildren(elts);
    }


    @NotNull
    @Override
    public Type resolve(@NotNull Scope s)
    {
        for (Node n : targets)
        {
            resolveExpr(n, s);
            if (n instanceof Name)
            {
                s.remove(n.asName().getId());
            }
        }
        return Indexer.idx.builtins.Cont;
    }


    @NotNull
    @Override
    public String toString()
    {
        return "<Delete:" + targets + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v)
    {
        if (v.visit(this))
        {
            visitNodeList(targets, v);
        }
    }
}
