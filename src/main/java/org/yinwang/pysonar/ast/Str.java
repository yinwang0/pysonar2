package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;


public class Str extends Node {

    private String value;


    public Str(@NotNull Object value, int start, int end) {
        super(start, end);
        this.value = value.toString();
    }


    public String getStr() {
        return value;
    }


    @NotNull
    @Override
    public Type resolve(Scope s) {
        return Indexer.idx.builtins.BaseStr;
    }


    @NotNull
    @Override
    public String toString() {
        return "<Str>";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        v.visit(this);
    }
}
