package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;


public class Bytes extends Node {

    private Object value;


    public Bytes(@NotNull Object value, int start, int end) {
        super(start, end);
        this.value = value.toString();
    }


    public Object getStr() {
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
        return "<Bytpes: " + value + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        v.visit(this);
    }
}
