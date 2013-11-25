package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;


public class Exec extends Node
{

    public Node body;
    public Node globals;
    public Node locals;


    public Exec(Node body, Node globals, Node locals, int start, int end)
    {
        super(start, end);
        this.body = body;
        this.globals = globals;
        this.locals = locals;
        addChildren(body, globals, locals);
    }


    @NotNull
    @Override
    public Type resolve(Scope s)
    {
        if (body != null)
        {
            resolveExpr(body, s);
        }
        if (globals != null)
        {
            resolveExpr(globals, s);
        }
        if (locals != null)
        {
            resolveExpr(locals, s);
        }
        return Indexer.idx.builtins.Cont;
    }


    @NotNull
    @Override
    public String toString()
    {
        return "<Exec:" + start + ":" + end + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v)
    {
        if (v.visit(this))
        {
            visitNode(body, v);
            visitNode(globals, v);
            visitNode(locals, v);
        }
    }
}
