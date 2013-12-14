package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.SymbolType;
import org.yinwang.pysonar.types.Type;


public class Symbol extends Node {

    @NotNull
    public final String id;  // identifier


    public Symbol(@NotNull String id, int start, int end) {
        super(start, end);
        this.id = id;
    }


    @NotNull
    @Override
    public Type transform(@NotNull State s) {
        return new SymbolType(id);
    }


    @NotNull
    @Override
    public String toString() {
        return "(sym:" + id + ":" + start + ")";
    }


    @NotNull
    @Override
    public String toDisplay() {
        return id;
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        v.visit(this);
    }
}
