package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;


/**
 * Non-AST node used to represent virtual source locations for builtins
 * as external urls.
 */
public class Url extends Node
{

    private String url;


    public Url(String url)
    {
        this.url = url;
    }


    public String getURL()
    {
        return url;
    }


    @NotNull
    @Override
    public Type resolve(Scope s)
    {
        return Indexer.idx.builtins.BaseStr;
    }


    @NotNull
    @Override
    public String toString()
    {
        return "<Url:\"" + url + "\">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v)
    {
        v.visit(this);
    }
}
