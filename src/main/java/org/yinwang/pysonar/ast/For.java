package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;


public class For extends Node
{

    public Node target;
    public Node iter;
    public Block body;
    public Block orelse;


    public For(Node target, Node iter, Block body, Block orelse,
               int start, int end)
    {
        super(start, end);
        this.target = target;
        this.iter = iter;
        this.body = body;
        this.orelse = orelse;
        addChildren(target, iter, body, orelse);
    }


    @Override
    public boolean bindsName()
    {
        return true;
    }


    @NotNull
    @Override
    public Type resolve(@NotNull Scope s)
    {
        NameBinder.bindIter(s, target, iter, Binding.Kind.SCOPE);

        Type ret;
        if (body == null)
        {
            ret = Indexer.idx.builtins.unknown;
        }
        else
        {
            ret = resolveExpr(body, s);
        }
        if (orelse != null)
        {
            ret = UnionType.union(ret, resolveExpr(orelse, s));
        }
        return ret;
    }


    @NotNull
    @Override
    public String toString()
    {
        return "<For:" + target + ":" + iter + ":" + body + ":" + orelse + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v)
    {
        if (v.visit(this))
        {
            visitNode(target, v);
            visitNode(iter, v);
            visitNode(body, v);
            visitNode(orelse, v);
        }
    }
}
