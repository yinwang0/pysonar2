package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

import java.util.List;


public class Compare extends Node
{

    @NotNull
    public Node left;
    public List<Node> ops;
    public List<Node> comparators;


    public Compare(@NotNull Node left, List<Node> ops, List<Node> comparators, int start, int end)
    {
        super(start, end);
        this.left = left;
        this.ops = ops;
        this.comparators = comparators;
        addChildren(left);
        addChildren(ops);
        addChildren(comparators);
    }


    @NotNull
    @Override
    public Type resolve(Scope s)
    {
        resolveExpr(left, s);
        resolveList(comparators, s);
        return Indexer.idx.builtins.BaseNum;
    }


    @NotNull
    @Override
    public String toString()
    {
        return "<Compare:" + left + ":" + ops + ":" + comparators + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v)
    {
        if (v.visit(this))
        {
            visitNode(left, v);
            visitNodeList(ops, v);
            visitNodeList(comparators, v);
        }
    }
}
